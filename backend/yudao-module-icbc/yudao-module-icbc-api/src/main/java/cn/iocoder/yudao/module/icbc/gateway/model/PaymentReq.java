package cn.iocoder.yudao.module.icbc.gateway.model;

import lombok.Builder;
import lombok.Data;

/**
 * 付方支付请求
 *
 * 对应工行 `/ui/jft/ui/invoice/pay/V1`，返回企业支付页面。
 */
@Data
@Builder
public class PaymentReq {

    /**
     * 合作方订单编号
     */
    private String outOrderId;
    /**
     * 收方编号（子商户 / 回收企业）
     */
    private String outVendorId;
    /**
     * 付方编号（与 appId 一致）
     */
    private String outUserId;
    /**
     * 机构编码
     */
    private String verifiedCode;
    /**
     * U盾 ID
     */
    private String ukeyId;

}
