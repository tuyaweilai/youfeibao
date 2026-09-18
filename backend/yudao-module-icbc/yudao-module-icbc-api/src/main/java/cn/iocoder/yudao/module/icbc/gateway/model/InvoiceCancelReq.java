package cn.iocoder.yudao.module.icbc.gateway.model;

import lombok.Builder;
import lombok.Data;

/**
 * 发票取消请求
 *
 * 对应工行 `/api/jft/api/invoice/reversal/V1`，仅限「预开票成功但未支付」的发票。
 */
@Data
@Builder
public class InvoiceCancelReq {

    /**
     * 合作方订单编号
     */
    private String outOrderId;

}
