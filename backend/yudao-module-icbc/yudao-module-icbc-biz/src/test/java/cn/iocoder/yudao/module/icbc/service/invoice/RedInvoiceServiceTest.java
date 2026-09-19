package cn.iocoder.yudao.module.icbc.service.invoice;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.icbc.UnitTestConfiguration;
import cn.iocoder.yudao.module.icbc.controller.admin.invoice.vo.RedInvoiceApplyReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.invoice.vo.RedInvoiceApplyResultVO;
import cn.iocoder.yudao.module.icbc.controller.admin.invoice.vo.RedInvoiceQueryRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.invoice.vo.RedInvoiceRevokeReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.invoice.InvoiceOrderDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.invoice.OrderItemDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.invoice.RedInvoiceDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.invoice.InvoiceOrderMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.invoice.OrderItemMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.invoice.RedInvoiceMapper;
import cn.iocoder.yudao.module.icbc.enums.InvoiceIssueStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.PreInvoiceStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.RedOffsetStatusEnum;
import cn.iocoder.yudao.module.icbc.gateway.IcbcGatewayResult;
import cn.iocoder.yudao.module.icbc.gateway.fake.FakeIcbcGateway;
import cn.iocoder.yudao.module.icbc.gateway.model.InvoiceInfo;
import cn.iocoder.yudao.module.icbc.gateway.model.InvoiceCancelResult;
import cn.iocoder.yudao.module.icbc.gateway.model.RedInvoiceReq;
import cn.iocoder.yudao.module.icbc.gateway.model.RedInvoiceRevokeResult;
import cn.iocoder.yudao.module.icbc.service.goodscfg.IcbcGoodsConfigService;
import cn.iocoder.yudao.module.icbc.service.invoice.impl.InvoiceOrderServiceImpl;
import cn.iocoder.yudao.module.icbc.service.invoice.impl.RedInvoiceServiceImpl;
import cn.iocoder.yudao.module.icbc.service.onboarding.SellerOnboardingService;
import cn.iocoder.yudao.module.icbc.service.qualification.IcbcQualificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import javax.annotation.Resource;
import java.math.BigDecimal;

import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 红冲与发票取消的单元测试（issue #14）。
 *
 * <p>覆盖验收：四种红冲原因、开票有误全额红冲且明细与蓝票一致、红蓝一一对应、
 * 撤销未生效的红字确认单、取消预开票成功但未支付的发票、失败 / 异常可见可重试不重复下单、
 * 通知乱序 / 重复 / 早到最终一致。
 */
@Import({UnitTestConfiguration.class, RedInvoiceServiceImpl.class, InvoiceOrderServiceImpl.class})
@TestPropertySource(properties = "icbc.gateway.mode=fake")
public class RedInvoiceServiceTest extends BaseDbUnitTest {

    private static final String PARTNER_ORDER_ID = "ACQ_RED_1";

    @Resource
    private RedInvoiceService redInvoiceService;

    @Resource
    private RedInvoiceMapper redInvoiceMapper;

    @Resource
    private InvoiceOrderMapper invoiceOrderMapper;

    @Resource
    private OrderItemMapper orderItemMapper;

    @Resource
    private FakeIcbcGateway fakeIcbcGateway;

    @MockBean
    private IcbcQualificationService qualificationService;

    @MockBean
    private IcbcGoodsConfigService goodsConfigService;

    @MockBean
    private SellerOnboardingService sellerOnboardingService;

    @BeforeEach
    public void setUp() {
        fakeIcbcGateway.resetAll();
    }

    // ==================== 开票有误：全额红冲、明细与蓝票一致 ====================

