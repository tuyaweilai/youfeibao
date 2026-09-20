package cn.iocoder.yudao.module.icbc.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * 采购订单履约异常的处理方式（#47 T09）。企业按异常类型各配一项。
 *
 * <p>两种方式覆盖需求原文的「拦截或提交授权审核」：
 * <ul>
 *   <li>{@link #BLOCK} 拦截：直接拒绝这次交货，不给出口；</li>
 *   <li>{@link #APPROVAL} 提交授权审核：先提交一张授权单，审核通过后按授权范围放行。</li>
 * </ul>
 * 一期不提供「一律放行」——异常要么被拦住，要么留下一条可追溯的授权记录，没有静默通过这一条路。
 */
public enum PurchaseDeliveryRuleEnum {

    BLOCK("BLOCK", "拦截"),
    APPROVAL("APPROVAL", "提交授权审核");

    private final String code;
    private final String name;

    PurchaseDeliveryRuleEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public static Optional<PurchaseDeliveryRuleEnum> ofCode(String code) {
        return Arrays.stream(values()).filter(item -> item.code.equals(code)).findFirst();
    }

    public static String nameOf(String code) {
        return ofCode(code).map(PurchaseDeliveryRuleEnum::getName).orElse(null);
    }

}
