package cn.iocoder.yudao.module.enterprise.enums;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 企业类型枚举
 *
 * @author 芋道源码
 */
@Getter
@AllArgsConstructor
public enum EnterpriseTypeEnum implements ArrayValuable<Integer> {

    /**
     * 废物产生企业
     */
    WASTE_PRODUCTION(1, "废物产生企业"),
    /**
     * 再生资源回收企业
     */
    RECYCLING(2, "再生资源回收企业"),
    /**
     * 再生资源处置企业
     */
    DISPOSAL(3, "再生资源处置企业"),
    /**
     * 物流运输企业
     */
    LOGISTICS(4, "物流运输企业");

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(EnterpriseTypeEnum::getType).toArray(Integer[]::new);

    /**
     * 类型
     */
    private final Integer type;
    /**
     * 名称
     */
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }
} 