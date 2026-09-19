package cn.iocoder.yudao.module.icbc.service.sellerauth;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.module.icbc.UnitTestConfiguration;
import cn.iocoder.yudao.module.icbc.controller.admin.naturalperson.vo.NaturalPersonRegisterReqVO;
import cn.iocoder.yudao.module.icbc.controller.app.sellerauth.vo.SellerLoginRespVO;
import cn.iocoder.yudao.module.icbc.controller.app.sellerauth.vo.SellerSmsLoginReqVO;
import cn.iocoder.yudao.module.icbc.controller.app.sellerauth.vo.SellerSubjectRespVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.naturalperson.IcbcNaturalPersonDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payee.PayeeInfoDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.payee.PayeeInfoMapper;
import cn.iocoder.yudao.module.icbc.service.naturalperson.NaturalPersonService;
import cn.iocoder.yudao.module.icbc.service.sellerauth.impl.SellerAuthServiceImpl;
import cn.iocoder.yudao.module.member.api.user.MemberUserApi;
import cn.iocoder.yudao.module.member.api.user.dto.MemberUserRespDTO;
import cn.iocoder.yudao.module.system.api.oauth2.OAuth2TokenApi;
import cn.iocoder.yudao.module.system.api.oauth2.dto.OAuth2AccessTokenRespDTO;
import cn.iocoder.yudao.module.system.api.sms.SmsCodeApi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * {@link SellerAuthServiceImpl} 的单元测试。
 *
 * <p>断言 ADR 0017 的对外行为：登录凭证落在平台租户、身份锚点与凭证分离、绑定按手机号一致性校验
 * （不一致就拒绝、不合并）、解绑只动凭证。
 */
@Import({SellerAuthServiceImpl.class, UnitTestConfiguration.class})
@Transactional
@Rollback
public class SellerAuthServiceImplTest extends BaseDbUnitTest {

    private static final Long PLATFORM_TENANT_ID = 0L;
    private static final Long MEMBER_USER_ID = 9001L;
    private static final String MOBILE = "13800138000";

    @Resource
    private SellerAuthService sellerAuthService;

    @Resource
    private NaturalPersonService naturalPersonService;

    @Resource
    private PayeeInfoMapper payeeInfoMapper;

    @MockBean
    private SmsCodeApi smsCodeApi;
    @MockBean
    private OAuth2TokenApi oauth2TokenApi;
    @MockBean
    private MemberUserApi memberUserApi;

    @AfterEach
    public void tearDown() {
        TenantContextHolder.clear();
    }

    // ==================== 登录 ====================

    @Test
    public void testSmsLogin_issuesTokenAndListsBoundSubjects() {
        mockCredential(MOBILE, MEMBER_USER_ID);
        mockToken("TOKEN_1");

        SellerLoginRespVO resp = sellerAuthService.smsLogin(smsLoginReq(MOBILE, "1234"));

        assertEquals("TOKEN_1", resp.getAccessToken());
        assertNotNull(resp.getExpiresTime());
        assertTrue(resp.getSubjects().isEmpty());
        verify(smsCodeApi).useSmsCode(any());
    }

    @Test
    public void testSmsLogin_createsCredentialInPlatformTenantNotEnterpriseTenant() {
        // 他从某个回收企业的场站扫码进来，请求头带着那家企业的租户
        MemberUserRespDTO user = new MemberUserRespDTO();
        user.setId(MEMBER_USER_ID);
        user.setMobile(MOBILE);
        when(memberUserApi.createUserIfAbsent(eq(MOBILE), any(), any())).thenAnswer(invocation -> {
            // 建凭证时必须在平台租户下：否则同一个手机号会在每家企业各建一个
            assertEquals(PLATFORM_TENANT_ID, TenantContextHolder.getTenantId());
            return user;
        });
        when(memberUserApi.getUser(MEMBER_USER_ID)).thenReturn(user);
        mockToken("TOKEN_2");

        SellerLoginRespVO resp = TenantUtils.execute(7L, () -> sellerAuthService.smsLogin(smsLoginReq(MOBILE, "1234")));

        assertNotNull(resp.getAccessToken());
        verify(memberUserApi).createUserIfAbsent(eq(MOBILE), any(), any());
    }

