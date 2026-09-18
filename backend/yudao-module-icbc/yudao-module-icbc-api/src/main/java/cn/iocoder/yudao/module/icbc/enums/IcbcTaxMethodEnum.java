package cn.iocoder.yudao.module.icbc.enums;

import java.util.Optional;

/**
 * 计税方法：回收企业按品类配置，决定该品类可开具的票种。
 *
 * <p>简易计税不得开具增值税专用发票（票种 01），只能开普通发票（票种 02）；
 * 一般计税不限制票种。开票申请预下单时按商品明细的税收分类合并编码回查本枚举，
 * 命中简易计税即拦截专票。
 */
public enum IcbcTaxMethodEnum {

    /** 简易计税 */
    SIMPLE("SIMPLE", "简易计税"),
    /** 一般计税 */
    GENERAL("GENERAL", "一般计税");

    /**
     * 合法取值的正则。供 {@code @Pattern} 与枚举共用，避免合法集合散落多处。
     */
    public static final String PATTERN = "^(SIMPLE|GENERAL)$";

    private final String code;
    private final String name;

    IcbcTaxMethodEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public static Optional<IcbcTaxMethodEnum> ofCode(String code) {
        for (IcbcTaxMethodEnum method : values()) {
            if (method.code.equals(code)) {
                return Optional.of(method);
            }
        }
        return Optional.empty();
    }

    /**
     * 是否为简易计税。空值按一般计税处理，兼容历史未配置的数据。
     */
    public static boolean isSimple(String code) {
        return SIMPLE.code.equals(code);
    }

}
