package cn.iocoder.yudao.module.contract.controller.admin.template.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Schema(description = "管理后台 - 合同模板创建 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ContractTemplateCreateReqVO extends ContractTemplateBaseVO {

    @Schema(description = "模板名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "危废处置合同模板")
    @NotEmpty(message = "模板名称不能为空")
    @Size(max = 255, message = "模板名称长度不能超过 255 个字符")
    private String templateName;

    @Schema(description = "模板编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "TEMPLATE_001")
    @NotEmpty(message = "模板编码不能为空")
    @Size(max = 100, message = "模板编码长度不能超过 100 个字符")
    private String templateCode;

    @Schema(description = "合同类型ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "合同类型ID不能为空")
    private Long contractTypeId;

    @Schema(description = "模板内容", example = "合同内容...")
    private String templateContent;

    @Schema(description = "模板文件URL", example = "https://example.com/template.docx")
    private String templateFileUrl;

    @Schema(description = "模板版本", example = "1.0")
    private String version;

    @Schema(description = "是否启用", example = "true")
    private Boolean isActive;

    @Schema(description = "是否默认模板", example = "false")
    private Boolean isDefault;

    @Schema(description = "备注", example = "这是一个备注")
    private String remark;
} 