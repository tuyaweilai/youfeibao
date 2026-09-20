package cn.iocoder.yudao.module.icbc.controller.admin.purchasecontract.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理后台 - 采购合同版本 Response VO（#45 / T07）。历史版本只读，不可改。
 */
@Schema(description = "管理后台 - 采购合同版本 Response VO")
@Data
public class PurchaseContractVersionRespVO {

    @Schema(description = "版本号")
    private Integer versionNo;

    @Schema(description = "变更原因")
    private String changeReason;

    @Schema(description = "送审人")
    private String changedBy;

    @Schema(description = "本版审核状态：0-待审核，1-已通过，2-已驳回")
    private Integer auditStatus;

    @Schema(description = "本版审核状态名")
    private String auditStatusName;

    @Schema(description = "本版审核人")
    private Long auditedBy;

    @Schema(description = "本版审核时间")
    private LocalDateTime auditedTime;

    @Schema(description = "本版审核意见")
    private String auditRemark;

    @Schema(description = "快照哈希（SHA-256）")
    private String snapshotHash;

    @Schema(description = "送审时间")
    private LocalDateTime createTime;

}
