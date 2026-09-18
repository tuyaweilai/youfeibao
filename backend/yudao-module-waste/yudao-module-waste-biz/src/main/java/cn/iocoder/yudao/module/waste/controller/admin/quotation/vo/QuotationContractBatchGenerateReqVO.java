package cn.iocoder.yudao.module.waste.controller.admin.quotation.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "管理后台 - 批量基于报价生成电子合同 Request VO")
@Data
public class QuotationContractBatchGenerateReqVO {

    @Schema(description = "报价ID列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "报价ID列表不能为空")
    private List<Long> quotationIds;

    @Schema(description = "合同类型ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "合同类型ID不能为空")
    private Long contractTypeId;

    @Schema(description = "合同模板ID", example = "1")
    private Long templateId;
} 