package cn.iocoder.yudao.module.logistics.controller.admin.transporttask.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 物流运输任务分页查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class TransportTaskPageReqVO extends PageParam {

    @Schema(description = "任务编号", example = "TT202401010001")
    private String taskNo;

    @Schema(description = "订单ID", example = "1024")
    private Long orderId;

    @Schema(description = "订单编号", example = "ORD202401010001")
    private String orderNo;

    @Schema(description = "所属企业ID", example = "1024")
    private Long enterpriseId;

    @Schema(description = "车辆ID", example = "1024")
    private Long vehicleId;

    @Schema(description = "司机ID", example = "1024")
    private Long driverId;

    @Schema(description = "任务状态", example = "0")
    private Integer taskStatus;

    @Schema(description = "是否异常", example = "false")
    private Boolean isAbnormal;

    @Schema(description = "是否临时任务", example = "false")
    private Boolean isTemporary;

    @Schema(description = "预计取货时间范围-开始", example = "2024-01-01 00:00:00")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime beginExpectedPickupTime;

    @Schema(description = "预计取货时间范围-结束", example = "2024-01-01 23:59:59")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime endExpectedPickupTime;

    @Schema(description = "预计送货时间范围-开始", example = "2024-01-01 00:00:00")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime beginExpectedDeliveryTime;

    @Schema(description = "预计送货时间范围-结束", example = "2024-01-01 23:59:59")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime endExpectedDeliveryTime;

    @Schema(description = "创建时间范围-开始", example = "2024-01-01 00:00:00")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime beginCreateTime;

    @Schema(description = "创建时间范围-结束", example = "2024-01-01 23:59:59")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime endCreateTime;

} 