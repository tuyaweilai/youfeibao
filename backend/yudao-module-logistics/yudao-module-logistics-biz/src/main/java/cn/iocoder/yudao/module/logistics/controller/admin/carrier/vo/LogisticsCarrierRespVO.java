package cn.iocoder.yudao.module.logistics.controller.admin.carrier.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 承运商 Response VO（同时用作导出 Excel 的行）")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class LogisticsCarrierRespVO extends LogisticsCarrierSaveReqVO {

    @ExcelProperty("编号")
    @Schema(description = "编号")
    private Long id;

    @ExcelProperty("承运商名称")
    @Schema(description = "承运商名称")
    private String name;

    @ExcelProperty("联系人")
    @Schema(description = "联系人")
    private String contactName;

    @ExcelProperty("联系电话")
    @Schema(description = "联系电话")
    private String contactMobile;

    @ExcelProperty("状态")
    @Schema(description = "状态名（导出用）")
    private String statusName;

    @ExcelProperty("备注")
    @Schema(description = "备注")
    private String remark;

    @ExcelProperty("创建时间")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
