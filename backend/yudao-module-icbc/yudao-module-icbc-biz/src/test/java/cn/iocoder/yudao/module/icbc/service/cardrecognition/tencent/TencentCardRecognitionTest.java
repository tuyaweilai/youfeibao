package cn.iocoder.yudao.module.icbc.service.cardrecognition.tencent;

import cn.iocoder.yudao.module.icbc.service.cardrecognition.CardRecognitionPort;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link TencentCardRecognition} 的单元测试（#93）：用假的 {@link TencentOcrClient} 不触网。
 *
 * <p>锁住的是**降级路径只有一条**：未配置密钥、客户端返回 {@code null}（网络 / 超时 / 额度耗尽 /
 * 厂商报错）、客户端抛异常——三种都返回空结果，绝不把异常抛给向导（ADR 0037）。
 * 成功路径断言的是「请求送对了 Action / CardSide，响应交给映射层」。
 */
public class TencentCardRecognitionTest {

    @Test
    public void testRecognize_whenNotConfigured_returnsEmptyAndNeverCallsClient() {
        TencentCardRecognitionProperties properties = new TencentCardRecognitionProperties(); // 无密钥
        RecordingClient client = new RecordingClient(properties);
        TencentCardRecognition port = new TencentCardRecognition(properties, client);

        CardRecognitionPort.IdCardFront front = port.recognizeIdCardFront("base64");
        CardRecognitionPort.IdCardBack back = port.recognizeIdCardBack("base64");
        CardRecognitionPort.BankCard bank = port.recognizeBankCard("base64");

        assertNotNull(front);
        assertNull(front.getName());
        assertNull(back.getIdValidityPeriod());
        assertNull(bank.getBankCardNo());
        assertEquals(0, client.calls, "未配置密钥不许触网");
    }

    @Test
    public void testRecognize_whenClientReturnsNull_isEmpty_noException() {
        // 网络 / 超时 / 额度耗尽 / 厂商报错：客户端一律返回 null，实现要走同一条降级路径
        TencentCardRecognitionProperties properties = configuredProperties();
        TencentCardRecognition port = new TencentCardRecognition(properties, new NullClient(properties));

        assertNull(port.recognizeIdCardFront("base64").getName());
        assertNull(port.recognizeIdCardBack("base64").getIdValidityPeriod());
        assertNull(port.recognizeBankCard("base64").getBankCardNo());
    }

    @Test
    public void testRecognize_whenClientThrows_isEmpty_noException() {
        TencentCardRecognitionProperties properties = configuredProperties();
        TencentCardRecognition port = new TencentCardRecognition(properties, new ThrowingClient(properties));

        assertNull(port.recognizeIdCardFront("base64").getName());
        assertNull(port.recognizeBankCard("base64").getBankCardNo());
    }

    @Test
    public void testRecognizeIdCardFront_sendsFrontSideAndMapsResponse() {
        TencentCardRecognitionProperties properties = configuredProperties();
        RecordingClient client = new RecordingClient(properties);
        client.result = JSON.parseObject("{\"Name\":\"张三\",\"IdNum\":\"110101199001011234\","
                + "\"Address\":\"北京市朝阳区\",\"AdvancedInfo\":\"{\\\"Quality\\\":\\\"80\\\"}\"}");
        TencentCardRecognition port = new TencentCardRecognition(properties, client);

        CardRecognitionPort.IdCardFront result = port.recognizeIdCardFront("data:image/jpeg;base64,QUJD");

        assertEquals("张三", result.getName());
        assertEquals("110101199001011234", result.getIdCardNo());
        assertEquals(80, result.getQualityScore());
        assertEquals("IDCardOCR", client.action);
        assertEquals("FRONT", client.payload.getString("CardSide"));
        assertEquals("QUJD", client.payload.getString("ImageBase64"), "dataURL 前缀要剥掉再发厂商");
        String config = client.payload.getString("Config");
        assertTrue(config.contains("TempIdWarn"), "身份证要开告警与质量分");
        assertTrue(config.contains("ReflectWarn"), "反光检测开关没开，AC5 的反光在真机上永不出现（SP-4）");
        assertTrue(config.contains("InvalidDateWarn"), "有效期不合法告警开关没开，AC6 少一条腿（SP-4）");
    }

