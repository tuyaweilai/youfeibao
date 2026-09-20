package cn.iocoder.yudao.module.icbc.controller.admin.stockops.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 跨仓调拨单明细 Response VO（#54 T16）。
 */
@Schema(description = "管理后台 - 跨仓调拨单明细 Response VO")
@Data
public class StockMoveItemRespVO {

    @Schema(description = "明细编号", example = "1")
    private Long id;

    @Schema(description = "调拨单编号", example = "2048")
    private Long moveId;

    @Schema(description = "品类编号", example = "1")
    private Long goodsConfigId;

    @Schema(description = "源仓库编号", example = "1")
    private Long fromWarehouseId;

    @Schema(description = "源库位编号（0 = 未指定）", example = "1")
    private Long fromLocationId;

    @Schema(description = "源批次编号（0 = 未指定）", example = "1")
    private Long fromBatchId;

    @Schema(description = "目标仓库编号", example = "2")
    private Long toWarehouseId;

    @Schema(description = "目标库位编号（0 = 未指定）", example = "2")
    private Long toLocationId;

    @Schema(description = "目标批次编号（0 = 未指定）", example = "1")
    private Long toBatchId;

    @Schema(description = "调拨数量", example = "300")
    private BigDecimal quantity;

    @Schema(description = "备注")
    private String remark;

}
