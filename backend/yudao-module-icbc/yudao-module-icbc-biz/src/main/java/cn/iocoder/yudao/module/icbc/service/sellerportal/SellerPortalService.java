package cn.iocoder.yudao.module.icbc.service.sellerportal;

import cn.iocoder.yudao.module.icbc.controller.app.seller.vo.*;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 自然人出售者端门户 Service（#34）。
 *
 * <p>把「他能自己翻账」所需的数据聚合出来：待我确认、卖货记录（按回收企业分组）、收款记录、
 * 发票与税费、我的资料与企业授权。所有读取都要求**显式指定 naturalPersonId 且校验它绑在当前登录名下**；
 * 跨租户读取只对自然人本人开放（CONTEXT.md「交易可见性边界」）。
 */
public interface SellerPortalService {

    /**
     * 首页摘要：待我确认（待签协议 + 待确认结算单）。
     */
    SellerHomeRespVO getHome(Long naturalPersonId);

    /**
     * 卖货记录，按回收企业分组。
     */
    List<SellerRecordGroupRespVO> getRecordGroups(Long naturalPersonId);

    /**
     * 收款记录（含「我收到了」的自行确认状态）。
     */
    List<SellerPaymentRespVO> getPayments(Long naturalPersonId);

    /**
     * 发票与税费（按年度汇总 + 逐票明细）。
     *
     * @param year 年度；为空取当前年
     */
    SellerInvoiceSummaryRespVO getInvoices(Long naturalPersonId, Integer year);

    /**
     * 企业授权列表（按回收企业）。
     */
    List<SellerAuthorizationRespVO> getAuthorizations(Long naturalPersonId);

    /**
     * 自助撤销对某一家回收企业的授权。**只拦未来，不追溯已开出的票**。
     */
    void revokeAuthorization(SellerRevokeAuthorizationReqVO reqVO);

    /**
     * 我的资料（收款账户尾号、联系方式、客服）。
     */
    SellerProfileRespVO getProfile(Long naturalPersonId);

    /**
     * 「我收到了」：自然人自行确认，不改动银行状态。
     */
    void confirmReceived(SellerConfirmReceiveReqVO reqVO, String ip);

    /**
     * 下载自己的发票 PDF（校验该票属于本人）。
     */
    void downloadInvoicePdf(Long naturalPersonId, Long invoiceOrderId, HttpServletResponse response);

    /**
     * 输出可打印的单笔收购确认书 HTML（H5 用浏览器「打印 / 保存为 PDF」）。
     */
    void writeAcquisitionConfirmation(Long naturalPersonId, Long acquisitionId, HttpServletResponse response);

    /**
     * 输出可打印的结算确认书 HTML（含当前版本逐条收购单与确认/异议状态）。
     */
    void writeSettlementConfirmation(Long naturalPersonId, Long settlementId, HttpServletResponse response);

}
