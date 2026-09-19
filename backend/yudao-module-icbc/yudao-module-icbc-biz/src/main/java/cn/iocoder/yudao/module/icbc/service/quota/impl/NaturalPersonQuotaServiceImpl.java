package cn.iocoder.yudao.module.icbc.service.quota.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.module.icbc.controller.admin.quota.vo.SellerQuotaCheckRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.quota.vo.SellerQuotaGuidanceHandleReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.quota.vo.SellerQuotaGuidancePageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.quota.vo.SellerQuotaMonthRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.quota.vo.SellerQuotaRespVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.invoice.InvoiceOrderDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.invoice.RedInvoiceDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.naturalperson.IcbcNaturalPersonDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payee.PayeeInfoDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.quota.SellerQuotaGuidanceDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.invoice.InvoiceOrderMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.invoice.RedInvoiceMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.payee.PayeeInfoMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.quota.SellerQuotaGuidanceMapper;
import cn.iocoder.yudao.module.icbc.enums.InvoiceIssueStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.IcbcTaxConstants;
import cn.iocoder.yudao.module.icbc.enums.PreInvoiceStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.RedOffsetStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.SellerQuotaGuidanceStatusEnum;
import cn.iocoder.yudao.module.icbc.service.naturalperson.NaturalPersonService;
import cn.iocoder.yudao.module.icbc.service.quota.NaturalPersonQuotaService;
import cn.iocoder.yudao.module.icbc.util.MaskUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.*;

/**
 * 自然人出售者额度台账实现（issue #12）。
 *
 * <p>台账是<b>派生视图</b>：直接从各租户的开票订单（蓝票）与红字发票（红票）算出来，
 * 不另外维护一张需要同步、会漂移的汇总表。这张「表」的每一行就是「出售者 × 月」，
 * 已用额度是窗口内各行的合计，红冲从合计里扣回来。
 */
@Slf4j
@Service
@Validated
public class NaturalPersonQuotaServiceImpl implements NaturalPersonQuotaService {

    /** 连续 12 个月滚动窗口上限（元）：500 万 */
    public static final BigDecimal CAP_AMOUNT = new BigDecimal("5000000.00");
    /** 月销售额免征线（元）：10 万。口径与代办税费申报一致，见 {@link IcbcTaxConstants} */
    public static final BigDecimal MONTHLY_EXEMPT_AMOUNT = IcbcTaxConstants.MONTHLY_EXEMPT_AMOUNT;

    private static final BigDecimal ONE_PERCENT_RATE = new BigDecimal("0.01");
    private static final BigDecimal THREE_PERCENT_RATE = new BigDecimal("0.03");
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final String REMEDY_QUOTA =
            "引导该出售者办理经营主体登记，由经营主体开票；若已开票金额有误，先走红冲把额度放出来";

    @Resource
    private PayeeInfoMapper payeeInfoMapper;
    @Resource
    private InvoiceOrderMapper invoiceOrderMapper;
    @Resource
    private RedInvoiceMapper redInvoiceMapper;
    @Resource
    private SellerQuotaGuidanceMapper guidanceMapper;
    @Resource
    private NaturalPersonService naturalPersonService;

    // ==================== 额度台账 ====================

    @Override
    public SellerQuotaRespVO getQuota(Long payeeId) {
        PayeeInfoDO payee = requirePayee(payeeId);
        return buildQuota(payee, loadLedger(payee));
    }

    @Override
    public SellerQuotaCheckRespVO checkQuota(Long payeeId, BigDecimal applyAmount) {
        PayeeInfoDO payee = requirePayee(payeeId);
        return check(loadLedger(payee), applyAmount);
    }

    @Override
    public BigDecimal getCrossTenantMonthlyNetAmount(Long payeeId, String month) {
        PayeeInfoDO payee = requirePayee(payeeId);
        return loadLedger(payee).netAmountOfMonth(month);
    }

