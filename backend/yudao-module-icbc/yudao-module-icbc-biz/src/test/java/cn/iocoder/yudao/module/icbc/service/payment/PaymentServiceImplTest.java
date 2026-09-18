package cn.iocoder.yudao.module.icbc.service.payment;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.icbc.UnitTestConfiguration;
import cn.iocoder.yudao.module.icbc.controller.admin.payment.vo.PaymentApplyReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.payment.vo.PaymentApplyRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.payment.vo.PaymentQueryReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.payment.vo.PaymentReceiptRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.payment.vo.PaymentStatusRespVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.acquisition.IcbcAcquisitionDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.invoice.InvoiceOrderDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payment.PaymentOrderDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.invoice.InvoiceOrderMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.payment.PaymentOrderMapper;
import cn.iocoder.yudao.module.icbc.enums.PaymentStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.PreInvoiceStatusEnum;
import cn.iocoder.yudao.module.icbc.gateway.IcbcGatewayResult;
import cn.iocoder.yudao.module.icbc.gateway.fake.FakeIcbcGateway;
import cn.iocoder.yudao.module.icbc.gateway.model.InvoiceInfo;
import cn.iocoder.yudao.module.icbc.gateway.model.PaymentReq;
import cn.iocoder.yudao.module.icbc.service.acquisition.AcquisitionService;
import cn.iocoder.yudao.module.icbc.service.invoice.InvoiceOrderService;
import cn.iocoder.yudao.module.icbc.service.payment.impl.PaymentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import javax.annotation.Resource;
import java.math.BigDecimal;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * {@link PaymentServiceImpl} 的单元测试，覆盖 issue #9 的验收：
 * 预开票成功才能发起、金额不一致被拦、重复发起不重复提交、异常状态可重新发起、
 * 通知与主动查询两侧收敛一致、成功后回单归档并挂回收购单。
 */
@Import({UnitTestConfiguration.class, PaymentServiceImpl.class})
@TestPropertySource(properties = "icbc.gateway.mode=fake")
public class PaymentServiceImplTest extends BaseDbUnitTest {

    private static final Long ACQUISITION_ID = 1024L;
    private static final String PARTNER_ORDER_ID = "ACQ202601011200001234";

    private Long invoiceOrderId;

    @Resource
    private PaymentService paymentService;

    @Resource
    private PaymentOrderMapper paymentOrderMapper;

    @Resource
    private InvoiceOrderMapper invoiceOrderMapper;

    @Resource
    private FakeIcbcGateway fakeIcbcGateway;

    @MockBean
    private AcquisitionService acquisitionService;

    @MockBean
    private InvoiceOrderService invoiceOrderService;

    @BeforeEach
    public void setUp() {
        fakeIcbcGateway.reset();
    }

    // ==================== 发起付款 ====================

    @Test
    public void testApplyPayment_successGeneratesPaymentPageAndLinksAcquisition() {
        stubInvoiceOrder(PreInvoiceStatusEnum.SUCCESS);
        stubAcquisition(new BigDecimal("1000.00"));

        PaymentApplyRespVO resp = paymentService.applyPayment(buildApply(
                PARTNER_ORDER_ID, new BigDecimal("1000.00")));

        assertTrue(resp.getSuccess());
        assertFalse(resp.getDuplicate());
        assertNotNull(resp.getPayPageHtml());
        assertTrue(resp.getPayPageHtml().contains("payment"));
        assertEquals(PARTNER_ORDER_ID, resp.getPartnerOrderId());
        assertEquals(new BigDecimal("1000.00"), resp.getPayAmount());

        // 工行收到的是公对私结算指令，资金不经平台
        PaymentReq submitted = fakeIcbcGateway.lastPayload(FakeIcbcGateway.OP_SUBMIT_PAYMENT);
        assertEquals(PARTNER_ORDER_ID, submitted.getOutOrderId());
        assertEquals("VENDOR_2001", submitted.getOutVendorId());
        assertEquals("USER_1001", submitted.getOutUserId());
        assertEquals(1L, fakeIcbcGateway.countOperation(FakeIcbcGateway.OP_SUBMIT_PAYMENT));

        // 支付单落库并挂到收购单与开票单上
        PaymentOrderDO order = paymentOrderMapper.selectByPartnerOrderId(PARTNER_ORDER_ID);
        assertNotNull(order);
        assertEquals(ACQUISITION_ID, order.getAcquisitionId());
        assertEquals(invoiceOrderId, order.getInvoiceOrderId());
        assertEquals(new BigDecimal("1000.00"), order.getPaymentAmount());
        assertEquals(PaymentStatusEnum.PENDING.getStatus(), order.getPaymentStatus());
    }

