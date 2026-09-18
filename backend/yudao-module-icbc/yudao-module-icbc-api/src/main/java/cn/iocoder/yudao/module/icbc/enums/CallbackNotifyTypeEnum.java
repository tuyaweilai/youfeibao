package cn.iocoder.yudao.module.icbc.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 工行回调通知类型枚举
 *
 * @author 芋道源码
 */
@Getter
@AllArgsConstructor
public enum CallbackNotifyTypeEnum {

    PAYEE_AUDIT("PAYEE_AUDIT", "收方审核"),
    PAYER_AUDIT("PAYER_AUDIT", "付方审核"),
    INVOICE_STATUS("INVOICE_STATUS", "发票状态"),
    PAYMENT_STATUS("PAYMENT_STATUS", "支付状态");

    /**
     * 类型
     */
    private final String type;
    /**
     * 类型名
     */
    private final String name;

} 