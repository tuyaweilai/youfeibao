package cn.iocoder.yudao.module.contract.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 合同电子签章状态枚举
 * 
 * @author 芋道源码
 */
@Getter
@AllArgsConstructor
public enum ContractEsignatureStatusEnum {

    NOT_INITIATED(0, "未发起"),
    IN_PROGRESS(1, "进行中"),
    COMPLETED(2, "已完成"),
    FAILED(3, "已失败");

    /**
     * 状态
     */
    private final Integer status;
    /**
     * 描述
     */
    private final String description;

} 