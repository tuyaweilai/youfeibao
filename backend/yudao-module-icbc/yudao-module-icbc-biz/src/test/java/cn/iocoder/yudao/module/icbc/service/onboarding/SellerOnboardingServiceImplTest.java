package cn.iocoder.yudao.module.icbc.service.onboarding;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.icbc.UnitTestConfiguration;
import cn.iocoder.yudao.module.icbc.controller.admin.naturalperson.vo.NaturalPersonRegisterReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.onboarding.vo.*;
import cn.iocoder.yudao.module.icbc.controller.admin.payee.vo.PayeeBankCardChangeSaveReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.agreement.IcbcFrameworkAgreementDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.authorization.IcbcSellerAuthorizationDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.lead.IcbcContactLeadDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.naturalperson.IcbcNaturalPersonDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payee.IcbcPayeeBankCardChangeDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payee.PayeeInfoDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payer.PayerInfoDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.agreement.IcbcFrameworkAgreementMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.authorization.IcbcSellerAuthorizationMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.lead.IcbcContactLeadMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.payee.PayeeInfoMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.payer.PayerInfoMapper;
import cn.iocoder.yudao.module.icbc.enums.IcbcStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.PayeeBankCardChangeStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.PayeeOnboardingOperaTypeEnum;
import cn.iocoder.yudao.module.icbc.enums.PayeeOnboardingOutcomeEnum;
import cn.iocoder.yudao.module.icbc.enums.PayeeRealNameStatusEnum;
import cn.iocoder.yudao.module.icbc.gateway.IcbcGateway;
import cn.iocoder.yudao.module.icbc.gateway.IcbcGatewayResult;
import cn.iocoder.yudao.module.icbc.gateway.model.FaceVerifyPageReq;
import cn.iocoder.yudao.module.icbc.gateway.model.FaceVerifyStatus;
import cn.iocoder.yudao.module.icbc.gateway.model.IcbcPage;
import cn.iocoder.yudao.module.icbc.gateway.model.PayeeBankCardUpdateReq;
import cn.iocoder.yudao.module.icbc.gateway.model.PayeeOnboardingReceipt;
import cn.iocoder.yudao.module.icbc.gateway.model.PayeeOnboardingReq;
import cn.iocoder.yudao.module.icbc.gateway.model.PayeeOnboardingStatus;
import cn.iocoder.yudao.module.icbc.service.naturalperson.NaturalPersonService;
import cn.iocoder.yudao.module.icbc.service.esign.FrameworkAgreementEsignService;
import cn.iocoder.yudao.module.icbc.service.onboarding.impl.SellerOnboardingServiceImpl;
import cn.iocoder.yudao.module.icbc.service.payee.PayeeBankCardChangeService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link SellerOnboardingServiceImpl} 的单元测试。
 *
 * <p>断言外部可观察行为：工行侧收到的指令、**自然人主体**上的实人认证结果、**收方档案**上的入驻状态、
 * 建档总览的结论。假适配层（{@link IcbcGateway} mock）记录调用序列。
 *
 * <p>归属划分见 ADR 0017：实人认证记在自然人主体上（跨企业复用），收方入驻记在收方档案上
 * （与子商户绑定的动作）。
 */
@Import({SellerOnboardingServiceImpl.class, UnitTestConfiguration.class})
@TestPropertySource(properties = {
        // 实名回跳（#82）要签发公开令牌，并需要自然人端入口地址
        "icbc.public-token.secret=test-public-token-secret-0123456789abcdef",
        "icbc.notify.seller-app-url=https://seller.example.com"
})
@Transactional
@Rollback
public class SellerOnboardingServiceImplTest extends BaseDbUnitTest {

    /** 本租户的子商户编号（= 付方档案的合作方付方编号） */
    private static final String OUT_VENDOR_ID = "PAYER_SUB_1";

    @Resource
    private SellerOnboardingService sellerOnboardingService;

    @Resource
    private NaturalPersonService naturalPersonService;

    @Resource
    private PayeeBankCardChangeService bankCardChangeService;

    @Resource
    private PayeeInfoMapper payeeInfoMapper;
    @Resource
    private PayerInfoMapper payerInfoMapper;
    @Resource
    private IcbcFrameworkAgreementMapper frameworkAgreementMapper;
    @Resource
    private IcbcSellerAuthorizationMapper sellerAuthorizationMapper;
    @Resource
    private IcbcContactLeadMapper contactLeadMapper;

    @MockBean
    private IcbcGateway icbcGateway;

    /** 电子签发起由 {@code FrameworkAgreementEsignServiceImplTest} 覆盖；本类只测落库与门禁。 */
    @MockBean
    private FrameworkAgreementEsignService frameworkAgreementEsignService;

    @BeforeEach
    public void setUp() {
        // 实名回跳要签发公开令牌（#82），而签发必须在某个租户上下文中
        TenantContextHolder.setTenantId(1L);
    }

    @AfterEach
    public void tearDown() {
        TenantContextHolder.clear();
    }

    // ==================== 回头客 ====================

    @Test
    public void testFindReturningCustomer_byIdCardAndMobile() {
        PayeeInfoDO payee = insertPayee("USER_RETURN", "110101199001010001", "13800000001");

        // 扫身份证带档
        assertEquals(payee.getId(),
                sellerOnboardingService.findReturningCustomer("110101199001010001", null).getId());
        // 手机号带档
        assertEquals(payee.getId(),
                sellerOnboardingService.findReturningCustomer(null, "13800000001").getId());
        // 都查不到
        assertNull(sellerOnboardingService.findReturningCustomer("110101199001019999", "13900000000"));
    }

    // ==================== 实人认证（记在自然人主体上） ====================

    @Test
    public void testStartRealName_usesPlatformOutUserIdAndMarksPending() {
        PayeeInfoDO payee = insertPayee("USER_A", "110101199001010002", "13800000002");
        IcbcNaturalPersonDO person = personOf(payee);
        when(icbcGateway.submitFaceVerification(any())).thenReturn(IcbcGatewayResult.success(
                IcbcPage.builder().formHtml("<form id=\"face\"/>").build(), 0, "成功"));

        SellerStepRespVO step = sellerOnboardingService.startRealName(realNameReq(payee.getId()));

        assertEquals("<form id=\"face\"/>", step.getFormHtml());
        assertEquals("REAL_NAME", step.getStep());
        // 发给工行的是**平台级**外部用户编号，不是收方档案编号
        ArgumentCaptor<FaceVerifyPageReq> captor = ArgumentCaptor.forClass(FaceVerifyPageReq.class);
        verify(icbcGateway).submitFaceVerification(captor.capture());
        assertEquals(person.getOutUserId(), captor.getValue().getOutUserId());
        // 认证中记在自然人主体上，收方档案不再承载实人认证状态
        assertEquals(PayeeRealNameStatusEnum.PENDING.getStatus(),
                naturalPersonService.getNaturalPerson(person.getId()).getRealNameStatus());
    }