    @Test
    public void testApplyMisInvoicedBuildsDetailsFromBlueAndCallsGateway() {
        insertIssuedOrder();
        insertOrderItem(new BigDecimal("2"), new BigDecimal("500.00"), new BigDecimal("1000.00"));

        RedInvoiceApplyResultVO result = redInvoiceService.apply(buildApply("01", null));

        assertTrue(result.getSuccess());
        assertNotNull(result.getRedOffsetNo());
        assertEquals(RedOffsetStatusEnum.INITIAL.getStatus(), result.getRedOffsetStatus());
        assertEquals(1L, fakeIcbcGateway.countOperation(FakeIcbcGateway.OP_APPLY_RED_INVOICE));
        RedInvoiceReq req = fakeIcbcGateway.lastPayload(FakeIcbcGateway.OP_APPLY_RED_INVOICE);
        assertEquals("01", req.getRedOffsetReason());
        assertEquals("1000.00", req.getRedOffsetAmount());
        assertEquals("N", req.getIsRedo());
        assertEquals(1, req.getGoods().size());
        assertEquals("1000.00", req.getGoods().get(0).getGoodsAmt());
        assertEquals(0, new BigDecimal(req.getGoods().get(0).getGoodsNum()).compareTo(new BigDecimal("2")));
        // 红蓝一一对应：记录挂回蓝票
        RedInvoiceDO red = redInvoiceMapper.selectByRedOffsetNo(result.getRedOffsetNo());
        assertNotNull(red);
        assertEquals(PARTNER_ORDER_ID, red.getPartnerOrderId());
        assertEquals("01", red.getReason());
    }

    @Test
    public void testApplyMisInvoicedWithDifferentAmountRejected() {
        insertIssuedOrder();
        RedInvoiceApplyReqVO req = buildApply("01", new BigDecimal("900.00"));

        ServiceException e = assertThrows(ServiceException.class, () -> redInvoiceService.apply(req));
        assertEquals(RED_INVOICE_AMOUNT_MISMATCH.getCode(), e.getCode());
        assertEquals(0L, fakeIcbcGateway.countOperation(FakeIcbcGateway.OP_APPLY_RED_INVOICE));
    }

    @Test
    public void testApplyMisInvoicedWithMismatchedGoodsRejected() {
        insertIssuedOrder();
        insertOrderItem(new BigDecimal("2"), new BigDecimal("500.00"), new BigDecimal("1000.00"));
        RedInvoiceApplyReqVO req = buildApply("01", null);
        RedInvoiceApplyReqVO.Goods goods = new RedInvoiceApplyReqVO.Goods();
        goods.setGoodsSeqno("1");
        goods.setBlueGoodsSeqno("1");
        goods.setGoodsNum("2");
        goods.setGoodsAmt("900.00");
        req.setGoods(java.util.Collections.singletonList(goods));

        ServiceException e = assertThrows(ServiceException.class, () -> redInvoiceService.apply(req));
        assertEquals(RED_INVOICE_GOODS_MISMATCH.getCode(), e.getCode());
        assertEquals(0L, fakeIcbcGateway.countOperation(FakeIcbcGateway.OP_APPLY_RED_INVOICE));
    }

    @Test
    public void testApplyOnUnissuedBlueRejected() {
        // 蓝票未开出
        InvoiceOrderDO order = baseOrder();
        order.setInvoiceStatus(InvoiceIssueStatusEnum.NOT_ISSUED.getStatus());
        order.setInvoiceNo(null);
        invoiceOrderMapper.insert(order);

        ServiceException e = assertThrows(ServiceException.class,
                () -> redInvoiceService.apply(buildApply("01", null)));
        assertEquals(RED_INVOICE_BLUE_NOT_ISSUED.getCode(), e.getCode());
        assertEquals(0L, fakeIcbcGateway.countOperation(FakeIcbcGateway.OP_APPLY_RED_INVOICE));
    }

    @Test
    public void testApplyInvalidReasonRejected() {
        insertIssuedOrder();
        ServiceException e = assertThrows(ServiceException.class,
                () -> redInvoiceService.apply(buildApply("99", null)));
        assertEquals(RED_INVOICE_REASON_INVALID.getCode(), e.getCode());
    }

