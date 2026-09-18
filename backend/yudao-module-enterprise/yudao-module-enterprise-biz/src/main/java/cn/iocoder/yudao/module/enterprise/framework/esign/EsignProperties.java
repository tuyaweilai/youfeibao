package cn.iocoder.yudao.module.enterprise.framework.esign;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "esign")
public class EsignProperties {
    /** e签宝开放平台基础地址，例如 https://openapi.esign.cn */
    private String baseUrl;

    /** 应用ID */
    private String appId;

    /** 应用密钥（沙箱环境使用） */
    private String appSecret;
    
    /** RSA私钥（生产环境推荐使用） */
    private String rsaPrivateKey;
    
    /** RSA公钥（用于验签） */
    private String rsaPublicKey;
    
    /** 回调通知基础URL */
    private String notifyBaseUrl;
    
    /** 重定向基础URL */
    private String redirectBaseUrl;
    
    /**
     * 是否使用RSA签名
     * true: 使用RSA签名（推荐）
     * false: 使用AppSecret（仅沙箱环境）
     */
    public boolean useRsaSignature() {
        return rsaPrivateKey != null && !rsaPrivateKey.trim().isEmpty();
    }
} 