package cn.iocoder.yudao.module.icbc.enums;

/**
 * 关联单据追溯里的差异 / 缺失关联提示（#55 T17，AC4）。
 *
 * <p>入库量与结算量**不能默认一对一**：结算重量只是计价基准（ADR 0019 / 0028），
 * 实物在库量按仓库 / 库位 / 批次归属，两者不要求相等。这里只把差异说清楚、把缺失的关联点出来，
 * 不做任何抹平。异常表的权威口径以 #57（T19）为准，本枚举只服务追溯页面。
 */
public enum TraceDifferenceCodeEnum {

    STOCK_IN_VS_SETTLEMENT("STOCK_IN_VS_SETTLEMENT", "入库量与结算量不一致",
            "结算重量是计价基准，实物在库是另一个口径，两者不要求相等；差额需人工核实，不能默认一对一。"),
    MISSING_STOCK_IN_LINK("MISSING_STOCK_IN_LINK", "已结算但缺少入库记录",
            "这批货已归入结算单，却查不到已过账的入库记录；可能是尚未入库、入库待过账，或漏登记了关联。"),
    MISSING_SETTLEMENT_LINK("MISSING_SETTLEMENT_LINK", "已入库但尚未结算",
            "已经入库，却还没归入结算单；结算确认是开票硬前置，缺这一环后续走不通。"),
    WEIGHT_DIFF_VS_SETTLEMENT("WEIGHT_DIFF_VS_SETTLEMENT", "实物量与结算量的称量差异",
            "收购单上记录的实物量（接收量优先，无则净重）与结算重量不等，扣杂只扣价款、不扣库存。");

    private final String code;
    private final String name;
    private final String definition;

    TraceDifferenceCodeEnum(String code, String name, String definition) {
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

}
