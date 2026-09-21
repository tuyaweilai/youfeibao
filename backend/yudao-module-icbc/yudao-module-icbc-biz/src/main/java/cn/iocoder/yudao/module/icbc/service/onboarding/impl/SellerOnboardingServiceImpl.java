package cn.iocoder.yudao.module.icbc.service.onboarding.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.module.icbc.controller.admin.onboarding.vo.*;
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
import cn.iocoder.yudao.module.icbc.enums.IcbcOccupationEnum;
import cn.iocoder.yudao.module.icbc.enums.PayeeBankCardChangeStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.PayeeOnboardingOutcomeEnum;
import cn.iocoder.yudao.module.icbc.enums.PayeeRealNameStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.PublicTokenPurposeEnum;
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
import cn.iocoder.yudao.module.icbc.service.payee.PayeeBankCardChangeService;
import cn.iocoder.yudao.module.icbc.service.payee.PayeeInfoService;
import cn.iocoder.yudao.module.icbc.service.onboarding.SellerOnboardingService;
import cn.iocoder.yudao.module.icbc.service.token.SellerAppLink;
import cn.iocoder.yudao.module.icbc.service.token.SellerAppLinkBuilder;
import cn.iocoder.yudao.module.icbc.util.MaskUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.*;

/**
 * 出售者建档 Service 实现
 *
 * <p>建档的归属刻意分成两层（见 ADR 0017）：
 * <ul>
 *   <li><b>实人认证</b>记在**自然人主体**上：同一个人在两家回收企业只需认证一次；</li>
 *   <li><b>收方入驻</b>记在**收方档案**上：入驻是与子商户（回收企业）绑定的动作，一家企业一次。</li>
 * </ul>
 */
@Slf4j
@Service
@Validated
public class SellerOnboardingServiceImpl implements SellerOnboardingService {

    /**
     * 工行职业字典的缺省值：「14-其他」。15 个取值里只有它**不宣称任何事实**——「13-无职业」是在断言
     * 这个人没有工作，那是编造；「其他」只表示我们没问。将来建档向导加一步问职业，这里就不再兜底。
     */
    private static final String DEFAULT_OCCUPATION = IcbcOccupationEnum.OTHER.getCode();

    /**
     * 「是否我行用户」的缺省值：1-我行用户。现场端拿到的多是工行卡；非工行卡由现场端或后续的
     * 银行卡 OCR 显式上送 0。猜错的代价是工行驳回入驻，而不是默默写错一笔钱。
     */
    private static final String DEFAULT_ACCOUNT_CODE = "1";

    /**
     * 自动发起入驻失败时写进「审核信息」的前缀（#85）。带前缀是为了与旧审核回调写入的 auditMsg 区分，
     * 只有自动发起的失败才会被摆到不可开票原因里。
     */
    private static final String AUTO_SUBMIT_FAIL_PREFIX = "自动发起收方入驻失败：";

    @Resource
    private PayeeInfoMapper payeeInfoMapper;
    @Resource
    private PayeeInfoService payeeInfoService;
    @Resource
    private PayerInfoMapper payerInfoMapper;
    @Resource
    private IcbcFrameworkAgreementMapper frameworkAgreementMapper;
    @Resource
    private IcbcSellerAuthorizationMapper sellerAuthorizationMapper;
    @Resource
    private IcbcContactLeadMapper contactLeadMapper;
    @Resource
    private NaturalPersonService naturalPersonService;
    @Resource
    private PayeeBankCardChangeService payeeBankCardChangeService;
    @Resource
    private IcbcGateway icbcGateway;
    @Resource
    private SellerAppLinkBuilder sellerAppLinkBuilder;

    /**
     * 收方入驻审核结果回调地址。是平台外网可达地址，不是工行地址，故不进适配层配置。
     */
    @Value("${icbc.seller-onboarding.callback-url:}")
    private String onboardingCallbackUrl;
    /**
     * 实人认证结果回调地址（同样是平台外网可达地址）
     */
    @Value("${icbc.seller-onboarding.face-callback-url:}")
    private String faceCallbackUrl;

    // ==================== 回头客 ====================

