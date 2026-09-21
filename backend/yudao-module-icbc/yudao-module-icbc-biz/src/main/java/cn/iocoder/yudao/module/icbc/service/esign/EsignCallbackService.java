package cn.iocoder.yudao.module.icbc.service.esign;

/**
 * 电子签章签署状态通知 Service（#92，ADR 0036）。
 *
 * <p>回调入口只做四件事：**验签 → 归一化 → 反查租户 → 交回调用方**。
 * 本票不改任何协议状态（协议状态机属 #95）：{@link EsignPort.EsignCallback#getTenantId()} 就是交给 #95
 * 路由到正确租户的线索。
 *
 * <p><b>验签失败必须明确失败</b>：伪造的通知不能被静默吞掉，也不能当成「什么都没发生」。
 */
public interface EsignCallbackService {

    /**
     * 处理一条签署状态通知。
     *
     * @param signature 签名
     * @param timestamp 时间戳
     * @param nonce     随机串
     * @param body      原始报文
     * @return 归一化后的通知（含反查出的租户编号）
     */
    EsignPort.EsignCallback handle(String signature, String timestamp, String nonce, String body);

}
