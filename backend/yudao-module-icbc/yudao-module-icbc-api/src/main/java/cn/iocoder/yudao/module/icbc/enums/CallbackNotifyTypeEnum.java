package cn.iocoder.yudao.module.icbc.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 工行异步通知类型
 *
 * 来源：`docs/icbc/银税协同通知报文说明-250331.doc`，共九类。报文外层为
 * `{ "notifyData": "<base64 JSON>", "signData": "..." }`。
 * 这九类从同一个入口进入、一律先落表后处理。
 *
 * <p>此外，出售者建档（#6）的两类回调（实人认证、收方入驻）也收在同一个入口：
 * 它们的报文没有 {@code notifyType} 字段，由 {@code IcbcNotifyParser} 按特征字段
 * 推断为 {@link #FACE_VERIFY} 与 {@link #PAYEE_ONBOARDING}，以复用落表 + 重放能力。
 */
@Getter
@AllArgsConstructor
public enum CallbackNotifyTypeEnum {

    PRE_ORDER_EXCEPTION("01", "预下单异常信息", "开票申请"),
    PAYMENT("02", "b2b 支付", "付款"),
    INVOICE("03", "开票", "开票"),
    TAX("04", "缴税", "缴税"),
    INVOICE_UPLOAD("05", "发票上传", "发票上传"),
    INVOICE_CANCEL("06", "发票取消", "发票取消"),
    RED_APPLY("07", "红票申请", "红冲"),
    RED_UPLOAD("08", "红票上传", "红冲"),
    RED_REVOKE("09", "红票撤销", "红冲"),
    /** 出售者建档：实人认证结果通知（报文无 notifyType，由特征字段推断） */
    FACE_VERIFY("10", "实人认证结果", "出售者建档"),
    /** 出售者建档：收方入驻 / 审核结果通知（报文无 notifyType，由特征字段推断） */
    PAYEE_ONBOARDING("11", "收方入驻结果", "出售者建档");

    /**
     * 通知类型编码
     */
    private final String type;
    /**
     * 类型名
     */
    private final String name;
    /**
     * 关联业务的中文名：这个通知说的是哪一类业务
     */
    private final String businessName;

    public static CallbackNotifyTypeEnum of(String type) {
        return Arrays.stream(values())
                .filter(item -> item.type.equals(type))
                .findFirst()
                .orElse(null);
    }

    /**
     * 是否为银税協同的九类通知（01–09）。10/11 是 #6 追加的出售者建档回调，
     * 平台通知概览只统计九类。
     */
    public boolean isBankNotify() {
        return Integer.parseInt(type) <= 9;
    }

    /**
     * 关联业务名；未知类型不伪造，返回 null。
     */
    public static String businessNameOf(String type) {
        CallbackNotifyTypeEnum typeEnum = of(type);
        return typeEnum != null ? typeEnum.getBusinessName() : null;
    }

}
