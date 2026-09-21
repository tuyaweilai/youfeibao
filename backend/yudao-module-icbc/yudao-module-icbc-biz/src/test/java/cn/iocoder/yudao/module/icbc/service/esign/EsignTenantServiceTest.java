package cn.iocoder.yudao.module.icbc.service.esign;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.icbc.UnitTestConfiguration;
import cn.iocoder.yudao.module.icbc.controller.admin.esign.vo.EsignActivateReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.esign.vo.EsignConfigSaveReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.esign.vo.EsignOpenConsoleRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.esign.vo.EsignTenantStatusRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.esign.vo.PlatformEsignQuotaSaveReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.esign.vo.PlatformEsignTenantRespVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.esign.IcbcEsignTenantDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.esign.IcbcEsignTenantMapper;
import cn.iocoder.yudao.module.icbc.enums.EsignActivationStatusEnum;
import cn.iocoder.yudao.module.icbc.service.esign.impl.EsignConfigServiceImpl;
import cn.iocoder.yudao.module.icbc.service.esign.impl.EsignTenantServiceImpl;
import cn.iocoder.yudao.module.system.api.tenant.TenantApi;
import cn.iocoder.yudao.test.icbc.IcbcTenantTestConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * {@link EsignTenantServiceImpl} 的单元测试（#92，ADR 0036）。
 *
 * <p>覆盖：子客编号稳定 / 不可重复、开通链接是一次性的且要先配齐平台参数、
 * 「已激活 + 印章就位」两件事都要、按子客编号反查租户、平台运营能看到各租户状态与额度。
 *
 * <p>导入 {@link IcbcTenantTestConfiguration} 真正打开租户拦截器，验证租户隔离是**真的**在生效，
 * 而不是测试里恰好只有一行数据。
 */
@Import({EsignTenantServiceImpl.class, EsignConfigServiceImpl.class, IcbcTenantTestConfiguration.class,
        UnitTestConfiguration.class})
