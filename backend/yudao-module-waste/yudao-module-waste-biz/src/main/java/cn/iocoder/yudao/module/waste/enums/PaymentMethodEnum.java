package cn.iocoder.yudao.module.waste.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 付款方式枚举
 *
 * @author 芋道源码
 */
@AllArgsConstructor
@Getter
public enum PaymentMethodEnum {

    BANK_TRANSFER(1, "银行转账"),
    CASH(2, "现金支付"),
    ONLINE_PAYMENT(3, "在线支付"),
    CHECK(4, "支票"),
    CREDIT_CARD(5, "信用卡");

    /**
     * 方式值
     */
    private final Integer method;
    /**
     * 方式名
     */
    private final String name;

    public static PaymentMethodEnum valueOf(Integer method) {
        for (PaymentMethodEnum methodEnum : values()) {
            if (methodEnum.getMethod().equals(method)) {
                return methodEnum;
            }
        }
        return null;
    }

} 