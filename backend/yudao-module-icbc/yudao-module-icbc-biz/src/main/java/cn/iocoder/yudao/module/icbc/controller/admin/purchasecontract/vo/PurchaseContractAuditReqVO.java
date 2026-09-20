package cn.iocoder.yudao.module.icbc.controller.admin.purchasecontract.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 管理后台 - 采购合同审核 Request VO（#45 / T07）。
 *
 * <p>审核是「合同能不能作为采购依据」的唯一开关：只有审核通过的合同才生效。
 */
@Schema(description = "管理后台 - 采购合同审核 Request VO")
@Data
public class PurchaseContractAuditReqVO {

    @Schema(description = "合同编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "合同编号不能为空")
    private Long id;

    @Schema(description = "是否通过：true-通过生效，false-驳回退回草稿", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "请选择审核结论")
    private Boolean approved;

    @Schema(description = "审核意见（驳回时必填）")
    private String remark;

}
