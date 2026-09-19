package cn.iocoder.yudao.module.icbc.service.callback.handler;

import cn.iocoder.yudao.module.icbc.enums.CallbackNotifyTypeEnum;
import cn.iocoder.yudao.module.icbc.service.callback.IcbcNotifyContext;
import cn.iocoder.yudao.module.icbc.service.callback.IcbcNotifyHandler;
import cn.iocoder.yudao.module.icbc.service.invoice.InvoiceNotifyInfoAssembler;
import cn.iocoder.yudao.module.icbc.service.invoice.InvoiceOrderService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 发票上传状态通知处理器（{@code notifyType=05}）。
 *
 * <p>发票文件上传至税务端后，工行推送 {@code uploadStatus}（00–05）。平台据此收敛上传状态线，
 * 与开票、缴税两条线互不阻塞。与查询路径共用
 * {@link InvoiceOrderService#applyInvoiceInfo}，两侧一致；通知乱序 / 重复 / 早到都不丢。
 */
@Component
public class InvoiceUploadNotifyHandler implements IcbcNotifyHandler {

    @Resource
    private InvoiceOrderService invoiceOrderService;

    @Override
    public CallbackNotifyTypeEnum supportType() {
        return CallbackNotifyTypeEnum.INVOICE_UPLOAD;
    }

    @Override
    public void handle(IcbcNotifyContext context) {
        invoiceOrderService.applyInvoiceInfo(context.getBusinessId(),
                InvoiceNotifyInfoAssembler.fromNotify(context.payload()));
    }

}
