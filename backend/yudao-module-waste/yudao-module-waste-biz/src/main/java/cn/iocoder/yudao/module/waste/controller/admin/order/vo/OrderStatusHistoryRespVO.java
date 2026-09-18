package cn.iocoder.yudao.module.waste.controller.admin.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 订单状态变更历史 Response VO")
@Data
public class OrderStatusHistoryRespVO {

    @Schema(description = "主键ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "订单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long orderId;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer status;

    @Schema(description = "状态名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "待确认")
    private String statusName;

    @Schema(description = "操作人", requiredMode = Schema.RequiredMode.REQUIRED, example = "张三")
    private String operator;

    @Schema(description = "操作时间", example = "2023-12-01 10:00:00")
    private LocalDateTime operationTime;

    @Schema(description = "备注", example = "订单状态变更备注")
    private String remark;

    @Schema(description = "是否里程碑", example = "false")
    private Boolean isMilestone;

    @Schema(description = "业务数据", example = "{\"key\":\"value\"}")
    private String businessData;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

    @Schema(description = "更新时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime updateTime;

} 