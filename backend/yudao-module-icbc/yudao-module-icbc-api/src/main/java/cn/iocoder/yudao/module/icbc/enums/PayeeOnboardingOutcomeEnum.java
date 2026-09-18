package cn.iocoder.yudao.module.icbc.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 出售者收方入驻的四种结果组合。
 *
 * <p>工行侧有两条独立的成败线：{@code openacctStatus}（开户状态：02-成功 / 03-失败）
 * 与 {@code result}（审核结果：pass / reject，见
 * {@code docs/icbc/智慧清分收方审核回调示例文档.md}）。四种组合各有不同的下一步，
 * 这就是本票「四种组合分别进入正确的下一步」的落地。
 *
 * <p>结算仍走公对私直付银行卡（ADR 0010），这里的「开户状态」是不透明字段透传，
 * 不代表平台给出售者开了电子钱包。
 */
@Getter
@AllArgsConstructor
public enum PayeeOnboardingOutcomeEnum {

    /** 开户成功 + 审核通过：可继续签署框架协议、完成授权后开票 */
    READY("READY", "入驻完成", "签署框架收购协议并完成首次授权"),
    /** 开户成功 + 审核拒绝：不可开票，留联系方式等待联系 */
    REJECTED("REJECTED", "审核拒绝", "留下联系方式等待平台联系"),
    /** 开户失败 + 审核通过：不可开票，重新发起收方入驻 */
    OPENACCT_FAILED("OPENACCT_FAILED", "开户失败", "重新发起收方入驻"),
    /** 开户失败 + 审核拒绝：不可开票，留联系方式等待联系 */
    FAILED_AND_REJECTED("FAILED_AND_REJECTED", "开户失败且审核拒绝", "留下联系方式等待平台联系");

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
     * 由两条成败线推导结果。两条线尚未同时到齐（开户仍在途，或审核结果未知）时返回 {@code null}，
     * 调用方据此保持当前状态、等待异步通知或下一次查询。
     *
     * @param openacctStatus 开户状态，02-成功，03-失败
     * @param result         审核结果，pass-通过，reject-拒绝
     */
    public static PayeeOnboardingOutcomeEnum of(String openacctStatus, String result) {
        boolean accountOpened = "02".equals(openacctStatus);
        boolean accountFailed = "03".equals(openacctStatus);
        boolean approved = "pass".equalsIgnoreCase(result);
        boolean rejected = "reject".equalsIgnoreCase(result);
        if (!(accountOpened || accountFailed) || !(approved || rejected)) {
            return null;
        }
        if (accountOpened && approved) {
            return READY;
        }
        if (accountOpened) {
            return REJECTED;
        }
        return approved ? OPENACCT_FAILED : FAILED_AND_REJECTED;
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
     * 是否是审核 / 开户失败，需要留联系方式等待人工跟进
     */
    public boolean needsContactFallback() {
        return this == REJECTED || this == FAILED_AND_REJECTED;
    }

}
