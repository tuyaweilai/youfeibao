package cn.iocoder.yudao.module.waste.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 报价模式枚举
 *
 * @author 芋道源码
 */
@AllArgsConstructor
@Getter
public enum QuotationModeEnum {

    SINGLE_QUOTE(1, "单次报价"),
    MULTIPLE_QUOTE(2, "多次报价"),
    REAL_TIME_QUOTE(3, "实时报价"),
    BATCH_QUOTE(4, "批量报价");

    /**
     * 模式值
     */
    private final Integer mode;
    /**
     * 模式名
     */
    private final String name;

    public static QuotationModeEnum valueOf(Integer mode) {
        for (QuotationModeEnum modeEnum : values()) {
            if (modeEnum.getMode().equals(mode)) {
                return modeEnum;
            }
        }
        return null;
    }

} 