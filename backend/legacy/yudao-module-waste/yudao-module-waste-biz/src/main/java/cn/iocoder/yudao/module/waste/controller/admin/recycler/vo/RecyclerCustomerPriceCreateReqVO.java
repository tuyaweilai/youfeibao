package cn.iocoder.yudao.module.waste.controller.admin.recycler.vo;

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

@Schema(description = "管理后台 - 回收企业客户专属价格配置创建 Request VO")
@Data
public class RecyclerCustomerPriceCreateReqVO {

    @Schema(description = "回收企业ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "回收企业ID不能为空")
    private Long recyclingEnterpriseId;

    @Schema(description = "客户企业ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    @NotNull(message = "客户企业ID不能为空")
    private Long customerEnterpriseId;

    @Schema(description = "废物代码", requiredMode = Schema.RequiredMode.REQUIRED, example = "HW01")
    @NotBlank(message = "废物代码不能为空")
    @Size(max = 20, message = "废物代码长度不能超过20个字符")
    private String wasteCode;

    @Schema(description = "废物名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "医疗废物")
    @NotBlank(message = "废物名称不能为空")
    @Size(max = 100, message = "废物名称长度不能超过100个字符")
    private String wasteName;

    @Schema(description = "价格类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "价格类型不能为空")
    private Integer priceType;

    @Schema(description = "专属价格", requiredMode = Schema.RequiredMode.REQUIRED, example = "1200.00")
    @NotNull(message = "专属价格不能为空")
    @DecimalMin(value = "0", message = "专属价格不能小于0")
    private BigDecimal specialPrice;

    @Schema(description = "价格单位", requiredMode = Schema.RequiredMode.REQUIRED, example = "元/吨")
    @NotBlank(message = "价格单位不能为空")
    @Size(max = 20, message = "价格单位长度不能超过20个字符")
    private String priceUnit;

    @Schema(description = "最小数量限制", example = "1.00")
    @DecimalMin(value = "0", message = "最小数量限制不能小于0")
    private BigDecimal minQuantityLimit;

    @Schema(description = "最大数量限制", example = "1000.00")
    @DecimalMin(value = "0", message = "最大数量限制不能小于0")
    private BigDecimal maxQuantityLimit;

    @Schema(description = "生效日期", requiredMode = Schema.RequiredMode.REQUIRED, example = "2023-12-01")
    @NotNull(message = "生效日期不能为空")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate effectiveDate;

    @Schema(description = "失效日期", example = "2024-12-01")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate expiryDate;

    @Schema(description = "合同ID", example = "3072")
    private Long contractId;

    @Schema(description = "状态", example = "1")
    private Integer status;

    @Schema(description = "备注", example = "专属价格配置备注")
    @Size(max = 500, message = "备注长度不能超过500个字符")
    private String remark;

} 