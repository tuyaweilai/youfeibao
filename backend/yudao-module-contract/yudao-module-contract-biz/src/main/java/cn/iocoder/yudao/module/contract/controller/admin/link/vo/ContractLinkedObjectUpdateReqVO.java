package cn.iocoder.yudao.module.contract.controller.admin.link.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - 合同关联对象更新 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ContractLinkedObjectUpdateReqVO extends ContractLinkedObjectCreateReqVO {

    @Schema(description = "关联ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "关联ID不能为空")
    private Long id;

} 