    @Test
    public void testStartRealName_sendsSuccessAndFailJumpUrlsWithBusinessKey() {
        PayeeInfoDO payee = insertPayee("USER_JUMP", "110101199001010077", "13800000077");
        when(icbcGateway.submitFaceVerification(any())).thenReturn(IcbcGatewayResult.success(
                IcbcPage.builder().formHtml("<form id='face'/>").build(), 0, "成功"));

        sellerOnboardingService.startRealName(realNameReq(payee.getId()));

        // 工行把结果页「确认」跳回我们的自然人端：成功与失败两个地址都要上送，各自带上令牌（#82）
        ArgumentCaptor<FaceVerifyPageReq> captor = ArgumentCaptor.forClass(FaceVerifyPageReq.class);
        verify(icbcGateway).submitFaceVerification(captor.capture());
        FaceVerifyPageReq req = captor.getValue();
        assertTrue(req.getJumpUrl().startsWith("https://seller.example.com/#/?token="), req.getJumpUrl());
        assertTrue(req.getJumpUrl().contains("purpose=ONBOARDING"), req.getJumpUrl());
        assertTrue(req.getJumpUrl().endsWith("from=face-success"), req.getJumpUrl());
        assertTrue(req.getFailJumpUrl().startsWith("https://seller.example.com/#/?token="), req.getFailJumpUrl());
        assertTrue(req.getFailJumpUrl().endsWith("from=face-fail"), req.getFailJumpUrl());
        // 两个地址定位的是同一笔：同一个令牌
        assertEquals(tokenOf(req.getJumpUrl()), tokenOf(req.getFailJumpUrl()));
    }

    @Test
    public void testSyncRealName_passedRecordsOnNaturalPerson() {
        PayeeInfoDO payee = insertPayee("USER_B", "110101199001010003", "13800000003");
        IcbcNaturalPersonDO person = personOf(payee);
        when(icbcGateway.queryFaceVerification(eq(person.getOutUserId()))).thenReturn(IcbcGatewayResult.success(
                FaceVerifyStatus.builder().outUserId(person.getOutUserId()).authResult("02").passed(true).build(),
                0, "成功"));

        sellerOnboardingService.syncRealName(payee.getId());

        IcbcNaturalPersonDO updated = naturalPersonService.getNaturalPerson(person.getId());
        assertEquals(PayeeRealNameStatusEnum.PASSED.getStatus(), updated.getRealNameStatus());
        assertNotNull(updated.getRealNameTime());
    }

    @Test
    public void testHandleFaceVerifyNotify_failedRecordsOnNaturalPerson() {
        PayeeInfoDO payee = insertPayee("USER_C", "110101199001010004", "13800000004");
        IcbcNaturalPersonDO person = personOf(payee);

        sellerOnboardingService.handleFaceVerifyNotify(person.getOutUserId(), false, "人脸比对不通过");

        IcbcNaturalPersonDO updated = naturalPersonService.getNaturalPerson(person.getId());
        assertEquals(PayeeRealNameStatusEnum.FAILED.getStatus(), updated.getRealNameStatus());
        assertEquals("人脸比对不通过", updated.getRealNameMsg());
    }

    @Test
    public void testRealNameIsReadFromNaturalPersonNotPayee() {
        // 实人认证的归属已经移到自然人主体（ADR 0017）：建档总览读的是主体，而不是收方档案。
        // 「同一个人在两家企业只认证一次」用租户拦截器在 IcbcTenantIsolationTest 里验证
        //（同一个租户内身份证号本身就唯一，这里造不出两家企业）。
        PayeeInfoDO payee = insertPayee("USER_SHARE_A", "110101199001010099", "13800000099");
        IcbcNaturalPersonDO person = personOf(payee);
        naturalPersonService.applyRealNameResult(person.getId(), true, null);

        assertEquals(PayeeRealNameStatusEnum.PASSED.getStatus(),
                sellerOnboardingService.getOnboarding(payee.getId()).getRealNameStatus());
        // 收方档案上的同名字段不再被写入
        assertEquals(PayeeRealNameStatusEnum.NOT_STARTED.getStatus(),
                payeeInfoMapper.selectById(payee.getId()).getRealNameStatus());
    }

    // ==================== 实名通过即自动入驻（#85） ====================

    @Test
    public void testSyncRealName_passedAutoSubmitsOnboardingAndBecomesPending() {
        PayeeInfoDO payee = insertPayee("USER_AUTO_Q", "110101199001010061", "13800000061");
        IcbcNaturalPersonDO person = personOf(payee);
        insertPayer(OUT_VENDOR_ID);
        when(icbcGateway.queryFaceVerification(eq(person.getOutUserId()))).thenReturn(IcbcGatewayResult.success(
                FaceVerifyStatus.builder().outUserId(person.getOutUserId()).authResult("02").passed(true).build(),
                0, "成功"));
        when(icbcGateway.submitPayeeOnboarding(any())).thenReturn(IcbcGatewayResult.success(
                PayeeOnboardingReceipt.builder().outUserId(person.getOutUserId()).build(), 0, "受理成功"));

        PayeeInfoDO result = sellerOnboardingService.syncRealName(payee.getId());

        // 查一次实名，入驻就自动发起：全程不需要任何人再点「发起收方入驻」
        assertEquals(PayeeOnboardingOutcomeEnum.PENDING.getCode(), result.getOnboardingState());
        ArgumentCaptor<PayeeOnboardingReq> captor = ArgumentCaptor.forClass(PayeeOnboardingReq.class);
        verify(icbcGateway).submitPayeeOnboarding(captor.capture());
        assertEquals(person.getOutUserId(), captor.getValue().getOutUserId());
        assertEquals(OUT_VENDOR_ID, captor.getValue().getOutVendorId());
    }

