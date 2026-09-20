package cn.iocoder.yudao.module.logistics.controller.admin.carriercontract.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 承运合同附加费项（V8 #75）：名称 + 金额 + **承担方**。
 *
 * <p>承担方决定这笔附加费进不进「应付给承运商的运费」：本企业承担则加进应有应付，
 * 承运商承担则从应付里净掉（CONTEXT.md「承运合同」的「附加费与其承担方」）。
 */
@Schema(description = "管理后台 - 承运合同附加费项")
@Data
public class LogisticsCarrierContractSurchargeVO {

    @Schema(description = "附加费名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "路桥附加费")
    private String name;

    @Schema(description = "金额（元）", requiredMode = Schema.RequiredMode.REQUIRED, example = "100.00")
    @NotNull(message = "附加费金额不能为空")
    private BigDecimal amount;

    @Schema(description = "承担方：1-承运商承担，2-本企业承担", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @NotNull(message = "附加费承担方不能为空")
    private Integer bearer;

}
