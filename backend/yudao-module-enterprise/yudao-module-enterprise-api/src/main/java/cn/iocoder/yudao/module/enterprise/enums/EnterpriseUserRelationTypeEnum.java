package cn.iocoder.yudao.module.enterprise.enums;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 企业用户关系类型枚举
 *
 * @author 芋道源码
 */
@Getter
@AllArgsConstructor
public enum EnterpriseUserRelationTypeEnum implements ArrayValuable<Integer> {

    /**
     * 企业管理员
     */
    ADMIN(1, "企业管理员"),
    /**
     * 企业员工
     */
    EMPLOYEE(2, "企业员工"),
    /**
     * 企业法人代表
     */
    LEGAL_PERSON(3, "企业法人代表"),
    /**
     * 其他
     */
    OTHER(99, "其他");

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(EnterpriseUserRelationTypeEnum::getType).toArray(Integer[]::new);

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