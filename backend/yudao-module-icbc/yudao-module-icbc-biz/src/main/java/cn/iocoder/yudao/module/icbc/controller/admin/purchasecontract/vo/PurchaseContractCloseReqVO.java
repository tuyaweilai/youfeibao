package cn.iocoder.yudao.module.icbc.controller.admin.purchasecontract.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 管理后台 - 采购合同关闭 Request VO（#45 / T07）。
 */
@Schema(description = "管理后台 - 采购合同关闭 Request VO")
@Data
public class PurchaseContractCloseReqVO {

    @Schema(description = "合同编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "合同编号不能为空")
    private Long id;

    @Schema(description = "关闭原因")
    private String reason;

}
