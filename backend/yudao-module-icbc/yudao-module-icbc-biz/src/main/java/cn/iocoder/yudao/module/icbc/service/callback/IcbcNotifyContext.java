package cn.iocoder.yudao.module.icbc.service.callback;

import cn.iocoder.yudao.module.icbc.enums.CallbackNotifyTypeEnum;
import com.alibaba.fastjson.JSONObject;
import lombok.Builder;
import lombok.Data;

/**
 * 通知处理上下文
 *
 * 处理器据此执行业务，并可用 {@link #getNotifyId()} 做业务侧幂等。
 */
@Data
@Builder
public class IcbcNotifyContext {

    private Long recordId;
    /**
     * 通知唯一编号（幂等键）
     */
    private String notifyId;
    /**
     * 通知类型
     */
    private CallbackNotifyTypeEnum notifyType;
    /**
     * 关联业务编号
     */
    private String businessId;
    /**
     * 解码后的通知 JSON
     */
    private String notifyData;

    public JSONObject payload() {
        return JSONObject.parseObject(notifyData);
    }

}
