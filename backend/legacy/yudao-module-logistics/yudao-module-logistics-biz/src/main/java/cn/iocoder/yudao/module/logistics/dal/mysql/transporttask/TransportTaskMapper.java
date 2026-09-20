package cn.iocoder.yudao.module.logistics.dal.mysql.transporttask;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.logistics.controller.admin.transporttask.vo.TransportTaskPageReqVO;
import cn.iocoder.yudao.module.logistics.dal.dataobject.transporttask.TransportTaskDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 物流运输任务 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface TransportTaskMapper extends BaseMapperX<TransportTaskDO> {

    default PageResult<TransportTaskDO> selectPage(TransportTaskPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<TransportTaskDO>()
                .likeIfPresent(TransportTaskDO::getTaskNo, reqVO.getTaskNo())
                .eqIfPresent(TransportTaskDO::getOrderId, reqVO.getOrderId())
                .likeIfPresent(TransportTaskDO::getOrderNo, reqVO.getOrderNo())
                .eqIfPresent(TransportTaskDO::getEnterpriseId, reqVO.getEnterpriseId())
                .eqIfPresent(TransportTaskDO::getVehicleId, reqVO.getVehicleId())
                .eqIfPresent(TransportTaskDO::getDriverId, reqVO.getDriverId())
                .eqIfPresent(TransportTaskDO::getTaskStatus, reqVO.getTaskStatus())
                .eqIfPresent(TransportTaskDO::getIsAbnormal, reqVO.getIsAbnormal())
                .eqIfPresent(TransportTaskDO::getIsTemporary, reqVO.getIsTemporary())
                .betweenIfPresent(TransportTaskDO::getExpectedPickupTime, reqVO.getBeginExpectedPickupTime(), reqVO.getEndExpectedPickupTime())
                .betweenIfPresent(TransportTaskDO::getExpectedDeliveryTime, reqVO.getBeginExpectedDeliveryTime(), reqVO.getEndExpectedDeliveryTime())
                .betweenIfPresent(TransportTaskDO::getCreateTime, reqVO.getBeginCreateTime(), reqVO.getEndCreateTime())
                .orderByDesc(TransportTaskDO::getId));
    }

    default List<TransportTaskDO> selectList(TransportTaskPageReqVO reqVO) {
        return selectList(new LambdaQueryWrapperX<TransportTaskDO>()
                .likeIfPresent(TransportTaskDO::getTaskNo, reqVO.getTaskNo())
                .eqIfPresent(TransportTaskDO::getOrderId, reqVO.getOrderId())
                .likeIfPresent(TransportTaskDO::getOrderNo, reqVO.getOrderNo())
                .eqIfPresent(TransportTaskDO::getEnterpriseId, reqVO.getEnterpriseId())
                .eqIfPresent(TransportTaskDO::getVehicleId, reqVO.getVehicleId())
                .eqIfPresent(TransportTaskDO::getDriverId, reqVO.getDriverId())
                .eqIfPresent(TransportTaskDO::getTaskStatus, reqVO.getTaskStatus())
                .eqIfPresent(TransportTaskDO::getIsAbnormal, reqVO.getIsAbnormal())
                .eqIfPresent(TransportTaskDO::getIsTemporary, reqVO.getIsTemporary())
                .betweenIfPresent(TransportTaskDO::getExpectedPickupTime, reqVO.getBeginExpectedPickupTime(), reqVO.getEndExpectedPickupTime())
                .betweenIfPresent(TransportTaskDO::getExpectedDeliveryTime, reqVO.getBeginExpectedDeliveryTime(), reqVO.getEndExpectedDeliveryTime())
                .betweenIfPresent(TransportTaskDO::getCreateTime, reqVO.getBeginCreateTime(), reqVO.getEndCreateTime())
                .orderByDesc(TransportTaskDO::getId));
    }

    default TransportTaskDO selectByTaskNo(String taskNo) {
        return selectOne(TransportTaskDO::getTaskNo, taskNo);
    }

    default List<TransportTaskDO> selectByOrderId(Long orderId) {
        return selectList(TransportTaskDO::getOrderId, orderId);
    }

    default List<TransportTaskDO> selectByVehicleId(Long vehicleId) {
        return selectList(TransportTaskDO::getVehicleId, vehicleId);
    }

    default List<TransportTaskDO> selectByDriverId(Long driverId) {
        return selectList(TransportTaskDO::getDriverId, driverId);
    }

    default List<TransportTaskDO> selectByTaskStatus(Integer taskStatus) {
        return selectList(TransportTaskDO::getTaskStatus, taskStatus);
    }

    default List<TransportTaskDO> selectByEnterpriseId(Long enterpriseId) {
        return selectList(TransportTaskDO::getEnterpriseId, enterpriseId);
    }

    default Long selectCountByVehicleId(Long vehicleId) {
        return selectCount(TransportTaskDO::getVehicleId, vehicleId);
    }

    default Long selectCountByDriverId(Long driverId) {
        return selectCount(TransportTaskDO::getDriverId, driverId);
    }

} 