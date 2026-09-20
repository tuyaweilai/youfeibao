package cn.iocoder.yudao.module.logistics.controller.admin.transporttask.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - 运输任务派车 Request VO")
@Data
public class LogisticsTransportTaskAssignReqVO {

    @Schema(description = "任务编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "任务编号不能为空")
    private Long id;

    @Schema(description = "车辆编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "车辆编号不能为空")
    private Long vehicleId;

    @Schema(description = "司机编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "司机编号不能为空")
    private Long driverId;

}
