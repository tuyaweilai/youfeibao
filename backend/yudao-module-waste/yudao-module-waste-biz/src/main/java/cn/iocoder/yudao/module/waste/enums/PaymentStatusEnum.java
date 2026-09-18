package cn.iocoder.yudao.module.waste.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 付款状态枚举
 *
 * @author 芋道源码
 */
@AllArgsConstructor
@Getter
public enum PaymentStatusEnum {

    UNPAID(0, "未付款"),
    PAID(1, "已付款"),
    PARTIAL_PAID(2, "部分付款"),
    REFUNDED(3, "已退款"),
    CANCELLED(99, "已取消");

    /**
     * 状态值
     */
    private final Integer status;
    /**
     * 状态名
     */
    private final String name;

    public static PaymentStatusEnum valueOf(Integer status) {
        for (PaymentStatusEnum statusEnum : values()) {
            if (statusEnum.getStatus().equals(status)) {
                return statusEnum;
            }
        }
        return null;
    }

} 