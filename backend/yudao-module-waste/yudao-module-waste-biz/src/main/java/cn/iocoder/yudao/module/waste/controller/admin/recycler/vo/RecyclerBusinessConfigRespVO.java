package cn.iocoder.yudao.module.waste.controller.admin.recycler.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 回收企业业务模式配置 Response VO")
@Data
public class RecyclerBusinessConfigRespVO {

    @Schema(description = "主键ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "回收企业ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long recyclingEnterpriseId;

    @Schema(description = "业务模式", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer businessMode;

    @Schema(description = "报价模式", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer quotationMode;

    @Schema(description = "客户模式选择", example = "1")
    private Integer customerModeSelection;

    @Schema(description = "报价超时时间(小时)", example = "24")
    private Integer quotationTimeoutHours;

    @Schema(description = "自动接受报价阈值", example = "1000.00")
    private BigDecimal autoAcceptThreshold;

    @Schema(description = "是否启用自动接受", example = "true")
    private Boolean autoAcceptEnabled;

    @Schema(description = "价格协商允许范围", example = "0.1")
    private BigDecimal priceNegotiationRange;

    @Schema(description = "是否允许价格协商", example = "true")
    private Boolean priceNegotiationEnabled;

    @Schema(description = "最大同时处理订单数", example = "10")
    private Integer maxConcurrentOrders;

    @Schema(description = "优先级权重配置", example = "{\"price\":0.4,\"distance\":0.3,\"quantity\":0.3}")
    private String priorityWeightConfig;

    @Schema(description = "状态", example = "1")
    private Integer status;

    @Schema(description = "备注", example = "业务模式配置备注")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

    @Schema(description = "更新时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime updateTime;

} 