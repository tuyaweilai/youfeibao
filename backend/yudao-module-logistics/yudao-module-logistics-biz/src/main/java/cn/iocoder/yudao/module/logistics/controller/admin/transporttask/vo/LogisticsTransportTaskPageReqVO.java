package cn.iocoder.yudao.module.logistics.controller.admin.transporttask.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 运输任务分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class LogisticsTransportTaskPageReqVO extends PageParam {

    @Schema(description = "任务单号（模糊）", example = "TT2026")
    private String taskNo;

    @Schema(description = "车牌号（模糊）", example = "浙A")
    private String plateNo;

    @Schema(description = "车辆编号", example = "1")
    private Long vehicleId;

    @Schema(description = "司机编号", example = "1")
    private Long driverId;

    @Schema(description = "任务状态：0-待分配，1-已分配，2-已接单，3-执行中，4-已完成，5-已取消", example = "1")
    private Integer status;

    @Schema(description = "提货点地址（模糊）")
    private String pickupAddress;

    @Schema(description = "时间窗开始（范围查询起点）")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] expectedStartTime;

    public LocalDateTime getExpectedStartTimeBegin() {
        return expectedStartTime != null && expectedStartTime.length > 0 ? expectedStartTime[0] : null;
    }

    public LocalDateTime getExpectedStartTimeEnd() {
        return expectedStartTime != null && expectedStartTime.length > 1 ? expectedStartTime[1] : null;
    }

}
