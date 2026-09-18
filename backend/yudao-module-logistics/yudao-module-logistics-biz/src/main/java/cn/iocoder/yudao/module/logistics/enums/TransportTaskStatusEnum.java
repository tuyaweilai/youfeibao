package cn.iocoder.yudao.module.logistics.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 运输任务状态枚举
 *
 * @author 芋道源码
 */
@AllArgsConstructor
@Getter
public enum TransportTaskStatusEnum {

    PENDING(0, "待分配"),
    ASSIGNED(1, "已分配"),
    ACCEPTED(2, "已接受"),
    IN_TRANSIT(3, "运输中"),
    PICKED_UP(4, "已取货"),
    DELIVERED(5, "已送达"),
    COMPLETED(6, "已完成"),
    CANCELLED(7, "已取消");

    /**
     * 状态值
     */
    private final Integer status;
    /**
     * 状态名
     */
    private final String name;

} 