package cn.iocoder.yudao.module.icbc.service.admission;

/**
 * 卖方主体准入（issue #48，见 ADR 0029）。
 *
 * <p>政策上「反向开票」只对**自然人**开放：国家税务总局公告 2024 年第 5 号第一条把出售者定义为自然人，
 * 因此个体工商户 / 个人独资企业 / 合伙企业 / 企业法人 / 农民专业合作社一律不能走反向开票，由对方
 * 自行开具增值税发票、我们收票。本服务把这个判断收成**硬约束**，不靠操作员判断。
 *
 * <p><b>必须与「租户资格」分开</b>：5 号公告第二条明确「资源回收企业（包括单位和个体工商户）」，
 * 即个体工商户**可以**作为回收企业去反向开票。前者是卖方的准入，后者是租户的资格，是两个位置，
 * 不能用一个门禁互相推断。
 */
public interface SellerAdmissionService {

    /**
     * 反向开票准入硬约束：卖方主体必须是自然人，否则抛
     * {@link cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants#SELLER_SUBJECT_TYPE_NOT_NATURAL}。
     *
     * @param sellerSubjectType 卖方主体类型，见 {@link cn.iocoder.yudao.module.erp.enums.purchase.SellerSubjectTypeEnum}
     */
    void assertReverseInvoiceAllowed(Integer sellerSubjectType);

    /**
     * 不做抛出的判定。{@code null} 视为自然人——反向开票通道此前只对自然人开放，历史单据没有该字段，
     * 按自然人放行才能让老单开出票。
     */
    boolean isReverseInvoiceAllowed(Integer sellerSubjectType);

    /**
     * 非自然人时的可读提示（哪里不满足），供开票前置校验直接展示。
     */
    String reverseInvoiceRejectionMessage(Integer sellerSubjectType);

}
