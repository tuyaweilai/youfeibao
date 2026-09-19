package cn.iocoder.yudao.module.icbc.controller.admin.settlement.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

/**
 * 企业对异议的处理动作之一：**改**（产生新版本 + 原因，自动回到待确认）。ADR 0022。
 */
@Schema(description = "管理后台 - 结算单改版 Request VO")
@Data
public class SettlementChangeReqVO {

    @Schema(description = "结算单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "结算单编号不能为空")
    private Long settlementId;

    @Schema(description = "变更原因（对应异议原因，说清改了哪一项）", requiredMode = Schema.RequiredMode.REQUIRED, example = "扣杂按 0.02 更正")
    @NotEmpty(message = "变更原因不能为空")
    private String changeReason;

    @Schema(description = "修正后的逐条收购单计价", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "至少要提交一条收购单的修正")
    @Valid
    private List<Line> lines;

    @Schema(description = "管理后台 - 结算单改版 - 收购单计价")
    @Data
    public static class Line {

        @Schema(description = "收购单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
        @NotNull(message = "收购单编号不能为空")
        private Long acquisitionId;

        @Schema(description = "扣杂原始值", example = "200.00")
        private BigDecimal deduction;

        @Schema(description = "扣杂录法：WEIGHT / RATIO", example = "WEIGHT")
        private String deductionMethod;

        @Schema(description = "含税单价（元）", example = "2600.00")
        private BigDecimal unitPrice;

        @Schema(description = "调整项（元，可正可负）", example = "-100.00")
        private BigDecimal adjustmentAmount;

        @Schema(description = "调整原因；调整项非零时必填", example = "扣运费 100 元")
        private String adjustmentReason;

    }

}
