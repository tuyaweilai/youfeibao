package cn.iocoder.yudao.module.icbc.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * 进项发票可勾稽的业务单据类型（#49 T11）。
 *
 * <p>勾稽关系落在通用关联表 {@code icbc_input_invoice_link}：一行表示「这张进项票的某段金额
 * 勾到了哪张单据」，因此单据类型必须是可枚举的稳定编码，而不是每种单据各自一张关联表。
 *
 * <p><b>联调边界（第二轮并行约定）</b>：
 * <ul>
 *     <li>{@link #ACQUISITION}：本分支已存在的收购单（退货 / 收购场景）；</li>
 *     <li>{@link #PURCHASE_ORDER}：采购订单由 #46（T08）落地。单据号与单据金额以订单事实为准，
 *         由 {@code InputInvoiceServiceImpl} 经 {@code PurchaseOrderService.getOrderAmount(id)} 取，
 *         不采用调用方传入的值；调用方传的 {@code bizNo} / {@code bizAmount} 只做展示，会被覆盖；</li>
 *     <li>{@link #STOCK_IN}：入库单由 #52（T14）落地，这里先预留编码。</li>
 * </ul>
 */
public enum InputInvoiceBizTypeEnum {

    /** 收购单（{@code icbc_acquisition}） */
    ACQUISITION("ACQUISITION", "收购单"),
    /** 采购订单（#46 T08） */
    PURCHASE_ORDER("PURCHASE_ORDER", "采购订单"),
    /** 入库单（#52 T14，预留） */
    STOCK_IN("STOCK_IN", "入库单");

    /** 单据类型编码 */
    private final String type;
    /** 单据类型名称 */
    private final String name;

    InputInvoiceBizTypeEnum(String type, String name) {
        this.type = type;
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public static Optional<InputInvoiceBizTypeEnum> ofType(String type) {
        return Arrays.stream(values()).filter(item -> item.type.equals(type)).findFirst();
    }

    public static String nameOf(String type) {
        return ofType(type).map(InputInvoiceBizTypeEnum::getName).orElse(null);
    }

}
