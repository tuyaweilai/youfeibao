package cn.iocoder.yudao.module.icbc.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * 进项发票登记状态（#49 T11）。
 *
 * <p>状态由「已勾稽金额」与「发票价税合计」的关系推导并落库（每次勾稽 / 取消勾稽后重算），
 * 让列表能按状态直接筛选，不用每次现算。
 *
 * <p>三态是验收要求的「已登记 / 部分勾稽 / 已勾稽」：
 * <ul>
 *     <li>{@link #REGISTERED}：刚登记、还没有任何勾稽；</li>
 *     <li>{@link #PARTIALLY_LINKED}：已勾稽一部分，仍有余额；</li>
 *     <li>{@link #LINKED}：勾稽金额已等于发票价税合计。</li>
 * </ul>
 */
public enum InputInvoiceStatusEnum {

    /** 已登记：还没有勾稽到任何单据 */
    REGISTERED(0, "已登记"),
    /** 部分勾稽：勾稽了一部分，仍有可勾稽余额 */
    PARTIALLY_LINKED(1, "部分勾稽"),
    /** 已勾稽：勾稽金额已等于发票价税合计 */
    LINKED(2, "已勾稽");

    private final Integer status;
    private final String name;

    InputInvoiceStatusEnum(Integer status, String name) {
        this.status = status;
        this.name = name;
    }

    public Integer getStatus() {
        return status;
    }

    public String getName() {
        return name;
    }

    public static Optional<InputInvoiceStatusEnum> ofStatus(Integer status) {
        return Arrays.stream(values()).filter(item -> item.status.equals(status)).findFirst();
    }

    public static String nameOf(Integer status) {
        return ofStatus(status).map(InputInvoiceStatusEnum::getName).orElse(null);
    }

}
