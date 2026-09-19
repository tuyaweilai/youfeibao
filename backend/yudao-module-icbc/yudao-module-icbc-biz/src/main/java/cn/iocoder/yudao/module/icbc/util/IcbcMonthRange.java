package cn.iocoder.yudao.module.icbc.util;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * 平台里的「月」口径：{@code yyyy-MM} 与它对应的 {@code [start, end)} 时间区间。
 *
 * <p>额度台账、代办税费申报、计费计量都按「开票日期落在同一个月」判定，口径只有这一处。
 */
public final class IcbcMonthRange {

    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    private final String periodMonth;
    private final LocalDateTime start;
    private final LocalDateTime end;

    private IcbcMonthRange(String periodMonth, LocalDateTime start, LocalDateTime end) {
        this.periodMonth = periodMonth;
        this.start = start;
        this.end = end;
    }

    /**
     * 解析月份。格式不合法时抛出 {@link DateTimeParseException} 或 {@link NullPointerException}，
     * 由调用方翻译成各自的业务错误码。
     */
    public static IcbcMonthRange of(String periodMonth) {
        YearMonth month = YearMonth.parse(periodMonth, MONTH_FORMATTER);
        LocalDateTime start = month.atDay(1).atStartOfDay();
        return new IcbcMonthRange(periodMonth, start, start.plusMonths(1));
    }

    public String getPeriodMonth() {
        return periodMonth;
    }

    /**
     * 当月一日零点，闭区间下界。
     */
    public LocalDateTime getStart() {
        return start;
    }

    /**
     * 月末的次日零点，开区间上界。
     */
    public LocalDateTime getEnd() {
        return end;
    }

}
