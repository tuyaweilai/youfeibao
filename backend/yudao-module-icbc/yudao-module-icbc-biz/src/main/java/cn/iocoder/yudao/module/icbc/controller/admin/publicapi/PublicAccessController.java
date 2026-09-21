package cn.iocoder.yudao.module.icbc.controller.admin.publicapi;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.icbc.controller.admin.publicapi.vo.PublicContactLeadReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.publicapi.vo.PublicOnboardingPageRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.publicapi.vo.PublicOnboardingStatusRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.publicapi.vo.PublicNoticeRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.publicapi.vo.PublicQuotaRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.publicapi.vo.PublicSettlementStatementRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.publicapi.vo.PublicStationRespVO;
import cn.iocoder.yudao.module.icbc.service.publicapi.PublicAccessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.servlet.ServletUtils.getClientIP;

/**
 * 公开端点 - 自然人免登录。
 *
 * <p>本控制器的 URL 已登记进安全白名单与 {@code yudao.tenant.ignore-urls}：没有登录态、
 * 也不带 {@code tenant-id} 请求头。租户由令牌解析出来，见 {@code PublicAccessServiceImpl}。
 */
@Tag(name = "公开端点 - 自然人免登录")
@RestController
@RequestMapping("/icbc/public")
@Validated
@Slf4j
public class PublicAccessController {

    @Resource
    private PublicAccessService publicAccessService;

    @GetMapping("/station")
    @Operation(summary = "解析场站二维码（只编码场站码，返回公开信息，不含个人数据）")
    @Parameter(name = "code", description = "场站码", required = true)
    public CommonResult<PublicStationRespVO> resolveStation(@RequestParam("code") String code) {
        return success(publicAccessService.resolveStation(code, getClientIP()));
    }

    @GetMapping("/invoice/download")
    @Operation(summary = "用令牌下载发票 PDF")
    @Parameter(name = "token", description = "公开令牌", required = true)
    public void downloadInvoice(@RequestParam("token") String token, HttpServletResponse response) {
        publicAccessService.downloadInvoicePdf(token, response);
    }

    @PostMapping("/contact-lead")
    @Operation(summary = "提交收方入驻失败后的联系方式")
    public CommonResult<Boolean> submitContactLead(@Valid @RequestBody PublicContactLeadReqVO reqVO) {
        publicAccessService.submitContactLead(reqVO);
        return success(true);
    }

    @GetMapping("/quota")
    @Operation(summary = "查询自然人滚动额度余量")
    @Parameter(name = "token", description = "公开令牌", required = true)
    public CommonResult<PublicQuotaRespVO> queryQuota(@RequestParam("token") String token) {
        return success(publicAccessService.queryQuota(token));
    }

    @GetMapping("/settlement")
    @Operation(summary = "查询自然人汇算清缴对账单（开票与已缴税款）")
    @Parameter(name = "token", description = "公开令牌", required = true)
    public CommonResult<PublicSettlementStatementRespVO> querySettlement(@RequestParam("token") String token) {
        return success(publicAccessService.querySettlement(token));
    }

    @GetMapping("/notice")
    @Operation(summary = "用令牌查看触达通知（待确认结算 / 付款异常），打开即可看，无需注册")
    @Parameter(name = "token", description = "公开令牌", required = true)
    public CommonResult<PublicNoticeRespVO> queryNotice(@RequestParam("token") String token) {
        return success(publicAccessService.queryNotice(token));
    }

    @GetMapping("/onboarding/page")
    @Operation(summary = "用令牌取自然人当前该做的工行实名页面")
    @Parameter(name = "token", description = "公开令牌", required = true)
    public CommonResult<PublicOnboardingPageRespVO> getOnboardingPage(@RequestParam("token") String token) {
        return success(publicAccessService.getOnboardingPage(token));
    }

    @PostMapping("/onboarding/sync")
    @Operation(summary = "用令牌刷新建档状态（向工行主动查询一次）")
    @Parameter(name = "token", description = "公开令牌", required = true)
    public CommonResult<PublicOnboardingStatusRespVO> syncOnboarding(@RequestParam("token") String token) {
        return success(publicAccessService.syncOnboarding(token));
    }

    @GetMapping("/onboarding/form")
    @Operation(summary = "用令牌直接取当前该做的工行实名页面 HTML")
    @Parameter(name = "token", description = "公开令牌", required = true)
    public void onboardingForm(@RequestParam("token") String token,
                              HttpServletResponse response) throws IOException {
        publicAccessService.writeOnboardingForm(token, response);
    }

}
