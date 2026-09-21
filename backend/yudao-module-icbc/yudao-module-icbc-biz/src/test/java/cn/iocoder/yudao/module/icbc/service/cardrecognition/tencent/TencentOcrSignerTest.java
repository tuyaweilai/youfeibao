package cn.iocoder.yudao.module.icbc.service.cardrecognition.tencent;

import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link TencentOcrSigner} 的单元测试：不触网，用**独立重算**（本测试自己的 MessageDigest / Mac）
 * 校验签名链的每一步，再用结构断言钉住格式。
 *
 * <p>签名的跨实现可比性有限（没有厂商提供的固定测试向量），但下面这些是真会出错的地方：
 * 日期取的不是 UTC、规范请求串的行分隔 / 空行数量、待签名串的字段顺序、派生密钥的层次。
 * 独立重算能抓住这些组成错误。
 */
public class TencentOcrSignerTest {

    /**
     * 测试用的**假凭据**：任意非空串即可，签名算法不校验它的内容。
     *
     * <p>这里一度用的是腾讯《签名方法 v3》文档示例的那组值（`AKIDz8krbs…`），而它正好命中
     * GitHub 的密钥扫描，push 被 push protection 直接挡下（`GH013`）。
     * 文档示例当然不是真密钥，但扫描器不认这个区别——**别再用任何厂商文档示例的凭据**。
     */
    private static final String SECRET_ID = "test-secret-id-0001";
    private static final String SECRET_KEY = "test-secret-key-0001";
    private static final String HOST = "ocr.tencentcloudapi.com";
    private static final String SERVICE = "ocr";
    private static final long TIMESTAMP = 1551113065L; // 2019-02-26 00:44:25 UTC+8

    @Test
    public void testUtcDate_usesUtcNotLocalZone() {
        // 1551113065 = 2019-02-25 16:44:25 UTC（东八区是 02-26 00:44），日期必须取 UTC 的 2019-02-25
        assertEquals("2019-02-25", TencentOcrSigner.utcDate(TIMESTAMP));
    }

    @Test
    public void testAuthorization_format() {
        String authorization = TencentOcrSigner.buildAuthorization(
                SECRET_ID, SECRET_KEY, HOST, SERVICE, "{\"ImageBase64\":\"abc\"}", TIMESTAMP);

        assertTrue(authorization.startsWith("TC3-HMAC-SHA256 Credential=" + SECRET_ID
                + "/2019-02-25/ocr/tc3_request, SignedHeaders=content-type;host, Signature="));
        // Signature 是 64 位十六进制
        String signature = authorization.substring(authorization.indexOf("Signature=") + "Signature=".length());
        assertTrue(signature.matches("[0-9a-f]{64}"), "签名应是 64 位十六进制，实际=" + signature);
    }

    @Test
    public void testAuthorization_matchesIndependentRecomputation() throws Exception {
        String payload = "{\"ImageBase64\":\"abc\",\"CardSide\":\"FRONT\"}";

        String authorization = TencentOcrSigner.buildAuthorization(
                SECRET_ID, SECRET_KEY, HOST, SERVICE, payload, TIMESTAMP);

        assertEquals(independentAuthorization(SECRET_ID, SECRET_KEY, HOST, SERVICE, payload, TIMESTAMP),
                authorization);
    }

    @Test
    public void testAuthorization_isDeterministic() {
        String payload = "{}";
        assertEquals(
                TencentOcrSigner.buildAuthorization(SECRET_ID, SECRET_KEY, HOST, SERVICE, payload, TIMESTAMP),
                TencentOcrSigner.buildAuthorization(SECRET_ID, SECRET_KEY, HOST, SERVICE, payload, TIMESTAMP));
    }

    @Test
    public void testAuthorization_changesWithPayloadTimestampAndSecret() {
        String baseline = TencentOcrSigner.buildAuthorization(
                SECRET_ID, SECRET_KEY, HOST, SERVICE, "{}", TIMESTAMP);

        assertNotEquals(baseline, TencentOcrSigner.buildAuthorization(
                SECRET_ID, SECRET_KEY, HOST, SERVICE, "{\"ImageBase64\":\"x\"}", TIMESTAMP), "请求体不同签名必须不同");
        assertNotEquals(baseline, TencentOcrSigner.buildAuthorization(
                SECRET_ID, SECRET_KEY, HOST, SERVICE, "{}", TIMESTAMP + 1), "时间戳不同签名必须不同");
        assertNotEquals(baseline, TencentOcrSigner.buildAuthorization(
                SECRET_ID, "another-secret", HOST, SERVICE, "{}", TIMESTAMP), "密钥不同签名必须不同");
    }

    // ==================== 独立重算（不复用生产代码） ====================

    private String independentAuthorization(String secretId, String secretKey, String host, String service,
                                            String payload, long timestamp) throws Exception {
        String date = Instant.ofEpochSecond(timestamp).atZone(ZoneOffset.UTC).toLocalDate()
                .format(DateTimeFormatter.ISO_LOCAL_DATE);
        String canonicalHeaders = "content-type:application/json; charset=utf-8\nhost:" + host + "\n";
        String canonicalRequest = "POST\n/\n\n" + canonicalHeaders + "\n" + "content-type;host" + "\n"
                + sha256Hex(payload.getBytes(StandardCharsets.UTF_8));
        String credentialScope = date + "/" + service + "/tc3_request";
        String stringToSign = "TC3-HMAC-SHA256\n" + timestamp + "\n" + credentialScope + "\n"
                + sha256Hex(canonicalRequest.getBytes(StandardCharsets.UTF_8));
        byte[] secretDate = hmac(("TC3" + secretKey).getBytes(StandardCharsets.UTF_8), date.getBytes(StandardCharsets.UTF_8));
        byte[] secretService = hmac(secretDate, service.getBytes(StandardCharsets.UTF_8));
        byte[] secretSigning = hmac(secretService, "tc3_request".getBytes(StandardCharsets.UTF_8));
        String signature = hex(hmac(secretSigning, stringToSign.getBytes(StandardCharsets.UTF_8)));
        return "TC3-HMAC-SHA256 Credential=" + secretId + "/" + credentialScope
                + ", SignedHeaders=content-type;host, Signature=" + signature;
    }

    private byte[] hmac(byte[] key, byte[] message) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(message);
    }

    private String sha256Hex(byte[] data) throws Exception {
        return hex(MessageDigest.getInstance("SHA-256").digest(data));
    }

    private String hex(byte[] data) {
        StringBuilder builder = new StringBuilder(data.length * 2);
        for (byte b : data) {
            builder.append(Character.forDigit((b >> 4) & 0xF, 16));
            builder.append(Character.forDigit(b & 0xF, 16));
        }
        return builder.toString();
    }

}
