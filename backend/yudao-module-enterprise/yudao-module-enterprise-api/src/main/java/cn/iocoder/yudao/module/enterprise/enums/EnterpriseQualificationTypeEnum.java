package cn.iocoder.yudao.module.enterprise.enums;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 企业资质类型枚举
 *
 * @author 芋道源码
 */
@Getter
@AllArgsConstructor
public enum EnterpriseQualificationTypeEnum implements ArrayValuable<Integer> {

    /**
     * 营业执照
     */
    BUSINESS_LICENSE(0, "营业执照"),
    /**
     * 危废经营许可证
     */
    HAZARDOUS_WASTE_OPERATION_LICENSE(1, "危废经营许可证"),
    /**
     * 道路运输许可证
     */
    ROAD_TRANSPORT_LICENSE(2, "道路运输许可证"),
    /**
     * 回收资质证明
     */
    RECYCLING_QUALIFICATION_CERTIFICATE(3, "回收资质证明"),
    /**
     * 其他
     */
    OTHER(99, "其他");

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(EnterpriseQualificationTypeEnum::getType).toArray(Integer[]::new);

    /**
     * 类型值
     */
    private final Integer type;
    /**
     * 类型名
     */
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }
} 