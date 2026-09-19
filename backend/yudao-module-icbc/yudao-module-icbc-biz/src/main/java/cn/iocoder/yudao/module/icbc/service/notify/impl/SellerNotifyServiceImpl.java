package cn.iocoder.yudao.module.icbc.service.notify.impl;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.icbc.controller.admin.notify.vo.*;
import cn.iocoder.yudao.module.icbc.controller.admin.publicapi.vo.PublicNoticeRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.publictoken.vo.PublicTokenCreateReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.publictoken.vo.PublicTokenRespVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.acquisition.IcbcAcquisitionDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.invoice.InvoiceOrderDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.notify.IcbcNotifySettingDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.notify.IcbcSellerNotifyDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payee.PayeeInfoDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payment.PaymentOrderDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.settlement.IcbcSettlementDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.acquisition.IcbcAcquisitionMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.invoice.InvoiceOrderMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.notify.IcbcNotifySettingMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.notify.IcbcSellerNotifyMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.payee.PayeeInfoMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.payment.PaymentOrderMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.settlement.IcbcSettlementMapper;
import cn.iocoder.yudao.module.icbc.enums.*;
import cn.iocoder.yudao.module.icbc.service.notify.SellerNotifyService;
import cn.iocoder.yudao.module.icbc.service.token.PublicTokenService;
import cn.iocoder.yudao.module.icbc.util.MaskUtils;
import cn.iocoder.yudao.module.system.api.sms.SmsSendApi;
import cn.iocoder.yudao.module.system.api.sms.dto.send.SmsSendSingleToUserReqDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.SELLER_NOTIFY_LINK_UNAVAILABLE;

/**
 * 出售者触达 Service 实现（#36，ADR 0023）。
 *
 * <p>三条短信都走 {@link #dispatch} 一个入口，把「幂等、开关、没留手机号、没配入口、通道拒绝」
 * 这几种情况都收敛成一条可解释的记录。短信是**尽力而为**：任何通道异常都不能把已经成功的
 * 业务（结算生成 / 付款收敛 / 开票收敛）拖回失败，所以这里不抛业务异常，只落一条失败记录。
 *
 * <p>短信通道用 {@code ObjectProvider} 拿：正常环境由 {@code yudao-module-system-biz} 提供
 * {@link SmsSendApi}，测试与未接入通道的环境下不因缺 Bean 启动失败。
 */
@Slf4j
@Service
@Validated
public class SellerNotifyServiceImpl implements SellerNotifyService {

    /** 三条短信的文案模板，与 {@code system_sms_template} 里的内容保持一致（只说可核验的事，ADR 0021）。 */
    private static final String TEMPLATE_SETTLEMENT_PENDING =
            "你有一批货待确认：结算单 {settlementNo}，共 {count} 笔、金额 {amount} 元。点此查看并确认：{link}";
    private static final String TEMPLATE_PAYMENT_EXCEPTION =
            "你有一笔货款付款未完成（{status}）：{acquisitionNo}，金额 {amount} 元。点此查看下一步：{link}";
    private static final String TEMPLATE_INVOICE_ISSUED =
            "你的发票已开出：发票号 {invoiceNo}，金额 {amount} 元。点此下载：{link}";

    /** 平台级短信开关（默认关闭；费用与到达率是运营成本） */
    @Value("${icbc.notify.sms-enabled:false}")
    private boolean platformSmsEnabled;
    /** 自然人端入口地址（拼一次性令牌链接）；未配置时退化用场站入口地址 */
    @Value("${icbc.notify.seller-app-url:}")
    private String sellerAppUrl;
    @Value("${icbc.station.entry-url:}")
    private String stationEntryUrl;

    @Resource
    private IcbcSellerNotifyMapper notifyMapper;
    @Resource
    private IcbcNotifySettingMapper settingMapper;
    @Resource
    private IcbcSettlementMapper settlementMapper;
    @Resource
    private InvoiceOrderMapper invoiceOrderMapper;
    @Resource
    private PaymentOrderMapper paymentOrderMapper;
    @Resource
    private IcbcAcquisitionMapper acquisitionMapper;
    @Resource
    private PayeeInfoMapper payeeInfoMapper;
    @Resource
    private PublicTokenService publicTokenService;
    @Resource
    private ObjectProvider<SmsSendApi> smsSendApiProvider;

    // ==================== 自动触达（幂等） ====================

