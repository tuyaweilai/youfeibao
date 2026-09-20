package cn.iocoder.yudao.module.waste.controller.admin.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 订单过磅分摊记录 Response VO")
@Data
public class OrderAllocationRecordRespVO {

    @Schema(description = "主键ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "订单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long orderId;

    @Schema(description = "车辆过磅记录ID", example = "2048")
    private Long vehicleWeighingId;

    @Schema(description = "分摊方法", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer allocationMethod;

    @Schema(description = "分摊比例", example = "0.5")
    private BigDecimal allocationRatio;

    @Schema(description = "分摊数量", example = "100.00")
    private BigDecimal allocationQuantity;

    @Schema(description = "分摊金额", example = "1000.00")
    private BigDecimal allocationAmount;

    @Schema(description = "是否人工调整", example = "false")
    private Boolean isManualAdjustment;

    @Schema(description = "调整原因", example = "过磅数据异常，需要人工调整")
    private String adjustmentReason;

    @Schema(description = "调整人员", example = "张三")
    private String adjustedBy;

    @Schema(description = "调整时间", example = "2023-12-01 10:00:00")
    private LocalDateTime adjustmentTime;

    @Schema(description = "备注", example = "分摊计算备注")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

    @Schema(description = "更新时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime updateTime;

} 