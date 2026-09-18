package cn.iocoder.yudao.module.logistics.controller.admin.temporaryorder.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 物流临时订单 Excel VO
 *
 * @author 芋道源码
 */
@Data
public class TemporaryOrderExcelVO {

    @ExcelProperty("主键ID")
    private Long id;

    @ExcelProperty("订单编号")
    private String orderNo;

    @ExcelProperty("运输任务ID")
    private Long taskId;

    @ExcelProperty("司机ID")
    private Long driverId;

    @ExcelProperty("司机姓名")
    private String driverName;

    @ExcelProperty("废料类型")
    private String wasteType;

    @ExcelProperty("废料名称")
    private String wasteName;

    @ExcelProperty("预计数量")
    private BigDecimal estimatedQuantity;

    @ExcelProperty("数量单位")
    private String quantityUnit;

    @ExcelProperty("取货地址")
    private String pickupLocation;

    @ExcelProperty("取货纬度")
    private BigDecimal pickupLatitude;

    @ExcelProperty("取货经度")
    private BigDecimal pickupLongitude;

    @ExcelProperty("取货时间")
    private LocalDateTime pickupTime;

    @ExcelProperty("产废方姓名")
    private String producerName;

    @ExcelProperty("产废方电话")
    private String producerPhone;

    @ExcelProperty("产废方身份证号")
    private String producerIdCard;

    @ExcelProperty("预估价值")
    private BigDecimal estimatedValue;

    @ExcelProperty("支付状态")
    private Integer paymentStatus;

    @ExcelProperty("支付状态名称")
    private String paymentStatusName;

    @ExcelProperty("支付金额")
    private BigDecimal paymentAmount;

    @ExcelProperty("支付时间")
    private LocalDateTime paymentTime;

    @ExcelProperty("支付方式")
    private String paymentMethod;

    @ExcelProperty("是否已转为正式订单")
    private Boolean convertedToFormal;

    @ExcelProperty("正式订单ID")
    private Long formalOrderId;

    @ExcelProperty("转换时间")
    private LocalDateTime conversionTime;

    @ExcelProperty("备注")
    private String remark;

    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

} 