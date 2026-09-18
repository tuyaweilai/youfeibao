package cn.iocoder.yudao.module.logistics.dal.mysql.cashadvance;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.logistics.controller.admin.cashadvance.vo.CashAdvancePageReqVO;
import cn.iocoder.yudao.module.logistics.dal.dataobject.cashadvance.CashAdvanceDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 物流现金代付记录 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface CashAdvanceMapper extends BaseMapperX<CashAdvanceDO> {

    default PageResult<CashAdvanceDO> selectPage(CashAdvancePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<CashAdvanceDO>()
                .eqIfPresent(CashAdvanceDO::getTaskId, reqVO.getTaskId())
                .eqIfPresent(CashAdvanceDO::getOrderId, reqVO.getOrderId())
                .eqIfPresent(CashAdvanceDO::getDriverId, reqVO.getDriverId())
                .likeIfPresent(CashAdvanceDO::getDriverName, reqVO.getDriverName())
                .likeIfPresent(CashAdvanceDO::getPaymentLocation, reqVO.getPaymentLocation())
                .eqIfPresent(CashAdvanceDO::getPaymentMethod, reqVO.getPaymentMethod())
                .likeIfPresent(CashAdvanceDO::getPayeeName, reqVO.getPayeeName())
                .likeIfPresent(CashAdvanceDO::getPayeePhone, reqVO.getPayeePhone())
                .eqIfPresent(CashAdvanceDO::getNotifyStatus, reqVO.getNotifyStatus())
                .eqIfPresent(CashAdvanceDO::getReconcileStatus, reqVO.getReconcileStatus())
                .betweenIfPresent(CashAdvanceDO::getPaymentTime, reqVO.getBeginPaymentTime(), reqVO.getEndPaymentTime())
                .betweenIfPresent(CashAdvanceDO::getCreateTime, reqVO.getBeginCreateTime(), reqVO.getEndCreateTime())
                .orderByDesc(CashAdvanceDO::getId));
    }

    default List<CashAdvanceDO> selectList(CashAdvancePageReqVO reqVO) {
        return selectList(new LambdaQueryWrapperX<CashAdvanceDO>()
                .eqIfPresent(CashAdvanceDO::getTaskId, reqVO.getTaskId())
                .eqIfPresent(CashAdvanceDO::getOrderId, reqVO.getOrderId())
                .eqIfPresent(CashAdvanceDO::getDriverId, reqVO.getDriverId())
                .likeIfPresent(CashAdvanceDO::getDriverName, reqVO.getDriverName())
                .likeIfPresent(CashAdvanceDO::getPaymentLocation, reqVO.getPaymentLocation())
                .eqIfPresent(CashAdvanceDO::getPaymentMethod, reqVO.getPaymentMethod())
                .likeIfPresent(CashAdvanceDO::getPayeeName, reqVO.getPayeeName())
                .likeIfPresent(CashAdvanceDO::getPayeePhone, reqVO.getPayeePhone())
                .eqIfPresent(CashAdvanceDO::getNotifyStatus, reqVO.getNotifyStatus())
                .eqIfPresent(CashAdvanceDO::getReconcileStatus, reqVO.getReconcileStatus())
                .betweenIfPresent(CashAdvanceDO::getPaymentTime, reqVO.getBeginPaymentTime(), reqVO.getEndPaymentTime())
                .betweenIfPresent(CashAdvanceDO::getCreateTime, reqVO.getBeginCreateTime(), reqVO.getEndCreateTime())
                .orderByDesc(CashAdvanceDO::getId));
    }

    default List<CashAdvanceDO> selectByTaskId(Long taskId) {
        return selectList(CashAdvanceDO::getTaskId, taskId);
    }

    default List<CashAdvanceDO> selectByOrderId(Long orderId) {
        return selectList(CashAdvanceDO::getOrderId, orderId);
    }

    default List<CashAdvanceDO> selectByDriverId(Long driverId) {
        return selectList(CashAdvanceDO::getDriverId, driverId);
    }

    default List<CashAdvanceDO> selectByNotifyStatus(Integer notifyStatus) {
        return selectList(CashAdvanceDO::getNotifyStatus, notifyStatus);
    }

    default List<CashAdvanceDO> selectByReconcileStatus(Integer reconcileStatus) {
        return selectList(CashAdvanceDO::getReconcileStatus, reconcileStatus);
    }

    default List<CashAdvanceDO> selectByPaymentMethod(String paymentMethod) {
        return selectList(CashAdvanceDO::getPaymentMethod, paymentMethod);
    }

    default Long selectCountByTaskId(Long taskId) {
        return selectCount(CashAdvanceDO::getTaskId, taskId);
    }

    default Long selectCountByOrderId(Long orderId) {
        return selectCount(CashAdvanceDO::getOrderId, orderId);
    }

    default Long selectCountByDriverId(Long driverId) {
        return selectCount(CashAdvanceDO::getDriverId, driverId);
    }

} 