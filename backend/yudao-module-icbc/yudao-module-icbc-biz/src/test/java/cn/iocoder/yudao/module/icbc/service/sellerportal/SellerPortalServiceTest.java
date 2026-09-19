package cn.iocoder.yudao.module.icbc.service.sellerportal;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.icbc.UnitTestConfiguration;
import cn.iocoder.yudao.module.icbc.controller.app.seller.vo.*;
import cn.iocoder.yudao.module.icbc.dal.dataobject.acquisition.IcbcAcquisitionDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.authorization.IcbcSellerAuthorizationDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.invoice.InvoiceOrderDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.naturalperson.IcbcNaturalPersonDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payee.PayeeInfoDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payment.PaymentOrderDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.acquisition.IcbcAcquisitionMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.authorization.IcbcSellerAuthorizationMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.invoice.InvoiceOrderMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.payee.PayeeInfoMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.payment.PaymentOrderMapper;
import cn.iocoder.yudao.module.icbc.enums.AcquisitionStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.InvoiceIssueStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.PaymentStatusEnum;
import cn.iocoder.yudao.module.icbc.service.download.InvoiceDownloadService;
import cn.iocoder.yudao.module.icbc.service.naturalperson.NaturalPersonService;
import cn.iocoder.yudao.module.icbc.service.naturalperson.impl.NaturalPersonServiceImpl;
import cn.iocoder.yudao.module.icbc.service.sellerportal.impl.SellerPortalServiceImpl;
import cn.iocoder.yudao.module.icbc.service.settlement.SettlementService;
import cn.iocoder.yudao.module.system.api.tenant.TenantApi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * {@link SellerPortalServiceImpl} 的单元测试（#34）。
 *
 * <p>覆盖：跨企业仅本人可见、按企业分组、收款状态只讲可核验的事、「我收到了」不改银行状态、
 * 企业授权列表与自助撤销。
 */
