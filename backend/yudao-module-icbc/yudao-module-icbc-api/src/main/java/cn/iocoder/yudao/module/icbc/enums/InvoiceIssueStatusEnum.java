package cn.iocoder.yudao.module.icbc.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * 开票状态（发票真正被开出）。
 *
 * <p>与「预开票状态」（{@link PreInvoiceStatusEnum}，工行 {@code invoiceStatus} 的 00–04）不是
 * 同一条线：预开票成功只代表出售者在工行页面确认了开票信息、票尚未开出。付款成功后工行才把
 * 报废产品收购发票真正开出来，并在通知（{@code notifyType=03}）里带上发票号码。
 *
 * <p>因此本状态由「付款成功」触发进入 {@link #ISSUING}，由「拿到发票号码」推进到 {@link #ISSUED}，
 * 开票失败时停在 {@link #FAILED} 并给出下一步动作。
 */
public enum InvoiceIssueStatusEnum {

    NOT_ISSUED(0, "未开票", "付款成功后自动开票；先确认该笔收购已完成付款"),
    ISSUING(1, "开票中", "开票处理中，可在「查状态」看到最新进度；长时间未变请联系工行"),
    ISSUED(2, "已开票", null),
    FAILED(3, "开票失败", "开票失败，请核对收购单与开票信息后重新发起开票申请，或联系工行核实");

    private final Integer status;
    private final String name;
    private final String nextAction;

    InvoiceIssueStatusEnum(Integer status, String name, String nextAction) {
        this.status = status;
        this.name = name;
        this.nextAction = nextAction;
    }

    public Integer getStatus() {
        return status;
    }

    public String getName() {
        return name;
    }

    /**
     * 异常 / 未完成时给用户看的下一步动作；正常终态返回 {@code null}
     */
    public String getNextAction() {
        return nextAction;
    }

    /**
     * 是否为「已开票」终态
     */
    public static boolean isIssued(Integer status) {
        return ISSUED.status.equals(status);
    }

    /**
     * 是否为需要人工关注的异常态
     */
    public static boolean isException(Integer status) {
        return FAILED.status.equals(status);
    }

    public static String nameOf(Integer status) {
        return ofStatus(status).map(InvoiceIssueStatusEnum::getName).orElse("未知");
    }

    public static String nextActionOf(Integer status) {
        return ofStatus(status).map(InvoiceIssueStatusEnum::getNextAction).orElse(null);
    }

    public static Optional<InvoiceIssueStatusEnum> ofStatus(Integer status) {
        return Arrays.stream(values()).filter(item -> item.status.equals(status)).findFirst();
    }
}
