package cn.iocoder.yudao.module.icbc.controller.admin.report.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 管理后台 - 采购履约表 Response VO（#57 T19）。
 *
 * <p>五口径分列（计划 / 验收 / 入库 / 结算 / 未履行），完成比例必须带口径；
 * 「实际履约量」与「余额」都按企业配置的履约口径（验收或结算）取数，与 #47 的履约进度同源。
 */
@Schema(description = "管理后台 - 采购履约表 Response VO")
@Data
public class ReportPurchasePerformanceRespVO {

    @Schema(description = "采购订单编号（下钻入口）")
    private Long orderId;

    @Schema(description = "采购订单号")
    private String orderNo;

    @Schema(description = "交易对方")
    private String counterpartyName;

    @Schema(description = "执行场站")
    private String stationName;

    @Schema(description = "状态名")
    private String statusName;

    @Schema(description = "到期日")
    private LocalDate endDate;

    @Schema(description = "是否已过期")
    private Boolean expired;

    // ==================== 五口径 ====================

    @Schema(description = "计划量")
    private BigDecimal planQuantity;

    @Schema(description = "实际履约量（按完成比例口径：验收或结算）")
    private BigDecimal performedQuantity;

    @Schema(description = "余额（计划量 − 实际履约量，可为负表示超收）")
    private BigDecimal balanceQuantity;

    @Schema(description = "验收量")
    private BigDecimal acceptedQuantity;

    @Schema(description = "入库量")
    private BigDecimal stockedQuantity;

    @Schema(description = "结算量")
    private BigDecimal settledQuantity;

    // ==================== 进度与异常 ====================

    @Schema(description = "完成比例采用的口径编码")
    private String completionBasis;

    @Schema(description = "完成比例口径名")
    private String completionBasisName;

    @Schema(description = "完成比例（0–1 的小数；计划量为 0 时为空）")
    private BigDecimal completionRatio;

    @Schema(description = "是否超量（验收量 > 计划量）")
    private Boolean overQuantity;

    @Schema(description = "异常名清单（超量 / 过期 / 待审核授权）")
    private List<String> anomalyNames;

}
