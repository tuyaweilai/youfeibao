package cn.iocoder.yudao.module.icbc.gateway.model;

import lombok.Builder;
import lombok.Data;

/**
 * 真实连通性校验结果
 *
 * 任何来自工行的业务响应（含业务错误）都说明「网关可达 + 签名有效」；
 * 只有代理异常 / 超时 / 未知才判定为不可达。
 */
@Data
@Builder
public class IcbcConnectivity {

    private boolean reachable;
    private int returnCode;
    private String returnMsg;

}
