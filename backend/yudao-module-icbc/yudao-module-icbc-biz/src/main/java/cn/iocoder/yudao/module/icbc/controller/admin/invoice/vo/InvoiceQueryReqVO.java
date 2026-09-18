package cn.iocoder.yudao.module.icbc.controller.admin.invoice.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;

/**
 * 工行反向开票预查询请求 VO
 *
 * @author 芋道源码
 */
@Schema(description = "管理后台 - 工行反向开票预查询请求 VO")
@Data
public class InvoiceQueryReqVO {

    @Schema(description = "合作方订单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "2018040908")
    @NotEmpty(message = "合作方订单ID不能为空")
    @Size(max = 35, message = "合作方订单ID长度不能超过35个字符")
    private String outOrderId;

    @Schema(description = "红冲流水号", example = "RED202312010001")
    @Size(max = 64, message = "红冲流水号长度不能超过64个字符")
    private String redSerialNo;

} 