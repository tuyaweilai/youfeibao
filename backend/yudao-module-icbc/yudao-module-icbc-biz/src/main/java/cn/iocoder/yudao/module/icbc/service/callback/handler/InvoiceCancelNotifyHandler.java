package cn.iocoder.yudao.module.icbc.service.callback.handler;

import cn.iocoder.yudao.module.icbc.enums.CallbackNotifyTypeEnum;
import cn.iocoder.yudao.module.icbc.service.callback.IcbcNotifyContext;
import cn.iocoder.yudao.module.icbc.service.callback.IcbcNotifyHandler;
import cn.iocoder.yudao.module.icbc.service.invoice.InvoiceOrderService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 发票取消通知处理器（{@code notifyType=06}）。
 *
 * <p>取消只可能发生在「预开票成功但未支付」时，工行回报 {@code invoiceStatus=04 预开票取消}。
 * 平台把预开票状态置为取消、订单置为已取消，与主动取消走同一条收敛逻辑。通知重复 / 早到都不丢。
 */
@Component
public class InvoiceCancelNotifyHandler implements IcbcNotifyHandler {

    @Resource
    private InvoiceOrderService invoiceOrderService;

    @Override
    public CallbackNotifyTypeEnum supportType() {
        return CallbackNotifyTypeEnum.INVOICE_CANCEL;
    }

    @Override
    public void handle(IcbcNotifyContext context) {
        invoiceOrderService.applyInvoiceCancelled(context.getBusinessId());
    }
}
