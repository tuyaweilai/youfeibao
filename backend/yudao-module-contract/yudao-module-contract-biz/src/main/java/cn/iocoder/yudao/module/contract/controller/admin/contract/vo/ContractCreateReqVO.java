package cn.iocoder.yudao.module.contract.controller.admin.contract.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Schema(description = "管理后台 - 合同创建 Request VO")
@Data
public class ContractCreateReqVO {

    @Schema(description = "合同名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "危废处置服务合同")
    @NotBlank(message = "合同名称不能为空")
    private String name;

    @Schema(description = "合同类型ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "合同类型不能为空")
    private Long typeId;

    @Schema(description = "合同模板ID", example = "1")
    private Long templateId;

    @Schema(description = "合同状态", example = "0")
    private Integer status;

    @Schema(description = "是否为电子合同", example = "true")
    private Boolean isElectronic;

    @Schema(description = "合同主要负责企业ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    @NotNull(message = "合同主要负责企业不能为空")
    private Long primaryOwnerEnterpriseId;

    @Schema(description = "合同总金额", example = "100000.00")
    private BigDecimal totalAmount;

    @Schema(description = "币种", example = "CNY")
    private String currency;

    @Schema(description = "优先级", example = "0")
    private Integer priorityLevel;

    @Schema(description = "备注", example = "这是一份重要合同")
    private String remark;

    @Schema(description = "生效日期", example = "2023-01-01")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate effectiveDate;

    @Schema(description = "到期日期", example = "2023-12-31")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate expiryDate;

    @Schema(description = "合同内容", example = "合同具体内容...")
    private String contractContent;

} 