    @Override
    public void onSettlementPending(Long settlementId) {
        IcbcSettlementDO settlement = settlementId == null ? null : settlementMapper.selectById(settlementId);
        if (settlement == null
                || !SettlementConfirmStatusEnum.PENDING.getStatus().equals(settlement.getConfirmStatus())) {
            return; // 只有「待确认」才提醒；已确认 / 有异议 / 已签字都不打扰
        }
        // 业务键带版本号：企业对异议改版后是新的一版，需要重新提醒一次；同一版只发一次
        String bizKey = settlement.getSettlementNo() + ":v" + settlement.getCurrentVersionNo();
        dispatch(SellerNotifyTypeEnum.SETTLEMENT_PENDING, bizKey, settlement.getPayeeId(),
                settlement.getNaturalPersonId(), settlement.getSellerName(), settlement.getSellerMobile(),
                () -> sellerNoticeLink(settlement.getPayeeId()),
                link -> {
                    Map<String, Object> params = new HashMap<>();
                    params.put("settlementNo", settlement.getSettlementNo());
                    params.put("count", acquisitionCount(settlement));
                    params.put("amount", settlementTotalAmount(settlement));
                    params.put("link", link);
                    return params;
                });
    }

    @Override
    public void onPaymentException(String partnerOrderId) {
        if (StrUtil.isBlank(partnerOrderId)) {
            return;
        }
        PaymentOrderDO order = paymentOrderMapper.selectByPartnerOrderId(partnerOrderId);
        if (order == null || !PaymentStatusEnum.isException(order.getPaymentStatus())) {
            return; // 只有异常态才提醒，正常在途 / 成功不打扰
        }
        InvoiceOrderDO invoice = order.getInvoiceOrderId() == null ? null
                : invoiceOrderMapper.selectById(order.getInvoiceOrderId());
        PayeeInfoDO payee = invoice == null || invoice.getPayeeId() == null
                ? null : payeeInfoMapper.selectById(invoice.getPayeeId());
        // 业务键带异常状态码：同一笔付款的同一异常只发一次；再次失败（新的状态）才再提醒
        String bizKey = partnerOrderId + ":" + order.getPaymentStatus();
        dispatch(SellerNotifyTypeEnum.PAYMENT_EXCEPTION, bizKey,
                payee == null ? null : payee.getId(), payee == null ? null : payee.getNaturalPersonId(),
                payee == null ? null : payee.getName(), payee == null ? null : payee.getMobile(),
                () -> sellerNoticeLink(payee == null ? null : payee.getId()),
                link -> {
                    Map<String, Object> params = new HashMap<>();
                    params.put("status", PaymentStatusEnum.nameOf(order.getPaymentStatus()));
                    params.put("acquisitionNo", partnerOrderId);
                    params.put("amount", order.getPaymentAmount());
                    params.put("link", link);
                    return params;
                });
    }

    @Override
    public void onInvoiceIssued(String partnerOrderId) {
        if (StrUtil.isBlank(partnerOrderId)) {
            return;
        }
        InvoiceOrderDO invoice = invoiceOrderMapper.selectByPartnerOrderId(partnerOrderId);
        if (invoice == null || !InvoiceIssueStatusEnum.isIssued(invoice.getInvoiceStatus())) {
            return;
        }
        PayeeInfoDO payee = invoice.getPayeeId() == null ? null : payeeInfoMapper.selectById(invoice.getPayeeId());
        dispatch(SellerNotifyTypeEnum.INVOICE_ISSUED, partnerOrderId,
                invoice.getPayeeId(), payee == null ? null : payee.getNaturalPersonId(),
                payee == null ? null : payee.getName(), payee == null ? null : payee.getMobile(),
                // 发票用现有的一次性发票下载令牌（ORDER 维度）：点开就能下载，不需要注册
                () -> invoiceDownloadLink(partnerOrderId),
                link -> {
                    Map<String, Object> params = new HashMap<>();
                    params.put("invoiceNo", StrUtil.blankToDefault(invoice.getInvoiceNo(), invoice.getInvoiceCode()));
                    params.put("amount", invoice.getInvoiceAmount() != null
                            ? invoice.getInvoiceAmount() : invoice.getTotalAmount());
                    params.put("link", link);
                    return params;
                });
    }

