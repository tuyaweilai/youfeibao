package cn.iocoder.yudao.module.icbc.gateway.model;

import lombok.Builder;
import lombok.Data;

/**
 * 收方入驻请求
 *
 * <p>对应工行 `/api/jft/api/user/edpreceive/add/V1`（数据接口）。平台直接发起，**没有任何页面**：
 * 收方入驻走数据接口（ADR 0035），所以这里不带预填信息、CAMS 公钥、交易渠道这些页面专属字段。
 *
 * <p>再生资源场景固定 `businessType=0004`、`accountKind=02`（个人）、`receiverType=03`（自然人）、
 * `idType=0`（身份证），由适配层填充，平台只给业务字段。
 */
@Data
@Builder
public class PayeeOnboardingReq {

    /**
     * 子商户编号（回收企业）。与预下单 / 付款用同一个值：本租户付方档案的合作方付方编号。
     * 为空时适配层回退到全局配置，仅用于本地 / 联调环境。
     */
    private String outVendorId;
    /**
     * 外部用户编号（自然人主体）
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
     * 是否我行用户：0-非我行用户，1-我行用户
     */
    private String accountCode;
    /**
     * 收方行名。非我行用户（{@link #accountCode}=0）时可选填，可以是行名级别
     */
    private String bankName;
    /**
     * 手机号
     */
    private String mobile;
    /**
     * 证件号码（仅支持身份证）
     */
    private String idNo;
    /**
     * 职业，字典见工行接口文档；自然人为必填
     */
    private String occupation;
    /**
     * 常用住址；自然人为必填，且不少于 4 个汉字或 7 个字符
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
     * 审核结果回调地址
     */
    private String callbackUrl;

}
