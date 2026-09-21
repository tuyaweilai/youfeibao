package cn.iocoder.yudao.module.icbc.gateway.model;

import lombok.Builder;
import lombok.Data;

/**
 * 收方修改（换卡）请求
 *
 * <p>对应工行 `/api/jft/api/user/edpreceive/update/V1`。工行只允许改这几个字段：收方账号、是否我行用户、
 * 收方行名与行号、证件签发日期、证件截止日期（见 `docs/icbc/收方入驻/聚富通智慧清分收方修改接口V1.pdf`）。
 * 户名与身份证号不可修改，所以不在本请求里。
 *
 * <p>换卡必须重走一次审核，审核期间按**修改前**的信息交易（ADR 0035 / 收方变更的既有规则）。
 */
@Data
@Builder
public class PayeeBankCardUpdateReq {

    /**
     * 子商户编号（回收企业）
     */
    private String outVendorId;
    /**
     * 外部用户编号（自然人主体）——必输，且是工行定位"改哪一个收方"的依据
     */
    private String outUserId;
    /**
     * 新的收方账号（银行卡号）。它与 {@link #accountCode} 要么同时上送、要么都不上送
     */
    private String receiverAccount;
    /**
     * 是否我行用户：0-非我行用户，1-我行用户
     */
    private String accountCode;
    /**
     * 收方行名（非我行用户时可选填）
     */
    private String bankName;
    /**
     * 证件签发日期 yyyy-MM-dd
     */
    private String signDate;
    /**
     * 证件截止日期 yyyy-MM-dd，永久有效传 9999-12-30
     */
    private String validityPeriod;
    /**
     * 审核结果回调地址
     */
    private String callbackUrl;

}