    @Override
    public PayeeInfoDO findReturningCustomer(String idCardNo, String mobile) {
        if (StrUtil.isNotBlank(idCardNo)) {
            PayeeInfoDO byIdCard = payeeInfoMapper.selectByIdCardNo(idCardNo);
            if (byIdCard != null) {
                return byIdCard;
            }
        }
        if (StrUtil.isNotBlank(mobile)) {
            return payeeInfoMapper.selectByMobile(mobile);
        }
        return null;
    }

    // ==================== 实人认证 ====================

    @Override
    public SellerStepRespVO startRealName(SellerRealNameReqVO reqVO) {
        PayeeInfoDO payee = validatePayeeExists(reqVO.getPayeeId());
        IcbcNaturalPersonDO person = payeeInfoService.ensureNaturalPerson(payee);
        // 实名结果页的「确认」要跳回自然人端，落点页读到令牌会自己刷一次结果（#82）。
        // jump / fail-jump 是工行标为必输的外网地址：缺配置当场报错，不带空值去求工行。
        SellerAppLink returnLink = sellerAppLinkBuilder.requirePayeeLink(
                payee.getId(), PublicTokenPurposeEnum.ONBOARDING);
        IcbcGatewayResult<IcbcPage> result = icbcGateway.submitFaceVerification(FaceVerifyPageReq.builder()
                // 平台级外部用户编号：实名一次，跨企业复用
                .outUserId(person.getOutUserId())
                .custName(payee.getName())
                .certNo(payee.getIdCardNo())
                .mobile(payee.getMobile())
                .transNo(generateTransNo("FACE"))
                .callbackUrl(StrUtil.blankToDefault(faceCallbackUrl, null))
                .jumpUrl(faceReturnUrl(returnLink, true))
                .failJumpUrl(faceReturnUrl(returnLink, false))
                .build());
        if (!result.isSuccess()) {
            throw exception(ICBC_API_CALL_FAILED);
        }
        naturalPersonService.markRealNamePending(person.getId());
        return step(payee.getId(), "REAL_NAME", formHtml(result));
    }

    @Override
    public PayeeInfoDO syncRealName(Long payeeId) {
        PayeeInfoDO payee = validatePayeeExists(payeeId);
        IcbcNaturalPersonDO person = payeeInfoService.ensureNaturalPerson(payee);
        IcbcGatewayResult<FaceVerifyStatus> result =
                icbcGateway.queryFaceVerification(person.getOutUserId());
        if (!result.isSuccess() || result.getData() == null) {
            throw exception(SELLER_REAL_NAME_RESULT_UNKNOWN);
        }
        naturalPersonService.applyRealNameResult(person.getId(), result.getData().isPassed(),
                result.getData().getFailReason());
        // 实名通过即自动发起入驻（#85）：不等人回头点「发起收方入驻」
        if (result.getData().isPassed()) {
            autoSubmitOnboarding(payeeId);
        }
        return payeeInfoMapper.selectById(payeeId);
    }

    // ==================== 收方入驻 ====================

