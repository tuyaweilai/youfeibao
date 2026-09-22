package cn.iocoder.yudao.module.icbc.service.acquisition.recognition.tencent;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.icbc.enums.DeductionMethodEnum;
import cn.iocoder.yudao.module.icbc.service.acquisition.recognition.AcquisitionRecognitionPort;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 把通用印刷体识别的**文字行**解析成磅单字段（#113）。
 *
 * <p><b>为什么解析在平台侧</b>：腾讯云的通用印刷体只给「文字行 + 坐标」，不给字段名-值；把行变成
 * 「总重 32220」这一步只能是我们自己的代码。厂商侧另有 {@code SmartStructuralOCR}（调用时传
 * {@code ItemNames} 直接抽字段），2026-09-22 用真实磅单 {@code 参考/过磅模版.jpg} 实测过：
 * 车号与总重**丢失**、净重被读成单位「Kg」、19130 被挂到「备注」上——误配比人工慢更糟，
 * 故不采用（ADR 0013 的修订节留了这条证据）。解析做成**纯函数**，才好拿真实磅单当测试用例迭代。
 *
 * <p><b>口径来自真实磅单，不是猜的</b>（{@code 参考/过磅模版.jpg}）：
 * <ul>
 *   <li>标签是**中英并列**的（{@code 总重GROSS} / {@code 皮重TARE} / {@code 净重 NET} /
 *       {@code 车号VEHICLE NO} / {@code 序号SERIALNO}）。**这张单子上「毛重」印的是「总重 GROSS」**，
 *       只认「毛重」两个字就永远匹配不到——关键词表必须中英并用。</li>
 *   <li>值在标签的**右侧**（标签 X≈140，值 X≈390），所以取值要先按 X 分列，否则会把隔壁列的
 *       数字或单位（{@code Kg} / {@code %}）当成值。</li>
 *   <li>值所在的**行带可以比标签低**（净重的值比标签低 43px、序号的值低 51px，行高约 32px），
 *       所以行带容差取 2 倍行高，并允许向下就近取。</li>
 *   <li>标签之间的行距（约 64px）**大于**值之间的行距（约 57px），于是「离哪个标签最近」会算错
 *       （`0307` 离「日期」比离「序号」更近）。所以取值用**标签从上到下先到先得**的认领法：
 *       一个值行只归第一个能要它的标签，后面的标签只能用剩下的——扣率就是这样才不会把皮重的值抢走。</li>
 * </ul>
 *
 * <p><b>不做的两件事</b>：
 * <ul>
 *   <li><b>不做单位换算</b>：磅单上印着 {@code Kg}，回填的就是磅单上的数字本身（平台对重量没有
 *       单位概念，{@code 结算重量 × 单价 = 金额} 是裸乘）。照抄才与收货员手输的口径一致——
 *       同一张单「识别填的」与「手填的」不是同一个数是最难查的错。</li>
 *   <li><b>不回填日期与时间</b>：现场端没有「过磅时间」输入框，收购单的 {@code tradeTime} 是
 *       「交易发生时间」而不是过磅时间。它们只留在原始文字行里。</li>
 * </ul>
 */
@Slf4j
public final class TencentWeightTicketParser {

    /** 一行识别结果：文字 + 在整图中的位置（取值靠的就是这个位置） */
    @Getter
    @AllArgsConstructor
    public static class OcrLine {

        private final String text;
        private final int x;
        private final int y;
        private final int width;
        private final int height;

        int centerY() {
            return y + height / 2;
        }

    }

    /** 要抽的字段。枚举是为了让「一个值行只能归一个字段」这件事写得下 */
    @Getter
    @AllArgsConstructor
    private enum Field {

        /** 磅单号：显式单号优先，退到序号 */
        TICKET_NO("磅单号"),
        PLATE("车号"),
        GROSS("毛重 / 总重"),
        TARE("皮重"),
        NET("净重"),
        DISCOUNT("扣率");

        private final String name;

    }

    private static final Map<Field, List<String>> KEYWORDS = new EnumMap<>(Field.class);
    /** 单号的第一层关键词取不到时才用的第二层（见 {@link #TICKET_NO_NOISE} 的说明） */
    private static final List<String> TICKET_NO_FALLBACK_KEYWORDS = Arrays.asList("序号", "SERIAL");

