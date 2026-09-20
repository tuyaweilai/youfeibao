package cn.iocoder.yudao.module.logistics.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 运输节点类型枚举
 *
 * @author 芋道源码
 */
@AllArgsConstructor
@Getter
public enum TransportNodeTypeEnum {

    TASK_CREATED(1, "任务创建"),
    TASK_ASSIGNED(2, "任务分配"),
    TASK_ACCEPTED(3, "任务接受"),
    TRANSPORT_STARTED(4, "开始运输"),
    ARRIVED_PICKUP(5, "到达取货点"),
    PICKUP_COMPLETED(6, "取货完成"),
    IN_TRANSIT(7, "运输中"),
    ARRIVED_DELIVERY(8, "到达送货点"),
    DELIVERY_COMPLETED(9, "送货完成"),
    TASK_COMPLETED(10, "任务完成"),
    TASK_CANCELLED(11, "任务取消"),
    ABNORMAL_REPORTED(12, "异常报告"),
    ABNORMAL_RESOLVED(13, "异常解决");

    /**
     * 节点类型值
     */
    private final Integer type;
    /**
     * 节点类型名
     */
    private final String name;

} 