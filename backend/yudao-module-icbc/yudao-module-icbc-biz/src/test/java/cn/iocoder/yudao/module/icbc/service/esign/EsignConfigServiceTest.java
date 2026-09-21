package cn.iocoder.yudao.module.icbc.service.esign;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.icbc.UnitTestConfiguration;
import cn.iocoder.yudao.module.icbc.controller.admin.esign.vo.EsignConfigRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.esign.vo.EsignConfigSaveReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.esign.IcbcEsignConfigDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.esign.IcbcEsignConfigMapper;
import cn.iocoder.yudao.module.icbc.service.esign.impl.EsignConfigServiceImpl;
import cn.iocoder.yudao.module.system.api.tenant.TenantApi;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link EsignConfigServiceImpl} 的单元测试（#92，ADR 0036）。
 *
 * <p>覆盖两条承诺：平台参数是**配置动作**（齐备判据只有一处，缺一项就答未配置），
 * 密钥**只写不读**（查询不回明文，留空不改动既有值）。
 */
@Import({EsignConfigServiceImpl.class, UnitTestConfiguration.class})
@Sql(scripts = "/sql/create_tables.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Transactional
public class EsignConfigServiceTest extends BaseDbUnitTest {

    @Resource
    private EsignConfigService esignConfigService;
    @Resource
    private IcbcEsignConfigMapper esignConfigMapper;

    @MockBean
    private TenantApi tenantApi;

    @Test
    public void testNotConfigured_missingFieldsReported() {
        EsignConfigRespVO resp = esignConfigService.getConfig();
        assertFalse(resp.getConfigured());
        assertFalse(resp.getSecretIdConfigured());
        assertFalse(resp.getSecretKeyConfigured());
        assertFalse(resp.getCallbackSignKeyConfigured());
        assertTrue(resp.getMissingFields().contains("应用标识"));
        assertFalse(esignConfigService.isPlatformConfigured());
        assertNull(esignConfigService.getRawConfig());
    }

    @Test
    public void testPartialConfig_notConfigured() {
        EsignConfigSaveReqVO req = fullConfig();
        req.setCallbackSignKey(null); // 回调验签密钥是必填项，缺它就不能收通知
        esignConfigService.saveConfig(req);

        EsignConfigRespVO resp = esignConfigService.getConfig();
        assertFalse(resp.getConfigured());
        assertTrue(resp.getMissingFields().contains("回调验签密钥"));
        assertFalse(esignConfigService.isPlatformConfigured());
    }

    @Test
    public void testSaveThenGet_secretsOnlyWrittenNeverRead() {
        esignConfigService.saveConfig(fullConfig());

        EsignConfigRespVO resp = esignConfigService.getConfig();
        assertTrue(resp.getConfigured());
        assertTrue(resp.getSecretIdConfigured());
        assertTrue(resp.getSecretKeyConfigured());
        assertTrue(resp.getCallbackSignKeyConfigured());
        assertEquals("TEST", resp.getEnvironment());
        assertEquals("https://ess.tencentcloudapi.com", resp.getApiEndpoint());
        assertEquals(0, resp.getMissingFields().size());
        assertTrue(esignConfigService.isPlatformConfigured());

        // 密钥落库、但响应里拿不到明文（响应 VO 根本没有这几个字段）
        IcbcEsignConfigDO raw = esignConfigMapper.selectConfig();
        assertEquals("secret-id-raw", raw.getSecretId());
        assertEquals("secret-key-raw", raw.getSecretKey());
        assertEquals("sign-key-raw", raw.getCallbackSignKey());
    }

    @Test
    public void testSaveBlankSecret_keepsExistingValue() {
        esignConfigService.saveConfig(fullConfig());

        // 只改 endpoint，密钥留空：不能把已配置的密钥清掉
        EsignConfigSaveReqVO update = new EsignConfigSaveReqVO();
        update.setEnvironment("PROD");
        update.setApiEndpoint("https://ess.example.com/api");
        esignConfigService.saveConfig(update);

        IcbcEsignConfigDO raw = esignConfigMapper.selectConfig();
        assertEquals("PROD", raw.getEnvironment());
        assertEquals("https://ess.example.com/api", raw.getApiEndpoint());
        assertEquals("secret-id-raw", raw.getSecretId());
        assertEquals("secret-key-raw", raw.getSecretKey());
        assertEquals("sign-key-raw", raw.getCallbackSignKey());
        // 平台参数仍齐备（密钥没被清掉）
        assertTrue(esignConfigService.isPlatformConfigured());
    }

    private EsignConfigSaveReqVO fullConfig() {
        EsignConfigSaveReqVO req = new EsignConfigSaveReqVO();
        req.setEnvironment("TEST");
        req.setApiEndpoint("https://ess.tencentcloudapi.com");
        req.setConsoleEndpoint("https://ess.tencent.cn");
        req.setAppId("app-123");
        req.setSecretId("secret-id-raw");
        req.setSecretKey("secret-key-raw");
        req.setCallbackUrl("https://yiyoubao.example.com/admin-api/icbc/esign/callback/notify");
        req.setCallbackSignKey("sign-key-raw");
        req.setSignLinkChannel("H5");
        req.setAgreementTemplateId("tpl-agreement");
        req.setNoticeTemplateId("tpl-notice");
        return req;
    }

}
