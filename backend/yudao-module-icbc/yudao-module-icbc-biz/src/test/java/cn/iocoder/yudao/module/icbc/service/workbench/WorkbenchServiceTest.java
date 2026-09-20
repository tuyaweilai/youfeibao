package cn.iocoder.yudao.module.icbc.service.workbench;

import org.springframework.boot.test.mock.mockito.MockBean;
import cn.iocoder.yudao.module.erp.api.stock.StockApi;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.icbc.UnitTestConfiguration;
import cn.iocoder.yudao.module.icbc.controller.admin.workbench.vo.WorkbenchItemRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.workbench.vo.WorkbenchOverviewRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.workbench.vo.WorkbenchReadinessItemRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.workbench.vo.WorkbenchTodoRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.workbench.vo.WorkbenchWarningRespVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.acquisition.IcbcAcquisitionDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.appointment.IcbcAppointmentDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.entauth.IcbcEnterpriseAuthDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.goodscfg.IcbcGoodsConfigDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.invoice.InvoiceOrderDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payer.PayerInfoDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payment.PaymentOrderDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.qualification.IcbcQualificationDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.quota.SellerQuotaGuidanceDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.settlement.IcbcSettlementDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.warning.IcbcExpiryWarningDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.acquisition.IcbcAcquisitionMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.appointment.IcbcAppointmentMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.entauth.IcbcEnterpriseAuthMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.goodscfg.IcbcGoodsConfigMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.invoice.InvoiceOrderMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.payer.PayerInfoMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.payment.PaymentOrderMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.qualification.IcbcQualificationMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.quota.SellerQuotaGuidanceMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.settlement.IcbcSettlementMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.warning.IcbcExpiryWarningMapper;
import cn.iocoder.yudao.module.icbc.enums.AcquisitionStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.AppointmentStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.InvoiceIssueStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.PaymentStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.PreInvoiceStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.SellerQuotaGuidanceStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.SettlementConfirmStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.TaxStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.UploadStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.WorkbenchTodoCodeEnum;
import cn.iocoder.yudao.module.icbc.service.goodscfg.impl.IcbcGoodsConfigServiceImpl;
import cn.iocoder.yudao.module.icbc.service.qualification.impl.IcbcQualificationServiceImpl;
import cn.iocoder.yudao.module.icbc.service.workbench.impl.WorkbenchServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link WorkbenchServiceImpl} 的单元测试（#56 T18）。
 *
 * <p>守三件事：八类待办都在且顺序固定、每个数字都能落到来源明细、拿不到数据源的项
 * （待入库）必须显式标注为「待接入」而不是伪造一个 0 之外的数字。
 */
@Import({WorkbenchServiceImpl.class, IcbcQualificationServiceImpl.class,
        IcbcGoodsConfigServiceImpl.class, UnitTestConfiguration.class})