    static {
        // 毛重：这张单子上印的是「总重 GROSS」，中文「毛重」与英文 GROSS 一并认
        KEYWORDS.put(Field.GROSS, Arrays.asList("总重", "毛重", "GROSS"));
        KEYWORDS.put(Field.TARE, Arrays.asList("皮重", "空车", "TARE"));
        KEYWORDS.put(Field.NET, Arrays.asList("净重", "实重", "NET"));
        KEYWORDS.put(Field.PLATE, Arrays.asList("车号", "车牌", "VEHICLE"));
        KEYWORDS.put(Field.DISCOUNT, Arrays.asList("扣率", "扣杂", "DISCOUNT"));
        // 显式单号：**不能把 `NO` 当关键词**——「车号 VEHICLE NO」「货号 GARGO NO」都带 NO，
        // 拿它当单号关键词会直接把车牌取成磅单号（真实样本上就会取到 `05648`）
        KEYWORDS.put(Field.TICKET_NO, Arrays.asList("磅单号", "单号"));
    }

    /** 数值：允许千分位逗号与小数点；{@code Kg} / {@code %} 这类单位由取值后的清洗剥掉 */
    private static final Pattern NUMBER = Pattern.compile("\\d[\\d,]*(?:\\.\\d+)?");
    private static final Pattern HAS_DIGIT = Pattern.compile("\\d");
    /** 车牌：省份简称 + 字母 + 数字（真实磅单上可能只读到后半截，那由告警兜住，不在这里拦） */
    private static final Pattern PLATE = Pattern.compile("[\\u4e00-\\u9fa5]?[A-Z]{0,2}\\d{3,6}[A-Z0-9]{0,3}");

    /** 车牌完整长度：不足就提示「可能只读到一部分」（真实样本上车号只读到 `05648`） */
    private static final int PLATE_MIN_LENGTH = 7;

    /**
     * 三个重量必须自洽：{@code 毛 - 皮 = 净}。磅机系统内部一致，误差只可能来自识别，
     * 所以容差只留一点给票据上的四舍五入。
     */
    private static final BigDecimal CONSISTENCY_TOLERANCE = new BigDecimal("0.01");

    public static final String WARN_WEIGHT_INCONSISTENT = "磅单三个重量不自洽（毛重-皮重≠净重），请核对";
    public static final String WARN_PLATE_INCOMPLETE = "车号可能只读到一部分，请核对";

    private TencentWeightTicketParser() {
    }

    /**
     * 把厂商响应里的 {@code TextDetections} 摊成行。字段名以官方 SDK 模型
     * （{@code GeneralBasicOCRResponse}）为准：{@code DetectedText} + {@code ItemPolygon.X/Y/Width/Height}。
     */
    public static List<OcrLine> toLines(JSONObject response) {
        JSONArray detections = response.getJSONArray("TextDetections");
        if (detections == null || detections.isEmpty()) {
            return Collections.emptyList();
        }
        List<OcrLine> lines = new ArrayList<>(detections.size());
        for (int i = 0; i < detections.size(); i++) {
            JSONObject detection = detections.getJSONObject(i);
            String text = StrUtil.trimToNull(detection.getString("DetectedText"));
            if (text == null) {
                continue;
            }
            JSONObject polygon = detection.getJSONObject("ItemPolygon");
            lines.add(new OcrLine(text,
                    polygon == null ? 0 : polygon.getIntValue("X"),
                    polygon == null ? 0 : polygon.getIntValue("Y"),
                    polygon == null ? 0 : polygon.getIntValue("Width"),
                    polygon == null ? 0 : polygon.getIntValue("Height")));
        }
        return lines;
    }

    /**
     * 解析磅单字段。解析不出来的字段一律留空——**绝不猜**：填一个错的数字比空着更危险。
     *
     * @param lines 识别出的文字行（带坐标）
     * @return 结构化结果 + 原始文字行（供现场端当场核对）+ 提示类告警
     */
    public static AcquisitionRecognitionPort.WeightTicketRecognition parse(List<OcrLine> lines) {
        List<String> rawLines = lines.stream().map(OcrLine::getText).collect(Collectors.toList());
        Map<Field, String> values = extract(lines);

        AcquisitionRecognitionPort.WeightTicketRecognition.WeightTicketRecognitionBuilder builder =
                AcquisitionRecognitionPort.WeightTicketRecognition.builder()
                        .weightTicketNo(values.get(Field.TICKET_NO))
                        .plateNo(values.get(Field.PLATE))
                        .grossWeight(toDecimal(values.get(Field.GROSS)))
                        .tareWeight(toDecimal(values.get(Field.TARE)))
                        .netWeight(toDecimal(values.get(Field.NET)))
                        .rawLines(rawLines);
        // 磅单上的「扣率 DISCOUNT %」是百分数（印着 %），而库里的比例口径是 0~1（ADR 0019 的扣杂）
        BigDecimal discount = toDecimal(values.get(Field.DISCOUNT));
        if (discount != null) {
            builder.deduction(discount.movePointLeft(2));
            builder.deductionMethod(DeductionMethodEnum.RATIO.getMethod());
        }
        AcquisitionRecognitionPort.WeightTicketRecognition parsed = builder.build();

        return AcquisitionRecognitionPort.WeightTicketRecognition.builder()
                .weightTicketNo(parsed.getWeightTicketNo())
                .plateNo(parsed.getPlateNo())
                .grossWeight(parsed.getGrossWeight())
                .tareWeight(parsed.getTareWeight())
                .netWeight(parsed.getNetWeight())
                .deduction(parsed.getDeduction())
                .deductionMethod(parsed.getDeductionMethod())
                .rawLines(rawLines)
                .warnings(warnings(parsed))
                .build();
    }

