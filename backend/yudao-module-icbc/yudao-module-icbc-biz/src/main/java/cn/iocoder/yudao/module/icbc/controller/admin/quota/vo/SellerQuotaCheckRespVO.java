package cn.iocoder.yudao.module.icbc.controller.admin.quota.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 「这笔金额还能不能开」的额度结论：收购登记时给余量提示，开票申请时做硬校验。
 *
 * <p>{@link #passed} 只回答 500 万上限；{@link #monthlyOverExempt} 是 10 万元免征线的
 * 提醒——超免征线不是拒绝理由，而是要按时代办申报缴款（见代办税费申报）。
 */
@Schema(description = "管理后台 - 出售者额度校验 Response VO")
@Data
public class SellerQuotaCheckRespVO {

    @Schema(description = "本次金额后是否仍在上限内")
    private Boolean passed;

    @Schema(description = "窗口上限（元）", example = "5000000.00")
    private BigDecimal capAmount;

    @Schema(description = "窗口内已用额度（元）")
    private BigDecimal usedAmount;

    @Schema(description = "当前余量（元）")
    private BigDecimal remainingAmount;

    @Schema(description = "本次金额入账后的剩余额度（元，可为负）")
    private BigDecimal remainingAfterAmount;

    @Schema(description = "本次是否超 500 万上限")
    private Boolean quotaExceeded;

    @Schema(description = "结论（哪里不满足 / 还剩多少）")
    private String message;

    @Schema(description = "不通过时怎么补（通过时为 null）")
    private String remedy;

    @Schema(description = "本月净销售额（元）")
    private BigDecimal currentMonthAmount;

    @Schema(description = "月销售额免征线（元）", example = "100000.00")
    private BigDecimal monthlyExemptAmount;

    @Schema(description = "本次金额入账后本月是否超过 10 万元免征线（须代办申报缴款）")
    private Boolean monthlyOverExempt;

}
