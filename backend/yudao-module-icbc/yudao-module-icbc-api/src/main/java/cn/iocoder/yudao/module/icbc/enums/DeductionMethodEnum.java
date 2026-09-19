package cn.iocoder.yudao.module.icbc.enums;

import cn.hutool.core.util.StrUtil;

import java.util.Arrays;
import java.util.Optional;

/**
 * 扣杂录法（ADR 0019）。
 *
 * <p>扣杂是收货员与出售者当场最容易起争执的一项，因此独立成字段：只存**原始录法**（按重量或按比例）
 * 与换算后的结算重量，不用自由文本覆盖结算重量（那样会和磅单对不上）。
 */
public enum DeductionMethodEnum {

    /** 按重量：扣杂值直接就是重量（与毛重、皮重同单位） */
    WEIGHT("WEIGHT", "按重量"),
    /** 按比例：扣杂值是比例（0~1），换算重量 = 净重 × 比例 */
    RATIO("RATIO", "按比例");

    private final String method;
    private final String name;

    DeductionMethodEnum(String method, String name) {
        this.method = method;
        this.name = name;
    }

    public String getMethod() {
        return method;
    }

    public String getName() {
        return name;
    }

    /**
     * 历史数据兼容：未录方法时一律按「按重量」处理（扣杂 = 0 时两种录法等价）。
     */
    public static DeductionMethodEnum ofMethod(String method) {
        if (StrUtil.isBlank(method)) {
            return WEIGHT;
        }
        return Arrays.stream(values())
                .filter(item -> item.method.equalsIgnoreCase(method))
                .findFirst()
                .orElse(WEIGHT);
    }

    public static Optional<DeductionMethodEnum> of(String method) {
        return Arrays.stream(values()).filter(item -> item.method.equalsIgnoreCase(method)).findFirst();
    }

}
