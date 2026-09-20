package cn.iocoder.yudao.module.icbc.service.trace.impl;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.framework.security.core.service.SecurityFrameworkService;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.icbc.controller.admin.trace.vo.TraceAttachmentRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.trace.vo.TraceDifferenceRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.trace.vo.TraceExportRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.trace.vo.TraceHistoryRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.trace.vo.TraceNodeRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.trace.vo.TraceRowRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.trace.vo.TraceSearchReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.trace.vo.TraceSearchRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.trace.vo.TraceSensitiveRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.trace.vo.TraceStageRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.trace.vo.TraceSummaryRespVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.acquisition.IcbcAcquisitionDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.handover.IcbcHandoverBatchDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.handover.IcbcWeighingDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.invoice.InvoiceOrderDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.invoice.OrderItemDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.invoice.RedInvoiceDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payee.PayeeInfoDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payer.PayerInfoDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payment.PaymentOrderDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.purchaseorder.IcbcPurchaseOrderDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.purchaseorder.IcbcPurchaseOrderItemDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.settlement.IcbcSettlementDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.settlement.IcbcSettlementVersionDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.stockin.IcbcStockInDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.stockin.IcbcStockInItemDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.acquisition.IcbcAcquisitionMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.handover.IcbcHandoverBatchMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.handover.IcbcWeighingMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.invoice.InvoiceOrderMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.invoice.OrderItemMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.invoice.RedInvoiceMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.payee.PayeeInfoMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.payer.PayerInfoMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.payment.PaymentOrderMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.purchaseorder.IcbcPurchaseOrderItemMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.purchaseorder.IcbcPurchaseOrderMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.settlement.IcbcSettlementMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.settlement.IcbcSettlementVersionMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.stockin.IcbcStockInItemMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.stockin.IcbcStockInMapper;
import cn.iocoder.yudao.module.icbc.enums.AcquisitionStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.InvoiceIssueStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.PaymentStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.PreInvoiceStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.PurchaseOrderStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.RedOffsetStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.RecyclingPermission;
import cn.iocoder.yudao.module.icbc.enums.SettlementConfirmStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.StockInStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.TaxStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.TraceDifferenceCodeEnum;
import cn.iocoder.yudao.module.icbc.enums.TraceKeywordTypeEnum;
import cn.iocoder.yudao.module.icbc.enums.TraceStageCodeEnum;
import cn.iocoder.yudao.module.icbc.enums.TraceStageStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.UploadStatusEnum;
import cn.iocoder.yudao.module.icbc.service.trace.TraceQueryService;
import cn.iocoder.yudao.module.icbc.util.MaskUtils;
import cn.iocoder.yudao.module.system.api.logger.OperateLogApi;
import cn.iocoder.yudao.module.system.api.logger.dto.OperateLogCreateReqDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.TRACE_ACQUISITION_NOT_EXISTS;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.TRACE_EXPORT_LIMIT_EXCEEDED;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.TRACE_KEYWORD_TYPE_INVALID;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.TRACE_QUERY_CONDITION_REQUIRED;

/**
 * 关联单据查询 Service 实现（#55 T17，只读聚合）。
 *
 * <p>聚合放在本包（{@code service/trace}），不去 import #57 的经营报表类，也不互相复用口径：
 * 追溯只展示链路自身的状态与单据事实，「异常」的权威判定归 #57 的异常表。
 *
 * <p>一次查询只做固定几批取数（收购单 → 关联主体 / 批次 / 订单 / 结算 / 入库 / 付款 / 发票），
 * 不在循环里逐行查库。
 */
@Slf4j
@Service
@Validated
public class TraceQueryServiceImpl implements TraceQueryService {

    /** 单次查询最多扫描多少张收购单（超出部分截断并在筛选范围里写明，避免关键字命中过大时打爆内存）。 */
    private static final int MAX_SCAN = 2000;
    /** 导出上限：导出是一次性动作，命中太多应先缩小筛选范围（或改走按期间报表 #57）。 */
    private static final int EXPORT_LIMIT = 500;

    /** 直接收购口径：未关联采购订单（{@code purchase_order_id = 0}）。 */
    private static final Long NO_PURCHASE_ORDER = 0L;

    @Resource
    private IcbcAcquisitionMapper acquisitionMapper;
    @Resource
    private IcbcHandoverBatchMapper handoverBatchMapper;
    @Resource
    private IcbcWeighingMapper weighingMapper;
    @Resource
    private IcbcPurchaseOrderMapper purchaseOrderMapper;
    @Resource
    private IcbcPurchaseOrderItemMapper purchaseOrderItemMapper;
    @Resource
    private IcbcSettlementMapper settlementMapper;
    @Resource
    private IcbcSettlementVersionMapper settlementVersionMapper;
    @Resource
    private IcbcStockInMapper stockInMapper;
    @Resource
    private IcbcStockInItemMapper stockInItemMapper;
    @Resource
    private PaymentOrderMapper paymentOrderMapper;
    @Resource
    private InvoiceOrderMapper invoiceOrderMapper;
    @Resource
    private OrderItemMapper invoiceItemMapper;
    @Resource
    private RedInvoiceMapper redInvoiceMapper;
    @Resource
    private PayeeInfoMapper payeeInfoMapper;
    @Resource
    private PayerInfoMapper payerInfoMapper;
    @Resource
    private SecurityFrameworkService securityFrameworkService;
    @Resource
    private OperateLogApi operateLogApi;

    // ==================== 查询 ====================

    @Override
    public TraceSearchRespVO search(TraceSearchReqVO reqVO) {
        List<TraceRowRespVO> rows = searchRows(reqVO);
        long total = rows.size();

        TraceSearchRespVO resp = new TraceSearchRespVO();
        resp.setTotal(total);
        resp.setSummary(buildSummary(rows));
        resp.setScopeNote(buildScopeNote(reqVO, total));
        List<TraceRowRespVO> page = pageOf(rows, reqVO);
        resp.setHiddenDetailCount(total - page.size());
        resp.setList(page);
        return resp;
    }

    @Override
    public TraceRowRespVO getTrace(Long acquisitionId, boolean unmask) {
        IcbcAcquisitionDO acquisition = acquisitionMapper.selectById(acquisitionId);
        if (acquisition == null) {
            throw exception(TRACE_ACQUISITION_NOT_EXISTS, acquisitionId);
        }
        TraceContext ctx = loadContext(Collections.singletonList(acquisition));
        return buildRow(acquisition, ctx, unmask);
    }

    @Override
    public void export(TraceSearchReqVO reqVO, HttpServletResponse response) {
        List<TraceRowRespVO> rows = searchRows(reqVO);
        if (rows.size() > EXPORT_LIMIT) {
            throw exception(TRACE_EXPORT_LIMIT_EXCEEDED, rows.size(), EXPORT_LIMIT);
        }
        List<TraceExportRespVO> excelRows = rows.stream().map(this::toExportRow).collect(Collectors.toList());
        try {
            ExcelUtils.write(response, "关联单据查询.xls", "关联单据查询", TraceExportRespVO.class, excelRows);
        } catch (Exception ex) {
            // 导出失败不影响数据一致性，但要让调用方看见
            throw new IllegalStateException("导出关联单据查询失败：" + ex.getMessage(), ex);
        }
        recordExport(rows.size(), reqVO);
    }

    // ==================== 候选集解析（按单号 / 车牌 / 主体反查） ====================

    /**
     * 反查命中的收购单：多个条件之间取交集，结果按 id 倒序、去重、截断。
     */
    private List<IcbcAcquisitionDO> resolveMatched(TraceSearchReqVO reqVO) {
        boolean hasKeyword = StrUtil.isNotBlank(reqVO.getKeyword());
        boolean hasPlate = StrUtil.isNotBlank(reqVO.getPlateNo());
        boolean hasBasic = StrUtil.isNotBlank(reqVO.getAcquisitionNo())
                || StrUtil.isNotBlank(reqVO.getSellerName())
                || (reqVO.getTradeTime() != null && reqVO.getTradeTime().length == 2);
        if (!hasKeyword && !hasPlate && !hasBasic) {
            throw exception(TRACE_QUERY_CONDITION_REQUIRED);
        }

        List<IcbcAcquisitionDO> candidates = null;
        if (hasKeyword) {
            candidates = intersect(candidates, resolveByKeyword(reqVO.getKeyword(), reqVO.getKeywordType()));
        }
        if (hasPlate) {
            candidates = intersect(candidates, resolveByPlate(reqVO.getPlateNo()));
        }
        if (hasBasic) {
            candidates = intersect(candidates, acquisitionMapper.selectListByTraceSearch(reqVO));
        }
        List<IcbcAcquisitionDO> result = dedupe(candidates);
        result.sort(Comparator.comparing(IcbcAcquisitionDO::getId).reversed());
        if (result.size() > MAX_SCAN) {
            result = new ArrayList<>(result.subList(0, MAX_SCAN));
        }
        return result;
    }

