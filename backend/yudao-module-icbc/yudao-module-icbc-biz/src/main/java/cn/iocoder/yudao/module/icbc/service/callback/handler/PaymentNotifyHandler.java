package cn.iocoder.yudao.module.icbc.service.callback.handler;

import cn.iocoder.yudao.module.icbc.enums.CallbackNotifyTypeEnum;
import cn.iocoder.yudao.module.icbc.service.callback.IcbcNotifyContext;
import cn.iocoder.yudao.module.icbc.service.callback.IcbcNotifyHandler;
import cn.iocoder.yudao.module.icbc.service.payment.PaymentService;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 支付状态通知处理器（{@code notifyType=02}）。
 *
 * <p>回收企业在工行支付页面授权、工行处理完成后推送这一条通知，携带 {@code payStatus}
 * 与 {@code payAmount} / {@code actuallyReceivedAmount}。平台据此把支付状态收敛回本地支付单；
 * 通知乱序或早于数据落库时，处理失败不丢记录，可重放。主动查询走的是同一条收敛路径，
 * 因此通知与查询两侧结果一致。
 */
@Component
public class PaymentNotifyHandler implements IcbcNotifyHandler {

    @Resource
    private PaymentService paymentService;

    @Override
    public CallbackNotifyTypeEnum supportType() {
        return CallbackNotifyTypeEnum.PAYMENT;
    }

    @Override
    public void handle(IcbcNotifyContext context) {
        JSONObject payload = context.payload();
        paymentService.applyPaymentStatus(context.getBusinessId(),
                payload.getString("payStatus"),
                payload.getString("payAmount"),
                payload.getString("actuallyReceivedAmount"),
                payload.getString("serialNo"),
                payload.getString("icbcOrderId"));
    }

}
