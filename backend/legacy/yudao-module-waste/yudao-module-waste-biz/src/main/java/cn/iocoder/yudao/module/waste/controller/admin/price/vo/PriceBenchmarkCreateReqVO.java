package cn.iocoder.yudao.module.waste.controller.admin.price.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Schema(description = "管理后台 - 危险废物市场价格基准创建 Request VO")
@Data
public class PriceBenchmarkCreateReqVO {

    @Schema(description = "废物代码", requiredMode = Schema.RequiredMode.REQUIRED, example = "HW01")
    @NotBlank(message = "废物代码不能为空")
    @Size(max = 20, message = "废物代码长度不能超过20个字符")
    private String wasteCode;

    @Schema(description = "废物名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "医疗废物")
    @NotBlank(message = "废物名称不能为空")
    @Size(max = 100, message = "废物名称长度不能超过100个字符")
    private String wasteName;

    @Schema(description = "废物类别", example = "危险废物")
    @Size(max = 50, message = "废物类别长度不能超过50个字符")
    private String wasteCategory;

    @Schema(description = "基准价格", requiredMode = Schema.RequiredMode.REQUIRED, example = "1000.00")
    @NotNull(message = "基准价格不能为空")
    @DecimalMin(value = "0", message = "基准价格不能小于0")
    private BigDecimal benchmarkPrice;

    @Schema(description = "价格单位", requiredMode = Schema.RequiredMode.REQUIRED, example = "元/吨")
    @NotBlank(message = "价格单位不能为空")
    @Size(max = 20, message = "价格单位长度不能超过20个字符")
    private String priceUnit;

    @Schema(description = "地区代码", example = "110000")
    @Size(max = 20, message = "地区代码长度不能超过20个字符")
    private String regionCode;

    @Schema(description = "地区名称", example = "北京市")
    @Size(max = 100, message = "地区名称长度不能超过100个字符")
    private String regionName;

    @Schema(description = "生效日期", requiredMode = Schema.RequiredMode.REQUIRED, example = "2023-12-01")
    @NotNull(message = "生效日期不能为空")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate effectiveDate;

    @Schema(description = "失效日期", example = "2024-12-01")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate expiryDate;

    @Schema(description = "数据来源", example = "政府发布")
    @Size(max = 100, message = "数据来源长度不能超过100个字符")
    private String source;

    @Schema(description = "状态", example = "1")
    private Integer status;

    @Schema(description = "备注", example = "价格基准备注")
    @Size(max = 500, message = "备注长度不能超过500个字符")
    private String remark;

} 