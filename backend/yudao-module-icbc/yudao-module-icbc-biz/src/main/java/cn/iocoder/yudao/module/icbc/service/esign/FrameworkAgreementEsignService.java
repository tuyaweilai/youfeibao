package cn.iocoder.yudao.module.icbc.service.esign;

import java.util.List;

/**
 * 框架收购协议的**合同组电子签署**业务层（#95，ADR 0036）。
 *
 * <p>端口（{@link EsignPort}）只讲「怎么和第三方说话」；本服务讲「协议怎么落地」：
 * <ul>
 *   <li><b>两份文书一个合同组</b>：框架收购协议与反向发票合规告知函各自是独立文件，
 *       装进同一个合同组、一个签署链接、必须整体签署（ADR 0036 决策 3）。</li>
 *   <li><b>发起 ≠ 签完</b>：发起成功只把协议落「待签署」并记下合同组任务号；
 *       签完由回调收敛为「生效」并盖签署时间。</li>
 *   <li><b>协议状态只住在一处</b>：待签署 / 生效 / 作废都在 {@code icbc_framework_agreement} 上，
 *       端口不承载业务状态。</li>
 *   <li><b>签署链接现生成现用</b>：本人在页面上点「去签署」才生成，不存、不复用、不发短信
 *       （ADR 0036 决策 19、ADR 0023）。</li>
 * </ul>
 *
 * <p>「电子签 vs 纸质签」的判据是 {@link EsignPort#isAvailable(Long)}，不是本服务；
 * 未开通时调用方根本不走到这里，协议直接落 {@code PAPER}（ADR 0036 决策 7）。
 */
public interface FrameworkAgreementEsignService {

    /**
     * 为一份**待签署**的电子协议发起合同组签署：回收企业先盖章、自然人后签署。
     *
     * <p>成功后写回第三方合同组任务号并消耗一份租户合同额度；第三方没有返回任务号时
     * 明确失败（调用方的事务随之回滚，不留一份拿不到签署链接的半成品协议）。
     *
     * @param tenantId    租户编号（发起方是这家回收企业，用租户级企业印章）
     * @param agreementId 协议编号（调用前已落「待签署」）
     * @return 第三方合同组任务号
     */
    String initiate(Long tenantId, Long agreementId);

    /**
     * 现生成现用：自然人在自己的页面上点「去签署」时调用。
     *
     * <p>协议不在待签署 / 没有任务号 / 第三方没返回链接，都给出可读错误；**不缓存、不复用**。
     *
     * @param payeeId 收方（出售者）编号
     * @return 一次性签署链接
     */
    String createSignUrl(Long payeeId);

    /**
     * 处理第三方的签署状态通知：**整体签完**才把协议推到生效并盖签署时间，
     * 同时把旧的生效协议作废留痕。
     *
     * <p>幂等：同一条通知重放不会产生第二次副作用（状态机自检），未签完的通知不改任何状态。
     *
     * @param callback 端口归一化后的通知（含反查出的租户编号）
     */
    void applyFinishedCallback(EsignPort.EsignCallback callback);

    /**
     * 已签文书（协议生效后可查询与下载；文件托管在第三方，我们只回地址）。
     *
     * <p>没生效 / 没有任务号 / 第三方查询不到时返回空列表，不报错。
     *
     * @param payeeId 收方（出售者）编号
     * @return 已签文书；没有时为空列表
     */
    List<EsignPort.SignedDocument> listSignedDocuments(Long payeeId);

}