    @Test
    public void testApplySaleReturnedPartialAmountAllowed() {
        insertIssuedOrder();
        insertOrderItem(new BigDecimal("2"), new BigDecimal("500.00"), new BigDecimal("1000.00"));

        RedInvoiceApplyResultVO result = redInvoiceService.apply(buildApply("02", new BigDecimal("300.00")));

        assertTrue(result.getSuccess());
        assertEquals(new BigDecimal("300.00"), result.getAmount());
        // 部分红冲未给明细时，单行明细的金额等于红冲金额，合计不会超出
        RedInvoiceReq req = fakeIcbcGateway.lastPayload(FakeIcbcGateway.OP_APPLY_RED_INVOICE);
        assertEquals("300.00", req.getGoods().get(0).getGoodsAmt());
    }

    // ==================== 重复提交不重复下单 ====================

    @Test
    public void testApplyDuplicateDoesNotSubmitAgain() {
        insertIssuedOrder();
        redInvoiceService.apply(buildApply("01", null));

        RedInvoiceApplyResultVO second = redInvoiceService.apply(buildApply("01", null));

        assertTrue(second.getDuplicate());
        assertEquals(1L, fakeIcbcGateway.countOperation(FakeIcbcGateway.OP_APPLY_RED_INVOICE));
    }

    @Test
    public void testApplyAfterRevokedCanReapply() {
        insertIssuedOrder();
        RedInvoiceApplyResultVO first = redInvoiceService.apply(buildApply("01", null));
        // 撤销成功
        redInvoiceService.applyRedInvoiceInfo(first.getRedOffsetNo(),
                InvoiceInfo.builder().redOffsetStatus("10").build());

        RedInvoiceApplyResultVO second = redInvoiceService.apply(buildApply("01", null));

        assertFalse(second.getDuplicate());
        assertNotEquals(first.getRedOffsetNo(), second.getRedOffsetNo());
        assertEquals(2L, fakeIcbcGateway.countOperation(FakeIcbcGateway.OP_APPLY_RED_INVOICE));
    }

    // ==================== 撤销 ====================

    @Test
    public void testRevokeSuccess() {
        insertIssuedOrder();
        RedInvoiceApplyResultVO applied = redInvoiceService.apply(buildApply("01", null));

        RedInvoiceQueryRespVO revoked = redInvoiceService.revoke(buildRevoke(applied.getRedOffsetNo()));

        assertEquals(RedOffsetStatusEnum.REVOKED.getStatus(), revoked.getRedOffsetStatus());
        assertEquals("10", revoked.getRevokeStatus());
        assertEquals(1L, fakeIcbcGateway.countOperation(FakeIcbcGateway.OP_REVOKE_RED_INVOICE));
    }

    @Test
    public void testRevokeFailedVisibleWithNextAction() {
        insertIssuedOrder();
        RedInvoiceApplyResultVO applied = redInvoiceService.apply(buildApply("01", null));
        fakeIcbcGateway.setRedInvoiceRevokeResult(IcbcGatewayResult.success(
                RedInvoiceRevokeResult.builder().revokeStatus("11").build(), 0, "成功"));

        RedInvoiceQueryRespVO revoked = redInvoiceService.revoke(buildRevoke(applied.getRedOffsetNo()));

        assertEquals(RedOffsetStatusEnum.REVOKE_FAILED.getStatus(), revoked.getRedOffsetStatus());
        assertNotNull(revoked.getNextAction());
    }

    @Test
    public void testRevokeIssuedRedRejected() {
        insertIssuedOrder();
        RedInvoiceApplyResultVO applied = redInvoiceService.apply(buildApply("01", null));
        redInvoiceService.applyRedInvoiceInfo(applied.getRedOffsetNo(),
                InvoiceInfo.builder().redOffsetStatus("07").redOffsetInvoiceCode("RED-INV-1").build());

        ServiceException e = assertThrows(ServiceException.class,
                () -> redInvoiceService.revoke(buildRevoke(applied.getRedOffsetNo())));
        assertEquals(RED_INVOICE_NOT_REVOCABLE.getCode(), e.getCode());
    }

    // ==================== 通知收敛：重复 / 乱序 / 早到 ====================

    @Test
    public void testRedInfoForUnknownIsReplayable() {
        ServiceException e = assertThrows(ServiceException.class,
                () -> redInvoiceService.applyRedInvoiceInfo("NOT_EXISTS",
                        InvoiceInfo.builder().redOffsetStatus("01").build()));
        assertEquals(CALLBACK_BUSINESS_NOT_EXISTS.getCode(), e.getCode());
    }

