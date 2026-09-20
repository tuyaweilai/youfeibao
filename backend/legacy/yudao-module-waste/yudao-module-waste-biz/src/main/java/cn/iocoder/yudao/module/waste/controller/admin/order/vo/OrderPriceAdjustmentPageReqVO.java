package cn.iocoder.yudao.module.waste.controller.admin.order.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 订单价格调整记录分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class OrderPriceAdjustmentPageReqVO extends PageParam {

    @Schema(description = "订单ID", example = "1024")
    private Long orderId;

    @Schema(description = "调整类型", example = "1")
    private Integer adjustmentType;

    @Schema(description = "状态", example = "1")
    private Integer status;

    @Schema(description = "确认人", example = "张三")
    private String confirmedBy;

    @Schema(description = "关联过磅记录ID", example = "2048")
    private Long relatedWeighingId;

    @Schema(description = "调整原因", example = "过磅数据调整")
    private String adjustmentReason;

    @Schema(description = "最小调整金额", example = "100.00")
    private BigDecimal minAdjustmentAmount;

    @Schema(description = "最大调整金额", example = "1000.00")
    private BigDecimal maxAdjustmentAmount;

    @Schema(description = "确认时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] confirmedTime;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

} 