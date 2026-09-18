package cn.iocoder.yudao.module.icbc.controller.admin.onboarding;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.icbc.controller.admin.onboarding.vo.*;
import cn.iocoder.yudao.module.icbc.controller.admin.payee.vo.PayeeInfoRespVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.agreement.IcbcFrameworkAgreementDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.authorization.IcbcSellerAuthorizationDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payee.PayeeInfoDO;
import cn.iocoder.yudao.module.icbc.service.onboarding.SellerOnboardingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * 管理后台 - 出售者建档
 *
 * <p>把 issue #6 的一次性手续收在一条链路上：实人认证 → 收方入驻（绑定本人银行卡）→
 * 签署框架收购协议 → 首次反向开票与代办税费授权。开办完成后，回头客只凭身份证 / 手机号
 * 即可带出档案，不再重新登记。
 */
@Tag(name = "管理后台 - 出售者建档")
@RestController
@RequestMapping("/icbc/seller-onboarding")
@Validated
public class SellerOnboardingController {

    @Resource
    private SellerOnboardingService sellerOnboardingService;

    @GetMapping("/get")
    @Operation(summary = "获得出售者建档总览")
    @Parameter(name = "payeeId", description = "出售者编号", required = true, example = "1024")
    @PreAuthorize("@icbc.hasPermission('icbc:seller-onboarding:execute')")
    public CommonResult<SellerOnboardingRespVO> getOnboarding(@RequestParam("payeeId") Long payeeId) {
        return success(sellerOnboardingService.getOnboarding(payeeId));
    }

    @GetMapping("/returning-customer")
    @Operation(summary = "回头客带档：按身份证或手机号带出既有档案")
    @PreAuthorize("@icbc.hasPermission('icbc:payee-info:query')")
    public CommonResult<PayeeInfoRespVO> findReturningCustomer(
            @RequestParam(value = "idCardNo", required = false) String idCardNo,
            @RequestParam(value = "mobile", required = false) String mobile) {
        PayeeInfoDO payee = sellerOnboardingService.findReturningCustomer(idCardNo, mobile);
        return success(payee == null ? null : BeanUtils.toBean(payee, PayeeInfoRespVO.class));
    }

    // ==================== 实人认证 ====================

    @PostMapping("/real-name/start")
    @Operation(summary = "发起实人认证（返回工行 H5 页面表单）")
    @PreAuthorize("@icbc.hasPermission('icbc:seller-onboarding:execute')")
    public CommonResult<SellerStepRespVO> startRealName(@Valid @RequestBody SellerRealNameReqVO reqVO) {
        return success(sellerOnboardingService.startRealName(reqVO));
    }

    @PostMapping("/real-name/sync")
    @Operation(summary = "查询实人认证结果")
    @Parameter(name = "payeeId", description = "出售者编号", required = true, example = "1024")
    @PreAuthorize("@icbc.hasPermission('icbc:seller-onboarding:execute')")
    public CommonResult<SellerOnboardingRespVO> syncRealName(@RequestParam("payeeId") Long payeeId) {
        sellerOnboardingService.syncRealName(payeeId);
        return success(sellerOnboardingService.getOnboarding(payeeId));
    }

    // ==================== 收方入驻 ====================

    @PostMapping("/onboarding/submit")
    @Operation(summary = "发起收方入驻（返回工行页面表单）")
    @PreAuthorize("@icbc.hasPermission('icbc:seller-onboarding:execute')")
    public CommonResult<SellerStepRespVO> submitOnboarding(
            @Valid @RequestBody SellerOnboardingSubmitReqVO reqVO) {
        return success(sellerOnboardingService.submitOnboarding(reqVO));
    }

    @PostMapping("/onboarding/sync")
    @Operation(summary = "查询收方入驻结果（两条成败线四种组合）")
    @Parameter(name = "payeeId", description = "出售者编号", required = true, example = "1024")
    @PreAuthorize("@icbc.hasPermission('icbc:seller-onboarding:execute')")
    public CommonResult<SellerOnboardingRespVO> syncOnboarding(@RequestParam("payeeId") Long payeeId) {
        sellerOnboardingService.syncOnboarding(payeeId);
        return success(sellerOnboardingService.getOnboarding(payeeId));
    }

    @PostMapping("/contact-fallback")
    @Operation(summary = "入驻失败时留下联系方式等待联系")
    @PreAuthorize("@icbc.hasPermission('icbc:seller-onboarding:execute')")
    public CommonResult<Boolean> leaveContactFallback(
            @Valid @RequestBody SellerContactFallbackReqVO reqVO) {
        sellerOnboardingService.leaveContactFallback(reqVO);
        return success(true);
    }

    // ==================== 框架收购协议 ====================

    @PostMapping("/agreement/create")
    @Operation(summary = "签署 / 更新框架收购协议")
    @PreAuthorize("@icbc.hasPermission('icbc:seller-agreement:manage')")
    public CommonResult<Long> saveFrameworkAgreement(
            @Valid @RequestBody FrameworkAgreementSaveReqVO reqVO) {
        return success(sellerOnboardingService.saveFrameworkAgreement(reqVO));
    }

    @GetMapping("/agreement/get")
    @Operation(summary = "获得生效中的框架收购协议")
    @Parameter(name = "payeeId", description = "出售者编号", required = true, example = "1024")
    @PreAuthorize("@icbc.hasPermission('icbc:seller-agreement:manage')")
    public CommonResult<FrameworkAgreementRespVO> getFrameworkAgreement(
            @RequestParam("payeeId") Long payeeId) {
        IcbcFrameworkAgreementDO agreement = sellerOnboardingService.getActiveFrameworkAgreement(payeeId);
        return success(agreement == null ? null : BeanUtils.toBean(agreement, FrameworkAgreementRespVO.class));
    }

    @GetMapping("/agreement/list")
    @Operation(summary = "获得出售者的全部框架收购协议")
    @Parameter(name = "payeeId", description = "出售者编号", required = true, example = "1024")
    @PreAuthorize("@icbc.hasPermission('icbc:seller-agreement:manage')")
    public CommonResult<List<FrameworkAgreementRespVO>> getFrameworkAgreementList(
            @RequestParam("payeeId") Long payeeId) {
        return success(sellerOnboardingService.getFrameworkAgreements(payeeId).stream()
                .map(item -> BeanUtils.toBean(item, FrameworkAgreementRespVO.class))
                .collect(Collectors.toList()));
    }

    // ==================== 首次授权 ====================

    @PostMapping("/authorization/create")
    @Operation(summary = "记录出售者首次反向开票与代办税费授权")
    @PreAuthorize("@icbc.hasPermission('icbc:seller-authorization:manage')")
    public CommonResult<Long> authorizeSeller(
            @Valid @RequestBody SellerAuthorizationSaveReqVO reqVO) {
        return success(sellerOnboardingService.authorizeSeller(reqVO));
    }

    @GetMapping("/authorization/get")
    @Operation(summary = "获得出售者最近一次授权")
    @Parameter(name = "payeeId", description = "出售者编号", required = true, example = "1024")
    @PreAuthorize("@icbc.hasPermission('icbc:seller-authorization:manage')")
    public CommonResult<SellerAuthorizationRespVO> getSellerAuthorization(
            @RequestParam("payeeId") Long payeeId) {
        IcbcSellerAuthorizationDO authorization = sellerOnboardingService.getSellerAuthorization(payeeId);
        return success(authorization == null ? null
                : BeanUtils.toBean(authorization, SellerAuthorizationRespVO.class));
    }

}
