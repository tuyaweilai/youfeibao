package cn.iocoder.yudao.module.icbc.gateway.model;

import lombok.Builder;
import lombok.Data;

/**
 * 收方入驻页面接口请求
 *
 * 对应工行 `/ui/jft/ui/user/edpopenacct/submit/V1`，收方入驻页面（实名 + 绑定银行卡 + 入驻）。
 * `edpopenacct` 是工行历史接口名；本项目**不开电子钱包**（见 ADR 0010），也不在端口上暴露钱包概念。
 * 再生资源场景固定 `businessType=0004`、`accountKind=02`、`receiverType=03`，
 * 由适配层填充，平台只给业务字段。
 */
@Data
@Builder
public class PayeeOnboardingPageReq {

    /**
     * 外部用户编号（自然人出售者）
     */
    private String outUserId;
    /**
     * 收方户名（自然人姓名）
     */
    private String receiverName;
    /**
     * 收方账号（银行卡号）
     */
    private String receiverAccount;
    /**
     * 手机号
     */
    private String mobile;
    /**
     * 证件号码（仅支持身份证）
     */
    private String idNo;
    /**
     * 职业，字典见工行接口文档
     */
    private String occupation;
    /**
     * 常用住址
     */
    private String address;
    /**
     * 证件签发日期 yyyy-MM-dd
     */
    private String signDate;
    /**
     * 证件截止日期 yyyy-MM-dd，永久有效传 9999-12-30
     */
    private String validityPeriod;
    /**
     * 申请入参：外部用户编号关联的交易流水号
     */
    private String corpSerno;
    /**
     * 成功返回页面
     */
    private String jumpUrl;
    /**
     * 失败返回页面
     */
    private String failJumpUrl;
    /**
     * 审核结果回调地址
     */
    private String callbackUrl;
    /**
     * 交易渠道：01 安卓 APP、02 iOS APP、03 H5、04 微信公众号、05 微信小程序、06 支付宝场景号
     */
    private String trxChannel;
    /**
     * 是否跳过影像上传
     */
    private String skipImgUpload;

}
