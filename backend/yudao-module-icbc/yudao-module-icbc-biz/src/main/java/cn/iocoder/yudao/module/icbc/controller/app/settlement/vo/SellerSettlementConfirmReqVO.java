package cn.iocoder.yudao.module.icbc.controller.app.settlement.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 自然人端确认结算（ADR 0024：勾选即确认，不再叠加验证码）。
 *
 * <p>必须显式带上 {@code naturalPersonId}：同一登录可代多个主体操作，不做静默推断（ADR 0017）。
 */
@Schema(description = "自然人端 - 确认结算 Request VO")
@Data
public class SellerSettlementConfirmReqVO {

    @Schema(description = "自然人主体编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "4096")
    @NotNull(message = "自然人主体编号不能为空")
    private Long naturalPersonId;

    @Schema(description = "结算单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "结算单编号不能为空")
    private Long settlementId;

    @Schema(description = "要确认的版本编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "版本编号不能为空")
    private Long versionId;

}
