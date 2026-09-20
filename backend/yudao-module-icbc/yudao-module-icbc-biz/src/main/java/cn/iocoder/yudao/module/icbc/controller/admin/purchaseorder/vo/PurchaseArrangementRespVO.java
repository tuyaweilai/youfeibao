package cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 可选采购安排 Response VO（#51 T13）。
 *
 * <p>收购登记现场要能选出一个**有效的采购安排**（执行中且未过期的采购订单 + 其品类明细），
 * 也可以什么都不选（不选即「直接收购」，报表照常统计）。这里只读，供现场端选择器使用；
 * 订单的执行进度口径（计划 / 验收 / 入库 / 结算 / 未履行）由 #47 承载，本 VO 不掺和。
 *
 * <p>{@code usableAsPurchaseBasis} 恒为 true：只有可作为依据的订单才会出现在列表里，
 * 判定复用 {@code PurchaseOrderService#assertUsableAsPurchaseBasis} 的同一套规则（执行中 + 未过期）。
 */
@Schema(description = "管理后台 - 可选采购安排（采购订单 + 品类明细）")
@Data
public class PurchaseArrangementRespVO {

    @Schema(description = "采购订单编号", example = "5120")
    private Long orderId;

    @Schema(description = "采购订单号", example = "PO202612011000001234")
    private String orderNo;

    @Schema(description = "采购合同号快照（可空）", example = "PC20261201001")
    private String contractNo;

    @Schema(description = "交易对方名称快照", example = "张三")
    private String counterpartyName;

    @Schema(description = "执行场站编号（可空）", example = "3072")
    private Long stationId;

    @Schema(description = "执行场站名称快照（可空）", example = "朝阳回收站")
    private String stationName;

    @Schema(description = "执行开始日期", example = "2026-12-01")
    private LocalDate startDate;

    @Schema(description = "执行结束日期", example = "2026-12-31")
    private LocalDate endDate;

    @Schema(description = "品类明细（现场按品类选到具体明细）")
    private List<Item> items;

    /**
     * 采购订单的一条品类明细：收购单关联到明细，而不是只关联到订单。
     */
    @Schema(description = "采购订单品类明细")
    @Data
    public static class Item {

        @Schema(description = "采购订单明细编号", example = "6144")
        private Long itemId;

        @Schema(description = "品类编号", example = "2048")
        private Long goodsConfigId;

        @Schema(description = "品类名称快照", example = "废钢")
        private String categoryName;

        @Schema(description = "计量单位快照", example = "吨")
        private String unit;

        @Schema(description = "计划量", example = "100.0000")
        private BigDecimal planQuantity;

        @Schema(description = "参考单价（固定单价或价格表兜底价）", example = "2000.00")
        private BigDecimal unitPrice;

    }

}
