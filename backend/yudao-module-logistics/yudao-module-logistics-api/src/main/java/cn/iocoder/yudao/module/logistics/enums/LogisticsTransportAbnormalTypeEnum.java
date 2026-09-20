package cn.iocoder.yudao.module.logistics.enums;

import cn.iocoder.yudao.framework.common.core.IntArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

/**
 * 运输异常类型（V4 #71）。
 *
 * <p><b>异常是独立标记，不是任务状态</b>（#59 的 Implementation Decisions 第 17 条）：
 * 车辆故障、道路封闭这些事只标在运输事实（节点）上，任务状态机照旧只走
 * 「待分配 → 已分配 → 已接单 → 执行中 → 已完成」。把异常塞进状态机会让每个统计口径都要额外
 * 判断「这单到底跑完没有」。
 *
 * <p>与节点类型一样是**固定枚举**，不允许用自由文本替代——统计与门禁都依赖它。
 *
 * @author 芋道源码
 */
@AllArgsConstructor
@Getter
public enum LogisticsTransportAbnormalTypeEnum implements IntArrayValuable {

    VEHICLE_BREAKDOWN(1, "车辆故障"),
    TRAFFIC_ACCIDENT(2, "交通事故"),
    WEATHER_DELAY(3, "天气延误"),
    ROAD_CLOSED(4, "道路封闭"),
    CARGO_DAMAGED(5, "货物损坏"),
    COUNTERPARTY_ABSENT(6, "对方不在"),
    WRONG_ADDRESS(7, "地址错误"),
    OTHER(8, "其他");

    public static final int[] ARRAYS = Arrays.stream(values())
            .mapToInt(LogisticsTransportAbnormalTypeEnum::getType).toArray();

    /** 类型值 */
    private final Integer type;
    /** 类型名 */
    private final String name;

    @Override
    public int[] array() {
        return ARRAYS;
    }

    public static Optional<LogisticsTransportAbnormalTypeEnum> ofType(Integer type) {
        return Arrays.stream(values()).filter(item -> item.type.equals(type)).findFirst();
    }

    public static String nameOf(Integer type) {
        return ofType(type).map(LogisticsTransportAbnormalTypeEnum::getName).orElse(null);
    }

}
