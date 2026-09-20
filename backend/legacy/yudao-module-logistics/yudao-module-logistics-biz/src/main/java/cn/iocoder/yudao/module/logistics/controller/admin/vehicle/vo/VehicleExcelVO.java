package cn.iocoder.yudao.module.logistics.controller.admin.vehicle.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 车辆信息 Excel VO
 *
 * @author 芋道源码
 */
@Data
@ExcelIgnoreUnannotated
public class VehicleExcelVO {

    @ExcelProperty("车辆ID")
    private Long id;

    @ExcelProperty("车牌号")
    private String plateNumber;

    @ExcelProperty("车辆类型")
    private String vehicleType;

    @ExcelProperty("载重能力(kg)")
    private BigDecimal capacityKg;

    @ExcelProperty("GPS设备ID")
    private String gpsDeviceId;

    @ExcelProperty("车辆状态")
    private String statusName;

    @ExcelProperty("行驶证到期日期")
    private LocalDate licenseExpiryDate;

    @ExcelProperty("保险到期日期")
    private LocalDate insuranceExpiryDate;

    @ExcelProperty("上次维护日期")
    private LocalDate maintenanceDate;

    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

} 