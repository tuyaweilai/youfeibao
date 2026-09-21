package cn.iocoder.yudao.module.icbc.controller.admin.wizard;

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
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.SELLER_ONBOARDING_EXECUTE + "')")
    public CommonResult<IdCardFrontRecognizeRespVO> recognizeIdCardFront(
            @Valid @RequestBody IdCardFrontRecognizeReqVO reqVO) {
        return success(onboardingWizardService.recognizeIdCardFront(reqVO));
    }

    @PostMapping("/id-card/back")
    @Operation(summary = "识别身份证国徽面（无状态，图片不留存）")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.SELLER_ONBOARDING_EXECUTE + "')")
    public CommonResult<IdCardBackRecognizeRespVO> recognizeIdCardBack(
            @Valid @RequestBody IdCardBackRecognizeReqVO reqVO) {
        return success(onboardingWizardService.recognizeIdCardBack(reqVO));
    }

    @PostMapping("/bank-card")
    @Operation(summary = "识别银行卡（无状态，图片不留存）")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.SELLER_ONBOARDING_EXECUTE + "')")
    public CommonResult<BankCardRecognizeRespVO> recognizeBankCard(
            @Valid @RequestBody BankCardRecognizeReqVO reqVO) {
        return success(onboardingWizardService.recognizeBankCard(reqVO));
    }

    @PostMapping("/submit")
    @Operation(summary = "提交建档：一次性落库并落框架收购协议（未开通电子签章即 PAPER）")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.SELLER_ONBOARDING_EXECUTE + "')")
    public CommonResult<OnboardingWizardSubmitRespVO> submit(
            @Valid @RequestBody OnboardingWizardSubmitReqVO reqVO) {
        return success(onboardingWizardService.submit(reqVO));
    }

}
