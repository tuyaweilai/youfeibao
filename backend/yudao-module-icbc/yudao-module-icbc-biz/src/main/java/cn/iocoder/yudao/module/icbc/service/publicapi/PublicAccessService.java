package cn.iocoder.yudao.module.icbc.service.publicapi;

import cn.iocoder.yudao.module.icbc.controller.admin.publicapi.vo.PublicContactLeadReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.publicapi.vo.PublicOnboardingPageRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.publicapi.vo.PublicOnboardingStatusRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.publicapi.vo.PublicNoticeRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.publicapi.vo.PublicQuotaRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.publicapi.vo.PublicSettlementStatementRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.publicapi.vo.PublicStationRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.tax.vo.SellerSettlementStatementRespVO;

import javax.servlet.http.HttpServletResponse;

/**
 * 免登录公开端点 Service。所有方法都从「短期单用途令牌」解析出租户与业务单，
 * 再在该租户下执行；平台不为自然人建任何账号。
 */
public interface PublicAccessService {

    /**
     * 用令牌下载发票 PDF。
     */
    void downloadInvoicePdf(String token, HttpServletResponse response);

    /**
     * 用令牌提交收方入驻失败后的联系方式。
     */
    void submitContactLead(PublicContactLeadReqVO reqVO);

    /**
     * 用令牌查询该自然人的滚动额度余量。
     */
    PublicQuotaRespVO queryQuota(String token);

    /**
     * 用令牌取该自然人的汇算清缴对账单（当年开票与已缴税款）。
     */
    PublicSettlementStatementRespVO querySettlement(String token);

    /**
     * 用令牌查看触达通知（#36）：待确认结算 / 付款异常。打开即可看，**不需要注册**。
     */
    PublicNoticeRespVO queryNotice(String token);

    /**
     * 用令牌取「当前该做的工行页面」。实名之后自然人这边没有事了——收方入驻是数据接口、
     * 由平台自动发起（ADR 0035），所以这里只剩实名认证一步。
     *
     * @param token 公开令牌（用途 ONBOARDING，绑定收方）
     * @return 当前步骤与工行表单 HTML
     */
    PublicOnboardingPageRespVO getOnboardingPage(String token);

    /**
     * 用令牌刷新建档状态：按当前步骤向工行主动查询一次并收敛。
     */
    PublicOnboardingStatusRespVO syncOnboarding(String token);

    /**
     * 用令牌直接输出「当前该做的工行页面」HTML（自动提交表单）。
     * 小程序 `web-view` 与 H5 新窗口都指向这个 URL，内容由后端生成，前端不拼工行 URL。
     */
    void writeOnboardingForm(String token, HttpServletResponse response);

    /**
     * 用令牌直接输出「自然人确认开票信息」的工行页面 HTML（自动提交表单，见 #106 / ADR 0039）。
     *
     * <p>它与 {@link #writeOnboardingForm} 同形，但里面是**工行预下单返回的那张表单**：
     * 自然人在自己的手机上打开，就是「本人在工行页面上确认开票信息」那一步的落点。
     * 页面在手机上是可关掉的，所以只要还没确认，重开同一张页面是安全的。
     */
    void writeInvoiceConfirmPage(String token, HttpServletResponse response);

    /**
     * 解析场站二维码：只编码场站码，服务端解析出企业与场站的公开信息。
     *
     * <p>二维码不带令牌（公开且长期贴），所以本方法不需要令牌；按 IP 限流，且**不返回个人数据**。
     *
     * @param stationCode 场站码
     * @param clientIp    调用方 IP（限流键）
     * @return 场站公开信息（含所属租户编号，供前端后续请求带 tenant-id）
     */
    PublicStationRespVO resolveStation(String stationCode, String clientIp);

}