    @Test
    public void testApplyPayment_byAcquisitionIdResolvesPartnerOrderId() {
        stubInvoiceOrder(PreInvoiceStatusEnum.SUCCESS);
        stubAcquisition(new BigDecimal("1000.00"));

        PaymentApplyReqVO reqVO = new PaymentApplyReqVO();
        reqVO.setAcquisitionId(ACQUISITION_ID);
        PaymentApplyRespVO resp = paymentService.applyPayment(reqVO);

        assertTrue(resp.getSuccess());
        assertEquals(PARTNER_ORDER_ID, resp.getPartnerOrderId());
        assertEquals(1L, fakeIcbcGateway.countOperation(FakeIcbcGateway.OP_SUBMIT_PAYMENT));
    }

    @Test
    public void testApplyPayment_rejectsWhenPreInvoiceNotSuccess() {
        stubInvoiceOrder(PreInvoiceStatusEnum.IN_PROGRESS);
        stubAcquisition(new BigDecimal("1000.00"));

        assertServiceException(() -> paymentService.applyPayment(buildApply(PARTNER_ORDER_ID, null)),
                PAYMENT_PRE_INVOICE_NOT_SUCCESS);

        assertEquals(0L, fakeIcbcGateway.countOperation(FakeIcbcGateway.OP_SUBMIT_PAYMENT));
        assertTrue(paymentOrderMapper.selectList().isEmpty());
    }

    @Test
    public void testApplyPayment_rejectsAmountMismatch() {
        stubInvoiceOrder(PreInvoiceStatusEnum.SUCCESS);
        stubAcquisition(new BigDecimal("1000.00"));

        assertServiceException(() -> paymentService.applyPayment(buildApply(
                        PARTNER_ORDER_ID, new BigDecimal("999.00"))),
                PAYMENT_AMOUNT_MISMATCH, "1000.00", "999.00");

        assertEquals(0L, fakeIcbcGateway.countOperation(FakeIcbcGateway.OP_SUBMIT_PAYMENT));
        assertTrue(paymentOrderMapper.selectList().isEmpty());
    }

    @Test
    public void testApplyPayment_duplicateDoesNotResubmitWhenAlreadySuccess() {
        stubInvoiceOrder(PreInvoiceStatusEnum.SUCCESS);
        stubAcquisition(new BigDecimal("1000.00"));
        insertPaymentOrder(PaymentStatusEnum.SUCCESS, 0);

        PaymentApplyRespVO resp = paymentService.applyPayment(buildApply(PARTNER_ORDER_ID, null));

        assertTrue(resp.getDuplicate());
        assertEquals(0L, fakeIcbcGateway.countOperation(FakeIcbcGateway.OP_SUBMIT_PAYMENT));
        assertEquals(1L, (long) paymentOrderMapper.selectList().size());
    }

