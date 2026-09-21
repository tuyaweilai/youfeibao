package cn.iocoder.yudao.module.icbc.service.wizard;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.icbc.UnitTestConfiguration;
import cn.iocoder.yudao.module.icbc.controller.admin.publictoken.vo.PublicTokenCreateReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.publictoken.vo.PublicTokenRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.payee.vo.PayeeInfoSaveReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.wizard.vo.*;
import cn.iocoder.yudao.module.icbc.dal.dataobject.agreement.IcbcFrameworkAgreementDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.naturalperson.IcbcNaturalPersonDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payee.PayeeInfoDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.agreement.IcbcFrameworkAgreementMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.payee.PayeeInfoMapper;
import cn.iocoder.yudao.module.icbc.enums.FrameworkAgreementSignMethodEnum;
import cn.iocoder.yudao.module.icbc.enums.PublicTokenPurposeEnum;
import cn.iocoder.yudao.module.icbc.service.cardrecognition.CardRecognitionPort;
import cn.iocoder.yudao.module.icbc.service.esign.EsignPort;
import cn.iocoder.yudao.module.icbc.service.naturalperson.NaturalPersonService;
import cn.iocoder.yudao.module.icbc.service.payee.PayeeInfoService;
import cn.iocoder.yudao.module.icbc.service.token.PublicTokenService;
import cn.iocoder.yudao.module.icbc.service.token.PublicTokenPayload;
import cn.iocoder.yudao.module.icbc.service.token.impl.PublicTokenServiceImpl;
import cn.iocoder.yudao.module.icbc.service.token.PublicTokenCodec;
import cn.iocoder.yudao.module.icbc.service.wizard.impl.OnboardingWizardServiceImpl;
import cn.iocoder.yudao.module.icbc.service.wizard.impl.PublicOnboardingWizardServiceImpl;
import cn.iocoder.yudao.test.icbc.IcbcTenantTestConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Collections;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * {@link PublicOnboardingWizardServiceImpl} 的单元测试（#94）。
 *
 * <p>断言的是「第二个壳复用第一个壳」这件事本身：公开端点从令牌解析租户、转发给同一个
 * {@code OnboardingWizardService}，落库位置与字段和代录壳完全一致；识别与重开页面不占令牌次数、
 * 成功建档才作废；中途退出不留半成品档案。
 *
 * <p>{@link IcbcTenantTestConfiguration} 打开多租户拦截器：不然「令牌里的租户决定档案落在哪家
 * 企业」这条根本没被执行（沿用 #91 评审 T-1 的口径）。
 */
@Import({PublicOnboardingWizardServiceImpl.class, OnboardingWizardServiceImpl.class,
        PublicTokenServiceImpl.class, PublicTokenCodec.class,
        UnitTestConfiguration.class, IcbcTenantTestConfiguration.class})
@TestPropertySource(properties = {
        "icbc.public-token.secret=test-public-token-secret-0123456789abcdef"
})
@Transactional
@Rollback
public class PublicOnboardingWizardServiceImplTest extends BaseDbUnitTest {

    private static final Long TENANT_ID = 1L;

    @Resource
    private PublicOnboardingWizardService publicOnboardingWizardService;
    @Resource
    private PublicTokenService publicTokenService;
    @Resource
    private PayeeInfoMapper payeeInfoMapper;
    @Resource
    private IcbcFrameworkAgreementMapper frameworkAgreementMapper;
    @Resource
    private NaturalPersonService naturalPersonService;
    @Resource
    private PayeeInfoService payeeInfoService;

    @MockBean
    private CardRecognitionPort cardRecognitionPort;
    @MockBean
    private EsignPort esignPort;

    @BeforeEach
    public void setUp() {
        TenantContextHolder.setTenantId(TENANT_ID);
    }

    @AfterEach
    public void tearDown() {
        TenantContextHolder.clear();
    }

    // ==================== 链接生成与打开 ====================

    @Test
    public void testMintInvite_needsNoExistingPayee() {
        // 「待建档」的自然人还没有收方档案，链接照样签得出来（绑定链接本身，不绑定收方 ID）
        PublicTokenRespVO resp = mintInvite();
        assertNotNull(resp.getToken());
        assertEquals(PublicTokenPurposeEnum.ONBOARDING_WIZARD.getCode(), resp.getPurpose());
        assertNotNull(resp.getBusinessKey());
        assertNotNull(resp.getExpiresTime());
    }

    @Test
    public void testContext_returnsPurposeAndExpiry() {
        PublicTokenRespVO minted = mintInvite();

        PublicOnboardingWizardContextRespVO context = publicOnboardingWizardService.context(minted.getToken());

        assertEquals(PublicTokenPurposeEnum.ONBOARDING_WIZARD.getCode(), context.getPurpose());
        assertEquals(PublicTokenPurposeEnum.ONBOARDING_WIZARD.getName(), context.getPurposeName());
        assertNotNull(context.getExpiresTime());
    }