    @Test
    public void testRecognizeIdCardBack_sendsBackSide() {
        TencentCardRecognitionProperties properties = configuredProperties();
        RecordingClient client = new RecordingClient(properties);
        client.result = JSON.parseObject("{\"ValidDate\":\"2018.08.12-长期\"}");
        TencentCardRecognition port = new TencentCardRecognition(properties, client);

        CardRecognitionPort.IdCardBack result = port.recognizeIdCardBack("QUJD");

        assertEquals("2018-08-12", result.getIdSignDate());
        assertEquals("9999-12-30", result.getIdValidityPeriod());
        assertEquals("BACK", client.payload.getString("CardSide"));
    }

    @Test
    public void testRecognizeBankCard_usesBankCardAction() {
        TencentCardRecognitionProperties properties = configuredProperties();
        RecordingClient client = new RecordingClient(properties);
        client.result = JSON.parseObject("{\"CardNo\":\"6222021234567890123\","
                + "\"BankInfo\":\"中国工商银行(03080000)\","
                + "\"CardCategory\":\"标准实体银行卡\","
                + "\"WarningCode\":[-9113],\"QualityValue\":88}");
        TencentCardRecognition port = new TencentCardRecognition(properties, client);

        CardRecognitionPort.BankCard result = port.recognizeBankCard("QUJD");

        assertEquals("6222021234567890123", result.getBankCardNo());
        assertEquals("中国工商银行", result.getBankName());
        assertEquals("1", result.getAccountCode());
        assertEquals(88, result.getQualityScore());
        assertTrue(result.getWarnings().contains("银行卡复印件"));
        assertEquals("BankCardOCR", client.action);
        assertNull(client.payload.getString("CardSide"), "银行卡识别不带 CardSide");
        assertNull(client.payload.getString("Config"), "BankCardOCR 没有 Config 参数");
        // 四个开关是官方文档字段，默认全 false：不带就什么都不回（SP-2）
        assertEquals(Boolean.TRUE, client.payload.getBoolean("EnableCopyCheck"));
        assertEquals(Boolean.TRUE, client.payload.getBoolean("EnableReshootCheck"));
        assertEquals(Boolean.TRUE, client.payload.getBoolean("EnableBorderCheck"));
        assertEquals(Boolean.TRUE, client.payload.getBoolean("EnableQualityValue"));
    }

    @Test
    public void testPlainBase64_stripsPrefixAndKeepsPlainValue() {
        assertEquals("QUJD", TencentCardRecognition.plainBase64("data:image/jpeg;base64,QUJD"));
        assertEquals("QUJD", TencentCardRecognition.plainBase64("QUJD"));
        assertNull(TencentCardRecognition.plainBase64(null));
    }

    @Test
    public void testRecognize_blankImageIsStillHandledWithoutNetwork() {
        // 控制器已用 @NotBlank 拦空图；即便漏到这里，也只能是空结果 + 不触网，不能抛异常
        TencentCardRecognitionProperties properties = configuredProperties();
        RecordingClient client = new RecordingClient(properties);
        TencentCardRecognition port = new TencentCardRecognition(properties, client);

        CardRecognitionPort.IdCardFront result = port.recognizeIdCardFront("  ");

        assertNotNull(result);
        assertNull(result.getName());
    }

    // ==================== 假客户端 ====================

    private TencentCardRecognitionProperties configuredProperties() {
        TencentCardRecognitionProperties properties = new TencentCardRecognitionProperties();
        properties.setSecretId("secret-id");
        properties.setSecretKey("secret-key");
        return properties;
    }

    private static class RecordingClient extends TencentOcrClient {
        JSONObject result = new JSONObject();
        String action;
        JSONObject payload;
        int calls;

        RecordingClient(TencentCardRecognitionProperties properties) {
            super(properties, new HutoolTencentOcrTransport());
        }

        @Override
        public JSONObject call(String action, JSONObject payload) {
            this.calls++;
            this.action = action;
            this.payload = payload;
            return result;
        }
    }

    private static class NullClient extends TencentOcrClient {
        NullClient(TencentCardRecognitionProperties properties) {
            super(properties, new HutoolTencentOcrTransport());
        }

        @Override
        public JSONObject call(String action, JSONObject payload) {
            return null;
        }
    }

    private static class ThrowingClient extends TencentOcrClient {
        ThrowingClient(TencentCardRecognitionProperties properties) {
            super(properties, new HutoolTencentOcrTransport());
        }

        @Override
        public JSONObject call(String action, JSONObject payload) {
            throw new IllegalStateException("network down");
        }
    }

}
