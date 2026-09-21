package cn.iocoder.yudao.module.icbc.service.wizard;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.icbc.UnitTestConfiguration;
import cn.iocoder.yudao.module.icbc.controller.admin.publictoken.vo.PublicTokenCreateReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.publictoken.vo.PublicTokenRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.payee.vo.PayeeBankCardChangeSaveReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.payee.vo.PayeeInfoPageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.payee.vo.PayeeInfoSaveReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.wizard.vo.*;
import cn.iocoder.yudao.module.icbc.dal.dataobject.agreement.IcbcFrameworkAgreementDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.naturalperson.IcbcNaturalPersonDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payee.PayeeInfoDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.agreement.IcbcFrameworkAgreementMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.payee.PayeeInfoMapper;
import cn.iocoder.yudao.module.icbc.enums.FrameworkAgreementSignMethodEnum;
import cn.iocoder.yudao.module.icbc.enums.PayeeOnboardingOutcomeEnum;
import cn.iocoder.yudao.module.icbc.enums.PublicTokenPurposeEnum;
import cn.iocoder.yudao.module.icbc.service.cardrecognition.CardRecognitionPort;
import cn.iocoder.yudao.module.icbc.service.esign.EsignPort;
import cn.iocoder.yudao.module.icbc.service.naturalperson.NaturalPersonService;
import cn.iocoder.yudao.module.icbc.service.payee.PayeeBankCardChangeService;
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
import cn.iocoder.yudao.module.system.api.tenant.TenantApi;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Collections;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
    @Resource
    private PayeeBankCardChangeService payeeBankCardChangeService;

    @MockBean
    private CardRecognitionPort cardRecognitionPort;
    @MockBean
    private EsignPort esignPort;
    /**
     * #95 起向导的协议落库会经 {@code SellerOnboardingServiceImpl#saveFrameworkAgreement} 走到
     * 电子签章的租户配置链（{@code EsignTenantServiceImpl} 要 {@code TenantApi}）。
     * 合并前 #94 的这条链不需要它；合并后需要——这是只在合并台上跑测试才会暴露的那一类。
     */
    @MockBean
    private TenantApi tenantApi;

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
    public void testContext_returnsExpiry() {
        PublicTokenRespVO minted = mintInvite();

        PublicOnboardingWizardContextRespVO context = publicOnboardingWizardService.context(minted.getToken());

        // 页面要把链接有效期显示给本人（#94 复审 ST-4）：只回有效期，不再回没人用的用途字段
        assertNotNull(context.getExpiresTime());
    }

    @Test
    public void testContext_revokedLinkSaysRevokedNotExpired() {
        PublicTokenRespVO minted = mintInvite();
        publicTokenService.revoke(minted.getToken());

        // 本人被收货员作废的链接打开：要看到「已被作废」，而不是「已过期」（#94 复审 ST-5）
        assertServiceException(() -> publicOnboardingWizardService.context(minted.getToken()),
                PUBLIC_TOKEN_REVOKED);
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
    @Transactional(propagation = Propagation.NOT_SUPPORTED) // 这条要看到真实回滚：不套测试事务，提交各自成事务
    public void testSubmit_successConsumesTheOnlyUse_sameLinkCannotBuildTwoArchives() {
        String token = mintInvite().getToken();
        String firstIdCardNo = "110101199001010202";
        publicOnboardingWizardService.submit(token, fullReq(firstIdCardNo, "13800000202"));

        // 一枚链接只建一份档案：换一个人再提交即被拒（maxUses = 1，成功落库才占用）
        assertServiceException(() -> publicOnboardingWizardService.submit(
                token, fullReq("110101199001010207", "13800000207")), PUBLIC_TOKEN_USED_UP);
        // 把注释声称的东西断言掉（#94 评审 S-6）：第二次调用里的落库在同一事务里回滚，没留下第二份档案，
        // 也没把第一次那份改坏
        assertNull(payeeInfoMapper.selectByIdCardNo("110101199001010207"), "第二个人不能凭同一枚链接建档");
        assertNotNull(payeeInfoMapper.selectByIdCardNo(firstIdCardNo), "第一个人的档案仍在");
        assertEquals(1, countByIdCardNo(firstIdCardNo));
    }

    @Test
    public void testSubmit_weakNetworkRetry_rejected_andLeavesOneArchive() {
        String token = mintInvite().getToken();
        String idCardNo = "110101199001010208";
        publicOnboardingWizardService.submit(token, fullReq(idCardNo, "13800000208"));

        // 弱网下服务端已落库、客户端超时后用**同一枚链接**重提：这个人现在已有档案，
        // 待建档链接不能再去改它（#94 修票 ST-1）——重提在同一事务里回滚，不会留下第二份档案，
        // 也不会把第一次那份改坏。
        assertServiceException(() -> publicOnboardingWizardService.submit(token, fullReq(idCardNo, "13800000208")),
                WIZARD_INVITE_PERSON_ALREADY_ARCHIVED);
        assertEquals(1, countByIdCardNo(idCardNo), "同一枚链接重复提交不产生第二份档案");
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
    public void testSubmit_alreadyArchivedInTenant_updatesThatArchive_andKeepsOneRow() {
        String idCardNo = "110101199001010204";
        String mobile = "13800000204";
        Long existingPayeeId = payeeInfoService.createPayeeInfo(payeeReq(idCardNo, mobile));
        // 已建档：链接绑 PAYEE，锁到这个人身上（#94 修票 ST-1 结构根因）
        String token = mintInvite(existingPayeeId).getToken();

        // AC1 的「已建档」分支：本人拿到一枚新链接也能走完，落在**既有那份档案**上，
        // 不新建、也不以「本企业已有档案」拒掉
        OnboardingWizardSubmitRespVO resp = publicOnboardingWizardService.submit(token, fullReq(idCardNo, mobile));

        assertEquals(existingPayeeId, resp.getPayeeId(), "已建档的人走自填向导要更新既有档案");
        assertEquals(1, countByIdCardNo(idCardNo), "幂等：同一张身份证仍只有一份档案");
        assertLandedShape(resp, idCardNo);
    }

    @Test
    public void testSubmit_inviteTokenCannotWriteAnExistingArchive() {
        String idCardNo = "110101199001010209";
        String mobile = "13800000209";
        Long existingPayeeId = payeeInfoService.createPayeeInfo(payeeReq(idCardNo, mobile));
        // 待建档链接（没绑人）：持链接者不能拿它改本租户里任何已有档案
        String token = mintInvite().getToken();

        assertServiceException(() -> publicOnboardingWizardService.submit(token, fullReq(idCardNo, mobile)),
                WIZARD_INVITE_PERSON_ALREADY_ARCHIVED);

        assertEquals(1, countByIdCardNo(idCardNo), "不新建第二份");
        assertNull(payeeInfoMapper.selectById(existingPayeeId).getBankCardNo(), "既有档案一个字都不能被改");
    }

    @Test
    public void testSubmit_payeeBoundTokenCannotWriteAnotherPerson() {
        Long payeeId = payeeInfoService.createPayeeInfo(payeeReq("110101199001010210", "13800000210"));
        // 链接锁在 110101199001010210 身上
        String token = mintInvite(payeeId).getToken();

        // 持链接者改另一个已知身份证的档案：拒绝，且不为那个人建档
        assertServiceException(() -> publicOnboardingWizardService.submit(
                token, fullReq("110101199001010211", "13800000211")), WIZARD_INVITE_PAYEE_MISMATCH);

        assertNull(payeeInfoMapper.selectByIdCardNo("110101199001010211"));
    }

    @Test
    public void testSubmit_onboardedArchiveWithCard_cannotRewriteCard() {
        String idCardNo = "110101199001010212";
        String mobile = "13800000212";
        String effectiveCard = "6222021111111111111";
        Long payeeId = payeeInfoService.createPayeeInfo(payeeReqWithCard(idCardNo, mobile, effectiveCard));
        markOnboardedReady(payeeId);
        String token = mintInvite(payeeId).getToken();

        // 本人自己拍了一张新卡：本入口只办首卡，不替工行改卡，明确拒绝并指向换卡
        OnboardingWizardSubmitReqVO reqVO = fullReq(idCardNo, mobile);
        reqVO.setBankCardNo("6222022222222222222");
        reqVO.setBankName("招商银行");
        reqVO.setAccountCode("0");

        assertServiceException(() -> publicOnboardingWizardService.submit(token, reqVO),
                WIZARD_CARD_CHANGE_REQUIRES_CHANGE_ORDER);

        PayeeInfoDO after = payeeInfoMapper.selectById(payeeId);
        assertEquals(effectiveCard, after.getBankCardNo(), "生效中的卡一个字段都不许由本入口改写");
        assertEquals("中国工商银行", after.getBankName());
        assertEquals("1", after.getAccountCode());
    }

    @Test
    public void testSubmit_onboardedArchiveSameCard_updatesOtherFieldsButKeepsCard() {
        String idCardNo = "110101199001010213";
        String mobile = "13800000213";
        String effectiveCard = "6222021234567890123"; // 与 fullReq 同一张
        Long payeeId = payeeInfoService.createPayeeInfo(payeeReqWithCard(idCardNo, mobile, effectiveCard));
        markOnboardedReady(payeeId);
        String token = mintInvite(payeeId).getToken();

        OnboardingWizardSubmitReqVO reqVO = fullReq(idCardNo, mobile);
        reqVO.setAddress("换了个住址");
        OnboardingWizardSubmitRespVO resp = publicOnboardingWizardService.submit(token, reqVO);

        assertEquals(payeeId, resp.getPayeeId());
        PayeeInfoDO after = payeeInfoMapper.selectById(payeeId);
        assertEquals(effectiveCard, after.getBankCardNo(), "确认的是同一张卡：卡字段原样保留");
        assertEquals("换了个住址", after.getAddress(), "同一枚链接仍可更新非卡字段");
    }

    @Test
    public void testSubmit_onboardedArchiveWithPendingCardChange_givesReadableError() {
        String idCardNo = "110101199001010214";
        String mobile = "13800000214";
        Long payeeId = payeeInfoService.createPayeeInfo(payeeReqWithCard(idCardNo, mobile, "6222021111111111111"));
        markOnboardedReady(payeeId);
        // 已有一笔在途换卡：换卡审核中，不能从本入口再改一次
        PayeeBankCardChangeSaveReqVO change = new PayeeBankCardChangeSaveReqVO();
        change.setPayeeId(payeeId);
        change.setNewBankCardNo("6222023333333333333");
        payeeBankCardChangeService.requestChange(change);
        String token = mintInvite(payeeId).getToken();

        OnboardingWizardSubmitReqVO reqVO = fullReq(idCardNo, mobile);
        reqVO.setBankCardNo("6222024444444444444");
        assertServiceException(() -> publicOnboardingWizardService.submit(token, reqVO),
                WIZARD_CARD_CHANGE_IN_PROGRESS);
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

    /** 待建档链接：不绑人（这个人还没有收方档案）。 */
    private PublicTokenRespVO mintInvite() {
        return mintInvite(null);
    }

    /** 已建档链接：绑到某个收方身上（#94 修票 ST-1 结构根因）。 */
    private PublicTokenRespVO mintInvite(Long payeeId) {
        PublicTokenCreateReqVO reqVO = new PublicTokenCreateReqVO();
        reqVO.setPurpose(PublicTokenPurposeEnum.ONBOARDING_WIZARD.getCode());
        reqVO.setPayeeId(payeeId);
        return publicTokenService.mint(reqVO);
    }

    private PayeeInfoSaveReqVO payeeReq(String idCardNo, String mobile) {
        PayeeInfoSaveReqVO reqVO = new PayeeInfoSaveReqVO();
        reqVO.setName("张三");
        reqVO.setIdCardNo(idCardNo);
        reqVO.setMobile(mobile);
        // 与 fullReq 的证件有效期一致：自然人主体按「复用不覆盖」规则保留先登记的值，
        // 断言落库形状时两边才对得上
        reqVO.setIdSignDate("2020-01-01");
        reqVO.setIdValidityPeriod("2030-01-01");
        return reqVO;
    }

    /** 已入驻且已有生效卡的档案（换卡边界用例的起点）。 */
    private PayeeInfoSaveReqVO payeeReqWithCard(String idCardNo, String mobile, String bankCardNo) {
        PayeeInfoSaveReqVO reqVO = payeeReq(idCardNo, mobile);
        reqVO.setBankCardNo(bankCardNo);
        reqVO.setBankName("中国工商银行");
        reqVO.setAccountCode("1");
        return reqVO;
    }

    private void markOnboardedReady(Long payeeId) {
        PayeeInfoDO update = new PayeeInfoDO();
        update.setId(payeeId);
        update.setOnboardingState(PayeeOnboardingOutcomeEnum.READY.getCode());
        payeeInfoMapper.updateById(update);
    }

    /** 某张身份证在本租户有几份收方档案：幂等断言用。 */
    private int countByIdCardNo(String idCardNo) {
        PayeeInfoPageReqVO page = new PayeeInfoPageReqVO();
        page.setIdCardNo(idCardNo);
        return payeeInfoMapper.selectList(page).size();
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
        assertEquals(FrameworkAgreementSignMethodEnum.PAPER.getCode(), resp.getSignMethod(),
                "两个壳的 submit 结果都带同一签署方式（#94 评审 S-6）");
        assertEquals(1, agreement.getStatus());
        assertNotNull(agreement.getSignedAt());

        // 本票不发起电子签署：不调 initiate / createSignUrl（与代录壳同一断言，
        // 两个壳都不许偷偷写一份「没人签过」的电子协议）
        verify(esignPort, never()).initiate(anyLong(), any());
        verify(esignPort, never()).createSignUrl(anyLong(), any(), any());
    }

}
