package cn.iocoder.yudao.module.icbc.service.publicapi.impl;

import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.icbc.controller.admin.download.vo.InvoiceDownloadRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.download.vo.InvoiceFileRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.onboarding.vo.SellerOnboardingRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.onboarding.vo.SellerOnboardingSubmitReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.onboarding.vo.SellerRealNameReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.onboarding.vo.SellerStepRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.publicapi.vo.PublicContactLeadReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.publicapi.vo.PublicOnboardingPageRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.publicapi.vo.PublicOnboardingStatusRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.publicapi.vo.PublicNoticeRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.publicapi.vo.PublicQuotaRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.publicapi.vo.PublicSettlementStatementRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.publicapi.vo.PublicStationRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.quota.vo.SellerQuotaRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.tax.vo.SellerSettlementStatementRespVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.lead.IcbcContactLeadDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.lead.IcbcContactLeadMapper;
import cn.iocoder.yudao.module.icbc.enums.PublicTokenPurposeEnum;
import cn.iocoder.yudao.module.icbc.service.download.InvoiceDownloadService;
import cn.iocoder.yudao.module.icbc.service.onboarding.SellerOnboardingService;
import cn.iocoder.yudao.module.icbc.service.notify.SellerNotifyService;
import cn.iocoder.yudao.module.icbc.service.publicapi.PublicAccessService;
import cn.iocoder.yudao.module.icbc.service.quota.NaturalPersonQuotaService;
import cn.iocoder.yudao.module.icbc.service.tax.AnnualSettlementService;
import cn.iocoder.yudao.module.icbc.service.token.PublicTokenPayload;
import cn.iocoder.yudao.module.icbc.service.token.PublicTokenService;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.function.Supplier;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.*;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.*;

/**
 * 公开端点 Service 实现。
 *
 * <p>入口在安全与租户白名单里（{@code /icbc/public/**}），没有租户上下文；每个方法先用
 * 令牌校验并占用次数，再显式 {@link #inTenant} 切到令牌解析出的租户下执行，等于把多租户
 * 隔离补回来，而不是绕过它。
 */
@Service
@Validated
public class PublicAccessServiceImpl implements PublicAccessService {

    /** 实人认证通过（realNameStatus=2） */
    private static final Integer REAL_NAME_PASSED = 2;
    /** 收方入驻就绪 */
    private static final String ONBOARDING_READY = "READY";
    private static final String STEP_REAL_NAME = "REAL_NAME";
    private static final String STEP_ONBOARDING = "ONBOARDING";
    private static final String STEP_DONE = "DONE";

    @Resource
    private PublicTokenService publicTokenService;
    @Resource
    private InvoiceDownloadService invoiceDownloadService;
    @Resource
    private IcbcContactLeadMapper contactLeadMapper;
    @Resource
    private NaturalPersonQuotaService naturalPersonQuotaService;
    @Resource
    private AnnualSettlementService annualSettlementService;
    @Resource
    private SellerOnboardingService sellerOnboardingService;
    @Resource
    private SellerNotifyService sellerNotifyService;
    @Resource
    private cn.iocoder.yudao.module.icbc.service.station.StationService stationService;

    @Override
    public void downloadInvoicePdf(String token, HttpServletResponse response) {
        PublicTokenPayload payload = publicTokenService.verify(token, PublicTokenPurposeEnum.INVOICE_DOWNLOAD);
        inTenant(payload.getTenantId(), () -> {
            InvoiceDownloadRespVO record = invoiceDownloadService.getDownloadRecord(payload.getBusinessKey());
            InvoiceFileRespVO pdf = (record.getFiles() == null
                    ? Collections.<InvoiceFileRespVO>emptyList() : record.getFiles()).stream()
                    .filter(file -> file.getFileType() != null && "PDF".equalsIgnoreCase(file.getFileType()))
                    .findFirst()
                    .orElseThrow(() -> exception(INVOICE_FILE_NOT_FOUND));
            // 确认原件确实可取，再扣次数：单用途令牌不该被一次失败的取件烧掉
            publicTokenService.consume(payload);
            invoiceDownloadService.downloadFile(record.getId(), pdf.getFileType(), response);
        });
    }

    @Override
    public void submitContactLead(PublicContactLeadReqVO reqVO) {
        PublicTokenPayload payload = publicTokenService.redeem(reqVO.getToken(), PublicTokenPurposeEnum.CONTACT_LEAD);
        inTenant(payload.getTenantId(), () -> contactLeadMapper.insert(IcbcContactLeadDO.builder()
                .payeeId(Long.valueOf(payload.getBusinessKey()))
                .name(reqVO.getName())
                .mobile(reqVO.getMobile())
                .remark(reqVO.getRemark())
                .build()));
    }

