package cn.iocoder.yudao.module.icbc.service.wizard;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.icbc.UnitTestConfiguration;
import cn.iocoder.yudao.module.icbc.controller.admin.naturalperson.vo.NaturalPersonRegisterReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.wizard.vo.*;
import cn.iocoder.yudao.module.icbc.dal.dataobject.agreement.IcbcFrameworkAgreementDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.naturalperson.IcbcNaturalPersonDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payee.PayeeInfoDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.agreement.IcbcFrameworkAgreementMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.payee.PayeeInfoMapper;
import cn.iocoder.yudao.module.icbc.enums.FrameworkAgreementSignMethodEnum;
import cn.iocoder.yudao.module.icbc.service.cardrecognition.CardRecognitionPort;
import cn.iocoder.yudao.module.icbc.service.esign.EsignPort;
import cn.iocoder.yudao.module.icbc.service.naturalperson.NaturalPersonService;
import cn.iocoder.yudao.module.icbc.service.wizard.impl.OnboardingWizardServiceImpl;
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
import static org.mockito.Mockito.*;

/**
 * {@link OnboardingWizardServiceImpl} 的单元测试。
 *
 * <p>两条缝各自 mock、都不触网：卡证识别端口（ADR 0037）与电子签章端口（ADR 0036）。
 * 断言的是外部可观察行为——识别结果只在空缺处回填、落库落到了自然人主体与收方档案、
 * 协议落 {@code PAPER} 且**没有**调电子签章端口的发起 / 链接。
 */
@Import({OnboardingWizardServiceImpl.class, UnitTestConfiguration.class})
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

    @MockBean
    private CardRecognitionPort cardRecognitionPort;

    @MockBean
    private EsignPort esignPort;

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

        // 收方档案：姓名 / 证件号 / 证件有效期 / 卡号 / 开户行 / 住址 / 是否我行卡
        PayeeInfoDO payee = payeeInfoMapper.selectById(resp.getPayeeId());
        assertNotNull(payee);
        assertEquals("张三", payee.getName());
        assertEquals("110101199001010101", payee.getIdCardNo());
        assertEquals("13800000101", payee.getMobile());
        assertEquals("2020-01-01", payee.getIdSignDate());
        assertEquals("2030-01-01", payee.getIdValidityPeriod());
        assertEquals("6222021234567890123", payee.getBankCardNo());
        assertEquals("中国工商银行", payee.getBankName());
        assertEquals("北京市朝阳区某街道 1 号", payee.getAddress());
        assertEquals("1", payee.getAccountCode(), "是否我行卡要落在收方档案上，供后续收方入驻取用");
        assertNotNull(payee.getNaturalPersonId(), "收方档案必须挂到平台级自然人主体上");

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
    public void testSubmit_sameIdCardReusesExistingNaturalPerson() {
        when(esignPort.isAvailable(anyLong())).thenReturn(false);
        // 同一身份证已在别的回收企业建过平台级自然人主体（ADR 0017）
        NaturalPersonRegisterReqVO register = new NaturalPersonRegisterReqVO();
        register.setName("张三");
        register.setIdCardNo("110101199001010102");
        register.setMobile("13800000102");
        IcbcNaturalPersonDO existing = naturalPersonService.register(register);

        OnboardingWizardSubmitRespVO resp = onboardingWizardService.submit(
                fullReq("110101199001010102", "13800000102"));

        assertEquals(existing.getId(), resp.getNaturalPersonId(),
                "同一自然人在第二家回收企业建档要复用同一个自然人主体（ADR 0017）");
        assertEquals(existing.getId(), payeeInfoMapper.selectById(resp.getPayeeId()).getNaturalPersonId());
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

}
