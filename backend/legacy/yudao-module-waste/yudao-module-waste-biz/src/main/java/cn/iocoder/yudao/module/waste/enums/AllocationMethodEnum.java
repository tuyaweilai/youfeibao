package cn.iocoder.yudao.module.waste.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 分摊方法枚举
 *
 * @author 芋道源码
 */
@AllArgsConstructor
@Getter
public enum AllocationMethodEnum {

    BY_WEIGHT(1, "按重量分摊"),
    BY_QUANTITY(2, "按数量分摊"),
    BY_AMOUNT(3, "按金额分摊"),
    AVERAGE(4, "平均分摊"),
    MANUAL(5, "人工分摊");

    /**
     * 方法值
     */
    private final Integer method;
    /**
     * 方法名
     */
    private final String name;

    public static AllocationMethodEnum valueOf(Integer method) {
        for (AllocationMethodEnum methodEnum : values()) {
            if (methodEnum.getMethod().equals(method)) {
                return methodEnum;
            }
        }
        return null;
    }

} 