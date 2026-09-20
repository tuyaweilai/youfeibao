package cn.iocoder.yudao.module.logistics.controller.admin.transporttask.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "管理后台 - 批量分配运输任务 Request VO")
@Data
public class BatchAssignReqVO {

    @Schema(description = "分配策略", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "分配策略不能为空")
    private Integer assignStrategy; // 0:手动分配, 1:智能分配, 2:就近分配

    @Schema(description = "任务分配列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "任务分配列表不能为空")
    @Valid
    private List<TaskAssignmentVO> assignments;

    @Schema(description = "分配备注", example = "批量分配处理")
    private String remark;

    @Schema(description = "任务分配项")
    @Data
    public static class TaskAssignmentVO {
        
        @Schema(description = "任务ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
        @NotNull(message = "任务ID不能为空")
        private Long taskId;

        @Schema(description = "车辆ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "101")
        @NotNull(message = "车辆ID不能为空")
        private Long vehicleId;

        @Schema(description = "司机ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "201")
        @NotNull(message = "司机ID不能为空")
        private Long driverId;

        @Schema(description = "优先级", example = "1")
        private Integer priority; // 1:高优先级, 2:普通, 3:低优先级
    }
} 