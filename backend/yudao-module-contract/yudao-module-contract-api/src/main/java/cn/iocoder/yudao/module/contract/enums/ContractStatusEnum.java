package cn.iocoder.yudao.module.contract.enums;

import cn.iocoder.yudao.framework.common.core.IntArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 合同状态枚举
 *
 * @author 芋道源码
 */
@AllArgsConstructor
@Getter
public enum ContractStatusEnum implements IntArrayValuable {

    DRAFT(0, "草稿"),
    PENDING_SIGN(1, "待签署"),
    SIGNING(2, "签署中"),
    EFFECTIVE(3, "已生效"),
    EXPIRED(4, "已过期"),
    TERMINATED(5, "已终止"),
    CANCELLED(6, "已作废");

    public static final int[] ARRAYS = Arrays.stream(values()).mapToInt(ContractStatusEnum::getStatus).toArray();

    /**
     * 状态值
     */
    private final Integer status;
    /**
     * 状态名
     */
    private final String name;

    @Override
    public int[] array() {
        return ARRAYS;
    }

    /**
     * 判断是否可以编辑
     */
    public static boolean canEdit(Integer status) {
        return DRAFT.getStatus().equals(status);
    }

    /**
     * 判断是否可以删除
     */
    public static boolean canDelete(Integer status) {
        return DRAFT.getStatus().equals(status) || CANCELLED.getStatus().equals(status);
    }

    /**
     * 判断是否可以签署
     */
    public static boolean canSign(Integer status) {
        return PENDING_SIGN.getStatus().equals(status) || SIGNING.getStatus().equals(status);
    }

} 