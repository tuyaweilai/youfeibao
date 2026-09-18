package cn.iocoder.yudao.module.waste.controller.admin.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.DecimalMin;
import java.math.BigDecimal;

@Schema(description = "管理后台 - 订单过磅分摊记录创建 Request VO")
@Data
public class OrderAllocationRecordCreateReqVO {

    @Schema(description = "订单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    @Schema(description = "车辆过磅记录ID", example = "2048")
    private Long vehicleWeighingId;

    @Schema(description = "分摊方法", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "分摊方法不能为空")
    private Integer allocationMethod;

    @Schema(description = "分摊比例", example = "0.5")
    @DecimalMin(value = "0", message = "分摊比例不能小于0")
    private BigDecimal allocationRatio;

    @Schema(description = "分摊数量", example = "100.00")
    @DecimalMin(value = "0", message = "分摊数量不能小于0")
    private BigDecimal allocationQuantity;

    @Schema(description = "分摊金额", example = "1000.00")
    @DecimalMin(value = "0", message = "分摊金额不能小于0")
    private BigDecimal allocationAmount;

    @Schema(description = "是否人工调整", example = "false")
    private Boolean isManualAdjustment;

    @Schema(description = "调整原因", example = "过磅数据异常，需要人工调整")
    private String adjustmentReason;

    @Schema(description = "调整人员", example = "张三")
    private String adjustedBy;

    @Schema(description = "备注", example = "分摊计算备注")
    private String remark;

} 