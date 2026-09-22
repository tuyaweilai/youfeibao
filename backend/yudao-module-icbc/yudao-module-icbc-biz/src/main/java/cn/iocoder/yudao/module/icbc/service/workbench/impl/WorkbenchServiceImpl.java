package cn.iocoder.yudao.module.icbc.service.workbench.impl;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.icbc.controller.admin.stockin.vo.StockInPendingPageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.stockin.vo.StockInPendingRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.workbench.vo.WorkbenchItemRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.workbench.vo.WorkbenchOverviewRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.workbench.vo.WorkbenchReadinessItemRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.workbench.vo.WorkbenchReadinessRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.workbench.vo.WorkbenchTodoRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.workbench.vo.WorkbenchWarningRespVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.acquisition.IcbcAcquisitionDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.appointment.IcbcAppointmentDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.invoice.InvoiceOrderDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payment.PaymentOrderDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.quota.SellerQuotaGuidanceDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.settlement.IcbcSettlementDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.warning.IcbcExpiryWarningDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.acquisition.IcbcAcquisitionMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.appointment.IcbcAppointmentMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.entauth.IcbcEnterpriseAuthMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.invoice.InvoiceOrderMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.payer.PayerInfoMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.payment.PaymentOrderMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.quota.SellerQuotaGuidanceMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.settlement.IcbcSettlementMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.warning.IcbcExpiryWarningMapper;
import cn.iocoder.yudao.module.icbc.enums.AppointmentStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.InvoiceIssueStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.PaymentStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.PreInvoiceStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.SellerQuotaGuidanceStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.SettlementConfirmStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.TaxStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.UploadStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.WorkbenchTodoCodeEnum;
import cn.iocoder.yudao.module.icbc.service.goodscfg.IcbcGoodsConfigService;
import cn.iocoder.yudao.module.icbc.service.qualification.IcbcQualificationService;
import cn.iocoder.yudao.module.icbc.service.stockin.StockInService;
import cn.iocoder.yudao.module.icbc.service.workbench.WorkbenchService;
import cn.iocoder.yudao.module.icbc.util.MaskUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 工作台 Service 实现（#56 T18）。
 *
 * <p>只读聚合：每个数字都是一条能落到具体单据的查询，口径写在
 * {@link WorkbenchTodoCodeEnum#getDefinition()} 里并随响应返回。这里不写任何业务数据，
 * 也不从别的数字推导状态（ADR 0021）。
 *
 * <p>每项待办附带最多 {@link #PREVIEW_LIMIT} 条来源明细，供界面直接下钻；超出部分由用户到
 * 对应模块查看全部。明细只取「谁、哪一笔、什么状态、多少钱」，不做二次加工。
 */
@Slf4j
@Service
@Validated
public class WorkbenchServiceImpl implements WorkbenchService {

    /** 每项待办 / 预警在界面上直接展示的来源明细条数上限 */
    private static final int PREVIEW_LIMIT = 10;

    private static final String LEVEL_OK = "OK";
    private static final String LEVEL_WARN = "WARN";
    private static final String LEVEL_DANGER = "DANGER";

    /** 企业授权状态：1-已授权（见 {@code IcbcEnterpriseAuthDO#authStatus}） */
    private static final Integer ENTERPRISE_AUTH_APPROVED = 1;
    /** 资质到期预警状态：0-待处理（见 {@code IcbcExpiryWarningDO#status}） */
    private static final Integer EXPIRY_WARNING_OPEN = 0;

    /** 待付款超时的天数阈值（#106）：超过它就进待办，只提醒不自动取消 */
    @Value("${icbc.invoice.pending-payment-days:7}")
    private int pendingPaymentDays;

    @Resource
    private IcbcAppointmentMapper icbcAppointmentMapper;
    @Resource
    private IcbcAcquisitionMapper acquisitionMapper;
    @Resource
    private IcbcSettlementMapper settlementMapper;
    @Resource
    private PaymentOrderMapper paymentOrderMapper;
    @Resource
    private InvoiceOrderMapper invoiceOrderMapper;
    @Resource
    private SellerQuotaGuidanceMapper quotaGuidanceMapper;
    @Resource
    private IcbcExpiryWarningMapper expiryWarningMapper;
    @Resource
    private IcbcEnterpriseAuthMapper enterpriseAuthMapper;
    @Resource
    private PayerInfoMapper payerInfoMapper;
    @Resource
    private IcbcQualificationService qualificationService;
    @Resource
    private StockInService stockInService;
    @Resource
    private IcbcGoodsConfigService goodsConfigService;

    @Override
    public WorkbenchOverviewRespVO getOverview() {
        WorkbenchReadinessRespVO readiness = buildReadiness();
        WorkbenchOverviewRespVO overview = new WorkbenchOverviewRespVO();
        overview.setTodos(buildTodos());
        overview.setReadiness(readiness);
        overview.setWarnings(buildWarnings(readiness));
        return overview;
    }

    // ==================== 八类待办 ====================

    private List<WorkbenchTodoRespVO> buildTodos() {
        List<WorkbenchTodoRespVO> todos = new ArrayList<>();
        for (WorkbenchTodoCodeEnum code : WorkbenchTodoCodeEnum.values()) {
            todos.add(buildTodo(code));
        }
        return todos;
    }

    private WorkbenchTodoRespVO buildTodo(WorkbenchTodoCodeEnum code) {
        WorkbenchTodoRespVO todo = new WorkbenchTodoRespVO();
        todo.setCode(code.getCode());
        todo.setName(code.getName());
        todo.setDefinition(code.getDefinition());
        TodoPreview preview = loadTodo(code);
        if (preview == null) {
            // 数据源尚未上线：标为「待接入」并写明原因，不伪造数字（见枚举类注释）
            todo.setAvailable(false);
            todo.setUnavailableReason(code.getUnavailableReason() != null
                    ? code.getUnavailableReason() : "该项取数逻辑尚未接入。");
            todo.setTotal(0L);
            todo.setItems(Collections.emptyList());
            return todo;
        }
        todo.setAvailable(true);
        todo.setTotal(preview.total);
        todo.setItems(preview.items);
        return todo;
    }

    /**
     * 取某项待办的条数与来源明细；返回 {@code null} 表示这一项的数据源还没上线。
     */
    private TodoPreview loadTodo(WorkbenchTodoCodeEnum code) {
        switch (code) {
            case ARRIVAL_TODAY:
                return arrivalToday();
            case PENDING_WEIGH:
                return pendingWeigh();
            case PENDING_INSPECT:
                return pendingInspection();
            case PENDING_STOCK_IN:
                return pendingStockIn();
            case PENDING_SETTLE_CONFIRM:
                return settlementByConfirmStatus(SettlementConfirmStatusEnum.PENDING);
            case SETTLE_DISPUTE:
                return settlementByConfirmStatus(SettlementConfirmStatusEnum.DISPUTED);
            case PAYMENT_FAILED:
                return paymentFailed();
            case INVOICE_FAILED:
                return invoiceFailed();
            case PAYMENT_PENDING_TIMEOUT:
                return pendingPaymentTimeout();
            default:
                // 八项待办都已接入；保留 default 以便将来新增枚举项时先落「待接入」
                return null;
        }
    }

    /** 待入库：已验收（已归入结算单）、未作废，且可入库实物量（接收量优先，无则净重）尚未全部入库的收购单。 */
    private TodoPreview pendingStockIn() {
        StockInPendingPageReqVO reqVO = new StockInPendingPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(PREVIEW_LIMIT);
        PageResult<StockInPendingRespVO> page = stockInService.getPendingPage(reqVO);
        List<WorkbenchItemRespVO> items = page.getList().stream().map(this::toItem)
                .collect(Collectors.toList());
        return TodoPreview.of(page.getTotal(), items);
    }

    private WorkbenchItemRespVO toItem(StockInPendingRespVO pending) {
        WorkbenchItemRespVO item = new WorkbenchItemRespVO();
        item.setId(pending.getAcquisitionId());
        item.setNo(pending.getAcquisitionNo());
        item.setTitle(firstNonBlank(pending.getSellerName(), "未留姓名"));
        item.setSubtitle(joinNonBlank(" · ", pending.getCategoryName(),
                "待入库 " + plainQuantity(pending.getRemainingQuantity(), pending.getUnit())));
        item.setTime(pending.getTradeTime());
        return item;
    }

    /** 数量文案：去掉多余的尾零；有单位就带上。 */
    private static String plainQuantity(BigDecimal quantity, String unit) {
        if (quantity == null) {
            return "0";
        }
        String text = quantity.stripTrailingZeros().toPlainString();
        return unit == null || unit.isBlank() ? text : text + unit;
    }

    /** 今日到场 / 上门：待到站的预约里，预计到站时间不晚于今日的（含已逾期）。 */
    private TodoPreview arrivalToday() {
        LocalDateTime deadline = LocalDate.now().atTime(LocalTime.MAX);
        long total = icbcAppointmentMapper.selectCountPendingArrivalBefore(deadline);
        List<WorkbenchItemRespVO> items = icbcAppointmentMapper
                .selectListPendingArrivalBefore(deadline, PREVIEW_LIMIT)
                .stream().map(this::toItem).collect(Collectors.toList());
        return TodoPreview.of(total, items);
    }

    private WorkbenchItemRespVO toItem(IcbcAppointmentDO appointment) {
        WorkbenchItemRespVO item = new WorkbenchItemRespVO();
        item.setId(appointment.getId());
        item.setNo(appointment.getAppointmentNo());
        item.setTitle(firstNonBlank(appointment.getSellerName(), appointment.getPlateNo(), "未留姓名"));
        item.setSubtitle(joinNonBlank(" · ", appointment.getStationName(),
                appointment.getCategoryName(), appointment.getPlateNo()));
        item.setStatusName(AppointmentStatusEnum.ofStatus(appointment.getStatus())
                .map(AppointmentStatusEnum::getName).orElse("未知"));
        item.setTime(appointment.getExpectedArrivalTime());
        return item;
    }

    /** 待称重：已登记但净重未录的收购单。 */
    private TodoPreview pendingWeigh() {
        long total = acquisitionMapper.selectCountPendingWeigh();
        List<WorkbenchItemRespVO> items = acquisitionMapper.selectListPendingWeigh(PREVIEW_LIMIT)
                .stream().map(acquisition -> toItem(acquisition, "未录磅重"))
                .collect(Collectors.toList());
        return TodoPreview.of(total, items);
    }

    /** 待验收：已录磅重、尚未归入结算单（现场还没「结束本次收货」）。 */
    private TodoPreview pendingInspection() {
        long total = acquisitionMapper.selectCountPendingInspection();
        List<WorkbenchItemRespVO> items = acquisitionMapper.selectListPendingInspection(PREVIEW_LIMIT)
                .stream().map(acquisition -> toItem(acquisition, "待结束本次收货"))
                .collect(Collectors.toList());
        return TodoPreview.of(total, items);
    }

    private WorkbenchItemRespVO toItem(IcbcAcquisitionDO acquisition, String statusName) {
        WorkbenchItemRespVO item = new WorkbenchItemRespVO();
        item.setId(acquisition.getId());
        item.setNo(acquisition.getAcquisitionNo());
        item.setTitle(firstNonBlank(acquisition.getSellerName(), acquisition.getVehiclePlateNo(), "未留姓名"));
        item.setSubtitle(joinNonBlank(" · ", acquisition.getCategoryName(), acquisition.getUnit()));
        item.setStatusName(statusName);
        item.setTime(acquisition.getTradeTime() != null ? acquisition.getTradeTime()
                : acquisition.getCreateTime());
        item.setAmount(acquisition.getAmount());
        return item;
    }

    /** 待结算确认 / 异议：处于给定确认状态的结算单。 */
    private TodoPreview settlementByConfirmStatus(SettlementConfirmStatusEnum confirmStatus) {
        long total = settlementMapper.selectCountByConfirmStatus(confirmStatus.getStatus());
        List<WorkbenchItemRespVO> items = settlementMapper
                .selectListByConfirmStatus(confirmStatus.getStatus(), PREVIEW_LIMIT)
                .stream().map(settlement -> toItem(settlement, confirmStatus))
                .collect(Collectors.toList());
        return TodoPreview.of(total, items);
    }

    private WorkbenchItemRespVO toItem(IcbcSettlementDO settlement, SettlementConfirmStatusEnum confirmStatus) {
        WorkbenchItemRespVO item = new WorkbenchItemRespVO();
        item.setId(settlement.getId());
        item.setNo(settlement.getSettlementNo());
        item.setTitle(firstNonBlank(settlement.getSellerName(), settlement.getSellerMobile(), "未留姓名"));
        item.setSubtitle(joinNonBlank(" · ", settlement.getStationName(),
                settlement.getCurrentVersionNo() == null ? null : "第 " + settlement.getCurrentVersionNo() + " 版",
                confirmStatus == SettlementConfirmStatusEnum.DISPUTED ? settlement.getDisputeNote() : null));
        item.setStatusName(confirmStatus.getName());
        item.setTime(settlement.getGenerateTime());
        return item;
    }

    /** 付款失败：异常状态的支付单（失败 / 关闭 / 冲正 / 退汇 / 他行已扣款 / 部分成功）。 */
    private TodoPreview paymentFailed() {
        long total = paymentOrderMapper.selectCountException();
        List<WorkbenchItemRespVO> items = paymentOrderMapper.selectListException(PREVIEW_LIMIT)
                .stream().map(this::toItem).collect(Collectors.toList());
        return TodoPreview.of(total, items);
    }

    private WorkbenchItemRespVO toItem(PaymentOrderDO payment) {
        WorkbenchItemRespVO item = new WorkbenchItemRespVO();
        item.setId(payment.getId());
        item.setNo(payment.getOrderNo());
        item.setTitle(firstNonBlank(payment.getPartnerOrderId(), payment.getPayeeNo(), "未知业务单"));
        item.setSubtitle(joinNonBlank(" · ", "收方 " + blankToDash(payment.getPayeeNo()),
                firstNonBlank(payment.getErrorMsg(), payment.getErrorCode())));
        item.setStatusName(PaymentStatusEnum.nameOf(payment.getPaymentStatus()));
        item.setTime(payment.getPaymentTime() != null ? payment.getPaymentTime() : payment.getUpdateTime());
        item.setAmount(payment.getPaymentAmount());
        return item;
    }

    /** 票务失败：开票 / 缴税 / 上传三条状态线任一异常。 */
    private TodoPreview invoiceFailed() {
        long total = invoiceOrderMapper.selectCountException();
        List<WorkbenchItemRespVO> items = invoiceOrderMapper.selectListException(PREVIEW_LIMIT)
                .stream().map(this::toItem).collect(Collectors.toList());
        return TodoPreview.of(total, items);
    }

    /**
     * 待付款超时（#106）：票备好了、钱一直没付。超时只提醒，不自动取消预开票。
     *
     * <p>取数在 {@code InvoiceOrderMapper} 里（按 preOrderTime + 配置天数），本项不落任何状态：
     * 「超时」是一个查出来的口径，不是一件被写下来的事实——免得它随时间变脏。
     */
    private TodoPreview pendingPaymentTimeout() {
        long total = invoiceOrderMapper.selectCountPendingPaymentOverdue(pendingPaymentDays);
        List<WorkbenchItemRespVO> items = invoiceOrderMapper
                .selectListPendingPaymentOverdue(pendingPaymentDays, PREVIEW_LIMIT)
                .stream().map(this::toPendingPaymentItem).collect(Collectors.toList());
        return TodoPreview.of(total, items);
    }

    private WorkbenchItemRespVO toPendingPaymentItem(InvoiceOrderDO invoice) {
        WorkbenchItemRespVO item = new WorkbenchItemRespVO();
        item.setId(invoice.getId());
        item.setNo(invoice.getOrderNo());
        item.setTitle(firstNonBlank(invoice.getPartnerOrderId(), invoice.getPayeeNo(), "未知业务单"));
        long days = invoice.getPreOrderTime() == null ? 0
                : ChronoUnit.DAYS.between(invoice.getPreOrderTime(), LocalDateTime.now());
        item.setSubtitle("预开票成功已 " + days + " 天未付款");
        item.setStatusName(PaymentStatusEnum.nameOf(invoice.getPaymentStatus()));
        item.setTime(invoice.getPreOrderTime());
        item.setAmount(invoice.getTotalAmount());
        return item;
    }

    private WorkbenchItemRespVO toItem(InvoiceOrderDO invoice) {
        WorkbenchItemRespVO item = new WorkbenchItemRespVO();
        item.setId(invoice.getId());
        item.setNo(invoice.getOrderNo());
        item.setTitle(firstNonBlank(invoice.getPartnerOrderId(), invoice.getPayeeNo(), "未知业务单"));
        item.setSubtitle(failedStatusLines(invoice));
        item.setStatusName(InvoiceIssueStatusEnum.nameOf(invoice.getInvoiceStatus()));
        item.setTime(invoice.getUpdateTime());
        item.setAmount(invoice.getTotalAmount());
        return item;
    }

    /**
     * 一张票可能有不止一条状态线异常，逐条列出「哪条线出了什么问题」。
     * 异常判定复用各枚举自己的 {@code isException} / {@code exceptionStatuses}，不在工作台重复一套规则。
     */
    private String failedStatusLines(InvoiceOrderDO invoice) {
        List<String> lines = new ArrayList<>();
        if (PreInvoiceStatusEnum.isException(invoice.getPreInvoiceStatus())) {
            lines.add(PreInvoiceStatusEnum.FAILED.getName());
        }
        if (InvoiceIssueStatusEnum.isException(invoice.getInvoiceStatus())) {
            lines.add("开票失败");
        }
        if (TaxStatusEnum.isException(invoice.getTaxStatus())) {
            lines.add(TaxStatusEnum.nameOf(invoice.getTaxStatus()));
        }
        if (UploadStatusEnum.isException(invoice.getUploadStatus())) {
            lines.add(UploadStatusEnum.nameOf(invoice.getUploadStatus()));
        }
        return String.join(" / ", lines);
    }

    // ==================== 三条预警 ====================

    private List<WorkbenchWarningRespVO> buildWarnings(WorkbenchReadinessRespVO readiness) {
        List<WorkbenchWarningRespVO> warnings = new ArrayList<>();
        warnings.add(quotaWarning());
        warnings.add(qualificationExpiryWarning());
        warnings.add(readinessWarning(readiness));
        return warnings;
    }

    /** 额度预警：本租户尚未办结的「超 500 万、需办理经营主体登记」引导记录。 */
    private WorkbenchWarningRespVO quotaWarning() {
        long total = quotaGuidanceMapper.selectCountOpen();
        WorkbenchWarningRespVO warning = new WorkbenchWarningRespVO();
        warning.setCode("QUOTA");
        warning.setName("额度");
        warning.setCount(total);
        warning.setLevel(total > 0 ? LEVEL_WARN : LEVEL_OK);
        warning.setMessage(total > 0
                ? total + " 名出售者连续 12 个月累计销售额已超 500 万，需引导其办理经营主体登记；办结前不能再为其反向开票。"
                : "本企业名下没有累计销售额超 500 万的出售者。");
        warning.setItems(quotaGuidanceMapper.selectListOpen(PREVIEW_LIMIT)
                .stream().map(this::toItem).collect(Collectors.toList()));
        return warning;
    }

    private WorkbenchItemRespVO toItem(SellerQuotaGuidanceDO guidance) {
        WorkbenchItemRespVO item = new WorkbenchItemRespVO();
        item.setId(guidance.getId());
        item.setTitle(firstNonBlank(guidance.getSellerName(), "未留姓名"));
        item.setSubtitle(joinNonBlank(" · ", MaskUtils.maskIdCard(guidance.getIdCardNo()),
                SellerQuotaGuidanceStatusEnum.nameOf(guidance.getStatus()),
                guidance.getTriggerBizNo()));
        item.setStatusName(SellerQuotaGuidanceStatusEnum.nameOf(guidance.getStatus()));
        item.setTime(guidance.getTriggeredAt());
        item.setAmount(guidance.getUsedAmount());
        return item;
    }

    /** 资质到期预警：待处理的到期预警（由定时任务扫描临近到期的资质落库）。 */
    private WorkbenchWarningRespVO qualificationExpiryWarning() {
        long total = expiryWarningMapper.selectCountOpen();
        WorkbenchWarningRespVO warning = new WorkbenchWarningRespVO();
        warning.setCode("QUALIFICATION_EXPIRY");
        warning.setName("资质到期");
        warning.setCount(total);
        warning.setLevel(total > 0 ? LEVEL_WARN : LEVEL_OK);
        warning.setMessage(total > 0
                ? total + " 项资质临近到期；到期后任一层失效即冻结开票，请尽快更新并重传。"
                : "没有临近到期的资质。");
        warning.setItems(expiryWarningMapper.selectListOpen(PREVIEW_LIMIT)
                .stream().map(this::toItem).collect(Collectors.toList()));
        return warning;
    }

    private WorkbenchItemRespVO toItem(IcbcExpiryWarningDO expiryWarning) {
        WorkbenchItemRespVO item = new WorkbenchItemRespVO();
        item.setId(expiryWarning.getId());
        item.setNo(expiryWarning.getType());
        item.setTitle(firstNonBlank(expiryWarning.getName(), expiryWarning.getType(), "未命名资质"));
        item.setSubtitle("有效期止 " + (expiryWarning.getValidTo() == null ? "-" : expiryWarning.getValidTo()));
        item.setStatusName(EXPIRY_WARNING_OPEN.equals(expiryWarning.getStatus()) ? "待处理" : "已处理");
        item.setTime(expiryWarning.getValidTo() == null ? null : expiryWarning.getValidTo().atStartOfDay());
        return item;
    }

    /**
     * 开票就绪预警：就绪时是 OK（提醒「已就绪」不算噪音），未就绪时列出缺哪几项。
     * 它同时是工作台顶部的徽标内容，见 {@link #buildReadiness()}。
     */
    private WorkbenchWarningRespVO readinessWarning(WorkbenchReadinessRespVO readiness) {
        List<String> missing = readiness.getItems().stream()
                .filter(item -> !item.isReady())
                .map(WorkbenchReadinessItemRespVO::getName)
                .collect(Collectors.toList());
        WorkbenchWarningRespVO warning = new WorkbenchWarningRespVO();
        warning.setCode("READINESS");
        warning.setName("开票就绪");
        warning.setCount(missing.size());
        warning.setLevel(missing.isEmpty() ? LEVEL_OK : LEVEL_DANGER);
        warning.setMessage(missing.isEmpty()
                ? "开票就绪：三层资质、企业授权、付方档案与编码配置都已具备。"
                : "开票未就绪，待补齐：" + String.join("、", missing) + "。任一层资质失效都会冻结开票。");
        warning.setItems(readiness.getItems().stream()
                .filter(item -> !item.isReady())
                .map(item -> {
                    WorkbenchItemRespVO respVO = new WorkbenchItemRespVO();
                    respVO.setNo(item.getCode());
                    respVO.setTitle(item.getName());
                    respVO.setSubtitle(item.getMessage());
                    respVO.setStatusName("待补齐");
                    return respVO;
                }).collect(Collectors.toList()));
        return warning;
    }

    // ==================== 开票就绪徽标 ====================

    /**
     * 就绪检查只取**本地库能判定**的部分：三层资质、企业授权、付方（子商户）档案、品类编码配置。
     * 适配层连通性要打网络，不放进工作台首屏（它在「开票就绪自检」页按需自检）。
     */
    private WorkbenchReadinessRespVO buildReadiness() {
        List<WorkbenchReadinessItemRespVO> items = new ArrayList<>();

        boolean qualificationReady = qualificationService.isTenantReady();
        items.add(readinessItem("QUALIFICATION", "三层资质齐全有效", qualificationReady,
                qualificationReady ? "三层资质都在有效期内。"
                        : "任一层缺失或已过期都会冻结开票，请到「三层资质」补齐并更新有效期。"));

        boolean authReady = enterpriseAuthMapper.selectCountByAuthStatus(ENTERPRISE_AUTH_APPROVED) > 0;
        items.add(readinessItem("ENTERPRISE_AUTH", "企业授权已生效", authReady,
                authReady ? "工行企业授权已生效。"
                        : "尚未有生效的企业授权，需法定代表人或财务负责人完成工行授权并回填有效期。"));

        boolean payerReady = payerInfoMapper.selectCount() > 0;
        items.add(readinessItem("PAYER", "付方（子商户）档案已配置", payerReady,
                payerReady ? "付方档案已配置。"
                        : "未配置付方档案时无法发起开票与付款，请到「付方档案」建档。"));

        boolean goodsReady = !goodsConfigService.getEnabledList().isEmpty();
        items.add(readinessItem("GOODS_CONFIG", "品类编码配置已启用", goodsReady,
                goodsReady ? "已有启用的品类与税收分类编码。"
                        : "没有启用的品类，开票申请带不出税率与税收分类编码，请到「编码配置」维护。"));

        WorkbenchReadinessRespVO readiness = new WorkbenchReadinessRespVO();
        readiness.setItems(items);
        readiness.setReady(items.stream().allMatch(WorkbenchReadinessItemRespVO::isReady));
        return readiness;
    }

    private WorkbenchReadinessItemRespVO readinessItem(String code, String name, boolean ready, String message) {
        WorkbenchReadinessItemRespVO item = new WorkbenchReadinessItemRespVO();
        item.setCode(code);
        item.setName(name);
        item.setReady(ready);
        item.setMessage(message);
        return item;
    }

    // ==================== 小工具 ====================

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value;
            }
        }
        return null;
    }

    private static String joinNonBlank(String delimiter, String... values) {
        List<String> kept = new ArrayList<>();
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                kept.add(value);
            }
        }
        return String.join(delimiter, kept);
    }

    private static String blankToDash(String value) {
        return value == null || value.trim().isEmpty() ? "-" : value;
    }

    /**
     * 一项待办的条数与来源明细。
     */
    private static final class TodoPreview {

        private final long total;
        private final List<WorkbenchItemRespVO> items;

        private TodoPreview(long total, List<WorkbenchItemRespVO> items) {
            this.total = total;
            this.items = items;
        }

        private static TodoPreview of(long total, List<WorkbenchItemRespVO> items) {
            return new TodoPreview(total, items);
        }

    }

}