@Sql(scripts = "/sql/create_tables.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Transactional
@Rollback
public class WorkbenchServiceTest extends BaseDbUnitTest {

    /** 库存域只通过 erp-api 的 StockApi 接入（#52）；单元测试不跨模块，用 Mock。 */
    @MockBean
    private StockApi stockApi;

    @Resource
    private WorkbenchService workbenchService;
    @Resource
    private IcbcAppointmentMapper appointmentMapper;
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
    private IcbcQualificationMapper qualificationMapper;
    @Resource
    private PayerInfoMapper payerInfoMapper;
    @Resource
    private IcbcGoodsConfigMapper goodsConfigMapper;

    // ==================== 八类待办的骨架 ====================

    @Test
    public void testOverview_hasAllEightTodosInFixedOrderWithDefinition() {
        WorkbenchOverviewRespVO overview = workbenchService.getOverview();

        assertEquals(8, overview.getTodos().size());
        assertEquals(Arrays.stream(WorkbenchTodoCodeEnum.values()).map(WorkbenchTodoCodeEnum::getCode)
                .collect(Collectors.toList()),
                overview.getTodos().stream().map(WorkbenchTodoRespVO::getCode).collect(Collectors.toList()));
        for (WorkbenchTodoRespVO todo : overview.getTodos()) {
            assertTrue(todo.getDefinition() != null && !todo.getDefinition().isEmpty(),
                    "每项待办都要带口径：" + todo.getCode());
            assertNotNull(todo.getName());
            assertNotNull(todo.getItems());
        }
    }

    @Test
    public void testPendingStockIn_countsAcceptedNotYetStockedIn() {
        // 已验收（已归入结算单）、未作废、可入库实物量大于 0 → 待入库
        insertAcquisition("ACQ_ACCEPTED", new BigDecimal("10.0000"), 9001L,
                AcquisitionStatusEnum.REGISTERED.getStatus());
        // 未验收的属于「待验收」，不算待入库
        insertAcquisition("ACQ_NOT_ACCEPTED", new BigDecimal("10.0000"), null,
                AcquisitionStatusEnum.REGISTERED.getStatus());
        // 已作废的不算
        insertAcquisition("ACQ_CANCELLED_STOCK", new BigDecimal("10.0000"), 9002L,
                AcquisitionStatusEnum.CANCELLED.getStatus());

        WorkbenchTodoRespVO todo = todo(workbenchService.getOverview(), "PENDING_STOCK_IN");

        assertTrue(todo.isAvailable());
        assertEquals(1L, todo.getTotal());
        assertEquals("ACQ_ACCEPTED", todo.getItems().get(0).getNo());
    }

    // ==================== 现场侧：今日到场 / 待称重 / 待验收 ====================

    @Test
    public void testArrivalToday_countsPendingUpToTodayIncludingOverdue() {
        insertAppointment("AP_TODAY", LocalDateTime.now().withHour(10).withMinute(0), 0);
        insertAppointment("AP_OVERDUE", LocalDateTime.now().minusDays(1), 0);
        insertAppointment("AP_TOMORROW", LocalDateTime.now().plusDays(1), 0);
        insertAppointment("AP_ARRIVED", LocalDateTime.now().withHour(9).withMinute(0),
                AppointmentStatusEnum.ARRIVED.getStatus());
        insertAppointment("AP_CANCELLED", LocalDateTime.now(), AppointmentStatusEnum.CANCELLED.getStatus());

        WorkbenchTodoRespVO todo = todo(workbenchService.getOverview(), "ARRIVAL_TODAY");

        assertTrue(todo.isAvailable());
        assertEquals(2L, todo.getTotal());
        // 逾期未处理的排在最前面（越早越该先接待）
        assertEquals("AP_OVERDUE", todo.getItems().get(0).getNo());
        assertEquals("AP_TODAY", todo.getItems().get(1).getNo());
        assertEquals("待到站", todo.getItems().get(0).getStatusName());
    }

    @Test
    public void testPendingWeigh_countsOnlyAcquisitionsWithoutWeight() {
        insertAcquisition("ACQ_NO_WEIGHT", null, null, AcquisitionStatusEnum.REGISTERED.getStatus());
        insertAcquisition("ACQ_WEIGHED", new BigDecimal("10.0000"), null,
                AcquisitionStatusEnum.REGISTERED.getStatus());
        insertAcquisition("ACQ_CANCELLED", null, null, AcquisitionStatusEnum.CANCELLED.getStatus());

        WorkbenchTodoRespVO todo = todo(workbenchService.getOverview(), "PENDING_WEIGH");

        assertEquals(1L, todo.getTotal());
        assertEquals("ACQ_NO_WEIGHT", todo.getItems().get(0).getNo());
        assertEquals("张三", todo.getItems().get(0).getTitle());
        assertEquals("未录磅重", todo.getItems().get(0).getStatusName());
    }

    @Test
    public void testPendingInspection_countsWeighedButNotYetGrouped() {
        insertAcquisition("ACQ_WEIGHED_UNGROUPED", new BigDecimal("10.0000"), null,
                AcquisitionStatusEnum.REGISTERED.getStatus());
        // 未过磅的属于「待称重」，不算待验收
        insertAcquisition("ACQ_NO_WEIGHT", null, null, AcquisitionStatusEnum.REGISTERED.getStatus());
        // 已归入结算单的属于「待结算确认」，也不算待验收
        insertAcquisition("ACQ_GROUPED", new BigDecimal("10.0000"), 9001L,
                AcquisitionStatusEnum.REGISTERED.getStatus());

        WorkbenchTodoRespVO todo = todo(workbenchService.getOverview(), "PENDING_INSPECT");

        assertEquals(1L, todo.getTotal());
        assertEquals("ACQ_WEIGHED_UNGROUPED", todo.getItems().get(0).getNo());
    }

    // ==================== 结算侧：待确认与异议 ====================

    @Test
    public void testSettlementTodos_splitByConfirmStatus() {
        LocalDateTime now = LocalDateTime.now();
        insertSettlement("ST_PENDING_OLD", SettlementConfirmStatusEnum.PENDING, 1, now.minusDays(2));
        insertSettlement("ST_PENDING_NEW", SettlementConfirmStatusEnum.PENDING, 2, now.minusMinutes(5));
        insertSettlement("ST_DISPUTED", SettlementConfirmStatusEnum.DISPUTED, 1, now);
        insertSettlement("ST_CONFIRMED", SettlementConfirmStatusEnum.CONFIRMED, 1, now);

        WorkbenchOverviewRespVO overview = workbenchService.getOverview();

        WorkbenchTodoRespVO pending = todo(overview, "PENDING_SETTLE_CONFIRM");
        assertEquals(2L, pending.getTotal());
        // 等待最久的排在最前面
        assertEquals("ST_PENDING_OLD", pending.getItems().get(0).getNo());
        assertEquals("待确认", pending.getItems().get(0).getStatusName());
        assertTrue(pending.getItems().get(0).getSubtitle().contains("第 1 版"));

        WorkbenchTodoRespVO dispute = todo(overview, "SETTLE_DISPUTE");
        assertEquals(1L, dispute.getTotal());
        assertEquals("ST_DISPUTED", dispute.getItems().get(0).getNo());
        assertEquals("有异议", dispute.getItems().get(0).getStatusName());
    }

    // ==================== 财务侧：付款失败与票务失败 ====================

    @Test
    public void testPaymentFailed_countsExceptionStatusesOnly() {
        insertPayment("PAY_OK", PaymentStatusEnum.SUCCESS);
        insertPayment("PAY_PAYING", PaymentStatusEnum.PAYING);
        insertPayment("PAY_FAILED", PaymentStatusEnum.FAILED);
        insertPayment("PAY_PARTIAL", PaymentStatusEnum.PARTIAL_SUCCESS);

        WorkbenchTodoRespVO todo = todo(workbenchService.getOverview(), "PAYMENT_FAILED");

        assertEquals(2L, todo.getTotal());
        List<String> statusNames = todo.getItems().stream().map(WorkbenchItemRespVO::getStatusName)
                .collect(Collectors.toList());
        assertTrue(statusNames.contains("部分成功"));
        assertTrue(statusNames.contains("支付失败"));
    }

    @Test
    public void testInvoiceFailed_countsAnyFailedLineOnce() {
        insertInvoice("INV_PRE_ORDER_FAILED", PreInvoiceStatusEnum.FAILED.getStatus(),
                InvoiceIssueStatusEnum.NOT_ISSUED.getStatus(), TaxStatusEnum.NOT_TAXED.getStatus(),
                UploadStatusEnum.NOT_UPLOADED.getStatus());
        insertInvoice("INV_INVOICE_FAILED", PreInvoiceStatusEnum.SUCCESS.getStatus(),
                InvoiceIssueStatusEnum.FAILED.getStatus(), TaxStatusEnum.NOT_TAXED.getStatus(),
                UploadStatusEnum.SUCCESS.getStatus());
        insertInvoice("INV_TAX_ABNORMAL", PreInvoiceStatusEnum.SUCCESS.getStatus(),
                InvoiceIssueStatusEnum.ISSUED.getStatus(), TaxStatusEnum.ABNORMAL_AMOUNT.getStatus(),
                UploadStatusEnum.SUCCESS.getStatus());
        insertInvoice("INV_UPLOAD_FAILED", PreInvoiceStatusEnum.SUCCESS.getStatus(),
                InvoiceIssueStatusEnum.ISSUED.getStatus(), TaxStatusEnum.SUCCESS.getStatus(),
                UploadStatusEnum.FAILED.getStatus());
        // 多条线同时异常只算一条待办
        insertInvoice("INV_TWO_LINES", PreInvoiceStatusEnum.FAILED.getStatus(),
                InvoiceIssueStatusEnum.FAILED.getStatus(), TaxStatusEnum.FAILED.getStatus(),
                UploadStatusEnum.FAILED.getStatus());
        insertInvoice("INV_HEALTHY", PreInvoiceStatusEnum.SUCCESS.getStatus(),
                InvoiceIssueStatusEnum.ISSUED.getStatus(), TaxStatusEnum.SUCCESS.getStatus(),
                UploadStatusEnum.SUCCESS.getStatus());

        WorkbenchTodoRespVO todo = todo(workbenchService.getOverview(), "INVOICE_FAILED");

        assertEquals(5L, todo.getTotal());
        WorkbenchItemRespVO twoLines = todo.getItems().stream()
                .filter(item -> "INV_TWO_LINES".equals(item.getNo())).findFirst().orElseThrow(AssertionError::new);
        assertEquals("预开票失败 / 开票失败 / 缴税失败 / 上传失败", twoLines.getSubtitle());
    }

    // ==================== 预警与开票就绪 ====================

    @Test
    public void testWarnings_quotaCountsOnlyOpenGuidance() {
        insertQuotaGuidance(SellerQuotaGuidanceStatusEnum.PENDING);
        insertQuotaGuidance(SellerQuotaGuidanceStatusEnum.INFORMED);
        insertQuotaGuidance(SellerQuotaGuidanceStatusEnum.RESOLVED);

        WorkbenchWarningRespVO warning = warning(workbenchService.getOverview(), "QUOTA");

        assertEquals(2L, warning.getCount());
        assertEquals("WARN", warning.getLevel());
        // 身份证在来源明细里脱敏
        assertTrue(warning.getItems().get(0).getSubtitle().contains("110101********1234"));
    }

    @Test
    public void testWarnings_qualificationExpiryCountsOpenOnly() {
        insertExpiryWarning(0);
        insertExpiryWarning(1);

        WorkbenchWarningRespVO warning = warning(workbenchService.getOverview(), "QUALIFICATION_EXPIRY");

        assertEquals(1L, warning.getCount());
        assertEquals("WARN", warning.getLevel());
        assertEquals("营业执照", warning.getItems().get(0).getTitle());
    }

    @Test
    public void testReadiness_notReadyThenReady() {
        WorkbenchOverviewRespVO notReady = workbenchService.getOverview();
        assertFalse(notReady.getReadiness().isReady());
        assertEquals(4, notReady.getReadiness().getItems().size());
        assertTrue(notReady.getReadiness().getItems().stream().noneMatch(WorkbenchReadinessItemRespVO::isReady));
        // 未就绪时预警里能看到缺哪几项
        WorkbenchWarningRespVO warning = warning(notReady, "READINESS");
        assertEquals("DANGER", warning.getLevel());
        assertEquals(4L, warning.getCount());
        assertTrue(warning.getMessage().contains("三层资质齐全有效"));

        insertQualification("TAX");
        insertQualification("INDUSTRY");
        insertQualification("PUBLIC_SECURITY");
        insertApprovedEnterpriseAuth();
        insertPayer();
        insertEnabledGoodsConfig();

        WorkbenchOverviewRespVO ready = workbenchService.getOverview();
        assertTrue(ready.getReadiness().isReady());
        assertTrue(ready.getReadiness().getItems().stream().allMatch(WorkbenchReadinessItemRespVO::isReady));
        assertEquals("OK", warning(ready, "READINESS").getLevel());
    }

    @Test
    public void testReadiness_qualificationMissingOneLayerIsNotReady() {
        insertQualification("TAX");
        insertQualification("INDUSTRY");

        WorkbenchReadinessItemRespVO item = workbenchService.getOverview().getReadiness().getItems().stream()
                .filter(it -> "QUALIFICATION".equals(it.getCode())).findFirst().orElseThrow(AssertionError::new);

        assertFalse(item.isReady());
        assertTrue(item.getMessage().contains("三层资质"));
    }

    // ==================== 明细上限 ====================

    @Test
    public void testTodoPreview_cappedAtTenWhileTotalKeepsGrowing() {
        for (int i = 0; i < 12; i++) {
            insertAppointment("AP_" + i, LocalDateTime.now().withHour(6).withMinute(i), 0);
        }

        WorkbenchTodoRespVO todo = todo(workbenchService.getOverview(), "ARRIVAL_TODAY");

        // 总数照实报，界面上只摆 10 条，其余到模块列表看
        assertEquals(12L, todo.getTotal());
        assertEquals(10, todo.getItems().size());
    }

    // ==================== helpers ====================

    private static WorkbenchTodoRespVO todo(WorkbenchOverviewRespVO overview, String code) {
        return overview.getTodos().stream().filter(item -> code.equals(item.getCode()))
                .findFirst().orElseThrow(() -> new AssertionError("没有这项待办：" + code));
    }

    private static WorkbenchWarningRespVO warning(WorkbenchOverviewRespVO overview, String code) {
        return overview.getWarnings().stream().filter(item -> code.equals(item.getCode()))
                .findFirst().orElseThrow(() -> new AssertionError("没有这项预警：" + code));
    }

    private void insertAppointment(String no, LocalDateTime expectedArrivalTime, Integer status) {
        IcbcAppointmentDO appointment = IcbcAppointmentDO.builder()
                .appointmentNo(no)
                .naturalPersonId(1L)
                .sellerName("李四")
                .stationId(7L)
                .stationCode("STATION_7")
                .stationName("城东场站")
                .goodsConfigId(11L)
                .categoryName("废钢")
                .unit("吨")
                .plateNo("苏A12345")
                .expectedArrivalTime(expectedArrivalTime)
                .status(status)
                .build();
        appointmentMapper.insert(appointment);
    }

    private void insertAcquisition(String no, BigDecimal netWeight, Long settlementId, Integer status) {
        IcbcAcquisitionDO acquisition = IcbcAcquisitionDO.builder()
                .acquisitionNo(no)
                .clientRequestId("CLIENT_" + no)
                .payeeId(100L)
                .partnerPayeeId("PARTNER_PAYEE")
                .sellerName("张三")
                .sellerMobile("13800138000")
                .goodsConfigId(11L)
                .categoryName("废钢")
                .unit("吨")
                .taxRate(new BigDecimal("0.01"))
                .taxMethod("SIMPLE")
                .mergedCode("1090101010000000000")
                .quantity(new BigDecimal("1"))
                .unitPrice(new BigDecimal("100.00"))
                .amount(new BigDecimal("1000.00"))
                .grossWeight(new BigDecimal("12.0000"))
                .tareWeight(new BigDecimal("2.0000"))
                .netWeight(netWeight)
                .deduction(BigDecimal.ZERO)
                .deductionMethod("WEIGHT")
                .settlementWeight(netWeight)
                .settlementId(settlementId)
                .tradeTime(LocalDateTime.now())
                .status(status)
                .build();
        acquisitionMapper.insert(acquisition);
    }

    private void insertSettlement(String no, SettlementConfirmStatusEnum confirmStatus, int versionNo,
                                 LocalDateTime generateTime) {
        IcbcSettlementDO settlement = IcbcSettlementDO.builder()
                .settlementNo(no)
                .payeeId(100L)
                .naturalPersonId(1L)
                .sellerName("张三")
                .sellerMobile("13800138000")
                .stationId(7L)
                .stationName("城东场站")
                .generateTime(generateTime)
                .currentVersionNo(versionNo)
                .confirmStatus(confirmStatus.getStatus())
                .disputeCount(0)
                .build();
        settlementMapper.insert(settlement);
    }

    private void insertPayment(String orderNo, PaymentStatusEnum status) {
        PaymentOrderDO payment = PaymentOrderDO.builder()
                .orderNo(orderNo)
                .partnerOrderId("ACQ_" + orderNo)
                .payeeNo("PAYEE_NO_1")
                .paymentAmount(new BigDecimal("1000.00"))
                .paymentStatus(status.getStatus())
                .retryCount(0)
                .build();
        paymentOrderMapper.insert(payment);
    }

    private void insertInvoice(String orderNo, Integer preInvoiceStatus, Integer invoiceStatus,
                              Integer taxStatus, Integer uploadStatus) {
        InvoiceOrderDO invoice = InvoiceOrderDO.builder()
                .orderNo(orderNo)
                .partnerOrderId("ACQ_" + orderNo)
                .payeeNo("PAYEE_NO_1")
                .totalAmount(new BigDecimal("1000.00"))
                .businessType("SCRAP")
                .orderStatus(0)
                .invoiceStatus(invoiceStatus)
                .paymentStatus(0)
                .taxStatus(taxStatus)
                .uploadStatus(uploadStatus)
                .confirmStatus(0)
                .preInvoiceStatus(preInvoiceStatus)
                .build();
        invoiceOrderMapper.insert(invoice);
    }

    private void insertQuotaGuidance(SellerQuotaGuidanceStatusEnum status) {
        SellerQuotaGuidanceDO guidance = new SellerQuotaGuidanceDO();
        guidance.setPayeeId(100L);
        guidance.setSellerName("张三");
        guidance.setIdCardNo("110101199001011234");
        guidance.setTriggerScene("INVOICE_APPLY");
        guidance.setTriggerBizNo("ACQ_1");
        guidance.setUsedAmount(new BigDecimal("5000000.00"));
        guidance.setCapAmount(new BigDecimal("5000000.00"));
        guidance.setStatus(status.getStatus());
        guidance.setTriggeredAt(LocalDateTime.now());
        quotaGuidanceMapper.insert(guidance);
    }

    private void insertExpiryWarning(Integer status) {
        IcbcExpiryWarningDO warning = new IcbcExpiryWarningDO();
        warning.setQualificationId(1L);
        warning.setType("TAX");
        warning.setName("营业执照");
        warning.setValidTo(LocalDate.now().plusDays(10));
        warning.setStatus(status);
        warning.setWarnedAt(LocalDateTime.now());
        expiryWarningMapper.insert(warning);
    }

    private void insertQualification(String type) {
        IcbcQualificationDO qualification = new IcbcQualificationDO();
        qualification.setType(type);
        qualification.setName(type + " 资质");
        qualification.setValidFrom(LocalDate.now().minusDays(10));
        qualification.setValidTo(LocalDate.now().plusDays(100));
        qualification.setStatus(1);
        qualificationMapper.insert(qualification);
    }

    private void insertApprovedEnterpriseAuth() {
        IcbcEnterpriseAuthDO auth = new IcbcEnterpriseAuthDO();
        auth.setOutVendorId("VENDOR_1");
        auth.setSiteType("01");
        auth.setUserType("01");
        auth.setAuthStatus(1);
        auth.setAuthTime(LocalDateTime.now());
        enterpriseAuthMapper.insert(auth);
    }

    private void insertPayer() {
        PayerInfoDO payer = new PayerInfoDO();
        payer.setName("某某再生资源有限公司");
        payer.setCreditCode("91320100MA1XXXXXXX");
        payer.setTaxNo("91320100MA1XXXXXXX");
        payer.setStatus(1);
        payerInfoMapper.insert(payer);
    }

    private void insertEnabledGoodsConfig() {
        IcbcGoodsConfigDO goods = new IcbcGoodsConfigDO();
        goods.setName("废钢 M2");
        goods.setUnit("吨");
        goods.setTaxRate(new BigDecimal("0.01"));
        goods.setTaxMethod("SIMPLE");
        goods.setMergedCode("1090101010000000000");
        goods.setStatus(0);
        goodsConfigMapper.insert(goods);
    }

}
