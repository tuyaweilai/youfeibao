package cn.iocoder.yudao.module.icbc.service.onboarding.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.icbc.controller.admin.onboarding.vo.*;
import cn.iocoder.yudao.module.icbc.dal.dataobject.agreement.IcbcFrameworkAgreementDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.authorization.IcbcSellerAuthorizationDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.lead.IcbcContactLeadDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payee.PayeeInfoDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.agreement.IcbcFrameworkAgreementMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.authorization.IcbcSellerAuthorizationMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.lead.IcbcContactLeadMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.payee.PayeeInfoMapper;
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
import cn.iocoder.yudao.module.icbc.service.onboarding.SellerOnboardingService;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
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
 */
@Slf4j
@Service
@Validated
public class SellerOnboardingServiceImpl implements SellerOnboardingService {

    /**
     * 默认交易渠道：H5。全程不要求出售者安装 App 或关注公众号（issue #6 验收）。
     */
    private static final String DEFAULT_TRX_CHANNEL = "03";

    @Resource
    private PayeeInfoMapper payeeInfoMapper;
    @Resource
    private IcbcFrameworkAgreementMapper frameworkAgreementMapper;
    @Resource
    private IcbcSellerAuthorizationMapper sellerAuthorizationMapper;
    @Resource
    private IcbcContactLeadMapper contactLeadMapper;
    @Resource
    private IcbcGateway icbcGateway;

