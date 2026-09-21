package cn.iocoder.yudao.module.icbc.service.publicapi;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.icbc.controller.admin.publicapi.vo.PublicContactLeadReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.publicapi.vo.PublicOnboardingPageRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.publicapi.vo.PublicOnboardingStatusRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.publicapi.vo.PublicQuotaRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.publicapi.vo.PublicSettlementStatementRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.onboarding.vo.SellerOnboardingRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.onboarding.vo.SellerStepRespVO;
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
import cn.iocoder.yudao.module.icbc.service.naturalperson.impl.NaturalPersonServiceImpl;
import cn.iocoder.yudao.module.icbc.service.notify.SellerNotifyService;
import cn.iocoder.yudao.module.icbc.service.quota.impl.NaturalPersonQuotaServiceImpl;
import cn.iocoder.yudao.module.icbc.service.onboarding.SellerOnboardingService;
import cn.iocoder.yudao.module.icbc.service.tax.impl.AnnualSettlementServiceImpl;
import cn.iocoder.yudao.module.icbc.service.token.PublicTokenCodec;
import cn.iocoder.yudao.module.icbc.service.token.PublicTokenService;
import cn.iocoder.yudao.module.icbc.service.token.impl.PublicTokenServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.File;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.PUBLIC_TOKEN_USED_UP;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link PublicAccessServiceImpl} 的单元测试：三类公开端点，从令牌解析租户后执行。
 */
