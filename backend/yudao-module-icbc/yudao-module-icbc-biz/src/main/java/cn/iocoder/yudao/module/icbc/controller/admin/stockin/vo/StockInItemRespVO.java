package cn.iocoder.yudao.module.icbc.controller.admin.stockin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 入库单明细 Response VO（#52 T14）。
 */
@Schema(description = "管理后台 - 入库单明细 Response VO")
@Data
public class StockInItemRespVO {

    @Schema(description = "明细编号", example = "1")
    private Long id;

    @Schema(description = "入库单编号", example = "2048")
    private Long stockInId;

    @Schema(description = "仓库编号", example = "1")
    private Long warehouseId;

    @Schema(description = "库位编号（0 = 未指定）", example = "1")
    private Long locationId;

    @Schema(description = "批次编号（0 = 未指定）", example = "1")
    private Long batchId;

    @Schema(description = "入库数量", example = "5000")
    private BigDecimal quantity;

    @Schema(description = "备注")
    private String remark;

}
