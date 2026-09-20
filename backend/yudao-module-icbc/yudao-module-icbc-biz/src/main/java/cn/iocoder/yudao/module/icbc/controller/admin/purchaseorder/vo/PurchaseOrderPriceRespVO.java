package cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 管理后台 - 采购订单交货日价格表 Response VO（#46 T08）。
 */
@Schema(description = "管理后台 - 采购订单交货日价格表 Response VO")
@Data
public class PurchaseOrderPriceRespVO {

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "采购订单明细编号")
    private Long itemId;

    @Schema(description = "生效交货日")
    private LocalDate deliveryDate;

    @Schema(description = "该日生效单价")
    private BigDecimal unitPrice;

    @Schema(description = "备注")
    private String remark;

}
