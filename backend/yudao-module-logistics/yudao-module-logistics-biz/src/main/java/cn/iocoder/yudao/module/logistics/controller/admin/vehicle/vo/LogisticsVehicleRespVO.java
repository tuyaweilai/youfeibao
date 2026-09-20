package cn.iocoder.yudao.module.logistics.controller.admin.vehicle.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 车辆 Response VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class LogisticsVehicleRespVO extends LogisticsVehicleSaveReqVO {

    @ExcelProperty("编号")
    @Schema(description = "编号")
    private Long id;

    @ExcelProperty("车牌号")
    @Schema(description = "车牌号")
    private String plateNo;

    @ExcelProperty("车辆类型")
    @Schema(description = "车辆类型")
    private String vehicleType;

    @ExcelProperty("载重（吨）")
    @Schema(description = "载重（吨）")
    private java.math.BigDecimal capacityTon;

    @ExcelProperty("行驶证到期日")
    @Schema(description = "行驶证到期日")
    private java.time.LocalDate drivingLicenseExpiryDate;

    @ExcelProperty("保险到期日")
    @Schema(description = "保险到期日")
    private java.time.LocalDate insuranceExpiryDate;

    @ExcelProperty("状态")
    @Schema(description = "状态名（导出用）")
    private String statusName;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
