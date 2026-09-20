package cn.iocoder.yudao.module.logistics.controller.admin.driver.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 司机资质信息 Excel VO
 *
 * @author 芋道源码
 */
@Data
@ExcelIgnoreUnannotated
public class DriverQualificationExcelVO {

    @ExcelProperty("主键ID")
    private Long id;

    @ExcelProperty("用户姓名")
    private String userName;

    @ExcelProperty("用户手机号")
    private String userMobile;

    @ExcelProperty("企业名称")
    private String enterpriseName;

    @ExcelProperty("司机编号")
    private String driverCode;

    @ExcelProperty("驾驶证号码")
    private String drivingLicenseNo;

    @ExcelProperty("驾驶证类型")
    private String drivingLicenseType;

    @ExcelProperty("驾驶证到期日期")
    private LocalDate drivingLicenseExpiryDate;

    @ExcelProperty("从业资格证号码")
    private String qualificationCertNo;

    @ExcelProperty("从业资格证到期日期")
    private LocalDate qualificationCertExpiryDate;

    @ExcelProperty("危险品运输资质证号")
    private String hazardousTransportCertNo;

    @ExcelProperty("危险品运输资质证到期日期")
    private LocalDate hazardousTransportCertExpiryDate;

    @ExcelProperty("准驾车型")
    private String vehicleTypePermitted;

    @ExcelProperty("驾龄(年)")
    private Integer yearsOfExperience;

    @ExcelProperty("司机状态")
    private Integer status;

    @ExcelProperty("入职日期")
    private LocalDate joinDate;

    @ExcelProperty("离职日期")
    private LocalDate leaveDate;

    @ExcelProperty("备注")
    private String remark;

    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

} 