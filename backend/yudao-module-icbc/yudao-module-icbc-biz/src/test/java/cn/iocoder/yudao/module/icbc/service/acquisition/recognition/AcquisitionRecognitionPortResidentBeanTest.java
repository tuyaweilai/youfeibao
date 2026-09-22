package cn.iocoder.yudao.module.icbc.service.acquisition.recognition;

import cn.iocoder.yudao.module.icbc.service.acquisition.recognition.tencent.TencentAcquisitionRecognition;
import cn.iocoder.yudao.module.icbc.service.cardrecognition.config.CardRecognitionConfigService;
import cn.iocoder.yudao.module.icbc.service.cardrecognition.config.CardRecognitionEffectiveConfig;
import cn.iocoder.yudao.module.icbc.service.cardrecognition.tencent.TencentOcrClient;
import cn.iocoder.yudao.module.icbc.service.cardrecognition.tencent.TencentOcrTransport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 锁住「收购现场识别端口**永远只有一枚常驻 Bean**」（照 {@code CardRecognitionPortResidentBeanTest}，#112）。
 *
 * <p>ADR 0013 说的「默认实现返回空」在这里的含义变了：#93 时代它是「一个 stub Bean」，现在是
 * **同一枚常驻实现按运行期配置判定供应商**——{@code provider=stub} 时它自己返回空。
 * 所以这里守两条：
 * <ul>
 *   <li>无论配置如何，{@link AcquisitionRecognitionPort} 都只有一枚、就是那个常驻实现
 *       （再加一个 stub 实现会让注入直接炸 {@code NoUniqueBeanDefinitionException}）；</li>
 *   <li>{@code provider=stub} 或 {@code provider=tencent} 未配密钥时，识别返回空结果且**不触网**。</li>
 * </ul>
 * 触网会在假 transport 上直接抛断言错误，所以「没配齐也不许打厂商」是硬钉住的。
 */
public class AcquisitionRecognitionPortResidentBeanTest {

    private final CardRecognitionConfigService configService = mock(CardRecognitionConfigService.class);

    /** 任何出站请求都判失败：未启用 / 未配齐时不该有网络调用。 */
    private final TencentOcrTransport transport = request -> {
        throw new AssertionError("未启用 / 未配齐时不许触网：" + request.url());
    };

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(CardRecognitionConfigService.class, () -> configService)
            .withBean(TencentOcrTransport.class, () -> transport)
            .withUserConfiguration(TencentOcrClient.class, TencentAcquisitionRecognition.class);

    @Test
    public void testAlwaysSingleResidentBean() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(AcquisitionRecognitionPort.class);
            assertThat(context.getBean(AcquisitionRecognitionPort.class))
                    .isInstanceOf(TencentAcquisitionRecognition.class);
        });
    }

    @Test
    public void testProviderStub_recognizeReturnsEmpty() {
        when(configService.resolveEffectiveConfig()).thenReturn(configured("stub", null, null));
        contextRunner.run(context -> {
            AcquisitionRecognitionPort port = context.getBean(AcquisitionRecognitionPort.class);
            assertThat(port.recognizePlate("QUJD").getPlateNo()).isNull();
            assertThat(port.recognizeWeightTicket("QUJD").getWeightTicketNo()).isNull();
        });
    }

    @Test
    public void testProviderTencentWithoutSecret_recognizeReturnsEmpty() {
        when(configService.resolveEffectiveConfig()).thenReturn(configured("tencent", null, null));
        contextRunner.run(context -> {
            AcquisitionRecognitionPort port = context.getBean(AcquisitionRecognitionPort.class);
            assertThat(port.recognizePlate("QUJD").getPlateNo()).isNull();
        });
    }

    private static CardRecognitionEffectiveConfig configured(String provider, String secretId, String secretKey) {
        return CardRecognitionEffectiveConfig.builder()
                .provider(provider).secretId(secretId).secretKey(secretKey)
                .region("ap-guangzhou").endpoint("ocr.tencentcloudapi.com").timeout(10000)
                .build();
    }

}
