package cn.iocoder.yudao.module.icbc.dal.mysql.payment;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payment.PaymentOrderDO;
import cn.iocoder.yudao.module.icbc.enums.PaymentStatusEnum;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 工行付方支付订单 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface PaymentOrderMapper extends BaseMapperX<PaymentOrderDO> {

    /**
     * 根据合作方订单ID查询支付订单
     *
     * @param partnerOrderId 合作方订单ID
     * @return 支付订单
     */
    default PaymentOrderDO selectByPartnerOrderId(String partnerOrderId) {
        return selectOne("partner_order_id", partnerOrderId);
    }

    /**
     * 根据订单号查询支付订单
     *
     * @param orderNo 订单号
     * @return 支付订单
     */
    default PaymentOrderDO selectByOrderNo(String orderNo) {
        return selectOne("order_no", orderNo);
    }

    /**
     * 根据工行订单号查询支付订单
     *
     * @param icbcOrderNo 工行订单号
     * @return 支付订单
     */
    default PaymentOrderDO selectByIcbcOrderNo(String icbcOrderNo) {
        return selectOne("icbc_order_no", icbcOrderNo);
    }

    /**
     * 按收购单编号批量查询支付订单（自然人端「收款记录」按他名下的收购单聚合）。
     */
    default List<PaymentOrderDO> selectListByAcquisitionIds(Collection<Long> acquisitionIds) {
        if (acquisitionIds == null || acquisitionIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<PaymentOrderDO>()
                .in(PaymentOrderDO::getAcquisitionId, acquisitionIds)
                .orderByDesc(PaymentOrderDO::getId));
    }

    // ==================== 工作台待办（#56 T18） ====================

    /** 工作台「付款失败」条数：异常状态（失败 / 关闭 / 冲正 / 退汇 / 他行已扣款 / 部分成功）。 */
    default long selectCountException() {
        return selectCount(new LambdaQueryWrapperX<PaymentOrderDO>()
                .in(PaymentOrderDO::getPaymentStatus, PaymentStatusEnum.exceptionStatuses()));
    }

    /** 工作台「付款失败」明细：最近发生的排在前面，最多 {@code limit} 条。 */
    default List<PaymentOrderDO> selectListException(int limit) {
        return selectList(new LambdaQueryWrapperX<PaymentOrderDO>()
                .in(PaymentOrderDO::getPaymentStatus, PaymentStatusEnum.exceptionStatuses())
                .orderByDesc(PaymentOrderDO::getId)
                .last("LIMIT " + limit));
    }

} 