    @Override
    public PayeeInfoDO submitOnboarding(SellerOnboardingSubmitReqVO reqVO) {
        PayeeInfoDO payee = validatePayeeExists(reqVO.getPayeeId());
        IcbcNaturalPersonDO person = payeeInfoService.ensureNaturalPerson(payee);
        // 实人认证是前置环节：认证记在自然人主体上，所以同一人在别的企业认证过也算数
        if (!PayeeRealNameStatusEnum.PASSED.getStatus().equals(person.getRealNameStatus())) {
            throw exception(SELLER_REAL_NAME_NOT_PASSED);
        }
        // 换卡（#37）：有在途变更就走**收方修改**。不能重跑一次新增——同一个 outUserId 档案下
        // 不可重复使用，而且换卡只允许改卡与证件有效期那几个字段。
        IcbcPayeeBankCardChangeDO change = payeeBankCardChangeService.getPending(payee.getId());
        if (change != null) {
            return submitBankCardChange(payee, person, change, reqVO);
        }
        // 已在途或已通过就不再打一次工行：重复提交会多出一条待审记录，且工行要求未知时先查询
        PayeeOnboardingOutcomeEnum current = PayeeOnboardingOutcomeEnum.ofCode(payee.getOnboardingState());
        if (current == PayeeOnboardingOutcomeEnum.PENDING || current == PayeeOnboardingOutcomeEnum.READY) {
            return payee;
        }
        if (StrUtil.isBlank(payee.getBankCardNo())) {
            throw exception(SELLER_BANK_CARD_REQUIRED);
        }
        // 首次建档：证件有效期与开户行一并写回档案。
        PayeeInfoDO update = new PayeeInfoDO();
        update.setId(payee.getId());
        update.setIdSignDate(reqVO.getIdSignDate());
        update.setIdValidityPeriod(reqVO.getIdValidityPeriod());
        update.setBankName(reqVO.getBankName());
        update.setBankBranch(reqVO.getBankBranch());
        payeeInfoMapper.updateById(update);
        payee.setIdSignDate(StrUtil.blankToDefault(reqVO.getIdSignDate(), payee.getIdSignDate()));
        payee.setIdValidityPeriod(StrUtil.blankToDefault(reqVO.getIdValidityPeriod(), payee.getIdValidityPeriod()));

        IcbcGatewayResult<PayeeOnboardingReceipt> result =
                icbcGateway.submitPayeeOnboarding(PayeeOnboardingReq.builder()
                        .outUserId(person.getOutUserId())
                        // 子商户 = 回收企业 = 本租户的付方档案；与预下单 / 付款用的是同一个值
                        .outVendorId(currentOutVendorId())
                        .receiverName(payee.getName())
                        .receiverAccount(payee.getBankCardNo())
                        .accountCode(StrUtil.blankToDefault(reqVO.getAccountCode(), DEFAULT_ACCOUNT_CODE))
                        .bankName(StrUtil.blankToDefault(payee.getBankName(), reqVO.getBankName()))
                        .mobile(payee.getMobile())
                        .idNo(payee.getIdCardNo())
                        .occupation(StrUtil.blankToDefault(payee.getOccupation(), DEFAULT_OCCUPATION))
                        .address(payee.getAddress())
                        .signDate(payee.getIdSignDate())
                        .validityPeriod(payee.getIdValidityPeriod())
                        .callbackUrl(StrUtil.blankToDefault(onboardingCallbackUrl, null))
                        .build());
        if (!result.isSuccess()) {
            throw exception(ICBC_API_CALL_FAILED);
        }
        // 受理即「审核中」：这是同步受理、异步审核，不能假设同步返回就是通过
        PayeeInfoDO accepted = new PayeeInfoDO();
        accepted.setId(payee.getId());
        accepted.setOnboardingState(PayeeOnboardingOutcomeEnum.PENDING.getCode());
        payeeInfoMapper.updateById(accepted);
        return payeeInfoMapper.selectById(payee.getId());
    }

    /**
     * 换卡：走收方修改接口，只上送允许修改的字段（卡号、是否我行卡、行名、行号、证件签发/截止日期）。
     *
     * <p>收方档案里的卡**一条都不动**：新卡要等工行审核通过才由
     * {@link PayeeBankCardChangeService#applyOnboardingResult} 搬过去。
     */
    private PayeeInfoDO submitBankCardChange(PayeeInfoDO payee, IcbcNaturalPersonDO person,
                                             IcbcPayeeBankCardChangeDO change,
                                             SellerOnboardingSubmitReqVO reqVO) {
        IcbcGatewayResult<PayeeOnboardingReceipt> result = icbcGateway.updatePayeeBankCard(PayeeBankCardUpdateReq.builder()
                .outUserId(person.getOutUserId())
                .outVendorId(currentOutVendorId())
                .receiverAccount(change.getNewBankCardNo())
                .accountCode(StrUtil.blankToDefault(reqVO.getAccountCode(), DEFAULT_ACCOUNT_CODE))
                .bankName(StrUtil.blankToDefault(reqVO.getBankName(), change.getNewBankName()))
                .signDate(StrUtil.blankToDefault(change.getIdSignDate(), payee.getIdSignDate()))
                .validityPeriod(StrUtil.blankToDefault(change.getIdValidityPeriod(), payee.getIdValidityPeriod()))
                .callbackUrl(StrUtil.blankToDefault(onboardingCallbackUrl, null))
                .build());
        if (!result.isSuccess()) {
            throw exception(ICBC_API_CALL_FAILED);
        }
        return payeeInfoMapper.selectById(payee.getId());
    }

