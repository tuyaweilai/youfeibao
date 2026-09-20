package cn.iocoder.yudao.module.icbc.controller.admin.stockops.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 非销售出库单明细 Request VO（#54 T16）。
 */
@Schema(description = "管理后台 - 非销售出库单明细 Request VO")
@Data
public class StockOutItemReqVO {

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

    @Schema(description = "出库数量（正数）", requiredMode = Schema.RequiredMode.REQUIRED, example = "120")
    @NotNull(message = "出库数量不能为空")
    @DecimalMin(value = "0", inclusive = false, message = "出库数量必须大于 0")
    private BigDecimal quantity;

    @Schema(description = "备注")
    private String remark;

}
