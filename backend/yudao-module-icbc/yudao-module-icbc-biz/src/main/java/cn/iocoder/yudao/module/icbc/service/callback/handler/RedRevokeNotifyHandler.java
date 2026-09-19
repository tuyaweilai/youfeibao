package cn.iocoder.yudao.module.icbc.service.callback.handler;

import cn.iocoder.yudao.module.icbc.enums.CallbackNotifyTypeEnum;
import org.springframework.stereotype.Component;

/**
 * 红票撤销通知处理器（{@code notifyType=09}）：撤销中 / 撤销成功 / 撤销失败。
 */
@Component
public class RedRevokeNotifyHandler extends AbstractRedNotifyHandler {

    @Override
    public CallbackNotifyTypeEnum supportType() {
        return CallbackNotifyTypeEnum.RED_REVOKE;
    }
}
