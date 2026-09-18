package cn.iocoder.yudao.module.icbc.service.invoice;

import cn.iocoder.yudao.module.icbc.controller.admin.invoice.vo.InvoicePreOrderReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.invoice.vo.InvoicePreOrderRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.invoice.vo.InvoiceQueryReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.invoice.vo.InvoiceQueryRespVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.invoice.InvoiceOrderDO;

/**
 * 工行反向开票订单 Service 接口
 *
 * @author 芋道源码
 */
public interface InvoiceOrderService {

    /**
     * 创建反向开票预下单
     *
     * @param createReqVO 创建信息
     * @return 预下单响应
     */
    InvoicePreOrderRespVO createPreOrder(InvoicePreOrderReqVO createReqVO);

    /**
     * 查询反向开票信息
     *
     * @param queryReqVO 查询信息
     * @return 查询响应
     */
    InvoiceQueryRespVO queryInvoiceInfo(InvoiceQueryReqVO queryReqVO);

    /**
     * 根据合作方订单ID获取订单
     *
     * @param partnerOrderId 合作方订单ID
     * @return 订单信息
     */
    InvoiceOrderDO getOrderByPartnerOrderId(String partnerOrderId);

    /**
     * 根据订单号获取订单
     *
     * @param orderNo 订单号
     * @return 订单信息
     */
    InvoiceOrderDO getOrderByOrderNo(String orderNo);

    /**
     * 更新订单状态
     *
     * @param id 订单ID
     * @param orderStatus 订单状态
     * @param invoiceStatus 开票状态
     * @param paymentStatus 支付状态
     * @param taxStatus 缴税状态
     */
    void updateOrderStatus(Long id, Integer orderStatus, Integer invoiceStatus, 
                          Integer paymentStatus, Integer taxStatus);

    /**
     * 更新发票信息
     *
     * @param id 订单ID
     * @param invoiceNo 发票号码
     * @param invoiceCode 发票代码
     * @param invoiceAmount 发票金额
     * @param taxAmount 税额
     */
    void updateInvoiceInfo(Long id, String invoiceNo, String invoiceCode, 
                          java.math.BigDecimal invoiceAmount, java.math.BigDecimal taxAmount);

} 