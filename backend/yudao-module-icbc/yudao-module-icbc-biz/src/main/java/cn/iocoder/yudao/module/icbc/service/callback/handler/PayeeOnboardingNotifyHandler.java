package cn.iocoder.yudao.module.icbc.service.callback.handler;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.icbc.enums.CallbackNotifyTypeEnum;
import cn.iocoder.yudao.module.icbc.service.callback.IcbcNotifyContext;
import cn.iocoder.yudao.module.icbc.service.callback.IcbcNotifyHandler;
import cn.iocoder.yudao.module.icbc.service.onboarding.SellerOnboardingService;import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 收方入驻结果通知处理器
 *
 * <p>报文形如 {@code {appId, appIdSub, outUserId, result}}（换卡时另带 {@code operaType}）。
 * 只有**审核一条线**：pass / reject（ADR 0035）。
 */
@Component
public class PayeeOnboardingNotifyHandler implements IcbcNotifyHandler {

    @Resource
    private SellerOnboardingService sellerOnboardingService;

    @Override
    public CallbackNotifyTypeEnum supportType() {
        return CallbackNotifyTypeEnum.PAYEE_ONBOARDING;
    }

    @Override
    public void handle(IcbcNotifyContext context) {
        JSONObject payload = context.payload();
        String outUserId = payload.getString("outUserId");
        String outVendorId = payload.getString("appIdSub");
        String result = payload.getString("result");
        String rejectReason = StrUtil.blankToDefault(payload.getString("rejectReason"),
                payload.getString("custStatusDetail"));
        // 修改（换卡）回调一定带 operaType=02，新增回调不带：结果落在不同对象上（#86）
        String operaType = payload.getString("operaType");
        sellerOnboardingService.handleOnboardingNotify(outUserId, outVendorId, result, rejectReason, operaType);
    }

}
