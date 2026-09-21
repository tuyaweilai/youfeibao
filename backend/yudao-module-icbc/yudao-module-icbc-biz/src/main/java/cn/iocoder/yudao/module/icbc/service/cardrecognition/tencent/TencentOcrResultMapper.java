package cn.iocoder.yudao.module.icbc.service.cardrecognition.tencent;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.icbc.enums.IcbcAccountCodeEnum;
import cn.iocoder.yudao.module.icbc.service.cardrecognition.CardRecognitionPort;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 把腾讯云 OCR 的响应报文映射成 {@link CardRecognitionPort} 的结果（#93）。
 *
 * <p>厂商差异全部收敛在这里：业务层只看到库内格式与可读文案（ADR 0037）。映射是**纯函数**，
 * 测试喂**官方文档示例**形状的报文、不触网。
 *
 * <p><b>字段名以腾讯云官方 API 文档与官方 SDK 模型为准</b>（{@code IDCardOCR} /
 * {@code BankCardOCR}，{@code 2018-11-19}）。两个 Action 的返回形状不同，不能互相套用：
 * <ul>
 *   <li>身份证：告警在 {@code AdvancedInfo.WarnInfos}（**告警码数组**），质量分在
 *       {@code AdvancedInfo.Quality}。</li>
 *   <li>银行卡：**没有 {@code AdvancedInfo}**。告警在顶层 {@code WarningCode}（int 数组），
 *       质量分在顶层 {@code QualityValue}，卡类别在顶层 {@code CardCategory}。</li>
 * </ul>
 * 告警码 → 可读文案的对照表在本类里，硬拦只看**码**，不猜中文字符串。
 *
 * <p>签发机关（{@code Authority}）**不入库**：端口里没有这个字段（ADR 0037）。
 */
@Slf4j
public final class TencentOcrResultMapper {

    /** 长期有效期：与库内既有约定一致（{@code icbc_payee_info.id_validity_period}）。 */
    public static final String LONG_TERM_VALIDITY = "9999-12-30";

    /** 有效期：`起-止`，起止可以是 `yyyy.MM.dd` / `yyyy-MM-dd` / `yyyy/MM/dd`，止可以是「长期」。 */
    private static final Pattern VALIDITY_PATTERN = Pattern.compile(
            "^(\\d{4}[.\\-/]\\d{1,2}[.\\-/]\\d{1,2})\\s*[-~—至]\\s*"
                    + "(长期|长期有效|\\d{4}[.\\-/]\\d{1,2}[.\\-/]\\d{1,2})$");

    private static final String BLOCK_TEMP_ID = "临时身份证";
    private static final String BLOCK_INVALID_VALIDITY = "身份证有效期不合法";
    private static final String BLOCK_ELECTRONIC_CARD =
            "这是电子银行卡信息截图，不是实体卡照片，请拍实体卡正面";

    /**
     * 身份证告警码 → 可读文案。腾讯云 {@code IDCardOCR} 文档「WarnInfos」释义：
     * -9109 有效期不合法 / -9101 边框不完整 / -9102、-9108 复印件 / -9103 翻拍 /
     * -9105 框内遮挡 / -9104 临时身份证 / -9106 疑似 PS / -9107 反光 /
     * -9110 电子身份证（仅 CardWarnType=Advanced）/ -9111 水印。
     *
     * <p>{@code -9100} 是 #93 修票书写的「有效期不合法」码，而 2026-09-18 的官方文档与官方 SDK
     * 模型都是 {@code -9109}。两个都收，优先保证 AC6（有效期不合法必须硬拦）在两种文档版本下都成立。
     */
    private static final Map<Integer, String> ID_CARD_WARNING_TEXTS;

    /** 身份证里需要**挡住继续**的告警码：临时身份证、有效期不合法。 */
    private static final Set<Integer> ID_CARD_BLOCK_CODES;

    /**
     * 银行卡告警码 → 可读文案。腾讯云 {@code BankCardOCR} 文档「WarningCode」释义：
     * -9110 日期无效 / -9111 边框不完整 / -9112 反光 / -9113 复印件 / -9114 翻拍件。
     *
     * <p>它们都是**提示不拦**：银行卡侧的硬拦只有「电子银行卡信息截图」（{@code CardCategory}）。
     */
    private static final Map<Integer, String> BANK_CARD_WARNING_TEXTS;

