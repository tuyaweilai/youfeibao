package cn.iocoder.yudao.module.icbc.service.callback.handler;

import cn.iocoder.yudao.module.icbc.enums.CallbackNotifyTypeEnum;
import org.springframework.stereotype.Component;

/**
 * 红票申请通知处理器（{@code notifyType=07}）：红字确认单生成 / 申请成功 / 申请失败。
 */
@Component
public class RedApplyNotifyHandler extends AbstractRedNotifyHandler {

    @Override
    public CallbackNotifyTypeEnum supportType() {
        return CallbackNotifyTypeEnum.RED_APPLY;
    }
}
