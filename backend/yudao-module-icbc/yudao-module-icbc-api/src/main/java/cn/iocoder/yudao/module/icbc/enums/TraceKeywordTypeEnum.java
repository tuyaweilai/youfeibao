package cn.iocoder.yudao.module.icbc.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * 关联单据查询的查号方式（#55 T17）。
 *
 * <p>「按单号 / 车牌 / 主体反查」落到具体实现时，单号本身有七八种（收购单号、交接批次号、
 * 采购订单号、入库单号、结算单号、发票号码 / 合作方订单号、支付单号）。{@link #AUTO}
 * 让使用者直接粘一个号进来，由服务端按固定顺序逐个试；也可以显式指定，避免同号异构时的歧义。
 */
public enum TraceKeywordTypeEnum {

    AUTO("AUTO", "自动识别", "按「收购单号 → 交接批次号 → 采购订单号 → 入库单号 → 结算单号 → 发票号 → 支付单号 → 车牌 → 出售者」的顺序逐个试"),
    ACQUISITION_NO("ACQUISITION_NO", "收购单号", "按收购登记单号反查"),
    HANDOVER_BATCH_NO("HANDOVER_BATCH_NO", "交接批次号", "按一次物理交接的批次号反查该批次下的全部收购单"),
    PURCHASE_ORDER_NO("PURCHASE_ORDER_NO", "采购订单号", "按采购执行依据反查它名下的收购单"),
    STOCK_IN_NO("STOCK_IN_NO", "入库单号", "按入库单号反查其来源收购单"),
    SETTLEMENT_NO("SETTLEMENT_NO", "结算单号", "按结算单号反查该批次下的全部收购单"),
    INVOICE_NO("INVOICE_NO", "发票号 / 合作方订单号", "按发票号码或合作方订单号反查收购单"),
    PAYMENT_ORDER_NO("PAYMENT_ORDER_NO", "支付单号", "按支付单号反查其来源收购单"),
    PLATE_NO("PLATE_NO", "车牌号", "按收购单的磅单 / 车辆车牌或交接批次的车牌反查"),
    SELLER("SELLER", "出售者主体", "按出售者姓名或手机号反查（跨企业的同一自然人不在此处合并）");

    private final String code;
    private final String name;
    private final String definition;

    TraceKeywordTypeEnum(String code, String name, String definition) {
        this.code = code;
        this.name = name;
        this.definition = definition;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getDefinition() {
        return definition;
    }

    public static Optional<TraceKeywordTypeEnum> ofCode(String code) {
        return Arrays.stream(values()).filter(item -> item.code.equalsIgnoreCase(code)).findFirst();
    }

}
