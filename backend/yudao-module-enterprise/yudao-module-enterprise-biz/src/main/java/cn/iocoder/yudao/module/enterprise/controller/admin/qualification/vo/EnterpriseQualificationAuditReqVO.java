package cn.iocoder.yudao.module.enterprise.controller.admin.qualification.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 管理后台 - 企业资质审核 Request VO
 *
 * @author 芋道源码
 */
@Schema(description = "管理后台 - 企业资质审核 Request VO")
@Data
public class EnterpriseQualificationAuditReqVO {

    @Schema(description = "资质ID", required = true, example = "1024")
    @NotNull(message = "资质ID不能为空")
    private Long id;

    @Schema(description = "审核决定", required = true, example = "true")
    @NotNull(message = "审核决定不能为空")
    private Boolean auditDecision;

    @Schema(description = "审核备注", example = "资料真实有效，审核通过")
    @Size(max = 500, message = "审核备注长度不能超过 500 个字符")
    private String auditRemarks;

} 