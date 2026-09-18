package cn.iocoder.yudao.module.logistics.controller.admin.transporttask.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 批量分配运输任务结果 Response VO")
@Data
public class BatchAssignResultVO {

    @Schema(description = "总任务数", example = "10")
    private Integer totalCount;

    @Schema(description = "成功分配数", example = "8")
    private Integer successCount;

    @Schema(description = "失败分配数", example = "2")
    private Integer failureCount;

    @Schema(description = "成功分配列表")
    private List<AssignResult> successList;

    @Schema(description = "失败分配列表")
    private List<AssignResult> failureList;

    @Schema(description = "分配结果项")
    @Data
    public static class AssignResult {
        
        @Schema(description = "任务ID", example = "1024")
        private Long taskId;

        @Schema(description = "任务编号", example = "TT202401010001")
        private String taskNo;

        @Schema(description = "车辆ID", example = "101")
        private Long vehicleId;

        @Schema(description = "车牌号", example = "京A12345")
        private String plateNumber;

        @Schema(description = "司机ID", example = "201")
        private Long driverId;

        @Schema(description = "司机姓名", example = "张三")
        private String driverName;

        @Schema(description = "分配状态", example = "true")
        private Boolean success;

        @Schema(description = "失败原因", example = "车辆不可用")
        private String failureReason;
    }
} 