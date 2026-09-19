package cn.iocoder.yudao.module.icbc.controller.app.seller;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.icbc.controller.app.seller.vo.*;
import cn.iocoder.yudao.module.icbc.service.sellerportal.SellerPortalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.servlet.ServletUtils.getClientIP;

/**
 * 自然人出售者端 - 首页与记录（#34）。
 *
 * <p>登录后按「该场站 + 该自然人主体」翻自己的账：待我确认 / 卖货记录（按回收企业分组）/
 * 收款记录 / 发票与税费 / 我的资料（含企业授权与自助撤销）。所有端点都要求**显式带 naturalPersonId**，
 * 并校验它绑在当前登录名下；跨企业仅本人可见。
 */
@Tag(name = "自然人出售者端 - 首页与记录")
@RestController
@RequestMapping("/icbc/seller/portal")
@Validated
@Slf4j
public class SellerPortalController {

    @Resource
    private SellerPortalService sellerPortalService;

    @GetMapping("/home")
    @Operation(summary = "首页摘要（待我确认）")
    @Parameter(name = "naturalPersonId", description = "自然人主体编号", required = true)
    @Parameter(name = "stationId", description = "场站编号（扫码进入时带上；待确认结算单按该场站匹配）")
    public CommonResult<SellerHomeRespVO> home(@RequestParam("naturalPersonId") Long naturalPersonId,
                                               @RequestParam(value = "stationId", required = false) Long stationId) {
        return success(sellerPortalService.getHome(naturalPersonId, stationId));
    }

    @GetMapping("/records")
    @Operation(summary = "卖货记录（按回收企业分组）")
    @Parameter(name = "naturalPersonId", description = "自然人主体编号", required = true)
    public CommonResult<List<SellerRecordGroupRespVO>> records(@RequestParam("naturalPersonId") Long naturalPersonId) {
        return success(sellerPortalService.getRecordGroups(naturalPersonId));
    }

    @GetMapping("/payments")
    @Operation(summary = "收款记录")
    @Parameter(name = "naturalPersonId", description = "自然人主体编号", required = true)
    public CommonResult<List<SellerPaymentRespVO>> payments(@RequestParam("naturalPersonId") Long naturalPersonId) {
        return success(sellerPortalService.getPayments(naturalPersonId));
    }

    @GetMapping("/invoices")
    @Operation(summary = "发票与税费（年度汇总 + 逐票明细）")
    @Parameter(name = "naturalPersonId", description = "自然人主体编号", required = true)
    @Parameter(name = "year", description = "年度，为空取当前年")
    public CommonResult<SellerInvoiceSummaryRespVO> invoices(@RequestParam("naturalPersonId") Long naturalPersonId,
                                                             @RequestParam(value = "year", required = false) Integer year) {
        return success(sellerPortalService.getInvoices(naturalPersonId, year));
    }

    @GetMapping("/authorizations")
    @Operation(summary = "企业授权列表")
    @Parameter(name = "naturalPersonId", description = "自然人主体编号", required = true)
    public CommonResult<List<SellerAuthorizationRespVO>> authorizations(
            @RequestParam("naturalPersonId") Long naturalPersonId) {
        return success(sellerPortalService.getAuthorizations(naturalPersonId));
    }

    @PostMapping("/authorizations/revoke")
    @Operation(summary = "自助撤销企业授权", description = "只拦未来，已开出的票不追溯")
    public CommonResult<Boolean> revokeAuthorization(
            @Valid @RequestBody SellerRevokeAuthorizationReqVO reqVO) {
        sellerPortalService.revokeAuthorization(reqVO);
        return success(true);
    }

    @GetMapping("/profile")
    @Operation(summary = "我的资料（收款账户尾号、联系方式、客服）")
    @Parameter(name = "naturalPersonId", description = "自然人主体编号", required = true)
    public CommonResult<SellerProfileRespVO> profile(@RequestParam("naturalPersonId") Long naturalPersonId) {
        return success(sellerPortalService.getProfile(naturalPersonId));
    }

    @PostMapping("/bank-card/change")
    @Operation(summary = "变更收款账户（换银行卡）",
            description = "卡号由本人填；返回一次性 ONBOARDING 令牌，前端用它打开工行收方入驻表单。审核期间新交易的付款挂起。")
    public CommonResult<SellerBankCardChangeRespVO> requestBankCardChange(
            @Valid @RequestBody SellerBankCardChangeReqVO reqVO) {
        return success(sellerPortalService.requestBankCardChange(reqVO, getClientIP()));
    }

    @PostMapping("/payments/received")
    @Operation(summary = "我收到了", description = "自然人自行确认，不改动银行状态")
    public CommonResult<Boolean> confirmReceived(@Valid @RequestBody SellerConfirmReceiveReqVO reqVO) {
        sellerPortalService.confirmReceived(reqVO, getClientIP());
        return success(true);
    }

    @GetMapping("/invoice/download")
    @Operation(summary = "下载自己的发票 PDF")
    @Parameter(name = "naturalPersonId", description = "自然人主体编号", required = true)
    @Parameter(name = "invoiceOrderId", description = "开票订单编号", required = true)
    public void downloadInvoice(@RequestParam("naturalPersonId") Long naturalPersonId,
                                @RequestParam("invoiceOrderId") Long invoiceOrderId,
                                HttpServletResponse response) {
        sellerPortalService.downloadInvoicePdf(naturalPersonId, invoiceOrderId, response);
    }

    @GetMapping("/acquisition/confirmation")
    @Operation(summary = "单笔收购确认书（可打印 / 保存为 PDF）")
    public void acquisitionConfirmation(@RequestParam("naturalPersonId") Long naturalPersonId,
                                        @RequestParam("acquisitionId") Long acquisitionId,
                                        HttpServletResponse response) {
        sellerPortalService.writeAcquisitionConfirmation(naturalPersonId, acquisitionId, response);
    }

    @GetMapping("/settlement/confirmation")
    @Operation(summary = "结算确认书（可打印 / 保存为 PDF）")
    public void settlementConfirmation(@RequestParam("naturalPersonId") Long naturalPersonId,
                                       @RequestParam("settlementId") Long settlementId,
                                       HttpServletResponse response) {
        sellerPortalService.writeSettlementConfirmation(naturalPersonId, settlementId, response);
    }

}
