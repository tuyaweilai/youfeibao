package cn.iocoder.yudao.module.icbc.service.callback.handler;

import cn.iocoder.yudao.module.icbc.enums.CallbackNotifyTypeEnum;
import cn.iocoder.yudao.module.icbc.service.callback.IcbcNotifyContext;
import cn.iocoder.yudao.module.icbc.service.callback.IcbcNotifyHandler;
import cn.iocoder.yudao.module.icbc.service.invoice.InvoiceOrderService;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 预下单异常通知处理器（{@code notifyType=01}）。
 *
 * <p>预下单 / 自然人确认阶段出问题时，工行推送这一条通知，携带 {@code invoiceStatus}
 * （例如 {@code 03} 预开票失败）。与开票通知共用同一条状态收敛路径。
 */
@Component
public class PreOrderExceptionNotifyHandler implements IcbcNotifyHandler {

    @Resource
    private InvoiceOrderService invoiceOrderService;

    @Override
    public CallbackNotifyTypeEnum supportType() {
        return CallbackNotifyTypeEnum.PRE_ORDER_EXCEPTION;
    }

    @Override
    public void handle(IcbcNotifyContext context) {
        JSONObject payload = context.payload();
        invoiceOrderService.applyPreInvoiceStatus(context.getBusinessId(),
                payload.getString("confirmStatus"), payload.getString("invoiceStatus"));
    }
}
