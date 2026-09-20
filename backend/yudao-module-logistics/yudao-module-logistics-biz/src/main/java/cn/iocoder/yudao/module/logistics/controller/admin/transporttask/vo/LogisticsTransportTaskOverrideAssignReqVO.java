package cn.iocoder.yudao.module.logistics.controller.admin.transporttask.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

/**
 * 授权放行派车（V3 #70）：证件过期时由管理员带着原因放行。
 *
 * <p>**只放行「证件过期」这一类软门禁**；车辆维修中、司机离职属于硬门禁，这条路也拦。
 */
@Schema(description = "管理后台 - 运输任务授权放行派车 Request VO")
@Data
public class LogisticsTransportTaskOverrideAssignReqVO {

    @Schema(description = "任务编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "任务编号不能为空")
    private Long id;

    @Schema(description = "车辆编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "车辆编号不能为空")
    private Long vehicleId;

    @Schema(description = "司机编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "司机编号不能为空")
    private Long driverId;

    @Schema(description = "授权放行原因（必填，留痕：为什么要带着过期证件出车）",
            requiredMode = Schema.RequiredMode.REQUIRED, example = "行驶证正在换证，已在办理中，客户催货")
    @NotEmpty(message = "授权放行原因不能为空")
    private String overrideReason;

}
