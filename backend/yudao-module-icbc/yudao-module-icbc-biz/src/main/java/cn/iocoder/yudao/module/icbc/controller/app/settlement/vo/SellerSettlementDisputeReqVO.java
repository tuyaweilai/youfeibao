package cn.iocoder.yudao.module.icbc.controller.app.settlement.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

/**
 * 自然人端提出异议（ADR 0022：固定原因枚举，不做聊天）。
 */
@Schema(description = "自然人端 - 提出异议 Request VO")
@Data
public class SellerSettlementDisputeReqVO {

    @Schema(description = "自然人主体编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "4096")
    @NotNull(message = "自然人主体编号不能为空")
    private Long naturalPersonId;

    @Schema(description = "结算单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "结算单编号不能为空")
    private Long settlementId;

    @Schema(description = "异议原因：01-重量不符，02-扣杂不符，03-单价不符，04-品类或等级不符，05-货物不符，99-其他（须附说明）",
            requiredMode = Schema.RequiredMode.REQUIRED, example = "02")
    @NotEmpty(message = "异议原因不能为空")
    private String reason;

    @Schema(description = "说明（选「其他」时必填）", example = "扣杂按 3% 收，现场说好是 1%")
    private String note;

}
