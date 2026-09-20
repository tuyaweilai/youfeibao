package cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 管理后台 - 修改采购履约配置 Request VO（#47 T09）。
 */
@Schema(description = "管理后台 - 修改采购履约配置 Request VO")
@Data
public class PurchaseOrderSettingSaveReqVO {

    @Schema(description = "完成比例采用的履约口径：ACCEPTED-验收口径，SETTLED-结算口径",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "完成比例采用的履约口径不能为空")
    private String performanceBasis;

    @Schema(description = "超量交货的处理方式：BLOCK-拦截，APPROVAL-提交授权审核",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "超量交货的处理方式不能为空")
    private String overQuantityRule;

    @Schema(description = "过期交货的处理方式：BLOCK-拦截，APPROVAL-提交授权审核",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "过期交货的处理方式不能为空")
    private String expiredRule;

    @Schema(description = "跨场站交货的处理方式：BLOCK-拦截，APPROVAL-提交授权审核",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "跨场站交货的处理方式不能为空")
    private String crossStationRule;

    @Schema(description = "备注")
    private String remark;

}
