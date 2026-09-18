package cn.iocoder.yudao.module.waste.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 客户模式枚举
 *
 * @author 芋道源码
 */
@AllArgsConstructor
@Getter
public enum CustomerModeEnum {

    OPEN_MODE(1, "开放模式"),
    WHITELIST_MODE(2, "白名单模式"),
    BLACKLIST_MODE(3, "黑名单模式"),
    INVITATION_MODE(4, "邀请模式");

    /**
     * 模式值
     */
    private final Integer mode;
    /**
     * 模式名
     */
    private final String name;

    public static CustomerModeEnum valueOf(Integer mode) {
        for (CustomerModeEnum modeEnum : values()) {
            if (modeEnum.getMode().equals(mode)) {
                return modeEnum;
            }
        }
        return null;
    }

} 