package cn.iocoder.yudao.module.logistics.enums;

import cn.iocoder.yudao.framework.common.core.IntArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

/**
 * 停靠点状态（V5 #72）。
 *
 * <p>每个停靠点**独立推进**：到达提货点 / 交接完成由该停靠点自己的节点驱动，取消也只取消这一个点，
 * 不影响同一任务里的其它停靠点（#59 的 Implementation Decisions 第 4 条）。
 *
 * <p>「已完成」以**交接完成**为准（货在场站处装上了车），而不是「起运」——起运只是离开，
 * 可能因为等其它单还没走。
 */
@AllArgsConstructor
@Getter
public enum LogisticsTransportStopStatusEnum implements IntArrayValuable {

    PENDING(0, "待处理"),
    IN_PROGRESS(1, "进行中"),
    COMPLETED(2, "已完成"),
    CANCELLED(3, "已取消");

    public static final int[] ARRAYS = Arrays.stream(values())
            .mapToInt(LogisticsTransportStopStatusEnum::getStatus).toArray();

    /** 状态值 */
    private final Integer status;
    /** 状态名 */
    private final String name;

    @Override
    public int[] array() {
        return ARRAYS;
    }

    public static Optional<LogisticsTransportStopStatusEnum> ofStatus(Integer status) {
        return Arrays.stream(values()).filter(item -> item.status.equals(status)).findFirst();
    }

    public static String nameOf(Integer status) {
        return ofStatus(status).map(LogisticsTransportStopStatusEnum::getName).orElse(null);
    }

    /** 终态：已完成与已取消不再推进，也不再接受节点上报。 */
    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED;
    }

}
