package cn.iocoder.yudao.module.logistics.service.freight;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.logistics.controller.admin.freight.vo.LogisticsFreightConfirmReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.freight.vo.LogisticsFreightCreateReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.freight.vo.LogisticsFreightPageReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.freight.vo.LogisticsFreightPayReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.freight.vo.LogisticsFreightReconciliationReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.freight.vo.LogisticsFreightReconciliationRespVO;
import cn.iocoder.yudao.module.logistics.controller.admin.freight.vo.LogisticsFreightRespVO;
import cn.iocoder.yudao.module.logistics.controller.admin.freight.vo.LogisticsFreightUpdateReqVO;
import cn.iocoder.yudao.module.logistics.dal.dataobject.freight.LogisticsFreightOrderDO;

import javax.validation.Valid;
import java.util.List;

/**
 * 承运商运费 Service（V8 #75）。
 *
 * <p>CONTEXT.md「运费」：运费是回收企业向**承运商**支付的运输服务费用，**是另一笔账**，
 * 不改变收购单金额与发票金额；与收购单调整项里那个「运费」不是同一个东西。本 Service
 * 不触碰收购、结算、发票、付款任何一边。
 *
 * <p>三条硬规则：
 * <ol>
 *   <li>一趟一张，运价来自**有效期内的承运合同**并被快照；</li>
 *   <li>**自有车不产生承运商运费**：不是承运商的车就建不出运费单；</li>
 *   <li>差异**不抹平**：实际应付与应有应付不同必须留原因。</li>
 * </ol>
 */
public interface LogisticsFreightService {

    /**
     * 按趟次汇集运费（建运费单）。同一任务已汇集过则拒绝改这一条。
     */
    Long createFreight(@Valid LogisticsFreightCreateReqVO createReqVO);

    /**
     * 修改运费单（仅待确认应付可改）：重算计费量对应的应有应付，并可填实际应付与差异原因。
     */
    void updateFreight(@Valid LogisticsFreightUpdateReqVO updateReqVO);

    /**
     * 确认应付：把应有应付确认为实际应付（差异必须留原因）。
     */
    void confirmPayable(@Valid LogisticsFreightConfirmReqVO confirmReqVO);

    /**
     * 登记外部付款凭证（**不接对公付款通道**）。必须先确认应付。
     */
    void registerPaymentVoucher(@Valid LogisticsFreightPayReqVO payReqVO);

    /**
     * 获得运费单；不存在时抛业务异常。
     */
    LogisticsFreightOrderDO getFreight(Long id);

    /**
     * 按任务取运费单；没有返回 {@code null}。
     */
    LogisticsFreightOrderDO getFreightByTaskId(Long taskId);

    PageResult<LogisticsFreightOrderDO> getFreightPage(LogisticsFreightPageReqVO pageReqVO);

    List<LogisticsFreightOrderDO> getFreightList(LogisticsFreightPageReqVO exportReqVO);

    /**
     * 按「承运商 + 合同」汇总运费（应有 / 实际 / 差异 / 待确认 / 已登记凭证）。
     */
    List<LogisticsFreightReconciliationRespVO> getReconciliation(@Valid LogisticsFreightReconciliationReqVO reqVO);

    /**
     * 转 Response VO（补齐状态名、计费方式名与差异）。
     */
    LogisticsFreightRespVO toResp(LogisticsFreightOrderDO order);

}
