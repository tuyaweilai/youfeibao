package cn.iocoder.yudao.module.icbc.service.callback.handler;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.icbc.UnitTestConfiguration;
import cn.iocoder.yudao.module.icbc.dal.dataobject.invoice.InvoiceOrderDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.invoice.RedInvoiceDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.invoice.InvoiceOrderMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.invoice.RedInvoiceMapper;
import cn.iocoder.yudao.module.icbc.enums.CallbackNotifyTypeEnum;
import cn.iocoder.yudao.module.icbc.enums.InvoiceIssueStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.PreInvoiceStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.RedOffsetStatusEnum;
import cn.iocoder.yudao.module.icbc.service.callback.IcbcNotifyContext;
import cn.iocoder.yudao.module.icbc.service.goodscfg.IcbcGoodsConfigService;
import cn.iocoder.yudao.module.icbc.service.invoice.impl.InvoiceOrderServiceImpl;
import cn.iocoder.yudao.module.icbc.service.invoice.impl.RedInvoiceServiceImpl;
import cn.iocoder.yudao.module.icbc.service.onboarding.SellerOnboardingService;
import cn.iocoder.yudao.module.icbc.service.qualification.IcbcQualificationService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import javax.annotation.Resource;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 红冲 / 取消四类通知处理器的测试（#14）。
 *
 * <p>重点验证红冲通知按 {@code outRedOffsetId} 定位红冲记录——报文里同时带
 * 蓝票的 {@code outOrderId}，若按通用业务键取就会找错对象。
 */
@Import({UnitTestConfiguration.class, RedInvoiceServiceImpl.class, InvoiceOrderServiceImpl.class,
        RedApplyNotifyHandler.class, RedUploadNotifyHandler.class, RedRevokeNotifyHandler.class,
        InvoiceCancelNotifyHandler.class})
@TestPropertySource(properties = "icbc.gateway.mode=fake")
public class RedInvoiceNotifyHandlerTest extends BaseDbUnitTest {

    private static final String PARTNER_ORDER_ID = "ACQ_NOTIFY_1";
    private static final String RED_OFFSET_NO = "RED_NOTIFY_1";

    @Resource
    private RedApplyNotifyHandler redApplyNotifyHandler;
    @Resource
    private RedRevokeNotifyHandler redRevokeNotifyHandler;
    @Resource
    private InvoiceCancelNotifyHandler invoiceCancelNotifyHandler;

    @Resource
    private RedInvoiceMapper redInvoiceMapper;
    @Resource
    private InvoiceOrderMapper invoiceOrderMapper;

    @MockBean
    private IcbcQualificationService qualificationService;
    @MockBean
    private IcbcGoodsConfigService goodsConfigService;
    @MockBean
    private SellerOnboardingService sellerOnboardingService;

    @Test
    public void testRedApplyNotifyLocatesByOutRedOffsetId() {
        insertRedInvoice();
        // 报文同时带 outOrderId 与 outRedOffsetId，业务键取的是 outOrderId
        String payload = "{\"outOrderId\":\"" + PARTNER_ORDER_ID + "\",\"outRedOffsetId\":\"" + RED_OFFSET_NO
                + "\",\"redOffsetStatus\":\"02\"}";

        redApplyNotifyHandler.handle(IcbcNotifyContext.builder()
                .notifyType(CallbackNotifyTypeEnum.RED_APPLY)
                .businessId(PARTNER_ORDER_ID)
                .notifyData(payload)
                .build());

        RedInvoiceDO red = redInvoiceMapper.selectByRedOffsetNo(RED_OFFSET_NO);
        assertEquals(RedOffsetStatusEnum.APPLIED.getStatus(), red.getRedOffsetStatus());
        assertEquals("02", red.getRedOffsetStatusCode());
    }

    @Test
    public void testRedRevokeNotifyConvergesRevoked() {
        insertRedInvoice();
        String payload = "{\"outRedOffsetId\":\"" + RED_OFFSET_NO + "\",\"redOffsetStatus\":\"10\"}";

        redRevokeNotifyHandler.handle(IcbcNotifyContext.builder()
                .notifyType(CallbackNotifyTypeEnum.RED_REVOKE)
                .businessId(RED_OFFSET_NO)
                .notifyData(payload)
                .build());

        RedInvoiceDO red = redInvoiceMapper.selectByRedOffsetNo(RED_OFFSET_NO);
        assertEquals(RedOffsetStatusEnum.REVOKED.getStatus(), red.getRedOffsetStatus());
    }

    @Test
    public void testInvoiceCancelNotifyMarkOrderCancelled() {
        insertPreInvoiceSuccessOrder();
        String payload = "{\"outOrderId\":\"" + PARTNER_ORDER_ID + "\",\"invoiceStatus\":\"04\"}";

        invoiceCancelNotifyHandler.handle(IcbcNotifyContext.builder()
                .notifyType(CallbackNotifyTypeEnum.INVOICE_CANCEL)
                .businessId(PARTNER_ORDER_ID)
                .notifyData(payload)
                .build());

        InvoiceOrderDO order = invoiceOrderMapper.selectByPartnerOrderId(PARTNER_ORDER_ID);
        assertEquals(PreInvoiceStatusEnum.CANCELLED.getStatus(), order.getPreInvoiceStatus());
        assertEquals(9, order.getOrderStatus());
    }

    private void insertRedInvoice() {
        redInvoiceMapper.insert(RedInvoiceDO.builder()
                .redOffsetNo(RED_OFFSET_NO)
                .partnerOrderId(PARTNER_ORDER_ID)
                .reason("01")
                .amount(new BigDecimal("1000.00"))
                .redOffsetStatus(RedOffsetStatusEnum.INITIAL.getStatus())
                .build());
    }

    private void insertPreInvoiceSuccessOrder() {
        invoiceOrderMapper.insert(InvoiceOrderDO.builder()
                .orderNo("INV_" + PARTNER_ORDER_ID)
                .partnerOrderId(PARTNER_ORDER_ID)
                .payeeNo("USER_1001")
                .payerNo("VENDOR_2001")
                .totalAmount(new BigDecimal("1000.00"))
                .invoiceType(1)
                .businessType("SCRAP")
                .orderStatus(1)
                .invoiceStatus(InvoiceIssueStatusEnum.NOT_ISSUED.getStatus())
                .paymentStatus(0)
                .taxStatus(0)
                .confirmStatus(1)
                .preInvoiceStatus(PreInvoiceStatusEnum.SUCCESS.getStatus())
                .build());
    }
}
