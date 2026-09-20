package cn.iocoder.yudao.module.icbc.controller.admin.stockops.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 盘点单明细 Response VO（#54 T16）。
 */
@Schema(description = "管理后台 - 盘点单明细 Response VO")
@Data
public class StockCheckItemRespVO {

    @Schema(description = "明细编号", example = "1")
    private Long id;

    @Schema(description = "盘点单编号", example = "2048")
    private Long checkId;

    @Schema(description = "品类编号", example = "1")
    private Long goodsConfigId;

    @Schema(description = "仓库编号", example = "1")
    private Long warehouseId;

    @Schema(description = "库位编号（0 = 未指定）", example = "1")
    private Long locationId;

    @Schema(description = "批次编号（0 = 未指定）", example = "1")
    private Long batchId;

    @Schema(description = "盘点实盘数", example = "4800")
    private BigDecimal actualQuantity;

    @Schema(description = "账面数量（过账时快照；待过账为空）", example = "5000")
    private BigDecimal bookQuantity;

    @Schema(description = "差额 = 实盘 − 账面（正数盘盈，负数盘亏；待过账为空）", example = "-200")
    private BigDecimal differenceQuantity;

    @Schema(description = "备注")
    private String remark;

}
