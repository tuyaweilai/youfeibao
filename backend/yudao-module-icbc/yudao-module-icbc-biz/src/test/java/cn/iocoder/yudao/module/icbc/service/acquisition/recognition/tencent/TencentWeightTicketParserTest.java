package cn.iocoder.yudao.module.icbc.service.acquisition.recognition.tencent;

import cn.iocoder.yudao.module.icbc.service.acquisition.recognition.AcquisitionRecognitionPort;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link TencentWeightTicketParser} 的单元测试（#113）。
 *
 * <p><b>主用例喂的是真实磅单的真实响应</b>：{@code src/test/resources/fixtures/weight-ticket-general-basic-ocr.json}
 * 是 2026-09-22 对 {@code 参考/过磅模版.jpg} 调 {@code GeneralBasicOCR} 的真实返回（只留了
 * 解析要用的 {@code DetectedText} 与 {@code ItemPolygon}）。手搓的坐标会让「单测全绿」与
 * 「真单子上什么都取不到」同时成立——这张单子就有两个只有真坐标才暴露的坑：值比标签低了
 * 43~51px（行高 32px），以及「毛重」印的是「总重 GROSS」。
 */
public class TencentWeightTicketParserTest {

    private static final String FIXTURE =
            "src/test/resources/fixtures/weight-ticket-general-basic-ocr.json";

    private static List<TencentWeightTicketParser.OcrLine> realLines() throws Exception {
        String json = new String(Files.readAllBytes(Paths.get(FIXTURE)), StandardCharsets.UTF_8);
        return TencentWeightTicketParser.toLines(JSON.parseObject(json));
    }

    // ==================== 真实磅单（参考/过磅模版.jpg） ====================

    @Test
    public void testParse_realWeightTicket() throws Exception {
        AcquisitionRecognitionPort.WeightTicketRecognition result =
                TencentWeightTicketParser.parse(realLines());

        // 32220 − 13090 = 19130，与单子上印的净重完全自洽
        assertEquals(0, new BigDecimal("32220").compareTo(result.getGrossWeight()));
        assertEquals(0, new BigDecimal("13090").compareTo(result.getTareWeight()));
        assertEquals(0, new BigDecimal("19130").compareTo(result.getNetWeight()));
        // 单号退到「序号 SERIAL NO」：真正的「No. 2105」被识别拆到左上角两处，按坐标取不到
        assertEquals("0307", result.getWeightTicketNo());
        // 车号只读到后半截（模版把前半截遮了）→ 回填 + 提示，不丢成空
        assertEquals("05648", result.getPlateNo());
        assertTrue(result.getWarnings().contains(TencentWeightTicketParser.WARN_PLATE_INCOMPLETE));
        // 三个重量自洽 + 车号不完整：只该有这一条告警
        assertEquals(1, result.getWarnings().size());
        // 这张单子上「扣率 DISCOUNT」是空的：绝不能把右边的 % 当成扣率
        assertNull(result.getDeduction());
        assertNull(result.getDeductionMethod());
        // 原始文字行整段回给现场端核对
        assertEquals(33, result.getRawLines().size());
        assertEquals("No", result.getRawLines().get(0));
    }

    // ==================== 中文标签的磅单 ====================

    @Test
    public void testParse_chineseLabels() {
        AcquisitionRecognitionPort.WeightTicketRecognition result = TencentWeightTicketParser.parse(Arrays.asList(
                line("磅单号：WD20261201001", 0, 0),
                line("毛重 30560", 0, 100),
                line("皮重 12000", 0, 200),
                line("净重 18560", 0, 300),
                line("车牌 豫A05648", 0, 400)));

        assertEquals("WD20261201001", result.getWeightTicketNo());
        assertEquals("30560", result.getGrossWeight().toPlainString());
        assertEquals("12000", result.getTareWeight().toPlainString());
        assertEquals("18560", result.getNetWeight().toPlainString());
        assertEquals("豫A05648", result.getPlateNo());
        assertTrue(result.getWarnings().isEmpty(), "重量自洽、车牌完整，不该有告警");
    }

    @Test
    public void testParse_labelAndValueInSameCell() {
        // 真实磅单上标签与值常在同一格里：光靠「取右边那一行」永远取不到
        AcquisitionRecognitionPort.WeightTicketRecognition result = TencentWeightTicketParser.parse(Arrays.asList(
                line("总重GROSS:32220Kg", 0, 0),
                line("皮重TARE:13090Kg", 0, 100)));

        assertEquals("32220", result.getGrossWeight().toPlainString());
        assertEquals("13090", result.getTareWeight().toPlainString());
        assertNull(result.getNetWeight(), "没有净重就留空，绝不拿毛重去皮重自己编一个值");
    }

    // ==================== 扣率 ====================