    @Override
    public PayeeInfoDO syncOnboarding(Long payeeId) {
        PayeeInfoDO payee = validatePayeeExists(payeeId);
        IcbcNaturalPersonDO person = payeeInfoService.ensureNaturalPerson(payee);
        IcbcGatewayResult<PayeeOnboardingStatus> result =
                icbcGateway.queryPayeeOnboarding(person.getOutUserId(), currentOutVendorId());
        if (!result.isSuccess() || result.getData() == null) {
            throw exception(ICBC_API_CALL_FAILED);
        }
        PayeeOnboardingStatus status = result.getData();
        return reconcileOnboardingStatus(payee.getId(), status.getAuditStatus(), status.getResult(),
                status.getRejectReason());
    }

    @Override
    public PayeeInfoDO reconcileOnboardingStatus(Long payeeId, String auditStatus, String result,
                                                 String rejectReason) {
        PayeeInfoDO payee = validatePayeeExists(payeeId);
        // 换卡（#37）：在途变更时，入驻结果属于**新卡**，不能拿它去改「建档状态」——
        // 被拒时原卡仍然有效，他依然可开票可收款（否则一次换卡失败会把他的收款能力打掉）。
        if (payeeBankCardChangeService.applyOnboardingResult(payeeId, result, rejectReason) != null) {
            return payeeInfoMapper.selectById(payeeId);
        }
        // 审核只一条线：回调只带 result，查询带 auditStatus（1-通过，2/3/4-还在审）
        PayeeOnboardingOutcomeEnum outcome = result != null
                ? PayeeOnboardingOutcomeEnum.of(result) : PayeeOnboardingOutcomeEnum.ofAuditStatus(auditStatus);

        PayeeInfoDO update = new PayeeInfoDO();
        update.setId(payee.getId());
        update.setAuditResult(StrUtil.blankToDefault(result, null));
        update.setRejectReason(StrUtil.blankToDefault(rejectReason, null));
        if (outcome != null) {
            update.setOnboardingState(outcome.getCode());
            update.setIcbcReceiverStatus(outcome.isInvoiceEligible() ? "1" : "0");
            if (outcome == PayeeOnboardingOutcomeEnum.READY) {
                update.setStatus(IcbcStatusEnum.AuditStatus.APPROVED.getStatus());
            } else if (outcome == PayeeOnboardingOutcomeEnum.REJECTED) {
                update.setStatus(IcbcStatusEnum.AuditStatus.REJECTED.getStatus());
                update.setAuditMsg(rejectReason);
            }
        }
        payeeInfoMapper.updateById(update);
        return payeeInfoMapper.selectById(payeeId);
    }

    @Override
    public void leaveContactFallback(SellerContactFallbackReqVO reqVO) {
        PayeeInfoDO payee = validatePayeeExists(reqVO.getPayeeId());
        contactLeadMapper.insert(IcbcContactLeadDO.builder()
                .payeeId(payee.getId())
                .name(payee.getName())
                .mobile(reqVO.getMobile())
                .remark(reqVO.getRemark())
                .build());
    }

    // ==================== 建档总览与开票门禁 ====================

    @Override
    public SellerOnboardingRespVO getOnboarding(Long payeeId) {
        PayeeInfoDO payee = validatePayeeExists(payeeId);
        IcbcNaturalPersonDO person = payeeInfoService.ensureNaturalPerson(payee);
        IcbcFrameworkAgreementDO agreement = frameworkAgreementMapper.selectActiveByPayeeId(payeeId);
        IcbcSellerAuthorizationDO authorization = sellerAuthorizationMapper.selectLatestByPayeeId(payeeId);

        SellerOnboardingRespVO resp = new SellerOnboardingRespVO();
        resp.setPayeeId(payee.getId());
        resp.setName(payee.getName());
        resp.setIdCardNo(payee.getIdCardNo());
        resp.setMobile(payee.getMobile());
        resp.setNaturalPersonId(person.getId());
        resp.setOutUserId(person.getOutUserId());
        resp.setRealNameStatus(person.getRealNameStatus());
        PayeeRealNameStatusEnum realName = PayeeRealNameStatusEnum.of(person.getRealNameStatus());
        resp.setRealNameStatusName(realName != null ? realName.getName() : null);
        resp.setRealNameMsg(person.getRealNameMsg());
        resp.setOnboardingState(payee.getOnboardingState());
        PayeeOnboardingOutcomeEnum outcome = PayeeOnboardingOutcomeEnum.ofCode(payee.getOnboardingState());
        resp.setOnboardingStateName(outcome != null ? outcome.getName() : null);
        resp.setNextStep(outcome != null ? outcome.getNextStep() : null);
        resp.setAuditResult(payee.getAuditResult());
        resp.setRejectReason(payee.getRejectReason());
        resp.setStatus(payee.getStatus());
        resp.setIcbcReceiverStatus(payee.getIcbcReceiverStatus());
        resp.setUpdateTime(payee.getUpdateTime());
        resp.setFrameworkAgreement(toAgreementResp(agreement));
        resp.setAuthorization(toAuthorizationResp(authorization));
        resp.setAgreementHistory(frameworkAgreementMapper.selectListByPayeeId(payeeId).stream()
                .map(this::toAgreementResp).collect(Collectors.toList()));

        // 收款账户变更（#37）：企业侧与自然人端都靠它说清「钱要打到哪张卡、审核到哪一步」
        IcbcPayeeBankCardChangeDO cardChange = payeeBankCardChangeService.getPending(payeeId);
        if (cardChange != null) {
            resp.setBankCardChangeStatus(cardChange.getStatus());
            resp.setBankCardChangeStatusName(PayeeBankCardChangeStatusEnum.nameOf(cardChange.getStatus()));
            resp.setBankCardChangeNewCardTail(MaskUtils.cardTail(cardChange.getNewBankCardNo()));
            resp.setBankCardChangeRequestedAt(cardChange.getRequestedAt());
        }

        String blockReason = blockReason(payee, person, agreement, authorization);
        resp.setInvoiceEligible(blockReason == null);
        resp.setInvoiceBlockReason(blockReason);
        return resp;
    }