    /**
     * 幂等入口：先按业务键抢占一条记录（唯一索引兜底并发），抢不到就说明发过了，本次不发。
     */
    private void dispatch(SellerNotifyTypeEnum type, String bizKey, Long payeeId, Long naturalPersonId,
                          String sellerName, String mobile,
                          Supplier<String> linkSupplier,
                          Function<String, Map<String, Object>> paramsBuilder) {
        if (StrUtil.isBlank(bizKey) || notifyMapper.selectByBizKey(type.getCode(), bizKey) != null) {
            return;
        }
        IcbcSellerNotifyDO record = IcbcSellerNotifyDO.builder()
                .bizType(type.getCode())
                .bizKey(bizKey)
                .templateCode(type.getTemplateCode())
                .payeeId(payeeId)
                .naturalPersonId(naturalPersonId)
                .sellerName(sellerName)
                .mobile(mobile)
                .status(SellerNotifyStatusEnum.SKIPPED_DISABLED.getStatus())
                .build();
        try {
            notifyMapper.insert(record);
        } catch (DuplicateKeyException e) {
            return; // 并发下另一个线程已抢占，避免重复发送
        }
        resolveAndSend(record, type, linkSupplier, paramsBuilder);
    }

    private void resolveAndSend(IcbcSellerNotifyDO record, SellerNotifyTypeEnum type,
                                Supplier<String> linkSupplier,
                                Function<String, Map<String, Object>> paramsBuilder) {
        try {
            if (!isEffectiveEnabled()) {
                record.setStatus(SellerNotifyStatusEnum.SKIPPED_DISABLED.getStatus());
                record.setErrorMsg("短信开关关闭（平台与租户都未开启）");
            } else {
                String link = linkSupplier.get();
                if (StrUtil.isBlank(link)) {
                    record.setStatus(SellerNotifyStatusEnum.SKIPPED_NO_LINK.getStatus());
                    record.setErrorMsg("未配置自然人端入口地址（icbc.notify.seller-app-url）或没有可绑定的业务单");
                } else {
                    record.setLink(link);
                    Map<String, Object> params = paramsBuilder.apply(link);
                    record.setContent(StrUtil.maxLength(render(type, params), 500));
                    if (StrUtil.isBlank(record.getMobile())) {
                        record.setStatus(SellerNotifyStatusEnum.SKIPPED_NO_MOBILE.getStatus());
                        record.setErrorMsg("出售者未留手机号，请由收货员当场把链接转达给他");
                    } else {
                        Long logId = sendSms(record.getMobile(), type.getTemplateCode(), params);
                        record.setStatus(SellerNotifyStatusEnum.SENT.getStatus());
                        record.setSmsLogId(logId);
                        record.setSendTime(LocalDateTime.now());
                    }
                }
            }
        } catch (Exception e) {
            // 短信是尽力而为：通道 / 模板 / 链接异常只落一条失败记录，不抛回业务
            log.warn("出售者触达发送失败 - type: {}, bizKey: {}", type.getCode(), record.getBizKey(), e);
            record.setStatus(SellerNotifyStatusEnum.FAILED.getStatus());
            record.setErrorMsg(StrUtil.maxLength(StrUtil.blankToDefault(e.getMessage(), e.getClass().getSimpleName()), 480));
        }
        record.setErrorMsg(StrUtil.maxLength(record.getErrorMsg(), 480));
        notifyMapper.updateById(record);
    }

    // ==================== 收货员一键转达 ====================

