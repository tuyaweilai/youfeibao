package cn.iocoder.yudao.module.logistics.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 现金代付对账状态枚举
 *
 * @author 芋道源码
 */
@AllArgsConstructor
@Getter
public enum CashAdvanceReconcileStatusEnum {

    NOT_RECONCILED(0, "未对账"),
    RECONCILED(1, "已对账"),
    RECONCILE_FAILED(2, "对账失败");

    /**
     * 对账状态值
     */
    private final Integer status;
    /**
     * 对账状态名
     */
    private final String name;

} 