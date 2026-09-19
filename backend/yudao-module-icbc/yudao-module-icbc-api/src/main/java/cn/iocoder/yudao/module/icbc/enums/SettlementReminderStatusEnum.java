package cn.iocoder.yudao.module.icbc.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * 出售者汇算清缴提醒的状态。
 *
 * <p>出售者须在次年 3 月 31 日前自行汇算清缴。平台不替他申报，但要提醒他、并把
 * 「当年开了多少票、已预缴多少税」给他——这是他自行汇算的依据，也是回收企业
 * 第十一条义务里「提供开票与已缴税款信息」的落地。
 */
public enum SettlementReminderStatusEnum {

    /** 待提醒：提醒记录已生成，尚未触达出售者 */
    PENDING(0, "待提醒", "把提醒与对账单交给出售者（可生成免登录链接）"),
    /** 已提醒：已把提醒与对账单交给出售者 */
    REMINDED(1, "已提醒", "等出售者于次年 3 月 31 日前自行完成汇算清缴");

    private final Integer status;
    private final String name;
    private final String nextAction;

    SettlementReminderStatusEnum(Integer status, String name, String nextAction) {
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

    public static Optional<SettlementReminderStatusEnum> ofStatus(Integer status) {
        return Arrays.stream(values()).filter(item -> item.status.equals(status)).findFirst();
    }

    public static String nameOf(Integer status) {
        return ofStatus(status).map(SettlementReminderStatusEnum::getName).orElse("未知");
    }

    public static String nextActionOf(Integer status) {
        return ofStatus(status).map(SettlementReminderStatusEnum::getNextAction).orElse(null);
    }

}
