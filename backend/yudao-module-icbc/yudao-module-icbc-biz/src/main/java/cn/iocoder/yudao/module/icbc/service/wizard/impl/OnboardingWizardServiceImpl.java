package cn.iocoder.yudao.module.icbc.service.wizard.impl;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.icbc.controller.admin.onboarding.vo.FrameworkAgreementSaveReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.payee.vo.PayeeInfoSaveReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.wizard.vo.*;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payee.PayeeInfoDO;
import cn.iocoder.yudao.module.icbc.enums.FrameworkAgreementSignMethodEnum;
import cn.iocoder.yudao.module.icbc.enums.FrameworkAgreementStatusEnum;
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
 * <p>「电子签 vs 纸质签」的唯一判据是 {@link EsignPort#isAvailable(Long)}：租户已开通时协议落
 * {@code ELECTRONIC + 待签署} 并**立刻发起合同组签署**（框架收购协议 + 反向发票合规告知函，
 * 企业先盖章、自然人后签署，ADR 0036），签完靠回调收敛为生效；未开通时降级为 {@code PAPER}，
 * 向导照常走完（ADR 0036 / 0037）。
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

        // 2. 框架收购协议：开通电子签章就走合同组电子签署（落待签署），否则纸签当场生效（#95 / ADR 0036）
        String signMethod = resolveSignMethod(TenantContextHolder.getRequiredTenantId());
        FrameworkAgreementSaveReqVO agreementReq = toAgreement(reqVO, payeeId, signMethod);
        // 电子签：saveFrameworkAgreement 在同一事务里落「待签署」并发起合同组签署（拿不到任务号就回滚）
        Long agreementId = sellerOnboardingService.saveFrameworkAgreement(agreementReq);
        Integer agreementStatus = FrameworkAgreementSignMethodEnum.ELECTRONIC.getCode().equals(signMethod)
                ? FrameworkAgreementStatusEnum.PENDING.getStatus()
                : FrameworkAgreementStatusEnum.EFFECTIVE.getStatus();

        return OnboardingWizardSubmitRespVO.builder()
                .payeeId(payeeId)
                .naturalPersonId(payee.getNaturalPersonId())
                .agreementId(agreementId)
                .signMethod(signMethod)
                .agreementStatus(agreementStatus)
                .message(buildSignMessage(signMethod))
                .build();
    }

    /**
     * 「电子签 vs 纸质签」的唯一判据（ADR 0036 / 0037）。
     *
     * <p>租户已开通（平台参数齐备 + 企业认证与印章就位 + 额度未耗尽，由端口回答）就发起电子签署，
     * 协议落待签署；否则降级纸质。两条路都不阻断建档。
     */
    private String resolveSignMethod(Long tenantId) {
        return esignPort.isAvailable(tenantId)
                ? FrameworkAgreementSignMethodEnum.ELECTRONIC.getCode()
                : FrameworkAgreementSignMethodEnum.PAPER.getCode();
    }

    /**
     * 给现场的可读说明：走了哪条路、本人接下来要做什么。
     */
    private String buildSignMessage(String signMethod) {
        if (FrameworkAgreementSignMethodEnum.ELECTRONIC.getCode().equals(signMethod)) {
            return "签署已发起：请在本人手机上点「去签署」，一次实名、一次签名把框架收购协议与反向发票合规告知函两份一起签完。";
        }
        return "本企业尚未开通电子签章，本次框架收购协议按纸质签署落库：请现场打印并与本人签字后留存。";
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
        // 状态由 service 按签署方式定：电子 = 待签署（签完靠回调推到生效），纸质 = 当场生效
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