    /**
     * 取值：标签**从上到下先到先得**，一个值行只归第一个能要它的标签。
     *
     * <p>先到先得是为真实磅单的排版服务的：标签行距（约 64px）大于值行距（约 57px），
     * 「离谁最近」会把 {@code 0307} 判给「日期」而不是「序号」；按标签顺序认领则各归各位，
     * 扣率也不会把皮重的值抢走。
     */
    private static Map<Field, String> extract(List<OcrLine> lines) {
        Map<Field, String> values = new EnumMap<>(Field.class);
        Set<OcrLine> claimed = Collections.newSetFromMap(new IdentityHashMap<>());
        for (LabelHit hit : labelHits(lines)) {
            if (values.containsKey(hit.field)) {
                continue;
            }
            // 1) 标签行自己就带值：真实磅单上标签与值常印在同一格（`净重：19130`）
            String own = clean(hit.field, hit.line.getText());
            if (StrUtil.isNotBlank(own)) {
                values.put(hit.field, own);
                continue;
            }
            // 2) 同一行带里、标签右侧、还没被别的字段认领的那一行
            String value = pickValueToTheRight(lines, hit.line, hit.field, claimed);
            if (value != null) {
                values.put(hit.field, value);
            }
            // 取不到就留给同字段的下一处标签（中英并列被切成两行时会出现）
        }
        return values;
    }

    /** 命中关键词的行 + 它属于哪个字段，按从上到下、同一行从左到右排——取值顺序就靠这个序 */
    private static List<LabelHit> labelHits(List<OcrLine> lines) {
        List<LabelHit> hits = new ArrayList<>(4);
        for (OcrLine line : lines) {
            Field field = fieldOf(line.getText());
            if (field != null) {
                hits.add(new LabelHit(field, line));
            }
        }
        hits.sort((a, b) -> a.line.centerY() != b.line.centerY()
                ? Integer.compare(a.line.centerY(), b.line.centerY())
                : Integer.compare(a.line.getX(), b.line.getX()));
        return hits;
    }

    /** 这一行是哪个字段的标签；都不是则 null */
    private static Field fieldOf(String text) {
        String upper = text.toUpperCase();
        for (Map.Entry<Field, List<String>> entry : KEYWORDS.entrySet()) {
            if (containsAny(upper, entry.getValue())) {
                return entry.getKey();
            }
        }
        // 序号只在显式单号取不到时才用，所以它不是第一层关键词
        if (Field.TICKET_NO == fieldOfFallback(upper)) {
            return Field.TICKET_NO;
        }
        return null;
    }

    private static Field fieldOfFallback(String upper) {
        return containsAny(upper, TICKET_NO_FALLBACK_KEYWORDS) ? Field.TICKET_NO : null;
    }

