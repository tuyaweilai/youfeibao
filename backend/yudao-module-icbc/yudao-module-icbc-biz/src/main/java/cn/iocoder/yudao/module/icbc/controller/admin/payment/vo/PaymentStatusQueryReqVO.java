package cn.iocoder.yudao.module.icbc.controller.admin.payment.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 工行付方支付状态查询请求 VO
 *
 * @author 芋道源码
 */
@Schema(description = "管理后台 - 工行付方支付状态查询请求")
@Data
public class PaymentStatusQueryReqVO {

    @Schema(description = "合作方订单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "2018040908")
    @NotBlank(message = "合作方订单ID不能为空")
    @Size(max = 35, message = "合作方订单ID长度不能超过35个字符")
    private String outOrderId;

    @Schema(description = "工行订单号", example = "ICBC202312010001")
    @Size(max = 64, message = "工行订单号长度不能超过64个字符")
    private String icbcOrderNo;

} 