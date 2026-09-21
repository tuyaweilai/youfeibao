package cn.iocoder.yudao.module.icbc.controller.admin.esign.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * 平台运营：调整租户电子签章合同额度 Request VO（#92）。
 */
@Schema(description = "管理后台 - 平台运营：调整租户电子签章合同额度 Request VO")
@Data
public class PlatformEsignQuotaSaveReqVO {

    @Schema(description = "租户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "租户编号不能为空")
    private Long tenantId;

    @Schema(description = "合同额度（份数）", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    @NotNull(message = "合同额度不能为空")
    @Min(value = 0, message = "合同额度不能为负")
    private Integer contractQuota;

    @Schema(description = "备注")
    private String remark;

}
