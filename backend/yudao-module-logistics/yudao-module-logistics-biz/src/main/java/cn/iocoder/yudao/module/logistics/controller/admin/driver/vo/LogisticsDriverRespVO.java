package cn.iocoder.yudao.module.logistics.controller.admin.driver.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 司机 Response VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class LogisticsDriverRespVO extends LogisticsDriverSaveReqVO {

    @ExcelProperty("编号")
    @Schema(description = "编号")
    private Long id;

    @ExcelProperty("司机姓名")
    @Schema(description = "司机姓名")
    private String name;

    @ExcelProperty("手机号")
    @Schema(description = "手机号")
    private String mobile;

    @ExcelProperty("驾驶证号码")
    @Schema(description = "驾驶证号码")
    private String drivingLicenseNo;

    @ExcelProperty("准驾车型")
    @Schema(description = "准驾车型")
    private String drivingLicenseType;

    @ExcelProperty("驾驶证到期日")
    @Schema(description = "驾驶证到期日")
    private java.time.LocalDate drivingLicenseExpiryDate;

    @ExcelProperty("从业资格证号码")
    @Schema(description = "从业资格证号码")
    private String qualificationCertNo;

    @ExcelProperty("从业资格证到期日")
    @Schema(description = "从业资格证到期日")
    private java.time.LocalDate qualificationCertExpiryDate;

    @ExcelProperty("来源")
    @Schema(description = "来源名（导出用）")
    private String sourceName;

    @ExcelProperty("状态")
    @Schema(description = "状态名（导出用）")
    private String statusName;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
