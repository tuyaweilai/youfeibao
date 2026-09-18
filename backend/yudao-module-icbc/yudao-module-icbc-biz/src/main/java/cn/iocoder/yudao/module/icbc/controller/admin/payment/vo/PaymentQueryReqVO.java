package cn.iocoder.yudao.module.icbc.controller.admin.payment.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 支付状态查询请求 VO
 */
@Schema(description = "管理后台 - 支付状态查询请求")
@Data
public class PaymentQueryReqVO {

    @Schema(description = "合作方订单号（等于收购单号）", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "ACQ202601011200001234")
    @NotBlank(message = "合作方订单号不能为空")
    @Size(max = 64, message = "合作方订单号长度不能超过 64 个字符")
    private String partnerOrderId;

}