    @Override
    public PublicQuotaRespVO queryQuota(String token) {
        PublicTokenPayload payload = publicTokenService.redeem(token, PublicTokenPurposeEnum.QUOTA_QUERY);
        return inTenant(payload.getTenantId(), () -> {
            // 额度是自然人的：这里跨租户合并了他在本平台其它租户的开票额
            SellerQuotaRespVO quota = naturalPersonQuotaService.getQuota(Long.valueOf(payload.getBusinessKey()));
            return BeanUtils.toBean(quota, PublicQuotaRespVO.class);
        });
    }

    @Override
    public PublicSettlementStatementRespVO querySettlement(String token) {
        PublicTokenPayload payload = publicTokenService.redeem(token, PublicTokenPurposeEnum.SETTLEMENT_STATEMENT);
        return inTenant(payload.getTenantId(), () -> {
            SellerSettlementStatementRespVO statement = annualSettlementService.getStatement(
                    Long.valueOf(payload.getBusinessKey()), null);
            return toPublicStatement(statement);
        });
    }

    @Override
    public PublicNoticeRespVO queryNotice(String token) {
        PublicTokenPayload payload = publicTokenService.redeem(token, PublicTokenPurposeEnum.SELLER_NOTICE);
        return inTenant(payload.getTenantId(), () ->
                sellerNotifyService.getNoticeForPayee(Long.valueOf(payload.getBusinessKey())));
    }

    private PublicSettlementStatementRespVO toPublicStatement(SellerSettlementStatementRespVO statement) {
        PublicSettlementStatementRespVO resp = BeanUtils.toBean(statement, PublicSettlementStatementRespVO.class);
        if (statement.getMonths() != null) {
            resp.setMonths(statement.getMonths().stream()
                    .map(month -> BeanUtils.toBean(month, PublicSettlementStatementRespVO.MonthStatement.class))
                    .toList());
        }
        return resp;
    }

    @Override
    public PublicOnboardingPageRespVO getOnboardingPage(String token, String trxChannel) {
        PublicTokenPayload payload = publicTokenService.redeem(token, PublicTokenPurposeEnum.ONBOARDING);
        Long payeeId = Long.valueOf(payload.getBusinessKey());
        return inTenant(payload.getTenantId(), () -> buildOnboardingPage(payeeId, trxChannel));
    }

    private PublicOnboardingPageRespVO buildOnboardingPage(Long payeeId, String trxChannel) {
        SellerOnboardingRespVO overview = sellerOnboardingService.getOnboarding(payeeId);
        PublicOnboardingPageRespVO resp = new PublicOnboardingPageRespVO();
        resp.setPayeeId(payeeId);
        if (!REAL_NAME_PASSED.equals(overview.getRealNameStatus())) {
            SellerRealNameReqVO req = new SellerRealNameReqVO();
            req.setPayeeId(payeeId);
            SellerStepRespVO step = sellerOnboardingService.startRealName(req);
            resp.setStep(STEP_REAL_NAME);
            resp.setStepName("实名认证");
            resp.setFormHtml(step == null ? null : step.getFormHtml());
            resp.setMessage("请在工行页面完成人脸识别实名认证。");
            return resp;
        }
        // 换卡（#37）：有在途变更时不能因「建档已完成」而短路——要输出**新卡**的收方入驻页面。
        // 走的是同一个 ONBOARDING 令牌与后端输出表单机制，不新造流程（ADR 0010）。
        if (sellerOnboardingService.hasPendingBankCardChange(payeeId)) {
            SellerOnboardingSubmitReqVO req = new SellerOnboardingSubmitReqVO();
            req.setPayeeId(payeeId);
            req.setTrxChannel(trxChannel == null || trxChannel.isBlank() ? "03" : trxChannel);
            SellerStepRespVO step = sellerOnboardingService.submitOnboarding(req);
            resp.setStep(STEP_ONBOARDING);
            resp.setStepName("变更银行卡");
            resp.setFormHtml(step == null ? null : step.getFormHtml());
            resp.setMessage("请在工行页面绑定你的新银行卡并完成审核。审核期间新交易的付款会挂起，原卡在你确认前仍然有效。");
            return resp;
        }
        if (!ONBOARDING_READY.equals(overview.getOnboardingState())) {
            if (isOnboardingFailed(overview.getOnboardingState())) {
                resp.setStep(STEP_DONE);
                resp.setStepName("已结束");
                resp.setMessage("收方入驻未通过"
                        + (overview.getOnboardingStateName() == null ? "" : "（" + overview.getOnboardingStateName() + "）")
                        + "，请留联系方式等待联系。");
                return resp;
            }
            SellerOnboardingSubmitReqVO req = new SellerOnboardingSubmitReqVO();
            req.setPayeeId(payeeId);
            req.setTrxChannel(trxChannel == null || trxChannel.isBlank() ? "03" : trxChannel);
            SellerStepRespVO step = sellerOnboardingService.submitOnboarding(req);
            resp.setStep(STEP_ONBOARDING);
            resp.setStepName("收方入驻");
            resp.setFormHtml(step == null ? null : step.getFormHtml());
            resp.setMessage("请在工行页面绑定本人银行卡完成收方入驻。");
            return resp;
        }
        resp.setStep(STEP_DONE);
        resp.setStepName("已完成");
        resp.setMessage("建档已完成，无需再办。");
        return resp;
    }