    @Override
    public boolean hasPendingBankCardChange(Long payeeId) {
        return payeeBankCardChangeService.hasPending(payeeId);
    }

    @Override
    public void assertReadyForInvoice(Long payeeId) {
        PayeeInfoDO payee = validatePayeeExists(payeeId);
        String reason = blockReason(payee, payeeInfoService.ensureNaturalPerson(payee),
                frameworkAgreementMapper.selectActiveByPayeeId(payeeId),
                sellerAuthorizationMapper.selectLatestByPayeeId(payeeId));
        if (reason != null) {
            throw exception(SELLER_ONBOARDING_NOT_READY);
        }
    }

    @Override
    public void assertReadyForInvoiceByOutUserId(String outUserId) {
        if (StrUtil.isBlank(outUserId)) {
            return;
        }
        // outUserId 是平台级编号：先在平台级身份层找人，再取**本租户**的收方档案
        // （别的回收企业的档案看不到，也不该看到，见 CONTEXT「交易可见性边界」）
        IcbcNaturalPersonDO person = naturalPersonService.getByOutUserId(outUserId);
        if (person == null) {
            return;
        }
        PayeeInfoDO payee = payeeInfoMapper.selectByNaturalPersonId(person.getId());
        if (payee != null) {
            assertReadyForInvoice(payee.getId());
        }
    }

    /**
     * 可开票的前提：实人认证通过 + 入驻 READY + 生效协议 + 两项授权齐备。返回第一条不满足的原因。
     */
    private String blockReason(PayeeInfoDO payee, IcbcNaturalPersonDO person, IcbcFrameworkAgreementDO agreement,
                               IcbcSellerAuthorizationDO authorization) {
        if (!PayeeRealNameStatusEnum.PASSED.getStatus().equals(person.getRealNameStatus())) {
            return "实人认证未通过";
        }
        PayeeOnboardingOutcomeEnum outcome = PayeeOnboardingOutcomeEnum.ofCode(payee.getOnboardingState());
        if (outcome == null || !outcome.isInvoiceEligible()) {
            // 自动发起失败时把原因摆到台面上（状态仍为「未发起」，人工可重试）
            if (outcome == null && StrUtil.startWith(payee.getAuditMsg(), AUTO_SUBMIT_FAIL_PREFIX)) {
                return payee.getAuditMsg();
            }
            return outcome != null ? outcome.getName() : "收方入驻未完成";
        }
        if (agreement == null) {
            return "未签署生效的框架收购协议";
        }
        if (authorization == null
                || !Boolean.TRUE.equals(authorization.getReverseInvoiceAuthorized())
                || !Boolean.TRUE.equals(authorization.getTaxAgencyAuthorized())) {
            return "未完成反向开票与代办税费授权";
        }
        return null;
    }

    // ==================== 框架收购协议 ====================

