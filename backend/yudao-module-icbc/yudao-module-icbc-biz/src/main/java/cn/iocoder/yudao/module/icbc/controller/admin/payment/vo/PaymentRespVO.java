package cn.iocoder.yudao.module.icbc.controller.admin.payment.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 工行付方支付响应 VO
 *
 * @author 芋道源码
 */
@Schema(description = "管理后台 - 工行付方支付响应")
@Data
public class PaymentRespVO {

    @Schema(description = "返回码", example = "0")
    private String returnCode;

    @Schema(description = "返回消息", example = "成功")
    private String returnMsg;

    @Schema(description = "支付页面重定向URL", example = "https://gw.open.icbc.com.cn/ui/jft/ui/invoice/pay/V1")
    private String redirectUrl;

    @Schema(description = "消息通讯唯一编号", example = "urcnl24ciutr9")
    private String msgId;

    @Schema(description = "合作方订单ID", example = "2018040908")
    private String outOrderId;

    @Schema(description = "工行订单号", example = "ICBC202312010001")
    private String icbcOrderNo;

    @Schema(description = "支付状态", example = "PENDING")
    private String paymentStatus;

} 