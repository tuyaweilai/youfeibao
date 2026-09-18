package cn.iocoder.yudao.module.icbc.controller.admin.payment.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付状态查询响应 VO。
 *
 * <p>同时给出平台状态（{@link #paymentStatus}）与工行原始码（{@link #payStatus}）：
 * 前者用于界面，后者用于对账与排障，两者都由「通知」与「主动查询」两条路径收敛而成，
 * 结果一致。
 */
@Schema(description = "管理后台 - 支付状态查询响应")
@Data
public class PaymentStatusRespVO {

    @Schema(description = "合作方订单号", example = "ACQ202601011200001234")
    private String partnerOrderId;

    @Schema(description = "支付订单号", example = "PAY202601011200001234")
    private String orderNo;

    @Schema(description = "收购单编号", example = "1024")
    private Long acquisitionId;

    @Schema(description = "应付金额（元）", example = "1000.00")
    private BigDecimal paymentAmount;

    @Schema(description = "实际到账金额（元），部分成功时小于应付金额", example = "1000.00")
    private BigDecimal actuallyReceivedAmount;

    @Schema(description = "支付状态值", example = "2")
    private Integer paymentStatus;

    @Schema(description = "支付状态名", example = "支付成功")
    private String paymentStatusName;

    @Schema(description = "工行原始支付状态码", example = "02")
    private String payStatus;

    @Schema(description = "是否可由用户重新发起", example = "false")
    private Boolean reInitiable;

    @Schema(description = "支付时间")
    private LocalDateTime paymentTime;

    @Schema(description = "支付流水号")
    private String paymentSerialNo;

    @Schema(description = "工行订单号")
    private String icbcOrderNo;

    @Schema(description = "转账回单号")
    private String receiptNo;

    @Schema(description = "转账回单归档时间")
    private LocalDateTime receiptTime;

    @Schema(description = "错误码")
    private String errorCode;

    @Schema(description = "错误信息")
    private String errorMsg;

}
