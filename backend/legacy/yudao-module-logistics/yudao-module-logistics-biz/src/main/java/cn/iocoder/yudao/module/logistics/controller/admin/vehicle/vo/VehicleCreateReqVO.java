package cn.iocoder.yudao.module.logistics.controller.admin.vehicle.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Schema(description = "管理后台 - 车辆信息创建 Request VO")
@Data
public class VehicleCreateReqVO {

    @Schema(description = "所属企业ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "所属企业ID不能为空")
    private Long enterpriseId;

    @Schema(description = "车牌号", requiredMode = Schema.RequiredMode.REQUIRED, example = "京A12345")
    @NotBlank(message = "车牌号不能为空")
    @Size(max = 20, message = "车牌号长度不能超过 20 个字符")
    private String plateNumber;

    @Schema(description = "车辆类型", example = "厢式货车")
    @Size(max = 50, message = "车辆类型长度不能超过 50 个字符")
    private String vehicleType;

    @Schema(description = "载重能力(kg)", example = "5000.00")
    @DecimalMin(value = "0", message = "载重能力必须大于等于0")
    @Digits(integer = 8, fraction = 2, message = "载重能力格式不正确")
    private BigDecimal capacityKg;

    @Schema(description = "GPS设备ID", example = "GPS001")
    @Size(max = 100, message = "GPS设备ID长度不能超过 100 个字符")
    private String gpsDeviceId;

    @Schema(description = "车辆状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "车辆状态不能为空")
    @Min(value = 0, message = "车辆状态值不能小于0")
    @Max(value = 2, message = "车辆状态值不能大于2")
    private Integer status;

    @Schema(description = "行驶证到期日期", example = "2025-12-31")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate licenseExpiryDate;

    @Schema(description = "保险到期日期", example = "2025-12-31")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate insuranceExpiryDate;

    @Schema(description = "上次维护日期", example = "2024-01-01")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate maintenanceDate;

    @Schema(description = "车辆照片URLs", example = "[\"http://example.com/photo1.jpg\", \"http://example.com/photo2.jpg\"]")
    private List<String> vehiclePhotos;

    @Schema(description = "行驶证照片URLs", example = "[\"http://example.com/license1.jpg\", \"http://example.com/license2.jpg\"]")
    private List<String> licensePhotos;

} 