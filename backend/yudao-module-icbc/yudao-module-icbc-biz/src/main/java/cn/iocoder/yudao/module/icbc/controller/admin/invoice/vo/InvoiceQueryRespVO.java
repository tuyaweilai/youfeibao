package cn.iocoder.yudao.module.icbc.controller.admin.invoice.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 工行反向开票预查询响应 VO
 *
 * @author 芋道源码
 */
@Schema(description = "管理后台 - 工行反向开票预查询响应 VO")
@Data
public class InvoiceQueryRespVO {

    @Schema(description = "返回码", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    private Integer returnCode;

    @Schema(description = "返回码说明", requiredMode = Schema.RequiredMode.REQUIRED, example = "成功")
    private String returnMsg;

    @Schema(description = "订单号（我方生成）", example = "ORD202312010001")
    private String orderNo;

    @Schema(description = "合作方订单ID", example = "2018040908")
    private String partnerOrderId;

    @Schema(description = "订单状态：0-待确认，1-已确认，2-已支付，3-已开票，4-已完成，9-已取消", example = "2")
    private Integer orderStatus;

    @Schema(description = "开票状态：0-未开票，1-开票中，2-开票成功，3-开票失败", example = "2")
    private Integer invoiceStatus;

    @Schema(description = "支付状态：0-未支付，1-支付中，2-支付成功，3-支付失败", example = "2")
    private Integer paymentStatus;

    @Schema(description = "缴税状态：0-未缴税，1-缴税中，2-缴税成功，3-缴税失败", example = "2")
    private Integer taxStatus;

    @Schema(description = "发票号码", example = "12345678")
    private String invoiceNo;

    @Schema(description = "发票代码", example = "144031909110")
    private String invoiceCode;

    @Schema(description = "开票日期", example = "2023-12-01 10:30:00")
    private LocalDateTime invoiceDate;

    @Schema(description = "发票金额", example = "1000.00")
    private BigDecimal invoiceAmount;

    @Schema(description = "税额", example = "130.00")
    private BigDecimal taxAmount;

    @Schema(description = "红冲流水号", example = "RED202312010001")
    private String redSerialNo;

    @Schema(description = "红票发票号码", example = "87654321")
    private String redInvoiceNo;

    @Schema(description = "红票开票日期", example = "2023-12-02 14:30:00")
    private LocalDateTime redInvoiceDate;

} 