@Sql(scripts = "/sql/create_tables.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Transactional
public class EsignTenantServiceTest extends BaseDbUnitTest {

    private static final Long TENANT_ID = 1L;

    @Resource
    private EsignTenantService esignTenantService;
    @Resource
    private EsignConfigService esignConfigService;
    @Resource
    private IcbcEsignTenantMapper esignTenantMapper;

    @MockBean
    private TenantApi tenantApi;

    @AfterEach
    public void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    public void testSubCustomerNo_stableAndUniquePerTenant() {
        TenantContextHolder.setTenantId(1L);
        IcbcEsignTenantDO first = esignTenantService.getOrCreateCurrent();
        IcbcEsignTenantDO again = esignTenantService.getOrCreateCurrent();
        assertEquals("ES0000000001", first.getSubCustomerNo());
        // 不可变：再次取到的是同一枚编号
        assertEquals(first.getSubCustomerNo(), again.getSubCustomerNo());
        assertEquals(EsignActivationStatusEnum.NOT_OPENED.getStatus(), first.getActivationStatus());

        TenantContextHolder.setTenantId(2L);
        assertEquals("ES0000000002", esignTenantService.getOrCreateCurrent().getSubCustomerNo());
    }

    @Test
    public void testOpenConsole_requiresPlatformConfig() {
        TenantContextHolder.setTenantId(TENANT_ID);
        assertServiceException(() -> esignTenantService.openConsole(), ESIGN_CONFIG_INCOMPLETE,
                "环境、服务端接口地址、控制台地址、应用标识、应用密钥 ID、应用密钥、回调地址、回调验签密钥");
    }

    @Test
    public void testOpenConsole_oneTimeLinkAndMarksAuthenticating() {
        savePlatformConfig();
        TenantContextHolder.setTenantId(TENANT_ID);

        EsignOpenConsoleRespVO first = esignTenantService.openConsole();
        assertEquals("ES0000000001", first.getSubCustomerNo());
        assertEquals(EsignActivationStatusEnum.AUTHENTICATING.getStatus(), first.getActivationStatus());
        assertNotNull(first.getExpiresTime());
        assertTrue(first.getLink().contains("subCustomerNo=ES0000000001"));
        assertTrue(first.getLink().contains("appId=app-123"));
        assertTrue(first.getLink().startsWith("https://ess.tencent.cn/enterprise/open"));

        // 一次性：再次开通换新链接，旧链接作废（令牌不同）
        EsignOpenConsoleRespVO second = esignTenantService.openConsole();
        assertNotEquals(first.getLink(), second.getLink());

        // 仍未激活、印章未就位
        assertFalse(esignTenantService.isTenantActivated());
        EsignTenantStatusRespVO status = esignTenantService.getStatus();
        assertEquals(EsignActivationStatusEnum.AUTHENTICATING.getStatus(), status.getActivationStatus());
        assertFalse(status.getSealReady());
    }

    @Test
    public void testActivate_requiresSealNo() {
        TenantContextHolder.setTenantId(TENANT_ID);
        EsignActivateReqVO reqVO = new EsignActivateReqVO();
        reqVO.setOperatorNo("OP1001");
        assertServiceException(() -> esignTenantService.activate(reqVO), ESIGN_SEAL_NO_REQUIRED);
    }

    @Test
    public void testActivate_setsSealReadyAndActivated() {
        savePlatformConfig();
        TenantContextHolder.setTenantId(TENANT_ID);
        EsignOpenConsoleRespVO opened = esignTenantService.openConsole();

        EsignActivateReqVO reqVO = new EsignActivateReqVO();
        reqVO.setConsoleToken(opened.getConsoleToken());
        reqVO.setOperatorNo("OP1001");
        reqVO.setSealNo("SEAL-001");
        esignTenantService.activate(reqVO);

        assertTrue(esignTenantService.isTenantActivated());
        EsignTenantStatusRespVO status = esignTenantService.getStatus();
        assertEquals(EsignActivationStatusEnum.ACTIVATED.getStatus(), status.getActivationStatus());
        assertTrue(status.getSealReady());
        assertEquals("SEAL-001", status.getSealNo());
        assertNotNull(status.getActivatedTime());
        assertTrue(status.getPlatformConfigured());
        // 激活即用掉令牌：同一枚令牌不能再用
        assertServiceException(() -> esignTenantService.activate(reqVO), ESIGN_CONSOLE_TOKEN_INVALID);
    }

    @Test
    public void testActivate_withoutOpenConsole_isRejected() {
        // 修 SPEC-2 之前的洞：NOT_OPENED 不能直接跳到 ACTIVATED
        TenantContextHolder.setTenantId(TENANT_ID);
        EsignActivateReqVO reqVO = new EsignActivateReqVO();
        reqVO.setConsoleToken("not-a-real-token");
        reqVO.setSealNo("SEAL-001");
        assertServiceException(() -> esignTenantService.activate(reqVO), ESIGN_CONSOLE_TOKEN_INVALID);
        assertFalse(esignTenantService.isTenantActivated());
    }

    @Test
    public void testActivate_oldTokenAfterReopen_isRejected() {
        savePlatformConfig();
        TenantContextHolder.setTenantId(TENANT_ID);
        EsignOpenConsoleRespVO first = esignTenantService.openConsole();
        // 再次开通：换新令牌，旧链接随即作废
        EsignOpenConsoleRespVO second = esignTenantService.openConsole();
        assertNotEquals(first.getConsoleToken(), second.getConsoleToken());

        EsignActivateReqVO reqVO = new EsignActivateReqVO();
        reqVO.setConsoleToken(first.getConsoleToken());
        reqVO.setSealNo("SEAL-001");
        assertServiceException(() -> esignTenantService.activate(reqVO), ESIGN_CONSOLE_TOKEN_INVALID);
        assertFalse(esignTenantService.isTenantActivated());
    }

    @Test
    public void testActivate_expiredToken_isRejected() {
        savePlatformConfig();
        TenantContextHolder.setTenantId(TENANT_ID);
        EsignOpenConsoleRespVO opened = esignTenantService.openConsole();

        // 把有效期拨到过去，模拟链接过期
        IcbcEsignTenantDO update = new IcbcEsignTenantDO();
        update.setId(esignTenantMapper.selectCurrent().getId());
        update.setConsoleTokenExpireTime(LocalDateTime.now().minusMinutes(1));
        esignTenantMapper.updateById(update);

        EsignActivateReqVO reqVO = new EsignActivateReqVO();
        reqVO.setConsoleToken(opened.getConsoleToken());
        reqVO.setSealNo("SEAL-001");
        assertServiceException(() -> esignTenantService.activate(reqVO), ESIGN_CONSOLE_TOKEN_EXPIRED);
        assertFalse(esignTenantService.isTenantActivated());
    }

    @Test
    public void testActivate_wrongStatus_isRejected() {
        savePlatformConfig();
        TenantContextHolder.setTenantId(TENANT_ID);
        EsignOpenConsoleRespVO opened = esignTenantService.openConsole();

        // 令牌有效、也未过期，但状态被拨回「未开通」——状态不对不能激活
        IcbcEsignTenantDO update = new IcbcEsignTenantDO();
        update.setId(esignTenantMapper.selectCurrent().getId());
        update.setActivationStatus(EsignActivationStatusEnum.NOT_OPENED.getStatus());
        esignTenantMapper.updateById(update);

        EsignActivateReqVO reqVO = new EsignActivateReqVO();
        reqVO.setConsoleToken(opened.getConsoleToken());
        reqVO.setSealNo("SEAL-001");
        assertServiceException(() -> esignTenantService.activate(reqVO), ESIGN_ACTIVATION_STATUS_INVALID, "未开通");
        assertFalse(esignTenantService.isTenantActivated());
    }

    @Test
    public void testQuota_updatedByPlatformOperator() {
        TenantContextHolder.setTenantId(TENANT_ID);
        esignTenantService.getOrCreateCurrent();

        esignTenantService.updateQuota(quotaReq(TENANT_ID, 100));

        EsignTenantStatusRespVO status = esignTenantService.getStatus();
        assertEquals(100, status.getContractQuota());
        assertEquals(0, status.getContractUsed());
        assertEquals(100, status.getRemainingQuota());
    }

    @Test
    public void testConsumeContract_incrementsUsedAndShrinksRemaining() {
        TenantContextHolder.setTenantId(TENANT_ID);
        esignTenantService.getOrCreateCurrent();
        esignTenantService.updateQuota(quotaReq(TENANT_ID, 2));

        // 发起成功一次记一次（#95）：原子自增，不覆盖别的字段
        esignTenantService.consumeContract();
        esignTenantService.consumeContract();

        EsignTenantStatusRespVO status = esignTenantService.getStatus();
        assertEquals(2, status.getContractUsed());
        assertEquals(0, status.getRemainingQuota(), "已用不得把剩余算成负数");
    }

    @Test
    public void testResolveTenantIdBySubCustomerNo() {
        TenantContextHolder.setTenantId(1L);
        esignTenantService.getOrCreateCurrent();
        TenantContextHolder.setTenantId(2L);
        esignTenantService.getOrCreateCurrent();

        // 回调没有租户上下文，靠子客编号跨租户反查
        TenantContextHolder.clear();
        assertEquals(1L, esignTenantService.resolveTenantIdBySubCustomerNo("ES0000000001"));
        assertEquals(2L, esignTenantService.resolveTenantIdBySubCustomerNo("ES0000000002"));
        assertNull(esignTenantService.resolveTenantIdBySubCustomerNo("ES9999999999"));
        assertNull(esignTenantService.resolveTenantIdBySubCustomerNo(null));
    }

    @Test
    public void testListPlatformTenants_showsActivatedAndNotOpened() {
        when(tenantApi.getTenantIdList()).thenReturn(List.of(1L, 2L, 3L));
        when(tenantApi.getTenantName(1L)).thenReturn("甲回收");
        when(tenantApi.getTenantName(2L)).thenReturn("乙回收");
        when(tenantApi.getTenantName(3L)).thenReturn("丙回收");

        // 租户 1 已激活 + 印章就位；租户 2 只开通过；租户 3 没碰过
        savePlatformConfig();
        TenantContextHolder.setTenantId(1L);
        EsignOpenConsoleRespVO opened = esignTenantService.openConsole();
        EsignActivateReqVO reqVO = new EsignActivateReqVO();
        reqVO.setConsoleToken(opened.getConsoleToken());
        reqVO.setSealNo("SEAL-001");
        esignTenantService.activate(reqVO);
        TenantContextHolder.setTenantId(2L);
        esignTenantService.openConsole();

        TenantContextHolder.clear();
        List<PlatformEsignTenantRespVO> list = esignTenantService.listPlatformTenants();
        assertEquals(3, list.size());

        PlatformEsignTenantRespVO first = list.get(0);
        assertEquals(1L, first.getTenantId());
        assertEquals("甲回收", first.getTenantName());
        assertEquals(EsignActivationStatusEnum.ACTIVATED.getStatus(), first.getActivationStatus());
        assertTrue(first.getSealReady());

        assertEquals(EsignActivationStatusEnum.AUTHENTICATING.getStatus(), list.get(1).getActivationStatus());

        PlatformEsignTenantRespVO third = list.get(2);
        assertEquals(3L, third.getTenantId());
        assertEquals(EsignActivationStatusEnum.NOT_OPENED.getStatus(), third.getActivationStatus());
        assertFalse(third.getSealReady());
        assertNull(third.getSubCustomerNo());
        assertEquals(0, third.getContractQuota());
    }

    private void savePlatformConfig() {
        EsignConfigSaveReqVO req = new EsignConfigSaveReqVO();
        req.setEnvironment("TEST");
        req.setApiEndpoint("https://ess.tencentcloudapi.com");
        req.setConsoleEndpoint("https://ess.tencent.cn");
        req.setAppId("app-123");
        req.setSecretId("secret-id");
        req.setSecretKey("secret-key");
        req.setCallbackUrl("https://yiyoubao.example.com/admin-api/icbc/esign/callback/notify");
        req.setCallbackSignKey("sign-key");
        esignConfigService.saveConfig(req);
    }

    private PlatformEsignQuotaSaveReqVO quotaReq(Long tenantId, Integer quota) {
        PlatformEsignQuotaSaveReqVO req = new PlatformEsignQuotaSaveReqVO();
        req.setTenantId(tenantId);
        req.setContractQuota(quota);
        return req;
    }

}
