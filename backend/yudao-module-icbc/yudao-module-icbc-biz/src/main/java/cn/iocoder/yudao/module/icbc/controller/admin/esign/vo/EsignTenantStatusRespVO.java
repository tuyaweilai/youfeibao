package cn.iocoder.yudao.module.icbc.controller.admin.esign.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 本租户电子签章状态 Response VO（#92）。
 *
 * <p>管理员在后台看到的就是这一屏：开通到哪一步、印章是否就位、合同额度还剩多少。
 */
@Schema(description = "管理后台 - 本租户电子签章状态 Response VO")
@Data
public class EsignTenantStatusRespVO {

    @Schema(description = "子客编号（我们生成、不可变、不可重复）", example = "ES0000000001")
    private String subCustomerNo;

    @Schema(description = "开通状态：0-未开通，1-认证中，2-已激活", example = "2")
    private Integer activationStatus;

    @Schema(description = "开通状态名", example = "已激活")
    private String activationStatusName;

    @Schema(description = "下一步该做什么（可直接展示给管理员）")
    private String nextStep;

    @Schema(description = "经办人编号")
    private String operatorNo;

    @Schema(description = "企业印章编号")
    private String sealNo;

    @Schema(description = "企业印章是否已就位（sealNo 非空）", example = "true")
    private Boolean sealReady;

    @Schema(description = "合同额度（份数）", example = "100")
    private Integer contractQuota;

    @Schema(description = "已用合同额度（份数）", example = "12")
    private Integer contractUsed;

    @Schema(description = "剩余合同额度（份数）", example = "88")
    private Integer remainingQuota;

    @Schema(description = "激活时间")
    private LocalDateTime activatedTime;

    @Schema(description = "平台级参数是否已齐备", example = "true")
    private Boolean platformConfigured;

}
