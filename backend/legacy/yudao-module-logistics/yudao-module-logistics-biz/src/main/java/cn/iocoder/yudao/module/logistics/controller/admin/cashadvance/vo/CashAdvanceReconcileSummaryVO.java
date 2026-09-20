package cn.iocoder.yudao.module.logistics.controller.admin.cashadvance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - 现金代付对账汇总 Response VO")
@Data
public class CashAdvanceReconcileSummaryVO {

    @Schema(description = "总记录数", example = "100")
    private Long totalCount;

    @Schema(description = "待对账数量", example = "20")
    private Long pendingCount;

    @Schema(description = "已确认数量", example = "70")
    private Long confirmedCount;

    @Schema(description = "有异议数量", example = "10")
    private Long disputedCount;

    @Schema(description = "总金额", example = "50000.00")
    private BigDecimal totalAmount;

    @Schema(description = "待对账金额", example = "10000.00")
    private BigDecimal pendingAmount;

    @Schema(description = "已确认金额", example = "35000.00")
    private BigDecimal confirmedAmount;

    @Schema(description = "有异议金额", example = "5000.00")
    private BigDecimal disputedAmount;
} 