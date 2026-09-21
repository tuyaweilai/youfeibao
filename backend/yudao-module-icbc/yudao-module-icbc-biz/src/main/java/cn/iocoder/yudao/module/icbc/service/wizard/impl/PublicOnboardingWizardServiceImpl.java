package cn.iocoder.yudao.module.icbc.service.wizard.impl;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.icbc.controller.admin.publictoken.vo.PublicTokenCreateReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.publictoken.vo.PublicTokenRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.wizard.vo.*;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payee.PayeeInfoDO;
import cn.iocoder.yudao.module.icbc.enums.PublicTokenPurposeEnum;
import cn.iocoder.yudao.module.icbc.service.payee.PayeeInfoService;
import cn.iocoder.yudao.module.icbc.service.token.PublicTokenPayload;
import cn.iocoder.yudao.module.icbc.service.token.PublicTokenService;
import cn.iocoder.yudao.module.icbc.service.wizard.OnboardingWizardService;
import cn.iocoder.yudao.module.icbc.service.wizard.PublicOnboardingWizardService;
import cn.iocoder.yudao.module.icbc.util.PublicTenantCall;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.*;

/**
 * 自填建档公开壳 Service 实现（#94）。
 *
 * <p>每个方法只做三件事：验令牌（用途必须是 {@code ONBOARDING_WIZARD}）→ 按令牌里的租户号
 * 切回正确租户 → 转发给代录壳同一个 {@link OnboardingWizardService}。图片与 PII 从不在这里落库，
 * 落库位置与字段完全交给向导 Service（保证两个壳「同一处落库」）。
 *
 * <p>链接锁到人（#94 修票 ST-1 结构根因）：已建档的人生成链接时绑 {@code PAYEE}（业务键是收方 ID），
 * 待建档的人才绑链接本身。{@code submit} 据此校验：绑了人的只允许改那个人，没绑人的只允许新建。
 *
 * <p>令牌占用口径：识别与打开页面**不占次数**（本人会重拍、回退、切后台），只有
 * {@code submit} 成功落库后才占用唯一一次（{@code maxUses = 1}）——一枚链接只建一份档案。
 * 校验不过不占次数，本人重试看得懂原因；同一枚链接再提交一次（如弱网重提）会读到「链接已用尽」，
 * 它上一次写下的东西随事务回滚，不会留下第二份档案（#94 评审 S-6）。
 */
@Service
@Validated
public class PublicOnboardingWizardServiceImpl implements PublicOnboardingWizardService {

    @Resource
    private PublicTokenService publicTokenService;
    @Resource
    private OnboardingWizardService onboardingWizardService;
    @Resource
    private PayeeInfoService payeeInfoService;

    @Override
    public PublicOnboardingWizardContextRespVO context(String token) {
        PublicTokenPayload payload = publicTokenService.verify(token, PublicTokenPurposeEnum.ONBOARDING_WIZARD);
        // 只回有效期：页面要把它显示给本人（#94 复审 ST-4）。用途由 URL 决定，不再回传没人用的字段。
        return PublicOnboardingWizardContextRespVO.builder()
                .expiresTime(toLocalDateTime(payload.getExpiresAt()))
                .build();
    }

    @Override
    public IdCardFrontRecognizeRespVO recognizeIdCardFront(String token, @Valid IdCardFrontRecognizeReqVO reqVO) {
        PublicTokenPayload payload = publicTokenService.verify(token, PublicTokenPurposeEnum.ONBOARDING_WIZARD);
        return PublicTenantCall.execute(payload.getTenantId(), () -> onboardingWizardService.recognizeIdCardFront(reqVO));
    }

    @Override
    public IdCardBackRecognizeRespVO recognizeIdCardBack(String token, @Valid IdCardBackRecognizeReqVO reqVO) {
        PublicTokenPayload payload = publicTokenService.verify(token, PublicTokenPurposeEnum.ONBOARDING_WIZARD);
        return PublicTenantCall.execute(payload.getTenantId(), () -> onboardingWizardService.recognizeIdCardBack(reqVO));
    }

