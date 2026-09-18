package cn.iocoder.yudao.module.icbc.dal.mysql.invoice;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.icbc.dal.dataobject.invoice.InvoiceOrderDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 工行反向开票订单 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface InvoiceOrderMapper extends BaseMapperX<InvoiceOrderDO> {

    /**
     * 根据合作方订单ID查询订单
     *
     * @param partnerOrderId 合作方订单ID
     * @return 订单信息
     */
    default InvoiceOrderDO selectByPartnerOrderId(String partnerOrderId) {
        return selectOne(InvoiceOrderDO::getPartnerOrderId, partnerOrderId);
    }

    /**
     * 根据订单号查询订单
     *
     * @param orderNo 订单号
     * @return 订单信息
     */
    default InvoiceOrderDO selectByOrderNo(String orderNo) {
        return selectOne(InvoiceOrderDO::getOrderNo, orderNo);
    }

    /**
     * 根据发票号码查询订单
     *
     * @param invoiceNo 发票号码
     * @return 订单信息
     */
    default InvoiceOrderDO selectByInvoiceNo(String invoiceNo) {
        return selectOne(InvoiceOrderDO::getInvoiceNo, invoiceNo);
    }

} 