package cn.iocoder.yudao.module.icbc.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

/**
 * 工行回调处理状态枚举
 *
 * @author 芋道源码
 */
@Getter
@AllArgsConstructor
public enum CallbackProcessStatusEnum {

    PENDING(0, "待处理"),
    SUCCESS(1, "处理成功"),
    FAILURE(2, "处理失败");

    /**
     * 状态
     */
    private final Integer status;
    /**
     * 状态名
     */
    private final String name;

    public static Optional<CallbackProcessStatusEnum> ofStatus(Integer status) {
        return Arrays.stream(values()).filter(item -> item.status.equals(status)).findFirst();
    }

    public static String nameOf(Integer status) {
        return ofStatus(status).map(CallbackProcessStatusEnum::getName).orElse("未知");
    }

} 