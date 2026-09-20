package cn.iocoder.yudao.module.logistics.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 临时订单支付状态枚举
 *
 * @author 芋道源码
 */
@AllArgsConstructor
@Getter
public enum TemporaryOrderPaymentStatusEnum {

    UNPAID(0, "未支付"),
    PAID(1, "已支付"),
    REFUNDED(2, "已退款"),
    CANCELLED(3, "已取消");

    /**
     * 支付状态值
     */
    private final Integer status;
    /**
     * 支付状态名
     */
    private final String name;

} 