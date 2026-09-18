package cn.iocoder.yudao.module.contract.controller.admin.party.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - 合同参与方更新 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ContractPartyUpdateReqVO extends ContractPartyCreateReqVO {

    @Schema(description = "参与方ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "参与方ID不能为空")
    private Long id;

} 