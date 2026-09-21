package cn.iocoder.yudao.module.icbc.service.publicapi.impl;

import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
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
import cn.iocoder.yudao.module.icbc.dal.dataobject.agreement.IcbcFrameworkAgreementDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.lead.IcbcContactLeadDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.agreement.IcbcFrameworkAgreementMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.lead.IcbcContactLeadMapper;
import cn.iocoder.yudao.module.icbc.enums.FrameworkAgreementSignMethodEnum;
import cn.iocoder.yudao.module.icbc.enums.PublicTokenPurposeEnum;
import cn.iocoder.yudao.module.icbc.service.download.InvoiceDownloadService;
import cn.iocoder.yudao.module.icbc.service.onboarding.SellerOnboardingService;
import cn.iocoder.yudao.module.icbc.service.notify.SellerNotifyService;
import cn.iocoder.yudao.module.icbc.service.publicapi.PublicAccessService;
import cn.iocoder.yudao.module.icbc.service.quota.NaturalPersonQuotaService;
import cn.iocoder.yudao.module.icbc.service.tax.AnnualSettlementService;
import cn.iocoder.yudao.module.icbc.service.token.PublicTokenPayload;
import cn.iocoder.yudao.module.icbc.service.token.PublicTokenService;
import cn.iocoder.yudao.module.icbc.util.PublicTenantCall;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.*;

/**
 * 公开端点 Service 实现。
 *
 * <p>入口在安全与租户白名单里（{@code /icbc/public/**}），没有租户上下文；每个方法先用
 * 令牌校验并占用次数，再用 {@link PublicTenantCall} 切到令牌解析出的租户下执行，等于把多租户
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
    private static final String STEP_DONE = "DONE";

    @Resource
    private PublicTokenService publicTokenService;
    @Resource
    private InvoiceDownloadService invoiceDownloadService;
    @Resource
    private IcbcContactLeadMapper contactLeadMapper;
    @Resource
    private IcbcFrameworkAgreementMapper frameworkAgreementMapper;
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
        PublicTenantCall.run(payload.getTenantId(), () -> {
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
        PublicTenantCall.run(payload.getTenantId(), () -> contactLeadMapper.insert(IcbcContactLeadDO.builder()
                .payeeId(Long.valueOf(payload.getBusinessKey()))
                .name(reqVO.getName())
                .mobile(reqVO.getMobile())
                .remark(reqVO.getRemark())
                .build()));
    }

    @Override
    public PublicQuotaRespVO queryQuota(String token) {
        PublicTokenPayload payload = publicTokenService.redeem(token, PublicTokenPurposeEnum.QUOTA_QUERY);
        return PublicTenantCall.execute(payload.getTenantId(), () -> {
            // 额度是自然人的：这里跨租户合并了他在本平台其它租户的开票额
            SellerQuotaRespVO quota = naturalPersonQuotaService.getQuota(Long.valueOf(payload.getBusinessKey()));
            return BeanUtils.toBean(quota, PublicQuotaRespVO.class);
        });
    }

    @Override
    public PublicSettlementStatementRespVO querySettlement(String token) {
        PublicTokenPayload payload = publicTokenService.redeem(token, PublicTokenPurposeEnum.SETTLEMENT_STATEMENT);
        return PublicTenantCall.execute(payload.getTenantId(), () -> {
            SellerSettlementStatementRespVO statement = annualSettlementService.getStatement(
                    Long.valueOf(payload.getBusinessKey()), null);
            return toPublicStatement(statement);
        });
    }

    @Override
    public PublicNoticeRespVO queryNotice(String token) {
        PublicTokenPayload payload = publicTokenService.redeem(token, PublicTokenPurposeEnum.SELLER_NOTICE);
        return PublicTenantCall.execute(payload.getTenantId(), () ->
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
    public PublicOnboardingPageRespVO getOnboardingPage(String token) {
        PublicTokenPayload payload = publicTokenService.redeem(token, PublicTokenPurposeEnum.ONBOARDING);
        Long payeeId = Long.valueOf(payload.getBusinessKey());
        return PublicTenantCall.execute(payload.getTenantId(), () -> buildOnboardingPage(payeeId));
    }

    private PublicOnboardingPageRespVO buildOnboardingPage(Long payeeId) {
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
        // 实名之后就没人需要再点东西了：收方入驻由平台自动发起（ADR 0035）
        resp.setStep(STEP_DONE);
        resp.setStepName("已完成");
        resp.setMessage("实名已完成，其余手续由平台办理，无需你再操作。");
        return resp;
    }

    @Override
    public PublicOnboardingStatusRespVO syncOnboarding(String token) {
        PublicTokenPayload payload = publicTokenService.redeem(token, PublicTokenPurposeEnum.ONBOARDING);
        Long payeeId = Long.valueOf(payload.getBusinessKey());
        return PublicTenantCall.execute(payload.getTenantId(), () -> {
            SellerOnboardingRespVO current = sellerOnboardingService.getOnboarding(payeeId);
            // 按当前步骤主动向工行查一次，把状态收敛回来
            if (!REAL_NAME_PASSED.equals(current.getRealNameStatus())) {
                sellerOnboardingService.syncRealName(payeeId);
            } else if (current.getBankCardChangeStatusName() != null
                    || !ONBOARDING_READY.equals(current.getOnboardingState())) {
                // 换卡在途时也要查：入驻结果是属于新卡的（#37）
                sellerOnboardingService.syncOnboarding(payeeId);
            }
            return toOnboardingStatus(payeeId, sellerOnboardingService.getOnboarding(payeeId));
        });
    }

    @Override
    public void writeOnboardingForm(String token, HttpServletResponse response) {
        PublicTokenPayload payload = publicTokenService.redeem(token, PublicTokenPurposeEnum.ONBOARDING);
        Long payeeId = Long.valueOf(payload.getBusinessKey());
        String formHtml = PublicTenantCall.execute(payload.getTenantId(),
                () -> buildOnboardingPage(payeeId).getFormHtml());
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

    private PublicOnboardingStatusRespVO toOnboardingStatus(Long payeeId, SellerOnboardingRespVO overview) {
        PublicOnboardingStatusRespVO resp = new PublicOnboardingStatusRespVO();
        resp.setStep(currentStep(overview));
        resp.setRealNameStatusName(overview.getRealNameStatusName());
        resp.setRealNameStatus(overview.getRealNameStatus());
        resp.setRealNameMsg(overview.getRealNameMsg());
        resp.setOnboardingStateName(overview.getOnboardingStateName());
        resp.setBankCardChangeStatusName(overview.getBankCardChangeStatusName());
        resp.setNextStep(overview.getNextStep());
        resp.setInvoiceEligible(overview.getInvoiceEligible());
        // 落点页（purpose=ONBOARDING）要能把「待签署」这件事显出来：向导第 5 步转达的链接就落到这里，
        // 本人做完实名不必关掉重开 App 才看得到「去签署」（#95 SP-1）。只有待签的**电子**协议才给入口，
        // 纸质协议当场生效、不存在这份待办。
        IcbcFrameworkAgreementDO pending = frameworkAgreementMapper.selectPendingByPayeeId(payeeId);
        boolean pendingElectronic = pending != null
                && FrameworkAgreementSignMethodEnum.ELECTRONIC.getCode().equals(pending.getSignMethod());
        resp.setPendingAgreement(pendingElectronic);
        resp.setPendingAgreementNo(pendingElectronic ? pending.getAgreementNo() : null);
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
        return REAL_NAME_PASSED.equals(overview.getRealNameStatus()) ? STEP_DONE : STEP_REAL_NAME;
    }

}
