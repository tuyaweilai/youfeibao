package cn.iocoder.yudao.module.icbc.controller.admin.settlement.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

/**
 * 作废未开票的收购单。货已收，作废是敏感动作：必须留原因，且对自然人可见（ADR 0018）。
 */
@Schema(description = "管理后台 - 作废收购单 Request VO")
@Data
public class SettlementAcquisitionCancelReqVO {

    @Schema(description = "收购单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    @NotNull(message = "收购单编号不能为空")
    private Long acquisitionId;

    @Schema(description = "作废原因（对自然人可见）", requiredMode = Schema.RequiredMode.REQUIRED, example = "磅单重复录入，已合并到另一笔")
    @NotEmpty(message = "作废原因不能为空")
    private String reason;

}
