package cn.iocoder.yudao.module.waste.dal.mysql.order;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.waste.controller.admin.order.vo.TransferOrderPageReqVO;
import cn.iocoder.yudao.module.waste.dal.dataobject.order.TransferOrderDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 危废转移订单 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface TransferOrderMapper extends BaseMapperX<TransferOrderDO> {

    default PageResult<TransferOrderDO> selectPage(TransferOrderPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<TransferOrderDO>()
                .eqIfPresent(TransferOrderDO::getOrderNo, reqVO.getOrderNo())
                .eqIfPresent(TransferOrderDO::getAppointmentId, reqVO.getAppointmentId())
                .eqIfPresent(TransferOrderDO::getProducingEnterpriseId, reqVO.getProducingEnterpriseId())
                .eqIfPresent(TransferOrderDO::getRecyclingEnterpriseId, reqVO.getRecyclingEnterpriseId())
                .eqIfPresent(TransferOrderDO::getWasteCode, reqVO.getWasteCode())
                .likeIfPresent(TransferOrderDO::getWasteName, reqVO.getWasteName())
                .eqIfPresent(TransferOrderDO::getBusinessStatus, reqVO.getBusinessStatus())
                .eqIfPresent(TransferOrderDO::getPaymentStatus, reqVO.getPaymentStatus())
                .eqIfPresent(TransferOrderDO::getSourceType, reqVO.getSourceType())
                .betweenIfPresent(TransferOrderDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(TransferOrderDO::getId));
    }

    default List<TransferOrderDO> selectList(TransferOrderPageReqVO reqVO) {
        return selectList(new LambdaQueryWrapperX<TransferOrderDO>()
                .eqIfPresent(TransferOrderDO::getOrderNo, reqVO.getOrderNo())
                .eqIfPresent(TransferOrderDO::getAppointmentId, reqVO.getAppointmentId())
                .eqIfPresent(TransferOrderDO::getProducingEnterpriseId, reqVO.getProducingEnterpriseId())
                .eqIfPresent(TransferOrderDO::getRecyclingEnterpriseId, reqVO.getRecyclingEnterpriseId())
                .eqIfPresent(TransferOrderDO::getWasteCode, reqVO.getWasteCode())
                .likeIfPresent(TransferOrderDO::getWasteName, reqVO.getWasteName())
                .eqIfPresent(TransferOrderDO::getBusinessStatus, reqVO.getBusinessStatus())
                .eqIfPresent(TransferOrderDO::getPaymentStatus, reqVO.getPaymentStatus())
                .eqIfPresent(TransferOrderDO::getSourceType, reqVO.getSourceType())
                .betweenIfPresent(TransferOrderDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(TransferOrderDO::getId));
    }

    default TransferOrderDO selectByOrderNo(String orderNo) {
        return selectOne(TransferOrderDO::getOrderNo, orderNo);
    }

    default List<TransferOrderDO> selectByAppointmentId(Long appointmentId) {
        return selectList(TransferOrderDO::getAppointmentId, appointmentId);
    }

    default List<TransferOrderDO> selectByProducingEnterpriseId(Long producingEnterpriseId) {
        return selectList(TransferOrderDO::getProducingEnterpriseId, producingEnterpriseId);
    }

    default List<TransferOrderDO> selectByRecyclingEnterpriseId(Long recyclingEnterpriseId) {
        return selectList(TransferOrderDO::getRecyclingEnterpriseId, recyclingEnterpriseId);
    }

    default List<TransferOrderDO> selectByBusinessStatus(Integer businessStatus) {
        return selectList(TransferOrderDO::getBusinessStatus, businessStatus);
    }

    default List<TransferOrderDO> selectByPaymentStatus(Integer paymentStatus) {
        return selectList(TransferOrderDO::getPaymentStatus, paymentStatus);
    }

    default List<TransferOrderDO> selectByTransportTaskId(Long transportTaskId) {
        return selectList(TransferOrderDO::getTransportTaskId, transportTaskId);
    }

    default List<TransferOrderDO> selectByVehicleWeighingId(Long vehicleWeighingId) {
        return selectList(TransferOrderDO::getVehicleWeighingId, vehicleWeighingId);
    }

    default List<TransferOrderDO> selectUnallocatedOrders() {
        return selectList(new LambdaQueryWrapperX<TransferOrderDO>()
                .isNull(TransferOrderDO::getAllocationCompletedTime)
                .isNotNull(TransferOrderDO::getVehicleWeighingId)
                .eq(TransferOrderDO::getBusinessStatus, 1) // 已确认状态
                .orderByAsc(TransferOrderDO::getCreateTime));
    }

    default List<TransferOrderDO> selectPendingPaymentOrders() {
        return selectList(new LambdaQueryWrapperX<TransferOrderDO>()
                .in(TransferOrderDO::getPaymentStatus, 0, 2, 3) // 未付款、付款失败、待凭证上传
                .eq(TransferOrderDO::getBusinessStatus, 2) // 待结算状态
                .orderByAsc(TransferOrderDO::getCreateTime));
    }

    default List<TransferOrderDO> selectOrdersCreatedBetween(LocalDateTime startTime, LocalDateTime endTime) {
        return selectList(new LambdaQueryWrapperX<TransferOrderDO>()
                .between(TransferOrderDO::getCreateTime, startTime, endTime)
                .orderByDesc(TransferOrderDO::getCreateTime));
    }

    // ==================== Service实现类中调用的方法 ====================

    /**
     * 获取待分摊的订单列表
     */
    default List<TransferOrderDO> selectPendingAllocationOrders() {
        return selectUnallocatedOrders();
    }

    /**
     * 根据产废企业ID查询订单列表
     */
    default List<TransferOrderDO> selectByProducerEnterpriseId(Long producerEnterpriseId) {
        return selectByProducingEnterpriseId(producerEnterpriseId);
    }

    /**
     * 根据回收企业ID查询订单列表
     */
    default List<TransferOrderDO> selectByRecyclerEnterpriseId(Long recyclerEnterpriseId) {
        return selectByRecyclingEnterpriseId(recyclerEnterpriseId);
    }

    /**
     * 根据状态查询订单列表
     */
    default List<TransferOrderDO> selectByStatus(Integer status) {
        return selectByBusinessStatus(status);
    }

    /**
     * 根据时间范围查询订单列表
     */
    default List<TransferOrderDO> selectByDateRange(LocalDateTime startTime, LocalDateTime endTime) {
        return selectOrdersCreatedBetween(startTime, endTime);
    }

    /**
     * 批量更新订单状态
     */
    default void batchUpdateStatus(@Param("ids") List<Long> ids, @Param("status") Integer status) {
        // 这里需要使用MyBatis的批量更新，可以通过XML实现或者循环更新
        for (Long id : ids) {
            TransferOrderDO updateObj = new TransferOrderDO();
            updateObj.setId(id);
            updateObj.setBusinessStatus(status);
            updateById(updateObj);
        }
    }

} 