    /**
     * 工行页面回调 / 跳转地址。是平台外网可达地址，不是工行地址，故不进适配层配置。
     */
    @Value("${icbc.seller-onboarding.callback-url:}")
    private String onboardingCallbackUrl;
    @Value("${icbc.seller-onboarding.jump-url:}")
    private String onboardingJumpUrl;
    @Value("${icbc.seller-onboarding.fail-jump-url:}")
    private String onboardingFailJumpUrl;
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
        IcbcGatewayResult<IcbcPage> result = icbcGateway.submitFaceVerification(FaceVerifyPageReq.builder()
                .outUserId(payee.getPartnerPayeeId())
                .custName(payee.getName())
                .certNo(payee.getIdCardNo())
                .mobile(payee.getMobile())
                .transNo(generateTransNo("FACE"))
                .callbackUrl(StrUtil.blankToDefault(faceCallbackUrl, null))
                .build());
        if (!result.isSuccess()) {
            throw exception(ICBC_API_CALL_FAILED);
        }
        // 认证中；已通过的不回退
        if (!PayeeRealNameStatusEnum.PASSED.getStatus().equals(payee.getRealNameStatus())) {
            PayeeInfoDO update = new PayeeInfoDO();
            update.setId(payee.getId());
            update.setRealNameStatus(PayeeRealNameStatusEnum.PENDING.getStatus());
            payeeInfoMapper.updateById(update);
        }
        return step(payee.getId(), "REAL_NAME", formHtml(result));
    }

    @Override
    public PayeeInfoDO syncRealName(Long payeeId) {
        PayeeInfoDO payee = validatePayeeExists(payeeId);
        IcbcGatewayResult<FaceVerifyStatus> result =
                icbcGateway.queryFaceVerification(payee.getPartnerPayeeId());
        if (!result.isSuccess() || result.getData() == null) {
            throw exception(SELLER_REAL_NAME_RESULT_UNKNOWN);
        }
        applyRealNameResult(payee, result.getData().isPassed(), result.getData().getFailReason());
        return payeeInfoMapper.selectById(payeeId);
    }

    /**
     * 回写实人认证结果；认证中 / 未返回时保持原状。通过时显式把失败原因置空。
     */
    private void applyRealNameResult(PayeeInfoDO payee, boolean passed, String failReason) {
        if (!passed && StrUtil.isBlank(failReason)) {
            return; // 认证中，不动状态
        }
        LambdaUpdateWrapper<PayeeInfoDO> update = new LambdaUpdateWrapper<PayeeInfoDO>()
                .eq(PayeeInfoDO::getId, payee.getId());
        if (passed) {
            update.set(PayeeInfoDO::getRealNameStatus, PayeeRealNameStatusEnum.PASSED.getStatus())
                    .set(PayeeInfoDO::getRealNameMsg, null)
                    .set(PayeeInfoDO::getRealNameTime, LocalDateTime.now());
        } else {
            update.set(PayeeInfoDO::getRealNameStatus, PayeeRealNameStatusEnum.FAILED.getStatus())
                    .set(PayeeInfoDO::getRealNameMsg, failReason);
        }
        payeeInfoMapper.update(null, update);
        PayeeInfoDO refreshed = payeeInfoMapper.selectById(payee.getId());
        payee.setRealNameStatus(refreshed.getRealNameStatus());
        payee.setRealNameMsg(refreshed.getRealNameMsg());
        payee.setRealNameTime(refreshed.getRealNameTime());
    }

    // ==================== 收方入驻 ====================

    @Override
    public SellerStepRespVO submitOnboarding(SellerOnboardingSubmitReqVO reqVO) {
        PayeeInfoDO payee = validatePayeeExists(reqVO.getPayeeId());
        // 实人认证是前置环节
        if (!PayeeRealNameStatusEnum.PASSED.getStatus().equals(payee.getRealNameStatus())) {
            throw exception(SELLER_REAL_NAME_NOT_PASSED);
        }
        if (StrUtil.isBlank(payee.getBankCardNo())) {
            throw exception(SELLER_BANK_CARD_REQUIRED);
        }

        // 银行卡识别结果与证件有效期一并写回档案
        PayeeInfoDO update = new PayeeInfoDO();
        update.setId(payee.getId());
        update.setIdSignDate(reqVO.getIdSignDate());
        update.setIdValidityPeriod(reqVO.getIdValidityPeriod());
        update.setBankName(reqVO.getBankName());
        update.setBankBranch(reqVO.getBankBranch());
        payeeInfoMapper.updateById(update);
        payee.setIdSignDate(StrUtil.blankToDefault(reqVO.getIdSignDate(), payee.getIdSignDate()));
        payee.setIdValidityPeriod(StrUtil.blankToDefault(reqVO.getIdValidityPeriod(), payee.getIdValidityPeriod()));
        payee.setBankName(StrUtil.blankToDefault(reqVO.getBankName(), payee.getBankName()));
        payee.setBankBranch(StrUtil.blankToDefault(reqVO.getBankBranch(), payee.getBankBranch()));

        IcbcGatewayResult<IcbcPage> result = icbcGateway.submitPayeeOnboarding(PayeeOnboardingPageReq.builder()
                .outUserId(payee.getPartnerPayeeId())
                .receiverName(payee.getName())
                .receiverAccount(payee.getBankCardNo())
                .mobile(payee.getMobile())
                .idNo(payee.getIdCardNo())
                .occupation(payee.getOccupation())
                .address(payee.getAddress())
                .signDate(payee.getIdSignDate())
                .validityPeriod(payee.getIdValidityPeriod())
                .corpSerno(generateTransNo("ONBOARD"))
                .trxChannel(StrUtil.blankToDefault(reqVO.getTrxChannel(), DEFAULT_TRX_CHANNEL))
                .callbackUrl(StrUtil.blankToDefault(onboardingCallbackUrl, null))
                .jumpUrl(StrUtil.blankToDefault(onboardingJumpUrl, null))
                .failJumpUrl(StrUtil.blankToDefault(onboardingFailJumpUrl, null))
                .build());
        if (!result.isSuccess()) {
            throw exception(ICBC_API_CALL_FAILED);
        }
        return step(payee.getId(), "ONBOARDING", formHtml(result));
    }

    @Override
    public PayeeInfoDO syncOnboarding(Long payeeId) {
        PayeeInfoDO payee = validatePayeeExists(payeeId);
        IcbcGatewayResult<PayeeOnboardingStatus> result =
                icbcGateway.queryPayeeOnboarding(payee.getPartnerPayeeId());
        if (!result.isSuccess() || result.getData() == null) {
            throw exception(ICBC_API_CALL_FAILED);
        }
        PayeeOnboardingStatus status = result.getData();
        reconcileOnboardingStatus(payee.getId(), status.getOpenacctStatus(), status.getResult(),
                status.getMediumId(), status.getRejectReason());
        return payeeInfoMapper.selectById(payeeId);
    }

    @Override
    public PayeeInfoDO reconcileOnboardingStatus(Long payeeId, String openacctStatus, String result,
                                                 String mediumId, String rejectReason) {
        PayeeInfoDO payee = validatePayeeExists(payeeId);
        PayeeOnboardingOutcomeEnum outcome = PayeeOnboardingOutcomeEnum.of(openacctStatus, result);
        // 审核拒绝是权威结论：即便开户状态缺失（如数据接口回调只带 result），也要把拒绝状态与原因落下来
        if (outcome == null && "reject".equalsIgnoreCase(result)) {
            outcome = PayeeOnboardingOutcomeEnum.REJECTED;
        }

        PayeeInfoDO update = new PayeeInfoDO();
        update.setId(payee.getId());
        // 两条线各自的原样透传：即便尚未收敛，也先把已知的一半记下来
        update.setIcbcOpenacctStatus(StrUtil.blankToDefault(openacctStatus, null));
        update.setIcbcMediumId(StrUtil.blankToDefault(mediumId, null));
        update.setAuditResult(StrUtil.blankToDefault(result, null));
        update.setRejectReason(StrUtil.blankToDefault(rejectReason, null));
        if (outcome != null) {
            update.setOnboardingState(outcome.getCode());
            update.setIcbcReceiverStatus(outcome.isInvoiceEligible() ? "1" : "0");
            if ("pass".equalsIgnoreCase(result)) {
                update.setStatus(IcbcStatusEnum.AuditStatus.APPROVED.getStatus());
            } else if ("reject".equalsIgnoreCase(result)) {
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
        IcbcFrameworkAgreementDO agreement = frameworkAgreementMapper.selectActiveByPayeeId(payeeId);
        IcbcSellerAuthorizationDO authorization = sellerAuthorizationMapper.selectLatestByPayeeId(payeeId);

        SellerOnboardingRespVO resp = new SellerOnboardingRespVO();
        resp.setPayeeId(payee.getId());
        resp.setName(payee.getName());
        resp.setIdCardNo(payee.getIdCardNo());
        resp.setMobile(payee.getMobile());
        resp.setRealNameStatus(payee.getRealNameStatus());
        PayeeRealNameStatusEnum realName = PayeeRealNameStatusEnum.of(payee.getRealNameStatus());
        resp.setRealNameStatusName(realName != null ? realName.getName() : null);
        resp.setRealNameMsg(payee.getRealNameMsg());
        resp.setOnboardingState(payee.getOnboardingState());
        PayeeOnboardingOutcomeEnum outcome = PayeeOnboardingOutcomeEnum.ofCode(payee.getOnboardingState());
        resp.setOnboardingStateName(outcome != null ? outcome.getName() : null);
        resp.setNextStep(outcome != null ? outcome.getNextStep() : null);
        resp.setAuditResult(payee.getAuditResult());
        resp.setRejectReason(payee.getRejectReason());
        resp.setStatus(payee.getStatus());
        resp.setIcbcOpenacctStatus(payee.getIcbcOpenacctStatus());
        resp.setIcbcReceiverStatus(payee.getIcbcReceiverStatus());
        resp.setIcbcMediumId(payee.getIcbcMediumId());
        resp.setUpdateTime(payee.getUpdateTime());
        resp.setFrameworkAgreement(toAgreementResp(agreement));
        resp.setAuthorization(toAuthorizationResp(authorization));
        resp.setAgreementHistory(frameworkAgreementMapper.selectListByPayeeId(payeeId).stream()
                .map(this::toAgreementResp).collect(Collectors.toList()));

        String blockReason = blockReason(payee, agreement, authorization);
        resp.setInvoiceEligible(blockReason == null);
        resp.setInvoiceBlockReason(blockReason);
        return resp;
    }

    @Override
    public void assertReadyForInvoice(Long payeeId) {
        PayeeInfoDO payee = validatePayeeExists(payeeId);
        String reason = blockReason(payee,
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
        PayeeInfoDO payee = payeeInfoMapper.selectByPartnerPayeeId(outUserId);
        if (payee != null) {
            assertReadyForInvoice(payee.getId());
        }
    }

    /**
     * 可开票的前提：实名通过 + 入驻 READY + 生效协议 + 两项授权齐备。返回第一条不满足的原因。
     */
    private String blockReason(PayeeInfoDO payee, IcbcFrameworkAgreementDO agreement,
                               IcbcSellerAuthorizationDO authorization) {
        if (!PayeeRealNameStatusEnum.PASSED.getStatus().equals(payee.getRealNameStatus())) {
            return "实人认证未通过";
        }
        PayeeOnboardingOutcomeEnum outcome = PayeeOnboardingOutcomeEnum.ofCode(payee.getOnboardingState());
        if (outcome == null || !outcome.isInvoiceEligible()) {
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
        PayeeInfoDO payee = payeeInfoMapper.selectByPartnerPayeeId(outUserId);
        if (payee == null) {
            log.warn("[handleFaceVerifyNotify][未找到出售者档案] outUserId={}", outUserId);
            return;
        }
        applyRealNameResult(payee, passed, failReason);
    }

    @Override
    public void handleOnboardingNotify(String outUserId, String result, String openacctStatus,
                                       String mediumId, String rejectReason) {
        PayeeInfoDO payee = payeeInfoMapper.selectByPartnerPayeeId(outUserId);
        if (payee == null) {
            log.warn("[handleOnboardingNotify][未找到出售者档案] outUserId={}", outUserId);
            return;
        }
        reconcileOnboardingStatus(payee.getId(), openacctStatus, result, mediumId, rejectReason);
    }

    // ==================== 内部方法 ====================

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
