package cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 管理后台 - 采购订单明细 Response VO（#46 T08）。
 *
 * <p>{@link #receivedQuantity} / {@link #remainingQuantity} 都是**推导值**：已收量 = 该明细全部
 * 成交记录的数量之和，不落字段（一次成交改数量不会漂移）。
 */
@Schema(description = "管理后台 - 采购订单明细 Response VO")
@Data
public class PurchaseOrderItemRespVO {

    @Schema(description = "主键（分次收货引用它）")
    private Long id;

    @Schema(description = "采购订单编号")
    private Long orderId;

    @Schema(description = "品类编号")
    private Long goodsConfigId;

    @Schema(description = "品类名称快照")
    private String categoryName;

    @Schema(description = "计量单位快照")
    private String unit;

    @Schema(description = "计划量")
    private BigDecimal quantity;

    @Schema(description = "定价方式：1-固定单价，2-按交货日价格表")
    private Integer priceMode;

    @Schema(description = "定价方式名")
    private String priceModeName;

    @Schema(description = "参考单价")
    private BigDecimal unitPrice;

    @Schema(description = "计划金额")
    private BigDecimal amount;

    @Schema(description = "已收量（由成交记录汇总推导）")
    private BigDecimal receivedQuantity;

    @Schema(description = "未收量（计划量 − 已收量，可为负表示超收）")
    private BigDecimal remainingQuantity;

    @Schema(description = "成交笔数（一条明细可分多次收货）")
    private Integer dealCount;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "交货日价格表")
    private List<PurchaseOrderPriceRespVO> prices;

}