    private List<IcbcAcquisitionDO> resolveByKeyword(String keyword, String keywordTypeCode) {
        TraceKeywordTypeEnum type = StrUtil.isBlank(keywordTypeCode)
                ? TraceKeywordTypeEnum.AUTO
                : TraceKeywordTypeEnum.ofCode(keywordTypeCode)
                        .orElseThrow(() -> exception(TRACE_KEYWORD_TYPE_INVALID, keywordTypeCode));
        switch (type) {
            case AUTO:
                return resolveAuto(keyword);
            case ACQUISITION_NO:
                return byAcquisitionNo(keyword);
            case HANDOVER_BATCH_NO:
                return byHandoverBatchNo(keyword);
            case PURCHASE_ORDER_NO:
                return byPurchaseOrderNo(keyword);
            case STOCK_IN_NO:
                return byStockInNo(keyword);
            case SETTLEMENT_NO:
                return bySettlementNo(keyword);
            case INVOICE_NO:
                return byInvoice(keyword);
            case PAYMENT_ORDER_NO:
                return byPaymentOrderNo(keyword);
            case PLATE_NO:
                return resolveByPlate(keyword);
            case SELLER:
                return acquisitionMapper.selectListBySellerKeyword(keyword);
            default:
                return Collections.emptyList();
        }
    }

    /** 自动识别：按固定顺序逐个试，取第一个非空结果（顺序即 {@link TraceKeywordTypeEnum#AUTO} 的口径）。 */
    private List<IcbcAcquisitionDO> resolveAuto(String keyword) {
        IcbcAcquisitionDO acquisition = acquisitionMapper.selectByAcquisitionNo(keyword);
        if (acquisition != null) {
            return Collections.singletonList(acquisition);
        }
        List<IcbcAcquisitionDO> byBatch = byHandoverBatchNo(keyword);
        if (!byBatch.isEmpty()) {
            return byBatch;
        }
        List<IcbcAcquisitionDO> byOrder = byPurchaseOrderNo(keyword);
        if (!byOrder.isEmpty()) {
            return byOrder;
        }
        List<IcbcAcquisitionDO> byStockIn = byStockInNo(keyword);
        if (!byStockIn.isEmpty()) {
            return byStockIn;
        }
        List<IcbcAcquisitionDO> bySettlement = bySettlementNo(keyword);
        if (!bySettlement.isEmpty()) {
            return bySettlement;
        }
        List<IcbcAcquisitionDO> byInvoice = byInvoice(keyword);
        if (!byInvoice.isEmpty()) {
            return byInvoice;
        }
        List<IcbcAcquisitionDO> byPayment = byPaymentOrderNo(keyword);
        if (!byPayment.isEmpty()) {
            return byPayment;
        }
        List<IcbcAcquisitionDO> byPlate = resolveByPlate(keyword);
        if (!byPlate.isEmpty()) {
            return byPlate;
        }
        return acquisitionMapper.selectListBySellerKeyword(keyword);
    }

    private List<IcbcAcquisitionDO> byAcquisitionNo(String acquisitionNo) {
        IcbcAcquisitionDO exact = acquisitionMapper.selectByAcquisitionNo(acquisitionNo);
        if (exact != null) {
            return Collections.singletonList(exact);
        }
        TraceSearchReqVO reqVO = new TraceSearchReqVO();
        reqVO.setAcquisitionNo(acquisitionNo);
        return acquisitionMapper.selectListByTraceSearch(reqVO);
    }

    private List<IcbcAcquisitionDO> byHandoverBatchNo(String batchNo) {
        IcbcHandoverBatchDO batch = handoverBatchMapper.selectByBatchNo(batchNo);
        return batch == null ? Collections.emptyList()
                : acquisitionMapper.selectListByHandoverBatchId(batch.getId());
    }

    private List<IcbcAcquisitionDO> byPurchaseOrderNo(String orderNo) {
        IcbcPurchaseOrderDO order = purchaseOrderMapper.selectByOrderNo(orderNo);
        return order == null ? Collections.emptyList()
                : acquisitionMapper.selectListByPurchaseOrderId(order.getId());
    }

    private List<IcbcAcquisitionDO> byStockInNo(String stockInNo) {
        IcbcStockInDO stockIn = stockInMapper.selectByStockInNo(stockInNo);
        return acquisitionOf(stockIn == null ? null : stockIn.getAcquisitionId());
    }

    private List<IcbcAcquisitionDO> bySettlementNo(String settlementNo) {
        IcbcSettlementDO settlement = settlementMapper.selectBySettlementNo(settlementNo);
        return settlement == null ? Collections.emptyList()
                : acquisitionMapper.selectListBySettlementId(settlement.getId());
    }

    private List<IcbcAcquisitionDO> byInvoice(String invoiceNo) {
        InvoiceOrderDO invoice = invoiceOrderMapper.selectByInvoiceNo(invoiceNo);
        if (invoice == null) {
            invoice = invoiceOrderMapper.selectByPartnerOrderId(invoiceNo);
        }
        if (invoice == null) {
            return Collections.emptyList();
        }
        if (invoice.getAcquisitionId() != null) {
            return acquisitionOf(invoice.getAcquisitionId());
        }
        return acquisitionMapper.selectListByInvoicePartnerOrderIds(
                Collections.singletonList(invoice.getPartnerOrderId()));
    }

    private List<IcbcAcquisitionDO> byPaymentOrderNo(String paymentOrderNo) {
        PaymentOrderDO payment = paymentOrderMapper.selectByOrderNo(paymentOrderNo);
        if (payment == null) {
            payment = paymentOrderMapper.selectByPartnerOrderId(paymentOrderNo);
        }
        if (payment == null) {
            return Collections.emptyList();
        }
        if (payment.getAcquisitionId() != null) {
            return acquisitionOf(payment.getAcquisitionId());
        }
        IcbcAcquisitionDO acquisition = acquisitionMapper.selectByAcquisitionNo(payment.getPartnerOrderId());
        return acquisition == null ? Collections.emptyList() : Collections.singletonList(acquisition);
    }

    /** 车牌反查：收购单上的磅单 / 车辆车牌与交接批次上的车牌都算。 */
    private List<IcbcAcquisitionDO> resolveByPlate(String plateNo) {
        List<IcbcAcquisitionDO> result = new ArrayList<>(acquisitionMapper.selectListByPlateNo(plateNo));
        for (IcbcHandoverBatchDO batch : handoverBatchMapper.selectListByPlateNo(plateNo)) {
            result.addAll(acquisitionMapper.selectListByHandoverBatchId(batch.getId()));
        }
        return dedupe(result);
    }

    private List<IcbcAcquisitionDO> acquisitionOf(Long acquisitionId) {
        if (acquisitionId == null) {
            return Collections.emptyList();
        }
        IcbcAcquisitionDO acquisition = acquisitionMapper.selectById(acquisitionId);
        return acquisition == null ? Collections.emptyList() : Collections.singletonList(acquisition);
    }

    // ==================== 上下文（一批取数，避免 N+1） ====================