    @Test
    public void testOutOfOrderRedUploadDoesNotRegress() {
        insertIssuedOrder();
        RedInvoiceApplyResultVO applied = redInvoiceService.apply(buildApply("01", null));

        redInvoiceService.applyRedInvoiceInfo(applied.getRedOffsetNo(),
                InvoiceInfo.builder().redOffsetStatus("07").redOffsetInvoiceCode("RED-INV-1").build());
        // 迟到的旧通知说还在上传中
        redInvoiceService.applyRedInvoiceInfo(applied.getRedOffsetNo(),
                InvoiceInfo.builder().redOffsetStatus("06").build());

        RedInvoiceDO red = redInvoiceMapper.selectByRedOffsetNo(applied.getRedOffsetNo());
        assertEquals(RedOffsetStatusEnum.SUCCESS.getStatus(), red.getRedOffsetStatus());
        assertEquals("RED-INV-1", red.getRedInvoiceNo());
    }

    @Test
    public void testDuplicateRedApplyNotifyIsIdempotent() {
        insertIssuedOrder();
        RedInvoiceApplyResultVO applied = redInvoiceService.apply(buildApply("01", null));

        InvoiceInfo info = InvoiceInfo.builder().redOffsetStatus("02").build();
        redInvoiceService.applyRedInvoiceInfo(applied.getRedOffsetNo(), info);
        redInvoiceService.applyRedInvoiceInfo(applied.getRedOffsetNo(), info);

        RedInvoiceDO red = redInvoiceMapper.selectByRedOffsetNo(applied.getRedOffsetNo());
        assertEquals(RedOffsetStatusEnum.APPLIED.getStatus(), red.getRedOffsetStatus());
    }

    @Test
    public void testRefreshConvergesFromGateway() {
        insertIssuedOrder();
        RedInvoiceApplyResultVO applied = redInvoiceService.apply(buildApply("01", null));
        fakeIcbcGateway.setInvoiceInfoResult(IcbcGatewayResult.success(InvoiceInfo.builder()
                .redOffsetStatus("07")
                .redOffsetInvoiceCode("RED-INV-9")
                .invoiceDate("2026-12-02 14:30:00")
                .build(), 0, "成功"));

        RedInvoiceQueryRespVO resp = redInvoiceService.refresh(applied.getRedOffsetNo());

        assertEquals(RedOffsetStatusEnum.SUCCESS.getStatus(), resp.getRedOffsetStatus());
        assertEquals("RED-INV-9", resp.getRedInvoiceNo());
        assertNotNull(resp.getRedInvoiceDate());
        assertEquals(1L, fakeIcbcGateway.countOperation(FakeIcbcGateway.OP_QUERY_INVOICE_INFO));
    }

    // ==================== 发票取消 ====================

    @Test
    public void testCancelPreInvoiceSuccess() {
        insertOrder(PreInvoiceStatusEnum.SUCCESS.getStatus(), 0, InvoiceIssueStatusEnum.NOT_ISSUED.getStatus());

        redInvoiceService.cancelPreInvoice(PARTNER_ORDER_ID);

        assertEquals(1L, fakeIcbcGateway.countOperation(FakeIcbcGateway.OP_CANCEL_INVOICE));
        InvoiceOrderDO order = invoiceOrderMapper.selectByPartnerOrderId(PARTNER_ORDER_ID);
        assertEquals(PreInvoiceStatusEnum.CANCELLED.getStatus(), order.getPreInvoiceStatus());
        assertEquals(9, order.getOrderStatus());
    }

    @Test
    public void testCancelPaidInvoiceRejected() {
        insertOrder(PreInvoiceStatusEnum.SUCCESS.getStatus(), 2, InvoiceIssueStatusEnum.NOT_ISSUED.getStatus());

        ServiceException e = assertThrows(ServiceException.class,
                () -> redInvoiceService.cancelPreInvoice(PARTNER_ORDER_ID));
        assertEquals(INVOICE_CANCEL_PAID.getCode(), e.getCode());
        assertEquals(0L, fakeIcbcGateway.countOperation(FakeIcbcGateway.OP_CANCEL_INVOICE));
    }

