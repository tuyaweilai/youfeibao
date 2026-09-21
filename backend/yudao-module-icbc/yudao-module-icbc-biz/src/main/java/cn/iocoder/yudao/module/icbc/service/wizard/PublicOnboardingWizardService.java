package cn.iocoder.yudao.module.icbc.service.wizard;

import cn.iocoder.yudao.module.icbc.controller.admin.wizard.vo.*;

import javax.validation.Valid;

/**
 * 自填建档公开壳 Service（#94，ADR 0007 补充）。
 *
 * <p>它是同一个 {@link OnboardingWizardService} 的**第二个壳**：代录壳走带登录态的管理端点，
 * 自填壳走免登录的公开端点，租户由公开令牌解析出来。文件字段、落库位置与代录壳完全一致
 * ——本类只做「验令牌 → 切租户 → 转发」，不复制一份向导、也不另起一套 VO。
 *
 * <p>令牌绑定的是**链接本身**（{@code BusinessKeyType.ONBOARDING_INVITE}）：链接生成时这个人
 * 可能还没有收方档案，档案要等 {@code submit} 落库时才建，所以中途退出不会留下半成品档案。
 */
public interface PublicOnboardingWizardService {

    /**
     * 打开链接时先验一次令牌：有效则返回用途与有效期；过期 / 作废 / 用途不符给出可读错误。
     */
    PublicOnboardingWizardContextRespVO context(String token);

    /**
     * 识别身份证人像面（无状态，图片不留存）。
     */
    IdCardFrontRecognizeRespVO recognizeIdCardFront(String token, @Valid IdCardFrontRecognizeReqVO reqVO);

    /**
     * 识别身份证国徽面（无状态，图片不留存）。
     */
    IdCardBackRecognizeRespVO recognizeIdCardBack(String token, @Valid IdCardBackRecognizeReqVO reqVO);

    /**
     * 识别银行卡（无状态，图片不留存）。
     */
    BankCardRecognizeRespVO recognizeBankCard(String token, @Valid BankCardRecognizeReqVO reqVO);

    /**
     * 一次性落库：与代录壳同一个 {@link OnboardingWizardService#submit}，落同一份收方档案。
     */
    OnboardingWizardSubmitRespVO submit(String token, @Valid OnboardingWizardSubmitReqVO reqVO);

}
