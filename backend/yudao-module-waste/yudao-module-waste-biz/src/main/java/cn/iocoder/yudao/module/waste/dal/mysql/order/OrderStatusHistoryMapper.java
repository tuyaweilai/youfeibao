package cn.iocoder.yudao.module.waste.dal.mysql.order;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.waste.controller.admin.order.vo.OrderStatusHistoryPageReqVO;
import cn.iocoder.yudao.module.waste.dal.dataobject.order.OrderStatusHistoryDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单状态变更历史 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface OrderStatusHistoryMapper extends BaseMapperX<OrderStatusHistoryDO> {

    default PageResult<OrderStatusHistoryDO> selectPage(OrderStatusHistoryPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<OrderStatusHistoryDO>()
                .eqIfPresent(OrderStatusHistoryDO::getOrderId, reqVO.getOrderId())
                .eqIfPresent(OrderStatusHistoryDO::getStatusTo, reqVO.getStatus())
                .likeIfPresent(OrderStatusHistoryDO::getOperatorName, reqVO.getOperator())
                .eqIfPresent(OrderStatusHistoryDO::getMilestoneFlag, reqVO.getIsMilestone())
                .betweenIfPresent(OrderStatusHistoryDO::getChangeTime, reqVO.getOperationTime())
                .betweenIfPresent(OrderStatusHistoryDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(OrderStatusHistoryDO::getChangeTime));
    }

    default List<OrderStatusHistoryDO> selectList(OrderStatusHistoryPageReqVO reqVO) {
        return selectList(new LambdaQueryWrapperX<OrderStatusHistoryDO>()
                .eqIfPresent(OrderStatusHistoryDO::getOrderId, reqVO.getOrderId())
                .eqIfPresent(OrderStatusHistoryDO::getStatusTo, reqVO.getStatus())
                .likeIfPresent(OrderStatusHistoryDO::getOperatorName, reqVO.getOperator())
                .eqIfPresent(OrderStatusHistoryDO::getMilestoneFlag, reqVO.getIsMilestone())
                .betweenIfPresent(OrderStatusHistoryDO::getChangeTime, reqVO.getOperationTime())
                .betweenIfPresent(OrderStatusHistoryDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(OrderStatusHistoryDO::getChangeTime));
    }

    default List<OrderStatusHistoryDO> selectByOrderId(Long orderId) {
        return selectList(OrderStatusHistoryDO::getOrderId, orderId);
    }

    default List<OrderStatusHistoryDO> selectByOrderIdOrderByTime(Long orderId) {
        return selectList(new LambdaQueryWrapperX<OrderStatusHistoryDO>()
                .eq(OrderStatusHistoryDO::getOrderId, orderId)
                .orderByAsc(OrderStatusHistoryDO::getChangeTime));
    }

    default List<OrderStatusHistoryDO> selectMilestonesByOrderId(Long orderId) {
        return selectList(new LambdaQueryWrapperX<OrderStatusHistoryDO>()
                .eq(OrderStatusHistoryDO::getOrderId, orderId)
                .eq(OrderStatusHistoryDO::getMilestoneFlag, true)
                .orderByAsc(OrderStatusHistoryDO::getChangeTime));
    }

    default List<OrderStatusHistoryDO> selectByOperatorId(Long operatorId) {
        return selectList(OrderStatusHistoryDO::getOperatorId, operatorId);
    }

    default List<OrderStatusHistoryDO> selectByOperatorType(Integer operatorType) {
        return selectList(OrderStatusHistoryDO::getOperatorType, operatorType);
    }

    default List<OrderStatusHistoryDO> selectByStatusTo(Integer statusTo) {
        return selectList(OrderStatusHistoryDO::getStatusTo, statusTo);
    }

    default List<OrderStatusHistoryDO> selectByChangeType(Integer changeType) {
        return selectList(OrderStatusHistoryDO::getChangeType, changeType);
    }

    default List<OrderStatusHistoryDO> selectByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        return selectList(new LambdaQueryWrapperX<OrderStatusHistoryDO>()
                .between(OrderStatusHistoryDO::getChangeTime, startTime, endTime)
                .orderByDesc(OrderStatusHistoryDO::getChangeTime));
    }

    default OrderStatusHistoryDO selectLatestByOrderId(Long orderId) {
        return selectOne(new LambdaQueryWrapperX<OrderStatusHistoryDO>()
                .eq(OrderStatusHistoryDO::getOrderId, orderId)
                .orderByDesc(OrderStatusHistoryDO::getChangeTime)
                .last("LIMIT 1"));
    }

    default List<OrderStatusHistoryDO> selectListByOrderId(Long orderId) {
        return selectList(OrderStatusHistoryDO::getOrderId, orderId);
    }

    default List<OrderStatusHistoryDO> selectListByStatus(Integer status) {
        return selectList(OrderStatusHistoryDO::getStatusTo, status);
    }

    default List<OrderStatusHistoryDO> selectListByOperator(String operator) {
        return selectList(new LambdaQueryWrapperX<OrderStatusHistoryDO>()
                .like(OrderStatusHistoryDO::getOperatorName, operator));
    }

    default List<OrderStatusHistoryDO> selectListByOperationTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        return selectList(new LambdaQueryWrapperX<OrderStatusHistoryDO>()
                .between(OrderStatusHistoryDO::getChangeTime, startTime, endTime)
                .orderByDesc(OrderStatusHistoryDO::getChangeTime));
    }

    default List<OrderStatusHistoryDO> selectMilestoneHistories() {
        return selectList(new LambdaQueryWrapperX<OrderStatusHistoryDO>()
                .eq(OrderStatusHistoryDO::getMilestoneFlag, true)
                .orderByDesc(OrderStatusHistoryDO::getChangeTime));
    }

    default OrderStatusHistoryDO selectFirstByOrderId(Long orderId) {
        return selectOne(new LambdaQueryWrapperX<OrderStatusHistoryDO>()
                .eq(OrderStatusHistoryDO::getOrderId, orderId)
                .orderByAsc(OrderStatusHistoryDO::getChangeTime)
                .last("LIMIT 1"));
    }

} 