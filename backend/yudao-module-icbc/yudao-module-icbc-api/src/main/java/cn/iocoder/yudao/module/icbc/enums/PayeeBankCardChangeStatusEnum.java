package cn.iocoder.yudao.module.icbc.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * 换银行卡（收款账户变更）的状态（#37，ADR 0010）。
 *
 * <p>工行收方入驻绑的是本人一张卡，换卡必须重走收方入驻（工行侧是「修改审核」）。
 * 待变更的新卡在审核通过前**不生效**：钱仍然打原卡，但审核期间新交易的付款挂起——避免打到废卡（退汇）。
 */
public enum PayeeBankCardChangeStatusEnum {

    /** 银行审核中：新卡已提交工行，尚未有结果；此时新交易的付款挂起 */
    PENDING_REVIEW(0, "银行审核中"),
    /** 已生效：新卡已绑定成功，收方档案的收款账户已换成它 */
    EFFECTIVE(1, "变更已生效"),
    /** 已拒绝：新卡未通过审核，原卡继续有效，**收款账户没变** */
    REJECTED(2, "变更未通过"),
    /** 已取消：企业侧人工清掉在途变更（如出售者不再办），原卡继续有效 */
    CANCELLED(9, "变更已取消");

    private final Integer status;
    private final String name;

    PayeeBankCardChangeStatusEnum(Integer status, String name) {
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
     * 是否还压在「银行审核中」：只有这一状态才拦付款。
     */
    public static boolean isPending(Integer status) {
        return PENDING_REVIEW.status.equals(status);
    }

    public static Optional<PayeeBankCardChangeStatusEnum> ofStatus(Integer status) {
        return Arrays.stream(values()).filter(item -> item.status.equals(status)).findFirst();
    }

    public static String nameOf(Integer status) {
        return ofStatus(status).map(PayeeBankCardChangeStatusEnum::getName).orElse(null);
    }

}
