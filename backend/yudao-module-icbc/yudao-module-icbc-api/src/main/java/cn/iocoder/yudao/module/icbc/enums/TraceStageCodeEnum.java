package cn.iocoder.yudao.module.icbc.enums;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * 关联单据追溯的阶段（#55 T17）。
 *
 * <p>「这批货经历了什么」按固定的六个阶段铺开：采购订单 → 现场收货 → 仓储入库 → 结算确认，
 * 后面接付款与发票。前四栏是规格 #38 里 M09 的四栏，付款 / 发票是四栏之后的收尾。
 *
 * <p>阶段的口径（{@link #getDefinition()}）随响应一起返回：前端只展示，不另算一遍；
 * 「哪些环节本来就不需要」（如「直接收购」没有采购订单）由 {@link TraceStageStatusEnum#NOT_APPLICABLE}
 * 表达，而不是留空。
 */
public enum TraceStageCodeEnum {

    PURCHASE_ORDER("PURCHASE_ORDER", "采购订单",
            "本次收购挂靠的采购执行依据（采购订单 + 品类明细）。零散散户未关联时标为「直接收购」，是无需该环节，不是缺失。"),
    RECEIPT("RECEIPT", "现场收货",
            "收购登记单本身：磅重、结算重量、接收结论与作废状态；它同一次物理交接可拆成多张收购单（多品类）。"),
    STOCK_IN("STOCK_IN", "仓储入库",
            "由收购单派生的入库动作。只有过账的入库才增加正式库存；待过账不算入库，已作废的按冲销处理。"),
    SETTLEMENT("SETTLEMENT", "结算确认",
            "一次到场批次（同出售者 + 同场站）的结算单与出售者确认状态；确认是开票的硬前置。"),
    PAYMENT("PAYMENT", "付款",
            "公对私结算的支付单：银行处理结果、实际到账金额与回单。平台只发指令、归集回单，不碰资金。"),
    INVOICE("INVOICE", "发票",
            "反向开票的预下单、开票、缴税与上传四条状态线；红冲是另一张单，单独展示。");

    private final String code;
    private final String name;
    private final String definition;

    TraceStageCodeEnum(String code, String name, String definition) {
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

    /** 固定顺序：四栏在前，付款 / 发票在后。 */
    public static List<TraceStageCodeEnum> ordered() {
        return Arrays.asList(values());
    }

    public static Optional<TraceStageCodeEnum> ofCode(String code) {
        return Arrays.stream(values()).filter(item -> item.code.equals(code)).findFirst();
    }

}
