package cn.iocoder.yudao.module.contract.controller.admin.attachment.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 合同附件分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ContractAttachmentPageReqVO extends PageParam {

    @Schema(description = "合同版本ID", example = "1")
    private Long versionId;

    @Schema(description = "文件名", example = "合同正文.pdf")
    private String fileName;

    @Schema(description = "附件类型", example = "0")
    private Integer attachmentType;

    @Schema(description = "是否公开", example = "true")
    private Boolean isPublic;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

} 