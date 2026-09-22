package cn.iocoder.yudao.module.icbc.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * 收购进度（ADR 0038）。
 *
 * <p>它回答的是收货员与出售者都要问的那一句：「这一笔卡在哪一步、下一步是谁的事？」判定依据
 * **全部**来自该笔的开票单（{@code icbc_invoice_order} 的自然人确认 / 预开票 / 付款 / 开票
 * 四条状态线），由 {@code AcquisitionProgressService} 单点派生。
 *
 * <p><b>这不是一个可以手写的状态</b>：{@code icbc_acquisition.status} 只是派生结果的缓存，
 * 任何业务代码不得直接写它。原因见 ADR 0038——手写的那个版本里，预下单返回即写「待付款」
 * （自然人此时还没确认），而「已开票」没有任何调用方、永远不会出现，两处都在骗人。
 *
 * <p><b>档位与状态码的对应关系是刻意错开的</b>：0 / 1 / 2 / 3 / 9 保持历史取值不变
 * （「待自然人确认」是新加的 4），以免既有数据被读成另一个意思；档位的先后不靠码值大小表达。
 *
 * <p>异常（预开票失败 / 开票失败 / 付款失败与退回 / 缴税异常 / 上传失败）**不占用档位**，
 * 以叠加标注呈现——「已付款」不许把「开票失败」盖住（ADR 0021）。
 */
public enum AcquisitionStatusEnum {

    /** 已登记：现场要件齐备，收购单已落库，尚未发起开票申请（没有开票单） */
    REGISTERED(0, "已登记", "等开票员发起开票申请；开票前出售者要先在结算单上确认计量与计价事实"),
    /** 待自然人确认：已发起预下单，出售者还没在工行页面上确认（或预开票仍在途） */
    WAITING_SELLER_CONFIRM(4, "待自然人确认", "等出售者本人在工行页面确认开票信息；未确认不能付款"),
    /** 待付款：预开票成功，等回收企业把货款付给出售者 */
    PENDING_PAYMENT(1, "待付款", "等回收企业付款；工行付款成功即开票"),
    /** 已付款：货款已通过工行公对私结算到出售者本人银行卡，票尚未开出 */
    PAID(2, "已付款", "货款已付出，等工行开出并上传发票"),
    /** 已开票：反向发票已开具（原件可下载） */
    INVOICED(3, "已开票", "发票已开出，可下载发票原件"),
    /** 已作废：登记有误或交易未成立（必带原因，对自然人可见）；或工行侧预开票被取消 */
    CANCELLED(9, "已作废", "已作废，不再开票付款");

    private final Integer status;
    private final String name;
    private final String nextStep;

    AcquisitionStatusEnum(Integer status, String name, String nextStep) {
        this.status = status;
        this.name = name;
        this.nextStep = nextStep;
    }

    public Integer getStatus() {
        return status;
    }

    public String getName() {
        return name;
    }

    public String getNextStep() {
        return nextStep;
    }

    public boolean is(Integer status) {
        return this.status.equals(status);
    }

    public static Optional<AcquisitionStatusEnum> ofStatus(Integer status) {
        return Arrays.stream(values())
                .filter(item -> item.status.equals(status))
                .findFirst();
    }

    public static String nameOf(Integer status) {
        return ofStatus(status).map(AcquisitionStatusEnum::getName).orElse("未知");
    }

    public static String nextStepOf(Integer status) {
        return ofStatus(status).map(AcquisitionStatusEnum::getNextStep).orElse(null);
    }

}
