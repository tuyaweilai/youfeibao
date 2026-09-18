package cn.iocoder.yudao.module.contract.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 合同关联类型枚举
 * 
 * @author 芋道源码
 */
@Getter
@AllArgsConstructor
public enum ContractLinkTypeEnum {

    BUSINESS_LINK(0, "业务关联"),
    DEPENDENCY_LINK(1, "依赖关联"),
    REFERENCE_LINK(2, "参考关联");

    /**
     * 类型
     */
    private final Integer type;
    /**
     * 描述
     */
    private final String description;

} 