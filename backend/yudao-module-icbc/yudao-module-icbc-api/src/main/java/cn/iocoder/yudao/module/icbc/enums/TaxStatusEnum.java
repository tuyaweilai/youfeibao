package cn.iocoder.yudao.module.icbc.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * 缴税状态（代办税费）。
 *
 * <p>工行的 {@code taxStatus} 不是「未缴 / 已缴」两个值，平台把它收敛成下面这组状态，
 * 让「缴税中」「异常（金额不一致）」「无需缴税」都能被看到。对应工行字典：
 *
 * <pre>
 * 00 初始 / 01 开票查询中 / 02 缴税中 / 03 缴税查询中 / 04 缴税成功 /
 * 05 缴税失败 / 97 缴税异常，缴税金额不一致 / 98 缴税异常，未知异常 / 99 无需缴税
 * </pre>
 *
 * <p>缴税成功（或无需缴税）后可出具缴税凭证；异常态给出下一步动作。
 */
public enum TaxStatusEnum {

    NOT_TAXED(0, "未缴税", "开票完成后由工行按代办税费流程自动缴税，无需操作"),
    TAXING(1, "缴税中", "缴税处理中，可在「查状态」看到最新进度；长时间未变请联系工行"),
    SUCCESS(2, "缴税成功", null),
    FAILED(3, "缴税失败", "缴税失败，请财务在「查状态」确认后重新发起缴税，或联系工行核对缴税付款账户"),
    ABNORMAL_AMOUNT(4, "缴税异常：缴税金额不一致", "缴税金额与应缴金额不一致，请财务核对发票税额与实缴税额后联系工行更正"),
    ABNORMAL_UNKNOWN(5, "缴税异常：未知异常", "缴税出现未知异常，请财务联系工行核实缴税结果"),
    NOT_REQUIRED(6, "无需缴税", null);

    private final Integer status;
    private final String name;
    private final String nextAction;

    TaxStatusEnum(Integer status, String name, String nextAction) {
        this.status = status;
        this.name = name;
        this.nextAction = nextAction;
    }

    public Integer getStatus() {
        return status;
    }

    public String getName() {
        return name;
    }

    public String getNextAction() {
        return nextAction;
    }

    /**
     * 工行 {@code taxStatus} 码转平台状态；未识别返回空，调用方应保持本地快照不动。
     */
    public static Integer toStatus(String code) {
        return ofCode(code).map(TaxStatusEnum::getStatus).orElse(null);
    }

    public static Optional<TaxStatusEnum> ofCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        switch (code) {
            case "00":
                return Optional.of(NOT_TAXED);
            case "01":
            case "02":
            case "03":
                return Optional.of(TAXING);
            case "04":
                return Optional.of(SUCCESS);
            case "05":
                return Optional.of(FAILED);
            case "97":
                return Optional.of(ABNORMAL_AMOUNT);
            case "98":
                return Optional.of(ABNORMAL_UNKNOWN);
            case "99":
                return Optional.of(NOT_REQUIRED);
            default:
                return Optional.empty();
        }
    }

    /**
     * 是否已结清（缴税成功或无需缴税），可出具缴税凭证
     */
    public static boolean isPaid(Integer status) {
        return SUCCESS.status.equals(status) || NOT_REQUIRED.status.equals(status);
    }

    /**
     * 是否为需要人工关注的异常态
     */
    public static boolean isException(Integer status) {
        return FAILED.status.equals(status) || ABNORMAL_AMOUNT.status.equals(status)
                || ABNORMAL_UNKNOWN.status.equals(status);
    }

    public static String nameOf(Integer status) {
        return ofStatus(status).map(TaxStatusEnum::getName).orElse("未知");
    }

    public static String nextActionOf(Integer status) {
        return ofStatus(status).map(TaxStatusEnum::getNextAction).orElse(null);
    }

    public static Optional<TaxStatusEnum> ofStatus(Integer status) {
        return Arrays.stream(values()).filter(item -> item.status.equals(status)).findFirst();
    }
}
