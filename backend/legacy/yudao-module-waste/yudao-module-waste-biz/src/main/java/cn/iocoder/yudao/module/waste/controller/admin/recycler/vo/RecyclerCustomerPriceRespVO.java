package cn.iocoder.yudao.module.waste.controller.admin.recycler.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 回收企业客户专属价格配置 Response VO")
@Data
public class RecyclerCustomerPriceRespVO {

    @Schema(description = "主键ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "回收企业ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long recyclingEnterpriseId;

    @Schema(description = "客户企业ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    private Long customerEnterpriseId;

    @Schema(description = "废物代码", requiredMode = Schema.RequiredMode.REQUIRED, example = "HW01")
    private String wasteCode;

    @Schema(description = "废物名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "医疗废物")
    private String wasteName;

    @Schema(description = "价格类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer priceType;

    @Schema(description = "专属价格", requiredMode = Schema.RequiredMode.REQUIRED, example = "1200.00")
    private BigDecimal specialPrice;

    @Schema(description = "价格单位", requiredMode = Schema.RequiredMode.REQUIRED, example = "元/吨")
    private String priceUnit;

    @Schema(description = "最小数量限制", example = "1.00")
    private BigDecimal minQuantityLimit;

    @Schema(description = "最大数量限制", example = "1000.00")
    private BigDecimal maxQuantityLimit;

    @Schema(description = "生效日期", requiredMode = Schema.RequiredMode.REQUIRED, example = "2023-12-01")
    private LocalDate effectiveDate;

    @Schema(description = "失效日期", example = "2024-12-01")
    private LocalDate expiryDate;

    @Schema(description = "合同ID", example = "3072")
    private Long contractId;

    @Schema(description = "状态", example = "1")
    private Integer status;

    @Schema(description = "备注", example = "专属价格配置备注")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

    @Schema(description = "更新时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime updateTime;

} 