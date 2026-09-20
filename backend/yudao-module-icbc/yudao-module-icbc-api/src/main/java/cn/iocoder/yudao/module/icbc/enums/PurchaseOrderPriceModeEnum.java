package cn.iocoder.yudao.module.icbc.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * 采购订单明细的定价方式（#46 T08，用户故事 18）。
 *
 * <ul>
 *     <li>{@link #FIXED}：固定单价，直接用明细上的参考单价；</li>
 *     <li>{@link #BY_DELIVERY_DATE}：按交货日价格表，取「交货日不晚于当日」的最新一条价格表价，
 *     价格表没有覆盖到的日期回退到明细上的参考单价。</li>
 * </ul>
 *
 * <p>每次成交都另落一条价格快照与调整原因（{@code icbc_purchase_order_deal}），
 * 事后能对得上账。
 */
public enum PurchaseOrderPriceModeEnum {

    FIXED(1, "固定单价"),
    BY_DELIVERY_DATE(2, "按交货日价格表");

    private final Integer mode;
    private final String name;

    PurchaseOrderPriceModeEnum(Integer mode, String name) {
        this.mode = mode;
        this.name = name;
    }

    public Integer getMode() {
        return mode;
    }

    public String getName() {
        return name;
    }

    public static Optional<PurchaseOrderPriceModeEnum> ofMode(Integer mode) {
        return Arrays.stream(values()).filter(item -> item.mode.equals(mode)).findFirst();
    }

    public static String nameOf(Integer mode) {
        return ofMode(mode).map(PurchaseOrderPriceModeEnum::getName).orElse(null);
    }

}