@Import({PublicAccessServiceImpl.class, PublicTokenServiceImpl.class, PublicTokenCodec.class,
        InvoiceDownloadServiceImpl.class, NaturalPersonQuotaServiceImpl.class,
        AnnualSettlementServiceImpl.class, NaturalPersonServiceImpl.class})
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

    /** 触达是另一条链路（#36），这里只验证公开端点如何解析并转交 */
    @MockBean
    private SellerNotifyService sellerNotifyService;
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

    @MockBean
    private SellerOnboardingService sellerOnboardingService;
    @MockBean
    private cn.iocoder.yudao.module.icbc.service.station.StationService stationService;

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
    public void testQueryQuota_onlyCountsIssuedInvoicesInRollingTwelveMonths() {
        PayeeInfoDO payee = insertPayee("李四", "110101199002022345");
        // 窗口内、已开票：1 个月前 10 万（按 3% 减按 1%）
        insertIssuedOrder("ORDER_Q_IN", payee, new BigDecimal("100000.00"),
                LocalDateTime.now().minusMonths(1), new BigDecimal("0.01"));
        // 窗口外、已开票：13 个月前 50 万，不应计入
        insertIssuedOrder("ORDER_Q_OUT", payee, new BigDecimal("500000.00"),
                LocalDateTime.now().minusMonths(13), new BigDecimal("0.01"));
        // 窗口内、但只是预下单没开出来：不算销售额，不占额度
        insertOrder("ORDER_Q_UNISSUED", payee.getId(), new BigDecimal("700000.00"), LocalDateTime.now().minusDays(5));
        String token = mint("QUOTA_QUERY", null, payee.getId());

        PublicQuotaRespVO quota = publicAccessService.queryQuota(token);

        assertEquals("李四", quota.getName());
        assertEquals(0, new BigDecimal("5000000.00").compareTo(quota.getCapAmount()));
        assertEquals(0, new BigDecimal("100000.00").compareTo(quota.getIssuedAmount()));
        assertEquals(0, new BigDecimal("100000.00").compareTo(quota.getUsedAmount()));
        assertEquals(0, new BigDecimal("4900000.00").compareTo(quota.getRemainingAmount()));
        assertEquals(0, new BigDecimal("100000.00").compareTo(quota.getAmountAtOnePercent()));
        assertEquals(0, BigDecimal.ZERO.compareTo(quota.getAmountAtThreePercent()));
        assertEquals(Boolean.FALSE, quota.getQuotaExceeded());
        assertNotNull(quota.getMonths());
        assertEquals("110101********2345", quota.getIdCardMasked());
    }

    @Test
    public void testQuerySettlement_returnsStatementUnderResolvedTenant() {
        int taxYear = LocalDate.now().getMonthValue() <= 3
                ? LocalDate.now().getYear() - 1 : LocalDate.now().getYear();
        PayeeInfoDO payee = insertPayee("王五", "110101199003033456");
        insertIssuedOrder("ORDER_S1", payee, new BigDecimal("120000.00"),
                LocalDate.of(taxYear, 6, 5).atTime(10, 0), new BigDecimal("0.01"));
        String token = mint("SETTLEMENT_STATEMENT", null, payee.getId());

        PublicSettlementStatementRespVO statement = publicAccessService.querySettlement(token);

        assertEquals("王五", statement.getSellerName());
        assertEquals(taxYear, statement.getTaxYear());
        assertEquals(1, statement.getInvoiceCount());
        assertEquals(0, new BigDecimal("120000.00").compareTo(statement.getInvoicedAmount()));
        assertTrue(statement.getMessage().contains("汇算清缴"), "实际：" + statement.getMessage());
    }

    @Test
    public void testGetOnboardingPage_realNameWhenNotVerified() {
        PayeeInfoDO payee = insertPayee("赵六", "110101199004044567");
        when(sellerOnboardingService.getOnboarding(payee.getId())).thenReturn(onboarding(0, null, null));
        SellerStepRespVO step = new SellerStepRespVO();
        step.setFormHtml("<form>real-name</form>");
        when(sellerOnboardingService.startRealName(any())).thenReturn(step);
        String token = mint("ONBOARDING", null, payee.getId());

        PublicOnboardingPageRespVO page = publicAccessService.getOnboardingPage(token);

        assertEquals("REAL_NAME", page.getStep());
        assertEquals("<form>real-name</form>", page.getFormHtml());
        verify(sellerOnboardingService).startRealName(any());
    }

    @Test
    public void testGetOnboardingPage_doneAfterRealNameWithoutAnyPage() {
        // 实名一过，自然人侧就没有事要做了：收方入驻是数据接口、由平台自动发起（ADR 0035），
        // 所以这里不再输出任何工行入驻页面。
        PayeeInfoDO payee = insertPayee("钱七", "110101199005055678");
        when(sellerOnboardingService.getOnboarding(payee.getId())).thenReturn(onboarding(2, null, null));
        String token = mint("ONBOARDING", null, payee.getId());

        PublicOnboardingPageRespVO page = publicAccessService.getOnboardingPage(token);

        assertEquals("DONE", page.getStep());
        assertNull(page.getFormHtml());
        assertTrue(page.getMessage().contains("平台"), "实际：" + page.getMessage());
        verify(sellerOnboardingService, never()).submitOnboarding(any());
    }

    @Test
    public void testGetOnboardingPage_doneWhenReady() {
        PayeeInfoDO payee = insertPayee("孙八", "110101199006066789");
        when(sellerOnboardingService.getOnboarding(payee.getId())).thenReturn(onboarding(2, "READY", true));
        String token = mint("ONBOARDING", null, payee.getId());

        PublicOnboardingPageRespVO page = publicAccessService.getOnboardingPage(token);

        assertEquals("DONE", page.getStep());
        assertNull(page.getFormHtml());
    }

    @Test
    public void testGetOnboardingPage_changeCardDoesNotOutputOnboardingPage() {
        PayeeInfoDO payee = insertPayee("王五", "110101199003033456");
        // 换卡在途也只是状态：换卡的入口是自然人端的表单（#89），不再是工行页面
        when(sellerOnboardingService.getOnboarding(payee.getId())).thenReturn(onboarding(2, "READY", true));
        when(sellerOnboardingService.hasPendingBankCardChange(payee.getId())).thenReturn(true);
        String token = mint("ONBOARDING", null, payee.getId());

        PublicOnboardingPageRespVO page = publicAccessService.getOnboardingPage(token);

        assertEquals("DONE", page.getStep());
        assertNull(page.getFormHtml());
        verify(sellerOnboardingService, never()).submitOnboarding(any());
    }

    @Test
    public void testSyncOnboarding_syncsCardChangeEvenWhenOnboardingReady() {
        PayeeInfoDO payee = insertPayee("冯六", "110101199010101234");
        SellerOnboardingRespVO ready = onboarding(2, "READY", true);
        ready.setBankCardChangeStatusName("银行审核中");
        when(sellerOnboardingService.getOnboarding(payee.getId()))
                .thenReturn(ready, onboarding(2, "READY", true));
        String token = mint("ONBOARDING", null, payee.getId());

        PublicOnboardingStatusRespVO status = publicAccessService.syncOnboarding(token);

        // 换卡在途时要主动向工行查一次（结果属于新卡），否则按钮会一直停在「建档已完成」
        verify(sellerOnboardingService).syncOnboarding(eq(payee.getId()));
        assertEquals("DONE", status.getStep());
    }

    @Test
    public void testGetOnboardingPage_failedOnboardingAsksContact() {
        PayeeInfoDO payee = insertPayee("周九", "110101199007077890");
        when(sellerOnboardingService.getOnboarding(payee.getId())).thenReturn(onboarding(2, "REJECTED", false));
        String token = mint("ONBOARDING", null, payee.getId());

        PublicOnboardingPageRespVO page = publicAccessService.getOnboardingPage(token);

        assertEquals("DONE", page.getStep());
        assertTrue(page.getMessage().contains("平台"), "实际：" + page.getMessage());
    }

    @Test
    public void testSyncOnboarding_syncsRealNameThenReturnsStatus() {
        PayeeInfoDO payee = insertPayee("吴十", "110101199008088901");
        when(sellerOnboardingService.getOnboarding(payee.getId()))
                .thenReturn(onboarding(0, null, null), onboarding(2, "READY", true));
        String token = mint("ONBOARDING", null, payee.getId());

        PublicOnboardingStatusRespVO status = publicAccessService.syncOnboarding(token);

        verify(sellerOnboardingService).syncRealName(eq(payee.getId()));
        assertEquals("DONE", status.getStep());
        assertEquals(Boolean.TRUE, status.getInvoiceEligible());
    }

    @Test
    public void testWriteOnboardingForm_returnsHtml() throws Exception {
        PayeeInfoDO payee = insertPayee("郑十一", "110101199009099012");
        when(sellerOnboardingService.getOnboarding(payee.getId())).thenReturn(onboarding(0, null, null));
        SellerStepRespVO step = new SellerStepRespVO();
        step.setFormHtml("<form>go</form>");
        when(sellerOnboardingService.startRealName(any())).thenReturn(step);
        String token = mint("ONBOARDING", null, payee.getId());

        MockHttpServletResponse response = new MockHttpServletResponse();
        publicAccessService.writeOnboardingForm(token, response);

        assertTrue(response.getContentAsString().contains("<form>go</form>"));
    }

    private SellerOnboardingRespVO onboarding(Integer realNameStatus, String onboardingState,
                                              Boolean invoiceEligible) {
        SellerOnboardingRespVO resp = new SellerOnboardingRespVO();
        resp.setRealNameStatus(realNameStatus);
        resp.setOnboardingState(onboardingState);
        resp.setOnboardingStateName(onboardingState);
        resp.setRealNameStatusName(realNameStatus != null && realNameStatus == 2 ? "认证通过" : "未认证");
        resp.setInvoiceEligible(invoiceEligible);
        resp.setInvoiceBlockReason(invoiceEligible != null && invoiceEligible ? null : "建档未完成");
        return resp;
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

    /**
     * 造一张已开出的蓝票：额度台账按开票日期落在窗口内才算
     */
    private void insertIssuedOrder(String partnerOrderId, PayeeInfoDO payee, BigDecimal amount,
                                   LocalDateTime invoiceDate, BigDecimal taxRate) {
        InvoiceOrderDO order = InvoiceOrderDO.builder()
                .orderNo("INV_" + partnerOrderId).partnerOrderId(partnerOrderId)
                .payeeId(payee.getId()).payeeNo(payee.getPartnerPayeeId()).payerNo("PAYER_1")
                .totalAmount(amount).invoiceAmount(amount).taxRate(taxRate)
                .invoiceType(1).businessType("SCRAP")
                .orderStatus(3).invoiceStatus(2).paymentStatus(2).taxStatus(0)
                .confirmStatus(1).preInvoiceStatus(2)
                .invoiceDate(invoiceDate).build();
        order.setCreateTime(invoiceDate);
        invoiceOrderMapper.insert(order);
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
