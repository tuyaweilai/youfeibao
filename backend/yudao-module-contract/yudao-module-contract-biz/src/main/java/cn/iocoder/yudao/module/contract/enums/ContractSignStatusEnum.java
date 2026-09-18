package cn.iocoder.yudao.module.contract.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 合同签署状态枚举
 * 
 * @author 芋道源码
 */
@Getter
@AllArgsConstructor
public enum ContractSignStatusEnum {

    PENDING(0, "待签署"),
    SIGNED(1, "已签署"),
    DECLINED(2, "已拒绝"),
    EXPIRED(3, "已过期");

    /**
     * 状态
     */
    private final Integer status;
    /**
     * 描述
     */
    private final String description;

} 