package cn.iocoder.yudao.module.waste.controller.admin.price.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "管理后台 - 回收企业价格配置创建 Request VO")
@Data
public class RecyclerPriceConfigCreateReqVO {

    @Schema(description = "回收企业ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "回收企业ID不能为空")
    private Long recyclingEnterpriseId;

    @Schema(description = "废物代码", requiredMode = Schema.RequiredMode.REQUIRED, example = "HW01")
    @NotBlank(message = "废物代码不能为空")
    @Size(max = 20, message = "废物代码长度不能超过20个字符")
    private String wasteCode;

    @Schema(description = "废物名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "医疗废物")
    @NotBlank(message = "废物名称不能为空")
    @Size(max = 100, message = "废物名称长度不能超过100个字符")
    private String wasteName;

    @Schema(description = "废物类别", requiredMode = Schema.RequiredMode.REQUIRED, example = "危险废物")
    @NotBlank(message = "废物类别不能为空")
    @Size(max = 50, message = "废物类别长度不能超过50个字符")
    private String wasteCategory;

    @Schema(description = "地区代码", requiredMode = Schema.RequiredMode.REQUIRED, example = "110000")
    @NotBlank(message = "地区代码不能为空")
    @Size(max = 20, message = "地区代码长度不能超过20个字符")
    private String regionCode;

    @Schema(description = "地区名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "北京市")
    @NotBlank(message = "地区名称不能为空")
    @Size(max = 100, message = "地区名称长度不能超过100个字符")
    private String regionName;

    @Schema(description = "基础价格", requiredMode = Schema.RequiredMode.REQUIRED, example = "100.00")
    @NotNull(message = "基础价格不能为空")
    private BigDecimal basePrice;

    @Schema(description = "最低价格", example = "80.00")
    private BigDecimal minPrice;

    @Schema(description = "最高价格", example = "120.00")
    private BigDecimal maxPrice;

    @Schema(description = "价格单位", requiredMode = Schema.RequiredMode.REQUIRED, example = "元/吨")
    @NotBlank(message = "价格单位不能为空")
    @Size(max = 20, message = "价格单位长度不能超过20个字符")
    private String priceUnit;

    @Schema(description = "是否支持议价", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    @NotNull(message = "是否支持议价不能为空")
    private Boolean negotiable;

    @Schema(description = "价格策略", example = "固定价格")
    @Size(max = 50, message = "价格策略长度不能超过50个字符")
    private String pricingStrategy;

    @Schema(description = "生效日期", requiredMode = Schema.RequiredMode.REQUIRED, example = "2023-01-01")
    @NotNull(message = "生效日期不能为空")
    private LocalDate effectiveDate;

    @Schema(description = "失效日期", example = "2023-12-31")
    private LocalDate expiryDate;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "状态不能为空")
    private Integer status;

    @Schema(description = "备注", example = "价格配置备注")
    @Size(max = 500, message = "备注长度不能超过500个字符")
    private String remark;

} 