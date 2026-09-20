package cn.iocoder.yudao.module.icbc.service.trace;

import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.security.core.service.SecurityFrameworkService;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.icbc.UnitTestConfiguration;
import cn.iocoder.yudao.module.icbc.controller.admin.trace.vo.TraceDifferenceRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.trace.vo.TraceRowRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.trace.vo.TraceSearchReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.trace.vo.TraceSearchRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.trace.vo.TraceStageRespVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.acquisition.IcbcAcquisitionDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.handover.IcbcHandoverBatchDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.invoice.InvoiceOrderDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payee.PayeeInfoDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payer.PayerInfoDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payment.PaymentOrderDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.purchaseorder.IcbcPurchaseOrderDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.purchaseorder.IcbcPurchaseOrderItemDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.settlement.IcbcSettlementDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.stockin.IcbcStockInDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.acquisition.IcbcAcquisitionMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.handover.IcbcHandoverBatchMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.invoice.InvoiceOrderMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.payee.PayeeInfoMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.payer.PayerInfoMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.payment.PaymentOrderMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.purchaseorder.IcbcPurchaseOrderItemMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.purchaseorder.IcbcPurchaseOrderMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.settlement.IcbcSettlementMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.stockin.IcbcStockInMapper;
import cn.iocoder.yudao.module.icbc.enums.AcquisitionStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants;
import cn.iocoder.yudao.module.icbc.enums.InvoiceIssueStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.PaymentStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.PreInvoiceStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.PurchaseOrderStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.RecyclingPermission;
import cn.iocoder.yudao.module.icbc.enums.SettlementConfirmStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.StockInStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.TaxStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.TraceDifferenceCodeEnum;
import cn.iocoder.yudao.module.icbc.enums.TraceKeywordTypeEnum;
import cn.iocoder.yudao.module.icbc.enums.TraceStageCodeEnum;
import cn.iocoder.yudao.module.icbc.enums.TraceStageStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.UploadStatusEnum;
import cn.iocoder.yudao.module.icbc.service.trace.impl.TraceQueryServiceImpl;
import cn.iocoder.yudao.module.system.api.logger.OperateLogApi;
import cn.iocoder.yudao.module.system.api.logger.dto.OperateLogCreateReqDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link TraceQueryServiceImpl} 的单元测试（#55 T17）。
 *
 * <p>守四件事：四栏 + 付款 / 发票按固定顺序铺开且一对多展开明细；「直接收购」是无需该环节而不是缺失；
 * 入库量与结算量不一致时能说清差异 / 缺失关联（不默认一对一）；敏感字段按岗位权限脱敏、导出同样受限并留记录。
 */
