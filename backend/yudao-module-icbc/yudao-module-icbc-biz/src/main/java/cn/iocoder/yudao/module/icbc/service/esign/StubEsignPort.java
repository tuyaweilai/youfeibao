package cn.iocoder.yudao.module.icbc.service.esign;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 默认的电子签章实现：签章不可用，业务降级为纸质签法。
 *
 * <p>一期只把这条缝立起来（端口 + 默认 stub + 平台级 / 租户级配置与开通流程见 #92），
 * 因此这里一律答「不可用」：{@link #isAvailable} 返回 {@code false}，其余方法返回空，
 * **不触网、不报错、不阻断建档**——协议落 {@code signMethod = PAPER}，向导照常走完（ADR 0036 / 0037）。
 *
 * <p>{@link #parseCallback} 是唯一的例外：签章不可用时不会有真实回调，走到这里说明配置或调用链
 * 有问题，明确失败比静默返回空更容易被发现；伪造的通知也不该被当成「什么都没发生」。
 *
 * <p>接入真实服务时替换本 Bean（#92）；替换时注意两个 Bean 不能同时在场，参考
 * {@code IcbcSdkGateway} / {@code FakeIcbcGateway} 用 {@code @ConditionalOnProperty}
 * 二选一的做法。
 */
@Component
public class StubEsignPort implements EsignPort {

    @Override
    public boolean isAvailable(Long tenantId) {
        return false;
    }

    @Override
    public EsignTask initiate(Long tenantId, EsignRequest request) {
        return EsignTask.empty();
    }

    @Override
    public String createSignUrl(Long tenantId, String signTaskId, EsignSigner signer) {
        return null;
    }

    @Override
    public List<SignedDocument> listSignedDocuments(Long tenantId, String signTaskId) {
        return Collections.emptyList();
    }

    @Override
    public EsignCallback parseCallback(String signature, String timestamp, String nonce, String body) {
        throw new UnsupportedOperationException("电子签章未开通，不应收到签署状态通知");
    }

}
