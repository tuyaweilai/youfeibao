package cn.iocoder.yudao.module.icbc.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 工行适配层配置
 *
 * 只包含「合作方身份 + 密钥 + 网关基地址」。接口路径是常量，见 {@code IcbcApiPaths}，
 * 不做成配置项，避免平台其它部分能读到工行网关地址。
 *
 * 敏感信息（appId、私钥、AES 密钥、SM2 密钥）一律不在代码库中硬编码，
 * 必须通过环境变量 / 密钥管理注入。本地开发在 yaml 里用 {@code ${ICBC_*}} 占位符引用。
 */
@Data
@Component
@ConfigurationProperties(prefix = "icbc.api")
public class IcbcProperties {

    /**
     * 应用 ID（合作方编号）
     */
    private String appId;

    /**
     * 付方平台外部编号（子商户编号），等价于档案参数 outVendorId
     */
    private String outVendorId;

    /**
     * RSA 私钥
     */
    private String privateKey;

    /**
     * 工行 API 网关 RSA 公钥
     */
    private String apigwPublicKey;

    /**
     * AES 加密密钥（Base64 格式，16 字节）
     */
    private String aesKey;

    /**
     * SM2 私钥
     */
    private String sm2PrivateKey;

    /**
     * SM2 公钥
     */
    private String sm2ApigwPublicKey;

    /**
     * 签名算法类型 RSA2 / SM2
     */
    private String signType = "RSA2";

    /**
     * 字符集
     */
    private String charset = "UTF-8";

    /**
     * 数据格式
     */
    private String format = "json";

    /**
     * 加密类型
     */
    private String encryptType = "AES";

    /**
     * 接口超时时间（毫秒）
     */
    private Integer timeout = 30000;

    /**
     * 工行网关基地址（可指向行方正式环境或本地反向代理）
     */
    private String baseUrl = "https://gw.open.icbc.com.cn";

}
