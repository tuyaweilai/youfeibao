package cn.iocoder.yudao.module.icbc.service.invoice.impl;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.icbc.controller.admin.invoice.vo.InvoiceTaxCertificateRespVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.acquisition.IcbcAcquisitionDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.invoice.InvoiceOrderDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payee.PayeeInfoDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payer.PayerInfoDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.acquisition.IcbcAcquisitionMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.invoice.InvoiceOrderMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.payee.PayeeInfoMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.payer.PayerInfoMapper;
import cn.iocoder.yudao.module.icbc.enums.IcbcTaxPaymentMethodEnum;
import cn.iocoder.yudao.module.icbc.enums.TaxStatusEnum;
import cn.iocoder.yudao.module.icbc.service.invoice.InvoiceTaxCertificateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.INVOICE_ORDER_NOT_EXISTS;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.INVOICE_TAX_CERTIFICATE_EXPORT_FAILED;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.INVOICE_TAX_CERTIFICATE_NOT_AVAILABLE;

/**
 * 代办税费缴税凭证 Service 实现。
 */
@Slf4j
@Service
public class InvoiceTaxCertificateServiceImpl implements InvoiceTaxCertificateService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Resource
    private InvoiceOrderMapper invoiceOrderMapper;

    @Resource
    private IcbcAcquisitionMapper acquisitionMapper;

    @Resource
    private PayeeInfoMapper payeeInfoMapper;

    @Resource
    private PayerInfoMapper payerInfoMapper;

    @Override
    public InvoiceTaxCertificateRespVO getCertificate(String partnerOrderId) {
        InvoiceOrderDO order = invoiceOrderMapper.selectByPartnerOrderId(partnerOrderId);
        if (order == null) {
            throw exception(INVOICE_ORDER_NOT_EXISTS);
        }
        if (!TaxStatusEnum.isPaid(order.getTaxStatus())) {
            throw exception(INVOICE_TAX_CERTIFICATE_NOT_AVAILABLE);
        }
        return buildCertificate(order);
    }

    @Override
    public void exportCertificate(String partnerOrderId, HttpServletResponse response) {
        InvoiceTaxCertificateRespVO certificate = getCertificate(partnerOrderId);
        try {
            ExcelUtils.write(response, "缴税凭证_" + partnerOrderId + ".xls",
                    "缴税凭证", InvoiceTaxCertificateRespVO.class, Collections.singletonList(certificate));
        } catch (IOException e) {
            log.error("导出缴税凭证失败 - partnerOrderId: {}", partnerOrderId, e);
            throw exception(INVOICE_TAX_CERTIFICATE_EXPORT_FAILED);
        }
    }

    private InvoiceTaxCertificateRespVO buildCertificate(InvoiceOrderDO order) {
        InvoiceTaxCertificateRespVO vo = new InvoiceTaxCertificateRespVO();
        vo.setCertificateNo("TAXCERT-" + StrUtil.blankToDefault(order.getPartnerOrderId(), order.getOrderNo()));
        vo.setPartnerOrderId(order.getPartnerOrderId());
        vo.setOrderNo(order.getOrderNo());
        vo.setInvoiceNo(order.getInvoiceNo());
        vo.setInvoiceCode(order.getInvoiceCode());
        vo.setInvoiceDate(order.getInvoiceDate() != null ? order.getInvoiceDate().format(TIME_FORMATTER) : null);
        vo.setInvoiceAmount(order.getInvoiceAmount());
        vo.setTaxAmount(order.getTaxAmount());
        vo.setTaxRealAmount(order.getTaxRealAmount());
        vo.setTaxTime(order.getTaxTime() != null ? order.getTaxTime().format(TIME_FORMATTER) : null);
        vo.setTaxPaymentMethodName(IcbcTaxPaymentMethodEnum.nameOf(order.getTaxPaymentMethod()));
        vo.setTaxVoucherNo(order.getTaxVoucherNo());
        vo.setTaxStatusName(TaxStatusEnum.nameOf(order.getTaxStatus()));
        vo.setIssuedTime(LocalDateTime.now().format(TIME_FORMATTER));

        IcbcAcquisitionDO acquisition = order.getAcquisitionId() != null
                ? acquisitionMapper.selectById(order.getAcquisitionId()) : null;
        if (acquisition != null) {
            vo.setAcquisitionNo(acquisition.getAcquisitionNo());
        }
        PayerInfoDO payer = order.getPayerId() != null ? payerInfoMapper.selectById(order.getPayerId()) : null;
        if (payer != null) {
            vo.setPayerName(payer.getName());
            vo.setPayerTaxNo(payer.getTaxNo());
        }
        PayeeInfoDO payee = order.getPayeeId() != null ? payeeInfoMapper.selectById(order.getPayeeId()) : null;
        if (payee != null) {
            vo.setSellerName(payee.getName());
            vo.setSellerIdCardNo(maskIdCard(payee.getIdCardNo()));
        }
        return vo;
    }

    /**
     * 身份证号脱敏：保留前 4 位与后 4 位，中间以 * 代替。
     */
    private String maskIdCard(String idCardNo) {
        if (StrUtil.isBlank(idCardNo) || idCardNo.length() <= 8) {
            return idCardNo;
        }
        return idCardNo.substring(0, 4)
                + StrUtil.repeat('*', idCardNo.length() - 8)
                + idCardNo.substring(idCardNo.length() - 4);
    }

}