    private TraceContext loadContext(List<IcbcAcquisitionDO> acquisitions) {
        TraceContext ctx = new TraceContext();
        if (acquisitions.isEmpty()) {
            return ctx;
        }
        Set<Long> acquisitionIds = acquisitions.stream().map(IcbcAcquisitionDO::getId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> payeeIds = acquisitions.stream().map(IcbcAcquisitionDO::getPayeeId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> batchIds = acquisitions.stream().map(IcbcAcquisitionDO::getHandoverBatchId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> orderIds = acquisitions.stream().map(IcbcAcquisitionDO::getPurchaseOrderId)
                .filter(id -> id != null && !NO_PURCHASE_ORDER.equals(id)).collect(Collectors.toSet());
        Set<Long> settlementIds = acquisitions.stream().map(IcbcAcquisitionDO::getSettlementId)
                .filter(Objects::nonNull).collect(Collectors.toSet());

        ctx.payees = index(payeeIds.isEmpty() ? Collections.emptyList() : payeeInfoMapper.selectByIds(payeeIds),
                PayeeInfoDO::getId);
        ctx.batches = index(batchIds.isEmpty() ? Collections.emptyList() : handoverBatchMapper.selectByIds(batchIds),
                IcbcHandoverBatchDO::getId);
        ctx.orders = index(orderIds.isEmpty() ? Collections.emptyList() : purchaseOrderMapper.selectByIds(orderIds),
                IcbcPurchaseOrderDO::getId);
        ctx.settlements = index(settlementIds.isEmpty() ? Collections.emptyList()
                : settlementMapper.selectByIds(settlementIds), IcbcSettlementDO::getId);

        // 批次磅次 / 订单明细 / 结算版本：量小，逐个查即可
        for (Long batchId : batchIds) {
            ctx.weighingsByBatch.put(batchId, weighingMapper.selectListByBatchId(batchId));
        }
        if (!orderIds.isEmpty()) {
            for (IcbcPurchaseOrderItemDO item : purchaseOrderItemMapper.selectListByOrderIds(orderIds)) {
                ctx.orderItemsByOrder.computeIfAbsent(item.getOrderId(), k -> new ArrayList<>()).add(item);
            }
        }
        for (Long settlementId : settlementIds) {
            ctx.versionsBySettlement.put(settlementId,
                    settlementVersionMapper.selectListBySettlementId(settlementId));
        }

        List<IcbcStockInDO> stockIns = stockInMapper.selectListByAcquisitionIds(acquisitionIds);
        for (IcbcStockInDO stockIn : stockIns) {
            ctx.stockInsByAcquisition.computeIfAbsent(stockIn.getAcquisitionId(), k -> new ArrayList<>())
                    .add(stockIn);
        }
        Set<Long> stockInIds = stockIns.stream().map(IcbcStockInDO::getId).collect(Collectors.toSet());
        if (!stockInIds.isEmpty()) {
            for (IcbcStockInItemDO item : stockInItemMapper.selectListByStockInIds(stockInIds)) {
                ctx.stockInItemsByStockIn.computeIfAbsent(item.getStockInId(), k -> new ArrayList<>()).add(item);
            }
        }
        for (PaymentOrderDO payment : paymentOrderMapper.selectListByAcquisitionIds(acquisitionIds)) {
            ctx.paymentsByAcquisition.computeIfAbsent(payment.getAcquisitionId(), k -> new ArrayList<>())
                    .add(payment);
        }
        for (InvoiceOrderDO invoice : invoiceOrderMapper.selectListByAcquisitionIds(acquisitionIds)) {
            ctx.invoicesByAcquisition.computeIfAbsent(invoice.getAcquisitionId(), k -> new ArrayList<>())
                    .add(invoice);
        }
        // 回收企业税号：租户内通常只有一条付方档案，取第一条（没有就留空，不猜）
        List<PayerInfoDO> payers = payerInfoMapper.selectList();
        ctx.payer = payers.isEmpty() ? null : payers.get(0);
        return ctx;
    }

    // ==================== 组装一行 ====================

    private TraceRowRespVO buildRow(IcbcAcquisitionDO acquisition, TraceContext ctx, boolean unmask) {
        TraceRowRespVO row = new TraceRowRespVO();
        IcbcHandoverBatchDO batch = ctx.batch(acquisition.getHandoverBatchId());
        boolean direct = isDirect(acquisition);
        BigDecimal physicalWeight = acquisition.resolvePhysicalWeight();
        BigDecimal stockedWeight = stockedWeight(ctx, acquisition.getId());

        row.setAnchorType(batch != null ? "HANDOVER_BATCH" : "ACQUISITION");
        row.setHandoverBatchId(batch != null ? batch.getId() : null);
        row.setHandoverBatchNo(batch != null ? batch.getBatchNo() : null);
        row.setAcquisitionId(acquisition.getId());
        row.setAcquisitionNo(acquisition.getAcquisitionNo());
        row.setPayeeId(acquisition.getPayeeId());
        row.setSellerName(acquisition.getSellerName());
        row.setCategoryName(acquisition.getCategoryName());
        row.setSpecification(acquisition.getSpecification());
        row.setUnit(acquisition.getUnit());
        row.setPlateNo(firstNonBlank(acquisition.getVehiclePlateNo(), acquisition.getWeightTicketPlateNo(),
                batch != null ? batch.getPlateNo() : null));
        row.setStationName(batch != null ? batch.getStationName() : null);
        row.setTradeAddress(acquisition.getTradeAddress());
        row.setTradeTime(firstNonNull(acquisition.getTradeTime(), batch != null ? batch.getOccurTime() : null));
        row.setDirectAcquisition(direct);
        row.setPhysicalWeight(physicalWeight);
        row.setSettlementWeight(acquisition.getSettlementWeight());
        row.setStockedWeight(stockedWeight);
        row.setAmount(acquisition.getAmount());
        row.setAcquisitionStatus(acquisition.getStatus());
        row.setAcquisitionStatusName(AcquisitionStatusEnum.ofStatus(acquisition.getStatus())
                .map(AcquisitionStatusEnum::getName).orElse(null));
        row.setSensitiveUnmasked(unmask);
        row.setSensitive(buildSensitive(acquisition, ctx, unmask));

        List<TraceStageRespVO> stages = new ArrayList<>();
        stages.add(purchaseOrderStage(acquisition, ctx));
        stages.add(receiptStage(acquisition, ctx, batch));
        stages.add(stockInStage(acquisition, ctx, physicalWeight, stockedWeight));
        stages.add(settlementStage(acquisition, ctx));
        stages.add(paymentStage(acquisition, ctx));
        stages.add(invoiceStage(acquisition, ctx));
        // 状态名由状态码统一推导，避免每个阶段各自拼一份
        for (TraceStageRespVO stage : stages) {
            stage.setStatusName(TraceStageStatusEnum.nameOf(stage.getStatus()));
        }
        row.setStages(stages);
        row.setDifferences(buildDifferences(acquisition, physicalWeight, stockedWeight));
        row.setHistories(buildHistories(acquisition, ctx, batch));
        row.setAttachments(buildAttachments(acquisition, ctx, batch));
        row.setHiddenNodeCount(0);
        return row;
    }

    /** 采购订单：未关联即「直接收购」= 无需该环节，不是缺失。 */
    private TraceStageRespVO purchaseOrderStage(IcbcAcquisitionDO acquisition, TraceContext ctx) {
        TraceStageRespVO stage = newStage(TraceStageCodeEnum.PURCHASE_ORDER);
        if (isDirect(acquisition)) {
            stage.setStatus(TraceStageStatusEnum.NOT_APPLICABLE.getStatus());
            stage.setNotApplicable(true);
            stage.setNote("直接收购：零散散户未挂采购订单，本环节无需处理（不是缺失，也不虚造订单）");
            return stage;
        }
        IcbcPurchaseOrderDO order = ctx.order(acquisition.getPurchaseOrderId());
        if (order == null) {
            stage.setStatus(TraceStageStatusEnum.EXCEPTION.getStatus());
            stage.setMissingLink(true);
            stage.setNote("收购单关联了采购订单 " + acquisition.getPurchaseOrderId() + "，但订单在本企业不可见");
            return stage;
        }
        IcbcPurchaseOrderItemDO item = ctx.orderItem(acquisition.getPurchaseOrderItemId());
        stage.setStatus(mapOrderStatus(order.getStatus()));
        stage.setQuantity(item != null ? item.getQuantity() : order.getTotalQuantity());
        stage.setAmount(item != null ? item.getAmount() : order.getTotalAmount());
        stage.setUnit(item != null ? item.getUnit() : null);
        stage.setNote("采购订单 " + order.getOrderNo()
                + (item != null ? "，明细品类 " + item.getCategoryName() : ""));

        TraceNodeRespVO orderNode = node("PURCHASE_ORDER", order.getId(), order.getOrderNo(), "采购订单",
                order.getStatus(), PurchaseOrderStatusEnum.nameOf(order.getStatus()),
                order.getTotalQuantity(), null, order.getTotalAmount(), null,
                order.getCreateTime(), "/purchase/order?id=" + order.getId());
        if (item != null) {
            orderNode.setChildren(Collections.singletonList(node("PURCHASE_ORDER_ITEM", item.getId(), null,
                    "订单明细：" + item.getCategoryName(), null, null, item.getQuantity(), null,
                    item.getAmount(), item.getUnit(), null, null)));
        }
        stage.setNodes(Collections.singletonList(orderNode));
        return stage;
    }

    /** 现场收货：收购单本身 + 交接批次与磅次。 */
    private TraceStageRespVO receiptStage(IcbcAcquisitionDO acquisition, TraceContext ctx, IcbcHandoverBatchDO batch) {
        TraceStageRespVO stage = newStage(TraceStageCodeEnum.RECEIPT);
        stage.setQuantity(acquisition.getQuantity());
        stage.setWeight(acquisition.resolvePhysicalWeight());
        stage.setAmount(acquisition.getAmount());
        stage.setUnit(acquisition.getUnit());
        if (Objects.equals(acquisition.getStatus(), AcquisitionStatusEnum.CANCELLED.getStatus())) {
            stage.setStatus(TraceStageStatusEnum.EXCEPTION.getStatus());
            stage.setNote("收购单已作废" + (StrUtil.isNotBlank(acquisition.getCancelReason())
                    ? "：" + acquisition.getCancelReason() : ""));
        } else if (acquisition.getNetWeight() == null) {
            stage.setStatus(TraceStageStatusEnum.IN_PROGRESS.getStatus());
            stage.setNote("已登记，待过磅录入净重");
        } else if (acquisition.getSettlementId() == null) {
            stage.setStatus(TraceStageStatusEnum.IN_PROGRESS.getStatus());
            stage.setNote("已录磅重，待现场「结束本次收货」归入结算单");
        } else {
            stage.setStatus(TraceStageStatusEnum.COMPLETED.getStatus());
            stage.setNote(acquisition.getAcceptedWeight() != null
                    ? "已做接收结论：接收 " + plain(acquisition.getAcceptedWeight())
                    + "，退回 " + plain(acquisition.getRejectedWeight())
                    + "，余货出场 " + plain(acquisition.getResidualWeight())
                    : "已验收归入结算单");
        }

        List<TraceNodeRespVO> nodes = new ArrayList<>();
        if (batch != null) {
            TraceNodeRespVO batchNode = node("HANDOVER_BATCH", batch.getId(), batch.getBatchNo(), "交接批次",
                    null, null, null, null, null, null, batch.getOccurTime(),
                    "/recycling/handover-batch?id=" + batch.getId());
            List<TraceNodeRespVO> weighings = new ArrayList<>();
            for (IcbcWeighingDO weighing : ctx.weighings(batch.getId())) {
                TraceNodeRespVO weighingNode = node("WEIGHING", weighing.getId(), weighing.getWeightTicketNo(),
                        Boolean.TRUE.equals(weighing.getEffective()) ? "有效磅次（第 " + weighing.getSeqNo() + " 次）"
                                : "磅次（第 " + weighing.getSeqNo() + " 次，不参与计量）",
                        null, null, null, weighing.getNetWeight(), null, acquisition.getUnit(),
                        weighing.getWeighTime(), null);
                weighings.add(weighingNode);
            }
            batchNode.setChildren(weighings);
            nodes.add(batchNode);
        }
        TraceNodeRespVO acquisitionNode = node("ACQUISITION", acquisition.getId(), acquisition.getAcquisitionNo(),
                "收购登记单", acquisition.getStatus(), AcquisitionStatusEnum.ofStatus(acquisition.getStatus())
                        .map(AcquisitionStatusEnum::getName).orElse(null),
                acquisition.getQuantity(), acquisition.resolvePhysicalWeight(), acquisition.getAmount(),
                acquisition.getUnit(), acquisition.getTradeTime() != null ? acquisition.getTradeTime()
                        : acquisition.getCreateTime(),
                "/recycling/acquisition?id=" + acquisition.getId());
        nodes.add(acquisitionNode);
        stage.setNodes(nodes);
        return stage;
    }

    /** 仓储入库：只有过账的入库才加库存；待过账不算，已作废按冲销。 */
    private TraceStageRespVO stockInStage(IcbcAcquisitionDO acquisition, TraceContext ctx,
                                           BigDecimal physicalWeight, BigDecimal stockedWeight) {
        TraceStageRespVO stage = newStage(TraceStageCodeEnum.STOCK_IN);
        List<IcbcStockInDO> stockIns = ctx.stockIns(acquisition.getId());
        stage.setWeight(stockedWeight);
        stage.setQuantity(stockedWeight);
        stage.setUnit(acquisition.getUnit());
        if (stockIns.isEmpty()) {
            stage.setStatus(TraceStageStatusEnum.NOT_STARTED.getStatus());
            stage.setNote(acquisition.getSettlementId() != null
                    ? "已结算但查不到入库记录：可能尚未入库，或漏登记了关联（见差异提示）"
                    : "尚未入库");
        } else if (ctx.hasPending(stockIns)) {
            stage.setStatus(TraceStageStatusEnum.IN_PROGRESS.getStatus());
            stage.setNote("有入库单待过账（待过账不加库存）");
        } else if (ctx.allCancelled(stockIns)) {
            stage.setStatus(TraceStageStatusEnum.EXCEPTION.getStatus());
            stage.setNote("入库单已全部作废");
        } else if (stockedWeight.compareTo(nz(physicalWeight)) < 0) {
            stage.setStatus(TraceStageStatusEnum.IN_PROGRESS.getStatus());
            stage.setNote("部分入库：已入库 " + plain(stockedWeight) + "，实物量 " + plain(physicalWeight));
        } else {
            stage.setStatus(TraceStageStatusEnum.COMPLETED.getStatus());
            stage.setNote(null);
        }
        List<TraceNodeRespVO> nodes = new ArrayList<>();
        for (IcbcStockInDO stockIn : stockIns) {
            TraceNodeRespVO node = node("STOCK_IN", stockIn.getId(), stockIn.getStockInNo(), "入库单",
                    stockIn.getStatus(), StockInStatusEnum.ofStatus(stockIn.getStatus())
                            .map(StockInStatusEnum::getName).orElse(null),
                    stockIn.getTotalQuantity(), stockIn.getTotalQuantity(), null, stockIn.getUnit(),
                    stockIn.getPostedTime() != null ? stockIn.getPostedTime() : stockIn.getCreateTime(),
                    "/warehouse/stock-in?id=" + stockIn.getId());
            List<TraceNodeRespVO> items = new ArrayList<>();
            for (IcbcStockInItemDO item : ctx.stockInItems(stockIn.getId())) {
                items.add(node("STOCK_IN_ITEM", item.getId(), null,
                        "仓库 " + item.getWarehouseId() + " / 库位 " + item.getLocationId()
                                + " / 批次 " + item.getBatchId(),
                        null, null, item.getQuantity(), item.getQuantity(), null, stockIn.getUnit(),
                        null, null));
            }
            node.setChildren(items);
            nodes.add(node);
        }
        stage.setNodes(nodes);
        return stage;
    }

    /** 结算确认：结算单与出售者确认状态。 */
    private TraceStageRespVO settlementStage(IcbcAcquisitionDO acquisition, TraceContext ctx) {
        TraceStageRespVO stage = newStage(TraceStageCodeEnum.SETTLEMENT);
        stage.setQuantity(acquisition.getQuantity());
        stage.setWeight(acquisition.getSettlementWeight());
        stage.setAmount(acquisition.getAmount());
        stage.setUnit(acquisition.getUnit());
        if (acquisition.getSettlementId() == null) {
            stage.setStatus(TraceStageStatusEnum.NOT_STARTED.getStatus());
            stage.setNote("尚未归入结算单（现场「结束本次收货」后生成并按出售者确认）");
            return stage;
        }
        IcbcSettlementDO settlement = ctx.settlement(acquisition.getSettlementId());
        if (settlement == null) {
            stage.setStatus(TraceStageStatusEnum.EXCEPTION.getStatus());
            stage.setMissingLink(true);
            stage.setNote("收购单已归入结算单 " + acquisition.getSettlementId() + "，但结算单在本企业不可见");
            return stage;
        }
        SettlementConfirmStatusEnum confirmStatus = SettlementConfirmStatusEnum.ofStatus(settlement.getConfirmStatus())
                .orElse(null);
        stage.setStatus(mapSettlementStatus(confirmStatus));
        stage.setNote("第 " + nzInt(settlement.getCurrentVersionNo()) + " 版・"
                + (confirmStatus != null ? confirmStatus.getName() : "未知状态")
                + (SettlementConfirmStatusEnum.DISPUTED == confirmStatus && StrUtil.isNotBlank(settlement.getDisputeNote())
                        ? "：" + settlement.getDisputeNote() : ""));
        TraceNodeRespVO settlementNode = node("SETTLEMENT", settlement.getId(), settlement.getSettlementNo(),
                "结算单", settlement.getConfirmStatus(),
                confirmStatus != null ? confirmStatus.getName() : null, null, acquisition.getSettlementWeight(),
                acquisition.getAmount(), acquisition.getUnit(), settlement.getGenerateTime(),
                "/settlement/list?id=" + settlement.getId());
        List<TraceNodeRespVO> versions = new ArrayList<>();
        for (IcbcSettlementVersionDO version : ctx.versions(settlement.getId())) {
            versions.add(node("SETTLEMENT_VERSION", version.getId(), null,
                    "第 " + version.getVersionNo() + " 版快照", null, null, null,
                    version.getTotalSettlementWeight(), version.getTotalAmount(), acquisition.getUnit(),
                    version.getCreateTime(), null));
        }
        settlementNode.setChildren(versions);
        stage.setNodes(Collections.singletonList(settlementNode));
        return stage;
    }

    /** 付款：公对私结算的支付单。 */
    private TraceStageRespVO paymentStage(IcbcAcquisitionDO acquisition, TraceContext ctx) {
        TraceStageRespVO stage = newStage(TraceStageCodeEnum.PAYMENT);
        List<PaymentOrderDO> payments = ctx.payments(acquisition.getId());
        if (payments.isEmpty()) {
            stage.setStatus(TraceStageStatusEnum.NOT_STARTED.getStatus());
            stage.setNote("尚未发起付款（预开票成功后按收购单逐笔付款）");
            return stage;
        }
        PaymentOrderDO latest = payments.get(0);
        stage.setStatus(mapPaymentStatus(latest.getPaymentStatus()));
        stage.setAmount(latest.getPaymentAmount());
        stage.setNote(PaymentStatusEnum.nameOf(latest.getPaymentStatus())
                + (latest.getReceiptNo() != null ? "，回单号 " + latest.getReceiptNo() : ""));
        List<TraceNodeRespVO> nodes = new ArrayList<>();
        for (PaymentOrderDO payment : payments) {
            nodes.add(node("PAYMENT", payment.getId(), payment.getOrderNo(), "支付单",
                    payment.getPaymentStatus(), PaymentStatusEnum.nameOf(payment.getPaymentStatus()),
                    null, null, payment.getPaymentAmount(), null,
                    payment.getPaymentTime() != null ? payment.getPaymentTime() : payment.getCreateTime(),
                    "/finance/payment?id=" + payment.getId()));
        }
        stage.setNodes(nodes);
        return stage;
    }

    /** 发票：预下单 / 开票 / 缴税 / 上传四条状态线；红冲单独作为子节点。 */
    private TraceStageRespVO invoiceStage(IcbcAcquisitionDO acquisition, TraceContext ctx) {
        TraceStageRespVO stage = newStage(TraceStageCodeEnum.INVOICE);
        List<InvoiceOrderDO> invoices = ctx.invoices(acquisition.getId());
        if (invoices.isEmpty()) {
            stage.setStatus(TraceStageStatusEnum.NOT_STARTED.getStatus());
            stage.setNote("尚未发起开票申请");
            return stage;
        }
        InvoiceOrderDO latest = invoices.get(0);
        stage.setStatus(mapInvoiceStatus(latest));
        stage.setAmount(latest.getInvoiceAmount() != null ? latest.getInvoiceAmount() : latest.getTotalAmount());
        stage.setNote(invoiceStatusLine(latest));
        List<TraceNodeRespVO> nodes = new ArrayList<>();
        for (InvoiceOrderDO invoice : invoices) {
            // 编号优先给发票号码（已开出时），否则给开票申请订单号；原单仍由 detailPath 打开
            TraceNodeRespVO node = node("INVOICE", invoice.getId(),
                    invoice.getInvoiceNo() != null ? invoice.getInvoiceNo() : invoice.getOrderNo(),
                    "开票申请", invoice.getInvoiceStatus(),
                    InvoiceIssueStatusEnum.nameOf(invoice.getInvoiceStatus()),
                    null, null, invoice.getTotalAmount(), null,
                    invoice.getInvoiceDate() != null ? invoice.getInvoiceDate() : invoice.getCreateTime(),
                    "/finance/invoice?id=" + invoice.getId());
            List<TraceNodeRespVO> children = new ArrayList<>();
            for (OrderItemDO item : invoiceItemMapper.selectListByOrderId(invoice.getId())) {
                children.add(node("INVOICE_ITEM", item.getId(), null, item.getItemName(), null, null,
                        item.getQuantity(), null, item.getAmount(), item.getUnit(), null, null));
            }
            RedInvoiceDO red = redInvoiceMapper.selectLatestByPartnerOrderId(invoice.getPartnerOrderId());
            if (red != null) {
                children.add(node("RED_INVOICE", red.getId(), red.getRedInvoiceNo(), "红字发票（红冲）",
                        red.getRedOffsetStatus(), RedOffsetStatusEnum.nameOf(red.getRedOffsetStatus()),
                        null, null, red.getAmount(), null,
                        red.getRedInvoiceDate() != null ? red.getRedInvoiceDate() : red.getCreateTime(), null));
            }
            node.setChildren(children);
            nodes.add(node);
        }
        stage.setNodes(nodes);
        return stage;
    }

    // ==================== 差异 / 缺失关联（AC4） ====================

    private List<TraceDifferenceRespVO> buildDifferences(IcbcAcquisitionDO acquisition, BigDecimal physicalWeight,
                                                         BigDecimal stockedWeight) {
        List<TraceDifferenceRespVO> differences = new ArrayList<>();
        BigDecimal settlementWeight = acquisition.getSettlementWeight();
        boolean hasSettlement = acquisition.getSettlementId() != null;
        boolean hasStockIn = stockedWeight != null && stockedWeight.signum() > 0;

        if (hasSettlement && !hasStockIn) {
            differences.add(difference(TraceDifferenceCodeEnum.MISSING_STOCK_IN_LINK,
                    settlementWeight, stockedWeight,
                    "已结算（" + plain(settlementWeight) + "）但查不到已过账的入库记录，不能默认一对一；"
                            + "请核实是否尚未入库、入库单还待过账，或漏登记了关联"));
        } else if (settlementWeight != null && stockedWeight != null
                && stockedWeight.compareTo(settlementWeight) != 0) {
            BigDecimal diff = stockedWeight.subtract(settlementWeight);
            differences.add(difference(TraceDifferenceCodeEnum.STOCK_IN_VS_SETTLEMENT,
                    settlementWeight, stockedWeight,
                    "已入库 " + plain(stockedWeight) + "，结算重量 " + plain(settlementWeight)
                            + "，相差 " + plain(diff) + "；结算重量只是计价基准，实物在库另有口径，"
                            + "差额需人工核实（不默认一对一、不静默抹平）"));
        }
        if (!hasSettlement && hasStockIn) {
            differences.add(difference(TraceDifferenceCodeEnum.MISSING_SETTLEMENT_LINK,
                    null, stockedWeight,
                    "已入库 " + plain(stockedWeight) + " 却还没归入结算单；结算确认是开票硬前置"));
        }
        if (acquisition.getWeightDiff() != null && acquisition.getWeightDiff().signum() != 0) {
            differences.add(difference(TraceDifferenceCodeEnum.WEIGHT_DIFF_VS_SETTLEMENT,
                    settlementWeight, physicalWeight,
                    "实物量 " + plain(physicalWeight) + " 与结算重量 " + plain(settlementWeight)
                            + " 相差 " + plain(acquisition.getWeightDiff())
                            + "（扣杂只扣价款、不扣库存；ADR 0028）"));
        }
        return differences;
    }

    private TraceDifferenceRespVO difference(TraceDifferenceCodeEnum code, BigDecimal expected, BigDecimal actual,
                                             String note) {
        TraceDifferenceRespVO vo = new TraceDifferenceRespVO();
        vo.setCode(code.getCode());
        vo.setName(code.getName());
        vo.setDefinition(code.getDefinition());
        vo.setExpectedWeight(expected);
        vo.setActualWeight(actual);
        vo.setDifference(actual == null || expected == null ? null : actual.subtract(expected));
        vo.setNote(note);
        return vo;
    }

    // ==================== 操作历史 / 附件 / 敏感字段 ====================

    private List<TraceHistoryRespVO> buildHistories(IcbcAcquisitionDO acquisition, TraceContext ctx,
                                                    IcbcHandoverBatchDO batch) {
        List<TraceHistoryRespVO> histories = new ArrayList<>();
        if (batch != null) {
            addHistory(histories, batch.getOccurTime(), TraceStageCodeEnum.RECEIPT, "登记交接批次", batch.getBatchNo());
        }
        addHistory(histories, acquisition.getCreateTime(), TraceStageCodeEnum.RECEIPT, "收购登记",
                acquisition.getAcquisitionNo());
        addHistory(histories, acquisition.getTradeTime(), TraceStageCodeEnum.RECEIPT, "现场交易时间",
                "实物量 " + plain(acquisition.resolvePhysicalWeight()) + acquisition.getUnit());
        if (Objects.equals(acquisition.getStatus(), AcquisitionStatusEnum.CANCELLED.getStatus())) {
            addHistory(histories, acquisition.getUpdateTime(), TraceStageCodeEnum.RECEIPT, "作废收购单",
                    acquisition.getCancelReason());
        }
        for (IcbcStockInDO stockIn : ctx.stockIns(acquisition.getId())) {
            addHistory(histories, stockIn.getCreateTime(), TraceStageCodeEnum.STOCK_IN, "登记入库单",
                    stockIn.getStockInNo());
            addHistory(histories, stockIn.getPostedTime(), TraceStageCodeEnum.STOCK_IN, "入库过账",
                    stockIn.getStockInNo() + "，" + plain(stockIn.getTotalQuantity()) + stockIn.getUnit());
            addHistory(histories, stockIn.getCancelledTime(), TraceStageCodeEnum.STOCK_IN, "作废入库单",
                    stockIn.getStockInNo() + (StrUtil.isNotBlank(stockIn.getCancelReason())
                            ? "：" + stockIn.getCancelReason() : ""));
        }
        IcbcSettlementDO settlement = ctx.settlement(acquisition.getSettlementId());
        if (settlement != null) {
            addHistory(histories, settlement.getGenerateTime(), TraceStageCodeEnum.SETTLEMENT, "生成结算单",
                    settlement.getSettlementNo());
            addHistory(histories, settlement.getConfirmTime(), TraceStageCodeEnum.SETTLEMENT, "出售者确认",
                    settlement.getSettlementNo());
            addHistory(histories, settlement.getDisputeTime(), TraceStageCodeEnum.SETTLEMENT, "出售者提异议",
                    settlement.getDisputeReason());
            addHistory(histories, settlement.getEnterpriseReplyTime(), TraceStageCodeEnum.SETTLEMENT, "企业处理异议",
                    settlement.getEnterpriseReplyNote());
            addHistory(histories, settlement.getOfflineSignTime(), TraceStageCodeEnum.SETTLEMENT, "线下签字确认",
                    settlement.getOfflineSignHandler());
        }
        for (PaymentOrderDO payment : ctx.payments(acquisition.getId())) {
            addHistory(histories, payment.getPaymentTime(), TraceStageCodeEnum.PAYMENT, "付款结果",
                    payment.getOrderNo() + "：" + PaymentStatusEnum.nameOf(payment.getPaymentStatus()));
            addHistory(histories, payment.getReceiptTime(), TraceStageCodeEnum.PAYMENT, "回单归档",
                    payment.getReceiptNo());
        }
        for (InvoiceOrderDO invoice : ctx.invoices(acquisition.getId())) {
            addHistory(histories, invoice.getPreOrderTime(), TraceStageCodeEnum.INVOICE, "预下单",
                    invoice.getOrderNo());
            addHistory(histories, invoice.getInvoiceDate(), TraceStageCodeEnum.INVOICE, "开票",
                    invoice.getInvoiceNo());
            addHistory(histories, invoice.getTaxTime(), TraceStageCodeEnum.INVOICE, "缴税",
                    invoice.getTaxVoucherNo());
        }
        histories.sort(Comparator.comparing(TraceHistoryRespVO::getTime,
                Comparator.nullsLast(Comparator.naturalOrder())));
        return histories;
    }

    /** 只有带真实时点的业务事件才进操作历史；没有时间字段的事件不乱编造。 */
    private void addHistory(List<TraceHistoryRespVO> histories, LocalDateTime time, TraceStageCodeEnum stage,
                            String action, String detail) {
        if (time == null) {
            return;
        }
        TraceHistoryRespVO vo = new TraceHistoryRespVO();
        vo.setTime(time);
        vo.setStageCode(stage.getCode());
        vo.setStageName(stage.getName());
        vo.setAction(action);
        vo.setDetail(detail);
        histories.add(vo);
    }

    private List<TraceAttachmentRespVO> buildAttachments(IcbcAcquisitionDO acquisition, TraceContext ctx,
                                                         IcbcHandoverBatchDO batch) {
        List<TraceAttachmentRespVO> attachments = new ArrayList<>();
        addAttachment(attachments, "磅单照片", acquisition.getWeightTicketImageUrl(), "ACQUISITION",
                acquisition.getAcquisitionNo());
        addAttachment(attachments, "车头照片", acquisition.getVehicleFrontImageUrl(), "ACQUISITION",
                acquisition.getAcquisitionNo());
        addAttachment(attachments, "车尾照片", acquisition.getVehicleRearImageUrl(), "ACQUISITION",
                acquisition.getAcquisitionNo());
        if (batch != null) {
            for (IcbcWeighingDO weighing : ctx.weighings(batch.getId())) {
                addAttachment(attachments, "磅次 " + weighing.getSeqNo() + " 磅单照片",
                        weighing.getWeightTicketImageUrl(), "WEIGHING", weighing.getWeightTicketNo());
            }
        }
        IcbcSettlementDO settlement = ctx.settlement(acquisition.getSettlementId());
        if (settlement != null) {
            addAttachment(attachments, "线下签字确认书", settlement.getOfflineSignFileUrl(), "SETTLEMENT",
                    settlement.getSettlementNo());
        }
        for (PaymentOrderDO payment : ctx.payments(acquisition.getId())) {
            addAttachment(attachments, "转账回单", payment.getReceiptFileUrl(), "PAYMENT", payment.getOrderNo());
        }
        for (InvoiceOrderDO invoice : ctx.invoices(acquisition.getId())) {
            addAttachment(attachments, "发票原件", invoice.getInvoiceFileUrl(), "INVOICE", invoice.getInvoiceNo());
        }
        return attachments;
    }

    private void addAttachment(List<TraceAttachmentRespVO> attachments, String name, String url,
                               String sourceType, String sourceNo) {
        if (StrUtil.isBlank(url)) {
            return;
        }
        TraceAttachmentRespVO vo = new TraceAttachmentRespVO();
        vo.setName(name);
        vo.setUrl(url);
        vo.setSourceType(sourceType);
        vo.setSourceNo(sourceNo);
        attachments.add(vo);
    }

    private TraceSensitiveRespVO buildSensitive(IcbcAcquisitionDO acquisition, TraceContext ctx, boolean unmask) {
        PayeeInfoDO payee = ctx.payee(acquisition.getPayeeId());
        String idCard = payee != null ? payee.getIdCardNo() : null;
        String mobile = firstNonBlank(payee != null ? payee.getMobile() : null, acquisition.getSellerMobile());
        String bankCard = payee != null ? payee.getBankCardNo() : null;
        String taxNo = ctx.payer != null ? ctx.payer.getTaxNo() : null;

        TraceSensitiveRespVO vo = new TraceSensitiveRespVO();
        vo.setBuyerTaxNo(unmask ? taxNo : MaskUtils.maskTaxNo(taxNo));
        vo.setSellerIdCard(unmask ? idCard : MaskUtils.maskIdCard(idCard));
        vo.setSellerMobile(unmask ? mobile : MaskUtils.maskMobile(mobile));
        vo.setSellerBankCard(unmask ? bankCard : MaskUtils.maskBankCard(bankCard));
        return vo;
    }

    // ==================== 汇总 / 筛选范围 / 导出 ====================

    private List<TraceRowRespVO> searchRows(TraceSearchReqVO reqVO) {
        List<IcbcAcquisitionDO> matched = resolveMatched(reqVO);
        TraceContext ctx = loadContext(matched);
        boolean unmask = canViewSensitive();
        List<TraceRowRespVO> rows = new ArrayList<>();
        for (IcbcAcquisitionDO acquisition : matched) {
            rows.add(buildRow(acquisition, ctx, unmask));
        }
        if (Boolean.TRUE.equals(reqVO.getOnlyDifference())) {
            rows.removeIf(row -> row.getDifferences().isEmpty());
        }
        return rows;
    }

    private List<TraceRowRespVO> pageOf(List<TraceRowRespVO> rows, TraceSearchReqVO reqVO) {
        int pageNo = reqVO.getPageNo() == null || reqVO.getPageNo() < 1 ? 1 : reqVO.getPageNo();
        int pageSize = reqVO.getPageSize() == null || reqVO.getPageSize() < 1 ? 10 : reqVO.getPageSize();
        int from = Math.min((pageNo - 1) * pageSize, rows.size());
        int to = Math.min(from + pageSize, rows.size());
        return new ArrayList<>(rows.subList(from, to));
    }

    private TraceSummaryRespVO buildSummary(List<TraceRowRespVO> rows) {
        TraceSummaryRespVO summary = new TraceSummaryRespVO();
        summary.setAcquisitionCount((long) rows.size());
        summary.setTotalSettlementWeight(sum(rows, TraceRowRespVO::getSettlementWeight));
        summary.setTotalStockedWeight(sum(rows, TraceRowRespVO::getStockedWeight));
        summary.setTotalAmount(sum(rows, TraceRowRespVO::getAmount));
        summary.setDifferenceCount(rows.stream().filter(row -> !row.getDifferences().isEmpty()).count());
        summary.setCountNote("合计口径：本次筛选命中的每张收购单各计一次，不把同一结算单 / 采购订单重复相加；"
                + "金额取自收购单，不从库存反推。");
        return summary;
    }

    private BigDecimal sum(List<TraceRowRespVO> rows,
                           java.util.function.Function<TraceRowRespVO, BigDecimal> getter) {
        BigDecimal total = BigDecimal.ZERO;
        for (TraceRowRespVO row : rows) {
            BigDecimal value = getter.apply(row);
            if (value != null) {
                total = total.add(value);
            }
        }
        return total;
    }

    private String buildScopeNote(TraceSearchReqVO reqVO, long total) {
        List<String> parts = new ArrayList<>();
        if (StrUtil.isNotBlank(reqVO.getKeyword())) {
            TraceKeywordTypeEnum type = StrUtil.isBlank(reqVO.getKeywordType()) ? TraceKeywordTypeEnum.AUTO
                    : TraceKeywordTypeEnum.ofCode(reqVO.getKeywordType()).orElse(TraceKeywordTypeEnum.AUTO);
            parts.add(type.getName() + "「" + reqVO.getKeyword() + "」");
        }
        if (StrUtil.isNotBlank(reqVO.getAcquisitionNo())) {
            parts.add("收购单号「" + reqVO.getAcquisitionNo() + "」");
        }
        if (StrUtil.isNotBlank(reqVO.getPlateNo())) {
            parts.add("车牌「" + reqVO.getPlateNo() + "」");
        }
        if (StrUtil.isNotBlank(reqVO.getSellerName())) {
            parts.add("出售者「" + reqVO.getSellerName() + "」");
        }
        if (reqVO.getTradeTime() != null && reqVO.getTradeTime().length == 2) {
            parts.add("交易时间 " + reqVO.getTradeTime()[0] + " ~ " + reqVO.getTradeTime()[1]);
        }
        if (Boolean.TRUE.equals(reqVO.getOnlyDifference())) {
            parts.add("只看有差异 / 缺失关联");
        }
        String scope = parts.isEmpty() ? "未限定条件" : String.join("、", parts);
        return "筛选范围：" + scope + "；共命中 " + total + " 张收购单（每张只计一次）。"
                + (total >= MAX_SCAN ? "命中已达扫描上限 " + MAX_SCAN + "，请缩小范围。" : "");
    }

    private TraceExportRespVO toExportRow(TraceRowRespVO row) {
        TraceExportRespVO export = new TraceExportRespVO();
        export.setAcquisitionNo(row.getAcquisitionNo());
        export.setHandoverBatchNo(row.getHandoverBatchNo());
        export.setTradeTime(row.getTradeTime());
        export.setSellerName(row.getSellerName());
        if (row.getSensitive() != null) {
            export.setSellerIdCard(row.getSensitive().getSellerIdCard());
            export.setSellerMobile(row.getSensitive().getSellerMobile());
            export.setSellerBankCard(row.getSensitive().getSellerBankCard());
        }
        export.setPlateNo(row.getPlateNo());
        export.setCategoryName(row.getCategoryName());
        export.setUnit(row.getUnit());
        export.setPhysicalWeight(row.getPhysicalWeight());
        export.setSettlementWeight(row.getSettlementWeight());
        export.setStockedWeight(row.getStockedWeight());
        if (row.getStockedWeight() != null && row.getSettlementWeight() != null) {
            export.setStockInSettlementDiff(row.getStockedWeight().subtract(row.getSettlementWeight()));
        }
        export.setPurchaseArrangement(Boolean.TRUE.equals(row.getDirectAcquisition())
                ? "直接收购" : purchaseOrderNoOf(row));
        export.setSettlementNo(nodeNoOf(row, TraceStageCodeEnum.SETTLEMENT));
        export.setSettlementStatusName(stageStatusNameOf(row, TraceStageCodeEnum.SETTLEMENT));
        export.setPaymentStatusName(stageStatusNameOf(row, TraceStageCodeEnum.PAYMENT));
        export.setInvoiceNo(invoiceNoOf(row));
        export.setDifferenceNote(row.getDifferences().isEmpty() ? null
                : row.getDifferences().stream().map(TraceDifferenceRespVO::getNote).collect(Collectors.joining("；")));
        return export;
    }

    private String purchaseOrderNoOf(TraceRowRespVO row) {
        TraceStageRespVO stage = stageOf(row, TraceStageCodeEnum.PURCHASE_ORDER);
        return stage != null && !stage.getNodes().isEmpty() ? stage.getNodes().get(0).getBizNo() : null;
    }

    private String nodeNoOf(TraceRowRespVO row, TraceStageCodeEnum code) {
        TraceStageRespVO stage = stageOf(row, code);
        return stage != null && stage.getNodes() != null && !stage.getNodes().isEmpty()
                ? stage.getNodes().get(0).getBizNo() : null;
    }

    private String stageStatusNameOf(TraceRowRespVO row, TraceStageCodeEnum code) {
        TraceStageRespVO stage = stageOf(row, code);
        return stage != null ? stage.getStatusName() : null;
    }

    private TraceStageRespVO stageOf(TraceRowRespVO row, TraceStageCodeEnum code) {
        if (row.getStages() == null) {
            return null;
        }
        return row.getStages().stream().filter(stage -> code.getCode().equals(stage.getCode())).findFirst()
                .orElse(null);
    }

    private String invoiceNoOf(TraceRowRespVO row) {
        // 开票阶段节点编号已是发票号码（未开出时是开票申请订单号）
        return nodeNoOf(row, TraceStageCodeEnum.INVOICE);
    }

    /** 导出留记录：写一条系统操作日志；取不到登录用户时跳过（如后台任务 / 测试）。 */
    private void recordExport(int count, TraceSearchReqVO reqVO) {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        if (userId == null) {
            return;
        }
        try {
            OperateLogCreateReqDTO dto = new OperateLogCreateReqDTO();
            dto.setUserId(userId);
            dto.setUserType(UserTypeEnum.ADMIN.getValue());
            dto.setType("ICBC 关联单据查询");
            dto.setSubType("导出");
            dto.setBizId(0L);
            dto.setAction("导出关联单据查询结果 " + count + " 条；" + buildScopeNote(reqVO, count));
            operateLogApi.createOperateLog(dto);
        } catch (Exception ex) {
            // 留记录失败不能反过来把导出结果判为失败
            log.warn("[recordExport][导出留记录失败]", ex);
        }
    }

    private boolean canViewSensitive() {
        try {
            return securityFrameworkService.hasPermission(RecyclingPermission.TRACE_SENSITIVE_VIEW);
        } catch (Exception ex) {
            // 权限服务不可用时按脱敏返回（安全默认）
            return false;
        }
    }

    // ==================== 状态映射 ====================

    private Integer mapOrderStatus(Integer status) {
        if (status == null) {
            return TraceStageStatusEnum.NOT_STARTED.getStatus();
        }
        if (PurchaseOrderStatusEnum.SUSPENDED.getStatus().equals(status)
                || PurchaseOrderStatusEnum.CLOSED.getStatus().equals(status)) {
            return TraceStageStatusEnum.EXCEPTION.getStatus();
        }
        if (PurchaseOrderStatusEnum.COMPLETED.getStatus().equals(status)) {
            return TraceStageStatusEnum.COMPLETED.getStatus();
        }
        return TraceStageStatusEnum.IN_PROGRESS.getStatus();
    }

    private Integer mapSettlementStatus(SettlementConfirmStatusEnum confirmStatus) {
        if (confirmStatus == null) {
            return TraceStageStatusEnum.NOT_STARTED.getStatus();
        }
        switch (confirmStatus) {
            case CONFIRMED:
            case OFFLINE_CONFIRMED:
                return TraceStageStatusEnum.COMPLETED.getStatus();
            case DISPUTED:
                return TraceStageStatusEnum.EXCEPTION.getStatus();
            case PENDING:
            case OFFLINE_REQUIRED:
            default:
                return TraceStageStatusEnum.IN_PROGRESS.getStatus();
        }
    }

    private Integer mapPaymentStatus(Integer status) {
        if (status == null) {
            return TraceStageStatusEnum.NOT_STARTED.getStatus();
        }
        if (PaymentStatusEnum.exceptionStatuses().contains(status)) {
            return TraceStageStatusEnum.EXCEPTION.getStatus();
        }
        if (PaymentStatusEnum.SUCCESS.getStatus().equals(status)) {
            return TraceStageStatusEnum.COMPLETED.getStatus();
        }
        return TraceStageStatusEnum.IN_PROGRESS.getStatus();
    }

    private Integer mapInvoiceStatus(InvoiceOrderDO invoice) {
        if (PreInvoiceStatusEnum.isException(invoice.getPreInvoiceStatus())
                || InvoiceIssueStatusEnum.isException(invoice.getInvoiceStatus())
                || TaxStatusEnum.isException(invoice.getTaxStatus())
                || UploadStatusEnum.isException(invoice.getUploadStatus())) {
            return TraceStageStatusEnum.EXCEPTION.getStatus();
        }
        if (InvoiceIssueStatusEnum.ISSUED.getStatus().equals(invoice.getInvoiceStatus())) {
            return TraceStageStatusEnum.COMPLETED.getStatus();
        }
        return TraceStageStatusEnum.IN_PROGRESS.getStatus();
    }

    private String invoiceStatusLine(InvoiceOrderDO invoice) {
        return "预开票：" + PreInvoiceStatusEnum.ofStatus(invoice.getPreInvoiceStatus())
                .map(PreInvoiceStatusEnum::getName).orElse("未知")
                + "；开票：" + InvoiceIssueStatusEnum.nameOf(invoice.getInvoiceStatus())
                + "；缴税：" + TaxStatusEnum.nameOf(invoice.getTaxStatus())
                + "；上传：" + UploadStatusEnum.nameOf(invoice.getUploadStatus());
    }

    // ==================== 小工具 ====================

    private TraceStageRespVO newStage(TraceStageCodeEnum code) {
        TraceStageRespVO stage = new TraceStageRespVO();
        stage.setCode(code.getCode());
        stage.setName(code.getName());
        stage.setDefinition(code.getDefinition());
        stage.setNotApplicable(false);
        stage.setMissingLink(false);
        stage.setNodes(new ArrayList<>());
        return stage;
    }

    private TraceNodeRespVO node(String bizType, Long bizId, String bizNo, String title, Integer status,
                                 String statusName, BigDecimal quantity, BigDecimal weight, BigDecimal amount,
                                 String unit, LocalDateTime time, String detailPath) {
        TraceNodeRespVO node = new TraceNodeRespVO();
        node.setBizType(bizType);
        node.setBizId(bizId);
        node.setBizNo(bizNo);
        node.setTitle(title);
        node.setStatus(status);
        node.setStatusName(statusName);
        node.setQuantity(quantity);
        node.setWeight(weight);
        node.setAmount(amount);
        node.setUnit(unit);
        node.setTime(time);
        node.setDetailPath(detailPath);
        return node;
    }

    private static boolean isDirect(IcbcAcquisitionDO acquisition) {
        return acquisition.getPurchaseOrderId() == null
                || NO_PURCHASE_ORDER.equals(acquisition.getPurchaseOrderId());
    }

    /** 已过账入库合计：口径与 {@code StockInService#getStockedQuantity} 一致（待过账不计，已作废不在其中）。 */
    private BigDecimal stockedWeight(TraceContext ctx, Long acquisitionId) {
        BigDecimal total = BigDecimal.ZERO;
        for (IcbcStockInDO stockIn : ctx.stockIns(acquisitionId)) {
            if (StockInStatusEnum.POSTED.getStatus().equals(stockIn.getStatus()) && stockIn.getTotalQuantity() != null) {
                total = total.add(stockIn.getTotalQuantity());
            }
        }
        return total;
    }

    private static <K, V> Map<K, V> index(List<V> list, java.util.function.Function<V, K> keyFn) {
        Map<K, V> map = new LinkedHashMap<>();
        for (V value : list) {
            map.put(keyFn.apply(value), value);
        }
        return map;
    }

    private List<IcbcAcquisitionDO> intersect(List<IcbcAcquisitionDO> left, List<IcbcAcquisitionDO> right) {
        if (left == null) {
            return dedupe(right);
        }
        if (right == null || right.isEmpty()) {
            return new ArrayList<>();
        }
        Set<Long> rightIds = right.stream().map(IcbcAcquisitionDO::getId).collect(Collectors.toSet());
        return left.stream().filter(acquisition -> rightIds.contains(acquisition.getId()))
                .collect(Collectors.toList());
    }

    private List<IcbcAcquisitionDO> dedupe(List<IcbcAcquisitionDO> list) {
        Map<Long, IcbcAcquisitionDO> map = new LinkedHashMap<>();
        if (list != null) {
            for (IcbcAcquisitionDO acquisition : list) {
                map.putIfAbsent(acquisition.getId(), acquisition);
            }
        }
        return new ArrayList<>(map.values());
    }

    private static BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static String plain(BigDecimal value) {
        return value == null ? "-" : value.stripTrailingZeros().toPlainString();
    }

    private static String nzInt(Integer value) {
        return value == null ? "-" : String.valueOf(value);
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (StrUtil.isNotBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private static <T> T firstNonNull(T first, T second) {
        return first != null ? first : second;
    }

    /**
     * 一次查询的关联数据快照（按需批量取数，避免逐行查询）。
     */
    private static final class TraceContext {

        private Map<Long, PayeeInfoDO> payees = Collections.emptyMap();
        private Map<Long, IcbcHandoverBatchDO> batches = Collections.emptyMap();
        private Map<Long, IcbcPurchaseOrderDO> orders = Collections.emptyMap();
        private Map<Long, IcbcSettlementDO> settlements = Collections.emptyMap();
        private PayerInfoDO payer;

        private final Map<Long, List<IcbcWeighingDO>> weighingsByBatch = new HashMap<>();
        private final Map<Long, List<IcbcPurchaseOrderItemDO>> orderItemsByOrder = new HashMap<>();
        private final Map<Long, List<IcbcSettlementVersionDO>> versionsBySettlement = new HashMap<>();
        private final Map<Long, List<IcbcStockInDO>> stockInsByAcquisition = new HashMap<>();
        private final Map<Long, List<IcbcStockInItemDO>> stockInItemsByStockIn = new HashMap<>();
        private final Map<Long, List<PaymentOrderDO>> paymentsByAcquisition = new HashMap<>();
        private final Map<Long, List<InvoiceOrderDO>> invoicesByAcquisition = new HashMap<>();

        private PayeeInfoDO payee(Long id) {
            return id == null ? null : payees.get(id);
        }

        private IcbcHandoverBatchDO batch(Long id) {
            return id == null ? null : batches.get(id);
        }

        private IcbcPurchaseOrderDO order(Long id) {
            return id == null ? null : orders.get(id);
        }

        private IcbcPurchaseOrderItemDO orderItem(Long id) {
            if (id == null) {
                return null;
            }
            return orderItemsByOrder.values().stream().flatMap(Collection::stream)
                    .filter(item -> item.getId().equals(id)).findFirst().orElse(null);
        }

        private IcbcSettlementDO settlement(Long id) {
            return id == null ? null : settlements.get(id);
        }

        private List<IcbcWeighingDO> weighings(Long batchId) {
            return weighingsByBatch.getOrDefault(batchId, Collections.emptyList());
        }

        private List<IcbcSettlementVersionDO> versions(Long settlementId) {
            return versionsBySettlement.getOrDefault(settlementId, Collections.emptyList());
        }

        private List<IcbcStockInDO> stockIns(Long acquisitionId) {
            return stockInsByAcquisition.getOrDefault(acquisitionId, Collections.emptyList());
        }

        private List<IcbcStockInItemDO> stockInItems(Long stockInId) {
            return stockInItemsByStockIn.getOrDefault(stockInId, Collections.emptyList());
        }

        private List<PaymentOrderDO> payments(Long acquisitionId) {
            return paymentsByAcquisition.getOrDefault(acquisitionId, Collections.emptyList());
        }

        private List<InvoiceOrderDO> invoices(Long acquisitionId) {
            return invoicesByAcquisition.getOrDefault(acquisitionId, Collections.emptyList());
        }

        private boolean hasPending(List<IcbcStockInDO> stockIns) {
            return stockIns.stream().anyMatch(stockIn ->
                    StockInStatusEnum.PENDING.getStatus().equals(stockIn.getStatus()));
        }

        private boolean allCancelled(List<IcbcStockInDO> stockIns) {
            return stockIns.stream().allMatch(stockIn ->
                    StockInStatusEnum.CANCELLED.getStatus().equals(stockIn.getStatus()));
        }

    }

}
