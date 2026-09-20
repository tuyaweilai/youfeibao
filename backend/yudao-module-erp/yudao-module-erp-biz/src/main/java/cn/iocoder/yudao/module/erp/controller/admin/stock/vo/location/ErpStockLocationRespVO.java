package cn.iocoder.yudao.module.erp.controller.admin.stock.vo.location;

import cn.iocoder.yudao.framework.excel.core.annotations.DictFormat;
import cn.iocoder.yudao.framework.excel.core.convert.DictConvert;
import cn.iocoder.yudao.module.system.enums.DictTypeConstants;
import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - ERP 库位 Response VO")
@Data
@ExcelIgnoreUnannotated
public class ErpStockLocationRespVO {

    @Schema(description = "库位编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "11614")
    @ExcelProperty("库位编号")
    private Long id;

    @Schema(description = "仓库编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("仓库编号")
    private Long warehouseId;

    @Schema(description = "库位名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "A 区 1 号堆")
    @ExcelProperty("库位名称")
    private String name;

    @Schema(description = "排序", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @ExcelProperty("排序")
    private Long sort;

    @Schema(description = "备注", example = "靠墙")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "开启状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @ExcelProperty(value = "开启状态", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.COMMON_STATUS)
    private Integer status;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

    @Schema(description = "仓库名称", example = "一号仓")
    @ExcelProperty("仓库名称")
    private String warehouseName;

}