    static {
        Map<Integer, String> idCard = new HashMap<>();
        idCard.put(-9109, BLOCK_INVALID_VALIDITY);
        idCard.put(-9100, BLOCK_INVALID_VALIDITY);
        idCard.put(-9101, "身份证边框不完整");
        idCard.put(-9102, "身份证复印件");
        idCard.put(-9108, "身份证复印件");
        idCard.put(-9103, "身份证翻拍");
        idCard.put(-9105, "身份证框内遮挡");
        idCard.put(-9104, BLOCK_TEMP_ID);
        idCard.put(-9106, "身份证疑似存在PS痕迹");
        idCard.put(-9107, "身份证反光");
        idCard.put(-9110, "电子身份证");
        idCard.put(-9111, "身份证有水印");
        ID_CARD_WARNING_TEXTS = Collections.unmodifiableMap(idCard);

        Set<Integer> blockCodes = new HashSet<>();
        blockCodes.add(-9104);
        blockCodes.add(-9109);
        blockCodes.add(-9100);
        ID_CARD_BLOCK_CODES = Collections.unmodifiableSet(blockCodes);

        Map<Integer, String> bankCard = new HashMap<>();
        bankCard.put(-9110, "银行卡日期无效");
        bankCard.put(-9111, "银行卡边框不完整");
        bankCard.put(-9112, "银行卡图片反光");
        bankCard.put(-9113, "银行卡复印件");
        bankCard.put(-9114, "银行卡翻拍");
        BANK_CARD_WARNING_TEXTS = Collections.unmodifiableMap(bankCard);
    }

    private static final String UNKNOWN_ID_CARD_WARNING = "身份证图片存在未识别的告警，请重拍或人工核对";
    private static final String UNKNOWN_BANK_CARD_WARNING = "银行卡图片存在未识别的告警，请重拍或人工核对";

    private TencentOcrResultMapper() {
    }

    // ==================== 身份证人像面 ====================

    public static CardRecognitionPort.IdCardFront toIdCardFront(JSONObject response) {
        if (response == null) {
            return CardRecognitionPort.IdCardFront.empty();
        }
        JSONObject advanced = advancedInfo(response);
        Warnings warnings = collectIdCardWarnings(advanced);
        return CardRecognitionPort.IdCardFront.builder()
                .name(text(response, "Name"))
                .idCardNo(text(response, "IdNum"))
                .address(text(response, "Address"))
                .qualityScore(integerOf(advanced.get("Quality")))
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
        Warnings warnings = collectIdCardWarnings(advanced);
        String rawValidDate = text(response, "ValidDate");
        String[] parsed = parseValidity(rawValidDate);
        if (parsed == null && StrUtil.isNotBlank(rawValidDate)) {
            // 兜底网：厂商回了值却读不成合法起止（如 2018.13.40-…）时绝不能静默落空——
            // 有效期是工行入驻的必输项。厂商的权威判据是 -9109（InvalidDateWarn），那条在上面已处理；
            // 这里只多拦一种「我们自己解析不出」的情况，宁可让收货员重拍，也不带着空日期往下走。
            warnings.addBlock(BLOCK_INVALID_VALIDITY);
        }
        return CardRecognitionPort.IdCardBack.builder()
                .idSignDate(parsed == null ? null : parsed[0])
                .idValidityPeriod(parsed == null ? null : parsed[1])
                .qualityScore(integerOf(advanced.get("Quality")))
                .warnings(warnings.warnings)
                .blockReasons(warnings.blockReasons)
                .build();
    }

    // ==================== 银行卡 ====================

