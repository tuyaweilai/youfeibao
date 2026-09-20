package cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 管理后台 - 采购订单执行进度 Response VO（#46 T08 立项，#47 T09 补五口径）。
 *
 * <p>履约分**计划 / 验收 / 入库 / 结算 / 未履行**五个口径，各有各的来源，不相加、不互相代替
 * （{@link cn.iocoder.yudao.module.icbc.enums.PurchaseProgressMeasureEnum} 是口径的唯一来源）。
 * 「完成比例」必须标明按哪个口径算（{@code completionBasis}），否则数字没有意义。
 *
 * <p>{@code measures} 把五个口径连同口径说明与数据来源一起返回，前端只负责展示，不在前端另算一遍。
 * 取不到数的口径（入库，#52 未落地）{@code available=false} 且带 {@code unavailableReason}，
 * 数量为空而不是 0——0 会被读成「一件没入库」。
 *
 * <p>{@code anomalies} 是「异常可见」：超量、执行中却已过期、有待审核的授权申请，都在订单详情里
 * 直接看到，不用去别的页面翻。
 */
@Schema(description = "管理后台 - 采购订单执行进度 Response VO")
@Data
public class PurchaseOrderProgressRespVO {

    @Schema(description = "采购订单编号")
    private Long orderId;

    @Schema(description = "采购订单号")
    private String orderNo;

    @Schema(description = "状态名（执行中却已过期时显示「过期」）")
    private String statusName;

    @Schema(description = "计划总量（= planQuantity，保留给列表与旧调用方）")
    private BigDecimal totalQuantity;

    @Schema(description = "计划总金额")
    private BigDecimal totalAmount;

    // ==================== 五口径 ====================

    @Schema(description = "计划量（订单明细计划量之和）")
    private BigDecimal planQuantity;

    @Schema(description = "验收量（已挂到本订单的成交数量之和；退货为负，自动扣回）")
    private BigDecimal acceptedQuantity;

    @Schema(description = "入库量（入库单 T14 / #52 未落地时为空）")
    private BigDecimal stockedQuantity;

    @Schema(description = "结算量（成交记录来源收购单已归入结算单的那部分）")
    private BigDecimal settledQuantity;

    @Schema(description = "未履行量（计划量 − 本单履约口径量；负数表示超收）")
    private BigDecimal unperformedQuantity;

    // ==================== 完成比例采用的口径 ====================

    @Schema(description = "完成比例采用的履约口径编码：ACCEPTED / SETTLED")
    private String completionBasis;

    @Schema(description = "完成比例口径名（如「验收口径」）")
    private String completionBasisName;

    @Schema(description = "完成比例口径说明")
    private String completionBasisDefinition;

    @Schema(description = "该口径当前的量（完成比例的分子）")
    private BigDecimal completionBasisQuantity;

    @Schema(description = "完成比例（0–1 之间的小数，4 位；计划量为 0 时为空）")
    private BigDecimal completionRatio;

    // ==================== 口径清单与异常 ====================

    @Schema(description = "五口径清单（编码 / 名称 / 数量 / 口径说明 / 数据来源 / 是否取得到数）")
    private List<Measure> measures;

    @Schema(description = "履约异常（超量、过期、待审核授权）")
    private List<Anomaly> anomalies;

    @Schema(description = "口径总说明")
    private String scopeNote;

    @Schema(description = "逐条明细的进度")
    private List<ItemProgress> items;

    /**
     * 单个口径。
     */
    @Schema(description = "履约口径")
    @Data
    public static class Measure {

        @Schema(description = "口径编码：PLAN / ACCEPTED / STOCKED_IN / SETTLED / UNPERFORMED")
        private String code;

        @Schema(description = "口径名")
        private String name;

        @Schema(description = "该口径的数量（取不到数时为空）")
        private BigDecimal quantity;

        @Schema(description = "口径说明：这个数字是怎么算出来的")
        private String definition;

        @Schema(description = "数据来源")
        private String source;

        @Schema(description = "现在是否取得到数")
        private Boolean available;

        @Schema(description = "取不到数的原因（取得到数时为空）")
        private String unavailableReason;
    }

    /**
     * 一条履约异常。
     */
    @Schema(description = "履约异常")
    @Data
    public static class Anomaly {

        @Schema(description = "异常编码：OVER_QUANTITY / EXPIRED_EXECUTING / PENDING_EXCEPTION")
        private String code;

        @Schema(description = "异常名")
        private String name;

        @Schema(description = "异常说明")
        private String message;

        @Schema(description = "严重程度：DANGER / WARNING")
        private String severity;
    }

    /**
     * 单条明细的进度（同样是五口径，口径与订单级一致）。
     */
    @Schema(description = "采购订单明细进度")
    @Data
    public static class ItemProgress {

        @Schema(description = "明细编号")
        private Long itemId;

        @Schema(description = "品类名称")
        private String categoryName;

        @Schema(description = "单位")
        private String unit;

        @Schema(description = "计划量")
        private BigDecimal quantity;

        @Schema(description = "验收量")
        private BigDecimal acceptedQuantity;

        @Schema(description = "入库量（#52 未落地时为空）")
        private BigDecimal stockedQuantity;

        @Schema(description = "结算量")
        private BigDecimal settledQuantity;

        @Schema(description = "未履行量（计划量 − 本单履约口径量）")
        private BigDecimal unperformedQuantity;

        @Schema(description = "是否已超量（验收量 > 计划量）")
        private Boolean overQuantity;

        @Schema(description = "成交笔数")
        private Integer dealCount;
    }

}
