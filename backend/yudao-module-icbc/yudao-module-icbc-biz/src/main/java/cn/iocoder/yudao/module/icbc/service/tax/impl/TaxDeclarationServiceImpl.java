package cn.iocoder.yudao.module.icbc.service.tax.impl;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.icbc.controller.admin.tax.vo.*;
import cn.iocoder.yudao.module.icbc.dal.dataobject.invoice.InvoiceOrderDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.invoice.RedInvoiceDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payee.PayeeInfoDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.tax.TaxDeclarationDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.tax.TaxDeclarationInvoiceDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.tax.TaxDeclarationItemDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.invoice.InvoiceOrderMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.invoice.RedInvoiceMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.payee.PayeeInfoMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.tax.TaxDeclarationInvoiceMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.tax.TaxDeclarationItemMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.tax.TaxDeclarationMapper;
import cn.iocoder.yudao.module.icbc.enums.*;
import cn.iocoder.yudao.module.icbc.service.quota.NaturalPersonQuotaService;
import cn.iocoder.yudao.module.icbc.service.tax.TaxDeclarationService;
import cn.iocoder.yudao.module.icbc.service.tax.TaxSupplementService;
import cn.iocoder.yudao.module.icbc.util.MaskUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.*;

/**
 * 代办税费申报 Service 实现（issue #13）。
 *
 * <p>清单是<b>派生视图</b>：当月已开出的蓝票按出售者归集，减去当月成功的红冲，得出每人
 * 的净销售额，按征收率分列，再算增值税（10 万元免征线以下免征）、附加税费（增值税 × 6%）、
 * 个人所得税（销售额 × 0.5%）。合计即申报单的应缴金额。
 *
 * <p>三处口径刻意保持单一来源：10 万元免征线取额度台账（跨租户）、税率常量取
 * {@link IcbcTaxConstants}、红冲只认「上传成功」。已缴款的申报单不再改写，差额转补缴。
 */
@Slf4j
@Service
@Validated
public class TaxDeclarationServiceImpl implements TaxDeclarationService {

    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
    /** 会挡住申报的缺项：出售者身份不全、征收率未识别 */
    private static final Set<String> BLOCKING_MISSING_TYPES = Set.of(
            "SELLER_INFO_MISSING", "TAX_RATE_UNKNOWN");

    @Resource
    private TaxDeclarationMapper declarationMapper;
    @Resource
    private TaxDeclarationItemMapper itemMapper;
    @Resource
    private TaxDeclarationInvoiceMapper declarationInvoiceMapper;
    @Resource
    private InvoiceOrderMapper invoiceOrderMapper;
    @Resource
    private RedInvoiceMapper redInvoiceMapper;
    @Resource
    private PayeeInfoMapper payeeInfoMapper;
    @Resource
    private NaturalPersonQuotaService naturalPersonQuotaService;
    @Resource
    private TaxSupplementService taxSupplementService;

    // ==================== 生成与查询 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TaxDeclarationRespVO generate(String periodMonth) {
        MonthRange range = MonthRange.of(periodMonth);
        TaxDeclarationDO existing = declarationMapper.selectByPeriodMonth(periodMonth);
        DeclarationBuild build = build(range);

        if (existing != null && TaxDeclarationStatusEnum.isPaid(existing.getStatus())) {
            // 已缴款：不改写已归档的申报单，把正差额转成补缴
            registerSupplementIfIncreased(existing, build);
            return toDeclarationResp(existing, itemMapper.selectByDeclarationId(existing.getId()),
                    "该申报单已缴款，金额已锁定；本次重新计算若高于已缴金额，差额已登记到待补缴");
        }

        TaxDeclarationDO declaration = existing != null ? existing : new TaxDeclarationDO();
        declaration.setDeclarationNo("TAXDECL-" + periodMonth);
        declaration.setPeriodMonth(periodMonth);
        declaration.setDeclarationDeadline(range.deadline());
        // 重新生成：金额没变则保留已报送状态与报送信息；金额变了才退回待申报，避免用对不上的报告表去缴款
        if (existing != null && TaxDeclarationStatusEnum.DECLARED.getStatus().equals(existing.getStatus())
                && sameTotals(existing, build)) {
            declaration.setStatus(TaxDeclarationStatusEnum.DECLARED.getStatus());
            declaration.setDeclaredAt(existing.getDeclaredAt());
            declaration.setDeclaredBy(existing.getDeclaredBy());
            declaration.setDeclaredRemark(existing.getDeclaredRemark());
        } else {
            declaration.setStatus(TaxDeclarationStatusEnum.PENDING.getStatus());
            declaration.setDeclaredAt(null);
            declaration.setDeclaredBy(null);
            declaration.setDeclaredRemark(null);
        }
        declaration.setSellerCount(build.items.size());
        declaration.setOverExemptSellerCount((int) build.items.stream()
                .filter(item -> Boolean.TRUE.equals(item.getOverExempt())).count());
        declaration.setTotalSalesAmount(build.totalSalesAmount);
        declaration.setAmountAtOnePercent(build.amountAtOnePercent);
        declaration.setAmountAtThreePercent(build.amountAtThreePercent);
        declaration.setOtherAmount(build.otherAmount);
        declaration.setVatAmount(build.vatAmount);
        declaration.setSurchargeAmount(build.surchargeAmount);
        declaration.setIitAmount(build.iitAmount);
        declaration.setTotalTaxAmount(build.totalTaxAmount);
        declaration.setDataReady(build.missing.isEmpty());
        declaration.setMissingDataCount(build.missing.size());
        if (existing == null) {
            declarationMapper.insert(declaration);
        } else {
            declarationMapper.updateById(declaration);
        }

