package cn.iocoder.yudao.module.icbc.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * 采购订单成交记录的业务来源类型（#47 T09）。
 *
 * <p>成交记录（{@code icbc_purchase_order_deal}）是「这次收货挂到订单上的量」的载体。它可能来自
 * 收购单（#51 把收购单挂到订单上），也可能是采购经办手工登记的一笔。来源类型决定了
 * **结算口径**能不能顺着它找到收购单（{@link #ACQUISITION} 才有下游事实），所以它必须是一个
 * 受控取值而不是随手写的字符串。
 */
public enum PurchaseDealSourceTypeEnum {

    /** 收购单：成交记录由收购登记产生（#51 接入后走这条）。 */
    ACQUISITION("ACQUISITION", "收购单"),

    /** 手工登记：没有下游单据的成交（仅用于补录历史价格，结算口径不计入）。 */
    MANUAL("MANUAL", "手工登记");

    private final String type;
    private final String name;

    PurchaseDealSourceTypeEnum(String type, String name) {
        this.type = type;
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public static Optional<PurchaseDealSourceTypeEnum> ofType(String type) {
        return Arrays.stream(values()).filter(item -> item.type.equals(type)).findFirst();
    }

    public static String nameOf(String type) {
        return ofType(type).map(PurchaseDealSourceTypeEnum::getName).orElse(null);
    }

}
