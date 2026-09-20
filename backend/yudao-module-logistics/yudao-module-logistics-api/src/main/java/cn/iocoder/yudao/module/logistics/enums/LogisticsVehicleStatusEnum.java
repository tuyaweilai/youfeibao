package cn.iocoder.yudao.module.logistics.enums;

import cn.iocoder.yudao.framework.common.core.IntArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

/**
 * 车辆状态。
 *
 * <p>「运输中」由运输任务驱动（V2b 起：任务执行时车辆占用），不是手工挑的状态；
 * 本票只做建档与手工维护「可用 / 维护中」。
 */
@AllArgsConstructor
@Getter
public enum LogisticsVehicleStatusEnum implements IntArrayValuable {

    AVAILABLE(0, "可用"),
    IN_TRANSIT(1, "运输中"),
    MAINTENANCE(2, "维护中");

    public static final int[] ARRAYS = Arrays.stream(values())
            .mapToInt(LogisticsVehicleStatusEnum::getStatus).toArray();

    private final Integer status;
    private final String name;

    @Override
    public int[] array() {
        return ARRAYS;
    }

    public static Optional<LogisticsVehicleStatusEnum> ofStatus(Integer status) {
        return Arrays.stream(values()).filter(item -> item.status.equals(status)).findFirst();
    }

}
