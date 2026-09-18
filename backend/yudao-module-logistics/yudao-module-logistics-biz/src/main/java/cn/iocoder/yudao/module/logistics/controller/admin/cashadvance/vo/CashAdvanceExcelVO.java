package cn.iocoder.yudao.module.logistics.controller.admin.cashadvance.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 物流现金代付记录 Excel VO
 *
 * @author 芋道源码
 */
@Data
public class CashAdvanceExcelVO {

    @ExcelProperty("主键ID")
    private Long id;

    @ExcelProperty("运输任务ID")
    private Long taskId;

    @ExcelProperty("订单ID")
    private Long orderId;

    @ExcelProperty("司机ID")
    private Long driverId;

    @ExcelProperty("司机姓名")
    private String driverName;

    @ExcelProperty("支付金额")
    private BigDecimal paymentAmount;

    @ExcelProperty("支付时间")
    private LocalDateTime paymentTime;

    @ExcelProperty("支付地点")
    private String paymentLocation;

    @ExcelProperty("支付方式")
    private String paymentMethod;

    @ExcelProperty("收款人姓名")
    private String payeeName;

    @ExcelProperty("收款人电话")
    private String payeePhone;

    @ExcelProperty("通知状态")
    private Integer notifyStatus;

    @ExcelProperty("通知状态名称")
    private String notifyStatusName;

    @ExcelProperty("通知时间")
    private LocalDateTime notifyTime;

    @ExcelProperty("对账状态")
    private Integer reconcileStatus;

    @ExcelProperty("对账状态名称")
    private String reconcileStatusName;

    @ExcelProperty("对账时间")
    private LocalDateTime reconcileTime;

    @ExcelProperty("对账备注")
    private String reconcileRemark;

    @ExcelProperty("对账操作员姓名")
    private String reconcileOperatorName;

    @ExcelProperty("备注")
    private String remark;

    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

} 