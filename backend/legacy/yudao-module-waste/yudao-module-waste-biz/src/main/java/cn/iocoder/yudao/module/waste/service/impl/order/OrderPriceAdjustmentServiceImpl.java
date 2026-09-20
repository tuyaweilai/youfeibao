package cn.iocoder.yudao.module.waste.service.impl.order;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.waste.controller.admin.order.vo.OrderPriceAdjustmentCreateReqVO;
import cn.iocoder.yudao.module.waste.controller.admin.order.vo.OrderPriceAdjustmentPageReqVO;
import cn.iocoder.yudao.module.waste.controller.admin.order.vo.OrderPriceAdjustmentRespVO;
import cn.iocoder.yudao.module.waste.dal.dataobject.order.OrderPriceAdjustmentDO;
import cn.iocoder.yudao.module.waste.dal.mysql.order.OrderPriceAdjustmentMapper;
import cn.iocoder.yudao.module.waste.service.order.OrderPriceAdjustmentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.waste.enums.ErrorCodeConstants.ORDER_PRICE_ADJUSTMENT_NOT_EXISTS;

/**
 * 订单价格调整记录 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
@Slf4j
public class OrderPriceAdjustmentServiceImpl implements OrderPriceAdjustmentService {

    @Resource
    private OrderPriceAdjustmentMapper orderPriceAdjustmentMapper;

    @Override
    public Long createOrderPriceAdjustment(@Valid OrderPriceAdjustmentCreateReqVO createReqVO) {
        // 插入
        OrderPriceAdjustmentDO orderPriceAdjustment = BeanUtils.toBean(createReqVO, OrderPriceAdjustmentDO.class);
        orderPriceAdjustmentMapper.insert(orderPriceAdjustment);
        // 返回
        return orderPriceAdjustment.getId();
    }

    @Override
    public void deleteOrderPriceAdjustment(Long id) {
        // 校验存在
        validateOrderPriceAdjustmentExists(id);
        // 删除
        orderPriceAdjustmentMapper.deleteById(id);
    }

    @Override
    public OrderPriceAdjustmentDO getOrderPriceAdjustment(Long id) {
        return orderPriceAdjustmentMapper.selectById(id);
    }

    @Override
    public OrderPriceAdjustmentRespVO getOrderPriceAdjustmentDetail(Long id) {
        OrderPriceAdjustmentDO orderPriceAdjustment = validateOrderPriceAdjustmentExists(id);
        return BeanUtils.toBean(orderPriceAdjustment, OrderPriceAdjustmentRespVO.class);
    }

    @Override
    public PageResult<OrderPriceAdjustmentDO> getOrderPriceAdjustmentPage(OrderPriceAdjustmentPageReqVO pageReqVO) {
        PageResult<OrderPriceAdjustmentDO> pageResult = orderPriceAdjustmentMapper.selectPage(pageReqVO);
        return pageResult;
    }

    @Override
    public List<OrderPriceAdjustmentDO> getOrderPriceAdjustmentList(OrderPriceAdjustmentPageReqVO exportReqVO) {
        return orderPriceAdjustmentMapper.selectList(exportReqVO);
    }

    @Override
    public List<OrderPriceAdjustmentDO> getOrderPriceAdjustmentListByOrderId(Long orderId) {
        return orderPriceAdjustmentMapper.selectListByOrderId(orderId);
    }

    @Override
    public List<OrderPriceAdjustmentDO> getOrderPriceAdjustmentListByAdjustmentType(Integer adjustmentType) {
        return orderPriceAdjustmentMapper.selectListByAdjustmentType(adjustmentType);
    }

    @Override
    public List<OrderPriceAdjustmentDO> getOrderPriceAdjustmentListByStatus(Integer status) {
        return orderPriceAdjustmentMapper.selectListByStatus(status);
    }

    @Override
    public List<OrderPriceAdjustmentDO> getPendingOrderPriceAdjustments() {
        return orderPriceAdjustmentMapper.selectPendingAdjustments();
    }

    @Override
    public List<OrderPriceAdjustmentDO> getConfirmedOrderPriceAdjustments() {
        return orderPriceAdjustmentMapper.selectConfirmedAdjustments();
    }

    @Override
    public List<OrderPriceAdjustmentDO> getRejectedOrderPriceAdjustments() {
        return orderPriceAdjustmentMapper.selectRejectedAdjustments();
    }

    @Override
    public List<OrderPriceAdjustmentDO> getOrderPriceAdjustmentListByRelatedWeighingId(Long relatedWeighingId) {
        return orderPriceAdjustmentMapper.selectListByRelatedWeighingId(relatedWeighingId);
    }

    @Override
    public List<OrderPriceAdjustmentDO> getOrderPriceAdjustmentListByConfirmedBy(String confirmedBy) {
        return orderPriceAdjustmentMapper.selectListByConfirmedBy(confirmedBy);
    }

    @Override
    public List<OrderPriceAdjustmentDO> getOrderPriceAdjustmentListByConfirmedTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        return orderPriceAdjustmentMapper.selectListByConfirmedTimeRange(startTime, endTime);
    }

    @Override
    public List<OrderPriceAdjustmentDO> getPriceAdjustments() {
        return orderPriceAdjustmentMapper.selectPriceAdjustments();
    }

    @Override
    public List<OrderPriceAdjustmentDO> getQuantityAdjustments() {
        return orderPriceAdjustmentMapper.selectQuantityAdjustments();
    }

    @Override
    public List<OrderPriceAdjustmentDO> getPositiveAdjustments() {
        return orderPriceAdjustmentMapper.selectPositiveAdjustments();
    }

    @Override
    public List<OrderPriceAdjustmentDO> getNegativeAdjustments() {
        return orderPriceAdjustmentMapper.selectNegativeAdjustments();
    }

    @Override
    public OrderPriceAdjustmentDO getLatestOrderPriceAdjustmentByOrderId(Long orderId) {
        return orderPriceAdjustmentMapper.selectLatestByOrderId(orderId);
    }

    @Override
    public OrderPriceAdjustmentDO getLatestConfirmedOrderPriceAdjustmentByOrderId(Long orderId) {
        return orderPriceAdjustmentMapper.selectLatestConfirmedByOrderId(orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmOrderPriceAdjustment(Long id, String confirmedBy) {
        OrderPriceAdjustmentDO adjustment = validateOrderPriceAdjustmentExists(id);
        adjustment.setStatus(2); // 已确认
        adjustment.setConfirmedBy(confirmedBy);
        adjustment.setConfirmedTime(LocalDateTime.now());
        orderPriceAdjustmentMapper.updateById(adjustment);
        
        log.info("[confirmOrderPriceAdjustment][确认价格调整] id={}, confirmedBy={}", id, confirmedBy);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectOrderPriceAdjustment(Long id, String confirmedBy, String rejectReason) {
        OrderPriceAdjustmentDO adjustment = validateOrderPriceAdjustmentExists(id);
        adjustment.setStatus(3); // 已拒绝
        adjustment.setConfirmedBy(confirmedBy);
        adjustment.setConfirmedTime(LocalDateTime.now());
        adjustment.setRejectReason(rejectReason);
        orderPriceAdjustmentMapper.updateById(adjustment);
        
        log.info("[rejectOrderPriceAdjustment][拒绝价格调整] id={}, confirmedBy={}, rejectReason={}", 
                id, confirmedBy, rejectReason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPriceAdjustment(Long orderId, Integer adjustmentType, BigDecimal originalPrice,
                                     BigDecimal adjustedPrice, BigDecimal originalQuantity, BigDecimal adjustedQuantity,
                                     String adjustmentReason, Long relatedWeighingId) {
        OrderPriceAdjustmentDO adjustment = new OrderPriceAdjustmentDO();
        adjustment.setOrderId(orderId);
        adjustment.setAdjustmentType(adjustmentType);
        adjustment.setOriginalPrice(originalPrice);
        adjustment.setAdjustedPrice(adjustedPrice);
        adjustment.setOriginalQuantity(originalQuantity);
        adjustment.setAdjustedQuantity(adjustedQuantity);
        adjustment.setAdjustmentReason(adjustmentReason);
        adjustment.setRelatedWeighingId(relatedWeighingId);
        adjustment.setStatus(1); // 待确认
        
        // 计算调整金额
        if (adjustedPrice != null && originalPrice != null) {
            adjustment.setAdjustmentAmount(adjustedPrice.subtract(originalPrice));
        }
        
        orderPriceAdjustmentMapper.insert(adjustment);
        log.info("[createPriceAdjustment][创建价格调整记录] orderId={}, adjustmentType={}, adjustmentAmount={}", 
                orderId, adjustmentType, adjustment.getAdjustmentAmount());
        return adjustment.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchConfirmOrderPriceAdjustments(List<Long> ids, String confirmedBy) {
        for (Long id : ids) {
            confirmOrderPriceAdjustment(id, confirmedBy);
        }
        log.info("[batchConfirmOrderPriceAdjustments][批量确认价格调整] ids={}, confirmedBy={}", ids, confirmedBy);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchRejectOrderPriceAdjustments(List<Long> ids, String confirmedBy, String rejectReason) {
        for (Long id : ids) {
            rejectOrderPriceAdjustment(id, confirmedBy, rejectReason);
        }
        log.info("[batchRejectOrderPriceAdjustments][批量拒绝价格调整] ids={}, confirmedBy={}, rejectReason={}", 
                ids, confirmedBy, rejectReason);
    }

    @Override
    public OrderPriceAdjustmentDO validateOrderPriceAdjustmentExists(Long id) {
        OrderPriceAdjustmentDO orderPriceAdjustment = orderPriceAdjustmentMapper.selectById(id);
        if (orderPriceAdjustment == null) {
            throw exception(ORDER_PRICE_ADJUSTMENT_NOT_EXISTS);
        }
        return orderPriceAdjustment;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyPriceAdjustment(Long orderId, Integer adjustmentType, BigDecimal adjustmentAmount, String adjustmentReason) {
        OrderPriceAdjustmentDO adjustment = new OrderPriceAdjustmentDO();
        adjustment.setOrderId(orderId);
        adjustment.setAdjustmentType(adjustmentType);
        adjustment.setAdjustmentAmount(adjustmentAmount);
        adjustment.setAdjustmentReason(adjustmentReason);
        adjustment.setStatus(1); // 待审批
        
        orderPriceAdjustmentMapper.insert(adjustment);
        log.info("[applyPriceAdjustment][申请价格调整] orderId={}, adjustmentType={}, adjustmentAmount={}", 
                orderId, adjustmentType, adjustmentAmount);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveAdjustment(Long id, Boolean approved, String approvalReason) {
        OrderPriceAdjustmentDO adjustment = validateOrderPriceAdjustmentExists(id);
        adjustment.setStatus(approved ? 2 : 3); // 2-已批准，3-已拒绝
        adjustment.setConfirmedTime(LocalDateTime.now());
        adjustment.setRemark(approvalReason);
        
        orderPriceAdjustmentMapper.updateById(adjustment);
        log.info("[approveAdjustment][审批价格调整] id={}, approved={}, reason={}", id, approved, approvalReason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void executeAdjustment(Long id) {
        OrderPriceAdjustmentDO adjustment = validateOrderPriceAdjustmentExists(id);
        if (adjustment.getStatus() != 2) { // 必须是已批准状态
            throw exception(ORDER_PRICE_ADJUSTMENT_NOT_EXISTS);
        }
        
        adjustment.setStatus(4); // 已执行
        orderPriceAdjustmentMapper.updateById(adjustment);
        
        log.info("[executeAdjustment][执行价格调整] id={}", id);
    }

    @Override
    public List<OrderPriceAdjustmentDO> getAdjustmentsByOrderId(Long orderId) {
        return orderPriceAdjustmentMapper.selectListByOrderId(orderId);
    }

    @Override
    public List<OrderPriceAdjustmentDO> getPendingApprovalAdjustments() {
        return orderPriceAdjustmentMapper.selectPendingAdjustments();
    }

} 