    @Override
    public PublicOnboardingStatusRespVO syncOnboarding(String token) {
        PublicTokenPayload payload = publicTokenService.redeem(token, PublicTokenPurposeEnum.ONBOARDING);
        Long payeeId = Long.valueOf(payload.getBusinessKey());
        return inTenant(payload.getTenantId(), () -> {
            SellerOnboardingRespVO current = sellerOnboardingService.getOnboarding(payeeId);
            // 按当前步骤主动向工行查一次，把状态收敛回来
            if (!REAL_NAME_PASSED.equals(current.getRealNameStatus())) {
                sellerOnboardingService.syncRealName(payeeId);
            } else if (current.getBankCardChangeStatusName() != null
                    || !ONBOARDING_READY.equals(current.getOnboardingState())) {
                // 换卡在途时也要查：入驻结果是属于新卡的（#37）
                sellerOnboardingService.syncOnboarding(payeeId);
            }
            return toOnboardingStatus(sellerOnboardingService.getOnboarding(payeeId));
        });
    }

    @Override
    public void writeOnboardingForm(String token, String trxChannel, HttpServletResponse response) {
        PublicTokenPayload payload = publicTokenService.redeem(token, PublicTokenPurposeEnum.ONBOARDING);
        Long payeeId = Long.valueOf(payload.getBusinessKey());
        String formHtml = inTenant(payload.getTenantId(),
                () -> buildOnboardingPage(payeeId, trxChannel).getFormHtml());
        if (formHtml == null || formHtml.isBlank()) {
            formHtml = "<!DOCTYPE html><html><head><meta charset=\"utf-8\">"
                    + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"></head>"
                    + "<body style=\"font-family:sans-serif;padding:48px;text-align:center;color:#8a919f\">"
                    + "建档已完成，无需再办。</body></html>";
        }
        response.setContentType("text/html;charset=UTF-8");
        try {
            response.getWriter().write(formHtml);
        } catch (IOException e) {
            throw exception(ICBC_API_CALL_FAILED);
        }
    }

    private PublicOnboardingStatusRespVO toOnboardingStatus(SellerOnboardingRespVO overview) {
        PublicOnboardingStatusRespVO resp = new PublicOnboardingStatusRespVO();
        resp.setStep(currentStep(overview));
        resp.setRealNameStatusName(overview.getRealNameStatusName());
        resp.setOnboardingStateName(overview.getOnboardingStateName());
        resp.setBankCardChangeStatusName(overview.getBankCardChangeStatusName());
        resp.setNextStep(overview.getNextStep());
        resp.setInvoiceEligible(overview.getInvoiceEligible());
        if (overview.getBankCardChangeStatusName() != null) {
            resp.setMessage("收款账户变更：" + overview.getBankCardChangeStatusName()
                    + "。审核通过前，新交易的付款会挂起；原卡在你确认变更前仍然有效。");
        } else {
            resp.setMessage(Boolean.TRUE.equals(overview.getInvoiceEligible())
                    ? "建档已完成。" : overview.getInvoiceBlockReason());
        }
        return resp;
    }

    @Override
    public PublicStationRespVO resolveStation(String stationCode, String clientIp) {
        return stationService.resolvePublic(stationCode, clientIp);
    }

    private String currentStep(SellerOnboardingRespVO overview) {
        if (!REAL_NAME_PASSED.equals(overview.getRealNameStatus())) {
            return STEP_REAL_NAME;
        }
        // 换卡在途：即使建档已完成，他也还有一步要做（在工行页面绑新卡）
        if (overview.getBankCardChangeStatusName() != null) {
            return STEP_ONBOARDING;
        }
        return ONBOARDING_READY.equals(overview.getOnboardingState()) ? STEP_DONE : STEP_ONBOARDING;
    }

    private boolean isOnboardingFailed(String state) {
        return "REJECTED".equals(state) || "OPENACCT_FAILED".equals(state)
                || "FAILED_AND_REJECTED".equals(state);
    }

    private void inTenant(Long tenantId, Runnable runnable) {
        inTenant(tenantId, () -> {
            runnable.run();
            return null;
        });
    }

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
