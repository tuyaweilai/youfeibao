package cn.iocoder.yudao.module.waste.controller.admin.recycler.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.*;
import java.math.BigDecimal;

@Schema(description = "管理后台 - 回收企业业务模式配置创建 Request VO")
@Data
public class RecyclerBusinessConfigCreateReqVO {

    @Schema(description = "回收企业ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "回收企业ID不能为空")
    private Long recyclingEnterpriseId;

    @Schema(description = "业务模式", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "业务模式不能为空")
    private Integer businessMode;

    @Schema(description = "报价模式", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "报价模式不能为空")
    private Integer quotationMode;

    @Schema(description = "客户模式选择", example = "1")
    private Integer customerModeSelection;

    @Schema(description = "报价超时时间(小时)", example = "24")
    @Min(value = 1, message = "报价超时时间不能小于1小时")
    @Max(value = 168, message = "报价超时时间不能超过168小时")
    private Integer quotationTimeoutHours;

    @Schema(description = "自动接受报价阈值", example = "1000.00")
    @DecimalMin(value = "0", message = "自动接受报价阈值不能小于0")
    private BigDecimal autoAcceptThreshold;

    @Schema(description = "是否启用自动接受", example = "true")
    private Boolean autoAcceptEnabled;

    @Schema(description = "价格协商允许范围", example = "0.1")
    @DecimalMin(value = "0", message = "价格协商允许范围不能小于0")
    @DecimalMax(value = "1", message = "价格协商允许范围不能大于1")
    private BigDecimal priceNegotiationRange;

    @Schema(description = "是否允许价格协商", example = "true")
    private Boolean priceNegotiationEnabled;

    @Schema(description = "最大同时处理订单数", example = "10")
    @Min(value = 1, message = "最大同时处理订单数不能小于1")
    private Integer maxConcurrentOrders;

    @Schema(description = "优先级权重配置", example = "{\"price\":0.4,\"distance\":0.3,\"quantity\":0.3}")
    @Size(max = 1000, message = "优先级权重配置长度不能超过1000个字符")
    private String priorityWeightConfig;

    @Schema(description = "状态", example = "1")
    private Integer status;

    @Schema(description = "备注", example = "业务模式配置备注")
    @Size(max = 500, message = "备注长度不能超过500个字符")
    private String remark;

} 