    @Override
    public Long saveFrameworkAgreement(FrameworkAgreementSaveReqVO reqVO) {
        validatePayeeExists(reqVO.getPayeeId());
        Integer status = reqVO.getStatus() != null ? reqVO.getStatus() : 1; // 默认生效
        // 一份生效协议：新协议生效时，旧生效协议作废并留痕
        if (Integer.valueOf(1).equals(status)) {
            IcbcFrameworkAgreementDO current = frameworkAgreementMapper.selectActiveByPayeeId(reqVO.getPayeeId());
            if (current != null && !current.getId().equals(reqVO.getId())) {
                IcbcFrameworkAgreementDO voided = new IcbcFrameworkAgreementDO();
                voided.setId(current.getId());
                voided.setStatus(2);
                frameworkAgreementMapper.updateById(voided);
            }
        }
        if (reqVO.getId() != null) {
            validateAgreementExists(reqVO.getId());
            IcbcFrameworkAgreementDO update = toAgreement(reqVO, status);
            update.setId(reqVO.getId());
            frameworkAgreementMapper.updateById(update);
            return reqVO.getId();
        }
        IcbcFrameworkAgreementDO agreement = toAgreement(reqVO, status);
        if (StrUtil.isBlank(agreement.getAgreementNo())) {
            agreement.setAgreementNo(generateAgreementNo());
        }
        if (agreement.getSignedAt() == null && Integer.valueOf(1).equals(status)) {
            agreement.setSignedAt(LocalDateTime.now());
        }
        frameworkAgreementMapper.insert(agreement);
        return agreement.getId();
    }

    @Override
    public IcbcFrameworkAgreementDO getActiveFrameworkAgreement(Long payeeId) {
        return frameworkAgreementMapper.selectActiveByPayeeId(payeeId);
    }

    @Override
    public List<IcbcFrameworkAgreementDO> getFrameworkAgreements(Long payeeId) {
        return frameworkAgreementMapper.selectListByPayeeId(payeeId);
    }

    // ==================== 首次授权 ====================

    @Override
    public Long authorizeSeller(SellerAuthorizationSaveReqVO reqVO) {
        validatePayeeExists(reqVO.getPayeeId());
        IcbcSellerAuthorizationDO authorization = IcbcSellerAuthorizationDO.builder()
                .payeeId(reqVO.getPayeeId())
                .reverseInvoiceAuthorized(Boolean.TRUE.equals(reqVO.getReverseInvoiceAuthorized()))
                .taxAgencyAuthorized(Boolean.TRUE.equals(reqVO.getTaxAgencyAuthorized()))
                .authorizedAt(reqVO.getAuthorizedAt() != null ? reqVO.getAuthorizedAt() : LocalDateTime.now())
                .channel(reqVO.getChannel())
                .operator(reqVO.getOperator())
                .evidenceUrl(reqVO.getEvidenceUrl())
                .remark(reqVO.getRemark())
                .build();
        sellerAuthorizationMapper.insert(authorization);
        return authorization.getId();
    }

    @Override
    public IcbcSellerAuthorizationDO getSellerAuthorization(Long payeeId) {
        return sellerAuthorizationMapper.selectLatestByPayeeId(payeeId);
    }

    // ==================== 异步通知处理 ====================

    @Override
    public void handleFaceVerifyNotify(String outUserId, boolean passed, String failReason) {
        IcbcNaturalPersonDO person = naturalPersonService.getByOutUserId(outUserId);
        if (person == null) {
            log.warn("[handleFaceVerifyNotify][未找到自然人主体] outUserId={}", outUserId);
            return;
        }
        naturalPersonService.applyRealNameResult(person.getId(), passed, failReason);
        // 实名通过即自动发起入驻（#85）：通知那条路也不等人点
        if (passed) {
            autoSubmitOnboardingForAllTenants(person);
        }
    }

