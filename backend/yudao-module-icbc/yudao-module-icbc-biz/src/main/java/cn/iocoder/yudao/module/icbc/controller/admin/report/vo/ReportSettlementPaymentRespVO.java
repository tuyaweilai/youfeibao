package cn.iocoder.yudao.module.icbc.controller.admin.report.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 管理后台 - 结算付款表 Response VO（#57 T19）。
 *
 * <p>一行 = 一张结算单。结算金额取该结算单下未作废收购单的应付金额合计（ADR 0021）。
 * 付款仍按收购单逐笔进行，这里只按结算单汇总进度：已支付 / 待办理 / 异常各多少笔，
 * 回单是否齐全，失败原因是什么，未办理多久了。
 */
@Schema(description = "管理后台 - 结算付款表 Response VO")
@Data
public class ReportSettlementPaymentRespVO {

    @Schema(description = "结算单编号（下钻入口）")
    private Long settlementId;

    @Schema(description = "结算单号")
    private String settlementNo;

    @Schema(description = "出售者")
    private String sellerName;

    @Schema(description = "场站名称")
    private String stationName;

    @Schema(description = "结算金额（未作废收购单应付金额合计）")
    private BigDecimal settlementAmount;

    @Schema(description = "收购单笔数")
    private Integer acquisitionCount;

    @Schema(description = "确认状态名")
    private String confirmStatusName;

    @Schema(description = "付款办理进度编码：UNPAID / PROCESSING / SUCCESS / FAILED")
    private String paymentProgress;

    @Schema(description = "付款办理进度文案")
    private String paymentProgressName;

    @Schema(description = "已支付笔数")
    private Integer paidCount;

    @Schema(description = "待办理（未发起或支付中）笔数")
    private Integer pendingCount;

    @Schema(description = "异常（支付失败 / 退汇 / 部分成功等）笔数")
    private Integer failedCount;

    @Schema(description = "回单状态编码：RECEIVED-已回单，PENDING-未回单")
    private String receiptStatus;

    @Schema(description = "失败原因（有异常时取最早一笔的银行错误信息）")
    private String failReason;

    @Schema(description = "未办理时长（小时；从结算单生成到最新一笔付款办结，或到当前时刻）")
    private Long unhandledHours;

    @Schema(description = "结算单生成时间")
    private LocalDateTime generateTime;

}
