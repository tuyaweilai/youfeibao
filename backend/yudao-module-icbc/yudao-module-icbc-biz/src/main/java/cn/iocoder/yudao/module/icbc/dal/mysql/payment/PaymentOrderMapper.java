package cn.iocoder.yudao.module.icbc.dal.mysql.payment;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payment.PaymentOrderDO;
import org.apache.ibatis.annotations.Mapper;

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

} 