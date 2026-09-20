package cn.iocoder.yudao.module.logistics.enums;

import cn.iocoder.yudao.framework.common.core.IntArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 司机状态枚举
 *
 * @author 芋道源码
 */
@AllArgsConstructor
@Getter
public enum DriverStatusEnum implements IntArrayValuable {

    ACTIVE(0, "在职"),
    INACTIVE(1, "离职"),
    ON_LEAVE(2, "请假");

    public static final int[] ARRAYS = Arrays.stream(values()).mapToInt(DriverStatusEnum::getStatus).toArray();

    /**
     * 状态值
     */
    private final Integer status;
    /**
     * 状态名
     */
    private final String name;

    @Override
    public int[] array() {
        return ARRAYS;
    }

} 