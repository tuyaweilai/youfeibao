package cn.iocoder.yudao.module.icbc.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * 进项发票票种（#49 T11）。
 *
 * <p>单位供货方向回收企业开具的增值税发票只有两种票种：**专用发票**（可抵扣）与
 * **普通发票**（一般不抵扣）。一期只做到「收票登记与勾稽」，不做发票查验平台对接，
 * 所以票种由财务在登记时选择并作为票面事实留档，见 ADR 0029。
 *
 * <p>票种与「能不能抵」不是一回事：票种是专票，**且**回收企业自身不是简易计税 / 免税项目，
 * 才谈得上进项抵扣；后者是回收企业侧的属性，不在本枚举内。
 */
public enum InputInvoiceTypeEnum {

    /** 增值税专用发票 */
    SPECIAL(1, "增值税专用发票"),
    /** 增值税普通发票 */
    GENERAL(2, "增值税普通发票");

    /** 票种编码 */
    private final Integer type;
    /** 票种名称 */
    private final String name;

    InputInvoiceTypeEnum(Integer type, String name) {
        this.type = type;
        this.name = name;
    }

    public Integer getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public static Optional<InputInvoiceTypeEnum> ofType(Integer type) {
        return Arrays.stream(values()).filter(item -> item.type.equals(type)).findFirst();
    }

    public static String nameOf(Integer type) {
        return ofType(type).map(InputInvoiceTypeEnum::getName).orElse(null);
    }

}
