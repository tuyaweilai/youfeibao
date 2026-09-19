package cn.iocoder.yudao.module.icbc.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * 代办税费申报的预警类型。
 *
 * <p>预警是<b>推导</b>出来的（截止日与当前时间、申报数据是否齐全），不落库，避免
 * 「任务没跑到就没有预警」。三类分别在申报期临近、已经逾期、以及数据不齐时出现。
 */
public enum TaxDeclarationWarningTypeEnum {

    /** 申报期临近：截止日进入预警窗口，仍未缴款 */
    DEADLINE_APPROACHING("DEADLINE_APPROACHING", "申报期临近",
            "核对缺项清单后尽快完成申报缴款"),
    /** 逾期未缴：已过申报期截止日仍未缴款，可能被暂停反向开票资格 */
    OVERDUE_SUSPENSION_RISK("OVERDUE_SUSPENSION_RISK", "逾期未缴，可能被暂停开票资格",
            "立即补报补缴；逾期未缴，主管税务机关会暂停本企业的反向开票资格"),
    /** 申报数据不齐：清单里还有无法申报的缺项 */
    MISSING_DATA("MISSING_DATA", "申报数据不齐",
            "按缺项清单补齐发票税额、出售者身份信息后重新生成清单");

    private final String code;
    private final String name;
    private final String nextAction;

    TaxDeclarationWarningTypeEnum(String code, String name, String nextAction) {
        this.code = code;
        this.name = name;
        this.nextAction = nextAction;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getNextAction() {
        return nextAction;
    }

    public static Optional<TaxDeclarationWarningTypeEnum> ofCode(String code) {
        return Arrays.stream(values()).filter(item -> item.code.equals(code)).findFirst();
    }

    public static String nameOf(String code) {
        return ofCode(code).map(TaxDeclarationWarningTypeEnum::getName).orElse(null);
    }

}
