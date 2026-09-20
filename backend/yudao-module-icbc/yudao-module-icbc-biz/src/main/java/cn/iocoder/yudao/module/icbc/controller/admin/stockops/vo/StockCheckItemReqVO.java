package cn.iocoder.yudao.module.icbc.controller.admin.stockops.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 盘点单明细 Request VO（#54 T16）。
 */
@Schema(description = "管理后台 - 盘点单明细 Request VO")
@Data
public class StockCheckItemReqVO {

    @Schema(description = "品类编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "品类不能为空")
    private Long goodsConfigId;

    @Schema(description = "仓库编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "仓库不能为空")
    private Long warehouseId;

    @Schema(description = "库位编号（可空，0 = 未指定）", example = "1")
    private Long locationId;

    @Schema(description = "批次编号（可空，0 = 未指定）", example = "1")
    private Long batchId;

    @Schema(description = "盘点实盘数（不小于 0）", requiredMode = Schema.RequiredMode.REQUIRED, example = "4800")
    @NotNull(message = "盘点实盘数不能为空")
    private BigDecimal actualQuantity;

    @Schema(description = "备注")
    private String remark;

}
