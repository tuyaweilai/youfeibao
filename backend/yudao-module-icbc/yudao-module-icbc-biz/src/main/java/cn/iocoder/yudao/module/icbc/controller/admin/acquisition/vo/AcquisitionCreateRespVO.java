package cn.iocoder.yudao.module.icbc.controller.admin.acquisition.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 收购登记响应：单据本身 + 这次交易的<b>额度余量提示</b>（issue #12）。
 *
 * <p>登记现场就要让收货员看到「这个出售者还剩多少额度」，而不是等到开票申请被拒才发现——
 * 额度是自然人跨租户累计的，收货员不看，现场谁也看不出来。这里只提示、不拦截：拦截发生在
 * 开票申请（{@code SELLER_QUOTA} 前置校验项）。
 */
@Schema(description = "管理后台 - 收购登记 Response VO")
@Data
public class AcquisitionCreateRespVO {

    @Schema(description = "收购单编号", example = "1024")
    private Long id;

    @Schema(description = "收购单号", example = "ACQ17645472000001234")
    private String acquisitionNo;

    @Schema(description = "连续 12 个月额度上限（元）", example = "5000000.00")
    private BigDecimal quotaCapAmount;

    @Schema(description = "该出售者连续 12 个月已用额度（元）")
    private BigDecimal quotaUsedAmount;

    @Schema(description = "剩余额度（元）", example = "4876543.22")
    private BigDecimal quotaRemainingAmount;

    @Schema(description = "本次交易后是否仍在上限内；false 表示这一笔将超过 500 万，登记后不能开票")
    private Boolean quotaPassed;

    @Schema(description = "额度结论（可直接展示给收货员 / 出售者）")
    private String quotaMessage;

    @Schema(description = "本次交易后本月销售额是否超过 10 万元免征线（须按时代办申报缴款）")
    private Boolean monthlyOverExempt;

}
