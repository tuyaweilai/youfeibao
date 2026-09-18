package cn.iocoder.yudao.module.icbc.gateway.model;

import lombok.Builder;
import lombok.Data;

/**
 * 实人认证 H5 页面接口请求
 *
 * 对应工行 `/ui/jft/ui/user/faceH5/submit/V1`。实名核验是收方入驻的前置环节：
 * 平台先发起实人认证，认证通过后才可用同一个 {@code outUserId} 走收方入驻与开票。
 * 本项目固定 {@code authScene=01 反向开票}，证件目前仅支持身份证，由适配层填充。
 */
@Data
@Builder
public class FaceVerifyPageReq {

    /**
     * 外部用户编号（自然人出售者）
     */
    private String outUserId;
    /**
     * 收方姓名
     */
    private String custName;
    /**
     * 证件号码（仅支持身份证）
     */
    private String certNo;
    /**
     * 手机号（用于发验证短信）
     */
    private String mobile;
    /**
     * 合作方交易单号
     */
    private String transNo;
    /**
     * 认证结果回调地址（不可带参数）
     */
    private String callbackUrl;
    /**
     * 成功返回页面（可带参数）
     */
    private String jumpUrl;
    /**
     * 失败返回页面
     */
    private String failJumpUrl;

}
