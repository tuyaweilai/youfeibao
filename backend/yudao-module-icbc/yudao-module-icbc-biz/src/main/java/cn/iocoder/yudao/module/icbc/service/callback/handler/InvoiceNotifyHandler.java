package cn.iocoder.yudao.module.icbc.service.callback.handler;

import cn.iocoder.yudao.module.icbc.enums.CallbackNotifyTypeEnum;
import cn.iocoder.yudao.module.icbc.service.callback.IcbcNotifyContext;
import cn.iocoder.yudao.module.icbc.service.callback.IcbcNotifyHandler;
import cn.iocoder.yudao.module.icbc.service.invoice.InvoiceNotifyInfoAssembler;
import cn.iocoder.yudao.module.icbc.service.invoice.InvoiceOrderService;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 开票状态通知处理器（{@code notifyType=03}）。
 *
 * <p>出售者在工行页面完成自然人确认后，工行推送这一条通知，携带
 * {@code confirmStatus} 与 {@code invoiceStatus}。平台据此把自然人确认与预开票两条状态线
 * 收敛回本地订单；通知乱序或早于数据落库时，处理失败不丢记录，可重放。
 */
@Component
public class InvoiceNotifyHandler implements IcbcNotifyHandler {

    @Resource
    private InvoiceOrderService invoiceOrderService;

    @Override
    public CallbackNotifyTypeEnum supportType() {
        return CallbackNotifyTypeEnum.INVOICE;
    }

    @Override
    public void handle(IcbcNotifyContext context) {
        JSONObject payload = context.payload();
        invoiceOrderService.applyInvoiceInfo(context.getBusinessId(),
                InvoiceNotifyInfoAssembler.fromNotify(payload));
    }
}
