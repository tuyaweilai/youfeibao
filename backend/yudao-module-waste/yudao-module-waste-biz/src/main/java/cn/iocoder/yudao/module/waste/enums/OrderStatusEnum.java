package cn.iocoder.yudao.module.waste.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 订单状态枚举
 *
 * @author 芋道源码
 */
@AllArgsConstructor
@Getter
public enum OrderStatusEnum {

    PENDING_CONFIRM(1, "待确认"),
    CONFIRMED(2, "已确认"),
    IN_PROGRESS(3, "进行中"),
    COMPLETED(4, "已完成"),
    CANCELLED(99, "已取消");

    /**
     * 状态值
     */
    private final Integer status;
    /**
     * 状态名
     */
    private final String name;

    public static OrderStatusEnum valueOf(Integer status) {
        for (OrderStatusEnum statusEnum : values()) {
            if (statusEnum.getStatus().equals(status)) {
                return statusEnum;
            }
        }
        return null;
    }

} 