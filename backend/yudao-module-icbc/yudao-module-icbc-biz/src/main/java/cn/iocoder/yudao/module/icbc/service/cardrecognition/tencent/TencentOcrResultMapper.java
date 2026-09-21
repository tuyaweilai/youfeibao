package cn.iocoder.yudao.module.icbc.service.cardrecognition.tencent;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.icbc.enums.IcbcAccountCodeEnum;
import cn.iocoder.yudao.module.icbc.service.cardrecognition.CardRecognitionPort;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 把腾讯云 OCR 的响应报文映射成 {@link CardRecognitionPort} 的结果（#93）。
 *
 * <p>厂商差异全部收敛在这里：业务层只看到库内格式与可读文案（ADR 0037）。映射是**纯函数**，
 * 测试喂厂商报文样本、不触网——本票的主战场就是它（边界条件多：有效期两种写法、行名带联行号、
 * 电子卡截图、临时身份证、复印件 / 翻拍 / 边框 / 反光）。
 *
 * <p>三条硬编码的库内口径：
 * <ul>
 *   <li>证件有效期：厂商 {@code 2018.08.12-2038.08.12} → {@code 2018-08-12} / {@code 2038-08-12}；
 *       「长期」→ {@code 9999-12-30}；解析不出来（且厂商给了值）→ 硬拦「有效期不合法」。</li>
 *   <li>开户行：剥掉尾部括号里的联行号（{@code 招商银行(03080000)} → {@code 招商银行}，ADR 0035 不做联行号）；
 *       行名含「工商银行 / 工行 / ICBC」→ {@code accountCode=1}，否则 {@code 0}。</li>
 *   <li>拒收 / 告警：厂商的布尔标志与 {@code WarnInfos} 文案在这里分成「提示不拦」与「硬拦」两类。</li>
 * </ul>
 *
 * <p>签发机关（{@code Authority}）**不入库**：端口里没有这个字段（ADR 0037）。
 */
public final class TencentOcrResultMapper {

    /** 长期有效期：与库内既有约定一致（{@code icbc_payee_info.id_validity_period}）。 */
    public static final String LONG_TERM_VALIDITY = "9999-12-30";

    /** 有效期：`起-止`，起止可以是 `yyyy.MM.dd` / `yyyy-MM-dd` / `yyyy/MM/dd`，止可以是「长期」。 */
    private static final Pattern VALIDITY_PATTERN = Pattern.compile(
            "^(\\d{4}[.\\-/]\\d{1,2}[.\\-/]\\d{1,2})\\s*[-~—至]\\s*"
                    + "(长期|长期有效|\\d{4}[.\\-/]\\d{1,2}[.\\-/]\\d{1,2})$");

    private static final String BLOCK_TEMP_ID = "临时身份证";
    private static final String BLOCK_INVALID_VALIDITY = "有效期不合法";
    private static final String BLOCK_ELECTRONIC_CARD =
            "这是电子银行卡信息截图，不是实体卡照片，请拍实体卡正面";

    private TencentOcrResultMapper() {
    }

    // ==================== 身份证人像面 ====================

    public static CardRecognitionPort.IdCardFront toIdCardFront(JSONObject response) {
        if (response == null) {
            return CardRecognitionPort.IdCardFront.empty();
        }
        JSONObject advanced = advancedInfo(response);
        Warnings warnings = collectWarnings(advanced, idCardFlagWarnings());
        return CardRecognitionPort.IdCardFront.builder()
                .name(text(response, "Name"))
                .idCardNo(text(response, "IdNum"))
                .address(text(response, "Address"))
                .qualityScore(qualityOf(response, advanced))
                .warnings(warnings.warnings)
                .blockReasons(warnings.blockReasons)
                .build();
    }

    // ==================== 身份证国徽面 ====================

    public static CardRecognitionPort.IdCardBack toIdCardBack(JSONObject response) {
        if (response == null) {
            return CardRecognitionPort.IdCardBack.empty();
        }
        JSONObject advanced = advancedInfo(response);
        Warnings warnings = collectWarnings(advanced, idCardFlagWarnings());
        String rawValidDate = text(response, "ValidDate");
        String[] parsed = parseValidity(rawValidDate);
        if (parsed == null && StrUtil.isNotBlank(rawValidDate)) {
            // 厂商给了有效期但读不成合法的起止：这正是工行入驻的必输项，不能带着一个错的日期往下走
            warnings.addBlock(BLOCK_INVALID_VALIDITY);
        }
        return CardRecognitionPort.IdCardBack.builder()
                .idSignDate(parsed == null ? null : parsed[0])
                .idValidityPeriod(parsed == null ? null : parsed[1])
                .qualityScore(qualityOf(response, advanced))
                .warnings(warnings.warnings)
                .blockReasons(warnings.blockReasons)
                .build();
    }

