package cn.iocoder.yudao.module.icbc.service.publicapi;

import cn.iocoder.yudao.module.icbc.controller.admin.publicapi.vo.PublicContactLeadReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.publicapi.vo.PublicQuotaRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.publicapi.vo.PublicSettlementStatementRespVO;
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

}