    public static CardRecognitionPort.BankCard toBankCard(JSONObject response) {
        if (response == null) {
            return CardRecognitionPort.BankCard.empty();
        }
        // BankCardOCR 没有 AdvancedInfo：告警与质量分都在顶层（官方文档 / SDK 模型已确证）
        Warnings warnings = collectBankCardWarnings(response);
        String cardCategory = text(response, "CardCategory");
        if (cardCategory != null && cardCategory.contains("电子银行卡")) {
            warnings.addBlock(BLOCK_ELECTRONIC_CARD);
        }
        String bankName = normalizeBankName(text(response, "BankInfo"));
        return CardRecognitionPort.BankCard.builder()
                .bankCardNo(text(response, "CardNo"))
                .bankName(bankName)
                .accountCode(inferAccountCode(bankName))
                .qualityScore(integerOf(response.get("QualityValue")))
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
     * 身份证告警：{@code AdvancedInfo.WarnInfos} 是**告警码数组**，按码分硬拦 / 提示。
     *
     * <p>不再读 {@code TempIdWarn} / {@code CopyWarn} 等——那些是 {@code Config} 的**请求**开关，
     * 不是返回键（独立评审 SP-3）。
     */
    static Warnings collectIdCardWarnings(JSONObject advanced) {
        Warnings result = new Warnings();
        for (Integer code : warnCodes(advanced)) {
            String text = ID_CARD_WARNING_TEXTS.get(code);
            if (text == null) {
                // 不静默：未知码记 warn，回一句可读文案，但不据此硬拦（无法确证其严重性）
                log.warn("[collectIdCardWarnings][身份证出现未识别的告警码={}]", code);
                result.addWarning(UNKNOWN_ID_CARD_WARNING);
            } else if (ID_CARD_BLOCK_CODES.contains(code)) {
                result.addBlock(text);
            } else {
                result.addWarning(text);
            }
        }
        return result;
    }

    /**
     * 银行卡告警：顶层 {@code WarningCode} 是**告警码数组**，全部是提示不拦。
     */
    static Warnings collectBankCardWarnings(JSONObject response) {
        Warnings result = new Warnings();
        for (Integer code : codeList(response == null ? null : response.get("WarningCode"))) {
            String text = BANK_CARD_WARNING_TEXTS.get(code);
            if (text == null) {
                log.warn("[collectBankCardWarnings][银行卡出现未识别的告警码={}]", code);
                result.addWarning(UNKNOWN_BANK_CARD_WARNING);
            } else {
                result.addWarning(text);
            }
        }
        return result;
    }

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
                log.warn("[advancedInfo][AdvancedInfo 不是合法 JSON，按空处理]");
                return new JSONObject();
            }
        }
        return new JSONObject();
    }

    /** 读 {@code AdvancedInfo.WarnInfos} 的告警码。 */
    private static List<Integer> warnCodes(JSONObject advanced) {
        return codeList(advanced == null ? null : advanced.get("WarnInfos"));
    }

    /**
     * 读一个「告警码数组」字段。文档说元素是 Code（int），容错地也接受数字字符串与逗号分隔字符串。
     */
    static List<Integer> codeList(Object raw) {
        List<Integer> result = new ArrayList<>();
        if (raw instanceof JSONArray) {
            for (Object item : (JSONArray) raw) {
                Integer code = integerOf(item);
                if (code != null) {
                    result.add(code);
                } else {
                    log.warn("[codeList][告警码不是整数，已忽略：value={}]", item);
                }
            }
        } else if (raw instanceof String && StrUtil.isNotBlank((String) raw)) {
            for (String item : ((String) raw).split("[,，;；]")) {
                Integer code = integerOf(item.trim());
                if (code != null) {
                    result.add(code);
                } else {
                    log.warn("[codeList][告警码不是整数，已忽略：value={}]", item);
                }
            }
        } else if (raw != null) {
            Integer code = integerOf(raw);
            if (code != null) {
                result.add(code);
            } else {
                log.warn("[codeList][告警码不是整数，已忽略：value={}]", raw);
            }
        }
        return result;
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

    /**
     * 告警汇总：可读文案分成「提示不拦」与「硬拦」两栏，各自去重。
     */
    static final class Warnings {
        private final List<String> warnings = new ArrayList<>();
        private final List<String> blockReasons = new ArrayList<>();

        void addWarning(String text) {
            String value = StrUtil.trimToNull(text);
            if (value != null && !warnings.contains(value)) {
                warnings.add(value);
            }
        }

        void addBlock(String text) {
            String value = StrUtil.trimToNull(text);
            if (value != null && !blockReasons.contains(value)) {
                blockReasons.add(value);
            }
        }
    }

}
