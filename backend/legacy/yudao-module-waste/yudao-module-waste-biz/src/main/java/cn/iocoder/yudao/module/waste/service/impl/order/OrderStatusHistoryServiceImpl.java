package cn.iocoder.yudao.module.waste.service.impl.order;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.waste.controller.admin.order.vo.OrderStatusHistoryCreateReqVO;
import cn.iocoder.yudao.module.waste.controller.admin.order.vo.OrderStatusHistoryPageReqVO;
import cn.iocoder.yudao.module.waste.controller.admin.order.vo.OrderStatusHistoryRespVO;
import cn.iocoder.yudao.module.waste.dal.dataobject.order.OrderStatusHistoryDO;
import cn.iocoder.yudao.module.waste.dal.mysql.order.OrderStatusHistoryMapper;
import cn.iocoder.yudao.module.waste.service.order.OrderStatusHistoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.waste.enums.ErrorCodeConstants.ORDER_STATUS_HISTORY_NOT_EXISTS;

/**
 * 订单状态变更历史 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
@Slf4j
public class OrderStatusHistoryServiceImpl implements OrderStatusHistoryService {

    @Resource
    private OrderStatusHistoryMapper orderStatusHistoryMapper;

    @Override
    public Long createOrderStatusHistory(@Valid OrderStatusHistoryCreateReqVO createReqVO) {
        // 插入
        OrderStatusHistoryDO orderStatusHistory = BeanUtils.toBean(createReqVO, OrderStatusHistoryDO.class);
        orderStatusHistoryMapper.insert(orderStatusHistory);
        // 返回
        return orderStatusHistory.getId();
    }

    @Override
    public void deleteOrderStatusHistory(Long id) {
        // 校验存在
        validateOrderStatusHistoryExists(id);
        // 删除
        orderStatusHistoryMapper.deleteById(id);
    }

    @Override
    public OrderStatusHistoryDO getOrderStatusHistory(Long id) {
        return orderStatusHistoryMapper.selectById(id);
    }

    @Override
    public OrderStatusHistoryRespVO getOrderStatusHistoryDetail(Long id) {
        OrderStatusHistoryDO orderStatusHistory = validateOrderStatusHistoryExists(id);
        return BeanUtils.toBean(orderStatusHistory, OrderStatusHistoryRespVO.class);
    }

    @Override
    public PageResult<OrderStatusHistoryDO> getOrderStatusHistoryPage(OrderStatusHistoryPageReqVO pageReqVO) {
        PageResult<OrderStatusHistoryDO> pageResult = orderStatusHistoryMapper.selectPage(pageReqVO);
        return pageResult;
    }

    @Override
    public List<OrderStatusHistoryDO> getOrderStatusHistoryList(OrderStatusHistoryPageReqVO exportReqVO) {
        return orderStatusHistoryMapper.selectList(exportReqVO);
    }

    @Override
    public List<OrderStatusHistoryDO> getOrderStatusHistoryListByOrderId(Long orderId) {
        return orderStatusHistoryMapper.selectListByOrderId(orderId);
    }

    @Override
    public List<OrderStatusHistoryDO> getOrderStatusHistoryListByStatus(Integer status) {
        return orderStatusHistoryMapper.selectListByStatus(status);
    }

    @Override
    public List<OrderStatusHistoryDO> getOrderStatusHistoryListByOperator(String operator) {
        return orderStatusHistoryMapper.selectListByOperator(operator);
    }

    @Override
    public List<OrderStatusHistoryDO> getOrderStatusHistoryListByOperationTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        return orderStatusHistoryMapper.selectListByOperationTimeRange(startTime, endTime);
    }

    @Override
    public List<OrderStatusHistoryDO> getMilestoneOrderStatusHistories() {
        return orderStatusHistoryMapper.selectMilestoneHistories();
    }

    @Override
    public OrderStatusHistoryDO getLatestOrderStatusHistoryByOrderId(Long orderId) {
        return orderStatusHistoryMapper.selectLatestByOrderId(orderId);
    }

    @Override
    public OrderStatusHistoryDO getFirstOrderStatusHistoryByOrderId(Long orderId) {
        return orderStatusHistoryMapper.selectFirstByOrderId(orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long recordOrderStatusChange(Long orderId, Integer status, String statusName, String operator,
                                       LocalDateTime operationTime, String remark, Boolean isMilestone, String businessData) {
        OrderStatusHistoryDO history = new OrderStatusHistoryDO();
        history.setOrderId(orderId);
        history.setStatusTo(status);
        history.setStatusName(statusName);
        history.setOperatorName(operator);
        history.setChangeTime(operationTime != null ? operationTime : LocalDateTime.now());
        history.setRemark(remark);
        history.setMilestoneFlag(isMilestone != null ? isMilestone : false);
        history.setBusinessData(businessData);
        
        orderStatusHistoryMapper.insert(history);
        log.info("[recordOrderStatusChange][记录订单状态变更] orderId={}, status={}, statusName={}, operator={}", 
                orderId, status, statusName, operator);
        return history.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long recordMilestoneStatusChange(Long orderId, Integer status, String statusName, String operator, String remark) {
        return recordOrderStatusChange(orderId, status, statusName, operator, LocalDateTime.now(), remark, true, null);
    }

    @Override
    public OrderStatusHistoryDO validateOrderStatusHistoryExists(Long id) {
        OrderStatusHistoryDO orderStatusHistory = orderStatusHistoryMapper.selectById(id);
        if (orderStatusHistory == null) {
            throw exception(ORDER_STATUS_HISTORY_NOT_EXISTS);
        }
        return orderStatusHistory;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordStatusChange(Long orderId, Integer fromStatus, Integer toStatus, String changeReason) {
        OrderStatusHistoryDO history = new OrderStatusHistoryDO();
        history.setOrderId(orderId);
        history.setStatusFrom(fromStatus);
        history.setStatusTo(toStatus);
        history.setStatusName("状态变更：" + fromStatus + " -> " + toStatus);
        history.setOperatorName("系统");
        history.setChangeTime(LocalDateTime.now());
        history.setRemark(changeReason);
        history.setMilestoneFlag(false);
        
        orderStatusHistoryMapper.insert(history);
        log.info("[recordStatusChange][记录状态变更] orderId={}, fromStatus={}, toStatus={}, reason={}", 
                orderId, fromStatus, toStatus, changeReason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordMilestone(Long orderId, Integer status, String milestone) {
        OrderStatusHistoryDO history = new OrderStatusHistoryDO();
        history.setOrderId(orderId);
        history.setStatusTo(status);
        history.setStatusName(milestone);
        history.setOperatorName("系统");
        history.setChangeTime(LocalDateTime.now());
        history.setRemark("里程碑状态");
        history.setMilestoneFlag(true);
        
        orderStatusHistoryMapper.insert(history);
        log.info("[recordMilestone][记录里程碑状态] orderId={}, status={}, milestone={}", orderId, status, milestone);
    }

    @Override
    public List<OrderStatusHistoryDO> getHistoryByOrderId(Long orderId) {
        return orderStatusHistoryMapper.selectListByOrderId(orderId);
    }

    @Override
    public List<OrderStatusHistoryDO> getMilestonesByOrderId(Long orderId) {
        return orderStatusHistoryMapper.selectMilestonesByOrderId(orderId);
    }

} 