    @Test
    public void testApplyPayment_doesNotResubmitWhilePending() {
        stubInvoiceOrder(PreInvoiceStatusEnum.SUCCESS);
        stubAcquisition(new BigDecimal("1000.00"));
        insertPaymentOrder(PaymentStatusEnum.PENDING, 0);

        PaymentApplyRespVO resp = paymentService.applyPayment(buildApply(PARTNER_ORDER_ID, null));

        assertTrue(resp.getDuplicate());
        assertEquals(0L, fakeIcbcGateway.countOperation(FakeIcbcGateway.OP_SUBMIT_PAYMENT));
    }

    @Test
    public void testApplyPayment_reInitiatesAfterFailure() {
        stubInvoiceOrder(PreInvoiceStatusEnum.SUCCESS);
        stubAcquisition(new BigDecimal("1000.00"));
        insertPaymentOrder(PaymentStatusEnum.FAILED, 0);

        PaymentApplyRespVO resp = paymentService.applyPayment(buildApply(PARTNER_ORDER_ID, null));

        assertTrue(resp.getSuccess());
        assertFalse(resp.getDuplicate());
        assertNotNull(resp.getPayPageHtml());
        assertEquals(1L, fakeIcbcGateway.countOperation(FakeIcbcGateway.OP_SUBMIT_PAYMENT));
        PaymentOrderDO order = paymentOrderMapper.selectByPartnerOrderId(PARTNER_ORDER_ID);
        assertEquals(PaymentStatusEnum.PENDING.getStatus(), order.getPaymentStatus());
        assertEquals(1, order.getRetryCount());
        assertEquals(1L, (long) paymentOrderMapper.selectList().size());
    }

    @Test
    public void testApplyPayment_businessFailureMarksFailed() {
        stubInvoiceOrder(PreInvoiceStatusEnum.SUCCESS);
        stubAcquisition(new BigDecimal("1000.00"));
        fakeIcbcGateway.setPaymentResult(IcbcGatewayResult.businessFailed(40001, "账户状态异常"));

        PaymentApplyRespVO resp = paymentService.applyPayment(buildApply(PARTNER_ORDER_ID, null));

        assertFalse(resp.getSuccess());
        PaymentOrderDO order = paymentOrderMapper.selectByPartnerOrderId(PARTNER_ORDER_ID);
        assertEquals(PaymentStatusEnum.FAILED.getStatus(), order.getPaymentStatus());
        assertEquals("40001", order.getErrorCode());
        assertEquals("账户状态异常", order.getErrorMsg());
        // 失败状态可见且可重新发起
        assertTrue(PaymentStatusEnum.isReInitiable(order.getPaymentStatus()));
    }

    // ==================== 状态收敛（通知 / 查询共用） ====================

    @Test
    public void testApplyPaymentStatus_successArchivesReceiptAndMarksAcquisitionPaid() {
        stubInvoiceOrder(PreInvoiceStatusEnum.SUCCESS);
        insertPaymentOrder(PaymentStatusEnum.PENDING, 0);

        paymentService.applyPaymentStatus(PARTNER_ORDER_ID, "02", "1000.00", "1000.00", "SN20260101", "ICBC-1");

        PaymentOrderDO order = paymentOrderMapper.selectByPartnerOrderId(PARTNER_ORDER_ID);
        assertEquals(PaymentStatusEnum.SUCCESS.getStatus(), order.getPaymentStatus());
        assertEquals("02", order.getPayStatus());
        assertEquals("SN20260101", order.getPaymentSerialNo());
        assertEquals("ICBC-1", order.getIcbcOrderNo());
        assertEquals(new BigDecimal("1000.00"), order.getActuallyReceivedAmount());
        assertEquals("SN20260101", order.getReceiptNo());
        assertNotNull(order.getReceiptTime());
        assertNotNull(order.getPaymentTime());

        // 回单归档可查
        PaymentReceiptRespVO receipt = paymentService.getReceipt(PARTNER_ORDER_ID);
        assertEquals("SN20260101", receipt.getReceiptNo());
        assertEquals(new BigDecimal("1000.00"), receipt.getActuallyReceivedAmount());

        // 开票单支付状态回写，收购单推进为「已付款」
        verify(invoiceOrderService).updateOrderStatus(invoiceOrderId, null, null,
                PaymentStatusEnum.SUCCESS.getStatus(), null);
        verify(acquisitionService).markPaidByInvoicePartnerOrderId(PARTNER_ORDER_ID);
    }

