package cn.iocoder.yudao.module.icbc.service.wizard;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.module.icbc.UnitTestConfiguration;
import cn.iocoder.yudao.module.icbc.controller.admin.onboarding.vo.FrameworkAgreementSaveReqVO;
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
import cn.iocoder.yudao.module.icbc.service.cardrecognition.CardRecognitionPort;
import cn.iocoder.yudao.module.icbc.service.esign.EsignPort;
import cn.iocoder.yudao.module.icbc.service.esign.FrameworkAgreementEsignService;
import cn.iocoder.yudao.module.icbc.service.naturalperson.NaturalPersonService;
import cn.iocoder.yudao.module.icbc.service.onboarding.SellerOnboardingService;
import cn.iocoder.yudao.module.icbc.service.payee.PayeeBankCardChangeService;
import cn.iocoder.yudao.module.icbc.service.payee.PayeeInfoService;
import cn.iocoder.yudao.module.icbc.service.wizard.impl.OnboardingWizardServiceImpl;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * {@link OnboardingWizardServiceImpl} 的单元测试。
 *
 * <p>两条缝各自 mock、都不触网：卡证识别端口（ADR 0037）与电子签章端口（ADR 0036）。
 * 断言的是外部可观察行为——识别结果只在空缺处回填、落库落到了自然人主体与收方档案、
 * 协议落 {@code PAPER} 且**没有**调电子签章端口的发起 / 链接。
 *
 * <p>{@link IcbcTenantTestConfiguration} 打开多租户拦截器：不然「第二家回收企业复用同一个
 * 自然人主体」这条根本没被执行（#91 评审 T-1 —— 名字骗人比没测更糟）。
 */
@Import({OnboardingWizardServiceImpl.class, UnitTestConfiguration.class, IcbcTenantTestConfiguration.class})
@TestPropertySource(properties = {
        "icbc.public-token.secret=test-public-token-secret-0123456789abcdef",
        "icbc.notify.seller-app-url=https://seller.example.com"
})
@Transactional
@Rollback
public class OnboardingWizardServiceImplTest extends BaseDbUnitTest {

    @Resource
    private OnboardingWizardService onboardingWizardService;

    @Resource
    private PayeeInfoMapper payeeInfoMapper;
    @Resource
    private IcbcFrameworkAgreementMapper frameworkAgreementMapper;
    @Resource
    private NaturalPersonService naturalPersonService;
    @Resource
    private PayeeInfoService payeeInfoService;
    @Resource
    private SellerOnboardingService sellerOnboardingService;
    @Resource
    private PayeeBankCardChangeService payeeBankCardChangeService;

    @MockBean
    private CardRecognitionPort cardRecognitionPort;

    @MockBean
    private EsignPort esignPort;

    /** 电子签发起由 {@code FrameworkAgreementEsignServiceImplTest} 单独覆盖，这里只验证向导把协议落成待签署并转交发起 */
    @MockBean
    private FrameworkAgreementEsignService frameworkAgreementEsignService;

    @BeforeEach
    public void setUp() {
        TenantContextHolder.setTenantId(1L);
    }

    @AfterEach
    public void tearDown() {
        TenantContextHolder.clear();
    }

    // ==================== 识别只在空缺处回填 ====================

    @Test
    public void testRecognizeIdCardFront_manualValuesWin_andBlankFilled() {
        when(cardRecognitionPort.recognizeIdCardFront(anyString()))
                .thenReturn(CardRecognitionPort.IdCardFront.builder()
                        .name("识别出来的名字")
                        .idCardNo("110101199001011234")
                        .address("识别出来的住址")
                        .qualityScore(88)
                        .warnings(Collections.singletonList("原件翻拍"))
                        .build());
        IdCardFrontRecognizeReqVO reqVO = new IdCardFrontRecognizeReqVO();
        reqVO.setImageBase64("base64-image");
        reqVO.setName("人工改过的名字");
        reqVO.setIdCardNo("   "); // 空白 = 空缺，应当回填
        // address 不填 = 空缺

        IdCardFrontRecognizeRespVO resp = onboardingWizardService.recognizeIdCardFront(reqVO);

        assertEquals("人工改过的名字", resp.getName(), "人工输入的值优先，识别结果不许覆盖");
        assertEquals("110101199001011234", resp.getIdCardNo(), "空缺处应回填识别结果");
        assertEquals("识别出来的住址", resp.getAddress(), "空缺处应回填识别结果");
        assertEquals(88, resp.getQualityScore());
        assertEquals(Collections.singletonList("原件翻拍"), resp.getWarnings());
    }

