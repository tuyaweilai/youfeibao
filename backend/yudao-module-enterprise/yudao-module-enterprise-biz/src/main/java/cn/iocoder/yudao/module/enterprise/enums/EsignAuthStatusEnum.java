package cn.iocoder.yudao.module.enterprise.enums;

import cn.iocoder.yudao.framework.common.core.IntArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * e签宝认证状态枚举
 */
@AllArgsConstructor
@Getter
public enum EsignAuthStatusEnum implements IntArrayValuable {

    PENDING(0, "待认证"),
    IN_PROGRESS(1, "认证中"), 
    SUCCESS(2, "认证成功"),
    FAILED(3, "认证失败");

    public static final int[] ARRAYS = Arrays.stream(values()).mapToInt(EsignAuthStatusEnum::getStatus).toArray();

    /**
     * 状态
     */
    private final Integer status;
    /**
     * 名字
     */
    private final String name;

    @Override
    public int[] array() {
        return ARRAYS;
    }
} 