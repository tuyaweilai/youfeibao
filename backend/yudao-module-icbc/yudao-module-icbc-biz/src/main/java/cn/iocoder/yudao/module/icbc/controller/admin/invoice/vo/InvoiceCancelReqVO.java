package cn.iocoder.yudao.module.icbc.controller.admin.invoice.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;

/**
 * 发票取消请求 VO（仅限「预开票成功但未支付」的发票）
 */
@Schema(description = "管理后台 - 发票取消请求 VO")
@Data
public class InvoiceCancelReqVO {

    @Schema(description = "合作方订单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "ACQ202312010001")
    @NotEmpty(message = "合作方订单ID不能为空")
    @Size(max = 35, message = "合作方订单ID长度不能超过35个字符")
    private String partnerOrderId;
}
