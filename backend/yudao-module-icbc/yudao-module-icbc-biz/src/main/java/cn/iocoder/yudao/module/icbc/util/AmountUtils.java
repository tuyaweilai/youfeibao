package cn.iocoder.yudao.module.icbc.util;

import cn.hutool.core.util.StrUtil;

import java.math.BigDecimal;

/**
 * 工行报文金额解析工具。
 *
 * <p>工行报文里的金额是字符串（可能为空、可能带币种前缀等脏数据），平台统一用
 * {@link #parse(String)} 兜底：解析不出就当没给，由调用方决定是否回退到缺省值。
 */
public final class AmountUtils {

    private AmountUtils() {
    }

    /**
     * 把工行返回的金额字符串解析成 {@link BigDecimal}；空串或非法值返回 {@code null}。
     */
    public static BigDecimal parse(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

}
