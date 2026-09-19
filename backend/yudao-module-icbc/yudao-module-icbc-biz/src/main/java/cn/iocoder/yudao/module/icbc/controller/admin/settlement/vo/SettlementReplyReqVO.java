package cn.iocoder.yudao.module.icbc.controller.admin.settlement.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

/**
 * 企业对异议的处理动作之二：**不改但附说明**（也回到待确认，自然人再确认一次即等于接受）。ADR 0022。
 */
@Schema(description = "管理后台 - 结算单不改但附说明 Request VO")
@Data
public class SettlementReplyReqVO {

    @Schema(description = "结算单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "结算单编号不能为空")
    private Long settlementId;

    @Schema(description = "说明（为什么这一项不改）", requiredMode = Schema.RequiredMode.REQUIRED, example = "扣杂按合同约定 1% 计，已附磅单与合同照片")
    @NotEmpty(message = "说明不能为空")
    private String note;

}
