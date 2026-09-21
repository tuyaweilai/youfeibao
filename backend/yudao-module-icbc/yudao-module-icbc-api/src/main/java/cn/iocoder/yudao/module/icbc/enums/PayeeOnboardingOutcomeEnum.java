package cn.iocoder.yudao.module.icbc.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 收方入驻的状态：**只有审核一条线**。
 *
 * <p>收方入驻改走数据接口后（ADR 0035），工行不再返回开户状态与电子账户账号，于是
 * 原先「开户 × 审核」四种组合的枚举收敛成三态：已受理待审核 / 通过 / 拒绝。
 *
 * <p>数据接口是同步受理、异步审核的：这一次调用成功只代表工行收下了申请（{@link #PENDING}），
 * 结论要等通知或查询；所以 {@link #PENDING} 必须保留——去掉它就等于假设同步返回即通过。
 *
 * <p>结算仍走公对私直付银行卡（ADR 0010），这里的状态只讲**收方登记是否可用**。
 */
@Getter
@AllArgsConstructor
public enum PayeeOnboardingOutcomeEnum {

    /** 已受理，等工行审核结论 */
    PENDING("PENDING", "审核中", "等待工行审核结果，通常很快；到点后可主动查询一次"),
    /** 审核通过：可继续签署框架协议、完成授权后开票 */
    READY("READY", "入驻完成", "签署框架收购协议并完成首次授权"),
    /** 审核拒绝：可重新发起，或留联系方式等待平台联系 */
    REJECTED("REJECTED", "审核拒绝", "重新发起收方入驻，或留下联系方式等待平台联系");

    /**
     * 持久化用的编码
     */
    private final String code;
    /**
     * 展示名
     */
    private final String name;
    /**
     * 下一步该做什么（可直接展示给收货员）
     */
    private final String nextStep;

    /**
     * 由审核结果推导状态。
     *
     * @param result 审核结果：pass-审核通过，reject-审核拒绝；其余（含 null）视为尚未出结论
     * @return 未出结论时返回 {@code null}，调用方据此保持当前状态
     */
    public static PayeeOnboardingOutcomeEnum of(String result) {
        if ("pass".equalsIgnoreCase(result)) {
            return READY;
        }
        return "reject".equalsIgnoreCase(result) ? REJECTED : null;
    }

    /**
     * 由查询接口的审核状态推导。
     *
     * @param auditStatus 1-审核通过，2-新增审核中，3-修改审核中，4-删除审核中
     * @return 未出结论或取值不在字典内时返回 {@code null}（不猜）
     */
    public static PayeeOnboardingOutcomeEnum ofAuditStatus(String auditStatus) {
        if ("1".equals(auditStatus)) {
            return READY;
        }
        return "2".equals(auditStatus) || "3".equals(auditStatus) || "4".equals(auditStatus) ? PENDING : null;
    }

    public static PayeeOnboardingOutcomeEnum ofCode(String code) {
        return Arrays.stream(values())
                .filter(item -> item.code.equals(code))
                .findFirst()
                .orElse(null);
    }

    /**
     * 是否可继续走开票链路（仍须补齐协议与授权，见 SellerOnboardingService）
     */
    public boolean isInvoiceEligible() {
        return this == READY;
    }

    /**
     * 是否是审核拒绝，需要留联系方式等待人工跟进
     */
    public boolean needsContactFallback() {
        return this == REJECTED;
    }

}
