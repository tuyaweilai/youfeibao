package cn.iocoder.yudao.module.icbc.service.wizard.impl;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.icbc.controller.admin.onboarding.vo.FrameworkAgreementSaveReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.payee.vo.PayeeInfoSaveReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.wizard.vo.*;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payee.PayeeInfoDO;
import cn.iocoder.yudao.module.icbc.enums.FrameworkAgreementSignMethodEnum;
import cn.iocoder.yudao.module.icbc.enums.IcbcAccountCodeEnum;
import cn.iocoder.yudao.module.icbc.service.cardrecognition.CardRecognitionPort;
import cn.iocoder.yudao.module.icbc.service.esign.EsignPort;
import cn.iocoder.yudao.module.icbc.service.onboarding.SellerOnboardingService;
import cn.iocoder.yudao.module.icbc.service.payee.PayeeInfoService;
import cn.iocoder.yudao.module.icbc.service.wizard.OnboardingWizardService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import javax.validation.Valid;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.*;

/**
 * 建档向导 Service 实现（#91）。
 *
 * <p>识别与落库是两件事，也刻意分在两种形态里：
 * <ul>
 *   <li>识别是**无状态**的：图片进来、调用 {@link CardRecognitionPort}、把结果合并后返回，
 *       图片不留存、不进文件服务（ADR 0037）；</li>
 *   <li>落库是**一次性**的：第 4 步确认后由 {@link #submit} 写收方档案（必要时登记 / 复用
 *       自然人主体）与框架收购协议，中间态不落后端草稿表（#81 决策 2）。</li>
 * </ul>
 *
 * <p>「电子签 vs 纸质签」的唯一判据是 {@link EsignPort#isAvailable(Long)}；本票不发起电子签署
 * （合同组签署见 #95），未开通时协议落 {@code PAPER}，向导照常走完（ADR 0036 / 0037）。
 */
@Slf4j
@Service
@Validated
public class OnboardingWizardServiceImpl implements OnboardingWizardService {

    /** 协议要素的缺省值：向导没改时与现场纸质件一致，但**不会留空**（税总 5 号公告第十七条）。 */
    private static final String DEFAULT_PRODUCT_NAME = "报废产品";
    private static final String DEFAULT_QUANTITY = "以实际交货为准";
    private static final String DEFAULT_SPECIFICATION = "以实际交货为准";
    private static final String DEFAULT_RECYCLE_PERIOD = "长期";
    private static final String DEFAULT_SETTLEMENT_METHOD = "银行转账";

    @Resource
    private CardRecognitionPort cardRecognitionPort;
    @Resource
    private EsignPort esignPort;
    @Resource
    private PayeeInfoService payeeInfoService;
    @Resource
    private SellerOnboardingService sellerOnboardingService;

    // ==================== 识别（无状态，图片不留存） ====================

    @Override
    public IdCardFrontRecognizeRespVO recognizeIdCardFront(@Valid IdCardFrontRecognizeReqVO reqVO) {
        requireImage(reqVO.getImageBase64());
        CardRecognitionPort.IdCardFront recognized =
                cardRecognitionPort.recognizeIdCardFront(reqVO.getImageBase64());
        return IdCardFrontRecognizeRespVO.builder()
                .name(pick(reqVO.getName(), recognized.getName()))
                .idCardNo(pick(reqVO.getIdCardNo(), recognized.getIdCardNo()))
                .address(pick(reqVO.getAddress(), recognized.getAddress()))
                .qualityScore(recognized.getQualityScore())
                .warnings(recognized.getWarnings())
                .blockReasons(recognized.getBlockReasons())
                .build();
    }

    @Override
    public IdCardBackRecognizeRespVO recognizeIdCardBack(@Valid IdCardBackRecognizeReqVO reqVO) {
        requireImage(reqVO.getImageBase64());
        CardRecognitionPort.IdCardBack recognized =
                cardRecognitionPort.recognizeIdCardBack(reqVO.getImageBase64());
        return IdCardBackRecognizeRespVO.builder()
                .idSignDate(pick(reqVO.getIdSignDate(), recognized.getIdSignDate()))
                .idValidityPeriod(pick(reqVO.getIdValidityPeriod(), recognized.getIdValidityPeriod()))
                .qualityScore(recognized.getQualityScore())
                .warnings(recognized.getWarnings())
                .blockReasons(recognized.getBlockReasons())
                .build();
    }

    @Override
    public BankCardRecognizeRespVO recognizeBankCard(@Valid BankCardRecognizeReqVO reqVO) {
        requireImage(reqVO.getImageBase64());
        CardRecognitionPort.BankCard recognized = cardRecognitionPort.recognizeBankCard(reqVO.getImageBase64());
        return BankCardRecognizeRespVO.builder()
                .bankCardNo(pick(reqVO.getBankCardNo(), recognized.getBankCardNo()))
                .bankName(pick(reqVO.getBankName(), recognized.getBankName()))
                .accountCode(pick(reqVO.getAccountCode(), recognized.getAccountCode()))
                .qualityScore(recognized.getQualityScore())
                .warnings(recognized.getWarnings())
                .blockReasons(recognized.getBlockReasons())
                .build();
    }

