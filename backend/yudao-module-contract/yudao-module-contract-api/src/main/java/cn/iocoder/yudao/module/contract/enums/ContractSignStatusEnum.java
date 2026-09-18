package cn.iocoder.yudao.module.contract.enums;

import cn.iocoder.yudao.framework.common.core.IntArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 合同签署状态枚举
 *
 * @author 芋道源码
 */
@AllArgsConstructor
@Getter
public enum ContractSignStatusEnum implements IntArrayValuable {

    PENDING(0, "待签署"),
    SIGNED(1, "已签署"),
    DECLINED(2, "已拒绝"),
    EXPIRED(3, "已过期");

    public static final int[] ARRAYS = Arrays.stream(values()).mapToInt(ContractSignStatusEnum::getStatus).toArray();

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

} 