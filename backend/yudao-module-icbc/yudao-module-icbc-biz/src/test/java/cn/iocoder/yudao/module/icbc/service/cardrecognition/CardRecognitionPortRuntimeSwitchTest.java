package cn.iocoder.yudao.module.icbc.service.cardrecognition;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.icbc.UnitTestConfiguration;
import cn.iocoder.yudao.module.icbc.controller.admin.cardrecognition.vo.CardRecognitionConfigSaveReqVO;
import cn.iocoder.yudao.module.icbc.service.cardrecognition.config.CardRecognitionConfigService;
import cn.iocoder.yudao.module.icbc.service.cardrecognition.config.CardRecognitionConfigServiceImpl;
import cn.iocoder.yudao.module.icbc.service.cardrecognition.tencent.TencentOcrTransport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * 本票唯一真正困难的断言（#103）：**保存配置后不重启，识别端口立刻改用刚存的 provider**。
 *
 * <p>#93 把 stub / tencent 做成启动期 {@code @ConditionalOnProperty} 二选一，后台改不动它。
 * 这条测试用真实 DB（{@code icbc_card_recognition_config}）+ 真实配置 Service + 真实常驻端口，
 * 只把厂商 HTTP 换成假的 {@link TencentOcrTransport}：先看默认（stub）不触网，保存 tencent + 密钥后
 * **同一枚 Bean** 立刻走到真实实现，再切回 stub 又立刻回到空结果。用「Bean 名字对了」代替它不算数。
 */
@Import({CardRecognitionConfigServiceImpl.class, UnitTestConfiguration.class})
@Sql(scripts = "/sql/create_tables.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Transactional
public class CardRecognitionPortRuntimeSwitchTest extends BaseDbUnitTest {

    private static final String OCR_RESPONSE =
            "{\"Response\":{\"Name\":\"张三\",\"IdNum\":\"110101199001011234\"}}";

    @Resource
    private CardRecognitionPort cardRecognitionPort;
    @Resource
    private CardRecognitionConfigService cardRecognitionConfigService;

    @MockBean
    private TencentOcrTransport tencentOcrTransport;

    @Test
    public void testSavedProviderTakesEffectWithoutRestart() {
        given(tencentOcrTransport.post(any()))
                .willReturn(new TencentOcrTransport.Result(200, OCR_RESPONSE));

        // 1) 未保存配置：provider 回落到 yaml / env 的默认 stub —— 返回空结果，绝不触网
        assertNull(cardRecognitionPort.recognizeIdCardFront("QUJD").getName());
        verify(tencentOcrTransport, never()).post(any());

        // 2) 保存 tencent + 密钥：**不重启**，同一枚 Bean 的下一次调用就走真实实现
        CardRecognitionConfigSaveReqVO tencent = new CardRecognitionConfigSaveReqVO();
        tencent.setProvider("tencent");
        tencent.setSecretId("secret-id");
        tencent.setSecretKey("secret-key");
        cardRecognitionConfigService.saveConfig(tencent);

        CardRecognitionPort.IdCardFront result = cardRecognitionPort.recognizeIdCardFront("QUJD");
        assertEquals("张三", result.getName());
        verify(tencentOcrTransport, times(1)).post(any());

        // 3) 切回 stub：立刻回到空结果，不再触网（保存即生效的两个方向都钉住）
        CardRecognitionConfigSaveReqVO stub = new CardRecognitionConfigSaveReqVO();
        stub.setProvider("stub");
        cardRecognitionConfigService.saveConfig(stub);

        assertNull(cardRecognitionPort.recognizeIdCardFront("QUJD").getName());
        verify(tencentOcrTransport, times(1)).post(any());
    }

}
