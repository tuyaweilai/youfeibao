package cn.iocoder.yudao.module.icbc.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * 工作台待办项（#56 T18）。
 *
 * <p>工作台一屏给出「今天我该处理什么」。本枚举是**待办项的唯一来源**：编码、名称与口径都在这里，
 * 服务实现按 {@code code} 取数，前端按 {@code code} 决定下钻到哪个模块。
 *
 * <p>每条待办都带一句 {@link #getDefinition()}（口径），界面上与数字一起显示——用户看到的每个
 * 数字都能追到「它是怎么算的」，这是 ADR 0021 对状态与数字的要求。
 *
 * <p><b>为什么有取不到数的项</b>：待称重 / 待验收属于「现场交接批次」这条链，批次（T12）与拒收
 * 与部分接收（T15）尚未全部接入。此时若拿别的字段硬凑一个数字，会给出一个用户无法处理的待办。
 * 这类项的 {@link #getUnavailableReason()} 非空，界面上标注「待接入」并写明原因，而不是伪造一个数字。
 * （待入库在 #52 落地后已接入，见 {@link #PENDING_STOCK_IN}。）
 */
public enum WorkbenchTodoCodeEnum {

    ARRIVAL_TODAY("ARRIVAL_TODAY", "今日到场 / 上门", null,
            "本租户待到站的预约中，预计到站时间不晚于今日的（含已逾期未处理的）。"
                    + "交接批次（T12）的「上门」来源接入后并入本项。"),

    PENDING_WEIGH("PENDING_WEIGH", "待称重", null,
            "已登记但还没录磅重的收购单（净重为空）。登记要件允许重量留空，这类单就是「车在、磅还没过」。"),

    PENDING_INSPECT("PENDING_INSPECT", "待验收", null,
            "已录磅重、尚未归入结算单的收购单：现场还没点「结束本次收货」。"
                    + "归批即现场对这一车货的接收动作；拒收与部分接收（T15）落地后再细分验收结论。"),

    PENDING_STOCK_IN("PENDING_STOCK_IN", "待入库", null,
            "已验收（已归入结算单）、未作废，且可入库实物量（接收量优先，无则净重）尚未全部入库的收购单。"
                    + "只计已过账的入库单；拒收与退回的部分不计入实物量（T15），已全部入库的不再列为待办。"),

    PENDING_SETTLE_CONFIRM("PENDING_SETTLE_CONFIRM", "待结算确认", null,
            "处于「待确认」的结算单：已生成或已改版，等出售者勾选确认。确认是开票的硬前置。"),

    SETTLE_DISPUTE("SETTLE_DISPUTE", "异议", null,
            "处于「有异议」的结算单：出售者对计量或计价提出不同意见，等企业改版或附说明后重新确认。"),

    PAYMENT_FAILED("PAYMENT_FAILED", "付款失败", null,
            "处于异常状态的支付单：支付失败 / 订单关闭 / 已冲正 / 已退汇 / 他行已扣款本行未入账 / 部分成功。"),

    INVOICE_FAILED("INVOICE_FAILED", "票务失败", null,
            "预开票失败、开票失败、缴税失败或缴税异常、上传失败四条状态线任一异常的发票（同一张票只算一条）。"
                    + "红冲是另一张单（红字冲销）的状态线，不在本项口径内。"),

    PAYMENT_PENDING_TIMEOUT("PAYMENT_PENDING_TIMEOUT", "待付款超时", null,
            "预开票成功、尚未付款成功，且预下单已超过配置天数（icbc.invoice.pending-payment-days，默认 7 天）的票。"
                    + "票已经备好、企业一直没打钱。超时**只提醒不自动取消**：取消预开票要走工行的 reversal，是企业的决定。");

    private final String code;
    private final String name;
    private final String unavailableReason;
    private final String definition;

    WorkbenchTodoCodeEnum(String code, String name, String unavailableReason, String definition) {
        this.code = code;
        this.name = name;
        this.unavailableReason = unavailableReason;
        this.definition = definition;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    /**
     * 数据源尚未上线的原因；取得到数时为 {@code null}。
     */
    public String getUnavailableReason() {
        return unavailableReason;
    }

    /**
     * 口径说明：这个数字是怎么算出来的。
     */
    public String getDefinition() {
        return definition;
    }

    public static Optional<WorkbenchTodoCodeEnum> ofCode(String code) {
        return Arrays.stream(values()).filter(item -> item.code.equals(code)).findFirst();
    }

}
