package cn.iocoder.yudao.module.icbc.enums;

import java.util.Arrays;

/**
 * 框架收购协议的状态（ADR 0036 / 0018）。
 *
 * <p>「发起 ≠ 签完」：电子签署发起后协议落 {@link #PENDING}，签署完成回调把它推到
 * {@link #EFFECTIVE} 并盖签署时间；重签生效时旧的生效协议转 {@link #VOIDED}、历史可查。
 *
 * <p>开票门禁里的「生效协议」一项只认 {@link #EFFECTIVE}：待签署天然不满足，
 * 所以门禁**不需要因为电子签章而改动**（#81 决策 18）。
 */
public enum FrameworkAgreementStatusEnum {

    /** 待签署：电子签署已发起、本人尚未签完 */
    PENDING(0, "待签署"),

    /** 生效：纸质当场签署，或电子签署已由回调确认整体签完 */
    EFFECTIVE(1, "生效"),

    /** 作废：重签生效时旧生效协议转此状态，保留历史 */
    VOIDED(2, "作废");

    private final Integer status;
    private final String name;

    FrameworkAgreementStatusEnum(Integer status, String name) {
        this.status = status;
        this.name = name;
    }

    public Integer getStatus() {
        return status;
    }

    public String getName() {
        return name;
    }

    public static FrameworkAgreementStatusEnum of(Integer status) {
        return Arrays.stream(values())
                .filter(item -> item.status.equals(status))
                .findFirst()
                .orElse(null);
    }

}
