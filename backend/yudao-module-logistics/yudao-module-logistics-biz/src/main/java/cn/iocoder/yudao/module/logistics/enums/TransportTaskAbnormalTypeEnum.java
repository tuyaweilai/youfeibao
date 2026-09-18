package cn.iocoder.yudao.module.logistics.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 运输任务异常类型枚举
 *
 * @author 芋道源码
 */
@AllArgsConstructor
@Getter
public enum TransportTaskAbnormalTypeEnum {

    VEHICLE_BREAKDOWN(1, "车辆故障"),
    TRAFFIC_ACCIDENT(2, "交通事故"),
    WEATHER_DELAY(3, "天气延误"),
    ROAD_CLOSURE(4, "道路封闭"),
    CARGO_DAMAGE(5, "货物损坏"),
    CUSTOMER_UNAVAILABLE(6, "客户不在"),
    ADDRESS_ERROR(7, "地址错误"),
    OTHER(99, "其他异常");

    /**
     * 异常类型值
     */
    private final Integer type;
    /**
     * 异常类型名
     */
    private final String name;

} 