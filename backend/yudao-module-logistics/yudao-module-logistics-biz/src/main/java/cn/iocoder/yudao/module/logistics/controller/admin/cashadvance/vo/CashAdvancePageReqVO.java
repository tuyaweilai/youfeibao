package cn.iocoder.yudao.module.logistics.controller.admin.cashadvance.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 物流现金代付记录分页查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class CashAdvancePageReqVO extends PageParam {

    @Schema(description = "运输任务ID", example = "1024")
    private Long taskId;

    @Schema(description = "订单ID", example = "1024")
    private Long orderId;

    @Schema(description = "司机ID", example = "1024")
    private Long driverId;

    @Schema(description = "司机姓名", example = "张三")
    private String driverName;

    @Schema(description = "支付地点", example = "北京市朝阳区")
    private String paymentLocation;

    @Schema(description = "支付方式", example = "CASH")
    private String paymentMethod;

    @Schema(description = "收款人姓名", example = "李四")
    private String payeeName;

    @Schema(description = "收款人电话", example = "13800138000")
    private String payeePhone;

    @Schema(description = "通知状态", example = "0")
    private Integer notifyStatus;

    @Schema(description = "对账状态", example = "0")
    private Integer reconcileStatus;

    @Schema(description = "支付时间范围-开始", example = "2024-01-01 00:00:00")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime beginPaymentTime;

    @Schema(description = "支付时间范围-结束", example = "2024-01-01 23:59:59")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime endPaymentTime;

    @Schema(description = "创建时间范围-开始", example = "2024-01-01 00:00:00")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime beginCreateTime;

    @Schema(description = "创建时间范围-结束", example = "2024-01-01 23:59:59")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime endCreateTime;

} 