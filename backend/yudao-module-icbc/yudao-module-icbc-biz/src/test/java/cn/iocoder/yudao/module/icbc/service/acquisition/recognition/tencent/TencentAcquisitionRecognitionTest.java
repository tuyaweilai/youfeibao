package cn.iocoder.yudao.module.icbc.service.acquisition.recognition.tencent;

import cn.iocoder.yudao.module.icbc.service.acquisition.recognition.AcquisitionRecognitionPort;
import cn.iocoder.yudao.module.icbc.service.cardrecognition.config.CardRecognitionConfigService;
import cn.iocoder.yudao.module.icbc.service.cardrecognition.config.CardRecognitionEffectiveConfig;
import cn.iocoder.yudao.module.icbc.service.cardrecognition.tencent.HutoolTencentOcrTransport;
import cn.iocoder.yudao.module.icbc.service.cardrecognition.tencent.TencentOcrClient;
import cn.iocoder.yudao.module.icbc.service.cardrecognition.tencent.TencentOcrSettings;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link TencentAcquisitionRecognition} 的单元测试（#112）：用假的配置 Service 与 OCR 客户端，不触网。
 *
 * <p>锁住的是**降级路径只有一条**（ADR 0013 / 0037 的安静降级）：
 * <ul>
 *   <li>{@code provider=stub}（未启用）：返回空结果、绝不触网；</li>
 *   <li>{@code provider=tencent} 但未配置密钥：返回空结果、绝不触网；</li>
 *   <li>配置读不出来（表未迁移 / DB 抖动）、客户端返回 {@code null}（网络 / 超时 / 额度耗尽 /
 *       车牌识别接口未开通）、客户端抛异常：一律空结果；</li>
 * </ul>
 * 绝不把异常抛给现场端。成功路径断言的是「请求送对了 Action 与 ImageBase64，响应交给映射层」。
 */
public class TencentAcquisitionRecognitionTest {

    @Test
    public void testRecognizePlate_whenProviderStub_returnsEmptyAndNeverCallsClient() {
        RecordingClient client = new RecordingClient();
        TencentAcquisitionRecognition port =
                new TencentAcquisitionRecognition(config(configured("stub", null, null)), client);

        assertNull(port.recognizePlate("QUJD").getPlateNo());
        assertEquals(0, client.calls, "未启用（stub）不许触网");
    }

    @Test
    public void testRecognizePlate_whenTencentButNoSecret_returnsEmptyAndNeverCallsClient() {
        RecordingClient client = new RecordingClient();
        TencentAcquisitionRecognition port =
                new TencentAcquisitionRecognition(config(configured("tencent", null, null)), client);

        assertNull(port.recognizePlate("QUJD").getPlateNo());
        assertEquals(0, client.calls, "未配置密钥不许触网");
    }

    @Test
    public void testRecognizePlate_whenConfigReadFails_isEmpty_noException() {
        CardRecognitionConfigService configService = mock(CardRecognitionConfigService.class);
        when(configService.resolveEffectiveConfig()).thenThrow(new RuntimeException("db down"));
        RecordingClient client = new RecordingClient();
        TencentAcquisitionRecognition port = new TencentAcquisitionRecognition(configService, client);

        assertNull(port.recognizePlate("QUJD").getPlateNo());
        assertEquals(0, client.calls, "配置读不出来也不许触网");
    }

    @Test
    public void testRecognizePlate_whenClientReturnsNull_isEmpty_noException() {
        // 网络 / 超时 / 额度耗尽 / 接口未开通：客户端一律返回 null，实现要走同一条降级路径
        TencentAcquisitionRecognition port =
                new TencentAcquisitionRecognition(configuredService(), new NullClient());

        assertNull(port.recognizePlate("QUJD").getPlateNo());
    }

    @Test
    public void testRecognizePlate_whenClientThrows_isEmpty_noException() {
        TencentAcquisitionRecognition port =
                new TencentAcquisitionRecognition(configuredService(), new ThrowingClient());

        assertNull(port.recognizePlate("QUJD").getPlateNo());
    }

    @Test
    public void testRecognizePlate_sendsLicensePlateActionAndMapsResponse() {
        RecordingClient client = new RecordingClient();
        client.result = JSON.parseObject("{\"Number\":\"京A12345\",\"Confidence\":95,"
                + "\"Color\":\"蓝\",\"LicensePlateCategory\":\"标准实体车牌\"}");
        TencentAcquisitionRecognition port = new TencentAcquisitionRecognition(configuredService(), client);

        AcquisitionRecognitionPort.PlateRecognition result = port.recognizePlate("data:image/jpeg;base64,QUJD");

        assertEquals("京A12345", result.getPlateNo());
        assertEquals(95, result.getConfidence());
        assertTrue(result.getWarnings().isEmpty());
        assertEquals("LicensePlateOCR", client.action, "车牌识别走腾讯云 OCR 的 LicensePlateOCR");
        assertEquals("QUJD", client.payload.getString("ImageBase64"), "dataURL 前缀要剥掉再发厂商");
        assertEquals(1, client.payload.size(), "只带 ImageBase64：车牌识别没有 CardSide / Config 参数");
    }

