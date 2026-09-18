package cn.iocoder.yudao.module.icbc.service.invoice;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.icbc.UnitTestConfiguration;
import cn.iocoder.yudao.module.icbc.controller.admin.invoice.vo.InvoiceApplicationApplyReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.invoice.vo.InvoiceApplicationBatchReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.invoice.vo.InvoiceApplicationResultVO;
import cn.iocoder.yudao.module.icbc.controller.admin.invoice.vo.InvoicePreCheckItemVO;
import cn.iocoder.yudao.module.icbc.controller.admin.invoice.vo.InvoicePreCheckRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.invoice.vo.InvoiceQueryReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.invoice.vo.InvoiceQueryRespVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.acquisition.IcbcAcquisitionDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.invoice.InvoiceOrderDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payee.PayeeInfoDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payer.PayerInfoDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.invoice.InvoiceOrderMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.payee.PayeeInfoMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.payer.PayerInfoMapper;
import cn.iocoder.yudao.module.icbc.enums.AcquisitionStatusEnum;
import cn.iocoder.yudao.module.icbc.gateway.fake.FakeIcbcGateway;
import cn.iocoder.yudao.module.icbc.gateway.model.InvoiceInfo;
import cn.iocoder.yudao.module.icbc.gateway.model.PreOrderReq;
import cn.iocoder.yudao.module.icbc.service.acquisition.AcquisitionService;
import cn.iocoder.yudao.module.icbc.service.goodscfg.IcbcGoodsConfigService;
import cn.iocoder.yudao.module.icbc.service.invoice.impl.InvoiceApplicationServiceImpl;
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
import java.util.Arrays;
import java.util.List;

import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.SELLER_ONBOARDING_NOT_READY;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * {@link InvoiceApplicationServiceImpl} 的单元测试。
 *
 * <p>覆盖 issue #8 的验收：发起前校验逐条给原因与补齐方式、单笔与批量发起、
 * 批量中一笔失败不影响其他笔、同一笔重复发起不产生第二笔业务、预查询能取回
 * 自然人确认状态与预开票状态。测试从平台服务进入，工行调用走 {@link FakeIcbcGateway}。
 */
@Import({UnitTestConfiguration.class, InvoiceApplicationServiceImpl.class, InvoiceOrderServiceImpl.class})
@TestPropertySource(properties = "icbc.gateway.mode=fake")
public class InvoiceApplicationServiceTest extends BaseDbUnitTest {

    @Resource
    private InvoiceApplicationService invoiceApplicationService;

    @Resource
    private InvoiceOrderService invoiceOrderService;

    @Resource
    private InvoiceOrderMapper invoiceOrderMapper;

    @Resource
    private PayeeInfoMapper payeeInfoMapper;

    @Resource
    private PayerInfoMapper payerInfoMapper;

    @Resource
    private FakeIcbcGateway fakeIcbcGateway;

    @MockBean
    private AcquisitionService acquisitionService;

    @MockBean
    private IcbcQualificationService qualificationService;

    @MockBean
    private IcbcGoodsConfigService goodsConfigService;

    @MockBean
    private SellerOnboardingService sellerOnboardingService;

    private Long payeeId;

    @BeforeEach
    public void setUp() {
        fakeIcbcGateway.reset();
        when(qualificationService.isTenantReady()).thenReturn(true);

        PayeeInfoDO payee = new PayeeInfoDO();
        payee.setName("张三");
        payee.setIdCardNo("110101199001011234");
        payee.setMobile("13800138000");
        payee.setAddress("北京市朝阳区");
        payee.setPartnerPayeeId("USER_1001");
        payeeInfoMapper.insert(payee);
        payeeId = payee.getId();

        PayerInfoDO payer = new PayerInfoDO();
        payer.setName("北京某某再生资源有限公司");
        payer.setTaxNo("91110000123456789X");
        payer.setPartnerPayerId("VENDOR_2001");
        payerInfoMapper.insert(payer);
    }

    // ==================== 发起前校验 ====================

