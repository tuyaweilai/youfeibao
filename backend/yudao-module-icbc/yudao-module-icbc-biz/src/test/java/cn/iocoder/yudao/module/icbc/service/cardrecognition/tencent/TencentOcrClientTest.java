package cn.iocoder.yudao.module.icbc.service.cardrecognition.tencent;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link TencentOcrClient} 的单元测试（#93，独立评审 ST-2）。
 *
 * <p>HTTP 走注入的 {@link TencentOcrTransport} 假实现，因此「厂商报错收成 null」这条最关键的
 * 降级判据、以及非 2xx / 非 JSON / 缺 {@code Response} 三个分支都被真正钉住——
 * 不再只是「号称走同一条路」。
 */
public class TencentOcrClientTest {

    /** 腾讯云官方错误形状：错误挂在 {@code Response.Error} 上，HTTP 仍是 200。 */
    private static final String VENDOR_ERROR_BODY = "{\"Response\":{"
            + "\"Error\":{\"Code\":\"FailedOperation.NoEnoughQuota\",\"Message\":\"配额已用完\"},"
            + "\"RequestId\":\"f0a1b2c3-d4e5-6789-abcd-ef0123456789\"}}";

    @Test
    public void testCall_whenVendorError_returnsNull() {
        RecordingTransport transport = new RecordingTransport();
        transport.result = new TencentOcrTransport.Result(200, VENDOR_ERROR_BODY);
        TencentOcrClient client = new TencentOcrClient(transport);

        assertNull(client.call(settings(), "IDCardOCR", payload()), "厂商报错（含额度耗尽）必须走降级路径收成 null");
    }

    @Test
    public void testCallRaw_whenVendorError_keepsErrorNodeForConnectivityProbe() {
        RecordingTransport transport = new RecordingTransport();
        transport.result = new TencentOcrTransport.Result(200, VENDOR_ERROR_BODY);
        TencentOcrClient client = new TencentOcrClient(transport);

        JSONObject response = client.callRaw(settings(), "IDCardOCR", payload());

        assertNotNull(response, "callRaw 保留厂商错误，供联调判断签名是否被接受");
        assertEquals("FailedOperation.NoEnoughQuota", response.getJSONObject("Error").getString("Code"));
    }

    @Test
    public void testCallRaw_whenStatusNot2xx_returnsNull() {
        RecordingTransport transport = new RecordingTransport();
        transport.result = new TencentOcrTransport.Result(500, "Internal Server Error");
        TencentOcrClient client = new TencentOcrClient(transport);

        assertNull(client.callRaw(settings(), "BankCardOCR", payload()));
    }

    @Test
    public void testCallRaw_whenBodyIsNotJson_returnsNull() {
        RecordingTransport transport = new RecordingTransport();
        transport.result = new TencentOcrTransport.Result(200, "<html>bad gateway</html>");
        TencentOcrClient client = new TencentOcrClient(transport);

        assertNull(client.callRaw(settings(), "BankCardOCR", payload()));
    }

    @Test
    public void testCallRaw_whenResponseNodeMissing_returnsNull() {
        RecordingTransport transport = new RecordingTransport();
        transport.result = new TencentOcrTransport.Result(200, "{\"Foo\":\"bar\"}");
        TencentOcrClient client = new TencentOcrClient(transport);

        assertNull(client.callRaw(settings(), "BankCardOCR", payload()));
    }

    @Test
    public void testCallRaw_whenBodyBlank_returnsNull() {
        RecordingTransport transport = new RecordingTransport();
        transport.result = new TencentOcrTransport.Result(200, "   ");
        TencentOcrClient client = new TencentOcrClient(transport);

        assertNull(client.callRaw(settings(), "BankCardOCR", payload()));
    }

    @Test
    public void testCallRaw_whenTransportThrows_returnsNull() {
        RecordingTransport transport = new RecordingTransport();
        transport.failure = new RuntimeException("connect timed out");
        TencentOcrClient client = new TencentOcrClient(transport);

        assertNull(client.callRaw(settings(), "IDCardOCR", payload()), "弱网 / 超时要降级，不能把异常抛给向导");
    }

    @Test
    public void testCall_whenSuccess_returnsResponseNode() {
        RecordingTransport transport = new RecordingTransport();
        transport.result = new TencentOcrTransport.Result(200,
                "{\"Response\":{\"Name\":\"刘洋\",\"RequestId\":\"abc\"}}");
        TencentOcrClient client = new TencentOcrClient(transport);

        JSONObject response = client.call(settings(), "IDCardOCR", payload());

        assertNotNull(response);
        assertEquals("刘洋", response.getString("Name"));
    }

    @Test
    public void testCallRaw_buildsSignedRequestWithActionHeadersAndBody() {
        RecordingTransport transport = new RecordingTransport();
        transport.result = new TencentOcrTransport.Result(200, "{\"Response\":{\"Name\":\"刘洋\"}}");
        TencentOcrClient client = new TencentOcrClient(transport);

        client.callRaw(settings(), "IDCardOCR", payload());

        assertNotNull(transport.request);
        assertEquals("https://ocr.tencentcloudapi.com", transport.request.url());
        assertEquals("IDCardOCR", transport.request.headers().get("X-TC-Action"));
        assertEquals("2018-11-19", transport.request.headers().get("X-TC-Version"));
        // 审评观察 5：region / timeout 曾被 handoff 说成「已覆盖」，其实当时没有任何断言
        assertEquals("ap-guangzhou", transport.request.headers().get("X-TC-Region"));
        assertEquals(10000, transport.request.timeoutMillis());
        assertTrue(transport.request.headers().get("Authorization").startsWith("TC3-HMAC-SHA256 "));
        assertTrue(transport.request.body().contains("\"ImageBase64\":\"QUJD\""));
    }

    private static TencentOcrSettings settings() {
        return TencentOcrSettings.builder()
                .secretId("secret-id")
                .secretKey("secret-key")
                .region("ap-guangzhou")
                .endpoint("ocr.tencentcloudapi.com")
                .timeout(10000)
                .build();
    }

    private static JSONObject payload() {
        JSONObject payload = new JSONObject();
        payload.put("ImageBase64", "QUJD");
        return payload;
    }

    private static class RecordingTransport implements TencentOcrTransport {
        TencentOcrTransport.Request request;
        TencentOcrTransport.Result result = new Result(200, "{}");
        RuntimeException failure;

        @Override
        public Result post(Request request) {
            this.request = request;
            if (failure != null) {
                throw failure;
            }
            return result;
        }
    }

}
