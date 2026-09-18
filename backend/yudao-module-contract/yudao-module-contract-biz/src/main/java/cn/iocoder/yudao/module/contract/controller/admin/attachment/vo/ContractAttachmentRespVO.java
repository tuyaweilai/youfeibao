package cn.iocoder.yudao.module.contract.controller.admin.attachment.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 合同附件 Response VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ContractAttachmentRespVO extends ContractAttachmentBaseVO {

    @Schema(description = "附件ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "合同版本ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long versionId;

    @Schema(description = "文件名", requiredMode = Schema.RequiredMode.REQUIRED, example = "合同正文.pdf")
    private String fileName;

    @Schema(description = "原始文件名", requiredMode = Schema.RequiredMode.REQUIRED, example = "合同正文.pdf")
    private String originalFileName;

    @Schema(description = "文件路径", requiredMode = Schema.RequiredMode.REQUIRED, example = "/contract/files/1.pdf")
    private String filePath;

    @Schema(description = "文件URL", requiredMode = Schema.RequiredMode.REQUIRED, example = "https://example.com/contract/files/1.pdf")
    private String fileUrl;

    @Schema(description = "文件大小(字节)", example = "1024")
    private Long fileSize;

    @Schema(description = "文件类型(MIME)", example = "application/pdf")
    private String fileType;

    @Schema(description = "附件类型(0:合同正文,1:扫描件,2:补充文件,3:签署凭证)", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    private Integer attachmentType;

    @Schema(description = "是否公开", example = "false")
    private Boolean isPublic;

    @Schema(description = "下载次数", example = "0")
    private Integer downloadCount;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

    @Schema(description = "更新时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime updateTime;

    @Schema(description = "创建者", example = "1")
    private String creator;

    @Schema(description = "更新者", example = "1")
    private String updater;
} 