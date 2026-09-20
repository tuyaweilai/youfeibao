package cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stock;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - ERP 库存 Response VO")
@Data
@ExcelIgnoreUnannotated
public class ErpStockRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "17086")
    @ExcelProperty("编号")
    private Long id;

    @Schema(description = "品类编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "19614")
    private Long goodsConfigId;

    @Schema(description = "仓库编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2802")
    private Long warehouseId;

    @Schema(description = "库存数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "21935")
    @ExcelProperty("库存数量")
    private BigDecimal count;

    // ========== 品类信息 ==========

    @Schema(description = "品类名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "苹果")
    @ExcelProperty("品类名称")
    private String productName;

    @Schema(description = "品类分类", requiredMode = Schema.RequiredMode.REQUIRED, example = "水果")
    @ExcelProperty("品类分类")
    private String categoryName;

    @Schema(description = "单位", requiredMode = Schema.RequiredMode.REQUIRED, example = "个")
    @ExcelProperty("单位")
    private String unitName;

    // ========== 仓库信息 ==========

    @Schema(description = "仓库名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "李四")
    @ExcelProperty("仓库名称")
    private String warehouseName;

}