    @Test
    public void testRecognizeIdCardBack_manualValuesWin() {
        when(cardRecognitionPort.recognizeIdCardBack(anyString()))
                .thenReturn(CardRecognitionPort.IdCardBack.builder()
                        .idSignDate("2018-08-12")
                        .idValidityPeriod("2038-08-12")
                        .build());
        IdCardBackRecognizeReqVO reqVO = new IdCardBackRecognizeReqVO();
        reqVO.setImageBase64("base64-image");
        reqVO.setIdSignDate("2020-01-01"); // 人工填过，保持

        IdCardBackRecognizeRespVO resp = onboardingWizardService.recognizeIdCardBack(reqVO);

        assertEquals("2020-01-01", resp.getIdSignDate());
        assertEquals("2038-08-12", resp.getIdValidityPeriod());
    }

    @Test
    public void testRecognizeBankCard_manualValuesWin() {
        when(cardRecognitionPort.recognizeBankCard(anyString()))
                .thenReturn(CardRecognitionPort.BankCard.builder()
                        .bankCardNo("6222021234567890123")
                        .bankName("招商银行")
                        .accountCode("0")
                        .build());
        BankCardRecognizeReqVO reqVO = new BankCardRecognizeReqVO();
        reqVO.setImageBase64("base64-image");
        reqVO.setBankCardNo("6222029999999999999"); // 人工核对过，保持
        reqVO.setAccountCode("1"); // 本人在确认页选过，保持

        BankCardRecognizeRespVO resp = onboardingWizardService.recognizeBankCard(reqVO);

        assertEquals("6222029999999999999", resp.getBankCardNo());
        assertEquals("招商银行", resp.getBankName());
        assertEquals("1", resp.getAccountCode(), "人工确认过的「是否我行卡」不许被识别结果覆盖");
    }

    @Test
    public void testRecognizeBankCard_blankAccountCodeFilledByRecognition() {
        // 「是否我行卡」的草稿初值必须是空（未确认），识别结果才回填得进来（#91 评审 SP-2）：
        // 初值若写死 '1'，下面的 pick 就永远命中人工分支，识别结果永远进不了这个字段。
        when(cardRecognitionPort.recognizeBankCard(anyString()))
                .thenReturn(CardRecognitionPort.BankCard.builder()
                        .bankCardNo("6222021234567890123")
                        .bankName("中国工商银行")
                        .accountCode("1")
                        .build());
        BankCardRecognizeReqVO reqVO = new BankCardRecognizeReqVO();
        reqVO.setImageBase64("base64-image");
        // accountCode 不填 = 未确认，应当回填识别结果

        BankCardRecognizeRespVO resp = onboardingWizardService.recognizeBankCard(reqVO);

        assertEquals("1", resp.getAccountCode(), "未确认时识别结果要回填进来，而不是被初值 1 挡住");
    }

    @Test
    public void testRecognize_whenPortReturnsEmpty_manualValuesUnchangedAndNoError() {
        // 未配置 / 额度耗尽：端口返回空结果，向导退化为手工录入，一样能走完（ADR 0037）
        when(cardRecognitionPort.recognizeIdCardFront(anyString()))
                .thenReturn(CardRecognitionPort.IdCardFront.empty());
        IdCardFrontRecognizeReqVO reqVO = new IdCardFrontRecognizeReqVO();
        reqVO.setImageBase64("base64-image");
        reqVO.setName("手工录入的名字");
        reqVO.setIdCardNo("110101199001019999");

        IdCardFrontRecognizeRespVO resp = onboardingWizardService.recognizeIdCardFront(reqVO);

        assertEquals("手工录入的名字", resp.getName());
        assertEquals("110101199001019999", resp.getIdCardNo());
        assertNull(resp.getAddress());
    }

