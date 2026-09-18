package cn.iocoder.yudao.module.icbc.controller.admin.payment.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 工行付方支付状态查询响应 VO
 *
 * @author 芋道源码
 */
@Schema(description = "管理后台 - 工行付方支付状态查询响应")
@Data
public class PaymentStatusQueryRespVO {

    @Schema(description = "返回码", example = "0")
    private String returnCode;

    @Schema(description = "返回消息", example = "成功")
    private String returnMsg;

    @Schema(description = "合作方订单ID", example = "2018040908")
    private String outOrderId;

    @Schema(description = "工行订单号", example = "ICBC202312010001")
    private String icbcOrderNo;

    @Schema(description = "支付状态", example = "SUCCESS")
    private String paymentStatus;

    @Schema(description = "支付金额", example = "1000.00")
    private BigDecimal paymentAmount;

    @Schema(description = "支付时间", example = "2023-12-01 10:30:00")
    private LocalDateTime paymentTime;

    @Schema(description = "收方编号", example = "010020200513111111")
    private String outVendorId;

    @Schema(description = "付方编号", example = "10000000000000003")
    private String outUserId;

    @Schema(description = "支付流水号", example = "PAY202312010001")
    private String paymentSerialNo;

    @Schema(description = "错误码", example = "10101901")
    private String errorCode;

    @Schema(description = "错误信息", example = "订单支付中或已支付成功")
    private String errorMsg;

} 