    @Override
    public BankCardRecognizeRespVO recognizeBankCard(String token, @Valid BankCardRecognizeReqVO reqVO) {
        PublicTokenPayload payload = publicTokenService.verify(token, PublicTokenPurposeEnum.ONBOARDING_WIZARD);
        return PublicTenantCall.execute(payload.getTenantId(), () -> onboardingWizardService.recognizeBankCard(reqVO));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OnboardingWizardSubmitRespVO submit(String token, @Valid OnboardingWizardSubmitReqVO reqVO) {
        PublicTokenPayload payload = publicTokenService.verify(token, PublicTokenPurposeEnum.ONBOARDING_WIZARD);
        return PublicTenantCall.execute(payload.getTenantId(), () -> {
            // 链接锁到人（#94 修票 ST-1 结构根因）：已建档的链接绑收方 ID，只允许改那个人；
            // 待建档的链接绑链接本身，只允许新建——持链接者不能拿去改本租户里别的已知身份证档案。
            assertTokenCoversSubmit(payload, reqVO);
            // 与代录壳**同一个 submit**：同一份收方档案、同一处落库、同一套字段
            OnboardingWizardSubmitRespVO resp = onboardingWizardService.submit(reqVO);
            // 建档完成，换一枚实名令牌，让本人接着在自己手机上做实名（ADR 0007 补充）
            resp.setOnboardingToken(mintOnboardingToken(resp.getPayeeId()));
            resp.setOnboardingExpiresTime(onboardingExpiresTime(resp.getOnboardingToken()));
            // 一枚链接只建一份档案：成功落库后才占用唯一一次使用次数。
            // 校验不过 / 弱网重提同一枚链接时不占（重提读到的是「链接已用尽」，上一次的写入随事务回滚）。
            publicTokenService.consume(payload);
            return resp;
        });
    }

    /**
     * 令牌与提交内容的绑定校验：已建档链接只能改令牌绑定的那一个人，待建档链接只能新建。
     */
    private void assertTokenCoversSubmit(PublicTokenPayload payload, OnboardingWizardSubmitReqVO reqVO) {
        Long boundPayeeId = boundPayeeId(payload);
        if (boundPayeeId != null) {
            PayeeInfoDO bound = payeeInfoService.getPayeeInfo(boundPayeeId);
            if (bound == null) {
                throw exception(WIZARD_INVITE_PAYEE_NOT_FOUND);
            }
            if (!StrUtil.equals(bound.getIdCardNo(), reqVO.getIdCardNo())) {
                throw exception(WIZARD_INVITE_PAYEE_MISMATCH);
            }
            return;
        }
        // 待建档链接：本人必须还没有档案，否则应先由收货员从「已建档」入口生成一枚锁到人的链接
        if (payeeInfoService.getPayeeInfoByIdCardNo(reqVO.getIdCardNo()) != null) {
            throw exception(WIZARD_INVITE_PERSON_ALREADY_ARCHIVED);
        }
    }

    /** 令牌绑的是收方 ID 时返回它，绑的是链接本身时返回 {@code null}（见 {@link PublicTokenPayload}）。 */
    private Long boundPayeeId(PublicTokenPayload payload) {
        if (!PublicTokenPurposeEnum.BusinessKeyType.PAYEE.name().equals(payload.getBusinessKeyType())) {
            return null;
        }
        return Long.valueOf(payload.getBusinessKey());
    }

    private String mintOnboardingToken(Long payeeId) {
        PublicTokenCreateReqVO createReqVO = new PublicTokenCreateReqVO();
        createReqVO.setPurpose(PublicTokenPurposeEnum.ONBOARDING.getCode());
        createReqVO.setPayeeId(payeeId);
        PublicTokenRespVO resp = publicTokenService.mint(createReqVO);
        return resp.getToken();
    }

    private LocalDateTime onboardingExpiresTime(String token) {
        return token == null ? null
                : toLocalDateTime(publicTokenService.verify(token, PublicTokenPurposeEnum.ONBOARDING).getExpiresAt());
    }

    private LocalDateTime toLocalDateTime(Long epochSecond) {
        return epochSecond == null ? null
                : LocalDateTime.ofInstant(Instant.ofEpochSecond(epochSecond), ZoneId.systemDefault());
    }

}
