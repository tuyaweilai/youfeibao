package cn.iocoder.yudao.module.icbc.controller.admin.report.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 管理后台 - 收购台账 Response VO（#57 T19）。
 *
 * <p>一行 = 一张收购单。各重量口径分列，**都来自这张收购单已经发生的计量结果**；
 * 预约约量与采购计划量不混入（见 {@code ReportTableEnum.ACQUISITION_LEDGER} 的口径说明）。
 */
@Schema(description = "管理后台 - 收购台账 Response VO")
@Data
public class ReportAcquisitionLedgerRespVO {

    @Schema(description = "收购单编号（下钻入口）")
    private Long acquisitionId;

    @Schema(description = "收购单号")
    private String acquisitionNo;

    @Schema(description = "交易对方")
    private String sellerName;

    @Schema(description = "场站编号")
    private Long stationId;

    @Schema(description = "场站名称")
    private String stationName;

    @Schema(description = "是否直接收购")
    private Boolean directAcquisition;

    @Schema(description = "回收方式文案（直接收购 / 采购订单 POxxx）")
    private String acquisitionModeText;

    @Schema(description = "关联采购订单编号")
    private Long purchaseOrderId;

    @Schema(description = "关联采购订单号")
    private String purchaseOrderNo;

    @Schema(description = "品类名称")
    private String categoryName;

    @Schema(description = "规格 / 等级")
    private String specification;

    @Schema(description = "单位")
    private String unit;

    // ==================== 各重量口径 ====================

    @Schema(description = "毛重")
    private BigDecimal grossWeight;

    @Schema(description = "皮重")
    private BigDecimal tareWeight;

    @Schema(description = "净重（毛重 − 皮重）")
    private BigDecimal netWeight;

    @Schema(description = "结算重量（毛重 − 皮重 − 扣杂，唯一计价基准，ADR 0019）")
    private BigDecimal settlementWeight;

    @Schema(description = "接收量")
    private BigDecimal acceptedWeight;

    @Schema(description = "退回量")
    private BigDecimal rejectedWeight;

    @Schema(description = "余货出场量")
    private BigDecimal residualWeight;

    @Schema(description = "称量差异（实物量 − 结算重量，#53 落库）")
    private BigDecimal weightDiff;

    // ==================== 成交与对应单据 ====================

    @Schema(description = "数量（发票明细口径）")
    private BigDecimal quantity;

    @Schema(description = "含税单价")
    private BigDecimal unitPrice;

    @Schema(description = "成交金额")
    private BigDecimal amount;

    @Schema(description = "所属结算单编号")
    private Long settlementId;

    @Schema(description = "所属结算单号")
    private String settlementNo;

    @Schema(description = "关联发票合作方订单号")
    private String invoicePartnerOrderId;

    @Schema(description = "交易时间")
    private LocalDateTime tradeTime;

    @Schema(description = "状态名")
    private String statusName;

}
