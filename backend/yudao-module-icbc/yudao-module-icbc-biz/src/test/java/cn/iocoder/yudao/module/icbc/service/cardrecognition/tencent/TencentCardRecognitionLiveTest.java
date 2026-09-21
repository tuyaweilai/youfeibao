package cn.iocoder.yudao.module.icbc.service.cardrecognition.tencent;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 腾讯云 OCR 的真实连通性校验（联调用，默认跳过）。
 *
 * <p>设置 {@code TENCENT_OCR_SECRET_ID} / {@code TENCENT_OCR_SECRET_KEY} 后才会执行：拿一张
 * 1x1 的占位图打一次 {@code IDCardOCR}，断言**签名被接受**（没有 {@code AuthFailure.*}），
 * 而不是断言识别出内容——内容识别要真证件，那一步只能在真机 / 联调环境里人工验。
 *
 * <pre>
 * TENCENT_OCR_SECRET_ID=... TENCENT_OCR_SECRET_KEY=... \
 *   mvn -pl yudao-module-icbc/yudao-module-icbc-biz test -Dtest=TencentCardRecognitionLiveTest
 * </pre>
 */
@EnabledIfEnvironmentVariable(named = "TENCENT_OCR_SECRET_ID", matches = ".+")
public class TencentCardRecognitionLiveTest {

    /** 1x1 PNG：足以让厂商走完鉴权、在解码 / 识别阶段报业务错。 */
    private static final String TINY_PNG_BASE64 =
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==";

    @Test
    public void testIdCardOcr_liveSignatureAccepted() {
        TencentCardRecognitionProperties properties = new TencentCardRecognitionProperties();
        properties.setSecretId(System.getenv("TENCENT_OCR_SECRET_ID"));
        properties.setSecretKey(System.getenv("TENCENT_OCR_SECRET_KEY"));
        String region = System.getenv("TENCENT_OCR_REGION");
        if (region != null && !region.trim().isEmpty()) {
            properties.setRegion(region);
        }
        TencentOcrClient client = new TencentOcrClient(properties, new HutoolTencentOcrTransport());

        JSONObject payload = JSON.parseObject("{\"ImageBase64\":\"" + TINY_PNG_BASE64
                + "\",\"CardSide\":\"FRONT\"}");
        JSONObject response = client.callRaw("IDCardOCR", payload);

        assertNotNull(response, "腾讯云 OCR 不可达：网络或域名配置有问题");
        JSONObject error = response.getJSONObject("Error");
        if (error != null) {
            String code = error.getString("Code");
            assertTrue(code == null || !code.startsWith("AuthFailure"),
                    "签名 / 密钥被拒绝：" + code + " / " + error.getString("Message"));
        }
    }

}
