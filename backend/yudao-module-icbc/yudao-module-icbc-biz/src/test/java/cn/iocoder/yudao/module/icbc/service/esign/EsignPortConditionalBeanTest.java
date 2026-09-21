package cn.iocoder.yudao.module.icbc.service.esign;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 锁住「电子签章端口二选一」（#92 验收第 1 条，brief 点名的坑）。
 *
 * <p>{@link StubEsignPort} 带 {@code @ConditionalOnProperty(..., havingValue="stub", matchIfMissing=true)}，
 * 真实实现会带 {@code havingValue="remote"}。两个 Bean 同时在场 Spring 启动即挂（同一类型多个候选），
 * 所以必须有测试证明：未配置时只有 stub、{@code mode=stub} 时只有 stub、{@code mode=remote} 时只有真实实现。
 * 只靠代码注释守不住这条。
 */
public class EsignPortConditionalBeanTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(StubEsignPort.class, RemoteEsignPortStub.class);

    @Test
    public void testDefaultMode_onlyStub() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(EsignPort.class);
            assertThat(context.getBean(EsignPort.class)).isInstanceOf(StubEsignPort.class);
        });
    }

    @Test
    public void testModeStub_onlyStub() {
        contextRunner.withPropertyValues("icbc.esign.mode=stub").run(context -> {
            assertThat(context).hasSingleBean(EsignPort.class);
            assertThat(context.getBean(EsignPort.class)).isInstanceOf(StubEsignPort.class);
        });
    }

    @Test
    public void testModeRemote_onlyRemote() {
        contextRunner.withPropertyValues("icbc.esign.mode=remote").run(context -> {
            assertThat(context).hasSingleBean(EsignPort.class);
            assertThat(context.getBean(EsignPort.class)).isInstanceOf(RemoteEsignPortStub.class);
        });
    }

    /**
     * 真实实现的替身：只用于验证二选一条件，业务语义无关。
     */
    @ConditionalOnProperty(prefix = "icbc.esign", name = "mode", havingValue = "remote")
    static class RemoteEsignPortStub implements EsignPort {

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
            throw new EsignCallbackRejectedException("测试替身");
        }

    }

}
