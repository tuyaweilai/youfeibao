package cn.iocoder.yudao.module.waste.enums;

import cn.iocoder.yudao.framework.common.core.IntArrayValuable;

import java.util.Arrays;

/**
 * 分配方式枚举
 *
 * @author 芋道源码
 */
public enum AssignmentTypeEnum implements IntArrayValuable {

    AUTO(0, "自动分配"),
    MANUAL(1, "手动指定");

    public static final int[] ARRAY = Arrays.stream(values()).mapToInt(AssignmentTypeEnum::getType).toArray();

    /**
     * 类型值
     */
    private final Integer type;
    /**
     * 类型名
     */
    private final String name;

    AssignmentTypeEnum(Integer type, String name) {
        this.type = type;
        this.name = name;
    }

    @Override
    public int[] array() {
        return ARRAY;
    }

    public Integer getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public static AssignmentTypeEnum valueOf(Integer type) {
        return Arrays.stream(values())
                .filter(typeEnum -> typeEnum.getType().equals(type))
                .findFirst()
                .orElse(null);
    }

} 