    @Override
    public NotifyForwardLinkRespVO forwardSettlementLink(@Valid NotifyForwardLinkReqVO reqVO) {
        IcbcSettlementDO settlement = settlementMapper.selectById(reqVO.getSettlementId());
        if (settlement == null) {
            throw exception(ErrorCodeConstants.SETTLEMENT_NOT_EXISTS);
        }
        String base = resolveSellerAppUrl();
        if (StrUtil.isBlank(base)) {
            throw exception(SELLER_NOTIFY_LINK_UNAVAILABLE);
        }
        if (settlement.getPayeeId() == null) {
            throw exception(ErrorCodeConstants.PAYEE_NOT_EXISTS);
        }
        NoticeLink noticeLink = mintNoticeLink(settlement.getPayeeId(), base);
        Map<String, Object> params = new HashMap<>();
        params.put("settlementNo", settlement.getSettlementNo());
        params.put("count", acquisitionCount(settlement));
        params.put("amount", settlementTotalAmount(settlement));
        params.put("link", noticeLink.link);
        String content = render(SellerNotifyTypeEnum.SETTLEMENT_PENDING, params);

        NotifyForwardLinkRespVO resp = new NotifyForwardLinkRespVO();
        resp.setSettlementId(settlement.getId());
        resp.setSettlementNo(settlement.getSettlementNo());
        resp.setToken(noticeLink.token);
        resp.setLink(noticeLink.link);
        resp.setLinkConfigured(true);
        resp.setExpiresTime(noticeLink.expiresTime);
        resp.setMobileMasked(MaskUtils.maskMobile(settlement.getSellerMobile()));
        resp.setNotificationText(content);
        resp.setSmsSent(false);
        if (Boolean.TRUE.equals(reqVO.getSendSms())) {
            if (StrUtil.isBlank(settlement.getSellerMobile())) {
                resp.setMessage("出售者未留手机号，请把链接或文案当面 / 微信转达给他；链接打开无需注册。");
            } else {
                try {
                    sendSms(settlement.getSellerMobile(), SellerNotifyTypeEnum.SETTLEMENT_PENDING.getTemplateCode(),
                            params);
                    resp.setSmsSent(true);
                    resp.setMessage("已发送短信，也可复制链接当面转达。");
                } catch (Exception e) {
                    log.warn("收货员转达短信发送失败 - settlementNo: {}", settlement.getSettlementNo(), e);
                    resp.setMessage("短信发送失败（" + StrUtil.maxLength(e.getMessage(), 120) + "），请复制链接当面转达。");
                }
            }
        } else {
            resp.setMessage("请复制链接转达给出售者；打开即可查看，无需注册。");
        }
        return resp;
    }

    // ==================== 记录与设置 ====================

    @Override
    public PageResult<NotifyRespVO> getNotifyPage(NotifyPageReqVO reqVO) {
        PageResult<IcbcSellerNotifyDO> page = notifyMapper.selectPage(reqVO);
        return new PageResult<>(page.getList().stream().map(this::toResp).toList(), page.getTotal());
    }

    @Override
    public NotifySettingRespVO getSetting() {
        IcbcNotifySettingDO setting = settingMapper.selectCurrent();
        boolean tenantEnabled = setting != null && Boolean.TRUE.equals(setting.getSmsEnabled());
        NotifySettingRespVO resp = new NotifySettingRespVO();
        resp.setPlatformEnabled(platformSmsEnabled);
        resp.setTenantEnabled(tenantEnabled);
        resp.setEffectiveEnabled(platformSmsEnabled || tenantEnabled);
        resp.setSellerAppUrlConfigured(StrUtil.isNotBlank(resolveSellerAppUrl()));
        resp.setRemark(setting == null ? null : setting.getRemark());
        return resp;
    }

    @Override
    public void saveSetting(@Valid NotifySettingSaveReqVO reqVO) {
        IcbcNotifySettingDO setting = settingMapper.selectCurrent();
        if (setting == null) {
            settingMapper.insert(IcbcNotifySettingDO.builder()
                    .smsEnabled(reqVO.getSmsEnabled())
                    .remark(reqVO.getRemark())
                    .build());
            return;
        }
        setting.setSmsEnabled(reqVO.getSmsEnabled());
        setting.setRemark(reqVO.getRemark());
        settingMapper.updateById(setting);
    }

    // ==================== 免登录通知 ====================

    @Override
    public PublicNoticeRespVO getNoticeForPayee(Long payeeId) {
        PublicNoticeRespVO resp = new PublicNoticeRespVO();
        resp.setPurpose(PublicTokenPurposeEnum.SELLER_NOTICE.getCode());
        resp.setTenantId(TenantContextHolder.getTenantId());
        resp.setPayeeId(payeeId);
        PayeeInfoDO payee = payeeId == null ? null : payeeInfoMapper.selectById(payeeId);
        resp.setSellerName(payee == null ? null : payee.getName());

        List<PublicNoticeRespVO.NoticeItem> items = new ArrayList<>();
        for (IcbcSettlementDO settlement : settlementMapper.selectListByPayeeId(payeeId)) {
            PublicNoticeRespVO.NoticeItem item = toSettlementNotice(settlement);
            if (item != null) {
                items.add(item);
            }
        }
        for (PaymentOrderDO order : exceptionPayments(payeeId)) {
            items.add(toPaymentNotice(order));
        }
        resp.setItems(items);
        resp.setMessage(items.isEmpty()
                ? "暂时没有需要你处理的事。"
                : "有 " + items.size() + " 件事需要你处理。");
        resp.setScopeNote("金额为本平台累计，不含你在其他渠道的交易；"
                + "「银行已受理」是我们能核验的银行状态，到账与否由你自己确认。");
        return resp;
    }

