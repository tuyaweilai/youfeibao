package cn.iocoder.yudao.module.icbc.service.callback.handler;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.icbc.enums.CallbackNotifyTypeEnum;
import cn.iocoder.yudao.module.icbc.service.callback.IcbcNotifyContext;
import cn.iocoder.yudao.module.icbc.service.callback.IcbcNotifyHandler;
import cn.iocoder.yudao.module.icbc.service.onboarding.SellerOnboardingService;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 收方入驻结果通知处理器
 *
 * <p>报文形如
 * {@code {appId, appIdSub, outUserId, result, receiverAccount, openacctStatus, mediumId, mobileNo}}。
 * 两条独立的成败线：{@code openacctStatus}（02 成功 / 03 失败）与 {@code result}
 * （pass / reject）。归一到出售者建档状态机的四种组合。
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
        String openacctStatus = payload.getString("openacctStatus");
        String mediumId = payload.getString("mediumId");
        String rejectReason = StrUtil.blankToDefault(payload.getString("rejectReason"),
                payload.getString("custStatusDetail"));
        sellerOnboardingService.handleOnboardingNotify(outUserId, outVendorId, result, openacctStatus,
                mediumId, rejectReason);
    }

}
