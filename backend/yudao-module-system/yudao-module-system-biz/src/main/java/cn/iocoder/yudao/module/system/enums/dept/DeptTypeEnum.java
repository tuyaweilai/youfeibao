package cn.iocoder.yudao.module.system.enums.dept;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 部门类型枚举
 *
 * @author 芋道源码
 */
@Getter
@AllArgsConstructor
public enum DeptTypeEnum implements ArrayValuable<Integer> {

    DEPT(0, "普通部门"),
    INSTITUTION(1, "机构");

    public static final int[] ARRAYS = Arrays.stream(values()).mapToInt(DeptTypeEnum::getType).toArray();

    /**
     * 类型
     */
    private final Integer type;
    /**
     * 类型名
     */
    private final String name;

    @Override
    public Integer[] array() {
        return Arrays.stream(values()).map(DeptTypeEnum::getType).toArray(Integer[]::new);
    }
    
    /**
     * 判断是否是机构
     * 
     * @param type 部门类型
     * @return 是否是机构
     */
    public static boolean isInstitution(Integer type) {
        return INSTITUTION.getType().equals(type);
    }

} 