        itemMapper.deleteByDeclarationId(declaration.getId());
        for (TaxDeclarationItemDO item : build.items) {
            item.setDeclarationId(declaration.getId());
            itemMapper.insert(item);
        }
        declarationInvoiceMapper.deleteByDeclarationId(declaration.getId());
        for (TaxDeclarationInvoiceDO link : build.invoices) {
            link.setDeclarationId(declaration.getId());
            declarationInvoiceMapper.insert(link);
        }
        return toDeclarationResp(declaration, build.items, buildMessage(declaration, build));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TaxDeclarationRespVO getDeclaration(String periodMonth) {
        TaxDeclarationDO declaration = declarationMapper.selectByPeriodMonth(periodMonth);
        if (declaration == null) {
            return generate(periodMonth);
        }
        return toDeclarationResp(declaration, itemMapper.selectByDeclarationId(declaration.getId()), null);
    }

    @Override
    public PageResult<TaxDeclarationRespVO> getDeclarationPage(TaxDeclarationPageReqVO reqVO) {
        PageResult<TaxDeclarationDO> page = declarationMapper.selectPage(reqVO);
        return new PageResult<>(page.getList().stream()
                .map(declaration -> toDeclarationResp(declaration, null, null)).toList(), page.getTotal());
    }

    @Override
    public PageResult<TaxDeclarationItemRespVO> getItemPage(TaxDeclarationItemPageReqVO reqVO) {
        PageResult<TaxDeclarationItemDO> page = itemMapper.selectPage(reqVO);
        return new PageResult<>(page.getList().stream().map(this::toItemResp).toList(), page.getTotal());
    }

    @Override
    public TaxDeclarationPrecheckRespVO precheck(String periodMonth) {
        MonthRange range = MonthRange.of(periodMonth);
        DeclarationBuild build = build(range);
        return toPrecheckResp(periodMonth, build);
    }

    // ==================== 预警 ====================

    @Override
    public List<TaxDeclarationWarningRespVO> getWarnings() {
        LocalDate today = LocalDate.now();
        List<TaxDeclarationWarningRespVO> warnings = new ArrayList<>();
        for (TaxDeclarationDO declaration : declarationMapper.selectUnpaid()) {
            if (declaration.getDeclarationDeadline() == null) {
                continue;
            }
            int daysLeft = (int) ChronoUnit.DAYS.between(today, declaration.getDeclarationDeadline());
            if (daysLeft < 0) {
                warnings.add(buildWarning(TaxDeclarationWarningTypeEnum.OVERDUE_SUSPENSION_RISK, declaration,
                        daysLeft, true));
            } else if (daysLeft <= IcbcTaxConstants.DEADLINE_WARNING_DAYS) {
                warnings.add(buildWarning(TaxDeclarationWarningTypeEnum.DEADLINE_APPROACHING, declaration,
                        daysLeft, false));
            }
            if (Boolean.FALSE.equals(declaration.getDataReady())) {
                warnings.add(buildWarning(TaxDeclarationWarningTypeEnum.MISSING_DATA, declaration, daysLeft, false));
            }
        }
        warnings.sort(Comparator.comparing(TaxDeclarationWarningRespVO::getDeclarationDeadline,
                Comparator.nullsLast(Comparator.naturalOrder())));
        return warnings;
    }

