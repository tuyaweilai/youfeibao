package cn.iocoder.yudao.module.icbc.service.tax;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.icbc.controller.admin.tax.vo.TaxSupplementCreateReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.tax.vo.TaxSupplementPageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.tax.vo.TaxSupplementPayReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.tax.vo.TaxSupplementRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.tax.vo.TaxSupplementSummaryRespVO;

import java.math.BigDecimal;

/**
 * 需补缴税费 Service（issue #13）。
 *
 * <p>补缴是申报之后才暴露出来的差额。它不能悄悄改一份已缴清的申报单，所以独立记录、
 * 独立缴清；待补缴累计金额按 1% 与 3% 分列，申报表才能填对。
 */
public interface TaxSupplementService {

    /**
     * 登记一条补缴记录
     */
    TaxSupplementRespVO create(TaxSupplementCreateReqVO reqVO);

    /**
     * 自动生成一条补缴记录（申报单已缴款后重新计算发现的正差额）。
     *
     * @param declarationId       申报单编号
     * @param periodMonth         申报月
     * @param amountAtOnePercent  1% 部分的补缴金额
     * @param amountAtThreePercent 3% 部分的补缴金额
     * @param reason              补缴原因
     */
    void recordAuto(Long declarationId, String periodMonth, BigDecimal amountAtOnePercent,
                    BigDecimal amountAtThreePercent, String reason);

    /**
     * 分页查询补缴记录
     */
    PageResult<TaxSupplementRespVO> getPage(TaxSupplementPageReqVO reqVO);

    /**
     * 待补缴累计：合计金额与 1% / 3% 分列。
     */
    TaxSupplementSummaryRespVO getSummary();

    /**
     * 缴清一条补缴记录并归档凭证
     */
    TaxSupplementRespVO recordPayment(TaxSupplementPayReqVO reqVO);

}
