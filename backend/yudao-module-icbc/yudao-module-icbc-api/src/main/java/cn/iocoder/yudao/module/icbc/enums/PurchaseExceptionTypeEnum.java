package cn.iocoder.yudao.module.icbc.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * 采购订单履约异常类型（#47 T09，AC「超量、过期、跨场站交货按企业配置拦截或提交授权审核」）。
 *
 * <p>三类异常都可以由企业配置成**拦截**（{@link PurchaseDeliveryRuleEnum#BLOCK}）或
 * **提交授权审核**（{@link PurchaseDeliveryRuleEnum#APPROVAL}）。审核通过后产生的授权单
 * （{@code icbc_purchase_exception}）是这类交货继续办理的依据；它只放宽被授权的这一件事，
 * 不等于把订单恢复成有效采购依据。
 */
public enum PurchaseExceptionTypeEnum {

    /** 超量：本次交货后累计验收量会超过计划量。授权按「追加量」计。 */
    OVER_QUANTITY("OVER_QUANTITY", "超量交货",
            "本次交货后累计验收量超过明细计划量。授权时给出可追加的超量上限，超出的部分仍被拦。"),

    /** 过期：订单结束日期已过。授权按「有效期」计。 */
    EXPIRED("EXPIRED", "过期交货",
            "订单的执行结束日期已过。授权给出有效期，有效期内的交货按已授权放行。"),

    /** 跨场站：本次交货场站不是订单的执行场站。授权按「场站」计。 */
    CROSS_STATION("CROSS_STATION", "跨场站交货",
            "本次交货场站不是订单的执行场站。授权必须指明允许交货的场站，只对该场站放行。");

    private final String code;
    private final String name;
    private final String definition;

    PurchaseExceptionTypeEnum(String code, String name, String definition) {
        this.code = code;
        this.name = name;
        this.definition = definition;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    /**
     * 口径说明：什么算这类异常、授权怎么放宽。
     */
    public String getDefinition() {
        return definition;
    }

    public static Optional<PurchaseExceptionTypeEnum> ofCode(String code) {
        return Arrays.stream(values()).filter(item -> item.code.equals(code)).findFirst();
    }

    public static String nameOf(String code) {
        return ofCode(code).map(PurchaseExceptionTypeEnum::getName).orElse(null);
    }

}
