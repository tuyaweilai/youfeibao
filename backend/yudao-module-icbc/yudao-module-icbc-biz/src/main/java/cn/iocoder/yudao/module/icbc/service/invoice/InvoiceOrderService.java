package cn.iocoder.yudao.module.icbc.service.invoice;

import cn.iocoder.yudao.module.icbc.controller.admin.invoice.vo.InvoicePreOrderReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.invoice.vo.InvoicePreOrderRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.invoice.vo.InvoiceQueryReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.invoice.vo.InvoiceQueryRespVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.invoice.InvoiceOrderDO;
import cn.iocoder.yudao.module.icbc.gateway.model.InvoiceInfo;

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

    /**
     * 用工行通知 / 预查询里的五条状态线收敛平台侧状态。
     *
     * <p>这是开票、缴税、上传三条状态线的<strong>唯一收敛入口</strong>：通知（{@code notifyType=01/03/04/05}）
     * 与主动预查询都调它，两侧天然一致。各条线独立更新、未上送的不覆盖；重复通知幂等，
     * 乱序通知不回退已结清的成功态；查不到业务单时抛业务异常，通知落失败可重放。
     *
     * @param partnerOrderId 合作方订单号
     * @param info           工行侧状态快照（通知或预查询）
     */
    void applyInvoiceInfo(String partnerOrderId, InvoiceInfo info);

    /**
     * 付款成功的回调：把开票状态推进为「开票中」，并尽力向工行确认一次最新开票状态。
     *
     * <p>付款成功是「真正开票」的触发点。该方法<strong>不抛异常</strong>：开票数据缺失或
     * 查询失败都不应把已经成功的付款拖回失败，后续通知与查询会补齐。
     *
     * @param partnerOrderId 合作方订单号
     */
    void onPaymentSucceeded(String partnerOrderId);

} 