    private SellerQuotaCheckRespVO check(QuotaLedger ledger, BigDecimal applyAmount) {
        BigDecimal amount = applyAmount == null ? BigDecimal.ZERO : applyAmount;
        BigDecimal used = ledger.usedAmount();
        BigDecimal remaining = CAP_AMOUNT.subtract(used).max(BigDecimal.ZERO);
        BigDecimal remainingAfter = CAP_AMOUNT.subtract(used).subtract(amount);
        BigDecimal monthAfter = ledger.currentMonthNetAmount().add(amount);
        boolean monthlyOverExempt = monthAfter.compareTo(MONTHLY_EXEMPT_AMOUNT) > 0;

        SellerQuotaCheckRespVO resp = new SellerQuotaCheckRespVO();
        resp.setCapAmount(CAP_AMOUNT);
        resp.setUsedAmount(used);
        resp.setRemainingAmount(remaining);
        resp.setRemainingAfterAmount(remainingAfter);
        resp.setCurrentMonthAmount(ledger.currentMonthNetAmount());
        resp.setMonthlyExemptAmount(MONTHLY_EXEMPT_AMOUNT);
        resp.setMonthlyOverExempt(monthlyOverExempt);

        if (used.compareTo(CAP_AMOUNT) > 0) {
            // 已经超了：这一笔完全不能开
            resp.setPassed(false);
            resp.setQuotaExceeded(true);
            resp.setMessage(ServiceExceptionUtil.doFormat(SELLER_QUOTA_EXCEEDED.getCode(),
                    SELLER_QUOTA_EXCEEDED.getMsg(), used.toPlainString()));
            resp.setRemedy(REMEDY_QUOTA);
            return resp;
        }
        if (remainingAfter.signum() < 0) {
            // 这一笔会把它顶出去：说清已用、余量与本次金额，便于拆单或引导
            resp.setPassed(false);
            resp.setQuotaExceeded(true);
            resp.setMessage(ServiceExceptionUtil.doFormat(SELLER_QUOTA_EXCEEDED_BY_THIS_ONE.getCode(),
                    SELLER_QUOTA_EXCEEDED_BY_THIS_ONE.getMsg(), amount.toPlainString(),
                    used.toPlainString(), remaining.toPlainString()));
            resp.setRemedy(REMEDY_QUOTA);
            return resp;
        }

        resp.setPassed(true);
        resp.setQuotaExceeded(false);
        StringBuilder message = new StringBuilder(StrUtil.format(
                "额度充足：连续 12 个月已用 {} 元，余量 {} 元", used.toPlainString(), remaining.toPlainString()));
        if (monthlyOverExempt) {
            message.append(StrUtil.format("；本次后本月销售额 {} 元将超过 10 万元免征线，须按时代办申报缴款",
                    monthAfter.toPlainString()));
        }
        resp.setMessage(message.toString());
        return resp;
    }

    private SellerQuotaRespVO buildQuota(PayeeInfoDO payee, QuotaLedger ledger) {
        BigDecimal used = ledger.usedAmount();
        BigDecimal remaining = CAP_AMOUNT.subtract(used).max(BigDecimal.ZERO);
        BigDecimal currentMonth = ledger.currentMonthNetAmount();
        boolean exceeded = used.compareTo(CAP_AMOUNT) > 0;
        boolean monthlyOverExempt = currentMonth.compareTo(MONTHLY_EXEMPT_AMOUNT) > 0;

        SellerQuotaRespVO resp = new SellerQuotaRespVO();
        resp.setPayeeId(payee.getId());
        resp.setName(payee.getName());
        resp.setIdCardMasked(MaskUtils.maskIdCard(payee.getIdCardNo()));
        resp.setCapAmount(CAP_AMOUNT);
        resp.setIssuedAmount(ledger.getIssuedAmount());
        resp.setPendingAmount(ledger.getPendingAmount());
        resp.setRedOffsetAmount(ledger.getRedOffsetAmount());
        resp.setUsedAmount(used);
        resp.setRemainingAmount(remaining);
        resp.setWindowStart(ledger.getWindowStart());
        resp.setWindowEnd(ledger.getWindowEnd());
        resp.setAmountAtOnePercent(ledger.getAmountAtOnePercent());
        resp.setAmountAtThreePercent(ledger.getAmountAtThreePercent());
        resp.setOtherAmount(ledger.getOtherAmount());
        resp.setMonthlyExemptAmount(MONTHLY_EXEMPT_AMOUNT);
        resp.setCurrentMonthAmount(currentMonth);
        resp.setCurrentMonthOverExempt(monthlyOverExempt);
        resp.setQuotaExceeded(exceeded);
        resp.setMonths(buildMonths(ledger));
        resp.setMessage(buildQuotaMessage(payee, ledger));
        return resp;
    }