    @Test
    public void testCancelNotPreSuccessRejected() {
        insertOrder(PreInvoiceStatusEnum.IN_PROGRESS.getStatus(), 0, InvoiceIssueStatusEnum.NOT_ISSUED.getStatus());

        ServiceException e = assertThrows(ServiceException.class,
                () -> redInvoiceService.cancelPreInvoice(PARTNER_ORDER_ID));
        assertEquals(INVOICE_CANCEL_NOT_PRE_SUCCESS.getCode(), e.getCode());
    }

    @Test
    public void testCancelUnknownResultNotMarkedCancelled() {
        insertOrder(PreInvoiceStatusEnum.SUCCESS.getStatus(), 0, InvoiceIssueStatusEnum.NOT_ISSUED.getStatus());
        fakeIcbcGateway.setInvoiceCancelResult(IcbcGatewayResult.unknown(0, "超时"));

        assertThrows(ServiceException.class, () -> redInvoiceService.cancelPreInvoice(PARTNER_ORDER_ID));
        InvoiceOrderDO order = invoiceOrderMapper.selectByPartnerOrderId(PARTNER_ORDER_ID);
        assertEquals(PreInvoiceStatusEnum.SUCCESS.getStatus(), order.getPreInvoiceStatus());
    }

    // ==================== 造数据 ====================

    private RedInvoiceApplyReqVO buildApply(String reason, BigDecimal amount) {
        RedInvoiceApplyReqVO req = new RedInvoiceApplyReqVO();
        req.setPartnerOrderId(PARTNER_ORDER_ID);
        req.setReason(reason);
        req.setAmount(amount);
        return req;
    }

    private RedInvoiceRevokeReqVO buildRevoke(String redOffsetNo) {
        RedInvoiceRevokeReqVO req = new RedInvoiceRevokeReqVO();
        req.setRedOffsetNo(redOffsetNo);
        return req;
    }

    private InvoiceOrderDO baseOrder() {
        return InvoiceOrderDO.builder()
                .orderNo("INV_" + PARTNER_ORDER_ID)
                .partnerOrderId(PARTNER_ORDER_ID)
                .payeeNo("USER_1001")
                .payerNo("VENDOR_2001")
                .totalAmount(new BigDecimal("1000.00"))
                .invoiceType(1)
                .businessType("SCRAP")
                .orderStatus(3)
                .invoiceStatus(InvoiceIssueStatusEnum.ISSUED.getStatus())
                .invoiceNo("INV-1001")
                .invoiceAmount(new BigDecimal("1000.00"))
                .paymentStatus(2)
                .taxStatus(0)
                .confirmStatus(1)
                .preInvoiceStatus(PreInvoiceStatusEnum.SUCCESS.getStatus())
                .build();
    }

    private void insertIssuedOrder() {
        invoiceOrderMapper.insert(baseOrder());
    }

    private void insertOrder(int preInvoiceStatus, int paymentStatus, int invoiceStatus) {
        InvoiceOrderDO order = baseOrder();
        order.setPreInvoiceStatus(preInvoiceStatus);
        order.setPaymentStatus(paymentStatus);
        order.setInvoiceStatus(invoiceStatus);
        order.setInvoiceNo(null);
        order.setOrderStatus(0);
        invoiceOrderMapper.insert(order);
    }

    private void insertOrderItem(BigDecimal quantity, BigDecimal unitPrice, BigDecimal amount) {
        InvoiceOrderDO order = invoiceOrderMapper.selectByPartnerOrderId(PARTNER_ORDER_ID);
        orderItemMapper.insert(OrderItemDO.builder()
                .orderId(order.getId())
                .orderNo(order.getOrderNo())
                .itemName("废钢铁")
                .unit("吨")
                .quantity(quantity)
                .unitPrice(unitPrice)
                .amount(amount)
                .taxRate(new BigDecimal("0.01"))
                .taxAmount(new BigDecimal("9.90"))
                .build());
    }
}
