package cn.iocoder.yudao.module.icbc.service.invoice;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.icbc.UnitTestConfiguration;
import cn.iocoder.yudao.module.icbc.controller.admin.invoice.vo.InvoiceQueryReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.invoice.vo.InvoiceQueryRespVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.invoice.InvoiceOrderDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.invoice.InvoiceOrderMapper;
import cn.iocoder.yudao.module.icbc.enums.InvoiceIssueStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.TaxStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.UploadStatusEnum;
import cn.iocoder.yudao.module.icbc.gateway.IcbcGatewayResult;
import cn.iocoder.yudao.module.icbc.gateway.fake.FakeIcbcGateway;
import cn.iocoder.yudao.module.icbc.gateway.model.InvoiceInfo;
import cn.iocoder.yudao.module.icbc.service.goodscfg.IcbcGoodsConfigService;
import cn.iocoder.yudao.module.icbc.service.invoice.impl.InvoiceOrderServiceImpl;
import cn.iocoder.yudao.module.icbc.service.onboarding.SellerOnboardingService;
import cn.iocoder.yudao.module.icbc.service.qualification.IcbcQualificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;

import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.CALLBACK_BUSINESS_NOT_EXISTS;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 开票 / 缴税 / 上传三条状态线的收敛测试（issue #10）。
 *
 * <p>覆盖验收：付款成功后自动触发开票、三类状态各自独立可见、通知与查询两侧一致、
 * 缴税成功后出凭证（凭证本身见 {@code InvoiceTaxCertificateServiceTest}）、失败 / 异常可见并给出下一步动作、
 * 通知乱序 / 重复 / 早到三种情况下状态最终一致。
 */
@Import({UnitTestConfiguration.class, InvoiceOrderServiceImpl.class})
@TestPropertySource(properties = "icbc.gateway.mode=fake")
public class InvoiceIssuanceServiceTest extends BaseDbUnitTest {

    private static final String PARTNER_ORDER_ID = "ACQ_ISSUE_1";

    @Resource
    private InvoiceOrderService invoiceOrderService;

