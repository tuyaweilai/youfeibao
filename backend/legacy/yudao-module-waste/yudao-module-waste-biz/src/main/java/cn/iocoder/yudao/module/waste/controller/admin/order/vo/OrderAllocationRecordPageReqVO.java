package cn.iocoder.yudao.module.waste.controller.admin.order.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 订单过磅分摊记录分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class OrderAllocationRecordPageReqVO extends PageParam {

    @Schema(description = "订单ID", example = "1024")
    private Long orderId;

    @Schema(description = "车辆过磅记录ID", example = "2048")
    private Long vehicleWeighingId;

    @Schema(description = "分摊方法", example = "1")
    private Integer allocationMethod;

    @Schema(description = "是否人工调整", example = "false")
    private Boolean isManualAdjustment;

    @Schema(description = "调整人员", example = "张三")
    private String adjustedBy;

    @Schema(description = "过磅批次号", example = "WB20240101001")
    private String weighingBatchNo;

    @Schema(description = "分摊操作员", example = "李四")
    private String allocationOperator;

    @Schema(description = "分摊时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] allocationTime;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

} 