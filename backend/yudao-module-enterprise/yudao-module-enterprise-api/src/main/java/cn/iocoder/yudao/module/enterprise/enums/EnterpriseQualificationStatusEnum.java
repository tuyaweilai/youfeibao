package cn.iocoder.yudao.module.enterprise.enums;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 企业资质状态枚举
 *
 * @author 芋道源码
 */
@Getter
@AllArgsConstructor
public enum EnterpriseQualificationStatusEnum implements ArrayValuable<Integer> {

    /**
     * 待审核
     */
    PENDING_AUDIT(0, "待审核"),
    /**
     * 有效
     */
    VALID(1, "有效"),
    /**
     * 已过期
     */
    EXPIRED(2, "已过期"),
    /**
     * 审核拒绝
     */
    AUDIT_REJECTED(3, "审核拒绝"),
    /**
     * 已作废
     */
    INVALID(4, "已作废");

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(EnterpriseQualificationStatusEnum::getStatus).toArray(Integer[]::new);

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