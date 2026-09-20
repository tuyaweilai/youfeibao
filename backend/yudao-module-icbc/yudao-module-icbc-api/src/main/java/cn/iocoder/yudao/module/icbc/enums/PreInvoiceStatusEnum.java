package cn.iocoder.yudao.module.icbc.enums;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

/**
 * 预开票状态。
 *
 * <p>预下单只生成自然人确认页面、<strong>不产生发票</strong>。出售者确认后进入「预开票成功」，
 * 真正的票要等付款之后。对应工行的 {@code invoiceStatus}：
 * {@code 00} 初始 / {@code 01} 预开票中 / {@code 02} 预开票成功 / {@code 03} 预开票失败 / {@code 04} 预开票取消。
 */
public enum PreInvoiceStatusEnum {

    INITIAL(0, "00", "初始"),
    IN_PROGRESS(1, "01", "预开票中"),
    SUCCESS(2, "02", "预开票成功"),
    FAILED(3, "03", "预开票失败"),
    CANCELLED(4, "04", "预开票取消");

    /** 需人工关注的异常状态：预开票失败 */
    private static final Set<Integer> EXCEPTION = Set.of(FAILED.status);

    private final Integer status;
    private final String code;
    private final String name;

    PreInvoiceStatusEnum(Integer status, String code, String name) {
        this.status = status;
        this.code = code;
        this.name = name;
    }

    public Integer getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    /**
     * 工行状态码转平台状态值；未识别返回 {@link #INITIAL}。
     */
    public static Integer toStatus(String code) {
        return ofCode(code).map(PreInvoiceStatusEnum::getStatus).orElse(INITIAL.getStatus());
    }

    public static Optional<PreInvoiceStatusEnum> ofStatus(Integer status) {
        return Arrays.stream(values()).filter(item -> item.status.equals(status)).findFirst();
    }

    public static Optional<PreInvoiceStatusEnum> ofCode(String code) {
        return Arrays.stream(values()).filter(item -> item.code.equals(code)).findFirst();
    }

    /**
     * 是否为需要人工关注的异常态
     */
    public static boolean isException(Integer status) {
        return status != null && EXCEPTION.contains(status);
    }

    /**
     * 全部异常状态。工作台等聚合场景按集合一次性取数。
     */
    public static Set<Integer> exceptionStatuses() {
        return EXCEPTION;
    }
}
