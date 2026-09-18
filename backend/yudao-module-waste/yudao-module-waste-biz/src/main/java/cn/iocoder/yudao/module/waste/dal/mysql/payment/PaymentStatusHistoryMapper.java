package cn.iocoder.yudao.module.waste.dal.mysql.payment;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.waste.controller.admin.payment.vo.PaymentStatusHistoryPageReqVO;
import cn.iocoder.yudao.module.waste.dal.dataobject.payment.PaymentStatusHistoryDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 付款状态变更历史 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface PaymentStatusHistoryMapper extends BaseMapperX<PaymentStatusHistoryDO> {

    default PageResult<PaymentStatusHistoryDO> selectPage(PaymentStatusHistoryPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<PaymentStatusHistoryDO>()
                .eqIfPresent(PaymentStatusHistoryDO::getOrderId, reqVO.getOrderId())
                .eqIfPresent(PaymentStatusHistoryDO::getStatusFrom, reqVO.getFromStatus())
                .eqIfPresent(PaymentStatusHistoryDO::getStatusTo, reqVO.getToStatus())
                .eqIfPresent(PaymentStatusHistoryDO::getOperatorId, reqVO.getOperatorId())
                .likeIfPresent(PaymentStatusHistoryDO::getOperatorName, reqVO.getOperatorName())
                .eqIfPresent(PaymentStatusHistoryDO::getOperatorType, reqVO.getOperatorType())
                .betweenIfPresent(PaymentStatusHistoryDO::getChangeTime, reqVO.getChangeTime())
                .betweenIfPresent(PaymentStatusHistoryDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(PaymentStatusHistoryDO::getChangeTime));
    }

    default List<PaymentStatusHistoryDO> selectList(PaymentStatusHistoryPageReqVO reqVO) {
        return selectList(new LambdaQueryWrapperX<PaymentStatusHistoryDO>()
                .eqIfPresent(PaymentStatusHistoryDO::getOrderId, reqVO.getOrderId())
                .eqIfPresent(PaymentStatusHistoryDO::getStatusFrom, reqVO.getFromStatus())
                .eqIfPresent(PaymentStatusHistoryDO::getStatusTo, reqVO.getToStatus())
                .eqIfPresent(PaymentStatusHistoryDO::getOperatorId, reqVO.getOperatorId())
                .likeIfPresent(PaymentStatusHistoryDO::getOperatorName, reqVO.getOperatorName())
                .eqIfPresent(PaymentStatusHistoryDO::getOperatorType, reqVO.getOperatorType())
                .betweenIfPresent(PaymentStatusHistoryDO::getChangeTime, reqVO.getChangeTime())
                .betweenIfPresent(PaymentStatusHistoryDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(PaymentStatusHistoryDO::getChangeTime));
    }

    default List<PaymentStatusHistoryDO> selectListByOrderId(Long orderId) {
        return selectList(PaymentStatusHistoryDO::getOrderId, orderId);
    }

    default List<PaymentStatusHistoryDO> selectListByStatusFrom(Integer statusFrom) {
        return selectList(PaymentStatusHistoryDO::getStatusFrom, statusFrom);
    }

    default List<PaymentStatusHistoryDO> selectListByStatusTo(Integer statusTo) {
        return selectList(PaymentStatusHistoryDO::getStatusTo, statusTo);
    }

    default List<PaymentStatusHistoryDO> selectListByOperatorType(Integer operatorType) {
        return selectList(PaymentStatusHistoryDO::getOperatorType, operatorType);
    }

    default List<PaymentStatusHistoryDO> selectListByOperatorId(Long operatorId) {
        return selectList(PaymentStatusHistoryDO::getOperatorId, operatorId);
    }

    default List<PaymentStatusHistoryDO> selectListByOperatorName(String operatorName) {
        return selectList(PaymentStatusHistoryDO::getOperatorName, operatorName);
    }

    default List<PaymentStatusHistoryDO> selectListByPaymentMethod(Integer paymentMethod) {
        return selectList(PaymentStatusHistoryDO::getPaymentMethod, paymentMethod);
    }

    default List<PaymentStatusHistoryDO> selectListByVoucherId(Long voucherId) {
        return selectList(PaymentStatusHistoryDO::getVoucherId, voucherId);
    }

    default List<PaymentStatusHistoryDO> selectListByChangeTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        return selectList(new LambdaQueryWrapperX<PaymentStatusHistoryDO>()
                .between(PaymentStatusHistoryDO::getChangeTime, startTime, endTime)
                .orderByDesc(PaymentStatusHistoryDO::getChangeTime));
    }

    default List<PaymentStatusHistoryDO> selectSystemOperationHistories() {
        return selectList(new LambdaQueryWrapperX<PaymentStatusHistoryDO>()
                .eq(PaymentStatusHistoryDO::getOperatorType, 1) // 系统操作
                .orderByDesc(PaymentStatusHistoryDO::getChangeTime));
    }

    default List<PaymentStatusHistoryDO> selectProducerOperationHistories() {
        return selectList(new LambdaQueryWrapperX<PaymentStatusHistoryDO>()
                .eq(PaymentStatusHistoryDO::getOperatorType, 2) // 产废企业操作
                .orderByDesc(PaymentStatusHistoryDO::getChangeTime));
    }

    default List<PaymentStatusHistoryDO> selectRecyclerOperationHistories() {
        return selectList(new LambdaQueryWrapperX<PaymentStatusHistoryDO>()
                .eq(PaymentStatusHistoryDO::getOperatorType, 3) // 回收企业操作
                .orderByDesc(PaymentStatusHistoryDO::getChangeTime));
    }

    default List<PaymentStatusHistoryDO> selectAdminOperationHistories() {
        return selectList(new LambdaQueryWrapperX<PaymentStatusHistoryDO>()
                .eq(PaymentStatusHistoryDO::getOperatorType, 4) // 平台管理员操作
                .orderByDesc(PaymentStatusHistoryDO::getChangeTime));
    }

    default List<PaymentStatusHistoryDO> selectBankTransferHistories() {
        return selectList(new LambdaQueryWrapperX<PaymentStatusHistoryDO>()
                .eq(PaymentStatusHistoryDO::getPaymentMethod, 1) // 银行转账
                .isNotNull(PaymentStatusHistoryDO::getPaymentAmount)
                .orderByDesc(PaymentStatusHistoryDO::getChangeTime));
    }

    default List<PaymentStatusHistoryDO> selectCashPaymentHistories() {
        return selectList(new LambdaQueryWrapperX<PaymentStatusHistoryDO>()
                .eq(PaymentStatusHistoryDO::getPaymentMethod, 2) // 现金
                .isNotNull(PaymentStatusHistoryDO::getPaymentAmount)
                .orderByDesc(PaymentStatusHistoryDO::getChangeTime));
    }

    default List<PaymentStatusHistoryDO> selectCheckPaymentHistories() {
        return selectList(new LambdaQueryWrapperX<PaymentStatusHistoryDO>()
                .eq(PaymentStatusHistoryDO::getPaymentMethod, 3) // 支票
                .isNotNull(PaymentStatusHistoryDO::getPaymentAmount)
                .orderByDesc(PaymentStatusHistoryDO::getChangeTime));
    }

    default List<PaymentStatusHistoryDO> selectHistoriesWithVoucher() {
        return selectList(new LambdaQueryWrapperX<PaymentStatusHistoryDO>()
                .isNotNull(PaymentStatusHistoryDO::getVoucherId)
                .orderByDesc(PaymentStatusHistoryDO::getChangeTime));
    }

    default PaymentStatusHistoryDO selectLatestByOrderId(Long orderId) {
        return selectOne(new LambdaQueryWrapperX<PaymentStatusHistoryDO>()
                .eq(PaymentStatusHistoryDO::getOrderId, orderId)
                .orderByDesc(PaymentStatusHistoryDO::getChangeTime)
                .last("LIMIT 1"));
    }

    default PaymentStatusHistoryDO selectFirstByOrderId(Long orderId) {
        return selectOne(new LambdaQueryWrapperX<PaymentStatusHistoryDO>()
                .eq(PaymentStatusHistoryDO::getOrderId, orderId)
                .orderByAsc(PaymentStatusHistoryDO::getChangeTime)
                .last("LIMIT 1"));
    }

    default List<PaymentStatusHistoryDO> selectListByStatusTransition(Integer statusFrom, Integer statusTo) {
        return selectList(new LambdaQueryWrapperX<PaymentStatusHistoryDO>()
                .eq(PaymentStatusHistoryDO::getStatusFrom, statusFrom)
                .eq(PaymentStatusHistoryDO::getStatusTo, statusTo)
                .orderByDesc(PaymentStatusHistoryDO::getChangeTime));
    }

    // 新增Service实现类需要的方法
    default List<PaymentStatusHistoryDO> selectListByPaymentRecordId(Long paymentRecordId) {
        return selectList(PaymentStatusHistoryDO::getOrderId, paymentRecordId);
    }

} 