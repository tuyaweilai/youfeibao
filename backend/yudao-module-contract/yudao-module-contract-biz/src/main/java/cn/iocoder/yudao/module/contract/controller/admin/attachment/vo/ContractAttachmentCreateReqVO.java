package cn.iocoder.yudao.module.contract.controller.admin.attachment.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Schema(description = "管理后台 - 合同附件创建 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ContractAttachmentCreateReqVO extends ContractAttachmentBaseVO {

    @Schema(description = "合同版本ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "合同版本ID不能为空")
    private Long versionId;

    @Schema(description = "文件名", requiredMode = Schema.RequiredMode.REQUIRED, example = "合同正文.pdf")
    @NotEmpty(message = "文件名不能为空")
    @Size(max = 255, message = "文件名长度不能超过 255 个字符")
    private String fileName;

    @Schema(description = "原始文件名", requiredMode = Schema.RequiredMode.REQUIRED, example = "合同正文.pdf")
    @NotEmpty(message = "原始文件名不能为空")
    @Size(max = 255, message = "原始文件名长度不能超过 255 个字符")
    private String originalFileName;

    @Schema(description = "文件路径", requiredMode = Schema.RequiredMode.REQUIRED, example = "/contract/files/1.pdf")
    @NotEmpty(message = "文件路径不能为空")
    @Size(max = 512, message = "文件路径长度不能超过 512 个字符")
    private String filePath;

    @Schema(description = "文件URL", requiredMode = Schema.RequiredMode.REQUIRED, example = "https://example.com/contract/files/1.pdf")
    @NotEmpty(message = "文件URL不能为空")
    @Size(max = 512, message = "文件URL长度不能超过 512 个字符")
    private String fileUrl;

    @Schema(description = "文件大小(字节)", example = "1024")
    private Long fileSize;

    @Schema(description = "文件类型(MIME)", example = "application/pdf")
    private String fileType;

    @Schema(description = "附件类型(0:合同正文,1:扫描件,2:补充文件,3:签署凭证)", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "附件类型不能为空")
    private Integer attachmentType;

    @Schema(description = "是否公开", example = "false")
    private Boolean isPublic;
} 