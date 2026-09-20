package cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 管理后台 - 采购订单交货日价格表 Request VO（#46 T08）。
 *
 * <p>一条 = 某个交货日生效的单价。「按交货日价格表」定价的明细用它。
 */
@Schema(description = "管理后台 - 采购订单交货日价格表 Request VO")
@Data
public class PurchaseOrderPriceReqVO {

    @Schema(description = "生效交货日", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "价格表的交货日不能为空")
    private LocalDate deliveryDate;

    @Schema(description = "该日生效单价", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "价格表的单价不能为空")
    private BigDecimal unitPrice;

    @Schema(description = "备注")
    private String remark;

}
