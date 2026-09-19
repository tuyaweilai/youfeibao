package cn.iocoder.yudao.module.icbc.service.publicapi;

import cn.iocoder.yudao.module.icbc.controller.admin.publicapi.vo.PublicContactLeadReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.publicapi.vo.PublicOnboardingPageRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.publicapi.vo.PublicOnboardingStatusRespVO;import cn.iocoder.yudao.module.icbc.controller.admin.publicapi.vo.PublicQuotaRespVO;
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
     * 用令牌取「当前该做的工行页面」：实名认证或收方入驻。
     *
     * @param token       公开令牌（用途 ONBOARDING，绑定收方）
     * @param trxChannel  交易渠道（H5=03、微信小程序=05）；为空按 H5 处理
     * @return 当前步骤与工行表单 HTML
     */
    PublicOnboardingPageRespVO getOnboardingPage(String token, String trxChannel);

    /**
     * 用令牌刷新建档状态：按当前步骤向工行主动查询一次并收敛。
     */
    PublicOnboardingStatusRespVO syncOnboarding(String token);

    /**
     * 用令牌直接输出「当前该做的工行页面」HTML（自动提交表单）。
     * 小程序 `web-view` 与 H5 新窗口都指向这个 URL，内容由后端生成，前端不拼工行 URL。
     */
    void writeOnboardingForm(String token, String trxChannel, HttpServletResponse response);

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
