package cn.iocoder.yudao.module.icbc.service.sellerportal.impl;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.module.icbc.controller.admin.download.vo.InvoiceDownloadRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.download.vo.InvoiceFileRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.payee.vo.PayeeBankCardChangeSaveReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.onboarding.vo.SellerOnboardingSubmitReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.publictoken.vo.PublicTokenCreateReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.publictoken.vo.PublicTokenRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.settlement.vo.SettlementRespVO;
import cn.iocoder.yudao.module.icbc.controller.app.seller.vo.*;
import cn.iocoder.yudao.module.icbc.dal.dataobject.acquisition.IcbcAcquisitionDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.agreement.IcbcFrameworkAgreementDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.authorization.IcbcSellerAuthorizationDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.invoice.InvoiceOrderDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.naturalperson.IcbcNaturalPersonDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payee.IcbcPayeeBankCardChangeDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payee.PayeeInfoDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payment.PaymentOrderDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.settlement.IcbcSettlementDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.acquisition.IcbcAcquisitionMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.agreement.IcbcFrameworkAgreementMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.authorization.IcbcSellerAuthorizationMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.invoice.InvoiceOrderMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.payee.PayeeInfoMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.payment.PaymentOrderMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.settlement.IcbcSettlementMapper;
import cn.iocoder.yudao.module.icbc.enums.*;
import cn.iocoder.yudao.module.icbc.service.download.InvoiceDownloadService;
import cn.iocoder.yudao.module.icbc.service.naturalperson.NaturalPersonService;
import cn.iocoder.yudao.module.icbc.service.payee.PayeeBankCardChangeService;
import cn.iocoder.yudao.module.icbc.service.onboarding.SellerOnboardingService;
import cn.iocoder.yudao.module.icbc.service.sellerportal.SellerPortalService;
import cn.iocoder.yudao.module.icbc.service.settlement.SettlementService;
import cn.iocoder.yudao.module.icbc.service.station.StationService;
import cn.iocoder.yudao.module.icbc.service.token.PublicTokenService;
import cn.iocoder.yudao.module.icbc.util.MaskUtils;
import cn.iocoder.yudao.module.system.api.tenant.TenantApi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.*;

/**
 * 自然人出售者端门户 Service 实现（#34）。
 *
 * <p>跨租户读取集中在本类，且一律用显式的 {@link TenantUtils#executeIgnore} 表达：只对自然人本人开放，
 * 回收企业侧不可见（CONTEXT.md「交易可见性边界」）。每次读取都先 {@link #assertBound}，
 * 不静默推断操作人。
 */
