package cn.iocoder.yudao.module.logistics.controller.admin.freight.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 运费对账汇总的一行（V8 #75）：按**承运商 + 承运合同**汇集。
 *
 * <p>「趟次」= 运费单张数；差异合计**不抹平**地摆出来，让财务能对承运商说清差在哪。
 */
@Schema(description = "管理后台 - 运费对账汇总行")
@Data
@ExcelIgnoreUnannotated
public class LogisticsFreightReconciliationRespVO {

    @Schema(description = "承运商编号")
    private Long carrierId;

    @ExcelProperty("承运商")
    @Schema(description = "承运商名称")
    private String carrierName;

    @Schema(description = "承运合同编号")
    private Long contractId;

    @ExcelProperty("承运合同")
    @Schema(description = "承运合同号")
    private String contractNo;

    @ExcelProperty("趟次")
    @Schema(description = "运费单张数（趟次）")
    private Integer tripCount;

    @ExcelProperty("应有应付合计")
    @Schema(description = "应有应付合计")
    private BigDecimal expectedTotal;

    @ExcelProperty("实际应付合计")
    @Schema(description = "实际应付合计（未对账的按应有计）")
    private BigDecimal actualTotal;

    @ExcelProperty("差异合计")
    @Schema(description = "差异合计（实际 − 应有）")
    private BigDecimal varianceTotal;

    @ExcelProperty("待确认应付")
    @Schema(description = "待确认应付的趟次")
    private Integer pendingConfirmCount;

    @ExcelProperty("已登记付款凭证")
    @Schema(description = "已登记付款凭证的趟次")
    private Integer voucherRegisteredCount;

}