    @Test
    public void testParse_discount_isConvertedToRatio() {
        // 磅单印的是百分数（带 %），库里按 ADR 0019 存 0~1 的比例
        AcquisitionRecognitionPort.WeightTicketRecognition result = TencentWeightTicketParser.parse(Arrays.asList(
                line("净重 NET", 0, 0), line("18560", 300, 0),
                line("扣率 DISCOUNT", 0, 60), line("2.5", 300, 60), line("%", 450, 60)));

        assertEquals(0, new BigDecimal("0.025").compareTo(result.getDeduction()));
        assertEquals("RATIO", result.getDeductionMethod());
    }

    @Test
    public void testParse_discountWithoutValue_isNotZero() {
        // 单子上印了「扣率」但没填：回填 0 会被当成「明确不扣杂」，那是替人做了没做的决定
        AcquisitionRecognitionPort.WeightTicketRecognition result = TencentWeightTicketParser.parse(Arrays.asList(
                line("扣率 DISCOUNT", 0, 0), line("%", 400, 0)));

        assertNull(result.getDeduction());
        assertNull(result.getDeductionMethod());
    }

    // ==================== 重量不自洽 / 空结果 ====================

    @Test
    public void testParse_inconsistentWeights_areStillReturnedWithWarning() {
        // 照填 + 告警：数字错一位就是钱的错，宁可疑
        AcquisitionRecognitionPort.WeightTicketRecognition result = TencentWeightTicketParser.parse(Arrays.asList(
                line("毛重 30560", 0, 0), line("皮重 12000", 0, 100), line("净重 18000", 0, 200)));

        assertEquals("18000", result.getNetWeight().toPlainString(), "净重以磅单印的为准，不被毛−皮覆盖");
        assertEquals(Collections.singletonList(TencentWeightTicketParser.WARN_WEIGHT_INCONSISTENT),
                result.getWarnings());
    }

    @Test
    public void testParse_emptyLines_isAllEmpty() {
        AcquisitionRecognitionPort.WeightTicketRecognition result = TencentWeightTicketParser.parse(
                new ArrayList<>());

        assertNull(result.getWeightTicketNo());
        assertNull(result.getGrossWeight());
        assertNull(result.getTareWeight());
        assertNull(result.getNetWeight());
        assertNull(result.getPlateNo());
        assertTrue(result.getRawLines().isEmpty());
        assertTrue(result.getWarnings().isEmpty());
    }

    @Test
    public void testParse_unrelatedText_isAllEmpty() {
        // 拍糊了 / 拍成别的票据：一个字都不该回填
        AcquisitionRecognitionPort.WeightTicketRecognition result = TencentWeightTicketParser.parse(Arrays.asList(
                line("农家石嫣", 0, 0), line("过磅单", 100, 0), line("WEIGHT BILL", 0, 60)));

        assertNull(result.getGrossWeight());
        assertNull(result.getPlateNo());
        assertNull(result.getWeightTicketNo());
    }

    @Test
    public void testToLines_skipsBlankTextAndMissingPolygon() {
        JSONObject response = JSON.parseObject("{'TextDetections':["
                + "{'DetectedText':'毛重','ItemPolygon':{'X':1,'Y':2,'Width':3,'Height':4}},"
                + "{'DetectedText':'   '},"
                + "{'DetectedText':'30560'}]}");

        List<TencentWeightTicketParser.OcrLine> lines = TencentWeightTicketParser.toLines(response);

        assertEquals(2, lines.size(), "空行丢掉；缺坐标的行仍要留住（整张图的位置信息缺失不该让整次识别作废）");
        assertEquals("毛重", lines.get(0).getText());
        assertEquals(1, lines.get(0).getX());
        assertEquals("30560", lines.get(1).getText());
    }

    // ==================== 数值与车牌的清洗 ====================

    @Test
    public void testDigitsOnly() {
        assertEquals("32220", TencentWeightTicketParser.digitsOnly("32220 Kg"));
        assertEquals("1234.5", TencentWeightTicketParser.digitsOnly("1,234.5"));
        assertEquals("19130", TencentWeightTicketParser.digitsOnly("净重：19130 公斤"));
        assertNull(TencentWeightTicketParser.digitsOnly("Kg"));
        assertNull(TencentWeightTicketParser.digitsOnly("%"));
        assertNull(TencentWeightTicketParser.digitsOnly(null));
    }

    @Test
    public void testPlateOnly() {
        assertEquals("豫A05648", TencentWeightTicketParser.plateOnly("豫A05648"));
        assertEquals("豫A05648", TencentWeightTicketParser.plateOnly("豫A 05648"));
        assertEquals("05648", TencentWeightTicketParser.plateOnly("05648"), "只读到后半截也留着，由告警提示");
        assertNull(TencentWeightTicketParser.plateOnly("车号"));
        assertNull(TencentWeightTicketParser.plateOnly(null));
    }

    // ==================== 辅助 ====================

    private static TencentWeightTicketParser.OcrLine line(String text, int x, int y) {
        return new TencentWeightTicketParser.OcrLine(text, x, y, 100, 30);
    }

}
