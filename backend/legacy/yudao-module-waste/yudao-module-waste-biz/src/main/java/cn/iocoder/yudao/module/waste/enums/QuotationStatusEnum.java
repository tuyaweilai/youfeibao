package cn.iocoder.yudao.module.waste.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 报价状态枚举
 *
 * @author 芋道源码
 */
@AllArgsConstructor
@Getter
public enum QuotationStatusEnum {

    PENDING(1, "待处理"),
    ACCEPTED(2, "已接受"),
    REJECTED(3, "已拒绝"),
    WITHDRAWN(4, "已撤回"),
    EXPIRED(5, "已过期");

    /**
     * 状态值
     */
    private final Integer status;
    /**
     * 状态名
     */
    private final String name;

    public static QuotationStatusEnum valueOf(Integer status) {
        for (QuotationStatusEnum statusEnum : values()) {
            if (statusEnum.getStatus().equals(status)) {
                return statusEnum;
            }
        }
        return null;
    }

} 