    // ==================== 银行卡 ====================

    public static CardRecognitionPort.BankCard toBankCard(JSONObject response) {
        if (response == null) {
            return CardRecognitionPort.BankCard.empty();
        }
        JSONObject advanced = advancedInfo(response);
        Warnings warnings = collectWarnings(advanced, bankCardFlagWarnings());
        String cardType = firstNonBlank(text(response, "CardType"), text(advanced, "CardType"));
        if (cardType != null && cardType.contains("电子银行卡")) {
            warnings.addBlock(BLOCK_ELECTRONIC_CARD);
        }
        String bankName = normalizeBankName(firstNonBlank(text(response, "BankInfo"), text(response, "BankName")));
        return CardRecognitionPort.BankCard.builder()
                .bankCardNo(text(response, "CardNo"))
                .bankName(bankName)
                .accountCode(inferAccountCode(bankName))
                .qualityScore(qualityOf(response, advanced))
                .warnings(warnings.warnings)
                .blockReasons(warnings.blockReasons)
                .build();
    }

    // ==================== 有效期 ====================

    /**
     * 解析厂商的有效期：{@code 2018.08.12-2038.08.12} / {@code 2018.08.12-长期}。
     *
     * @return {@code [签发日期, 有效期截止]}；解析不出来时返回 {@code null}（空值也返回 {@code null}，
     * 由调用方区分「厂商没给」与「给了但不合法」）
     */
    static String[] parseValidity(String raw) {
        if (StrUtil.isBlank(raw)) {
            return null;
        }
        Matcher matcher = VALIDITY_PATTERN.matcher(raw.trim());
        if (!matcher.matches()) {
            return null;
        }
        String signDate = normalizeDate(matcher.group(1));
        if (signDate == null) {
            return null;
        }
        String endToken = matcher.group(2);
        String validity = endToken.startsWith("长期") ? LONG_TERM_VALIDITY : normalizeDate(endToken);
        if (validity == null) {
            return null;
        }
        return new String[]{signDate, validity};
    }

    /**
     * 归一化到库内格式 {@code yyyy-MM-dd}；月日非法（如 {@code 2018.13.40}）返回 {@code null}。
     */
    static String normalizeDate(String raw) {
        if (StrUtil.isBlank(raw)) {
            return null;
        }
        String[] parts = raw.trim().split("[.\\-/]");
        if (parts.length != 3) {
            return null;
        }
        try {
            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            int day = Integer.parseInt(parts[2]);
            LocalDate date = LocalDate.of(year, month, day);
            return String.format("%04d-%02d-%02d", date.getYear(), date.getMonthValue(), date.getDayOfMonth());
        } catch (RuntimeException e) {
            return null;
        }
    }

    // ==================== 开户行 ====================

    /**
     * 剥掉行名尾部括号里的联行号：{@code 招商银行(03080000)} → {@code 招商银行}。
     */
    static String normalizeBankName(String raw) {
        if (StrUtil.isBlank(raw)) {
            return null;
        }
        String name = raw.trim().replaceAll("[（(][^）)]*[）)]", "").trim();
        return StrUtil.emptyToNull(name);
    }

    /**
     * 由开户行名推断「是否我行用户」：工商银行 → {@code 1}，其余 → {@code 0}，行名为空 → {@code null}。
     *
     * <p>它只是缺省值的一个更好的来源，**仍以本人在确认页的选择为准**（#93 / #91 评审 SP-2）。
     */
    static String inferAccountCode(String bankName) {
        if (StrUtil.isBlank(bankName)) {
            return null;
        }
        String name = bankName.replace(" ", "");
        boolean icbc = name.contains("工商银行") || name.contains("工行")
                || name.toUpperCase().contains("ICBC");
        return icbc ? IcbcAccountCodeEnum.ICBC.getCode() : IcbcAccountCodeEnum.NON_ICBC.getCode();
    }

    // ==================== 告警 / 拒收 ====================

    /**
     * 身份证提示类标志 → 可读文案（不拦继续）。
     */
    private static List<FlagWarning> idCardFlagWarnings() {
        List<FlagWarning> flags = new ArrayList<>();
        flags.add(new FlagWarning("CopyWarn", "身份证复印件"));
        flags.add(new FlagWarning("BorderCheckWarn", "身份证边框不完整"));
        flags.add(new FlagWarning("ReshootWarn", "身份证翻拍"));
        flags.add(new FlagWarning("DetectPsWarn", "疑似 PS 处理"));
        flags.add(new FlagWarning("ReflectWarn", "身份证反光"));
        return flags;
    }

