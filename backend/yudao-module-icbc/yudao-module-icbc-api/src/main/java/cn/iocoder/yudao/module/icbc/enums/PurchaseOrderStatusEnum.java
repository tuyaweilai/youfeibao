package cn.iocoder.yudao.module.icbc.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * 采购订单状态（#46 T08）。
 *
 * <p>流转：草稿 → 执行中 ⇄ 暂停 → 完成 → 关闭。**只有「执行中」的订单可作为有效采购依据**
 * （{@code PurchaseOrderService#assertUsableAsPurchaseBasis} 是唯一门禁）：暂停 / 完成 / 关闭的
 * 订单不再被收购登记选为采购安排。关闭不删除已发生的业务（收购单 / 入库等历史仍可回查，#47）。
 *
 * <p>与采购合同一样，「超期」不由状态表达，而由「已生效 + 结束日期早于今天」推导。
 */
public enum PurchaseOrderStatusEnum {

    /** 草稿：可自由编辑，还不是任何采购依据 */
    DRAFT(0, "草稿"),
    /** 执行中：可作为有效采购依据，允许按明细分次收货 */
    EXECUTING(1, "执行中"),
    /** 暂停：暂时不再作为有效依据，恢复后继续 */
    SUSPENDED(2, "暂停"),
    /** 完成：订单已履行完毕，不再接收新的收货 */
    COMPLETED(3, "完成"),
    /** 关闭：人工终止，历史业务保留 */
    CLOSED(4, "关闭");

    private final Integer status;
    private final String name;

    PurchaseOrderStatusEnum(Integer status, String name) {
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
     * 该状态是否可作为有效采购依据（采购登记时能选中的「有效采购安排」）。
     */
    public boolean isUsableAsBasis() {
        return this == EXECUTING;
    }

    public static Optional<PurchaseOrderStatusEnum> ofStatus(Integer status) {
        return Arrays.stream(values()).filter(item -> item.status.equals(status)).findFirst();
    }

    public static String nameOf(Integer status) {
        return ofStatus(status).map(PurchaseOrderStatusEnum::getName).orElse(null);
    }

}
