package cn.iocoder.yudao.module.icbc.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * 公开令牌的用途。每类用途绑定不同的业务对象、配不同的使用次数上限。
 *
 * <p>令牌给的是「没有账号的自然人」：它必须自己解析出租户与业务单，所以业务键类型
 * 在这里写死，mint 时按类型校验被绑定的业务是否存在。
 */
public enum PublicTokenPurposeEnum {

    /** 发票 PDF 下载：绑定一张票（合作方订单号），单次有效 */
    INVOICE_DOWNLOAD("INVOICE_DOWNLOAD", "发票下载", BusinessKeyType.ORDER, 1),
    /** 收方入驻失败后留联系方式：绑定一个收方，单次有效 */
    CONTACT_LEAD("CONTACT_LEAD", "失败留联系方式", BusinessKeyType.PAYEE, 1),
    /** 额度查询：绑定一个自然人收方，有效期内限次使用（页面可刷新） */
    QUOTA_QUERY("QUOTA_QUERY", "额度查询", BusinessKeyType.PAYEE, 20),
    /** 汇算清缴对账：绑定一个自然人收方，出售者查自己的开票与已缴税款（可刷新） */
    SETTLEMENT_STATEMENT("SETTLEMENT_STATEMENT", "汇算清缴对账", BusinessKeyType.PAYEE, 20),
    /** 出售者建档：绑定一个自然人收方，让自然人在自己手机上完成工行实名 / 收方入驻（可重开页面） */
    ONBOARDING("ONBOARDING", "出售者建档", BusinessKeyType.PAYEE, 20),
    /**
     * 触达通知（#36）：短信 / 收货员转达的链接，绑定一个自然人收方，让他打开就能看到
     * 「待确认的结算 / 付款异常 / 已开出的票」，**不需要先注册**（ADR 0023）。
     */
    SELLER_NOTICE("SELLER_NOTICE", "触达通知", BusinessKeyType.PAYEE, 20);

    /**
     * 令牌绑定的业务键类型。
     */
    public enum BusinessKeyType {
        /** 业务键是合作方订单号 */
        ORDER,
        /** 业务键是收方 ID */
        PAYEE
    }

    private final String code;
    private final String name;
    private final BusinessKeyType businessKeyType;
    private final int maxUses;

    PublicTokenPurposeEnum(String code, String name, BusinessKeyType businessKeyType, int maxUses) {
        this.code = code;
        this.name = name;
        this.businessKeyType = businessKeyType;
        this.maxUses = maxUses;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public BusinessKeyType getBusinessKeyType() {
        return businessKeyType;
    }

    public int getMaxUses() {
        return maxUses;
    }

    public static Optional<PublicTokenPurposeEnum> ofCode(String code) {
        return Arrays.stream(values())
                .filter(purpose -> purpose.code.equals(code))
                .findFirst();
    }

}
