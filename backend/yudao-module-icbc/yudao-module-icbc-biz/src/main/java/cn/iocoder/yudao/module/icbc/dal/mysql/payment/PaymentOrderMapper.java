package cn.iocoder.yudao.module.icbc.dal.mysql.payment;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payment.PaymentOrderDO;
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

} 