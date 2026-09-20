package cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 管理后台 - 采购订单成交记录 Response VO（#46 T08）。
 */
@Schema(description = "管理后台 - 采购订单成交记录 Response VO")
@Data
public class PurchaseOrderDealRespVO {

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "采购订单编号")
    private Long orderId;

    @Schema(description = "采购订单明细编号")
    private Long itemId;

    @Schema(description = "品类名称快照")
    private String categoryName;

    @Schema(description = "成交单号")
    private String dealNo;

    @Schema(description = "成交时间")
    private LocalDateTime dealTime;

    @Schema(description = "交货日")
    private LocalDate deliveryDate;

    @Schema(description = "成交数量")
    private BigDecimal quantity;

    @Schema(description = "成交单价（价格快照）")
    private BigDecimal unitPrice;

    @Schema(description = "参考单价")
    private BigDecimal referenceUnitPrice;

    @Schema(description = "成交价是否做过调整")
    private Boolean priceAdjusted;

    @Schema(description = "调整原因")
    private String adjustReason;

    @Schema(description = "关联业务来源类型")
    private String sourceType;

    @Schema(description = "关联业务来源编号")
    private Long sourceId;

    @Schema(description = "关联业务来源单号")
    private String sourceNo;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
