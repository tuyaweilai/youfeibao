package cn.iocoder.yudao.module.icbc.service.callback.handler;

import cn.iocoder.yudao.module.icbc.enums.CallbackNotifyTypeEnum;
import org.springframework.stereotype.Component;

/**
 * 红票上传通知处理器（{@code notifyType=08}）：红票上传处理中 / 成功 / 失败。
 */
@Component
public class RedUploadNotifyHandler extends AbstractRedNotifyHandler {

    @Override
    public CallbackNotifyTypeEnum supportType() {
        return CallbackNotifyTypeEnum.RED_UPLOAD;
    }
}
