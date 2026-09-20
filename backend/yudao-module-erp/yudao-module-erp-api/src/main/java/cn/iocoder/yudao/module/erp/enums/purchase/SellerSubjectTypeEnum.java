package cn.iocoder.yudao.module.erp.enums.purchase;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * 卖方主体类型枚举（六态）。
 *
 * <p>依据 ADR 0029，采购侧的交易对方按「是否属于自然人」分成两条取票链路（不是「有没有营业执照」）：
 * <ul>
 *     <li>{@link #NATURAL} 走反向开票，档案在 {@code icbc_payee_info}；</li>
 *     <li>其余五类走「对方开票、我们收票」的进项收票链路，档案在 {@code erp_supplier}。</li>
 * </ul>
 *
 * <p>放在 {@code erp-api} 是因为 {@code erp_supplier} 在 ERP 侧，而 {@code icbc} 只能依赖
 * {@code erp-api}（依赖方向恒为 icbc → erp，见 ADR 0025）；枚举落在 api 层两端都能用。
 */
@RequiredArgsConstructor
@Getter
public enum SellerSubjectTypeEnum implements ArrayValuable<Integer> {

    NATURAL(1, "自然人出售者", true),
    INDIVIDUAL_BUSINESS(2, "个体工商户", false),
    SOLE_PROPRIETORSHIP(3, "个人独资企业", false),
    PARTNERSHIP(4, "合伙企业", false),
    ENTERPRISE_LEGAL(5, "企业法人", false),
    FARMER_COOPERATIVE(6, "农民专业合作社", false),
    ;

    public static final Integer[] ARRAYS = Arrays.stream(values())
            .map(SellerSubjectTypeEnum::getType).toArray(Integer[]::new);

    /**
     * 主体类型
     */
    private final Integer type;
    /**
     * 主体类型名
     */
    private final String name;
    /**
     * 是否属于自然人 —— 唯一的判定规则，决定走反向开票还是进项收票。
     */
    private final boolean natural;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

    public static SellerSubjectTypeEnum valueOf(Integer type) {
        return Arrays.stream(values()).filter(e -> e.getType().equals(type)).findFirst().orElse(null);
    }

    public static String nameOf(Integer type) {
        SellerSubjectTypeEnum subjectType = valueOf(type);
        return subjectType != null ? subjectType.getName() : null;
    }

    /**
     * 判定某个主体类型是否属于自然人。未知类型返回 {@code false}（宁可不认，也不误放进反向开票）。
     */
    public static boolean isNaturalType(Integer type) {
        SellerSubjectTypeEnum subjectType = valueOf(type);
        return subjectType != null && subjectType.isNatural();
    }

}
