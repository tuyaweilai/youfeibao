package cn.iocoder.yudao.module.icbc.service.tax.impl;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.icbc.controller.admin.tax.vo.SellerSettlementStatementRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.tax.vo.SettlementReminderHandleReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.tax.vo.SettlementReminderPageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.tax.vo.SettlementReminderRespVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.invoice.InvoiceOrderDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payee.PayeeInfoDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.tax.SettlementReminderDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.invoice.InvoiceOrderMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.payee.PayeeInfoMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.tax.SettlementReminderMapper;
import cn.iocoder.yudao.module.icbc.enums.IcbcTaxConstants;
import cn.iocoder.yudao.module.icbc.enums.InvoiceIssueStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.SettlementReminderStatusEnum;
import cn.iocoder.yudao.module.icbc.service.tax.AnnualSettlementService;
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
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.PAYEE_NOT_EXISTS;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.SETTLEMENT_REMINDER_NOT_EXISTS;

/**
 * 出售者汇算清缴提醒 Service 实现。
 *
 * <p>对账单只统计<b>本租户</b>当年开给该出售者的票与已缴税款：出售者可能同时给多家
 * 回收企业供货，每家各自出自己这一份，合并的汇算由出售者自己在税务端完成。
 */
@Slf4j
@Service
@Validated
public class AnnualSettlementServiceImpl implements AnnualSettlementService {

    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    @Resource
    private SettlementReminderMapper settlementReminderMapper;
    @Resource
    private InvoiceOrderMapper invoiceOrderMapper;
    @Resource
    private PayeeInfoMapper payeeInfoMapper;

