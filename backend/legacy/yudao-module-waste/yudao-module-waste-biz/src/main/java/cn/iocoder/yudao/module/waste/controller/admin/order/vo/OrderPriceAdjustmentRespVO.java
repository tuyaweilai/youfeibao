package cn.iocoder.yudao.module.waste.controller.admin.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 订单价格调整记录 Response VO")
@Data
public class OrderPriceAdjustmentRespVO {

    @Schema(description = "主键ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "订单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long orderId;

    @Schema(description = "调整类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer adjustmentType;

    @Schema(description = "原价格", example = "100.00")
    private BigDecimal originalPrice;

    @Schema(description = "调整后价格", example = "120.00")
    private BigDecimal adjustedPrice;

    @Schema(description = "原数量", example = "10.00")
    private BigDecimal originalQuantity;

    @Schema(description = "调整后数量", example = "12.00")
    private BigDecimal adjustedQuantity;

    @Schema(description = "调整金额", example = "200.00")
    private BigDecimal adjustmentAmount;

    @Schema(description = "调整原因", requiredMode = Schema.RequiredMode.REQUIRED, example = "过磅数据调整")
    private String adjustmentReason;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer status;

    @Schema(description = "确认人", example = "张三")
    private String confirmedBy;

    @Schema(description = "确认时间", example = "2023-12-01 10:00:00")
    private LocalDateTime confirmedTime;

    @Schema(description = "关联过磅记录ID", example = "2048")
    private Long relatedWeighingId;

    @Schema(description = "备注", example = "价格调整备注")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

    @Schema(description = "更新时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime updateTime;

} 