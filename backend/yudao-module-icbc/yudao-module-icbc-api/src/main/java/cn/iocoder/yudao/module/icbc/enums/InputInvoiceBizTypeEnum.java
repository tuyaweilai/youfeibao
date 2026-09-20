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
 *     <li>{@link #PURCHASE_ORDER}：采购订单由 #46（T08）并行落地。本分支**不 import 它的类**，
 *         单据号（{@code bizNo}）与单据金额（{@code bizAmount}）一律由调用方传入，
 *         金额上限只按调用方给出的 {@code bizAmount} 校验，保证本分支独立可编译可测；
 *         #46 合并后由人工把 {@code bizAmount} 的查数接到采购订单的只读方法上；</li>
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
