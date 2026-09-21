package cn.iocoder.yudao.module.icbc.service.cardrecognition.tencent;

import cn.hutool.core.util.StrUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 一次腾讯云 OCR 调用用的**生效参数**（#103）。
 *
 * <p>为什么单独抽出来：识别实现从「启动时二选一」改成「常驻一枚 Bean、调用时判定供应商」后，
 * 密钥 / 地域 / endpoint / 超时可能来自 DB（后台配置，保存即生效）或 yaml / env（回落）。
 * 把「这一次用哪套参数」当成一个显式入参传给 {@link TencentOcrClient}，就不必让客户端去猜
 * 当前该读哪一层——测试也能直接钉住「用了哪套参数」。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TencentOcrSettings {

    private String secretId;

    private String secretKey;

    private String region;

    private String endpoint;

    private Integer timeout;

    /**
     * 密钥是否齐备：不齐时调用方一律返回空结果（安静降级），不触网。
     */
    public boolean hasCredentials() {
        return StrUtil.isNotBlank(secretId) && StrUtil.isNotBlank(secretKey);
    }

}
