package cn.iocoder.yudao.module.icbc.service.report.impl;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.api.stock.StockReportApi;
import cn.iocoder.yudao.module.erp.api.stock.dto.StockBalanceRespDTO;
import cn.iocoder.yudao.module.erp.api.stock.dto.StockRecordRespDTO;
import cn.iocoder.yudao.module.erp.api.stock.dto.StockReportQueryDTO;
import cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo.PurchaseOrderPageReqVO;
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
import cn.iocoder.yudao.module.icbc.dal.dataobject.goodscfg.IcbcGoodsConfigDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payment.PaymentOrderDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.purchaseorder.IcbcPurchaseOrderDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.settlement.IcbcSettlementDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.station.IcbcStationDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.stockin.IcbcStockInDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.acquisition.IcbcAcquisitionMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.goodscfg.IcbcGoodsConfigMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.payment.PaymentOrderMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.purchaseorder.IcbcPurchaseOrderMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.settlement.IcbcSettlementMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.station.IcbcStationMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.stockin.IcbcStockInMapper;
import cn.iocoder.yudao.module.icbc.enums.AcquisitionStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.PaymentStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.PurchaseOrderStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.ReportAnomalyTypeEnum;
import cn.iocoder.yudao.module.icbc.enums.ReportTableEnum;
import cn.iocoder.yudao.module.icbc.enums.SettlementConfirmStatusEnum;
import cn.iocoder.yudao.module.icbc.service.purchaseorder.PurchaseOrderService;
import cn.iocoder.yudao.module.icbc.service.report.ReportService;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.REPORT_ANOMALY_TYPE_UNKNOWN;

/**
 * 经营报表 Service 实现（#57 T19）。
 *
 * <p>只读聚合，四张表 + 一张派生异常表。跨域取数只有两个口子：
 * <ul>
 *   <li>采购履约：{@code PurchaseOrderService#getProgress}（#47 的五口径唯一来源，本包不重算）；</li>
 *   <li>库存：{@code StockReportApi}（ERP 库存域只读端口，icbc 不直接碰 {@code erp_stock*}）。</li>
 * </ul>
 * 其余的收购 / 结算 / 付款数据都在 icbc 自己的表上，直接读 Mapper。
 *
 * <p>异常表是**派生清单**：磅差读 #53 已落库的 {@code weight_diff}，超采购量取自 #47 的履约口径，
 * 超入库量对比 #52 的累计入库与实物量，重复关联查付款单，长期未确认查结算单，资料缺失查收购单要件。
 * 一条业务事实都不新写。
 */
@Slf4j
@Service
@Validated
public class ReportServiceImpl implements ReportService {

    /** 异常表每类最多扫描的来源行数（派生清单规模有限，超出部分提示用户去对应模块筛） */
    private static final int ANOMALY_SCAN_LIMIT = 500;

    /** 超采购量逐单取履约进度，最多扫描的订单数（避免全表逐单调用） */
    private static final int MAX_ORDER_SCAN = 200;

    /** 结算单未设确认截止时间时，多久算「长期未确认」 */
    private static final long DEFAULT_UNCONFIRMED_HOURS = 48L;

    /** 未作废付款单：订单关闭（4）/ 已冲正（5）不算「有效关联」 */
    private static final Set<Integer> INACTIVE_PAYMENT_STATUSES =
            Set.of(PaymentStatusEnum.CLOSED.getStatus(), PaymentStatusEnum.REVERSED.getStatus());

    /** 已过账入库单状态（见 {@code IcbcStockInDO#status}） */
    private static final Integer POSTED_STOCK_IN = 1;

    /** 结算单上「还没确认」的状态：待确认 / 有异议 / 需线下签字确认 */
    private static final Set<Integer> UNCONFIRMED_SETTLEMENT_STATUSES = Set.of(
            SettlementConfirmStatusEnum.PENDING.getStatus(),
            SettlementConfirmStatusEnum.DISPUTED.getStatus(),
            SettlementConfirmStatusEnum.OFFLINE_REQUIRED.getStatus());

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
    private PurchaseOrderService purchaseOrderService;
    @Resource
    private StockReportApi stockReportApi;

    // ==================== 表清单 ====================

    @Override
    public List<ReportTableRespVO> getTables() {
        return java.util.Arrays.stream(ReportTableEnum.values()).map(table -> {
            ReportTableRespVO vo = new ReportTableRespVO();
            vo.setCode(table.getCode());
            vo.setName(table.getName());
            vo.setDefinition(table.getDefinition());
            return vo;
        }).collect(Collectors.toList());
    }

    // ==================== 一、采购履约表 ====================

    @Override
    public PageResult<ReportPurchasePerformanceRespVO> getPurchasePerformancePage(
            @Valid ReportPurchasePerformancePageReqVO reqVO) {
        PurchaseOrderPageReqVO pageReqVO = new PurchaseOrderPageReqVO();
        pageReqVO.setOrderNo(reqVO.getOrderNo());
        pageReqVO.setCounterpartyName(reqVO.getCounterpartyName());
        pageReqVO.setStatus(reqVO.getStatus());
        pageReqVO.setPageNo(reqVO.getPageNo());
        pageReqVO.setPageSize(reqVO.getPageSize());

        PageResult<IcbcPurchaseOrderDO> page = purchaseOrderMapper.selectPage(pageReqVO);
        if (page.getList().isEmpty()) {
            return PageResult.empty(page.getTotal());
        }
        List<ReportPurchasePerformanceRespVO> list = page.getList().stream()
                .map(this::toPurchasePerformance)
                .collect(Collectors.toList());
        return new PageResult<>(list, page.getTotal());
    }

