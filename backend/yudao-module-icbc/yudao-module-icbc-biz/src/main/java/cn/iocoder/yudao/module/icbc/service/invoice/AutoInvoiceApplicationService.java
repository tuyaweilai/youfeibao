package cn.iocoder.yudao.module.icbc.service.invoice;

import cn.iocoder.yudao.module.icbc.controller.app.settlement.vo.SellerInvoiceConfirmItemVO;

import java.util.List;

/**
 * 结算确认后自动预下单（#106，ADR 0039）。
 *
 * <p>自然人确认结算之后，这一批货的每一张收购单都要在**工行的页面**上由本人确认开票信息。
 * 本服务把「确认结算 → 逐张预下单」这一步接起来，并把每张的进度整理成自然人看得懂的一列步骤。
 *
 * <p>三件事必须守住：
 * <ul>
 *   <li><b>幂等</b>：同一张收购单只下一次预下单（已有开票单就跳过），重复确认不会重复下单；</li>
 *   <li><b>失败不连坐</b>：某一张失败不影响其他张，也不影响已经落库的结算确认；</li>
 *   <li><b>门禁全保留</b>：走的还是开票申请那一套前置校验，不因为「自动」就少一道。</li>
 * </ul>
 */
public interface AutoInvoiceApplicationService {

    /**
     * 该结算单下每张收购单都去发起一次预下单（已有开票单的跳过）。
     *
     * <p>逐张独立成败：本方法不抛业务异常，失败只记日志——结算确认已经落库，不能因为它回滚。
     */
    void applyForSettlement(Long settlementId);

    /**
     * 该结算单下每张收购单的开票确认进度（只读）。不能发起的会带上逐项原因与补齐方式。
     */
    List<SellerInvoiceConfirmItemVO> statusForSettlement(Long settlementId);

    /**
     * 取某张票的自然人确认页表单 HTML（按需重开用）。
     *
     * @return 表单 HTML；没有开票单或没存过时返回 {@code null}
     */
    String confirmPageHtml(String partnerOrderId);

}
