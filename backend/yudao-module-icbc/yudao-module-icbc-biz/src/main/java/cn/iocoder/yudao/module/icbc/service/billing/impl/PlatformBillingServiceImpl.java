package cn.iocoder.yudao.module.icbc.service.billing.impl;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.module.icbc.controller.admin.billing.vo.IcbcBillingLedgerPageReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.billing.IcbcBillingLedgerDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.invoice.InvoiceOrderDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.invoice.RedInvoiceDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.billing.IcbcBillingLedgerMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.invoice.InvoiceOrderMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.invoice.RedInvoiceMapper;
import cn.iocoder.yudao.module.icbc.enums.RedOffsetStatusEnum;
import cn.iocoder.yudao.module.icbc.service.billing.PlatformBillingService;
import cn.iocoder.yudao.module.icbc.util.IcbcMonthRange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.BILLING_PERIOD_MONTH_INVALID;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.BILLING_TENANT_REQUIRED;

/**
 * 平台计费计量 Service 实现（#16）。
 *
 * <p>计量口径只有这一处：蓝票「成功开具」按 {@code invoice_status=已开票 且 有发票号}、
 * 业务类型为报废产品收购（{@code SCRAP}）、开票日期落在期间内；被成功红冲（红票上传成功）
 * 的蓝票从计费张数里扣掉，红票本身永不单独计入——这正是「对账口径一致、红冲后不重复计入」。
 *
 * <p>台账是平台自己的账：{@code icbc_billing_ledger} 为全局表，租户侧没有任何读写入口。
 */
@Service
public class PlatformBillingServiceImpl implements PlatformBillingService {

    @Resource
    private InvoiceOrderMapper invoiceOrderMapper;
    @Resource
    private RedInvoiceMapper redInvoiceMapper;
    @Resource
    private IcbcBillingLedgerMapper billingLedgerMapper;

    /**
     * 每张成功开具的报废产品收购发票的计费单价（元）。一期价格未定，默认 0，由运营按合同注入。
     */
    @Value("${icbc.billing.unit-price:0.00}")
    private BigDecimal unitPrice;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public IcbcBillingLedgerDO generate(Long tenantId, String periodMonth) {
        if (tenantId == null) {
            throw exception(BILLING_TENANT_REQUIRED);
        }
        IcbcMonthRange range = parsePeriod(periodMonth);
        return TenantUtils.executeIgnore(() -> upsert(tenantId, range));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<IcbcBillingLedgerDO> generate(String periodMonth) {
        IcbcMonthRange range = parsePeriod(periodMonth);
        return TenantUtils.executeIgnore(() -> {
            // 只有本期真正开出过票的租户才落台账；没开票的租户计费为 0，不凭空造记录
            Set<Long> tenantIds = invoiceOrderMapper
                    .selectIssuedScrapInPeriod(null, range.getStart(), range.getEnd()).stream()
                    .map(InvoiceOrderDO::getTenantId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            List<IcbcBillingLedgerDO> ledgers = new ArrayList<>();
            for (Long tenantId : tenantIds) {
                ledgers.add(upsert(tenantId, range));
            }
            return ledgers;
        });
    }

    @Override
    public PageResult<IcbcBillingLedgerDO> getPage(IcbcBillingLedgerPageReqVO reqVO) {
        return TenantUtils.executeIgnore(() -> billingLedgerMapper.selectPage(reqVO));
    }

    private IcbcBillingLedgerDO upsert(Long tenantId, IcbcMonthRange range) {
        List<InvoiceOrderDO> issued = invoiceOrderMapper.selectIssuedScrapInPeriod(
                tenantId, range.getStart(), range.getEnd());
        int reversedCount = countReversed(issued);
        int billableCount = issued.size() - reversedCount;
        BigDecimal amount = unitPrice.multiply(BigDecimal.valueOf(billableCount))
                .setScale(2, RoundingMode.HALF_UP);

        IcbcBillingLedgerDO existing =
                billingLedgerMapper.selectByTenantAndPeriod(tenantId, range.getPeriodMonth());
        IcbcBillingLedgerDO ledger = existing != null ? existing : new IcbcBillingLedgerDO();
        ledger.setTenantId(tenantId);
        ledger.setPeriodMonth(range.getPeriodMonth());
        ledger.setIssuedCount(issued.size());
        ledger.setReversedCount(reversedCount);
        ledger.setBillableCount(billableCount);
        ledger.setUnitPrice(unitPrice);
        ledger.setAmount(amount);
        ledger.setGeneratedTime(LocalDateTime.now());
        if (existing != null) {
            billingLedgerMapper.updateById(ledger);
        } else {
            billingLedgerMapper.insert(ledger);
        }
        return ledger;
    }

    /**
     * 本期蓝票里有多少张已被成功红冲。判断只认「红票上传成功」这一条终态。
     */
    private int countReversed(List<InvoiceOrderDO> issued) {
        List<String> partnerOrderIds = issued.stream()
                .map(InvoiceOrderDO::getPartnerOrderId)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .collect(Collectors.toList());
        if (partnerOrderIds.isEmpty()) {
            return 0;
        }
        Set<String> reversedOrderIds = redInvoiceMapper
                .selectList(RedInvoiceDO::getPartnerOrderId, partnerOrderIds).stream()
                .filter(red -> RedOffsetStatusEnum.SUCCESS.getStatus().equals(red.getRedOffsetStatus()))
                .map(RedInvoiceDO::getPartnerOrderId)
                .collect(Collectors.toSet());
        return (int) issued.stream()
                .filter(order -> reversedOrderIds.contains(order.getPartnerOrderId()))
                .count();
    }

    private IcbcMonthRange parsePeriod(String periodMonth) {
        try {
            return IcbcMonthRange.of(periodMonth);
        } catch (DateTimeParseException | NullPointerException e) {
            throw exception(BILLING_PERIOD_MONTH_INVALID, String.valueOf(periodMonth));
        }
    }

}
