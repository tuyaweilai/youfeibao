package cn.iocoder.yudao.module.icbc.service.cardrecognition.tencent;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 腾讯云卡证识别配置（#93，ADR 0037）。
 *
 * <p>密钥只落后端：前端只把压缩后的图片交给平台，腾讯的 SecretId / SecretKey 不出现在任何
 * 前端产物里（验收「密钥只落在后端」）。未配置（{@code secret-id} / {@code secret-key} 为空）时
 * 实现走**降级路径**返回空结果，与「额度耗尽」同一条路（ADR 0037）。
 *
 * <p>是否启用真实实现由 {@code icbc.card-recognition.mode} 决定（{@code tencent} / 缺省 {@code stub}），
 * 与 {@code icbc.gateway.mode} 同一种做法：两个 Bean 用 {@code @ConditionalOnProperty} 二选一。
 */
@Data
@Component
@ConfigurationProperties(prefix = "icbc.card-recognition")
public class TencentCardRecognitionProperties {

    /**
     * 腾讯云 API 密钥 SecretId（走环境变量注入，不硬编码）
     */
    private String secretId;

    /**
     * 腾讯云 API 密钥 SecretKey
     */
    private String secretKey;

    /**
     * 地域（OCR 是全局服务，签名需要，默认广州）
     */
    private String region = "ap-guangzhou";

    /**
     * OCR 服务域名
     */
    private String endpoint = "ocr.tencentcloudapi.com";

    /**
     * 服务名（签名用）
     */
    private String service = "ocr";

    /**
     * API 版本（签名与请求头用）
     */
    private String version = "2018-11-19";

    /**
     * 接口超时（毫秒）：现场弱网，超时即降级为手工录入，不把收货员卡在识别上
     */
    private Integer timeout = 10000;

}