    @Override
    public void handleOnboardingNotify(String outUserId, String outVendorId, String result,
                                       String rejectReason) {
        IcbcNaturalPersonDO person = naturalPersonService.getByOutUserId(outUserId);
        if (person == null) {
            log.warn("[handleOnboardingNotify][未找到自然人主体] outUserId={}", outUserId);
            return;
        }
        // 入驻是「自然人 × 子商户」的事，而回执只带平台级 outUserId，所以必须靠 appIdSub 定位是
        // 哪家回收企业（租户）。定位不了就抛出去让通知落失败、在通知监控里人工处理——不猜、不跨企业乱写。
        Long tenantId = resolveTenantIdByOutVendorId(outVendorId);
        if (tenantId == null) {
            throw exception(SELLER_ONBOARDING_VENDOR_UNRESOLVED, StrUtil.blankToDefault(outVendorId, "（空）"));
        }
        TenantUtils.execute(tenantId, () -> {
            PayeeInfoDO payee = payeeInfoMapper.selectByNaturalPersonId(person.getId());
            if (payee == null) {
                log.warn("[handleOnboardingNotify][该租户下未找到收方档案] tenantId={} outUserId={}",
                        tenantId, outUserId);
                return;
            }
            reconcileOnboardingStatus(payee.getId(), null, result, rejectReason);
        });
    }

    // ==================== 内部方法 ====================

    /**
     * 实名结果页「确认」的落点：成功 / 失败都回自然人端，只用一个 `from` 标记区分，
     * 落点页据此显示「已提交」或「未通过，可重试」（#82）。
     */
    private String faceReturnUrl(SellerAppLink link, boolean passed) {
        return link.getLink() + (passed ? "&from=face-success" : "&from=face-fail");
    }

    /**
     * 实名通过后自动发起入驻（#85）。
     *
     * <p>实名是平台级的、入驻是「自然人 × 回收企业」级的：同一个自然人可能在多家回收企业都有收方
     * 档案，回调（无租户上下文）要跨租户找出全部档案，再逐租户发起。
     */
    private void autoSubmitOnboardingForAllTenants(IcbcNaturalPersonDO person) {
        List<PayeeInfoDO> payees = TenantUtils.executeIgnore(
                () -> payeeInfoMapper.selectListByNaturalPersonId(person.getId()));
        for (PayeeInfoDO payee : payees) {
            TenantUtils.execute(payee.getTenantId(), () -> autoSubmitOnboarding(payee.getId()));
        }
    }

    /**
     * 对一个收方档案自动发起入驻。
     *
     * <p>幂等：已有入驻状态（审核中 / 通过 / 拒绝）就直接返回——「通知与查询都到」只会打一次工行。
     * 发起失败不吞掉也不回滚实名，把原因写进审核信息、状态停在「未发起」，留人工在后台重试。
     */
    private void autoSubmitOnboarding(Long payeeId) {
        PayeeInfoDO payee = payeeInfoMapper.selectById(payeeId);
        if (payee == null) {
            return;
        }
        if (PayeeOnboardingOutcomeEnum.ofCode(payee.getOnboardingState()) != null) {
            return;
        }
        SellerOnboardingSubmitReqVO reqVO = new SellerOnboardingSubmitReqVO();
        reqVO.setPayeeId(payeeId);
        reqVO.setIdSignDate(payee.getIdSignDate());
        reqVO.setIdValidityPeriod(payee.getIdValidityPeriod());
        reqVO.setBankName(payee.getBankName());
        reqVO.setBankBranch(payee.getBankBranch());
        try {
            submitOnboarding(reqVO);
        } catch (ServiceException e) {
            log.warn("[autoSubmitOnboarding][自动发起入驻失败] payeeId={} reason={}", payeeId, e.getMessage());
            PayeeInfoDO failed = new PayeeInfoDO();
            failed.setId(payeeId);
            failed.setAuditMsg(AUTO_SUBMIT_FAIL_PREFIX + e.getMessage());
            payeeInfoMapper.updateById(failed);
        }
    }

    /**
     * 本租户的子商户编号 = 付方档案的合作方付方编号（工行 {@code outVendorId} / {@code appIdSub}）。
     * 与预下单、付款用的是同一个值。
     */
    private String currentOutVendorId() {
        PayerInfoDO payer = firstPayer();
        if (payer == null || StrUtil.isBlank(payer.getPartnerPayerId())) {
            throw exception(SELLER_ONBOARDING_PAYER_NOT_CONFIGURED);
        }
        return payer.getPartnerPayerId();
    }

    private PayerInfoDO firstPayer() {
        List<PayerInfoDO> payers = payerInfoMapper.selectList(
                new LambdaQueryWrapperX<PayerInfoDO>().orderByAsc(PayerInfoDO::getId));
        return payers == null || payers.isEmpty() ? null : payers.get(0);
    }

