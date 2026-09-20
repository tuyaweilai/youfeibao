package cn.iocoder.yudao.module.waste.dal.mysql.payment;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.waste.controller.admin.payment.vo.CompanyPaymentVoucherPageReqVO;
import cn.iocoder.yudao.module.waste.dal.dataobject.payment.CompanyPaymentVoucherDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDate;
import java.util.List;

/**
 * 对公付款凭证 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface CompanyPaymentVoucherMapper extends BaseMapperX<CompanyPaymentVoucherDO> {

    default PageResult<CompanyPaymentVoucherDO> selectPage(CompanyPaymentVoucherPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<CompanyPaymentVoucherDO>()
                .eqIfPresent(CompanyPaymentVoucherDO::getOrderId, reqVO.getOrderId())
                .likeIfPresent(CompanyPaymentVoucherDO::getVoucherNo, reqVO.getVoucherNumber())
                .eqIfPresent(CompanyPaymentVoucherDO::getPaymentType, reqVO.getPaymentType())
                .eqIfPresent(CompanyPaymentVoucherDO::getPayeeName, reqVO.getPayeeEnterpriseName())
                .likeIfPresent(CompanyPaymentVoucherDO::getTransactionNo, reqVO.getTransactionNumber())
                .betweenIfPresent(CompanyPaymentVoucherDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(CompanyPaymentVoucherDO::getCreateTime));
    }

    default List<CompanyPaymentVoucherDO> selectList(CompanyPaymentVoucherPageReqVO reqVO) {
        return selectList(new LambdaQueryWrapperX<CompanyPaymentVoucherDO>()
                .eqIfPresent(CompanyPaymentVoucherDO::getOrderId, reqVO.getOrderId())
                .likeIfPresent(CompanyPaymentVoucherDO::getVoucherNo, reqVO.getVoucherNumber())
                .eqIfPresent(CompanyPaymentVoucherDO::getPaymentType, reqVO.getPaymentType())
                .likeIfPresent(CompanyPaymentVoucherDO::getPayeeName, reqVO.getPayeeEnterpriseName())
                .likeIfPresent(CompanyPaymentVoucherDO::getTransactionNo, reqVO.getTransactionNumber())
                .betweenIfPresent(CompanyPaymentVoucherDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(CompanyPaymentVoucherDO::getCreateTime));
    }

    default List<CompanyPaymentVoucherDO> selectByOrderId(Long orderId) {
        return selectList(CompanyPaymentVoucherDO::getOrderId, orderId);
    }

    default CompanyPaymentVoucherDO selectByVoucherNo(String voucherNo) {
        return selectOne(CompanyPaymentVoucherDO::getVoucherNo, voucherNo);
    }

    default List<CompanyPaymentVoucherDO> selectByPaymentType(Integer paymentType) {
        return selectList(CompanyPaymentVoucherDO::getPaymentType, paymentType);
    }

    default List<CompanyPaymentVoucherDO> selectByVoucherStatus(Integer voucherStatus) {
        return selectList(CompanyPaymentVoucherDO::getVoucherStatus, voucherStatus);
    }

    default List<CompanyPaymentVoucherDO> selectPendingConfirmVouchers() {
        return selectList(new LambdaQueryWrapperX<CompanyPaymentVoucherDO>()
                .eq(CompanyPaymentVoucherDO::getVoucherStatus, 0) // 待确认状态
                .orderByDesc(CompanyPaymentVoucherDO::getPaymentDate));
    }

    default List<CompanyPaymentVoucherDO> selectConfirmedVouchers() {
        return selectList(new LambdaQueryWrapperX<CompanyPaymentVoucherDO>()
                .eq(CompanyPaymentVoucherDO::getVoucherStatus, 1) // 已确认状态
                .orderByDesc(CompanyPaymentVoucherDO::getPaymentDate));
    }

    default List<CompanyPaymentVoucherDO> selectDisputedVouchers() {
        return selectList(new LambdaQueryWrapperX<CompanyPaymentVoucherDO>()
                .eq(CompanyPaymentVoucherDO::getVoucherStatus, 2) // 有争议状态
                .orderByDesc(CompanyPaymentVoucherDO::getPaymentDate));
    }

    default List<CompanyPaymentVoucherDO> selectByTransactionNo(String transactionNo) {
        return selectList(CompanyPaymentVoucherDO::getTransactionNo, transactionNo);
    }

    default List<CompanyPaymentVoucherDO> selectByOperatorId(Long operatorId) {
        return selectList(CompanyPaymentVoucherDO::getOperatorId, operatorId);
    }

    default List<CompanyPaymentVoucherDO> selectByPaymentDateRange(LocalDate startDate, LocalDate endDate) {
        return selectList(new LambdaQueryWrapperX<CompanyPaymentVoucherDO>()
                .between(CompanyPaymentVoucherDO::getPaymentDate, startDate, endDate)
                .orderByDesc(CompanyPaymentVoucherDO::getPaymentDate));
    }

    default List<CompanyPaymentVoucherDO> selectUnconfirmedByProducer() {
        return selectList(new LambdaQueryWrapperX<CompanyPaymentVoucherDO>()
                .eq(CompanyPaymentVoucherDO::getConfirmedByProducer, false)
                .in(CompanyPaymentVoucherDO::getVoucherStatus, 0, 2) // 待确认或有争议
                .orderByDesc(CompanyPaymentVoucherDO::getPaymentDate));
    }

    default List<CompanyPaymentVoucherDO> selectUnconfirmedByRecycling() {
        return selectList(new LambdaQueryWrapperX<CompanyPaymentVoucherDO>()
                .eq(CompanyPaymentVoucherDO::getConfirmedByRecycling, false)
                .in(CompanyPaymentVoucherDO::getVoucherStatus, 0, 2) // 待确认或有争议
                .orderByDesc(CompanyPaymentVoucherDO::getPaymentDate));
    }

    default List<CompanyPaymentVoucherDO> selectFullyConfirmedVouchers() {
        return selectList(new LambdaQueryWrapperX<CompanyPaymentVoucherDO>()
                .eq(CompanyPaymentVoucherDO::getConfirmedByProducer, true)
                .eq(CompanyPaymentVoucherDO::getConfirmedByRecycling, true)
                .eq(CompanyPaymentVoucherDO::getVoucherStatus, 1) // 已确认状态
                .orderByDesc(CompanyPaymentVoucherDO::getPaymentDate));
    }

    default CompanyPaymentVoucherDO selectLatestByOrderId(Long orderId) {
        return selectOne(new LambdaQueryWrapperX<CompanyPaymentVoucherDO>()
                .eq(CompanyPaymentVoucherDO::getOrderId, orderId)
                .orderByDesc(CompanyPaymentVoucherDO::getPaymentDate)
                .last("LIMIT 1"));
    }

    // Service实现类需要的方法
    default List<CompanyPaymentVoucherDO> selectListByOrderId(Long orderId) {
        return selectByOrderId(orderId);
    }

    default List<CompanyPaymentVoucherDO> selectListByPaymentType(Integer paymentType) {
        return selectByPaymentType(paymentType);
    }

    default List<CompanyPaymentVoucherDO> selectListByProducerConfirmStatus(Integer status) {
        if (status == 1) {
            return selectUnconfirmedByProducer(); // 待确认
        } else {
            return selectList(new LambdaQueryWrapperX<CompanyPaymentVoucherDO>()
                    .eq(CompanyPaymentVoucherDO::getConfirmedByProducer, status == 2)
                    .orderByDesc(CompanyPaymentVoucherDO::getPaymentDate));
        }
    }

    default List<CompanyPaymentVoucherDO> selectListByRecyclerConfirmStatus(Integer status) {
        if (status == 1) {
            return selectUnconfirmedByRecycling(); // 待确认
        } else {
            return selectList(new LambdaQueryWrapperX<CompanyPaymentVoucherDO>()
                    .eq(CompanyPaymentVoucherDO::getConfirmedByRecycling, status == 2)
                    .orderByDesc(CompanyPaymentVoucherDO::getPaymentDate));
        }
    }

    default List<CompanyPaymentVoucherDO> selectPendingConfirmationVouchers() {
        return selectPendingConfirmVouchers();
    }

    default List<CompanyPaymentVoucherDO> selectListByPaymentTimeRange(java.time.LocalDateTime startTime, java.time.LocalDateTime endTime) {
        return selectList(new LambdaQueryWrapperX<CompanyPaymentVoucherDO>()
                .between(CompanyPaymentVoucherDO::getCreateTime, startTime, endTime)
                .orderByDesc(CompanyPaymentVoucherDO::getCreateTime));
    }

} 