    @Test
    public void testSendSmsCode_usesMemberLoginScene() {
        sellerAuthService.sendSmsCode(MOBILE);

        ArgumentCaptor<cn.iocoder.yudao.module.system.api.sms.dto.code.SmsCodeSendReqDTO> captor =
                ArgumentCaptor.forClass(cn.iocoder.yudao.module.system.api.sms.dto.code.SmsCodeSendReqDTO.class);
        verify(smsCodeApi).sendSmsCode(captor.capture());
        assertEquals(MOBILE, captor.getValue().getMobile());
        assertEquals(cn.iocoder.yudao.module.system.enums.sms.SmsSceneEnum.MEMBER_LOGIN.getScene(),
                captor.getValue().getScene());
    }

    @Test
    public void testLogout_removesAccessToken() {
        sellerAuthService.logout("TOKEN_3");

        verify(oauth2TokenApi).removeAccessToken("TOKEN_3");
    }

    // ==================== 绑定身份（确认时才注册） ====================

    @Test
    public void testBindSubject_bindsWhenMobileMatches() {
        mockCredential(MOBILE, MEMBER_USER_ID);
        PayeeInfoDO payee = insertPayeeInTenant(1L, "张三", "110101199001011234", MOBILE);
        IcbcNaturalPersonDO person = personOf(payee);

        SellerSubjectRespVO subject = TenantUtils.execute(1L, () -> {
            setLoginUser(MEMBER_USER_ID);
            return sellerAuthService.bindSubject(payee.getId());
        });

        assertEquals(person.getId(), subject.getNaturalPersonId());
        // 脱敏展示
        assertNotEquals(MOBILE, subject.getMobile());
        assertTrue(subject.getMobile().endsWith("8000"));
        assertTrue(naturalPersonService.isBoundToLogin(person.getId(), MEMBER_USER_ID));
    }

    @Test
    public void testBindSubject_rejectsDifferentMobileWithoutMerging() {
        mockCredential("13900139000", MEMBER_USER_ID);
        // 这个身份在别的回收企业已按另一个手机号建档
        PayeeInfoDO payee = insertPayeeInTenant(1L, "张三", "110101199001011234", MOBILE);

        assertServiceException(() -> TenantUtils.execute(1L, () -> {
            setLoginUser(MEMBER_USER_ID);
            sellerAuthService.bindSubject(payee.getId());
        }), NATURAL_PERSON_IDENTITY_TAKEN);

        // 没有被静默绑定，也没有把已有手机号改掉
        IcbcNaturalPersonDO person = personOf(payee);
        assertFalse(naturalPersonService.isBoundToLogin(person.getId(), MEMBER_USER_ID));
        assertEquals(MOBILE, person.getMobile());
    }

    @Test
    public void testBindSubject_unknownPayee() {
        mockCredential(MOBILE, MEMBER_USER_ID);

        assertServiceException(() -> TenantUtils.execute(1L, () -> {
            setLoginUser(MEMBER_USER_ID);
            sellerAuthService.bindSubject(999999L);
        }), PAYEE_NOT_EXISTS);
    }

    @Test
    public void testBindSubject_withoutStationTenantIsRejected() {
        mockCredential(MOBILE, MEMBER_USER_ID);
        PayeeInfoDO payee = insertPayeeInTenant(1L, "张三", "110101199001011234", MOBILE);

        // 没有租户（没带场站所属企业的 tenant-id）时必须直接拒绝，不能在没有租户条件下去读别人的档案
        TenantContextHolder.clear();
        assertServiceException(() -> {
            setLoginUser(MEMBER_USER_ID);
            sellerAuthService.bindSubject(payee.getId());
        }, SELLER_STATION_TENANT_REQUIRED);
    }

