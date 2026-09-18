package cn.iocoder.yudao.module.waste.enums;

import cn.iocoder.yudao.framework.common.core.IntArrayValuable;

import java.util.Arrays;

/**
 * 业务模式枚举
 *
 * @author 芋道源码
 */
public enum BusinessModeEnum implements IntArrayValuable {

    INDEPENDENT(0, "独立运营"),
    PLATFORM_BIDDING(1, "平台竞价"),
    HYBRID(2, "混合模式");

    public static final int[] ARRAY = Arrays.stream(values()).mapToInt(BusinessModeEnum::getMode).toArray();

    /**
     * 模式值
     */
    private final Integer mode;
    /**
     * 模式名
     */
    private final String name;

    BusinessModeEnum(Integer mode, String name) {
        this.mode = mode;
        this.name = name;
    }

    @Override
    public int[] array() {
        return ARRAY;
    }

    public Integer getMode() {
        return mode;
    }

    public String getName() {
        return name;
    }

    public static BusinessModeEnum valueOf(Integer mode) {
        return Arrays.stream(values())
                .filter(modeEnum -> modeEnum.getMode().equals(mode))
                .findFirst()
                .orElse(null);
    }

} 