    private PublicNoticeRespVO.NoticeItem toSettlementNotice(IcbcSettlementDO settlement) {
        SettlementConfirmStatusEnum status =
                SettlementConfirmStatusEnum.ofStatus(settlement.getConfirmStatus()).orElse(null);
        if (status == null || SettlementConfirmStatusEnum.isConfirmed(status.getStatus())) {
            return null;
        }
        PublicNoticeRespVO.NoticeItem item = new PublicNoticeRespVO.NoticeItem();
        item.setType(SellerNotifyTypeEnum.SETTLEMENT_PENDING.getCode());
        item.setTypeName(SellerNotifyTypeEnum.SETTLEMENT_PENDING.getName());
        item.setTitle("结算单 " + settlement.getSettlementNo());
        item.setStatusName(status.getName());
        item.setSettlementId(settlement.getId());
        item.setAmount(settlementTotalAmount(settlement));
        item.setAcquisitionCount(acquisitionCount(settlement));
        item.setDeadlineTime(settlement.getDeadlineTime());
        item.setNextStep(settlement.getConfirmStatus().equals(SettlementConfirmStatusEnum.PENDING.getStatus())
                ? "点开核对计量与计价，确认或提异议" : "等企业处理你的异议");
        return item;
    }

    private PublicNoticeRespVO.NoticeItem toPaymentNotice(PaymentOrderDO order) {
        PublicNoticeRespVO.NoticeItem item = new PublicNoticeRespVO.NoticeItem();
        item.setType(SellerNotifyTypeEnum.PAYMENT_EXCEPTION.getCode());
        item.setTypeName(SellerNotifyTypeEnum.PAYMENT_EXCEPTION.getName());
        item.setTitle("货款 " + order.getPartnerOrderId());
        item.setStatusName(PaymentStatusEnum.nameOf(order.getPaymentStatus()));
        item.setPartnerOrderId(order.getPartnerOrderId());
        item.setAmount(order.getPaymentAmount());
        item.setNextStep("请联系回收企业重新发起付款，或到现场核对收款账户");
        return item;
    }

    private List<PaymentOrderDO> exceptionPayments(Long payeeId) {
        List<IcbcAcquisitionDO> acquisitions = acquisitionMapper.selectListByPayeeId(payeeId);
        if (acquisitions.isEmpty()) {
            return List.of();
        }
        List<Long> acquisitionIds = acquisitions.stream().map(IcbcAcquisitionDO::getId).toList();
        return paymentOrderMapper.selectListByAcquisitionIds(acquisitionIds).stream()
                .filter(order -> PaymentStatusEnum.isException(order.getPaymentStatus()))
                .toList();
    }

    // ==================== 内部工具 ====================

    private boolean isEffectiveEnabled() {
        if (platformSmsEnabled) {
            return true;
        }
        IcbcNotifySettingDO setting = settingMapper.selectCurrent();
        return setting != null && Boolean.TRUE.equals(setting.getSmsEnabled());
    }

    private String resolveSellerAppUrl() {
        if (StrUtil.isNotBlank(sellerAppUrl)) {
            return sellerAppUrl.trim();
        }
        return StrUtil.isBlank(stationEntryUrl) ? null : stationEntryUrl.trim();
    }

    /**
     * 结算 / 付款通知用的一次性链接：绑定收方（PAYEE），打开进自然人端看「待处理的事」。
     */
    private String sellerNoticeLink(Long payeeId) {
        if (payeeId == null) {
            return null;
        }
        String base = resolveSellerAppUrl();
        if (StrUtil.isBlank(base)) {
            return null;
        }
        return mintNoticeLink(payeeId, base).link;
    }

