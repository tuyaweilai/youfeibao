package cn.iocoder.yudao.module.icbc.enums;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

/**
 * 付方支付状态。
 *
 * <p>资金流不是「成功 / 失败」两个值：工行的 {@code payStatus} 有十一个状态，平台把它们收敛成
 * 这里的一组平台状态，既保留「转账在路上」的中间态，也让冲正、退汇、部分成功这类异常可见。
 * 对应的工行字典见 issue #9 / {@code docs/research/2026-09-18-工行融e聚反向开票接口研究.md}：
 *
 * <pre>
 * -1 待通知 / 00 初始 / 01 处理中 / 02 成功 / 03 失败 / 04 订单关闭 /
 * 05 冲正 / 06 已退汇 / 07 他行已扣款本行未入账 / 12 已支付待签收（票据支付）/ 25 部分成功
 * </pre>
 */
public enum PaymentStatusEnum {

    /** 待支付：已生成支付页面，等待回收企业授权 */
    PENDING(0, "待支付"),
    /** 支付中：工行处理中 */
    PAYING(1, "支付中"),
    /** 支付成功：货款已到出售者本人银行卡，转账回单可归档 */
    SUCCESS(2, "支付成功"),
    /** 支付失败 */
    FAILED(3, "支付失败"),
    /** 订单关闭 */
    CLOSED(4, "订单关闭"),
    /** 已冲正 */
    REVERSED(5, "已冲正"),
    /** 已退汇 */
    REFUNDED(6, "已退汇"),
    /** 他行已扣款本行未入账 */
    OTHER_BANK_DEBITED(7, "他行已扣款本行未入账"),
    /** 已支付待签收（票据支付） */
    PAID_PENDING_RECEIPT(8, "已支付待签收"),
    /** 部分成功：实际到账金额小于应付金额 */
    PARTIAL_SUCCESS(9, "部分成功");

    /** 可重新发起的异常状态：失败 / 关闭 / 冲正 / 退汇 / 部分成功 */
    private static final Set<Integer> RE_INITIABLE = Set.of(
            FAILED.status, CLOSED.status, REVERSED.status, REFUNDED.status, PARTIAL_SUCCESS.status);

    /** 异常状态：需要人工关注或重新发起 */
    private static final Set<Integer> EXCEPTION = Set.of(
            FAILED.status, CLOSED.status, REVERSED.status, REFUNDED.status,
            OTHER_BANK_DEBITED.status, PARTIAL_SUCCESS.status);

    private final Integer status;
    private final String name;

    PaymentStatusEnum(Integer status, String name) {
        this.status = status;
        this.name = name;
    }

    public Integer getStatus() {
        return status;
    }

    public String getName() {
        return name;
    }

    /**
     * 工行 {@code payStatus} 码转平台状态；未识别按「待支付」处理，等待通知或预查询再次收敛。
     */
    public static Integer toStatus(String icbcPayStatus) {
        if (icbcPayStatus == null) {
            return PENDING.status;
        }
        switch (icbcPayStatus) {
            case "-1":
            case "00":
                return PENDING.status;
            case "01":
                return PAYING.status;
            case "02":
                return SUCCESS.status;
            case "03":
                return FAILED.status;
            case "04":
                return CLOSED.status;
            case "05":
                return REVERSED.status;
            case "06":
                return REFUNDED.status;
            case "07":
                return OTHER_BANK_DEBITED.status;
            case "12":
                return PAID_PENDING_RECEIPT.status;
            case "25":
                return PARTIAL_SUCCESS.status;
            default:
                return PENDING.status;
        }
    }

    /**
     * 是否允许重新发起付款：失败 / 关闭 / 冲正 / 退汇 / 部分成功可再发起；
     * 成功与「在途 / 待签收」不允许，避免重复指令造成二次付款。
     */
    public static boolean isReInitiable(Integer status) {
        return status != null && RE_INITIABLE.contains(status);
    }

    public static boolean isSuccess(Integer status) {
        return SUCCESS.status.equals(status);
    }

    public static boolean isException(Integer status) {
        return status != null && EXCEPTION.contains(status);
    }

    /**
     * 全部异常状态。工作台等聚合场景按集合一次性取数，不逐个状态拼条件。
     */
    public static Set<Integer> exceptionStatuses() {
        return EXCEPTION;
    }

    public static String nameOf(Integer status) {
        return ofStatus(status).map(PaymentStatusEnum::getName).orElse("未知");
    }

    public static Optional<PaymentStatusEnum> ofStatus(Integer status) {
        return Arrays.stream(values()).filter(item -> item.status.equals(status)).findFirst();
    }

}
