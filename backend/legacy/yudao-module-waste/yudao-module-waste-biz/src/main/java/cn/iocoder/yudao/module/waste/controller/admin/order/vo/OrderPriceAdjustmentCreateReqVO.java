package cn.iocoder.yudao.module.waste.controller.admin.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Size;
import java.math.BigDecimal;

@Schema(description = "管理后台 - 订单价格调整记录创建 Request VO")
@Data
public class OrderPriceAdjustmentCreateReqVO {

    @Schema(description = "订单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    @Schema(description = "调整类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "调整类型不能为空")
    private Integer adjustmentType;

    @Schema(description = "原价格", example = "100.00")
    @DecimalMin(value = "0", message = "原价格不能小于0")
    private BigDecimal originalPrice;

    @Schema(description = "调整后价格", example = "120.00")
    @DecimalMin(value = "0", message = "调整后价格不能小于0")
    private BigDecimal adjustedPrice;

    @Schema(description = "原数量", example = "10.00")
    @DecimalMin(value = "0", message = "原数量不能小于0")
    private BigDecimal originalQuantity;

    @Schema(description = "调整后数量", example = "12.00")
    @DecimalMin(value = "0", message = "调整后数量不能小于0")
    private BigDecimal adjustedQuantity;

    @Schema(description = "调整金额", example = "200.00")
    private BigDecimal adjustmentAmount;

    @Schema(description = "调整原因", requiredMode = Schema.RequiredMode.REQUIRED, example = "过磅数据调整")
    @NotNull(message = "调整原因不能为空")
    @Size(max = 500, message = "调整原因长度不能超过500个字符")
    private String adjustmentReason;

    @Schema(description = "关联过磅记录ID", example = "2048")
    private Long relatedWeighingId;

    @Schema(description = "备注", example = "价格调整备注")
    @Size(max = 500, message = "备注长度不能超过500个字符")
    private String remark;

} 