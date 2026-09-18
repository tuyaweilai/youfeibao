package cn.iocoder.yudao.module.enterprise.enums;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 企业状态枚举
 *
 * @author 芋道源码
 */
@Getter
@AllArgsConstructor
public enum EnterpriseStatusEnum implements ArrayValuable<Integer> {

    /**
      * 入驻待审核
     */
    PENDING_ONBOARDING_REVIEW(0, "入驻待审核"),
    /**
     * 入驻申请拒绝
     */
    ONBOARDING_REJECTED(1, "入驻申请拒绝"),
    /**
     * 入驻成功，待启动第三方认证
     */
    ONBOARDING_APPROVED_AWAITING_CERTIFICATION(2, "入驻成功待认证"),
    /**
     * 个人实名认证中/待处理
     */
    PERSONAL_CERTIFICATION_PENDING(3, "个人认证中"),
    /**
     * 企业认证中/待处理
     */
    ENTERPRISE_CERTIFICATION_PENDING(4, "企业认证中"),
    /**
     * 任一第三方认证失败
     */
    CERTIFICATION_FAILED(5, "认证失败"),
    /**
     * 所有必要第三方认证完成，企业完全激活
     */
    FULLY_CERTIFIED(6, "认证完成"),
    /**
     * 锁定
     */
    LOCKED(7, "锁定"),
    /**
     * 已注销
     */
    CANCELLED(8, "已注销");

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(EnterpriseStatusEnum::getStatus).toArray(Integer[]::new);

    /**
     * 状态
     */
    private final Integer status;
    /**
     * 状态描述
     */
    private final String desc;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }
} 