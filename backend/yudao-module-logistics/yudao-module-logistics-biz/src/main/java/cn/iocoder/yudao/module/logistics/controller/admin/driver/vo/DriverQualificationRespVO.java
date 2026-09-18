package cn.iocoder.yudao.module.logistics.controller.admin.driver.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 司机资质信息 Response VO")
@Data
public class DriverQualificationRespVO {

    @Schema(description = "主键ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "关联的用户ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long userId;

    @Schema(description = "用户姓名", example = "张三")
    private String userName;

    @Schema(description = "用户手机号", example = "13800138000")
    private String userMobile;

    @Schema(description = "所属企业ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long enterpriseId;

    @Schema(description = "企业名称", example = "XX物流公司")
    private String enterpriseName;

    @Schema(description = "司机编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "D001")
    private String driverCode;

    @Schema(description = "驾驶证号码", requiredMode = Schema.RequiredMode.REQUIRED, example = "310101199001010001")
    private String drivingLicenseNo;

    @Schema(description = "驾驶证类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "C1")
    private String drivingLicenseType;

    @Schema(description = "驾驶证到期日期", requiredMode = Schema.RequiredMode.REQUIRED, example = "2030-12-31")
    private LocalDate drivingLicenseExpiryDate;

    @Schema(description = "从业资格证号码", example = "QC001")
    private String qualificationCertNo;

    @Schema(description = "从业资格证到期日期", example = "2030-12-31")
    private LocalDate qualificationCertExpiryDate;

    @Schema(description = "危险品运输资质证号", example = "HZ001")
    private String hazardousTransportCertNo;

    @Schema(description = "危险品运输资质证到期日期", example = "2030-12-31")
    private LocalDate hazardousTransportCertExpiryDate;

    @Schema(description = "准驾车型", example = "C1,B2")
    private String vehicleTypePermitted;

    @Schema(description = "驾龄(年)", example = "5")
    private Integer yearsOfExperience;

    @Schema(description = "司机状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    private Integer status;

    @Schema(description = "入职日期", example = "2024-01-01")
    private LocalDate joinDate;

    @Schema(description = "离职日期", example = "2024-12-31")
    private LocalDate leaveDate;

    @Schema(description = "驾驶证照片URLs", example = "[\"http://example.com/license1.jpg\", \"http://example.com/license2.jpg\"]")
    private List<String> driverLicensePhotos;

    @Schema(description = "从业资格证照片URLs", example = "[\"http://example.com/cert1.jpg\", \"http://example.com/cert2.jpg\"]")
    private List<String> qualificationCertPhotos;

    @Schema(description = "危险品运输资质证照片URLs", example = "[\"http://example.com/hazard1.jpg\", \"http://example.com/hazard2.jpg\"]")
    private List<String> hazardousCertPhotos;

    @Schema(description = "最近一次培训日期", example = "2024-01-01")
    private LocalDate lastTrainingDate;

    @Schema(description = "下次培训日期", example = "2024-06-01")
    private LocalDate nextTrainingDate;

    @Schema(description = "备注", example = "经验丰富的司机")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED, example = "2024-01-01 00:00:00")
    private LocalDateTime createTime;

    @Schema(description = "更新时间", requiredMode = Schema.RequiredMode.REQUIRED, example = "2024-01-01 00:00:00")
    private LocalDateTime updateTime;

} 