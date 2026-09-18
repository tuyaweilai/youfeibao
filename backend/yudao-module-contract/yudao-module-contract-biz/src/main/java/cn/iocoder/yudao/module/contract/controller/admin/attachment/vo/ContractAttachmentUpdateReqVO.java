package cn.iocoder.yudao.module.contract.controller.admin.attachment.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - 合同附件更新 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ContractAttachmentUpdateReqVO extends ContractAttachmentCreateReqVO {

    @Schema(description = "附件ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "附件ID不能为空")
    private Long id;

} 