    @Test
    public void testRecognizePlate_whenVendorFindsNoPlate_isEmpty() {
        RecordingClient client = new RecordingClient();
        client.result = JSON.parseObject("{\"RequestId\":\"req-no-plate\"}");
        TencentAcquisitionRecognition port = new TencentAcquisitionRecognition(configuredService(), client);

        // 未识别 = 空结果：现场端据此去试车尾、再退回手工录入
        assertNull(port.recognizePlate("QUJD").getPlateNo());
    }

    @Test
    public void testRecognizeWeightTicket_sendsGeneralBasicOcrAndParsesLines() {
        RecordingClient client = new RecordingClient();
        client.result = JSON.parseObject("{'TextDetections':["
                + "{'DetectedText':'毛重','ItemPolygon':{'X':0,'Y':0,'Width':60,'Height':30}},"
                + "{'DetectedText':'32220 Kg','ItemPolygon':{'X':300,'Y':0,'Width':90,'Height':30}},"
                + "{'DetectedText':'皮重','ItemPolygon':{'X':0,'Y':60,'Width':60,'Height':30}},"
                + "{'DetectedText':'13090','ItemPolygon':{'X':300,'Y':60,'Width':90,'Height':30}},"
                + "{'DetectedText':'净重','ItemPolygon':{'X':0,'Y':120,'Width':60,'Height':30}},"
                + "{'DetectedText':'19130','ItemPolygon':{'X':300,'Y':120,'Width':90,'Height':30}}]}");
        TencentAcquisitionRecognition port = new TencentAcquisitionRecognition(configuredService(), client);

        AcquisitionRecognitionPort.WeightTicketRecognition result =
                port.recognizeWeightTicket("data:image/jpeg;base64,QUJD");

        assertEquals("32220", result.getGrossWeight().toPlainString());
        assertEquals("13090", result.getTareWeight().toPlainString());
        assertEquals("19130", result.getNetWeight().toPlainString());
        assertEquals(6, result.getRawLines().size(), "原始文字行一并回，供现场端核对");
        assertEquals("GeneralBasicOCR", client.action, "磅单走通用印刷体：厂商的结构化抽取实测误配（#113）");
        assertEquals("QUJD", client.payload.getString("ImageBase64"), "dataURL 前缀要剥掉再发厂商");
        assertEquals(1, client.payload.size(), "只带 ImageBase64");
    }

    @Test
    public void testRecognizeWeightTicket_whenProviderStub_returnsEmptyAndNeverCallsClient() {
        RecordingClient client = new RecordingClient();
        TencentAcquisitionRecognition port =
                new TencentAcquisitionRecognition(config(configured("stub", null, null)), client);

        AcquisitionRecognitionPort.WeightTicketRecognition result = port.recognizeWeightTicket("QUJD");

        assertNull(result.getGrossWeight());
        assertTrue(result.getRawLines().isEmpty());
        assertEquals(0, client.calls, "未启用（stub）不许触网");
    }

    @Test
    public void testRecognizeWeightTicket_whenClientReturnsNull_isEmpty_noException() {
        // 网络 / 超时 / 额度耗尽 / 接口未开通：客户端一律返回 null，两条识别走同一条降级路径
        TencentAcquisitionRecognition port =
                new TencentAcquisitionRecognition(configuredService(), new NullClient());

        AcquisitionRecognitionPort.WeightTicketRecognition result = port.recognizeWeightTicket("QUJD");

        assertNull(result.getWeightTicketNo());
        assertTrue(result.getWarnings().isEmpty());
    }

    @Test
    public void testRecognizeWeightTicket_whenClientThrows_isEmpty_noException() {
        TencentAcquisitionRecognition port =
                new TencentAcquisitionRecognition(configuredService(), new ThrowingClient());

        assertNull(port.recognizeWeightTicket("QUJD").getGrossWeight());
    }

    // ==================== 假的配置 Service 与客户端 ====================

    private static CardRecognitionEffectiveConfig configured(String provider, String secretId, String secretKey) {
        return CardRecognitionEffectiveConfig.builder()
                .provider(provider).secretId(secretId).secretKey(secretKey)
                .region("ap-guangzhou").endpoint("ocr.tencentcloudapi.com").timeout(10000)
                .build();
    }

    private static CardRecognitionConfigService configuredService() {
        return config(configured("tencent", "secret-id", "secret-key"));
    }

    private static CardRecognitionConfigService config(CardRecognitionEffectiveConfig effective) {
        CardRecognitionConfigService configService = mock(CardRecognitionConfigService.class);
        when(configService.resolveEffectiveConfig()).thenReturn(effective);
        return configService;
    }

    private static class RecordingClient extends TencentOcrClient {
        JSONObject result = new JSONObject();
        String action;
        JSONObject payload;
        int calls;

        RecordingClient() {
            super(new HutoolTencentOcrTransport());
        }

        @Override
        public JSONObject call(TencentOcrSettings settings, String action, JSONObject payload) {
            this.calls++;
            this.action = action;
            this.payload = payload;
            return result;
        }
    }

    private static class NullClient extends TencentOcrClient {
        NullClient() {
            super(new HutoolTencentOcrTransport());
        }

        @Override
        public JSONObject call(TencentOcrSettings settings, String action, JSONObject payload) {
            return null;
        }
    }

    private static class ThrowingClient extends TencentOcrClient {
        ThrowingClient() {
            super(new HutoolTencentOcrTransport());
        }

        @Override
        public JSONObject call(TencentOcrSettings settings, String action, JSONObject payload) {
            throw new RuntimeException("boom");
        }
    }

}
