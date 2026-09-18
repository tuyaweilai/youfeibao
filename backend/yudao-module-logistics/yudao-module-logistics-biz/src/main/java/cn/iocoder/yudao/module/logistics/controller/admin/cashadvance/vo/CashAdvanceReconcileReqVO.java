package cn.iocoder.yudao.module.logistics.controller.admin.cashadvance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Schema(description = "管理后台 - 现金代付对账确认 Request VO")
@Data
public class CashAdvanceReconcileReqVO {

    @Schema(description = "代付记录ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "代付记录ID不能为空")
    private Long id;

    @Schema(description = "对账状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "对账状态不能为空")
    private Integer reconcileStatus; // 0:待对账, 1:已确认, 2:有异议

    @Schema(description = "核实金额", example = "500.00")
    private BigDecimal verifiedAmount;

    @Schema(description = "对账备注", example = "金额核实无误")
    private String reconcileRemark;

    @Schema(description = "异议原因", example = "实际支付金额与记录不符")
    private String disputeReason;

    @Schema(description = "处理建议", example = "需要重新核实支付凭证")
    private String handleSuggestion;

    @Schema(description = "对账操作员ID", example = "1024")
    private Long reconcileOperatorId;

    @Schema(description = "对账操作员姓名", example = "张三")
    private String reconcileOperatorName;
} 