    @Test
    public void testAssertBound_rejectsSubjectOfAnotherLogin() {
        mockCredential(MOBILE, MEMBER_USER_ID);
        PayeeInfoDO payee = insertPayeeInTenant(1L, "张三", "110101199001011234", MOBILE);
        IcbcNaturalPersonDO person = personOf(payee);

        assertServiceException(() -> {
            setLoginUser(MEMBER_USER_ID);
            sellerAuthService.assertBound(person.getId());
        }, NATURAL_PERSON_NOT_BOUND_TO_LOGIN);

        // 绑定之后同一个主体就被认了
        naturalPersonService.bindLogin(person.getId(), MEMBER_USER_ID, "REGISTER", null);
        assertDoesNotThrow(() -> {
            setLoginUser(MEMBER_USER_ID);
            sellerAuthService.assertBound(person.getId());
        });
    }

    @Test
    public void testUnbindSubject_keepsIdentity() {
        PayeeInfoDO payee = insertPayeeInTenant(1L, "张三", "110101199001011234", MOBILE);
        IcbcNaturalPersonDO person = personOf(payee);
        naturalPersonService.bindLogin(person.getId(), MEMBER_USER_ID, "REGISTER", null);

        setLoginUser(MEMBER_USER_ID);
        sellerAuthService.unbindSubject(person.getId());

        assertFalse(naturalPersonService.isBoundToLogin(person.getId(), MEMBER_USER_ID));
        // 注销/解绑不等于删除交易：主体还在
        assertNotNull(naturalPersonService.getNaturalPerson(person.getId()));
    }

    // ==================== 助手 ====================

    private SellerSmsLoginReqVO smsLoginReq(String mobile, String code) {
        SellerSmsLoginReqVO reqVO = new SellerSmsLoginReqVO();
        reqVO.setMobile(mobile);
        reqVO.setCode(code);
        return reqVO;
    }

    private void mockCredential(String mobile, Long memberUserId) {
        MemberUserRespDTO user = new MemberUserRespDTO();
        user.setId(memberUserId);
        user.setMobile(mobile);
        when(memberUserApi.createUserIfAbsent(eq(mobile), any(), any())).thenReturn(user);
        when(memberUserApi.getUser(memberUserId)).thenReturn(user);
    }

    private void mockToken(String token) {
        OAuth2AccessTokenRespDTO resp = new OAuth2AccessTokenRespDTO();
        resp.setAccessToken(token).setRefreshToken(token + "_R").setExpiresTime(LocalDateTime.now().plusHours(1));
        when(oauth2TokenApi.createAccessToken(any())).thenReturn(resp);
    }

    /**
     * 单测里没有 Security 上下文，直接给 SecurityFrameworkUtils 的 ThreadLocal 放一个登录用户。
     */
    private void setLoginUser(Long memberUserId) {
        cn.iocoder.yudao.framework.security.core.LoginUser loginUser =
                new cn.iocoder.yudao.framework.security.core.LoginUser();
        loginUser.setId(memberUserId);
        loginUser.setUserType(cn.iocoder.yudao.framework.common.enums.UserTypeEnum.MEMBER.getValue());
        org.springframework.security.core.context.SecurityContextHolder.getContext()
                .setAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        loginUser, null, null));
    }

    private PayeeInfoDO insertPayeeInTenant(Long tenantId, String name, String idCardNo, String mobile) {
        return TenantUtils.execute(tenantId, () -> {
            NaturalPersonRegisterReqVO reqVO = new NaturalPersonRegisterReqVO();
            reqVO.setName(name);
            reqVO.setIdCardNo(idCardNo);
            reqVO.setMobile(mobile);
            IcbcNaturalPersonDO person = naturalPersonService.register(reqVO);
            PayeeInfoDO payee = PayeeInfoDO.builder()
                    .partnerPayeeId("PARTNER_" + mobile)
                    .naturalPersonId(person.getId())
                    .name(name)
                    .idCardNo(idCardNo)
                    .mobile(mobile)
                    .build();
            payeeInfoMapper.insert(payee);
            return payee;
        });
    }

    private IcbcNaturalPersonDO personOf(PayeeInfoDO payee) {
        return naturalPersonService.getNaturalPerson(payee.getNaturalPersonId());
    }

}
