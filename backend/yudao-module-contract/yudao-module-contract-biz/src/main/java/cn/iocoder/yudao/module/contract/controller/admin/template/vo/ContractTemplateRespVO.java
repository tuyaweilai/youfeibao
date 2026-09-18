package cn.iocoder.yudao.module.contract.controller.admin.template.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 合同模板 Response VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ContractTemplateRespVO extends ContractTemplateBaseVO {

    @Schema(description = "模板ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "模板名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "危废处置合同模板")
    private String templateName;

    @Schema(description = "模板编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "TEMPLATE_001")
    private String templateCode;

    @Schema(description = "合同类型ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
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

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

    @Schema(description = "更新时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime updateTime;

    @Schema(description = "创建者", example = "1")
    private String creator;

    @Schema(description = "更新者", example = "1")
    private String updater;
} 