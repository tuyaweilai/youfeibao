package cn.iocoder.yudao.module.icbc.service.invoice;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.icbc.UnitTestConfiguration;
import cn.iocoder.yudao.module.icbc.controller.admin.invoice.vo.InvoiceApplicationApplyReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.invoice.vo.InvoiceApplicationResultVO;
import cn.iocoder.yudao.module.icbc.controller.admin.invoice.vo.InvoicePreCheckItemVO;
import cn.iocoder.yudao.module.icbc.controller.admin.invoice.vo.InvoicePreCheckRespVO;
import cn.iocoder.yudao.module.icbc.controller.app.settlement.vo.SellerInvoiceConfirmItemVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.acquisition.IcbcAcquisitionDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.invoice.InvoiceOrderDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payer.PayerInfoDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.acquisition.IcbcAcquisitionMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.payer.PayerInfoMapper;
import cn.iocoder.yudao.module.icbc.enums.AcquisitionStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.InvoiceConfirmStageEnum;
import cn.iocoder.yudao.module.icbc.enums.PreInvoiceStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.PublicTokenPurposeEnum;
import cn.iocoder.yudao.module.icbc.controller.admin.publictoken.vo.PublicTokenRespVO;
import cn.iocoder.yudao.module.icbc.service.invoice.impl.AutoInvoiceApplicationServiceImpl;
import cn.iocoder.yudao.module.icbc.service.token.PublicTokenService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link AutoInvoiceApplicationServiceImpl} 的单元测试（#106，ADR 0039）。
 *
 * <p>要守住的三条：**幂等**（同一张收购单只下一次预下单）、**失败不连坐**（一张失败不拖累其他张，
 * 也不回滚结算确认）、**门禁与参数**（本企业开票参数不齐时不发起，而不是拿空值去求工行）。
 */
@Import({UnitTestConfiguration.class, AutoInvoiceApplicationServiceImpl.class})
@TestPropertySource(properties = {
        "icbc.invoice.jump-url-base=https://seller.example.com",
        "icbc.public-base-url=https://api.example.com/admin-api"
})
public class AutoInvoiceApplicationServiceImplTest extends BaseDbUnitTest {

    private static final Long SETTLEMENT_ID = 9001L;

    @Resource
    private AutoInvoiceApplicationService autoInvoiceApplicationService;

    @Resource
    private IcbcAcquisitionMapper acquisitionMapper;
    @Resource
    private PayerInfoMapper payerInfoMapper;

    @MockBean
    private InvoiceApplicationService invoiceApplicationService;
    @MockBean
    private InvoiceOrderService invoiceOrderService;
    @MockBean
    private PublicTokenService publicTokenService;

    // ==================== 自动发起 ====================

    @Test
    public void testApplyForSettlement_appliesEachAcquisitionOnce() {
        insertPayer("李四", "110101199001011234", "110000");
        IcbcAcquisitionDO first = insertAcquisition("ACQ_CHAIN_1", null);
        IcbcAcquisitionDO second = insertAcquisition("ACQ_CHAIN_2", "ORDER_CHAIN_2");
        when(invoiceApplicationService.apply(any())).thenReturn(success("ACQ_CHAIN_1"));

        autoInvoiceApplicationService.applyForSettlement(SETTLEMENT_ID);

        // 已有开票单的那张不再发起（幂等）：只对第一张下过单，且参数取自付方档案
        ArgumentCaptor<InvoiceApplicationApplyReqVO> captor = ArgumentCaptor.forClass(InvoiceApplicationApplyReqVO.class);
        verify(invoiceApplicationService, times(1)).apply(captor.capture());
        InvoiceApplicationApplyReqVO reqVO = captor.getValue();
        assertEquals(first.getId(), reqVO.getAcquisitionId());
        assertEquals("02", reqVO.getInvoiceType());
        assertEquals("110000", reqVO.getAreaCode());
        assertEquals("李四", reqVO.getDrawerName());
        assertEquals("110101199001011234", reqVO.getDrawerCardNumber());
        assertEquals("https://seller.example.com", reqVO.getJumpUrlBase());
        // second 有开票单：不该被再次发起
        assertTrue("ORDER_CHAIN_2".equals(second.getInvoicePartnerOrderId()));
    }

    @Test
    public void testApplyForSettlement_oneFailureDoesNotStopTheOthers() {
        insertPayer("李四", "110101199001011234", "110000");
        IcbcAcquisitionDO doomed = insertAcquisition("ACQ_FAIL", null);
        insertAcquisition("ACQ_OK", null);
        when(invoiceApplicationService.apply(any()))
                .thenThrow(new IllegalStateException("工行预下单炸了"))
                .thenReturn(success("ACQ_OK"));

        // 不抛出去：结算确认已经落库，不能因为一张单失败把它拖回来
        autoInvoiceApplicationService.applyForSettlement(SETTLEMENT_ID);

        verify(invoiceApplicationService, times(2)).apply(any());
        assertNull(doomed.getInvoicePartnerOrderId());
    }