    private ReportPurchasePerformanceRespVO toPurchasePerformance(IcbcPurchaseOrderDO order) {
        // 五口径与完成比例直接取自 #47 的 getProgress，报表不重算（口径以 PurchaseProgressMeasureEnum 为准）
        PurchaseOrderProgressRespVO progress = purchaseOrderService.getProgress(order.getId());
        ReportPurchasePerformanceRespVO vo = new ReportPurchasePerformanceRespVO();
        vo.setOrderId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setCounterpartyName(order.getCounterpartyName());
        vo.setStationName(order.getStationName());
        vo.setStatusName(progress.getStatusName());
        vo.setEndDate(order.getEndDate());
        vo.setExpired(order.getEndDate() != null && order.getEndDate().isBefore(LocalDate.now()));
        vo.setPlanQuantity(progress.getPlanQuantity());
        vo.setPerformedQuantity(progress.getCompletionBasisQuantity());
        vo.setBalanceQuantity(progress.getUnperformedQuantity());
        vo.setAcceptedQuantity(progress.getAcceptedQuantity());
        vo.setStockedQuantity(progress.getStockedQuantity());
        vo.setSettledQuantity(progress.getSettledQuantity());
        vo.setCompletionBasis(progress.getCompletionBasis());
        vo.setCompletionBasisName(progress.getCompletionBasisName());
        vo.setCompletionRatio(progress.getCompletionRatio());
        boolean over = progress.getAnomalies() != null && progress.getAnomalies().stream()
                .anyMatch(anomaly -> "OVER_QUANTITY".equals(anomaly.getCode()));
        vo.setOverQuantity(over);
        vo.setAnomalyNames(progress.getAnomalies() == null ? Collections.emptyList()
                : progress.getAnomalies().stream().map(PurchaseOrderProgressRespVO.Anomaly::getName)
                .collect(Collectors.toList()));
        return vo;
    }

    // ==================== 二、收购台账 ====================

    @Override
    public PageResult<ReportAcquisitionLedgerRespVO> getAcquisitionLedgerPage(
            @Valid ReportAcquisitionLedgerPageReqVO reqVO) {
        // 注意：LambdaQueryWrapperX 的 xxxIfPresent 只在自己的类型上；链上父类的 ne / eq 会退回 LambdaQueryWrapper，
        // 所以固定条件用单独语句追加。
        LambdaQueryWrapperX<IcbcAcquisitionDO> wrapper = new LambdaQueryWrapperX<IcbcAcquisitionDO>()
                .eqIfPresent(IcbcAcquisitionDO::getPayeeId, reqVO.getPayeeId())
                .eqIfPresent(IcbcAcquisitionDO::getStationId, reqVO.getStationId())
                .likeIfPresent(IcbcAcquisitionDO::getAcquisitionNo, reqVO.getAcquisitionNo())
                .likeIfPresent(IcbcAcquisitionDO::getSellerName, reqVO.getSellerName())
                .likeIfPresent(IcbcAcquisitionDO::getCategoryName, reqVO.getCategoryName())
                .betweenIfPresent(IcbcAcquisitionDO::getTradeTime, reqVO.getTradeTime());
        wrapper.ne(IcbcAcquisitionDO::getStatus, AcquisitionStatusEnum.CANCELLED.getStatus());
        if (Boolean.TRUE.equals(reqVO.getDirectAcquisition())) {
            wrapper.eq(IcbcAcquisitionDO::getPurchaseOrderId, 0L);
        } else if (Boolean.FALSE.equals(reqVO.getDirectAcquisition())) {
            wrapper.ne(IcbcAcquisitionDO::getPurchaseOrderId, 0L);
        }
        PageResult<IcbcAcquisitionDO> page = acquisitionMapper.selectPage(reqVO, wrapper
                .orderByDesc(IcbcAcquisitionDO::getTradeTime)
                .orderByDesc(IcbcAcquisitionDO::getId));
        if (page.getList().isEmpty()) {
            return PageResult.empty(page.getTotal());
        }
        List<ReportAcquisitionLedgerRespVO> list = toLedgerList(page.getList());
        return new PageResult<>(list, page.getTotal());
    }

    /** 批量把收购单转成台账行：一次性取场站名 / 订单号 / 结算单号，避免 N+1。 */
    private List<ReportAcquisitionLedgerRespVO> toLedgerList(List<IcbcAcquisitionDO> acquisitions) {
        Map<Long, String> stationNames = stationNames(acquisitions.stream()
                .map(IcbcAcquisitionDO::getStationId).collect(Collectors.toList()));
        Map<Long, String> orderNos = orderNos(acquisitions.stream()
                .map(IcbcAcquisitionDO::getPurchaseOrderId).collect(Collectors.toList()));
        Map<Long, String> settlementNos = settlementNos(acquisitions.stream()
                .map(IcbcAcquisitionDO::getSettlementId).collect(Collectors.toList()));
        return acquisitions.stream()
                .map(acquisition -> toLedger(acquisition, stationNames, orderNos, settlementNos))
                .collect(Collectors.toList());
    }

