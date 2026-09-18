package cn.iocoder.yudao.module.icbc.service.publicapi;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.icbc.controller.admin.publicapi.vo.PublicContactLeadReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.publicapi.vo.PublicQuotaRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.publictoken.vo.PublicTokenCreateReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.download.InvoiceDownloadDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.download.InvoiceFileDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.invoice.InvoiceOrderDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payee.PayeeInfoDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.download.InvoiceDownloadMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.download.InvoiceFileMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.invoice.InvoiceOrderMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.lead.IcbcContactLeadMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.payee.PayeeInfoMapper;
import cn.iocoder.yudao.module.icbc.service.download.impl.InvoiceDownloadServiceImpl;
import cn.iocoder.yudao.module.icbc.service.publicapi.impl.PublicAccessServiceImpl;
import cn.iocoder.yudao.module.icbc.service.quota.NaturalPersonQuotaService;
import cn.iocoder.yudao.module.icbc.service.token.PublicTokenCodec;
import cn.iocoder.yudao.module.icbc.service.token.PublicTokenService;
import cn.iocoder.yudao.module.icbc.service.token.impl.PublicTokenServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.File;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.PUBLIC_TOKEN_USED_UP;
import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link PublicAccessServiceImpl} 的单元测试：三类公开端点，从令牌解析租户后执行。
 */
@Import({PublicAccessServiceImpl.class, PublicTokenServiceImpl.class, PublicTokenCodec.class,
        InvoiceDownloadServiceImpl.class, NaturalPersonQuotaService.class})
@TestPropertySource(properties = {
        "icbc.public-token.secret=test-public-token-secret-0123456789abcdef",
        "yudao.file.base-path=/tmp/test"})
