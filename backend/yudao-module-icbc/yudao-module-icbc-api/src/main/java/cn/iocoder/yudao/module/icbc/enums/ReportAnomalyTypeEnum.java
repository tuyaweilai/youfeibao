package cn.iocoder.yudao.module.icbc.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * 经营报表「异常表」的六类异常（#57 T19，用户故事 11 / 47）。
 *
 * <p>异常表是**派生清单，不新建表**（与额度台账 ADR 0014 同一做法）：每一条都来自已经存在的业务单据，
 * 不重算别人已经算好的口径——磅差直接读 {@code icbc_acquisition.weight_diff}（#53 落库），
 * 超采购量取自 #47 的履约口径，超入库量对比 #52 的累计入库与实物量，等等。
 *
 * <p>每类异常的判定口径（{@link #getDefinition()}）与建议下钻入口（{@link #getDrillDown()}）
 * 都在这里唯一登记，前端只展示、不自己判。
 */
public enum ReportAnomalyTypeEnum {

    /** 磅差：实物量（接收量优先，无则净重）− 结算重量，ADR 0028 要求差额可见、不静默抹平。 */
    WEIGHT_DIFF("WEIGHT_DIFF", "磅差",
            "实物量（接收量优先，无接收结论时取净重）− 结算重量，取 #53 已落库的 weight_diff，差额不为 0 即为异常。"
                    + "正数表示实物多于计价，负数表示计价多于实物。",
            "DANGER", "acquisition-weight-diff"),

    /** 超采购量：采购订单明细的验收量超过计划量（消费 #47 的履约口径，不重算）。 */
    OVER_PURCHASE_QUANTITY("OVER_PURCHASE_QUANTITY", "超采购量",
            "采购订单明细的**验收量**（#47 履约口径 ACCEPTED）超过该明细的计划量；超出的部分会被 #47 的交货门禁"
                    + "按配置拦截或要求授权，本表把已经发生的超量列出来。",
            "DANGER", "purchase-order-progress"),

    /** 超入库量：同一收购单累计已过账入库量超过可入库实物量（#52 应在过账时拦住，这里只读复核）。 */
    OVER_STOCK_IN("OVER_STOCK_IN", "超入库量",
            "同一收购单**累计已过账**的入库量超过可入库实物量（接收量优先，无接收结论时取净重）。"
                    + "#52 在过账时会用 maxCount 拦住，本表只读复核，防止历史数据或并发下出现越界。",
            "DANGER", "stock-in"),

    /** 重复关联：同一收购单关联了多张未作废付款单（重复付款风险）。 */
    DUPLICATE_LINK("DUPLICATE_LINK", "重复关联",
            "同一收购单关联了多张未作废付款单（状态不是订单关闭 / 已冲正），存在**重复付款**风险；"
                    + "付款按收购单逐笔进行，正常情况下一张收购单只应有一笔有效付款。",
            "WARNING", "payment"),

    /** 长期未确认：结算单过了确认截止时间仍未确认（待确认 / 有异议 / 需线下签字确认）。 */
    LONG_UNCONFIRMED("LONG_UNCONFIRMED", "长期未确认",
            "结算单处于待确认 / 有异议 / 需线下签字确认，且已过确认截止时间（deadline_time；"
                    + "未设截止时间时按生成时间 + 48 小时）。结算确认是开票申请的硬前置（ADR 0018），长期不确认会拖住后续。",
            "WARNING", "settlement"),

    /** 资料缺失：未作废收购单缺失五流骨架要件。 */
    MISSING_EVIDENCE("MISSING_EVIDENCE", "资料缺失",
            "未作废收购单缺失五流骨架要件：品类 / 交易时间 / 交易地点 / 出售者 / 重量（净重）/ 单价 / 磅单号，"
                    + "任一为空即列为缺失。五流齐备是反向开票的交付指标，缺项应在开票前补齐。",
            "WARNING", "acquisition");

    private final String code;
    private final String name;
    private final String definition;
    private final String severity;
    private final String drillDown;

    ReportAnomalyTypeEnum(String code, String name, String definition, String severity, String drillDown) {
        this.code = code;
        this.name = name;
        this.definition = definition;
        this.severity = severity;
        this.drillDown = drillDown;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    /**
     * 判定口径：这类异常是怎么判出来的。
     */
    public String getDefinition() {
        return definition;
    }

    /**
     * 严重程度：DANGER（会直接影响钱 / 合规）/ WARNING（要跟进但不阻塞）。
     */
    public String getSeverity() {
        return severity;
    }

    /**
     * 建议下钻入口（前端用它决定跳到哪个模块的明细）。
     */
    public String getDrillDown() {
        return drillDown;
    }

    public static Optional<ReportAnomalyTypeEnum> ofCode(String code) {
        return Arrays.stream(values()).filter(item -> item.code.equals(code)).findFirst();
    }

    public static String nameOf(String code) {
        return ofCode(code).map(ReportAnomalyTypeEnum::getName).orElse(null);
    }

}