    @Override
    public int currentTaxYear() {
        LocalDate now = LocalDate.now();
        return now.getMonthValue() <= IcbcTaxConstants.ANNUAL_SETTLEMENT_DEADLINE_MONTH
                ? now.getYear() - 1 : now.getYear();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int remind(Integer taxYear) {
        int year = taxYear == null ? currentTaxYear() : taxYear;
        LocalDate deadline = LocalDate.of(year + 1,
                IcbcTaxConstants.ANNUAL_SETTLEMENT_DEADLINE_MONTH,
                IcbcTaxConstants.ANNUAL_SETTLEMENT_DEADLINE_DAY);
        Map<Long, SettlementSummary> summaries = summarize(taxYearRange(year));
        int created = 0;
        for (Map.Entry<Long, SettlementSummary> entry : summaries.entrySet()) {
            if (settlementReminderMapper.selectByPayeeIdAndTaxYear(entry.getKey(), year) != null) {
                continue; // 幂等：同一年度同一出售者只生成一条
            }
            PayeeInfoDO payee = payeeInfoMapper.selectById(entry.getKey());
            if (payee == null) {
                continue;
            }
            SettlementSummary summary = entry.getValue();
            SettlementReminderDO reminder = SettlementReminderDO.builder()
                    .payeeId(payee.getId())
                    .sellerName(payee.getName())
                    .idCardNo(payee.getIdCardNo())
                    .taxYear(year)
                    .deadline(deadline)
                    .invoiceCount(summary.invoiceCount())
                    .invoicedAmount(summary.invoicedAmount())
                    .paidTaxAmount(summary.paidTaxAmount())
                    .iitAmount(summary.iitAmount())
                    .status(SettlementReminderStatusEnum.PENDING.getStatus())
                    .remindedAt(LocalDateTime.now())
                    .build();
            settlementReminderMapper.insert(reminder);
            created++;
        }
        if (created > 0) {
            log.info("[remind][纳税年度 {} 新增 {} 条汇算清缴提醒]", year, created);
        }
        return created;
    }

    @Override
    public PageResult<SettlementReminderRespVO> getPage(SettlementReminderPageReqVO reqVO) {
        PageResult<SettlementReminderDO> page = settlementReminderMapper.selectPage(reqVO);
        return new PageResult<>(page.getList().stream().map(this::toResp).toList(), page.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SettlementReminderRespVO handle(SettlementReminderHandleReqVO reqVO) {
        SettlementReminderDO reminder = settlementReminderMapper.selectById(reqVO.getId());
        if (reminder == null) {
            throw exception(SETTLEMENT_REMINDER_NOT_EXISTS);
        }
        boolean targetValid = SettlementReminderStatusEnum.REMINDED.getStatus().equals(reqVO.getStatus());
        boolean currentReminded = SettlementReminderStatusEnum.REMINDED.getStatus().equals(reminder.getStatus());
        if (!targetValid || currentReminded) {
            throw exception(cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.TAX_DECLARATION_STATUS_INVALID,
                    "汇算清缴提醒", "处理", SettlementReminderStatusEnum.nameOf(reminder.getStatus()), "只能标记为已提醒");
        }
        SettlementReminderDO update = new SettlementReminderDO();
        update.setId(reminder.getId());
        update.setStatus(reqVO.getStatus());
        update.setHandleRemark(reqVO.getHandleRemark());
        update.setRemindedAt(LocalDateTime.now());
        settlementReminderMapper.updateById(update);
        return toResp(settlementReminderMapper.selectById(reminder.getId()));
    }

    @Override
    public SellerSettlementStatementRespVO getStatement(Long payeeId, Integer taxYear) {
        PayeeInfoDO payee = payeeId == null ? null : payeeInfoMapper.selectById(payeeId);
        if (payee == null) {
            throw exception(PAYEE_NOT_EXISTS);
        }
        int year = taxYear == null ? currentTaxYear() : taxYear;
        LocalDate deadline = LocalDate.of(year + 1,
                IcbcTaxConstants.ANNUAL_SETTLEMENT_DEADLINE_MONTH,
                IcbcTaxConstants.ANNUAL_SETTLEMENT_DEADLINE_DAY);
        SettlementSummary summary = summarize(taxYearRange(year)).get(payeeId);
        if (summary == null) {
            summary = SettlementSummary.empty();
        }

        SellerSettlementStatementRespVO resp = new SellerSettlementStatementRespVO();
        resp.setPayeeId(payee.getId());
        resp.setSellerName(payee.getName());
        resp.setIdCardMasked(MaskUtils.maskIdCard(payee.getIdCardNo()));
        resp.setTaxYear(year);
        resp.setDeadline(deadline);
        resp.setDaysLeft((int) ChronoUnit.DAYS.between(LocalDate.now(), deadline));
        resp.setOverdue(deadline.isBefore(LocalDate.now()));
        resp.setInvoiceCount(summary.invoiceCount());
        resp.setInvoicedAmount(summary.invoicedAmount());
        resp.setPaidTaxAmount(summary.paidTaxAmount());
        resp.setIitAmount(summary.iitAmount());
        resp.setMonths(summary.months());
        resp.setMessage(buildStatementMessage(payee, year, deadline, summary));
        resp.setGeneratedAt(LocalDateTime.now());
        return resp;
    }

    private String buildStatementMessage(PayeeInfoDO payee, int taxYear, LocalDate deadline,
                                         SettlementSummary summary) {
        if (summary.invoiceCount() == 0) {
            return StrUtil.format("{} 年度没有反向开票记录，无需汇算清缴", taxYear);
        }
        return StrUtil.format("{} 年度反向开票 {} 张、金额 {} 元，平台已代办预缴税费 {} 元（其中个人所得税 {} 元）。"
                        + "请于 {} 前自行完成经营所得汇算清缴，逾期可能被要求停止反向开票",
                taxYear, summary.invoiceCount(), summary.invoicedAmount().toPlainString(),
                summary.paidTaxAmount().toPlainString(), summary.iitAmount().toPlainString(), deadline);
    }

    private SettlementReminderRespVO toResp(SettlementReminderDO reminder) {
        SettlementReminderRespVO resp = new SettlementReminderRespVO();
        resp.setId(reminder.getId());
        resp.setPayeeId(reminder.getPayeeId());
        resp.setSellerName(reminder.getSellerName());
        resp.setIdCardMasked(MaskUtils.maskIdCard(reminder.getIdCardNo()));
        resp.setTaxYear(reminder.getTaxYear());
        resp.setDeadline(reminder.getDeadline());
        if (reminder.getDeadline() != null) {
            resp.setDaysLeft((int) ChronoUnit.DAYS.between(LocalDate.now(), reminder.getDeadline()));
            resp.setOverdue(reminder.getDeadline().isBefore(LocalDate.now()));
        }
        resp.setInvoiceCount(reminder.getInvoiceCount());
        resp.setInvoicedAmount(reminder.getInvoicedAmount());
        resp.setPaidTaxAmount(reminder.getPaidTaxAmount());
        resp.setIitAmount(reminder.getIitAmount());
        resp.setStatus(reminder.getStatus());
        resp.setStatusName(SettlementReminderStatusEnum.nameOf(reminder.getStatus()));
        resp.setNextAction(SettlementReminderStatusEnum.nextActionOf(reminder.getStatus()));
        resp.setRemindedAt(reminder.getRemindedAt());
        resp.setHandleRemark(reminder.getHandleRemark());
        resp.setRemark(reminder.getRemark());
        return resp;
    }

    /**
     * 按出售者归集某年已开出的票与已缴税款。
     */
    private Map<Long, SettlementSummary> summarize(LocalDateTime[] range) {
        List<InvoiceOrderDO> orders = invoiceOrderMapper.selectList(new LambdaQueryWrapper<InvoiceOrderDO>()
                .eq(InvoiceOrderDO::getInvoiceStatus, InvoiceIssueStatusEnum.ISSUED.getStatus())
                .ge(InvoiceOrderDO::getInvoiceDate, range[0])
                .lt(InvoiceOrderDO::getInvoiceDate, range[1]));
        Map<Long, SettlementSummary> summaries = new LinkedHashMap<>();
        for (InvoiceOrderDO order : orders) {
            if (order.getPayeeId() == null) {
                continue;
            }
            SettlementSummary summary = summaries.computeIfAbsent(order.getPayeeId(), key -> new SettlementSummary());
            BigDecimal amount = order.getInvoiceAmount() != null ? order.getInvoiceAmount() : order.getTotalAmount();
            amount = amount == null ? BigDecimal.ZERO : amount;
            BigDecimal paidTax = order.getTaxRealAmount() != null ? order.getTaxRealAmount() : order.getTaxAmount();
            paidTax = paidTax == null ? BigDecimal.ZERO : paidTax;
            summary.add(order.getInvoiceDate().format(MONTH_FORMATTER), amount, paidTax);
        }
        return summaries;
    }

    private LocalDateTime[] taxYearRange(int taxYear) {
        LocalDateTime start = LocalDate.of(taxYear, 1, 1).atStartOfDay();
        return new LocalDateTime[]{start, start.plusYears(1)};
    }

    /**
     * 一个出售者某年的开票与缴税汇总。
     */
    private static final class SettlementSummary {

        private int invoiceCount;
        private BigDecimal invoicedAmount = BigDecimal.ZERO;
        private BigDecimal paidTaxAmount = BigDecimal.ZERO;
        private final Map<String, SellerSettlementStatementRespVO.MonthStatement> monthMap = new LinkedHashMap<>();

        static SettlementSummary empty() {
            return new SettlementSummary();
        }

        void add(String month, BigDecimal amount, BigDecimal paidTax) {
            invoiceCount++;
            invoicedAmount = invoicedAmount.add(amount);
            paidTaxAmount = paidTaxAmount.add(paidTax);
            SellerSettlementStatementRespVO.MonthStatement statement = monthMap.computeIfAbsent(month, key -> {
                SellerSettlementStatementRespVO.MonthStatement item = new SellerSettlementStatementRespVO.MonthStatement();
                item.setMonth(key);
                item.setInvoiceCount(0);
                item.setInvoicedAmount(BigDecimal.ZERO);
                item.setPaidTaxAmount(BigDecimal.ZERO);
                return item;
            });
            statement.setInvoiceCount(statement.getInvoiceCount() + 1);
            statement.setInvoicedAmount(statement.getInvoicedAmount().add(amount));
            statement.setPaidTaxAmount(statement.getPaidTaxAmount().add(paidTax));
        }

        int invoiceCount() {
            return invoiceCount;
        }

        BigDecimal invoicedAmount() {
            return invoicedAmount;
        }

        BigDecimal paidTaxAmount() {
            return paidTaxAmount;
        }

        /** 个人所得税预缴额 = 销售额 × 0.5%（平台代办口径） */
        BigDecimal iitAmount() {
            return invoicedAmount.multiply(IcbcTaxConstants.IIT_RATE).setScale(2, RoundingMode.HALF_UP);
        }

        List<SellerSettlementStatementRespVO.MonthStatement> months() {
            List<SellerSettlementStatementRespVO.MonthStatement> months = new ArrayList<>(monthMap.values());
            BigDecimal iitRate = IcbcTaxConstants.IIT_RATE;
            for (SellerSettlementStatementRespVO.MonthStatement statement : months) {
                statement.setIitAmount(statement.getInvoicedAmount().multiply(iitRate)
                        .setScale(2, RoundingMode.HALF_UP));
            }
            return months;
        }

    }

}