    private String buildQuotaMessage(PayeeInfoDO payee, QuotaLedger ledger) {
        BigDecimal used = ledger.usedAmount();
        if (used.compareTo(CAP_AMOUNT) > 0) {
            return StrUtil.format("{} 连续 12 个月反向开票累计销售额 {} 元已超过 500 万元上限，不能再反向开票；"
                            + "请引导其办理经营主体登记，由经营主体开票",
                    payee.getName(), used.toPlainString());
        }
        BigDecimal remaining = CAP_AMOUNT.subtract(used).max(BigDecimal.ZERO);
        String message = StrUtil.format("连续 12 个月累计销售额 {} 元，余量 {} 元（上限 500 万元）",
                used.toPlainString(), remaining.toPlainString());
        BigDecimal currentMonth = ledger.currentMonthNetAmount();
        if (currentMonth.compareTo(MONTHLY_EXEMPT_AMOUNT) > 0) {
            message += StrUtil.format("；本月销售额 {} 元已超过 10 万元免征线，须按时代办申报缴款",
                    currentMonth.toPlainString());
        }
        return message;
    }

    private List<SellerQuotaMonthRespVO> buildMonths(QuotaLedger ledger) {
        List<SellerQuotaMonthRespVO> months = new ArrayList<>();
        for (String month : ledger.getMonths().keySet()) {
            MonthAmount amount = ledger.getMonths().get(month);
            SellerQuotaMonthRespVO vo = new SellerQuotaMonthRespVO();
            vo.setMonth(month);
            vo.setIssuedAmount(amount.getIssuedAmount());
            vo.setPendingAmount(amount.getPendingAmount());
            vo.setRedOffsetAmount(amount.getRedOffsetAmount());
            vo.setNetAmount(amount.netAmount());
            vo.setAmountAtOnePercent(amount.getAmountAtOnePercent());
            vo.setAmountAtThreePercent(amount.getAmountAtThreePercent());
            vo.setOtherAmount(amount.getOtherAmount());
            vo.setOverMonthlyExempt(amount.netAmount().compareTo(MONTHLY_EXEMPT_AMOUNT) > 0);
            months.add(vo);
        }
        return months;
    }

    // ==================== 引导记录 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordGuidance(Long payeeId, String scene, String bizNo) {
        PayeeInfoDO payee = requirePayee(payeeId);
        BigDecimal used = loadLedger(payee).usedAmount();
        LocalDateTime now = LocalDateTime.now();

        SellerQuotaGuidanceDO open = guidanceMapper.selectOpenByPayeeId(payeeId);
        if (open != null) {
            // 已有未办结的引导：只刷新额度与最近触发时间，不新增（否则被拒一次就多一条）
            SellerQuotaGuidanceDO update = new SellerQuotaGuidanceDO();
            update.setId(open.getId());
            update.setSellerName(payee.getName());
            update.setIdCardNo(payee.getIdCardNo());
            update.setTriggerScene(scene);
            update.setTriggerBizNo(bizNo);
            update.setUsedAmount(used);
            update.setCapAmount(CAP_AMOUNT);
            update.setLastTriggeredAt(now);
            guidanceMapper.updateById(update);
            return;
        }

