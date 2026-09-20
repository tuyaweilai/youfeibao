package cn.iocoder.yudao.module.logistics.enums;

import cn.iocoder.yudao.framework.common.core.IntArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

/**
 * 承运商运费单的状态（V8 #75）。
 *
 * <p>状态机只有一条路，且**付款不出平台**（ADR 0006：不接对公付款通道）：
 * <pre>
 *   待确认应付 --确认应付--> 已确认应付 --登记外部付款凭证--> 已登记付款凭证
 * </pre>
 *
 * <p>「差异」不是状态，是运单上的两个数（应有应付 {@code expectedAmount} 与确认应付
 * {@code actualAmount}）加一条原因 {@code varianceReason}——差异**不抹平**：确认了也留着，
 * 事后说得清差在哪。
 */
@AllArgsConstructor
@Getter
public enum LogisticsFreightStatusEnum implements IntArrayValuable {

    PENDING_CONFIRM(0, "待确认应付"),
    CONFIRMED(1, "已确认应付"),
    VOUCHER_REGISTERED(2, "已登记付款凭证");

    public static final int[] ARRAYS = Arrays.stream(values())
            .mapToInt(LogisticsFreightStatusEnum::getStatus).toArray();

    private final Integer status;
    private final String name;

    @Override
    public int[] array() {
        return ARRAYS;
    }

    public static Optional<LogisticsFreightStatusEnum> ofStatus(Integer status) {
        return Arrays.stream(values()).filter(item -> item.status.equals(status)).findFirst();
    }

    public static String nameOf(Integer status) {
        return ofStatus(status).map(LogisticsFreightStatusEnum::getName).orElse(null);
    }

}
