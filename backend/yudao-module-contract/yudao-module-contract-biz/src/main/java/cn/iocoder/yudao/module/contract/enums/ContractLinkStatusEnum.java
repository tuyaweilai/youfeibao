package cn.iocoder.yudao.module.contract.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 合同关联状态枚举
 * 
 * @author 芋道源码
 */
@Getter
@AllArgsConstructor
public enum ContractLinkStatusEnum {

    ACTIVE(0, "有效"),
    INACTIVE(1, "失效"),
    SUSPENDED(2, "暂停");

    /**
     * 状态
     */
    private final Integer status;
    /**
     * 描述
     */
    private final String description;

} 