package cn.iocoder.yudao.module.icbc.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * 采购合同状态（#45 / T07，ADR 0027）。
 *
 * <p>流转：草稿 → 待审核 → 生效 → 关闭。**审核通过前不得作为有效采购依据**
 * （{@code PurchaseContractService#assertUsableAsPurchaseBasis} 是唯一门禁）。
 *
 * <p>与 icbc 侧的「框架收购协议」是两件事：框架收购协议是自然人出售者对开票与代办税费的
 * 授权附件，是**开票前置**；采购合同是**采购条款**，两者不合并（ADR 0027）。
 *
 * <p>{@link #EXPIRED} **不落库**，由「已生效 + 有效期止早于今天」推导，与 #13 的「逾期」同一做法：
 * 状态由事实推导，不维护一个会说谎的字段。
 */
public enum PurchaseContractStatusEnum {

    /** 草稿：可自由编辑，还不是任何采购依据 */
    DRAFT(0, "草稿"),
    /** 待审核：已送审，等待审核动作；审核通过前不得作为采购依据 */
    PENDING_AUDIT(1, "待审核"),
    /** 生效：审核通过，可作为采购依据 */
    EFFECTIVE(2, "生效"),
    /** 关闭：不再作为采购依据，历史业务仍可回查 */
    CLOSED(3, "关闭"),
    /** 过期：已生效但有效期止早于今天（推导状态，不落库） */
    EXPIRED(4, "过期");

    private final Integer status;
    private final String name;

    PurchaseContractStatusEnum(Integer status, String name) {
        this.status = status;
        this.name = name;
    }

    public Integer getStatus() {
        return status;
    }

    public String getName() {
        return name;
    }

    public static Optional<PurchaseContractStatusEnum> ofStatus(Integer status) {
        return Arrays.stream(values()).filter(item -> item.status.equals(status)).findFirst();
    }

    public static String nameOf(Integer status) {
        return ofStatus(status).map(PurchaseContractStatusEnum::getName).orElse(null);
    }

}
