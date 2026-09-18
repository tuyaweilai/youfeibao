package cn.iocoder.yudao.module.logistics.dal.mysql.temporaryorder;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.logistics.controller.admin.temporaryorder.vo.TemporaryOrderPageReqVO;
import cn.iocoder.yudao.module.logistics.dal.dataobject.temporaryorder.TemporaryOrderDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 物流临时订单 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface TemporaryOrderMapper extends BaseMapperX<TemporaryOrderDO> {

    default PageResult<TemporaryOrderDO> selectPage(TemporaryOrderPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<TemporaryOrderDO>()
                .likeIfPresent(TemporaryOrderDO::getOrderNo, reqVO.getOrderNo())
                .eqIfPresent(TemporaryOrderDO::getTaskId, reqVO.getTaskId())
                .eqIfPresent(TemporaryOrderDO::getDriverId, reqVO.getDriverId())
                .likeIfPresent(TemporaryOrderDO::getDriverName, reqVO.getDriverName())
                .eqIfPresent(TemporaryOrderDO::getWasteType, reqVO.getWasteType())
                .likeIfPresent(TemporaryOrderDO::getWasteName, reqVO.getWasteName())
                .likeIfPresent(TemporaryOrderDO::getPickupLocation, reqVO.getPickupLocation())
                .likeIfPresent(TemporaryOrderDO::getProducerName, reqVO.getProducerName())
                .likeIfPresent(TemporaryOrderDO::getProducerPhone, reqVO.getProducerPhone())
                .eqIfPresent(TemporaryOrderDO::getPaymentStatus, reqVO.getPaymentStatus())
                .eqIfPresent(TemporaryOrderDO::getConvertedToFormal, reqVO.getConvertedToFormal())
                .betweenIfPresent(TemporaryOrderDO::getPickupTime, reqVO.getBeginPickupTime(), reqVO.getEndPickupTime())
                .betweenIfPresent(TemporaryOrderDO::getCreateTime, reqVO.getBeginCreateTime(), reqVO.getEndCreateTime())
                .orderByDesc(TemporaryOrderDO::getId));
    }

    default List<TemporaryOrderDO> selectList(TemporaryOrderPageReqVO reqVO) {
        return selectList(new LambdaQueryWrapperX<TemporaryOrderDO>()
                .likeIfPresent(TemporaryOrderDO::getOrderNo, reqVO.getOrderNo())
                .eqIfPresent(TemporaryOrderDO::getTaskId, reqVO.getTaskId())
                .eqIfPresent(TemporaryOrderDO::getDriverId, reqVO.getDriverId())
                .likeIfPresent(TemporaryOrderDO::getDriverName, reqVO.getDriverName())
                .eqIfPresent(TemporaryOrderDO::getWasteType, reqVO.getWasteType())
                .likeIfPresent(TemporaryOrderDO::getWasteName, reqVO.getWasteName())
                .likeIfPresent(TemporaryOrderDO::getPickupLocation, reqVO.getPickupLocation())
                .likeIfPresent(TemporaryOrderDO::getProducerName, reqVO.getProducerName())
                .likeIfPresent(TemporaryOrderDO::getProducerPhone, reqVO.getProducerPhone())
                .eqIfPresent(TemporaryOrderDO::getPaymentStatus, reqVO.getPaymentStatus())
                .eqIfPresent(TemporaryOrderDO::getConvertedToFormal, reqVO.getConvertedToFormal())
                .betweenIfPresent(TemporaryOrderDO::getPickupTime, reqVO.getBeginPickupTime(), reqVO.getEndPickupTime())
                .betweenIfPresent(TemporaryOrderDO::getCreateTime, reqVO.getBeginCreateTime(), reqVO.getEndCreateTime())
                .orderByDesc(TemporaryOrderDO::getId));
    }

    default TemporaryOrderDO selectByOrderNo(String orderNo) {
        return selectOne(TemporaryOrderDO::getOrderNo, orderNo);
    }

    default List<TemporaryOrderDO> selectByTaskId(Long taskId) {
        return selectList(TemporaryOrderDO::getTaskId, taskId);
    }

    default List<TemporaryOrderDO> selectByDriverId(Long driverId) {
        return selectList(TemporaryOrderDO::getDriverId, driverId);
    }

    default List<TemporaryOrderDO> selectByPaymentStatus(Integer paymentStatus) {
        return selectList(TemporaryOrderDO::getPaymentStatus, paymentStatus);
    }

    default List<TemporaryOrderDO> selectByConvertedToFormal(Boolean convertedToFormal) {
        return selectList(TemporaryOrderDO::getConvertedToFormal, convertedToFormal);
    }

    default List<TemporaryOrderDO> selectByWasteType(String wasteType) {
        return selectList(TemporaryOrderDO::getWasteType, wasteType);
    }

    default Long selectCountByTaskId(Long taskId) {
        return selectCount(TemporaryOrderDO::getTaskId, taskId);
    }

    default Long selectCountByDriverId(Long driverId) {
        return selectCount(TemporaryOrderDO::getDriverId, driverId);
    }

} 