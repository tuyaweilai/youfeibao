package cn.iocoder.yudao.module.icbc.service.admission.impl;

import cn.iocoder.yudao.module.erp.enums.purchase.SellerSubjectTypeEnum;
import cn.iocoder.yudao.module.icbc.service.admission.SellerAdmissionService;
import org.springframework.stereotype.Service;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.SELLER_SUBJECT_TYPE_NOT_NATURAL;

/**
 * {@link SellerAdmissionService} 的实现。
 *
 * <p>判定规则只有一条：主体是不是自然人（{@link SellerSubjectTypeEnum#isNaturalType(Integer)}）。
 * 不是「有没有营业执照」——个体工商户有营业执照，但在增值税法体系里与自然人并列（《增值税法实施条例》
 * 第三条「个人，包括个体工商户和自然人」），不属于自然人。
 */
@Service
public class SellerAdmissionServiceImpl implements SellerAdmissionService {

    @Override
    public void assertReverseInvoiceAllowed(Integer sellerSubjectType) {
        if (!isReverseInvoiceAllowed(sellerSubjectType)) {
            throw exception(SELLER_SUBJECT_TYPE_NOT_NATURAL, subjectTypeName(sellerSubjectType));
        }
    }

    @Override
    public boolean isReverseInvoiceAllowed(Integer sellerSubjectType) {
        // null = 历史数据（反向开票通道此前只对自然人开放）：按自然人放行，不把老单卡死。
        // 未知类型不是 null，isNaturalType 返回 false，同样被拒——宁可不认，也不误放开。
        return sellerSubjectType == null || SellerSubjectTypeEnum.isNaturalType(sellerSubjectType);
    }

    @Override
    public String reverseInvoiceRejectionMessage(Integer sellerSubjectType) {
        return "该卖方主体是「" + subjectTypeName(sellerSubjectType) + "」，不是自然人，不能走反向开票；"
                + "请由对方自行开具增值税发票，并在「进项收票」登记与勾稽";
    }

    private String subjectTypeName(Integer subjectType) {
        SellerSubjectTypeEnum type = SellerSubjectTypeEnum.valueOf(subjectType);
        return type != null ? type.getName() : "未知主体类型(" + subjectType + ")";
    }

}
