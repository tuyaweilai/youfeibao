package cn.iocoder.yudao.module.contract.controller.admin.link.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Schema(description = "管理后台 - 合同关联对象创建 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ContractLinkedObjectCreateReqVO extends ContractLinkedObjectBaseVO {

    @Schema(description = "合同ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "合同ID不能为空")
    private Long contractId;

    @Schema(description = "合同版本ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "合同版本ID不能为空")
    private Long versionId;

    @Schema(description = "关联对象ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @NotNull(message = "关联对象ID不能为空")
    private Long objectId;

    @Schema(description = "关联对象类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "waste_transfer")
    @NotEmpty(message = "关联对象类型不能为空")
    @Size(max = 50, message = "关联对象类型长度不能超过 50 个字符")
    private String objectType;

    @Schema(description = "关联对象编号", example = "WTO20230101001")
    private String objectNo;

    @Schema(description = "关联类型(0:业务关联,1:依赖关联,2:参考关联)", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "关联类型不能为空")
    private Integer linkType;

    @Schema(description = "关联状态(0:有效,1:失效,2:暂停)", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "关联状态不能为空")
    private Integer linkStatus;

    @Schema(description = "关联说明", example = "测试关联说明")
    private String linkDescription;
} 