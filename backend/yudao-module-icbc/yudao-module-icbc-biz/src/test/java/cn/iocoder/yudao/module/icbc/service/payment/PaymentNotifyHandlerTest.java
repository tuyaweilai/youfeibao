package cn.iocoder.yudao.module.icbc.service.payment;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.icbc.UnitTestConfiguration;
import cn.iocoder.yudao.module.icbc.dal.dataobject.callback.CallbackNotifyDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.invoice.InvoiceOrderDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payment.PaymentOrderDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.callback.CallbackNotifyMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.invoice.InvoiceOrderMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.payment.PaymentOrderMapper;
import cn.iocoder.yudao.module.icbc.enums.CallbackProcessStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.PaymentStatusEnum;
import cn.iocoder.yudao.module.icbc.service.acquisition.AcquisitionService;
import cn.iocoder.yudao.module.icbc.service.callback.IcbcNotifyParser;
import cn.iocoder.yudao.module.icbc.service.callback.handler.PaymentNotifyHandler;
import cn.iocoder.yudao.module.icbc.service.callback.impl.CallbackNotifyServiceImpl;
import cn.iocoder.yudao.module.icbc.service.esign.FrameworkAgreementEsignService;
import cn.iocoder.yudao.module.icbc.service.invoice.InvoiceOrderService;
import cn.iocoder.yudao.module.icbc.service.payment.impl.PaymentServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

/**
 * 支付状态通知走唯一入口的端到端测试。
 *
 * <p>回收企业在工行支付页面授权后，工行推送 {@code notifyType=02} 通知。这条通知经
 * 「解析 → 落表 → 分发 → 回写状态」后，本地支付单收敛为工行的 {@code payStatus}，
 * 支付成功时归档回单并把收购单推进为「已付款」。
 */
@Import({CallbackNotifyServiceImpl.class, IcbcNotifyParser.class,
        PaymentNotifyHandler.class, PaymentServiceImpl.class, UnitTestConfiguration.class})
@Transactional
@Rollback
public class PaymentNotifyHandlerTest extends BaseDbUnitTest {

    private static final String PARTNER_ORDER_ID = "ACQ_NOTIFY_PAY";

    @Resource
    private CallbackNotifyServiceImpl callbackNotifyService;

    @Resource
    private PaymentOrderMapper paymentOrderMapper;

    @Resource
    private InvoiceOrderMapper invoiceOrderMapper;

    @Resource
    private CallbackNotifyMapper callbackNotifyMapper;

    @MockBean
    private AcquisitionService acquisitionService;

    @MockBean
    private InvoiceOrderService invoiceOrderService;

    /** 出站电子签端口不在这里测；置空以免走真实实现（它需要平台租户配置） */
    @MockBean
    private FrameworkAgreementEsignService frameworkAgreementEsignService;

    @Test
    public void testPaymentNotifySuccessArchivesReceipt() {
        insertBusiness();

        String result = callbackNotifyService.receive(
                "{\"notifyType\":\"02\",\"outOrderId\":\"" + PARTNER_ORDER_ID + "\","
                        + "\"payStatus\":\"02\",\"payAmount\":\"1000.00\","
                        + "\"actuallyReceivedAmount\":\"1000.00\",\"serialNo\":\"SN-NOTIFY\"}");

        assertEquals("SUCCESS", result);
        PaymentOrderDO order = paymentOrderMapper.selectByPartnerOrderId(PARTNER_ORDER_ID);
        assertEquals(PaymentStatusEnum.SUCCESS.getStatus(), order.getPaymentStatus());
        assertEquals("SN-NOTIFY", order.getReceiptNo());
        assertNotNull(order.getReceiptTime());
        verify(acquisitionService).markPaidByInvoicePartnerOrderId(PARTNER_ORDER_ID);
    }

    @Test
    public void testPaymentNotifyPartialSuccessVisible() {
        insertBusiness();

        String result = callbackNotifyService.receive(
                "{\"notifyType\":\"02\",\"outOrderId\":\"" + PARTNER_ORDER_ID + "\","
                        + "\"payStatus\":\"25\",\"payAmount\":\"1000.00\","
                        + "\"actuallyReceivedAmount\":\"600.00\",\"serialNo\":\"SN-PART\"}");

        assertEquals("SUCCESS", result);
        PaymentOrderDO order = paymentOrderMapper.selectByPartnerOrderId(PARTNER_ORDER_ID);
        assertEquals(PaymentStatusEnum.PARTIAL_SUCCESS.getStatus(), order.getPaymentStatus());
        assertEquals(new BigDecimal("600.00"), order.getActuallyReceivedAmount());
    }

    @Test
    public void testNotifyBeforePaymentPersistedIsRecordedFailure() {
        // 通知先于平台支付单落库：处理失败但记录仍在，数据落库后可重放
        String result = callbackNotifyService.receive(
                "{\"notifyType\":\"02\",\"outOrderId\":\"ACQ_NOTIFY_MISSING\",\"payStatus\":\"02\"}");

        assertEquals("FAILURE", result);
        CallbackNotifyDO record = callbackNotifyMapper.selectList().stream()
                .filter(item -> "ACQ_NOTIFY_MISSING".equals(item.getBusinessId())).findFirst().orElse(null);
        assertNotNull(record);
        assertEquals(CallbackProcessStatusEnum.FAILURE.getStatus(), record.getProcessStatus());
    }

    private void insertBusiness() {
        InvoiceOrderDO invoiceOrder = new InvoiceOrderDO();
        invoiceOrder.setOrderNo("INV_" + PARTNER_ORDER_ID);
        invoiceOrder.setPartnerOrderId(PARTNER_ORDER_ID);
        invoiceOrder.setAcquisitionId(1024L);
        invoiceOrder.setPayeeNo("USER_1001");
        invoiceOrder.setPayerNo("VENDOR_2001");
        invoiceOrder.setTotalAmount(new BigDecimal("1000.00"));
        invoiceOrder.setInvoiceType(1);
        invoiceOrder.setBusinessType("SCRAP");
        invoiceOrder.setOrderStatus(1);
        invoiceOrder.setInvoiceStatus(0);
        invoiceOrder.setPaymentStatus(0);
        invoiceOrder.setTaxStatus(0);
        invoiceOrder.setConfirmStatus(1);
        invoiceOrder.setPreInvoiceStatus(2);
        invoiceOrderMapper.insert(invoiceOrder);

        PaymentOrderDO payment = PaymentOrderDO.builder()
                .orderNo("PAY_" + PARTNER_ORDER_ID)
                .partnerOrderId(PARTNER_ORDER_ID)
                .acquisitionId(1024L)
                .invoiceOrderId(invoiceOrder.getId())
                .payeeNo("USER_1001")
                .payerNo("VENDOR_2001")
                .paymentAmount(new BigDecimal("1000.00"))
                .paymentStatus(PaymentStatusEnum.PENDING.getStatus())
                .retryCount(0)
                .build();
        paymentOrderMapper.insert(payment);
    }

}
