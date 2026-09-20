package cn.iocoder.yudao.module.logistics.controller.admin.freight.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 运输费用（内部成本）Response VO")
@Data
@ExcelIgnoreUnannotated
public class LogisticsTransportCostRespVO {

    @ExcelProperty("编号")
    @Schema(description = "编号")
    private Long id;

    @Schema(description = "运输任务编号")
    private Long taskId;

    @ExcelProperty("任务单号")
    @Schema(description = "运输任务单号")
    private String taskNo;

    @ExcelProperty("费用类型")
    @Schema(description = "费用类型名")
    private String costTypeName;

    @Schema(description = "费用类型：1-路桥费，2-燃油费，3-其他")
    private Integer costType;

    @ExcelProperty("费用名称")
    @Schema(description = "费用名称")
    private String name;

    @ExcelProperty("金额")
    @Schema(description = "金额")
    private BigDecimal amount;

    @ExcelProperty("承担方")
    @Schema(description = "承担方名")
    private String bearerName;

    @Schema(description = "承担方：1-承运商承担，2-本企业承担")
    private Integer bearer;

    @ExcelProperty("发生日期")
    @Schema(description = "发生日期")
    private LocalDate occurDate;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
