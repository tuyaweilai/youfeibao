package cn.iocoder.yudao.module.icbc.service.invoice;

import cn.iocoder.yudao.module.erp.api.stock.StockApi;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.icbc.UnitTestConfiguration;
import cn.iocoder.yudao.module.icbc.dal.dataobject.invoice.InvoiceOrderDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.invoice.InvoiceOrderMapper;
import cn.iocoder.yudao.module.icbc.dal.dataobject.callback.CallbackNotifyDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.callback.CallbackNotifyMapper;
import cn.iocoder.yudao.module.icbc.enums.CallbackProcessStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.TaxStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.UploadStatusEnum;
import cn.iocoder.yudao.module.icbc.gateway.IcbcGateway;
import cn.iocoder.yudao.module.icbc.service.callback.IcbcNotifyParser;
import cn.iocoder.yudao.module.icbc.service.callback.handler.InvoiceNotifyHandler;
import cn.iocoder.yudao.module.icbc.service.callback.handler.InvoiceUploadNotifyHandler;
import cn.iocoder.yudao.module.icbc.service.callback.handler.PreOrderExceptionNotifyHandler;
import cn.iocoder.yudao.module.icbc.service.callback.handler.TaxNotifyHandler;
import cn.iocoder.yudao.module.icbc.service.callback.impl.CallbackNotifyServiceImpl;
import cn.iocoder.yudao.module.icbc.service.goodscfg.IcbcGoodsConfigService;
import cn.iocoder.yudao.module.icbc.service.invoice.impl.InvoiceOrderServiceImpl;
import cn.iocoder.yudao.module.icbc.service.onboarding.SellerOnboardingService;
import cn.iocoder.yudao.module.icbc.service.qualification.IcbcQualificationService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 开票状态通知走唯一入口的端到端测试。
 *
 * <p>出售者确认后，工行推送 {@code notifyType=03} 通知。这条通知经「解析 → 落表 → 分发 →
 * 回写状态」后，本地订单的自然人确认与预开票状态收敛为「确认完成 / 预开票成功」。
 */
@Import({CallbackNotifyServiceImpl.class, IcbcNotifyParser.class,
        InvoiceNotifyHandler.class, PreOrderExceptionNotifyHandler.class,
        TaxNotifyHandler.class, InvoiceUploadNotifyHandler.class,
        InvoiceOrderServiceImpl.class, UnitTestConfiguration.class})
@Transactional
@Rollback
public class InvoiceNotifyHandlerTest extends BaseDbUnitTest {

    /** 库存域只通过 erp-api 的 StockApi 接入（#52）；单元测试不跨模块，用 Mock。 */
    @MockBean
    private StockApi stockApi;

    @Resource
    private CallbackNotifyServiceImpl callbackNotifyService;

    @Resource
    private InvoiceOrderMapper invoiceOrderMapper;

    @Resource
    private CallbackNotifyMapper callbackNotifyMapper;

    @MockBean
    private IcbcGateway icbcGateway;

    @MockBean
    private IcbcQualificationService qualificationService;

    @MockBean
    private IcbcGoodsConfigService goodsConfigService;

    @MockBean
    private SellerOnboardingService sellerOnboardingService;

    @Test
    public void testInvoiceNotifyConvergesConfirmAndPreInvoiceStatus() {
        insertOrder("ACQ_NOTIFY_1");

        String result = callbackNotifyService.receive(
                "{\"notifyType\":\"03\",\"outOrderId\":\"ACQ_NOTIFY_1\",\"confirmStatus\":\"01\",\"invoiceStatus\":\"02\"}");

        assertEquals("SUCCESS", result);
        InvoiceOrderDO order = invoiceOrderMapper.selectByPartnerOrderId("ACQ_NOTIFY_1");
        assertEquals(1, order.getConfirmStatus());
        assertEquals(2, order.getPreInvoiceStatus());
        assertEquals(1, order.getOrderStatus());
    }

    @Test
    public void testPreOrderExceptionNotifyConvergesFailure() {
        insertOrder("ACQ_NOTIFY_2");

        String result = callbackNotifyService.receive(
                "{\"notifyType\":\"01\",\"outOrderId\":\"ACQ_NOTIFY_2\",\"invoiceStatus\":\"03\"}");

        assertEquals("SUCCESS", result);
        InvoiceOrderDO order = invoiceOrderMapper.selectByPartnerOrderId("ACQ_NOTIFY_2");
        assertEquals(3, order.getPreInvoiceStatus());
    }