    @Test
    public void testHandleFaceVerifyNotify_passedAutoSubmitsOnboarding() {
        PayeeInfoDO payee = insertPayee("USER_AUTO_N", "110101199001010062", "13800000062");
        IcbcNaturalPersonDO person = personOf(payee);
        insertPayer(OUT_VENDOR_ID);
        when(icbcGateway.submitPayeeOnboarding(any())).thenReturn(IcbcGatewayResult.success(
                PayeeOnboardingReceipt.builder().build(), 0, "受理成功"));

        sellerOnboardingService.handleFaceVerifyNotify(person.getOutUserId(), true, null);

        // 异步通知那条路也自动发起，不需要现场再点一次
        verify(icbcGateway, times(1)).submitPayeeOnboarding(any());
        assertEquals(PayeeOnboardingOutcomeEnum.PENDING.getCode(),
                payeeInfoMapper.selectById(payee.getId()).getOnboardingState());
    }

    @Test
    public void testAutoSubmit_isIdempotentAcrossNotifyAndQuery() {
        PayeeInfoDO payee = insertPayee("USER_AUTO_IDEM", "110101199001010063", "13800000063");
        IcbcNaturalPersonDO person = personOf(payee);
        insertPayer(OUT_VENDOR_ID);
        when(icbcGateway.queryFaceVerification(eq(person.getOutUserId()))).thenReturn(IcbcGatewayResult.success(
                FaceVerifyStatus.builder().outUserId(person.getOutUserId()).authResult("02").passed(true).build(),
                0, "成功"));
        when(icbcGateway.submitPayeeOnboarding(any())).thenReturn(IcbcGatewayResult.success(
                PayeeOnboardingReceipt.builder().build(), 0, "受理成功"));

        // 通知与查询都到、查询还被点了多次：只向工行发起一次
        sellerOnboardingService.handleFaceVerifyNotify(person.getOutUserId(), true, null);
        sellerOnboardingService.syncRealName(payee.getId());
        sellerOnboardingService.syncRealName(payee.getId());

        verify(icbcGateway, times(1)).submitPayeeOnboarding(any());
    }

    @Test
    public void testAutoSubmit_notTriggeredWhenRealNameFailed() {
        PayeeInfoDO payee = insertPayee("USER_AUTO_FAIL", "110101199001010064", "13800000064");
        IcbcNaturalPersonDO person = personOf(payee);
        insertPayer(OUT_VENDOR_ID);

        sellerOnboardingService.handleFaceVerifyNotify(person.getOutUserId(), false, "人脸比对不通过");

        verify(icbcGateway, never()).submitPayeeOnboarding(any());
    }

    @Test
    public void testAutoSubmit_skippedWhenAlreadyRejected() {
        PayeeInfoDO payee = insertPayee("USER_AUTO_REJ", "110101199001010065", "13800000065");
        markRealNamePassed(payee);
        insertPayer(OUT_VENDOR_ID);
        sellerOnboardingService.reconcileOnboardingStatus(payee.getId(), null, "reject", "资料不符", null);

        // 又收到一次实名通过通知，也不能把已被拒的入驻又重新发起一次（拒绝后由人工决定是否重试）
        sellerOnboardingService.handleFaceVerifyNotify(personOf(payee).getOutUserId(), true, null);

        verify(icbcGateway, never()).submitPayeeOnboarding(any());
    }

    @Test
    public void testAutoSubmit_failureVisibleAndRetryable() {
        PayeeInfoDO payee = insertPayee("USER_AUTO_ERR", "110101199001010066", "13800000066");
        IcbcNaturalPersonDO person = personOf(payee);
        insertPayer(OUT_VENDOR_ID);
        when(icbcGateway.queryFaceVerification(eq(person.getOutUserId()))).thenReturn(IcbcGatewayResult.success(
                FaceVerifyStatus.builder().outUserId(person.getOutUserId()).authResult("02").passed(true).build(),
                0, "成功"));
        when(icbcGateway.submitPayeeOnboarding(any())).thenReturn(
                IcbcGatewayResult.businessFailed(500, "工行拒绝了"));

        // 自动发起失败不回滚实名、也不静默：状态停在「未发起」，原因可见
        PayeeInfoDO result = sellerOnboardingService.syncRealName(payee.getId());
        assertNull(result.getOnboardingState());
        assertNotNull(result.getAuditMsg());
        assertTrue(sellerOnboardingService.getOnboarding(payee.getId()).getInvoiceBlockReason()
                .contains("工行接口调用失败"));
        assertEquals(PayeeRealNameStatusEnum.PASSED.getStatus(),
                naturalPersonService.getNaturalPerson(person.getId()).getRealNameStatus());

        // 人工把数据补齐后可重试（现有 submitOnboarding 入口）
        when(icbcGateway.submitPayeeOnboarding(any())).thenReturn(IcbcGatewayResult.success(
                PayeeOnboardingReceipt.builder().build(), 0, "受理成功"));
        SellerOnboardingSubmitReqVO reqVO = new SellerOnboardingSubmitReqVO();
        reqVO.setPayeeId(payee.getId());
        assertEquals(PayeeOnboardingOutcomeEnum.PENDING.getCode(),
                sellerOnboardingService.submitOnboarding(reqVO).getOnboardingState());
    }

    // ==================== 收方入驻前置 ====================

    @Test
    public void testSubmitOnboarding_rejectedBeforeRealName() {
        PayeeInfoDO payee = insertPayee("USER_D", "110101199001010005", "13800000005");

        SellerOnboardingSubmitReqVO reqVO = new SellerOnboardingSubmitReqVO();
        reqVO.setPayeeId(payee.getId());
        assertServiceException(() -> sellerOnboardingService.submitOnboarding(reqVO),
                SELLER_REAL_NAME_NOT_PASSED);
    }

    @Test
    public void testSubmitOnboarding_requiresBankCard() {
        PayeeInfoDO payee = insertPayee("USER_E", "110101199001010006", "13800000006");
        markRealNamePassed(payee);
        payeeInfoMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<PayeeInfoDO>()
                .eq(PayeeInfoDO::getId, payee.getId())
                .set(PayeeInfoDO::getBankCardNo, null));

