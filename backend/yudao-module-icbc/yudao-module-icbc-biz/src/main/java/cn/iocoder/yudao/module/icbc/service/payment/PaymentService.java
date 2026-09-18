package cn.iocoder.yudao.module.icbc.service.payment;

import cn.iocoder.yudao.module.icbc.controller.admin.payment.vo.PaymentApplyReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.payment.vo.PaymentApplyRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.payment.vo.PaymentQueryReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.payment.vo.PaymentReceiptRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.payment.vo.PaymentStatusRespVO;

/**
 * 付方支付 Service 接口（issue #9）。
 *
 * <p>平台代回收企业向出售者付款：对一笔「已预开票成功」的收购发起付款指令，经工行适配层
 * 生成企业支付页面，回收企业在页面上授权后，货款从回收企业账户经<strong>公对私结算</strong>
 * 直付到出售者本人银行卡。平台只发指令、归集回单，不碰资金（见 ADR 0006 / 0010）。
 *
 * <p>支付状态有两条收敛路径：工行的 {@code notifyType=02} 异步通知，与主动查询
 * （经 {@code queryInvoiceInfo} 取 {@code payStatus}）。两条路径都汇入
 * {@link #applyPaymentStatus}，保证通知与查询结果一致。
 */
public interface PaymentService {

    /**
     * 对预开票成功的收购发起付款，返回企业支付页面。
     *
     * <p>付款金额恒等于收购单金额；调用方若显式传入不一致的金额将被拦截。
     * 同一业务单号已成功或在途时不重复提交；处于失败 / 冲正 / 退汇 / 部分成功等
     * 异常状态时允许重新发起。
     */
    PaymentApplyRespVO applyPayment(PaymentApplyReqVO reqVO);

    /**
     * 查询支付状态：先取本地快照，再经适配层主动查询收敛一次，返回收敛后的结果。
     */
    PaymentStatusRespVO queryPaymentStatus(PaymentQueryReqVO reqVO);

    /**
     * 用工行 {@code payStatus} 收敛平台侧支付状态。
     *
     * <p>通知路径（{@code PaymentNotifyHandler}）与主动查询路径共用本方法。支付成功时归档
     * 转账回单、回写开票单支付状态，并把收购单推进为「已付款」。
     *
     * @param partnerOrderId       合作方订单号（收购单号）
     * @param payStatus            工行原始支付状态码，可空
     * @param payAmount            工行回传的支付金额，可空
     * @param actuallyReceivedAmount 实际到账金额（部分成功时与支付金额不同），可空
     * @param serialNo             支付流水号，可空
     * @param icbcOrderId          工行订单号，可空
     */
    void applyPaymentStatus(String partnerOrderId, String payStatus, String payAmount,
                            String actuallyReceivedAmount, String serialNo, String icbcOrderId);

    /**
     * 取转账回单归档信息（仅支付成功 / 部分成功后可取）。
     */
    PaymentReceiptRespVO getReceipt(String partnerOrderId);

}