    @Test
    public void testPreCheck_allPassed() {
        IcbcAcquisitionDO acquisition = stubAcquisition(100L, "ACQ100", "GENERAL", "1090101010000000000");
        when(acquisitionService.getAcquisition(100L)).thenReturn(acquisition);

        InvoicePreCheckRespVO resp = invoiceApplicationService.preCheck(100L, "02");

        assertTrue(resp.getAllPassed());
        assertEquals("ACQ100", resp.getAcquisitionNo());
        assertTrue(resp.getItems().stream().allMatch(item -> Boolean.TRUE.equals(item.getPassed())));
    }

    @Test
    public void testPreCheck_listsEveryFailureWithRemedy() {
        // 租户资质失效 + 简易计税 + 专票 + 品类无编码 + 缺少磅单
        when(qualificationService.isTenantReady()).thenReturn(false);
        IcbcAcquisitionDO acquisition = stubAcquisition(101L, "ACQ101", "SIMPLE", null);
        acquisition.setWeightTicketNo(null);
        when(acquisitionService.getAcquisition(101L)).thenReturn(acquisition);

        InvoicePreCheckRespVO resp = invoiceApplicationService.preCheck(101L, "01");

        assertFalse(resp.getAllPassed());
        List<InvoicePreCheckItemVO> failures = resp.getItems().stream()
                .filter(item -> !Boolean.TRUE.equals(item.getPassed())).toList();
        assertTrue(failures.size() >= 3);
        // 每一条不通过都同时给出「哪里不满足」与「怎么补齐」
        assertTrue(failures.stream().allMatch(item -> item.getMessage() != null && item.getRemedy() != null));

        assertTrue(failures.stream().anyMatch(item -> "TENANT_QUALIFICATION".equals(item.getCode())));
        assertTrue(failures.stream().anyMatch(item -> "TAX_METHOD_INVOICE_TYPE".equals(item.getCode())));
        assertTrue(failures.stream().anyMatch(item -> "GOODS_CODE_CONFIGURED".equals(item.getCode())));
        assertTrue(failures.stream().anyMatch(item -> "ACQUISITION_ELEMENTS".equals(item.getCode())));
    }

    @Test
    public void testPreCheck_sellerNotReadyReported() {
        doThrow(new ServiceException(SELLER_ONBOARDING_NOT_READY))
                .when(sellerOnboardingService).assertReadyForInvoice(payeeId);
        IcbcAcquisitionDO acquisition = stubAcquisition(102L, "ACQ102", "GENERAL", "1090101010000000000");
        when(acquisitionService.getAcquisition(102L)).thenReturn(acquisition);

        InvoicePreCheckRespVO resp = invoiceApplicationService.preCheck(102L, "02");

        assertFalse(resp.getAllPassed());
        InvoicePreCheckItemVO sellerItem = resp.getItems().stream()
                .filter(item -> "SELLER_AVAILABLE".equals(item.getCode())).findFirst().orElseThrow();
        assertFalse(sellerItem.getPassed());
        assertEquals(SELLER_ONBOARDING_NOT_READY.getMsg(), sellerItem.getMessage());
    }

    // ==================== 单笔发起 ====================

    @Test
    public void testApply_successReturnsConfirmationPageAndLinksAcquisition() {
        IcbcAcquisitionDO acquisition = stubAcquisition(100L, "ACQ100", "GENERAL", "1090101010000000000");
        when(acquisitionService.getAcquisition(100L)).thenReturn(acquisition);

        InvoiceApplicationResultVO result = invoiceApplicationService.apply(buildApply(100L, "02"));

        assertTrue(result.getSuccess());
        assertFalse(result.getDuplicate());
        assertEquals("ACQ100", result.getPartnerOrderId());
        assertNotNull(result.getConfirmPageHtml());
        assertTrue(result.getConfirmPageHtml().contains("pre-order"));
        // 收购单被挂回并推进，业务单号以收购单号为准（保证幂等）
        verify(acquisitionService).linkInvoice(100L, "ACQ100");
        // 工行报文使用公对私结算
        PreOrderReq submitted = fakeIcbcGateway.lastPayload(FakeIcbcGateway.OP_SUBMIT_PRE_ORDER);
        assertEquals("05", submitted.getPayChannel());
        assertEquals("04", submitted.getBuyerInvTypeCode());
        // 订单已落库并绑定收购单
        InvoiceOrderDO order = invoiceOrderMapper.selectByPartnerOrderId("ACQ100");
        assertNotNull(order);
        assertEquals(100L, order.getAcquisitionId());
    }