    // ==================== 申报与缴款 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TaxDeclarationRespVO declare(TaxDeclarationDeclareReqVO reqVO) {
        TaxDeclarationDO declaration = requireDeclaration(reqVO.getPeriodMonth());
        if (TaxDeclarationStatusEnum.isPaid(declaration.getStatus())) {
            throw exception(TAX_DECLARATION_PAID_IMMUTABLE, reqVO.getPeriodMonth());
        }
        if (TaxDeclarationStatusEnum.DECLARED.getStatus().equals(declaration.getStatus())) {
            return toDeclarationResp(declaration, itemMapper.selectByDeclarationId(declaration.getId()),
                    "该申报单已报送，无需重复报送");
        }
        TaxDeclarationPrecheckRespVO precheck = precheck(reqVO.getPeriodMonth());
        if (Boolean.FALSE.equals(precheck.getReady())) {
            throw exception(TAX_DECLARATION_NOT_READY, reqVO.getPeriodMonth(), firstMissingMessage(precheck));
        }
        TaxDeclarationDO update = new TaxDeclarationDO();
        update.setId(declaration.getId());
        update.setStatus(TaxDeclarationStatusEnum.DECLARED.getStatus());
        update.setDeclaredAt(LocalDateTime.now());
        update.setDeclaredBy(reqVO.getDeclaredBy());
        update.setDeclaredRemark(reqVO.getRemark());
        declarationMapper.updateById(update);
        TaxDeclarationDO latest = declarationMapper.selectById(declaration.getId());
        return toDeclarationResp(latest, itemMapper.selectByDeclarationId(latest.getId()),
                "已报送《代办税费报告表》《代办税费明细报告表》，请按应缴合计完成缴款");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TaxDeclarationRespVO recordPayment(TaxDeclarationPayReqVO reqVO) {
        TaxDeclarationDO declaration = requireDeclaration(reqVO.getPeriodMonth());
        if (!TaxDeclarationStatusEnum.DECLARED.getStatus().equals(declaration.getStatus())) {
            throw exception(TAX_DECLARATION_STATUS_INVALID, reqVO.getPeriodMonth(), "缴款",
                    TaxDeclarationStatusEnum.nameOf(declaration.getStatus()), "请先报送报告表");
        }
        LocalDateTime paidAt = reqVO.getPaidAt() == null ? LocalDateTime.now() : reqVO.getPaidAt();
        TaxDeclarationDO update = new TaxDeclarationDO();
        update.setId(declaration.getId());
        update.setStatus(TaxDeclarationStatusEnum.PAID.getStatus());
        update.setPaidAmount(reqVO.getPaidAmount() == null ? declaration.getTotalTaxAmount() : reqVO.getPaidAmount());
        update.setPaidAt(paidAt);
        update.setPaymentMethod(reqVO.getPaymentMethod());
        update.setVoucherNo(reqVO.getVoucherNo());
        update.setVoucherFileUrl(reqVO.getVoucherFileUrl());
        update.setRemark(reqVO.getRemark());
        declarationMapper.updateById(update);

        // 明细逐条置已缴：凭证与对应发票通过 icbc_tax_declaration_invoice 关联
        for (TaxDeclarationItemDO item : itemMapper.selectByDeclarationId(declaration.getId())) {
            TaxDeclarationItemDO itemUpdate = new TaxDeclarationItemDO();
            itemUpdate.setId(item.getId());
            itemUpdate.setStatus(TaxDeclarationStatusEnum.PAID.getStatus());
            itemUpdate.setPaidAmount(item.getTotalTaxAmount());
            itemUpdate.setPaidAt(paidAt);
            itemMapper.updateById(itemUpdate);
        }
        TaxDeclarationDO latest = declarationMapper.selectById(declaration.getId());
        return toDeclarationResp(latest, itemMapper.selectByDeclarationId(latest.getId()),
                StrUtil.format("缴款成功，凭证 {} 已归档，{} 张发票已关联到本申报单",
                        StrUtil.blankToDefault(reqVO.getVoucherNo(), "（未填凭证号）"), countInvoices(latest.getId())));
    }

    // ==================== 组装清单 ====================

    private DeclarationBuild build(MonthRange range) {
        DeclarationBuild build = new DeclarationBuild();
        // 1. 当月已开出的蓝票
        List<InvoiceOrderDO> blues = invoiceOrderMapper.selectList(new LambdaQueryWrapper<InvoiceOrderDO>()
                .eq(InvoiceOrderDO::getInvoiceStatus, InvoiceIssueStatusEnum.ISSUED.getStatus())
                .ge(InvoiceOrderDO::getInvoiceDate, range.start)
                .lt(InvoiceOrderDO::getInvoiceDate, range.end));
        Map<Long, PayeeAgg> aggs = new TreeMap<>();
        Map<String, InvoiceOrderDO> blueIndex = new HashMap<>();
        Map<Long, PayeeInfoDO> payeeCache = new HashMap<>();
        for (InvoiceOrderDO blue : blues) {
            if (blue.getPayeeId() == null) {
                continue;
            }
            if (blue.getPartnerOrderId() != null) {
                blueIndex.put(blue.getPartnerOrderId(), blue);
            }
            BigDecimal amount = amountOf(blue);
            aggs.computeIfAbsent(blue.getPayeeId(), PayeeAgg::new).addBlue(amount, blue.getTaxRate());
            build.invoices.add(toInvoiceLink(blue, blue.getInvoiceNo(), amount, "BLUE"));
        }
        // 2. 当月成功的红冲（可能冲的是上月蓝票，所以按开票订单回查收方与征收率）
        List<RedInvoiceDO> reds = redInvoiceMapper.selectList(new LambdaQueryWrapper<RedInvoiceDO>()
                .eq(RedInvoiceDO::getRedOffsetStatus, RedOffsetStatusEnum.SUCCESS.getStatus())
                .ge(RedInvoiceDO::getRedInvoiceDate, range.start)
                .lt(RedInvoiceDO::getRedInvoiceDate, range.end));
        for (RedInvoiceDO red : reds) {
            InvoiceOrderDO blue = resolveBlue(red, blueIndex);
            if (blue == null || blue.getPayeeId() == null) {
                continue;
            }
            BigDecimal amount = red.getAmount() == null ? BigDecimal.ZERO : red.getAmount();
            aggs.computeIfAbsent(blue.getPayeeId(), PayeeAgg::new).addRed(amount, blue.getTaxRate());
            build.invoices.add(toInvoiceLink(blue,
                    StrUtil.blankToDefault(red.getRedInvoiceNo(), blue.getInvoiceNo()), amount, "RED"));
        }
        // 3. 每个出售者一条明细
        for (PayeeAgg agg : aggs.values()) {
            PayeeInfoDO payee = payeeCache.computeIfAbsent(agg.payeeId, payeeInfoMapper::selectById);
            if (payee == null) {
                continue;
            }
            BigDecimal crossTenant = naturalPersonQuotaService.getCrossTenantMonthlyNetAmount(
                    agg.payeeId, range.periodMonth);
            build.addItem(agg.toItem(payee, range.periodMonth, crossTenant));
        }
        // 4. 缺什么数据
        build.missing = collectMissing(range, blues, aggs, payeeCache);
        return build;
    }