    /**
     * 发票已开出的链接：复用现有的一次性发票下载令牌（ORDER 维度），点开就能下载 PDF，
     * 不需要注册（ADR 0023）。
     */
    private String invoiceDownloadLink(String partnerOrderId) {
        String base = resolveSellerAppUrl();
        if (StrUtil.isBlank(base) || StrUtil.isBlank(partnerOrderId)) {
            return null;
        }
        PublicTokenCreateReqVO reqVO = new PublicTokenCreateReqVO();
        reqVO.setPurpose(PublicTokenPurposeEnum.INVOICE_DOWNLOAD.getCode());
        reqVO.setPartnerOrderId(partnerOrderId);
        PublicTokenRespVO token = publicTokenService.mint(reqVO);
        return base.replaceAll("/+$", "") + "/#/?token=" + token.getToken()
                + "&purpose=" + PublicTokenPurposeEnum.INVOICE_DOWNLOAD.getCode();
    }

    /**
     * 签发一枚触达令牌并拼出链接。链接形如 {@code https://<seller-app>/#/?token=xxx&purpose=SELLER_NOTICE}，
     * 打开即可查看，**不需要注册**；要确认 / 操作时再用手机号验证。
     */
    private NoticeLink mintNoticeLink(Long payeeId, String base) {
        PublicTokenCreateReqVO reqVO = new PublicTokenCreateReqVO();
        reqVO.setPurpose(PublicTokenPurposeEnum.SELLER_NOTICE.getCode());
        reqVO.setPayeeId(payeeId);
        PublicTokenRespVO token = publicTokenService.mint(reqVO);
        NoticeLink info = new NoticeLink();
        info.token = token.getToken();
        info.expiresTime = token.getExpiresTime();
        info.link = base.replaceAll("/+$", "") + "/#/?token=" + token.getToken()
                + "&purpose=" + PublicTokenPurposeEnum.SELLER_NOTICE.getCode();
        return info;
    }

    private Long sendSms(String mobile, String templateCode, Map<String, Object> params) {
        SmsSendApi smsSendApi = smsSendApiProvider.getIfAvailable();
        if (smsSendApi == null) {
            throw new IllegalStateException("短信通道未接入（SmsSendApi 不可用）");
        }
        SmsSendSingleToUserReqDTO reqDTO = new SmsSendSingleToUserReqDTO();
        reqDTO.setMobile(mobile);
        reqDTO.setTemplateCode(templateCode);
        reqDTO.setTemplateParams(params);
        return smsSendApi.sendSingleSmsToMember(reqDTO);
    }

    private String render(SellerNotifyTypeEnum type, Map<String, Object> params) {
        return StrUtil.format(templateOf(type), params);
    }

    private String templateOf(SellerNotifyTypeEnum type) {
        switch (type) {
            case PAYMENT_EXCEPTION:
                return TEMPLATE_PAYMENT_EXCEPTION;
            case INVOICE_ISSUED:
                return TEMPLATE_INVOICE_ISSUED;
            case SETTLEMENT_PENDING:
            default:
                return TEMPLATE_SETTLEMENT_PENDING;
        }
    }

    private Integer acquisitionCount(IcbcSettlementDO settlement) {
        return acquisitionMapper.selectListBySettlementId(settlement.getId()).size();
    }

    /**
     * 合计金额取当前版本快照（快照就是那一版的事实），快照缺失时退回逐条收购单相加。
     */
    private BigDecimal settlementTotalAmount(IcbcSettlementDO settlement) {
        return acquisitionMapper.selectListBySettlementId(settlement.getId()).stream()
                .map(IcbcAcquisitionDO::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private NotifyRespVO toResp(IcbcSellerNotifyDO record) {
        NotifyRespVO resp = new NotifyRespVO();
        resp.setId(record.getId());
        resp.setBizType(record.getBizType());
        resp.setBizTypeName(SellerNotifyTypeEnum.nameOf(record.getBizType()));
        resp.setBizKey(record.getBizKey());
        resp.setTemplateCode(record.getTemplateCode());
        resp.setPayeeId(record.getPayeeId());
        resp.setSellerName(record.getSellerName());
        resp.setMobileMasked(MaskUtils.maskMobile(record.getMobile()));
        resp.setStatus(record.getStatus());
        resp.setStatusName(SellerNotifyStatusEnum.nameOf(record.getStatus()));
        resp.setContent(record.getContent());
        resp.setLink(record.getLink());
        resp.setErrorMsg(record.getErrorMsg());
        resp.setSendTime(record.getSendTime());
        resp.setCreateTime(record.getCreateTime());
        return resp;
    }

    /** 一次性令牌链接的中间结果 */
    private static class NoticeLink {
        private String token;
        private String link;
        private LocalDateTime expiresTime;
    }

}
