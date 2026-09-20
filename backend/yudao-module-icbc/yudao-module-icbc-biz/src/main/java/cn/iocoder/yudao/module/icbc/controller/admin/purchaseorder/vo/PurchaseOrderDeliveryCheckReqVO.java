package cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 管理后台 - 校验一次交货是否被允许 Request VO（#47 T09）。
 *
 * <p>收购登记（#51）选好采购安排后调这里：给定订单、明细、本次交货量与交货场站，返回三类异常
 * （超量 / 过期 / 跨场站）各自是否成立、有没有生效中的授权，以及企业配置是拦还是要求授权审核。
 */
@Schema(description = "管理后台 - 校验一次交货是否被允许 Request VO")
@Data
public class PurchaseOrderDeliveryCheckReqVO {

    @Schema(description = "采购订单编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "采购订单编号不能为空")
    private Long orderId;

    @Schema(description = "采购订单明细编号（不传则只校验订单级：过期 / 跨场站）")
    private Long itemId;

    @Schema(description = "本次交货量（验收口径，正数）")
    private BigDecimal quantity;

    @Schema(description = "本次交货场站编号（订单未指定执行场站时不校验跨场站）")
    private Long stationId;

}