    @Test
    public void testRecognize_blankImageRejected() {
        IdCardFrontRecognizeReqVO reqVO = new IdCardFrontRecognizeReqVO();
        reqVO.setImageBase64("  ");

        assertServiceException(() -> onboardingWizardService.recognizeIdCardFront(reqVO),
                WIZARD_IMAGE_REQUIRED);
    }

    // ==================== 落库：自然人主体 + 收方档案 + 纸质协议 ====================

    @Test
    public void testSubmit_registersPersonAndLandsPaperAgreement() {
        when(esignPort.isAvailable(anyLong())).thenReturn(false); // 未开通电子签章

        OnboardingWizardSubmitReqVO reqVO = fullReq("110101199001010101", "13800000101");

        OnboardingWizardSubmitRespVO resp = onboardingWizardService.submit(reqVO);

        assertLandedShape(resp, "110101199001010101");
    }

    @Test
    public void testSubmit_whenPortReturnsEmpty_stillLandsSameShape() {
        // 端口全空（未配置 / 额度耗尽）：识别三个接口都不报错、不返回任何识别值，
        // 收货员手工录入后一样能提交，落库形状与识别成功时同形（#91 验收 / 评审 T-2）。
        when(cardRecognitionPort.recognizeIdCardFront(anyString()))
                .thenReturn(CardRecognitionPort.IdCardFront.empty());
        when(cardRecognitionPort.recognizeIdCardBack(anyString()))
                .thenReturn(CardRecognitionPort.IdCardBack.empty());
        when(cardRecognitionPort.recognizeBankCard(anyString()))
                .thenReturn(CardRecognitionPort.BankCard.empty());
        when(esignPort.isAvailable(anyLong())).thenReturn(false);

        OnboardingWizardSubmitReqVO reqVO = fullReq("110101199001010105", "13800000105");
        IdCardFrontRecognizeReqVO frontReq = new IdCardFrontRecognizeReqVO();
        frontReq.setImageBase64("id-front");
        frontReq.setName(reqVO.getName());
        frontReq.setIdCardNo(reqVO.getIdCardNo());
        frontReq.setAddress(reqVO.getAddress());
        IdCardFrontRecognizeRespVO front = onboardingWizardService.recognizeIdCardFront(frontReq);
        assertEquals(reqVO.getName(), front.getName(), "端口全空时人工录入的值原样保留");
        assertNull(front.getBlockReasons());
        IdCardBackRecognizeReqVO backReq = new IdCardBackRecognizeReqVO();
        backReq.setImageBase64("id-back");
        backReq.setIdSignDate(reqVO.getIdSignDate());
        backReq.setIdValidityPeriod(reqVO.getIdValidityPeriod());
        IdCardBackRecognizeRespVO back = onboardingWizardService.recognizeIdCardBack(backReq);
        assertEquals(reqVO.getIdSignDate(), back.getIdSignDate());
        assertEquals(reqVO.getIdValidityPeriod(), back.getIdValidityPeriod());
        BankCardRecognizeReqVO bankReq = new BankCardRecognizeReqVO();
        bankReq.setImageBase64("bank-card");
        bankReq.setBankCardNo(reqVO.getBankCardNo());
        bankReq.setBankName(reqVO.getBankName());
        BankCardRecognizeRespVO bank = onboardingWizardService.recognizeBankCard(bankReq);
        assertEquals(reqVO.getBankCardNo(), bank.getBankCardNo());
        assertNull(bank.getAccountCode(), "端口全空且本人未确认时，「是否我行卡」就是空的（缺省由提交时兜底）");

        // 照前端确认页的路径，把识别的（此处为空）与人工确认的值一起提交
        OnboardingWizardSubmitRespVO resp = onboardingWizardService.submit(reqVO);

        assertLandedShape(resp, "110101199001010105");
    }

