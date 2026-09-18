package cn.iocoder.yudao.module.icbc.controller.admin.payment.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 转账回单归档信息 VO。
 *
 * <p>支付成功后工行回传的转账凭证被归档到支付单上，并作为该笔收购的<strong>资金流</strong>
 * 证据挂回收购单。这里返回的即归档结果。
 */
@Schema(description = "管理后台 - 转账回单")
@Data
public class PaymentReceiptRespVO {

    @Schema(description = "合作方订单号", example = "ACQ202601011200001234")
    private String partnerOrderId;

    @Schema(description = "支付订单号", example = "PAY202601011200001234")
    private String orderNo;

    @Schema(description = "收购单编号", example = "1024")
    private Long acquisitionId;

    @Schema(description = "转账回单号", example = "SN202601011200001234")
    private String receiptNo;

    @Schema(description = "回单归档时间")
    private LocalDateTime receiptTime;

    @Schema(description = "回单文件地址（可空，工行未提供文件时以回单号与流水为准）")
    private String receiptFileUrl;

    @Schema(description = "支付流水号")
    private String paymentSerialNo;

    @Schema(description = "应付金额（元）", example = "1000.00")
    private BigDecimal paymentAmount;

    @Schema(description = "实际到账金额（元）", example = "1000.00")
    private BigDecimal actuallyReceivedAmount;

    @Schema(description = "支付状态名", example = "支付成功")
    private String paymentStatusName;

}
