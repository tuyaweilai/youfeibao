package cn.iocoder.yudao.module.waste.dal.mysql.order;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.waste.controller.admin.order.vo.OrderPriceAdjustmentPageReqVO;
import cn.iocoder.yudao.module.waste.dal.dataobject.order.OrderPriceAdjustmentDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单价格调整记录 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface OrderPriceAdjustmentMapper extends BaseMapperX<OrderPriceAdjustmentDO> {

    default PageResult<OrderPriceAdjustmentDO> selectPage(OrderPriceAdjustmentPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<OrderPriceAdjustmentDO>()
                .eqIfPresent(OrderPriceAdjustmentDO::getOrderId, reqVO.getOrderId())
                .eqIfPresent(OrderPriceAdjustmentDO::getAdjustmentType, reqVO.getAdjustmentType())
                .eqIfPresent(OrderPriceAdjustmentDO::getStatus, reqVO.getStatus())
                .likeIfPresent(OrderPriceAdjustmentDO::getConfirmedBy, reqVO.getConfirmedBy())
                .eqIfPresent(OrderPriceAdjustmentDO::getRelatedWeighingId, reqVO.getRelatedWeighingId())
                .likeIfPresent(OrderPriceAdjustmentDO::getAdjustmentReason, reqVO.getAdjustmentReason())
                .geIfPresent(OrderPriceAdjustmentDO::getAdjustmentAmount, reqVO.getMinAdjustmentAmount())
                .leIfPresent(OrderPriceAdjustmentDO::getAdjustmentAmount, reqVO.getMaxAdjustmentAmount())
                .betweenIfPresent(OrderPriceAdjustmentDO::getConfirmedTime, reqVO.getConfirmedTime())
                .betweenIfPresent(OrderPriceAdjustmentDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(OrderPriceAdjustmentDO::getCreateTime));
    }

    default List<OrderPriceAdjustmentDO> selectList(OrderPriceAdjustmentPageReqVO reqVO) {
        return selectList(new LambdaQueryWrapperX<OrderPriceAdjustmentDO>()
                .eqIfPresent(OrderPriceAdjustmentDO::getOrderId, reqVO.getOrderId())
                .eqIfPresent(OrderPriceAdjustmentDO::getAdjustmentType, reqVO.getAdjustmentType())
                .eqIfPresent(OrderPriceAdjustmentDO::getStatus, reqVO.getStatus())
                .likeIfPresent(OrderPriceAdjustmentDO::getConfirmedBy, reqVO.getConfirmedBy())
                .eqIfPresent(OrderPriceAdjustmentDO::getRelatedWeighingId, reqVO.getRelatedWeighingId())
                .likeIfPresent(OrderPriceAdjustmentDO::getAdjustmentReason, reqVO.getAdjustmentReason())
                .geIfPresent(OrderPriceAdjustmentDO::getAdjustmentAmount, reqVO.getMinAdjustmentAmount())
                .leIfPresent(OrderPriceAdjustmentDO::getAdjustmentAmount, reqVO.getMaxAdjustmentAmount())
                .betweenIfPresent(OrderPriceAdjustmentDO::getConfirmedTime, reqVO.getConfirmedTime())
                .betweenIfPresent(OrderPriceAdjustmentDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(OrderPriceAdjustmentDO::getCreateTime));
    }

    default List<OrderPriceAdjustmentDO> selectByOrderId(Long orderId) {
        return selectList(OrderPriceAdjustmentDO::getOrderId, orderId);
    }

    default List<OrderPriceAdjustmentDO> selectByAdjustmentType(Integer adjustmentType) {
        return selectList(OrderPriceAdjustmentDO::getAdjustmentType, adjustmentType);
    }

    default List<OrderPriceAdjustmentDO> selectByStatus(Integer status) {
        return selectList(OrderPriceAdjustmentDO::getStatus, status);
    }

    default List<OrderPriceAdjustmentDO> selectPendingAdjustments() {
        return selectList(new LambdaQueryWrapperX<OrderPriceAdjustmentDO>()
                .eq(OrderPriceAdjustmentDO::getStatus, 0) // 待确认
                .orderByDesc(OrderPriceAdjustmentDO::getCreateTime));
    }

    default List<OrderPriceAdjustmentDO> selectConfirmedAdjustments() {
        return selectList(new LambdaQueryWrapperX<OrderPriceAdjustmentDO>()
                .eq(OrderPriceAdjustmentDO::getStatus, 1) // 已确认
                .orderByDesc(OrderPriceAdjustmentDO::getConfirmedTime));
    }

    default List<OrderPriceAdjustmentDO> selectRejectedAdjustments() {
        return selectList(new LambdaQueryWrapperX<OrderPriceAdjustmentDO>()
                .eq(OrderPriceAdjustmentDO::getStatus, 2) // 已拒绝
                .orderByDesc(OrderPriceAdjustmentDO::getConfirmedTime));
    }

    default List<OrderPriceAdjustmentDO> selectByRelatedWeighingId(Long relatedWeighingId) {
        return selectList(OrderPriceAdjustmentDO::getRelatedWeighingId, relatedWeighingId);
    }

    default List<OrderPriceAdjustmentDO> selectByConfirmedBy(String confirmedBy) {
        return selectList(OrderPriceAdjustmentDO::getConfirmedBy, confirmedBy);
    }

    default List<OrderPriceAdjustmentDO> selectByConfirmedTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        return selectList(new LambdaQueryWrapperX<OrderPriceAdjustmentDO>()
                .between(OrderPriceAdjustmentDO::getConfirmedTime, startTime, endTime)
                .orderByDesc(OrderPriceAdjustmentDO::getConfirmedTime));
    }

    default List<OrderPriceAdjustmentDO> selectPriceAdjustments() {
        return selectList(new LambdaQueryWrapperX<OrderPriceAdjustmentDO>()
                .in(OrderPriceAdjustmentDO::getAdjustmentType, 1, 3) // 价格调整或价格和数量调整
                .orderByDesc(OrderPriceAdjustmentDO::getCreateTime));
    }

    default List<OrderPriceAdjustmentDO> selectQuantityAdjustments() {
        return selectList(new LambdaQueryWrapperX<OrderPriceAdjustmentDO>()
                .in(OrderPriceAdjustmentDO::getAdjustmentType, 2, 3) // 数量调整或价格和数量调整
                .orderByDesc(OrderPriceAdjustmentDO::getCreateTime));
    }

    default List<OrderPriceAdjustmentDO> selectPositiveAdjustments() {
        return selectList(new LambdaQueryWrapperX<OrderPriceAdjustmentDO>()
                .gt(OrderPriceAdjustmentDO::getAdjustmentAmount, 0) // 正调整（增加金额）
                .orderByDesc(OrderPriceAdjustmentDO::getCreateTime));
    }

    default List<OrderPriceAdjustmentDO> selectNegativeAdjustments() {
        return selectList(new LambdaQueryWrapperX<OrderPriceAdjustmentDO>()
                .lt(OrderPriceAdjustmentDO::getAdjustmentAmount, 0) // 负调整（减少金额）
                .orderByDesc(OrderPriceAdjustmentDO::getCreateTime));
    }

    default OrderPriceAdjustmentDO selectLatestByOrderId(Long orderId) {
        return selectOne(new LambdaQueryWrapperX<OrderPriceAdjustmentDO>()
                .eq(OrderPriceAdjustmentDO::getOrderId, orderId)
                .orderByDesc(OrderPriceAdjustmentDO::getCreateTime)
                .last("LIMIT 1"));
    }

    default OrderPriceAdjustmentDO selectLatestConfirmedByOrderId(Long orderId) {
        return selectOne(new LambdaQueryWrapperX<OrderPriceAdjustmentDO>()
                .eq(OrderPriceAdjustmentDO::getOrderId, orderId)
                .eq(OrderPriceAdjustmentDO::getStatus, 1) // 已确认
                .orderByDesc(OrderPriceAdjustmentDO::getConfirmedTime)
                .last("LIMIT 1"));
    }

    // ==================== Service实现类调用的缺失方法 ====================

    default List<OrderPriceAdjustmentDO> selectListByOrderId(Long orderId) {
        return selectList(OrderPriceAdjustmentDO::getOrderId, orderId);
    }

    default List<OrderPriceAdjustmentDO> selectListByAdjustmentType(Integer adjustmentType) {
        return selectList(OrderPriceAdjustmentDO::getAdjustmentType, adjustmentType);
    }

    default List<OrderPriceAdjustmentDO> selectListByStatus(Integer status) {
        return selectList(OrderPriceAdjustmentDO::getStatus, status);
    }

    default List<OrderPriceAdjustmentDO> selectListByRelatedWeighingId(Long relatedWeighingId) {
        return selectList(OrderPriceAdjustmentDO::getRelatedWeighingId, relatedWeighingId);
    }

    default List<OrderPriceAdjustmentDO> selectListByConfirmedBy(String confirmedBy) {
        return selectList(OrderPriceAdjustmentDO::getConfirmedBy, confirmedBy);
    }

    default List<OrderPriceAdjustmentDO> selectListByConfirmedTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        return selectList(new LambdaQueryWrapperX<OrderPriceAdjustmentDO>()
                .between(OrderPriceAdjustmentDO::getConfirmedTime, startTime, endTime)
                .orderByDesc(OrderPriceAdjustmentDO::getConfirmedTime));
    }

} 