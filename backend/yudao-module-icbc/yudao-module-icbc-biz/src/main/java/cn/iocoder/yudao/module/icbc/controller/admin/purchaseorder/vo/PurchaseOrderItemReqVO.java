package cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

/**
 * 管理后台 - 采购订单明细 Request VO（#46 T08）。
 *
 * <p>一条明细一个品类；一条明细可分多次收货。定价方式见
 * {@code PurchaseOrderPriceModeEnum}：固定单价直接用 {@link #unitPrice}；
 * 按交货日价格表时用 {@link #prices}，没覆盖到的交货日回退到 {@link #unitPrice}。
 */
@Schema(description = "管理后台 - 采购订单明细 Request VO")
@Data
public class PurchaseOrderItemReqVO {

    @Schema(description = "品类编号（icbc_goods_config）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "采购订单明细的品类不能为空")
    private Long goodsConfigId;

    @Schema(description = "计划量", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "采购订单明细的计划量不能为空")
    private BigDecimal quantity;

    @Schema(description = "定价方式：1-固定单价，2-按交货日价格表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "采购订单明细的定价方式不能为空")
    private Integer priceMode;

    @Schema(description = "参考单价（固定单价方式的成交价；价格表方式的兜底价）")
    private BigDecimal unitPrice;

    @Schema(description = "交货日价格表（按交货日价格表定价时使用）")
    private List<PurchaseOrderPriceReqVO> prices;

    @Schema(description = "备注（等级 / 规格等）")
    private String remark;

}
