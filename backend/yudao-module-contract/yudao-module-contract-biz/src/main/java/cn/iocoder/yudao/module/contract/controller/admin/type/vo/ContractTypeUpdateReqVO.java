package cn.iocoder.yudao.module.contract.controller.admin.type.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - 合同类型更新 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ContractTypeUpdateReqVO extends ContractTypeCreateReqVO {

    @Schema(description = "类型ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "类型ID不能为空")
    private Long id;

} 