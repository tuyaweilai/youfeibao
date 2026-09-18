package cn.iocoder.yudao.module.enterprise.controller.admin.enterprise.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
@Schema(description = "企业入驻审核 Request VO")
public class EnterpriseInfoAuditReqVO {
    @Schema(description = "企业ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "企业ID不能为空")
    private Long id;

    @Schema(description = "是否通过", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "审核结果不能为空")
    private Boolean approved;

    @Schema(description = "审核备注", example = "资料齐全，审核通过")
    private String auditRemarks;
} 