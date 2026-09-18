package cn.iocoder.yudao.module.waste.controller.admin.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 订单状态变更历史创建 Request VO")
@Data
public class OrderStatusHistoryCreateReqVO {

    @Schema(description = "订单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "状态不能为空")
    private Integer status;

    @Schema(description = "状态名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "待确认")
    @NotNull(message = "状态名称不能为空")
    @Size(max = 50, message = "状态名称长度不能超过50个字符")
    private String statusName;

    @Schema(description = "操作人", requiredMode = Schema.RequiredMode.REQUIRED, example = "张三")
    @NotNull(message = "操作人不能为空")
    @Size(max = 50, message = "操作人长度不能超过50个字符")
    private String operator;

    @Schema(description = "操作时间", example = "2023-12-01 10:00:00")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime operationTime;

    @Schema(description = "备注", example = "订单状态变更备注")
    @Size(max = 500, message = "备注长度不能超过500个字符")
    private String remark;

    @Schema(description = "是否里程碑", example = "false")
    private Boolean isMilestone;

    @Schema(description = "业务数据", example = "{\"key\":\"value\"}")
    private String businessData;

} 