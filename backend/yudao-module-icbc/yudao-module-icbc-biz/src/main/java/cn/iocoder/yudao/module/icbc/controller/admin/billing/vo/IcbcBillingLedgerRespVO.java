package cn.iocoder.yudao.module.icbc.controller.admin.billing.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 平台计费计量台账 Response VO")
@Data
public class IcbcBillingLedgerRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "租户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long tenantId;

    @Schema(description = "计费期间（yyyy-MM）", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-09")
    private String periodMonth;

    @Schema(description = "成功开具的报废产品收购发票张数", requiredMode = Schema.RequiredMode.REQUIRED, example = "12")
    private Integer issuedCount;

    @Schema(description = "蓝票中被成功红冲的张数", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer reversedCount;

    @Schema(description = "计费张数", requiredMode = Schema.RequiredMode.REQUIRED, example = "11")
    private Integer billableCount;

    @Schema(description = "计费单价（元/张）", requiredMode = Schema.RequiredMode.REQUIRED, example = "10.00")
    private BigDecimal unitPrice;

    @Schema(description = "应计费用（元）", requiredMode = Schema.RequiredMode.REQUIRED, example = "110.00")
    private BigDecimal amount;

    @Schema(description = "计量时间")
    private LocalDateTime generatedTime;

}