    /**
     * 落库形状：收方档案的字段、自然人主体的关联与证件有效期、纸质协议。
     *
     * <p>识别成功与「端口全空、纯手工录入」两种路径共用它——「同形」是断言出来的，不是声称的。
     */
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
        assertEquals("1", payee.getAccountCode(), "是否我行卡要落在收方档案上，供后续收方入驻取用");
        assertNotNull(payee.getNaturalPersonId(), "收方档案必须挂到平台级自然人主体上");

        // 姓名 / 证件号 / 证件有效期由**自然人主体**持有（#81 决策 5、CONTEXT）：一并落在主体上
        IcbcNaturalPersonDO person = naturalPersonService.getNaturalPerson(payee.getNaturalPersonId());
        assertEquals(payee.getName(), person.getName());
        assertEquals(payee.getIdCardNo(), person.getIdCardNo());
        assertEquals("2020-01-01", person.getIdSignDate(), "证件签发日期要落在自然人主体上（#91 评审 SP-1）");
        assertEquals("2030-01-01", person.getIdValidityPeriod(), "证件截止日期要落在自然人主体上（#91 评审 SP-1）");

        // 框架收购协议：落 PAPER、signMethod 有值、生效
        IcbcFrameworkAgreementDO agreement = frameworkAgreementMapper.selectById(resp.getAgreementId());
        assertNotNull(agreement);
        assertEquals(FrameworkAgreementSignMethodEnum.PAPER.getCode(), agreement.getSignMethod());
        assertEquals(FrameworkAgreementSignMethodEnum.PAPER.getCode(), resp.getSignMethod());
        assertEquals(1, agreement.getStatus());
        assertNotNull(agreement.getSignedAt());

