package cn.iocoder.yudao.module.contract.controller.admin.version.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - 合同版本更新 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ContractVersionUpdateReqVO extends ContractVersionCreateReqVO {

    @Schema(description = "版本ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "版本ID不能为空")
    private Long id;

} 