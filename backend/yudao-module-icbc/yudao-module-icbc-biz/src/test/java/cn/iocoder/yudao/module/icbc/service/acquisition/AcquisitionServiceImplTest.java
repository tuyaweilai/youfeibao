package cn.iocoder.yudao.module.icbc.service.acquisition;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.icbc.UnitTestConfiguration;
import cn.iocoder.yudao.module.icbc.controller.admin.acquisition.vo.*;
import cn.iocoder.yudao.module.erp.enums.purchase.SellerSubjectTypeEnum;
import cn.iocoder.yudao.module.icbc.controller.admin.handover.vo.HandoverBatchCreateReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.handover.vo.HandoverWeighingAddReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.handover.vo.HandoverWeighingEffectiveReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.acquisition.IcbcAcquisitionDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.goodscfg.IcbcGoodsConfigDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.handover.IcbcWeighingDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.invoice.InvoiceOrderDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payee.PayeeInfoDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.acquisition.IcbcAcquisitionMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.goodscfg.IcbcGoodsConfigMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.handover.IcbcWeighingMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.invoice.InvoiceOrderMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.payee.PayeeInfoMapper;
import cn.iocoder.yudao.module.icbc.enums.AcquisitionStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.HandoverSourceTypeEnum;
import cn.iocoder.yudao.module.icbc.enums.InvoiceIssueStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.PreInvoiceStatusEnum;
import cn.iocoder.yudao.module.icbc.gateway.IcbcGateway;
import cn.iocoder.yudao.module.icbc.service.acquisition.impl.AcquisitionServiceImpl;
import cn.iocoder.yudao.module.icbc.service.acquisition.recognition.AcquisitionRecognitionPort;
import cn.iocoder.yudao.module.icbc.service.handover.HandoverBatchService;
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
    @Resource
    private HandoverBatchService handoverBatchService;
    @Resource
    private IcbcWeighingMapper weighingMapper;

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
    public void testCreateAcquisition_snapshotsSellerSubjectTypeAsNatural() {
        PayeeInfoDO payee = insertPayee("张三", "13800138000");
        IcbcGoodsConfigDO config = insertGoodsConfig("废钢", "吨", "0.01", "GENERAL");
        AcquisitionCreateReqVO reqVO = baseReq(payee.getId(), config.getId());
        reqVO.setQuantity(new BigDecimal("10"));
        reqVO.setAmount(new BigDecimal("1000.00"));

        Long id = acquisitionService.createAcquisition(reqVO).getId();

        // 采购单据上留卖方主体类型快照（ADR 0029）：收购单只收自然人，默认即自然人出售者
        assertEquals(SellerSubjectTypeEnum.NATURAL.getType(),
                acquisitionMapper.selectById(id).getSellerSubjectType());
    }

    @Test
    public void testCreateAcquisition_nonNaturalSellerRejected() {
        PayeeInfoDO payee = insertPayee("某某个体工商户", "13800138000");
        IcbcGoodsConfigDO config = insertGoodsConfig("废钢", "吨", "0.01", "GENERAL");
        AcquisitionCreateReqVO reqVO = baseReq(payee.getId(), config.getId());
        reqVO.setQuantity(new BigDecimal("10"));
        reqVO.setAmount(new BigDecimal("1000.00"));
        // 个体工商户不是自然人：反向开票通道（收购单）一律拦下，指向进项收票
        reqVO.setSellerSubjectType(SellerSubjectTypeEnum.INDIVIDUAL_BUSINESS.getType());

        assertServiceException(() -> acquisitionService.createAcquisition(reqVO),
                SELLER_SUBJECT_TYPE_NOT_NATURAL, SellerSubjectTypeEnum.INDIVIDUAL_BUSINESS.getName());
        assertEquals(0, acquisitionMapper.selectList().size());
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

    // ==================== 计价模型（#32 / ADR 0019） ====================

    @Test
    public void testCreateAcquisition_settlementWeightByWeightDeduction() {
        PayeeInfoDO payee = insertPayee("甲一", "13800138100");
        IcbcGoodsConfigDO config = insertGoodsConfig("废钢", "吨", "0.01", "GENERAL");

        AcquisitionCreateReqVO reqVO = baseReq(payee.getId(), config.getId());
        reqVO.setQuantity(new BigDecimal("5"));
        reqVO.setGrossWeight(new BigDecimal("18000.00"));
        reqVO.setTareWeight(new BigDecimal("5500.00"));
        reqVO.setDeduction(new BigDecimal("500.00"));
        reqVO.setDeductionMethod("WEIGHT");
        reqVO.setUnitPrice(new BigDecimal("2.00"));

        Long id = acquisitionService.createAcquisition(reqVO).getId();

        IcbcAcquisitionDO saved = acquisitionMapper.selectById(id);
        // 结算重量 = 毛重 − 皮重 − 扣杂
        assertEquals(0, new BigDecimal("12000.00").compareTo(saved.getSettlementWeight()));
        // 金额 = 结算重量 × 单价（数量不再参与）
        assertEquals(0, new BigDecimal("24000.00").compareTo(saved.getAmount()));
    }

    @Test
    public void testCreateAcquisition_settlementWeightByRatioDeduction() {
        PayeeInfoDO payee = insertPayee("甲二", "13800138101");
        IcbcGoodsConfigDO config = insertGoodsConfig("废钢", "吨", "0.01", "GENERAL");

        AcquisitionCreateReqVO reqVO = baseReq(payee.getId(), config.getId());
        reqVO.setQuantity(new BigDecimal("5"));
        reqVO.setGrossWeight(new BigDecimal("18000.00"));
        reqVO.setTareWeight(new BigDecimal("5500.00"));
        reqVO.setDeduction(new BigDecimal("0.10"));
        reqVO.setDeductionMethod("RATIO");
        reqVO.setUnitPrice(new BigDecimal("2.00"));

        Long id = acquisitionService.createAcquisition(reqVO).getId();

        IcbcAcquisitionDO saved = acquisitionMapper.selectById(id);
        // 净重 12500，按比例扣 10% = 1250，结算重量 11250
        assertEquals(0, new BigDecimal("11250.0000").compareTo(saved.getSettlementWeight()));
        assertEquals(0, new BigDecimal("22500.00").compareTo(saved.getAmount()));
    }

    @Test
    public void testCreateAcquisition_adjustmentRequiresReasonAndIsAddedToAmount() {
        PayeeInfoDO payee = insertPayee("甲三", "13800138102");
        IcbcGoodsConfigDO config = insertGoodsConfig("废钢", "吨", "0.01", "GENERAL");

        AcquisitionCreateReqVO missingReason = baseReq(payee.getId(), config.getId());
        missingReason.setQuantity(new BigDecimal("5"));
        missingReason.setGrossWeight(new BigDecimal("18000.00"));
        missingReason.setTareWeight(new BigDecimal("5500.00"));
        missingReason.setUnitPrice(new BigDecimal("2.00"));
        missingReason.setAdjustmentAmount(new BigDecimal("-100.00"));
        assertServiceException(() -> acquisitionService.createAcquisition(missingReason),
                ACQUISITION_ADJUSTMENT_REASON_REQUIRED);

        AcquisitionCreateReqVO withReason = baseReq(payee.getId(), config.getId());
        withReason.setQuantity(new BigDecimal("5"));
        withReason.setGrossWeight(new BigDecimal("18000.00"));
        withReason.setTareWeight(new BigDecimal("5500.00"));
        withReason.setUnitPrice(new BigDecimal("2.00"));
        withReason.setAdjustmentAmount(new BigDecimal("-100.00"));
        withReason.setAdjustmentReason("扣运费 100 元");
        Long id = acquisitionService.createAcquisition(withReason).getId();

        // 金额 = 结算重量 × 单价 + 调整项 = 12500 × 2 − 100
        assertEquals(0, new BigDecimal("24900.00").compareTo(acquisitionMapper.selectById(id).getAmount()));
    }

    @Test
    public void testCreateAcquisition_deductionExceedsNetWeightRejected() {
        PayeeInfoDO payee = insertPayee("甲四", "13800138103");
        IcbcGoodsConfigDO config = insertGoodsConfig("废钢", "吨", "0.01", "GENERAL");

        AcquisitionCreateReqVO reqVO = baseReq(payee.getId(), config.getId());
        reqVO.setQuantity(new BigDecimal("5"));
        reqVO.setGrossWeight(new BigDecimal("18000.00"));
        reqVO.setTareWeight(new BigDecimal("5500.00"));
        reqVO.setDeduction(new BigDecimal("13000.00"));
        reqVO.setDeductionMethod("WEIGHT");
        reqVO.setUnitPrice(new BigDecimal("2.00"));

        assertServiceException(() -> acquisitionService.createAcquisition(reqVO),
                ACQUISITION_SETTLEMENT_WEIGHT_INVALID);
    }

    @Test
    public void testCreateAcquisition_legacyWithoutWeightStillUsesQuantityFormula() {
        PayeeInfoDO payee = insertPayee("甲五", "13800138104");
        IcbcGoodsConfigDO config = insertGoodsConfig("废钢", "吨", "0.01", "GENERAL");

        AcquisitionCreateReqVO reqVO = baseReq(payee.getId(), config.getId());
        reqVO.setQuantity(new BigDecimal("3"));
        reqVO.setUnitPrice(new BigDecimal("10.00"));

        Long id = acquisitionService.createAcquisition(reqVO).getId();

        IcbcAcquisitionDO saved = acquisitionMapper.selectById(id);
        // 历史口径：没有重量时不强行造结算重量
        assertNull(saved.getSettlementWeight());
        assertEquals(0, new BigDecimal("30.00").compareTo(saved.getAmount()));
    }

    @Test
    public void testCorrectRecognition_recomputesSettlementWeightAndAmount() {
        PayeeInfoDO payee = insertPayee("甲六", "13800138105");
        IcbcGoodsConfigDO config = insertGoodsConfig("废钢", "吨", "0.01", "GENERAL");

        AcquisitionCreateReqVO reqVO = baseReq(payee.getId(), config.getId());
        reqVO.setQuantity(new BigDecimal("5"));
        reqVO.setGrossWeight(new BigDecimal("18000.00"));
        reqVO.setTareWeight(new BigDecimal("5500.00"));
        reqVO.setUnitPrice(new BigDecimal("2.00"));
        Long id = acquisitionService.createAcquisition(reqVO).getId();
        assertEquals(0, new BigDecimal("25000.00").compareTo(acquisitionMapper.selectById(id).getAmount()));

        // 现场更正扣杂：结算重量与金额都要跟着重算
        AcquisitionCorrectionReqVO correction = new AcquisitionCorrectionReqVO();
        correction.setId(id);
        correction.setDeduction(new BigDecimal("500.00"));
        correction.setDeductionMethod("WEIGHT");
        acquisitionService.correctRecognition(correction);

        IcbcAcquisitionDO saved = acquisitionMapper.selectById(id);
        assertEquals(0, new BigDecimal("12000.00").compareTo(saved.getSettlementWeight()));
        assertEquals(0, new BigDecimal("24000.00").compareTo(saved.getAmount()));
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

    // ==================== 交接批次与有效磅次（#50 T12） ====================

    @Test
    public void testCreateAcquisition_fromBatch_usesEffectiveWeighingNotManualInput() {
        PayeeInfoDO payee = insertPayee("张三", "13800138000");
        IcbcGoodsConfigDO config = insertGoodsConfig("废钢", "吨", "0.01", "SIMPLE");
        Long batchId = createBatch(payee.getId(), "京A12345");
        Long first = addWeighing(batchId, "18000", "5500", "WD-AM");
        Long second = addWeighing(batchId, "18100", "5500", "WD-AM-RE");
        selectEffective(batchId, second, "复磅后以第二次为准");

        AcquisitionCreateReqVO reqVO = baseReq(payee.getId(), config.getId());
        reqVO.setHandoverBatchId(batchId);
        reqVO.setQuantity(new BigDecimal("10"));
        reqVO.setUnitPrice(new BigDecimal("100.00"));
        // 现场手填了一套不对的重量与磅单号；挂了批次就必须以有效磅次为准
        reqVO.setGrossWeight(new BigDecimal("20000"));
        reqVO.setTareWeight(new BigDecimal("6000"));
        reqVO.setWeightTicketNo("WD-WRONG");

        Long id = acquisitionService.createAcquisition(reqVO).getId();

        IcbcAcquisitionDO saved = acquisitionMapper.selectById(id);
        assertEquals(batchId, saved.getHandoverBatchId());
        // 计量结果引用的是被选定的那一次（version = seqNo）
        assertEquals(second, saved.getWeighingId());
        assertEquals(2, saved.getWeighingSeqNo());
        assertEquals(0, new BigDecimal("18100.0000").compareTo(saved.getGrossWeight()));
        assertEquals(0, new BigDecimal("5500.0000").compareTo(saved.getTareWeight()));
        assertEquals(0, new BigDecimal("12600.0000").compareTo(saved.getNetWeight()));
        assertEquals("WD-AM-RE", saved.getWeightTicketNo());
        assertEquals("京A12345", saved.getVehiclePlateNo());
        // 结算重量 = 有效磅次的净重 − 扣杂（扣杂为 0），金额 = 结算重量 × 单价
        assertEquals(0, new BigDecimal("12600.0000").compareTo(saved.getSettlementWeight()));
        assertEquals(0, new BigDecimal("1260000.00").compareTo(saved.getAmount()));
        // 第一次磅次留档但不参与
        assertFalse(weighingMapper.selectById(first).getEffective());
    }

    @Test
    public void testCreateAcquisition_batchWithoutEffectiveWeighing_rejected() {
        PayeeInfoDO payee = insertPayee("张三", "13800138000");
        IcbcGoodsConfigDO config = insertGoodsConfig("废钢", "吨", "0.01", "SIMPLE");
        Long batchId = createBatch(payee.getId(), "京A12345");

        AcquisitionCreateReqVO reqVO = baseReq(payee.getId(), config.getId());
        reqVO.setHandoverBatchId(batchId);
        reqVO.setQuantity(new BigDecimal("10"));
        reqVO.setUnitPrice(new BigDecimal("100.00"));
        // 计量依据不明就不能计价：不猜、不退回手填值
        assertServiceException(() -> acquisitionService.createAcquisition(reqVO),
                WEIGHING_EFFECTIVE_NOT_SELECTED);
    }

    @Test
    public void testCreateAcquisition_batchCounterpartyMismatch_rejected() {
        PayeeInfoDO seller = insertPayee("张三", "13800138000");
        PayeeInfoDO other = insertPayee("李四", "13800138001");
        IcbcGoodsConfigDO config = insertGoodsConfig("废钢", "吨", "0.01", "SIMPLE");
        Long batchId = createBatch(seller.getId(), "京A12345");
        addWeighing(batchId, "18000", "5500", "WD-AM");

        AcquisitionCreateReqVO reqVO = baseReq(other.getId(), config.getId());
        reqVO.setHandoverBatchId(batchId);
        reqVO.setQuantity(new BigDecimal("10"));
        reqVO.setUnitPrice(new BigDecimal("100.00"));
        assertServiceException(() -> acquisitionService.createAcquisition(reqVO),
                ACQUISITION_BATCH_PAYEE_MISMATCH);
    }

    @Test
    public void testCreateAcquisition_twoBatchesSameVehicleSameDay_doNotCross() {
        PayeeInfoDO payee = insertPayee("张三", "13800138000");
        IcbcGoodsConfigDO config = insertGoodsConfig("废钢", "吨", "0.01", "SIMPLE");
        // AC3：同一车同一天两次送货 = 两个交接批次，磅单与收购单不串
        Long morning = createBatch(payee.getId(), "京A12345");
        Long morningWeighing = addWeighing(morning, "18000", "5500", "WD-AM");
        Long afternoon = createBatch(payee.getId(), "京A12345");
        Long afternoonWeighing = addWeighing(afternoon, "9000", "3000", "WD-PM");

        Long morningAcquisition = acquisitionService.createAcquisition(
                fromBatch(baseReq(payee.getId(), config.getId()), morning)).getId();
        Long afternoonAcquisition = acquisitionService.createAcquisition(
                fromBatch(baseReq(payee.getId(), config.getId()), afternoon)).getId();

        IcbcAcquisitionDO morningSaved = acquisitionMapper.selectById(morningAcquisition);
        IcbcAcquisitionDO afternoonSaved = acquisitionMapper.selectById(afternoonAcquisition);
        assertEquals(morning, morningSaved.getHandoverBatchId());
        assertEquals(morningWeighing, morningSaved.getWeighingId());
        assertEquals("WD-AM", morningSaved.getWeightTicketNo());
        assertEquals(0, new BigDecimal("12500.0000").compareTo(morningSaved.getNetWeight()));
        assertEquals(afternoon, afternoonSaved.getHandoverBatchId());
        assertEquals(afternoonWeighing, afternoonSaved.getWeighingId());
        assertEquals("WD-PM", afternoonSaved.getWeightTicketNo());
        assertEquals(0, new BigDecimal("6000.0000").compareTo(afternoonSaved.getNetWeight()));
    }

    @Test
    public void testCreateAcquisition_sameBatchSplitIntoTwoAcquisitions() {
        PayeeInfoDO payee = insertPayee("张三", "13800138000");
        IcbcGoodsConfigDO steel = insertGoodsConfig("废钢", "吨", "0.01", "SIMPLE");
        IcbcGoodsConfigDO paper = insertGoodsConfig("废纸", "吨", "0.01", "GENERAL");
        Long batchId = createBatch(payee.getId(), "京A12345");
        Long weighingId = addWeighing(batchId, "18000", "5500", "WD-AM");

        // 一次混装按品类拆成多张收购单：同一个批次、同一次有效磅次（计量基础只有一个）
        Long first = acquisitionService.createAcquisition(
                fromBatch(baseReq(payee.getId(), steel.getId()), batchId)).getId();
        Long second = acquisitionService.createAcquisition(
                fromBatch(baseReq(payee.getId(), paper.getId()), batchId)).getId();

        assertEquals(weighingId, acquisitionMapper.selectById(first).getWeighingId());
        assertEquals(weighingId, acquisitionMapper.selectById(second).getWeighingId());
        assertEquals(2L, handoverBatchService.countAcquisitions(batchId));
        assertEquals(2L, handoverBatchService.getBatch(batchId).getAcquisitionCount());
        // 但有效磅次从此锁住：第一张单之后就改不了
        HandoverWeighingEffectiveReqVO locked = new HandoverWeighingEffectiveReqVO();
        locked.setBatchId(batchId);
        locked.setWeighingId(weighingId);
        assertServiceException(() -> handoverBatchService.selectEffectiveWeighing(locked),
                WEIGHING_BATCH_IN_USE);
    }

    @Test
    public void testCorrectRecognition_weightsLockedWhenMeasuredByWeighing() {
        PayeeInfoDO payee = insertPayee("张三", "13800138000");
        IcbcGoodsConfigDO config = insertGoodsConfig("废钢", "吨", "0.01", "SIMPLE");
        Long batchId = createBatch(payee.getId(), "京A12345");
        addWeighing(batchId, "18000", "5500", "WD-AM");
        Long id = acquisitionService.createAcquisition(
                fromBatch(baseReq(payee.getId(), config.getId()), batchId)).getId();

        AcquisitionCorrectionReqVO wrong = new AcquisitionCorrectionReqVO();
        wrong.setId(id);
        wrong.setGrossWeight(new BigDecimal("19000"));
        assertServiceException(() -> acquisitionService.correctRecognition(wrong),
                WEIGHING_LOCKED_FOR_ACQUISITION);

        // 与重量无关的补录照旧（车牌识别结果、备注）
        AcquisitionCorrectionReqVO remarkOnly = new AcquisitionCorrectionReqVO();
        remarkOnly.setId(id);
        remarkOnly.setRemark("现场目测含少量杂质");
        acquisitionService.correctRecognition(remarkOnly);
        assertEquals("现场目测含少量杂质", acquisitionMapper.selectById(id).getRemark());
        // 重量仍是那一版磅次的值
        assertEquals(0, new BigDecimal("18000.0000").compareTo(
                acquisitionMapper.selectById(id).getGrossWeight()));
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

    /**
     * 交接批次（#50）：这里用「上门地址」而不是场站，避免额外造场站数据。
     */
    private Long createBatch(Long payeeId, String plateNo) {
        HandoverBatchCreateReqVO reqVO = new HandoverBatchCreateReqVO();
        reqVO.setPayeeId(payeeId);
        reqVO.setVisitAddress("北京市朝阳区回收站");
        reqVO.setPlateNo(plateNo);
        reqVO.setSourceType(HandoverSourceTypeEnum.WALK_IN.getType());
        reqVO.setDriverName("李师傅");
        return handoverBatchService.createBatch(reqVO);
    }

    private Long addWeighing(Long batchId, String gross, String tare, String ticketNo) {
        HandoverWeighingAddReqVO reqVO = new HandoverWeighingAddReqVO();
        reqVO.setBatchId(batchId);
        reqVO.setGrossWeight(new BigDecimal(gross));
        reqVO.setTareWeight(new BigDecimal(tare));
        reqVO.setWeightTicketNo(ticketNo);
        reqVO.setPlateNo("京A12345");
        return handoverBatchService.addWeighing(reqVO);
    }

    private void selectEffective(Long batchId, Long weighingId, String reason) {
        HandoverWeighingEffectiveReqVO reqVO = new HandoverWeighingEffectiveReqVO();
        reqVO.setBatchId(batchId);
        reqVO.setWeighingId(weighingId);
        reqVO.setReason(reason);
        handoverBatchService.selectEffectiveWeighing(reqVO);
    }

    /** 收购单挂在批次上：重量与磅单以该批次的有效磅次为准。 */
    private AcquisitionCreateReqVO fromBatch(AcquisitionCreateReqVO reqVO, Long batchId) {
        reqVO.setHandoverBatchId(batchId);
        reqVO.setQuantity(new BigDecimal("10"));
        reqVO.setUnitPrice(new BigDecimal("100.00"));
        return reqVO;
    }

}
