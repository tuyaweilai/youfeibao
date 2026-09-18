package cn.iocoder.yudao.module.icbc.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 工行API调用日志状态枚举
 *
 * @author 芋道源码
 */
@Getter
@AllArgsConstructor
public enum ApiLogStatusEnum {

    SUCCESS(1, "成功"),
    FAILURE(2, "失败");

    /**
     * 状态
     */
    private final Integer status;
    /**
     * 状态名
     */
    private final String name;

} 