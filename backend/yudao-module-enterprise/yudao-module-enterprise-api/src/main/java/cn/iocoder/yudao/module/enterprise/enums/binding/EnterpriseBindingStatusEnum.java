package cn.iocoder.yudao.module.enterprise.enums.binding;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 企业绑定状态枚举
 */
@Getter
@AllArgsConstructor
public enum EnterpriseBindingStatusEnum {

    SKIP_BINDING(0, "无需处理"), // 例如超级管理员
    AUTO_BIND_SUCCESS(1, "自动绑定成功"),
    MANUAL_AUTH_REQUIRED(2, "需要手动认证/引导至企业中心"),
    ALREADY_BOUND_DEFAULT(3, "已绑定且是默认企业(或唯一企业)"),
    ALREADY_BOUND_NOT_DEFAULT(4, "已绑定但非默认企业"),
    ALREADY_BOUND_NO_DEPT_LINK(5, "用户已有关联企业，但当前登录的部门未直接关联到这些企业"), // 可能需要用户选择或确认
    BINDING_ERROR(9, "绑定过程发生错误"); // 添加错误状态用于异常处理

    private final Integer code;
    private final String description;

    public static EnterpriseBindingStatusEnum getByCode(Integer code) {
        for (EnterpriseBindingStatusEnum anEnum : values()) {
            if (anEnum.getCode().equals(code)) {
                return anEnum;
            }
        }
        return null;
    }
} 