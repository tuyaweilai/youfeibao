package cn.iocoder.yudao.module.icbc.service.notify;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.icbc.UnitTestConfiguration;
import cn.iocoder.yudao.module.icbc.controller.admin.notify.vo.*;
import cn.iocoder.yudao.module.icbc.controller.admin.publicapi.vo.PublicNoticeRespVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.acquisition.IcbcAcquisitionDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.invoice.InvoiceOrderDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.notify.IcbcSellerNotifyDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payee.PayeeInfoDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payment.PaymentOrderDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.settlement.IcbcSettlementDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.acquisition.IcbcAcquisitionMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.invoice.InvoiceOrderMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.notify.IcbcSellerNotifyMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.payee.PayeeInfoMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.payment.PaymentOrderMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.settlement.IcbcSettlementMapper;
import cn.iocoder.yudao.module.icbc.enums.*;
import cn.iocoder.yudao.module.icbc.service.notify.impl.SellerNotifyServiceImpl;
import cn.iocoder.yudao.module.icbc.service.token.PublicTokenCodec;
import cn.iocoder.yudao.module.icbc.service.token.PublicTokenService;
import cn.iocoder.yudao.module.icbc.service.token.impl.PublicTokenServiceImpl;
import cn.iocoder.yudao.module.system.api.sms.SmsSendApi;
import cn.iocoder.yudao.module.system.api.sms.dto.send.SmsSendSingleToUserReqDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * {@link SellerNotifyServiceImpl} 的单元测试（#36，ADR 0023）。
 *
 * <p>守住这条功能的边界：短信只发三条、开关默认关闭、链接是一次性令牌链接、
 * 同一业务事件不重复轰炸、没留手机号 / 没配入口都要落可解释的记录、
 * 收货员能一键拿到可复制转达的链接。
 */
@Import({SellerNotifyServiceImpl.class, PublicTokenServiceImpl.class, PublicTokenCodec.class,
        UnitTestConfiguration.class})
