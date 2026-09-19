package cn.iocoder.yudao.module.icbc.service.onboarding;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.icbc.UnitTestConfiguration;
import cn.iocoder.yudao.module.icbc.controller.admin.naturalperson.vo.NaturalPersonRegisterReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.onboarding.vo.*;
import cn.iocoder.yudao.module.icbc.dal.dataobject.agreement.IcbcFrameworkAgreementDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.authorization.IcbcSellerAuthorizationDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.lead.IcbcContactLeadDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.naturalperson.IcbcNaturalPersonDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payee.PayeeInfoDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payer.PayerInfoDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.agreement.IcbcFrameworkAgreementMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.authorization.IcbcSellerAuthorizationMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.lead.IcbcContactLeadMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.payee.PayeeInfoMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.payer.PayerInfoMapper;
import cn.iocoder.yudao.module.icbc.enums.IcbcStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.PayeeOnboardingOutcomeEnum;
import cn.iocoder.yudao.module.icbc.enums.PayeeRealNameStatusEnum;
import cn.iocoder.yudao.module.icbc.gateway.IcbcGateway;
import cn.iocoder.yudao.module.icbc.gateway.IcbcGatewayResult;
import cn.iocoder.yudao.module.icbc.gateway.model.FaceVerifyPageReq;
import cn.iocoder.yudao.module.icbc.gateway.model.FaceVerifyStatus;
import cn.iocoder.yudao.module.icbc.gateway.model.IcbcPage;
import cn.iocoder.yudao.module.icbc.gateway.model.PayeeOnboardingPageReq;
import cn.iocoder.yudao.module.icbc.gateway.model.PayeeOnboardingStatus;
import cn.iocoder.yudao.module.icbc.service.naturalperson.NaturalPersonService;
import cn.iocoder.yudao.module.icbc.service.onboarding.impl.SellerOnboardingServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    public void testSubmitOnboarding_sendsTenantSubMerchantNotGlobalConfig() {
        PayeeInfoDO payee = insertPayee("USER_F", "110101199001010007", "13800000007");
        IcbcNaturalPersonDO person = personOf(payee);
        markRealNamePassed(payee);
        insertPayer(OUT_VENDOR_ID);
        when(icbcGateway.submitPayeeOnboarding(any())).thenReturn(IcbcGatewayResult.success(
                IcbcPage.builder().formHtml("<form id=\"onboard\"/>").build(), 0, "成功"));

        SellerOnboardingSubmitReqVO reqVO = new SellerOnboardingSubmitReqVO();
        reqVO.setPayeeId(payee.getId());
        reqVO.setIdSignDate("2020-01-01");
        reqVO.setIdValidityPeriod("9999-12-30");
        reqVO.setBankName("中国工商银行");
        reqVO.setBankBranch("北京分行营业部");

        SellerStepRespVO step = sellerOnboardingService.submitOnboarding(reqVO);

        assertEquals("<form id=\"onboard\"/>", step.getFormHtml());
        // 子商户必须是本租户的付方档案（与预下单 / 付款同一口径），不是全局配置
        ArgumentCaptor<PayeeOnboardingPageReq> captor = ArgumentCaptor.forClass(PayeeOnboardingPageReq.class);
        verify(icbcGateway).submitPayeeOnboarding(captor.capture());
        assertEquals(OUT_VENDOR_ID, captor.getValue().getOutVendorId());
        assertEquals(person.getOutUserId(), captor.getValue().getOutUserId());
        // 银行卡识别结果写回档案
        PayeeInfoDO updated = payeeInfoMapper.selectById(payee.getId());
        assertEquals("中国工商银行", updated.getBankName());
        assertEquals("9999-12-30", updated.getIdValidityPeriod());
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

    // ==================== 两条成败线的四种组合 ====================

    @Test
    public void testReconcileOnboarding_fourCombinations() {
        // 开户成功 + 审核通过 → READY
        assertOutcome(1, "02", "pass", PayeeOnboardingOutcomeEnum.READY, true,
                IcbcStatusEnum.AuditStatus.APPROVED.getStatus());
        // 开户成功 + 审核拒绝 → REJECTED
        assertOutcome(2, "02", "reject", PayeeOnboardingOutcomeEnum.REJECTED, false,
                IcbcStatusEnum.AuditStatus.REJECTED.getStatus());
        // 开户失败 + 审核通过 → OPENACCT_FAILED
        assertOutcome(3, "03", "pass", PayeeOnboardingOutcomeEnum.OPENACCT_FAILED, false,
                IcbcStatusEnum.AuditStatus.APPROVED.getStatus());
        // 开户失败 + 审核拒绝 → FAILED_AND_REJECTED
        assertOutcome(4, "03", "reject", PayeeOnboardingOutcomeEnum.FAILED_AND_REJECTED, false,
                IcbcStatusEnum.AuditStatus.REJECTED.getStatus());
    }

    private void assertOutcome(int seq, String openacctStatus, String result, PayeeOnboardingOutcomeEnum expected,
                               boolean eligible, Integer expectedAuditStatus) {
        PayeeInfoDO payee = insertPayee("USER_CASE_" + seq,
                "11010119900101" + String.format("%04d", 1000 + seq),
                "138" + String.format("%08d", 10000000 + seq));

        PayeeInfoDO updated = sellerOnboardingService.reconcileOnboardingStatus(
                payee.getId(), openacctStatus, result, "MEDIUM_1", "reject".equals(result) ? "资料不符" : null);

        assertEquals(expected.getCode(), updated.getOnboardingState());
        assertEquals(expected.isInvoiceEligible(), eligible);
        if (eligible) {
            assertEquals("1", updated.getIcbcReceiverStatus());
        } else {
            assertEquals("0", updated.getIcbcReceiverStatus());
        }
        assertEquals(expectedAuditStatus, updated.getStatus());
        assertEquals(openacctStatus, updated.getIcbcOpenacctStatus());
    }

    @Test
    public void testReconcileOnboarding_pendingDoesNotAdvance() {
        PayeeInfoDO payee = insertPayee("USER_PENDING", "110101199001010020", "13800000020");

        // 开户在途（01），两条线未到齐 → 不推进状态机
        PayeeInfoDO updated = sellerOnboardingService.reconcileOnboardingStatus(
                payee.getId(), "01", "pass", null, null);
        assertNull(updated.getOnboardingState());
        // 但已知的一半仍被记下
        assertEquals("01", updated.getIcbcOpenacctStatus());
    }

    // ==================== 入驻通知按子商户归位 ====================

    @Test
    public void testHandleOnboardingNotify_reconcilesOwnTenantPayee() {
        PayeeInfoDO payee = insertPayee("USER_NOTIFY", "110101199001010021", "13800000021");
        IcbcNaturalPersonDO person = personOf(payee);
        insertPayer(OUT_VENDOR_ID);

        sellerOnboardingService.handleOnboardingNotify(person.getOutUserId(), OUT_VENDOR_ID,
                "pass", "02", "MEDIUM_9", null);

        PayeeInfoDO updated = payeeInfoMapper.selectById(payee.getId());
        assertEquals(PayeeOnboardingOutcomeEnum.READY.getCode(), updated.getOnboardingState());
        assertEquals("MEDIUM_9", updated.getIcbcMediumId());
    }

    @Test
    public void testHandleOnboardingNotify_rejectedWithoutOpenacctStatus() {
        // 数据接口回调只带 result（无 openacctStatus），拒绝仍要可见
        PayeeInfoDO payee = insertPayee("USER_REJECT_ONLY", "110101199001010031", "13800000031");
        IcbcNaturalPersonDO person = personOf(payee);
        insertPayer(OUT_VENDOR_ID);

        sellerOnboardingService.handleOnboardingNotify(person.getOutUserId(), OUT_VENDOR_ID,
                "reject", null, null, "资料不符");

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
                "NOT_A_PAYER", "pass", "02", "M", null), SELLER_ONBOARDING_VENDOR_UNRESOLVED, "NOT_A_PAYER");
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
                                .openacctStatus("02").result("pass").mediumId("MEDIUM_SYNC").build(), 0, "成功"));

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
        sellerOnboardingService.reconcileOnboardingStatus(payee.getId(), "02", "pass", "M1", null);
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
        sellerOnboardingService.reconcileOnboardingStatus(payee.getId(), "02", "reject", null, "资料不符");
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
        sellerOnboardingService.reconcileOnboardingStatus(payee.getId(), "02", "pass", "M2", null);
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

    private SellerRealNameReqVO realNameReq(Long payeeId) {
        SellerRealNameReqVO reqVO = new SellerRealNameReqVO();
        reqVO.setPayeeId(payeeId);
        return reqVO;
    }

    private FrameworkAgreementSaveReqVO agreementReq(Long payeeId, String productName) {
        FrameworkAgreementSaveReqVO reqVO = new FrameworkAgreementSaveReqVO();
        reqVO.setPayeeId(payeeId);
        reqVO.setProductName(productName);
        reqVO.setQuantity("5 吨");
        reqVO.setSpecification("重型");
        reqVO.setRecyclePeriod("2026 年 9 月第 1 期");
        reqVO.setSettlementMethod("银行转账，过磅后 3 日内结清");
        reqVO.setSignMethod("ELECTRONIC");
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
