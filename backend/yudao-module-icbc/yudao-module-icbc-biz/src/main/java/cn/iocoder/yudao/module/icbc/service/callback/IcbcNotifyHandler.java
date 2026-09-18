package cn.iocoder.yudao.module.icbc.service.callback;

import cn.iocoder.yudao.module.icbc.enums.CallbackNotifyTypeEnum;

/**
 * 工行异步通知处理器
 *
 * 九类通知各自的业务实现（#10 开票 / 缴税 / 上传，#14 红冲，#9 支付…）实现本接口，
 * 由 {@link CallbackNotifyService} 按类型分发。处理器必须是幂等的：
 * 同一 {@link IcbcNotifyContext#getNotifyId()} 重复调用只能产生一次业务效果。
 */
public interface IcbcNotifyHandler {

    /**
     * 本处理器负责的通知类型
     */
    CallbackNotifyTypeEnum supportType();

    /**
     * 处理通知
     */
    void handle(IcbcNotifyContext context);

}
