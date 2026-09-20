package cn.iocoder.yudao.module.icbc.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * 采购订单履约五口径（#47 T09，用户故事 21）。
 *
 * <p>一张订单的履约情况必须**分口径**说清楚：计划、验收、入库、结算、未履行。五个口径各有各的来源，
 * 不能相加、不能互相代替——「订单完成 80%」这种说法只有在标明按哪个口径算之后才有意义。
 *
 * <p>本枚举是五口径的**唯一来源**：编码、名称、口径（{@link #getDefinition()}）、数据来源
 * （{@link #getSource()}）都在这里，服务实现按 {@code code} 取数，前端按 {@code code} 决定怎么展示。
 *
 * <p><b>为什么「入库」取不到数</b>：入库单是 T14（#52）才有的东西。在它落地前，「已入库」与
 * 「待入库」区分不开，硬算会给出一个**用户无法清零**的数字。所以这一项的
 * {@link #getUnavailableReason()} 非空，界面上标注「待接入」并写明原因，而不是伪造一个数字。
 * 落地后按「成交记录来源收购单已入库」接入，见 {@link #getDefinition()}。
 */
public enum PurchaseProgressMeasureEnum {

    /** 计划：订单自己的约定量 */
    PLAN("PLAN", "计划",
            "订单明细的计划量之和（icbc_purchase_order_item.quantity 的汇总快照）。",
            "icbc_purchase_order_item.quantity", null),

    /** 验收：到场验收 / 计量后挂到订单的量 */
    ACCEPTED("ACCEPTED", "验收",
            "已挂到本订单的成交数量之和（icbc_purchase_order_deal.quantity）。"
                    + "成交记录就是「这一车货验收了多少」的载体；退货记负数，自动按本口径扣回。",
            "icbc_purchase_order_deal.quantity", null),

    /** 入库：货实际放进了库里（#52 才有的动作） */
    STOCKED_IN("STOCKED_IN", "入库",
            "入库单落地后按「成交记录来源收购单已入库」取数：只计已过账的入库存量，"
                    + "待入库与拒收部分不计入。实物入库量与结算重量不要求相等（ADR 0028），差额在异常表可见。",
            "icbc_stock_in（T14 / #52 落地后接入）",
            "入库动作（T14 / #52）尚未上线：系统还没有入库单，「已入库」与「待入库」无法区分，"
                    + "本项暂不计数，以免给出一个无法处理的数字。"),

    /** 结算：已被结算单归集的量（结算重量口径，ADR 0019） */
    SETTLED("SETTLED", "结算",
            "本订单的成交记录中，来源收购单已归入结算单（icbc_acquisition.settlement_id 非空）的那部分数量之和。"
                    + "结算单「已生成」即计入，是否已确认、是否有异议看结算单自身的状态，不在这里重复表达。",
            "icbc_purchase_order_deal.quantity（按来源收购单的结算状态过滤）", null),

    /** 未履行：还欠多少货 */
    UNPERFORMED("UNPERFORMED", "未履行",
            "计划量 − 本单采用的履约口径量（口径见 completionBasis / completionBasisName）。"
                    + "负数表示超收，超收同样是一种要被看见的履约异常。",
            "计划量与本单履约口径量相减", null);

    private final String code;
    private final String name;
    private final String definition;
    private final String source;
    private final String unavailableReason;

    PurchaseProgressMeasureEnum(String code, String name, String definition, String source,
                                String unavailableReason) {
        this.code = code;
        this.name = name;
        this.definition = definition;
        this.source = source;
        this.unavailableReason = unavailableReason;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    /**
     * 口径说明：这个数字是怎么算出来的。
     */
    public String getDefinition() {
        return definition;
    }

    /**
     * 数据来源：这个数字取自哪里。
     */
    public String getSource() {
        return source;
    }

    /**
     * 数据源尚未上线的原因；取得到数时为 {@code null}。
     */
    public String getUnavailableReason() {
        return unavailableReason;
    }

    /**
     * 该口径现在是否取得到数。
     */
    public boolean isAvailable() {
        return unavailableReason == null;
    }

    public static Optional<PurchaseProgressMeasureEnum> ofCode(String code) {
        return Arrays.stream(values()).filter(item -> item.code.equals(code)).findFirst();
    }

    public static String nameOf(String code) {
        return ofCode(code).map(PurchaseProgressMeasureEnum::getName).orElse(null);
    }

}
