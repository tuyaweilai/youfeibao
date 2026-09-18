package cn.iocoder.yudao.module.icbc.gateway.model;

import lombok.Builder;
import lombok.Data;

/**
 * 红字冲销确认单撤销结果
 */
@Data
@Builder
public class RedInvoiceRevokeResult {

    private String outRedOffsetId;
    private String revokeStatus;

}
