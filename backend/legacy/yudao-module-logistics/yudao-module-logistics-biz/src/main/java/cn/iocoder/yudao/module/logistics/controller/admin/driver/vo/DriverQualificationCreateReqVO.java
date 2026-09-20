package cn.iocoder.yudao.module.logistics.controller.admin.driver.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Schema(description = "管理后台 - 司机资质信息创建 Request VO")
@Data
public class DriverQualificationCreateReqVO {

    @Schema(description = "关联的用户ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "关联的用户ID不能为空")
    private Long userId;

    @Schema(description = "所属企业ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "所属企业ID不能为空")
    private Long enterpriseId;

    @Schema(description = "司机编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "D001")
    @NotBlank(message = "司机编号不能为空")
    @Size(max = 32, message = "司机编号长度不能超过 32 个字符")
    private String driverCode;

    @Schema(description = "驾驶证号码", requiredMode = Schema.RequiredMode.REQUIRED, example = "310101199001010001")
    @NotBlank(message = "驾驶证号码不能为空")
    @Size(max = 50, message = "驾驶证号码长度不能超过 50 个字符")
    private String drivingLicenseNo;

    @Schema(description = "驾驶证类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "C1")
    @NotBlank(message = "驾驶证类型不能为空")
    @Size(max = 20, message = "驾驶证类型长度不能超过 20 个字符")
    private String drivingLicenseType;

    @Schema(description = "驾驶证到期日期", requiredMode = Schema.RequiredMode.REQUIRED, example = "2030-12-31")
    @NotNull(message = "驾驶证到期日期不能为空")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate drivingLicenseExpiryDate;

    @Schema(description = "从业资格证号码", example = "QC001")
    @Size(max = 50, message = "从业资格证号码长度不能超过 50 个字符")
    private String qualificationCertNo;

    @Schema(description = "从业资格证到期日期", example = "2030-12-31")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate qualificationCertExpiryDate;

    @Schema(description = "危险品运输资质证号", example = "HZ001")
    @Size(max = 50, message = "危险品运输资质证号长度不能超过 50 个字符")
    private String hazardousTransportCertNo;

    @Schema(description = "危险品运输资质证到期日期", example = "2030-12-31")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate hazardousTransportCertExpiryDate;

    @Schema(description = "准驾车型", example = "C1,B2")
    @Size(max = 100, message = "准驾车型长度不能超过 100 个字符")
    private String vehicleTypePermitted;

    @Schema(description = "驾龄(年)", example = "5")
    @Min(value = 0, message = "驾龄不能小于0")
    @Max(value = 50, message = "驾龄不能大于50")
    private Integer yearsOfExperience;

    @Schema(description = "司机状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "司机状态不能为空")
    @Min(value = 0, message = "司机状态值不能小于0")
    @Max(value = 2, message = "司机状态值不能大于2")
    private Integer status;

    @Schema(description = "入职日期", example = "2024-01-01")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate joinDate;

    @Schema(description = "离职日期", example = "2024-12-31")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate leaveDate;

    @Schema(description = "驾驶证照片URLs", example = "[\"http://example.com/license1.jpg\", \"http://example.com/license2.jpg\"]")
    private List<String> driverLicensePhotos;

    @Schema(description = "从业资格证照片URLs", example = "[\"http://example.com/cert1.jpg\", \"http://example.com/cert2.jpg\"]")
    private List<String> qualificationCertPhotos;

    @Schema(description = "危险品运输资质证照片URLs", example = "[\"http://example.com/hazard1.jpg\", \"http://example.com/hazard2.jpg\"]")
    private List<String> hazardousCertPhotos;

    @Schema(description = "最近一次培训日期", example = "2024-01-01")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate lastTrainingDate;

    @Schema(description = "下次培训日期", example = "2024-06-01")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate nextTrainingDate;

    @Schema(description = "备注", example = "经验丰富的司机")
    @Size(max = 500, message = "备注长度不能超过 500 个字符")
    private String remark;

} 