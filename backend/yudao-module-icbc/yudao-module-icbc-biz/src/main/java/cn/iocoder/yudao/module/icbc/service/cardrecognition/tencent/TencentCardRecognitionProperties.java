package cn.iocoder.yudao.module.icbc.service.cardrecognition.tencent;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 腾讯云卡证识别配置（#93，ADR 0037）。
 *
 * <p>它是**回落层**：自 #103 起，供应商 / 密钥 / 地域 / endpoint / 超时的第一优先级是后台
 * 「平台运营 / 卡证识别」存的 DB 配置；DB 为空才用这里的 yaml / env（页面会标注「来自配置文件」）。
 * 所以本地 `application-local.yaml` 的 {@code icbc.card-recognition.*} 既是默认值也是一份可用的兜底。
 *
 * <p>密钥只落后端：前端只把压缩后的图片交给平台，腾讯的 SecretId / SecretKey 不出现在任何
 * 前端产物里（验收「密钥只落在后端」）。未配置（{@code secret-id} / {@code secret-key} 为空）时
 * 真实实现走**降级路径**返回空结果，与「额度耗尽」同一条路（ADR 0037）。
 */
@Data
@Component
@ConfigurationProperties(prefix = "icbc.card-recognition")
public class TencentCardRecognitionProperties {

    /**
     * 供应商：{@code stub}（默认，不触网、返回空结果）/ {@code tencent}（走腾讯云）。
     *
     * <p>它取代了 #93 的 {@code mode}：不再是启动期 {@code @ConditionalOnProperty} 二选一的开关，
     * 而是**运行期**由 {@link cn.iocoder.yudao.module.icbc.service.cardrecognition.CardRecognitionPort}
     * 每次调用时读的回落值（DB 有值以 DB 为准）。
     */
    private String provider = "stub";

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
     * 接口超时（毫秒）：现场弱网，超时即降级为手工录入，不把收货员卡在识别上
     */
    private Integer timeout = 10000;

}
