package cn.iocoder.yudao.module.icbc.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * 采购订单履约异常授权单的状态（#47 T09）。
 *
 * <p>只有「已通过」的授权单能放宽交货门禁，且还要看授权范围（追加量 / 有效期 / 场站）是否覆盖本次交货。
 * 待审核与已拒绝都不放宽；重复提交同一异常不会覆盖已有记录。
 */
public enum PurchaseExceptionStatusEnum {

    PENDING(0, "待审核"),
    APPROVED(1, "已通过"),
    REJECTED(2, "已拒绝");

    private final Integer status;
    private final String name;

    PurchaseExceptionStatusEnum(Integer status, String name) {
        this.status = status;
        this.name = name;
    }

    public Integer getStatus() {
        return status;
    }

    public String getName() {
        return name;
    }

    public boolean isApproved() {
        return this == APPROVED;
    }

    public static Optional<PurchaseExceptionStatusEnum> ofStatus(Integer status) {
        return Arrays.stream(values()).filter(item -> item.status.equals(status)).findFirst();
    }

    public static String nameOf(Integer status) {
        return ofStatus(status).map(PurchaseExceptionStatusEnum::getName).orElse(null);
    }

}
