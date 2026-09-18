package cn.iocoder.yudao.module.icbc.gateway.model;

import lombok.Builder;
import lombok.Data;

/**
 * 发票下载请求
 *
 * 对应工行 `/api/jft/api/invoice/download/V1`。工行只返回 PDF。
 */
@Data
@Builder
public class InvoiceDownloadReq {

    private String outOrderId;
    private String outInvoiceId;
    private String outRedOffsetId;
    /**
     * 红蓝标识：01 蓝票，02 红票；不上送默认蓝票
     */
    private String isRedOrblue;

}
