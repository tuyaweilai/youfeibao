package cn.iocoder.yudao.module.icbc.service.wizard.impl;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.icbc.controller.admin.publictoken.vo.PublicTokenCreateReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.publictoken.vo.PublicTokenRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.wizard.vo.*;
import cn.iocoder.yudao.module.icbc.enums.PublicTokenPurposeEnum;
import cn.iocoder.yudao.module.icbc.service.token.PublicTokenPayload;
import cn.iocoder.yudao.module.icbc.service.token.PublicTokenService;
import cn.iocoder.yudao.module.icbc.service.wizard.OnboardingWizardService;
import cn.iocoder.yudao.module.icbc.service.wizard.PublicOnboardingWizardService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.function.Supplier;

/**
 * 自填建档公开壳 Service 实现（#94）。
 *
 * <p>每个方法只做三件事：验令牌（用途必须是 {@code ONBOARDING_WIZARD}）→ 按令牌里的租户号
 * 切回正确租户 → 转发给代录壳同一个 {@link OnboardingWizardService}。图片与 PII 从不在这里落库，
 * 落库位置与字段完全交给向导 Service（保证两个壳「同一处落库」）。
 *
 * <p>令牌占用口径：识别与打开页面**不占次数**（本人会重拍、回退、切后台），只有
 * {@code submit} 成功落库后才占用唯一一次（{@code maxUses = 1}）——一枚链接只建一份档案。
 * 失败（校验不过 / 撞已有档案）不占次数，本人重试看得懂原因。
 */
@Service
@Validated
public class PublicOnboardingWizardServiceImpl implements PublicOnboardingWizardService {

    @Resource
    private PublicTokenService publicTokenService;
    @Resource
    private OnboardingWizardService onboardingWizardService;

    @Override
    public PublicOnboardingWizardContextRespVO context(String token) {
        PublicTokenPayload payload = publicTokenService.verify(token, PublicTokenPurposeEnum.ONBOARDING_WIZARD);
        return PublicOnboardingWizardContextRespVO.builder()
                .purpose(payload.getPurpose())
                .purposeName(PublicTokenPurposeEnum.ONBOARDING_WIZARD.getName())
                .expiresTime(toLocalDateTime(payload.getExpiresAt()))
                .build();
    }

    @Override
    public IdCardFrontRecognizeRespVO recognizeIdCardFront(String token, @Valid IdCardFrontRecognizeReqVO reqVO) {
        PublicTokenPayload payload = publicTokenService.verify(token, PublicTokenPurposeEnum.ONBOARDING_WIZARD);
        return inTenant(payload.getTenantId(), () -> onboardingWizardService.recognizeIdCardFront(reqVO));
    }

    @Override
    public IdCardBackRecognizeRespVO recognizeIdCardBack(String token, @Valid IdCardBackRecognizeReqVO reqVO) {
        PublicTokenPayload payload = publicTokenService.verify(token, PublicTokenPurposeEnum.ONBOARDING_WIZARD);
        return inTenant(payload.getTenantId(), () -> onboardingWizardService.recognizeIdCardBack(reqVO));
    }

    @Override
    public BankCardRecognizeRespVO recognizeBankCard(String token, @Valid BankCardRecognizeReqVO reqVO) {
        PublicTokenPayload payload = publicTokenService.verify(token, PublicTokenPurposeEnum.ONBOARDING_WIZARD);
        return inTenant(payload.getTenantId(), () -> onboardingWizardService.recognizeBankCard(reqVO));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OnboardingWizardSubmitRespVO submit(String token, @Valid OnboardingWizardSubmitReqVO reqVO) {
        PublicTokenPayload payload = publicTokenService.verify(token, PublicTokenPurposeEnum.ONBOARDING_WIZARD);
        return inTenant(payload.getTenantId(), () -> {
            // 与代录壳**同一个 submit**：同一份收方档案、同一处落库、同一套字段
            OnboardingWizardSubmitRespVO resp = onboardingWizardService.submit(reqVO);
            // 建档完成，换一枚实名令牌，让本人接着在自己手机上做实名（ADR 0007 补充）
            resp.setOnboardingToken(mintOnboardingToken(resp.getPayeeId()));
            resp.setOnboardingExpiresTime(onboardingExpiresTime(resp.getOnboardingToken()));
            // 一枚链接只建一份档案：成功落库后才占用唯一一次使用次数。
            // 若不成功（校验不过 / 弱网重提撞已有档案）就不占，本人重试的仍是同一条可读错误。
            publicTokenService.consume(payload);
            return resp;
        });
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

    /**
     * 切到令牌里的租户执行。
     *
     * <p>不用框架的 {@code TenantUtils.execute(Callable)}：它把异常包成 {@code RuntimeException}，
     * 会把 {@code ServiceException} 的业务错误码吃掉变成 500。这里保留原样抛出，与公开端点既有的
     * {@code PublicAccessServiceImpl#inTenant} 同一口径。
     */
    private <T> T inTenant(Long tenantId, Supplier<T> supplier) {
        Long oldTenantId = TenantContextHolder.getTenantId();
        Boolean oldIgnore = TenantContextHolder.isIgnore();
        TenantContextHolder.setTenantId(tenantId);
        TenantContextHolder.setIgnore(false);
        try {
            return supplier.get();
        } finally {
            TenantContextHolder.setTenantId(oldTenantId);
            TenantContextHolder.setIgnore(oldIgnore);
        }
    }

}
