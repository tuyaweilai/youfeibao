package cn.iocoder.yudao.module.logistics.controller.admin.transporttask.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 物流运输任务 Excel VO
 *
 * @author 芋道源码
 */
@Data
public class TransportTaskExcelVO {

    @ExcelProperty("主键ID")
    private Long id;

    @ExcelProperty("任务编号")
    private String taskNo;

    @ExcelProperty("订单ID")
    private Long orderId;

    @ExcelProperty("订单编号")
    private String orderNo;

    @ExcelProperty("所属企业ID")
    private Long enterpriseId;

    @ExcelProperty("车辆ID")
    private Long vehicleId;

    @ExcelProperty("司机ID")
    private Long driverId;

    @ExcelProperty("分配时间")
    private LocalDateTime assignTime;

    @ExcelProperty("接受时间")
    private LocalDateTime acceptTime;

    @ExcelProperty("取货企业ID")
    private Long pickupEnterpriseId;

    @ExcelProperty("取货地址")
    private String pickupAddress;

    @ExcelProperty("取货联系人姓名")
    private String pickupContactName;

    @ExcelProperty("取货联系人电话")
    private String pickupContactPhone;

    @ExcelProperty("预计取货时间")
    private LocalDateTime expectedPickupTime;

    @ExcelProperty("实际取货时间")
    private LocalDateTime actualPickupTime;

    @ExcelProperty("送货企业ID")
    private Long deliveryEnterpriseId;

    @ExcelProperty("送货地址")
    private String deliveryAddress;

    @ExcelProperty("送货联系人姓名")
    private String deliveryContactName;

    @ExcelProperty("送货联系人电话")
    private String deliveryContactPhone;

    @ExcelProperty("预计送货时间")
    private LocalDateTime expectedDeliveryTime;

    @ExcelProperty("实际送货时间")
    private LocalDateTime actualDeliveryTime;

    @ExcelProperty("废料代码")
    private String wasteCode;

    @ExcelProperty("废料名称")
    private String wasteName;

    @ExcelProperty("预计数量")
    private BigDecimal estimatedQuantity;

    @ExcelProperty("实际数量")
    private BigDecimal actualQuantity;

    @ExcelProperty("数量单位")
    private String quantityUnit;

    @ExcelProperty("任务状态")
    private Integer taskStatus;

    @ExcelProperty("任务状态名称")
    private String taskStatusName;

    @ExcelProperty("当前位置")
    private String currentLocation;

    @ExcelProperty("当前纬度")
    private BigDecimal currentLatitude;

    @ExcelProperty("当前经度")
    private BigDecimal currentLongitude;

    @ExcelProperty("是否异常")
    private Boolean isAbnormal;

    @ExcelProperty("异常类型")
    private Integer abnormalType;

    @ExcelProperty("异常类型名称")
    private String abnormalTypeName;

    @ExcelProperty("异常原因")
    private String abnormalReason;

    @ExcelProperty("是否临时任务")
    private Boolean isTemporary;

    @ExcelProperty("备注")
    private String remark;

    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

} 