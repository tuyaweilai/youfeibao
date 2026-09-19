package cn.iocoder.yudao.module.icbc.service.tax;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.icbc.controller.admin.tax.vo.SellerSettlementStatementRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.tax.vo.SettlementReminderHandleReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.tax.vo.SettlementReminderPageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.tax.vo.SettlementReminderRespVO;

/**
 * 出售者汇算清缴提醒 Service（issue #13）。
 *
 * <p>出售者须在次年 3 月 31 日前自行汇算清缴。平台不替他申报，但要提醒他，并把
 * 「当年开了多少票、已预缴多少税」交给他——这既是回收企业的法定义务，也是出售者
 * 不会在汇算时抓瞎的前提。
 */
public interface AnnualSettlementService {

    /**
     * 扫描某纳税年度有开票记录的出售者并生成提醒。
     *
     * @param taxYear 纳税年度
     * @return 新增提醒数
     */
    int remind(Integer taxYear);

    /**
     * 当前应提醒的纳税年度：3 月 31 日前是上一年，之后是本年度。
     */
    int currentTaxYear();

    /**
     * 分页查询汇算清缴提醒
     */
    PageResult<SettlementReminderRespVO> getPage(SettlementReminderPageReqVO reqVO);

    /**
     * 处理提醒：标记为已提醒。
     */
    SettlementReminderRespVO handle(SettlementReminderHandleReqVO reqVO);

    /**
     * 取出售者某纳税年度的开票与已缴税款对账单。
     *
     * @param payeeId 出售者档案编号
     * @param taxYear 纳税年度；为空时取 {@link #currentTaxYear()}
     */
    SellerSettlementStatementRespVO getStatement(Long payeeId, Integer taxYear);

}