    @Test
    public void testApplyPaymentStatus_reversalIsVisibleAndReInitiable() {
        stubInvoiceOrder(PreInvoiceStatusEnum.SUCCESS);
        insertPaymentOrder(PaymentStatusEnum.PENDING, 0);

        paymentService.applyPaymentStatus(PARTNER_ORDER_ID, "05", "1000.00", null, null, null);

        PaymentOrderDO order = paymentOrderMapper.selectByPartnerOrderId(PARTNER_ORDER_ID);
        assertEquals(PaymentStatusEnum.REVERSED.getStatus(), order.getPaymentStatus());
        assertEquals("已冲正", PaymentStatusEnum.nameOf(order.getPaymentStatus()));
        assertTrue(PaymentStatusEnum.isReInitiable(order.getPaymentStatus()));
        verify(acquisitionService, never()).markPaidByInvoicePartnerOrderId(anyString());
    }

    @Test
    public void testApplyPaymentStatus_refundIsVisibleAndReInitiable() {
        stubInvoiceOrder(PreInvoiceStatusEnum.SUCCESS);
        insertPaymentOrder(PaymentStatusEnum.PENDING, 0);

        paymentService.applyPaymentStatus(PARTNER_ORDER_ID, "06", "1000.00", null, null, null);

        PaymentOrderDO order = paymentOrderMapper.selectByPartnerOrderId(PARTNER_ORDER_ID);
        assertEquals(PaymentStatusEnum.REFUNDED.getStatus(), order.getPaymentStatus());
        assertTrue(PaymentStatusEnum.isReInitiable(order.getPaymentStatus()));
        verify(acquisitionService, never()).markPaidByInvoicePartnerOrderId(anyString());
    }

    @Test
    public void testApplyPaymentStatus_partialSuccessIsVisible() {
        stubInvoiceOrder(PreInvoiceStatusEnum.SUCCESS);
        insertPaymentOrder(PaymentStatusEnum.PENDING, 0);

        paymentService.applyPaymentStatus(PARTNER_ORDER_ID, "25", "1000.00", "600.00", "SN-PART", null);

        PaymentOrderDO order = paymentOrderMapper.selectByPartnerOrderId(PARTNER_ORDER_ID);
        assertEquals(PaymentStatusEnum.PARTIAL_SUCCESS.getStatus(), order.getPaymentStatus());
        assertEquals(new BigDecimal("600.00"), order.getActuallyReceivedAmount());
        assertTrue(PaymentStatusEnum.isReInitiable(order.getPaymentStatus()));
        verify(acquisitionService, never()).markPaidByInvoicePartnerOrderId(anyString());
    }

    @Test
    public void testApplyPaymentStatus_unknownOrderIsReplayable() {
        assertThrows(ServiceException.class,
                () -> paymentService.applyPaymentStatus("NOT_EXISTS", "02", "1000.00", null, null, null));
    }

    // ==================== 主动查询 ====================

    @Test
    public void testQueryPaymentStatus_convergesFromActiveQuery() {
        stubInvoiceOrder(PreInvoiceStatusEnum.SUCCESS);
        insertPaymentOrder(PaymentStatusEnum.PENDING, 0);
        fakeIcbcGateway.setInvoiceInfoResult(IcbcGatewayResult.success(
                InvoiceInfo.builder().payStatus("02").payAmount("1000.00")
                        .actuallyReceivedAmount("1000.00").serialNo("SN-QUERY").build(), 0, "成功"));

        PaymentStatusRespVO resp = paymentService.queryPaymentStatus(buildQuery(PARTNER_ORDER_ID));

        assertEquals(PaymentStatusEnum.SUCCESS.getStatus(), resp.getPaymentStatus());
        assertEquals("02", resp.getPayStatus());
        assertEquals("SN-QUERY", resp.getPaymentSerialNo());
        // 落库，且与通知路径得到同一结果
        PaymentOrderDO order = paymentOrderMapper.selectByPartnerOrderId(PARTNER_ORDER_ID);
        assertEquals(PaymentStatusEnum.SUCCESS.getStatus(), order.getPaymentStatus());
    }

