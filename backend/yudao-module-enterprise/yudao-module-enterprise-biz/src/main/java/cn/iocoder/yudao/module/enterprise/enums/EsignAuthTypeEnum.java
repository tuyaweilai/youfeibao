package cn.iocoder.yudao.module.enterprise.enums;

import cn.iocoder.yudao.framework.common.core.IntArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * e签宝认证类型枚举
 */
@AllArgsConstructor
@Getter
public enum EsignAuthTypeEnum implements IntArrayValuable {

    ENTERPRISE(1, "企业认证"),
    PERSONAL(2, "个人认证");

    public static final int[] ARRAYS = Arrays.stream(values()).mapToInt(EsignAuthTypeEnum::getType).toArray();

    /**
     * 类型
     */
    private final Integer type;
    /**
     * 名字
     */
    private final String name;

    @Override
    public int[] array() {
        return ARRAYS;
    }
} 