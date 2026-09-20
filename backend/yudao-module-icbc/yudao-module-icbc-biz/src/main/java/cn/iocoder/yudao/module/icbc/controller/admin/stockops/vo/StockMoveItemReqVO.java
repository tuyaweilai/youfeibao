package cn.iocoder.yudao.module.icbc.controller.admin.stockops.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 跨仓调拨单明细 Request VO（#54 T16）。
 */
@Schema(description = "管理后台 - 跨仓调拨单明细 Request VO")
@Data
public class StockMoveItemReqVO {

    @Schema(description = "品类编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "品类不能为空")
    private Long goodsConfigId;

    @Schema(description = "源仓库编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "源仓库不能为空")
    private Long fromWarehouseId;

    @Schema(description = "源库位编号（可空，0 = 未指定）", example = "1")
    private Long fromLocationId;

    @Schema(description = "源批次编号（可空，0 = 未指定）", example = "1")
    private Long fromBatchId;

    @Schema(description = "目标仓库编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @NotNull(message = "目标仓库不能为空")
    private Long toWarehouseId;

    @Schema(description = "目标库位编号（可空，0 = 未指定）", example = "2")
    private Long toLocationId;

    @Schema(description = "目标批次编号（可空，0 = 未指定）", example = "1")
    private Long toBatchId;

    @Schema(description = "调拨数量（正数）", requiredMode = Schema.RequiredMode.REQUIRED, example = "300")
    @NotNull(message = "调拨数量不能为空")
    @DecimalMin(value = "0", inclusive = false, message = "调拨数量必须大于 0")
    private BigDecimal quantity;

    @Schema(description = "备注")
    private String remark;

}
