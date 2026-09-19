package cn.iocoder.yudao.module.icbc.service.callback.handler;

import cn.iocoder.yudao.module.icbc.enums.CallbackNotifyTypeEnum;
import cn.iocoder.yudao.module.icbc.service.callback.IcbcNotifyContext;
import cn.iocoder.yudao.module.icbc.service.callback.IcbcNotifyHandler;
import cn.iocoder.yudao.module.icbc.service.invoice.InvoiceNotifyInfoAssembler;
import cn.iocoder.yudao.module.icbc.service.invoice.InvoiceOrderService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 缴税状态通知处理器（{@code notifyType=04}）。
 *
 * <p>开票完成后工行按代办税费流程缴税，并推送 {@code taxStatus}、{@code taxRealAmount}、
 * {@code tradeTime}、{@code taxPaymentMethod}。平台据此把缴税状态线与缴税字段收敛回本地订单；
 * 缴税成功（或无需缴税）后即可出具缴税凭证。与查询路径共用
 * {@link InvoiceOrderService#applyInvoiceInfo}，两侧一致；通知乱序 / 重复 / 早到都不丢。
 */
@Component
public class TaxNotifyHandler implements IcbcNotifyHandler {

    @Resource
    private InvoiceOrderService invoiceOrderService;

    @Override
    public CallbackNotifyTypeEnum supportType() {
        return CallbackNotifyTypeEnum.TAX;
    }

    @Override
    public void handle(IcbcNotifyContext context) {
        invoiceOrderService.applyInvoiceInfo(context.getBusinessId(),
                InvoiceNotifyInfoAssembler.fromNotify(context.payload()));
    }

}