    /**
     * 由子商户编号反查租户：付方档案是租户级、子商户编号写在里面，所以要跨租户找一次。
     */
    private Long resolveTenantIdByOutVendorId(String outVendorId) {
        if (StrUtil.isBlank(outVendorId)) {
            return null;
        }
        PayerInfoDO payer = TenantUtils.executeIgnore(() -> payerInfoMapper.selectByPartnerPayerId(outVendorId));
        return payer != null ? payer.getTenantId() : null;
    }

    private PayeeInfoDO validatePayeeExists(Long id) {
        PayeeInfoDO payee = id == null ? null : payeeInfoMapper.selectById(id);
        if (payee == null) {
            throw exception(PAYEE_NOT_EXISTS);
        }
        return payee;
    }

    private void validateAgreementExists(Long id) {
        if (frameworkAgreementMapper.selectById(id) == null) {
            throw exception(FRAMEWORK_AGREEMENT_NOT_EXISTS);
        }
    }

    private SellerStepRespVO step(Long payeeId, String step, String formHtml) {
        SellerStepRespVO resp = new SellerStepRespVO();
        resp.setPayeeId(payeeId);
        resp.setStep(step);
        resp.setFormHtml(formHtml);
        return resp;
    }

    private String formHtml(IcbcGatewayResult<IcbcPage> result) {
        return result.getData() != null ? result.getData().getFormHtml() : null;
    }

    private String generateTransNo(String prefix) {
        return prefix + System.currentTimeMillis() + IdUtil.fastSimpleUUID().substring(0, 8);
    }

    private String generateAgreementNo() {
        return "FW" + System.currentTimeMillis() + IdUtil.fastSimpleUUID().substring(0, 6).toUpperCase();
    }

    private IcbcFrameworkAgreementDO toAgreement(FrameworkAgreementSaveReqVO reqVO, Integer status) {
        return IcbcFrameworkAgreementDO.builder()
                .payeeId(reqVO.getPayeeId())
                .agreementNo(reqVO.getAgreementNo())
                .productName(reqVO.getProductName())
                .quantity(reqVO.getQuantity())
                .specification(reqVO.getSpecification())
                .recyclePeriod(reqVO.getRecyclePeriod())
                .settlementMethod(reqVO.getSettlementMethod())
                .signMethod(reqVO.getSignMethod())
                .signedAt(reqVO.getSignedAt())
                .fileUrl(reqVO.getFileUrl())
                .status(status)
                .remark(reqVO.getRemark())
                .build();
    }

    private FrameworkAgreementRespVO toAgreementResp(IcbcFrameworkAgreementDO agreement) {
        if (agreement == null) {
            return null;
        }
        FrameworkAgreementRespVO resp = new FrameworkAgreementRespVO();
        resp.setId(agreement.getId());
        resp.setPayeeId(agreement.getPayeeId());
        resp.setAgreementNo(agreement.getAgreementNo());
        resp.setProductName(agreement.getProductName());
        resp.setQuantity(agreement.getQuantity());
        resp.setSpecification(agreement.getSpecification());
        resp.setRecyclePeriod(agreement.getRecyclePeriod());
        resp.setSettlementMethod(agreement.getSettlementMethod());
        resp.setSignMethod(agreement.getSignMethod());
        resp.setSignedAt(agreement.getSignedAt());
        resp.setFileUrl(agreement.getFileUrl());
        resp.setStatus(agreement.getStatus());
        resp.setRemark(agreement.getRemark());
        resp.setCreateTime(agreement.getCreateTime());
        return resp;
    }

    private SellerAuthorizationRespVO toAuthorizationResp(IcbcSellerAuthorizationDO authorization) {
        if (authorization == null) {
            return null;
        }
        SellerAuthorizationRespVO resp = new SellerAuthorizationRespVO();
        resp.setId(authorization.getId());
        resp.setPayeeId(authorization.getPayeeId());
        resp.setReverseInvoiceAuthorized(authorization.getReverseInvoiceAuthorized());
        resp.setTaxAgencyAuthorized(authorization.getTaxAgencyAuthorized());
        resp.setAuthorizedAt(authorization.getAuthorizedAt());
        resp.setChannel(authorization.getChannel());
        resp.setOperator(authorization.getOperator());
        resp.setEvidenceUrl(authorization.getEvidenceUrl());
        resp.setRemark(authorization.getRemark());
        return resp;
    }

}
