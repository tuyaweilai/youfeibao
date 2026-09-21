package cn.iocoder.yudao.module.icbc.controller.admin.esign.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 电子签章签署状态通知 Response VO（#92）。
 *
 * <p>回调报文里带的是**子客编号**而不是我们的租户号，所以这里回带解析出的 {@code tenantId}，
 * 供后续 #95 路由到正确的租户收敛协议状态。本票只做「收到 + 验签 + 归一化 + 反查租户」，
 * 不改任何协议状态（协议状态机属 #95）。
 */
@Schema(description = "管理后台 - 电子签章签署状态通知 Response VO")
@Data
public class EsignCallbackRespVO {

    @Schema(description = "由回调里的子客编号反查出的租户编号", example = "1")
    private Long tenantId;

    @Schema(description = "合同组任务号", example = "TASK20260921001")
    private String signTaskId;

    @Schema(description = "合同组是否已整体签署完成", example = "true")
    private Boolean finished;

    @Schema(description = "未完成时的可读原因")
    private String unfinishedReason;

    @Schema(description = "签署完成时间（finished=true 时有值）")
    private LocalDateTime signedAt;

}