@Import({TraceQueryServiceImpl.class, UnitTestConfiguration.class})
@Sql(scripts = "/sql/create_tables.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Transactional
public class TraceQueryServiceTest extends BaseDbUnitTest {

    @Resource
    private TraceQueryService traceQueryService;
    @Resource
    private IcbcAcquisitionMapper acquisitionMapper;
    @Resource
    private IcbcHandoverBatchMapper handoverBatchMapper;
    @Resource
    private IcbcPurchaseOrderMapper purchaseOrderMapper;
    @Resource
    private IcbcPurchaseOrderItemMapper purchaseOrderItemMapper;
    @Resource
    private IcbcSettlementMapper settlementMapper;
    @Resource
    private IcbcStockInMapper stockInMapper;
    @Resource
    private PaymentOrderMapper paymentOrderMapper;
    @Resource
    private InvoiceOrderMapper invoiceOrderMapper;
    @Resource
    private PayeeInfoMapper payeeInfoMapper;
    @Resource
    private PayerInfoMapper payerInfoMapper;

    @MockBean
    private SecurityFrameworkService securityFrameworkService;
    @MockBean
    private OperateLogApi operateLogApi;

    @AfterEach
    public void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ==================== 查询条件与反查入口 ====================

    @Test
    public void testSearch_requiresAtLeastOneCondition() {
        assertServiceException(() -> traceQueryService.search(new TraceSearchReqVO()),
                ErrorCodeConstants.TRACE_QUERY_CONDITION_REQUIRED);
    }

    @Test
    public void testSearchByKeyword_invalidKeywordType() {
        TraceSearchReqVO reqVO = new TraceSearchReqVO();
        reqVO.setKeyword("ACQ001");
        reqVO.setKeywordType("NOT_A_TYPE");
        assertServiceException(() -> traceQueryService.search(reqVO),
                ErrorCodeConstants.TRACE_KEYWORD_TYPE_INVALID, "NOT_A_TYPE");
    }

    @Test
    public void testGetTrace_notFound() {
        assertServiceException(() -> traceQueryService.getTrace(99999L, false),
                ErrorCodeConstants.TRACE_ACQUISITION_NOT_EXISTS, 99999L);
    }

    @Test
    public void testSearchByAcquisitionNo_returnsSixStagesInOrder() {
        IcbcAcquisitionDO acquisition = insertFullChainAcquisition("ACQ001", 1L);

        TraceSearchRespVO resp = traceQueryService.search(byAcquisitionNo("ACQ001"));

        assertEquals(1L, resp.getTotal());
        TraceRowRespVO row = resp.getList().get(0);
        assertEquals(acquisition.getId(), row.getAcquisitionId());
        List<String> expected = TraceStageCodeEnum.ordered().stream()
                .map(TraceStageCodeEnum::getCode).collect(Collectors.toList());
        List<String> actual = row.getStages().stream().map(TraceStageRespVO::getCode)
                .collect(Collectors.toList());
        assertEquals(expected, actual);
        assertEquals(TraceStageStatusEnum.nameOf(TraceStageStatusEnum.COMPLETED.getStatus()),
                stageOf(row, TraceStageCodeEnum.STOCK_IN).getStatusName());
        assertEquals(TraceStageStatusEnum.nameOf(TraceStageStatusEnum.COMPLETED.getStatus()),
                stageOf(row, TraceStageCodeEnum.SETTLEMENT).getStatusName());
        assertEquals(TraceStageStatusEnum.nameOf(TraceStageStatusEnum.COMPLETED.getStatus()),
                stageOf(row, TraceStageCodeEnum.PAYMENT).getStatusName());
        assertEquals(TraceStageStatusEnum.nameOf(TraceStageStatusEnum.COMPLETED.getStatus()),
                stageOf(row, TraceStageCodeEnum.INVOICE).getStatusName());
        // 数量 / 重量 / 金额按阶段取数，取不到的留空而不是 0
        assertEquals(0, new BigDecimal("28.33").compareTo(
                stageOf(row, TraceStageCodeEnum.SETTLEMENT).getWeight()));
        assertEquals(0, new BigDecimal("14.74").compareTo(
                stageOf(row, TraceStageCodeEnum.STOCK_IN).getWeight()));
    }

    @Test
    public void testDirectAcquisition_purchaseOrderStageIsNotApplicable() {
        IcbcAcquisitionDO acquisition = insertAcquisition("ACQ_DIRECT", new BigDecimal("14.74"), 1L);

        TraceRowRespVO row = traceQueryService.getTrace(acquisition.getId(), false);

        assertTrue(row.getDirectAcquisition());
        TraceStageRespVO stage = stageOf(row, TraceStageCodeEnum.PURCHASE_ORDER);
        assertTrue(stage.getNotApplicable());
        assertEquals(TraceStageStatusEnum.NOT_APPLICABLE.getStatus(), stage.getStatus());
        assertTrue(stage.getNodes().isEmpty());
    }

    // ==================== AC4：差异 / 缺失关联 ====================

    @Test
    public void testStockInVsSettlement_differenceExplained() {
        IcbcAcquisitionDO acquisition = insertAcquisition("ACQ_DIFF", new BigDecimal("14.74"), 1L);
        insertStockIn(acquisition, "14.74", StockInStatusEnum.POSTED);
        linkSettlement(acquisition.getId(), insertSettlement("ST001", SettlementConfirmStatusEnum.CONFIRMED).getId());

        TraceRowRespVO row = traceQueryService.getTrace(acquisition.getId(), false);

        TraceDifferenceRespVO difference = row.getDifferences().stream()
                .filter(d -> TraceDifferenceCodeEnum.STOCK_IN_VS_SETTLEMENT.getCode().equals(d.getCode()))
                .findFirst().orElseThrow();
        assertEquals(0, new BigDecimal("28.33").compareTo(difference.getExpectedWeight()));
        assertEquals(0, new BigDecimal("14.74").compareTo(difference.getActualWeight()));
        assertEquals(0, new BigDecimal("-13.59").compareTo(difference.getDifference()));
        assertTrue(difference.getNote().contains("不默认一对一"));
        assertEquals(TraceStageStatusEnum.nameOf(TraceStageStatusEnum.COMPLETED.getStatus()),
                stageOf(row, TraceStageCodeEnum.STOCK_IN).getStatusName());
    }

    @Test
    public void testSettledWithoutStockIn_missingLinkHinted() {
        IcbcAcquisitionDO acquisition = insertAcquisition("ACQ_SETTLED", null, 1L);
        linkSettlement(acquisition.getId(), insertSettlement("ST001", SettlementConfirmStatusEnum.CONFIRMED).getId());

        TraceRowRespVO row = traceQueryService.getTrace(acquisition.getId(), false);

        assertTrue(row.getDifferences().stream()
                .anyMatch(d -> TraceDifferenceCodeEnum.MISSING_STOCK_IN_LINK.getCode().equals(d.getCode())));
        assertEquals(TraceStageStatusEnum.NOT_STARTED.getStatus(),
                stageOf(row, TraceStageCodeEnum.STOCK_IN).getStatus());
        assertTrue(stageOf(row, TraceStageCodeEnum.STOCK_IN).getNote().contains("已结算"));
    }

    @Test
    public void testStockedWithoutSettlement_missingLinkHinted() {
        IcbcAcquisitionDO acquisition = insertAcquisition("ACQ_STOCKED", new BigDecimal("14.74"), 1L);
        insertStockIn(acquisition, "14.74", StockInStatusEnum.POSTED);

        TraceRowRespVO row = traceQueryService.getTrace(acquisition.getId(), false);

        assertTrue(row.getDifferences().stream()
                .anyMatch(d -> TraceDifferenceCodeEnum.MISSING_SETTLEMENT_LINK.getCode().equals(d.getCode())));
        assertEquals(TraceStageStatusEnum.NOT_STARTED.getStatus(),
                stageOf(row, TraceStageCodeEnum.SETTLEMENT).getStatus());
    }

    @Test
    public void testWeightDiffVsSettlement_reported() {
        IcbcAcquisitionDO acquisition = insertAcquisition("ACQ_WEIGHT_DIFF", new BigDecimal("14.74"), 1L);
        IcbcAcquisitionDO update = new IcbcAcquisitionDO();
        update.setId(acquisition.getId());
        update.setWeightDiff(new BigDecimal("-13.59"));
        acquisitionMapper.updateById(update);
        insertStockIn(acquisition, "14.74", StockInStatusEnum.POSTED);
        linkSettlement(acquisition.getId(), insertSettlement("ST001", SettlementConfirmStatusEnum.CONFIRMED).getId());

        TraceRowRespVO row = traceQueryService.getTrace(acquisition.getId(), false);

        assertTrue(row.getDifferences().stream()
                .anyMatch(d -> TraceDifferenceCodeEnum.WEIGHT_DIFF_VS_SETTLEMENT.getCode().equals(d.getCode())));
    }

    // ==================== 脱敏（AC6） ====================

    @Test
    public void testSensitive_maskedByDefault() {
        PayeeInfoDO payee = insertPayee();
        insertPayer();
        IcbcAcquisitionDO acquisition = insertFullChainAcquisition("ACQ_MASK", payee.getId());

        TraceRowRespVO row = traceQueryService.getTrace(acquisition.getId(), false);

        assertFalse(row.getSensitiveUnmasked());
        assertEquals("110101********1234", row.getSensitive().getSellerIdCard());
        assertEquals("138****9999", row.getSensitive().getSellerMobile());
        assertEquals("****0123", row.getSensitive().getSellerBankCard());
        assertEquals("9111**********278M", row.getSensitive().getBuyerTaxNo());
    }

    @Test
    public void testSensitive_unmaskedWithPermission() {
        PayeeInfoDO payee = insertPayee();
        insertPayer();
        IcbcAcquisitionDO acquisition = insertFullChainAcquisition("ACQ_UNMASK", payee.getId());

        TraceRowRespVO row = traceQueryService.getTrace(acquisition.getId(), true);

        assertTrue(row.getSensitiveUnmasked());
        assertEquals("110101199001011234", row.getSensitive().getSellerIdCard());
        assertEquals("13800139999", row.getSensitive().getSellerMobile());
        assertEquals("6222021234567890123", row.getSensitive().getSellerBankCard());
        assertEquals("91110105MA01R2278M", row.getSensitive().getBuyerTaxNo());
    }

    @Test
    public void testSearch_sensitiveFollowsRolePermission() {
        PayeeInfoDO payee = insertPayee();
        insertAcquisition("ACQ_ROLE", new BigDecimal("14.74"), payee.getId());

        TraceSearchRespVO masked = traceQueryService.search(byAcquisitionNo("ACQ_ROLE"));
        assertEquals("110101********1234", masked.getList().get(0).getSensitive().getSellerIdCard());

        when(securityFrameworkService.hasPermission(RecyclingPermission.TRACE_SENSITIVE_VIEW)).thenReturn(true);
        TraceSearchRespVO unmasked = traceQueryService.search(byAcquisitionNo("ACQ_ROLE"));
        assertEquals("110101199001011234", unmasked.getList().get(0).getSensitive().getSellerIdCard());
    }

    // ==================== 反查维度：车牌 / 单号 / 主体 ====================

    @Test
    public void testSearchByPlate_matchesBatchPlate() {
        IcbcHandoverBatchDO batch = insertBatch("B001", "京A12345");
        IcbcAcquisitionDO acquisition = insertAcquisition("ACQ_BATCH", new BigDecimal("14.74"), 1L);
        updateHandoverBatchLink(acquisition.getId(), batch.getId());
        // 清掉收购单自身车牌，确保命中的是批次车牌
        IcbcAcquisitionDO clear = new IcbcAcquisitionDO();
        clear.setId(acquisition.getId());
        clear.setVehiclePlateNo(" ");
        clear.setWeightTicketPlateNo(" ");
        acquisitionMapper.updateById(clear);

        TraceSearchReqVO reqVO = new TraceSearchReqVO();
        reqVO.setPlateNo("京A12345");
        TraceSearchRespVO resp = traceQueryService.search(reqVO);

        assertEquals(1L, resp.getTotal());
        assertEquals("B001", resp.getList().get(0).getHandoverBatchNo());
        assertEquals("京A12345", resp.getList().get(0).getPlateNo());
    }

    @Test
    public void testSearchByKeyword_autoResolvesSettlementNo() {
        IcbcAcquisitionDO acquisition = insertAcquisition("ACQ_ST", new BigDecimal("14.74"), 1L);
        linkSettlement(acquisition.getId(), insertSettlement("ST001", SettlementConfirmStatusEnum.PENDING).getId());

        TraceSearchReqVO reqVO = new TraceSearchReqVO();
        reqVO.setKeyword("ST001");
        TraceSearchRespVO resp = traceQueryService.search(reqVO);

        assertEquals(1L, resp.getTotal());
        assertEquals(acquisition.getId(), resp.getList().get(0).getAcquisitionId());
    }

    @Test
    public void testSearchByKeyword_resolvesInvoiceNo() {
        IcbcAcquisitionDO acquisition = insertAcquisition("ACQ_INV", new BigDecimal("14.74"), 1L);
        insertInvoice(acquisition, "INVNO001", InvoiceIssueStatusEnum.ISSUED);

        TraceSearchReqVO reqVO = new TraceSearchReqVO();
        reqVO.setKeyword("INVNO001");
        reqVO.setKeywordType(TraceKeywordTypeEnum.INVOICE_NO.getCode());
        TraceSearchRespVO resp = traceQueryService.search(reqVO);

        assertEquals(1L, resp.getTotal());
        assertEquals("INVNO001",
                stageOf(resp.getList().get(0), TraceStageCodeEnum.INVOICE).getNodes().get(0).getBizNo());
    }

    @Test
    public void testSearchBySellerKeyword_likeMatch() {
        insertAcquisition("ACQ_SELLER", new BigDecimal("14.74"), 1L);

        TraceSearchReqVO reqVO = new TraceSearchReqVO();
        reqVO.setSellerName("张三");
        TraceSearchRespVO resp = traceQueryService.search(reqVO);

        assertEquals(1L, resp.getTotal());
    }

    // ==================== 汇总 / 分页 / 未展示明细（AC5） ====================

    @Test
    public void testSummary_countsEachAcquisitionOnce() {
        IcbcAcquisitionDO first = insertAcquisition("ACQ_S1", new BigDecimal("10"), 1L);
        IcbcAcquisitionDO second = insertAcquisition("ACQ_S2", new BigDecimal("20"), 1L);
        Long settlementId = insertSettlement("ST001", SettlementConfirmStatusEnum.CONFIRMED).getId();
        linkSettlement(first.getId(), settlementId);
        linkSettlement(second.getId(), settlementId);

        TraceSearchReqVO reqVO = new TraceSearchReqVO();
        reqVO.setSellerName("张三");
        TraceSearchRespVO resp = traceQueryService.search(reqVO);

        assertEquals(2L, resp.getTotal());
        assertEquals(2L, resp.getSummary().getAcquisitionCount());
        assertEquals(0, new BigDecimal("56.66").compareTo(resp.getSummary().getTotalSettlementWeight()));
        assertTrue(resp.getScopeNote().contains("每张只计一次"));
        assertTrue(resp.getSummary().getCountNote().contains("不把同一结算单"));
    }

    @Test
    public void testSearch_paginationAndHiddenDetailCount() {
        for (int i = 0; i < 3; i++) {
            insertAcquisition("ACQ_P" + i, new BigDecimal("14.74"), 1L);
        }
        TraceSearchReqVO reqVO = new TraceSearchReqVO();
        reqVO.setSellerName("张三");
        reqVO.setPageNo(1);
        reqVO.setPageSize(2);
        TraceSearchRespVO resp = traceQueryService.search(reqVO);

        assertEquals(3L, resp.getTotal());
        assertEquals(2, resp.getList().size());
        assertEquals(1L, resp.getHiddenDetailCount());
    }

    @Test
    public void testOnlyDifferenceFilter() {
        // 无差异：结算重量与入库量一致、无称量差异
        IcbcAcquisitionDO clean = insertAcquisition("ACQ_CLEAN", new BigDecimal("14.74"), 1L);
        IcbcAcquisitionDO update = new IcbcAcquisitionDO();
        update.setId(clean.getId());
        update.setSettlementWeight(new BigDecimal("14.74"));
        acquisitionMapper.updateById(update);
        insertStockIn(clean, "14.74", StockInStatusEnum.POSTED);
        linkSettlement(clean.getId(), insertSettlement("ST001", SettlementConfirmStatusEnum.CONFIRMED).getId());
        // 有差异：已结算但未入库
        insertAcquisition("ACQ_DIRTY", new BigDecimal("14.74"), 1L);

        TraceSearchReqVO reqVO = new TraceSearchReqVO();
        reqVO.setSellerName("张三");
        reqVO.setOnlyDifference(true);
        TraceSearchRespVO resp = traceQueryService.search(reqVO);

        assertEquals(1L, resp.getTotal());
        assertEquals("ACQ_DIRTY", resp.getList().get(0).getAcquisitionNo());
    }

    // ==================== 附件 / 操作历史（AC3） ====================

    @Test
    public void testAttachmentsAndHistoriesCollected() {
        IcbcAcquisitionDO acquisition = insertFullChainAcquisition("ACQ_HIST", 1L);
        IcbcAcquisitionDO update = new IcbcAcquisitionDO();
        update.setId(acquisition.getId());
        update.setWeightTicketImageUrl("http://file/ticket.jpg");
        update.setVehicleFrontImageUrl("http://file/front.jpg");
        acquisitionMapper.updateById(update);

        TraceRowRespVO row = traceQueryService.getTrace(acquisition.getId(), false);

        assertEquals(2, row.getAttachments().size());
        assertTrue(row.getAttachments().stream().anyMatch(a -> "磅单照片".equals(a.getName())));
        assertTrue(row.getHistories().stream().anyMatch(h -> "收购登记".equals(h.getAction())));
        assertTrue(row.getHistories().stream().anyMatch(h -> "入库过账".equals(h.getAction())));
        for (int i = 1; i < row.getHistories().size(); i++) {
            assertFalse(row.getHistories().get(i).getTime()
                    .isBefore(row.getHistories().get(i - 1).getTime()));
        }
    }

    @Test
    public void testInvoiceException_stageIsException() {
        IcbcAcquisitionDO acquisition = insertAcquisition("ACQ_INV_FAIL", new BigDecimal("14.74"), 1L);
        insertInvoice(acquisition, null, InvoiceIssueStatusEnum.FAILED);

        TraceRowRespVO row = traceQueryService.getTrace(acquisition.getId(), false);

        assertEquals(TraceStageStatusEnum.EXCEPTION.getStatus(),
                stageOf(row, TraceStageCodeEnum.INVOICE).getStatus());
    }

    // ==================== 导出（AC6） ====================

    @Test
    public void testExport_writesXlsAndRecordsLog() throws Exception {
        PayeeInfoDO payee = insertPayee();
        IcbcAcquisitionDO acquisition = insertFullChainAcquisition("ACQ_EXPORT", payee.getId());
        setLoginUser(1L);

        MockHttpServletResponse response = new MockHttpServletResponse();
        traceQueryService.export(byAcquisitionNo(acquisition.getAcquisitionNo()), response);

        assertTrue(response.getContentAsByteArray().length > 0);
        ArgumentCaptor<OperateLogCreateReqDTO> captor = ArgumentCaptor.forClass(OperateLogCreateReqDTO.class);
        verify(operateLogApi).createOperateLog(captor.capture());
        assertEquals(1L, captor.getValue().getUserId());
        assertEquals("ICBC 关联单据查询", captor.getValue().getType());
        assertTrue(captor.getValue().getAction().contains("导出关联单据查询结果 1 条"));
    }

    @Test
    public void testExport_exceedsLimitRejected() {
        for (int i = 0; i <= 500; i++) {
            insertAcquisition("ACQ_LIMIT_" + i, new BigDecimal("14.74"), 1L);
        }
        TraceSearchReqVO reqVO = new TraceSearchReqVO();
        reqVO.setSellerName("张三");

        ServiceException exception = assertThrows(ServiceException.class,
                () -> traceQueryService.export(reqVO, new MockHttpServletResponse()));
        assertEquals(ErrorCodeConstants.TRACE_EXPORT_LIMIT_EXCEEDED.getCode(), exception.getCode());
    }

    // ==================== 测试数据 ====================

    private TraceSearchReqVO byAcquisitionNo(String acquisitionNo) {
        TraceSearchReqVO reqVO = new TraceSearchReqVO();
        reqVO.setAcquisitionNo(acquisitionNo);
        return reqVO;
    }

    private TraceStageRespVO stageOf(TraceRowRespVO row, TraceStageCodeEnum code) {
        return row.getStages().stream().filter(stage -> code.getCode().equals(stage.getCode()))
                .findFirst().orElseThrow();
    }

    /** 完整链路：采购订单 + 交接批次 + 已过账入库 + 已确认结算 + 成功付款 + 已开票。 */
    private IcbcAcquisitionDO insertFullChainAcquisition(String acquisitionNo, Long payeeId) {
        IcbcPurchaseOrderDO order = insertPurchaseOrder();
        IcbcPurchaseOrderItemDO item = insertPurchaseOrderItem(order.getId());
        IcbcHandoverBatchDO batch = insertBatch("B_" + acquisitionNo, "京A88888");
        IcbcAcquisitionDO acquisition = insertAcquisition(acquisitionNo, new BigDecimal("14.74"), payeeId);
        updatePurchaseOrderLink(acquisition.getId(), order.getId(), item.getId());
        updateHandoverBatchLink(acquisition.getId(), batch.getId());
        insertStockIn(acquisition, "14.74", StockInStatusEnum.POSTED);
        linkSettlement(acquisition.getId(),
                insertSettlement("ST_" + acquisitionNo, SettlementConfirmStatusEnum.CONFIRMED).getId());
        insertPayment(acquisition, PaymentStatusEnum.SUCCESS);
        insertInvoice(acquisition, "INVNO_" + acquisitionNo, InvoiceIssueStatusEnum.ISSUED);
        return acquisition;
    }

    private IcbcAcquisitionDO insertAcquisition(String no, BigDecimal netWeight, Long payeeId) {
        IcbcAcquisitionDO acquisition = IcbcAcquisitionDO.builder()
                .acquisitionNo(no)
                .payeeId(payeeId)
                .sellerName("张三")
                .sellerMobile("13800139999")
                .goodsConfigId(100L)
                .categoryName("废钢")
                .unit("吨")
                .quantity(new BigDecimal("28.33"))
                .netWeight(netWeight)
                .settlementWeight(new BigDecimal("28.33"))
                .amount(new BigDecimal("12345.67"))
                .status(AcquisitionStatusEnum.PAID.getStatus())
                .tradeTime(LocalDateTime.of(2026, 9, 20, 10, 0))
                .tradeAddress("北京市朝阳区回收站")
                .vehiclePlateNo("京A12345")
                .build();
        acquisitionMapper.insert(acquisition);
        return acquisition;
    }

    private void updatePurchaseOrderLink(Long acquisitionId, Long orderId, Long itemId) {
        IcbcAcquisitionDO update = new IcbcAcquisitionDO();
        update.setId(acquisitionId);
        update.setPurchaseOrderId(orderId);
        update.setPurchaseOrderItemId(itemId);
        acquisitionMapper.updateById(update);
    }

    private void updateHandoverBatchLink(Long acquisitionId, Long batchId) {
        IcbcAcquisitionDO update = new IcbcAcquisitionDO();
        update.setId(acquisitionId);
        update.setHandoverBatchId(batchId);
        acquisitionMapper.updateById(update);
    }

    private void linkSettlement(Long acquisitionId, Long settlementId) {
        IcbcAcquisitionDO update = new IcbcAcquisitionDO();
        update.setId(acquisitionId);
        update.setSettlementId(settlementId);
        acquisitionMapper.updateById(update);
    }

    private IcbcHandoverBatchDO insertBatch(String batchNo, String plateNo) {
        IcbcHandoverBatchDO batch = IcbcHandoverBatchDO.builder()
                .batchNo(batchNo)
                .payeeId(1L)
                .sellerName("张三")
                .stationName("朝阳站")
                .plateNo(plateNo)
                .occurTime(LocalDateTime.of(2026, 9, 20, 9, 30))
                .build();
        handoverBatchMapper.insert(batch);
        return batch;
    }

    private IcbcPurchaseOrderDO insertPurchaseOrder() {
        IcbcPurchaseOrderDO order = IcbcPurchaseOrderDO.builder()
                .orderNo("PO001")
                .counterpartyType(1)
                .payeeId(1L)
                .counterpartyName("张三")
                .startDate(java.time.LocalDate.of(2026, 9, 1))
                .endDate(java.time.LocalDate.of(2026, 9, 30))
                .status(PurchaseOrderStatusEnum.EXECUTING.getStatus())
                .totalQuantity(new BigDecimal("100"))
                .totalAmount(new BigDecimal("50000"))
                .build();
        purchaseOrderMapper.insert(order);
        return order;
    }

    private IcbcPurchaseOrderItemDO insertPurchaseOrderItem(Long orderId) {
        IcbcPurchaseOrderItemDO item = IcbcPurchaseOrderItemDO.builder()
                .orderId(orderId)
                .goodsConfigId(100L)
                .categoryName("废钢")
                .unit("吨")
                .quantity(new BigDecimal("100"))
                .unitPrice(new BigDecimal("500"))
                .amount(new BigDecimal("50000"))
                .build();
        purchaseOrderItemMapper.insert(item);
        return item;
    }

    private void insertStockIn(IcbcAcquisitionDO acquisition, String quantity, StockInStatusEnum status) {
        IcbcStockInDO stockIn = IcbcStockInDO.builder()
                .stockInNo("SIN_" + acquisition.getAcquisitionNo())
                .acquisitionId(acquisition.getId())
                .acquisitionNo(acquisition.getAcquisitionNo())
                .payeeId(acquisition.getPayeeId())
                .sellerName(acquisition.getSellerName())
                .goodsConfigId(acquisition.getGoodsConfigId())
                .categoryName(acquisition.getCategoryName())
                .unit(acquisition.getUnit())
                .totalQuantity(new BigDecimal(quantity))
                .status(status.getStatus())
                .postedTime(StockInStatusEnum.POSTED.equals(status) ? LocalDateTime.of(2026, 9, 20, 11, 0) : null)
                .build();
        stockInMapper.insert(stockIn);
    }

    private IcbcSettlementDO insertSettlement(String settlementNo, SettlementConfirmStatusEnum status) {
        IcbcSettlementDO settlement = IcbcSettlementDO.builder()
                .settlementNo(settlementNo)
                .payeeId(1L)
                .sellerName("张三")
                .stationName("朝阳站")
                .confirmStatus(status.getStatus())
                .currentVersionNo(1)
                .generateTime(LocalDateTime.of(2026, 9, 20, 11, 30))
                .build();
        settlementMapper.insert(settlement);
        return settlement;
    }

    private void insertPayment(IcbcAcquisitionDO acquisition, PaymentStatusEnum status) {
        PaymentOrderDO payment = PaymentOrderDO.builder()
                .orderNo("PAY_" + acquisition.getAcquisitionNo())
                .partnerOrderId(acquisition.getAcquisitionNo())
                .acquisitionId(acquisition.getId())
                .paymentAmount(new BigDecimal("12345.67"))
                .paymentStatus(status.getStatus())
                .receiptNo("R001")
                .paymentTime(LocalDateTime.of(2026, 9, 20, 12, 0))
                .build();
        paymentOrderMapper.insert(payment);
    }

    private void insertInvoice(IcbcAcquisitionDO acquisition, String invoiceNo, InvoiceIssueStatusEnum issueStatus) {
        InvoiceOrderDO invoice = InvoiceOrderDO.builder()
                .orderNo("ORD_" + acquisition.getAcquisitionNo())
                .partnerOrderId(acquisition.getAcquisitionNo())
                .acquisitionId(acquisition.getId())
                .payeeId(acquisition.getPayeeId())
                .totalAmount(new BigDecimal("12345.67"))
                .invoiceAmount(new BigDecimal("12345.67"))
                .invoiceType(1)
                .businessType("SCRAP")
                .invoiceStatus(issueStatus.getStatus())
                .preInvoiceStatus(PreInvoiceStatusEnum.SUCCESS.getStatus())
                .taxStatus(TaxStatusEnum.SUCCESS.getStatus())
                .uploadStatus(UploadStatusEnum.SUCCESS.getStatus())
                .invoiceNo(invoiceNo)
                .invoiceDate(LocalDateTime.of(2026, 9, 20, 13, 0))
                .build();
        invoiceOrderMapper.insert(invoice);
    }

    private PayeeInfoDO insertPayee() {
        PayeeInfoDO payee = PayeeInfoDO.builder()
                .name("张三")
                .idCardNo("110101199001011234")
                .mobile("13800139999")
                .bankCardNo("6222021234567890123")
                .build();
        payeeInfoMapper.insert(payee);
        return payee;
    }

    private void insertPayer() {
        PayerInfoDO payer = new PayerInfoDO();
        payer.setPayerNo("PAYER001");
        payer.setName("某回收企业");
        payer.setCreditCode("91110105MA01R2278M");
        payer.setTaxNo("91110105MA01R2278M");
        payer.setStatus(0);
        payerInfoMapper.insert(payer);
    }

    private void setLoginUser(Long userId) {
        LoginUser loginUser = new LoginUser();
        loginUser.setId(userId);
        loginUser.setUserType(UserTypeEnum.ADMIN.getValue());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, null));
    }

}
