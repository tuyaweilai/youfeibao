package cn.iocoder.yudao.module.icbc.service.invoice;

import cn.iocoder.yudao.module.icbc.controller.admin.invoice.vo.InvoiceTaxCertificateRespVO;

import javax.servlet.http.HttpServletResponse;

/**
 * 代办税费缴税凭证 Service。
 *
 * <p>缴税成功（或无需缴税）后，把一张票的「应缴 / 实缴 / 缴税时间 / 应征凭证序号 / 扣缴义务人」
 * 固化成可打印、可导出的凭证。尚未缴清时不给凭证，并提示当前状态与下一步动作。
 */
public interface InvoiceTaxCertificateService {

    /**
     * 取缴税凭证；缴税尚未成功时抛业务异常
     *
     * @param partnerOrderId 合作方订单号
     */
    InvoiceTaxCertificateRespVO getCertificate(String partnerOrderId);

    /**
     * 导出缴税凭证（Excel，可打印）
     *
     * @param partnerOrderId 合作方订单号
     * @param response       HTTP 响应
     */
    void exportCertificate(String partnerOrderId, HttpServletResponse response);

}