    @Test
    public void testContext_expiredOrRevokedGivesReadableError() {
        PublicTokenRespVO minted = mintInvite();
        publicTokenService.revoke(minted.getToken());

        assertServiceException(() -> publicOnboardingWizardService.context(minted.getToken()),
                PUBLIC_TOKEN_EXPIRED);
    }

    // ==================== 识别：走同一个向导 Service ====================

    @Test
    public void testRecognizeIdCardFront_usesSameFieldMergeRule() {
        when(cardRecognitionPort.recognizeIdCardFront(anyString()))
                .thenReturn(CardRecognitionPort.IdCardFront.builder()
                        .name("识别出来的名字")
                        .idCardNo("110101199001011234")
                        .address("识别出来的住址")
                        .warnings(Collections.singletonList("原件翻拍"))
                        .build());
        String token = mintInvite().getToken();
        IdCardFrontRecognizeReqVO reqVO = new IdCardFrontRecognizeReqVO();
        reqVO.setImageBase64("base64-image");
        reqVO.setName("人工改过的名字"); // 人工值优先；其余空缺处回填

        IdCardFrontRecognizeRespVO resp = publicOnboardingWizardService.recognizeIdCardFront(token, reqVO);

        assertEquals("人工改过的名字", resp.getName());
        assertEquals("110101199001011234", resp.getIdCardNo());
        assertEquals("识别出来的住址", resp.getAddress());
        assertEquals(Collections.singletonList("原件翻拍"), resp.getWarnings());
    }

    // ==================== 落库：与代录壳同一处、同一形状 ====================

    @Test
    public void testSubmit_landsSameArchiveAsRecorderShell_andExchangesOnboardingToken() {
        String token = mintInvite().getToken();

        OnboardingWizardSubmitRespVO resp = publicOnboardingWizardService.submit(token, fullReq("110101199001010201", "13800000201"));

        assertLandedShape(resp, "110101199001010201");

        // 建档完成后换一枚实名令牌：本人接着在自己手机上做实名（ADR 0007 补充）
        assertNotNull(resp.getOnboardingToken());
        PublicTokenPayload onboarding = publicTokenService.verify(
                resp.getOnboardingToken(), PublicTokenPurposeEnum.ONBOARDING);
        assertEquals(String.valueOf(resp.getPayeeId()), onboarding.getBusinessKey());
    }

    @Test
    public void testSubmit_successConsumesTheOnlyUse_sameLinkCannotBuildTwoArchives() {
        String token = mintInvite().getToken();
        publicOnboardingWizardService.submit(token, fullReq("110101199001010202", "13800000202"));

        // 一枚链接只建一份档案：换一个人再提交即被拒（maxUses = 1，成功落库才占用；
        // 后面的落库会在同一事务里回滚，不会留下第二份档案）
        assertServiceException(() -> publicOnboardingWizardService.submit(
                token, fullReq("110101199001010207", "13800000207")), PUBLIC_TOKEN_USED_UP);
    }

    @Test
    public void testSubmit_weakNetworkRetry_sameePersonReadsAlreadyArchived() {
        String token = mintInvite().getToken();
        String idCardNo = "110101199001010208";
        publicOnboardingWizardService.submit(token, fullReq(idCardNo, "13800000208"));

        // 弱网下服务端已落库、客户端超时后重提：拿到的是「本企业已有档案」而不是「链接用尽」，
        // 本人看得懂发生了什么（失败重试不占次数，所以到了落库这一步才撞）
        assertServiceException(() -> publicOnboardingWizardService.submit(token, fullReq(idCardNo, "13800000208")),
                WIZARD_PAYEE_ALREADY_ARCHIVED);
    }

    @Test
    public void testAbandon_reopenLeavesNoHalfArchive_andTokenStillUsable() {
        String token = mintInvite().getToken();
        // 只识别、没提交（本人切后台 / 中途退出）
        when(cardRecognitionPort.recognizeIdCardFront(anyString()))
                .thenReturn(CardRecognitionPort.IdCardFront.empty());
        IdCardFrontRecognizeReqVO reqVO = new IdCardFrontRecognizeReqVO();
        reqVO.setImageBase64("base64-image");
        publicOnboardingWizardService.recognizeIdCardFront(token, reqVO);

        // 不产生半成品档案；链接仍然可用（识别不占次数）
        assertNull(payeeInfoMapper.selectByIdCardNo("110101199001010203"));
        assertDoesNotThrow(() -> publicOnboardingWizardService.context(token));

        // 从干净状态重来仍能走完
        String idCardNo = "110101199001010203";
        OnboardingWizardSubmitRespVO resp = publicOnboardingWizardService.submit(token, fullReq(idCardNo, "13800000203"));
        assertLandedShape(resp, idCardNo);
    }

