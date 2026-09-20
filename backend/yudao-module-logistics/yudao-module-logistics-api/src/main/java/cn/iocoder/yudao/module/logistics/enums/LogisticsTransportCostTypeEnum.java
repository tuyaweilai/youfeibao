package cn.iocoder.yudao.module.logistics.enums;

import cn.iocoder.yudao.framework.common.core.IntArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

/**
 * 运输费用（内部成本）的类型（V8 #75）。
 *
 * <p>这里记的是**本企业自己的运输成本**（路桥、燃油、其他），不是承运商运费：
 * 承运商运费是「付给承运商的应付」，走 {@code logistics_freight_order}；本枚举走
 * {@code logistics_transport_cost}，两者**不互相污染**（CONTEXT.md「运费」）。
 *
 * <p>自有车不会虚造承运商运费，但它真实发生的路桥与燃油要按**实际承担方**
 *（{@link LogisticsFreightBearerEnum}）记下来。
 */
@AllArgsConstructor
@Getter
public enum LogisticsTransportCostTypeEnum implements IntArrayValuable {

    TOLL(1, "路桥费"),
    FUEL(2, "燃油费"),
    OTHER(3, "其他");

    public static final int[] ARRAYS = Arrays.stream(values())
            .mapToInt(LogisticsTransportCostTypeEnum::getType).toArray();

    private final Integer type;
    private final String name;

    @Override
    public int[] array() {
        return ARRAYS;
    }

    public static Optional<LogisticsTransportCostTypeEnum> ofType(Integer type) {
        return Arrays.stream(values()).filter(item -> item.type.equals(type)).findFirst();
    }

    public static String nameOf(Integer type) {
        return ofType(type).map(LogisticsTransportCostTypeEnum::getName).orElse(null);
    }

}