    @Resource
    private InvoiceOrderMapper invoiceOrderMapper;

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
        fakeIcbcGateway.reset();
    }

    // ==================== 预查询：五条状态线一起收敛 ====================

    @Test
    public void testQueryConvergesInvoiceTaxAndUploadLines() {
        insertOrder(2, 2, 2, 0, null);
        fakeIcbcGateway.setInvoiceInfoResult(IcbcGatewayResult.success(InvoiceInfo.builder()
                .confirmStatus("01")
                .invoiceStatus("02")
                .invoiceCode("044001900111")
                .invoiceNo("044001900111")
                .invoiceDate("2026-12-01 10:30:00")
                .taxStatus("04")
                .taxAmount("10.00")
                .taxRealAmount("10.00")
                .tradeTime("2026-12-01 10:35:00")
                .taxPaymentMethod("1")
                .uploadStatus("04")
                .payAmount("1000.00")
                .levyItems(Collections.singletonList(InvoiceInfo.LevyItem.builder()
                        .levyItemName("增值税").voucherNum("2026120100001234").build()))
                .build(), 0, "成功"));

        InvoiceQueryRespVO resp = invoiceOrderService.queryInvoiceInfo(buildQuery(PARTNER_ORDER_ID));

        // 开票、缴税、上传三条线各自可见
        assertEquals(InvoiceIssueStatusEnum.ISSUED.getStatus(), resp.getInvoiceStatus());
        assertEquals("已开票", resp.getInvoiceStatusName());
        assertEquals(TaxStatusEnum.SUCCESS.getStatus(), resp.getTaxStatus());
        assertEquals("缴税成功", resp.getTaxStatusName());
        assertEquals(UploadStatusEnum.SUCCESS.getStatus(), resp.getUploadStatus());
        assertEquals("上传成功", resp.getUploadStatusName());
        // 发票与缴税字段落库
        assertEquals("044001900111", resp.getInvoiceNo());
        assertEquals(new BigDecimal("10.00"), resp.getTaxRealAmount());
        assertEquals("1", resp.getTaxPaymentMethod());
        assertEquals("企业委托扣缴", resp.getTaxPaymentMethodName());
        assertEquals("2026120100001234", resp.getTaxVoucherNo());
        assertNotNull(resp.getTaxTime());
        // 全部正常终态：订单已完成，且没有下一步动作
        assertEquals(4, resp.getOrderStatus());
        assertNull(resp.getNextAction());
    }

    @Test
    public void testApplyInvoiceInfoIsIdempotentOnDuplicateNotify() {
        insertOrder(2, 2, 2, null, null);

        InvoiceInfo info = InvoiceInfo.builder()
                .invoiceCode("INV-DUP").invoiceStatus("02").taxStatus("04").uploadStatus("04").build();
        invoiceOrderService.applyInvoiceInfo(PARTNER_ORDER_ID, info);
        invoiceOrderService.applyInvoiceInfo(PARTNER_ORDER_ID, info);

        InvoiceOrderDO order = invoiceOrderMapper.selectByPartnerOrderId(PARTNER_ORDER_ID);
        assertEquals(InvoiceIssueStatusEnum.ISSUED.getStatus(), order.getInvoiceStatus());
        assertEquals(TaxStatusEnum.SUCCESS.getStatus(), order.getTaxStatus());
        assertEquals(UploadStatusEnum.SUCCESS.getStatus(), order.getUploadStatus());
        assertEquals(4, order.getOrderStatus());
    }

    // ==================== 乱序：已结清的成功态不回退 ====================

    @Test
    public void testOutOfOrderTaxNotifyDoesNotRegress() {
        insertOrder(2, 2, 2, null, null);

        invoiceOrderService.applyInvoiceInfo(PARTNER_ORDER_ID,
                InvoiceInfo.builder().taxStatus("04").taxRealAmount("10.00").build());
        // 迟到的旧通知说还在缴税中
        invoiceOrderService.applyInvoiceInfo(PARTNER_ORDER_ID, InvoiceInfo.builder().taxStatus("02").build());

        InvoiceOrderDO order = invoiceOrderMapper.selectByPartnerOrderId(PARTNER_ORDER_ID);
        assertEquals(TaxStatusEnum.SUCCESS.getStatus(), order.getTaxStatus());
        assertEquals(new BigDecimal("10.00"), order.getTaxRealAmount());
    }

    @Test
    public void testOutOfOrderUploadNotifyDoesNotRegress() {
        insertOrder(2, 2, 2, null, null);

        invoiceOrderService.applyInvoiceInfo(PARTNER_ORDER_ID, InvoiceInfo.builder().uploadStatus("04").build());
        invoiceOrderService.applyInvoiceInfo(PARTNER_ORDER_ID, InvoiceInfo.builder().uploadStatus("02").build());

        InvoiceOrderDO order = invoiceOrderMapper.selectByPartnerOrderId(PARTNER_ORDER_ID);
        assertEquals(UploadStatusEnum.SUCCESS.getStatus(), order.getUploadStatus());
    }

    @Test
    public void testOutOfOrderInvoiceNotifyDoesNotRegress() {
        insertOrder(2, 2, 2, null, null);

        invoiceOrderService.applyInvoiceInfo(PARTNER_ORDER_ID,
                InvoiceInfo.builder().invoiceCode("INV-A").invoiceStatus("02").build());
        // 迟到的旧通知没有发票号码
        invoiceOrderService.applyInvoiceInfo(PARTNER_ORDER_ID, InvoiceInfo.builder().invoiceStatus("02").build());

        InvoiceOrderDO order = invoiceOrderMapper.selectByPartnerOrderId(PARTNER_ORDER_ID);
        assertEquals(InvoiceIssueStatusEnum.ISSUED.getStatus(), order.getInvoiceStatus());
        assertEquals("INV-A", order.getInvoiceNo());
    }

    @Test
    public void testApplyInvoiceInfoForUnknownOrderIsReplayable() {
        assertThrows(ServiceException.class, () -> invoiceOrderService.applyInvoiceInfo(
                "NOT_EXISTS", InvoiceInfo.builder().taxStatus("04").build()));
        // 错误码可识别，回调落失败后能重放
        try {
            invoiceOrderService.applyInvoiceInfo("NOT_EXISTS", InvoiceInfo.builder().taxStatus("04").build());
        } catch (ServiceException e) {
            assertEquals(CALLBACK_BUSINESS_NOT_EXISTS.getCode(), e.getCode());
        }
    }

    // ==================== 付款成功触发开票 ====================

    @Test
    public void testPaymentSuccessTriggersIssuingAndConfirmsWithIcbc() {
        insertOrder(2, 2, 2, null, null);
        fakeIcbcGateway.setInvoiceInfoResult(IcbcGatewayResult.success(
                InvoiceInfo.builder().invoiceCode("INV-AUTO").invoiceStatus("02").build(), 0, "成功"));

        invoiceOrderService.onPaymentSucceeded(PARTNER_ORDER_ID);

        // 付款成功即向工行确认了一次开票状态（自动触发）
        assertEquals(1L, fakeIcbcGateway.countOperation(FakeIcbcGateway.OP_QUERY_INVOICE_INFO));
        InvoiceOrderDO order = invoiceOrderMapper.selectByPartnerOrderId(PARTNER_ORDER_ID);
        assertEquals(InvoiceIssueStatusEnum.ISSUED.getStatus(), order.getInvoiceStatus());
        assertEquals("INV-AUTO", order.getInvoiceNo());
    }

    @Test
    public void testPaymentSuccessQueryFailureDoesNotThrowAndStaysIssuing() {
        insertOrder(2, 2, 2, null, null);
        fakeIcbcGateway.setInvoiceInfoResult(IcbcGatewayResult.businessFailed(40001, "查询失败"));

        assertDoesNotThrow(() -> invoiceOrderService.onPaymentSucceeded(PARTNER_ORDER_ID));

        InvoiceOrderDO order = invoiceOrderMapper.selectByPartnerOrderId(PARTNER_ORDER_ID);
        assertEquals(InvoiceIssueStatusEnum.ISSUING.getStatus(), order.getInvoiceStatus());
    }

    @Test
    public void testPaymentSuccessForUnknownOrderDoesNotThrow() {
        // 付款已收敛，不因开票数据缺失把付款拖回失败
        assertDoesNotThrow(() -> invoiceOrderService.onPaymentSucceeded("NOT_EXISTS"));
    }

    // ==================== 失败 / 异常可见并给出下一步 ====================

    @Test
    public void testUploadFailureIsVisibleWithNextAction() {
        insertOrder(2, 2, 2, null, null);

        invoiceOrderService.applyInvoiceInfo(PARTNER_ORDER_ID, InvoiceInfo.builder().uploadStatus("05").build());

        InvoiceQueryRespVO resp = invoiceOrderService.queryInvoiceInfo(buildQuery(PARTNER_ORDER_ID));
        assertEquals(UploadStatusEnum.FAILED.getStatus(), resp.getUploadStatus());
        assertEquals("上传失败", resp.getUploadStatusName());
        assertNotNull(resp.getNextAction());
        assertTrue(resp.getNextAction().contains("上传失败"));
    }

    @Test
    public void testTaxAmountMismatchIsVisibleWithNextAction() {
        insertOrder(2, 2, 2, null, null);

        invoiceOrderService.applyInvoiceInfo(PARTNER_ORDER_ID, InvoiceInfo.builder().taxStatus("97").build());

        InvoiceQueryRespVO resp = invoiceOrderService.queryInvoiceInfo(buildQuery(PARTNER_ORDER_ID));
        assertEquals(TaxStatusEnum.ABNORMAL_AMOUNT.getStatus(), resp.getTaxStatus());
        assertEquals("缴税异常：缴税金额不一致", resp.getTaxStatusName());
        assertTrue(TaxStatusEnum.isException(resp.getTaxStatus()));
        assertTrue(resp.getNextAction().contains("缴税金额"));
    }

    // ==================== 造数据 ====================

    private InvoiceQueryReqVO buildQuery(String partnerOrderId) {
        InvoiceQueryReqVO reqVO = new InvoiceQueryReqVO();
        reqVO.setOutOrderId(partnerOrderId);
        return reqVO;
    }

    private void insertOrder(int orderStatus, int preInvoiceStatus, int paymentStatus,
                             Integer invoiceStatus, Integer taxStatus) {
        InvoiceOrderDO order = InvoiceOrderDO.builder()
                .orderNo("INV_" + PARTNER_ORDER_ID)
                .partnerOrderId(PARTNER_ORDER_ID)
                .payeeNo("USER_1001")
                .payerNo("VENDOR_2001")
                .totalAmount(new BigDecimal("1000.00"))
                .invoiceType(1)
                .businessType("SCRAP")
                .orderStatus(orderStatus)
                .invoiceStatus(invoiceStatus == null ? 0 : invoiceStatus)
                .paymentStatus(paymentStatus)
                .taxStatus(taxStatus == null ? 0 : taxStatus)
                .confirmStatus(1)
                .preInvoiceStatus(preInvoiceStatus)
                .build();
        invoiceOrderMapper.insert(order);
    }
}
