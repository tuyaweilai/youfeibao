package cn.iocoder.yudao.module.icbc.controller.admin.wizard;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.icbc.controller.admin.wizard.vo.*;
import cn.iocoder.yudao.module.icbc.service.wizard.PublicOnboardingWizardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * 公开端点 - 本人自填建档（#94）。
 *
 * <p>建档向导（{@link OnboardingWizardController}）的**第二个壳**：收货员把一枚免注册一次性链接
 * 交给自然人本人，本人在自己手机上打开链接走完同一套五步向导。这里只做「验令牌 → 切租户 → 转发」
 * 到同一个 {@code OnboardingWizardService}，字段与落库位置和代录壳完全一致。
 *
 * <p>URL 已随 {@code /icbc/public/**} 进安全白名单与 {@code yudao.tenant.ignore-urls}：不带登录态、
 * 也不带 {@code tenant-id} 请求头，租户由令牌解析。
 *
 * <p><b>请求体一律不记访问日志</b>（{@code @ApiAccessLog(requestEnable = false)}，与代录壳同一条口径）：
 * 三枚识别的请求体就是证件 / 银行卡影像本身，{@code submit} 带完整 PII。平台的
 * {@code ApiAccessLogFilter} 默认把 {@code /admin-api} 的 JSON 请求体写进
 * {@code infra_api_access_log.request_params}，不关就与 ADR 0037「识别完即弃」、本票「不落库」相抵。
 * 由 {@code PublicOnboardingWizardAccessLogAnnotationTest} 钉住。
 */
@Tag(name = "公开端点 - 本人自填建档")
@RestController
@RequestMapping("/icbc/public/onboarding-wizard")
@Validated
public class PublicOnboardingWizardController {

    @Resource
    private PublicOnboardingWizardService publicOnboardingWizardService;

    @GetMapping("/context")
    @Operation(summary = "打开链接先验一次令牌：有效返回用途与有效期，失效给可读错误")
    @Parameter(name = "token", description = "自填建档公开令牌", required = true)
    public CommonResult<PublicOnboardingWizardContextRespVO> context(@RequestParam("token") String token) {
        return success(publicOnboardingWizardService.context(token));
    }

    @PostMapping("/id-card/front")
    @Operation(summary = "识别身份证人像面（无状态，图片不留存）")
    @ApiAccessLog(requestEnable = false)
    public CommonResult<IdCardFrontRecognizeRespVO> recognizeIdCardFront(
            @RequestParam("token") String token,
            @Valid @RequestBody IdCardFrontRecognizeReqVO reqVO) {
        return success(publicOnboardingWizardService.recognizeIdCardFront(token, reqVO));
    }

    @PostMapping("/id-card/back")
    @Operation(summary = "识别身份证国徽面（无状态，图片不留存）")
    @ApiAccessLog(requestEnable = false)
    public CommonResult<IdCardBackRecognizeRespVO> recognizeIdCardBack(
            @RequestParam("token") String token,
            @Valid @RequestBody IdCardBackRecognizeReqVO reqVO) {
        return success(publicOnboardingWizardService.recognizeIdCardBack(token, reqVO));
    }

    @PostMapping("/bank-card")
    @Operation(summary = "识别银行卡（无状态，图片不留存）")
    @ApiAccessLog(requestEnable = false)
    public CommonResult<BankCardRecognizeRespVO> recognizeBankCard(
            @RequestParam("token") String token,
            @Valid @RequestBody BankCardRecognizeReqVO reqVO) {
        return success(publicOnboardingWizardService.recognizeBankCard(token, reqVO));
    }

    @PostMapping("/submit")
    @Operation(summary = "提交建档：一次性落库（与代录壳同一个向导 Service）")
    @ApiAccessLog(requestEnable = false)
    public CommonResult<OnboardingWizardSubmitRespVO> submit(
            @RequestParam("token") String token,
            @Valid @RequestBody OnboardingWizardSubmitReqVO reqVO) {
        return success(publicOnboardingWizardService.submit(token, reqVO));
    }

}
