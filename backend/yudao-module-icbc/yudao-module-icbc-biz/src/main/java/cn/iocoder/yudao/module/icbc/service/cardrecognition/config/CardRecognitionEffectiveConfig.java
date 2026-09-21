package cn.iocoder.yudao.module.icbc.service.cardrecognition.config;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.icbc.enums.CardRecognitionProviderEnum;
import cn.iocoder.yudao.module.icbc.service.cardrecognition.tencent.TencentOcrSettings;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 卡证识别**运行期生效**的一整套参数（#103）。
 *
 * <p>它由 {@link CardRecognitionConfigService#resolveEffectiveConfig()} 按「DB 有值用 DB、
 * DB 为空回落 yaml / env」算出来，供常驻的识别端口每次调用时取用——这就是「保存后无需重启即生效」
 * 的落点：端口不再持有启动期决定，只持有这份每次现算的参数。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardRecognitionEffectiveConfig {

    /** 生效的供应商：stub / tencent */
    private String provider;

    private String secretId;

    private String secretKey;

    private String region;

    private String endpoint;

    private Integer timeout;

    /**
     * 是否腾讯云供应商。非 tencent 的取值一律当 stub（安静降级），不触网。
     */
    public boolean isTencent() {
        return CardRecognitionProviderEnum.TENCENT.getCode().equals(provider);
    }

    /**
     * 密钥是否齐备：不齐时真实实现也走空结果，与额度耗尽同一条降级路径（ADR 0037）。
     */
    public boolean hasCredentials() {
        return StrUtil.isNotBlank(secretId) && StrUtil.isNotBlank(secretKey);
    }

    /**
     * 识别能力是否真的可用：供应商是 tencent 且密钥齐备。
     */
    public boolean isEnabled() {
        return isTencent() && hasCredentials();
    }

    /**
     * 转成一次 {@link TencentOcrSettings}，直接交给 {@code TencentOcrClient}。
     */
    public TencentOcrSettings toOcrSettings() {
        return TencentOcrSettings.builder()
                .secretId(secretId)
                .secretKey(secretKey)
                .region(region)
                .endpoint(endpoint)
                .timeout(timeout)
                .build();
    }

}