@Service
@Validated
@Slf4j
public class SellerPortalServiceImpl implements SellerPortalService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /** 「待我确认」只放需要他动作的：待确认 + 需线下签字。有异议（等待企业回复）不算他的动作。 */
    private static final Set<Integer> PENDING_SETTLEMENT_STATUSES = Set.of(
            SettlementConfirmStatusEnum.PENDING.getStatus(),
            SettlementConfirmStatusEnum.OFFLINE_REQUIRED.getStatus());

    /** 「银行已受理」：可核验的成功态 */
    private static final Set<Integer> BANK_ACCEPTED_STATUSES = Set.of(
            PaymentStatusEnum.SUCCESS.getStatus());
    /** 钱可能已经动了（部分成功 / 他行已扣款 / 已支付待签收）：「我收到了」按钮放行 */
    private static final Set<Integer> RECEIVE_CONFIRMABLE_STATUSES = Set.of(
            PaymentStatusEnum.SUCCESS.getStatus(),
            PaymentStatusEnum.PARTIAL_SUCCESS.getStatus(),
            PaymentStatusEnum.PAID_PENDING_RECEIPT.getStatus(),
            PaymentStatusEnum.OTHER_BANK_DEBITED.getStatus());

    @Value("${icbc.seller.service-mobile:400-000-0000}")
    private String serviceMobile;

    @Resource
    private NaturalPersonService naturalPersonService;
    @Resource
    private PayeeInfoMapper payeeInfoMapper;
    @Resource
    private IcbcAcquisitionMapper acquisitionMapper;
    @Resource
    private PaymentOrderMapper paymentOrderMapper;
    @Resource
    private InvoiceOrderMapper invoiceOrderMapper;
    @Resource
    private IcbcSellerAuthorizationMapper authorizationMapper;
    @Resource
    private IcbcFrameworkAgreementMapper agreementMapper;
    @Resource
    private IcbcSettlementMapper settlementMapper;
    @Resource
    private SettlementService settlementService;
    @Resource
    private InvoiceDownloadService invoiceDownloadService;
    @Resource
    private TenantApi tenantApi;
    @Resource
    private PayeeBankCardChangeService payeeBankCardChangeService;
    @Resource
    private StationService stationService;
    @Resource
    private PublicTokenService publicTokenService;
    @Resource
    private SellerOnboardingService sellerOnboardingService;

    // ==================== 首页 ====================

    @Override
    public SellerHomeRespVO getHome(Long naturalPersonId, Long stationId) {
        assertBound(naturalPersonId);
        List<PayeeInfoDO> payees = payeesOf(naturalPersonId);
        Map<Long, PayeeInfoDO> payeeById = payees.stream()
                .collect(Collectors.toMap(PayeeInfoDO::getId, Function.identity(), (a, b) -> a));

        List<SellerPendingItemVO> items = new ArrayList<>();
        // 1) 待确认结算单：待确认 / 需线下签字（有异议是等企业回复，不算他需要动作的事）。
        //    扫码进入时按「该场站 + 该自然人主体」匹配（#34 / ADR 0018）；不传场站则不按场站筛。
        TenantUtils.executeIgnore(() -> settlementMapper
                        .selectListByNaturalPersonIdAndStation(naturalPersonId, stationId))
                .stream()
                .filter(settlement -> PENDING_SETTLEMENT_STATUSES.contains(settlement.getConfirmStatus()))
                .forEach(settlement -> items.add(toPendingSettlement(settlement)));
        // 2) 待签框架协议
        if (!payeeById.isEmpty()) {
            TenantUtils.executeIgnore(() -> agreementMapper.selectPendingByPayeeIds(payeeById.keySet()))
                    .forEach(agreement -> items.add(toPendingAgreement(agreement, payeeById.get(agreement.getPayeeId()))));
        }

        SellerHomeRespVO resp = new SellerHomeRespVO();
        resp.setPendingItems(items);
        resp.setPendingCount(items.size());
        resp.setPendingSettlementCount((int) items.stream()
                .filter(item -> "SETTLEMENT".equals(item.getType())).count());
        resp.setPendingAgreementCount((int) items.stream()
                .filter(item -> "AGREEMENT".equals(item.getType())).count());
        resp.setAmountScopeNote("本平台累计，不含你在其他渠道的交易");
        resp.setStationId(stationId);
        if (stationId != null) {
            resp.setStationName(TenantUtils.executeIgnore(() -> stationService.getStation(stationId).getName()));
        }
        return resp;
    }

    // ==================== 卖货记录 ====================

    @Override
    public List<SellerRecordGroupRespVO> getRecordGroups(Long naturalPersonId) {
        assertBound(naturalPersonId);
        List<PayeeInfoDO> payees = payeesOf(naturalPersonId);
        if (payees.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, PayeeInfoDO> payeeById = payees.stream()
                .collect(Collectors.toMap(PayeeInfoDO::getId, Function.identity(), (a, b) -> a));
        List<IcbcAcquisitionDO> acquisitions = TenantUtils.executeIgnore(
                () -> acquisitionMapper.selectListByPayeeIds(payeeById.keySet()));

        Map<Long, List<IcbcAcquisitionDO>> byTenant = new LinkedHashMap<>();
        for (IcbcAcquisitionDO acquisition : acquisitions) {
            PayeeInfoDO payee = payeeById.get(acquisition.getPayeeId());
            if (payee == null) {
                continue;
            }
            byTenant.computeIfAbsent(payee.getTenantId(), k -> new ArrayList<>()).add(acquisition);
        }

        List<SellerRecordGroupRespVO> groups = new ArrayList<>();
        for (Map.Entry<Long, List<IcbcAcquisitionDO>> entry : byTenant.entrySet()) {
            List<IcbcAcquisitionDO> list = entry.getValue();
            SellerRecordGroupRespVO group = new SellerRecordGroupRespVO();
            group.setTenantId(entry.getKey());
            group.setEnterpriseName(enterpriseName(entry.getKey()));
            group.setCount(list.size());
            group.setTotalAmount(list.stream()
                    .filter(acq -> !AcquisitionStatusEnum.CANCELLED.getStatus().equals(acq.getStatus()))
                    .map(acq -> nullSafe(acq.getAmount()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
            group.setTotalSettlementWeight(list.stream()
                    .filter(acq -> !AcquisitionStatusEnum.CANCELLED.getStatus().equals(acq.getStatus()))
                    .map(acq -> nullSafe(acq.getSettlementWeight()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
            group.setRecords(list.stream().map(acq -> toRecord(acq, group.getEnterpriseName())).toList());
            groups.add(group);
        }
        groups.sort(Comparator.comparing(SellerRecordGroupRespVO::getTenantId));
        return groups;
    }

    // ==================== 收款记录 ====================

    @Override
    public List<SellerPaymentRespVO> getPayments(Long naturalPersonId) {
        assertBound(naturalPersonId);
        List<PayeeInfoDO> payees = payeesOf(naturalPersonId);
        if (payees.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, PayeeInfoDO> payeeById = payees.stream()
                .collect(Collectors.toMap(PayeeInfoDO::getId, Function.identity(), (a, b) -> a));
        List<IcbcAcquisitionDO> acquisitions = TenantUtils.executeIgnore(
                () -> acquisitionMapper.selectListByPayeeIds(payeeById.keySet()));
        Map<Long, IcbcAcquisitionDO> acqById = acquisitions.stream()
                .collect(Collectors.toMap(IcbcAcquisitionDO::getId, Function.identity(), (a, b) -> a));
        Map<Long, PayeeInfoDO> payeeByAcqId = acquisitions.stream()
                .filter(acq -> payeeById.containsKey(acq.getPayeeId()))
                .collect(Collectors.toMap(IcbcAcquisitionDO::getId, acq -> payeeById.get(acq.getPayeeId()), (a, b) -> a));
        if (acqById.isEmpty()) {
            return Collections.emptyList();
        }
        List<PaymentOrderDO> payments = TenantUtils.executeIgnore(
                () -> paymentOrderMapper.selectListByAcquisitionIds(acqById.keySet()));
        return payments.stream().map(payment -> {
            IcbcAcquisitionDO acq = acqById.get(payment.getAcquisitionId());
            PayeeInfoDO payee = payeeByAcqId.get(payment.getAcquisitionId());
            return toPayment(payment, acq, payee == null ? null : enterpriseName(payee.getTenantId()));
        }).toList();
    }

    // ==================== 发票与税费 ====================

    @Override
    public SellerInvoiceSummaryRespVO getInvoices(Long naturalPersonId, Integer year) {
        assertBound(naturalPersonId);
        int targetYear = year == null ? LocalDateTime.now().getYear() : year;
        List<PayeeInfoDO> payees = payeesOf(naturalPersonId);
        Map<Long, PayeeInfoDO> payeeById = payees.stream()
                .collect(Collectors.toMap(PayeeInfoDO::getId, Function.identity(), (a, b) -> a));

        SellerInvoiceSummaryRespVO summary = new SellerInvoiceSummaryRespVO();
        summary.setYear(targetYear);
        summary.setTaxScopeNote("税务端可核验的口径由各回收企业的开票记录构成");
        summary.setInvoices(Collections.emptyList());
        summary.setInvoiceCount(0);
        summary.setTotalInvoiceAmount(BigDecimal.ZERO);
        summary.setTotalTaxAmount(BigDecimal.ZERO);
        if (payeeById.isEmpty()) {
            return summary;
        }
        List<InvoiceOrderDO> invoices = TenantUtils.executeIgnore(
                () -> invoiceOrderMapper.selectListByPayeeIds(payeeById.keySet()));
        List<SellerInvoiceRespVO> details = invoices.stream()
                .map(invoice -> toInvoice(invoice, payeeById.get(invoice.getPayeeId())))
                .toList();
        // 未开票的按创建时间归年，已开票的按开票日期归年
        List<SellerInvoiceRespVO> ofYear = details.stream()
                .filter(invoice -> invoiceYear(invoice) != null && invoiceYear(invoice) == targetYear)
                .toList();
        summary.setInvoices(ofYear);
        summary.setInvoiceCount(ofYear.size());
        summary.setTotalInvoiceAmount(ofYear.stream()
                .map(invoice -> nullSafe(invoice.getInvoiceAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        summary.setTotalTaxAmount(ofYear.stream()
                .map(invoice -> nullSafe(invoice.getTaxAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        return summary;
    }

    // ==================== 企业授权 ====================

    @Override
    public List<SellerAuthorizationRespVO> getAuthorizations(Long naturalPersonId) {
        assertBound(naturalPersonId);
        List<PayeeInfoDO> payees = payeesOf(naturalPersonId);
        if (payees.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, PayeeInfoDO> payeeById = payees.stream()
                .collect(Collectors.toMap(PayeeInfoDO::getId, Function.identity(), (a, b) -> a));
        List<IcbcSellerAuthorizationDO> authorizations = TenantUtils.executeIgnore(
                () -> authorizationMapper.selectListByPayeeIds(payeeById.keySet()));
        // 每个收方档案只取最新一条
        Map<Long, IcbcSellerAuthorizationDO> latestByPayee = new HashMap<>();
        for (IcbcSellerAuthorizationDO authorization : authorizations) {
            latestByPayee.putIfAbsent(authorization.getPayeeId(), authorization);
        }
        List<SellerAuthorizationRespVO> result = new ArrayList<>();
        for (PayeeInfoDO payee : payees) {
            result.add(toAuthorization(payee, latestByPayee.get(payee.getId())));
        }
        result.sort(Comparator.comparing(SellerAuthorizationRespVO::getTenantId));
        return result;
    }

    @Override
    public void revokeAuthorization(SellerRevokeAuthorizationReqVO reqVO) {
        assertBound(reqVO.getNaturalPersonId());
        Long tenantId = reqVO.getTenantId();
        PayeeInfoDO payee = TenantUtils.execute(tenantId,
                () -> payeeInfoMapper.selectByNaturalPersonId(reqVO.getNaturalPersonId()));
        if (payee == null) {
            throw exception(SELLER_RECORD_NOT_FOUND);
        }
        IcbcSellerAuthorizationDO authorization = TenantUtils.execute(tenantId,
                () -> authorizationMapper.selectLatestByPayeeId(payee.getId()));
        if (authorization == null) {
            throw exception(SELLER_RECORD_NOT_FOUND);
        }
        if (authorization.getRevokedAt() != null
                || (!Boolean.TRUE.equals(authorization.getReverseInvoiceAuthorized())
                    && !Boolean.TRUE.equals(authorization.getTaxAgencyAuthorized()))) {
            throw exception(SELLER_AUTHORIZATION_ALREADY_REVOKED);
        }
        IcbcSellerAuthorizationDO update = new IcbcSellerAuthorizationDO();
        update.setId(authorization.getId());
        update.setReverseInvoiceAuthorized(false);
        update.setTaxAgencyAuthorized(false);
        update.setRevokedAt(LocalDateTime.now());
        update.setRevokeReason(StrUtil.blankToDefault(reqVO.getReason(), "自然人自助撤销"));
        TenantUtils.execute(tenantId, () -> authorizationMapper.updateById(update));
    }

    // ==================== 我的资料 ====================

    @Override
    public SellerProfileRespVO getProfile(Long naturalPersonId) {
        assertBound(naturalPersonId);
        IcbcNaturalPersonDO person = naturalPersonService.getNaturalPerson(naturalPersonId);
        SellerProfileRespVO resp = new SellerProfileRespVO();
        resp.setName(person.getName());
        resp.setMobileMasked(MaskUtils.maskMobile(person.getMobile()));
        resp.setIdCardMasked(MaskUtils.maskIdCard(person.getIdCardNo()));
        PayeeRealNameStatusEnum realName = PayeeRealNameStatusEnum.of(person.getRealNameStatus());
        resp.setRealNameStatus(person.getRealNameStatus());
        resp.setRealNameStatusName(realName == null ? null : realName.getName());
        resp.setServiceMobile(serviceMobile);
        resp.setLogoutNote("注销账号不等于删除交易记录：交易记录是税务凭证，会永久保留");
        resp.setBankCards(payeesOf(naturalPersonId).stream().map(payee -> {
            SellerProfileRespVO.SellerBankCardVO card = new SellerProfileRespVO.SellerBankCardVO();
            card.setPayeeId(payee.getId());
            card.setTenantId(payee.getTenantId());
            card.setEnterpriseName(enterpriseName(payee.getTenantId()));
            card.setBankName(payee.getBankName());
            card.setCardTail(MaskUtils.cardTail(payee.getBankCardNo()));
            return card;
        }).toList());
        fillCardChangeStatus(resp.getBankCards());
        return resp;
    }

    /**
     * 收款账户变更状态（#37）：「钱正在换卡途中」对本人必须可见，文案只讲工行能给的那一步（ADR 0021）。
     */
    private void fillCardChangeStatus(List<SellerProfileRespVO.SellerBankCardVO> cards) {
        if (cards == null || cards.isEmpty()) {
            return;
        }
        Set<Long> payeeIds = cards.stream().map(SellerProfileRespVO.SellerBankCardVO::getPayeeId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        if (payeeIds.isEmpty()) {
            return;
        }
        Map<Long, IcbcPayeeBankCardChangeDO> pending = TenantUtils.executeIgnore(
                () -> payeeBankCardChangeService.pendingMap(payeeIds));
        for (SellerProfileRespVO.SellerBankCardVO card : cards) {
            IcbcPayeeBankCardChangeDO change = pending.get(card.getPayeeId());
            if (change == null) {
                continue;
            }
            card.setChangeStatus(change.getStatus());
            card.setChangeStatusName(PayeeBankCardChangeStatusEnum.nameOf(change.getStatus()));
            card.setChangeRequestedAt(change.getRequestedAt());
        }
    }

    // ==================== 变更收款账户（换银行卡，#37） ====================

    @Override
    public SellerBankCardChangeRespVO requestBankCardChange(SellerBankCardChangeReqVO reqVO, String ip) {
        assertBound(reqVO.getNaturalPersonId());
        Long tenantId = reqVO.getTenantId();
        // 只能换本人在**指定回收企业**的那一份收款账户（企业之间互相看不到，ADR 0017）
        PayeeInfoDO payee = TenantUtils.execute(tenantId,
                () -> payeeInfoMapper.selectByNaturalPersonId(reqVO.getNaturalPersonId()));
        if (payee == null) {
            throw exception(SELLER_RECORD_NOT_FOUND);
        }
        PayeeBankCardChangeSaveReqVO saveReqVO = new PayeeBankCardChangeSaveReqVO();
        saveReqVO.setPayeeId(payee.getId());
        saveReqVO.setNewBankCardNo(reqVO.getBankCardNo());
        saveReqVO.setNewBankName(reqVO.getBankName());
        saveReqVO.setNewBankBranch(reqVO.getBankBranch());
        saveReqVO.setAccountCode(reqVO.getAccountCode());
        saveReqVO.setIdSignDate(reqVO.getIdSignDate());
        saveReqVO.setIdValidityPeriod(reqVO.getIdValidityPeriod());
        saveReqVO.setRequestSource("SELLER_PORTAL");
        saveReqVO.setRequestIp(ip);
        IcbcPayeeBankCardChangeDO change = TenantUtils.execute(tenantId,
                () -> payeeBankCardChangeService.requestChange(saveReqVO));
        // 直接提交给工行的**收方修改数据接口**（#89）：不再把一次性令牌交给本人去开页面
        try {
            SellerOnboardingSubmitReqVO submitReqVO = new SellerOnboardingSubmitReqVO();
            submitReqVO.setPayeeId(payee.getId());
            // 用 Runnable 重载：Callable 那支会把 ServiceException 包成 RuntimeException，下面接不住
            TenantUtils.execute(tenantId, () -> {
                sellerOnboardingService.submitOnboarding(submitReqVO);
            });
        } catch (ServiceException e) {
            // 工行没受理：把在途变更取消掉，否则该企业新交易的付款会一直挂起，而本人又无从重试
            TenantUtils.execute(tenantId, () -> payeeBankCardChangeService.cancelChange(
                    change.getId(), "工行受理失败，自动取消"));
            throw e;
        }

        SellerBankCardChangeRespVO resp = new SellerBankCardChangeRespVO();
        resp.setChangeNo(change.getChangeNo());
        resp.setStatus(change.getStatus());
        resp.setStatusName(PayeeBankCardChangeStatusEnum.nameOf(change.getStatus()));
        resp.setOldCardTail(change.getOldCardTail());
        resp.setNewCardTail(MaskUtils.cardTail(change.getNewBankCardNo()));
        resp.setMessage("银行已受理，审核结果以银行为准。");
        resp.setScopeNote("新卡审核通过前，原卡仍然有效；审核期间该企业新交易的付款会挂起，不会打到废卡。");
        return resp;
    }

    @Override
    public SellerRealNameLinkRespVO mintRealNameLink(SellerRealNameLinkReqVO reqVO) {
        assertBound(reqVO.getNaturalPersonId());
        // 本人可能在多家企业都有档案；实名是平台级的，任取其一（这里由调用方给，且必须是他的）
        PayeeInfoDO payee = payeesOf(reqVO.getNaturalPersonId()).stream()
                .filter(item -> Objects.equals(item.getId(), reqVO.getPayeeId()))
                .findFirst()
                .orElseThrow(() -> exception(SELLER_RECORD_NOT_FOUND));
        PublicTokenCreateReqVO tokenReqVO = new PublicTokenCreateReqVO();
        tokenReqVO.setPurpose(PublicTokenPurposeEnum.ONBOARDING.getCode());
        tokenReqVO.setPayeeId(payee.getId());
        // 令牌要落到该档案所属租户下，公开端点才能把请求放回正确的企业
        PublicTokenRespVO token = TenantUtils.execute(payee.getTenantId(), () -> publicTokenService.mint(tokenReqVO));
        SellerRealNameLinkRespVO resp = new SellerRealNameLinkRespVO();
        resp.setToken(token.getToken());
        resp.setExpiresTime(token.getExpiresTime());
        resp.setMessage("实名由你本人在微信里完成；完成后收方入驻由平台自动办理，你不需要再操作。");
        return resp;
    }

    // ==================== 「我收到了」 ====================

    @Override
    public void confirmReceived(SellerConfirmReceiveReqVO reqVO, String ip) {
        assertBound(reqVO.getNaturalPersonId());
        PaymentOrderDO payment = TenantUtils.executeIgnore(
                () -> paymentOrderMapper.selectById(reqVO.getPaymentOrderId()));
        if (payment == null || payment.getAcquisitionId() == null) {
            throw exception(SELLER_RECORD_NOT_FOUND);
        }
        assertAcquisitionOwned(reqVO.getNaturalPersonId(), payment.getAcquisitionId());
        if (payment.getSellerReceivedConfirmedAt() != null) {
            return; // 幂等：重复点不再改时间
        }
        PaymentOrderDO update = new PaymentOrderDO();
        update.setId(payment.getId());
        update.setSellerReceivedConfirmedAt(LocalDateTime.now());
        update.setSellerReceivedConfirmIp(ip);
        // 只记自然人自行确认，**不改 payStatus**（ADR 0021）
        TenantUtils.executeIgnore(() -> paymentOrderMapper.updateById(update));
    }

    // ==================== 发票 PDF / 确认书打印 ====================

    @Override
    public void downloadInvoicePdf(Long naturalPersonId, Long invoiceOrderId, HttpServletResponse response) {
        assertBound(naturalPersonId);
        InvoiceOrderDO invoice = TenantUtils.executeIgnore(
                () -> invoiceOrderMapper.selectById(invoiceOrderId));
        if (invoice == null) {
            throw exception(SELLER_RECORD_NOT_FOUND);
        }
        assertPayeeOwned(naturalPersonId, invoice.getPayeeId());
        InvokeResult result = withIgnoreTenant(() -> {
            InvoiceDownloadRespVO record = invoiceDownloadService.getDownloadRecord(invoice.getPartnerOrderId());
            InvoiceFileRespVO pdf = (record.getFiles() == null
                    ? Collections.<InvoiceFileRespVO>emptyList() : record.getFiles()).stream()
                    .filter(file -> file.getFileType() != null && "PDF".equalsIgnoreCase(file.getFileType()))
                    .findFirst()
                    .orElse(null);
            return new InvokeResult(record, pdf);
        });
        if (result.pdf == null) {
            throw exception(INVOICE_FILE_NOT_FOUND);
        }
        // downloadFile 可能抛业务异常，必须用不吞异常的 withIgnoreTenant，否则错误码会被包成 RuntimeException
        withIgnoreTenant(() -> {
            invoiceDownloadService.downloadFile(result.record.getId(), result.pdf.getFileType(), response);
            return null;
        });
    }

    /** downloadInvoicePdf 的中间结果，避免在 lambda 里抛业务异常 */
    private static final class InvokeResult {
        private final InvoiceDownloadRespVO record;
        private final InvoiceFileRespVO pdf;

        private InvokeResult(InvoiceDownloadRespVO record, InvoiceFileRespVO pdf) {
            this.record = record;
            this.pdf = pdf;
        }
    }

    /**
     * 忽略租户执行（不捕获 / 不包装异常，保证业务错误码能透传）。
     */
    private <T> T withIgnoreTenant(java.util.function.Supplier<T> supplier) {
        Boolean oldIgnore = cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.isIgnore();
        cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.setIgnore(true);
        try {
            return supplier.get();
        } finally {
            cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.setIgnore(oldIgnore);
        }
    }

    @Override
    public void writeAcquisitionConfirmation(Long naturalPersonId, Long acquisitionId, HttpServletResponse response) {
        assertBound(naturalPersonId);
        IcbcAcquisitionDO acquisition = TenantUtils.executeIgnore(
                () -> acquisitionMapper.selectById(acquisitionId));
        if (acquisition == null) {
            throw exception(SELLER_RECORD_NOT_FOUND);
        }
        assertPayeeOwned(naturalPersonId, acquisition.getPayeeId());
        String enterprise = enterpriseName(tenantOfPayee(acquisition.getPayeeId()));
        StringBuilder html = new StringBuilder();
        html.append(pageStart("单笔收购确认书"));
        html.append("<h1>单笔收购确认书</h1>");
        html.append("<p class=\"hint\">本确认书由平台按收购登记事实生成，可打印或保存为 PDF。</p>");
        html.append("<table>");
        kv(html, "收购单号", acquisition.getAcquisitionNo());
        kv(html, "收购方", enterprise);
        kv(html, "出售者", acquisition.getSellerName());
        kv(html, "品类", acquisition.getCategoryName());
        kv(html, "结算重量", num(acquisition.getSettlementWeight()) + " " + StrUtil.blankToDefault(acquisition.getUnit(), ""));
        kv(html, "单价", num(acquisition.getUnitPrice()));
        kv(html, "金额", num(acquisition.getAmount()));
        kv(html, "扣杂", num(acquisition.getDeduction()));
        kv(html, "交易时间", acquisition.getTradeTime() == null ? "" : acquisition.getTradeTime().format(TIME_FORMATTER));
        kv(html, "交易地点", acquisition.getTradeAddress());
        kv(html, "状态", acquisitionStatusName(acquisition.getStatus()));
        if (StrUtil.isNotBlank(acquisition.getCancelReason())) {
            kv(html, "作废原因", acquisition.getCancelReason());
        }
        html.append("</table>");
        html.append(pageEnd());
        writeHtml(response, html.toString());
    }

    @Override
    public void writeSettlementConfirmation(Long naturalPersonId, Long settlementId, HttpServletResponse response) {
        assertBound(naturalPersonId);
        SettlementRespVO settlement = settlementService.getDetailForSeller(naturalPersonId, settlementId);
        String enterprise = enterpriseName(tenantOfSettlement(settlementId));
        StringBuilder html = new StringBuilder();
        html.append(pageStart("结算确认书"));
        html.append("<h1>结算确认书</h1>");
        html.append("<p class=\"hint\">一次到场批次一张结算单。确认的是计量与计价事实，不是付款节奏的承诺。</p>");
        html.append("<table>");
        kv(html, "结算单号", settlement.getSettlementNo());
        kv(html, "收购方", enterprise);
        kv(html, "出售者", settlement.getSellerName());
        kv(html, "版本号", settlement.getCurrentVersionNo() == null ? "" : String.valueOf(settlement.getCurrentVersionNo()));
        kv(html, "确认状态", settlement.getConfirmStatusName());
        kv(html, "生成时间", settlement.getGenerateTime() == null ? ""
                : settlement.getGenerateTime().format(TIME_FORMATTER));
        kv(html, "合计结算重量", num(settlement.getTotalSettlementWeight()));
        kv(html, "合计金额", num(settlement.getTotalAmount()));
        html.append("</table>");
        html.append("<table><thead><tr><th>品类</th><th>结算重量</th><th>单价</th><th>金额</th><th>状态</th></tr></thead><tbody>");
        if (settlement.getLines() != null) {
            for (SettlementRespVO.LineVO line : settlement.getLines()) {
                html.append("<tr><td>").append(esc(line.getCategoryName())).append("</td><td>")
                        .append(num(line.getSettlementWeight())).append("</td><td>")
                        .append(num(line.getUnitPrice())).append("</td><td>")
                        .append(num(line.getAmount())).append("</td><td>")
                        .append(esc(line.getStatusName())).append("</td></tr>");
            }
        }
        html.append("</tbody></table>");
        html.append(pageEnd());
        writeHtml(response, html.toString());
    }

    // ==================== 内部方法 ====================

    private void assertBound(Long naturalPersonId) {
        if (naturalPersonId == null
                || !naturalPersonService.isBoundToLogin(naturalPersonId, SecurityFrameworkUtils.getLoginUserId())) {
            throw exception(NATURAL_PERSON_NOT_BOUND_TO_LOGIN);
        }
    }

    private List<PayeeInfoDO> payeesOf(Long naturalPersonId) {
        return TenantUtils.executeIgnore(() -> payeeInfoMapper.selectListByNaturalPersonId(naturalPersonId));
    }

    private void assertPayeeOwned(Long naturalPersonId, Long payeeId) {
        if (payeeId == null || payeesOf(naturalPersonId).stream()
                .noneMatch(payee -> Objects.equals(payee.getId(), payeeId))) {
            throw exception(SELLER_RECORD_NOT_FOUND);
        }
    }

    private void assertAcquisitionOwned(Long naturalPersonId, Long acquisitionId) {
        IcbcAcquisitionDO acquisition = TenantUtils.executeIgnore(
                () -> acquisitionMapper.selectById(acquisitionId));
        if (acquisition == null) {
            throw exception(SELLER_RECORD_NOT_FOUND);
        }
        assertPayeeOwned(naturalPersonId, acquisition.getPayeeId());
    }

    private Long tenantOfPayee(Long payeeId) {
        PayeeInfoDO payee = TenantUtils.executeIgnore(() -> payeeInfoMapper.selectById(payeeId));
        return payee == null ? null : payee.getTenantId();
    }

    private Long tenantOfSettlement(Long settlementId) {
        IcbcSettlementDO settlement = TenantUtils.executeIgnore(() -> settlementMapper.selectById(settlementId));
        return settlement == null ? null : settlement.getTenantId();
    }

    private String enterpriseName(Long tenantId) {
        String name = tenantApi.getTenantName(tenantId);
        return StrUtil.isBlank(name) ? "回收企业" : name;
    }

    private SellerPendingItemVO toPendingSettlement(IcbcSettlementDO settlement) {
        SellerPendingItemVO item = new SellerPendingItemVO();
        item.setType("SETTLEMENT");
        item.setTypeName(SettlementConfirmStatusEnum.OFFLINE_REQUIRED.getStatus().equals(settlement.getConfirmStatus())
                ? "需线下签字确认" : "待确认结算单");
        item.setTenantId(settlement.getTenantId());
        item.setEnterpriseName(enterpriseName(settlement.getTenantId()));
        item.setSettlementId(settlement.getId());
        item.setPayeeId(settlement.getPayeeId());
        item.setTitle(settlement.getSettlementNo());
        SettlementConfirmStatusEnum status = SettlementConfirmStatusEnum.ofStatus(settlement.getConfirmStatus())
                .orElse(null);
        item.setStatusName(status == null ? null : status.getName());
        item.setDeadlineTime(settlement.getDeadlineTime());
        item.setUrgent(SettlementConfirmStatusEnum.OFFLINE_REQUIRED.getStatus().equals(settlement.getConfirmStatus())
                || (settlement.getDeadlineTime() != null
                    && settlement.getDeadlineTime().isBefore(LocalDateTime.now())));
        return item;
    }

    private SellerPendingItemVO toPendingAgreement(IcbcFrameworkAgreementDO agreement, PayeeInfoDO payee) {
        SellerPendingItemVO item = new SellerPendingItemVO();
        item.setType("AGREEMENT");
        item.setTypeName("待签框架收购协议");
        item.setPayeeId(agreement.getPayeeId());
        if (payee != null) {
            item.setTenantId(payee.getTenantId());
            item.setEnterpriseName(enterpriseName(payee.getTenantId()));
        }
        item.setTitle(agreement.getAgreementNo());
        item.setStatusName("待签署");
        item.setUrgent(false);
        return item;
    }

    private SellerRecordRespVO toRecord(IcbcAcquisitionDO acq, String enterpriseName) {
        SellerRecordRespVO vo = new SellerRecordRespVO();
        vo.setAcquisitionId(acq.getId());
        vo.setAcquisitionNo(acq.getAcquisitionNo());
        vo.setCategoryName(acq.getCategoryName());
        vo.setUnit(acq.getUnit());
        vo.setSettlementWeight(acq.getSettlementWeight());
        vo.setDeduction(acq.getDeduction());
        vo.setDeductionMethod(acq.getDeductionMethod());
        vo.setUnitPrice(acq.getUnitPrice());
        vo.setAmount(acq.getAmount());
        vo.setAcquirerName(enterpriseName);
        vo.setStatus(acq.getStatus());
        vo.setStatusName(acquisitionStatusName(acq.getStatus()));
        vo.setCancelReason(acq.getCancelReason());
        vo.setTradeTime(acq.getTradeTime());
        vo.setTradeAddress(acq.getTradeAddress());
        vo.setPrintable(true);
        return vo;
    }

    private SellerPaymentRespVO toPayment(PaymentOrderDO payment, IcbcAcquisitionDO acq, String enterpriseName) {
        SellerPaymentRespVO vo = new SellerPaymentRespVO();
        vo.setPaymentOrderId(payment.getId());
        vo.setOrderNo(payment.getOrderNo());
        vo.setAcquisitionNo(acq == null ? null : acq.getAcquisitionNo());
        vo.setCategoryName(acq == null ? null : acq.getCategoryName());
        vo.setAcquirerName(enterpriseName);
        vo.setPaymentAmount(payment.getPaymentAmount());
        vo.setActuallyReceivedAmount(payment.getActuallyReceivedAmount());
        vo.setStatus(payment.getPaymentStatus());
        vo.setStatusName(paymentStatusName(payment.getPaymentStatus()));
        vo.setReceiptNo(payment.getReceiptNo());
        vo.setReceiptTime(payment.getReceiptTime());
        vo.setPaymentTime(payment.getPaymentTime());
        vo.setNextStep(paymentNextStep(payment.getPaymentStatus()));
        vo.setSellerReceivedConfirmed(payment.getSellerReceivedConfirmedAt() != null);
        vo.setSellerReceivedConfirmedAt(payment.getSellerReceivedConfirmedAt());
        vo.setCanConfirmReceive(payment.getSellerReceivedConfirmedAt() == null
                && RECEIVE_CONFIRMABLE_STATUSES.contains(payment.getPaymentStatus()));
        return vo;
    }

    private SellerInvoiceRespVO toInvoice(InvoiceOrderDO invoice, PayeeInfoDO payee) {
        SellerInvoiceRespVO vo = new SellerInvoiceRespVO();
        vo.setInvoiceOrderId(invoice.getId());
        vo.setOrderNo(invoice.getOrderNo());
        vo.setAcquisitionNo(invoice.getPartnerOrderId());
        vo.setAcquirerName(payee == null ? null : enterpriseName(payee.getTenantId()));
        vo.setInvoiceNo(invoice.getInvoiceNo());
        vo.setInvoiceDate(invoice.getInvoiceDate());
        vo.setCreateTime(invoice.getCreateTime());
        vo.setInvoiceAmount(invoice.getInvoiceAmount());
        vo.setTaxAmount(invoice.getTaxAmount());
        vo.setInvoiceStatus(invoice.getInvoiceStatus());
        vo.setInvoiceStatusName(InvoiceIssueStatusEnum.nameOf(invoice.getInvoiceStatus()));
        vo.setTaxStatus(invoice.getTaxStatus());
        vo.setTaxStatusName(TaxStatusEnum.nameOf(invoice.getTaxStatus()));
        vo.setUploadStatus(invoice.getUploadStatus());
        vo.setUploadStatusName(UploadStatusEnum.nameOf(invoice.getUploadStatus()));
        vo.setPdfAvailable(invoice.getInvoiceNo() != null
                && InvoiceIssueStatusEnum.ISSUED.getStatus().equals(invoice.getInvoiceStatus()));
        return vo;
    }

    private SellerAuthorizationRespVO toAuthorization(PayeeInfoDO payee, IcbcSellerAuthorizationDO authorization) {
        SellerAuthorizationRespVO vo = new SellerAuthorizationRespVO();
        vo.setTenantId(payee.getTenantId());
        vo.setEnterpriseName(enterpriseName(payee.getTenantId()));
        vo.setPayeeId(payee.getId());
        vo.setReverseInvoiceAuthorized(authorization != null
                && Boolean.TRUE.equals(authorization.getReverseInvoiceAuthorized()));
        vo.setTaxAgencyAuthorized(authorization != null
                && Boolean.TRUE.equals(authorization.getTaxAgencyAuthorized()));
        vo.setRevoked(authorization != null && authorization.getRevokedAt() != null);
        vo.setAuthorizedAt(authorization == null ? null : authorization.getAuthorizedAt());
        vo.setRevokedAt(authorization == null ? null : authorization.getRevokedAt());
        vo.setRevokeReason(authorization == null ? null : authorization.getRevokeReason());
        return vo;
    }

    private String acquisitionStatusName(Integer status) {
        return AcquisitionStatusEnum.ofStatus(status).map(AcquisitionStatusEnum::getName).orElse("未知");
    }

    private Integer invoiceYear(SellerInvoiceRespVO invoice) {
        LocalDateTime time = invoice.getInvoiceDate() != null ? invoice.getInvoiceDate() : invoice.getCreateTime();
        return time == null ? null : time.getYear();
    }

    private String paymentStatusName(Integer status) {
        if (status == null) {
            return "待付款";
        }
        if (BANK_ACCEPTED_STATUSES.contains(status)) {
            return "银行已受理";
        }
        if (PaymentStatusEnum.PENDING.getStatus().equals(status)) {
            return "待付款";
        }
        if (PaymentStatusEnum.PAYING.getStatus().equals(status)
                || PaymentStatusEnum.OTHER_BANK_DEBITED.getStatus().equals(status)
                || PaymentStatusEnum.PAID_PENDING_RECEIPT.getStatus().equals(status)) {
            return "处理中";
        }
        if (PaymentStatusEnum.PARTIAL_SUCCESS.getStatus().equals(status)) {
            return "部分成功（金额不一致，需核对）";
        }
        if (PaymentStatusEnum.FAILED.getStatus().equals(status)) {
            return "付款失败";
        }
        if (PaymentStatusEnum.CLOSED.getStatus().equals(status)) {
            return "订单已关闭";
        }
        if (PaymentStatusEnum.REVERSED.getStatus().equals(status)) {
            return "已冲正";
        }
        if (PaymentStatusEnum.REFUNDED.getStatus().equals(status)) {
            return "已退汇";
        }
        return PaymentStatusEnum.nameOf(status);
    }

    private String paymentNextStep(Integer status) {
        if (status == null) {
            return null;
        }
        if (PaymentStatusEnum.FAILED.getStatus().equals(status)
                || PaymentStatusEnum.CLOSED.getStatus().equals(status)
                || PaymentStatusEnum.REVERSED.getStatus().equals(status)
                || PaymentStatusEnum.REFUNDED.getStatus().equals(status)
                || PaymentStatusEnum.PARTIAL_SUCCESS.getStatus().equals(status)
                || PaymentStatusEnum.OTHER_BANK_DEBITED.getStatus().equals(status)) {
            return "请联系客服核实，或让回收企业重新发起付款";
        }
        return null;
    }

    private BigDecimal nullSafe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String num(BigDecimal value) {
        return value == null ? "" : value.stripTrailingZeros().toPlainString();
    }

    // ==================== HTML 小工具 ====================

    private String pageStart(String title) {
        return "<!DOCTYPE html><html lang=\"zh-CN\"><head><meta charset=\"utf-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">"
                + "<title>" + esc(title) + "</title><style>"
                + "body{font-family:-apple-system,BlinkMacSystemFont,'PingFang SC','Microsoft YaHei',sans-serif;"
                + "margin:0;padding:24px;color:#1f2329}"
                + "h1{font-size:20px;margin:0 0 8px}"
                + ".hint{color:#8a919f;font-size:13px;margin:0 0 16px}"
                + "table{width:100%;border-collapse:collapse;margin-bottom:16px}"
                + "td,th{border:1px solid #e5e6eb;padding:8px 10px;font-size:14px;text-align:left}"
                + "th{background:#f7f8fa}"
                + "@media print{body{padding:0}}"
                + "</style></head><body>";
    }

    private String pageEnd() {
        return "</body></html>";
    }

    private void kv(StringBuilder html, String key, String value) {
        html.append("<tr><th style=\"width:32%\">").append(esc(key)).append("</th><td>")
                .append(esc(value == null ? "" : value)).append("</td></tr>");
    }

    private String esc(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private void writeHtml(HttpServletResponse response, String html) {
        response.setContentType("text/html;charset=UTF-8");
        try {
            response.getWriter().write(html);
        } catch (IOException e) {
            throw exception(ICBC_API_CALL_FAILED);
        }
    }

}
