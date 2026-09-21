package cn.iocoder.yudao.module.icbc.controller.admin.wizard;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.icbc.controller.admin.wizard.vo.*;
import cn.iocoder.yudao.module.icbc.enums.RecyclingPermission;
import cn.iocoder.yudao.module.icbc.service.wizard.OnboardingWizardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * 管理后台 - 建档向导（现场端五步壳，#91）。
 *
 * <p>三步识别都是**无状态**的上传 + 识别：图片随请求进来、识别完即弃，不进文件服务、不落库
 * （ADR 0037）。识别结果按「只在空缺处回填、人工输入优先」合并后返回，前端确认页据此展示。
 *
 * <p>权限复用既有的建档权限，**不新增门禁**（#81）；收货员本就有
 * {@link RecyclingPermission#SELLER_ONBOARDING_EXECUTE} 与
 * {@link RecyclingPermission#SELLER_AGREEMENT_MANAGE}。
 *
 * <p><b>请求体一律不记访问日志</b>（{@code @ApiAccessLog(requestEnable = false)}，#91 复审 ST-A）：
 * 三枚识别的请求体就是证件 / 银行卡影像本身（{@code imageBase64}，现场端按 10M 上限传），{@code submit}
 * 带完整 PII（姓名 / 身份证号 / 手机号 / 住址 / 银行卡号）。平台的 {@code ApiAccessLogFilter} 默认把
 * {@code /admin-api} 的 JSON 请求体截 8000 字符写进 {@code infra_api_access_log.request_params}，而
 * {@code SANITIZE_KEYS} 只脱敏 password / token——不关就会与 ADR 0037 决策 5「图片识别完即弃」、
 * 本票验收「影像不落库」相抵。这条不变量由 {@code OnboardingWizardAccessLogAnnotationTest} 钉住。
 */
@Tag(name = "管理后台 - 建档向导")
@RestController
@RequestMapping("/icbc/onboarding-wizard")
@Validated
public class OnboardingWizardController {

    @Resource
    private OnboardingWizardService onboardingWizardService;

    @PostMapping("/id-card/front")
    @Operation(summary = "识别身份证人像面（无状态，图片不留存）")
    @ApiAccessLog(requestEnable = false)
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.SELLER_ONBOARDING_EXECUTE + "')")
    public CommonResult<IdCardFrontRecognizeRespVO> recognizeIdCardFront(
            @Valid @RequestBody IdCardFrontRecognizeReqVO reqVO) {
        return success(onboardingWizardService.recognizeIdCardFront(reqVO));
    }

    @PostMapping("/id-card/back")
    @Operation(summary = "识别身份证国徽面（无状态，图片不留存）")
    @ApiAccessLog(requestEnable = false)
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.SELLER_ONBOARDING_EXECUTE + "')")
    public CommonResult<IdCardBackRecognizeRespVO> recognizeIdCardBack(
            @Valid @RequestBody IdCardBackRecognizeReqVO reqVO) {
        return success(onboardingWizardService.recognizeIdCardBack(reqVO));
    }

    @PostMapping("/bank-card")
    @Operation(summary = "识别银行卡（无状态，图片不留存）")
    @ApiAccessLog(requestEnable = false)
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.SELLER_ONBOARDING_EXECUTE + "')")
    public CommonResult<BankCardRecognizeRespVO> recognizeBankCard(
            @Valid @RequestBody BankCardRecognizeReqVO reqVO) {
        return success(onboardingWizardService.recognizeBankCard(reqVO));
    }

    @PostMapping("/submit")
    @Operation(summary = "提交建档：一次性落库并落框架收购协议（未开通电子签章即 PAPER）")
    @ApiAccessLog(requestEnable = false)
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.SELLER_ONBOARDING_EXECUTE + "')")
    public CommonResult<OnboardingWizardSubmitRespVO> submit(
            @Valid @RequestBody OnboardingWizardSubmitReqVO reqVO) {
        return success(onboardingWizardService.submit(reqVO));
    }

}
