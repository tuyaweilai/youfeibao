package cn.iocoder.yudao.module.icbc.controller.admin.invoice.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

/**
 * 单笔开票申请入参：对一张已登记的收购单发起开票。
 */
@Schema(description = "管理后台 - 单笔开票申请入参")
@Data
@EqualsAndHashCode(callSuper = true)
public class InvoiceApplicationApplyReqVO extends InvoiceApplicationBaseReqVO {

    @Schema(description = "收购单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "收购单编号不能为空")
    private Long acquisitionId;
}
