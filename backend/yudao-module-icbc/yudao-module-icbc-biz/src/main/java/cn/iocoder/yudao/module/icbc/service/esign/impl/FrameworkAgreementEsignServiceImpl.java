package cn.iocoder.yudao.module.icbc.service.esign.impl;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.module.icbc.dal.dataobject.agreement.IcbcFrameworkAgreementDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.esign.IcbcEsignConfigDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payee.PayeeInfoDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.agreement.IcbcFrameworkAgreementMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.payee.PayeeInfoMapper;
import cn.iocoder.yudao.module.icbc.enums.FrameworkAgreementSignMethodEnum;
import cn.iocoder.yudao.module.icbc.enums.FrameworkAgreementStatusEnum;
import cn.iocoder.yudao.module.icbc.service.esign.EsignConfigService;
import cn.iocoder.yudao.module.icbc.service.esign.EsignPort;
import cn.iocoder.yudao.module.icbc.service.esign.EsignTenantService;
import cn.iocoder.yudao.module.icbc.service.esign.FrameworkAgreementEsignService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.*;

/**
 * 框架收购协议合同组电子签署实现（#95，ADR 0036）。
 *
 * <p>三个决定值得单独说明：
 * <ol>
 *   <li><b>两份文书装一个合同组</b>：{@link #initiate} 只发一次 {@link EsignPort#initiate}，
 *       请求里带两份文书；「必须整体签署」是端口契约（{@code finished} 只认整体）。</li>
 *   <li><b>协议状态机用回调自检兜幂等</b>：回调先按任务号找协议，已是「生效」就直接返回，
 *       不做第二次作废 / 盖时间 / 写文件（照 {@code CallbackNotifyServiceImpl} 的做法）。</li>
 *   <li><b>企业先盖章、自然人后签署</b>：回收企业是发起方、用租户级企业印章，**不在签署方列表里**；
 *       请求里唯一的签署方是自然人本人（ADR 0036 决策 5）。</li>
 * </ol>
 */
@Service
@Validated
@Slf4j
public class FrameworkAgreementEsignServiceImpl implements FrameworkAgreementEsignService {

    /** 文书名：框架收购协议（与证据类型 {@code FRAMEWORK_AGREEMENT} 的展示名一致） */
    static final String DOC_FRAMEWORK_AGREEMENT = "框架收购协议";
    /** 文书名：反向发票合规告知函 */
    static final String DOC_REVERSE_INVOICE_NOTICE = "反向发票合规告知函";

    @Resource
    private EsignPort esignPort;
    @Resource
    private EsignConfigService esignConfigService;
    @Resource
    private EsignTenantService esignTenantService;
    @Resource
    private IcbcFrameworkAgreementMapper frameworkAgreementMapper;
    @Resource
    private PayeeInfoMapper payeeInfoMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String initiate(Long tenantId, Long agreementId) {
        IcbcFrameworkAgreementDO agreement = agreementId == null ? null
                : frameworkAgreementMapper.selectById(agreementId);
        if (agreement == null) {
            throw exception(FRAMEWORK_AGREEMENT_NOT_EXISTS);
        }
        if (!FrameworkAgreementStatusEnum.PENDING.getStatus().equals(agreement.getStatus())) {
            throw exception(ESIGN_AGREEMENT_NOT_PENDING, statusLabel(agreement.getStatus()));
        }
        PayeeInfoDO payee = payeeInfoMapper.selectById(agreement.getPayeeId());
        if (payee == null) {
            throw exception(PAYEE_NOT_EXISTS);
        }

        IcbcEsignConfigDO platform = esignConfigService.getRawConfig();
        EsignPort.EsignTask task = esignPort.initiate(tenantId, EsignPort.EsignRequest.builder()
                .payeeId(payee.getId())
                .groupName(buildGroupName(agreement))
                .documents(buildDocuments(agreement, payee, platform))
                .signer(EsignPort.EsignSigner.builder()
                        .name(payee.getName())
                        .mobile(payee.getMobile())
                        .idCardNo(payee.getIdCardNo())
                        .build())
                .build());
        if (task == null || StrUtil.isBlank(task.getSignTaskId())) {
            // 拿不到任务号 = 这份协议永远生成不了签署链接：宁可让本次事务回滚，也不留半成品
            throw exception(ESIGN_INITIATE_FAILED);
        }
        IcbcFrameworkAgreementDO update = new IcbcFrameworkAgreementDO();
        update.setId(agreement.getId());
        update.setSignTaskId(task.getSignTaskId());
        // 不回写 signMethod：调用方（saveFrameworkAgreement / 向导）落库时就写了 ELECTRONIC，
        // 且本方法只接受 status=0 的待签署协议（上面已校验），这里重复写没有信息量（#95 评审 S-4）。
        frameworkAgreementMapper.updateById(update);
        // 发起成功才消耗一份合同额度
        esignTenantService.consumeContract();
        return task.getSignTaskId();
    }

