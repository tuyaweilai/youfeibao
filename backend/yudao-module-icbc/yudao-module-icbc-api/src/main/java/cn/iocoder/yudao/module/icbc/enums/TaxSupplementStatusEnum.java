package cn.iocoder.yudao.module.icbc.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * 需补缴税费的处理状态。
 *
 * <p>补缴是申报之后才暴露出来的差额：重新计算发现销售额增加、跨期红冲调整、或事后被
 * 要求补税。它不能悄悄改掉一份已经缴清的申报单，所以单独立一条记录，可跟进、可结案。
 */
public enum TaxSupplementStatusEnum {

    /** 待补缴 */
    PENDING(0, "待补缴", "在申报期内完成补缴并归档缴款凭证"),
    /** 已补缴 */
    PAID(1, "已补缴", null);

    private final Integer status;
    private final String name;
    private final String nextAction;

    TaxSupplementStatusEnum(Integer status, String name, String nextAction) {
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

    public static Optional<TaxSupplementStatusEnum> ofStatus(Integer status) {
        return Arrays.stream(values()).filter(item -> item.status.equals(status)).findFirst();
    }

    public static String nameOf(Integer status) {
        return ofStatus(status).map(TaxSupplementStatusEnum::getName).orElse("未知");
    }

    public static String nextActionOf(Integer status) {
        return ofStatus(status).map(TaxSupplementStatusEnum::getNextAction).orElse(null);
    }

}