    private ReportAcquisitionLedgerRespVO toLedger(IcbcAcquisitionDO acquisition,
                                                   Map<Long, String> stationNames,
                                                   Map<Long, String> orderNos,
                                                   Map<Long, String> settlementNos) {
        ReportAcquisitionLedgerRespVO vo = new ReportAcquisitionLedgerRespVO();
        vo.setAcquisitionId(acquisition.getId());
        vo.setAcquisitionNo(acquisition.getAcquisitionNo());
        vo.setSellerName(acquisition.getSellerName());
        vo.setStationId(acquisition.getStationId());
        vo.setStationName(stationNames.get(acquisition.getStationId()));
        boolean direct = acquisition.getPurchaseOrderId() == null || acquisition.getPurchaseOrderId() == 0L;
        vo.setDirectAcquisition(direct);
        vo.setPurchaseOrderId(direct ? null : acquisition.getPurchaseOrderId());
        String orderNo = orderNos.get(acquisition.getPurchaseOrderId());
        vo.setPurchaseOrderNo(orderNo);
        vo.setAcquisitionModeText(direct ? "直接收购" : "采购订单 " + StrUtil.blankToDefault(orderNo, ""));
        vo.setCategoryName(acquisition.getCategoryName());
        vo.setSpecification(acquisition.getSpecification());
        vo.setUnit(acquisition.getUnit());
        vo.setGrossWeight(acquisition.getGrossWeight());
        vo.setTareWeight(acquisition.getTareWeight());
        vo.setNetWeight(acquisition.getNetWeight());
        vo.setSettlementWeight(acquisition.getSettlementWeight());
        vo.setAcceptedWeight(acquisition.getAcceptedWeight());
        vo.setRejectedWeight(acquisition.getRejectedWeight());
        vo.setResidualWeight(acquisition.getResidualWeight());
        vo.setWeightDiff(acquisition.getWeightDiff());
        vo.setQuantity(acquisition.getQuantity());
        vo.setUnitPrice(acquisition.getUnitPrice());
        vo.setAmount(acquisition.getAmount());
        vo.setSettlementId(acquisition.getSettlementId());
        vo.setSettlementNo(settlementNos.get(acquisition.getSettlementId()));
        vo.setInvoicePartnerOrderId(acquisition.getInvoicePartnerOrderId());
        vo.setTradeTime(acquisition.getTradeTime());
        vo.setStatusName(acquisitionStatusName(acquisition.getStatus()));
        return vo;
    }

    // ==================== 三、库存表 ====================

    @Override
    public PageResult<ReportStockBalanceRespVO> getStockBalancePage(@Valid ReportStockBalancePageReqVO reqVO) {
        StockReportQueryDTO query = new StockReportQueryDTO();
        query.setGoodsConfigId(reqVO.getGoodsConfigId());
        query.setWarehouseId(reqVO.getWarehouseId());
        query.setLocationId(reqVO.getLocationId());
        query.setBatchId(reqVO.getBatchId());
        query.setPageNo(reqVO.getPageNo());
        query.setPageSize(reqVO.getPageSize());

        PageResult<StockBalanceRespDTO> page = stockReportApi.getStockBalancePage(query);
        if (page.getList().isEmpty()) {
            return PageResult.empty(page.getTotal());
        }
        Map<Long, String> categoryNames = categoryNames(page.getList().stream()
                .map(StockBalanceRespDTO::getGoodsConfigId).collect(Collectors.toList()));
        List<ReportStockBalanceRespVO> list = page.getList().stream()
                .map(balance -> toStockBalance(balance, categoryNames))
                .collect(Collectors.toList());
        return new PageResult<>(list, page.getTotal());
    }

    private ReportStockBalanceRespVO toStockBalance(StockBalanceRespDTO balance, Map<Long, String> categoryNames) {
        ReportStockBalanceRespVO vo = new ReportStockBalanceRespVO();
        vo.setGoodsConfigId(balance.getGoodsConfigId());
        vo.setCategoryName(categoryNames.get(balance.getGoodsConfigId()));
        vo.setWarehouseId(balance.getWarehouseId());
        vo.setWarehouseName(balance.getWarehouseName());
        vo.setLocationId(balance.getLocationId());
        vo.setLocationName(balance.getLocationName());
        vo.setBatchId(balance.getBatchId());
        vo.setBatchNo(balance.getBatchNo());
        vo.setBatchInTime(balance.getBatchInTime());
        vo.setAgeDays(ageDays(balance.getBatchInTime()));
        vo.setCount(balance.getCount());
        return vo;
    }

    @Override
    public PageResult<ReportStockRecordRespVO> getStockRecordPage(@Valid ReportStockRecordPageReqVO reqVO) {
        StockReportQueryDTO query = new StockReportQueryDTO();
        query.setGoodsConfigId(reqVO.getGoodsConfigId());
        query.setWarehouseId(reqVO.getWarehouseId());
        query.setLocationId(reqVO.getLocationId());
        query.setBatchId(reqVO.getBatchId());
        query.setBizType(reqVO.getBizType());
        query.setBizNo(reqVO.getBizNo());
        query.setPageNo(reqVO.getPageNo());
        query.setPageSize(reqVO.getPageSize());

        PageResult<StockRecordRespDTO> page = stockReportApi.getStockRecordPage(query);
        if (page.getList().isEmpty()) {
            return PageResult.empty(page.getTotal());
        }
        Map<Long, String> categoryNames = categoryNames(page.getList().stream()
                .map(StockRecordRespDTO::getGoodsConfigId).collect(Collectors.toList()));
        List<ReportStockRecordRespVO> list = page.getList().stream()
                .map(record -> toStockRecord(record, categoryNames))
                .collect(Collectors.toList());
        return new PageResult<>(list, page.getTotal());
    }