    @Override
    public String createSignUrl(Long payeeId) {
        IcbcFrameworkAgreementDO pending = frameworkAgreementMapper.selectPendingByPayeeId(payeeId);
        if (pending == null) {
            throw exception(ESIGN_AGREEMENT_NOT_PENDING, latestStatusLabel(payeeId));
        }
        if (StrUtil.isBlank(pending.getSignTaskId())) {
            throw exception(ESIGN_SIGN_TASK_ID_MISSING);
        }
        PayeeInfoDO payee = payeeInfoMapper.selectById(payeeId);
        if (payee == null) {
            throw exception(PAYEE_NOT_EXISTS);
        }
        // 现生成现用：每次点「去签署」都重新向第三方要一枚新链接，不落库、不复用
        String signUrl = esignPort.createSignUrl(TenantContextHolder.getRequiredTenantId(),
                pending.getSignTaskId(), EsignPort.EsignSigner.builder()
                        .name(payee.getName())
                        .mobile(payee.getMobile())
                        .idCardNo(payee.getIdCardNo())
                        .build());
        if (StrUtil.isBlank(signUrl)) {
            throw exception(ESIGN_SIGN_URL_UNAVAILABLE);
        }
        return signUrl;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyFinishedCallback(EsignPort.EsignCallback callback) {
        if (callback == null || !callback.isFinished()) {
            // 未签完（放弃 / 过期 / 撤销）不改协议状态：协议停在「待签署」，本人可以重开链接
            log.info("[applyFinishedCallback][合同组未整体签完，协议状态不变] signTaskId={} reason={}",
                    callback == null ? null : callback.getSignTaskId(),
                    callback == null ? null : callback.getUnfinishedReason());
            return;
        }
        if (callback.getTenantId() == null || StrUtil.isBlank(callback.getSignTaskId())) {
            log.warn("[applyFinishedCallback][通知缺少租户或任务号，忽略] callback={}", callback);
            return;
        }
        TenantUtils.execute(callback.getTenantId(), () -> applyFinished(callback));
    }

    private void applyFinished(EsignPort.EsignCallback callback) {
        IcbcFrameworkAgreementDO agreement = frameworkAgreementMapper.selectBySignTaskId(callback.getSignTaskId());
        if (agreement == null) {
            throw exception(ESIGN_AGREEMENT_NOT_FOUND, callback.getSignTaskId());
        }
        if (FrameworkAgreementStatusEnum.EFFECTIVE.getStatus().equals(agreement.getStatus())) {
            // 幂等：已生效不再作废 / 不再盖时间；但若上次回调早于文件可查、fileUrl 为空，
            // 这里补一次取址（只补空，不改状态 / 不改时间），否则重放永远不会再取，证据链永久缺 URL（#95 评审 SP-5）
            fillMissingDocumentUrls(agreement, callback.getTenantId(), callback.getSignTaskId());
            log.info("[applyFinished][协议已生效，重复回调只补缺失的文件地址] agreementId={} signTaskId={}",
                    agreement.getId(), callback.getSignTaskId());
            return;
        }
        if (!FrameworkAgreementStatusEnum.PENDING.getStatus().equals(agreement.getStatus())) {
            // 已作废的协议不被回调复活
            log.warn("[applyFinished][协议不是待签署，忽略回调] agreementId={} status={}",
                    agreement.getId(), agreement.getStatus());
            return;
        }

        List<EsignPort.SignedDocument> documents =
                esignPort.listSignedDocuments(callback.getTenantId(), callback.getSignTaskId());
        String fileUrl = pickDocumentUrl(documents, DOC_FRAMEWORK_AGREEMENT, true);
        String noticeFileUrl = pickDocumentUrl(documents, DOC_REVERSE_INVOICE_NOTICE, false);
        LocalDateTime signedAt = callback.getSignedAt() != null ? callback.getSignedAt() : LocalDateTime.now();

        // 先取「当前生效中的旧协议」：此刻本协议还是待签署，这个查询拿到的才是旧的那一份。
        // 必须赶在下面把它推进生效之前取，否则查到的会是自己。
        IcbcFrameworkAgreementDO previousActive = frameworkAgreementMapper.selectActiveByPayeeId(agreement.getPayeeId());

        // 条件更新兜并发：同一通知并发两次时只有一个线程改得动，另一个受影响 0 行、按重放处理（SP-5）
        int affected = frameworkAgreementMapper.promoteToEffectiveIfPending(
                agreement.getId(), signedAt, fileUrl, noticeFileUrl);
        if (affected == 0) {
            log.info("[applyFinished][协议已被并发回调推进，跳过后续副作用] agreementId={} signTaskId={}",
                    agreement.getId(), callback.getSignTaskId());
            return;
        }

        // 新协议生效时旧生效协议作废并留痕（ADR 0036 决策 18）。只在真正推进成功的那一次做，
        // 避免并发重放把同一份旧协议重复作废。
        if (previousActive != null && !previousActive.getId().equals(agreement.getId())) {
            IcbcFrameworkAgreementDO voided = new IcbcFrameworkAgreementDO();
            voided.setId(previousActive.getId());
            voided.setStatus(FrameworkAgreementStatusEnum.VOIDED.getStatus());
            frameworkAgreementMapper.updateById(voided);
        }
    }

    /**
     * 协议已生效但文件地址缺失时补取：回调可能早于第三方文件可查（SP-5）。
     *
     * <p>只在**有缺**时才查询第三方；两份都齐了就不再触网，保证重复回调没有第二次外部调用。
     */
    private void fillMissingDocumentUrls(IcbcFrameworkAgreementDO agreement, Long tenantId, String signTaskId) {
        boolean fileMissing = StrUtil.isBlank(agreement.getFileUrl());
        boolean noticeMissing = StrUtil.isBlank(agreement.getNoticeFileUrl());
        if (!fileMissing && !noticeMissing) {
            return;
        }
        List<EsignPort.SignedDocument> documents = esignPort.listSignedDocuments(tenantId, signTaskId);
        if (fileMissing) {
            String fileUrl = pickDocumentUrl(documents, DOC_FRAMEWORK_AGREEMENT, true);
            if (StrUtil.isNotBlank(fileUrl)) {
                frameworkAgreementMapper.fillFileUrlIfBlank(agreement.getId(), fileUrl);
            }
        }
        if (noticeMissing) {
            String noticeFileUrl = pickDocumentUrl(documents, DOC_REVERSE_INVOICE_NOTICE, false);
            if (StrUtil.isNotBlank(noticeFileUrl)) {
                frameworkAgreementMapper.fillNoticeFileUrlIfBlank(agreement.getId(), noticeFileUrl);
            }
        }
    }

    @Override
    public List<EsignPort.SignedDocument> listSignedDocuments(Long payeeId) {
        IcbcFrameworkAgreementDO agreement = frameworkAgreementMapper.selectActiveByPayeeId(payeeId);
        if (agreement == null
                || !FrameworkAgreementSignMethodEnum.ELECTRONIC.getCode().equals(agreement.getSignMethod())
                || StrUtil.isBlank(agreement.getSignTaskId())) {
            return Collections.emptyList();
        }
        List<EsignPort.SignedDocument> documents = esignPort.listSignedDocuments(
                TenantContextHolder.getRequiredTenantId(), agreement.getSignTaskId());
        return documents != null ? documents : Collections.emptyList();
    }

    // ==================== 内部方法 ====================

    /**
     * 一个合同组，两份独立文书：框架收购协议 + 反向发票合规告知函（ADR 0036 决策 3）。
     *
     * <p>模板由平台维护（{@code icbc_esign_config}），这里只填变量；租户没有编辑权。
     */
    private List<EsignPort.EsignDocument> buildDocuments(IcbcFrameworkAgreementDO agreement, PayeeInfoDO payee,
                                                         IcbcEsignConfigDO platform) {
        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("productName", agreement.getProductName());
        variables.put("quantity", agreement.getQuantity());
        variables.put("specification", agreement.getSpecification());
        variables.put("recyclePeriod", agreement.getRecyclePeriod());
        variables.put("settlementMethod", agreement.getSettlementMethod());
        variables.put("sellerName", payee.getName());
        variables.put("sellerIdCardNo", payee.getIdCardNo());
        variables.put("signDate", LocalDate.now().toString());
        List<EsignPort.EsignDocument> documents = new ArrayList<>(2);
        documents.add(EsignPort.EsignDocument.builder()
                .name(DOC_FRAMEWORK_AGREEMENT)
                .templateId(platform != null ? platform.getAgreementTemplateId() : null)
                .variables(variables)
                .build());
        documents.add(EsignPort.EsignDocument.builder()
                .name(DOC_REVERSE_INVOICE_NOTICE)
                .templateId(platform != null ? platform.getNoticeTemplateId() : null)
                .variables(variables)
                .build());
        return documents;
    }

    private String buildGroupName(IcbcFrameworkAgreementDO agreement) {
        return DOC_FRAMEWORK_AGREEMENT + "与" + DOC_REVERSE_INVOICE_NOTICE + "-" + agreement.getAgreementNo();
    }

    /**
     * 从已签文书里取某一份的地址。
     *
     * <p><b>主文书选定口径</b>：{@code file_url} 只放**框架收购协议**（合同组里的主文书），
     * 它是后台协议列表下载、以及一票一档「框架收购协议」条目的地址；告知函单独落
     * {@code notice_file_url}，在证据链上另成一条。
     *
     * <p>第三方没按名回时：主文书退而取第一份非空地址（宁可挂一份也比整条缺地址强）；
     * 告知函**不兜底**——兜底会把主文书再挂一遍，制造两条指向同一文件的假证据。
     *
     * @param allowFirstFallback 找不到指定名字时是否退用第一份地址
     */
    private String pickDocumentUrl(List<EsignPort.SignedDocument> documents, String documentName,
                                   boolean allowFirstFallback) {
        if (documents == null || documents.isEmpty()) {
            return null;
        }
        String named = documents.stream()
                .filter(doc -> documentName.equals(doc.getName()))
                .map(EsignPort.SignedDocument::getFileUrl)
                .filter(StrUtil::isNotBlank)
                .findFirst()
                .orElse(null);
        if (named != null || !allowFirstFallback) {
            return named;
        }
        return documents.stream()
                .map(EsignPort.SignedDocument::getFileUrl)
                .filter(StrUtil::isNotBlank)
                .findFirst()
                .orElse(null);
    }

    private String latestStatusLabel(Long payeeId) {
        List<IcbcFrameworkAgreementDO> agreements = frameworkAgreementMapper.selectListByPayeeId(payeeId);
        return agreements.isEmpty() ? "无协议" : statusLabel(agreements.get(0).getStatus());
    }

    private String statusLabel(Integer status) {
        FrameworkAgreementStatusEnum value = FrameworkAgreementStatusEnum.of(status);
        return value != null ? value.getName() : String.valueOf(status);
    }

}