    @Test
    public void testApply_duplicateDoesNotCreateSecondBusiness() {
        IcbcAcquisitionDO acquisition = stubAcquisition(100L, "ACQ100", "GENERAL", "1090101010000000000");
        acquisition.setInvoicePartnerOrderId("ACQ100");
        when(acquisitionService.getAcquisition(100L)).thenReturn(acquisition);
        // 既有的订单
        InvoiceOrderDO existing = new InvoiceOrderDO();
        existing.setOrderNo("INV_EXIST");
        existing.setPartnerOrderId("ACQ100");
        existing.setPayeeNo("USER_1001");
        existing.setPayerNo("VENDOR_2001");
        existing.setTotalAmount(new BigDecimal("1000.00"));
        existing.setInvoiceType(1);
        existing.setBusinessType("SCRAP");
        existing.setOrderStatus(0);
        existing.setInvoiceStatus(0);
        existing.setPaymentStatus(0);
        existing.setTaxStatus(0);
        existing.setConfirmStatus(0);
        existing.setPreInvoiceStatus(1);
        invoiceOrderMapper.insert(existing);

        InvoiceApplicationResultVO result = invoiceApplicationService.apply(buildApply(100L, "02"));

        assertTrue(result.getSuccess());
        assertTrue(result.getDuplicate());
        assertEquals("INV_EXIST", result.getOrderNo());
        assertEquals(0L, fakeIcbcGateway.countOperation(FakeIcbcGateway.OP_SUBMIT_PRE_ORDER));
        verify(acquisitionService, never()).linkInvoice(anyLong(), anyString());
        assertEquals(1L, (long) invoiceOrderMapper.selectList().size());
    }

    @Test
    public void testApply_preCheckFailureDoesNotSubmit() {
        IcbcAcquisitionDO acquisition = stubAcquisition(103L, "ACQ103", "SIMPLE", null);
        when(acquisitionService.getAcquisition(103L)).thenReturn(acquisition);

        InvoiceApplicationResultVO result = invoiceApplicationService.apply(buildApply(103L, "01"));

        assertFalse(result.getSuccess());
        assertNotNull(result.getFailures());
        assertFalse(result.getFailures().isEmpty());
        assertEquals(0L, fakeIcbcGateway.countOperation(FakeIcbcGateway.OP_SUBMIT_PRE_ORDER));
        verify(acquisitionService, never()).linkInvoice(anyLong(), anyString());
    }

    // ==================== 批量发起 ====================

    @Test
    public void testApplyBatch_oneFailureDoesNotAffectOthers() {
        IcbcAcquisitionDO good = stubAcquisition(200L, "ACQ200", "GENERAL", "1090101010000000000");
        IcbcAcquisitionDO bad = stubAcquisition(201L, "ACQ201", "GENERAL", null);
        when(acquisitionService.getAcquisition(200L)).thenReturn(good);
        when(acquisitionService.getAcquisition(201L)).thenReturn(bad);

        InvoiceApplicationBatchReqVO reqVO = new InvoiceApplicationBatchReqVO();
        reqVO.setAcquisitionIds(Arrays.asList(200L, 201L));
        reqVO.setInvoiceType("02");
        reqVO.setAreaCode("110000");
        reqVO.setDrawerName("李四");
        reqVO.setDrawerCardNumber("110101199001011234");
        reqVO.setJumpUrlBase("https://platform.example.com");

        List<InvoiceApplicationResultVO> results = invoiceApplicationService.applyBatch(reqVO);

        assertEquals(2, results.size());
        InvoiceApplicationResultVO success = results.stream()
                .filter(r -> 200L == r.getAcquisitionId()).findFirst().orElseThrow();
        InvoiceApplicationResultVO failure = results.stream()
                .filter(r -> 201L == r.getAcquisitionId()).findFirst().orElseThrow();
        assertTrue(success.getSuccess());
        assertFalse(failure.getSuccess());
        assertNotNull(failure.getFailures());
        // 只有成功的那一笔下发了工行
        assertEquals(1L, fakeIcbcGateway.countOperation(FakeIcbcGateway.OP_SUBMIT_PRE_ORDER));
        verify(acquisitionService).linkInvoice(200L, "ACQ200");
        verify(acquisitionService, never()).linkInvoice(201L, "ACQ201");
    }