        // 本票不发起电子签署：不调 initiate / createSignUrl
        verify(esignPort, never()).initiate(anyLong(), any());
        verify(esignPort, never()).createSignUrl(anyLong(), any(), any());
    }

    @Test
    public void testSubmit_whenEsignAvailable_landsPendingAndInitiatesContractGroup() {
        // 租户已开通电子签：协议落「待签署」（不盖 signedAt），并立刻发起合同组签署
        when(esignPort.isAvailable(anyLong())).thenReturn(true);

        OnboardingWizardSubmitRespVO resp = onboardingWizardService.submit(
                fullReq("110101199001010107", "13800000107"));

        assertEquals(FrameworkAgreementSignMethodEnum.ELECTRONIC.getCode(), resp.getSignMethod());
        assertEquals(0, resp.getAgreementStatus(), "发起 ≠ 签完：协议必须停在待签署");
        assertTrue(resp.getMessage().contains("去签署"), "要给现场一句可读说明：本人接下来点「去签署」");

        IcbcFrameworkAgreementDO agreement = frameworkAgreementMapper.selectById(resp.getAgreementId());
        assertEquals(FrameworkAgreementSignMethodEnum.ELECTRONIC.getCode(), agreement.getSignMethod());
        assertEquals(0, agreement.getStatus());
        assertNull(agreement.getSignedAt(), "没人签过就不该盖签署时间（#81 Problem Statement）");
        // 发起交给合同组服务，带上本租户与刚落的协议编号
        verify(frameworkAgreementEsignService).initiate(eq(1L), eq(resp.getAgreementId()));
    }

    @Test
    public void testSubmit_whenEsignUnavailable_landsPaperEffectiveWithReadableMessage() {
        when(esignPort.isAvailable(anyLong())).thenReturn(false);

        OnboardingWizardSubmitRespVO resp = onboardingWizardService.submit(
                fullReq("110101199001010108", "13800000108"));

        assertEquals(FrameworkAgreementSignMethodEnum.PAPER.getCode(), resp.getSignMethod());
        assertEquals(1, resp.getAgreementStatus());
        assertTrue(resp.getMessage().contains("纸质"), "未开通要给出可读降级说明");
        IcbcFrameworkAgreementDO agreement = frameworkAgreementMapper.selectById(resp.getAgreementId());
        assertEquals(1, agreement.getStatus());
        assertNotNull(agreement.getSignedAt());
        // 未开通不得发起电子签
        verify(frameworkAgreementEsignService, never()).initiate(anyLong(), anyLong());
    }

    @Test
    public void testSubmit_secondRecycleEnterpriseReusesPersonAndKeepsRegisteredIdValidity() {
        when(esignPort.isAvailable(anyLong())).thenReturn(false);
        String idCardNo = "110101199001010102";
        String mobile = "13800000102";
        // 第二家回收企业（tenantId = 2）先给这个人建过档，主体上落了证件有效期；
        // 多租户拦截器已打开，这条不是「同一个租户里再建一次」的假戏（#91 评审 T-1）
        Long otherPayeeId = TenantUtils.execute(2L, () -> payeeInfoService.createPayeeInfo(
                payeeReq(idCardNo, mobile, "2015-05-05", "2035-05-05")));
        Long personId = TenantUtils.execute(2L, () -> payeeInfoMapper.selectById(otherPayeeId)).getNaturalPersonId();
        assertNotNull(personId);

        // 本企业（tenantId = 1）再走一次向导：复用同一个主体，本企业档案用本次确认的值
        OnboardingWizardSubmitRespVO resp = onboardingWizardService.submit(fullReq(idCardNo, mobile));

        assertEquals(personId, resp.getNaturalPersonId(),
                "同一自然人在第二家回收企业建档要复用同一个自然人主体（ADR 0017）");
        assertEquals(personId, payeeInfoMapper.selectById(resp.getPayeeId()).getNaturalPersonId());
        assertEquals("2020-01-01", payeeInfoMapper.selectById(resp.getPayeeId()).getIdSignDate(),
                "本企业档案以本次确认的值为准");
        // 主体上已填的证件有效期不被后来的登记覆盖（与 reuse 那条「不覆盖」同一精神，#91 评审 SP-1）
        IcbcNaturalPersonDO person = naturalPersonService.getNaturalPerson(personId);
        assertEquals("2015-05-05", person.getIdSignDate());
        assertEquals("2035-05-05", person.getIdValidityPeriod());
    }

    @Test
    public void testSubmit_sameTenantSecondSubmitUpdatesExistingArchive_andVoidsOldAgreement() {
        when(esignPort.isAvailable(anyLong())).thenReturn(false);
        String idCardNo = "110101199001010106";
        String mobile = "13800000106";
        Long payeeId = payeeInfoService.createPayeeInfo(payeeReq(idCardNo, mobile, "2010-01-01", "2020-01-01"));
        // 既有生效协议：重签要作废它、历史仍可查
        FrameworkAgreementSaveReqVO oldAgreement = new FrameworkAgreementSaveReqVO();
        oldAgreement.setPayeeId(payeeId);
        oldAgreement.setProductName("报废产品");
        oldAgreement.setQuantity("以实际交货为准");
        oldAgreement.setSpecification("以实际交货为准");
        oldAgreement.setRecyclePeriod("长期");
        oldAgreement.setSettlementMethod("银行转账");
        Long oldAgreementId = sellerOnboardingService.saveFrameworkAgreement(oldAgreement);

        // 已建档但**还没有卡**（建档未完成 → 补首卡）：本次确认过的字段要更新上去，不新建第二份。
        // 首卡是本入口唯一可写卡的形状；已有生效卡的边界见下面三条用例（#94 复审 ST-1）。
        OnboardingWizardSubmitReqVO reqVO = fullReq(idCardNo, mobile);
        reqVO.setAddress("新住址");
        reqVO.setIdSignDate("2021-01-01");
        reqVO.setIdValidityPeriod("2031-01-01");
        reqVO.setBankCardNo("6222029999999999999");
        reqVO.setBankName("招商银行");
        reqVO.setAccountCode("0");

        OnboardingWizardSubmitRespVO resp = onboardingWizardService.submit(reqVO);

        assertEquals(payeeId, resp.getPayeeId(), "已有档案时返回既有那一份，不新建");
        PayeeInfoPageReqVO page = new PayeeInfoPageReqVO();
        page.setIdCardNo(idCardNo);
        assertEquals(1, payeeInfoMapper.selectList(page).size(), "同一租户一张身份证只有一份档案（幂等）");

        PayeeInfoDO payee = payeeInfoMapper.selectById(payeeId);
        assertEquals("新住址", payee.getAddress());
        assertEquals("2021-01-01", payee.getIdSignDate());
        assertEquals("2031-01-01", payee.getIdValidityPeriod());
        assertEquals("6222029999999999999", payee.getBankCardNo());
        assertEquals("招商银行", payee.getBankName());
        assertEquals("0", payee.getAccountCode());
        // 姓名 / 手机号是身份字段，不动
        assertEquals("张三", payee.getName());
        assertEquals(mobile, payee.getMobile());
        // 自然人主体按既有「复用不覆盖」规则，不由向导再写一次（#91 SP-1）
        IcbcNaturalPersonDO person = naturalPersonService.getNaturalPerson(payee.getNaturalPersonId());
        assertEquals("2010-01-01", person.getIdSignDate());
        assertEquals("2020-01-01", person.getIdValidityPeriod());
        // 新协议生效、旧协议作废（saveFrameworkAgreement 既有留痕规则）
        assertEquals(1, frameworkAgreementMapper.selectById(resp.getAgreementId()).getStatus());
        assertEquals(2, frameworkAgreementMapper.selectById(oldAgreementId).getStatus(), "旧生效协议被新协议作废");
    }

    @Test
    public void testSubmit_notYetOnboardedWithCard_mayCorrectCard() {
        when(esignPort.isAvailable(anyLong())).thenReturn(false);
        String idCardNo = "110101199001010110";
        String mobile = "13800000110";
        // 卡已在档案上、但还没送到工行（入驻未受理）：允许本人修正拍到一半的卡
        Long payeeId = createPayeeWithCard(idCardNo, mobile, "6222021111111111111");

        OnboardingWizardSubmitReqVO reqVO = fullReq(idCardNo, mobile);
        reqVO.setBankCardNo("6222022222222222222");
        OnboardingWizardSubmitRespVO resp = onboardingWizardService.submit(reqVO);

        assertEquals(payeeId, resp.getPayeeId());
        assertEquals("6222022222222222222", payeeInfoMapper.selectById(payeeId).getBankCardNo(),
                "卡还没送到工行审过，本路径可以修正");
    }

    @Test
    public void testSubmit_secondSubmitOnboardedWithCard_keepsCardAndUpdatesOtherFields() {
        when(esignPort.isAvailable(anyLong())).thenReturn(false);
        String idCardNo = "110101199001010107";
        String mobile = "13800000107";
        String effectiveCard = "6222021234567890123"; // 与 fullReq 同一张
        Long payeeId = createOnboardedPayee(idCardNo, mobile, effectiveCard);

        OnboardingWizardSubmitReqVO reqVO = fullReq(idCardNo, mobile);
        reqVO.setAddress("换了个住址");
        reqVO.setIdSignDate("2021-01-01");

        OnboardingWizardSubmitRespVO resp = onboardingWizardService.submit(reqVO);

        assertEquals(payeeId, resp.getPayeeId());
        PayeeInfoDO payee = payeeInfoMapper.selectById(payeeId);
        assertEquals(effectiveCard, payee.getBankCardNo(), "生效中的卡不许由本路径改写");
        assertEquals("中国工商银行", payee.getBankName());
        assertEquals("1", payee.getAccountCode());
        assertEquals("换了个住址", payee.getAddress(), "非卡字段照常更新");
        assertEquals("2021-01-01", payee.getIdSignDate());
    }

    @Test
    public void testSubmit_secondSubmitOnboardedWithDifferentCard_rejected() {
        when(esignPort.isAvailable(anyLong())).thenReturn(false);
        String idCardNo = "110101199001010108";
        String mobile = "13800000108";
        String effectiveCard = "6222021111111111111";
        Long payeeId = createOnboardedPayee(idCardNo, mobile, effectiveCard);

        // 已入驻的人拍了一张新卡：本路径只办首卡，必须走换卡单（#37），不能静默写成与工行不一致的卡
        OnboardingWizardSubmitReqVO reqVO = fullReq(idCardNo, mobile);
        reqVO.setBankCardNo("6222022222222222222");

        assertServiceException(() -> onboardingWizardService.submit(reqVO), WIZARD_CARD_CHANGE_REQUIRES_CHANGE_ORDER);

        PayeeInfoDO payee = payeeInfoMapper.selectById(payeeId);
        assertEquals(effectiveCard, payee.getBankCardNo());
        assertEquals("中国工商银行", payee.getBankName());
        assertEquals("1", payee.getAccountCode());
    }

    @Test
    public void testSubmit_secondSubmitWithPendingCardChange_rejected() {
        when(esignPort.isAvailable(anyLong())).thenReturn(false);
        String idCardNo = "110101199001010109";
        String mobile = "13800000109";
        Long payeeId = createOnboardedPayee(idCardNo, mobile, "6222021111111111111");
        PayeeBankCardChangeSaveReqVO change = new PayeeBankCardChangeSaveReqVO();
        change.setPayeeId(payeeId);
        change.setNewBankCardNo("6222023333333333333");
        payeeBankCardChangeService.requestChange(change);

        OnboardingWizardSubmitReqVO reqVO = fullReq(idCardNo, mobile);
        reqVO.setBankCardNo("6222024444444444444");

        assertServiceException(() -> onboardingWizardService.submit(reqVO), WIZARD_CARD_CHANGE_IN_PROGRESS);
    }

    @Test
    public void testSubmit_missingBankCardRejected() {
        when(esignPort.isAvailable(anyLong())).thenReturn(false);
        OnboardingWizardSubmitReqVO reqVO = fullReq("110101199001010103", "13800000103");
        reqVO.setBankCardNo(null);

        assertServiceException(() -> onboardingWizardService.submit(reqVO), WIZARD_BANK_CARD_NO_REQUIRED);
    }

    // ==================== 助手 ====================

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

    /** 建一个「已有卡、入驻未受理」的档案（卡但未送工行）。 */
    private Long createPayeeWithCard(String idCardNo, String mobile, String bankCardNo) {
        PayeeInfoSaveReqVO reqVO = payeeReq(idCardNo, mobile, "2020-01-01", "2030-01-01");
        reqVO.setBankCardNo(bankCardNo);
        reqVO.setBankName("中国工商银行");
        reqVO.setAccountCode("1");
        return payeeInfoService.createPayeeInfo(reqVO);
    }

    /** 建一个「已入驻 READY + 已有生效卡」的档案（换卡边界用例的起点）。 */
    private Long createOnboardedPayee(String idCardNo, String mobile, String bankCardNo) {
        Long payeeId = createPayeeWithCard(idCardNo, mobile, bankCardNo);
        PayeeInfoDO update = new PayeeInfoDO();
        update.setId(payeeId);
        update.setOnboardingState(PayeeOnboardingOutcomeEnum.READY.getCode());
        payeeInfoMapper.updateById(update);
        return payeeId;
    }

    /** 别的回收企业给同一个人建档用的入参（跨租户复用测试）。 */
    private PayeeInfoSaveReqVO payeeReq(String idCardNo, String mobile, String idSignDate, String idValidityPeriod) {
        PayeeInfoSaveReqVO reqVO = new PayeeInfoSaveReqVO();
        reqVO.setName("张三");
        reqVO.setIdCardNo(idCardNo);
        reqVO.setMobile(mobile);
        reqVO.setIdSignDate(idSignDate);
        reqVO.setIdValidityPeriod(idValidityPeriod);
        return reqVO;
    }

}
