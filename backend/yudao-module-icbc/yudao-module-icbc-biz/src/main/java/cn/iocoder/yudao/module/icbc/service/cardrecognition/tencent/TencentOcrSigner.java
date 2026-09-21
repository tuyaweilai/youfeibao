package cn.iocoder.yudao.module.icbc.service.cardrecognition.tencent;

import cn.hutool.core.util.HexUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.crypto.digest.HMac;
import cn.hutool.crypto.digest.HmacAlgorithm;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * 腾讯云 API 3.0 签名（TC3-HMAC-SHA256）。
 *
 * <p>只做一件事：把一次 HTTP 请求压成 {@code Authorization} 头。厂商 SDK 会替我们做这件事，
 * 但引入 SDK 会带进一整棵依赖树，而本模块只需要三个 Action（身份证正反面、银行卡），
 * 用现成的 Hutool 摘要 / HMAC 自己算更轻（ADR 0037 说的是「宿主是独立端口，不是具体 SDK」）。
 *
 * <p>算法见腾讯云《签名方法 v3》：规范请求串 → 待签名字符串 → 逐级派生签名密钥 → 拼
 * {@code Authorization}。步骤全部是纯函数，可用固定输入钉住（不触网）。
 */
public final class TencentOcrSigner {

    private static final String ALGORITHM = "TC3-HMAC-SHA256";
    private static final String CONTENT_TYPE = "application/json; charset=utf-8";
    private static final String SIGNED_HEADERS = "content-type;host";

    private TencentOcrSigner() {
    }

    /**
     * 生成 {@code Authorization} 头。
     *
     * @param secretId  腾讯云 SecretId
     * @param secretKey 腾讯云 SecretKey
     * @param host      请求域名（如 {@code ocr.tencentcloudapi.com}）
     * @param service   服务名（如 {@code ocr}）
     * @param payload   请求体 JSON（与真正发出去的一字不差，否则签名对不上）
     * @param timestamp Unix 秒（与 {@code X-TC-Timestamp} 一致）
     */
    public static String buildAuthorization(String secretId, String secretKey, String host, String service,
                                            String payload, long timestamp) {
        String date = utcDate(timestamp);
        // 1. 规范请求串
        String canonicalHeaders = "content-type:" + CONTENT_TYPE + "\n" + "host:" + host + "\n";
        String canonicalRequest = "POST\n/\n\n" + canonicalHeaders + "\n" + SIGNED_HEADERS + "\n"
                + DigestUtil.sha256Hex(payload);
        // 2. 待签名字符串
        String credentialScope = date + "/" + service + "/tc3_request";
        String stringToSign = ALGORITHM + "\n" + timestamp + "\n" + credentialScope + "\n"
                + DigestUtil.sha256Hex(canonicalRequest);
        // 3. 逐级派生签名密钥：TC3 + SecretKey → 日期 → 服务 → tc3_request
        byte[] secretDate = hmac256(("TC3" + secretKey).getBytes(StandardCharsets.UTF_8), date);
        byte[] secretService = hmac256(secretDate, service);
        byte[] secretSigning = hmac256(secretService, "tc3_request");
        String signature = HexUtil.encodeHexStr(hmac256(secretSigning, stringToSign));
        // 4. 拼 Authorization
        return ALGORITHM + " Credential=" + secretId + "/" + credentialScope
                + ", SignedHeaders=" + SIGNED_HEADERS + ", Signature=" + signature;
    }

    /**
     * 时间戳对应的 UTC 日期（{@code yyyy-MM-dd}）——腾讯签名里的日期取的是 UTC，不是东八区。
     */
    public static String utcDate(long timestamp) {
        return Instant.ofEpochSecond(timestamp)
                .atZone(ZoneOffset.UTC)
                .toLocalDate()
                .format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    private static byte[] hmac256(byte[] key, String message) {
        return new HMac(HmacAlgorithm.HmacSHA256, key).digest(message.getBytes(StandardCharsets.UTF_8));
    }

}