        SellerQuotaGuidanceDO guidance = new SellerQuotaGuidanceDO();
        guidance.setPayeeId(payeeId);
        guidance.setSellerName(payee.getName());
        guidance.setIdCardNo(payee.getIdCardNo());
        guidance.setTriggerScene(scene);
        guidance.setTriggerBizNo(bizNo);
        guidance.setUsedAmount(used);
        guidance.setCapAmount(CAP_AMOUNT);
        guidance.setStatus(SellerQuotaGuidanceStatusEnum.PENDING.getStatus());
        guidance.setTriggeredAt(now);
        guidance.setLastTriggeredAt(now);
        guidanceMapper.insert(guidance);
        log.warn("出售者额度超限，已生成经营主体登记引导 - payeeId: {}, usedAmount: {}, scene: {}",
                payeeId, used, scene);
    }

    @Override
    public PageResult<SellerQuotaGuidanceDO> getGuidancePage(SellerQuotaGuidancePageReqVO reqVO) {
        return guidanceMapper.selectPage(reqVO);
    }

    @Override
    public void handleGuidance(SellerQuotaGuidanceHandleReqVO reqVO) {
        SellerQuotaGuidanceDO guidance = guidanceMapper.selectById(reqVO.getId());
        if (guidance == null) {
            throw exception(SELLER_QUOTA_GUIDANCE_NOT_EXISTS);
        }
        boolean targetValid = SellerQuotaGuidanceStatusEnum.INFORMED.getStatus().equals(reqVO.getStatus())
                || SellerQuotaGuidanceStatusEnum.RESOLVED.getStatus().equals(reqVO.getStatus());
        // 已办结不能回退；「待引导」是系统给的状态，不能手工设回
        boolean currentResolved = SellerQuotaGuidanceStatusEnum.RESOLVED.getStatus().equals(guidance.getStatus());
        if (!targetValid || currentResolved) {
            throw exception(SELLER_QUOTA_GUIDANCE_STATUS_INVALID,
                    SellerQuotaGuidanceStatusEnum.nameOf(reqVO.getStatus()));
        }
        SellerQuotaGuidanceDO update = new SellerQuotaGuidanceDO();
        update.setId(reqVO.getId());
        update.setStatus(reqVO.getStatus());
        update.setHandleRemark(reqVO.getHandleRemark());
        update.setHandledAt(LocalDateTime.now());
        guidanceMapper.updateById(update);
    }

    // ==================== 台账取数 ====================

    private PayeeInfoDO requirePayee(Long payeeId) {
        PayeeInfoDO payee = payeeId == null ? null : payeeInfoMapper.selectById(payeeId);
        if (payee == null) {
            throw exception(PAYEE_NOT_EXISTS);
        }
        return payee;
    }

    /**
     * 汇总窗口内这个自然人的全部蓝票与红票。
     *
     * <p>按身份证号把同一自然人在本平台多个租户下的收方档案找齐，再跨租户汇总——额度是自然人
     * 的，不是租户的，租户拦截器在这里必须让开（{@link TenantUtils#executeIgnore}），否则
     * 「跨企业合并计算」就无从谈起。
     */
    private QuotaLedger loadLedger(PayeeInfoDO payee) {
        LocalDateTime windowEnd = LocalDateTime.now();
        LocalDateTime windowStart = windowEnd.minusMonths(12);
        QuotaLedger ledger = new QuotaLedger();
        ledger.setWindowStart(windowStart);
        ledger.setWindowEnd(windowEnd);

        List<PayeeInfoDO> samePerson = TenantUtils.executeIgnore(() -> findSamePerson(payee));
        Set<Long> payeeIds = samePerson.stream().map(PayeeInfoDO::getId)
                .filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> payeeNos = new LinkedHashSet<>();
        Set<Long> naturalPersonIds = new LinkedHashSet<>();
        for (PayeeInfoDO item : samePerson) {
            if (StrUtil.isNotBlank(item.getPayeeNo())) {
                payeeNos.add(item.getPayeeNo());
            }
            if (StrUtil.isNotBlank(item.getPartnerPayeeId())) {
                payeeNos.add(item.getPartnerPayeeId());
            }
            if (item.getNaturalPersonId() != null) {
                naturalPersonIds.add(item.getNaturalPersonId());
            }
        }
        // 票据上的 payee_no 存的是工行 outUserId：迁移前的历史票是收方档案编号，
        // 迁移后的新票是**平台级**外部用户编号，两个都要收进来，额度才算得全（ADR 0017）
        for (IcbcNaturalPersonDO person : naturalPersonService.getNaturalPersonList(naturalPersonIds)) {
            if (StrUtil.isNotBlank(person.getOutUserId())) {
                payeeNos.add(person.getOutUserId());
            }
        }
        if (payeeIds.isEmpty() && payeeNos.isEmpty()) {
            return ledger;
        }

        LambdaQueryWrapper<InvoiceOrderDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(identity -> {
            if (CollUtil.isNotEmpty(payeeIds)) {
                identity.in(InvoiceOrderDO::getPayeeId, payeeIds);
            }
            if (CollUtil.isNotEmpty(payeeNos)) {
                if (CollUtil.isNotEmpty(payeeIds)) {
                    identity.or();
                }
                identity.in(InvoiceOrderDO::getPayeeNo, payeeNos);
            }
        });
        // 窗口按「开票日期」算，但预开票在途的还没开票日期，所以两者取或，落到 Java 里再精确判定
        wrapper.and(effective -> effective.ge(InvoiceOrderDO::getCreateTime, windowStart)
                .or().ge(InvoiceOrderDO::getInvoiceDate, windowStart));

        List<InvoiceOrderDO> orders = TenantUtils.executeIgnore(() -> invoiceOrderMapper.selectList(wrapper));
        Set<String> countedPartnerOrderIds = new LinkedHashSet<>();
        for (InvoiceOrderDO order : orders) {
            LocalDateTime effectiveTime = effectiveTime(order);
            if (effectiveTime == null || effectiveTime.isBefore(windowStart)) {
                continue;
            }
            BigDecimal amount = orderAmount(order);
            boolean issued = InvoiceIssueStatusEnum.isIssued(order.getInvoiceStatus());
            boolean pending = !issued
                    && !InvoiceIssueStatusEnum.FAILED.getStatus().equals(order.getInvoiceStatus())
                    && PreInvoiceStatusEnum.SUCCESS.getStatus().equals(order.getPreInvoiceStatus());
            if (!issued && !pending) {
                continue; // 未开票 / 开票失败 / 预开票未成功：不占额度
            }
            if (StrUtil.isNotBlank(order.getPartnerOrderId())) {
                countedPartnerOrderIds.add(order.getPartnerOrderId());
            }
            ledger.count(monthOf(effectiveTime), amount, issued, order.getTaxRate());
        }

        loadRedOffsets(ledger, countedPartnerOrderIds, windowStart);
        // 本月即使没有销售额也要出现在台账里，否则界面上看不到「本月」
        ledger.touchMonth(monthOf(windowEnd));
        return ledger;
    }

    /**
     * 红冲把销售额冲回去，已用额度也要跟着降。
     *
     * <p>只有「原蓝票也在窗口内」的红冲才扣：蓝票已经滚出窗口的，本就已不计入额度，
     * 再扣一次会凭空多出余量。
     */
    private void loadRedOffsets(QuotaLedger ledger, Set<String> countedPartnerOrderIds, LocalDateTime windowStart) {
        if (CollUtil.isEmpty(countedPartnerOrderIds)) {
            return;
        }
        List<RedInvoiceDO> redInvoices = TenantUtils.executeIgnore(() -> redInvoiceMapper.selectList(
                new LambdaQueryWrapper<RedInvoiceDO>()
                        .in(RedInvoiceDO::getPartnerOrderId, countedPartnerOrderIds)
                        .eq(RedInvoiceDO::getRedOffsetStatus, RedOffsetStatusEnum.SUCCESS.getStatus())));
        for (RedInvoiceDO redInvoice : redInvoices) {
            LocalDateTime effectiveTime = redInvoice.getRedInvoiceDate() != null
                    ? redInvoice.getRedInvoiceDate() : redInvoice.getCreateTime();
            if (effectiveTime == null || effectiveTime.isBefore(windowStart)) {
                continue;
            }
            ledger.redOffset(monthOf(effectiveTime),
                    redInvoice.getAmount() == null ? BigDecimal.ZERO : redInvoice.getAmount());
        }
    }

    private List<PayeeInfoDO> findSamePerson(PayeeInfoDO payee) {
        // 同一自然人可能在本平台多个租户各有一份收方档案，按身份证号把它们找齐
        List<PayeeInfoDO> payees = StrUtil.isNotBlank(payee.getIdCardNo())
                ? payeeInfoMapper.selectList(PayeeInfoDO::getIdCardNo, payee.getIdCardNo())
                : List.of();
        if (payees == null || payees.isEmpty()) {
            return List.of(payee);
        }
        Map<Long, PayeeInfoDO> byId = new LinkedHashMap<>();
        for (PayeeInfoDO item : payees) {
            if (item.getId() != null) {
                byId.put(item.getId(), item);
            }
        }
        byId.putIfAbsent(payee.getId(), payee);
        return new ArrayList<>(byId.values());
    }

    private LocalDateTime effectiveTime(InvoiceOrderDO order) {
        return order.getInvoiceDate() != null ? order.getInvoiceDate() : order.getCreateTime();
    }

    private BigDecimal orderAmount(InvoiceOrderDO order) {
        if (order.getInvoiceAmount() != null) {
            return order.getInvoiceAmount();
        }
        return order.getTotalAmount() == null ? BigDecimal.ZERO : order.getTotalAmount();
    }

    private String monthOf(LocalDateTime time) {
        return time.toLocalDate().format(MONTH_FORMATTER);
    }

    // ==================== 台账结构 ====================

    /**
     * 滚动窗口内这个自然人的额度占用：已开票 + 在途 − 红冲，以及按月分列。
     */
    @Getter
    @Setter
    private static final class QuotaLedger {

        private BigDecimal issuedAmount = BigDecimal.ZERO;
        private BigDecimal pendingAmount = BigDecimal.ZERO;
        private BigDecimal redOffsetAmount = BigDecimal.ZERO;
        private BigDecimal amountAtOnePercent = BigDecimal.ZERO;
        private BigDecimal amountAtThreePercent = BigDecimal.ZERO;
        private BigDecimal otherAmount = BigDecimal.ZERO;
        /** 月份 yyyy-MM → 该月金额；最近的月份在前 */
        private final Map<String, MonthAmount> months = new java.util.TreeMap<>(Comparator.reverseOrder());

        private LocalDateTime windowStart;
        private LocalDateTime windowEnd;

        void count(String month, BigDecimal amount, boolean issued, BigDecimal taxRate) {
            MonthAmount bucket = touchMonth(month);
            if (issued) {
                issuedAmount = issuedAmount.add(amount);
                bucket.setIssuedAmount(bucket.getIssuedAmount().add(amount));
            } else {
                pendingAmount = pendingAmount.add(amount);
                bucket.setPendingAmount(bucket.getPendingAmount().add(amount));
            }
            if (isOnePercent(taxRate)) {
                amountAtOnePercent = amountAtOnePercent.add(amount);
                bucket.setAmountAtOnePercent(bucket.getAmountAtOnePercent().add(amount));
            } else if (isThreePercent(taxRate)) {
                amountAtThreePercent = amountAtThreePercent.add(amount);
                bucket.setAmountAtThreePercent(bucket.getAmountAtThreePercent().add(amount));
            } else {
                otherAmount = otherAmount.add(amount);
                bucket.setOtherAmount(bucket.getOtherAmount().add(amount));
            }
        }

        void redOffset(String month, BigDecimal amount) {
            redOffsetAmount = redOffsetAmount.add(amount);
            MonthAmount bucket = touchMonth(month);
            bucket.setRedOffsetAmount(bucket.getRedOffsetAmount().add(amount));
        }

        MonthAmount touchMonth(String month) {
            return months.computeIfAbsent(month, key -> new MonthAmount());
        }

        /** 已用额度 = 已开票 + 在途 − 红冲 */
        BigDecimal usedAmount() {
            return issuedAmount.add(pendingAmount).subtract(redOffsetAmount).max(BigDecimal.ZERO);
        }

        BigDecimal currentMonthNetAmount() {
            MonthAmount bucket = months.get(LocalDate.now().format(MONTH_FORMATTER));
            return bucket == null ? BigDecimal.ZERO : bucket.netAmount();
        }

        BigDecimal netAmountOfMonth(String month) {
            MonthAmount bucket = months.get(month);
            return bucket == null ? BigDecimal.ZERO : bucket.netAmount();
        }

        private static boolean isOnePercent(BigDecimal taxRate) {
            return taxRate != null && taxRate.compareTo(ONE_PERCENT_RATE) == 0;
        }

        private static boolean isThreePercent(BigDecimal taxRate) {
            return taxRate != null && taxRate.compareTo(THREE_PERCENT_RATE) == 0;
        }
    }

    /**
     * 「出售者 × 月」的一行。
     */
    @Getter
    @Setter
    private static final class MonthAmount {

        private BigDecimal issuedAmount = BigDecimal.ZERO;
        private BigDecimal pendingAmount = BigDecimal.ZERO;
        private BigDecimal redOffsetAmount = BigDecimal.ZERO;
        private BigDecimal amountAtOnePercent = BigDecimal.ZERO;
        private BigDecimal amountAtThreePercent = BigDecimal.ZERO;
        private BigDecimal otherAmount = BigDecimal.ZERO;

        BigDecimal netAmount() {
            return issuedAmount.add(pendingAmount).subtract(redOffsetAmount).max(BigDecimal.ZERO);
        }

    }

}
