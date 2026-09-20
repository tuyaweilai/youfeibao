package cn.iocoder.yudao.module.icbc.service.purchaseorder;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 采购订单「单号 + 单据金额」只读视图（#46 T08）。
 *
 * <p>供进项收票登记与勾稽（#49）按订单编号取 {@code bizNo} / {@code bizAmount} 用，
 * 不暴露订单内部结构，避免两张票互相编译依赖（并行约定：第二轮 #46 / #49）。
 */
@Data
public class PurchaseOrderAmountDTO {

    /** 采购订单编号 */
    private Long orderId;

    /** 采购订单号 */
    private String orderNo;

    /** 交易对方名称快照 */
    private String counterpartyName;

    /** 单据金额（计划总金额） */
    private BigDecimal totalAmount;

    /** 是否可作为采购依据（执行中且未过期） */
    private Boolean usableAsPurchaseBasis;

}
