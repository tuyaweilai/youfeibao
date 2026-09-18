package cn.iocoder.yudao.module.icbc.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 出售者实人认证状态。
 *
 * 实人认证是收方入驻的前置环节：平台先发起工行实人认证 H5，认证通过后才可用同一
 * {@code outUserId} 走收方入驻与开票（见工行答复 §五）。
 */
@Getter
@AllArgsConstructor
public enum PayeeRealNameStatusEnum {

    NOT_STARTED(0, "未认证"),
    PENDING(1, "认证中"),
    PASSED(2, "认证通过"),
    FAILED(3, "认证未通过");

    private final Integer status;
    private final String name;

    public static PayeeRealNameStatusEnum of(Integer status) {
        return Arrays.stream(values())
                .filter(item -> item.status.equals(status))
                .findFirst()
                .orElse(null);
    }

}