    private List<TaxMissingDataVO> collectMissing(MonthRange range, List<InvoiceOrderDO> blues,
                                                  Map<Long, PayeeAgg> aggs, Map<Long, PayeeInfoDO> payeeCache) {
        List<TaxMissingDataVO> missing = new ArrayList<>();
        // 发票未开出
        List<InvoiceOrderDO> unissued = invoiceOrderMapper.selectList(new LambdaQueryWrapper<InvoiceOrderDO>()
                .ne(InvoiceOrderDO::getInvoiceStatus, InvoiceIssueStatusEnum.ISSUED.getStatus())
                .ge(InvoiceOrderDO::getCreateTime, range.start)
                .lt(InvoiceOrderDO::getCreateTime, range.end));
        if (!unissued.isEmpty()) {
            missing.add(missing("INVOICE_NOT_ISSUED", "发票未开出",
                    StrUtil.format("本月还有 {} 张发票未开出，其销售额尚未纳入申报", unissued.size()),
                    unissued.size(), "在「开票申请」确认这些票的开票与上传状态；确实开不出的请作废并重开",
                    unissued.stream().map(order -> StrUtil.blankToDefault(order.getPartnerOrderId(),
                            order.getOrderNo())).limit(5).toList()));
        }
        // 缴税未成功
        List<InvoiceOrderDO> notPaid = blues.stream()
                .filter(order -> !TaxStatusEnum.isPaid(order.getTaxStatus())).toList();
        if (!notPaid.isEmpty()) {
            missing.add(missing("TAX_NOT_PAID", "发票缴税未成功",
                    StrUtil.format("本月 {} 张发票的缴税尚未结清，确认后本申报单的金额才与实缴一致", notPaid.size()),
                    notPaid.size(), "在「开票申请」查状态；异常态联系工行核实",
                    notPaid.stream().map(order -> StrUtil.blankToDefault(order.getInvoiceNo(),
                            order.getPartnerOrderId())).limit(5).toList()));
        }
        // 出售者身份不全
        List<String> sellerSamples = new ArrayList<>();
        int sellerMissing = 0;
        for (PayeeAgg agg : aggs.values()) {
            PayeeInfoDO payee = payeeCache.get(agg.payeeId);
            if (payee == null || StrUtil.isBlank(payee.getName()) || StrUtil.isBlank(payee.getIdCardNo())) {
                sellerMissing++;
                if (sellerSamples.size() < 5) {
                    sellerSamples.add(payee == null ? String.valueOf(agg.payeeId) : payee.getName());
                }
            }
        }
        if (sellerMissing > 0) {
            missing.add(missing("SELLER_INFO_MISSING", "出售者身份信息不全",
                    StrUtil.format("{} 个出售者缺姓名或身份证号，不能申报", sellerMissing),
                    sellerMissing, "在「出售者建档」补齐姓名、身份证与手机号",
                    sellerSamples));
        }
        // 红冲未成功
        int redPending = countPendingRedOffsets(range);
        if (redPending > 0) {
            missing.add(missing("RED_INVOICE_PENDING", "红冲未成功",
                    StrUtil.format("本月 {} 笔红冲尚未成功，冲减可能不完整", redPending),
                    redPending, "在「开票申请」查红冲状态；撤销后重新发起",
                    Collections.emptyList()));
        }
        // 征收率未识别
        BigDecimal unknown = BigDecimal.ZERO;
        int unknownSellers = 0;
        List<String> unknownSamples = new ArrayList<>();
        for (PayeeAgg agg : aggs.values()) {
            BigDecimal other = agg.netOther();
            if (other.signum() > 0) {
                unknown = unknown.add(other);
                unknownSellers++;
                if (unknownSamples.size() < 5) {
                    PayeeInfoDO payee = payeeCache.get(agg.payeeId);
                    unknownSamples.add(payee == null ? String.valueOf(agg.payeeId) : payee.getName());
                }
            }
        }
        if (unknown.signum() > 0) {
            missing.add(missing("TAX_RATE_UNKNOWN", "征收率未识别",
                    StrUtil.format("{} 元销售额未识别征收率，无法计算增值税", unknown.toPlainString()),
                    unknownSellers, "在「编码配置」补齐品类征收率；历史票请人工确认后在「编码配置」补录",
                    unknownSamples));
        }
        return missing;
    }

