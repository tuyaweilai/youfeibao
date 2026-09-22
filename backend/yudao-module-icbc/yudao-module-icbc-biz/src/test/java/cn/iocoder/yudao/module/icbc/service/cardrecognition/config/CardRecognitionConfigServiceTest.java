package cn.iocoder.yudao.module.icbc.service.cardrecognition.config;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.icbc.UnitTestConfiguration;
import cn.iocoder.yudao.module.icbc.controller.admin.cardrecognition.vo.CardRecognitionCheckReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.cardrecognition.vo.CardRecognitionCheckRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.cardrecognition.vo.CardRecognitionConfigRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.cardrecognition.vo.CardRecognitionConfigSaveReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.cardrecognition.IcbcCardRecognitionConfigDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.cardrecognition.IcbcCardRecognitionConfigMapper;
import cn.iocoder.yudao.module.icbc.service.cardrecognition.tencent.TencentOcrTransport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.CARD_RECOGNITION_CHECK_NOT_CONFIGURED;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

/**
 * {@link CardRecognitionConfigServiceImpl} 的单元测试（#103，ADR 0037）。
 *
 * <p>覆盖四条承诺：供应商 / 密钥的**运行期生效与回落**（DB 优先、空则配置文件）、密钥**只写不读**
 * （查询不回明文、留空不改动）、`missingFields` 逐项、以及连通性自检的**四类结果**与「分类落库」。
 * 厂商 HTTP 用假的 {@link TencentOcrTransport}，不触网。
 */