    // ==================== 落库 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OnboardingWizardSubmitRespVO submit(@Valid OnboardingWizardSubmitReqVO reqVO) {
        validateSubmit(reqVO);
        // 同一租户内一张身份证只能有一份收方档案：已有档案时该做的是「去改那一份」，不是再建一份。
        // 这里换成一句可读的话，而不是让 createPayeeInfo 抛出「身份证号码已存在」的错码（#91 评审 SP-5）
        if (payeeInfoService.getPayeeInfoByIdCardNo(reqVO.getIdCardNo()) != null) {
            throw exception(WIZARD_PAYEE_ALREADY_ARCHIVED);
        }

        // 1. 收方档案：姓名 / 证件号 / 证件有效期登记并关联平台级自然人主体（有则复用、主体上已填的值不覆盖），
        //    证件有效期同时作为本次确认值留在档案上；卡号 / 开户行 / 住址 / 是否我行卡也写进档案
        //    （ADR 0017、#81 决策 5）
        PayeeInfoSaveReqVO saveReqVO = new PayeeInfoSaveReqVO();
        saveReqVO.setName(reqVO.getName());
        saveReqVO.setIdCardNo(reqVO.getIdCardNo());
        saveReqVO.setMobile(reqVO.getMobile());
        saveReqVO.setAddress(reqVO.getAddress());
        saveReqVO.setIdSignDate(reqVO.getIdSignDate());
        saveReqVO.setIdValidityPeriod(reqVO.getIdValidityPeriod());
        saveReqVO.setBankCardNo(reqVO.getBankCardNo());
        saveReqVO.setBankName(reqVO.getBankName());
        saveReqVO.setBankBranch(reqVO.getBankBranch());
        saveReqVO.setAccountCode(StrUtil.blankToDefault(reqVO.getAccountCode(), IcbcAccountCodeEnum.ICBC.getCode()));
        saveReqVO.setBusinessType("RECYCLE");
        Long payeeId = payeeInfoService.createPayeeInfo(saveReqVO);
        PayeeInfoDO payee = payeeInfoService.getPayeeInfo(payeeId);

        // 2. 框架收购协议：电子签章未开通即落 PAPER，本票不发起电子签署（#95）
        String signMethod = resolveSignMethod(TenantContextHolder.getRequiredTenantId());
        Long agreementId = sellerOnboardingService.saveFrameworkAgreement(toAgreement(reqVO, payeeId, signMethod));

        return OnboardingWizardSubmitRespVO.builder()
                .payeeId(payeeId)
                .naturalPersonId(payee.getNaturalPersonId())
                .agreementId(agreementId)
                .signMethod(signMethod)
                .build();
    }

    /**
     * 「电子签 vs 纸质签」的唯一判据（ADR 0036 / 0037）。
     *
     * <p>合同组的电子签署由 #95 落地，本票不调 {@link EsignPort#initiate} /
     * {@link EsignPort#createSignUrl}：即使端口意外答可用，也按纸质落库并留下告警——
     * 绝不写一份「没人签过」的电子协议。
     */
    private String resolveSignMethod(Long tenantId) {
        if (esignPort.isAvailable(tenantId)) {
            log.warn("[resolveSignMethod][电子签章可用，但合同组电子签署尚未落地（#95），本次按纸质落库] tenantId={}",
                    tenantId);
        }
        return FrameworkAgreementSignMethodEnum.PAPER.getCode();
    }

    private FrameworkAgreementSaveReqVO toAgreement(OnboardingWizardSubmitReqVO reqVO, Long payeeId,
                                                    String signMethod) {
        FrameworkAgreementSaveReqVO agreement = new FrameworkAgreementSaveReqVO();
        agreement.setPayeeId(payeeId);
        agreement.setProductName(StrUtil.blankToDefault(reqVO.getProductName(), DEFAULT_PRODUCT_NAME));
        agreement.setQuantity(StrUtil.blankToDefault(reqVO.getQuantity(), DEFAULT_QUANTITY));
        agreement.setSpecification(StrUtil.blankToDefault(reqVO.getSpecification(), DEFAULT_SPECIFICATION));
        agreement.setRecyclePeriod(StrUtil.blankToDefault(reqVO.getRecyclePeriod(), DEFAULT_RECYCLE_PERIOD));
        agreement.setSettlementMethod(StrUtil.blankToDefault(reqVO.getSettlementMethod(), DEFAULT_SETTLEMENT_METHOD));
        agreement.setSignMethod(signMethod);
        // 纸质协议当场签署：状态生效、盖签署时间；电子签署时状态该落「待签署」，那是 #95
        agreement.setStatus(1);
        return agreement;
    }

    private void validateSubmit(OnboardingWizardSubmitReqVO reqVO) {
        if (StrUtil.isBlank(reqVO.getName())) {
            throw exception(WIZARD_NAME_REQUIRED);
        }
        if (StrUtil.isBlank(reqVO.getIdCardNo())) {
            throw exception(WIZARD_ID_CARD_NO_REQUIRED);
        }
        if (StrUtil.isBlank(reqVO.getMobile())) {
            throw exception(WIZARD_MOBILE_REQUIRED);
        }
        if (StrUtil.isBlank(reqVO.getBankCardNo())) {
            throw exception(WIZARD_BANK_CARD_NO_REQUIRED);
        }
    }

    private void requireImage(String imageBase64) {
        if (StrUtil.isBlank(imageBase64)) {
            throw exception(WIZARD_IMAGE_REQUIRED);
        }
    }

    /**
     * 只在空缺处回填：人工输入的值优先，识别结果只补空白（#81 决策 7）。
     */
    private String pick(String manual, String recognized) {
        return StrUtil.isNotBlank(manual) ? manual : StrUtil.trimToNull(recognized);
    }

}
