package cn.iocoder.yudao.module.logistics.enums;

import cn.iocoder.yudao.framework.common.core.IntArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

/**
 * 司机状态。
 */
@AllArgsConstructor
@Getter
public enum LogisticsDriverStatusEnum implements IntArrayValuable {

    ACTIVE(0, "在职"),
    INACTIVE(1, "离职"),
    ON_LEAVE(2, "请假");

    public static final int[] ARRAYS = Arrays.stream(values())
            .mapToInt(LogisticsDriverStatusEnum::getStatus).toArray();

    private final Integer status;
    private final String name;

    @Override
    public int[] array() {
        return ARRAYS;
    }

    public static Optional<LogisticsDriverStatusEnum> ofStatus(Integer status) {
        return Arrays.stream(values()).filter(item -> item.status.equals(status)).findFirst();
    }

}
