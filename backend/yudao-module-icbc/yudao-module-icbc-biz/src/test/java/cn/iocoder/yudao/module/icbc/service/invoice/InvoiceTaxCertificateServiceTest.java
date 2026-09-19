package cn.iocoder.yudao.module.icbc.service.invoice;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.icbc.UnitTestConfiguration;
import cn.iocoder.yudao.module.icbc.controller.admin.invoice.vo.InvoiceTaxCertificateRespVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.acquisition.IcbcAcquisitionDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.invoice.InvoiceOrderDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payee.PayeeInfoDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payer.PayerInfoDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.acquisition.IcbcAcquisitionMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.invoice.InvoiceOrderMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.payee.PayeeInfoMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.payer.PayerInfoMapper;
import cn.iocoder.yudao.module.icbc.enums.TaxStatusEnum;
import cn.iocoder.yudao.module.icbc.service.invoice.impl.InvoiceTaxCertificateServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.INVOICE_ORDER_NOT_EXISTS;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.INVOICE_TAX_CERTIFICATE_NOT_AVAILABLE;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 代办税费缴税凭证测试（issue #10 验收：缴税成功后有对应凭证）。
 */
@Import({UnitTestConfiguration.class, InvoiceTaxCertificateServiceImpl.class})
public class InvoiceTaxCertificateServiceTest extends BaseDbUnitTest {

    private static final String PARTNER_ORDER_ID = "ACQ_CERT_1";

    @Resource
    private InvoiceTaxCertificateService invoiceTaxCertificateService;

    @Resource
    private InvoiceOrderMapper invoiceOrderMapper;

    @Resource
    private IcbcAcquisitionMapper acquisitionMapper;

    @Resource
    private PayerInfoMapper payerInfoMapper;

    @Resource
    private PayeeInfoMapper payeeInfoMapper;

    @Test
    public void testCertificateNotAvailableBeforeTaxPaid() {
        insertOrder(TaxStatusEnum.TAXING.getStatus(), null, null, null);

        assertServiceException(() -> invoiceTaxCertificateService.getCertificate(PARTNER_ORDER_ID),
                INVOICE_TAX_CERTIFICATE_NOT_AVAILABLE);
    }

    @Test
    public void testCertificateForUnknownOrder() {
        assertServiceException(() -> invoiceTaxCertificateService.getCertificate("NOT_EXISTS"),
                INVOICE_ORDER_NOT_EXISTS);
    }

    @Test
    public void testCertificateAfterTaxPaid() {
        PayerInfoDO payer = insertPayer();
        PayeeInfoDO payee = insertPayee();
        Long acquisitionId = insertAcquisition(payee.getId());
        insertOrder(TaxStatusEnum.SUCCESS.getStatus(), acquisitionId, payee.getId(), payer.getId());

        InvoiceTaxCertificateRespVO certificate =
                invoiceTaxCertificateService.getCertificate(PARTNER_ORDER_ID);

        assertEquals("TAXCERT-" + PARTNER_ORDER_ID, certificate.getCertificateNo());
        assertEquals(PARTNER_ORDER_ID, certificate.getPartnerOrderId());
        assertEquals("ACQ-CERT-1", certificate.getAcquisitionNo());
        assertEquals("北京再生资源回收有限公司", certificate.getPayerName());
        assertEquals("91110105MA01ABCDEF", certificate.getPayerTaxNo());
        assertEquals("张三", certificate.getSellerName());
        // 身份证号脱敏
        assertEquals("1101**********1234", certificate.getSellerIdCardNo());
        assertEquals("INV-001", certificate.getInvoiceNo());
        assertEquals("044001900111", certificate.getInvoiceCode());
        assertEquals(new BigDecimal("1000.00"), certificate.getInvoiceAmount());
        assertEquals(new BigDecimal("10.00"), certificate.getTaxAmount());
        assertEquals(new BigDecimal("10.00"), certificate.getTaxRealAmount());
        assertEquals("企业委托扣缴", certificate.getTaxPaymentMethodName());
        assertEquals("2026120100001234", certificate.getTaxVoucherNo());
        assertEquals("缴税成功", certificate.getTaxStatusName());
        assertNotNull(certificate.getTaxTime());
        assertNotNull(certificate.getIssuedTime());
    }

    @Test
    public void testCertificateAvailableWhenTaxNotRequired() {
        insertOrder(TaxStatusEnum.NOT_REQUIRED.getStatus(), null, null, null);

        InvoiceTaxCertificateRespVO certificate =
                invoiceTaxCertificateService.getCertificate(PARTNER_ORDER_ID);

        assertEquals("无需缴税", certificate.getTaxStatusName());
    }

    @Test
    public void testExportCertificateWritesExcel() throws Exception {
        insertOrder(TaxStatusEnum.SUCCESS.getStatus(), null, null, null);
        MockHttpServletResponse response = new MockHttpServletResponse();

        invoiceTaxCertificateService.exportCertificate(PARTNER_ORDER_ID, response);

        assertTrue(response.getContentAsByteArray().length > 0);
        assertEquals("application/vnd.ms-excel;charset=UTF-8", response.getContentType());
    }

    // ==================== 造数据 ====================

    private PayerInfoDO insertPayer() {
        PayerInfoDO payer = new PayerInfoDO();
        payer.setName("北京再生资源回收有限公司");
        payer.setTaxNo("91110105MA01ABCDEF");
        payerInfoMapper.insert(payer);
        return payer;
    }

    private PayeeInfoDO insertPayee() {
        PayeeInfoDO payee = new PayeeInfoDO();
        payee.setName("张三");
        payee.setIdCardNo("110101199001011234");
        payee.setMobile("13800138000");
        payeeInfoMapper.insert(payee);
        return payee;
    }

    private Long insertAcquisition(Long payeeId) {
        IcbcAcquisitionDO acquisition = IcbcAcquisitionDO.builder()
                .acquisitionNo("ACQ-CERT-1")
                .payeeId(payeeId)
                .build();
        acquisitionMapper.insert(acquisition);
        return acquisition.getId();
    }

    private void insertOrder(Integer taxStatus, Long acquisitionId, Long payeeId, Long payerId) {
        InvoiceOrderDO order = InvoiceOrderDO.builder()
                .orderNo("INV_CERT_1")
                .partnerOrderId(PARTNER_ORDER_ID)
                .acquisitionId(acquisitionId)
                .payeeId(payeeId)
                .payerId(payerId)
                .payeeNo("USER_1001")
                .payerNo("VENDOR_2001")
                .totalAmount(new BigDecimal("1000.00"))
                .invoiceType(1)
                .businessType("SCRAP")
                .orderStatus(3)
                .invoiceStatus(2)
                .paymentStatus(2)
                .taxStatus(taxStatus)
                .confirmStatus(1)
                .preInvoiceStatus(2)
                .invoiceNo("INV-001")
                .invoiceCode("044001900111")
                .invoiceDate(LocalDateTime.of(2026, 12, 1, 10, 30))
                .invoiceAmount(new BigDecimal("1000.00"))
                .taxAmount(new BigDecimal("10.00"))
                .taxRealAmount(new BigDecimal("10.00"))
                .taxTime(LocalDateTime.of(2026, 12, 1, 10, 35))
                .taxPaymentMethod("1")
                .taxVoucherNo("2026120100001234")
                .build();
        invoiceOrderMapper.insert(order);
    }
}
