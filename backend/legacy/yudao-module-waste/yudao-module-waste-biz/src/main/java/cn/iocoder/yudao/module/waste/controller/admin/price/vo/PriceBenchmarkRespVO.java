package cn.iocoder.yudao.module.waste.controller.admin.price.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 危险废物市场价格基准 Response VO")
@Data
public class PriceBenchmarkRespVO {

    @Schema(description = "主键ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "废物代码", requiredMode = Schema.RequiredMode.REQUIRED, example = "HW01")
    private String wasteCode;

    @Schema(description = "废物名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "医疗废物")
    private String wasteName;

    @Schema(description = "废物类别", example = "危险废物")
    private String wasteCategory;

    @Schema(description = "基准价格", requiredMode = Schema.RequiredMode.REQUIRED, example = "1000.00")
    private BigDecimal benchmarkPrice;

    @Schema(description = "价格单位", requiredMode = Schema.RequiredMode.REQUIRED, example = "元/吨")
    private String priceUnit;

    @Schema(description = "地区代码", example = "110000")
    private String regionCode;

    @Schema(description = "地区名称", example = "北京市")
    private String regionName;

    @Schema(description = "生效日期", requiredMode = Schema.RequiredMode.REQUIRED, example = "2023-12-01")
    private LocalDate effectiveDate;

    @Schema(description = "失效日期", example = "2024-12-01")
    private LocalDate expiryDate;

    @Schema(description = "数据来源", example = "政府发布")
    private String source;

    @Schema(description = "状态", example = "1")
    private Integer status;

    @Schema(description = "备注", example = "价格基准备注")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

    @Schema(description = "更新时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime updateTime;

} 