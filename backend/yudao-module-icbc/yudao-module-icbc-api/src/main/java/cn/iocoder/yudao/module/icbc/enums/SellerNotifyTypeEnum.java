package cn.iocoder.yudao.module.icbc.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * 出售者触达的类型（#36，ADR 0023）。
 *
 * <p>一期触达只有两条路：短信（可配置开关）与**收货员一键把确认链接转达给他**。
 * 短信**只发这三条**，多一条都不发——每条都对应一个他能自己处理的动作。
 * 每类绑定一个短信模板编码，模板落 {@code system_sms_template}。
 */
public enum SellerNotifyTypeEnum {

    /** 结算单待确认：确认是开票硬前置，他得知道有一单在等他 */
    SETTLEMENT_PENDING("SETTLEMENT_PENDING", "结算单待确认", "icbc_seller_notify_settlement_pending"),
    /** 付款异常（失败 / 退汇 / 冲正 / 部分成功等）：钱没走通，他要知道下一步 */
    PAYMENT_EXCEPTION("PAYMENT_EXCEPTION", "付款异常", "icbc_seller_notify_payment_exception"),
    /** 发票已开出：票出来了，他可以下载留存 */
    INVOICE_ISSUED("INVOICE_ISSUED", "发票已开出", "icbc_seller_notify_invoice_issued");

    private final String code;
    private final String name;
    private final String templateCode;

    SellerNotifyTypeEnum(String code, String name, String templateCode) {
        this.code = code;
        this.name = name;
        this.templateCode = templateCode;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getTemplateCode() {
        return templateCode;
    }

    public static Optional<SellerNotifyTypeEnum> ofCode(String code) {
        return Arrays.stream(values()).filter(item -> item.code.equals(code)).findFirst();
    }

    public static String nameOf(String code) {
        return ofCode(code).map(SellerNotifyTypeEnum::getName).orElse(null);
    }

}
