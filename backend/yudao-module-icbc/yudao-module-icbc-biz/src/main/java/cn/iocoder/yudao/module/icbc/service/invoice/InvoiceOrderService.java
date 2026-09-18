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

    // ==================== 开票申请（#8 预下单与自然人确认） ====================

    /**
     * 把订单挂回它来源的收购单，并补齐收方 / 付方档案编号。
     *
     * @param partnerOrderId 合作方订单号
     * @param acquisitionId  收购单编号
     * @param payeeId        收方（出售者）档案编号
     * @param payerId        付方（回收企业）档案编号
     */
    void bindAcquisition(String partnerOrderId, Long acquisitionId, Long payeeId, Long payerId);

    /**
     * 用工行通知 / 预查询里的自然人确认状态与预开票状态收敛平台侧状态。
     *
     * <p>确认完成且预开票成功时订单状态推进为「已确认」；预开票失败 / 取消时保留在「待确认」。
     * 这两条状态线独立更新，不互相覆盖，允许通知乱序到达。
     *
     * @param partnerOrderId     合作方订单号
     * @param confirmStatusCode  自然人确认状态码：00/01/02，可空
     * @param preInvoiceStatusCode 预开票状态码：00/01/02/03/04，可空
     */
    void applyPreInvoiceStatus(String partnerOrderId, String confirmStatusCode, String preInvoiceStatusCode);

} 