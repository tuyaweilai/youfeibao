package cn.iocoder.yudao.module.icbc.service.platform.impl;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.module.icbc.controller.admin.evidence.vo.EvidenceCompletenessItemRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.evidence.vo.EvidenceCompletenessSummaryRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.evidence.vo.EvidenceScopeReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.platform.vo.PlatformExceptionInvoiceRespVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.invoice.InvoiceOrderDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.invoice.RedInvoiceDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.invoice.InvoiceOrderMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.invoice.RedInvoiceMapper;
import cn.iocoder.yudao.module.icbc.enums.InvoiceIssueStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.PaymentStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.RedOffsetStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.TaxStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.UploadStatusEnum;
import cn.iocoder.yudao.module.icbc.service.evidence.InvoiceEvidenceService;
import cn.iocoder.yudao.module.icbc.service.platform.PlatformEvidenceService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 平台运营：全平台证据与异常票 Service 实现。
 *
 * <p>跨租户读取统一用一个显式的 {@link TenantUtils#executeIgnore} 表达，
 * 只在这两个方法的范围内关掉租户过滤，读完全平台就立刻恢复。
 */
@Service
public class PlatformEvidenceServiceImpl implements PlatformEvidenceService {

    @Resource
    private InvoiceEvidenceService invoiceEvidenceService;
    @Resource
    private InvoiceOrderMapper invoiceOrderMapper;
    @Resource
    private RedInvoiceMapper redInvoiceMapper;

    @Override
    public EvidenceCompletenessSummaryRespVO getPlatformCompleteness() {
        return TenantUtils.executeIgnore(() ->
                invoiceEvidenceService.getCompleteness(new EvidenceScopeReqVO()));
    }

    @Override
    public List<PlatformExceptionInvoiceRespVO> getExceptionInvoiceList() {
        EvidenceCompletenessSummaryRespVO completeness = getPlatformCompleteness();
        Map<String, EvidenceCompletenessItemRespVO> completenessByOrder = completeness.getItems() == null
                ? Map.of()
                : completeness.getItems().stream()
                .collect(Collectors.toMap(EvidenceCompletenessItemRespVO::getPartnerOrderId,
                        item -> item, (left, right) -> left));

        return TenantUtils.executeIgnore(() -> {
            Map<String, RedInvoiceDO> latestRedByOrder = latestRedInvoices();
            List<PlatformExceptionInvoiceRespVO> result = new ArrayList<>();
            for (InvoiceOrderDO order : invoiceOrderMapper.selectList()) {
                List<String> reasons = buildReasons(order,
                        completenessByOrder.get(order.getPartnerOrderId()),
                        latestRedByOrder.get(order.getPartnerOrderId()));
                if (reasons.isEmpty()) {
                    continue;
                }
                result.add(toExceptionVO(order, completenessByOrder.get(order.getPartnerOrderId()), reasons));
            }
            return result;
        });
    }

    /**
     * 一条票可能同时有多个异常：状态线各自独立，不合并成一句话。
     */
    private List<String> buildReasons(InvoiceOrderDO order, EvidenceCompletenessItemRespVO completeness,
                                      RedInvoiceDO red) {
        List<String> reasons = new ArrayList<>();
        if (InvoiceIssueStatusEnum.FAILED.getStatus().equals(order.getInvoiceStatus())) {
            reasons.add(InvoiceIssueStatusEnum.nameOf(order.getInvoiceStatus()));
        }
        if (TaxStatusEnum.isException(order.getTaxStatus())) {
            reasons.add(TaxStatusEnum.nameOf(order.getTaxStatus()));
        }
        if (UploadStatusEnum.FAILED.getStatus().equals(order.getUploadStatus())) {
            reasons.add(UploadStatusEnum.nameOf(order.getUploadStatus()));
        }
        if (PaymentStatusEnum.isException(order.getPaymentStatus())) {
            reasons.add("付款异常：" + PaymentStatusEnum.nameOf(order.getPaymentStatus()));
        }
        if (isRedException(red)) {
            reasons.add("红冲异常：" + RedOffsetStatusEnum.nameOf(red.getRedOffsetStatus()));
        }
        // 票已开出才谈得上「经不起查」；未开出的票缺证据是流程尚未走到，不算异常
        if (StrUtil.isNotBlank(order.getInvoiceNo())
                && completeness != null && !isComplete(completeness)) {
            reasons.add("五流不齐（缺 " + String.join("、", completeness.getMissingFlows()) + "）");
        }
        return reasons;
    }

    private boolean isComplete(EvidenceCompletenessItemRespVO item) {
        return item.getMissingFlows() == null || item.getMissingFlows().isEmpty();
    }

    private boolean isRedException(RedInvoiceDO red) {
        if (red == null) {
            return false;
        }
        return RedOffsetStatusEnum.ofStatus(red.getRedOffsetStatus())
                .map(RedOffsetStatusEnum::isException)
                .orElse(false);
    }

    private Map<String, RedInvoiceDO> latestRedInvoices() {
        Map<String, RedInvoiceDO> latest = new LinkedHashMap<>();
        for (RedInvoiceDO red : redInvoiceMapper.selectList()) {
            RedInvoiceDO exists = latest.get(red.getPartnerOrderId());
            if (exists == null || Objects.compare(red.getId(), exists.getId(), Long::compareTo) > 0) {
                latest.put(red.getPartnerOrderId(), red);
            }
        }
        return latest;
    }

    private PlatformExceptionInvoiceRespVO toExceptionVO(InvoiceOrderDO order,
                                                         EvidenceCompletenessItemRespVO completeness,
                                                         List<String> reasons) {
        PlatformExceptionInvoiceRespVO vo = new PlatformExceptionInvoiceRespVO();
        vo.setTenantId(order.getTenantId());
        vo.setPartnerOrderId(order.getPartnerOrderId());
        vo.setOrderNo(order.getOrderNo());
        vo.setInvoiceNo(order.getInvoiceNo());
        vo.setInvoiceStatusName(InvoiceIssueStatusEnum.nameOf(order.getInvoiceStatus()));
        vo.setTaxStatusName(TaxStatusEnum.nameOf(order.getTaxStatus()));
        vo.setUploadStatusName(order.getUploadStatus() != null ? UploadStatusEnum.nameOf(order.getUploadStatus()) : null);
        vo.setPaymentStatusName(order.getPaymentStatus() != null ? PaymentStatusEnum.nameOf(order.getPaymentStatus()) : null);
        vo.setReasons(reasons);
        vo.setCreateTime(order.getCreateTime());
        if (completeness != null) {
            vo.setCompletenessRate(completeness.getCompletenessRate());
            vo.setMissingFlows(completeness.getMissingFlows());
        }
        return vo;
    }

}
