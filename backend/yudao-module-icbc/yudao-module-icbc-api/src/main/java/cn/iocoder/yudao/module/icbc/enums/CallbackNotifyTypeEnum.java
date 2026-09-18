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
 */
@Getter
@AllArgsConstructor
public enum CallbackNotifyTypeEnum {

    PRE_ORDER_EXCEPTION("01", "预下单异常信息"),
    PAYMENT("02", "b2b 支付"),
    INVOICE("03", "开票"),
    TAX("04", "缴税"),
    INVOICE_UPLOAD("05", "发票上传"),
    INVOICE_CANCEL("06", "发票取消"),
    RED_APPLY("07", "红票申请"),
    RED_UPLOAD("08", "红票上传"),
    RED_REVOKE("09", "红票撤销");

    /**
     * 通知类型编码
     */
    private final String type;
    /**
     * 类型名
     */
    private final String name;

    public static CallbackNotifyTypeEnum of(String type) {
        return Arrays.stream(values())
                .filter(item -> item.type.equals(type))
                .findFirst()
                .orElse(null);
    }

}
