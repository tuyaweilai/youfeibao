package cn.iocoder.yudao.module.erp.controller.admin.stock.vo.batch;

import cn.iocoder.yudao.framework.excel.core.annotations.DictFormat;
import cn.iocoder.yudao.framework.excel.core.convert.DictConvert;
import cn.iocoder.yudao.module.system.enums.DictTypeConstants;
import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - ERP 批次 Response VO")
@Data
@ExcelIgnoreUnannotated
public class ErpStockBatchRespVO {

    @Schema(description = "批次编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "11614")
    @ExcelProperty("批次编号")
    private Long id;

    @Schema(description = "批次号", requiredMode = Schema.RequiredMode.REQUIRED, example = "B20260920-01")
    @ExcelProperty("批次号")
    private String batchNo;

    @Schema(description = "品类编号", example = "1024")
    @ExcelProperty("品类编号")
    private Long goodsConfigId;

    @Schema(description = "入库时间")
    @ExcelProperty("入库时间")
    private LocalDateTime inTime;

    @Schema(description = "备注", example = "第一批")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "开启状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @ExcelProperty(value = "开启状态", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.COMMON_STATUS)
    private Integer status;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}
