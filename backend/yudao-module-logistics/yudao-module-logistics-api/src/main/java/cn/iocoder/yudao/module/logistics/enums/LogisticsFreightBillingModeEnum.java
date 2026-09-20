package cn.iocoder.yudao.module.logistics.enums;

import cn.iocoder.yudao.framework.common.core.IntArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

/**
 * 承运合同的计费方式（V8 #75）。
 *
 * <p>CONTEXT.md「承运合同」：运价与计费方式的约定——**按车 / 按吨 / 按公里**。
 * 计费方式决定「计费量」这一列填什么：
 * <ul>
 *   <li>{@link #BY_TRIP} 按车：计费量 = 趟数（通常 1），运价是每趟多少钱；</li>
 *   <li>{@link #BY_WEIGHT} 按吨：计费量 = 吨数（回场复磅后的计量结果），运价是每吨多少钱；</li>
 *   <li>{@link #BY_KM} 按公里：计费量 = 公里数，运价是每公里多少钱。</li>
 * </ul>
 *
 * <p>计费方式在**合同上**定义、在**运单上快照**：合同改了计费方式，已汇集的运费不被回算
 *（历史账要留原样）。
 */
@AllArgsConstructor
@Getter
public enum LogisticsFreightBillingModeEnum implements IntArrayValuable {

    BY_TRIP(1, "按车"),
    BY_WEIGHT(2, "按吨"),
    BY_KM(3, "按公里");

    public static final int[] ARRAYS = Arrays.stream(values())
            .mapToInt(LogisticsFreightBillingModeEnum::getMode).toArray();

    private final Integer mode;
    private final String name;

    @Override
    public int[] array() {
        return ARRAYS;
    }

    public static Optional<LogisticsFreightBillingModeEnum> ofMode(Integer mode) {
        return Arrays.stream(values()).filter(item -> item.mode.equals(mode)).findFirst();
    }

    public static String nameOf(Integer mode) {
        return ofMode(mode).map(LogisticsFreightBillingModeEnum::getName).orElse(null);
    }

}