@Import({SellerPortalServiceImpl.class, NaturalPersonServiceImpl.class, UnitTestConfiguration.class})
@Sql(scripts = "/sql/create_tables.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Transactional
@Rollback
public class SellerPortalServiceTest extends BaseDbUnitTest {

    private static final Long MEMBER_USER_ID = 9001L;

    @Resource
    private SellerPortalService sellerPortalService;
    @Resource
    private NaturalPersonService naturalPersonService;
    @Resource
    private PayeeInfoMapper payeeInfoMapper;
    @Resource
    private IcbcAcquisitionMapper acquisitionMapper;
    @Resource
    private PaymentOrderMapper paymentOrderMapper;
    @Resource
    private InvoiceOrderMapper invoiceOrderMapper;
    @Resource
    private IcbcSellerAuthorizationMapper authorizationMapper;

    @MockBean
    private TenantApi tenantApi;
    @MockBean
    private SettlementService settlementService;
    @MockBean
    private InvoiceDownloadService invoiceDownloadService;

    @AfterEach
    public void tearDown() {
        cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.clear();
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @Test
    public void testNotBound_rejected() {
        IcbcNaturalPersonDO person = register("110101199001011234", "13800138000");
        setLoginUser(MEMBER_USER_ID);
        assertServiceException(() -> sellerPortalService.getHome(person.getId()),
                NATURAL_PERSON_NOT_BOUND_TO_LOGIN);
    }

    @Test
    public void testRecords_groupedByEnterpriseAcrossTenants() {
        IcbcNaturalPersonDO person = register("110101199001011234", "13800138000");
        bindLogin(person);
        PayeeInfoDO payeeA = insertPayee(person.getId(), 1L, "PARTNER_A");
        PayeeInfoDO payeeB = insertPayee(person.getId(), 2L, "PARTNER_B");
        when(tenantApi.getTenantName(1L)).thenReturn("甲回收");
        when(tenantApi.getTenantName(2L)).thenReturn("乙回收");

        insertAcquisition(payeeA.getId(), "ACQ_A", new BigDecimal("100.00"), AcquisitionStatusEnum.INVOICED);
        insertAcquisition(payeeB.getId(), "ACQ_B", new BigDecimal("200.00"), AcquisitionStatusEnum.PAID);
        insertAcquisition(payeeB.getId(), "ACQ_B_CANCEL", new BigDecimal("300.00"), AcquisitionStatusEnum.CANCELLED);

        List<SellerRecordGroupRespVO> groups = sellerPortalService.getRecordGroups(person.getId());
        assertEquals(2, groups.size());
        SellerRecordGroupRespVO groupA = groups.stream()
                .filter(g -> g.getTenantId().equals(1L)).findFirst().orElseThrow();
        assertEquals("甲回收", groupA.getEnterpriseName());
        assertEquals(1, groupA.getCount());
        assertEquals(0, new BigDecimal("100.00").compareTo(groupA.getTotalAmount()));
        SellerRecordGroupRespVO groupB = groups.stream()
                .filter(g -> g.getTenantId().equals(2L)).findFirst().orElseThrow();
        assertEquals(2, groupB.getCount());
        // 取消的单不计入合计，但仍在列表里可见
        assertEquals(0, new BigDecimal("200.00").compareTo(groupB.getTotalAmount()));
        assertTrue(groupB.getRecords().stream().anyMatch(r -> r.getCancelReason() != null
                || AcquisitionStatusEnum.CANCELLED.getStatus().equals(r.getStatus())));
    }

    @Test
    public void testPayments_statusNameAndReceivedConfirm() {
        IcbcNaturalPersonDO person = register("110101199001011234", "13800138000");
        bindLogin(person);
        PayeeInfoDO payee = insertPayee(person.getId(), 1L, "PARTNER_A");
        when(tenantApi.getTenantName(1L)).thenReturn("甲回收");
        IcbcAcquisitionDO acquisition = insertAcquisition(payee.getId(), "ACQ_A",
                new BigDecimal("100.00"), AcquisitionStatusEnum.PAID);
        PaymentOrderDO payment = insertPayment(acquisition.getId(), PaymentStatusEnum.SUCCESS);

        List<SellerPaymentRespVO> payments = sellerPortalService.getPayments(person.getId());
        assertEquals(1, payments.size());
        assertEquals("银行已受理", payments.get(0).getStatusName());
        assertEquals("RCPT_1", payments.get(0).getReceiptNo());
        assertTrue(payments.get(0).getCanConfirmReceive());
        assertFalse(payments.get(0).getSellerReceivedConfirmed());

        SellerConfirmReceiveReqVO req = new SellerConfirmReceiveReqVO();
        req.setNaturalPersonId(person.getId());
        req.setPaymentOrderId(payment.getId());
        sellerPortalService.confirmReceived(req, "10.0.0.1");

        PaymentOrderDO updated = paymentOrderMapper.selectById(payment.getId());
        assertNotNull(updated.getSellerReceivedConfirmedAt());
        assertEquals("10.0.0.1", updated.getSellerReceivedConfirmIp());
        // 银行状态不变
        assertEquals(PaymentStatusEnum.SUCCESS.getStatus(), updated.getPaymentStatus());
    }

    @Test
    public void testPayments_failedGivesNextStep() {
        IcbcNaturalPersonDO person = register("110101199001011234", "13800138000");
        bindLogin(person);
        PayeeInfoDO payee = insertPayee(person.getId(), 1L, "PARTNER_A");
        when(tenantApi.getTenantName(1L)).thenReturn("甲回收");
        IcbcAcquisitionDO acquisition = insertAcquisition(payee.getId(), "ACQ_A",
                new BigDecimal("100.00"), AcquisitionStatusEnum.PENDING_PAYMENT);
        insertPayment(acquisition.getId(), PaymentStatusEnum.FAILED);

        List<SellerPaymentRespVO> payments = sellerPortalService.getPayments(person.getId());
        assertEquals("付款失败", payments.get(0).getStatusName());
        assertNotNull(payments.get(0).getNextStep());
        assertFalse(payments.get(0).getCanConfirmReceive());
    }

    @Test
    public void testInvoices_summaryAndThreeStatusLines() {
        IcbcNaturalPersonDO person = register("110101199001011234", "13800138000");
        bindLogin(person);
        PayeeInfoDO payee = insertPayee(person.getId(), 1L, "PARTNER_A");
        when(tenantApi.getTenantName(1L)).thenReturn("甲回收");
        insertInvoice(payee.getId(), "INV_1", new BigDecimal("100.00"), new BigDecimal("9.00"),
                LocalDateTime.of(2026, 6, 1, 10, 0), InvoiceIssueStatusEnum.ISSUED);
        insertInvoice(payee.getId(), "INV_2", new BigDecimal("200.00"), new BigDecimal("18.00"),
                LocalDateTime.of(2025, 6, 1, 10, 0), InvoiceIssueStatusEnum.ISSUED);

        SellerInvoiceSummaryRespVO summary = sellerPortalService.getInvoices(person.getId(), 2026);
        assertEquals(1, summary.getInvoiceCount());
        assertEquals(0, new BigDecimal("100.00").compareTo(summary.getTotalInvoiceAmount()));
        assertEquals(0, new BigDecimal("9.00").compareTo(summary.getTotalTaxAmount()));
        assertEquals("税务端可核验的口径由各回收企业的开票记录构成", summary.getTaxScopeNote());
        // 三条状态线分别有名字
        assertEquals("已开票", summary.getInvoices().get(0).getInvoiceStatusName());
        assertNotNull(summary.getInvoices().get(0).getTaxStatusName());
        assertNotNull(summary.getInvoices().get(0).getUploadStatusName());
    }

    @Test
    public void testAuthorizations_revokeAndIdempotentGuard() {
        IcbcNaturalPersonDO person = register("110101199001011234", "13800138000");
        bindLogin(person);
        PayeeInfoDO payee = insertPayee(person.getId(), 1L, "PARTNER_A");
        when(tenantApi.getTenantName(1L)).thenReturn("甲回收");
        insertAuthorization(payee.getId(), true, true);

        List<SellerAuthorizationRespVO> list = sellerPortalService.getAuthorizations(person.getId());
        assertEquals(1, list.size());
        assertTrue(list.get(0).getReverseInvoiceAuthorized());
        assertFalse(list.get(0).getRevoked());

        SellerRevokeAuthorizationReqVO req = new SellerRevokeAuthorizationReqVO();
        req.setNaturalPersonId(person.getId());
        req.setTenantId(1L);
        req.setReason("不想继续授权");
        sellerPortalService.revokeAuthorization(req);

        SellerAuthorizationRespVO revoked = sellerPortalService.getAuthorizations(person.getId()).get(0);
        assertFalse(revoked.getReverseInvoiceAuthorized());
        assertFalse(revoked.getTaxAgencyAuthorized());
        assertTrue(revoked.getRevoked());
        assertEquals("不想继续授权", revoked.getRevokeReason());

        assertServiceException(() -> sellerPortalService.revokeAuthorization(req),
                SELLER_AUTHORIZATION_ALREADY_REVOKED);
    }

    @Test
    public void testProfile_masksCardAndMobile() {
        IcbcNaturalPersonDO person = register("110101199001011234", "13800138000");
        bindLogin(person);
        PayeeInfoDO payee = insertPayee(person.getId(), 1L, "PARTNER_A");
        payee.setBankCardNo("6222020200001234");
        payee.setBankName("中国工商银行");
        payeeInfoMapper.updateById(payee);
        when(tenantApi.getTenantName(1L)).thenReturn("甲回收");

        SellerProfileRespVO profile = sellerPortalService.getProfile(person.getId());
        assertEquals("138****8000", profile.getMobileMasked());
        assertEquals(1, profile.getBankCards().size());
        assertEquals("1234", profile.getBankCards().get(0).getCardTail());
        assertEquals("中国工商银行", profile.getBankCards().get(0).getBankName());
        assertNotNull(profile.getServiceMobile());
        assertTrue(profile.getLogoutNote().contains("不等于删除交易记录"));
    }

    // ==================== 造数 ====================

    private IcbcNaturalPersonDO register(String idCardNo, String mobile) {
        cn.iocoder.yudao.module.icbc.controller.admin.naturalperson.vo.NaturalPersonRegisterReqVO req =
                new cn.iocoder.yudao.module.icbc.controller.admin.naturalperson.vo.NaturalPersonRegisterReqVO();
        req.setName("张三");
        req.setIdCardNo(idCardNo);
        req.setMobile(mobile);
        return naturalPersonService.register(req);
    }

    private void bindLogin(IcbcNaturalPersonDO person) {
        naturalPersonService.bindLogin(person.getId(), MEMBER_USER_ID, "REGISTER", null);
        setLoginUser(MEMBER_USER_ID);
    }

    private void setLoginUser(Long memberUserId) {
        cn.iocoder.yudao.framework.security.core.LoginUser loginUser =
                new cn.iocoder.yudao.framework.security.core.LoginUser();
        loginUser.setId(memberUserId);
        loginUser.setUserType(cn.iocoder.yudao.framework.common.enums.UserTypeEnum.MEMBER.getValue());
        org.springframework.security.core.context.SecurityContextHolder.getContext()
                .setAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        loginUser, null, null));
    }

    private PayeeInfoDO insertPayee(Long naturalPersonId, Long tenantId, String partnerPayeeId) {
        PayeeInfoDO payee = PayeeInfoDO.builder()
                .partnerPayeeId(partnerPayeeId)
                .naturalPersonId(naturalPersonId)
                .name("张三")
                .mobile("13800138000")
                .idCardNo("110101199001011234")
                .build();
        payee.setTenantId(tenantId);
        payeeInfoMapper.insert(payee);
        return payee;
    }

    private IcbcAcquisitionDO insertAcquisition(Long payeeId, String no, BigDecimal amount,
                                                AcquisitionStatusEnum status) {
        IcbcAcquisitionDO acquisition = IcbcAcquisitionDO.builder()
                .acquisitionNo(no)
                .payeeId(payeeId)
                .partnerPayeeId("PARTNER_1")
                .sellerName("张三")
                .sellerMobile("13800138000")
                .categoryName("废钢")
                .unit("吨")
                .quantity(new BigDecimal("1"))
                .unitPrice(new BigDecimal("100"))
                .settlementWeight(new BigDecimal("10"))
                .amount(amount)
                .status(status.getStatus())
                .cancelReason(status == AcquisitionStatusEnum.CANCELLED ? "货不对" : null)
                .build();
        acquisitionMapper.insert(acquisition);
        return acquisition;
    }

    private PaymentOrderDO insertPayment(Long acquisitionId, PaymentStatusEnum status) {
        PaymentOrderDO payment = PaymentOrderDO.builder()
                .orderNo("PAY_" + acquisitionId + "_" + System.nanoTime())
                .partnerOrderId("ACQ_" + acquisitionId + "_" + System.nanoTime())
                .acquisitionId(acquisitionId)
                .paymentAmount(new BigDecimal("100.00"))
                .paymentStatus(status.getStatus())
                .receiptNo(PaymentStatusEnum.SUCCESS.equals(status) ? "RCPT_1" : null)
                .build();
        paymentOrderMapper.insert(payment);
        return payment;
    }

    private InvoiceOrderDO insertInvoice(Long payeeId, String no, BigDecimal amount, BigDecimal tax,
                                         LocalDateTime invoiceDate, InvoiceIssueStatusEnum status) {
        InvoiceOrderDO invoice = InvoiceOrderDO.builder()
                .orderNo("ORDER_" + no)
                .partnerOrderId(no)
                .payeeId(payeeId)
                .totalAmount(amount)
                .invoiceStatus(status.getStatus())
                .taxStatus(2)
                .uploadStatus(1)
                .invoiceNo("NO_" + no)
                .invoiceDate(invoiceDate)
                .invoiceAmount(amount)
                .taxAmount(tax)
                .build();
        invoiceOrderMapper.insert(invoice);
        return invoice;
    }

    private void insertAuthorization(Long payeeId, boolean reverse, boolean tax) {
        IcbcSellerAuthorizationDO authorization = IcbcSellerAuthorizationDO.builder()
                .payeeId(payeeId)
                .reverseInvoiceAuthorized(reverse)
                .taxAgencyAuthorized(tax)
                .authorizedAt(LocalDateTime.now())
                .channel("ONSITE")
                .build();
        authorizationMapper.insert(authorization);
    }

}
