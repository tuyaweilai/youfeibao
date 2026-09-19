package cn.iocoder.yudao.module.icbc.controller.admin.tax.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 代办税费申报预警：申报期临近、已逾期可能被暂停开票资格、或申报数据不齐。
 */
@Schema(description = "管理后台 - 代办税费申报预警 Response VO")
@Data
public class TaxDeclarationWarningRespVO {

    @Schema(description = "预警类型：DEADLINE_APPROACHING / OVERDUE_SUSPENSION_RISK / MISSING_DATA")
    private String type;

    @Schema(description = "预警类型名称", example = "申报期临近")
    private String typeName;

    @Schema(description = "申报单编号")
    private Long declarationId;

    @Schema(description = "申报月 yyyy-MM", example = "2026-08")
    private String periodMonth;

    @Schema(description = "申报期截止日", example = "2026-09-15")
    private LocalDate declarationDeadline;

    @Schema(description = "距截止日天数（负数表示已逾期）", example = "3")
    private Integer daysLeft;

    @Schema(description = "是否已逾期", example = "false")
    private Boolean overdue;

    @Schema(description = "应缴税费合计（元）")
    private BigDecimal totalTaxAmount;

    @Schema(description = "是否可能被暂停开票资格", example = "false")
    private Boolean suspensionRisk;

    @Schema(description = "缺项数量")
    private Integer missingDataCount;

    @Schema(description = "说明")
    private String message;

    @Schema(description = "下一步动作")
    private String nextAction;

}
