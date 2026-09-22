package cn.iocoder.yudao.module.icbc.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 卡证识别连通性自检的分类结果（#103）。
 *
 * <p>它落进 {@code icbc_card_recognition_config.last_check_result}，页面据此显示「已配置 / 未验证 /
 * 验证通过（时间）」。**只存分类，不存密钥、也不整段存厂商原始报文**——它是给人看的状态，
 * 不是调试垃圾桶（ADR 0037 的「识别完即弃」）。
 *
 * <p>自检走的是同一枚 {@code TencentOcrClient} 的 1x1 占位图：足以走完鉴权，在解码 / 识别阶段
 * 报业务错（照 {@code TencentCardRecognitionLiveTest}）。因此判据只有一条：**只有 {@code AuthFailure.*}
 * 判 AUTH_FAILED**，其余厂商错误都归 {@link #VENDOR_ERROR}——它其实意味着「鉴权已通过、占位图
 * 识别不了」，页面按验证通过处理（网络不可达则是另一条硬失败）。
 *
 * <p><b>#112 起多探一路 {@code LicensePlateOCR}</b>：腾讯云的识别接口要**按接口开通**，未开通时
 * 报 {@code FailedOperation.ServiceNotOpened}。那一类不再被当成「鉴权通过」——它是一条硬失败
 * {@link #SERVICE_NOT_OPENED}，否则「自检通过」要到现场第一次拍车头照时才说谎。
 */
@Getter
@AllArgsConstructor
public enum CardRecognitionCheckResultEnum {

    OK("验证通过"),
    AUTH_FAILED("密钥被拒绝"),
    NETWORK("网络不可达"),
    VENDOR_ERROR("鉴权通过（厂商在识别阶段报错）"),
    SERVICE_NOT_OPENED("识别接口未开通"),
    ;

    /** 展示名 */
    private final String name;

}
