package cn.iocoder.yudao.module.waste.dal.mysql.order;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.waste.controller.admin.order.vo.OrderAllocationRecordPageReqVO;
import cn.iocoder.yudao.module.waste.dal.dataobject.order.OrderAllocationRecordDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单过磅分摊记录 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface OrderAllocationRecordMapper extends BaseMapperX<OrderAllocationRecordDO> {

    default PageResult<OrderAllocationRecordDO> selectPage(OrderAllocationRecordPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<OrderAllocationRecordDO>()
                .eqIfPresent(OrderAllocationRecordDO::getOrderId, reqVO.getOrderId())
                .eqIfPresent(OrderAllocationRecordDO::getVehicleWeighingId, reqVO.getVehicleWeighingId())
                .likeIfPresent(OrderAllocationRecordDO::getWeighingBatchNo, reqVO.getWeighingBatchNo())
                .eqIfPresent(OrderAllocationRecordDO::getAllocationMethod, reqVO.getAllocationMethod())
                .eqIfPresent(OrderAllocationRecordDO::getIsManualAdjustment, reqVO.getIsManualAdjustment())
                .likeIfPresent(OrderAllocationRecordDO::getAllocationOperator, reqVO.getAllocationOperator())
                .betweenIfPresent(OrderAllocationRecordDO::getAllocationTime, reqVO.getAllocationTime())
                .betweenIfPresent(OrderAllocationRecordDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(OrderAllocationRecordDO::getAllocationTime));
    }

    default List<OrderAllocationRecordDO> selectList(OrderAllocationRecordPageReqVO reqVO) {
        return selectList(new LambdaQueryWrapperX<OrderAllocationRecordDO>()
                .eqIfPresent(OrderAllocationRecordDO::getOrderId, reqVO.getOrderId())
                .eqIfPresent(OrderAllocationRecordDO::getVehicleWeighingId, reqVO.getVehicleWeighingId())
                .likeIfPresent(OrderAllocationRecordDO::getWeighingBatchNo, reqVO.getWeighingBatchNo())
                .eqIfPresent(OrderAllocationRecordDO::getAllocationMethod, reqVO.getAllocationMethod())
                .eqIfPresent(OrderAllocationRecordDO::getIsManualAdjustment, reqVO.getIsManualAdjustment())
                .likeIfPresent(OrderAllocationRecordDO::getAllocationOperator, reqVO.getAllocationOperator())
                .betweenIfPresent(OrderAllocationRecordDO::getAllocationTime, reqVO.getAllocationTime())
                .betweenIfPresent(OrderAllocationRecordDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(OrderAllocationRecordDO::getAllocationTime));
    }

    default List<OrderAllocationRecordDO> selectByOrderId(Long orderId) {
        return selectList(OrderAllocationRecordDO::getOrderId, orderId);
    }

    default List<OrderAllocationRecordDO> selectByVehicleWeighingId(Long vehicleWeighingId) {
        return selectList(OrderAllocationRecordDO::getVehicleWeighingId, vehicleWeighingId);
    }

    default OrderAllocationRecordDO selectByOrderIdAndVehicleWeighingId(Long orderId, Long vehicleWeighingId) {
        return selectOne(new LambdaQueryWrapperX<OrderAllocationRecordDO>()
                .eq(OrderAllocationRecordDO::getOrderId, orderId)
                .eq(OrderAllocationRecordDO::getVehicleWeighingId, vehicleWeighingId));
    }

    default List<OrderAllocationRecordDO> selectByWeighingBatchNo(String weighingBatchNo) {
        return selectList(OrderAllocationRecordDO::getWeighingBatchNo, weighingBatchNo);
    }

    default List<OrderAllocationRecordDO> selectByAllocationMethod(Integer allocationMethod) {
        return selectList(OrderAllocationRecordDO::getAllocationMethod, allocationMethod);
    }

    default List<OrderAllocationRecordDO> selectManualAdjustmentRecords() {
        return selectList(new LambdaQueryWrapperX<OrderAllocationRecordDO>()
                .eq(OrderAllocationRecordDO::getIsManualAdjustment, true)
                .orderByDesc(OrderAllocationRecordDO::getAllocationTime));
    }

    default List<OrderAllocationRecordDO> selectByOperator(String operator) {
        return selectList(OrderAllocationRecordDO::getAllocationOperator, operator);
    }

    default List<OrderAllocationRecordDO> selectByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        return selectList(new LambdaQueryWrapperX<OrderAllocationRecordDO>()
                .between(OrderAllocationRecordDO::getAllocationTime, startTime, endTime)
                .orderByDesc(OrderAllocationRecordDO::getAllocationTime));
    }

    default OrderAllocationRecordDO selectLatestByOrderId(Long orderId) {
        return selectOne(new LambdaQueryWrapperX<OrderAllocationRecordDO>()
                .eq(OrderAllocationRecordDO::getOrderId, orderId)
                .orderByDesc(OrderAllocationRecordDO::getAllocationTime)
                .last("LIMIT 1"));
    }

    // ==================== Service实现类调用的缺失方法 ====================

    default List<OrderAllocationRecordDO> selectListByOrderId(Long orderId) {
        return selectList(OrderAllocationRecordDO::getOrderId, orderId);
    }

    default List<OrderAllocationRecordDO> selectListByVehicleWeighingId(Long vehicleWeighingId) {
        return selectList(OrderAllocationRecordDO::getVehicleWeighingId, vehicleWeighingId);
    }

    default List<OrderAllocationRecordDO> selectListByAllocationMethod(Integer allocationMethod) {
        return selectList(OrderAllocationRecordDO::getAllocationMethod, allocationMethod);
    }

    default List<OrderAllocationRecordDO> selectSystemAllocationRecords() {
        return selectList(new LambdaQueryWrapperX<OrderAllocationRecordDO>()
                .eq(OrderAllocationRecordDO::getAllocationMethod, 1) // 系统自动分摊
                .orderByDesc(OrderAllocationRecordDO::getAllocationTime));
    }

    default List<OrderAllocationRecordDO> selectListByAllocationTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        return selectList(new LambdaQueryWrapperX<OrderAllocationRecordDO>()
                .between(OrderAllocationRecordDO::getAllocationTime, startTime, endTime)
                .orderByDesc(OrderAllocationRecordDO::getAllocationTime));
    }

} 