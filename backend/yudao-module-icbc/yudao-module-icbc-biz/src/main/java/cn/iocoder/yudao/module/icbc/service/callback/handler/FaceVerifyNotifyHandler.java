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
 * 实人认证结果通知处理器
 *
 * <p>报文形如 {@code {appId, transNode, outUserId, verifyResult}}，{@code verifyResult}
 * 取 1 通过 / 0 未通过。回写出售者档案的实人认证状态。
 */
@Component
public class FaceVerifyNotifyHandler implements IcbcNotifyHandler {

    @Resource
    private SellerOnboardingService sellerOnboardingService;

    @Override
    public CallbackNotifyTypeEnum supportType() {
        return CallbackNotifyTypeEnum.FACE_VERIFY;
    }

    @Override
    public void handle(IcbcNotifyContext context) {
        JSONObject payload = context.payload();
        String outUserId = payload.getString("outUserId");
        boolean passed = "1".equals(payload.getString("verifyResult"));
        String failReason = StrUtil.blankToDefault(payload.getString("failReason"),
                payload.getString("custStatusDetail"));
        sellerOnboardingService.handleFaceVerifyNotify(outUserId, passed, failReason);
    }

}