    // ==================== 申报单 / 明细映射 ====================

    private TaxDeclarationRespVO toDeclarationResp(TaxDeclarationDO declaration,
                                                   List<TaxDeclarationItemDO> items, String message) {
        TaxDeclarationRespVO resp = new TaxDeclarationRespVO();
        resp.setId(declaration.getId());
        resp.setDeclarationNo(declaration.getDeclarationNo());
        resp.setPeriodMonth(declaration.getPeriodMonth());
        resp.setDeclarationDeadline(declaration.getDeclarationDeadline());
        resp.setStatus(declaration.getStatus());
        resp.setStatusName(TaxDeclarationStatusEnum.nameOf(declaration.getStatus()));
        resp.setNextAction(TaxDeclarationStatusEnum.nextActionOf(declaration.getStatus()));
        resp.setDataReady(declaration.getDataReady());
        resp.setMissingDataCount(declaration.getMissingDataCount());
        resp.setSellerCount(declaration.getSellerCount());
        resp.setOverExemptSellerCount(declaration.getOverExemptSellerCount());
        resp.setTotalSalesAmount(declaration.getTotalSalesAmount());
        resp.setAmountAtOnePercent(declaration.getAmountAtOnePercent());
        resp.setAmountAtThreePercent(declaration.getAmountAtThreePercent());
        resp.setOtherAmount(declaration.getOtherAmount());
        resp.setVatAmount(declaration.getVatAmount());
        resp.setSurchargeAmount(declaration.getSurchargeAmount());
        resp.setIitAmount(declaration.getIitAmount());
        resp.setTotalTaxAmount(declaration.getTotalTaxAmount());
        resp.setPaidAmount(declaration.getPaidAmount());
        resp.setDeclaredAt(declaration.getDeclaredAt());
        resp.setDeclaredBy(declaration.getDeclaredBy());
        resp.setDeclaredRemark(declaration.getDeclaredRemark());
        resp.setPaidAt(declaration.getPaidAt());
        resp.setPaymentMethod(declaration.getPaymentMethod());
        resp.setVoucherNo(declaration.getVoucherNo());
        resp.setVoucherFileUrl(declaration.getVoucherFileUrl());
        if (declaration.getDeclarationDeadline() != null) {
            resp.setDaysLeft((int) ChronoUnit.DAYS.between(LocalDate.now(), declaration.getDeclarationDeadline()));
            resp.setOverdue(declaration.getDeclarationDeadline().isBefore(LocalDate.now())
                    && !TaxDeclarationStatusEnum.isPaid(declaration.getStatus()));
        }
        if (items != null) {
            resp.setItems(items.stream().map(this::toItemResp).toList());
            resp.setInvoiceCount(items.stream().mapToInt(item ->
                    item.getInvoiceCount() == null ? 0 : item.getInvoiceCount()).sum());
        }
        resp.setMessage(message != null ? message : buildMessage(declaration, null));
        return resp;
    }

    private TaxDeclarationItemRespVO toItemResp(TaxDeclarationItemDO item) {
        TaxDeclarationItemRespVO resp = new TaxDeclarationItemRespVO();
        resp.setId(item.getId());
        resp.setDeclarationId(item.getDeclarationId());
        resp.setPeriodMonth(item.getPeriodMonth());
        resp.setPayeeId(item.getPayeeId());
        resp.setSellerName(item.getSellerName());
        resp.setIdCardMasked(MaskUtils.maskIdCard(item.getIdCardNo()));
        resp.setInvoiceCount(item.getInvoiceCount());
        resp.setSalesAmount(item.getSalesAmount());
        resp.setAmountAtOnePercent(item.getAmountAtOnePercent());
        resp.setAmountAtThreePercent(item.getAmountAtThreePercent());
        resp.setOtherAmount(item.getOtherAmount());
        resp.setCrossTenantMonthAmount(item.getCrossTenantMonthAmount());
        resp.setVatExempt(item.getVatExempt());
        resp.setOverExempt(item.getOverExempt());
        resp.setVatAmount(item.getVatAmount());
        resp.setSurchargeAmount(item.getSurchargeAmount());
        resp.setIitAmount(item.getIitAmount());
        resp.setTotalTaxAmount(item.getTotalTaxAmount());
        resp.setPaidAmount(item.getPaidAmount() != null ? item.getPaidAmount()
                : (TaxDeclarationStatusEnum.isPaid(item.getStatus()) ? item.getTotalTaxAmount() : null));
        resp.setStatus(item.getStatus());
        resp.setStatusName(TaxDeclarationStatusEnum.nameOf(item.getStatus()));
        resp.setPaidAt(item.getPaidAt());
        resp.setRemark(item.getRemark());
        return resp;
    }

