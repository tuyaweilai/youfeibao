package cn.iocoder.yudao.module.logistics.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 现金代付通知状态枚举
 *
 * @author 芋道源码
 */
@AllArgsConstructor
@Getter
public enum CashAdvanceNotifyStatusEnum {

    NOT_NOTIFIED(0, "未通知"),
    NOTIFIED(1, "已通知"),
    NOTIFY_FAILED(2, "通知失败");

    /**
     * 通知状态值
     */
    private final Integer status;
    /**
     * 通知状态名
     */
    private final String name;

} 