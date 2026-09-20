package cn.iocoder.yudao.module.logistics.controller.admin.transporttask.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 运输任务改派承接记录 Response VO（V4 #71）。
 *
 * <p>前后两份快照都留着：原车原人是谁、换成谁、为什么。页面按改派时间正序列出来。
 */
@Schema(description = "管理后台 - 运输任务改派承接记录 Response VO")
@Data
public class LogisticsTransportTaskReassignRespVO {

    @Schema(description = "记录编号")
    private Long id;

    @Schema(description = "运输任务编号")
    private Long taskId;

    @Schema(description = "原车辆编号")
    private Long prevVehicleId;

    @Schema(description = "原车牌号快照")
    private String prevPlateNo;

    @Schema(description = "原司机编号")
    private Long prevDriverId;

    @Schema(description = "原司机姓名快照")
    private String prevDriverName;

    @Schema(description = "原司机手机号快照")
    private String prevDriverMobile;

    @Schema(description = "新车辆编号")
    private Long vehicleId;

    @Schema(description = "新车牌号快照")
    private String plateNo;

    @Schema(description = "新司机编号")
    private Long driverId;

    @Schema(description = "新司机姓名快照")
    private String driverName;

    @Schema(description = "新司机手机号快照")
    private String driverMobile;

    @Schema(description = "改派原因")
    private String reason;

    @Schema(description = "改派人（系统用户编号）")
    private Long operatorId;

    @Schema(description = "改派人姓名快照")
    private String operatorName;

    @Schema(description = "改派时间")
    private LocalDateTime reassignTime;

}
