package cn.iocoder.yudao.module.icbc.controller.admin.stockops.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 非销售出库单明细 Response VO（#54 T16）。
 */
@Schema(description = "管理后台 - 非销售出库单明细 Response VO")
@Data
public class StockOutItemRespVO {

    @Schema(description = "明细编号", example = "1")
    private Long id;

    @Schema(description = "出库单编号", example = "2048")
    private Long stockOutId;

    @Schema(description = "品类编号", example = "1")
    private Long goodsConfigId;

    @Schema(description = "仓库编号", example = "1")
    private Long warehouseId;

    @Schema(description = "库位编号（0 = 未指定）", example = "1")
    private Long locationId;

    @Schema(description = "批次编号（0 = 未指定）", example = "1")
    private Long batchId;

    @Schema(description = "出库数量", example = "120")
    private BigDecimal quantity;

    @Schema(description = "备注")
    private String remark;

}
