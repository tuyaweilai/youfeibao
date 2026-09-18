package cn.iocoder.yudao.module.icbc.service.token;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.HMac;
import cn.hutool.crypto.digest.HmacAlgorithm;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.*;

/**
 * 公开令牌的签发与校验：{@code base64url(payloadJson) + "." + base64url(hmacSha256)}。
 *
 * <p>签名只保证「没被篡改」；单用途、限次、是否已用尽由 {@link PublicTokenService}
 * 查库判定。密钥走环境变量 {@code ICBC_PUBLIC_TOKEN_SECRET}，不硬编码。
 */
@Slf4j
@Component
public class PublicTokenCodec {

    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    private final String secret;

    public PublicTokenCodec(@Value("${icbc.public-token.secret:}") String secret) {
        this.secret = secret;
    }

    /**
     * 签发令牌。
     */
    public String sign(PublicTokenPayload payload) {
        String payloadPart = ENCODER.encodeToString(JsonUtils.toJsonByte(payload));
        return payloadPart + "." + signPayload(payloadPart);
    }

    /**
     * 校验令牌的格式、签名与有效期。篡改 / 格式错 → 无效；过期 → 过期。
     */
    public PublicTokenPayload verify(String token) {
        if (StrUtil.isBlank(token)) {
            throw exception(PUBLIC_TOKEN_INVALID);
        }
        int dot = token.lastIndexOf('.');
        if (dot <= 0 || dot == token.length() - 1) {
            throw exception(PUBLIC_TOKEN_INVALID);
        }
        String payloadPart = token.substring(0, dot);
        String signaturePart = token.substring(dot + 1);
        if (!MessageDigest.isEqual(signPayload(payloadPart).getBytes(StandardCharsets.UTF_8),
                signaturePart.getBytes(StandardCharsets.UTF_8))) {
            throw exception(PUBLIC_TOKEN_INVALID);
        }
        PublicTokenPayload payload;
        try {
            payload = JsonUtils.parseObject(DECODER.decode(payloadPart), PublicTokenPayload.class);
        } catch (Exception e) {
            throw exception(PUBLIC_TOKEN_INVALID);
        }
        if (payload == null || payload.getExpiresAt() == null) {
            throw exception(PUBLIC_TOKEN_INVALID);
        }
        if (payload.getExpiresAt() < System.currentTimeMillis() / 1000) {
            throw exception(PUBLIC_TOKEN_EXPIRED);
        }
        return payload;
    }

    private String signPayload(String payloadPart) {
        if (StrUtil.isBlank(secret)) {
            log.error("icbc.public-token.secret 未配置，无法签发/校验公开令牌");
            throw exception(PUBLIC_TOKEN_SECRET_MISSING);
        }
        HMac mac = new HMac(HmacAlgorithm.HmacSHA256, secret.getBytes(StandardCharsets.UTF_8));
        return ENCODER.encodeToString(mac.digest(payloadPart.getBytes(StandardCharsets.UTF_8)));
    }

}
