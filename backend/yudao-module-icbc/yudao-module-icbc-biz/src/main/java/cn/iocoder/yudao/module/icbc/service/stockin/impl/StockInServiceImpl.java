package cn.iocoder.yudao.module.icbc.service.stockin.impl;

import cn.hutool.core.util.RandomUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.api.stock.StockApi;
import cn.iocoder.yudao.module.erp.api.stock.dto.StockChangeReqDTO;
import cn.iocoder.yudao.module.erp.enums.stock.ErpStockRecordBizTypeEnum;
import cn.iocoder.yudao.module.icbc.controller.admin.stockin.vo.StockInCancelReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.stockin.vo.StockInItemReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.stockin.vo.StockInItemRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.stockin.vo.StockInPageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.stockin.vo.StockInPendingPageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.stockin.vo.StockInPendingRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.stockin.vo.StockInRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.stockin.vo.StockInSaveReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.acquisition.IcbcAcquisitionDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.stockin.IcbcStockInDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.stockin.IcbcStockInItemDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.acquisition.IcbcAcquisitionMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.stockin.IcbcStockInItemMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.stockin.IcbcStockInMapper;
import cn.iocoder.yudao.module.icbc.enums.AcquisitionStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.StockInStatusEnum;
import cn.iocoder.yudao.module.icbc.service.stockin.StockInService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.*;

/**
 * 待入库 → 入库单 → 库存流水 Service 实现（#52 T14）。
 *
 * <p>写入路径只有两条，且都经 {@code StockApi}：过账走 {@code RECEIPT_IN(90)}，
 * 作废已过账的单走 {@code RECEIPT_IN_CANCEL(91)}。icbc 不直接碰 {@code erp_stock*}。
 *
 * <p>三个关键不变量：
 * <ul>
 *     <li>**只有过账才加库存**：建单是待过账，过账才写流水（ADR 0027 / 规格 #38）；</li>
 *     <li>**累计入库不超可入库实物量**：上限由 {@code StockApi} 的 {@code maxCount} 跨入库单兜底，
 *         过账前锁住收购单行把同一收购单的并发入库串行化；</li>
 *     <li>**重复确认不重复加库存**：业务项编号 = 入库明细编号，{@code StockApi} 按业务项幂等。</li>
 * </ul>
 */
@Slf4j
@Service
@Validated
public class StockInServiceImpl implements StockInService {

    private static final DateTimeFormatter NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Resource
    private IcbcAcquisitionMapper icbcAcquisitionMapper;
    @Resource
    private IcbcStockInMapper icbcStockInMapper;
    @Resource
    private IcbcStockInItemMapper icbcStockInItemMapper;
    @Resource
    private StockApi stockApi;

    // ==================== 取数：可入库实物量与累计入库 ====================

