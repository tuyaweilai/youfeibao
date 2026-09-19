package cn.iocoder.yudao.module.icbc.controller.admin.quota.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 出售者「出售者 × 月」额度台账中的一个月。
 *
 * <p>月销售额的 10 万元免征线按<b>自然人 × 月</b>判定，与租户无关：同一个人在多个回收
 * 企业卖货，免征额度只有一份。所以每一行都是跨租户合并后的结果。
 */
@Schema(description = "管理后台 - 出售者月度额度 Response VO")
@Data
public class SellerQuotaMonthRespVO {

    @Schema(description = "月份 yyyy-MM", example = "2026-09")
    private String month;

    @Schema(description = "当月已开票金额（元）")
    private BigDecimal issuedAmount;

    @Schema(description = "当月预开票在途金额（元，已确认待开票）")
    private BigDecimal pendingAmount;

    @Schema(description = "当月红冲金额（元，已成功红冲）")
    private BigDecimal redOffsetAmount;

    @Schema(description = "当月净销售额（元）：已开票 + 在途 − 红冲")
    private BigDecimal netAmount;

    @Schema(description = "当月按 3% 征收率减按 1% 计算的金额（元）")
    private BigDecimal amountAtOnePercent;

    @Schema(description = "当月放弃减按、按 3% 征收率计算的金额（元）")
    private BigDecimal amountAtThreePercent;

    @Schema(description = "当月征收率未识别的金额（元，历史数据兜底，不参与 1% / 3% 分列）")
    private BigDecimal otherAmount;

    @Schema(description = "当月净销售额是否超过 10 万元免征线（超过则须代办申报缴款）")
    private Boolean overMonthlyExempt;

}
