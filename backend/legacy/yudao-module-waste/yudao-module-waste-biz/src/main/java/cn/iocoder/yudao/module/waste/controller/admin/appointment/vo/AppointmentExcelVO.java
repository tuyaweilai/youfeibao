package cn.iocoder.yudao.module.waste.controller.admin.appointment.vo;

import cn.iocoder.yudao.framework.excel.core.annotations.DictFormat;
import cn.iocoder.yudao.framework.excel.core.convert.DictConvert;
import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 危废转移预约 Excel VO
 *
 * @author 芋道源码
 */
@Data
@ExcelIgnoreUnannotated
public class AppointmentExcelVO {

    @ExcelProperty("预约ID")
    private Long id;

    @ExcelProperty("预约单号")
    private String appointmentNo;

    @ExcelProperty("产废企业名称")
    private String producerEnterpriseName;

    @ExcelProperty("产废企业联系人")
    private String producerContactName;

    @ExcelProperty("产废企业联系电话")
    private String producerContactPhone;

    @ExcelProperty("回收企业名称")
    private String recyclerEnterpriseName;

    @ExcelProperty(value = "分配方式", converter = DictConvert.class)
    @DictFormat("waste_assignment_type")
    private Integer assignmentType;

    @ExcelProperty("危险废物代码")
    private String wasteCode;

    @ExcelProperty("危险废物名称")
    private String wasteName;

    @ExcelProperty("废物类别")
    private String wasteCategory;

    @ExcelProperty("预估数量")
    private BigDecimal estimatedQuantity;

    @ExcelProperty("数量单位")
    private String quantityUnit;

    @ExcelProperty("取货地址")
    private String pickupAddress;

    @ExcelProperty("期望取货时间")
    private LocalDateTime expectedPickupTime;

    @ExcelProperty(value = "预约状态", converter = DictConvert.class)
    @DictFormat("waste_appointment_status")
    private Integer appointmentStatus;

    @ExcelProperty(value = "业务模式", converter = DictConvert.class)
    @DictFormat("waste_business_mode")
    private Integer businessMode;

    @ExcelProperty(value = "是否紧急", converter = DictConvert.class)
    @DictFormat("common_boolean")
    private Boolean isUrgent;

    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

} 