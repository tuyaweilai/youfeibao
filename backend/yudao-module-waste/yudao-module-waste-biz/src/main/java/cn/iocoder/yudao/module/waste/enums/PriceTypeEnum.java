package cn.iocoder.yudao.module.waste.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 价格类型枚举
 *
 * @author 芋道源码
 */
@AllArgsConstructor
@Getter
public enum PriceTypeEnum {

    FIXED(1, "固定价格"),
    FLOATING(2, "浮动价格"),
    TIERED(3, "阶梯价格"),
    NEGOTIABLE(4, "议价");

    /**
     * 类型值
     */
    private final Integer type;
    /**
     * 类型名
     */
    private final String name;

    public static PriceTypeEnum valueOf(Integer type) {
        for (PriceTypeEnum typeEnum : values()) {
            if (typeEnum.getType().equals(type)) {
                return typeEnum;
            }
        }
        return null;
    }

} 