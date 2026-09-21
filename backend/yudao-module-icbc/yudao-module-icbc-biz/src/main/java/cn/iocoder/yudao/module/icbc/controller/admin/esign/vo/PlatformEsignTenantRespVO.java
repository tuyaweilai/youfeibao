package cn.iocoder.yudao.module.icbc.controller.admin.esign.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 平台运营：某个租户的电子签章状态 Response VO（#92）。
 *
 * <p>「看到每个租户的电子签章激活状态与合同额度」（#81 用户故事 30）：跨租户只读，
 * 便于运维提前发现谁快用完了额度。平台运营不代盖、也不代做认证，这里只是可见性。
 */
@Schema(description = "管理后台 - 平台运营：租户电子签章状态 Response VO")
@Data
public class PlatformEsignTenantRespVO {

    @Schema(description = "租户编号", example = "1")
    private Long tenantId;

    @Schema(description = "回收企业名称", example = "某某再生资源有限公司")
    private String tenantName;

    @Schema(description = "子客编号", example = "ES0000000001")
    private String subCustomerNo;

    @Schema(description = "开通状态：0-未开通，1-认证中，2-已激活", example = "2")
    private Integer activationStatus;

    @Schema(description = "开通状态名", example = "已激活")
    private String activationStatusName;

    @Schema(description = "企业印章编号")
    private String sealNo;

    @Schema(description = "企业印章是否已就位", example = "true")
    private Boolean sealReady;

    @Schema(description = "合同额度（份数）", example = "100")
    private Integer contractQuota;

    @Schema(description = "已用合同额度（份数）", example = "12")
    private Integer contractUsed;

    @Schema(description = "剩余合同额度（份数）", example = "88")
    private Integer remainingQuota;

    @Schema(description = "激活时间")
    private LocalDateTime activatedTime;

}
