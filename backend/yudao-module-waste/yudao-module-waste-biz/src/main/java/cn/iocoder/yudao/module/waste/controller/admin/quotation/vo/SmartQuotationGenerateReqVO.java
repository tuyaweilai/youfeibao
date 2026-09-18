package cn.iocoder.yudao.module.waste.controller.admin.quotation.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;

@Schema(description = "管理后台 - 智能报价生成 Request VO")
@Data
public class SmartQuotationGenerateReqVO {

    @Schema(description = "预约单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "预约单ID不能为空")
    private Long appointmentId;

    @Schema(description = "回收企业ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "回收企业ID不能为空")
    private Long recyclingEnterpriseId;

    @Schema(description = "废物代码", requiredMode = Schema.RequiredMode.REQUIRED, example = "HW01")
    @NotBlank(message = "废物代码不能为空")
    private String wasteCode;

    @Schema(description = "数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "100.00")
    @NotNull(message = "数量不能为空")
    @Positive(message = "数量必须大于0")
    private BigDecimal quantity;

    @Schema(description = "客户企业ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @NotNull(message = "客户企业ID不能为空")
    private Long customerEnterpriseId;

    @Schema(description = "地区", example = "北京市")
    private String region;
} 