package cn.iocoder.yudao.module.icbc.service.cardrecognition;

import cn.iocoder.yudao.module.icbc.service.cardrecognition.tencent.HutoolTencentOcrTransport;
import cn.iocoder.yudao.module.icbc.service.cardrecognition.tencent.TencentCardRecognition;
import cn.iocoder.yudao.module.icbc.service.cardrecognition.tencent.TencentCardRecognitionProperties;
import cn.iocoder.yudao.module.icbc.service.cardrecognition.tencent.TencentOcrClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 锁住「卡证识别端口二选一」（#93，ADR 0037，brief 点名的坑）。
 *
 * <p>{@link StubCardRecognition} 带 {@code @ConditionalOnProperty(..., havingValue="stub", matchIfMissing=true)}，
 * {@link TencentCardRecognition} 带 {@code havingValue="tencent"}。两个 Bean 同时在场 Spring 启动即挂
 * （同一类型多个候选），所以必须有测试证明：未配置时只有 stub、{@code mode=stub} 时只有 stub、
 * {@code mode=tencent} 时只有腾讯实现。只靠代码注释守不住这条（照 {@code EsignPortConditionalBeanTest}）。
 */
public class CardRecognitionPortConditionalBeanTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(StubCardRecognition.class, TencentCardRecognition.class,
                    TencentCardRecognitionProperties.class, TencentOcrClient.class,
                    HutoolTencentOcrTransport.class);

    @Test
    public void testDefaultMode_onlyStub() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(CardRecognitionPort.class);
            assertThat(context.getBean(CardRecognitionPort.class)).isInstanceOf(StubCardRecognition.class);
        });
    }

    @Test
    public void testModeStub_onlyStub() {
        contextRunner.withPropertyValues("icbc.card-recognition.mode=stub").run(context -> {
            assertThat(context).hasSingleBean(CardRecognitionPort.class);
            assertThat(context.getBean(CardRecognitionPort.class)).isInstanceOf(StubCardRecognition.class);
        });
    }

    @Test
    public void testModeTencent_onlyTencent() {
        contextRunner.withPropertyValues("icbc.card-recognition.mode=tencent").run(context -> {
            assertThat(context).hasSingleBean(CardRecognitionPort.class);
            assertThat(context.getBean(CardRecognitionPort.class)).isInstanceOf(TencentCardRecognition.class);
        });
    }

}
