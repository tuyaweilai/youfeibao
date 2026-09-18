package cn.iocoder.yudao.module.icbc.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 工商银行接口配置属性
 *
 * 注意：appId、合作方编号、私钥、AES 密钥、SM2 密钥等敏感信息
 * 一律不在代码库中硬编码，必须通过环境变量 / 密钥管理注入。
 * 本地开发可在 application-local.yaml 中通过 {@code ${ICBC_*}} 占位符引用环境变量。
 *
 * @author 芋道源码
 */
@Data
@Component
@ConfigurationProperties(prefix = "icbc.api")
public class IcbcProperties {

    /**
     * 应用ID（合作方编号）
     */
    private String appId;

    /**
     * 付方平台外部编号（子商户标识），等价于档案参数 outVendorId
     */
    private String outVendorId;

    /**
     * RSA私钥
     */
    private String privateKey;

    /**
     * 工行API网关RSA公钥
     */
    private String apigwPublicKey;

    /**
     * AES加密密钥（Base64格式，16字节）
     */
    private String aesKey;

    /**
     * SM2私钥
     */
    private String sm2PrivateKey;

    /**
     * SM2公钥
     */
    private String sm2ApigwPublicKey;

    /**
     * 签名算法类型 RSA2/SM2
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
     * 工行网关基础URL
     */
    private String baseUrl = "https://gw.open.icbc.com.cn";

    /**
     * 支付页面URL
     */
    private String paymentUrl = "https://gw.open.icbc.com.cn/ui/jft/ui/invoice/pay/V1";

    /**
     * 发票查询URL
     */
    private String invoiceQueryUrl = "https://gw.open.icbc.com.cn/api/jft/api/invoice/queryInvoiceInfo/V1";

    /**
     * 反向开票预下单URL
     */
    private String preOrderUrl = "https://gw.open.icbc.com.cn/ui/jft/ui/invoice/pre/order/V1";

    /**
     * 付方支付查询URL
     */
    private String paymentQueryUrl = "https://gw.open.icbc.com.cn/api/jft/api/payment/queryPaymentStatus/V1";

    /**
     * 聚富通智慧清分收方查询URL
     */
    private String userQueryUrl = "https://gw.open.icbc.com.cn/api/jft/api/user/edpreceive/query/V1";

}
