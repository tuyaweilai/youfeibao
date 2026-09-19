package cn.iocoder.yudao.module.icbc.enums;

import cn.hutool.core.util.StrUtil;

import java.util.Arrays;
import java.util.Optional;

/**
 * 异议原因（固定枚举，ADR 0022）。
 *
 * <p>枚举的作用是让企业一眼知道该改哪个字段——自由文本会变成扯皮，也是 ADR 0019 把扣杂
 * 独立成字段（而不是备注）的另一半理由。选「其他」时必须附说明。
 */
public enum SettlementDisputeReasonEnum {

    WEIGHT("01", "重量不符"),
    DEDUCTION("02", "扣杂不符"),
    PRICE("03", "单价不符"),
    CATEGORY("04", "品类或等级不符"),
    GOODS("05", "货物不符"),
    OTHER("99", "其他（须附说明）");

    private final String reason;
    private final String name;

    SettlementDisputeReasonEnum(String reason, String name) {
        this.reason = reason;
        this.name = name;
    }

    public String getReason() {
        return reason;
    }

    public String getName() {
        return name;
    }

    public static Optional<SettlementDisputeReasonEnum> ofReason(String reason) {
        if (StrUtil.isBlank(reason)) {
            return Optional.empty();
        }
        return Arrays.stream(values()).filter(item -> item.reason.equals(reason)).findFirst();
    }

    public static String nameOf(String reason) {
        return ofReason(reason).map(SettlementDisputeReasonEnum::getName).orElse(null);
    }

}
