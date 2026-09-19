package cn.iocoder.yudao.module.icbc.controller.admin.tax.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 待补缴累计金额：按 1% 与 3% 分列。
 *
 * <p>申报表上「3% 征收率减按 1%」与「放弃减按」是两栏，补缴也得对得上，
 * 所以待补缴不能只有一个合计。
 */
@Schema(description = "管理后台 - 待补缴累计 Response VO")
@Data
public class TaxSupplementSummaryRespVO {

    @Schema(description = "待补缴笔数", example = "2")
    private Integer pendingCount;

    @Schema(description = "待补缴累计金额（元）")
    private BigDecimal pendingAmount;

    @Schema(description = "其中按 3% 减按 1% 部分（元）")
    private BigDecimal pendingAmountAtOnePercent;

    @Schema(description = "其中放弃减按、按 3% 部分（元）")
    private BigDecimal pendingAmountAtThreePercent;

    @Schema(description = "结论")
    private String message;

}