@TestPropertySource(properties = {
        "icbc.notify.seller-app-url=https://seller.example.com",
        "icbc.public-token.secret=test-public-token-secret-0123456789abcdef"
})
@Sql(scripts = "/sql/create_tables.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Transactional
public class SellerNotifyServiceTest extends BaseDbUnitTest {

    private static final Long TENANT_ID = 1L;

    @Resource
    private SellerNotifyService sellerNotifyService;
    @Resource
    private IcbcSellerNotifyMapper notifyMapper;
    @Resource
    private IcbcSettlementMapper settlementMapper;
    @Resource
    private PayeeInfoMapper payeeInfoMapper;
    @Resource
    private PaymentOrderMapper paymentOrderMapper;
    @Resource
    private InvoiceOrderMapper invoiceOrderMapper;
    @Resource
    private IcbcAcquisitionMapper acquisitionMapper;

    @MockBean
    private SmsSendApi smsSendApi;

    @BeforeEach
    public void setUp() {
        TenantContextHolder.setTenantId(TENANT_ID);
        when(smsSendApi.sendSingleSmsToMember(any())).thenReturn(90001L);
    }

    @AfterEach
    public void tearDown() {
        TenantContextHolder.clear();
    }

    // ==================== 开关与幂等 ====================

    @Test
    public void testSettlementPending_defaultSwitchOffOnlyRecordsSkipped() {
        PayeeInfoDO payee = insertPayee("13800138000");
        IcbcSettlementDO settlement = insertSettlement(payee);

        sellerNotifyService.onSettlementPending(settlement.getId());

        List<IcbcSellerNotifyDO> records = notifyMapper.selectList();
        assertEquals(1, records.size());
        assertEquals(SellerNotifyStatusEnum.SKIPPED_DISABLED.getStatus(), records.get(0).getStatus());
        assertEquals(SellerNotifyTypeEnum.SETTLEMENT_PENDING.getCode(), records.get(0).getBizType());
        assertTrue(records.get(0).getBizKey().startsWith(settlement.getSettlementNo() + ":v1"));
        verifyNoInteractions(smsSendApi);
    }

    @Test
    public void testSettlementPending_tenantSwitchOnSendsOnceAndStaysIdempotent() {
        openTenantSms();
        PayeeInfoDO payee = insertPayee("13800138001");
        IcbcSettlementDO settlement = insertSettlement(payee);

        sellerNotifyService.onSettlementPending(settlement.getId());
        sellerNotifyService.onSettlementPending(settlement.getId()); // 重复调用不应重复发

        List<IcbcSellerNotifyDO> records = notifyMapper.selectList();
        assertEquals(1, records.size());
        IcbcSellerNotifyDO record = records.get(0);
        assertEquals(SellerNotifyStatusEnum.SENT.getStatus(), record.getStatus());
        assertEquals(90001L, record.getSmsLogId());
        assertNotNull(record.getSendTime());
        assertNotNull(record.getLink());
        assertTrue(record.getLink().contains("purpose=" + PublicTokenPurposeEnum.SELLER_NOTICE.getCode()));
        assertTrue(record.getContent().contains(settlement.getSettlementNo()));

        ArgumentCaptor<SmsSendSingleToUserReqDTO> captor =
                ArgumentCaptor.forClass(SmsSendSingleToUserReqDTO.class);
        verify(smsSendApi, times(1)).sendSingleSmsToMember(captor.capture());
        assertEquals("13800138001", captor.getValue().getMobile());
        assertEquals(SellerNotifyTypeEnum.SETTLEMENT_PENDING.getTemplateCode(),
                captor.getValue().getTemplateCode());
        assertNotNull(captor.getValue().getTemplateParams().get("link"));
    }

    @Test
    public void testSettlementPending_noMobileRecordsReasonWithoutSending() {
        openTenantSms();
        PayeeInfoDO payee = insertPayee(null);
        IcbcSettlementDO settlement = insertSettlement(payee);

        sellerNotifyService.onSettlementPending(settlement.getId());

        IcbcSellerNotifyDO record = notifyMapper.selectList().get(0);
        assertEquals(SellerNotifyStatusEnum.SKIPPED_NO_MOBILE.getStatus(), record.getStatus());
        assertNotNull(record.getLink());
        assertNotNull(record.getErrorMsg());
        verifyNoInteractions(smsSendApi);
    }

    @Test
    public void testSettlementPending_onlyWarnsOnPendingStatus() {
        openTenantSms();
        PayeeInfoDO payee = insertPayee("13800138002");
        IcbcSettlementDO settlement = insertSettlement(payee);
        settlement.setConfirmStatus(SettlementConfirmStatusEnum.CONFIRMED.getStatus());
        settlementMapper.updateById(settlement);

        sellerNotifyService.onSettlementPending(settlement.getId());

        assertTrue(notifyMapper.selectList().isEmpty());
        verifyNoInteractions(smsSendApi);
    }

    // ==================== 付款异常 ====================

    @Test
    public void testPaymentException_sendsOnlyOnExceptionState() {
        openTenantSms();
        PayeeInfoDO payee = insertPayee("13800138003");
        InvoiceOrderDO invoice = insertInvoiceOrder(payee, "ACQ_PAY_1");
        insertPaymentOrder("ACQ_PAY_1", invoice.getId(), PaymentStatusEnum.FAILED.getStatus(), null);

        sellerNotifyService.onPaymentException("ACQ_PAY_1");
        sellerNotifyService.onPaymentException("ACQ_PAY_1"); // 幂等

        List<IcbcSellerNotifyDO> records = notifyMapper.selectList();
        assertEquals(1, records.size());
        assertEquals(SellerNotifyStatusEnum.SENT.getStatus(), records.get(0).getStatus());
        assertEquals(SellerNotifyTypeEnum.PAYMENT_EXCEPTION.getCode(), records.get(0).getBizType());
        verify(smsSendApi, times(1)).sendSingleSmsToMember(any());

        // 正常成功态不打扰
        insertInvoiceOrder(payee, "ACQ_PAY_2");
        insertPaymentOrder("ACQ_PAY_2", invoiceOrderMapper.selectByPartnerOrderId("ACQ_PAY_2").getId(),
                PaymentStatusEnum.SUCCESS.getStatus(), null);
        sellerNotifyService.onPaymentException("ACQ_PAY_2");
        assertEquals(1, notifyMapper.selectList().size());
    }

    // ==================== 发票开出 ====================

    @Test
    public void testInvoiceIssued_sendsWithInvoiceNo() {
        openTenantSms();
        PayeeInfoDO payee = insertPayee("13800138004");
        InvoiceOrderDO order = insertInvoiceOrder(payee, "ACQ_INV_1");
        order.setInvoiceStatus(InvoiceIssueStatusEnum.ISSUED.getStatus());
        order.setInvoiceNo("INV-2026-0001");
        order.setInvoiceAmount(new BigDecimal("1234.56"));
        invoiceOrderMapper.updateById(order);

        sellerNotifyService.onInvoiceIssued("ACQ_INV_1");

        IcbcSellerNotifyDO record = notifyMapper.selectList().get(0);
        assertEquals(SellerNotifyStatusEnum.SENT.getStatus(), record.getStatus());
        assertTrue(record.getContent().contains("INV-2026-0001"));
        assertEquals("ACQ_INV_1", record.getBizKey());
        // 发票用现有的一次性下载令牌（ORDER 维度）：点开就能下载，不需要注册
        assertTrue(record.getLink().contains("purpose=" + PublicTokenPurposeEnum.INVOICE_DOWNLOAD.getCode()));
    }

    // ==================== 收货员一键转达 ====================

    @Test
    public void testForwardLink_returnsTokenAndCopyableText() {
        PayeeInfoDO payee = insertPayee("13800138005");
        IcbcSettlementDO settlement = insertSettlement(payee);
        NotifyForwardLinkReqVO reqVO = new NotifyForwardLinkReqVO();
        reqVO.setSettlementId(settlement.getId());
        reqVO.setSendSms(true);

        NotifyForwardLinkRespVO resp = sellerNotifyService.forwardSettlementLink(reqVO);

        assertTrue(resp.getLinkConfigured());
        assertNotNull(resp.getToken());
        assertTrue(resp.getLink().contains(resp.getToken()));
        assertTrue(resp.getLink().contains("purpose=SELLER_NOTICE"));
        assertTrue(resp.getNotificationText().contains(settlement.getSettlementNo()));
        assertTrue(resp.getSmsSent());
        assertEquals("138****8005", resp.getMobileMasked());
        verify(smsSendApi, times(1)).sendSingleSmsToMember(any());
    }

    @Test
    public void testForwardLink_noMobileStillReturnsLinkForManualRelay() {
        PayeeInfoDO payee = insertPayee(null);
        IcbcSettlementDO settlement = insertSettlement(payee);
        NotifyForwardLinkReqVO reqVO = new NotifyForwardLinkReqVO();
        reqVO.setSettlementId(settlement.getId());
        reqVO.setSendSms(true);

        NotifyForwardLinkRespVO resp = sellerNotifyService.forwardSettlementLink(reqVO);

        assertNotNull(resp.getLink());
        assertFalse(resp.getSmsSent());
        assertNotNull(resp.getMessage());
        verifyNoInteractions(smsSendApi);
    }

    // ==================== 免登录通知 ====================

    @Test
    public void testGetNoticeForPayee_listsPendingSettlementAndPaymentException() {
        PayeeInfoDO payee = insertPayee("13800138006");
        IcbcSettlementDO settlement = insertSettlement(payee);
        IcbcAcquisitionDO acquisition = insertAcquisition(payee.getId(), "ACQ_NOTICE_1");
        InvoiceOrderDO invoice = insertInvoiceOrder(payee, "ACQ_NOTICE_1");
        insertPaymentOrder("ACQ_NOTICE_1", invoice.getId(), PaymentStatusEnum.REFUNDED.getStatus(), acquisition.getId());

        PublicNoticeRespVO notice = sellerNotifyService.getNoticeForPayee(payee.getId());

        assertEquals(TENANT_ID, notice.getTenantId());
        assertEquals(payee.getId(), notice.getPayeeId());
        assertEquals("张三", notice.getSellerName());
        assertEquals(2, notice.getItems().size());
        assertTrue(notice.getItems().stream().anyMatch(item ->
                SellerNotifyTypeEnum.SETTLEMENT_PENDING.getCode().equals(item.getType())
                        && settlement.getId().equals(item.getSettlementId())));
        assertTrue(notice.getItems().stream().anyMatch(item ->
                SellerNotifyTypeEnum.PAYMENT_EXCEPTION.getCode().equals(item.getType())));
        // 对外口径：金额为本平台累计，不出现「已到账」
        assertTrue(notice.getScopeNote().contains("本平台累计"));
        assertFalse(notice.getScopeNote().contains("已到账"));
        assertNotNull(acquisition.getId());
    }

    // ==================== 设置 ====================

    @Test
    public void testSetting_defaultOffThenTenantCanOpen() {
        NotifySettingRespVO initial = sellerNotifyService.getSetting();
        assertFalse(initial.getPlatformEnabled());
        assertFalse(initial.getTenantEnabled());
        assertFalse(initial.getEffectiveEnabled());
        assertTrue(initial.getSellerAppUrlConfigured());

        NotifySettingSaveReqVO reqVO = new NotifySettingSaveReqVO();
        reqVO.setSmsEnabled(true);
        reqVO.setRemark("运营同意承担短信费用");
        sellerNotifyService.saveSetting(reqVO);

        NotifySettingRespVO after = sellerNotifyService.getSetting();
        assertTrue(after.getTenantEnabled());
        assertTrue(after.getEffectiveEnabled());
        assertEquals("运营同意承担短信费用", after.getRemark());
    }

    @Test
    public void testNotifyPage_mapsStatusAndMasksMobile() {
        openTenantSms();
        PayeeInfoDO payee = insertPayee("13800138007");
        IcbcSettlementDO settlement = insertSettlement(payee);
        sellerNotifyService.onSettlementPending(settlement.getId());

        PageResult<NotifyRespVO> page = sellerNotifyService.getNotifyPage(new NotifyPageReqVO());

        assertEquals(1, page.getTotal());
        NotifyRespVO item = page.getList().get(0);
        assertEquals(SellerNotifyTypeEnum.SETTLEMENT_PENDING.getName(), item.getBizTypeName());
        assertEquals(SellerNotifyStatusEnum.SENT.getName(), item.getStatusName());
        assertEquals("138****8007", item.getMobileMasked());
        assertNotNull(item.getLink());
    }

    @Test
    public void testSettlementPending_secondVersionSendsAgain() {
        openTenantSms();
        PayeeInfoDO payee = insertPayee("13800138008");
        IcbcSettlementDO settlement = insertSettlement(payee);
        sellerNotifyService.onSettlementPending(settlement.getId());

        // 企业对异议改版：新版本号 → 需要重新提醒一次
        settlement.setCurrentVersionNo(2);
        settlementMapper.updateById(settlement);
        sellerNotifyService.onSettlementPending(settlement.getId());

        assertEquals(2, notifyMapper.selectList().size());
        verify(smsSendApi, times(2)).sendSingleSmsToMember(any());
    }

    // ==================== 辅助 ====================

    private void openTenantSms() {
        NotifySettingSaveReqVO reqVO = new NotifySettingSaveReqVO();
        reqVO.setSmsEnabled(true);
        reqVO.setRemark("测试开启");
        sellerNotifyService.saveSetting(reqVO);
    }

    private PayeeInfoDO insertPayee(String mobile) {
        PayeeInfoDO payee = PayeeInfoDO.builder()
                .partnerPayeeId("PARTNER_" + (mobile == null ? "NO_MOBILE" : mobile))
                .name("张三")
                .mobile(mobile)
                .idCardNo("11010119900101" + (mobile == null ? "0000" : mobile.substring(7)))
                .naturalPersonId(5000L)
                .build();
        payeeInfoMapper.insert(payee);
        return payee;
    }

    private IcbcSettlementDO insertSettlement(PayeeInfoDO payee) {
        IcbcSettlementDO settlement = IcbcSettlementDO.builder()
                .settlementNo("ST" + System.nanoTime())
                .payeeId(payee.getId())
                .naturalPersonId(payee.getNaturalPersonId())
                .sellerName(payee.getName())
                .sellerMobile(payee.getMobile())
                .currentVersionNo(1)
                .confirmStatus(SettlementConfirmStatusEnum.PENDING.getStatus())
                .disputeCount(0)
                .build();
        settlementMapper.insert(settlement);
        return settlement;
    }

    private IcbcAcquisitionDO insertAcquisition(Long payeeId, String no) {
        IcbcAcquisitionDO acquisition = IcbcAcquisitionDO.builder()
                .acquisitionNo(no)
                .payeeId(payeeId)
                .sellerName("张三")
                .sellerMobile("13800138000")
                .categoryName("废钢")
                .unit("吨")
                .amount(new BigDecimal("1000.00"))
                .status(AcquisitionStatusEnum.REGISTERED.getStatus())
                .build();
        acquisitionMapper.insert(acquisition);
        return acquisition;
    }

    private InvoiceOrderDO insertInvoiceOrder(PayeeInfoDO payee, String partnerOrderId) {
        InvoiceOrderDO order = InvoiceOrderDO.builder()
                .orderNo("ORD" + System.nanoTime())
                .partnerOrderId(partnerOrderId)
                .payeeId(payee.getId())
                .totalAmount(new BigDecimal("1000.00"))
                .orderStatus(0)
                .invoiceStatus(InvoiceIssueStatusEnum.NOT_ISSUED.getStatus())
                .paymentStatus(PaymentStatusEnum.SUCCESS.getStatus())
                .taxStatus(TaxStatusEnum.NOT_TAXED.getStatus())
                .confirmStatus(InvoiceConfirmStatusEnum.NOT_CONFIRMED.getStatus())
                .preInvoiceStatus(PreInvoiceStatusEnum.SUCCESS.getStatus())
                .build();
        invoiceOrderMapper.insert(order);
        return order;
    }

    private PaymentOrderDO insertPaymentOrder(String partnerOrderId, Long invoiceOrderId, Integer status,
                                             Long acquisitionId) {
        PaymentOrderDO order = PaymentOrderDO.builder()
                .orderNo("PAY" + System.nanoTime())
                .partnerOrderId(partnerOrderId)
                .invoiceOrderId(invoiceOrderId)
                .acquisitionId(acquisitionId)
                .paymentAmount(new BigDecimal("1000.00"))
                .paymentStatus(status)
                .retryCount(0)
                .build();
        paymentOrderMapper.insert(order);
        return order;
    }

}
