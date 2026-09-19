package cn.iocoder.yudao.module.icbc.controller.app.seller.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 自然人端 - 「我收到了」请求。
 *
 * <p>这是自然人**自行确认**收到货款，**不改动银行状态**（{@code payStatus} 仍是唯一权威，ADR 0021）。
 */
@Schema(description = "自然人端 - 我收到了请求")
@Data
public class SellerConfirmReceiveReqVO {

    @Schema(description = "自然人主体编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "自然人主体编号不能为空")
    private Long naturalPersonId;

    @Schema(description = "支付订单编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "支付订单编号不能为空")
    private Long paymentOrderId;

}
