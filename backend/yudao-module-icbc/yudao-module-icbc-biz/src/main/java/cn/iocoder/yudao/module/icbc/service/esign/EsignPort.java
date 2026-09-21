package cn.iocoder.yudao.module.icbc.service.esign;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 电子签章端口（第三方电子合同）。
 *
 * <p>框架收购协议与「反向发票合规告知函」两份文书装进一个**合同组**，交给第三方电子签章平台
 * （目标供应商腾讯电子签）：回收企业作为发起方盖上**租户级企业印章**，自然人本人在自己手机上
 * 一次实名、一次签名把两份一起签完（ADR 0036）。
 *
 * <p>与工行的银税通道是两回事，因此**不进 {@code IcbcGateway}**（ADR 0009 / 0037）。
 *
 * <p><b>契约要点</b>：
 * <ul>
 *   <li>默认实现 {@link StubEsignPort} 使签章**不可用**：业务层把协议落
 *       {@code signMethod = PAPER}（取值见 {@code IcbcFrameworkAgreementDO#signMethod}），
 *       向导照常走完，**不抛异常、不阻断建档**（ADR 0036 / 0037）。</li>
 *   <li>判断「能不能电子签」只有一个入口：{@link #isAvailable(Long)}。业务层先问它，
 *       为 {@code false} 就走纸质签法；不要把空返回值当作失败来兜底。</li>
 *   <li>**章是租户级的**：发起方只能是回收企业（第三方接口约束，也与 ADR 0001 一致），
 *       平台不代盖。所以每个方法都显式带 {@code tenantId}，实现内部按该租户的子客编号与
 *       企业印章编号出站，不依赖环境里的租户上下文。</li>
 *   <li>**发起 ≠ 签完**：{@link #initiate} 返回即结束，协议状态落「待签署」；签署完成靠
 *       {@link #parseCallback} 收敛（ADR 0036）。</li>
 *   <li>签署链接**现生成现用**：第三方默认 30 分钟有效，不存、不复用（ADR 0036）。</li>
 * </ul>
 *
 * <p>本端口只覆盖「出站 + 验签」这层缝；协议状态怎么变、证据链怎么挂、门禁怎么判，都在业务层
 * （#95），端口不承载业务状态。
 */
public interface EsignPort {

    /**
     * 本租户此刻能不能发起电子签署：平台级参数已配置、租户已激活、企业印章已就位、额度未耗尽。
     *
     * <p>这是「电子签 vs 纸质签」的唯一判据。任何一项缺失都返回 {@code false}，
     * **不抛异常**——未开通的功能要安静降级，不能把建档流程卡死。
     *
     * @param tenantId 租户编号（章是租户级的）
     * @return true 表示可以发起电子签署
     */
    boolean isAvailable(Long tenantId);

    /**
     * 发起签署：一个合同组、多份文书，必须整体签署。
     *
     * <p>调用前应先确认 {@link #isAvailable(Long)}；未开通时返回
     * {@link EsignTask#empty()}（不抛异常）。签署顺序由实现按 ADR 0036 处理：
     * 企业先盖章、自然人后签署。
     *
     * @param tenantId 租户编号（发起方是这家回收企业）
     * @param request  合同组与签署方
     * @return 签署任务；没发起时 {@link EsignTask#empty()}
     */
    EsignTask initiate(Long tenantId, EsignRequest request);

    /**
     * 生成自然人的签署链接：**现生成现用，不存、不复用**。
     *
     * <p>第三方链接默认 30 分钟有效（ADR 0036：发了短信、本人过几天才点，旧链接不能复用），
     * 所以本人在我们页面上点「去签署」时才调它，拿到就跳转。
     *
     * @param tenantId   租户编号
     * @param signTaskId {@link #initiate} 返回的签署任务号
     * @param signer     自然人签署方
     * @return 一次性签署链接；未开通或任务不存在时返回 {@code null}
     */
    String createSignUrl(Long tenantId, String signTaskId, EsignSigner signer);

    /**
     * 查询并下载已签文件（协议生效后要能挂进该出售者的证据链，#95）。
     *
     * @param tenantId   租户编号
     * @param signTaskId 签署任务号
     * @return 已签文书；没签完或未开通时返回空列表
     */
    List<SignedDocument> listSignedDocuments(Long tenantId, String signTaskId);

    /**
     * 验签并解析第三方的签署状态通知。
     *
     * <p>回调报文里带的是**子客编号**而不是我们的租户号，所以返回结果必须带回解析出的
     * {@link EsignCallback#getTenantId()}，供调用方路由到正确的租户。
     *
     * <p><b>验签失败必须明确失败</b>（抛 {@link EsignCallbackRejectedException}），不得返回 {@code null}
     * 或「空通知」：伪造的通知不能被静默吞掉。实现必须把「验签不过 / 未开通 / 报文损坏」等
     * **受控拒绝**统一包成这一种异常；实现内部的 NPE / 数据库异常不算拒绝，照实抛出，
     * 免得真 bug 被伪装成伪造攻击。
     *
     * @param signature 签名
     * @param timestamp 时间戳
     * @param nonce     随机串
     * @param body      原始报文
     * @return 归一化后的签署状态通知
     * @throws EsignCallbackRejectedException 回调被明确拒绝（验签失败 / 未开通 / 报文不可解析）
     */
    EsignCallback parseCallback(String signature, String timestamp, String nonce, String body);

    /**
     * 端口对一条回调的**受控拒绝**：验签不过、未开通、报文不可解析。
     *
     * <p>它是端口契约里唯一一种「预期内的失败」：业务层把它翻译成平台错误码与可读原因；
     * 其它运行时异常（NPE、DB 异常……）不属于拒绝，必须原样向上抛，不能被伪装成「攻击」。
     */
    class EsignCallbackRejectedException extends RuntimeException {

        public EsignCallbackRejectedException(String message) {
            super(message);
        }

        public EsignCallbackRejectedException(String message, Throwable cause) {
            super(message, cause);
        }

    }

    /**
     * 发起签署的入参。
     */
    @Data
    @Builder
    class EsignRequest {

        /**
         * 业务键：收方（出售者）编号；回调据此定位协议
         */
        private Long payeeId;

        /**
         * 合同组名
         */
        private String groupName;

        /**
         * 组内文书：各自是独立文件，必须整体签署（ADR 0036）
         */
        private List<EsignDocument> documents;

        /**
         * 自然人签署方。
         *
         * <p>回收企业是发起方、用租户级企业印章，不出现在这里——章是租户级的，平台不代盖。
         */
        private EsignSigner signer;

    }

    /**
     * 合同组里的一份文书。
     */
    @Data
    @Builder
    class EsignDocument {

        /**
         * 文书名（框架收购协议 / 反向发票合规告知函）
         */
        private String name;

        /**
         * 平台模板编号。
         *
         * <p>模板由**平台**维护、托管在第三方（租户只能填变量）：协议要素是税总 5 号公告
         * 第十七条点名的合同流证据，不给租户编辑权（#95）。
         */
        private String templateId;

        /**
         * 模板变量：名称、数量、规格、回收期次、结算方式、姓名、证件号、日期……
         */
        private Map<String, String> variables;

    }

    /**
     * 自然人签署方。
     */
    @Data
    @Builder
    class EsignSigner {

        private String name;

        private String mobile;

        private String idCardNo;

    }

    /**
     * 签署任务。
     */
    @Data
    @Builder
    class EsignTask {

        /**
         * 第三方签署任务号；没发起时为空
         */
        private String signTaskId;

        public static EsignTask empty() {
            return EsignTask.builder().build();
        }

    }

    /**
     * 已签文书。
     */
    @Data
    @Builder
    class SignedDocument {

        /**
         * 文书名
         */
        private String name;

        /**
         * 已签文件地址（文件托管在第三方，我们只留地址）
         */
        private String fileUrl;

        private LocalDateTime signedAt;

    }

    /**
     * 归一化后的签署状态通知。
     *
     * <p>只讲「这个合同组签完了没有」，不讲我们这边的协议状态（待签署 / 生效 / 作废在
     * {@code icbc_framework_agreement} 上）：厂商的任务状态取值各不同，映射的责任在实现里，
     * 业务层只认 {@link #isFinished()}。
     */
    @Data
    @Builder
    class EsignCallback {

        /**
         * 由回调里的子客编号反查出的租户编号
         */
        private Long tenantId;

        /**
         * 合同组任务号
         */
        private String signTaskId;

        /**
         * 合同组是否已**整体**签署完成：两份文书一次签完，只认整体完成，不认单份
         */
        private boolean finished;

        /**
         * 未完成时的可读原因（放弃签署 / 已过期 / 已撤销……）；已完成时为空
         */
        private String unfinishedReason;

        /**
         * 签署完成时间；{@code finished = true} 时有值
         */
        private LocalDateTime signedAt;

    }

}
