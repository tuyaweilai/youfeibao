package cn.iocoder.yudao.module.icbc.service.purchaseorder.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.erp.enums.purchase.SellerSubjectTypeEnum;
import cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo.PurchaseOrderDealReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo.PurchaseOrderDealRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo.PurchaseOrderItemReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo.PurchaseOrderItemRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo.PurchaseOrderPageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo.PurchaseOrderPriceReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo.PurchaseOrderPriceRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo.PurchaseOrderProgressRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo.PurchaseOrderRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo.PurchaseOrderSaveReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo.PurchaseOrderStatusUpdateReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.goodscfg.IcbcGoodsConfigDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payee.PayeeInfoDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.purchasecontract.IcbcPurchaseContractDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.purchaseorder.IcbcPurchaseOrderDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.purchaseorder.IcbcPurchaseOrderDealDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.purchaseorder.IcbcPurchaseOrderItemDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.purchaseorder.IcbcPurchaseOrderPriceDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.station.IcbcStationDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.goodscfg.IcbcGoodsConfigMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.payee.PayeeInfoMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.purchaseorder.IcbcPurchaseOrderDealMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.purchaseorder.IcbcPurchaseOrderItemMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.purchaseorder.IcbcPurchaseOrderMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.purchaseorder.IcbcPurchaseOrderPriceMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.station.IcbcStationMapper;
import cn.iocoder.yudao.module.icbc.enums.PurchaseOrderPriceModeEnum;
import cn.iocoder.yudao.module.icbc.enums.PurchaseOrderStatusEnum;
import cn.iocoder.yudao.module.icbc.service.purchasecontract.PurchaseContractService;
import cn.iocoder.yudao.module.icbc.service.purchaseorder.PurchaseOrderAmountDTO;
import cn.iocoder.yudao.module.icbc.service.purchaseorder.PurchaseOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.*;

/**
 * 采购订单 Service 实现（#46 T08，ADR 0027）。
 *
 * <p>状态机一条主线：草稿 →（开始执行）执行中 ⇄（暂停 / 恢复）→ 完成 →（关闭）关闭。
 * **只有「执行中」的订单可作为有效采购依据**；超期不落库，由结束日期推导。
 * 一条明细可分多次收货，已收量由成交记录汇总推导，不落冗余字段。
 */
@Service("icbcPurchaseOrderServiceImpl")
@Validated
@Slf4j
public class PurchaseOrderServiceImpl implements PurchaseOrderService {

    private static final DateTimeFormatter NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Resource
    private IcbcPurchaseOrderMapper orderMapper;
    @Resource
    private IcbcPurchaseOrderItemMapper itemMapper;
    @Resource
    private IcbcPurchaseOrderPriceMapper priceMapper;
    @Resource
    private IcbcPurchaseOrderDealMapper dealMapper;
    @Resource
    private PayeeInfoMapper payeeInfoMapper;
    @Resource
    private IcbcStationMapper stationMapper;
    @Resource
    private IcbcGoodsConfigMapper goodsConfigMapper;
    @Resource
    private PurchaseContractService purchaseContractService;

