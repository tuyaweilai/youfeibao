package cn.iocoder.yudao.module.icbc.service.payment;

import cn.iocoder.yudao.module.icbc.controller.admin.payment.vo.PaymentReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.payment.vo.PaymentRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.payment.vo.PaymentStatusQueryReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.payment.vo.PaymentStatusQueryRespVO;

/**
 * 工行付方支付 Service 接口
 *
 * @author 芋道源码
 */
public interface PaymentService {

    /**
     * 创建付方支付
     * 
     * 企业（付方）向个人（收方）支付货款
     *
     * @param paymentReqVO 支付请求信息
     * @return 支付响应，包含重定向URL
     */
    PaymentRespVO createPayment(PaymentReqVO paymentReqVO);

    /**
     * 查询支付状态
     *
     * @param queryReqVO 查询请求信息
     * @return 支付状态信息
     */
    PaymentStatusQueryRespVO queryPaymentStatus(PaymentStatusQueryReqVO queryReqVO);

    /**
     * 处理支付回调通知
     *
     * @param notifyData 回调通知数据
     * @return 处理结果
     */
    boolean handlePaymentNotify(String notifyData);

} 