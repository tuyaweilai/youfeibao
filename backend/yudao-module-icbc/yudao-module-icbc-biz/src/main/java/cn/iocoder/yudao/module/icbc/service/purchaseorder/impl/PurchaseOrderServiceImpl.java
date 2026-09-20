package cn.iocoder.yudao.module.icbc.service.purchaseorder.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.erp.enums.purchase.SellerSubjectTypeEnum;
import cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo.PurchaseArrangementRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo.PurchaseOrderDealReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo.PurchaseOrderDealRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo.PurchaseOrderDeliveryCheckReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo.PurchaseOrderDeliveryCheckRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo.PurchaseOrderItemReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo.PurchaseOrderItemRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo.PurchaseOrderPageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo.PurchaseOrderPriceReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo.PurchaseOrderPriceRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo.PurchaseOrderProgressRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo.PurchaseOrderRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo.PurchaseOrderSaveReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo.PurchaseOrderSettingRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo.PurchaseOrderSettingSaveReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo.PurchaseOrderStatusUpdateReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.acquisition.IcbcAcquisitionDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.goodscfg.IcbcGoodsConfigDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payee.PayeeInfoDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.purchasecontract.IcbcPurchaseContractDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.purchaseorder.IcbcPurchaseExceptionDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.purchaseorder.IcbcPurchaseOrderDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.purchaseorder.IcbcPurchaseOrderDealDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.purchaseorder.IcbcPurchaseOrderItemDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.purchaseorder.IcbcPurchaseOrderPriceDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.purchaseorder.IcbcPurchaseSettingDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.station.IcbcStationDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.acquisition.IcbcAcquisitionMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.goodscfg.IcbcGoodsConfigMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.payee.PayeeInfoMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.purchaseorder.IcbcPurchaseExceptionMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.purchaseorder.IcbcPurchaseOrderDealMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.purchaseorder.IcbcPurchaseOrderItemMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.purchaseorder.IcbcPurchaseOrderMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.purchaseorder.IcbcPurchaseOrderPriceMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.purchaseorder.IcbcPurchaseSettingMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.station.IcbcStationMapper;
import cn.iocoder.yudao.module.icbc.enums.PurchaseDeliveryRuleEnum;
import cn.iocoder.yudao.module.icbc.enums.PurchaseDealSourceTypeEnum;
import cn.iocoder.yudao.module.icbc.enums.PurchaseExceptionStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.PurchaseExceptionTypeEnum;
import cn.iocoder.yudao.module.icbc.enums.PurchasePerformanceBasisEnum;
import cn.iocoder.yudao.module.icbc.enums.PurchaseOrderPriceModeEnum;
import cn.iocoder.yudao.module.icbc.enums.PurchaseOrderStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.PurchaseProgressMeasureEnum;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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
    private IcbcPurchaseSettingMapper settingMapper;
    @Resource
    private IcbcPurchaseExceptionMapper purchaseExceptionMapper;
    @Resource
    private IcbcAcquisitionMapper acquisitionMapper;
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
        IcbcPurchaseOrderDO order = getOrder(reqVO.getOrderId());
        if (reqVO.getQuantity() == null || reqVO.getQuantity().signum() == 0) {
            throw exception(PURCHASE_ORDER_DEAL_QUANTITY_INVALID);
        }
        if (reqVO.getQuantity().signum() > 0) {
            // 收货走交货门禁：超量 / 过期 / 跨场站按企业配置拦截或要求授权（#47 AC2）。
            // 门禁放在这里，是因为成交记录就是「这一车验收了多少」落到订单上的唯一入口。
            PurchaseOrderDeliveryCheckReqVO deliveryCheck = new PurchaseOrderDeliveryCheckReqVO();
            deliveryCheck.setOrderId(order.getId());
            deliveryCheck.setItemId(reqVO.getItemId());
            deliveryCheck.setQuantity(reqVO.getQuantity());
            assertDeliveryAllowed(deliveryCheck);
        } else if (PurchaseOrderStatusEnum.DRAFT.getStatus().equals(order.getStatus())) {
            // 退货（负数）是扣回已发生的业务，草稿单还没有任何业务可言；
            // 已暂停 / 完成 / 关闭的订单仍可登记退货，否则货退回来没地方记（#47 AC3）。
            throw exception(PURCHASE_ORDER_NOT_DELIVERABLE, order.getOrderNo());
        }
        IcbcPurchaseOrderItemDO item = getItem(order.getId(), reqVO.getItemId());
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
        IcbcPurchaseSettingDO setting = resolveSetting();
        PurchasePerformanceBasisEnum basis = performanceBasisOf(setting);
        List<IcbcPurchaseOrderItemDO> items = itemMapper.selectListByOrderId(order.getId());
        List<IcbcPurchaseOrderDealDO> deals = dealMapper.selectListByOrderId(order.getId());
        Set<Long> settledAcquisitionIds = loadSettledAcquisitionIds(deals);
        Map<Long, List<IcbcPurchaseOrderDealDO>> dealsByItem = deals.stream()
                .collect(Collectors.groupingBy(IcbcPurchaseOrderDealDO::getItemId));

        // 整单五口径。验收 = 全部成交记录；结算 = 其中来源收购单已归入结算单的那部分
        BigDecimal plan = zeroIfNull(order.getTotalQuantity());
        BigDecimal accepted = sumDealQuantity(deals);
        BigDecimal settled = sumDealQuantity(settledDeals(deals, settledAcquisitionIds));
        BigDecimal basisQuantity = basis == PurchasePerformanceBasisEnum.SETTLED ? settled : accepted;
        BigDecimal unperformed = plan.subtract(basisQuantity);

        PurchaseOrderProgressRespVO resp = new PurchaseOrderProgressRespVO();
        resp.setOrderId(order.getId());
        resp.setOrderNo(order.getOrderNo());
        resp.setStatusName(statusName(order));
        resp.setTotalQuantity(order.getTotalQuantity());
        resp.setTotalAmount(order.getTotalAmount());
        resp.setPlanQuantity(plan);
        resp.setAcceptedQuantity(accepted);
        // 入库单（#52）尚未落地：不给 0（会被读成「一件没入库」），给 null + 原因，见 measures
        resp.setStockedQuantity(null);
        resp.setSettledQuantity(settled);
        resp.setUnperformedQuantity(unperformed);
        resp.setCompletionBasis(basis.getCode());
        resp.setCompletionBasisName(basis.getName());
        resp.setCompletionBasisDefinition(basis.getMeasure().getDefinition());
        resp.setCompletionBasisQuantity(basisQuantity);
        resp.setCompletionRatio(completionRatio(plan, basisQuantity));

        // 逐条明细同样五口径，口径与整单一致
        List<String> overQuantityCategories = new ArrayList<>();
        List<PurchaseOrderProgressRespVO.ItemProgress> itemProgresses = new ArrayList<>();
        for (IcbcPurchaseOrderItemDO item : items) {
            List<IcbcPurchaseOrderDealDO> itemDeals = dealsByItem.getOrDefault(item.getId(), Collections.emptyList());
            BigDecimal itemPlan = zeroIfNull(item.getQuantity());
            BigDecimal itemAccepted = sumDealQuantity(itemDeals);
            BigDecimal itemSettled = sumDealQuantity(settledDeals(itemDeals, settledAcquisitionIds));
            BigDecimal itemBasis = basis == PurchasePerformanceBasisEnum.SETTLED ? itemSettled : itemAccepted;
            boolean over = itemAccepted.compareTo(itemPlan) > 0;
            if (over) {
                overQuantityCategories.add(item.getCategoryName());
            }
            PurchaseOrderProgressRespVO.ItemProgress progress = new PurchaseOrderProgressRespVO.ItemProgress();
            progress.setItemId(item.getId());
            progress.setCategoryName(item.getCategoryName());
            progress.setUnit(item.getUnit());
            progress.setQuantity(itemPlan);
            progress.setAcceptedQuantity(itemAccepted);
            progress.setStockedQuantity(null);
            progress.setSettledQuantity(itemSettled);
            progress.setUnperformedQuantity(itemPlan.subtract(itemBasis));
            progress.setOverQuantity(over);
            progress.setDealCount(itemDeals.size());
            itemProgresses.add(progress);
        }
        resp.setItems(itemProgresses);

        Map<String, BigDecimal> measureQuantities = new HashMap<>();
        measureQuantities.put(PurchaseProgressMeasureEnum.PLAN.getCode(), plan);
        measureQuantities.put(PurchaseProgressMeasureEnum.ACCEPTED.getCode(), accepted);
        measureQuantities.put(PurchaseProgressMeasureEnum.SETTLED.getCode(), settled);
        measureQuantities.put(PurchaseProgressMeasureEnum.UNPERFORMED.getCode(), unperformed);
        resp.setMeasures(buildMeasures(measureQuantities));
        resp.setAnomalies(buildAnomalies(order, overQuantityCategories));
        resp.setScopeNote("履约分计划 / 验收 / 入库 / 结算 / 未履行五个口径，各有各的来源，不相加、不互相代替；"
                + "完成比例按「" + basis.getName() + "」计算（可在采购履约配置里改）。"
                + "入库口径等入库单（T14 / #52）落地后接入，现在不计数。");
        return resp;
    }

    @Override
    public PurchaseOrderDeliveryCheckRespVO checkDelivery(PurchaseOrderDeliveryCheckReqVO reqVO) {
        IcbcPurchaseOrderDO order = getOrder(reqVO.getOrderId());
        IcbcPurchaseSettingDO setting = resolveSetting();
        PurchaseOrderDeliveryCheckRespVO resp = new PurchaseOrderDeliveryCheckRespVO();
        resp.setOrderId(order.getId());
        resp.setOrderNo(order.getOrderNo());
        resp.setItemId(reqVO.getItemId());
        resp.setQuantity(reqVO.getQuantity());
        resp.setViolations(new ArrayList<>());
        resp.setScopeNote("超量 / 过期 / 跨场站三类异常各自按企业配置处理（拦截或提交授权审核）；"
                + "已通过的授权单只放宽它自己那一件事，不等于把订单变回有效采购依据。"
                + "订单处于草稿 / 暂停 / 完成 / 关闭时不接受交货，与异常配置无关。");

        // 一、订单状态硬门禁：与三企业配置无关，任何授权都放宽不了
        PurchaseOrderStatusEnum status = PurchaseOrderStatusEnum.ofStatus(order.getStatus()).orElse(null);
        if (status == PurchaseOrderStatusEnum.CLOSED) {
            return reject(resp, "NOT_DELIVERABLE", "订单已关闭，不能再登记交货；已发生的业务仍可回查");
        }
        if (status != PurchaseOrderStatusEnum.EXECUTING) {
            return reject(resp, "NOT_DELIVERABLE", "订单当前状态为「" + PurchaseOrderStatusEnum.nameOf(order.getStatus())
                    + "」，不是「执行中」的有效采购依据");
        }

        // 二、过期
        if (isExpired(order)) {
            resp.getViolations().add(newViolation(PurchaseExceptionTypeEnum.EXPIRED, setting,
                    "订单执行结束日期 " + order.getEndDate() + " 已过", null));
        }
        // 三、跨场站（订单未指定执行场站时不构成异常）
        if (order.getStationId() != null && reqVO.getStationId() != null
                && !order.getStationId().equals(reqVO.getStationId())) {
            resp.getViolations().add(newViolation(PurchaseExceptionTypeEnum.CROSS_STATION, setting,
                    "本次交货场站（" + stationName(reqVO.getStationId()) + "）不是订单的执行场站（"
                            + stationName(order.getStationId()) + "）", null));
        }
        // 四、超量（未指明明细时无法按明细比对，不猜）
        if (reqVO.getItemId() != null && reqVO.getQuantity() != null) {
            IcbcPurchaseOrderItemDO item = getItem(order.getId(), reqVO.getItemId());
            BigDecimal after = sumDealQuantity(dealMapper.selectListByItemId(item.getId()))
                    .add(reqVO.getQuantity());
            BigDecimal overage = after.subtract(zeroIfNull(item.getQuantity()));
            if (overage.signum() > 0) {
                resp.getViolations().add(newViolation(PurchaseExceptionTypeEnum.OVER_QUANTITY, setting,
                        "本次交货后累计验收量 " + plain(after) + " 超过计划量 " + plain(item.getQuantity()),
                        overage));
            }
        }

        // 五、逐条看有没有生效中的授权放行，再给出结论
        String resolution = "OK";
        for (PurchaseOrderDeliveryCheckRespVO.Violation violation : resp.getViolations()) {
            resolveViolation(order, violation, reqVO);
            if (Boolean.TRUE.equals(violation.getResolved())) {
                continue;
            }
            if (PurchaseDeliveryRuleEnum.BLOCK.getCode().equals(violation.getRule())) {
                resolution = "BLOCKED";
            } else if ("OK".equals(resolution)) {
                resolution = "NEEDS_APPROVAL";
            }
        }
        resp.setResolution(resolution);
        resp.setResolutionName(resolutionName(resolution));
        resp.setAllowed("OK".equals(resolution));
        return resp;
    }

    @Override
    public void assertDeliveryAllowed(PurchaseOrderDeliveryCheckReqVO reqVO) {
        PurchaseOrderDeliveryCheckRespVO check = checkDelivery(reqVO);
        if (Boolean.TRUE.equals(check.getAllowed())) {
            return;
        }
        // 订单状态不允许：与异常配置无关，没有授权这一条路（错误码区分「已关闭」与「不是执行中」）
        if ("NOT_DELIVERABLE".equals(check.getResolution())) {
            IcbcPurchaseOrderDO order = getOrder(reqVO.getOrderId());
            if (PurchaseOrderStatusEnum.CLOSED.getStatus().equals(order.getStatus())) {
                throw exception(PURCHASE_ORDER_CLOSED_NOT_DELIVERABLE, order.getOrderNo());
            }
            throw exception(PURCHASE_ORDER_NOT_DELIVERABLE, order.getOrderNo());
        }
        String summary = check.getViolations().stream()
                .filter(violation -> !Boolean.TRUE.equals(violation.getResolved()))
                .map(violation -> violation.getExceptionTypeName() + "：" + violation.getMessage())
                .collect(Collectors.joining("；"));
        if ("BLOCKED".equals(check.getResolution())) {
            throw exception(PURCHASE_ORDER_DELIVERY_BLOCKED, summary);
        }
        throw exception(PURCHASE_ORDER_DELIVERY_NEEDS_APPROVAL, summary);
    }

    @Override
    public PurchaseOrderSettingRespVO getSetting() {
        IcbcPurchaseSettingDO setting = resolveSetting();
        PurchaseOrderSettingRespVO resp = BeanUtils.toBean(setting, PurchaseOrderSettingRespVO.class);
        PurchasePerformanceBasisEnum basis = performanceBasisOf(setting);
        resp.setPerformanceBasisName(basis.getName());
        resp.setPerformanceBasisDefinition(basis.getMeasure().getDefinition());
        resp.setOverQuantityRuleName(PurchaseDeliveryRuleEnum.nameOf(setting.getOverQuantityRule()));
        resp.setExpiredRuleName(PurchaseDeliveryRuleEnum.nameOf(setting.getExpiredRule()));
        resp.setCrossStationRuleName(PurchaseDeliveryRuleEnum.nameOf(setting.getCrossStationRule()));
        resp.setScopeNote("完成比例只按能取到数的口径算（验收 / 结算）；三类异常要么被拦截，要么留下一条"
                + "可追溯的授权记录，没有默认放行。未配置过的租户取默认值：按验收口径、三类异常都拦截。");
        return resp;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSetting(PurchaseOrderSettingSaveReqVO reqVO) {
        PurchasePerformanceBasisEnum basis = PurchasePerformanceBasisEnum.ofCode(reqVO.getPerformanceBasis())
                .orElseThrow(() -> exception(PURCHASE_SETTING_PERFORMANCE_BASIS_INVALID,
                        reqVO.getPerformanceBasis()));
        if (!basis.getMeasure().isAvailable()) {
            throw exception(PURCHASE_SETTING_PERFORMANCE_BASIS_INVALID, reqVO.getPerformanceBasis());
        }
        assertRule(reqVO.getOverQuantityRule());
        assertRule(reqVO.getExpiredRule());
        assertRule(reqVO.getCrossStationRule());

        IcbcPurchaseSettingDO setting = settingMapper.selectSetting();
        boolean insert = setting == null;
        if (insert) {
            setting = new IcbcPurchaseSettingDO();
        }
        setting.setPerformanceBasis(basis.getCode());
        setting.setOverQuantityRule(reqVO.getOverQuantityRule());
        setting.setExpiredRule(reqVO.getExpiredRule());
        setting.setCrossStationRule(reqVO.getCrossStationRule());
        setting.setRemark(reqVO.getRemark());
        if (insert) {
            settingMapper.insert(setting);
        } else {
            settingMapper.updateById(setting);
        }
    }

    // ==================== 内部：五口径与履约门禁 ====================

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

    @Override
    public List<PurchaseArrangementRespVO> getUsableArrangements(Long payeeId) {
        if (payeeId == null) {
            return Collections.emptyList();
        }
        // 只列自然人交易对方、执行中的订单；过期（end_date 早于今天）在内存里剔掉，
        // 与 assertUsableAsPurchaseBasis 共用同一个 isExpired 判定，不另写一套。
        return orderMapper.selectList(new LambdaQueryWrapperX<IcbcPurchaseOrderDO>()
                        .eq(IcbcPurchaseOrderDO::getCounterpartyType, SellerSubjectTypeEnum.NATURAL.getType())
                        .eq(IcbcPurchaseOrderDO::getPayeeId, payeeId)
                        .eq(IcbcPurchaseOrderDO::getStatus, PurchaseOrderStatusEnum.EXECUTING.getStatus())
                        .orderByDesc(IcbcPurchaseOrderDO::getId))
                .stream()
                .filter(order -> !isExpired(order))
                .map(this::toArrangement)
                .toList();
    }

    @Override
    public IcbcPurchaseOrderItemDO getOrderItem(Long orderId, Long itemId) {
        return getItem(orderId, itemId);
    }

    private PurchaseArrangementRespVO toArrangement(IcbcPurchaseOrderDO order) {
        PurchaseArrangementRespVO resp = new PurchaseArrangementRespVO();
        resp.setOrderId(order.getId());
        resp.setOrderNo(order.getOrderNo());
        resp.setContractNo(order.getContractNo());
        resp.setCounterpartyName(order.getCounterpartyName());
        resp.setStationId(order.getStationId());
        resp.setStationName(order.getStationName());
        resp.setStartDate(order.getStartDate());
        resp.setEndDate(order.getEndDate());
        resp.setItems(itemMapper.selectListByOrderId(order.getId()).stream().map(item -> {
            PurchaseArrangementRespVO.Item itemResp = new PurchaseArrangementRespVO.Item();
            itemResp.setItemId(item.getId());
            itemResp.setGoodsConfigId(item.getGoodsConfigId());
            itemResp.setCategoryName(item.getCategoryName());
            itemResp.setUnit(item.getUnit());
            itemResp.setPlanQuantity(item.getQuantity());
            itemResp.setUnitPrice(item.getUnitPrice());
            return itemResp;
        }).toList());
        return resp;
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

    // ==================== 内部：配置解析 ====================

    /**
     * 取本租户的履约配置；没配过时给默认值，而不是「没配就放行」。
     * 默认：完成比例按验收口径，三类异常都拦截。
     */
    private IcbcPurchaseSettingDO resolveSetting() {
        IcbcPurchaseSettingDO setting = settingMapper.selectSetting();
        if (setting != null) {
            return setting;
        }
        return IcbcPurchaseSettingDO.builder()
                .performanceBasis(PurchasePerformanceBasisEnum.ACCEPTED.getCode())
                .overQuantityRule(PurchaseDeliveryRuleEnum.BLOCK.getCode())
                .expiredRule(PurchaseDeliveryRuleEnum.BLOCK.getCode())
                .crossStationRule(PurchaseDeliveryRuleEnum.BLOCK.getCode())
                .build();
    }

    private PurchasePerformanceBasisEnum performanceBasisOf(IcbcPurchaseSettingDO setting) {
        return PurchasePerformanceBasisEnum.ofCode(setting.getPerformanceBasis())
                .orElse(PurchasePerformanceBasisEnum.ACCEPTED);
    }

    private PurchaseDeliveryRuleEnum ruleOf(IcbcPurchaseSettingDO setting, PurchaseExceptionTypeEnum type) {
        String code;
        switch (type) {
            case OVER_QUANTITY:
                code = setting.getOverQuantityRule();
                break;
            case EXPIRED:
                code = setting.getExpiredRule();
                break;
            default:
                code = setting.getCrossStationRule();
                break;
        }
        return PurchaseDeliveryRuleEnum.ofCode(code).orElse(PurchaseDeliveryRuleEnum.BLOCK);
    }

    private void assertRule(String rule) {
        if (!PurchaseDeliveryRuleEnum.ofCode(rule).isPresent()) {
            throw exception(PURCHASE_SETTING_RULE_INVALID, rule);
        }
    }

    // ==================== 内部：五口径取数 ====================

    /**
     * 成交记录的单位（部门）数量口径：正数是验收，负数是退货，求和即口径量。
     */
    private BigDecimal sumDealQuantity(List<IcbcPurchaseOrderDealDO> deals) {
        return deals.stream()
                .map(IcbcPurchaseOrderDealDO::getQuantity)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 从成交记录里找出「已归入结算单」的来源收购单编号。
     *
     * <p>只读收购单已有的 {@code settlement_id}，不依赖收购单与订单的关联列（那是 #51 的落地物）。
     */
    private Set<Long> loadSettledAcquisitionIds(List<IcbcPurchaseOrderDealDO> deals) {
        Set<Long> ids = deals.stream()
                .filter(deal -> PurchaseDealSourceTypeEnum.ACQUISITION.getType().equals(deal.getSourceType()))
                .map(IcbcPurchaseOrderDealDO::getSourceId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Collections.emptySet();
        }
        Set<Long> settled = new HashSet<>();
        for (IcbcAcquisitionDO acquisition : acquisitionMapper.selectBatchIds(ids)) {
            if (acquisition.getSettlementId() != null) {
                settled.add(acquisition.getId());
            }
        }
        return settled;
    }

    private List<IcbcPurchaseOrderDealDO> settledDeals(List<IcbcPurchaseOrderDealDO> deals,
                                                      Set<Long> settledAcquisitionIds) {
        return deals.stream()
                .filter(deal -> isSettledDeal(deal, settledAcquisitionIds))
                .collect(Collectors.toList());
    }

    private boolean isSettledDeal(IcbcPurchaseOrderDealDO deal, Set<Long> settledAcquisitionIds) {
        return PurchaseDealSourceTypeEnum.ACQUISITION.getType().equals(deal.getSourceType())
                && deal.getSourceId() != null
                && settledAcquisitionIds.contains(deal.getSourceId());
    }

    private BigDecimal completionRatio(BigDecimal plan, BigDecimal basisQuantity) {
        if (plan == null || plan.signum() == 0) {
            return null;
        }
        return basisQuantity.divide(plan, 4, RoundingMode.HALF_UP);
    }

    /**
     * 五口径清单：编码 / 名称 / 数量 / 口径说明 / 数据来源 / 是否取得到数。顺序与枚举一致。
     */
    private List<PurchaseOrderProgressRespVO.Measure> buildMeasures(Map<String, BigDecimal> quantities) {
        List<PurchaseOrderProgressRespVO.Measure> measures = new ArrayList<>();
        for (PurchaseProgressMeasureEnum measure : PurchaseProgressMeasureEnum.values()) {
            PurchaseOrderProgressRespVO.Measure vo = new PurchaseOrderProgressRespVO.Measure();
            vo.setCode(measure.getCode());
            vo.setName(measure.getName());
            vo.setQuantity(measure.isAvailable() ? quantities.get(measure.getCode()) : null);
            vo.setDefinition(measure.getDefinition());
            vo.setSource(measure.getSource());
            vo.setAvailable(measure.isAvailable());
            vo.setUnavailableReason(measure.getUnavailableReason());
            measures.add(vo);
        }
        return measures;
    }

    /**
     * 履约异常：超量、执行中却已过期、有待审核的授权申请。数字之外还要看得见异常。
     */
    private List<PurchaseOrderProgressRespVO.Anomaly> buildAnomalies(IcbcPurchaseOrderDO order,
                                                                    List<String> overQuantityCategories) {
        List<PurchaseOrderProgressRespVO.Anomaly> anomalies = new ArrayList<>();
        if (!overQuantityCategories.isEmpty()) {
            anomalies.add(buildAnomaly("OVER_QUANTITY", "超量",
                    "验收量已超过计划量：" + String.join("、", overQuantityCategories)
                            + "；超出部分按企业配置拦截或提交授权审核", "DANGER"));
        }
        if (PurchaseOrderStatusEnum.EXECUTING.getStatus().equals(order.getStatus()) && isExpired(order)) {
            anomalies.add(buildAnomaly("EXPIRED_EXECUTING", "过期",
                    "订单执行结束日期 " + order.getEndDate() + " 已过，仍处于执行中；继续交货需按配置提交授权审核",
                    "WARNING"));
        }
        long pending = purchaseExceptionMapper.selectListByOrderId(order.getId()).stream()
                .filter(exception -> PurchaseExceptionStatusEnum.PENDING.getStatus().equals(exception.getStatus()))
                .count();
        if (pending > 0) {
            anomalies.add(buildAnomaly("PENDING_EXCEPTION", "待审核授权",
                    "本单有 " + pending + " 条履约异常授权申请待审核", "WARNING"));
        }
        return anomalies;
    }

    private PurchaseOrderProgressRespVO.Anomaly buildAnomaly(String code, String name, String message,
                                                             String severity) {
        PurchaseOrderProgressRespVO.Anomaly anomaly = new PurchaseOrderProgressRespVO.Anomaly();
        anomaly.setCode(code);
        anomaly.setName(name);
        anomaly.setMessage(message);
        anomaly.setSeverity(severity);
        return anomaly;
    }

    // ==================== 内部：交货门禁 ====================

    private PurchaseOrderDeliveryCheckRespVO.Violation newViolation(PurchaseExceptionTypeEnum type,
                                                                    IcbcPurchaseSettingDO setting,
                                                                    String message, BigDecimal overage) {
        PurchaseOrderDeliveryCheckRespVO.Violation violation = new PurchaseOrderDeliveryCheckRespVO.Violation();
        violation.setExceptionType(type.getCode());
        violation.setExceptionTypeName(type.getName());
        violation.setMessage(message);
        PurchaseDeliveryRuleEnum rule = ruleOf(setting, type);
        violation.setRule(rule.getCode());
        violation.setRuleName(rule.getName());
        violation.setResolved(false);
        violation.setOverageQuantity(overage);
        return violation;
    }

    /**
     * 订单状态硬门禁的结论：订单不是有效采购依据时不给异常配置留口子。
     */
    private PurchaseOrderDeliveryCheckRespVO reject(PurchaseOrderDeliveryCheckRespVO resp, String resolution,
                                                    String message) {
        PurchaseOrderDeliveryCheckRespVO.Violation violation = new PurchaseOrderDeliveryCheckRespVO.Violation();
        violation.setMessage(message);
        violation.setResolved(false);
        resp.getViolations().add(violation);
        resp.setResolution(resolution);
        resp.setResolutionName(resolutionName(resolution));
        resp.setAllowed(false);
        return resp;
    }

    private String resolutionName(String resolution) {
        switch (resolution) {
            case "OK":
                return "允许";
            case "BLOCKED":
                return "被企业配置拦截";
            case "NEEDS_APPROVAL":
                return "需先提交授权审核";
            case "NOT_DELIVERABLE":
                return "订单状态不允许";
            default:
                return resolution;
        }
    }

    /**
     * 看这条异常有没有被生效中的授权放行；没放行时把待审核的那条带出来，告诉用户下一步去哪。
     */
    private void resolveViolation(IcbcPurchaseOrderDO order, PurchaseOrderDeliveryCheckRespVO.Violation violation,
                                  PurchaseOrderDeliveryCheckReqVO reqVO) {
        PurchaseExceptionTypeEnum type = PurchaseExceptionTypeEnum.ofCode(violation.getExceptionType()).orElse(null);
        if (type == null) {
            violation.setResolved(false);
            return;
        }
        List<IcbcPurchaseExceptionDO> approved = purchaseExceptionMapper
                .selectListByOrderIdAndTypeAndStatus(order.getId(), type.getCode(),
                        PurchaseExceptionStatusEnum.APPROVED.getStatus())
                .stream().filter(this::isEffective).collect(Collectors.toList());
        IcbcPurchaseExceptionDO matched = null;
        switch (type) {
            case OVER_QUANTITY:
                BigDecimal allowance = BigDecimal.ZERO;
                for (IcbcPurchaseExceptionDO exception : approved) {
                    if (!Objects.equals(exception.getItemId(), reqVO.getItemId())) {
                        continue;
                    }
                    BigDecimal extra = zeroIfNull(exception.getApprovedQuantity());
                    if (extra.signum() <= 0) {
                        continue;
                    }
                    allowance = allowance.add(extra);
                    if (matched == null) {
                        matched = exception;
                    }
                }
                // 超量部分必须被授权量盖住；没盖住就不算放行
                if (matched == null || violation.getOverageQuantity() == null
                        || violation.getOverageQuantity().compareTo(allowance) > 0) {
                    matched = null;
                }
                break;
            case CROSS_STATION:
                matched = approved.stream()
                        .filter(exception -> exception.getStationId() != null
                                && exception.getStationId().equals(reqVO.getStationId()))
                        .findFirst().orElse(null);
                break;
            default:
                matched = approved.isEmpty() ? null : approved.get(0);
                break;
        }
        violation.setResolved(matched != null);
        if (matched != null) {
            violation.setResolvedByExceptionId(matched.getId());
            violation.setResolvedByExceptionNo(matched.getExceptionNo());
            return;
        }
        IcbcPurchaseExceptionDO pending = purchaseExceptionMapper.selectPending(order.getId(), type.getCode(),
                reqVO.getItemId());
        violation.setPendingExceptionId(pending == null ? null : pending.getId());
    }

    private boolean isEffective(IcbcPurchaseExceptionDO exception) {
        return exception.getValidUntil() == null || !exception.getValidUntil().isBefore(LocalDate.now());
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /**
     * 面向人的数字：去掉没意义的尾零（105.0000 → 105），避免把库里的精度漏到界面与报错上。
     */
    private String plain(BigDecimal value) {
        return value == null ? "-" : value.stripTrailingZeros().toPlainString();
    }

    /**
     * 场站名（报错里不能只给编号）。查不到时退回编号，不编造名字。
     */
    private String stationName(Long stationId) {
        IcbcStationDO station = stationId == null ? null : stationMapper.selectById(stationId);
        return station == null ? String.valueOf(stationId) : station.getName();
    }

}
