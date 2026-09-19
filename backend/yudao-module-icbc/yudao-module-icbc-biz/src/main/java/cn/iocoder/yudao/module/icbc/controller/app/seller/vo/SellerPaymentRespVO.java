package cn.iocoder.yudao.module.icbc.controller.app.seller.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 自然人端 - 收款记录（一条支付订单）。
 *
 * <p>口径见 ADR 0021：**不使用「已到账」**。银行状态只讲可核验的事——「银行已受理（回单号）」；
 * 「我收到了」是自然人自行确认，**不改动银行状态**。
 */
@Schema(description = "自然人端 - 收款记录")
@Data
public class SellerPaymentRespVO {

    @Schema(description = "支付订单编号", example = "2048")
    private Long paymentOrderId;

    @Schema(description = "支付订单号", example = "PAY202612010001")
    private String orderNo;

    @Schema(description = "对应的收购单号", example = "AC202612010001")
    private String acquisitionNo;

    @Schema(description = "品类", example = "废钢")
    private String categoryName;

    @Schema(description = "收购方（回收企业名称）", example = "某某再生资源有限公司")
    private String acquirerName;

    @Schema(description = "付款金额", example = "980.00")
    private BigDecimal paymentAmount;

    @Schema(description = "实际到账金额（部分成功时与付款金额不同）", example = "980.00")
    private BigDecimal actuallyReceivedAmount;

    @Schema(description = "收款状态编码", example = "2")
    private Integer status;

    @Schema(description = "收款状态名（只讲可核验的事）", example = "银行已受理")
    private String statusName;

    @Schema(description = "银行已受理的回单号", example = "RCPT202612010001")
    private String receiptNo;

    @Schema(description = "回单时间")
    private LocalDateTime receiptTime;

    @Schema(description = "支付时间")
    private LocalDateTime paymentTime;

    @Schema(description = "失败等异常时的下一步提示")
    private String nextStep;

    @Schema(description = "自然人是否已自行确认收到（不改银行状态）", example = "false")
    private Boolean sellerReceivedConfirmed;

    @Schema(description = "自然人自行确认收到的时间")
    private LocalDateTime sellerReceivedConfirmedAt;

    @Schema(description = "当前是否可点「我收到了」", example = "true")
    private Boolean canConfirmReceive;

}
