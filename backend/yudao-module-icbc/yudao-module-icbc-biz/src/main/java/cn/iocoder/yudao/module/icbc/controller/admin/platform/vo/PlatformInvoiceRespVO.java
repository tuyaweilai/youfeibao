package cn.iocoder.yudao.module.icbc.controller.admin.platform.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 平台运营 - 跨租户发票视图 Response VO
 *
 * <p>平台运营看的是「哪个租户的哪张票」，因此显式带上 {@code tenantId}。
 */
@Schema(description = "管理后台 - 平台运营跨租户发票 Response VO")
@Data
public class PlatformInvoiceRespVO {

    @Schema(description = "租户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long tenantId;

    @Schema(description = "订单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "INV202409180001")
    private String orderNo;

    @Schema(description = "合作方订单ID", example = "PARTNER_202409180001")
    private String partnerOrderId;

    @Schema(description = "收方编号", example = "PAYEE001")
    private String payeeNo;

    @Schema(description = "付方编号", example = "PAYER001")
    private String payerNo;

    @Schema(description = "订单总金额（元）", example = "1000.00")
    private BigDecimal totalAmount;

    @Schema(description = "发票号码", example = "24312000000012345678")
    private String invoiceNo;

    @Schema(description = "订单状态", example = "3")
    private Integer orderStatus;

    @Schema(description = "开票状态", example = "2")
    private Integer invoiceStatus;

    @Schema(description = "支付状态", example = "2")
    private Integer paymentStatus;

    @Schema(description = "缴税状态", example = "2")
    private Integer taxStatus;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
