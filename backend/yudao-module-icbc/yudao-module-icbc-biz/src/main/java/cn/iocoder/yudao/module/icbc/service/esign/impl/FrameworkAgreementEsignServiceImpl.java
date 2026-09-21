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
        update.setSignMethod(FrameworkAgreementSignMethodEnum.ELECTRONIC.getCode());
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
            // 幂等：同一条通知重放不再作废 / 不再盖时间 / 不再查文件
            log.info("[applyFinished][协议已生效，重复回调跳过] agreementId={} signTaskId={}",
                    agreement.getId(), callback.getSignTaskId());
            return;
        }
        if (!FrameworkAgreementStatusEnum.PENDING.getStatus().equals(agreement.getStatus())) {
            // 已作废的协议不被回调复活
            log.warn("[applyFinished][协议不是待签署，忽略回调] agreementId={} status={}",
                    agreement.getId(), agreement.getStatus());
            return;
        }

        // 新协议生效时旧生效协议作废并留痕（ADR 0036 决策 18）
        IcbcFrameworkAgreementDO current = frameworkAgreementMapper.selectActiveByPayeeId(agreement.getPayeeId());
        if (current != null && !current.getId().equals(agreement.getId())) {
            IcbcFrameworkAgreementDO voided = new IcbcFrameworkAgreementDO();
            voided.setId(current.getId());
            voided.setStatus(FrameworkAgreementStatusEnum.VOIDED.getStatus());
            frameworkAgreementMapper.updateById(voided);
        }

        List<EsignPort.SignedDocument> documents =
                esignPort.listSignedDocuments(callback.getTenantId(), callback.getSignTaskId());
        IcbcFrameworkAgreementDO update = new IcbcFrameworkAgreementDO();
        update.setId(agreement.getId());
        update.setStatus(FrameworkAgreementStatusEnum.EFFECTIVE.getStatus());
        update.setSignedAt(callback.getSignedAt() != null ? callback.getSignedAt() : LocalDateTime.now());
        update.setFileUrl(pickPrimaryDocumentUrl(documents));
        frameworkAgreementMapper.updateById(update);
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
     * 协议文件地址取框架收购协议那一份（主文书）；第三方没按名回时退而取第一份。
     */
    private String pickPrimaryDocumentUrl(List<EsignPort.SignedDocument> documents) {
        if (documents == null || documents.isEmpty()) {
            return null;
        }
        return documents.stream()
                .filter(doc -> DOC_FRAMEWORK_AGREEMENT.equals(doc.getName()))
                .map(EsignPort.SignedDocument::getFileUrl)
                .filter(StrUtil::isNotBlank)
                .findFirst()
                .orElseGet(() -> documents.stream()
                        .map(EsignPort.SignedDocument::getFileUrl)
                        .filter(StrUtil::isNotBlank)
                        .findFirst()
                        .orElse(null));
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
