package cn.iocoder.yudao.module.icbc.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 收方审核回调的操作类型：区分这条审核结果是「新增」还是「修改（换卡）」。
 *
 * <p>工行《智慧清分收方审核回调示例文档》里，新增回调的报文**没有** {@code operaType}，
 * 修改回调一定带 {@code operaType="02"}。查询接口没有这个字段，但它用 {@code auditStatus}
 * 表达同一件事：{@code 3-修改审核中}。
 *
 * <p>为什么要显式区分：两种结果落在**不同的对象**上——新增结果改收方档案的建档状态，
 * 修改结果只收敛换卡单（新卡审核通过才生效，被拒时原卡继续有效）。以前靠「有没有在途换卡单」
 * 推断，会把「本机已取消、工行仍在下发」的修改结果错当成新增结果改写建档状态（#86）。
 */
@Getter
@AllArgsConstructor
public enum PayeeOnboardingOperaTypeEnum {

    /** 收方修改（换卡）：回调一定返回 "02" */
    MODIFY("02", "收方修改");

    /**
     * 查询接口里表示「修改审核中」的 auditStatus
     */
    public static final String AUDIT_STATUS_MODIFYING = "3";

    private final String code;
    private final String name;

    public static PayeeOnboardingOperaTypeEnum ofCode(String code) {
        return MODIFY.code.equals(code) ? MODIFY : null;
    }

    /**
     * 回调是不是一次「收方修改」。缺省 / 未知都当新增——新增那份报文本来就不带 {@code operaType}。
     */
    public static boolean isModify(String operaType) {
        return MODIFY.code.equals(operaType);
    }

    /**
     * 查询结果是不是「修改审核中」（{@code auditStatus=3}）。
     */
    public static boolean isModifyAuditStatus(String auditStatus) {
        return AUDIT_STATUS_MODIFYING.equals(auditStatus);
    }

}