    private static boolean containsAny(String upper, List<String> keywords) {
        for (String keyword : keywords) {
            if (upper.contains(keyword.toUpperCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 在标签的**右侧**、**同一行带**（容差 2 倍行高）里取最近的那个能成值的行。
     *
     * <p>要求 {@code line.x >= 标签右边界}：磅单是表格，同一行里右边可能还有「单位」或下一列的数字，
     * 按 X 分列才不会串列。
     *
     * <p><b>「能不能成值」要按字段的清洗规则判，不能只判近</b>：{@code 总重GROSS} 右边紧挨着的是
     * 单位行 {@code Kg}（比数值行还近），只按距离取会把 {@code Kg} 取回来、清洗成空，
     * 于是 32220 反而取不到。
     *
     * @return 清洗后的值；取不到返回 null（不认领任何行）
     */
    private static String pickValueToTheRight(List<OcrLine> lines, OcrLine label, Field field,
                                             Set<OcrLine> claimed) {
        int labelRight = label.getX() + label.getWidth();
        int maxDeltaY = Math.max(label.getHeight(), 1) * 2;
        OcrLine best = null;
        int bestDeltaY = Integer.MAX_VALUE;
        for (OcrLine line : lines) {
            if (line == label || line.getX() < labelRight || claimed.contains(line)) {
                continue;
            }
            // 别的标签行不能当值：中英并列被切成两行时（`总重` / `GROSS`），英文那半截就在中文右侧
            if (fieldOf(line.getText()) != null || StrUtil.isBlank(clean(field, line.getText()))) {
                continue;
            }
            int deltaY = Math.abs(line.centerY() - label.centerY());
            if (deltaY > maxDeltaY) {
                continue;
            }
            if (deltaY < bestDeltaY) {
                best = line;
                bestDeltaY = deltaY;
            }
        }
        if (best == null) {
            return null;
        }
        // 一个值行只归第一个能要它的标签：后面的标签（扣率）就不能把它抢走
        claimed.add(best);
        return clean(field, best.getText());
    }

    /** 按字段清洗取到的文字；清洗后为空表示「这一格不是值」 */
    private static String clean(Field field, String text) {
        switch (field) {
            case PLATE:
                return plateOnly(text);
            case TICKET_NO:
                return ticketNoOnly(text);
            default:
                return digitsOnly(text);
        }
    }

    private static BigDecimal toDecimal(String value) {
        return StrUtil.isBlank(value) ? null : new BigDecimal(value);
    }

    /** 提示类告警：只提示不拦（ADR 0013 的失败语义是「人工补录即可」） */
    private static List<String> warnings(AcquisitionRecognitionPort.WeightTicketRecognition result) {
        List<String> warnings = new ArrayList<>(2);
        if (result.getGrossWeight() != null && result.getTareWeight() != null && result.getNetWeight() != null
                && result.getGrossWeight().subtract(result.getTareWeight()).subtract(result.getNetWeight())
                .abs().compareTo(CONSISTENCY_TOLERANCE) > 0) {
            warnings.add(WARN_WEIGHT_INCONSISTENT);
        }
        if (result.getPlateNo() != null && result.getPlateNo().length() < PLATE_MIN_LENGTH) {
            warnings.add(WARN_PLATE_INCOMPLETE);
        }
        return warnings;
    }

    /**
     * 只留数字与小数点：`32220 Kg` → `32220`，`1,234.5` → `1234.5`；没有数字返回 null
     * （`Kg`、`%` 这类单位行因此永远成不了值）。
     */
    static String digitsOnly(String text) {
        if (text == null) {
            return null;
        }
        Matcher matcher = NUMBER.matcher(text.replace(",", ""));
        return matcher.find() ? matcher.group() : null;
    }

    /**
     * 只留单号：先把标签词本身剥掉，再剥掉冸号与空白。
     *
     * <p>剥完必须**含数字**才认（单号总是带数字）：这样「序号SERIALNO」剥完剩下 `NO`、没有数字，
     * 就会返回 null 而转去它右边取值——否则会把标签残字 `NO` 当成单号回填。
     */
    static String ticketNoOnly(String text) {
        if (text == null) {
            return null;
        }
        String cleaned = text;
        for (String noise : TICKET_NO_NOISE) {
            cleaned = Pattern.compile(Pattern.quote(noise), Pattern.CASE_INSENSITIVE).matcher(cleaned)
                    .replaceAll("");
        }
        cleaned = cleaned.replaceAll("[\\s:：.。\\-*#]", "");
        return cleaned.isEmpty() || !HAS_DIGIT.matcher(cleaned).find() ? null : cleaned;
    }

    /** 单号标签的噪声词：中文标签词 + 那些在磅单上只是「编号」意思、本身不带值的英文（`NO`） */
    private static final List<String> TICKET_NO_NOISE = Arrays.asList(
            "磅单号", "单号", "序号", "SERIAL", "NO");

    /**
     * 只留车牌：去掉空格与括号等噪声，再按车牌形状截取。
     *
     * <p>车牌可能只读到一部分（真实样本 `05648`），那由 {@link #WARN_PLATE_INCOMPLETE} 提示，
     * 不在这里丢成 null——半个车牌也比空着有用，且**比对仍由人工确认**。
     */
    static String plateOnly(String text) {
        if (text == null || !HAS_DIGIT.matcher(text).find()) {
            return null;
        }
        String compact = text.replaceAll("\\s", "").toUpperCase();
        Matcher matcher = PLATE.matcher(compact);
        return matcher.find() ? matcher.group() : null;
    }

    /** 一处标签命中 */
    @Getter
    @AllArgsConstructor
    private static class LabelHit {

        private final Field field;
        private final OcrLine line;

    }

}
