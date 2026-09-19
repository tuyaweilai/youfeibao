package cn.iocoder.yudao.module.icbc.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * 红字冲销原因（工行 {@code redOffsetReason}）。
 *
 * <p>四种原因对应的事实不同，凭证要求也不同：<strong>开票有误</strong>必须全额红冲，
 * 且明细的单价、金额、数量必须与原蓝票一致；其余三种按实际退货 / 中止 / 折让金额冲。
 * 平台不把四种原因压成一种，也不允许用户「选错原因」——
 * {@link #MIS_INVOICED} 走全额校验，其余走按额校验。
 */
public enum RedInvoiceReasonEnum {

    MIS_INVOICED("01", "开票有误", true),
    SALE_RETURNED("02", "销货退回", false),
    SERVICE_SUSPENDED("03", "服务中止", false),
    PRICE_DISCOUNT("04", "销售折让", false);

    private final String code;
    private final String name;
    /** 是否必须全额红冲（明细与蓝票一致） */
    private final boolean fullOffsetRequired;

    RedInvoiceReasonEnum(String code, String name, boolean fullOffsetRequired) {
        this.code = code;
        this.name = name;
        this.fullOffsetRequired = fullOffsetRequired;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public boolean isFullOffsetRequired() {
        return fullOffsetRequired;
    }

    public static Optional<RedInvoiceReasonEnum> ofCode(String code) {
        return Arrays.stream(values()).filter(item -> item.code.equals(code)).findFirst();
    }

    public static String nameOf(String code) {
        return ofCode(code).map(RedInvoiceReasonEnum::getName).orElse("未知");
    }
}
