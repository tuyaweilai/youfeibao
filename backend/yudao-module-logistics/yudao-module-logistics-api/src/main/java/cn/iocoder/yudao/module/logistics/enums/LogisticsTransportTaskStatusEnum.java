package cn.iocoder.yudao.module.logistics.enums;

import cn.iocoder.yudao.framework.common.core.IntArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

/**
 * 运输任务状态（V2b #78）。
 *
 * <p>一期「一个任务 = 一车 + 一司机 + 一次执行」（ADR 0032），所以状态是**一次执行**的进度，
 * 不是「一趟活里各停靠点」的进度——停靠点级进度归 V5（#72）多停靠点集货。
 *
 * <p>推进规则（状态机写在 {@link #canTransitTo} 里，改规则请连测试一起改）：
 * <ul>
 *   <li>待分配 →（派车：给车给人）已分配 →（司机接单）已接单 →（上报起运）执行中 →（调度确认）已完成</li>
 *   <li>任意非终态 →（取消，必填原因）已取消</li>
 * </ul>
 *
 * <p>「已接单」在 V2c 由司机端点，在 V2b 由调度在 PC 上代记（本票还没有司机端）。
 * 异常（车辆故障、道路封闭等）**不是状态**，是独立标记，归 V4（#71）。
 */
@AllArgsConstructor
@Getter
public enum LogisticsTransportTaskStatusEnum implements IntArrayValuable {

    PENDING(0, "待分配"),
    ASSIGNED(1, "已分配"),
    ACCEPTED(2, "已接单"),
    IN_TRANSIT(3, "执行中"),
    COMPLETED(4, "已完成"),
    CANCELLED(5, "已取消");

    public static final int[] ARRAYS = Arrays.stream(values())
            .mapToInt(LogisticsTransportTaskStatusEnum::getStatus).toArray();

    private final Integer status;
    private final String name;

    @Override
    public int[] array() {
        return ARRAYS;
    }

    public static Optional<LogisticsTransportTaskStatusEnum> ofStatus(Integer status) {
        return Arrays.stream(values()).filter(item -> item.status.equals(status)).findFirst();
    }

    public static String nameOf(Integer status) {
        return ofStatus(status).map(LogisticsTransportTaskStatusEnum::getName).orElse(null);
    }

    /**
     * 终态：已完成与已取消不再推进（也不能再取消）。
     */
    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED;
    }

    /**
     * 状态机：能不能从当前状态走到 {@code target}。
     *
     * <p>同状态视为允许（幂等重放不报错），这在离线补传与重复点击下是必需的。
     */
    public boolean canTransitTo(LogisticsTransportTaskStatusEnum target) {
        if (target == null) {
            return false;
        }
        if (this == target) {
            return true;
        }
        if (isTerminal()) {
            return false;
        }
        switch (this) {
            case PENDING:
                return target == ASSIGNED || target == CANCELLED;
            case ASSIGNED:
                return target == ACCEPTED || target == IN_TRANSIT || target == COMPLETED
                        || target == CANCELLED;
            case ACCEPTED:
                return target == IN_TRANSIT || target == COMPLETED || target == CANCELLED;
            case IN_TRANSIT:
                return target == COMPLETED || target == CANCELLED;
            default:
                return false;
        }
    }

}