    /**
     * 银行卡提示类标志 → 可读文案（不拦继续）。电子银行卡截图是**硬拦**，单独判。
     */
    private static List<FlagWarning> bankCardFlagWarnings() {
        List<FlagWarning> flags = new ArrayList<>();
        flags.add(new FlagWarning("CopyWarn", "银行卡复印件"));
        flags.add(new FlagWarning("BorderCheckWarn", "银行卡边框不完整"));
        flags.add(new FlagWarning("ReshootWarn", "银行卡翻拍"));
        flags.add(new FlagWarning("ReflectWarn", "银行卡反光"));
        return flags;
    }

    /**
     * 汇总告警：布尔标志 + {@code WarnInfos} 文案，并把硬拦类（临时身份证 / 有效期不合法 /
     * 电子银行卡截图）分到 {@code blockReasons}。
     */
    private static Warnings collectWarnings(JSONObject advanced, List<FlagWarning> flags) {
        Warnings result = new Warnings();
        for (FlagWarning flag : flags) {
            if (flagOf(advanced, flag.name)) {
                result.add(flag.text);
            }
        }
        if (flagOf(advanced, "TempIdWarn")) {
            result.add(BLOCK_TEMP_ID);
        }
        for (String warn : warnInfos(advanced)) {
            result.add(warn);
        }
        return result;
    }

    private static final class FlagWarning {
        private final String name;
        private final String text;

        private FlagWarning(String name, String text) {
            this.name = name;
            this.text = text;
        }
    }

    private static final class Warnings {
        private final List<String> warnings = new ArrayList<>();
        private final List<String> blockReasons = new ArrayList<>();

        private void add(String text) {
            String value = StrUtil.trimToNull(text);
            if (value == null) {
                return;
            }
            if (isBlocking(value)) {
                if (!blockReasons.contains(value)) {
                    blockReasons.add(value);
                }
            } else if (!warnings.contains(value)) {
                warnings.add(value);
            }
        }

        private void addBlock(String text) {
            String value = StrUtil.trimToNull(text);
            if (value != null && !blockReasons.contains(value)) {
                blockReasons.add(value);
            }
        }

        private static boolean isBlocking(String text) {
            if (text.contains(BLOCK_TEMP_ID)) {
                return true;
            }
            if (text.contains("电子银行卡")) {
                return true;
            }
            return text.contains("有效期")
                    && (text.contains("不合法") || text.contains("无效") || text.contains("异常"));
        }
    }

    // ==================== 厂商报文的读取 ====================

    /**
     * {@code AdvancedInfo} 可能是一段 JSON 字符串，也可能已经是对象；两种都要吃得下。
     */
    static JSONObject advancedInfo(JSONObject response) {
        Object raw = response.get("AdvancedInfo");
        if (raw instanceof JSONObject) {
            return (JSONObject) raw;
        }
        if (raw instanceof String && StrUtil.isNotBlank((String) raw)) {
            try {
                return JSON.parseObject((String) raw);
            } catch (RuntimeException e) {
                return new JSONObject();
            }
        }
        return new JSONObject();
    }

    /**
     * 读取布尔标志；厂商有时给 {@code true}、有时给字符串 {@code "true"}、有时给 {@code 1}。
     */
    static boolean flagOf(JSONObject source, String name) {
        if (source == null) {
            return false;
        }
        Object value = source.get(name);
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        String text = String.valueOf(value).trim();
        return "true".equalsIgnoreCase(text) || "1".equals(text) || "是".equals(text);
    }

    /**
     * {@code WarnInfos}：腾讯返回的是文案数组；容错地也接受逗号分隔字符串。
     */
    static List<String> warnInfos(JSONObject advanced) {
        List<String> result = new ArrayList<>();
        if (advanced == null) {
            return result;
        }
        Object raw = advanced.get("WarnInfos");
        if (raw instanceof JSONArray) {
            for (Object item : (JSONArray) raw) {
                if (item != null) {
                    result.add(String.valueOf(item));
                }
            }
        } else if (raw instanceof String && StrUtil.isNotBlank((String) raw)) {
            for (String item : ((String) raw).split("[,，;；]")) {
                if (StrUtil.isNotBlank(item)) {
                    result.add(item.trim());
                }
            }
        }
        return result;
    }

    /**
     * 图片质量分：银行卡在顶层 {@code Quality}，身份证在 {@code AdvancedInfo.Quality}，两处都找。
     */
    static Integer qualityOf(JSONObject response, JSONObject advanced) {
        Integer quality = integerOf(advanced.get("Quality"));
        if (quality == null && response != null) {
            quality = integerOf(response.get("Quality"));
        }
        return quality;
    }

    private static Integer integerOf(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.valueOf(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String text(JSONObject source, String name) {
        if (source == null) {
            return null;
        }
        return StrUtil.trimToNull(source.getString(name));
    }

    private static String firstNonBlank(String first, String second) {
        return StrUtil.isNotBlank(first) ? first : StrUtil.trimToNull(second);
    }

}
