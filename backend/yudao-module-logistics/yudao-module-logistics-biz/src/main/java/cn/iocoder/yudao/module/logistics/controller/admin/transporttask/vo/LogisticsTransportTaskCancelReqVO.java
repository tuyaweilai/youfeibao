package cn.iocoder.yudao.module.logistics.controller.admin.transporttask.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - 运输任务取消 Request VO")
@Data
public class LogisticsTransportTaskCancelReqVO {

    @Schema(description = "任务编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "任务编号不能为空")
    private Long id;

    @Schema(description = "取消原因（必填：为什么这趟活没跑）",
            requiredMode = Schema.RequiredMode.REQUIRED, example = "客户临时改期")
    @NotEmpty(message = "取消原因不能为空")
    private String cancelReason;

}
