package cn.iocoder.yudao.module.icbc.service.acquisition.recognition.tencent;

import cn.iocoder.yudao.module.icbc.service.acquisition.recognition.AcquisitionRecognitionPort;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link TencentPlateResultMapper} 的单元测试（#112）。
 *
 * <p>报文形状取自**腾讯云官方 SDK 模型**（{@code LicensePlateOCRResponse} / {@code LicensePlateInfo}，
 * {@code 2018-11-19}）：{@code Number} / {@code Confidence} / {@code Rect} / {@code Color} /
 * {@code LicensePlateCategory} / {@code LicensePlateInfos}。取值（车牌颜色与车牌类别的枚举文案）
 * 也照官方文档的释义写。文档正文是前端渲染的，抓不到官方的**输出示例**原文，所以这里以 SDK 模型
 * 为准——手搓字段名的风险仍然被这条测试挡在门外（字段名对不上就是 null，断言会红）。
 */
public class TencentPlateResultMapperTest {

    /** 单张标准车牌：官方 SDK 模型的字段全给上 */
    private static final String SINGLE_PLATE = "{\"Number\":\"京A12345\",\"Confidence\":95,"
            + "\"Color\":\"蓝\",\"LicensePlateCategory\":\"标准实体车牌\","
            + "\"Rect\":{\"X\":100,\"Y\":200,\"Width\":440,\"Height\":140},"
            + "\"LicensePlateInfos\":[{\"Number\":\"京A12345\",\"Confidence\":95,\"Color\":\"蓝\","
            + "\"LicensePlateCategory\":\"标准实体车牌\"}],"
            + "\"RequestId\":\"req-plate-1\"}";

    @Test
    public void testToPlate_singlePlate() {
        AcquisitionRecognitionPort.PlateRecognition result =
                TencentPlateResultMapper.toPlate(JSON.parseObject(SINGLE_PLATE));

        assertEquals("京A12345", result.getPlateNo());
        assertEquals(95, result.getConfidence());
        assertTrue(result.getWarnings().isEmpty(), "置信度够高就不提示");
    }

    @Test
    public void testToPlate_lowConfidence_isWarnedButStillReturned() {
        // 读数偏低仍比空着强：回填 + 提示，不退回「未识别」、不拦提交
        JSONObject response = JSON.parseObject(SINGLE_PLATE);
        response.put("Confidence", TencentPlateResultMapper.LOW_CONFIDENCE_THRESHOLD - 1);

        AcquisitionRecognitionPort.PlateRecognition result = TencentPlateResultMapper.toPlate(response);

        assertEquals("京A12345", result.getPlateNo());
        assertEquals(TencentPlateResultMapper.LOW_CONFIDENCE_THRESHOLD - 1, result.getConfidence());
        assertEquals(1, result.getWarnings().size());
        assertEquals(TencentPlateResultMapper.LOW_CONFIDENCE_WARNING, result.getWarnings().get(0));
    }

    @Test
    public void testToPlate_confidenceExactlyAtThreshold_isNotWarned() {
        JSONObject response = JSON.parseObject(SINGLE_PLATE);
        response.put("Confidence", TencentPlateResultMapper.LOW_CONFIDENCE_THRESHOLD);

        assertTrue(TencentPlateResultMapper.toPlate(response).getWarnings().isEmpty());
    }

    @Test
    public void testToPlate_missingConfidence_isNotWarned() {
        // 厂商没给置信度 ≠ 置信度低：不能把一次读得好好的车牌标成可疑
        JSONObject response = JSON.parseObject(SINGLE_PLATE);
        response.remove("Confidence");

        AcquisitionRecognitionPort.PlateRecognition result = TencentPlateResultMapper.toPlate(response);

        assertEquals("京A12345", result.getPlateNo());
        assertNull(result.getConfidence());
        assertTrue(result.getWarnings().isEmpty());
    }

    @Test
    public void testToPlate_nonNumericConfidence_isTreatedAsMissing() {
        JSONObject response = JSON.parseObject(SINGLE_PLATE);
        response.put("Confidence", "很高");

        AcquisitionRecognitionPort.PlateRecognition result = TencentPlateResultMapper.toPlate(response);

        assertEquals("京A12345", result.getPlateNo());
        assertTrue(result.getWarnings().isEmpty());
    }

    @Test
    public void testToPlate_noNumber_isUnrecognized() {
        assertNull(TencentPlateResultMapper.toPlate(JSON.parseObject("{\"RequestId\":\"req-x\"}")).getPlateNo());
        assertNull(TencentPlateResultMapper.toPlate(JSON.parseObject("{\"Number\":\"\"}")).getPlateNo());
        assertNull(TencentPlateResultMapper.toPlate(JSON.parseObject("{\"Number\":\"   \"}")).getPlateNo());
    }

    @Test
    public void testToPlate_trimsPlateNo() {
        assertEquals("京A12345", TencentPlateResultMapper.toPlate(
                JSON.parseObject("{\"Number\":\" 京A12345 \"}")).getPlateNo());
    }

    @Test
    public void testToPlate_multiplePlates_takesVendorTopLevelResult() {
        // 车头照里出现第二张牌通常是背景里别的车：取厂商给的主结果，不做现场选择
        JSONObject response = JSON.parseObject(SINGLE_PLATE);
        response.put("LicensePlateInfos", JSON.parseArray(
                "[{\"Number\":\"京B00001\"},{\"Number\":\"京A12345\"}]"));

        assertEquals("京A12345", TencentPlateResultMapper.toPlate(response).getPlateNo());
    }

    @Test
    public void testToPlate_temporaryAndPaintedPlates_areAccepted() {
        // 临牌、车身放大号（喷漆）同样是车牌的合法来源，照常回填、不拒收
        for (String category : new String[]{"临牌", "喷漆车牌", "非标准实体车牌"}) {
            JSONObject response = JSON.parseObject(SINGLE_PLATE);
            response.put("LicensePlateCategory", category);
            assertEquals("京A12345", TencentPlateResultMapper.toPlate(response).getPlateNo(), category);
        }
    }

    @Test
    public void testToPlate_withoutConfidenceKey_fieldIsNamespacedAsExpected() {
        // 防呆：官方模型的字段名是 Confidence（不是 Score / Quality），改错了这条会红
        JSONObject response = JSON.parseObject("{\"Number\":\"京A12345\",\"Score\":10}");
        assertNull(TencentPlateResultMapper.toPlate(response).getConfidence());
    }

}
