package cn.iocoder.yudao.module.icbc.service.report;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.erp.api.stock.StockReportApi;
import cn.iocoder.yudao.module.erp.api.stock.dto.StockBalanceRespDTO;
import cn.iocoder.yudao.module.erp.api.stock.dto.StockRecordRespDTO;
import cn.iocoder.yudao.module.erp.api.stock.dto.StockReportQueryDTO;
import cn.iocoder.yudao.module.icbc.UnitTestConfiguration;
import cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo.PurchaseOrderProgressRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.report.vo.ReportAcquisitionLedgerPageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.report.vo.ReportAcquisitionLedgerRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.report.vo.ReportAnomalyPageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.report.vo.ReportAnomalyRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.report.vo.ReportPurchasePerformancePageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.report.vo.ReportPurchasePerformanceRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.report.vo.ReportSettlementPaymentPageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.report.vo.ReportSettlementPaymentRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.report.vo.ReportStockBalancePageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.report.vo.ReportStockBalanceRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.report.vo.ReportStockRecordPageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.report.vo.ReportStockRecordRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.report.vo.ReportTableRespVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.acquisition.IcbcAcquisitionDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.appointment.IcbcAppointmentDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.goodscfg.IcbcGoodsConfigDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payment.PaymentOrderDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.purchaseorder.IcbcPurchaseOrderDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.settlement.IcbcSettlementDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.station.IcbcStationDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.stockin.IcbcStockInDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.acquisition.IcbcAcquisitionMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.appointment.IcbcAppointmentMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.goodscfg.IcbcGoodsConfigMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.payment.PaymentOrderMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.purchaseorder.IcbcPurchaseOrderMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.settlement.IcbcSettlementMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.station.IcbcStationMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.stockin.IcbcStockInMapper;
import cn.iocoder.yudao.module.icbc.enums.AcquisitionStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.PaymentStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.ReportAnomalyTypeEnum;
import cn.iocoder.yudao.module.icbc.enums.ReportTableEnum;
import cn.iocoder.yudao.module.icbc.enums.SettlementConfirmStatusEnum;
import cn.iocoder.yudao.module.icbc.service.purchaseorder.PurchaseOrderService;
import cn.iocoder.yudao.module.icbc.service.report.impl.ReportServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * {@link ReportServiceImpl} 的单元测试（#57 T19）。
 *
 * <p>守四件事：每张表的数字都指得到来源单据；采购履约与异常表**消费**别票的口径而不是重算；
 * 实际收购量不混入预约约量与采购计划量（AC6）；异常表未知类型要报错。
 */
