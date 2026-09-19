package cn.iocoder.yudao.module.icbc.service.callback.handler;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.icbc.service.callback.IcbcNotifyContext;
import cn.iocoder.yudao.module.icbc.service.callback.IcbcNotifyHandler;
import cn.iocoder.yudao.module.icbc.service.invoice.InvoiceNotifyInfoAssembler;
import cn.iocoder.yudao.module.icbc.service.invoice.RedInvoiceService;
import com.alibaba.fastjson.JSONObject;

import javax.annotation.Resource;

/**
 * 红冲三类通知（07 红票申请 / 08 红票上传 / 09 红票撤销）的公共处理。
 *
 * <p>红冲通知的业务键是 {@code outRedOffsetId}，而报文里同时带蓝票的 {@code outOrderId}；
 * 通知解析器按通用优先级取了 {@code outOrderId}，因此这里显式改取红冲流水号，
 * 避免三类红冲通知被误当成蓝票通知。
 */
public abstract class AbstractRedNotifyHandler implements IcbcNotifyHandler {

    @Resource
    protected RedInvoiceService redInvoiceService;

    @Override
    public void handle(IcbcNotifyContext context) {
        JSONObject payload = context.payload();
        String redOffsetNo = StrUtil.blankToDefault(
                payload != null ? payload.getString("outRedOffsetId") : null, context.getBusinessId());
        redInvoiceService.applyRedInvoiceInfo(redOffsetNo, InvoiceNotifyInfoAssembler.fromNotify(payload));
    }
}