    private TaxDeclarationPrecheckRespVO toPrecheckResp(String periodMonth, DeclarationBuild build) {
        TaxDeclarationPrecheckRespVO resp = new TaxDeclarationPrecheckRespVO();
        resp.setPeriodMonth(periodMonth);
        boolean blocking = build.missing.stream().anyMatch(item -> BLOCKING_MISSING_TYPES.contains(item.getType()));
        resp.setReady(!blocking);
        resp.setSellerCount(build.items.size());
        resp.setInvoiceCount(build.invoices.size());
        resp.setMissingCount(build.missing.size());
        resp.setMissing(build.missing);
        if (build.missing.isEmpty()) {
            resp.setMessage(StrUtil.format("数据齐备：{} 个出售者、{} 张发票，可申报缴款",
                    build.items.size(), build.invoices.size()));
        } else if (blocking) {
            resp.setMessage(StrUtil.format("还缺 {} 项数据，其中 {} 项会挡住申报：{}",
                    build.missing.size(), build.missing.stream()
                            .filter(item -> BLOCKING_MISSING_TYPES.contains(item.getType())).count(),
                    firstMissingMessage(resp)));
        } else {
            resp.setMessage(StrUtil.format("有 {} 项提示（不挡申报）：{}",
                    build.missing.size(), build.missing.get(0).getMessage()));
        }
        return resp;
    }

    private TaxDeclarationWarningRespVO buildWarning(TaxDeclarationWarningTypeEnum type,
                                                     TaxDeclarationDO declaration, int daysLeft,
                                                     boolean suspensionRisk) {
        TaxDeclarationWarningRespVO resp = new TaxDeclarationWarningRespVO();
        resp.setType(type.getCode());
        resp.setTypeName(type.getName());
        resp.setDeclarationId(declaration.getId());
        resp.setPeriodMonth(declaration.getPeriodMonth());
        resp.setDeclarationDeadline(declaration.getDeclarationDeadline());
        resp.setDaysLeft(daysLeft);
        resp.setOverdue(daysLeft < 0);
        resp.setTotalTaxAmount(declaration.getTotalTaxAmount());
        resp.setSuspensionRisk(suspensionRisk);
        resp.setMissingDataCount(declaration.getMissingDataCount());
        resp.setNextAction(type.getNextAction());
        switch (type) {
            case MISSING_DATA:
                resp.setMessage(StrUtil.format("{} 的申报数据有 {} 项缺失，补齐后才能申报",
                        declaration.getPeriodMonth(), declaration.getMissingDataCount()));
                break;
            case OVERDUE_SUSPENSION_RISK:
                resp.setMessage(StrUtil.format("{} 的申报单已逾期 {} 天未缴款，逾期未缴会被暂停反向开票资格",
                        declaration.getPeriodMonth(), Math.abs(daysLeft)));
                resp.setSuspensionRisk(true);
                break;
            default:
                resp.setMessage(StrUtil.format("{} 的申报期还有 {} 天截止（{}），请尽快完成申报缴款",
                        declaration.getPeriodMonth(), daysLeft, declaration.getDeclarationDeadline()));
        }
        return resp;
    }

    // ==================== 辅助 ====================

    private TaxDeclarationDO requireDeclaration(String periodMonth) {
        TaxDeclarationDO declaration = declarationMapper.selectByPeriodMonth(periodMonth);
        if (declaration == null) {
            throw exception(TAX_DECLARATION_NOT_EXISTS, periodMonth);
        }
        return declaration;
    }

    private InvoiceOrderDO resolveBlue(RedInvoiceDO red, Map<String, InvoiceOrderDO> blueIndex) {
        if (red.getInvoiceOrderId() != null) {
            InvoiceOrderDO blue = invoiceOrderMapper.selectById(red.getInvoiceOrderId());
            if (blue != null) {
                return blue;
            }
        }
        return red.getPartnerOrderId() == null ? null : blueIndex.get(red.getPartnerOrderId());
    }

    private TaxDeclarationInvoiceDO toInvoiceLink(InvoiceOrderDO blue, String invoiceNo,
                                                  BigDecimal amount, String direction) {
        return TaxDeclarationInvoiceDO.builder()
                .invoiceOrderId(blue.getId())
                .partnerOrderId(blue.getPartnerOrderId())
                .invoiceNo(invoiceNo)
                .payeeId(blue.getPayeeId())
                .direction(direction)
                .amount(amount)
                .taxRate(blue.getTaxRate())
                .build();
    }

    /** 统计当月尚未成功的红冲笔数（缺项清单里要能看到「冲减可能不完整」）。 */
    private int countPendingRedOffsets(MonthRange range) {
        return redInvoiceMapper.selectList(new LambdaQueryWrapper<RedInvoiceDO>()
                .ne(RedInvoiceDO::getRedOffsetStatus, RedOffsetStatusEnum.SUCCESS.getStatus())
                .ge(RedInvoiceDO::getCreateTime, range.start)
                .lt(RedInvoiceDO::getCreateTime, range.end)).size();
    }

    private Long countInvoices(Long declarationId) {
        return (long) declarationInvoiceMapper.selectByDeclarationId(declarationId).size();
    }

    /** 重新计算后金额与出售者数未变，已报送的申报单不该被打回待申报。 */
    private boolean sameTotals(TaxDeclarationDO existing, DeclarationBuild build) {
        return nullToZero(existing.getTotalTaxAmount()).compareTo(build.totalTaxAmount) == 0
                && nullToZero(existing.getTotalSalesAmount()).compareTo(build.totalSalesAmount) == 0
                && Integer.valueOf(build.items.size()).equals(existing.getSellerCount());
    }

