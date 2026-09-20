package cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 管理后台 - 采购订单成交记录 Request VO（#46 T08，用户故事 18）。
 *
 * <p>每次成交留价格快照与调整原因：成交价由调用方给出（通常来自收购单），
 * 服务端算出参考价并比对——不一致时 {@link #adjustReason} 必填。
 */
@Schema(description = "管理后台 - 采购订单成交记录 Request VO")
@Data
public class PurchaseOrderDealReqVO {

    @Schema(description = "采购订单编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "采购订单编号不能为空")
    private Long orderId;

    @Schema(description = "采购订单明细编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "采购订单明细编号不能为空")
    private Long itemId;

    @Schema(description = "交货日（按交货日价格表取价用）")
    private LocalDate deliveryDate;

    @Schema(description = "成交数量（结算量 / 计价基准）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "成交数量不能为空")
    private BigDecimal quantity;

    @Schema(description = "验收量（实物接收量）；不传则与成交数量相同")
    private BigDecimal acceptedQuantity;

    @Schema(description = "成交单价", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "成交单价不能为空")
    private BigDecimal unitPrice;

    @Schema(description = "调整原因（成交价与参考价不一致时必填）")
    private String adjustReason;

    @Schema(description = "交货场站编号（跨场站交货校验用；不传则不校验跨场站）")
    private Long stationId;

    @Schema(description = "关联业务来源类型（可空，如 ACQUISITION）")
    private String sourceType;

    @Schema(description = "关联业务来源编号")
    private Long sourceId;

    @Schema(description = "关联业务来源单号")
    private String sourceNo;

    @Schema(description = "备注")
    private String remark;

}
