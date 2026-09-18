package cn.iocoder.yudao.module.contract.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 合同操作类型枚举
 * 
 * @author 芋道源码
 */
@Getter
@AllArgsConstructor
public enum ContractOperationTypeEnum {

    CREATE(1, "创建合同"),
    UPDATE(2, "修改合同"),
    SUBMIT_APPROVAL(3, "提交审核"),
    APPROVAL(4, "审核"),
    SIGN(5, "签署"),
    ACTIVATE(6, "激活"),
    TERMINATE(7, "终止"),
    ARCHIVE(8, "归档");

    /**
     * 类型
     */
    private final Integer type;
    /**
     * 描述
     */
    private final String description;

} 