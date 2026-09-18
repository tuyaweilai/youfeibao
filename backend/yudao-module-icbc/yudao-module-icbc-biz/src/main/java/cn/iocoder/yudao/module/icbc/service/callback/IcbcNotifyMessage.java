package cn.iocoder.yudao.module.icbc.service.callback;

import cn.iocoder.yudao.module.icbc.enums.CallbackNotifyTypeEnum;
import lombok.Builder;
import lombok.Data;

/**
 * 解码后的工行异步通知
 */
@Data
@Builder
public class IcbcNotifyMessage {

    /**
     * 通知唯一编号（幂等键）
     */
    private String notifyId;
    /**
     * 通知类型
     */
    private CallbackNotifyTypeEnum notifyType;
    /**
     * 关联业务编号（合作方订单号 / 红冲流水号 / 外部用户编号）
     */
    private String businessId;
    /**
     * 解码后的通知 JSON
     */
    private String notifyData;
    /**
     * 工行签名（signData）
     */
    private String sign;

}
