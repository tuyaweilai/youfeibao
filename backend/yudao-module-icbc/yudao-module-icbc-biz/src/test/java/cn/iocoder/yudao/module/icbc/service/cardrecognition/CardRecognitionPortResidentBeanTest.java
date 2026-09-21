package cn.iocoder.yudao.module.icbc.service.cardrecognition;

import cn.iocoder.yudao.module.icbc.service.cardrecognition.config.CardRecognitionConfigService;
import cn.iocoder.yudao.module.icbc.service.cardrecognition.config.CardRecognitionEffectiveConfig;
import cn.iocoder.yudao.module.icbc.service.cardrecognition.tencent.TencentCardRecognition;
import cn.iocoder.yudao.module.icbc.service.cardrecognition.tencent.TencentOcrClient;
import cn.iocoder.yudao.module.icbc.service.cardrecognition.tencent.TencentOcrTransport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 锁住「卡证识别端口**永远只有一枚常驻 Bean**」（#93 引入，机制在 #103 收敛）。
 *
 * <p>#93 的 stub / tencent 是启动期二选一，本测试原来守的是「不能同时有两个候选」。 #103 把端口
 * 收敛成一枚常驻实现（{@link TencentCardRecognition}），供应商改在**调用时**由配置判定，
 * 所以这里守两条新判据：
 * <ul>
 *   <li>无论配置如何，{@link CardRecognitionPort} 都只有一枚、就是那个常驻实现；</li>
 *   <li>{@code provider=stub} 或 {@code provider=tencent} 未配密钥时，识别返回空结果且**不触网**。</li>
 * </ul>
 * 触网会在假 transport 上直接抛断言错误，所以「没配齐也不许打厂商」是硬钉住的。
 */
public class CardRecognitionPortResidentBeanTest {

    private final CardRecognitionConfigService configService = mock(CardRecognitionConfigService.class);

    /** 任何出站请求都判失败：未启用 / 未配齐时不该有网络调用。 */
    private final TencentOcrTransport transport = request -> {
        throw new AssertionError("未启用 / 未配齐时不许触网：" + request.url());
    };

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(CardRecognitionConfigService.class, () -> configService)
            .withBean(TencentOcrTransport.class, () -> transport)
            .withUserConfiguration(TencentOcrClient.class, TencentCardRecognition.class);

    @Test
    public void testAlwaysSingleResidentBean() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(CardRecognitionPort.class);
            assertThat(context.getBean(CardRecognitionPort.class)).isInstanceOf(TencentCardRecognition.class);
        });
    }

    @Test
    public void testProviderStub_recognizeReturnsEmpty() {
        when(configService.resolveEffectiveConfig()).thenReturn(configured("stub", null, null));
        contextRunner.run(context -> {
            CardRecognitionPort port = context.getBean(CardRecognitionPort.class);
            assertThat(port.recognizeIdCardFront("QUJD").getName()).isNull();
            assertThat(port.recognizeIdCardBack("QUJD").getIdValidityPeriod()).isNull();
            assertThat(port.recognizeBankCard("QUJD").getBankCardNo()).isNull();
        });
    }

    @Test
    public void testProviderTencentWithoutSecret_recognizeReturnsEmpty() {
        when(configService.resolveEffectiveConfig()).thenReturn(configured("tencent", null, null));
        contextRunner.run(context -> {
            CardRecognitionPort port = context.getBean(CardRecognitionPort.class);
            assertThat(port.recognizeIdCardFront("QUJD").getName()).isNull();
            assertThat(port.recognizeBankCard("QUJD").getBankCardNo()).isNull();
        });
    }

    private static CardRecognitionEffectiveConfig configured(String provider, String secretId, String secretKey) {
        return CardRecognitionEffectiveConfig.builder()
                .provider(provider).secretId(secretId).secretKey(secretKey)
                .region("ap-guangzhou").endpoint("ocr.tencentcloudapi.com").timeout(10000)
                .build();
    }

}