    // ==================== 写入 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createOrder(PurchaseOrderSaveReqVO createReqVO) {
        assertDateRange(createReqVO);
        IcbcPurchaseOrderDO order = new IcbcPurchaseOrderDO();
        order.setOrderNo(generateOrderNo());
        order.setStatus(PurchaseOrderStatusEnum.DRAFT.getStatus());
        applyCounterparty(order, createReqVO);
        applyContract(order, createReqVO.getContractId());
        applyStation(order, createReqVO.getStationId());
        order.setStartDate(createReqVO.getStartDate());
        order.setEndDate(createReqVO.getEndDate());
        order.setRemark(createReqVO.getRemark());
        orderMapper.insert(order);
        replaceItems(order, createReqVO.getItems());
        return order.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateOrder(PurchaseOrderSaveReqVO updateReqVO) {
        IcbcPurchaseOrderDO order = getOrder(updateReqVO.getId());
        if (!PurchaseOrderStatusEnum.DRAFT.getStatus().equals(order.getStatus())) {
            throw exception(PURCHASE_ORDER_STATUS_NOT_ALLOW,
                    PurchaseOrderStatusEnum.nameOf(order.getStatus()));
        }
        assertDateRange(updateReqVO);
        applyCounterparty(order, updateReqVO);
        applyContract(order, updateReqVO.getContractId());
        applyStation(order, updateReqVO.getStationId());
        order.setStartDate(updateReqVO.getStartDate());
        order.setEndDate(updateReqVO.getEndDate());
        order.setRemark(updateReqVO.getRemark());
        orderMapper.updateById(order);
        replaceItems(order, updateReqVO.getItems());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(PurchaseOrderStatusUpdateReqVO reqVO) {
        IcbcPurchaseOrderDO order = getOrder(reqVO.getId());
        PurchaseOrderStatusEnum from = PurchaseOrderStatusEnum.ofStatus(order.getStatus()).orElse(null);
        PurchaseOrderStatusEnum to = PurchaseOrderStatusEnum.ofStatus(reqVO.getStatus()).orElse(null);
        if (from == null || to == null || !isTransitionAllowed(from, to)) {
            throw exception(PURCHASE_ORDER_STATUS_TRANSITION_INVALID,
                    PurchaseOrderStatusEnum.nameOf(order.getStatus()),
                    PurchaseOrderStatusEnum.nameOf(reqVO.getStatus()));
        }
        if (to == PurchaseOrderStatusEnum.SUSPENDED && StrUtil.isBlank(reqVO.getReason())) {
            throw exception(PURCHASE_ORDER_SUSPEND_REASON_REQUIRED);
        }

        order.setStatus(to.getStatus());
        switch (to) {
            case EXECUTING:
                // 开始执行或从暂停恢复：清掉暂停痕迹
                order.setSuspendReason(null);
                order.setSuspendedTime(null);
                break;
            case SUSPENDED:
                order.setSuspendReason(StrUtil.trim(reqVO.getReason()));
                order.setSuspendedTime(LocalDateTime.now());
                break;
            case COMPLETED:
                order.setCompletedTime(LocalDateTime.now());
                break;
            case CLOSED:
                order.setClosedBy(SecurityFrameworkUtils.getLoginUserId());
                order.setClosedTime(LocalDateTime.now());
                order.setCloseReason(StrUtil.trim(reqVO.getReason()));
                break;
            default:
                break;
        }
        orderMapper.updateById(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteOrder(Long id) {
        IcbcPurchaseOrderDO order = getOrder(id);
        if (!PurchaseOrderStatusEnum.DRAFT.getStatus().equals(order.getStatus())) {
            throw exception(PURCHASE_ORDER_ONLY_DRAFT_DELETABLE);
        }
        itemMapper.deleteByOrderId(id);
        priceMapper.deleteByOrderId(id);
        orderMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long recordDeal(PurchaseOrderDealReqVO reqVO) {
        IcbcPurchaseOrderDO order = assertUsableAsPurchaseBasis(reqVO.getOrderId());
        IcbcPurchaseOrderItemDO item = getItem(order.getId(), reqVO.getItemId());
        if (reqVO.getQuantity() == null || reqVO.getQuantity().signum() <= 0) {
            throw exception(PURCHASE_ORDER_DEAL_QUANTITY_INVALID);
        }
        if (reqVO.getUnitPrice() == null || reqVO.getUnitPrice().signum() < 0) {
            throw exception(PURCHASE_ORDER_DEAL_PRICE_INVALID);
        }
        BigDecimal reference = resolveUnitPrice(item, reqVO.getDeliveryDate());
        boolean adjusted = reference != null
                && reqVO.getUnitPrice().compareTo(reference) != 0;
        if (adjusted && StrUtil.isBlank(reqVO.getAdjustReason())) {
            throw exception(PURCHASE_ORDER_DEAL_ADJUST_REASON_REQUIRED);
        }
        IcbcPurchaseOrderDealDO deal = IcbcPurchaseOrderDealDO.builder()
                .orderId(order.getId())
                .itemId(item.getId())
                .dealNo(generateDealNo())
                .dealTime(LocalDateTime.now())
                .deliveryDate(reqVO.getDeliveryDate())
                .quantity(reqVO.getQuantity())
                .unitPrice(reqVO.getUnitPrice())
                .referenceUnitPrice(reference)
                .priceAdjusted(adjusted)
                .adjustReason(StrUtil.trim(reqVO.getAdjustReason()))
                .sourceType(StrUtil.trim(reqVO.getSourceType()))
                .sourceId(reqVO.getSourceId())
                .sourceNo(StrUtil.trim(reqVO.getSourceNo()))
                .remark(reqVO.getRemark())
                .build();
        dealMapper.insert(deal);
        log.info("采购订单成交登记成功 - orderNo: {}, itemId: {}, quantity: {}, priceAdjusted: {}",
                order.getOrderNo(), item.getId(), deal.getQuantity(), adjusted);
        return deal.getId();
    }

    // ==================== 查询 ====================

    @Override
    public IcbcPurchaseOrderDO getOrder(Long id) {
        IcbcPurchaseOrderDO order = id == null ? null : orderMapper.selectById(id);
        if (order == null) {
            throw exception(PURCHASE_ORDER_NOT_EXISTS);
        }
        return order;
    }

    @Override
    public PurchaseOrderRespVO getDetail(Long id) {
        IcbcPurchaseOrderDO order = getOrder(id);
        return toResp(order, true);
    }

    @Override
    public PageResult<PurchaseOrderRespVO> getOrderPage(PurchaseOrderPageReqVO pageReqVO) {
        PageResult<IcbcPurchaseOrderDO> page = orderMapper.selectPage(pageReqVO);
        // 列表不带明细：计划量 / 金额已在表头快照，避免逐行展开明细与成交（N+1）
        return new PageResult<>(page.getList().stream().map(order -> toResp(order, false)).toList(),
                page.getTotal());
    }

    @Override
    public PurchaseOrderProgressRespVO getProgress(Long id) {
        IcbcPurchaseOrderDO order = getOrder(id);
        PurchaseOrderProgressRespVO resp = new PurchaseOrderProgressRespVO();
        resp.setOrderId(order.getId());
        resp.setOrderNo(order.getOrderNo());
        resp.setStatusName(statusName(order));
        resp.setTotalQuantity(order.getTotalQuantity());
        resp.setTotalAmount(order.getTotalAmount());
        List<PurchaseOrderItemRespVO> items = listItemResp(order.getId(), false);
        resp.setItems(items.stream().map(item -> {
            PurchaseOrderProgressRespVO.ItemProgress progress = new PurchaseOrderProgressRespVO.ItemProgress();
            progress.setItemId(item.getId());
            progress.setCategoryName(item.getCategoryName());
            progress.setUnit(item.getUnit());
            progress.setQuantity(item.getQuantity());
            progress.setReceivedQuantity(item.getReceivedQuantity());
            progress.setRemainingQuantity(item.getRemainingQuantity());
            progress.setDealCount(item.getDealCount());
            return progress;
        }).toList());
        resp.setReceivedQuantity(sum(items, PurchaseOrderItemRespVO::getReceivedQuantity));
        resp.setRemainingQuantity(resp.getTotalQuantity() == null ? null
                : resp.getTotalQuantity().subtract(resp.getReceivedQuantity()));
        resp.setScopeNote("本票只有「计划 / 已收」两个口径（已收 = 成交记录数量之和）；"
                + "计划 / 验收 / 入库 / 结算 / 未履行五个口径分列见 #47（T09）。");
        return resp;
    }

    @Override
    public IcbcPurchaseOrderDO assertUsableAsPurchaseBasis(Long id) {
        IcbcPurchaseOrderDO order = getOrder(id);
        if (!PurchaseOrderStatusEnum.EXECUTING.getStatus().equals(order.getStatus())
                || isExpired(order)) {
            throw exception(PURCHASE_ORDER_NOT_EFFECTIVE, order.getOrderNo());
        }
        return order;
    }

    @Override
    public List<PurchaseOrderDealRespVO> getDealList(Long orderId) {
        getOrder(orderId);
        return dealMapper.selectListByOrderId(orderId).stream().map(deal -> {
            PurchaseOrderDealRespVO resp = BeanUtils.toBean(deal, PurchaseOrderDealRespVO.class);
            IcbcPurchaseOrderItemDO item = itemMapper.selectById(deal.getItemId());
            resp.setCategoryName(item == null ? null : item.getCategoryName());
            return resp;
        }).toList();
    }

    @Override
    public BigDecimal resolveUnitPrice(Long itemId, LocalDate deliveryDate) {
        IcbcPurchaseOrderItemDO item = itemId == null ? null : itemMapper.selectById(itemId);
        if (item == null) {
            throw exception(PURCHASE_ORDER_ITEM_NOT_EXISTS, itemId);
        }
        return resolveUnitPrice(item, deliveryDate);
    }

    @Override
    public PurchaseOrderAmountDTO getOrderAmount(Long id) {
        IcbcPurchaseOrderDO order = getOrder(id);
        PurchaseOrderAmountDTO dto = new PurchaseOrderAmountDTO();
        dto.setOrderId(order.getId());
        dto.setOrderNo(order.getOrderNo());
        dto.setCounterpartyName(order.getCounterpartyName());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setUsableAsPurchaseBasis(
                PurchaseOrderStatusEnum.EXECUTING.getStatus().equals(order.getStatus()) && !isExpired(order));
        return dto;
    }

    // ==================== 内部：对手方 / 合同 / 场站 ====================

    /**
     * 对手方按主体类型六态落到其中一个档案，恰好一个非空（ADR 0029）。
     */
    private void applyCounterparty(IcbcPurchaseOrderDO order, PurchaseOrderSaveReqVO reqVO) {
        SellerSubjectTypeEnum subjectType = SellerSubjectTypeEnum.valueOf(reqVO.getCounterpartyType());
        if (subjectType == null) {
            throw exception(PURCHASE_ORDER_COUNTERPARTY_REQUIRED);
        }
        order.setCounterpartyType(subjectType.getType());
        if (subjectType.isNatural()) {
            if (reqVO.getPayeeId() == null || reqVO.getSupplierId() != null) {
                throw exception(PURCHASE_ORDER_COUNTERPARTY_REQUIRED);
            }
            PayeeInfoDO payee = payeeInfoMapper.selectById(reqVO.getPayeeId());
            if (payee == null) {
                throw exception(PURCHASE_ORDER_PAYEE_NOT_EXISTS);
            }
            order.setPayeeId(payee.getId());
            order.setSupplierId(null);
            order.setCounterpartyName(payee.getName());
        } else {
            if (reqVO.getSupplierId() == null || reqVO.getPayeeId() != null) {
                throw exception(PURCHASE_ORDER_COUNTERPARTY_REQUIRED);
            }
            if (StrUtil.isBlank(reqVO.getCounterpartyName())) {
                throw exception(PURCHASE_ORDER_SUPPLIER_NAME_REQUIRED);
            }
            order.setPayeeId(null);
            order.setSupplierId(reqVO.getSupplierId());
            order.setCounterpartyName(reqVO.getCounterpartyName().trim());
        }
    }

    /**
     * 可选关联采购合同：填了就必须已审核生效且未过期（唯一门禁在 PurchaseContractService）。
     */
    private void applyContract(IcbcPurchaseOrderDO order, Long contractId) {
        if (contractId == null) {
            order.setContractId(null);
            order.setContractNo(null);
            return;
        }
        IcbcPurchaseContractDO contract = purchaseContractService.assertUsableAsPurchaseBasis(contractId);
        order.setContractId(contract.getId());
        order.setContractNo(contract.getContractNo());
    }

    private void applyStation(IcbcPurchaseOrderDO order, Long stationId) {
        if (stationId == null) {
            order.setStationId(null);
            order.setStationName(null);
            return;
        }
        IcbcStationDO station = stationMapper.selectById(stationId);
        if (station == null) {
            throw exception(PURCHASE_ORDER_STATION_NOT_EXISTS);
        }
        order.setStationId(station.getId());
        order.setStationName(station.getName());
    }

    // ==================== 内部：明细与价格表 ====================

    /**
     * 明细整组重建（逻辑删旧行 + 插新行），与采购合同适用品类同一做法。
     */
    private void replaceItems(IcbcPurchaseOrderDO order, List<PurchaseOrderItemReqVO> items) {
        if (CollUtil.isEmpty(items)) {
            throw exception(PURCHASE_ORDER_ITEM_REQUIRED);
        }
        itemMapper.deleteByOrderId(order.getId());
        priceMapper.deleteByOrderId(order.getId());
        BigDecimal totalQuantity = BigDecimal.ZERO;
        BigDecimal totalAmount = BigDecimal.ZERO;
        int index = 0;
        for (PurchaseOrderItemReqVO itemReqVO : items) {
            index++;
            IcbcPurchaseOrderItemDO item = buildItem(order, itemReqVO, index);
            itemMapper.insert(item);
            insertPrices(order, item, itemReqVO);
            totalQuantity = totalQuantity.add(item.getQuantity());
            totalAmount = totalAmount.add(item.getAmount());
        }
        order.setTotalQuantity(totalQuantity);
        order.setTotalAmount(totalAmount.setScale(2, RoundingMode.HALF_UP));
        orderMapper.updateById(order);
    }

    private IcbcPurchaseOrderItemDO buildItem(IcbcPurchaseOrderDO order, PurchaseOrderItemReqVO reqVO, int index) {
        IcbcGoodsConfigDO goodsConfig = goodsConfigMapper.selectById(reqVO.getGoodsConfigId());
        if (goodsConfig == null) {
            throw exception(PURCHASE_ORDER_CATEGORY_NOT_EXISTS, reqVO.getGoodsConfigId());
        }
        if (reqVO.getQuantity() == null || reqVO.getQuantity().signum() <= 0) {
            throw exception(PURCHASE_ORDER_QUANTITY_INVALID, "第 " + index + " 条");
        }
        if (reqVO.getUnitPrice() != null && reqVO.getUnitPrice().signum() < 0) {
            throw exception(PURCHASE_ORDER_PRICE_INVALID, "第 " + index + " 条");
        }
        PurchaseOrderPriceModeEnum priceMode = PurchaseOrderPriceModeEnum.ofMode(reqVO.getPriceMode())
                .orElseThrow(() -> exception(PURCHASE_ORDER_PRICE_MODE_INVALID, "第 " + index + " 条"));
        // 参考单价是所有方式的必填项：固定单价就是成交价，按交货日价格表时是未覆盖日期的兜底价，
        // 也是订单计划金额（#49 的单据金额）的基准
        if (reqVO.getUnitPrice() == null) {
            throw exception(PURCHASE_ORDER_REFERENCE_PRICE_REQUIRED, "第 " + index + " 条");
        }
        BigDecimal unitPrice = reqVO.getUnitPrice();
        return IcbcPurchaseOrderItemDO.builder()
                .orderId(order.getId())
                .goodsConfigId(goodsConfig.getId())
                .categoryName(goodsConfig.getName())
                .unit(goodsConfig.getUnit())
                .quantity(reqVO.getQuantity())
                .priceMode(priceMode.getMode())
                .unitPrice(reqVO.getUnitPrice())
                .amount(reqVO.getQuantity().multiply(unitPrice).setScale(2, RoundingMode.HALF_UP))
                .remark(reqVO.getRemark())
                .build();
    }

    private void insertPrices(IcbcPurchaseOrderDO order, IcbcPurchaseOrderItemDO item,
                             PurchaseOrderItemReqVO reqVO) {
        if (CollUtil.isEmpty(reqVO.getPrices())) {
            return;
        }
        for (PurchaseOrderPriceReqVO priceReqVO : reqVO.getPrices()) {
            if (priceReqVO.getDeliveryDate() == null || priceReqVO.getUnitPrice() == null
                    || priceReqVO.getUnitPrice().signum() < 0) {
                throw exception(PURCHASE_ORDER_PRICE_TABLE_INVALID);
            }
            priceMapper.insert(IcbcPurchaseOrderPriceDO.builder()
                    .orderId(order.getId())
                    .itemId(item.getId())
                    .deliveryDate(priceReqVO.getDeliveryDate())
                    .unitPrice(priceReqVO.getUnitPrice())
                    .remark(priceReqVO.getRemark())
                    .build());
        }
    }

    /**
     * 参考价：固定单价直接用明细单价；按交货日价格表取「不晚于交货日的最新一条」，
     * 没覆盖到则回退明细单价。
     */
    private BigDecimal resolveUnitPrice(IcbcPurchaseOrderItemDO item, LocalDate deliveryDate) {
        if (!PurchaseOrderPriceModeEnum.BY_DELIVERY_DATE.getMode().equals(item.getPriceMode())
                || deliveryDate == null) {
            return item.getUnitPrice();
        }
        IcbcPurchaseOrderPriceDO price = priceMapper.selectEffectiveByItemId(item.getId(), deliveryDate);
        return price != null ? price.getUnitPrice() : item.getUnitPrice();
    }

    private IcbcPurchaseOrderItemDO getItem(Long orderId, Long itemId) {
        IcbcPurchaseOrderItemDO item = itemId == null ? null : itemMapper.selectById(itemId);
        if (item == null || !Objects.equals(item.getOrderId(), orderId)) {
            throw exception(PURCHASE_ORDER_ITEM_NOT_EXISTS, itemId);
        }
        return item;
    }

    // ==================== 内部：校验与映射 ====================

    private void assertDateRange(PurchaseOrderSaveReqVO reqVO) {
        if (reqVO.getStartDate() != null && reqVO.getEndDate() != null
                && reqVO.getEndDate().isBefore(reqVO.getStartDate())) {
            throw exception(PURCHASE_ORDER_DATE_INVALID);
        }
    }

    /**
     * 状态流转白名单。完成 / 关闭是终态（可完成后再关闭）；暂停可恢复；不允许回头再编辑。
     */
    private boolean isTransitionAllowed(PurchaseOrderStatusEnum from, PurchaseOrderStatusEnum to) {
        if (from == PurchaseOrderStatusEnum.CLOSED) {
            return false;
        }
        switch (to) {
            case EXECUTING:
                return from == PurchaseOrderStatusEnum.DRAFT || from == PurchaseOrderStatusEnum.SUSPENDED;
            case SUSPENDED:
                return from == PurchaseOrderStatusEnum.EXECUTING;
            case COMPLETED:
                return from == PurchaseOrderStatusEnum.EXECUTING;
            case CLOSED:
                return from != PurchaseOrderStatusEnum.CLOSED;
            default:
                return false;
        }
    }

    private boolean isExpired(IcbcPurchaseOrderDO order) {
        return order.getEndDate() != null && order.getEndDate().isBefore(LocalDate.now());
    }

    private String statusName(IcbcPurchaseOrderDO order) {
        boolean expired = isExpired(order);
        Integer status = order.getStatus();
        if (expired && PurchaseOrderStatusEnum.EXECUTING.getStatus().equals(status)) {
            return "过期";
        }
        return PurchaseOrderStatusEnum.nameOf(status);
    }

    private String generateOrderNo() {
        return "PO" + LocalDateTime.now().format(NO_FORMATTER) + RandomUtil.randomNumbers(4);
    }

    private String generateDealNo() {
        return "PD" + LocalDateTime.now().format(NO_FORMATTER) + RandomUtil.randomNumbers(4);
    }

    private PurchaseOrderRespVO toResp(IcbcPurchaseOrderDO order, boolean withItems) {
        PurchaseOrderRespVO resp = BeanUtils.toBean(order, PurchaseOrderRespVO.class);
        resp.setCounterpartyTypeName(SellerSubjectTypeEnum.nameOf(order.getCounterpartyType()));
        boolean expired = isExpired(order);
        resp.setExpired(expired);
        boolean usable = PurchaseOrderStatusEnum.EXECUTING.getStatus().equals(order.getStatus()) && !expired;
        resp.setUsableAsPurchaseBasis(usable);
        resp.setStatusName(statusName(order));
        resp.setItems(withItems ? listItemResp(order.getId(), true) : Collections.emptyList());
        return resp;
    }

    private List<PurchaseOrderItemRespVO> listItemResp(Long orderId, boolean withPrices) {
        return itemMapper.selectListByOrderId(orderId).stream().map(item -> {
            PurchaseOrderItemRespVO itemResp = BeanUtils.toBean(item, PurchaseOrderItemRespVO.class);
            itemResp.setPriceModeName(PurchaseOrderPriceModeEnum.nameOf(item.getPriceMode()));
            List<IcbcPurchaseOrderDealDO> deals = dealMapper.selectListByItemId(item.getId());
            BigDecimal received = deals.stream()
                    .map(IcbcPurchaseOrderDealDO::getQuantity)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            itemResp.setReceivedQuantity(received);
            itemResp.setRemainingQuantity(item.getQuantity() == null ? null
                    : item.getQuantity().subtract(received));
            itemResp.setDealCount(deals.size());
            if (withPrices) {
                itemResp.setPrices(priceMapper.selectListByItemId(item.getId()).stream()
                        .map(price -> BeanUtils.toBean(price, PurchaseOrderPriceRespVO.class)).toList());
            }
            return itemResp;
        }).toList();
    }

    private BigDecimal sum(List<PurchaseOrderItemRespVO> items,
                           Function<PurchaseOrderItemRespVO, BigDecimal> getter) {
        return items.stream().map(getter).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

}
