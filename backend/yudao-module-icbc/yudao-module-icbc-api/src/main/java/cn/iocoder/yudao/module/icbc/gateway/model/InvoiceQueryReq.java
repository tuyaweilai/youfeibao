package cn.iocoder.yudao.module.icbc.gateway.model;

import lombok.Builder;
import lombok.Data;

/**
 * 开票信息预查询请求
 *
 * 对应工行 `/api/jft/api/invoice/queryInvoiceInfo/V1`。它是「不重复提交」策略里的
 * 查询侧：当预下单 / 付方支付 / 发票取消 / 红冲撤销的结果未知时，用同一个
 * {@code outOrderId} / {@code outRedOffsetId} 查回指令的最新状态。
 */
@Data
@Builder
public class InvoiceQueryReq {

    /**
     * 合作方订单编号
     */
    private String outOrderId;
    /**
     * 付方编号
     */
    private String outUserId;
    /**
     * 收方编号（子商户）
     */
    private String outVendorId;
    /**
     * 开票请求流水号
     */
    private String outInvoiceId;
    /**
     * 红冲流水号
     */
    private String outRedOffsetId;

}