    @Test
    public void testQueryPaymentStatus_unknownOrderNotFound() {
        assertServiceException(() -> paymentService.queryPaymentStatus(buildQuery("NOT_EXISTS")),
                PAYMENT_ORDER_NOT_EXISTS);
    }

    @Test
    public void testGetReceipt_notAvailableBeforeSuccess() {
        insertPaymentOrder(PaymentStatusEnum.PENDING, 0);
        assertServiceException(() -> paymentService.getReceipt(PARTNER_ORDER_ID),
                PAYMENT_RECEIPT_NOT_AVAILABLE);
    }

    // ==================== 造数据 ====================

    private PaymentApplyReqVO buildApply(String partnerOrderId, BigDecimal amount) {
        PaymentApplyReqVO reqVO = new PaymentApplyReqVO();
        reqVO.setPartnerOrderId(partnerOrderId);
        reqVO.setAmount(amount);
        reqVO.setVerifiedCode("20201128531215026");
        reqVO.setUkeyId("20201128531215026");
        return reqVO;
    }

    private PaymentQueryReqVO buildQuery(String partnerOrderId) {
        PaymentQueryReqVO reqVO = new PaymentQueryReqVO();
        reqVO.setPartnerOrderId(partnerOrderId);
        return reqVO;
    }

    private void stubInvoiceOrder(PreInvoiceStatusEnum preInvoiceStatus) {
        InvoiceOrderDO order = new InvoiceOrderDO();
        order.setOrderNo("INV_" + PARTNER_ORDER_ID);
        order.setPartnerOrderId(PARTNER_ORDER_ID);
        order.setAcquisitionId(ACQUISITION_ID);
        order.setPayeeNo("USER_1001");
        order.setPayerNo("VENDOR_2001");
        order.setTotalAmount(new BigDecimal("1000.00"));
        order.setInvoiceType(1);
        order.setBusinessType("SCRAP");
        order.setOrderStatus(0);
        order.setInvoiceStatus(0);
        order.setPaymentStatus(0);
        order.setTaxStatus(0);
        order.setConfirmStatus(1);
        order.setPreInvoiceStatus(preInvoiceStatus.getStatus());
        invoiceOrderMapper.insert(order);
        this.invoiceOrderId = order.getId();
    }

    private void stubAcquisition(BigDecimal amount) {
        IcbcAcquisitionDO acquisition = IcbcAcquisitionDO.builder()
                .id(ACQUISITION_ID)
                .acquisitionNo(PARTNER_ORDER_ID)
                .invoicePartnerOrderId(PARTNER_ORDER_ID)
                .amount(amount)
                .build();
        when(acquisitionService.getAcquisition(ACQUISITION_ID)).thenReturn(acquisition);
    }

    private void insertPaymentOrder(PaymentStatusEnum status, int retryCount) {
        PaymentOrderDO order = PaymentOrderDO.builder()
                .orderNo("PAY_" + PARTNER_ORDER_ID)
                .partnerOrderId(PARTNER_ORDER_ID)
                .acquisitionId(ACQUISITION_ID)
                .invoiceOrderId(invoiceOrderId)
                .payeeNo("USER_1001")
                .payerNo("VENDOR_2001")
                .paymentAmount(new BigDecimal("1000.00"))
                .paymentStatus(status.getStatus())
                .retryCount(retryCount)
                .build();
        paymentOrderMapper.insert(order);
    }

}