    private void registerSupplementIfIncreased(TaxDeclarationDO existing, DeclarationBuild build) {
        BigDecimal deltaTax = build.totalTaxAmount.subtract(nullToZero(existing.getTotalTaxAmount()));
        if (deltaTax.signum() <= 0) {
            return;
        }
        BigDecimal oneSales = build.amountAtOnePercent.subtract(nullToZero(existing.getAmountAtOnePercent())).max(BigDecimal.ZERO);
        BigDecimal threeSales = build.amountAtThreePercent.subtract(nullToZero(existing.getAmountAtThreePercent())).max(BigDecimal.ZERO);
        BigDecimal totalSales = oneSales.add(threeSales);
        BigDecimal one;
        BigDecimal three;
        if (totalSales.signum() == 0) {
            one = deltaTax;
            three = BigDecimal.ZERO;
        } else {
            one = deltaTax.multiply(oneSales).divide(totalSales, 2, RoundingMode.HALF_UP);
            three = deltaTax.subtract(one);
        }
        taxSupplementService.recordAuto(existing.getId(), existing.getPeriodMonth(), one, three,
                "申报单缴款后重新计算，应补缴差额");
    }

    private String buildMessage(TaxDeclarationDO declaration, DeclarationBuild build) {
        if (build == null) {
            return StrUtil.format("{} 应缴税费合计 {} 元（增值税 {} + 附加税费 {} + 个人所得税 {}）",
                    declaration.getPeriodMonth(), nullToZero(declaration.getTotalTaxAmount()).toPlainString(),
                    nullToZero(declaration.getVatAmount()).toPlainString(),
                    nullToZero(declaration.getSurchargeAmount()).toPlainString(),
                    nullToZero(declaration.getIitAmount()).toPlainString());
        }
        String base = StrUtil.format("{} 共 {} 个出售者（其中 {} 人当月销售额超 10 万元需单独申报），"
                        + "净销售额 {} 元，应缴税费合计 {} 元",
                declaration.getPeriodMonth(), build.items.size(), declaration.getOverExemptSellerCount(),
                build.totalSalesAmount.toPlainString(), build.totalTaxAmount.toPlainString());
        if (!build.missing.isEmpty()) {
            base += StrUtil.format("；还有 {} 项数据缺失", build.missing.size());
        }
        return base;
    }

    private TaxMissingDataVO missing(String type, String typeName, String message, int count,
                                     String remedy, List<String> samples) {
        TaxMissingDataVO vo = new TaxMissingDataVO();
        vo.setType(type);
        vo.setTypeName(typeName);
        vo.setMessage(message);
        vo.setCount(count);
        vo.setRemedy(remedy);
        vo.setSamples(samples);
        return vo;
    }

    private String firstMissingMessage(TaxDeclarationPrecheckRespVO precheck) {
        return precheck.getMissing() == null || precheck.getMissing().isEmpty()
                ? null : precheck.getMissing().get(0).getMessage();
    }

    private BigDecimal amountOf(InvoiceOrderDO order) {
        if (order.getInvoiceAmount() != null) {
            return order.getInvoiceAmount();
        }
        return order.getTotalAmount() == null ? BigDecimal.ZERO : order.getTotalAmount();
    }

    private static BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    // ==================== 内部结构 ====================

    /**
     * 申报月与它对应的申报期范围。截止日固定为次月 15 日。
     */
    private static final class MonthRange {

        private final String periodMonth;
        private final LocalDateTime start;
        private final LocalDateTime end;

        private MonthRange(String periodMonth, LocalDateTime start, LocalDateTime end) {
            this.periodMonth = periodMonth;
            this.start = start;
            this.end = end;
        }

        static MonthRange of(String periodMonth) {
            try {
                YearMonth month = YearMonth.parse(periodMonth, MONTH_FORMATTER);
                LocalDateTime start = month.atDay(1).atStartOfDay();
                return new MonthRange(periodMonth, start, start.plusMonths(1));
            } catch (DateTimeParseException | NullPointerException e) {
                throw exception(TAX_PERIOD_MONTH_INVALID, String.valueOf(periodMonth));
            }
        }

        LocalDate deadline() {
            return start.toLocalDate().plusMonths(1).withDayOfMonth(IcbcTaxConstants.DECLARATION_DEADLINE_DAY);
        }

    }

    /**
     * 一个出售者的当月归集：蓝票按征收率入桶，红票按原蓝票征收率冲减。
     */
    private static final class PayeeAgg {

        private final Long payeeId;
        private BigDecimal blueOne = BigDecimal.ZERO;
        private BigDecimal blueThree = BigDecimal.ZERO;
        private BigDecimal blueOther = BigDecimal.ZERO;
        private BigDecimal redOne = BigDecimal.ZERO;
        private BigDecimal redThree = BigDecimal.ZERO;
        private BigDecimal redOther = BigDecimal.ZERO;
        private int invoiceCount;

        PayeeAgg(Long payeeId) {
            this.payeeId = payeeId;
        }

