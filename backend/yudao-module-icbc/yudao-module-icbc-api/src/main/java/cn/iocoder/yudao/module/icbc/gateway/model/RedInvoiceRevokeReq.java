package cn.iocoder.yudao.module.icbc.gateway.model;

import lombok.Builder;
import lombok.Data;

/**
 * 红字冲销确认单撤销请求
 *
 * 对应工行 `/api/jft/api/red/invoice/offset/revoke/V1`。
 */
@Data
@Builder
public class RedInvoiceRevokeReq {

    /**
     * 红冲流水号
     */
    private String outRedOffsetId;
    /**
     * 原蓝字合作方订单编号
     */
    private String outOrderId;

}