    @Test
    public void testNotifyBeforeOrderPersistedIsRecordedFailure() {
        // 通知先于平台订单落库：处理失败但记录仍在，数据落库后可重放
        String result = callbackNotifyService.receive(
                "{\"notifyType\":\"03\",\"outOrderId\":\"ACQ_NOTIFY_MISSING\",\"invoiceStatus\":\"02\"}");

        assertEquals("FAILURE", result);
        CallbackNotifyDO record = callbackNotifyMapper.selectList().stream()
                .filter(item -> "ACQ_NOTIFY_MISSING".equals(item.getBusinessId())).findFirst().orElse(null);
        assertNotNull(record);
        assertEquals(CallbackProcessStatusEnum.FAILURE.getStatus(), record.getProcessStatus());
    }

    @Test
    public void testTaxNotifyConvergesTaxStatusAndFields() {
        insertOrder("ACQ_NOTIFY_TAX");

        String result = callbackNotifyService.receive("{\"notifyType\":\"04\",\"outOrderId\":\"ACQ_NOTIFY_TAX\","
                + "\"taxStatus\":\"04\",\"taxRealAmount\":\"10.00\",\"tradeTime\":\"2026-12-01 10:35:00\","
                + "\"taxPaymentMethod\":\"1\"}");

        assertEquals("SUCCESS", result);
        InvoiceOrderDO order = invoiceOrderMapper.selectByPartnerOrderId("ACQ_NOTIFY_TAX");
        assertEquals(TaxStatusEnum.SUCCESS.getStatus(), order.getTaxStatus());
        assertEquals(new BigDecimal("10.00"), order.getTaxRealAmount());
        assertNotNull(order.getTaxTime());
        assertEquals("1", order.getTaxPaymentMethod());
    }

    @Test
    public void testUploadNotifyConvergesUploadStatus() {
        insertOrder("ACQ_NOTIFY_UPLOAD");

        String result = callbackNotifyService.receive(
                "{\"notifyType\":\"05\",\"outOrderId\":\"ACQ_NOTIFY_UPLOAD\",\"uploadStatus\":\"04\"}");

        assertEquals("SUCCESS", result);
        InvoiceOrderDO order = invoiceOrderMapper.selectByPartnerOrderId("ACQ_NOTIFY_UPLOAD");
        assertEquals(UploadStatusEnum.SUCCESS.getStatus(), order.getUploadStatus());
    }

    @Test
    public void testDuplicateTaxNotifyIsIdempotent() {
        insertOrder("ACQ_NOTIFY_TAX_DUP");
        String body = "{\"notifyType\":\"04\",\"notifyId\":\"TAX-DUP-1\",\"outOrderId\":\"ACQ_NOTIFY_TAX_DUP\","
                + "\"taxStatus\":\"04\",\"taxRealAmount\":\"10.00\"}";

        assertEquals("SUCCESS", callbackNotifyService.receive(body));
        assertEquals("SUCCESS", callbackNotifyService.receive(body));

        InvoiceOrderDO order = invoiceOrderMapper.selectByPartnerOrderId("ACQ_NOTIFY_TAX_DUP");
        assertEquals(TaxStatusEnum.SUCCESS.getStatus(), order.getTaxStatus());
        assertEquals(new BigDecimal("10.00"), order.getTaxRealAmount());
    }

    @Test
    public void testEarlyTaxNotifyCanBeReplayedAfterOrderPersisted() {
        String body = "{\"notifyType\":\"04\",\"outOrderId\":\"ACQ_NOTIFY_LATE\","
                + "\"taxStatus\":\"04\",\"taxRealAmount\":\"10.00\"}";
        // 通知早到：落失败
        assertEquals("FAILURE", callbackNotifyService.receive(body));
        CallbackNotifyDO record = callbackNotifyMapper.selectList().stream()
                .filter(item -> "ACQ_NOTIFY_LATE".equals(item.getBusinessId())).findFirst().orElse(null);
        assertNotNull(record);

        // 平台数据补落库后重放，状态收敛
        insertOrder("ACQ_NOTIFY_LATE");
        callbackNotifyService.replay(record.getId());

        InvoiceOrderDO order = invoiceOrderMapper.selectByPartnerOrderId("ACQ_NOTIFY_LATE");
        assertEquals(TaxStatusEnum.SUCCESS.getStatus(), order.getTaxStatus());
    }

    private void insertOrder(String partnerOrderId) {
        InvoiceOrderDO order = new InvoiceOrderDO();
        order.setOrderNo("INV_" + partnerOrderId);
        order.setPartnerOrderId(partnerOrderId);
        order.setPayeeNo("USER_1001");
        order.setPayerNo("VENDOR_2001");
        order.setTotalAmount(new BigDecimal("1000.00"));
        order.setInvoiceType(1);
        order.setBusinessType("SCRAP");
        order.setOrderStatus(0);
        order.setInvoiceStatus(0);
        order.setPaymentStatus(0);
        order.setTaxStatus(0);
        order.setConfirmStatus(0);
        order.setPreInvoiceStatus(1);
        invoiceOrderMapper.insert(order);
    }
}
