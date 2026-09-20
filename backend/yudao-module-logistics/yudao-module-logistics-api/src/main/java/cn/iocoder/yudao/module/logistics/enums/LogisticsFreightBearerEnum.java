package cn.iocoder.yudao.module.logistics.enums;

import cn.iocoder.yudao.framework.common.core.IntArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

/**
 * 费用的承担方（V8 #75）。
 *
 * <p>用在两处：
 * <ul>
 *   <li>承运合同上的**附加费**——附加费由谁承担，决定它进不进「应付给承运商的运费」；</li>
 *   <li>运输费用（路桥 / 燃油 / 其他）——记住这笔钱**实际是谁出的**（CONTEXT.md「运费」的
 *       「按实际对象处理」）：自有车出的路桥与燃油是本企业的成本，不能假装成给承运商的运费。</li>
 * </ul>
 */
@AllArgsConstructor
@Getter
public enum LogisticsFreightBearerEnum implements IntArrayValuable {

    CARRIER(1, "承运商承担"),
    COMPANY(2, "本企业承担");

    public static final int[] ARRAYS = Arrays.stream(values())
            .mapToInt(LogisticsFreightBearerEnum::getBearer).toArray();

    private final Integer bearer;
    private final String name;

    @Override
    public int[] array() {
        return ARRAYS;
    }

    public static Optional<LogisticsFreightBearerEnum> ofBearer(Integer bearer) {
        return Arrays.stream(values()).filter(item -> item.bearer.equals(bearer)).findFirst();
    }

    public static String nameOf(Integer bearer) {
        return ofBearer(bearer).map(LogisticsFreightBearerEnum::getName).orElse(null);
    }

}
