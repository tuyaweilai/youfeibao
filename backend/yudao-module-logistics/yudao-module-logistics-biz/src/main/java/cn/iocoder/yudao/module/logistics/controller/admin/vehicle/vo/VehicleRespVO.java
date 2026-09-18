package cn.iocoder.yudao.module.logistics.controller.admin.vehicle.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 车辆信息 Response VO")
@Data
public class VehicleRespVO {

    @Schema(description = "车辆ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "所属企业ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long enterpriseId;

    @Schema(description = "企业名称", example = "XX物流公司")
    private String enterpriseName;

    @Schema(description = "车牌号", requiredMode = Schema.RequiredMode.REQUIRED, example = "京A12345")
    private String plateNumber;

    @Schema(description = "车辆类型", example = "厢式货车")
    private String vehicleType;

    @Schema(description = "载重能力(kg)", example = "5000.00")
    private BigDecimal capacityKg;

    @Schema(description = "GPS设备ID", example = "GPS001")
    private String gpsDeviceId;

    @Schema(description = "车辆状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    private Integer status;

    @Schema(description = "车辆状态名称", example = "可用")
    private String statusName;

    @Schema(description = "行驶证到期日期", example = "2025-12-31")
    private LocalDate licenseExpiryDate;

    @Schema(description = "保险到期日期", example = "2025-12-31")
    private LocalDate insuranceExpiryDate;

    @Schema(description = "上次维护日期", example = "2024-01-01")
    private LocalDate maintenanceDate;

    @Schema(description = "车辆照片URLs", example = "[\"http://example.com/photo1.jpg\", \"http://example.com/photo2.jpg\"]")
    private List<String> vehiclePhotos;

    @Schema(description = "行驶证照片URLs", example = "[\"http://example.com/license1.jpg\", \"http://example.com/license2.jpg\"]")
    private List<String> licensePhotos;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED, example = "2024-01-01 00:00:00")
    private LocalDateTime createTime;

    @Schema(description = "更新时间", requiredMode = Schema.RequiredMode.REQUIRED, example = "2024-01-01 00:00:00")
    private LocalDateTime updateTime;

} 