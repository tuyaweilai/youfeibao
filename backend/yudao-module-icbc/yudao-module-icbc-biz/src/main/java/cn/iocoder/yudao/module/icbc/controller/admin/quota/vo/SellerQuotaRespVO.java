package cn.iocoder.yudao.module.icbc.controller.admin.quota.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 自然人出售者的额度台账：连续 12 个月滚动窗口内的累计销售额与余量。
 *
 * <p>额度属于<b>自然人</b>，不属于租户：同一个自然人在本平台多个租户下的开票额合并计算。
 * 台账是<b>派生视图</b>——直接从各租户的开票订单（蓝票）与红字发票（红票）算出来，不另外
 * 维护一张会漂移的汇总表。口径：
 * <ul>
 *   <li>已开票：{@code invoice_status=已开票}，按开票日期落在窗口内；</li>
 *   <li>在途：预开票成功但尚未开票（已付款待开票、开票中），按预下单日期落在窗口内；</li>
 *   <li>红冲：红票已上传成功（{@link cn.iocoder.yudao.module.icbc.enums.RedOffsetStatusEnum#SUCCESS}），
 *       且原蓝票也在窗口内，才从已用额度里扣；</li>
 *   <li>已用额度 = 已开票 + 在途 − 红冲，开票申请按「已用 + 本次金额」与 500 万上限比较。</li>
 * </ul>
 */
@Schema(description = "管理后台 - 出售者额度台账 Response VO")
@Data
public class SellerQuotaRespVO {

    @Schema(description = "出售者档案编号", example = "1024")
    private Long payeeId;

    @Schema(description = "出售者姓名", example = "张三")
    private String name;

    @Schema(description = "脱敏身份证号", example = "110101********1234")
    private String idCardMasked;

    @Schema(description = "滚动窗口上限（元）", example = "5000000.00")
    private BigDecimal capAmount;

    @Schema(description = "窗口内已开票金额（元）")
    private BigDecimal issuedAmount;

    @Schema(description = "窗口内在途金额（元，预开票成功待开票）")
    private BigDecimal pendingAmount;

    @Schema(description = "窗口内红冲金额（元）")
    private BigDecimal redOffsetAmount;

    @Schema(description = "窗口内已用额度（元）= 已开票 + 在途 − 红冲")
    private BigDecimal usedAmount;

    @Schema(description = "剩余额度（元）", example = "4876543.22")
    private BigDecimal remainingAmount;

    @Schema(description = "窗口开始时间")
    private LocalDateTime windowStart;

    @Schema(description = "窗口结束时间")
    private LocalDateTime windowEnd;

    @Schema(description = "窗口内按 3% 征收率减按 1% 计算的金额（元）")
    private BigDecimal amountAtOnePercent;

    @Schema(description = "窗口内放弃减按、按 3% 征收率计算的金额（元）")
    private BigDecimal amountAtThreePercent;

    @Schema(description = "窗口内征收率未识别的金额（元，历史数据兜底）")
    private BigDecimal otherAmount;

    @Schema(description = "月销售额免征线（元）", example = "100000.00")
    private BigDecimal monthlyExemptAmount;

    @Schema(description = "本月净销售额（元）")
    private BigDecimal currentMonthAmount;

    @Schema(description = "本月净销售额是否超过 10 万元免征线")
    private Boolean currentMonthOverExempt;

    @Schema(description = "是否已超 500 万上限")
    private Boolean quotaExceeded;

    @Schema(description = "额度结论（可直接展示给收货员 / 出售者）")
    private String message;

    @Schema(description = "按月台账，最近的月份在前")
    private List<SellerQuotaMonthRespVO> months;

}
