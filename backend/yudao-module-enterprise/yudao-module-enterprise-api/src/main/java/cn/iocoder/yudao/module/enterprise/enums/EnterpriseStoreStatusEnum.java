package cn.iocoder.yudao.module.enterprise.enums;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 企业门店状态枚举
 *
 * @author 芋道源码
 */
@Getter
@AllArgsConstructor
public enum EnterpriseStoreStatusEnum implements ArrayValuable<Integer> {

    /**
     * 启用/营业中
     */
    ENABLED(0, "启用"),
    /**
     * 禁用/暂停营业
     */
    DISABLED(1, "禁用");

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(EnterpriseStoreStatusEnum::getStatus).toArray(Integer[]::new);

    /**
     * 状态值
     */
    private final Integer status;
    /**
     * 状态名
     */
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }
} 