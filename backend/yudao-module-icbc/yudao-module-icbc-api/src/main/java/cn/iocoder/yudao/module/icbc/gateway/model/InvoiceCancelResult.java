package cn.iocoder.yudao.module.icbc.gateway.model;

import lombok.Builder;
import lombok.Data;

/**
 * 发票取消结果
 */
@Data
@Builder
public class InvoiceCancelResult {

    private String reversalStatus;
    private String reversalMsg;

}