        SellerOnboardingSubmitReqVO reqVO = new SellerOnboardingSubmitReqVO();
        reqVO.setPayeeId(payee.getId());
        assertServiceException(() -> sellerOnboardingService.submitOnboarding(reqVO),
                SELLER_BANK_CARD_REQUIRED);
    }

    @Test
    public void testSubmitOnboarding_sendsDataInterfaceFieldsWithoutPageParams() {
        PayeeInfoDO payee = insertPayee("USER_F", "110101199001010007", "13800000007");
        IcbcNaturalPersonDO person = personOf(payee);
        markRealNamePassed(payee);
        insertPayer(OUT_VENDOR_ID);
        when(icbcGateway.submitPayeeOnboarding(any())).thenReturn(IcbcGatewayResult.success(
                PayeeOnboardingReceipt.builder().outUserId(person.getOutUserId()).build(), 0, "受理成功"));

        SellerOnboardingSubmitReqVO reqVO = new SellerOnboardingSubmitReqVO();
        reqVO.setPayeeId(payee.getId());
        reqVO.setIdSignDate("2020-01-01");
        reqVO.setIdValidityPeriod("9999-12-30");
        reqVO.setBankName("中国工商银行");
        reqVO.setBankBranch("北京分行营业部");

        PayeeInfoDO accepted = sellerOnboardingService.submitOnboarding(reqVO);

        // 受理即「审核中」：同步返回只代表工行收下了申请，不能当通过
        assertEquals(PayeeOnboardingOutcomeEnum.PENDING.getCode(), accepted.getOnboardingState());
        // 子商户必须是本租户的付方档案（与预下单 / 付款同一口径），不是全局配置
        ArgumentCaptor<PayeeOnboardingReq> captor = ArgumentCaptor.forClass(PayeeOnboardingReq.class);
        verify(icbcGateway).submitPayeeOnboarding(captor.capture());
        PayeeOnboardingReq sent = captor.getValue();
        assertEquals(OUT_VENDOR_ID, sent.getOutVendorId());
        assertEquals(person.getOutUserId(), sent.getOutUserId());
        assertEquals(payee.getName(), sent.getReceiverName());
        assertEquals(payee.getBankCardNo(), sent.getReceiverAccount());
        assertEquals(payee.getIdCardNo(), sent.getIdNo());
        assertEquals(payee.getMobile(), sent.getMobile());
        assertEquals("2020-01-01", sent.getSignDate());
        assertEquals("9999-12-30", sent.getValidityPeriod());
        assertEquals("中国工商银行", sent.getBankName());
        // 职业与「是否我行用户」缺省时补上不宣称事实 / 猜错只被驳回的取值
        assertEquals("14", sent.getOccupation());
        assertEquals("1", sent.getAccountCode());
        // 银行卡识别结果写回档案
        PayeeInfoDO updated = payeeInfoMapper.selectById(payee.getId());
        assertEquals("中国工商银行", updated.getBankName());
        assertEquals("9999-12-30", updated.getIdValidityPeriod());
    }

    @Test
    public void testSubmitOnboarding_doesNotResubmitWhilePendingOrReady() {
        PayeeInfoDO pendingPayee = insertPayee("USER_IDEM_P", "110101199001010051", "13800000051");
        markRealNamePassed(pendingPayee);
        insertPayer(OUT_VENDOR_ID);
        SellerOnboardingSubmitReqVO reqVO = new SellerOnboardingSubmitReqVO();
        reqVO.setPayeeId(pendingPayee.getId());
        when(icbcGateway.submitPayeeOnboarding(any())).thenReturn(IcbcGatewayResult.success(
                PayeeOnboardingReceipt.builder().build(), 0, "受理成功"));

        sellerOnboardingService.submitOnboarding(reqVO);
        sellerOnboardingService.submitOnboarding(reqVO);
        // 已在途：工行只收到一次（未知时先查询，不重复提交）
        verify(icbcGateway, times(1)).submitPayeeOnboarding(any());
    }

    @Test
    public void testSubmitOnboarding_withoutPayerIsRejected() {
        PayeeInfoDO payee = insertPayee("USER_NOPAYER", "110101199001010008", "13800000008");
        markRealNamePassed(payee);

        SellerOnboardingSubmitReqVO reqVO = new SellerOnboardingSubmitReqVO();
        reqVO.setPayeeId(payee.getId());
        // 没有付方档案就定不出子商户（回收企业），不能拿全局配置糊过去
        assertServiceException(() -> sellerOnboardingService.submitOnboarding(reqVO),
                SELLER_ONBOARDING_PAYER_NOT_CONFIGURED);
    }

    // ==================== 审核一条线：三个状态 ====================

    @Test
    public void testReconcileOnboarding_threeOutcomes() {
        // 回调带 result=pass → 通过
        assertOutcome(1, null, "pass", PayeeOnboardingOutcomeEnum.READY, true,
                IcbcStatusEnum.AuditStatus.APPROVED.getStatus());
        // 回调带 result=reject → 拒绝
        assertOutcome(2, null, "reject", PayeeOnboardingOutcomeEnum.REJECTED, false,
                IcbcStatusEnum.AuditStatus.REJECTED.getStatus());
        // 查询带 auditStatus=1 → 通过（查询接口不返回 result）
        assertOutcome(3, "1", null, PayeeOnboardingOutcomeEnum.READY, true,
                IcbcStatusEnum.AuditStatus.APPROVED.getStatus());
        // 查询带 auditStatus=2 → 还在审，落「审核中」
        assertOutcome(4, "2", null, PayeeOnboardingOutcomeEnum.PENDING, false, null);
    }

    private void assertOutcome(int seq, String auditStatus, String result, PayeeOnboardingOutcomeEnum expected,
                               boolean eligible, Integer expectedAuditStatus) {
        PayeeInfoDO payee = insertPayee("USER_CASE_" + seq,
                "11010119900101" + String.format("%04d", 1000 + seq),
                "138" + String.format("%08d", 10000000 + seq));

        PayeeInfoDO updated = sellerOnboardingService.reconcileOnboardingStatus(
                payee.getId(), auditStatus, result, "reject".equals(result) ? "资料不符" : null, null);

        assertEquals(expected.getCode(), updated.getOnboardingState());
        assertEquals(expected.isInvoiceEligible(), eligible);
        if (expected == PayeeOnboardingOutcomeEnum.READY) {
            assertEquals("1", updated.getIcbcReceiverStatus());
        } else {
            assertEquals("0", updated.getIcbcReceiverStatus());
        }
        if (expectedAuditStatus != null) {
            assertEquals(expectedAuditStatus, updated.getStatus());
        }
    }

    @Test
    public void testReconcileOnboarding_unknownResultDoesNotAdvance() {
        PayeeInfoDO payee = insertPayee("USER_PENDING", "110101199001010020", "13800000020");

        // 两条输入都没给（结果未回）→ 不推进状态机
        PayeeInfoDO updated = sellerOnboardingService.reconcileOnboardingStatus(
                payee.getId(), null, null, null, null);
        assertNull(updated.getOnboardingState());
    }

    // ==================== 入驻通知按子商户归位 ====================

    @Test
    public void testHandleOnboardingNotify_reconcilesOwnTenantPayee() {
        PayeeInfoDO payee = insertPayee("USER_NOTIFY", "110101199001010021", "13800000021");
        IcbcNaturalPersonDO person = personOf(payee);
        insertPayer(OUT_VENDOR_ID);

        sellerOnboardingService.handleOnboardingNotify(person.getOutUserId(), OUT_VENDOR_ID,
                "pass", null, null);

        PayeeInfoDO updated = payeeInfoMapper.selectById(payee.getId());
        assertEquals(PayeeOnboardingOutcomeEnum.READY.getCode(), updated.getOnboardingState());
    }

    @Test
    public void testHandleOnboardingNotify_rejectedCarriesReason() {
        // 数据接口回调只带 result，拒绝仍要可见
        PayeeInfoDO payee = insertPayee("USER_REJECT_ONLY", "110101199001010031", "13800000031");
        IcbcNaturalPersonDO person = personOf(payee);
        insertPayer(OUT_VENDOR_ID);

        sellerOnboardingService.handleOnboardingNotify(person.getOutUserId(), OUT_VENDOR_ID,
                "reject", "资料不符", null);

        PayeeInfoDO updated = payeeInfoMapper.selectById(payee.getId());
        assertEquals(PayeeOnboardingOutcomeEnum.REJECTED.getCode(), updated.getOnboardingState());
        assertEquals("资料不符", updated.getRejectReason());
        assertEquals(IcbcStatusEnum.AuditStatus.REJECTED.getStatus(), updated.getStatus());
    }

    @Test
    public void testHandleOnboardingNotify_unknownVendorIsFailureNotGuess() {
        // 定位不到是哪家回收企业时抛出去让通知落失败，在通知监控里人工处理——不猜、不跨企业乱写
        PayeeInfoDO payee = insertPayee("USER_VENDOR_UNKNOWN", "110101199001010032", "13800000032");
        IcbcNaturalPersonDO person = personOf(payee);

        assertServiceException(() -> sellerOnboardingService.handleOnboardingNotify(person.getOutUserId(),
                "NOT_A_PAYER", "pass", null, null), SELLER_ONBOARDING_VENDOR_UNRESOLVED, "NOT_A_PAYER");
        assertNull(payeeInfoMapper.selectById(payee.getId()).getOnboardingState());
    }

    @Test
    public void testSyncOnboarding_viaGatewayQuery() {
        PayeeInfoDO payee = insertPayee("USER_SYNC", "110101199001010022", "13800000022");
        IcbcNaturalPersonDO person = personOf(payee);
        insertPayer(OUT_VENDOR_ID);
        when(icbcGateway.queryPayeeOnboarding(eq(person.getOutUserId()), eq(OUT_VENDOR_ID)))
                .thenReturn(IcbcGatewayResult.success(
                        PayeeOnboardingStatus.builder().outUserId(person.getOutUserId())
                                .auditStatus("1").build(), 0, "成功"));

        PayeeInfoDO updated = sellerOnboardingService.syncOnboarding(payee.getId());

        assertEquals(PayeeOnboardingOutcomeEnum.READY.getCode(), updated.getOnboardingState());
    }

    // ==================== 留联系方式 ====================

    @Test
    public void testLeaveContactFallback() {
        PayeeInfoDO payee = insertPayee("USER_LEAD", "110101199001010023", "13800000023");

        SellerContactFallbackReqVO reqVO = new SellerContactFallbackReqVO();
        reqVO.setPayeeId(payee.getId());
        reqVO.setMobile("13900000001");
        reqVO.setRemark("审核拒绝，等待回电");
        sellerOnboardingService.leaveContactFallback(reqVO);

        List<IcbcContactLeadDO> leads = contactLeadMapper.selectListByPayeeId(payee.getId());
        assertEquals(1, leads.size());
        assertEquals("13900000001", leads.get(0).getMobile());
    }

    // ==================== 框架收购协议 ====================

    @Test
    public void testSaveFrameworkAgreement_keepsSingleActive() {
        PayeeInfoDO payee = insertPayee("USER_AGREE", "110101199001010024", "13800000024");

        Long first = sellerOnboardingService.saveFrameworkAgreement(agreementReq(payee.getId(), "废钢"));
        Long second = sellerOnboardingService.saveFrameworkAgreement(agreementReq(payee.getId(), "废纸"));

        // 只有一份生效协议，且是后一份
        IcbcFrameworkAgreementDO active = sellerOnboardingService.getActiveFrameworkAgreement(payee.getId());
        assertNotNull(active);
        assertEquals(second, active.getId());
        assertEquals("废纸", active.getProductName());
        // 旧协议作废留痕
        assertEquals(2, frameworkAgreementMapper.selectById(first).getStatus());
        assertEquals(2, sellerOnboardingService.getFrameworkAgreements(payee.getId()).size());
    }

    @Test
    public void testSaveFrameworkAgreement_noRequiredFields() {
        // Bean Validation 由 controller 层触发；service 层只保证落库字段完整
        PayeeInfoDO payee = insertPayee("USER_AGREE2", "110101199001010025", "13800000025");
        Long id = sellerOnboardingService.saveFrameworkAgreement(agreementReq(payee.getId(), "废钢"));
        assertNotNull(frameworkAgreementMapper.selectById(id).getAgreementNo());
    }

    @Test
    public void testSaveFrameworkAgreement_electronicCannotBeSelfMarkedEffective() {
        // SP-4：手送 signMethod=ELECTRONIC&status=1 不能再当场盖 signedAt（#81 Problem Statement 点名的那件事）
        PayeeInfoDO payee = insertPayee("USER_AGREE_E", "110101199001010031", "13800000031");
        FrameworkAgreementSaveReqVO reqVO = agreementReq(payee.getId(), "废钢");
        reqVO.setSignMethod("ELECTRONIC");
        reqVO.setStatus(1);

        Long id = sellerOnboardingService.saveFrameworkAgreement(reqVO);

        IcbcFrameworkAgreementDO stored = frameworkAgreementMapper.selectById(id);
        assertEquals(0, stored.getStatus(), "电子协议只能停在待签署：生效只能由签署回调推");
        assertNull(stored.getSignedAt(), "没人签过就不该有签署时间");
        verify(frameworkAgreementEsignService).initiate(eq(1L), eq(id));
    }

    @Test
    public void testSaveFrameworkAgreement_updateKeepsSignatureStateAndInitiatesWhenTaskMissing() {
        // SP-4：更新不得把待签署改成生效，也不能改成待签署却不发起（否则「去签署」必然报任务号缺失）
        PayeeInfoDO payee = insertPayee("USER_AGREE_U", "110101199001010032", "13800000032");
        IcbcFrameworkAgreementDO existing = IcbcFrameworkAgreementDO.builder()
                .payeeId(payee.getId()).agreementNo("FW_UPDATE_1").productName("废钢").quantity("5 吨")
                .specification("重型").recyclePeriod("2026 年 9 月第 1 期").settlementMethod("银行转账")
                .signMethod("ELECTRONIC").status(0).build();
        frameworkAgreementMapper.insert(existing);

        FrameworkAgreementSaveReqVO reqVO = agreementReq(payee.getId(), "废纸");
        reqVO.setId(existing.getId());
        reqVO.setSignMethod("ELECTRONIC");
        reqVO.setStatus(1);

        Long id = sellerOnboardingService.saveFrameworkAgreement(reqVO);

        assertEquals(existing.getId(), id);
        IcbcFrameworkAgreementDO stored = frameworkAgreementMapper.selectById(id);
        assertEquals(0, stored.getStatus(), "更新不得把待签署手改成生效");
        assertNull(stored.getSignedAt());
        assertEquals("废纸", stored.getProductName(), "协议要素照常更新");
        verify(frameworkAgreementEsignService).initiate(eq(1L), eq(existing.getId()));
    }

    @Test
    public void testSaveFrameworkAgreement_updateCannotStampSignTimeOrFileUrl() {
        // #81 Problem Statement / #95 SP-4：修改仍走 toAgreement(reqVO, existing.getStatus())，
        // 而 updateById 只写非空字段。若不显式清空，POST /agreement/create {id=<待签署协议>,
        // signedAt=…} 就能给一份没人签过、状态仍是待签署的协议盖上签署时间。
        PayeeInfoDO payee = insertPayee("USER_AGREE_STAMP", "110101199001010033", "13800000033");
        IcbcFrameworkAgreementDO existing = IcbcFrameworkAgreementDO.builder()
                .payeeId(payee.getId()).agreementNo("FW_STAMP_1").productName("废钢").quantity("5 吨")
                .specification("重型").recyclePeriod("2026 年 9 月第 1 期").settlementMethod("银行转账")
                .signMethod("ELECTRONIC").signTaskId("TASK_STAMP").status(0).build();
        frameworkAgreementMapper.insert(existing);

        FrameworkAgreementSaveReqVO reqVO = agreementReq(payee.getId(), "废纸");
        reqVO.setId(existing.getId());
        reqVO.setSignedAt(LocalDateTime.of(2026, 9, 21, 10, 0));
        reqVO.setFileUrl("https://forged/signed.pdf");

        sellerOnboardingService.saveFrameworkAgreement(reqVO);

        IcbcFrameworkAgreementDO stored = frameworkAgreementMapper.selectById(existing.getId());
        assertNull(stored.getSignedAt(), "没人签过，外部入参不得盖上签署时间");
        assertNull(stored.getFileUrl(), "文书地址只能由签署回调写");
        assertEquals(0, stored.getStatus(), "状态仍是待签署");
    }

    // ==================== 首次授权 ====================

    @Test
    public void testAuthorizeSeller_recordsTrace() {
        PayeeInfoDO payee = insertPayee("USER_AUTH", "110101199001010026", "13800000026");

        SellerAuthorizationSaveReqVO reqVO = new SellerAuthorizationSaveReqVO();
        reqVO.setPayeeId(payee.getId());
        reqVO.setReverseInvoiceAuthorized(true);
        reqVO.setTaxAgencyAuthorized(true);
        reqVO.setChannel("ICBC_H5");
        reqVO.setOperator("李四");
        Long id = sellerOnboardingService.authorizeSeller(reqVO);

        IcbcSellerAuthorizationDO saved = sellerAuthorizationMapper.selectById(id);
        assertEquals(Boolean.TRUE, saved.getReverseInvoiceAuthorized());
        assertEquals(Boolean.TRUE, saved.getTaxAgencyAuthorized());
        assertNotNull(saved.getAuthorizedAt());
        assertEquals("李四", saved.getOperator());
    }

    // ==================== 开票门禁 ====================

    @Test
    public void testAssertReadyForInvoice_fullChain() {
        PayeeInfoDO payee = insertPayee("USER_READY", "110101199001010027", "13800000027");
        markRealNamePassed(payee);
        sellerOnboardingService.reconcileOnboardingStatus(payee.getId(), null, "pass", null, null);
        sellerOnboardingService.saveFrameworkAgreement(agreementReq(payee.getId(), "废钢"));
        authorize(payee.getId());

        // 全链路齐备 → 放行
        assertDoesNotThrow(() -> sellerOnboardingService.assertReadyForInvoice(payee.getId()));
        SellerOnboardingRespVO overview = sellerOnboardingService.getOnboarding(payee.getId());
        assertTrue(overview.getInvoiceEligible());
        assertNull(overview.getInvoiceBlockReason());
        assertNotNull(overview.getFrameworkAgreement());
        assertNotNull(overview.getAuthorization());
        // 建档总览要带出平台级身份，方便定位「这个人是谁」
        assertEquals(personOf(payee).getId(), overview.getNaturalPersonId());
        assertEquals(personOf(payee).getOutUserId(), overview.getOutUserId());
    }

    @Test
    public void testAssertReadyForInvoice_blockedOnRejected() {
        PayeeInfoDO payee = insertPayee("USER_REJECT", "110101199001010028", "13800000028");
        markRealNamePassed(payee);
        sellerOnboardingService.reconcileOnboardingStatus(payee.getId(), null, "reject", "资料不符", null);
        sellerOnboardingService.saveFrameworkAgreement(agreementReq(payee.getId(), "废钢"));
        authorize(payee.getId());

        // 审核拒绝 → 即使协议与授权齐备也不能开票
        assertServiceException(() -> sellerOnboardingService.assertReadyForInvoice(payee.getId()),
                SELLER_ONBOARDING_NOT_READY);
        SellerOnboardingRespVO overview = sellerOnboardingService.getOnboarding(payee.getId());
        assertFalse(overview.getInvoiceEligible());
        assertEquals("审核拒绝", overview.getInvoiceBlockReason());
        assertEquals("资料不符", overview.getRejectReason());
    }

    @Test
    public void testAssertReadyForInvoice_blockedOnMissingAgreement() {
        PayeeInfoDO payee = insertPayee("USER_NOAGREE", "110101199001010029", "13800000029");
        markRealNamePassed(payee);
        sellerOnboardingService.reconcileOnboardingStatus(payee.getId(), null, "pass", null, null);
        authorize(payee.getId());

        assertServiceException(() -> sellerOnboardingService.assertReadyForInvoice(payee.getId()),
                SELLER_ONBOARDING_NOT_READY);
        assertEquals("未签署生效的框架收购协议",
                sellerOnboardingService.getOnboarding(payee.getId()).getInvoiceBlockReason());
    }

    @Test
    public void testAssertReadyForInvoiceByOutUserId_unknownIdentityIsAllowed() {
        // 平台级身份层查不到这个人时放行，保持对既有数据的兼容
        assertDoesNotThrow(() -> sellerOnboardingService.assertReadyForInvoiceByOutUserId("NP_NOT_EXIST"));
    }

    @Test
    public void testAssertReadyForInvoiceByOutUserId_blocksIncompletePayee() {
        PayeeInfoDO payee = insertPayee("USER_GATE", "110101199001010030", "13800000030");

        assertServiceException(
                () -> sellerOnboardingService.assertReadyForInvoiceByOutUserId(personOf(payee).getOutUserId()),
                SELLER_ONBOARDING_NOT_READY);
    }

    // ==================== 换银行卡（#37） ====================

    @Test
    public void testSubmitOnboarding_routesPendingChangeToCardUpdate() {
        PayeeInfoDO payee = insertPayee("USER_CHANGE", "110101199001010041", "13800000041");
        markReady(payee);
        insertPayer(OUT_VENDOR_ID);
        // 「是否我行卡」是换卡发起侧在变更单上定下来的（#86），不从入驻提交 VO 借
        PayeeBankCardChangeSaveReqVO changeReq = changeReq(payee.getId(), "6222029999888877", "中国工商银行");
        changeReq.setAccountCode("0");
        bankCardChangeService.requestChange(changeReq);
        when(icbcGateway.updatePayeeBankCard(any())).thenReturn(IcbcGatewayResult.success(
                PayeeOnboardingReceipt.builder().build(), 0, "受理成功"));

        SellerOnboardingSubmitReqVO reqVO = new SellerOnboardingSubmitReqVO();
        reqVO.setPayeeId(payee.getId());
        // 故意传一个假的旧卡识别结果：换卡在途时不该被它覆盖
        reqVO.setBankName("骗人的银行");
        reqVO.setIdValidityPeriod("1999-01-01");
        sellerOnboardingService.submitOnboarding(reqVO);

        // 换卡走的是**收方修改**，不是重复新增（同一个 outUserId 不能重复入驻）
        ArgumentCaptor<PayeeBankCardUpdateReq> captor = ArgumentCaptor.forClass(PayeeBankCardUpdateReq.class);
        verify(icbcGateway).updatePayeeBankCard(captor.capture());
        verify(icbcGateway, never()).submitPayeeOnboarding(any());
        // 送给工行的是**变更单上的新卡**，不是生效中的旧卡
        assertEquals("6222029999888877", captor.getValue().getReceiverAccount());
        // 是否我行卡也从变更单上取
        assertEquals("0", captor.getValue().getAccountCode());
        // 新卡行名也从变更单上取，不把入驻提交里那个假的识别结果带上
        assertEquals("中国工商银行", captor.getValue().getBankName());
        // 生效卡与档案字段都不动：换卡要等工行审核通过（PayeeBankCardChangeService）
        PayeeInfoDO updated = payeeInfoMapper.selectById(payee.getId());
        assertEquals("6222021234567890", updated.getBankCardNo());
        assertNotEquals("骗人的银行", updated.getBankName());
        assertNotEquals("1999-01-01", updated.getIdValidityPeriod());
    }

    @Test
    public void testReconcileOnboardingStatus_changeRejectedKeepsOldCardEffective() {
        PayeeInfoDO payee = insertPayee("USER_CHANGE2", "110101199001010042", "13800000042");
        markReady(payee);
        bankCardChangeService.requestChange(changeReq(payee.getId(), "6222029999888877", null));

        // 工行审核拒绝：结果属于新卡，不该改写建档状态
        sellerOnboardingService.reconcileOnboardingStatus(payee.getId(), null, "reject", "卡号与姓名不符",
                PayeeOnboardingOperaTypeEnum.MODIFY.getCode());

        PayeeInfoDO updated = payeeInfoMapper.selectById(payee.getId());
        assertEquals("6222021234567890", updated.getBankCardNo());
        assertEquals(PayeeOnboardingOutcomeEnum.READY.getCode(), updated.getOnboardingState());
        List<IcbcPayeeBankCardChangeDO> history = bankCardChangeService.listByPayeeId(payee.getId());
        assertEquals(1, history.size());
        assertEquals(PayeeBankCardChangeStatusEnum.REJECTED.getStatus(), history.get(0).getStatus());
        assertEquals("卡号与姓名不符", history.get(0).getRejectReason());
        assertFalse(sellerOnboardingService.hasPendingBankCardChange(payee.getId()));
    }

    @Test
    public void testReconcileOnboardingStatus_changePassedPromotesNewCard() {
        PayeeInfoDO payee = insertPayee("USER_CHANGE3", "110101199001010043", "13800000043");
        markReady(payee);
        bankCardChangeService.requestChange(changeReq(payee.getId(), "6222029999888877", "中国工商银行"));

        sellerOnboardingService.reconcileOnboardingStatus(payee.getId(), null, "pass", null,
                PayeeOnboardingOperaTypeEnum.MODIFY.getCode());

        PayeeInfoDO updated = payeeInfoMapper.selectById(payee.getId());
        assertEquals("6222029999888877", updated.getBankCardNo());
        assertEquals("中国工商银行", updated.getBankName());
        assertEquals(PayeeOnboardingOutcomeEnum.READY.getCode(), updated.getOnboardingState());
        assertFalse(sellerOnboardingService.hasPendingBankCardChange(payee.getId()));
    }

    @Test
    public void testHandleOnboardingNotify_modifyCallbackResolvesCardChangeNotPayeeState() {
        PayeeInfoDO payee = insertPayee("USER_CHANGE_N", "110101199001010044", "13800000044");
        markReady(payee);
        insertPayer(OUT_VENDOR_ID);
        bankCardChangeService.requestChange(changeReq(payee.getId(), "6222029999888877", "中国工商银行"));

        // 修改回调带 operaType=02：结果只收敛换卡单（换卡单的 pass 才是新卡生效的依据）
        sellerOnboardingService.handleOnboardingNotify(personOf(payee).getOutUserId(), OUT_VENDOR_ID,
                "pass", null, PayeeOnboardingOperaTypeEnum.MODIFY.getCode());

        PayeeInfoDO updated = payeeInfoMapper.selectById(payee.getId());
        assertEquals("6222029999888877", updated.getBankCardNo());
        assertEquals(PayeeOnboardingOutcomeEnum.READY.getCode(), updated.getOnboardingState());
        assertFalse(sellerOnboardingService.hasPendingBankCardChange(payee.getId()));
    }

    @Test
    public void testHandleOnboardingNotify_modifyCallbackWithoutPendingChangeLeavesPayeeUntouched() {
        PayeeInfoDO payee = insertPayee("USER_CHANGE_O", "110101199001010045", "13800000045");
        markReady(payee);
        insertPayer(OUT_VENDOR_ID);

        // 本机没有在途换卡单（例如已手动取消），工行仍下发修改结果：不猜，更不能用它改写建档状态
        sellerOnboardingService.handleOnboardingNotify(personOf(payee).getOutUserId(), OUT_VENDOR_ID,
                "reject", "卡号与姓名不符", PayeeOnboardingOperaTypeEnum.MODIFY.getCode());

        PayeeInfoDO updated = payeeInfoMapper.selectById(payee.getId());
        assertEquals("6222021234567890", updated.getBankCardNo());
        assertEquals(PayeeOnboardingOutcomeEnum.READY.getCode(), updated.getOnboardingState());
    }

    @Test
    public void testGetOnboarding_showsPendingCardChangeStatus() {
        PayeeInfoDO payee = insertPayee("USER_CHANGE_V", "110101199001010046", "13800000046");
        markReady(payee);
        bankCardChangeService.requestChange(changeReq(payee.getId(), "6222029999888877", "中国工商银行"));

        SellerOnboardingRespVO overview = sellerOnboardingService.getOnboarding(payee.getId());

        // 企业侧要看得见「钱要打到哪张卡、审核到哪一步」
        assertEquals("银行审核中", overview.getBankCardChangeStatusName());
        assertNotNull(overview.getBankCardChangeStatus());
        assertNotNull(overview.getBankCardChangeRequestedAt());
        // 生效中的卡在审核通过前不动
        assertEquals("6222021234567890", payeeInfoMapper.selectById(payee.getId()).getBankCardNo());
    }

    // ==================== 助手 ====================

    /**
     * 建一条收方档案，并像 {@code PayeeInfoServiceImpl} 那样挂到平台级自然人主体上。
     */
    private PayeeInfoDO insertPayee(String partnerPayeeId, String idCardNo, String mobile) {
        return insertPayeeWithPerson(partnerPayeeId, registerPerson("张三", idCardNo, mobile), mobile);
    }

    private PayeeInfoDO insertPayeeWithPerson(String partnerPayeeId, IcbcNaturalPersonDO person, String mobile) {
        PayeeInfoDO payee = PayeeInfoDO.builder()
                .partnerPayeeId(partnerPayeeId)
                .naturalPersonId(person.getId())
                .name(person.getName())
                .idCardNo(person.getIdCardNo())
                .mobile(mobile)
                .bankCardNo("6222021234567890")
                .businessType("RECYCLE")
                .status(IcbcStatusEnum.AuditStatus.PENDING.getStatus())
                .realNameStatus(PayeeRealNameStatusEnum.NOT_STARTED.getStatus())
                .build();
        payeeInfoMapper.insert(payee);
        return payee;
    }

    private IcbcNaturalPersonDO personOf(PayeeInfoDO payee) {
        return naturalPersonService.getNaturalPerson(payee.getNaturalPersonId());
    }

    private IcbcNaturalPersonDO registerPerson(String name, String idCardNo, String mobile) {
        NaturalPersonRegisterReqVO reqVO = new NaturalPersonRegisterReqVO();
        reqVO.setName(name);
        reqVO.setIdCardNo(idCardNo);
        reqVO.setMobile(mobile);
        return naturalPersonService.register(reqVO);
    }

    private void insertPayer(String outVendorId) {
        PayerInfoDO payer = new PayerInfoDO();
        payer.setPartnerPayerId(outVendorId);
        payer.setName("某某再生资源有限公司");
        payer.setTenantId(1L);
        payerInfoMapper.insert(payer);
    }

    private void markRealNamePassed(PayeeInfoDO payee) {
        naturalPersonService.applyRealNameResult(payee.getNaturalPersonId(), true, null);
    }

    /** 先把建档推到 READY：换卡是「已入驻之后」的事。 */
    private void markReady(PayeeInfoDO payee) {
        markRealNamePassed(payee);
        sellerOnboardingService.reconcileOnboardingStatus(payee.getId(), null, "pass", null, null);
    }

    private PayeeBankCardChangeSaveReqVO changeReq(Long payeeId, String newCardNo, String bankName) {
        PayeeBankCardChangeSaveReqVO reqVO = new PayeeBankCardChangeSaveReqVO();
        reqVO.setPayeeId(payeeId);
        reqVO.setNewBankCardNo(newCardNo);
        reqVO.setNewBankName(bankName);
        reqVO.setRequestSource("SELLER_PORTAL");
        return reqVO;
    }

    private SellerRealNameReqVO realNameReq(Long payeeId) {
        SellerRealNameReqVO reqVO = new SellerRealNameReqVO();
        reqVO.setPayeeId(payeeId);
        return reqVO;
    }

    /** 从 `...?token=xxx&purpose=...` 里取出令牌 */
    private String tokenOf(String url) {
        String after = url.substring(url.indexOf("token=") + "token=".length());
        return after.substring(0, after.indexOf('&'));
    }

    private FrameworkAgreementSaveReqVO agreementReq(Long payeeId, String productName) {
        FrameworkAgreementSaveReqVO reqVO = new FrameworkAgreementSaveReqVO();
        reqVO.setPayeeId(payeeId);
        reqVO.setProductName(productName);
        reqVO.setQuantity("5 吨");
        reqVO.setSpecification("重型");
        reqVO.setRecyclePeriod("2026 年 9 月第 1 期");
        reqVO.setSettlementMethod("银行转账，过磅后 3 日内结清");
        // 本类的用例围绕「一份生效协议 + 门禁」：纸签当场生效；电子签的待签署路径另有专门用例（#95）
        reqVO.setSignMethod("PAPER");
        return reqVO;
    }

    private void authorize(Long payeeId) {
        SellerAuthorizationSaveReqVO reqVO = new SellerAuthorizationSaveReqVO();
        reqVO.setPayeeId(payeeId);
        reqVO.setReverseInvoiceAuthorized(true);
        reqVO.setTaxAgencyAuthorized(true);
        reqVO.setChannel("ONSITE");
        sellerOnboardingService.authorizeSeller(reqVO);
    }

}