@Import({ReportServiceImpl.class, UnitTestConfiguration.class})
@Sql(scripts = "/sql/create_tables.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Transactional
@Rollback
public class ReportServiceTest extends BaseDbUnitTest {

    /** #47 的履约口径通过 Service 消费；本测试只关心报表如何映射，用 Mock。 */
    @MockBean
    private PurchaseOrderService purchaseOrderService;
    /** 库存域只通过 erp-api 的只读端口接入，单元测试不跨模块，用 Mock。 */
    @MockBean
    private StockReportApi stockReportApi;

    @Resource
    private ReportService reportService;
    @Resource
    private IcbcAcquisitionMapper acquisitionMapper;
    @Resource
    private IcbcPurchaseOrderMapper purchaseOrderMapper;
    @Resource
    private IcbcSettlementMapper settlementMapper;
    @Resource
    private PaymentOrderMapper paymentOrderMapper;
    @Resource
    private IcbcStockInMapper stockInMapper;
    @Resource
    private IcbcStationMapper stationMapper;
    @Resource
    private IcbcGoodsConfigMapper goodsConfigMapper;
    @Resource
    private IcbcAppointmentMapper appointmentMapper;

    // ==================== 表清单与口径 ====================

    @Test
    public void testTables_listsFiveTablesEachWithDefinition() {
        List<ReportTableRespVO> tables = reportService.getTables();

        assertEquals(5, tables.size());
        assertEquals(java.util.Arrays.stream(ReportTableEnum.values()).map(ReportTableEnum::getCode)
                        .collect(Collectors.toList()),
                tables.stream().map(vo -> vo.getCode()).collect(Collectors.toList()));
        for (ReportTableRespVO table : tables) {
            assertNotNull(table.getName());
            assertTrue(table.getDefinition() != null && !table.getDefinition().isEmpty(),
                    "每张表都要带口径：" + table.getCode());
        }
    }

    // ==================== 一、采购履约表 ====================

    @Test
    public void testPurchasePerformance_consumesProgressMeasures() {
        Long orderId = insertOrder("PO_1", "张三", 1, LocalDate.now().minusDays(1));
        // 报表不重算五口径：getProgress 返回什么就展示什么
        PurchaseOrderProgressRespVO progress = new PurchaseOrderProgressRespVO();
        progress.setOrderId(orderId);
        progress.setOrderNo("PO_1");
        progress.setStatusName("执行中");
        progress.setPlanQuantity(new BigDecimal("100"));
        progress.setAcceptedQuantity(new BigDecimal("120"));
        progress.setStockedQuantity(new BigDecimal("80"));
        progress.setSettledQuantity(new BigDecimal("50"));
        progress.setUnperformedQuantity(new BigDecimal("-20"));
        progress.setCompletionBasis("ACCEPTED");
        progress.setCompletionBasisName("验收口径");
        progress.setCompletionBasisQuantity(new BigDecimal("120"));
        progress.setCompletionRatio(new BigDecimal("1.2000"));
        PurchaseOrderProgressRespVO.Anomaly over = new PurchaseOrderProgressRespVO.Anomaly();
        over.setCode("OVER_QUANTITY");
        over.setName("超量");
        PurchaseOrderProgressRespVO.Anomaly expired = new PurchaseOrderProgressRespVO.Anomaly();
        expired.setCode("EXPIRED_EXECUTING");
        expired.setName("过期");
        progress.setAnomalies(List.of(over, expired));
        when(purchaseOrderService.getProgress(orderId)).thenReturn(progress);

        ReportPurchasePerformancePageReqVO reqVO = new ReportPurchasePerformancePageReqVO();
        PageResult<ReportPurchasePerformanceRespVO> page = reportService.getPurchasePerformancePage(reqVO);

        assertEquals(1L, page.getTotal());
        ReportPurchasePerformanceRespVO row = page.getList().get(0);
        assertEquals("PO_1", row.getOrderNo());
        assertEquals(new BigDecimal("100"), row.getPlanQuantity());
        assertEquals(new BigDecimal("120"), row.getPerformedQuantity());
        assertEquals(new BigDecimal("-20"), row.getBalanceQuantity());
        assertEquals(new BigDecimal("80"), row.getStockedQuantity());
        assertEquals("验收口径", row.getCompletionBasisName());
        assertTrue(row.getOverQuantity());
        assertTrue(row.getExpired());
        assertEquals(List.of("超量", "过期"), row.getAnomalyNames());
    }

    // ==================== 二、收购台账 ====================

    @Test
    public void testAcquisitionLedger_mapsWeightsAndExcludesCancelled() {
        Long stationId = insertStation("城东场站");
        insertLedgerAcquisition("ACQ_1", AcquisitionStatusEnum.REGISTERED.getStatus(), stationId,
                new BigDecimal("12"), new BigDecimal("2"), new BigDecimal("10"),
                new BigDecimal("9.5"), null, null, new BigDecimal("950.00"));
        insertLedgerAcquisition("ACQ_CANCELLED", AcquisitionStatusEnum.CANCELLED.getStatus(), stationId,
                new BigDecimal("12"), new BigDecimal("2"), new BigDecimal("10"), null, null, null, null);

        ReportAcquisitionLedgerPageReqVO reqVO = new ReportAcquisitionLedgerPageReqVO();
        PageResult<ReportAcquisitionLedgerRespVO> page = reportService.getAcquisitionLedgerPage(reqVO);

        assertEquals(1L, page.getTotal());
        ReportAcquisitionLedgerRespVO row = page.getList().get(0);
        assertEquals("ACQ_1", row.getAcquisitionNo());
        assertEquals("张三", row.getSellerName());
        assertEquals("城东场站", row.getStationName());
        assertEquals("废钢", row.getCategoryName());
        assertTrue(row.getDirectAcquisition());
        assertEquals("直接收购", row.getAcquisitionModeText());
        assertDecimal("12", row.getGrossWeight());
        assertDecimal("10", row.getNetWeight());
        assertDecimal("9.5", row.getSettlementWeight());
        assertDecimal("950.00", row.getAmount());
        assertEquals("已登记", row.getStatusName());
    }

    @Test
    public void testAcquisitionLedger_doesNotMixAppointmentOrPlanQuantity() {
        // 有预约约量、也有采购计划量，但一张收购单都没有 → 实际收购量为空，不拿计划 / 约量顶替
        insertAppointment("AP_1", new BigDecimal("88"));
        insertOrder("PO_ONLY_PLAN", "李四", 1, LocalDate.now().plusDays(3));

        PageResult<ReportAcquisitionLedgerRespVO> page =
                reportService.getAcquisitionLedgerPage(new ReportAcquisitionLedgerPageReqVO());

        assertEquals(0L, page.getTotal());
        assertTrue(page.getList().isEmpty());
    }

    // ==================== 三、库存表 ====================

    @Test
    public void testStockBalance_delegatesToErpAndComputesAge() {
        Long goodsConfigId = insertGoodsConfig("废钢");
        StockBalanceRespDTO dto = new StockBalanceRespDTO();
        dto.setId(1L);
        dto.setGoodsConfigId(goodsConfigId);
        dto.setWarehouseId(7L);
        dto.setWarehouseName("一号库");
        dto.setLocationId(0L);
        dto.setBatchId(9L);
        dto.setBatchNo("B20260101");
        dto.setBatchInTime(LocalDateTime.now().minusDays(3));
        dto.setCount(new BigDecimal("14.7400"));
        when(stockReportApi.getStockBalancePage(org.mockito.ArgumentMatchers.any(StockReportQueryDTO.class)))
                .thenReturn(new PageResult<>(List.of(dto), 1L));

        ReportStockBalancePageReqVO reqVO = new ReportStockBalancePageReqVO();
        PageResult<ReportStockBalanceRespVO> page = reportService.getStockBalancePage(reqVO);

        assertEquals(1L, page.getTotal());
        ReportStockBalanceRespVO row = page.getList().get(0);
        assertEquals("废钢", row.getCategoryName());
        assertEquals("一号库", row.getWarehouseName());
        assertEquals("B20260101", row.getBatchNo());
        assertEquals(new BigDecimal("14.7400"), row.getCount());
        assertEquals(3L, row.getAgeDays());
    }

    @Test
    public void testStockRecord_delegatesToErp() {
        Long goodsConfigId = insertGoodsConfig("废钢");
        StockRecordRespDTO dto = new StockRecordRespDTO();
        dto.setId(1L);
        dto.setGoodsConfigId(goodsConfigId);
        dto.setWarehouseName("一号库");
        dto.setCount(new BigDecimal("-2.0000"));
        dto.setTotalCount(new BigDecimal("12.7400"));
        dto.setBizTypeName("收货入库（作废）");
        dto.setBizNo("SI_1");
        dto.setCreateTime(LocalDateTime.now());
        when(stockReportApi.getStockRecordPage(org.mockito.ArgumentMatchers.any(StockReportQueryDTO.class)))
                .thenReturn(new PageResult<>(List.of(dto), 1L));

        PageResult<ReportStockRecordRespVO> page =
                reportService.getStockRecordPage(new ReportStockRecordPageReqVO());

        assertEquals(1L, page.getTotal());
        assertEquals("废钢", page.getList().get(0).getCategoryName());
        assertEquals(new BigDecimal("-2.0000"), page.getList().get(0).getCount());
        assertEquals("SI_1", page.getList().get(0).getBizNo());
    }

    // ==================== 四、结算付款表 ====================

    @Test
    public void testSettlementPayment_aggregatesAmountProgressAndReceipt() {
        Long settlementId = insertSettlement("ST_1", SettlementConfirmStatusEnum.CONFIRMED,
                LocalDateTime.now().minusDays(1));
        Long acq1 = insertLedgerAcquisition("ACQ_P1", AcquisitionStatusEnum.REGISTERED.getStatus(), 7L,
                null, null, new BigDecimal("10"), new BigDecimal("9.5"), settlementId, null,
                new BigDecimal("950.00"));
        Long acq2 = insertLedgerAcquisition("ACQ_P2", AcquisitionStatusEnum.REGISTERED.getStatus(), 7L,
                null, null, new BigDecimal("10"), new BigDecimal("9.5"), settlementId, null,
                new BigDecimal("1050.00"));
        insertPayment("PAY_1", acq1, PaymentStatusEnum.SUCCESS, "R001", null);
        insertPayment("PAY_2", acq2, PaymentStatusEnum.SUCCESS, "R002", null);

        PageResult<ReportSettlementPaymentRespVO> page =
                reportService.getSettlementPaymentPage(new ReportSettlementPaymentPageReqVO());

        assertEquals(1L, page.getTotal());
        ReportSettlementPaymentRespVO row = page.getList().get(0);
        assertEquals(new BigDecimal("2000.00"), row.getSettlementAmount());
        assertEquals(2, row.getAcquisitionCount());
        assertEquals(2, row.getPaidCount());
        assertEquals(0, row.getFailedCount());
        assertEquals("SUCCESS", row.getPaymentProgress());
        assertEquals("RECEIVED", row.getReceiptStatus());
        assertEquals("已确认", row.getConfirmStatusName());
        assertNotNull(row.getUnhandledHours());
    }

    @Test
    public void testSettlementPayment_reportsFailureAndMissingReceipt() {
        Long settlementId = insertSettlement("ST_FAIL", SettlementConfirmStatusEnum.PENDING,
                LocalDateTime.now().minusDays(3));
        Long acq1 = insertLedgerAcquisition("ACQ_F1", AcquisitionStatusEnum.REGISTERED.getStatus(), 7L,
                null, null, new BigDecimal("10"), new BigDecimal("9.5"), settlementId, null,
                new BigDecimal("950.00"));
        insertPayment("PAY_F1", acq1, PaymentStatusEnum.FAILED, null, "余额不足");

        PageResult<ReportSettlementPaymentRespVO> page =
                reportService.getSettlementPaymentPage(new ReportSettlementPaymentPageReqVO());

        ReportSettlementPaymentRespVO row = page.getList().get(0);
        assertEquals("FAILED", row.getPaymentProgress());
        assertEquals(1, row.getFailedCount());
        assertEquals("PENDING", row.getReceiptStatus());
        assertEquals("余额不足", row.getFailReason());
        assertTrue(row.getUnhandledHours() >= 72);
    }

    @Test
    public void testSettlementPayment_filtersByDerivedProgressWithCorrectTotal() {
        // 一张失败的、一张成功的：按派生字段筛选时 total 必须是全量命中数，不能只算当前页
        Long failedSettlement = insertSettlement("ST_F", SettlementConfirmStatusEnum.PENDING,
                LocalDateTime.now().minusDays(1));
        Long failedAcq = insertLedgerAcquisition("ACQ_F", AcquisitionStatusEnum.REGISTERED.getStatus(), 7L,
                null, null, new BigDecimal("10"), new BigDecimal("9.5"), failedSettlement, null,
                new BigDecimal("950.00"));
        insertPayment("PAY_F_ONLY", failedAcq, PaymentStatusEnum.FAILED, null, "余额不足");

        Long okSettlement = insertSettlement("ST_OK", SettlementConfirmStatusEnum.CONFIRMED,
                LocalDateTime.now().minusDays(1));
        Long okAcq = insertLedgerAcquisition("ACQ_OK", AcquisitionStatusEnum.REGISTERED.getStatus(), 7L,
                null, null, new BigDecimal("10"), new BigDecimal("9.5"), okSettlement, null,
                new BigDecimal("950.00"));
        insertPayment("PAY_OK_ONLY", okAcq, PaymentStatusEnum.SUCCESS, "R_OK", null);

        ReportSettlementPaymentPageReqVO reqVO = new ReportSettlementPaymentPageReqVO();
        reqVO.setPaymentProgress("FAILED");
        PageResult<ReportSettlementPaymentRespVO> page = reportService.getSettlementPaymentPage(reqVO);

        assertEquals(1L, page.getTotal());
        assertEquals("ST_F", page.getList().get(0).getSettlementNo());
    }

    // ==================== 五、异常表 ====================

    @Test
    public void testAnomaly_weightDiff_and_missingEvidence() {
        insertLedgerAcquisition("ACQ_DIFF", AcquisitionStatusEnum.REGISTERED.getStatus(), 7L,
                new BigDecimal("12"), new BigDecimal("2"), new BigDecimal("10"), new BigDecimal("9"),
                null, null, new BigDecimal("900.00"));

        List<ReportAnomalyRespVO> weightDiff = anomalies(ReportAnomalyTypeEnum.WEIGHT_DIFF.getCode());
        assertEquals(1, weightDiff.size());
        assertEquals("ACQ_DIFF", weightDiff.get(0).getBizNo());
        assertEquals("DANGER", weightDiff.get(0).getSeverity());
        assertNotNull(weightDiff.get(0).getDefinition());

        // 同一张单缺交易地点 / 磅单号 → 资料缺失
        List<ReportAnomalyRespVO> missing = anomalies(ReportAnomalyTypeEnum.MISSING_EVIDENCE.getCode());
        assertEquals(1, missing.size());
        assertTrue(missing.get(0).getDetail().contains("交易地点"));
        assertTrue(missing.get(0).getDetail().contains("磅单号"));
    }

    @Test
    public void testAnomaly_overPurchaseQuantity_consumesProgress() {
        Long orderId = insertOrder("PO_OVER", "张三", 1, LocalDate.now().plusDays(5));
        PurchaseOrderProgressRespVO progress = new PurchaseOrderProgressRespVO();
        PurchaseOrderProgressRespVO.ItemProgress item = new PurchaseOrderProgressRespVO.ItemProgress();
        item.setItemId(1L);
        item.setCategoryName("废钢");
        item.setQuantity(new BigDecimal("100"));
        item.setAcceptedQuantity(new BigDecimal("130"));
        item.setOverQuantity(true);
        progress.setItems(List.of(item));
        when(purchaseOrderService.getProgress(anyLong())).thenReturn(progress);

        List<ReportAnomalyRespVO> rows = anomalies(ReportAnomalyTypeEnum.OVER_PURCHASE_QUANTITY.getCode());

        assertEquals(1, rows.size());
        assertEquals("PO_OVER", rows.get(0).getBizNo());
        assertEquals(new BigDecimal("30"), rows.get(0).getQuantity());
        assertTrue(rows.get(0).getDetail().contains("超过计划量"));
    }

    @Test
    public void testAnomaly_overStockIn() {
        Long acqId = insertLedgerAcquisition("ACQ_OVER_IN", AcquisitionStatusEnum.REGISTERED.getStatus(),
                7L, null, null, new BigDecimal("10"), new BigDecimal("10"), null, null,
                new BigDecimal("1000.00"));
        // 接收量 10，过账入库合计 12 → 超入库量
        insertStockIn("SI_1", acqId, 1, new BigDecimal("7"));
        insertStockIn("SI_2", acqId, 1, new BigDecimal("5"));
        // 待过账的不计
        insertStockIn("SI_3", acqId, 0, new BigDecimal("99"));

        List<ReportAnomalyRespVO> rows = anomalies(ReportAnomalyTypeEnum.OVER_STOCK_IN.getCode());

        assertEquals(1, rows.size());
        assertEquals("ACQ_OVER_IN", rows.get(0).getBizNo());
        assertDecimal("2", rows.get(0).getQuantity());
    }

    @Test
    public void testAnomaly_duplicateLink_countsOnlyActivePayments() {
        Long acqId = insertLedgerAcquisition("ACQ_DUP", AcquisitionStatusEnum.REGISTERED.getStatus(), 7L,
                null, null, new BigDecimal("10"), new BigDecimal("10"), null, null,
                new BigDecimal("1000.00"));
        insertPayment("PAY_D1", acqId, PaymentStatusEnum.SUCCESS, "R1", null);
        insertPayment("PAY_D2", acqId, PaymentStatusEnum.PENDING, null, null);
        // 已冲正的不算有效关联
        insertPayment("PAY_D3", acqId, PaymentStatusEnum.REVERSED, null, null);

        List<ReportAnomalyRespVO> rows = anomalies(ReportAnomalyTypeEnum.DUPLICATE_LINK.getCode());

        assertEquals(1, rows.size());
        assertEquals("ACQ_DUP", rows.get(0).getBizNo());
        assertTrue(rows.get(0).getDetail().contains("2 张未作废付款单"));
    }

    @Test
    public void testAnomaly_longUnconfirmed() {
        // 已过确认截止时间 → 异常
        insertSettlementWithDeadline("ST_OVERDUE", SettlementConfirmStatusEnum.PENDING,
                LocalDateTime.now().minusDays(3));
        // 还没到截止时间 → 不算
        insertSettlementWithDeadline("ST_FRESH", SettlementConfirmStatusEnum.PENDING,
                LocalDateTime.now().plusDays(3));
        // 已确认 → 不算
        insertSettlementWithDeadline("ST_DONE", SettlementConfirmStatusEnum.CONFIRMED,
                LocalDateTime.now().minusDays(3));

        List<ReportAnomalyRespVO> rows = anomalies(ReportAnomalyTypeEnum.LONG_UNCONFIRMED.getCode());

        assertEquals(1, rows.size());
        assertEquals("ST_OVERDUE", rows.get(0).getBizNo());
        assertEquals("WARNING", rows.get(0).getSeverity());
    }

    @Test
    public void testAnomaly_allTypes_mergedAndSortedByTimeDesc() {
        insertLedgerAcquisition("ACQ_ALL", AcquisitionStatusEnum.REGISTERED.getStatus(), 7L,
                new BigDecimal("12"), new BigDecimal("2"), new BigDecimal("10"), new BigDecimal("9"),
                null, null, new BigDecimal("900.00"));

        ReportAnomalyPageReqVO reqVO = new ReportAnomalyPageReqVO();
        PageResult<ReportAnomalyRespVO> page = reportService.getAnomalyPage(reqVO);

        // 同一张单：磅差 + 资料缺失
        assertTrue(page.getTotal() >= 2);
        assertTrue(page.getList().stream().anyMatch(row -> ReportAnomalyTypeEnum.WEIGHT_DIFF.getCode()
                .equals(row.getType())));
        assertTrue(page.getList().stream().anyMatch(row -> ReportAnomalyTypeEnum.MISSING_EVIDENCE.getCode()
                .equals(row.getType())));
    }

    @Test
    public void testAnomaly_unknownType_throws() {
        ReportAnomalyPageReqVO reqVO = new ReportAnomalyPageReqVO();
        reqVO.setType("NOT_A_TYPE");

        ServiceException ex = assertThrows(ServiceException.class, () -> reportService.getAnomalyPage(reqVO));
        assertTrue(ex.getMessage().contains("未知的异常类型"));
    }

    @Test
    public void testAnomaly_filtersBySellerName() {
        insertLedgerAcquisitionForSeller("ACQ_OTHER", "李四", AcquisitionStatusEnum.REGISTERED.getStatus(),
                7L, new BigDecimal("12"), new BigDecimal("2"), new BigDecimal("10"), new BigDecimal("9"),
                null, null, new BigDecimal("900.00"));
        insertLedgerAcquisitionForSeller("ACQ_ZHANG", "张三", AcquisitionStatusEnum.REGISTERED.getStatus(),
                7L, new BigDecimal("12"), new BigDecimal("2"), new BigDecimal("10"), new BigDecimal("9"),
                null, null, new BigDecimal("900.00"));

        ReportAnomalyPageReqVO reqVO = new ReportAnomalyPageReqVO();
        reqVO.setType(ReportAnomalyTypeEnum.WEIGHT_DIFF.getCode());
        reqVO.setSellerName("张三");

        PageResult<ReportAnomalyRespVO> page = reportService.getAnomalyPage(reqVO);
        assertEquals(1L, page.getTotal());
        assertEquals("ACQ_ZHANG", page.getList().get(0).getBizNo());
    }

    // ==================== helpers ====================

    /** BigDecimal 只比数值，不比小数位（H2 会按列定义补零）。 */
    private static void assertDecimal(String expected, BigDecimal actual) {
        assertNotNull(actual, "期望 " + expected + "，实际为空");
        assertEquals(0, actual.compareTo(new BigDecimal(expected)),
                "期望 " + expected + "，实际 " + actual);
    }

    private List<ReportAnomalyRespVO> anomalies(String type) {
        ReportAnomalyPageReqVO reqVO = new ReportAnomalyPageReqVO();
        reqVO.setType(type);
        return reportService.getAnomalyPage(reqVO).getList();
    }

    private Long insertGoodsConfig(String name) {
        IcbcGoodsConfigDO config = new IcbcGoodsConfigDO();
        config.setName(name);
        config.setUnit("吨");
        config.setTaxRate(new BigDecimal("0.01"));
        config.setTaxMethod("SIMPLE");
        config.setMergedCode("1090101010000000000");
        config.setStatus(0);
        goodsConfigMapper.insert(config);
        return config.getId();
    }

    private Long insertStation(String name) {
        IcbcStationDO station = IcbcStationDO.builder()
                .stationCode("STATION_" + System.nanoTime()).name(name).address("城东").openStatus(1).build();
        stationMapper.insert(station);
        return station.getId();
    }

    private Long insertOrder(String orderNo, String counterpartyName, Integer status, LocalDate endDate) {
        IcbcPurchaseOrderDO order = IcbcPurchaseOrderDO.builder()
                .orderNo(orderNo)
                .counterpartyType(1)
                .payeeId(100L)
                .counterpartyName(counterpartyName)
                .startDate(LocalDate.now().minusDays(10))
                .endDate(endDate)
                .status(status)
                .totalQuantity(new BigDecimal("100.0000"))
                .totalAmount(new BigDecimal("10000.00"))
                .build();
        purchaseOrderMapper.insert(order);
        return order.getId();
    }

    private Long insertLedgerAcquisition(String no, Integer status, Long stationId, BigDecimal gross,
                                         BigDecimal tare, BigDecimal net, BigDecimal settlement,
                                         Long settlementId, Long purchaseOrderId, BigDecimal amount) {
        return insertLedgerAcquisitionForSeller(no, "张三", status, stationId, gross, tare, net,
                settlement, settlementId, purchaseOrderId, amount);
    }

    private Long insertLedgerAcquisitionForSeller(String no, String sellerName, Integer status, Long stationId,
                                                  BigDecimal gross, BigDecimal tare, BigDecimal net,
                                                  BigDecimal settlement, Long settlementId,
                                                  Long purchaseOrderId, BigDecimal amount) {
        IcbcAcquisitionDO acquisition = IcbcAcquisitionDO.builder()
                .acquisitionNo(no)
                .clientRequestId("CLIENT_" + no)
                .payeeId(100L)
                .partnerPayeeId("PARTNER")
                .sellerName(sellerName)
                .sellerMobile("13800138000")
                .goodsConfigId(11L)
                .categoryName("废钢")
                .unit("吨")
                .taxRate(new BigDecimal("0.01"))
                .taxMethod("SIMPLE")
                .mergedCode("1090101010000000000")
                .specification("M2")
                .quantity(BigDecimal.ONE)
                .unitPrice(new BigDecimal("100.00"))
                .amount(amount)
                .grossWeight(gross)
                .tareWeight(tare)
                .netWeight(net)
                .deduction(BigDecimal.ZERO)
                .deductionMethod("WEIGHT")
                .settlementWeight(settlement)
                .acceptedWeight(net)
                .weightDiff(settlement == null || net == null ? null : net.subtract(settlement))
                .settlementId(settlementId)
                .purchaseOrderId(purchaseOrderId == null ? 0L : purchaseOrderId)
                .purchaseOrderItemId(0L)
                .stationId(stationId)
                .tradeAddress(null) // 故意留空：资料缺失异常要能测出来
                .weightTicketNo(null)
                .tradeTime(LocalDateTime.now().minusHours(1))
                .status(status)
                .build();
        acquisitionMapper.insert(acquisition);
        return acquisition.getId();
    }

    private Long insertSettlement(String no, SettlementConfirmStatusEnum confirmStatus,
                                  LocalDateTime generateTime) {
        return insertSettlementWithDeadline(no, confirmStatus, generateTime == null ? null
                : generateTime.plusDays(7));
    }

    private Long insertSettlementWithDeadline(String no, SettlementConfirmStatusEnum confirmStatus,
                                              LocalDateTime deadline) {
        IcbcSettlementDO settlement = IcbcSettlementDO.builder()
                .settlementNo(no)
                .payeeId(100L)
                .naturalPersonId(1L)
                .sellerName("张三")
                .sellerMobile("13800138000")
                .stationId(7L)
                .stationName("城东场站")
                .generateTime(deadline == null ? LocalDateTime.now() : deadline.minusDays(7))
                .currentVersionNo(1)
                .confirmStatus(confirmStatus.getStatus())
                .deadlineTime(deadline)
                .disputeCount(0)
                .build();
        settlementMapper.insert(settlement);
        return settlement.getId();
    }

    private void insertPayment(String orderNo, Long acquisitionId, PaymentStatusEnum status,
                               String receiptNo, String errorMsg) {
        PaymentOrderDO payment = PaymentOrderDO.builder()
                .orderNo(orderNo)
                .partnerOrderId("PARTNER_" + orderNo)
                .acquisitionId(acquisitionId)
                .payeeNo("PAYEE_NO")
                .paymentAmount(new BigDecimal("950.00"))
                .paymentStatus(status.getStatus())
                .receiptNo(receiptNo)
                .receiptTime(receiptNo == null ? null : LocalDateTime.now())
                .paymentTime(PaymentStatusEnum.SUCCESS.equals(status) ? LocalDateTime.now() : null)
                .errorMsg(errorMsg)
                .retryCount(0)
                .build();
        paymentOrderMapper.insert(payment);
    }

    private void insertStockIn(String no, Long acquisitionId, Integer status, BigDecimal totalQuantity) {
        IcbcStockInDO stockIn = IcbcStockInDO.builder()
                .stockInNo(no)
                .acquisitionId(acquisitionId)
                .acquisitionNo("ACQ")
                .payeeId(100L)
                .sellerName("张三")
                .goodsConfigId(11L)
                .categoryName("废钢")
                .unit("吨")
                .totalQuantity(totalQuantity)
                .status(status)
                .build();
        stockInMapper.insert(stockIn);
    }

    private void insertAppointment(String no, BigDecimal expectedQuantity) {
        IcbcAppointmentDO appointment = IcbcAppointmentDO.builder()
                .appointmentNo(no)
                .naturalPersonId(1L)
                .sellerName("张三")
                .stationId(7L)
                .stationCode("STATION_7")
                .stationName("城东场站")
                .goodsConfigId(11L)
                .categoryName("废钢")
                .unit("吨")
                .expectedQuantity(expectedQuantity)
                .expectedArrivalTime(LocalDateTime.now().plusDays(1))
                .status(0)
                .build();
        appointmentMapper.insert(appointment);
    }

}
