package cn.iocoder.yudao.module.icbc.service.acquisition;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.icbc.UnitTestConfiguration;
import cn.iocoder.yudao.module.icbc.controller.admin.acquisition.vo.*;
import cn.iocoder.yudao.module.icbc.dal.dataobject.acquisition.IcbcAcquisitionDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.goodscfg.IcbcGoodsConfigDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.invoice.InvoiceOrderDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payee.PayeeInfoDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.acquisition.IcbcAcquisitionMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.goodscfg.IcbcGoodsConfigMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.invoice.InvoiceOrderMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.payee.PayeeInfoMapper;
import cn.iocoder.yudao.module.icbc.enums.AcquisitionStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.InvoiceIssueStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.PreInvoiceStatusEnum;
import cn.iocoder.yudao.module.icbc.gateway.IcbcGateway;
import cn.iocoder.yudao.module.icbc.service.acquisition.impl.AcquisitionServiceImpl;
import cn.iocoder.yudao.module.icbc.service.acquisition.recognition.AcquisitionRecognitionPort;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * {@link AcquisitionServiceImpl} 的单元测试。
 *
 * <p>从 Service 接口进去、用真实 Mapper 读真库，断言的是外部可观察行为：落库的收购单、
 * 缺要件时的提示、离线补传的去重结果、车牌比对结论、状态推进。现场识别走端口 mock。
 */