    /**
     * 可入库实物量（**唯一取数点**）：接收量优先，无接收结论时退回净重（实物口径，毛重 − 皮重）。
     *
     * <p>接收量来自 T15（#53）的验收结论：拒收 / 退回 / 余货出场的部分不形成可入库实物量，
     * 所以这里只能取 {@code resolvePhysicalWeight()}，不能取结算重量（ADR 0028）。
     */
    @Override
    public BigDecimal resolveAvailableQuantity(IcbcAcquisitionDO acquisition) {
        if (acquisition == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal physical = acquisition.resolvePhysicalWeight();
        return physical == null ? BigDecimal.ZERO : physical;
    }

    @Override
    public BigDecimal getStockedQuantity(Long acquisitionId) {
        // 已过账合计；作废会把状态改成 CANCELLED，自然不再计入（不需要再减一遍）
        return sumByStatus(icbcStockInMapper.selectListByAcquisitionId(acquisitionId),
                StockInStatusEnum.POSTED);
    }

    @Override
    public Map<Long, BigDecimal> getStockedQuantityByOrderItems(Long purchaseOrderId) {
        if (purchaseOrderId == null) {
            return Map.of();
        }
        // 1. 订单下的收购单（挂在订单明细上）：入库单不直接挂采购订单，中间隔了一层收购单
        Map<Long, Long> itemIdByAcquisitionId = icbcAcquisitionMapper.selectListByPurchaseOrderId(purchaseOrderId)
                .stream()
                .filter(acquisition -> acquisition.getPurchaseOrderItemId() != null
                        && acquisition.getPurchaseOrderItemId() != 0L)
                .collect(Collectors.toMap(IcbcAcquisitionDO::getId, IcbcAcquisitionDO::getPurchaseOrderItemId));
        if (itemIdByAcquisitionId.isEmpty()) {
            return Map.of();
        }
        // 2. 只算已过账的入库单；待过账与已作废都不算（后者作废时已冲销流水）
        Map<Long, Long> acquisitionIdByStockInId = icbcStockInMapper
                .selectListByAcquisitionIds(itemIdByAcquisitionId.keySet())
                .stream()
                .filter(stockIn -> Objects.equals(stockIn.getStatus(), StockInStatusEnum.POSTED.getStatus()))
                .collect(Collectors.toMap(IcbcStockInDO::getId, IcbcStockInDO::getAcquisitionId));
        if (acquisitionIdByStockInId.isEmpty()) {
            return Map.of();
        }
        // 3. 按采购订单明细汇总入库明细的数量
        Map<Long, BigDecimal> stocked = new HashMap<>();
        for (IcbcStockInItemDO item : icbcStockInItemMapper
                .selectListByStockInIds(acquisitionIdByStockInId.keySet())) {
            Long acquisitionId = acquisitionIdByStockInId.get(item.getStockInId());
            Long orderItemId = acquisitionId == null ? null : itemIdByAcquisitionId.get(acquisitionId);
            if (orderItemId == null || item.getQuantity() == null) {
                continue;
            }
            stocked.merge(orderItemId, item.getQuantity(), BigDecimal::add);
        }
        return stocked;
    }

    // ==================== 待入库 ====================

    @Override
    public PageResult<StockInPendingRespVO> getPendingPage(@Valid StockInPendingPageReqVO reqVO) {
        List<IcbcAcquisitionDO> candidates = icbcAcquisitionMapper.selectListPendingStockIn(reqVO);
        // 累计入库一次性批量取，避免逐行查询
        Map<Long, List<IcbcStockInDO>> stockInMap = candidates.isEmpty() ? Map.of()
                : icbcStockInMapper.selectListByAcquisitionIds(
                        candidates.stream().map(IcbcAcquisitionDO::getId).collect(Collectors.toList()))
                .stream().collect(Collectors.groupingBy(IcbcStockInDO::getAcquisitionId));

        List<StockInPendingRespVO> pending = new ArrayList<>();
        for (IcbcAcquisitionDO acquisition : candidates) {
            BigDecimal available = resolveAvailableQuantity(acquisition);
            if (available.signum() <= 0) {
                continue;
            }
            List<IcbcStockInDO> stockIns = stockInMap.getOrDefault(acquisition.getId(), List.of());
            BigDecimal stocked = sumByStatus(stockIns, StockInStatusEnum.POSTED);
            BigDecimal remaining = available.subtract(stocked);
            if (remaining.signum() <= 0) {
                continue; // 已全部入库：不再是待办
            }
            pending.add(toPendingResp(acquisition, available, stocked, remaining));
        }
        return paginate(pending, reqVO.getPageNo(), reqVO.getPageSize());
    }

    // ==================== 建单 / 过账 / 作废 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createStockIn(@Valid StockInSaveReqVO reqVO) {
        return doCreateStockIn(reqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long confirmStockIn(@Valid StockInSaveReqVO reqVO) {
        // 仓管一次成型：建单（待过账）→ 过账
        return doPostStockIn(doCreateStockIn(reqVO));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void postStockIn(Long id) {
        doPostStockIn(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelStockIn(@Valid StockInCancelReqVO reqVO) {
        // 加锁读作为事务第一条语句：让后续的库存校验看到已提交的最新数据
        IcbcStockInDO stockIn = getStockInDOForUpdate(reqVO.getId());
        if (Objects.equals(stockIn.getStatus(), StockInStatusEnum.CANCELLED.getStatus())) {
            throw exception(STOCK_IN_STATUS_NOT_ALLOW, StockInStatusEnum.CANCELLED.getName());
        }
        IcbcAcquisitionDO acquisition = getAcquisitionForUpdate(stockIn.getAcquisitionId());
        if (Objects.equals(stockIn.getStatus(), StockInStatusEnum.POSTED.getStatus())) {
            // 已过账：按相反方向冲销，业务类型用 RECEIPT_IN_CANCEL(91)
            for (IcbcStockInItemDO item : icbcStockInItemMapper.selectListByStockInId(stockIn.getId())) {
                stockApi.out(buildChangeReq(stockIn, acquisition, item,
                        ErpStockRecordBizTypeEnum.RECEIPT_IN_CANCEL.getType()));
            }
        }
        IcbcStockInDO update = new IcbcStockInDO();
        update.setId(stockIn.getId());
        update.setStatus(StockInStatusEnum.CANCELLED.getStatus());
        update.setCancelReason(reqVO.getReason());
        update.setCancelledTime(LocalDateTime.now());
        icbcStockInMapper.updateById(update);
        log.info("入库单已作废 - stockInNo: {}, 原状态: {}, 原因: {}",
                stockIn.getStockInNo(), stockIn.getStatus(), reqVO.getReason());
    }

    /**
     * 建入库单（待过账，不动库存）。校验：收购单存在且未作废、已验收、可入库实物量大于 0、
     * 明细齐备、总量不超过「可入库实物量 − 已累计入库」。
     */
    private Long doCreateStockIn(StockInSaveReqVO reqVO) {
        // 加锁读作为第一条语句：同一收购单的并发建单 / 过账在这里串行化，
        // 后续的累计入库校验才能在可重复读下看到最新已提交数据
        IcbcAcquisitionDO acquisition = getAcquisitionForUpdate(reqVO.getAcquisitionId());
        assertNotCancelled(acquisition);
        assertAccepted(acquisition);
        BigDecimal available = resolveAvailableQuantity(acquisition);
        if (available.signum() <= 0) {
            throw exception(STOCK_IN_AVAILABLE_QUANTITY_EMPTY, label(acquisition));
        }
        BigDecimal total = BigDecimal.ZERO;
        for (StockInItemReqVO item : reqVO.getItems()) {
            if (item.getQuantity() == null || item.getQuantity().signum() <= 0) {
                throw exception(STOCK_IN_ITEM_INVALID, "入库数量必须大于 0");
            }
            if (item.getWarehouseId() == null) {
                throw exception(STOCK_IN_ITEM_INVALID, "每条明细都要选择仓库");
            }
            total = total.add(item.getQuantity());
        }
        BigDecimal remaining = available.subtract(getStockedQuantity(acquisition.getId()));
        if (total.compareTo(remaining) > 0) {
            throw exception(STOCK_IN_EXCEED_AVAILABLE, remaining, available, total);
        }
        IcbcStockInDO stockIn = IcbcStockInDO.builder()
                .stockInNo(generateStockInNo())
                .acquisitionId(acquisition.getId())
                .acquisitionNo(acquisition.getAcquisitionNo())
                .payeeId(acquisition.getPayeeId())
                .sellerName(acquisition.getSellerName())
                .goodsConfigId(acquisition.getGoodsConfigId())
                .categoryName(acquisition.getCategoryName())
                .unit(acquisition.getUnit())
                .availableQuantity(available)
                .totalQuantity(total)
                .status(StockInStatusEnum.PENDING.getStatus())
                .remark(reqVO.getRemark())
                .build();
        icbcStockInMapper.insert(stockIn);
        for (StockInItemReqVO item : reqVO.getItems()) {
            icbcStockInItemMapper.insert(IcbcStockInItemDO.builder()
                    .stockInId(stockIn.getId())
                    .warehouseId(item.getWarehouseId())
                    .locationId(normalizeZero(item.getLocationId()))
                    .batchId(normalizeZero(item.getBatchId()))
                    .quantity(item.getQuantity())
                    .remark(item.getRemark())
                    .build());
        }
        return stockIn.getId();
    }

    /**
     * 过账：写库存流水并增量余额。已过账时幂等返回（重复确认不加库存）。
     */
    private Long doPostStockIn(Long id) {
        // 加锁读作为第一条语句（原因见 doCreateStockIn）
        IcbcStockInDO stockIn = getStockInDOForUpdate(id);
        if (Objects.equals(stockIn.getStatus(), StockInStatusEnum.POSTED.getStatus())) {
            return stockIn.getId(); // 幂等：重复确认不再加库存
        }
        if (Objects.equals(stockIn.getStatus(), StockInStatusEnum.CANCELLED.getStatus())) {
            throw exception(STOCK_IN_STATUS_NOT_ALLOW, StockInStatusEnum.CANCELLED.getName());
        }
        IcbcAcquisitionDO acquisition = getAcquisitionForUpdate(stockIn.getAcquisitionId());
        assertNotCancelled(acquisition);
        BigDecimal available = resolveAvailableQuantity(acquisition);
        // maxCount 是「已过账流水之和」的绝对上限：把已冲销的量加回去，
        // 使上限判定的净效果是「累计入库（已过账 − 已冲销）不超过可入库实物量」。
        BigDecimal reversed = sumReversed(icbcStockInMapper.selectListByAcquisitionId(acquisition.getId()));
        BigDecimal maxCount = available.add(reversed);
        for (IcbcStockInItemDO item : icbcStockInItemMapper.selectListByStockInId(stockIn.getId())) {
            StockChangeReqDTO reqDTO = buildChangeReq(stockIn, acquisition, item,
                    ErpStockRecordBizTypeEnum.RECEIPT_IN.getType());
            reqDTO.setMaxCount(maxCount);
            stockApi.in(reqDTO);
        }
        IcbcStockInDO update = new IcbcStockInDO();
        update.setId(stockIn.getId());
        update.setStatus(StockInStatusEnum.POSTED.getStatus());
        update.setPostedTime(LocalDateTime.now());
        icbcStockInMapper.updateById(update);
        log.info("入库单已过账 - stockInNo: {}, acquisitionNo: {}, 数量: {}",
                stockIn.getStockInNo(), stockIn.getAcquisitionNo(), stockIn.getTotalQuantity());
        return stockIn.getId();
    }

    // ==================== 查询 ====================

    @Override
    public StockInRespVO getStockIn(Long id) {
        IcbcStockInDO stockIn = getStockInDO(id);
        StockInRespVO resp = toResp(stockIn);
        resp.setItems(icbcStockInItemMapper.selectListByStockInId(id).stream()
                .map(item -> BeanUtils.toBean(item, StockInItemRespVO.class))
                .collect(Collectors.toList()));
        return resp;
    }

    @Override
    public PageResult<StockInRespVO> getStockInPage(@Valid StockInPageReqVO reqVO) {
        PageResult<IcbcStockInDO> page = icbcStockInMapper.selectPage(reqVO);
        return new PageResult<>(page.getList().stream().map(this::toResp).collect(Collectors.toList()),
                page.getTotal());
    }

    // ==================== 内部方法 ====================

    private IcbcAcquisitionDO getAcquisitionForUpdate(Long id) {
        IcbcAcquisitionDO acquisition = id == null ? null : icbcAcquisitionMapper.selectByIdForUpdate(id);
        if (acquisition == null) {
            throw exception(ACQUISITION_NOT_EXISTS);
        }
        return acquisition;
    }

    /** 加锁读入库单行：作废 / 过账的并发从第一条语句起串行化。 */
    private IcbcStockInDO getStockInDOForUpdate(Long id) {
        IcbcStockInDO stockIn = id == null ? null : icbcStockInMapper.selectByIdForUpdate(id);
        if (stockIn == null) {
            throw exception(STOCK_IN_NOT_EXISTS);
        }
        return stockIn;
    }

    private IcbcStockInDO getStockInDO(Long id) {
        IcbcStockInDO stockIn = id == null ? null : icbcStockInMapper.selectById(id);
        if (stockIn == null) {
            throw exception(STOCK_IN_NOT_EXISTS);
        }
        return stockIn;
    }

    private void assertNotCancelled(IcbcAcquisitionDO acquisition) {
        if (Objects.equals(acquisition.getStatus(), AcquisitionStatusEnum.CANCELLED.getStatus())) {
            throw exception(STOCK_IN_ACQUISITION_CANCELLED);
        }
    }

    /** 入库是「验收后」的动作：现场还没「结束本次收货」的收购单不进待入库，也不能入库。 */
    private void assertAccepted(IcbcAcquisitionDO acquisition) {
        if (acquisition.getSettlementId() == null) {
            throw exception(STOCK_IN_ACQUISITION_NOT_ACCEPTED);
        }
    }

    private StockChangeReqDTO buildChangeReq(IcbcStockInDO stockIn, IcbcAcquisitionDO acquisition,
                                             IcbcStockInItemDO item, Integer bizType) {
        StockChangeReqDTO reqDTO = new StockChangeReqDTO();
        reqDTO.setGoodsConfigId(acquisition.getGoodsConfigId());
        reqDTO.setWarehouseId(item.getWarehouseId());
        reqDTO.setLocationId(normalizeZero(item.getLocationId()));
        reqDTO.setBatchId(normalizeZero(item.getBatchId()));
        reqDTO.setCount(item.getQuantity());
        reqDTO.setBizType(bizType);
        // 业务编号用收购单：同一收购单分多次入库时，累计上限跨入库单生效
        reqDTO.setBizId(acquisition.getId());
        // 业务项编号用入库明细：同一明细只写一次流水（重复确认不重复加库存）
        reqDTO.setBizItemId(item.getId());
        reqDTO.setBizNo(stockIn.getStockInNo());
        return reqDTO;
    }

    private BigDecimal sumByStatus(List<IcbcStockInDO> stockIns, StockInStatusEnum status) {
        return stockIns.stream()
                .filter(item -> Objects.equals(item.getStatus(), status.getStatus()))
                .map(IcbcStockInDO::getTotalQuantity)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 已冲销量 = 已过账后被作废的入库单合计。
     *
     * <p>{@code StockApi} 的上限校验看的是 {@code RECEIPT_IN} 流水之和（包含已冲销的那些），
     * 所以传 {@code maxCount} 时要把它加回去。用 {@code postedTime} 区分「已过账后作废」与
     * 「待过账直接作废」：后者从未写过 {@code RECEIPT_IN} 流水。
     */
    private BigDecimal sumReversed(List<IcbcStockInDO> stockIns) {
        return stockIns.stream()
                .filter(item -> Objects.equals(item.getStatus(), StockInStatusEnum.CANCELLED.getStatus()))
                .filter(item -> item.getPostedTime() != null)
                .map(IcbcStockInDO::getTotalQuantity)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private StockInPendingRespVO toPendingResp(IcbcAcquisitionDO acquisition, BigDecimal available,
                                               BigDecimal stocked, BigDecimal remaining) {
        StockInPendingRespVO resp = new StockInPendingRespVO();
        resp.setAcquisitionId(acquisition.getId());
        resp.setAcquisitionNo(acquisition.getAcquisitionNo());
        resp.setPayeeId(acquisition.getPayeeId());
        resp.setSellerName(acquisition.getSellerName());
        resp.setGoodsConfigId(acquisition.getGoodsConfigId());
        resp.setCategoryName(acquisition.getCategoryName());
        resp.setUnit(acquisition.getUnit());
        resp.setTradeTime(acquisition.getTradeTime());
        resp.setNetWeight(acquisition.getNetWeight());
        resp.setAvailableQuantity(available);
        resp.setStockedQuantity(stocked);
        resp.setRemainingQuantity(remaining);
        return resp;
    }

    private StockInRespVO toResp(IcbcStockInDO stockIn) {
        StockInRespVO resp = BeanUtils.toBean(stockIn, StockInRespVO.class);
        StockInStatusEnum.ofStatus(stockIn.getStatus())
                .ifPresent(status -> resp.setStatusName(status.getName()));
        return resp;
    }

    /** 待入库是工作队列，按 id 倒序在内存里分页（候选集是「已验收且未入库」这类小集合）。 */
    private PageResult<StockInPendingRespVO> paginate(List<StockInPendingRespVO> list,
                                                      Integer pageNo, Integer pageSize) {
        int size = pageSize == null ? 10 : pageSize;
        if (size <= 0) {
            return new PageResult<>(list, (long) list.size());
        }
        int no = pageNo == null || pageNo < 1 ? 1 : pageNo;
        int from = (no - 1) * size;
        if (from >= list.size()) {
            return new PageResult<>(List.of(), (long) list.size());
        }
        return new PageResult<>(list.subList(from, Math.min(from + size, list.size())), (long) list.size());
    }

    private static Long normalizeZero(Long id) {
        return id == null ? 0L : id;
    }

    private String label(IcbcAcquisitionDO acquisition) {
        return acquisition.getCategoryName() == null ? "该收购单" : acquisition.getCategoryName();
    }

    private String generateStockInNo() {
        return "SI" + LocalDateTime.now().format(NO_FORMATTER) + RandomUtil.randomNumbers(4);
    }

}
