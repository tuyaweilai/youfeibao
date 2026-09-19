package cn.iocoder.yudao.module.icbc.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * 税费缴纳方式。
 *
 * <p>对应工行通知里的 {@code taxPaymentMethod}：{@code 0} 自然人自行办理、{@code 1} 企业委托扣缴。
 * 代办税费是回收企业的法定义务，凭证上要写清这一笔由谁缴纳，所以把它显式建模，而不是散落的字符串比较。
 */
public enum IcbcTaxPaymentMethodEnum {

    NATURAL_SELF("0", "自然人自行办理"),
    ENTERPRISE_WITHHOLDING("1", "企业委托扣缴");

    private final String code;
    private final String name;

    IcbcTaxPaymentMethodEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public static Optional<IcbcTaxPaymentMethodEnum> ofCode(String code) {
        return Arrays.stream(values()).filter(item -> item.code.equals(code)).findFirst();
    }

    /**
     * 工行码转名称；未识别或为空返回 {@code null}
     */
    public static String nameOf(String code) {
        return ofCode(code).map(IcbcTaxPaymentMethodEnum::getName).orElse(null);
    }
}
