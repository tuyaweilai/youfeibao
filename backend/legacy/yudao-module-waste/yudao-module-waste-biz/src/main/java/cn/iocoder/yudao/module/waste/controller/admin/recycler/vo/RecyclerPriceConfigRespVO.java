package cn.iocoder.yudao.module.waste.controller.admin.recycler.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 回收企业价格配置 Response VO")
@Data
public class RecyclerPriceConfigRespVO {

    @Schema(description = "主键ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "回收企业ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long recyclingEnterpriseId;

    @Schema(description = "废物代码", requiredMode = Schema.RequiredMode.REQUIRED, example = "HW01")
    private String wasteCode;

    @Schema(description = "废物名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "医疗废物")
    private String wasteName;

    @Schema(description = "废物类别", example = "危险废物")
    private String wasteCategory;

    @Schema(description = "收购价格", requiredMode = Schema.RequiredMode.REQUIRED, example = "1000.00")
    private BigDecimal purchasePrice;

    @Schema(description = "价格单位", requiredMode = Schema.RequiredMode.REQUIRED, example = "元/吨")
    private String priceUnit;

    @Schema(description = "最小收购数量", example = "1.00")
    private BigDecimal minPurchaseQuantity;

    @Schema(description = "最大收购数量", example = "1000.00")
    private BigDecimal maxPurchaseQuantity;

    @Schema(description = "地区代码", example = "110000")
    private String regionCode;

    @Schema(description = "地区名称", example = "北京市")
    private String regionName;

    @Schema(description = "生效日期", requiredMode = Schema.RequiredMode.REQUIRED, example = "2023-12-01")
    private LocalDate effectiveDate;

    @Schema(description = "失效日期", example = "2024-12-01")
    private LocalDate expiryDate;

    @Schema(description = "是否支持议价", example = "true")
    private Boolean negotiable;

    @Schema(description = "议价范围下限", example = "0.8")
    private BigDecimal negotiationLowerLimit;

    @Schema(description = "议价范围上限", example = "1.2")
    private BigDecimal negotiationUpperLimit;

    @Schema(description = "价格策略", example = "固定价格")
    private String pricingStrategy;

    @Schema(description = "状态", example = "1")
    private Integer status;

    @Schema(description = "备注", example = "价格配置备注")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

    @Schema(description = "更新时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime updateTime;

} 