    @Test
    public void testSubmit_alreadyArchivedInTenant_rejectsReadably() {
        String idCardNo = "110101199001010204";
        String mobile = "13800000204";
        payeeInfoService.createPayeeInfo(payeeReq(idCardNo, mobile));
        String token = mintInvite().getToken();

        // 同一租户一张身份证只能有一份档案：自填壳给出与代录壳同一句可读提示
        assertServiceException(() -> publicOnboardingWizardService.submit(token, fullReq(idCardNo, mobile)),
                WIZARD_PAYEE_ALREADY_ARCHIVED);
    }

    @Test
    public void testSubmit_landsArchiveInTokenTenant() {
        // 令牌是另一家回收企业签的：档案必须落在那家企业的租户下，而不是当前上下文里的租户
        PublicTokenRespVO minted = TenantUtils.execute(2L, () -> {
            PublicTokenCreateReqVO req = new PublicTokenCreateReqVO();
            req.setPurpose(PublicTokenPurposeEnum.ONBOARDING_WIZARD.getCode());
            return publicTokenService.mint(req);
        });
        String idCardNo = "110101199001010205";

        OnboardingWizardSubmitRespVO resp = publicOnboardingWizardService.submit(minted.getToken(), fullReq(idCardNo, "13800000205"));

        assertEquals(Long.valueOf(2L), TenantUtils.execute(2L, () -> payeeInfoMapper.selectByIdCardNo(idCardNo).getTenantId()));
        assertNull(payeeInfoMapper.selectByIdCardNo(idCardNo), "当前租户（1）里不该出现别家企业的档案");
        assertNotNull(resp.getPayeeId());
    }

    // ==================== 助手 ====================

    private PublicTokenRespVO mintInvite() {
        PublicTokenCreateReqVO reqVO = new PublicTokenCreateReqVO();
        reqVO.setPurpose(PublicTokenPurposeEnum.ONBOARDING_WIZARD.getCode());
        return publicTokenService.mint(reqVO);
    }

    private PayeeInfoSaveReqVO payeeReq(String idCardNo, String mobile) {
        PayeeInfoSaveReqVO reqVO = new PayeeInfoSaveReqVO();
        reqVO.setName("张三");
        reqVO.setIdCardNo(idCardNo);
        reqVO.setMobile(mobile);
        return reqVO;
    }

    private OnboardingWizardSubmitReqVO fullReq(String idCardNo, String mobile) {
        OnboardingWizardSubmitReqVO reqVO = new OnboardingWizardSubmitReqVO();
        reqVO.setName("张三");
        reqVO.setIdCardNo(idCardNo);
        reqVO.setMobile(mobile);
        reqVO.setAddress("北京市朝阳区某街道 1 号");
        reqVO.setIdSignDate("2020-01-01");
        reqVO.setIdValidityPeriod("2030-01-01");
        reqVO.setBankCardNo("6222021234567890123");
        reqVO.setBankName("中国工商银行");
        reqVO.setAccountCode("1");
        return reqVO;
    }

    /** 与代录壳 {@code OnboardingWizardServiceImplTest#assertLandedShape} 同一形状：两个壳落到同一处。 */
    private void assertLandedShape(OnboardingWizardSubmitRespVO resp, String idCardNo) {
        PayeeInfoDO payee = payeeInfoMapper.selectById(resp.getPayeeId());
        assertNotNull(payee);
        assertEquals("张三", payee.getName());
        assertEquals(idCardNo, payee.getIdCardNo());
        assertEquals("2020-01-01", payee.getIdSignDate());
        assertEquals("2030-01-01", payee.getIdValidityPeriod());
        assertEquals("6222021234567890123", payee.getBankCardNo());
        assertEquals("中国工商银行", payee.getBankName());
        assertEquals("北京市朝阳区某街道 1 号", payee.getAddress());
        assertEquals("1", payee.getAccountCode());
        assertNotNull(payee.getNaturalPersonId());

        IcbcNaturalPersonDO person = naturalPersonService.getNaturalPerson(payee.getNaturalPersonId());
        assertEquals(payee.getName(), person.getName());
        assertEquals(payee.getIdCardNo(), person.getIdCardNo());
        assertEquals("2020-01-01", person.getIdSignDate());
        assertEquals("2030-01-01", person.getIdValidityPeriod());

        IcbcFrameworkAgreementDO agreement = frameworkAgreementMapper.selectById(resp.getAgreementId());
        assertNotNull(agreement);
        assertEquals(FrameworkAgreementSignMethodEnum.PAPER.getCode(), agreement.getSignMethod());
        assertEquals(1, agreement.getStatus());
        assertNotNull(agreement.getSignedAt());
    }

}
