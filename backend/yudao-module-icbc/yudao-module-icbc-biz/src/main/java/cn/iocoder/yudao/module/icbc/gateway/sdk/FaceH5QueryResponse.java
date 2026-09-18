package cn.iocoder.yudao.module.icbc.gateway.sdk;

import com.icbc.api.IcbcResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 实人认证结果查询响应
 *
 * 字段名与工行返回报文对齐；SDK 缺少该类，故在适配层内自建。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class FaceH5QueryResponse extends IcbcResponse {

    private String outUserId;
    /**
     * 认证状态：00-初始，01-认证中，认证通过 / 未通过的取值以工行返回为准
     */
    private String authResult;
    /**
     * 认证未通过时的原因
     */
    private String failReason;

}