@Import({AcquisitionServiceImpl.class, UnitTestConfiguration.class})
@Sql(scripts = "/sql/create_tables.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Transactional
public class AcquisitionServiceImplTest extends BaseDbUnitTest {

    @Resource
    private AcquisitionService acquisitionService;
    @Resource
    private IcbcAcquisitionMapper acquisitionMapper;
    @Resource
    private PayeeInfoMapper payeeInfoMapper;
    @Resource
    private IcbcGoodsConfigMapper goodsConfigMapper;
    @Resource
    private InvoiceOrderMapper invoiceOrderMapper;

    @MockBean
    private AcquisitionRecognitionPort recognitionPort;
    @MockBean
    private IcbcGateway icbcGateway;

    // ==================== 登记与按品类带出 ====================

    @Test
    public void testCreateAcquisition_autoPopulatesFromCategory() {
        PayeeInfoDO payee = insertPayee("张三", "13800138000");
        IcbcGoodsConfigDO config = insertGoodsConfig("废钢", "吨", "0.01", "SIMPLE");

        AcquisitionCreateReqVO reqVO = baseReq(payee.getId(), config.getId());
        reqVO.setQuantity(new BigDecimal("10"));
        reqVO.setUnitPrice(new BigDecimal("100.00"));

        Long id = acquisitionService.createAcquisition(reqVO).getId();

        IcbcAcquisitionDO saved = acquisitionMapper.selectById(id);
        assertNotNull(saved);
        assertNotNull(saved.getAcquisitionNo());
        // 计量单位、税率、税收分类编码按品类自动带出
        assertEquals("吨", saved.getUnit());
        assertEquals(0, new BigDecimal("0.01").compareTo(saved.getTaxRate()));
        assertEquals("SIMPLE", saved.getTaxMethod());
        assertEquals("1090101010000000000", saved.getMergedCode());
        assertEquals("废钢", saved.getCategoryName());
        // 金额按数量 × 单价推算
        assertEquals(0, new BigDecimal("1000.00").compareTo(saved.getAmount()));
        // 出售者快照
        assertEquals("张三", saved.getSellerName());
        assertEquals("13800138000", saved.getSellerMobile());
        // 初始状态
        assertEquals(AcquisitionStatusEnum.REGISTERED.getStatus(), saved.getStatus());
        assertEquals("ONLINE", saved.getSource());
    }

    @Test
    public void testCreateAcquisition_computesNetWeight() {
        PayeeInfoDO payee = insertPayee("李四", "13800138001");
        IcbcGoodsConfigDO config = insertGoodsConfig("废纸", "吨", "0.01", "GENERAL");

        AcquisitionCreateReqVO reqVO = baseReq(payee.getId(), config.getId());
        reqVO.setQuantity(new BigDecimal("5"));
        reqVO.setAmount(new BigDecimal("500.00"));
        reqVO.setGrossWeight(new BigDecimal("18000.00"));
        reqVO.setTareWeight(new BigDecimal("5500.00"));

        Long id = acquisitionService.createAcquisition(reqVO).getId();

        assertEquals(0, new BigDecimal("12500.00").compareTo(acquisitionMapper.selectById(id).getNetWeight()));
    }

    @Test
    public void testCreateAcquisition_netWeightSmallerThanTareRejected() {
        PayeeInfoDO payee = insertPayee("王五", "13800138002");
        IcbcGoodsConfigDO config = insertGoodsConfig("废铁", "吨", "0.01", "GENERAL");

        AcquisitionCreateReqVO reqVO = baseReq(payee.getId(), config.getId());
        reqVO.setQuantity(new BigDecimal("5"));
        reqVO.setAmount(new BigDecimal("500.00"));
        reqVO.setGrossWeight(new BigDecimal("100"));
        reqVO.setTareWeight(new BigDecimal("200"));

        assertServiceException(() -> acquisitionService.createAcquisition(reqVO), ACQUISITION_WEIGHT_INVALID);
    }

    // ==================== 必须要件 ====================

    @Test
    public void testCreateAcquisition_missingElementsListedIndividually() {
        // 空请求：出售者、品类、数量、金额、磅单全缺
        AcquisitionCreateReqVO reqVO = new AcquisitionCreateReqVO();

        assertServiceException(() -> acquisitionService.createAcquisition(reqVO),
                ACQUISITION_REQUIRED_ELEMENT_MISSING, "出售者、品类、数量、金额、磅单");
    }

    @Test
    public void testCreateAcquisition_missingOnlyWeightTicket() {
        PayeeInfoDO payee = insertPayee("赵六", "13800138003");
        IcbcGoodsConfigDO config = insertGoodsConfig("废钢", "吨", "0.01", "GENERAL");

        AcquisitionCreateReqVO reqVO = baseReq(payee.getId(), config.getId());
        reqVO.setQuantity(new BigDecimal("5"));
        reqVO.setAmount(new BigDecimal("500.00"));
        reqVO.setWeightTicketNo(null);
        reqVO.setWeightTicketImageUrl(null);

        assertServiceException(() -> acquisitionService.createAcquisition(reqVO),
                ACQUISITION_REQUIRED_ELEMENT_MISSING, "磅单");
    }

    @Test
    public void testCreateAcquisition_sellerNotFound() {
        IcbcGoodsConfigDO config = insertGoodsConfig("废钢", "吨", "0.01", "GENERAL");
        AcquisitionCreateReqVO reqVO = baseReq(99999L, config.getId());
        reqVO.setQuantity(new BigDecimal("5"));
        reqVO.setAmount(new BigDecimal("500.00"));

        assertServiceException(() -> acquisitionService.createAcquisition(reqVO), ACQUISITION_SELLER_NOT_EXISTS);
    }

    @Test
    public void testCreateAcquisition_goodsConfigNotFound() {
        PayeeInfoDO payee = insertPayee("孙七", "13800138004");
        AcquisitionCreateReqVO reqVO = baseReq(payee.getId(), 88888L);
        reqVO.setQuantity(new BigDecimal("5"));
        reqVO.setAmount(new BigDecimal("500.00"));

        assertServiceException(() -> acquisitionService.createAcquisition(reqVO), ACQUISITION_GOODS_CONFIG_NOT_EXISTS);
    }

    // ==================== 幂等与离线补传 ====================

    @Test
    public void testCreateAcquisition_idempotentByClientRequestId() {
        PayeeInfoDO payee = insertPayee("周八", "13800138005");
        IcbcGoodsConfigDO config = insertGoodsConfig("废钢", "吨", "0.01", "GENERAL");

        AcquisitionCreateReqVO reqVO = baseReq(payee.getId(), config.getId());
        reqVO.setClientRequestId("CLIENT_REQ_1");
        reqVO.setQuantity(new BigDecimal("5"));
        reqVO.setAmount(new BigDecimal("500.00"));

        Long first = acquisitionService.createAcquisition(reqVO).getId();
        Long second = acquisitionService.createAcquisition(reqVO).getId();

        assertEquals(first, second);
        assertEquals(1, acquisitionMapper.selectList().size());
    }

    @Test
    public void testSyncOffline_dedupesAndIsolatesFailures() {
        PayeeInfoDO payee = insertPayee("吴九", "13800138006");
        IcbcGoodsConfigDO config = insertGoodsConfig("废钢", "吨", "0.01", "GENERAL");

        // 先在线登记一笔
        AcquisitionCreateReqVO online = baseReq(payee.getId(), config.getId());
        online.setClientRequestId("OFFLINE_DUP");
        online.setQuantity(new BigDecimal("5"));
        online.setAmount(new BigDecimal("500.00"));
        Long onlineId = acquisitionService.createAcquisition(online).getId();

        // 补传三条：一条与在线重复、一条新、一条缺品类
        AcquisitionCreateReqVO duplicate = baseReq(payee.getId(), config.getId());
        duplicate.setClientRequestId("OFFLINE_DUP");
        duplicate.setQuantity(new BigDecimal("5"));
        duplicate.setAmount(new BigDecimal("500.00"));

        AcquisitionCreateReqVO fresh = baseReq(payee.getId(), config.getId());
        fresh.setClientRequestId("OFFLINE_NEW");
        fresh.setQuantity(new BigDecimal("8"));
        fresh.setAmount(new BigDecimal("800.00"));
        fresh.setSource("OFFLINE_SYNC");

        AcquisitionCreateReqVO bad = baseReq(payee.getId(), null);
        bad.setClientRequestId("OFFLINE_BAD");
        bad.setQuantity(new BigDecimal("1"));
        bad.setAmount(new BigDecimal("1.00"));

        AcquisitionOfflineSyncReqVO syncReq = new AcquisitionOfflineSyncReqVO();
        syncReq.setItems(List.of(duplicate, fresh, bad));

        List<AcquisitionSyncResultVO> results = acquisitionService.syncOffline(syncReq);

        assertEquals(3, results.size());
        // 第 1 条：命中去重，返回既有单据
        assertTrue(results.get(0).getSuccess());
        assertTrue(results.get(0).getDuplicated());
        assertEquals(onlineId, results.get(0).getId());
        // 第 2 条：新登记成功
        assertTrue(results.get(1).getSuccess());
        assertFalse(results.get(1).getDuplicated());
        assertNotNull(results.get(1).getAcquisitionNo());
        assertEquals("OFFLINE_SYNC", acquisitionMapper.selectById(results.get(1).getId()).getSource());
        // 第 3 条：失败但不影响其他条
        assertFalse(results.get(2).getSuccess());
        assertTrue(results.get(2).getErrorMsg().contains("品类"));
        // 总共两条收购单
        assertEquals(2, acquisitionMapper.selectList().size());
    }

    // ==================== 车牌比对与识别修正 ====================

    @Test
    public void testCreateAcquisition_plateComparison() {
        PayeeInfoDO payee = insertPayee("郑十", "13800138007");
        IcbcGoodsConfigDO config = insertGoodsConfig("废钢", "吨", "0.01", "GENERAL");

        // 一致（含空格差异）
        AcquisitionCreateReqVO matched = baseReq(payee.getId(), config.getId());
        matched.setQuantity(new BigDecimal("1"));
        matched.setAmount(new BigDecimal("1.00"));
        matched.setWeightTicketPlateNo("京A12345");
        matched.setVehiclePlateNo("京a12345 ");
        assertEquals(Boolean.TRUE,
                acquisitionMapper.selectById(acquisitionService.createAcquisition(matched).getId()).getPlateMatched());

        // 不一致
        AcquisitionCreateReqVO mismatched = baseReq(payee.getId(), config.getId());
        mismatched.setQuantity(new BigDecimal("1"));
        mismatched.setAmount(new BigDecimal("1.00"));
        mismatched.setWeightTicketPlateNo("京A12345");
        mismatched.setVehiclePlateNo("京A99999");
        assertEquals(Boolean.FALSE,
                acquisitionMapper.selectById(acquisitionService.createAcquisition(mismatched).getId()).getPlateMatched());

        // 只有一侧：无法比对
        AcquisitionCreateReqVO single = baseReq(payee.getId(), config.getId());
        single.setQuantity(new BigDecimal("1"));
        single.setAmount(new BigDecimal("1.00"));
        single.setWeightTicketPlateNo("京A12345");
        assertNull(acquisitionMapper.selectById(acquisitionService.createAcquisition(single).getId()).getPlateMatched());
    }

    @Test
    public void testCreateAcquisition_recognitionFillsBlanksButHumanValueWins() {
        PayeeInfoDO payee = insertPayee("钱十一", "13800138008");
        IcbcGoodsConfigDO config = insertGoodsConfig("废钢", "吨", "0.01", "GENERAL");

        when(recognitionPort.recognizeWeightTicket("https://cdn/w.jpg")).thenReturn(
                AcquisitionRecognitionPort.WeightTicketRecognition.builder()
                        .weightTicketNo("WD_RECOG")
                        .grossWeight(new BigDecimal("18000"))
                        .tareWeight(new BigDecimal("5500"))
                        .plateNo("京B00001")
                        .build());
        when(recognitionPort.recognizePlate("https://cdn/front.jpg")).thenReturn(
                AcquisitionRecognitionPort.PlateRecognition.builder().plateNo("京B00001").build());

        AcquisitionCreateReqVO reqVO = baseReq(payee.getId(), config.getId());
        reqVO.setQuantity(new BigDecimal("5"));
        reqVO.setAmount(new BigDecimal("500.00"));
        reqVO.setWeightTicketNo(null);
        reqVO.setWeightTicketImageUrl("https://cdn/w.jpg");
        reqVO.setVehicleFrontImageUrl("https://cdn/front.jpg");
        // 人工已经填了毛重，识别不得覆盖
        reqVO.setGrossWeight(new BigDecimal("19000"));

        Long id = acquisitionService.createAcquisition(reqVO).getId();

        IcbcAcquisitionDO saved = acquisitionMapper.selectById(id);
        // 识别回填
        assertEquals("WD_RECOG", saved.getWeightTicketNo());
        assertEquals(0, new BigDecimal("5500").compareTo(saved.getTareWeight()));
        assertEquals("京B00001", saved.getVehiclePlateNo());
        assertEquals("京B00001", saved.getWeightTicketPlateNo());
        // 人工值优先
        assertEquals(0, new BigDecimal("19000").compareTo(saved.getGrossWeight()));
        // 净重按人工毛重与识别皮重算出
        assertEquals(0, new BigDecimal("13500").compareTo(saved.getNetWeight()));
    }

    @Test
    public void testCorrectRecognition_recomputesPlateAndNetWeight() {
        PayeeInfoDO payee = insertPayee("冯十二", "13800138009");
        IcbcGoodsConfigDO config = insertGoodsConfig("废钢", "吨", "0.01", "GENERAL");

        AcquisitionCreateReqVO reqVO = baseReq(payee.getId(), config.getId());
        reqVO.setQuantity(new BigDecimal("5"));
        reqVO.setAmount(new BigDecimal("500.00"));
        reqVO.setWeightTicketPlateNo("京A11111");
        reqVO.setVehiclePlateNo("京A22222");
        reqVO.setGrossWeight(new BigDecimal("18000"));
        reqVO.setTareWeight(new BigDecimal("5500"));
        Long id = acquisitionService.createAcquisition(reqVO).getId();
        assertEquals(Boolean.FALSE, acquisitionMapper.selectById(id).getPlateMatched());

        AcquisitionCorrectionReqVO correction = new AcquisitionCorrectionReqVO();
        correction.setId(id);
        correction.setVehiclePlateNo("京A11111");
        correction.setGrossWeight(new BigDecimal("17000"));
        correction.setRemark("车牌识别错位，已按照片改正");
        acquisitionService.correctRecognition(correction);

        IcbcAcquisitionDO saved = acquisitionMapper.selectById(id);
        assertEquals(Boolean.TRUE, saved.getPlateMatched());
        assertEquals(0, new BigDecimal("11500").compareTo(saved.getNetWeight()));
        assertEquals("车牌识别错位，已按照片改正", saved.getRemark());
    }

    // ==================== 状态推进 ====================

    @Test
    public void testLinkInvoiceAndStatusTransitions() {
        PayeeInfoDO payee = insertPayee("陈十三", "13800138010");
        IcbcGoodsConfigDO config = insertGoodsConfig("废钢", "吨", "0.01", "GENERAL");

        AcquisitionCreateReqVO reqVO = baseReq(payee.getId(), config.getId());
        reqVO.setQuantity(new BigDecimal("5"));
        reqVO.setAmount(new BigDecimal("500.00"));
        Long id = acquisitionService.createAcquisition(reqVO).getId();

        acquisitionService.linkInvoice(id, "ORDER_LINK_1");
        IcbcAcquisitionDO linked = acquisitionMapper.selectById(id);
        assertEquals(AcquisitionStatusEnum.PENDING_PAYMENT.getStatus(), linked.getStatus());
        assertEquals("ORDER_LINK_1", linked.getInvoicePartnerOrderId());

        acquisitionService.markPaidByInvoicePartnerOrderId("ORDER_LINK_1");
        assertEquals(AcquisitionStatusEnum.PAID.getStatus(),
                acquisitionMapper.selectById(id).getStatus());

        acquisitionService.markInvoicedByInvoicePartnerOrderId("ORDER_LINK_1");
        assertEquals(AcquisitionStatusEnum.INVOICED.getStatus(),
                acquisitionMapper.selectById(id).getStatus());
    }

    @Test
    public void testGetAcquisition_notExists() {
        assertServiceException(() -> acquisitionService.getAcquisition(12345L), ACQUISITION_NOT_EXISTS);
    }

    // ==================== 额度余量提示（#12） ====================

    @Test
    public void testCreateAcquisition_withinQuotaTellsRemaining() {
        PayeeInfoDO payee = insertPayee("卫十五", "13800138012");
        IcbcGoodsConfigDO config = insertGoodsConfig("废钢", "吨", "0.01", "GENERAL");
        // 该出售者此前已开出 10 万元（跨租户累计的口径，这里同一个租户）
        insertIssuedOrder(payee, new BigDecimal("100000.00"));

        AcquisitionCreateReqVO reqVO = baseReq(payee.getId(), config.getId());
        reqVO.setQuantity(new BigDecimal("1"));
        reqVO.setAmount(new BigDecimal("5000.00"));

        AcquisitionCreateRespVO resp = acquisitionService.createAcquisition(reqVO);

        assertNotNull(resp.getId());
        assertEquals(0, new BigDecimal("100000.00").compareTo(resp.getQuotaUsedAmount()));
        assertEquals(0, new BigDecimal("4900000.00").compareTo(resp.getQuotaRemainingAmount()));
        assertEquals(Boolean.TRUE, resp.getQuotaPassed());
        assertTrue(resp.getQuotaMessage().contains("余量"));
    }

    @Test
    public void testCreateAcquisition_overCapIsHintedButNotBlocked() {
        PayeeInfoDO payee = insertPayee("蒋十六", "13800138013");
        IcbcGoodsConfigDO config = insertGoodsConfig("废钢", "吨", "0.01", "GENERAL");
        // 已用 4,999,000 元，只剩 1000 元额度
        insertIssuedOrder(payee, new BigDecimal("4999000.00"));

        AcquisitionCreateReqVO reqVO = baseReq(payee.getId(), config.getId());
        reqVO.setQuantity(new BigDecimal("1"));
        reqVO.setAmount(new BigDecimal("5000.00"));

        AcquisitionCreateRespVO resp = acquisitionService.createAcquisition(reqVO);

        // 登记不拦（硬校验在开票申请），但必须当场告诉收货员：这一笔开不出票
        assertNotNull(resp.getId());
        assertNotNull(acquisitionMapper.selectById(resp.getId()));
        assertEquals(Boolean.FALSE, resp.getQuotaPassed());
        assertEquals(0, new BigDecimal("1000.00").compareTo(resp.getQuotaRemainingAmount()));
        assertTrue(resp.getQuotaMessage().contains("500 万"), "实际：" + resp.getQuotaMessage());
    }

    // ==================== 确认书导出 ====================

    @Test
    public void testExportConfirmation_writesExcel() throws Exception {
        PayeeInfoDO payee = insertPayee("褚十四", "13800138011");
        IcbcGoodsConfigDO config = insertGoodsConfig("废钢", "吨", "0.01", "GENERAL");

        AcquisitionCreateReqVO reqVO = baseReq(payee.getId(), config.getId());
        reqVO.setQuantity(new BigDecimal("5"));
        reqVO.setAmount(new BigDecimal("500.00"));
        reqVO.setVehiclePlateNo("京C00001");
        reqVO.setTradeAddress("北京市朝阳区回收站");
        reqVO.setSettlementMethod("银行转账，过磅后 3 日内结清");
        Long id = acquisitionService.createAcquisition(reqVO).getId();

        MockHttpServletResponse response = new MockHttpServletResponse();
        acquisitionService.exportConfirmation(id, response);

        assertTrue(response.getContentAsByteArray().length > 0);
        assertTrue(response.getContentType().contains("ms-excel"), "实际：" + response.getContentType());
    }

    // ==================== 造数 ====================

    private PayeeInfoDO insertPayee(String name, String mobile) {
        PayeeInfoDO payee = PayeeInfoDO.builder()
                .partnerPayeeId("PARTNER_" + mobile)
                .name(name)
                .mobile(mobile)
                .idCardNo("11010119900101" + mobile.substring(7))
                .build();
        payeeInfoMapper.insert(payee);
        return payee;
    }

    /**
     * 造一张已开出的蓝票：额度台账只认「真开出来了」的销售额（与在途），
     * 未开票 / 开票失败 / 预开票未成功都不占额度。
     */
    private void insertIssuedOrder(PayeeInfoDO payee, BigDecimal amount) {
        invoiceOrderMapper.insert(InvoiceOrderDO.builder()
                .orderNo("INV_QUOTA_" + payee.getId())
                .partnerOrderId("ORDER_QUOTA_" + payee.getId())
                .payeeId(payee.getId())
                .payeeNo(payee.getPartnerPayeeId())
                .totalAmount(amount)
                .invoiceAmount(amount)
                .taxRate(new BigDecimal("0.01"))
                .invoiceType(1)
                .businessType("SCRAP")
                .orderStatus(3)
                .invoiceStatus(InvoiceIssueStatusEnum.ISSUED.getStatus())
                .paymentStatus(2)
                .taxStatus(0)
                .preInvoiceStatus(PreInvoiceStatusEnum.SUCCESS.getStatus())
                .invoiceDate(LocalDateTime.now().minusDays(10))
                .build());
    }

    private IcbcGoodsConfigDO insertGoodsConfig(String name, String unit, String taxRate, String taxMethod) {
        IcbcGoodsConfigDO config = new IcbcGoodsConfigDO();
        config.setName(name);
        config.setUnit(unit);
        config.setTaxRate(new BigDecimal(taxRate));
        config.setTaxMethod(taxMethod);
        config.setMergedCode("1090101010000000000");
        config.setStatus(0);
        goodsConfigMapper.insert(config);
        return config;
    }

    private AcquisitionCreateReqVO baseReq(Long payeeId, Long goodsConfigId) {
        AcquisitionCreateReqVO reqVO = new AcquisitionCreateReqVO();
        reqVO.setPayeeId(payeeId);
        reqVO.setGoodsConfigId(goodsConfigId);
        reqVO.setWeightTicketNo("WD_DEFAULT");
        reqVO.setTradeAddress("北京市朝阳区回收站");
        reqVO.setTradeTime(LocalDateTime.of(2026, 12, 1, 10, 0));
        return reqVO;
    }

}
