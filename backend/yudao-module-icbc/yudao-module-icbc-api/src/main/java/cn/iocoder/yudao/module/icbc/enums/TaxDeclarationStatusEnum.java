package cn.iocoder.yudao.module.icbc.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * 代办税费申报单的状态。
 *
 * <p>状态机是单向的：<b>待申报</b> → <b>已申报待缴款</b> → <b>已缴款</b>。
 * 「逾期」不是一个独立状态，而是「未缴款且已过申报期截止日」这一事实——由时间推导，
 * 见 {@link #isOverdue}。这样过期的判断不会被一次没跑到的定时任务卡住。
 */
public enum TaxDeclarationStatusEnum {

    /** 待申报：清单已生成，尚未报送《代办税费报告表》 */
    PENDING(0, "待申报", "核对清单与金额后报送《代办税费报告表》《代办税费明细报告表》"),
    /** 已申报待缴款：报告表已报送，尚未缴款归档 */
    DECLARED(1, "已申报待缴款", "按应缴合计完成缴款，并归档缴款凭证"),
    /** 已缴款：已缴款并归档凭证，与对应发票关联 */
    PAID(2, "已缴款", null);

    private final Integer status;
    private final String name;
    private final String nextAction;

    TaxDeclarationStatusEnum(Integer status, String name, String nextAction) {
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

    public String getNextAction() {
        return nextAction;
    }

    public static Optional<TaxDeclarationStatusEnum> ofStatus(Integer status) {
        return Arrays.stream(values()).filter(item -> item.status.equals(status)).findFirst();
    }

    public static String nameOf(Integer status) {
        return ofStatus(status).map(TaxDeclarationStatusEnum::getName).orElse("未知");
    }

    public static String nextActionOf(Integer status) {
        return ofStatus(status).map(TaxDeclarationStatusEnum::getNextAction).orElse(null);
    }

    /** 是否已缴款 */
    public static boolean isPaid(Integer status) {
        return PAID.getStatus().equals(status);
    }

}