    @Test
    public void testApplyForSettlement_skipsWhenInvoiceParamsMissing() {
        // 付方档案没有开票人 / 应税行为发生地：不发起，也不拿空值去求工行
        insertPayer(null, null, null);
        insertAcquisition("ACQ_NO_PARAM", null);

        autoInvoiceApplicationService.applyForSettlement(SETTLEMENT_ID);

        verify(invoiceApplicationService, never()).apply(any());
    }

    @Test
    public void testApplyForSettlement_skipsCancelledAcquisition() {
        insertPayer("李四", "110101199001011234", "110000");
        IcbcAcquisitionDO cancelled = insertAcquisition("ACQ_CANCELLED", null);
        cancelled.setCancelReason("现场登记有误");
        acquisitionMapper.updateById(cancelled);

        autoInvoiceApplicationService.applyForSettlement(SETTLEMENT_ID);

        verify(invoiceApplicationService, never()).apply(any());
    }

    // ==================== 自然人看到的步骤 ====================

    @Test
    public void testStatusForSettlement_blockedCarriesReasons() {
        insertAcquisition("ACQ_BLOCKED", null);
        when(invoiceApplicationService.preCheck(anyLong(), anyString())).thenReturn(preCheck(
                item("SELLER_QUOTA", "出售者额度", false, "已超 500 万", "办理经营主体登记")));

        List<SellerInvoiceConfirmItemVO> items = autoInvoiceApplicationService.statusForSettlement(SETTLEMENT_ID);

        assertEquals(1, items.size());
        SellerInvoiceConfirmItemVO item = items.get(0);
        assertEquals(InvoiceConfirmStageEnum.BLOCKED.getCode(), item.getStage());
        assertEquals(1, item.getFailures().size());
        assertEquals("已超 500 万", item.getFailures().get(0).getMessage());
        assertEquals(Boolean.FALSE, item.getConfirmPageAvailable());
    }

    @Test
    public void testStatusForSettlement_waitingConfirmCarriesPageUrl() {
        insertAcquisition("ACQ_WAIT", "ORDER_WAIT");
        when(invoiceOrderService.getOrderByPartnerOrderId("ORDER_WAIT")).thenReturn(order("ORDER_WAIT",
                PreInvoiceStatusEnum.IN_PROGRESS.getStatus(), 0, "<form id=\"pre-order\"></form>"));
        when(publicTokenService.mint(any())).thenReturn(token("tk"));

        List<SellerInvoiceConfirmItemVO> items = autoInvoiceApplicationService.statusForSettlement(SETTLEMENT_ID);

        SellerInvoiceConfirmItemVO item = items.get(0);
        assertEquals(InvoiceConfirmStageEnum.WAITING_CONFIRM.getCode(), item.getStage());
        assertEquals(Boolean.TRUE, item.getConfirmPageAvailable());
        assertEquals("https://api.example.com/admin-api/icbc/public/invoice/confirm-page?token=tk",
                item.getConfirmPageUrl());
        ArgumentCaptor<cn.iocoder.yudao.module.icbc.controller.admin.publictoken.vo.PublicTokenCreateReqVO> mintCaptor
                = ArgumentCaptor.forClass(cn.iocoder.yudao.module.icbc.controller.admin.publictoken.vo.PublicTokenCreateReqVO.class);
        verify(publicTokenService).mint(mintCaptor.capture());
        assertEquals(PublicTokenPurposeEnum.INVOICE_CONFIRM_PAGE.getCode(), mintCaptor.getValue().getPurpose());
        assertEquals("ORDER_WAIT", mintCaptor.getValue().getPartnerOrderId());
    }

    @Test
    public void testStatusForSettlement_confirmedWhenNaturalPersonDone() {
        insertAcquisition("ACQ_DONE", "ORDER_DONE");
        when(invoiceOrderService.getOrderByPartnerOrderId("ORDER_DONE")).thenReturn(order("ORDER_DONE",
                PreInvoiceStatusEnum.SUCCESS.getStatus(), 1, "<form id=\"pre-order\"></form>"));

        List<SellerInvoiceConfirmItemVO> items = autoInvoiceApplicationService.statusForSettlement(SETTLEMENT_ID);

        assertEquals(InvoiceConfirmStageEnum.CONFIRMED.getCode(), items.get(0).getStage());
        assertEquals(Boolean.FALSE, items.get(0).getConfirmPageAvailable());
    }