        void addBlue(BigDecimal amount, BigDecimal taxRate) {
            invoiceCount++;
            if (isOnePercent(taxRate)) {
                blueOne = blueOne.add(amount);
            } else if (isThreePercent(taxRate)) {
                blueThree = blueThree.add(amount);
            } else {
                blueOther = blueOther.add(amount);
            }
        }

        void addRed(BigDecimal amount, BigDecimal taxRate) {
            if (isOnePercent(taxRate)) {
                redOne = redOne.add(amount);
            } else if (isThreePercent(taxRate)) {
                redThree = redThree.add(amount);
            } else {
                redOther = redOther.add(amount);
            }
        }

        BigDecimal netOne() {
            return blueOne.subtract(redOne).max(BigDecimal.ZERO);
        }

        BigDecimal netThree() {
            return blueThree.subtract(redThree).max(BigDecimal.ZERO);
        }

        BigDecimal netOther() {
            return blueOther.subtract(redOther).max(BigDecimal.ZERO);
        }

        TaxDeclarationItemDO toItem(PayeeInfoDO payee, String periodMonth, BigDecimal crossTenantMonthAmount) {
            BigDecimal one = scale(netOne());
            BigDecimal three = scale(netThree());
            BigDecimal other = scale(netOther());
            BigDecimal sales = one.add(three).add(other);
            boolean overExempt = crossTenantMonthAmount != null
                    && crossTenantMonthAmount.compareTo(IcbcTaxConstants.MONTHLY_EXEMPT_AMOUNT) > 0;
            BigDecimal vat = overExempt
                    ? one.multiply(IcbcTaxConstants.VAT_RATE_ONE_PERCENT)
                    .add(three.multiply(IcbcTaxConstants.VAT_RATE_THREE_PERCENT)).setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            BigDecimal surcharge = vat.multiply(IcbcTaxConstants.SURCHARGE_RATE).setScale(2, RoundingMode.HALF_UP);
            BigDecimal iit = sales.multiply(IcbcTaxConstants.IIT_RATE).setScale(2, RoundingMode.HALF_UP);

            return TaxDeclarationItemDO.builder()
                    .periodMonth(periodMonth)
                    .payeeId(payee.getId())
                    .sellerName(payee.getName())
                    .idCardNo(payee.getIdCardNo())
                    .invoiceCount(invoiceCount)
                    .salesAmount(sales)
                    .amountAtOnePercent(one)
                    .amountAtThreePercent(three)
                    .otherAmount(other)
                    .crossTenantMonthAmount(crossTenantMonthAmount)
                    .vatExempt(!overExempt)
                    .overExempt(overExempt)
                    .vatAmount(vat)
                    .surchargeAmount(surcharge)
                    .iitAmount(iit)
                    .totalTaxAmount(vat.add(surcharge).add(iit))
                    .status(TaxDeclarationStatusEnum.PENDING.getStatus())
                    .build();
        }

        private static boolean isOnePercent(BigDecimal taxRate) {
            return taxRate != null && taxRate.compareTo(IcbcTaxConstants.VAT_RATE_ONE_PERCENT) == 0;
        }

        private static boolean isThreePercent(BigDecimal taxRate) {
            return taxRate != null && taxRate.compareTo(IcbcTaxConstants.VAT_RATE_THREE_PERCENT) == 0;
        }

        private static BigDecimal scale(BigDecimal value) {
            return value.setScale(2, RoundingMode.HALF_UP);
        }

    }

    /**
     * 一次清单计算的中间结果：明细、关联发票、缺项与合计。
     */
    private static final class DeclarationBuild {

        private final List<TaxDeclarationItemDO> items = new ArrayList<>();
        private final List<TaxDeclarationInvoiceDO> invoices = new ArrayList<>();
        private List<TaxMissingDataVO> missing = new ArrayList<>();
        private BigDecimal totalSalesAmount = BigDecimal.ZERO;
        private BigDecimal amountAtOnePercent = BigDecimal.ZERO;
        private BigDecimal amountAtThreePercent = BigDecimal.ZERO;
        private BigDecimal otherAmount = BigDecimal.ZERO;
        private BigDecimal vatAmount = BigDecimal.ZERO;
        private BigDecimal surchargeAmount = BigDecimal.ZERO;
        private BigDecimal iitAmount = BigDecimal.ZERO;
        private BigDecimal totalTaxAmount = BigDecimal.ZERO;

        void addItem(TaxDeclarationItemDO item) {
            items.add(item);
            totalSalesAmount = totalSalesAmount.add(nullToZero(item.getSalesAmount()));
            amountAtOnePercent = amountAtOnePercent.add(nullToZero(item.getAmountAtOnePercent()));
            amountAtThreePercent = amountAtThreePercent.add(nullToZero(item.getAmountAtThreePercent()));
            otherAmount = otherAmount.add(nullToZero(item.getOtherAmount()));
            vatAmount = vatAmount.add(nullToZero(item.getVatAmount()));
            surchargeAmount = surchargeAmount.add(nullToZero(item.getSurchargeAmount()));
            iitAmount = iitAmount.add(nullToZero(item.getIitAmount()));
            totalTaxAmount = totalTaxAmount.add(nullToZero(item.getTotalTaxAmount()));
        }

    }

}
