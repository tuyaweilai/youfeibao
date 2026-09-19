package cn.iocoder.yudao.module.icbc.service.tax;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.icbc.controller.admin.tax.vo.*;

import java.util.List;

/**
 * 代办税费申报 Service（issue #13）。
 *
 * <p>回收企业为出售者代办增值税及附加税费、个人所得税，按<b>月</b>申报：次月申报期
 * （次月 15 日）前报送《代办税费报告表》《代办税费明细报告表》并缴款。逾期未缴，主管
 * 税务机关会暂停其反向开票资格。所以这份清单不是报表，是一道有时间压力的动作清单。
 *
 * <p>清单是<b>派生</b>的：从当月已开出的蓝票（减去当月红冲）按出售者归集，不另维护汇总表。
 * 10 万元免征线按「自然人 × 月跨租户」判定，由额度台账提供，本服务不重算。
 */
public interface TaxDeclarationService {

    /**
     * 生成（或刷新）某申报月的申报清单与合计金额。
     *
     * <p>幂等：同一个月重复生成不会产生第二份申报单；未缴款时可刷新（数据补充后重新计算），
     * 已缴款的申报单不再改写，差额转成补缴。
     *
     * @param periodMonth 申报月 yyyy-MM
     */
    TaxDeclarationRespVO generate(String periodMonth);

    /**
     * 取某申报月的申报单；尚未生成时即时生成一份。
     *
     * @param periodMonth 申报月 yyyy-MM
     */
    TaxDeclarationRespVO getDeclaration(String periodMonth);

    /**
     * 分页查询申报单
     */
    PageResult<TaxDeclarationRespVO> getDeclarationPage(TaxDeclarationPageReqVO reqVO);

    /**
     * 分页查询申报明细（可按出售者、是否超 10 万元筛选）
     */
    PageResult<TaxDeclarationItemRespVO> getItemPage(TaxDeclarationItemPageReqVO reqVO);

    /**
     * 申报数据齐备性检查：还缺哪些数据。
     *
     * @param periodMonth 申报月 yyyy-MM
     */
    TaxDeclarationPrecheckRespVO precheck(String periodMonth);

    /**
     * 当前待处理的申报预警：申报期临近、逾期未缴可能被暂停开票资格、数据不齐。
     */
    List<TaxDeclarationWarningRespVO> getWarnings();

    /**
     * 报送《代办税费报告表》《代办税费明细报告表》，状态推进为「已申报待缴款」。
     */
    TaxDeclarationRespVO declare(TaxDeclarationDeclareReqVO reqVO);

    /**
     * 缴款成功：归档缴款凭证、与对应发票关联、状态推进为「已缴款」。
     */
    TaxDeclarationRespVO recordPayment(TaxDeclarationPayReqVO reqVO);

}
