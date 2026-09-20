package cn.iocoder.yudao.module.logistics.controller.admin.transporttask.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

/**
 * 改派运输任务（V4 #71）：换车换人**保留承接关系**，不覆盖原记录。
 *
 * <p>改派原因必填：为什么要中途换车换人，事后要能解释（与取消、授权放行同一口径）。
 */
@Schema(description = "管理后台 - 运输任务改派 Request VO")
@Data
public class LogisticsTransportTaskReassignReqVO {

    @Schema(description = "任务编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "任务编号不能为空")
    private Long id;

    @Schema(description = "新车辆编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "新车辆编号不能为空")
    private Long vehicleId;

    @Schema(description = "新司机编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "新司机编号不能为空")
    private Long driverId;

    @Schema(description = "改派原因（必填）", requiredMode = Schema.RequiredMode.REQUIRED, example = "原车故障，货转备用车")
    @NotEmpty(message = "改派原因不能为空")
    private String reason;

}
