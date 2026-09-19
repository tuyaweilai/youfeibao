package cn.iocoder.yudao.module.icbc.controller.admin.tax.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.math.BigDecimal;

@Schema(description = "管理后台 - 登记需补缴税费 Request VO")
@Data
public class TaxSupplementCreateReqVO {

    @Schema(description = "所属申报月 yyyy-MM", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-08")
    @NotBlank(message = "申报月不能为空")
    private String periodMonth;

    @Schema(description = "关联申报单编号", example = "1024")
    private Long declarationId;

    @Schema(description = "出售者档案编号", example = "2048")
    private Long payeeId;

    @Schema(description = "补缴原因", example = "跨期红冲调整，原申报月已缴清")
    private String reason;

    @Schema(description = "按 3% 减按 1% 部分的补缴金额（元）", example = "100.00")
    private BigDecimal amountAtOnePercent;

    @Schema(description = "放弃减按、按 3% 部分的补缴金额（元）", example = "300.00")
    private BigDecimal amountAtThreePercent;

    @Schema(description = "备注")
    private String remark;

}