    @Test
    public void testStatusForSettlement_pageNotStored_saysWhy() {
        insertAcquisition("ACQ_NO_HTML", "ORDER_NO_HTML");
        when(invoiceOrderService.getOrderByPartnerOrderId("ORDER_NO_HTML")).thenReturn(order("ORDER_NO_HTML",
                PreInvoiceStatusEnum.IN_PROGRESS.getStatus(), 0, null));

        List<SellerInvoiceConfirmItemVO> items = autoInvoiceApplicationService.statusForSettlement(SETTLEMENT_ID);

        SellerInvoiceConfirmItemVO item = items.get(0);
        assertEquals(InvoiceConfirmStageEnum.WAITING_CONFIRM.getCode(), item.getStage());
        assertEquals(Boolean.FALSE, item.getConfirmPageAvailable());
        assertTrue(item.getMessage().contains("过期"));
    }

    // ==================== 确认页 ====================

    @Test
    public void testConfirmPageHtml_returnsStoredHtml() {
        when(invoiceOrderService.getOrderByPartnerOrderId("ORDER_WAIT")).thenReturn(order("ORDER_WAIT",
                PreInvoiceStatusEnum.IN_PROGRESS.getStatus(), 0, "<form id=\"pre-order\"></form>"));

        assertEquals("<form id=\"pre-order\"></form>",
                autoInvoiceApplicationService.confirmPageHtml("ORDER_WAIT"));
    }

    @Test
    public void testConfirmPageHtml_noOrder_returnsNull() {
        when(invoiceOrderService.getOrderByPartnerOrderId("ORDER_NONE")).thenReturn(null);

        assertNull(autoInvoiceApplicationService.confirmPageHtml("ORDER_NONE"));
        assertNull(autoInvoiceApplicationService.confirmPageHtml(" "));
    }

    // ==================== 造数 ====================

    private IcbcAcquisitionDO insertAcquisition(String no, String partnerOrderId) {
        IcbcAcquisitionDO acquisition = IcbcAcquisitionDO.builder()
                .acquisitionNo(no)
                .settlementId(SETTLEMENT_ID)
                .payeeId(1001L)
                .partnerPayeeId("PARTNER_1")
                .sellerName("张三")
                .sellerMobile("13800138000")
                .categoryName("废钢")
                .unit("吨")
                .amount(new BigDecimal("500.00"))
                .status(AcquisitionStatusEnum.REGISTERED.getStatus())
                .invoicePartnerOrderId(partnerOrderId)
                .build();
        acquisitionMapper.insert(acquisition);
        return acquisition;
    }

    private void insertPayer(String drawerName, String drawerCardNumber, String areaCode) {
        PayerInfoDO payer = new PayerInfoDO();
        payer.setPartnerPayerId("PAYER_1");
        payer.setName("甲回收");
        payer.setCreditCode("91110105MA01R2278M");
        payer.setTaxNo("91110105MA01R2278M");
        payer.setDrawerName(drawerName);
        payer.setDrawerCardNumber(drawerCardNumber);
        payer.setAreaCode(areaCode);
        payer.setStatus(1);
        payerInfoMapper.insert(payer);
    }

    private InvoiceOrderDO order(String partnerOrderId, Integer preInvoiceStatus, Integer confirmStatus,
                                 String confirmPageHtml) {
        return InvoiceOrderDO.builder()
                .partnerOrderId(partnerOrderId)
                .preInvoiceStatus(preInvoiceStatus)
                .confirmStatus(confirmStatus)
                .confirmPageHtml(confirmPageHtml)
                .build();
    }

    private PublicTokenRespVO token(String value) {
        PublicTokenRespVO resp = new PublicTokenRespVO();
        resp.setToken(value);
        resp.setBusinessKey("ORDER_WAIT");
        return resp;
    }

    private InvoiceApplicationResultVO success(String acquisitionNo) {
        InvoiceApplicationResultVO result = new InvoiceApplicationResultVO();
        result.setSuccess(true);
        result.setAcquisitionNo(acquisitionNo);
        return result;
    }

    private InvoicePreCheckRespVO preCheck(InvoicePreCheckItemVO... items) {
        InvoicePreCheckRespVO resp = new InvoicePreCheckRespVO();
        resp.setItems(List.of(items));
        return resp;
    }

    private InvoicePreCheckItemVO item(String code, String name, boolean passed, String message, String remedy) {
        return InvoicePreCheckItemVO.builder()
                .code(code).name(name).passed(passed).message(message).remedy(remedy).build();
    }

}