    private ReportStockRecordRespVO toStockRecord(StockRecordRespDTO record, Map<Long, String> categoryNames) {
        ReportStockRecordRespVO vo = new ReportStockRecordRespVO();
        vo.setId(record.getId());
        vo.setGoodsConfigId(record.getGoodsConfigId());
        vo.setCategoryName(categoryNames.get(record.getGoodsConfigId()));
        vo.setWarehouseId(record.getWarehouseId());
        vo.setWarehouseName(record.getWarehouseName());
        vo.setLocationId(record.getLocationId());
        vo.setLocationName(record.getLocationName());
        vo.setBatchId(record.getBatchId());
        vo.setBatchNo(record.getBatchNo());
        vo.setCount(record.getCount());
        vo.setTotalCount(record.getTotalCount());
        vo.setBizTypeName(record.getBizTypeName());
        vo.setBizId(record.getBizId());
        vo.setBizNo(record.getBizNo());
        vo.setCreateTime(record.getCreateTime());
        return vo;
    }

    // ==================== 四、结算付款表 ====================

    @Override
    public PageResult<ReportSettlementPaymentRespVO> getSettlementPaymentPage(
            @Valid ReportSettlementPaymentPageReqVO reqVO) {
        // 付款进度 / 回单状态是派生字段（要先把结算单下的收购单与付款单聚起来才知道），
        // 所以先把命中的结算单全部算成行，再按派生字段筛选、排序、在内存里分页。
        LambdaQueryWrapperX<IcbcSettlementDO> wrapper = new LambdaQueryWrapperX<IcbcSettlementDO>()
                .eqIfPresent(IcbcSettlementDO::getSettlementNo, reqVO.getSettlementNo())
                .eqIfPresent(IcbcSettlementDO::getPayeeId, reqVO.getPayeeId())
                .eqIfPresent(IcbcSettlementDO::getConfirmStatus, reqVO.getConfirmStatus())
                .likeIfPresent(IcbcSettlementDO::getSellerName, reqVO.getSellerName())
                .betweenIfPresent(IcbcSettlementDO::getGenerateTime, reqVO.getGenerateTime());
        List<ReportSettlementPaymentRespVO> rows = settlementMapper.selectList(wrapper).stream()
                .map(this::toSettlementPayment)
                .filter(row -> reqVO.getPaymentProgress() == null
                        || reqVO.getPaymentProgress().equals(row.getPaymentProgress()))
                .filter(row -> reqVO.getReceiptStatus() == null
                        || reqVO.getReceiptStatus().equals(row.getReceiptStatus()))
                .sorted(Comparator.comparing(ReportSettlementPaymentRespVO::getGenerateTime,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
        return pageInMemory(rows, reqVO.getPageNo(), reqVO.getPageSize());
    }

    private ReportSettlementPaymentRespVO toSettlementPayment(IcbcSettlementDO settlement) {
        List<IcbcAcquisitionDO> acquisitions = acquisitionMapper.selectListBySettlementId(settlement.getId())
                .stream()
                .filter(acquisition -> !AcquisitionStatusEnum.CANCELLED.getStatus().equals(acquisition.getStatus()))
                .collect(Collectors.toList());
        BigDecimal amount = acquisitions.stream().map(IcbcAcquisitionDO::getAmount)
                .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<Long> acquisitionIds = acquisitions.stream().map(IcbcAcquisitionDO::getId)
                .collect(Collectors.toList());
        List<PaymentOrderDO> payments = acquisitionIds.isEmpty() ? Collections.emptyList()
                : paymentOrderMapper.selectListByAcquisitionIds(acquisitionIds);

        int paid = 0;
        int failed = 0;
        int receipted = 0;
        String failReason = null;
        LocalDateTime lastDoneTime = null;
        for (PaymentOrderDO payment : payments) {
            boolean success = PaymentStatusEnum.SUCCESS.getStatus().equals(payment.getPaymentStatus());
            boolean exception = PaymentStatusEnum.isException(payment.getPaymentStatus());
            if (success) {
                paid++;
            } else if (exception) {
                failed++;
                if (failReason == null) {
                    failReason = StrUtil.blankToDefault(payment.getErrorMsg(), "银行返回异常状态："
                            + PaymentStatusEnum.nameOf(payment.getPaymentStatus()));
                }
            }
            if (payment.getReceiptNo() != null) {
                receipted++;
            }
            LocalDateTime doneTime = payment.getReceiptTime() != null ? payment.getReceiptTime()
                    : payment.getPaymentTime();
            if (doneTime != null && (lastDoneTime == null || doneTime.isAfter(lastDoneTime))) {
                lastDoneTime = doneTime;
            }
        }
        int total = payments.size();
        int pending = Math.max(0, total - paid - failed);

        ReportSettlementPaymentRespVO vo = new ReportSettlementPaymentRespVO();
        vo.setSettlementId(settlement.getId());
        vo.setSettlementNo(settlement.getSettlementNo());
        vo.setSellerName(settlement.getSellerName());
        vo.setStationName(settlement.getStationName());
        vo.setSettlementAmount(amount);
        vo.setAcquisitionCount(acquisitions.size());
        vo.setConfirmStatusName(settlementStatusName(settlement.getConfirmStatus()));
        vo.setPaidCount(paid);
        vo.setPendingCount(pending);
        vo.setFailedCount(failed);
        vo.setPaymentProgress(resolvePaymentProgress(total, paid, failed));
        vo.setPaymentProgressName(paymentProgressName(vo.getPaymentProgress()));
        vo.setReceiptStatus(total > 0 && receipted >= total ? "RECEIVED" : "PENDING");
        vo.setFailReason(failReason);
        vo.setGenerateTime(settlement.getGenerateTime());
        vo.setUnhandledHours(resolveUnhandledHours(settlement.getGenerateTime(), total, paid, failed, lastDoneTime));
        return vo;
    }

    private static String resolvePaymentProgress(int total, int paid, int failed) {
        if (failed > 0) {
            return "FAILED";
        }
        if (total > 0 && paid >= total) {
            return "SUCCESS";
        }
        if (paid > 0 || total > 0) {
            return "PROCESSING";
        }
        return "UNPAID";
    }

    private static String paymentProgressName(String progress) {
        switch (progress) {
            case "SUCCESS":
                return "已支付";
            case "FAILED":
                return "异常";
            case "PROCESSING":
                return "办理中";
            default:
                return "未办理";
        }
    }

    /**
     * 未办理时长：还没办完（有未支付或异常）时算到当前，全部支付成功后算到最后一笔办结时间。
     */
    private static Long resolveUnhandledHours(LocalDateTime generateTime, int total, int paid, int failed,
                                              LocalDateTime lastDoneTime) {
        if (generateTime == null) {
            return null;
        }
        boolean fullyDone = total > 0 && paid >= total && failed == 0;
        LocalDateTime end = fullyDone && lastDoneTime != null ? lastDoneTime : LocalDateTime.now();
        return Math.max(0, Duration.between(generateTime, end).toHours());
    }

    // ==================== 五、异常表（派生清单） ====================

    @Override
    public PageResult<ReportAnomalyRespVO> getAnomalyPage(@Valid ReportAnomalyPageReqVO reqVO) {
        List<ReportAnomalyTypeEnum> types = resolveAnomalyTypes(reqVO.getType());
        List<ReportAnomalyRespVO> rows = new ArrayList<>();
        for (ReportAnomalyTypeEnum type : types) {
            rows.addAll(buildAnomalies(type, reqVO));
        }
        // 通用筛选：单号按包含，时间按区间
        List<ReportAnomalyRespVO> filtered = rows.stream()
                .filter(row -> StrUtil.isBlank(reqVO.getBizNo())
                        || (row.getBizNo() != null && row.getBizNo().contains(reqVO.getBizNo())))
                .filter(row -> inTimeRange(row.getTime(), reqVO.getTime()))
                .sorted(Comparator.comparing(ReportAnomalyRespVO::getTime,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
        return pageInMemory(filtered, reqVO.getPageNo(), reqVO.getPageSize());
    }

    private static List<ReportAnomalyTypeEnum> resolveAnomalyTypes(String type) {
        if (StrUtil.isBlank(type)) {
            return java.util.Arrays.asList(ReportAnomalyTypeEnum.values());
        }
        ReportAnomalyTypeEnum match = ReportAnomalyTypeEnum.ofCode(type)
                .orElseThrow(() -> exception(REPORT_ANOMALY_TYPE_UNKNOWN, type));
        return Collections.singletonList(match);
    }

    private List<ReportAnomalyRespVO> buildAnomalies(ReportAnomalyTypeEnum type, ReportAnomalyPageReqVO reqVO) {
        switch (type) {
            case WEIGHT_DIFF:
                return weightDiffAnomalies(reqVO);
            case OVER_PURCHASE_QUANTITY:
                return overPurchaseQuantityAnomalies(reqVO);
            case OVER_STOCK_IN:
                return overStockInAnomalies(reqVO);
            case DUPLICATE_LINK:
                return duplicateLinkAnomalies(reqVO);
            case LONG_UNCONFIRMED:
                return longUnconfirmedAnomalies(reqVO);
            case MISSING_EVIDENCE:
                return missingEvidenceAnomalies(reqVO);
            default:
                return Collections.emptyList();
        }
    }

    /** 磅差：直接读 #53 已落库的 {@code weight_diff}，不重算。 */
    private List<ReportAnomalyRespVO> weightDiffAnomalies(ReportAnomalyPageReqVO reqVO) {
        LambdaQueryWrapper<IcbcAcquisitionDO> wrapper = new LambdaQueryWrapper<IcbcAcquisitionDO>()
                .ne(IcbcAcquisitionDO::getStatus, AcquisitionStatusEnum.CANCELLED.getStatus())
                .isNotNull(IcbcAcquisitionDO::getWeightDiff)
                .ne(IcbcAcquisitionDO::getWeightDiff, BigDecimal.ZERO)
                .orderByDesc(IcbcAcquisitionDO::getTradeTime)
                .orderByDesc(IcbcAcquisitionDO::getId);
        applyAcquisitionFilter(wrapper, reqVO);
        return acquisitionMapper.selectList(wrapper).stream()
                .limit(ANOMALY_SCAN_LIMIT)
                .map(acquisition -> {
                    ReportAnomalyRespVO vo = baseAnomaly(ReportAnomalyTypeEnum.WEIGHT_DIFF);
                    vo.setBizType("ACQUISITION");
                    vo.setBizId(acquisition.getId());
                    vo.setBizNo(acquisition.getAcquisitionNo());
                    vo.setSubject(acquisition.getSellerName());
                    vo.setCategoryName(acquisition.getCategoryName());
                    vo.setQuantity(acquisition.getWeightDiff());
                    vo.setAmount(acquisition.getAmount());
                    vo.setTime(acquisition.getTradeTime());
                    vo.setDetail("实物量 " + plain(acquisition.resolvePhysicalWeight())
                            + " 与结算重量 " + plain(acquisition.getSettlementWeight())
                            + " 相差 " + plain(acquisition.getWeightDiff())
                            + "（正为实物多、负为计价多，ADR 0028 不抹平）");
                    return vo;
                }).collect(Collectors.toList());
    }

    /** 超采购量：消费 #47 的履约口径（getProgress 的明细 overQuantity），不重算。 */
    private List<ReportAnomalyRespVO> overPurchaseQuantityAnomalies(ReportAnomalyPageReqVO reqVO) {
        LambdaQueryWrapperX<IcbcPurchaseOrderDO> wrapper = new LambdaQueryWrapperX<IcbcPurchaseOrderDO>()
                .likeIfPresent(IcbcPurchaseOrderDO::getCounterpartyName, reqVO.getSellerName());
        wrapper.notIn(IcbcPurchaseOrderDO::getStatus,
                PurchaseOrderStatusEnum.DRAFT.getStatus(),
                PurchaseOrderStatusEnum.CLOSED.getStatus());
        wrapper.orderByDesc(IcbcPurchaseOrderDO::getId);
        List<ReportAnomalyRespVO> rows = new ArrayList<>();
        int scannedOrders = 0;
        for (IcbcPurchaseOrderDO order : purchaseOrderMapper.selectList(wrapper)) {
            if (rows.size() >= ANOMALY_SCAN_LIMIT || scannedOrders++ >= MAX_ORDER_SCAN) {
                break;
            }
            PurchaseOrderProgressRespVO progress = purchaseOrderService.getProgress(order.getId());
            if (progress == null || progress.getItems() == null) {
                continue;
            }
            for (PurchaseOrderProgressRespVO.ItemProgress item : progress.getItems()) {
                if (!Boolean.TRUE.equals(item.getOverQuantity())) {
                    continue;
                }
                BigDecimal overage = safe(item.getAcceptedQuantity()).subtract(safe(item.getQuantity()));
                ReportAnomalyRespVO vo = baseAnomaly(ReportAnomalyTypeEnum.OVER_PURCHASE_QUANTITY);
                vo.setBizType("PURCHASE_ORDER");
                vo.setBizId(order.getId());
                vo.setBizNo(order.getOrderNo());
                vo.setSubject(order.getCounterpartyName());
                vo.setCategoryName(item.getCategoryName());
                vo.setQuantity(overage);
                vo.setTime(order.getUpdateTime());
                vo.setDetail("验收量 " + plain(item.getAcceptedQuantity()) + " 超过计划量 "
                        + plain(item.getQuantity()) + "，超出 " + plain(overage));
                rows.add(vo);
            }
        }
        return rows;
    }

    /** 超入库量：累计已过账入库 > 可入库实物量（#52 应在过账时拦住，这里只读复核）。 */
    private List<ReportAnomalyRespVO> overStockInAnomalies(ReportAnomalyPageReqVO reqVO) {
        LambdaQueryWrapper<IcbcStockInDO> wrapper = new LambdaQueryWrapper<IcbcStockInDO>()
                .eq(IcbcStockInDO::getStatus, POSTED_STOCK_IN)
                .orderByDesc(IcbcStockInDO::getId);
        Map<Long, BigDecimal> stockedByAcquisition = new LinkedHashMap<>();
        for (IcbcStockInDO stockIn : stockInMapper.selectList(wrapper)) {
            stockedByAcquisition.merge(stockIn.getAcquisitionId(), safe(stockIn.getTotalQuantity()),
                    BigDecimal::add);
        }
        List<ReportAnomalyRespVO> rows = new ArrayList<>();
        for (Map.Entry<Long, BigDecimal> entry : stockedByAcquisition.entrySet()) {
            if (rows.size() >= ANOMALY_SCAN_LIMIT) {
                break;
            }
            IcbcAcquisitionDO acquisition = acquisitionMapper.selectById(entry.getKey());
            if (acquisition == null || AcquisitionStatusEnum.CANCELLED.getStatus().equals(acquisition.getStatus())
                    || !matchesSeller(acquisition, reqVO)) {
                continue;
            }
            BigDecimal available = acquisition.resolvePhysicalWeight();
            if (available == null || entry.getValue().compareTo(available) <= 0) {
                continue;
            }
            ReportAnomalyRespVO vo = baseAnomaly(ReportAnomalyTypeEnum.OVER_STOCK_IN);
            vo.setBizType("ACQUISITION");
            vo.setBizId(acquisition.getId());
            vo.setBizNo(acquisition.getAcquisitionNo());
            vo.setSubject(acquisition.getSellerName());
            vo.setCategoryName(acquisition.getCategoryName());
            vo.setQuantity(entry.getValue().subtract(available));
            vo.setAmount(acquisition.getAmount());
            vo.setTime(acquisition.getTradeTime());
            vo.setDetail("累计已过账入库 " + plain(entry.getValue()) + " 超过可入库实物量 "
                    + plain(available) + "，超出 " + plain(entry.getValue().subtract(available)));
            rows.add(vo);
        }
        return rows;
    }

    /** 重复关联：同一收购单关联了多张未作废付款单（重复付款风险）。 */
    private List<ReportAnomalyRespVO> duplicateLinkAnomalies(ReportAnomalyPageReqVO reqVO) {
        List<PaymentOrderDO> payments = paymentOrderMapper.selectList(new LambdaQueryWrapper<PaymentOrderDO>()
                .isNotNull(PaymentOrderDO::getAcquisitionId)
                .notIn(PaymentOrderDO::getPaymentStatus, INACTIVE_PAYMENT_STATUSES));
        Map<Long, List<PaymentOrderDO>> byAcquisition = payments.stream()
                .collect(Collectors.groupingBy(PaymentOrderDO::getAcquisitionId));
        List<ReportAnomalyRespVO> rows = new ArrayList<>();
        for (Map.Entry<Long, List<PaymentOrderDO>> entry : byAcquisition.entrySet()) {
            if (entry.getValue().size() < 2 || rows.size() >= ANOMALY_SCAN_LIMIT) {
                continue;
            }
            IcbcAcquisitionDO acquisition = acquisitionMapper.selectById(entry.getKey());
            if (acquisition == null || !matchesSeller(acquisition, reqVO)) {
                continue;
            }
            ReportAnomalyRespVO vo = baseAnomaly(ReportAnomalyTypeEnum.DUPLICATE_LINK);
            vo.setBizType("ACQUISITION");
            vo.setBizId(acquisition.getId());
            vo.setBizNo(acquisition.getAcquisitionNo());
            vo.setSubject(acquisition.getSellerName());
            vo.setCategoryName(acquisition.getCategoryName());
            vo.setAmount(acquisition.getAmount());
            vo.setTime(acquisition.getTradeTime());
            vo.setDetail("该收购单关联了 " + entry.getValue().size() + " 张未作废付款单（"
                    + entry.getValue().stream().map(PaymentOrderDO::getOrderNo).collect(Collectors.joining("、"))
                    + "），存在重复付款风险");
            rows.add(vo);
        }
        return rows;
    }

    /** 长期未确认：结算单过了确认截止时间仍未确认。 */
    private List<ReportAnomalyRespVO> longUnconfirmedAnomalies(ReportAnomalyPageReqVO reqVO) {
        LambdaQueryWrapperX<IcbcSettlementDO> wrapper = new LambdaQueryWrapperX<IcbcSettlementDO>()
                .likeIfPresent(IcbcSettlementDO::getSellerName, reqVO.getSellerName());
        wrapper.in(IcbcSettlementDO::getConfirmStatus, UNCONFIRMED_SETTLEMENT_STATUSES);
        wrapper.orderByAsc(IcbcSettlementDO::getGenerateTime);
        List<ReportAnomalyRespVO> rows = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (IcbcSettlementDO settlement : settlementMapper.selectList(wrapper)) {
            if (rows.size() >= ANOMALY_SCAN_LIMIT) {
                break;
            }
            boolean overdue = settlement.getDeadlineTime() != null
                    ? now.isAfter(settlement.getDeadlineTime())
                    : settlement.getGenerateTime() != null
                    && settlement.getGenerateTime().isBefore(now.minusHours(DEFAULT_UNCONFIRMED_HOURS));
            if (!overdue) {
                continue;
            }
            ReportAnomalyRespVO vo = baseAnomaly(ReportAnomalyTypeEnum.LONG_UNCONFIRMED);
            vo.setBizType("SETTLEMENT");
            vo.setBizId(settlement.getId());
            vo.setBizNo(settlement.getSettlementNo());
            vo.setSubject(settlement.getSellerName());
            vo.setTime(settlement.getGenerateTime());
            vo.setDetail("结算单处于「" + settlementStatusName(settlement.getConfirmStatus())
                    + "」，已过确认截止时间仍未确认"
                    + (settlement.getDeadlineTime() != null ? "（截止 " + settlement.getDeadlineTime() + "）" : "")
                    + "；结算确认是开票申请的硬前置");
            rows.add(vo);
        }
        return rows;
    }

    /** 资料缺失：未作废收购单缺失五流骨架要件。 */
    private List<ReportAnomalyRespVO> missingEvidenceAnomalies(ReportAnomalyPageReqVO reqVO) {
        LambdaQueryWrapper<IcbcAcquisitionDO> wrapper = new LambdaQueryWrapper<IcbcAcquisitionDO>()
                .ne(IcbcAcquisitionDO::getStatus, AcquisitionStatusEnum.CANCELLED.getStatus())
                .orderByDesc(IcbcAcquisitionDO::getTradeTime)
                .orderByDesc(IcbcAcquisitionDO::getId);
        applyAcquisitionFilter(wrapper, reqVO);
        List<ReportAnomalyRespVO> rows = new ArrayList<>();
        for (IcbcAcquisitionDO acquisition : acquisitionMapper.selectList(wrapper)) {
            if (rows.size() >= ANOMALY_SCAN_LIMIT) {
                break;
            }
            List<String> missing = missingFields(acquisition);
            if (missing.isEmpty()) {
                continue;
            }
            ReportAnomalyRespVO vo = baseAnomaly(ReportAnomalyTypeEnum.MISSING_EVIDENCE);
            vo.setBizType("ACQUISITION");
            vo.setBizId(acquisition.getId());
            vo.setBizNo(acquisition.getAcquisitionNo());
            vo.setSubject(acquisition.getSellerName());
            vo.setCategoryName(acquisition.getCategoryName());
            vo.setAmount(acquisition.getAmount());
            vo.setTime(acquisition.getTradeTime());
            vo.setDetail("缺少：" + String.join("、", missing));
            rows.add(vo);
        }
        return rows;
    }

    private static List<String> missingFields(IcbcAcquisitionDO acquisition) {
        List<String> missing = new ArrayList<>();
        if (StrUtil.isBlank(acquisition.getCategoryName())) {
            missing.add("品类");
        }
        if (acquisition.getTradeTime() == null) {
            missing.add("交易时间");
        }
        if (StrUtil.isBlank(acquisition.getTradeAddress())) {
            missing.add("交易地点");
        }
        if (StrUtil.isBlank(acquisition.getSellerName())) {
            missing.add("出售者");
        }
        if (acquisition.getNetWeight() == null) {
            missing.add("净重");
        }
        if (acquisition.getUnitPrice() == null) {
            missing.add("单价");
        }
        if (StrUtil.isBlank(acquisition.getWeightTicketNo())) {
            missing.add("磅单号");
        }
        return missing;
    }

    // ==================== 异常表公共小工具 ====================

    private void applyAcquisitionFilter(LambdaQueryWrapper<IcbcAcquisitionDO> wrapper,
                                        ReportAnomalyPageReqVO reqVO) {
        if (reqVO.getPayeeId() != null) {
            wrapper.eq(IcbcAcquisitionDO::getPayeeId, reqVO.getPayeeId());
        }
        if (StrUtil.isNotBlank(reqVO.getSellerName())) {
            wrapper.like(IcbcAcquisitionDO::getSellerName, reqVO.getSellerName());
        }
    }

    private static boolean matchesSeller(IcbcAcquisitionDO acquisition, ReportAnomalyPageReqVO reqVO) {
        if (reqVO.getPayeeId() != null && !reqVO.getPayeeId().equals(acquisition.getPayeeId())) {
            return false;
        }
        return StrUtil.isBlank(reqVO.getSellerName())
                || (acquisition.getSellerName() != null
                && acquisition.getSellerName().contains(reqVO.getSellerName()));
    }

    private static ReportAnomalyRespVO baseAnomaly(ReportAnomalyTypeEnum type) {
        ReportAnomalyRespVO vo = new ReportAnomalyRespVO();
        vo.setType(type.getCode());
        vo.setTypeName(type.getName());
        vo.setDefinition(type.getDefinition());
        vo.setSeverity(type.getSeverity());
        vo.setDrillDown(type.getDrillDown());
        return vo;
    }

    private static boolean inTimeRange(LocalDateTime time, LocalDateTime[] range) {
        if (range == null || range.length < 2 || range[0] == null || range[1] == null) {
            return true;
        }
        if (time == null) {
            return false;
        }
        return !time.isBefore(range[0]) && !time.isAfter(range[1]);
    }

    private static <T> PageResult<T> pageInMemory(List<T> rows, Integer pageNo, Integer pageSize) {
        long total = rows.size();
        if (pageSize == null || pageSize <= 0 || pageSize >= total) {
            return new PageResult<>(rows, total);
        }
        int from = Math.max(0, ((pageNo == null ? 1 : pageNo) - 1) * pageSize);
        if (from >= rows.size()) {
            return new PageResult<>(Collections.emptyList(), total);
        }
        int to = Math.min(rows.size(), from + pageSize);
        return new PageResult<>(new ArrayList<>(rows.subList(from, to)), total);
    }

    // ==================== 名称 / 编号批量加载 ====================

    private Map<Long, String> stationNames(Collection<Long> ids) {
        List<Long> valid = distinctPositive(ids);
        if (valid.isEmpty()) {
            return Collections.emptyMap();
        }
        return stationMapper.selectBatchIds(valid).stream().collect(Collectors.toMap(
                IcbcStationDO::getId, IcbcStationDO::getName, (a, b) -> a));
    }

    private Map<Long, String> orderNos(Collection<Long> ids) {
        List<Long> valid = distinctPositive(ids);
        if (valid.isEmpty()) {
            return Collections.emptyMap();
        }
        return purchaseOrderMapper.selectBatchIds(valid).stream().collect(Collectors.toMap(
                IcbcPurchaseOrderDO::getId, IcbcPurchaseOrderDO::getOrderNo, (a, b) -> a));
    }

    private Map<Long, String> settlementNos(Collection<Long> ids) {
        List<Long> valid = distinctPositive(ids);
        if (valid.isEmpty()) {
            return Collections.emptyMap();
        }
        return settlementMapper.selectBatchIds(valid).stream().collect(Collectors.toMap(
                IcbcSettlementDO::getId, IcbcSettlementDO::getSettlementNo, (a, b) -> a));
    }

    private Map<Long, String> categoryNames(Collection<Long> ids) {
        List<Long> valid = distinctPositive(ids);
        if (valid.isEmpty()) {
            return Collections.emptyMap();
        }
        return goodsConfigMapper.selectBatchIds(valid).stream().collect(Collectors.toMap(
                IcbcGoodsConfigDO::getId, IcbcGoodsConfigDO::getName, (a, b) -> a));
    }

    private static List<Long> distinctPositive(Collection<Long> ids) {
        if (ids == null) {
            return Collections.emptyList();
        }
        return ids.stream().filter(id -> id != null && id > 0).distinct().collect(Collectors.toList());
    }

    private static Long ageDays(LocalDateTime inTime) {
        if (inTime == null) {
            return null;
        }
        return ChronoUnit.DAYS.between(inTime.toLocalDate(), LocalDate.now());
    }

    private static String acquisitionStatusName(Integer status) {
        return status == null ? null
                : AcquisitionStatusEnum.ofStatus(status).map(AcquisitionStatusEnum::getName).orElse(null);
    }

    private static String settlementStatusName(Integer status) {
        return status == null ? null
                : SettlementConfirmStatusEnum.ofStatus(status).map(SettlementConfirmStatusEnum::getName).orElse(null);
    }

    private static BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static String plain(BigDecimal value) {
        return value == null ? "—" : value.stripTrailingZeros().toPlainString();
    }

}
