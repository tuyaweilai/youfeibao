package cn.iocoder.yudao.module.icbc.controller.admin.purchasecontract.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 管理后台 - 采购合同送审 Request VO（#45 / T07）。
 *
 * <p>送审落一版快照；审核通过前不得作为采购依据。
 */
@Schema(description = "管理后台 - 采购合同送审 Request VO")
@Data
public class PurchaseContractSubmitReqVO {

    @Schema(description = "合同编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "合同编号不能为空")
    private Long id;

    @Schema(description = "本次送审说明 / 变更原因")
    private String changeReason;

}
