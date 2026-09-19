package cn.iocoder.yudao.module.icbc.util;

import cn.hutool.core.util.StrUtil;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * 工行报文时间解析工具。
 *
 * <p>工行回传的时间在不同接口 / 通知里格式不完全一致（{@code yyyy-MM-dd HH:mm:ss}、{@code yyyy-MM-dd}
 * 或 ISO），平台统一宽容解析：解析不出就当没给，不阻断其余字段的收敛。
 */
public final class IcbcTimeUtils {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private IcbcTimeUtils() {
    }

    /**
     * 宽容解析工行回传的时间，解析失败返回 {@code null}。
     */
    public static LocalDateTime parse(String text) {
        if (StrUtil.isBlank(text)) {
            return null;
        }
        String value = text.trim();
        for (DateTimeFormatter formatter : new DateTimeFormatter[]{DATE_TIME_FORMATTER, DATE_FORMATTER}) {
            try {
                return LocalDateTime.parse(value, formatter);
            } catch (DateTimeParseException ignored) {
                // 换下一种格式
            }
        }
        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }
}