    // ==================== 预查询 ====================

    @Test
    public void testQuery_returnsConfirmAndPreInvoiceStatusAndPersists() {
        InvoiceOrderDO order = new InvoiceOrderDO();
        order.setOrderNo("INV_Q1");
        order.setPartnerOrderId("ACQ300");
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
        // 出售者已在工行页面确认，预开票成功
        fakeIcbcGateway.setInvoiceInfoResult(cn.iocoder.yudao.module.icbc.gateway.IcbcGatewayResult.success(
                InvoiceInfo.builder().confirmStatus("01").invoiceStatus("02").build(), 0, "成功"));

        InvoiceQueryReqVO reqVO = new InvoiceQueryReqVO();
        reqVO.setOutOrderId("ACQ300");
        InvoiceQueryRespVO resp = invoiceOrderService.queryInvoiceInfo(reqVO);

        assertEquals(1, resp.getConfirmStatus());
        assertEquals(2, resp.getPreInvoiceStatus());
        assertEquals(1, resp.getOrderStatus());
        // 已落库
        InvoiceOrderDO refreshed = invoiceOrderMapper.selectByPartnerOrderId("ACQ300");
        assertEquals(1, refreshed.getConfirmStatus());
        assertEquals(2, refreshed.getPreInvoiceStatus());
    }

    // ==================== 异步通知收敛 ====================

    @Test
    public void testNotifyConfirmationConvergesPreInvoiceStatus() {
        InvoiceOrderDO order = new InvoiceOrderDO();
        order.setOrderNo("INV_N1");
        order.setPartnerOrderId("ACQ400");
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

        // 出售者确认完成 + 预开票成功
        invoiceOrderService.applyPreInvoiceStatus("ACQ400", "01", "02");

        InvoiceOrderDO refreshed = invoiceOrderMapper.selectByPartnerOrderId("ACQ400");
        assertEquals(1, refreshed.getConfirmStatus());
        assertEquals(2, refreshed.getPreInvoiceStatus());
        assertEquals(1, refreshed.getOrderStatus());
    }

    @Test
    public void testNotifyForUnknownOrderIsReplayable() {
        // 通知先于平台数据到达：抛业务异常，通知落为失败，数据落库后可重放
        assertThrows(ServiceException.class,
                () -> invoiceOrderService.applyPreInvoiceStatus("NOT_EXISTS", "01", "02"));
    }

    // ==================== 造数据 ====================

    private InvoiceApplicationApplyReqVO buildApply(Long acquisitionId, String invoiceType) {
        InvoiceApplicationApplyReqVO reqVO = new InvoiceApplicationApplyReqVO();
        reqVO.setAcquisitionId(acquisitionId);
        reqVO.setInvoiceType(invoiceType);
        reqVO.setAreaCode("110000");
        reqVO.setDrawerName("李四");
        reqVO.setDrawerCardNumber("110101199001011234");
        reqVO.setJumpUrlBase("https://platform.example.com");
        return reqVO;
    }

    private IcbcAcquisitionDO stubAcquisition(Long id, String no, String taxMethod, String mergedCode) {
        return IcbcAcquisitionDO.builder()
                .id(id)
                .acquisitionNo(no)
                .payeeId(payeeId)
                .partnerPayeeId("USER_1001")
                .sellerName("张三")
                .sellerMobile("13800138000")
                .goodsConfigId(1L)
                .categoryName("废铁")
                .unit("吨")
                .taxRate(new BigDecimal("0.01"))
                .taxMethod(taxMethod)
                .mergedCode(mergedCode)
                .quantity(new BigDecimal("10"))
                .unitPrice(new BigDecimal("100"))
                .amount(new BigDecimal("1000.00"))
                .weightTicketNo("WT-" + no)
                .status(AcquisitionStatusEnum.REGISTERED.getStatus())
                .build();
    }
}
