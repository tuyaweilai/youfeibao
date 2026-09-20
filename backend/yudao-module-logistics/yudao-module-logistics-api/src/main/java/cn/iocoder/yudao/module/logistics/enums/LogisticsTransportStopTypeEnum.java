package cn.iocoder.yudao.module.logistics.enums;

import cn.iocoder.yudao.framework.common.core.IntArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

/**
 * 停靠点类型（V5 #72）。
 *
 * <p>停靠点是运输任务里的一个**提货或送货地点**，一个任务可有多个、对应不同出售者（集货）。
 * 与节点类型一样是固定枚举，不允许自由文本替代。
 *
 * @author 芋道源码
 */
@AllArgsConstructor
@Getter
public enum LogisticsTransportStopTypeEnum implements IntArrayValuable {

    PICKUP(1, "提货"),
    DELIVERY(2, "送货");

    public static final int[] ARRAYS = Arrays.stream(values())
            .mapToInt(LogisticsTransportStopTypeEnum::getType).toArray();

    /** 类型值 */
    private final Integer type;
    /** 类型名 */
    private final String name;

    @Override
    public int[] array() {
        return ARRAYS;
    }

    public static Optional<LogisticsTransportStopTypeEnum> ofType(Integer type) {
        return Arrays.stream(values()).filter(item -> item.type.equals(type)).findFirst();
    }

    public static String nameOf(Integer type) {
        return ofType(type).map(LogisticsTransportStopTypeEnum::getName).orElse(null);
    }

}
