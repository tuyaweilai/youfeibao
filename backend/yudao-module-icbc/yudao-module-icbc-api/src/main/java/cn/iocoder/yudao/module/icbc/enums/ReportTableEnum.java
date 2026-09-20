package cn.iocoder.yudao.module.icbc.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * 经营报表的五张表（#57 T19，用户故事 11 / 21 / 47）。
 *
 * <p>报表最容易出的问题不是算错，而是**口径混用**：把预约约量当成实际收购量、把计划量当成履约量、
 * 把结算重量当成在库量。所以每张表都必须能说清「这个数字从哪来、代表什么」，口径定义在这里唯一登记，
 * 随响应返回给前端展示，不在前端另写一遍。
 *
 * <p>「实际收购量」只取 {@code icbc_acquisition} 上已经发生的计量结果（毛 / 皮 / 净 / 结算 / 接收），
 * **不含预约约量（{@code icbc_appointment.approximate_quantity}）与采购计划量
 * （{@code icbc_purchase_order_item.quantity}）**。
 */
public enum ReportTableEnum {

    PURCHASE_PERFORMANCE("PURCHASE_PERFORMANCE", "采购履约表",
            "计划量 / 实际履约量 / 余额 / 到期日 / 进度 / 超量与过期异常。"
                    + "实际履约量与完成比例一律采用采购履约配置里选定的口径（验收或结算，见 #47），"
                    + "计划量只作分母，不混入实际收购量。"),

    ACQUISITION_LEDGER("ACQUISITION_LEDGER", "收购台账",
            "交易对方 / 场站 / 回收方式（直接收购或采购订单）/ 品类与等级 / 各重量口径 / 成交金额与对应单据。"
                    + "各重量口径分列：毛重、皮重、净重、结算重量（计价基准，ADR 0019）、接收量 / 退回量 / 余货出场量。"
                    + "**实际收购量只取已发生的收购单计量结果，预约约量与采购计划量不混入。**"),

    STOCK("STOCK", "库存表",
            "入出流水与在库量，维度是品类 + 仓库 + 库位 + 批次，带库龄（批次入库时间起算）。"
                    + "**只给数量口径，不展示成本**（ADR 0027：本期库存不做成本核算）；"
                    + "实物在库量与结算重量不要求相等（ADR 0028），差额在异常表可见。"),

    SETTLEMENT_PAYMENT("SETTLEMENT_PAYMENT", "结算付款表",
            "结算金额 / 办理进度 / 回单状态 / 失败原因 / 未办理时长。"
                    + "结算金额 = 该结算单下未作废收购单的应付金额合计（ADR 0021：只报可核验的金额）；"
                    + "付款仍按收购单逐笔进行，本表按结算单汇总进度，不把多张收购单变成一笔银行付款。"),

    ANOMALY("ANOMALY", "异常表",
            "派生清单，不新建表（与额度台账 ADR 0014 同一做法）：磅差、超采购量、超入库量、"
                    + "重复关联、长期未确认、资料缺失。每一条都指得到来源单据，判定口径见异常类型枚举。");

    private final String code;
    private final String name;
    private final String definition;

    ReportTableEnum(String code, String name, String definition) {
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

    /**
     * 口径说明：这张表的数字从哪来、代表什么。
     */
    public String getDefinition() {
        return definition;
    }

    public static Optional<ReportTableEnum> ofCode(String code) {
        return Arrays.stream(values()).filter(item -> item.code.equals(code)).findFirst();
    }

}
