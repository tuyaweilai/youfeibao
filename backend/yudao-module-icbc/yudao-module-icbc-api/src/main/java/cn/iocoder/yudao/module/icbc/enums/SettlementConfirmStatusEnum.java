package cn.iocoder.yudao.module.icbc.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * 结算单的确认状态（ADR 0018 / 0024）。
 *
 * <p>注意：这里**不是**「结算是否结清」的状态。结算是否结清由其下的收购单推导（还有未完成的不叫已结清），
 * 不单独维护一个会说谎的状态。本枚举只表达「出售者是否认可这一版计量与计价事实」。
 */
public enum SettlementConfirmStatusEnum {

    /** 待确认：结算单已生成或已改版，等待出售者确认 */
    PENDING(0, "待确认"),
    /** 已确认：出售者勾选确认（含快照哈希、时间、IP、设备留痕） */
    CONFIRMED(1, "已确认"),
    /** 有异议：出售者提出异议，等待企业处理 */
    DISPUTED(2, "有异议"),
    /** 需线下签字确认：长期不确认或连续异议，升级为线下签字（到期不自动确认） */
    OFFLINE_REQUIRED(3, "需线下签字确认"),
    /** 已线下签字确认：企业上传带签字的纸质确认书并标注办理人 */
    OFFLINE_CONFIRMED(4, "已线下签字确认");

    private final Integer status;
    private final String name;

    SettlementConfirmStatusEnum(Integer status, String name) {
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
     * 是否算「已认可」：已确认与已线下签字确认都等价于出售者认可了这一版事实。
     */
    public static boolean isConfirmed(Integer status) {
        return CONFIRMED.status.equals(status) || OFFLINE_CONFIRMED.status.equals(status);
    }

    public static Optional<SettlementConfirmStatusEnum> ofStatus(Integer status) {
        return Arrays.stream(values()).filter(item -> item.status.equals(status)).findFirst();
    }

}
