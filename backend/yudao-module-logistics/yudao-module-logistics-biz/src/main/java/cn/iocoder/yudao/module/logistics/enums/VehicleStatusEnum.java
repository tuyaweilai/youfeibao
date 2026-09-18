package cn.iocoder.yudao.module.logistics.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 车辆状态枚举
 *
 * @author 芋道源码
 */
@AllArgsConstructor
@Getter
public enum VehicleStatusEnum {

    AVAILABLE(0, "可用"),
    IN_TRANSIT(1, "运输中"),
    MAINTENANCE(2, "维护中");

    /**
     * 状态值
     */
    private final Integer status;
    /**
     * 状态名
     */
    private final String name;

} 