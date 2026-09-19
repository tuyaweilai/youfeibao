package cn.iocoder.yudao.module.icbc.controller.app.seller.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 自然人端 - 年度发票与税费汇总。
 *
 * <p>额度处另标「税务端可核验的口径由各回收企业的开票记录构成」（ADR 0021）。
 */
@Schema(description = "自然人端 - 年度发票汇总")
@Data
public class SellerInvoiceSummaryRespVO {

    @Schema(description = "年度", example = "2026")
    private Integer year;

    @Schema(description = "开票张数", example = "5")
    private Integer invoiceCount;

    @Schema(description = "开票金额合计（价税合计，本平台累计）", example = "5000.00")
    private BigDecimal totalInvoiceAmount;

    @Schema(description = "税额合计", example = "50.00")
    private BigDecimal totalTaxAmount;

    @Schema(description = "税务端可核验口径说明")
    private String taxScopeNote;

    @Schema(description = "当年的发票明细")
    private List<SellerInvoiceRespVO> invoices;

}