@Sql(scripts = "/sql/create_tables.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Transactional
public class PublicAccessServiceImplTest extends BaseDbUnitTest {

    private static final Long TENANT_ID = 1L;

    @Resource
    private PublicAccessService publicAccessService;
    @Resource
    private PublicTokenService publicTokenService;
    @Resource
    private InvoiceOrderMapper invoiceOrderMapper;
    @Resource
    private PayeeInfoMapper payeeInfoMapper;
    @Resource
    private InvoiceDownloadMapper invoiceDownloadMapper;
    @Resource
    private InvoiceFileMapper invoiceFileMapper;
    @Resource
    private IcbcContactLeadMapper contactLeadMapper;

    @BeforeEach
    public void setUp() {
        TenantContextHolder.setTenantId(TENANT_ID);
    }

    @AfterEach
    public void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    public void testDownloadInvoicePdf_successThenSingleUseConsumed() throws Exception {
        File pdf = File.createTempFile("invoice-", ".pdf");
        pdf.deleteOnExit();
        byte[] content = "PDF-BYTES-测试".getBytes(StandardCharsets.UTF_8);
        Files.write(pdf.toPath(), content);
        prepareInvoiceWithFile("ORDER_D1", pdf, content.length);
        String token = mint("INVOICE_DOWNLOAD", "ORDER_D1", null);

        MockHttpServletResponse response = new MockHttpServletResponse();
        publicAccessService.downloadInvoicePdf(token, response);

        assertArrayEquals(content, response.getContentAsByteArray());
        // 单用途：取过一次后再取即拒绝
        assertServiceException(
                () -> publicAccessService.downloadInvoicePdf(token, new MockHttpServletResponse()),
                PUBLIC_TOKEN_USED_UP);
    }

    @Test
    public void testSubmitContactLead_storesUnderResolvedTenant() {
        PayeeInfoDO payee = insertPayee("张三", "110101199001011234");
        String token = mint("CONTACT_LEAD", null, payee.getId());

        PublicContactLeadReqVO reqVO = new PublicContactLeadReqVO();
        reqVO.setToken(token);
        reqVO.setName("张三");
        reqVO.setMobile("13800138000");
        reqVO.setRemark("银行卡未识别");
        publicAccessService.submitContactLead(reqVO);

        List<?> leads = contactLeadMapper.selectListByPayeeId(payee.getId());
        assertEquals(1, leads.size());
        // 单用途：再次提交即拒绝
        assertServiceException(() -> publicAccessService.submitContactLead(reqVO), PUBLIC_TOKEN_USED_UP);
    }

    @Test
    public void testQueryQuota_onlyCountsRollingTwelveMonths() {
        PayeeInfoDO payee = insertPayee("李四", "110101199002022345");
        // 窗口内：1 个月前 10 万
        insertOrder("ORDER_Q_IN", payee.getId(), new BigDecimal("100000.00"), LocalDateTime.now().minusMonths(1));
        // 窗口外：13 个月前 50 万，不应计入
        insertOrder("ORDER_Q_OUT", payee.getId(), new BigDecimal("500000.00"), LocalDateTime.now().minusMonths(13));
        String token = mint("QUOTA_QUERY", null, payee.getId());

        PublicQuotaRespVO quota = publicAccessService.queryQuota(token);

        assertEquals("李四", quota.getName());
        assertEquals(0, new BigDecimal("5000000.00").compareTo(quota.getCapAmount()));
        assertEquals(0, new BigDecimal("100000.00").compareTo(quota.getUsedAmount()));
        assertEquals(0, new BigDecimal("4900000.00").compareTo(quota.getRemainingAmount()));
        assertEquals("110101********2345", quota.getIdCardMasked());
    }

    // ==================== 造数 ====================

    private String mint(String purpose, String partnerOrderId, Long payeeId) {
        PublicTokenCreateReqVO reqVO = new PublicTokenCreateReqVO();
        reqVO.setPurpose(purpose);
        reqVO.setPartnerOrderId(partnerOrderId);
        reqVO.setPayeeId(payeeId);
        return publicTokenService.mint(reqVO).getToken();
    }

    private PayeeInfoDO insertPayee(String name, String idCardNo) {
        PayeeInfoDO payee = PayeeInfoDO.builder()
                .name(name).mobile("13800138000").idCardNo(idCardNo)
                .partnerPayeeId("PARTNER_" + idCardNo)
                .build();
        payeeInfoMapper.insert(payee);
        return payee;
    }

    private void prepareInvoiceWithFile(String partnerOrderId, File file, long size) {
        InvoiceOrderDO order = insertOrder(partnerOrderId, null, new BigDecimal("1000.00"), LocalDateTime.now());
        InvoiceDownloadDO download = InvoiceDownloadDO.builder()
                .invoiceOrderId(order.getId()).partnerOrderId(partnerOrderId)
                .orderNumber("ICBC_" + partnerOrderId).invoiceNumber("NUM_" + partnerOrderId)
                .downloadStatus(2).retryCount(0).build();
        invoiceDownloadMapper.insert(download);
        invoiceFileMapper.insert(InvoiceFileDO.builder()
                .downloadId(download.getId()).invoiceNumber("NUM_" + partnerOrderId)
                .fileType("PDF").filePath(file.getAbsolutePath()).fileName("invoice.pdf")
                .fileSize(size).fileMd5("d41d8cd98f00b204e9800998ecf8427e").accessCount(0).build());
    }

    private InvoiceOrderDO insertOrder(String partnerOrderId, Long payeeId, BigDecimal amount,
                                       LocalDateTime createTime) {
        InvoiceOrderDO order = InvoiceOrderDO.builder()
                .orderNo("INV_" + partnerOrderId).partnerOrderId(partnerOrderId)
                .payeeId(payeeId).payeeNo(payeeId != null ? "PARTNER_P" + payeeId : null).payerNo("PAYER_1")
                .totalAmount(amount).invoiceType(1).businessType("SCRAP")
                .orderStatus(0).invoiceStatus(0).paymentStatus(0).taxStatus(0).build();
        order.setCreateTime(createTime);
        invoiceOrderMapper.insert(order);
        return order;
    }

}
