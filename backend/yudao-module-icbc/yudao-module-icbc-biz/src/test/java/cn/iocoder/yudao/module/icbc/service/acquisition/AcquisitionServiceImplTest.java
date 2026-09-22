package cn.iocoder.yudao.module.icbc.service.acquisition;

import cn.iocoder.yudao.module.erp.api.stock.StockApi;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.icbc.UnitTestConfiguration;
import cn.iocoder.yudao.module.icbc.controller.admin.acquisition.vo.*;
import cn.iocoder.yudao.module.erp.enums.purchase.SellerSubjectTypeEnum;
import cn.iocoder.yudao.module.icbc.controller.admin.acquisition.vo.AcquisitionCompleteDocumentsReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.handover.vo.HandoverBatchCreateReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.handover.vo.HandoverWeighingAddReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.handover.vo.HandoverWeighingEffectiveReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo.PurchaseOrderItemReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo.PurchaseOrderSaveReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo.PurchaseOrderStatusUpdateReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.acquisition.IcbcAcquisitionDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.goodscfg.IcbcGoodsConfigDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.handover.IcbcHandoverBatchDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.handover.IcbcWeighingDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.invoice.InvoiceOrderDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payee.PayeeInfoDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.station.IcbcStationDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.acquisition.IcbcAcquisitionMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.goodscfg.IcbcGoodsConfigMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.handover.IcbcHandoverBatchMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.handover.IcbcWeighingMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.invoice.InvoiceOrderMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.payee.PayeeInfoMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.station.IcbcStationMapper;
import cn.iocoder.yudao.module.icbc.enums.AcquisitionStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.AcquisitionDocumentStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.HandoverSourceTypeEnum;
import cn.iocoder.yudao.module.icbc.enums.InvoiceIssueStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.PaymentStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.PreInvoiceStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.PurchaseOrderPriceModeEnum;
import cn.iocoder.yudao.module.icbc.enums.PurchaseOrderStatusEnum;
import cn.iocoder.yudao.module.icbc.gateway.IcbcGateway;
import cn.iocoder.yudao.module.icbc.service.acquisition.impl.AcquisitionServiceImpl;
import cn.iocoder.yudao.module.icbc.service.acquisition.recognition.AcquisitionRecognitionPort;
import cn.iocoder.yudao.module.icbc.service.handover.HandoverBatchService;
import cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo.PurchaseOrderProgressRespVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.purchaseorder.IcbcPurchaseOrderDealDO;
import cn.iocoder.yudao.module.icbc.enums.PurchaseDealSourceTypeEnum;
import cn.iocoder.yudao.module.icbc.service.purchaseorder.PurchaseOrderService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
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

    /** 库存域只通过 erp-api 的 StockApi 接入（#52）；单元测试不跨模块，用 Mock。 */
    @MockBean
    private StockApi stockApi;

    @Resource
    private AcquisitionService acquisitionService;
    @Resource
    private IcbcAcquisitionMapper acquisitionMapper;
    @Resource
    private PayeeInfoMapper payeeInfoMapper;
    @Resource
    private IcbcGoodsConfigMapper goodsConfigMapper;
    @Resource
    private AcquisitionProgressService acquisitionProgressService;
    @Resource
    private InvoiceOrderMapper invoiceOrderMapper;
    @Resource
    private HandoverBatchService handoverBatchService;
    @Resource
    private IcbcWeighingMapper weighingMapper;
    @Resource
    private IcbcHandoverBatchMapper handoverBatchMapper;
    @Resource
    private PurchaseOrderService purchaseOrderService;
    @Resource
    private IcbcStationMapper stationMapper;

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
    public void testCreateAcquisition_doesNotCallRecognition() {
        // #112：识别从「提交时后端静默回填」搬到「现场拍照那一刻」，登记路径上不再调识别——
        // 图片字节只在现场端手上，且只有当场回显给收货员，识别结果才有被核对的机会。
        PayeeInfoDO payee = insertPayee("钱十一", "13800138008");
        IcbcGoodsConfigDO config = insertGoodsConfig("废钢", "吨", "0.01", "GENERAL");

        AcquisitionCreateReqVO reqVO = baseReq(payee.getId(), config.getId());
        reqVO.setQuantity(new BigDecimal("5"));
        reqVO.setAmount(new BigDecimal("500.00"));
        reqVO.setWeightTicketNo("WD20261201001");
        reqVO.setWeightTicketImageUrl("https://cdn/w.jpg");
        reqVO.setVehicleFrontImageUrl("https://cdn/front.jpg");

        Long id = acquisitionService.createAcquisition(reqVO).getId();

        IcbcAcquisitionDO saved = acquisitionMapper.selectById(id);
        assertNull(saved.getVehiclePlateNo(), "没有识别回填，车牌留给现场手工录入");
        verifyNoInteractions(recognitionPort);
    }

    @Test
    public void testRecognizePlate_mapsPortResultToResp() {
        when(recognitionPort.recognizePlate("QUJD")).thenReturn(
                AcquisitionRecognitionPort.PlateRecognition.builder()
                        .plateNo("京B00001").confidence(95)
                        .warnings(Collections.singletonList("识别置信度偏低，请核对车牌"))
                        .build());

        AcquisitionPlateRecognitionReqVO reqVO = new AcquisitionPlateRecognitionReqVO();
        reqVO.setImageBase64("QUJD");
        AcquisitionPlateRecognitionRespVO resp = acquisitionService.recognizePlate(reqVO);

        assertEquals("京B00001", resp.getPlateNo());
        assertEquals(95, resp.getConfidence());
        assertEquals(Collections.singletonList("识别置信度偏低，请核对车牌"), resp.getWarnings());
    }

    @Test
    public void testRecognizeWeightTicket_mapsPortResultToResp() {
        when(recognitionPort.recognizeWeightTicket("QUJD")).thenReturn(
                AcquisitionRecognitionPort.WeightTicketRecognition.builder()
                        .weightTicketNo("2105").grossWeight(new BigDecimal("32220"))
                        .tareWeight(new BigDecimal("13090")).netWeight(new BigDecimal("19130"))
                        .plateNo("豫A05648").deduction(new BigDecimal("0.025")).deductionMethod("RATIO")
                        .warnings(Collections.singletonList("磅单三个重量不自洽（毛重-皮重≠净重），请核对"))
                        .rawLines(Arrays.asList("过磅单", "总重 GROSS 32220 Kg"))
                        .build());

        AcquisitionWeightTicketRecognitionReqVO reqVO = new AcquisitionWeightTicketRecognitionReqVO();
        reqVO.setImageBase64("QUJD");
        AcquisitionWeightTicketRecognitionRespVO resp = acquisitionService.recognizeWeightTicket(reqVO);

        assertEquals("2105", resp.getWeightTicketNo());
        assertEquals(0, new BigDecimal("32220").compareTo(resp.getGrossWeight()));
        assertEquals(0, new BigDecimal("19130").compareTo(resp.getNetWeight()));
        assertEquals("豫A05648", resp.getPlateNo());
        assertEquals(0, new BigDecimal("0.025").compareTo(resp.getDeduction()));
        assertEquals("RATIO", resp.getDeductionMethod());
        assertEquals(Arrays.asList("过磅单", "总重 GROSS 32220 Kg"), resp.getRawLines());
        assertEquals(1, resp.getWarnings().size());
    }

    @Test
    public void testRecognizeWeightTicket_emptyResultIsNotAnError() {
        // 读不出来、未配置供应商、厂商报错都走空结果这条路：现场手工录入，不抛异常
        when(recognitionPort.recognizeWeightTicket("QUJD"))
                .thenReturn(AcquisitionRecognitionPort.WeightTicketRecognition.empty());

        AcquisitionWeightTicketRecognitionReqVO reqVO = new AcquisitionWeightTicketRecognitionReqVO();
        reqVO.setImageBase64("QUJD");
        AcquisitionWeightTicketRecognitionRespVO resp = acquisitionService.recognizeWeightTicket(reqVO);

        assertNull(resp.getWeightTicketNo());
        assertNull(resp.getGrossWeight());
    }

    @Test
    public void testRecognizePlate_emptyResultIsNotAnError() {
        // 识别失败（stub / 未配 / 厂商报错）必须走空结果这条路，现场退化为手工录入，不抛异常
        when(recognitionPort.recognizePlate("QUJD"))
                .thenReturn(AcquisitionRecognitionPort.PlateRecognition.empty());

        AcquisitionPlateRecognitionReqVO reqVO = new AcquisitionPlateRecognitionReqVO();
        reqVO.setImageBase64("QUJD");
        AcquisitionPlateRecognitionRespVO resp = acquisitionService.recognizePlate(reqVO);

        assertNull(resp.getPlateNo());
        assertNull(resp.getConfidence());
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

    // ==================== 进度派生（ADR 0038） ====================

    @Test
    public void testProgress_derivedFromInvoiceOrder() {
        PayeeInfoDO payee = insertPayee("陈十三", "13800138010");
        IcbcGoodsConfigDO config = insertGoodsConfig("废钢", "吨", "0.01", "GENERAL");

        AcquisitionCreateReqVO reqVO = baseReq(payee.getId(), config.getId());
        reqVO.setQuantity(new BigDecimal("5"));
        reqVO.setAmount(new BigDecimal("500.00"));
        Long id = acquisitionService.createAcquisition(reqVO).getId();
        assertEquals(AcquisitionStatusEnum.REGISTERED.getStatus(),
                acquisitionMapper.selectById(id).getStatus());

        // 预下单返回后挂单：预开票还在途、出售者也还没在工行页面上确认——不是「待付款」
        insertInvoiceOrder("ORDER_PROGRESS_1", id, PreInvoiceStatusEnum.IN_PROGRESS.getStatus(),
                PaymentStatusEnum.PENDING.getStatus(), InvoiceIssueStatusEnum.NOT_ISSUED.getStatus());
        acquisitionService.linkInvoice(id, "ORDER_PROGRESS_1");
        IcbcAcquisitionDO linked = acquisitionMapper.selectById(id);
        assertEquals("ORDER_PROGRESS_1", linked.getInvoicePartnerOrderId());
        assertEquals(AcquisitionStatusEnum.WAITING_SELLER_CONFIRM.getStatus(), linked.getStatus());

        // 预开票成功：票已备好、等回收企业掏钱——这才是「待付款」
        syncOrderState("ORDER_PROGRESS_1", PreInvoiceStatusEnum.SUCCESS.getStatus(),
                PaymentStatusEnum.PENDING.getStatus(), InvoiceIssueStatusEnum.NOT_ISSUED.getStatus());
        assertEquals(AcquisitionStatusEnum.PENDING_PAYMENT.getStatus(),
                acquisitionMapper.selectById(id).getStatus());

        // 付款成功：货款已到出售者卡上，票还没开出来
        syncOrderState("ORDER_PROGRESS_1", PreInvoiceStatusEnum.SUCCESS.getStatus(),
                PaymentStatusEnum.SUCCESS.getStatus(), InvoiceIssueStatusEnum.ISSUING.getStatus());
        assertEquals(AcquisitionStatusEnum.PAID.getStatus(),
                acquisitionMapper.selectById(id).getStatus());

        // 票开出：「已开票」不再是一个永远不会出现的档位
        syncOrderState("ORDER_PROGRESS_1", PreInvoiceStatusEnum.SUCCESS.getStatus(),
                PaymentStatusEnum.SUCCESS.getStatus(), InvoiceIssueStatusEnum.ISSUED.getStatus());
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

    // ==================== 采购安排关联与「直接收购」（#51 T13） ====================

    @Test
    public void testCreateAcquisition_linksUsablePurchaseArrangement() {
        PayeeInfoDO payee = insertPayee("张三", "13800138200");
        IcbcGoodsConfigDO config = insertGoodsConfig("废钢", "吨", "0.01", "SIMPLE");
        Long orderId = createPurchaseOrder(payee.getId(), config.getId(), "100", "2000");
        Long itemId = purchaseOrderService.getDetail(orderId).getItems().get(0).getId();

        AcquisitionCreateReqVO reqVO = baseReq(payee.getId(), config.getId());
        reqVO.setQuantity(new BigDecimal("10"));
        reqVO.setUnitPrice(new BigDecimal("2000.00"));
        reqVO.setPurchaseOrderId(orderId);
        reqVO.setPurchaseOrderItemId(itemId);

        Long id = acquisitionService.createAcquisition(reqVO).getId();

        IcbcAcquisitionDO saved = acquisitionMapper.selectById(id);
        assertEquals(orderId, saved.getPurchaseOrderId());
        assertEquals(itemId, saved.getPurchaseOrderItemId());
    }

    @Test
    public void testCreateAcquisition_linkedToOrder_recordsDealWithAcceptedAndPricedQuantity() {
        PayeeInfoDO payee = insertPayee("张三", "13800138209");
        IcbcGoodsConfigDO config = insertGoodsConfig("废钢", "吨", "0.01", "SIMPLE");
        Long orderId = createPurchaseOrder(payee.getId(), config.getId(), "100", "2000");
        Long itemId = purchaseOrderService.getDetail(orderId).getItems().get(0).getId();

        AcquisitionCreateReqVO reqVO = baseReq(payee.getId(), config.getId());
        reqVO.setQuantity(new BigDecimal("10"));
        reqVO.setUnitPrice(new BigDecimal("2000.00"));
        reqVO.setGrossWeight(new BigDecimal("12"));
        reqVO.setTareWeight(new BigDecimal("2"));
        reqVO.setDeduction(new BigDecimal("1"));
        reqVO.setDeductionMethod("WEIGHT");
        reqVO.setPurchaseOrderId(orderId);
        reqVO.setPurchaseOrderItemId(itemId);

        Long id = acquisitionService.createAcquisition(reqVO).getId();

        // #58：结算量取计价基准（毛 12 − 皮 2 − 扣杂 1 = 9），验收量取实物量（净重 10）
        List<IcbcPurchaseOrderDealDO> deals = purchaseOrderService.selectDealsBySource(
                PurchaseDealSourceTypeEnum.ACQUISITION.getType(), id);
        assertEquals(1, deals.size());
        assertEquals(0, new BigDecimal("9").compareTo(deals.get(0).getQuantity()));
        assertEquals(0, new BigDecimal("10").compareTo(deals.get(0).getAcceptedQuantity()));

        // 订单的验收口径看到的是实物量（不是计价基准）
        PurchaseOrderProgressRespVO progress = purchaseOrderService.getProgress(orderId);
        assertEquals(0, new BigDecimal("10").compareTo(progress.getAcceptedQuantity()));
        assertEquals(0, new BigDecimal("90").compareTo(progress.getUnperformedQuantity()));
    }

    @Test
    public void testCreateAcquisition_offlineRetryDoesNotRecordSecondDeal() {
        PayeeInfoDO payee = insertPayee("张三", "13800138210");
        IcbcGoodsConfigDO config = insertGoodsConfig("废钢", "吨", "0.01", "SIMPLE");
        Long orderId = createPurchaseOrder(payee.getId(), config.getId(), "100", "2000");
        Long itemId = purchaseOrderService.getDetail(orderId).getItems().get(0).getId();

        AcquisitionCreateReqVO reqVO = baseReq(payee.getId(), config.getId());
        reqVO.setClientRequestId("CLIENT_RETRY_1");
        reqVO.setQuantity(new BigDecimal("10"));
        reqVO.setUnitPrice(new BigDecimal("2000.00"));
        reqVO.setPurchaseOrderId(orderId);
        reqVO.setPurchaseOrderItemId(itemId);

        Long first = acquisitionService.createAcquisition(reqVO).getId();
        Long second = acquisitionService.createAcquisition(reqVO).getId();

        assertEquals(first, second);
        // 重复补传不产生第二条成交（收购单本身按 clientRequestId 幂等，根本走不到写成交）
        assertEquals(1, purchaseOrderService.selectDealsBySource(
                PurchaseDealSourceTypeEnum.ACQUISITION.getType(), first).size());
        assertEquals(0, new BigDecimal("10").compareTo(
                purchaseOrderService.getProgress(orderId).getAcceptedQuantity()));
    }

    @Test
    public void testRecordAcceptance_updatesLinkedOrderDeal() {
        PayeeInfoDO payee = insertPayee("张三", "13800138212");
        IcbcGoodsConfigDO config = insertGoodsConfig("废钢", "吨", "0.01", "SIMPLE");
        Long orderId = createPurchaseOrder(payee.getId(), config.getId(), "100", "2000");
        Long itemId = purchaseOrderService.getDetail(orderId).getItems().get(0).getId();

        AcquisitionCreateReqVO create = baseReq(payee.getId(), config.getId());
        create.setQuantity(new BigDecimal("10"));
        create.setUnitPrice(new BigDecimal("2000.00"));
        create.setGrossWeight(new BigDecimal("12"));
        create.setTareWeight(new BigDecimal("2"));
        create.setPurchaseOrderId(orderId);
        create.setPurchaseOrderItemId(itemId);
        Long id = acquisitionService.createAcquisition(create).getId();

        // 拒收 2：接收量 8，关联订单的成交要跟着降（不追加新的一条）
        AcquisitionAcceptanceReqVO acceptance = acceptanceReq(id, "8", "2", null);
        acceptance.setRejectReason("杂质多");
        acquisitionService.recordAcceptance(acceptance);

        List<IcbcPurchaseOrderDealDO> deals = purchaseOrderService.selectDealsBySource(
                PurchaseDealSourceTypeEnum.ACQUISITION.getType(), id);
        assertEquals(1, deals.size());
        assertEquals(0, new BigDecimal("8").compareTo(deals.get(0).getAcceptedQuantity()));
        assertEquals(0, new BigDecimal("8").compareTo(
                purchaseOrderService.getProgress(orderId).getAcceptedQuantity()));
    }

    @Test
    public void testSyncPurchaseDeal_cancelReversesOnceAndIsIdempotent() {
        PayeeInfoDO payee = insertPayee("张三", "13800138211");
        IcbcGoodsConfigDO config = insertGoodsConfig("废钢", "吨", "0.01", "SIMPLE");
        Long orderId = createPurchaseOrder(payee.getId(), config.getId(), "100", "2000");
        Long itemId = purchaseOrderService.getDetail(orderId).getItems().get(0).getId();

        AcquisitionCreateReqVO reqVO = baseReq(payee.getId(), config.getId());
        reqVO.setQuantity(new BigDecimal("10"));
        reqVO.setUnitPrice(new BigDecimal("2000.00"));
        reqVO.setPurchaseOrderId(orderId);
        reqVO.setPurchaseOrderItemId(itemId);
        Long id = acquisitionService.createAcquisition(reqVO).getId();
        IcbcAcquisitionDO saved = acquisitionMapper.selectById(id);

        // 作废反冲：重复调用只扣一次
        acquisitionService.syncPurchaseDeal(saved, -1);
        acquisitionService.syncPurchaseDeal(saved, -1);

        assertEquals(1, purchaseOrderService.selectDealsBySource(
                PurchaseDealSourceTypeEnum.ACQUISITION_CANCEL.getType(), id).size());
        PurchaseOrderProgressRespVO progress = purchaseOrderService.getProgress(orderId);
        assertEquals(0, BigDecimal.ZERO.compareTo(progress.getAcceptedQuantity()));
    }

    @Test
    public void testCreateAcquisition_withoutArrangementIsDirectAcquisition() {
        PayeeInfoDO payee = insertPayee("张三", "13800138201");
        IcbcGoodsConfigDO config = insertGoodsConfig("废钢", "吨", "0.01", "GENERAL");

        AcquisitionCreateReqVO reqVO = baseReq(payee.getId(), config.getId());
        reqVO.setQuantity(new BigDecimal("5"));
        reqVO.setAmount(new BigDecimal("500.00"));

        Long id = acquisitionService.createAcquisition(reqVO).getId();

        // 不关联不是失败：落 0，报表 / 列表据此标「直接收购」
        IcbcAcquisitionDO saved = acquisitionMapper.selectById(id);
        assertEquals(0L, saved.getPurchaseOrderId());
        assertEquals(0L, saved.getPurchaseOrderItemId());
    }

    @Test
    public void testCreateAcquisition_draftOrderRejected() {
        PayeeInfoDO payee = insertPayee("张三", "13800138202");
        IcbcGoodsConfigDO config = insertGoodsConfig("废钢", "吨", "0.01", "SIMPLE");
        Long orderId = createPurchaseOrder(payee.getId(), config.getId(), "100", "2000", false);
        Long itemId = purchaseOrderService.getDetail(orderId).getItems().get(0).getId();

        AcquisitionCreateReqVO reqVO = baseReq(payee.getId(), config.getId());
        reqVO.setQuantity(new BigDecimal("10"));
        reqVO.setUnitPrice(new BigDecimal("2000.00"));
        reqVO.setPurchaseOrderId(orderId);
        reqVO.setPurchaseOrderItemId(itemId);

        assertServiceException(() -> acquisitionService.createAcquisition(reqVO),
                PURCHASE_ORDER_NOT_EFFECTIVE, purchaseOrderService.getOrder(orderId).getOrderNo());
        assertEquals(0, acquisitionMapper.selectList().size());
    }

    @Test
    public void testCreateAcquisition_expiredOrderRejected() {
        PayeeInfoDO payee = insertPayee("张三", "13800138203");
        IcbcGoodsConfigDO config = insertGoodsConfig("废钢", "吨", "0.01", "SIMPLE");
        Long orderId = createPurchaseOrder(payee.getId(), config.getId(), "100", "2000",
                LocalDate.now().minusDays(10), LocalDate.now().minusDays(1));
        Long itemId = purchaseOrderService.getDetail(orderId).getItems().get(0).getId();

        AcquisitionCreateReqVO reqVO = baseReq(payee.getId(), config.getId());
        reqVO.setQuantity(new BigDecimal("10"));
        reqVO.setUnitPrice(new BigDecimal("2000.00"));
        reqVO.setPurchaseOrderId(orderId);
        reqVO.setPurchaseOrderItemId(itemId);

        assertServiceException(() -> acquisitionService.createAcquisition(reqVO),
                PURCHASE_ORDER_NOT_EFFECTIVE, purchaseOrderService.getOrder(orderId).getOrderNo());
    }

    @Test
    public void testCreateAcquisition_itemNotInOrderRejected() {
        PayeeInfoDO payee = insertPayee("张三", "13800138204");
        IcbcGoodsConfigDO config = insertGoodsConfig("废钢", "吨", "0.01", "SIMPLE");
        Long firstOrder = createPurchaseOrder(payee.getId(), config.getId(), "100", "2000");
        Long secondOrder = createPurchaseOrder(payee.getId(), config.getId(), "50", "2000");
        Long secondItem = purchaseOrderService.getDetail(secondOrder).getItems().get(0).getId();

        AcquisitionCreateReqVO reqVO = baseReq(payee.getId(), config.getId());
        reqVO.setQuantity(new BigDecimal("10"));
        reqVO.setUnitPrice(new BigDecimal("2000.00"));
        // 把第二张订单的明细挂到第一张订单上：明细不属于该订单，拒
        reqVO.setPurchaseOrderId(firstOrder);
        reqVO.setPurchaseOrderItemId(secondItem);

        assertServiceException(() -> acquisitionService.createAcquisition(reqVO),
                PURCHASE_ORDER_ITEM_NOT_EXISTS, secondItem);
    }

    @Test
    public void testCreateAcquisition_itemCategoryMismatchRejected() {
        PayeeInfoDO payee = insertPayee("张三", "13800138205");
        IcbcGoodsConfigDO steel = insertGoodsConfig("废钢", "吨", "0.01", "SIMPLE");
        IcbcGoodsConfigDO paper = insertGoodsConfig("废纸", "吨", "0.01", "GENERAL");
        Long orderId = createPurchaseOrder(payee.getId(), steel.getId(), "100", "2000");
        Long itemId = purchaseOrderService.getDetail(orderId).getItems().get(0).getId();

        // 采购的是废钢，本次收购是废纸：关联会让履约进度串品类，拒
        AcquisitionCreateReqVO reqVO = baseReq(payee.getId(), paper.getId());
        reqVO.setQuantity(new BigDecimal("10"));
        reqVO.setUnitPrice(new BigDecimal("2000.00"));
        reqVO.setPurchaseOrderId(orderId);
        reqVO.setPurchaseOrderItemId(itemId);

        assertServiceException(() -> acquisitionService.createAcquisition(reqVO),
                ACQUISITION_PURCHASE_ITEM_CATEGORY_MISMATCH, "废钢");
    }

    @Test
    public void testCreateAcquisition_orderCounterpartyMismatchRejected() {
        PayeeInfoDO payee = insertPayee("张三", "13800138209");
        PayeeInfoDO other = insertPayee("李四", "13800138210");
        IcbcGoodsConfigDO config = insertGoodsConfig("废钢", "吨", "0.01", "SIMPLE");
        Long otherOrder = createPurchaseOrder(other.getId(), config.getId(), "100", "2000");
        Long itemId = purchaseOrderService.getDetail(otherOrder).getItems().get(0).getId();

        // 拿别人的订单来挂自己的收购：交易对方不是同一主体，拒
        AcquisitionCreateReqVO reqVO = baseReq(payee.getId(), config.getId());
        reqVO.setQuantity(new BigDecimal("10"));
        reqVO.setUnitPrice(new BigDecimal("2000.00"));
        reqVO.setPurchaseOrderId(otherOrder);
        reqVO.setPurchaseOrderItemId(itemId);

        assertServiceException(() -> acquisitionService.createAcquisition(reqVO),
                ACQUISITION_PURCHASE_ORDER_COUNTERPARTY_MISMATCH, "李四");
    }

    @Test
    public void testCreateAcquisition_itemWithoutOrderRejected() {
        PayeeInfoDO payee = insertPayee("张三", "13800138206");
        IcbcGoodsConfigDO config = insertGoodsConfig("废钢", "吨", "0.01", "SIMPLE");

        AcquisitionCreateReqVO reqVO = baseReq(payee.getId(), config.getId());
        reqVO.setQuantity(new BigDecimal("10"));
        reqVO.setUnitPrice(new BigDecimal("2000.00"));
        // 只给明细不给订单：成对约束破了
        reqVO.setPurchaseOrderItemId(999L);

        assertServiceException(() -> acquisitionService.createAcquisition(reqVO),
                ACQUISITION_PURCHASE_ARRANGEMENT_INCOMPLETE);
    }

    @Test
    public void testCreateAcquisition_sameBatchSameSellerAndStation() {
        PayeeInfoDO payee = insertPayee("张三", "13800138207");
        IcbcGoodsConfigDO steel = insertGoodsConfig("废钢", "吨", "0.01", "SIMPLE");
        IcbcGoodsConfigDO paper = insertGoodsConfig("废纸", "吨", "0.01", "GENERAL");
        IcbcStationDO station = insertStation("朝阳回收站");
        Long batchId = createBatchAtStation(payee.getId(), station.getId(), "京A12345");
        addWeighing(batchId, "18000", "5500", "WD-AM");

        // AC1 + AC5：同一交接批次下多张收购单，同出售者 + 同场站（一次混装按品类拆）
        Long first = acquisitionService.createAcquisition(
                fromBatch(baseReq(payee.getId(), steel.getId()), batchId)).getId();
        Long second = acquisitionService.createAcquisition(
                fromBatch(baseReq(payee.getId(), paper.getId()), batchId)).getId();

        IcbcAcquisitionDO firstSaved = acquisitionMapper.selectById(first);
        IcbcAcquisitionDO secondSaved = acquisitionMapper.selectById(second);
        assertEquals(batchId, firstSaved.getHandoverBatchId());
        assertEquals(batchId, secondSaved.getHandoverBatchId());
        assertEquals(payee.getId(), firstSaved.getPayeeId());
        assertEquals(payee.getId(), secondSaved.getPayeeId());
        assertEquals(station.getId(), firstSaved.getStationId());
        assertEquals(station.getId(), secondSaved.getStationId());
        assertEquals(2L, handoverBatchService.countAcquisitions(batchId));
    }

    @Test
    public void testGetAcquisitionPage_filterByDirectAcquisition() {
        PayeeInfoDO payee = insertPayee("张三", "13800138208");
        IcbcGoodsConfigDO config = insertGoodsConfig("废钢", "吨", "0.01", "SIMPLE");
        Long orderId = createPurchaseOrder(payee.getId(), config.getId(), "100", "2000");
        Long itemId = purchaseOrderService.getDetail(orderId).getItems().get(0).getId();

        // 一张挂了采购安排、一张不挂
        AcquisitionCreateReqVO linked = baseReq(payee.getId(), config.getId());
        linked.setQuantity(new BigDecimal("10"));
        linked.setUnitPrice(new BigDecimal("2000.00"));
        linked.setPurchaseOrderId(orderId);
        linked.setPurchaseOrderItemId(itemId);
        acquisitionService.createAcquisition(linked);

        AcquisitionCreateReqVO direct = baseReq(payee.getId(), config.getId());
        direct.setQuantity(new BigDecimal("5"));
        direct.setAmount(new BigDecimal("500.00"));
        acquisitionService.createAcquisition(direct);

        AcquisitionPageReqVO directQuery = new AcquisitionPageReqVO();
        directQuery.setPageNo(1);
        directQuery.setPageSize(10);
        directQuery.setDirectAcquisition(true);
        assertEquals(1, acquisitionService.getAcquisitionPage(directQuery).getTotal());

        AcquisitionPageReqVO linkedQuery = new AcquisitionPageReqVO();
        linkedQuery.setPageNo(1);
        linkedQuery.setPageSize(10);
        linkedQuery.setDirectAcquisition(false);
        assertEquals(1, acquisitionService.getAcquisitionPage(linkedQuery).getTotal());
    }

    // ==================== 造数 ====================

    // ==================== 接收结论与称量差异（#53 T15，ADR 0028） ====================

    /**
     * 造一张带扣杂的收购单：毛 18000 − 皮 5500 = 净 12500，扣杂 500 → 结算 12000，单价 2 → 应付 24000。
     */
    private Long createAcquisitionForAcceptance(PayeeInfoDO payee, IcbcGoodsConfigDO config) {
        AcquisitionCreateReqVO reqVO = baseReq(payee.getId(), config.getId());
        reqVO.setQuantity(new BigDecimal("5"));
        reqVO.setGrossWeight(new BigDecimal("18000.00"));
        reqVO.setTareWeight(new BigDecimal("5500.00"));
        reqVO.setDeduction(new BigDecimal("500.00"));
        reqVO.setDeductionMethod("WEIGHT");
        reqVO.setUnitPrice(new BigDecimal("2.00"));
        return acquisitionService.createAcquisition(reqVO).getId();
    }

    private AcquisitionAcceptanceReqVO acceptanceReq(Long id, String accepted, String rejected, String residual) {
        AcquisitionAcceptanceReqVO reqVO = new AcquisitionAcceptanceReqVO();
        reqVO.setId(id);
        reqVO.setAcceptedWeight(accepted == null ? null : new BigDecimal(accepted));
        reqVO.setRejectedWeight(rejected == null ? null : new BigDecimal(rejected));
        reqVO.setResidualWeight(residual == null ? null : new BigDecimal(residual));
        return reqVO;
    }

    @Test
    public void testRecordAcceptance_partialReceipt_recomputesPayableAndDiff() {
        PayeeInfoDO payee = insertPayee("乙一", "13800138200");
        IcbcGoodsConfigDO config = insertGoodsConfig("废钢", "吨", "0.01", "GENERAL");
        Long id = createAcquisitionForAcceptance(payee, config);
        assertEquals(0, new BigDecimal("24000.00").compareTo(acquisitionMapper.selectById(id).getAmount()));

        // 部分接收：接收 11500，退回 500（质量），余货出场 500，三者合计 = 净重 12500
        AcquisitionAcceptanceReqVO reqVO = acceptanceReq(id, "11500", "500", "500");
        reqVO.setRejectReason("含水率超标，杂质过多");
        acquisitionService.recordAcceptance(reqVO);

        IcbcAcquisitionDO saved = acquisitionMapper.selectById(id);
        assertEquals(0, new BigDecimal("11500").compareTo(saved.getAcceptedWeight()));
        assertEquals(0, new BigDecimal("500").compareTo(saved.getRejectedWeight()));
        assertEquals(0, new BigDecimal("500").compareTo(saved.getResidualWeight()));
        assertEquals("含水率超标，杂质过多", saved.getRejectReason());
        // 拒收部分不进应付：应付量 = 12000 − 500 − 500 = 11000，金额 = 11000 × 2 = 22000
        assertEquals(0, new BigDecimal("22000.00").compareTo(saved.getAmount()));
        // 称量差异 = 实物量（接收量优先）− 结算重量 = 11500 − 12000 = -500
        assertEquals(0, new BigDecimal("-500").compareTo(saved.getWeightDiff()));
    }

    @Test
    public void testRecordAcceptance_fullReject_payableZeroButStillTraceable() {
        PayeeInfoDO payee = insertPayee("乙二", "13800138201");
        IcbcGoodsConfigDO config = insertGoodsConfig("废钢", "吨", "0.01", "GENERAL");
        Long id = createAcquisitionForAcceptance(payee, config);

        AcquisitionAcceptanceReqVO reqVO = acceptanceReq(id, "0", "12500", null);
        reqVO.setRejectReason("整车不合格，全部拒收");
        acquisitionService.recordAcceptance(reqVO);

        IcbcAcquisitionDO saved = acquisitionMapper.selectById(id);
        // 全部拒收：应付为 0（不进应付），但单据、接收量与拒收原因仍在（可追溯，不进库存）
        assertEquals(0, BigDecimal.ZERO.compareTo(saved.getAmount()));
        assertEquals(0, BigDecimal.ZERO.compareTo(saved.getAcceptedWeight()));
        assertEquals(0, new BigDecimal("12500").compareTo(saved.getRejectedWeight()));
        assertEquals("整车不合格，全部拒收", saved.getRejectReason());
        assertEquals(0, new BigDecimal("-12000").compareTo(saved.getWeightDiff()));
    }

    @Test
    public void testRecordAcceptance_rejectWithoutReasonRejected() {
        PayeeInfoDO payee = insertPayee("乙三", "13800138202");
        IcbcGoodsConfigDO config = insertGoodsConfig("废钢", "吨", "0.01", "GENERAL");
        Long id = createAcquisitionForAcceptance(payee, config);

        assertServiceException(() -> acquisitionService.recordAcceptance(acceptanceReq(id, "12000", "500", null)),
                ACQUISITION_REJECT_REASON_REQUIRED);
    }

    @Test
    public void testRecordAcceptance_exceedNetWeightRejected() {
        PayeeInfoDO payee = insertPayee("乙四", "13800138203");
        IcbcGoodsConfigDO config = insertGoodsConfig("废钢", "吨", "0.01", "GENERAL");
        Long id = createAcquisitionForAcceptance(payee, config);

        // 接收 12500 + 退回 500 = 13000 > 净重 12500：同一车货不能重复分配
        AcquisitionAcceptanceReqVO reqVO = acceptanceReq(id, "12500", "500", null);
        reqVO.setRejectReason("部分不合格");
        assertServiceException(() -> acquisitionService.recordAcceptance(reqVO),
                ACQUISITION_ACCEPTANCE_EXCEED_NET_WEIGHT, "13000", "12500");
    }

    @Test
    public void testRecordAcceptance_negativeWeightRejected() {
        PayeeInfoDO payee = insertPayee("乙五", "13800138204");
        IcbcGoodsConfigDO config = insertGoodsConfig("废钢", "吨", "0.01", "GENERAL");
        Long id = createAcquisitionForAcceptance(payee, config);

        AcquisitionAcceptanceReqVO reqVO = acceptanceReq(id, "12000", "-100", null);
        reqVO.setRejectReason("手误");
        assertServiceException(() -> acquisitionService.recordAcceptance(reqVO),
                ACQUISITION_ACCEPTANCE_WEIGHT_INVALID);
    }

    @Test
    public void testRecordAcceptance_afterInvoiceLinkedRejected() {
        PayeeInfoDO payee = insertPayee("乙六", "13800138205");
        IcbcGoodsConfigDO config = insertGoodsConfig("废钢", "吨", "0.01", "GENERAL");
        Long id = createAcquisitionForAcceptance(payee, config);
        // 已挂开票申请：金额口径已固定，不能再改接收结论
        acquisitionService.linkInvoice(id, "ORDER_ACCEPT_1");

        assertServiceException(() -> acquisitionService.recordAcceptance(acceptanceReq(id, "12000", "500", null)),
                ACQUISITION_ACCEPTANCE_AFTER_INVOICE_LINKED);
    }

    @Test
    public void testRecordAcceptance_afterSettlementGroupedRejected() {
        PayeeInfoDO payee = insertPayee("乙十", "13800138209");
        IcbcGoodsConfigDO config = insertGoodsConfig("废钢", "吨", "0.01", "GENERAL");
        Long id = createAcquisitionForAcceptance(payee, config);
        // 已归入结算单：结算版本已快照，不能让接收结论再静默改金额（#33）
        IcbcAcquisitionDO grouped = new IcbcAcquisitionDO();
        grouped.setId(id);
        grouped.setSettlementId(9001L);
        acquisitionMapper.updateById(grouped);

        assertServiceException(() -> acquisitionService.recordAcceptance(acceptanceReq(id, "12000", null, null)),
                ACQUISITION_ACCEPTANCE_AFTER_SETTLEMENT);
    }

    @Test
    public void testWeightDiff_registeredByDeduction_notSilentlyZeroed() {
        PayeeInfoDO payee = insertPayee("乙七", "13800138206");
        IcbcGoodsConfigDO config = insertGoodsConfig("废钢", "吨", "0.01", "GENERAL");
        Long id = createAcquisitionForAcceptance(payee, config);

        // 未做接收结论时实物量取净重：差异 = 12500 − 12000 = 500（扣杂），不静默抹平成 0
        IcbcAcquisitionDO saved = acquisitionMapper.selectById(id);
        assertEquals(0, new BigDecimal("500").compareTo(saved.getWeightDiff()));
        assertNull(saved.getAcceptedWeight());
    }

    @Test
    public void testCorrectRecognition_recomputesWeightDiff() {
        PayeeInfoDO payee = insertPayee("乙八", "13800138207");
        IcbcGoodsConfigDO config = insertGoodsConfig("废钢", "吨", "0.01", "GENERAL");
        AcquisitionCreateReqVO reqVO = baseReq(payee.getId(), config.getId());
        reqVO.setQuantity(new BigDecimal("5"));
        reqVO.setGrossWeight(new BigDecimal("18000.00"));
        reqVO.setTareWeight(new BigDecimal("5500.00"));
        reqVO.setUnitPrice(new BigDecimal("2.00"));
        Long id = acquisitionService.createAcquisition(reqVO).getId();
        // 无扣杂：结算 = 净重，差异为 0
        assertEquals(0, BigDecimal.ZERO.compareTo(acquisitionMapper.selectById(id).getWeightDiff()));

        AcquisitionCorrectionReqVO correction = new AcquisitionCorrectionReqVO();
        correction.setId(id);
        correction.setDeduction(new BigDecimal("500.00"));
        correction.setDeductionMethod("WEIGHT");
        acquisitionService.correctRecognition(correction);

        assertEquals(0, new BigDecimal("500").compareTo(acquisitionMapper.selectById(id).getWeightDiff()));
    }

    @Test
    public void testGetWeightDiffPage_filtersHasDifferenceAndAccepted() {
        PayeeInfoDO payee = insertPayee("乙九", "13800138208");
        IcbcGoodsConfigDO config = insertGoodsConfig("废钢", "吨", "0.01", "GENERAL");
        Long id = createAcquisitionForAcceptance(payee, config);

        // 未做接收结论时也能按差异看到（差异 500）
        AcquisitionWeightDiffPageReqVO hasDiff = new AcquisitionWeightDiffPageReqVO();
        hasDiff.setPayeeId(payee.getId());
        hasDiff.setHasDifference(true);
        assertEquals(1, acquisitionService.getWeightDiffPage(hasDiff).getTotal());

        // onlyAccepted 只看已做接收结论的：现在还没有
        AcquisitionWeightDiffPageReqVO onlyAccepted = new AcquisitionWeightDiffPageReqVO();
        onlyAccepted.setPayeeId(payee.getId());
        onlyAccepted.setOnlyAccepted(true);
        assertEquals(0, acquisitionService.getWeightDiffPage(onlyAccepted).getTotal());

        // 做接收结论（实物量 = 结算重量，差异归零）后：onlyAccepted 看得到，hasDifference 看不到
        AcquisitionAcceptanceReqVO acceptance = acceptanceReq(id, "12000", null, null);
        acquisitionService.recordAcceptance(acceptance);
        assertEquals(1, acquisitionService.getWeightDiffPage(onlyAccepted).getTotal());
        assertEquals(0, acquisitionService.getWeightDiffPage(hasDiff).getTotal());
    }

    // ==================== 测试辅助 ====================

    /**
     * 造一张可作采购依据的采购订单（执行中、未过期），返回订单编号。
     */
    private Long createPurchaseOrder(Long payeeId, Long goodsConfigId, String quantity, String unitPrice) {
        return createPurchaseOrder(payeeId, goodsConfigId, quantity, unitPrice,
                LocalDate.now().minusDays(1), LocalDate.now().plusDays(30), true);
    }

    private Long createPurchaseOrder(Long payeeId, Long goodsConfigId, String quantity, String unitPrice,
                                     boolean executing) {
        return createPurchaseOrder(payeeId, goodsConfigId, quantity, unitPrice,
                LocalDate.now().minusDays(1), LocalDate.now().plusDays(30), executing);
    }

    private Long createPurchaseOrder(Long payeeId, Long goodsConfigId, String quantity, String unitPrice,
                                     LocalDate startDate, LocalDate endDate) {
        return createPurchaseOrder(payeeId, goodsConfigId, quantity, unitPrice, startDate, endDate, true);
    }

    private Long createPurchaseOrder(Long payeeId, Long goodsConfigId, String quantity, String unitPrice,
                                     LocalDate startDate, LocalDate endDate, boolean executing) {
        PurchaseOrderSaveReqVO reqVO = new PurchaseOrderSaveReqVO();
        reqVO.setCounterpartyType(SellerSubjectTypeEnum.NATURAL.getType());
        reqVO.setPayeeId(payeeId);
        reqVO.setStartDate(startDate);
        reqVO.setEndDate(endDate);
        PurchaseOrderItemReqVO item = new PurchaseOrderItemReqVO();
        item.setGoodsConfigId(goodsConfigId);
        item.setQuantity(new BigDecimal(quantity));
        item.setPriceMode(PurchaseOrderPriceModeEnum.FIXED.getMode());
        item.setUnitPrice(new BigDecimal(unitPrice));
        reqVO.setItems(List.of(item));
        Long orderId = purchaseOrderService.createOrder(reqVO);
        if (executing) {
            PurchaseOrderStatusUpdateReqVO status = new PurchaseOrderStatusUpdateReqVO();
            status.setId(orderId);
            status.setStatus(PurchaseOrderStatusEnum.EXECUTING.getStatus());
            purchaseOrderService.updateStatus(status);
        }
        return orderId;
    }

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

    private void insertInvoiceOrder(String partnerOrderId, Long acquisitionId, Integer preInvoiceStatus,
                                    Integer paymentStatus, Integer invoiceStatus) {
        invoiceOrderMapper.insert(InvoiceOrderDO.builder()
                .orderNo("INV_" + partnerOrderId)
                .partnerOrderId(partnerOrderId)
                .acquisitionId(acquisitionId)
                .totalAmount(new BigDecimal("500.00"))
                .orderStatus(0)
                .invoiceStatus(invoiceStatus)
                .paymentStatus(paymentStatus)
                .taxStatus(0)
                .confirmStatus(0)
                .preInvoiceStatus(preInvoiceStatus)
                .build());
    }

    /**
     * 把开票单推到指定状态并触发一次进度收敛——走的就是回调那条路
     * （{@code AcquisitionProgressService#syncByPartnerOrderId}）。
     */
    private void syncOrderState(String partnerOrderId, Integer preInvoiceStatus,
                               Integer paymentStatus, Integer invoiceStatus) {
        InvoiceOrderDO update = new InvoiceOrderDO();
        update.setId(invoiceOrderMapper.selectByPartnerOrderId(partnerOrderId).getId());
        update.setPreInvoiceStatus(preInvoiceStatus);
        update.setPaymentStatus(paymentStatus);
        update.setInvoiceStatus(invoiceStatus);
        invoiceOrderMapper.updateById(update);
        acquisitionProgressService.syncByPartnerOrderId(partnerOrderId);
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


    // ==================== 交接登记 → 回场复磅 → 收购单（V6 #73） ====================

    @Test
    public void testCreateAcquisition_fromHandoverBatch_defaultsPriceToReference_andQuantityToSettlementWeight() {
        PayeeInfoDO payee = insertPayee("张三", "13800138000");
        IcbcGoodsConfigDO config = insertGoodsConfig("废钢", "吨", "0.01", "SIMPLE");
        Long batchId = createHandoverBatch(payee.getId(), "京A12345", "12.5", "2600.00",
                AcquisitionDocumentStatusEnum.COMPLETE.getStatus(), null);
        addWeighing(batchId, "18000", "5500", "WD-1");

        // 磅房只选现场交接登记、过磅，不重录价格与数量
        AcquisitionCreateReqVO reqVO = baseReq(payee.getId(), config.getId());
        reqVO.setHandoverBatchId(batchId);
        reqVO.setWeightTicketNo(null);

        IcbcAcquisitionDO saved = acquisitionMapper.selectById(
                acquisitionService.createAcquisition(reqVO).getId());

        // 单价取现场参考价；计量取有效磅次（净重 12500）
        assertEquals(0, new BigDecimal("2600.00").compareTo(saved.getUnitPrice()));
        assertEquals(0, new BigDecimal("12500.0000").compareTo(saved.getSettlementWeight()));
        assertEquals(0, new BigDecimal("32500000.00").compareTo(saved.getAmount()));
        // 没给数量时数量取结算重量（数量只是展示与发票明细字段），不是现场参考量
        assertEquals(0, new BigDecimal("12500.0000").compareTo(saved.getQuantity()));
        // 现场参考值留档，来源与要件状态一并落下来
        assertEquals(0, new BigDecimal("12.5").compareTo(saved.getReferenceQuantity()));
        assertEquals(0, new BigDecimal("2600.00").compareTo(saved.getReferenceUnitPrice()));
        assertNotNull(saved.getLogisticsHandoverId());
        assertEquals(AcquisitionDocumentStatusEnum.COMPLETE.getStatus(), saved.getDocumentStatus());
        assertFalse(saved.isDocumentPending());
    }

    @Test
    public void testCreateAcquisition_fixReferencePriceOrQuantity_requiresReason() {
        PayeeInfoDO payee = insertPayee("张三", "13800138000");
        IcbcGoodsConfigDO config = insertGoodsConfig("废钢", "吨", "0.01", "SIMPLE");
        Long batchId = createHandoverBatch(payee.getId(), "京A12345", "12.5", "2600.00",
                AcquisitionDocumentStatusEnum.COMPLETE.getStatus(), null);
        addWeighing(batchId, "18000", "5500", "WD-1");

        // 修正单价（2600 → 2500）却不给原因：拦住
        AcquisitionCreateReqVO fixPrice = baseReq(payee.getId(), config.getId());
        fixPrice.setHandoverBatchId(batchId);
        fixPrice.setUnitPrice(new BigDecimal("2500.00"));
        assertServiceException(() -> acquisitionService.createAcquisition(fixPrice),
                ACQUISITION_REFERENCE_FIX_REASON_REQUIRED);

        // 修正现场参考量（12.5 → 10）却不给原因：同样拦住
        AcquisitionCreateReqVO fixQuantity = baseReq(payee.getId(), config.getId());
        fixQuantity.setHandoverBatchId(batchId);
        fixQuantity.setQuantity(new BigDecimal("10"));
        assertServiceException(() -> acquisitionService.createAcquisition(fixQuantity),
                ACQUISITION_REFERENCE_FIX_REASON_REQUIRED);

        // 带上原因就放行，并且留痕
        fixPrice.setReferenceFixReason("现场复磅后杂质比目测多，按实际谈定单价");
        IcbcAcquisitionDO saved = acquisitionMapper.selectById(
                acquisitionService.createAcquisition(fixPrice).getId());
        assertEquals(0, new BigDecimal("2500.00").compareTo(saved.getUnitPrice()));
        assertEquals("现场复磅后杂质比目测多，按实际谈定单价", saved.getReferenceFixReason());
        assertEquals(0, new BigDecimal("2600.00").compareTo(saved.getReferenceUnitPrice()),
                "参考价原值仍要留档：改的是什么、改成了什么都要看得见");
    }

    @Test
    public void testCreateAcquisition_pendingDocumentsInherited_andCompleteDocumentsReleases() {
        PayeeInfoDO payee = insertPayee("张三", "13800138000");
        IcbcGoodsConfigDO config = insertGoodsConfig("废钢", "吨", "0.01", "SIMPLE");
        Long batchId = createHandoverBatch(payee.getId(), "京A12345", "12.5", "2600.00",
                AcquisitionDocumentStatusEnum.PENDING.getStatus(), "缺身份证");
        addWeighing(batchId, "18000", "5500", "WD-1");

        AcquisitionCreateReqVO reqVO = baseReq(payee.getId(), config.getId());
        reqVO.setHandoverBatchId(batchId);
        Long id = acquisitionService.createAcquisition(reqVO).getId();

        // 缺要件照记事实，但状态是待补档
        IcbcAcquisitionDO pending = acquisitionMapper.selectById(id);
        assertTrue(pending.isDocumentPending());
        assertEquals("缺身份证", pending.getDocumentGap());

        // 补档放行：留办理人与时间
        AcquisitionCompleteDocumentsReqVO complete = new AcquisitionCompleteDocumentsReqVO();
        complete.setId(id);
        complete.setRemark("身份证与银行卡已补齐并核验");
        acquisitionService.completeDocuments(complete);

        IcbcAcquisitionDO released = acquisitionMapper.selectById(id);
        assertFalse(released.isDocumentPending());
        assertNotNull(released.getDocumentCompletedAt());
        assertEquals("身份证与银行卡已补齐并核验", released.getDocumentCompleteRemark());
        assertEquals("缺身份证", released.getDocumentGap(), "当时缺什么要留档，不被补档抹掉");

        // 已齐的单再补一次是操作错误，不是幂等成功
        assertServiceException(() -> acquisitionService.completeDocuments(complete),
                ACQUISITION_DOCUMENT_NOT_PENDING);
    }

    @Test
    public void testCreateAcquisition_multiStopHandovers_staySeparatePerSellerAndStation() {
        IcbcGoodsConfigDO config = insertGoodsConfig("废钢", "吨", "0.01", "SIMPLE");
        PayeeInfoDO sellerA = insertPayee("张三", "13800138000");
        PayeeInfoDO sellerB = insertPayee("李四", "13800138001");
        IcbcStationDO station = insertStation("城东收货点");
        // 一车提两家：各自一次现场交接、各自一个批次
        Long batchA = createHandoverBatch(sellerA.getId(), "京A12345", "12.5", "2600.00",
                AcquisitionDocumentStatusEnum.COMPLETE.getStatus(), null, station.getId());
        Long batchB = createHandoverBatch(sellerB.getId(), "京A12345", "8", "1200.00",
                AcquisitionDocumentStatusEnum.COMPLETE.getStatus(), null, station.getId());
        addWeighing(batchA, "18000", "5500", "WD-A");
        addWeighing(batchB, "9000", "1000", "WD-B");

        AcquisitionCreateReqVO reqA = baseReq(sellerA.getId(), config.getId());
        reqA.setHandoverBatchId(batchA);
        AcquisitionCreateReqVO reqB = baseReq(sellerB.getId(), config.getId());
        reqB.setHandoverBatchId(batchB);

        IcbcAcquisitionDO savedA = acquisitionMapper.selectById(
                acquisitionService.createAcquisition(reqA).getId());
        IcbcAcquisitionDO savedB = acquisitionMapper.selectById(
                acquisitionService.createAcquisition(reqB).getId());

        // 两家各自一张单、各自一个批次、各自的重量与单价：一次集货不构成合并结算的依据（ADR 0031）
        assertNotEquals(savedA.getId(), savedB.getId());
        assertEquals(batchA, savedA.getHandoverBatchId());
        assertEquals(batchB, savedB.getHandoverBatchId());
        assertEquals(0, new BigDecimal("12500.0000").compareTo(savedA.getNetWeight()));
        assertEquals(0, new BigDecimal("8000.0000").compareTo(savedB.getNetWeight()));
        assertEquals(sellerA.getId(), savedA.getPayeeId());
        assertEquals(sellerB.getId(), savedB.getPayeeId());
        // 收购单与结算单归**派单场站**（ADR 0031）
        assertEquals(station.getId(), savedA.getStationId());
        assertEquals(station.getId(), savedB.getStationId());
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
     * 造一个「按现场交接登记回场复磅」出来的批次：带 logisticsHandoverId、现场参考值与要件状态。
     *
     * <p>不走 `intakeFromHandover` 是因为 icbc 的单测上下文用空实现的物流读取面（Stub），
     * 这里直接落库，把被测对象收敛到收购单本身；intake 的行为由 HandoverBatchServiceTest 钉。
     */
    private Long createHandoverBatch(Long payeeId, String plateNo, String referenceQuantity,
                                     String referenceUnitPrice, String documentStatus, String documentGap) {
        return createHandoverBatch(payeeId, plateNo, referenceQuantity, referenceUnitPrice,
                documentStatus, documentGap, null);
    }

    private Long createHandoverBatch(Long payeeId, String plateNo, String referenceQuantity,
                                     String referenceUnitPrice, String documentStatus, String documentGap,
                                     Long stationId) {
        IcbcHandoverBatchDO batch = IcbcHandoverBatchDO.builder()
                .batchNo("HB" + System.nanoTime())
                .logisticsHandoverId(System.nanoTime() % 1000000 + 1)
                .payeeId(payeeId)
                .sellerName("张三")
                .stationId(stationId)
                .visitAddress("某某路 1 号")
                .occurTime(LocalDateTime.now())
                .sourceType(HandoverSourceTypeEnum.ON_SITE.getType())
                .driverId(77L)
                .driverName("李师傅")
                .vehicleId(88L)
                .plateNo(plateNo)
                .documentStatus(documentStatus)
                .documentGap(documentGap)
                .referenceQuantity(new BigDecimal(referenceQuantity))
                .referenceUnitPrice(new BigDecimal(referenceUnitPrice))
                .build();
        handoverBatchMapper.insert(batch);
        return batch.getId();
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

    /** 到场收货：批次挂场站，收购单因此带上同场站（#51 AC1）。 */
    private Long createBatchAtStation(Long payeeId, Long stationId, String plateNo) {
        HandoverBatchCreateReqVO reqVO = new HandoverBatchCreateReqVO();
        reqVO.setPayeeId(payeeId);
        reqVO.setStationId(stationId);
        reqVO.setPlateNo(plateNo);
        reqVO.setSourceType(HandoverSourceTypeEnum.WALK_IN.getType());
        return handoverBatchService.createBatch(reqVO);
    }

    private IcbcStationDO insertStation(String name) {
        IcbcStationDO station = IcbcStationDO.builder()
                .stationCode("ST_" + name)
                .name(name)
                .openStatus(1)
                .build();
        stationMapper.insert(station);
        return station;
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