@Import({CardRecognitionConfigServiceImpl.class, UnitTestConfiguration.class})
@Sql(scripts = "/sql/create_tables.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Transactional
public class CardRecognitionConfigServiceTest extends BaseDbUnitTest {

    private static final String AUTH_FAILURE_BODY = "{\"Response\":{"
            + "\"Error\":{\"Code\":\"AuthFailure.SignatureFailure\",\"Message\":\"签名验证失败：SecretKey 不正确\"},"
            + "\"RequestId\":\"req-auth\"}}";
    private static final String VENDOR_ERROR_BODY = "{\"Response\":{"
            + "\"Error\":{\"Code\":\"FailedOperation.ImageDecodeFailed\",\"Message\":\"图片解码失败\"},"
            + "\"RequestId\":\"req-vendor\"}}";
    private static final String SERVICE_NOT_OPENED_BODY = "{\"Response\":{"
            + "\"Error\":{\"Code\":\"FailedOperation.ServiceNotOpened\",\"Message\":\"服务未开通\"},"
            + "\"RequestId\":\"req-not-opened\"}}";
    private static final String OK_BODY = "{\"Response\":{\"Name\":\"张三\",\"RequestId\":\"req-ok\"}}";

    @Resource
    private CardRecognitionConfigService cardRecognitionConfigService;
    @Resource
    private IcbcCardRecognitionConfigMapper cardRecognitionConfigMapper;

    @MockBean
    private TencentOcrTransport tencentOcrTransport;

    // ==================== 配置回显与回落 ====================

    @Test
    public void testNotConfigured_providerFallsBackToStub() {
        CardRecognitionConfigRespVO resp = cardRecognitionConfigService.getConfig();

        assertEquals("stub", resp.getProvider());
        assertTrue(resp.getConfigFileFields().contains("供应商"));
        assertFalse(resp.getConfigured());
        assertFalse(resp.getSecretIdConfigured());
        assertFalse(resp.getSecretKeyConfigured());
        assertTrue(resp.getMissingFields().isEmpty(), "stub 不是「缺项」，它是「未启用」");
        // 地域 / endpoint / 超时的默认值来自配置文件
        assertEquals("ap-guangzhou", resp.getRegion());
        assertEquals("ocr.tencentcloudapi.com", resp.getEndpoint());
        assertEquals(10000, resp.getTimeout());
    }

    @Test
    public void testSaveTencentWithoutSecret_reportsMissing() {
        CardRecognitionConfigSaveReqVO req = new CardRecognitionConfigSaveReqVO();
        req.setProvider("tencent");
        cardRecognitionConfigService.saveConfig(req);

        CardRecognitionConfigRespVO resp = cardRecognitionConfigService.getConfig();
        assertEquals("tencent", resp.getProvider());
        assertFalse(resp.getConfigured(), "缺密钥时不算启用");
        assertTrue(resp.getMissingFields().contains("SecretId"));
        assertTrue(resp.getMissingFields().contains("SecretKey"));
    }

    @Test
    public void testSaveThenGet_secretsOnlyWrittenNeverRead() {
        cardRecognitionConfigService.saveConfig(fullTencent());

        CardRecognitionConfigRespVO resp = cardRecognitionConfigService.getConfig();
        assertTrue(resp.getConfigured());
        assertEquals("tencent", resp.getProvider());
        assertFalse(resp.getConfigFileFields().contains("供应商"), "DB 有值就不该再标「来自配置文件」");
        assertTrue(resp.getSecretIdConfigured());
        assertTrue(resp.getSecretKeyConfigured());
        assertTrue(resp.getMissingFields().isEmpty());

        // 密钥落库、但响应里拿不到明文（响应 VO 根本没有 secret 字段）
        IcbcCardRecognitionConfigDO raw = cardRecognitionConfigMapper.selectConfig();
        assertEquals("secret-id-raw", raw.getSecretId());
        assertEquals("secret-key-raw", raw.getSecretKey());
    }

    @Test
    public void testSaveBlank_keepsExistingValue() {
        cardRecognitionConfigService.saveConfig(fullTencent());

        // 只改 endpoint：密钥 / 供应商 / 地域留空，不能把已配置的值清掉
        CardRecognitionConfigSaveReqVO update = new CardRecognitionConfigSaveReqVO();
        update.setEndpoint("ocr.example.com");
        cardRecognitionConfigService.saveConfig(update);

        IcbcCardRecognitionConfigDO raw = cardRecognitionConfigMapper.selectConfig();
        assertEquals("tencent", raw.getProvider());
        assertEquals("ocr.example.com", raw.getEndpoint());
        assertEquals("secret-id-raw", raw.getSecretId());
        assertEquals("secret-key-raw", raw.getSecretKey());
        assertTrue(cardRecognitionConfigService.getConfig().getConfigured());
    }

    // ==================== 连通性自检：四类结果 ====================

    @Test
    public void testCheck_authFailure_classifiedAndPersisted() {
        // 审评观察 3：只有用**已存配置**的密钥自检才落库；先保存、再空请求
        cardRecognitionConfigService.saveConfig(fullTencent());
        given(tencentOcrTransport.post(any()))
                .willReturn(new TencentOcrTransport.Result(200, AUTH_FAILURE_BODY));

        CardRecognitionCheckRespVO resp = cardRecognitionConfigService.checkConnectivity(
                new CardRecognitionCheckReqVO());

        assertFalse(resp.getOk());
        assertEquals("AUTH_FAILED", resp.getResult());
        assertTrue(resp.getPersisted());
        assertNotNull(resp.getCheckTime());
        IcbcCardRecognitionConfigDO raw = cardRecognitionConfigMapper.selectConfig();
        assertEquals("AUTH_FAILED", raw.getLastCheckResult());
        assertNotNull(raw.getLastCheckTime());
    }

    @Test
    public void testCheck_vendorBusinessError_stillPassesAuth() {
        // 1x1 占位图必然在识别阶段报业务错：它不是密钥问题，页面按「鉴权通过」处理
        cardRecognitionConfigService.saveConfig(fullTencent());
        given(tencentOcrTransport.post(any()))
                .willReturn(new TencentOcrTransport.Result(200, VENDOR_ERROR_BODY));

        CardRecognitionCheckRespVO resp = cardRecognitionConfigService.checkConnectivity(
                new CardRecognitionCheckReqVO());

        assertTrue(resp.getOk());
        assertEquals("VENDOR_ERROR", resp.getResult());
        assertEquals("VENDOR_ERROR", cardRecognitionConfigMapper.selectConfig().getLastCheckResult());
    }

    @Test
    public void testCheck_ok() {
        cardRecognitionConfigService.saveConfig(fullTencent());
        given(tencentOcrTransport.post(any()))
                .willReturn(new TencentOcrTransport.Result(200, OK_BODY));

        CardRecognitionCheckRespVO resp = cardRecognitionConfigService.checkConnectivity(
                new CardRecognitionCheckReqVO());

        assertTrue(resp.getOk());
        assertEquals("OK", resp.getResult());
    }

    @Test
    public void testCheck_networkFailure() {
        cardRecognitionConfigService.saveConfig(fullTencent());
        given(tencentOcrTransport.post(any()))
                .willThrow(new RuntimeException("connect timed out"));

        CardRecognitionCheckRespVO resp = cardRecognitionConfigService.checkConnectivity(
                new CardRecognitionCheckReqVO());

        assertFalse(resp.getOk());
        assertEquals("NETWORK", resp.getResult());
        assertEquals("NETWORK", cardRecognitionConfigMapper.selectConfig().getLastCheckResult());
    }

    @Test
    public void testCheck_requestSecretUsedBeforeSave_notPersisted() {
        // 还没保存过配置，请求体里带密钥：先验证、再保存能成立
        given(tencentOcrTransport.post(any()))
                .willReturn(new TencentOcrTransport.Result(200, OK_BODY));
        CardRecognitionCheckRespVO resp = cardRecognitionConfigService.checkConnectivity(
                requestWithSecret());

        assertTrue(resp.getOk());
        // #112 起自检探两路（身份证 + 车牌），所以是两次调用而不是一次
        verify(tencentOcrTransport, atLeastOnce()).post(any());
        // 审评观察 3：未保存的密钥只是「临时凭据试通」，不是「已存配置验证通过」
        assertFalse(resp.getPersisted());
        assertNull(cardRecognitionConfigMapper.selectConfig(), "临时凭据的结果不落库");
    }

    @Test
    public void testCheck_requestBlank_usesStoredSecret() {
        // 审评观察 4：「请求留空 → 用库里已存密钥」这条分支此前没有测试
        cardRecognitionConfigService.saveConfig(fullTencent());
        given(tencentOcrTransport.post(any()))
                .willReturn(new TencentOcrTransport.Result(200, OK_BODY));

        CardRecognitionCheckRespVO resp = cardRecognitionConfigService.checkConnectivity(
                new CardRecognitionCheckReqVO());

        assertTrue(resp.getOk(), "请求留空时必须用已存密钥，否则会因 hasCredentials=false 直接抛未配置");
        assertTrue(resp.getPersisted());
        verify(tencentOcrTransport, atLeastOnce()).post(any());
    }

    @Test
    public void testCheck_plateActionNotOpened_isHardFailure() {
        // #112：腾讯云的识别接口按接口开通，车牌那一路没开时报 FailedOperation.ServiceNotOpened。
        // 它是硬失败，不能被当成「鉴权已通过」——否则自检通过、现场第一次拍车头照才失败。
        cardRecognitionConfigService.saveConfig(fullTencent());
        given(tencentOcrTransport.post(any()))
                .willReturn(new TencentOcrTransport.Result(200, OK_BODY))
                .willReturn(new TencentOcrTransport.Result(200, SERVICE_NOT_OPENED_BODY));

        CardRecognitionCheckRespVO resp = cardRecognitionConfigService.checkConnectivity(
                new CardRecognitionCheckReqVO());

        assertFalse(resp.getOk());
        assertEquals("SERVICE_NOT_OPENED", resp.getResult());
        assertEquals("SERVICE_NOT_OPENED", cardRecognitionConfigMapper.selectConfig().getLastCheckResult());
    }

    @Test
    public void testCheck_noSecretAnywhere_throws() {
        ServiceException exception = assertThrows(ServiceException.class,
                () -> cardRecognitionConfigService.checkConnectivity(new CardRecognitionCheckReqVO()));
        assertEquals(CARD_RECOGNITION_CHECK_NOT_CONFIGURED.getCode(), exception.getCode());
    }

    // ==================== 辅助 ====================

    private CardRecognitionConfigSaveReqVO fullTencent() {
        CardRecognitionConfigSaveReqVO req = new CardRecognitionConfigSaveReqVO();
        req.setProvider("tencent");
        req.setSecretId("secret-id-raw");
        req.setSecretKey("secret-key-raw");
        req.setRegion("ap-guangzhou");
        req.setEndpoint("ocr.tencentcloudapi.com");
        req.setTimeout(10000);
        req.setRemark("联调");
        return req;
    }

    private CardRecognitionCheckReqVO requestWithSecret() {
        CardRecognitionCheckReqVO req = new CardRecognitionCheckReqVO();
        req.setSecretId("secret-id-raw");
        req.setSecretKey("secret-key-raw");
        return req;
    }

}
