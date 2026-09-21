package cn.iocoder.yudao.module.icbc.service.tax;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.icbc.UnitTestConfiguration;
import cn.iocoder.yudao.module.icbc.controller.admin.tax.vo.*;
import cn.iocoder.yudao.module.icbc.dal.dataobject.invoice.InvoiceOrderDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.invoice.RedInvoiceDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payee.PayeeInfoDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.tax.TaxDeclarationDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.tax.TaxSupplementDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.invoice.InvoiceOrderMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.invoice.RedInvoiceMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.payee.PayeeInfoMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.tax.TaxDeclarationItemMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.tax.TaxDeclarationMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.tax.TaxSupplementMapper;
import cn.iocoder.yudao.module.icbc.enums.*;
import cn.iocoder.yudao.module.icbc.service.quota.impl.NaturalPersonQuotaServiceImpl;
import cn.iocoder.yudao.module.icbc.service.tax.impl.TaxDeclarationServiceImpl;
import cn.iocoder.yudao.module.icbc.service.tax.impl.TaxSupplementServiceImpl;
import cn.iocoder.yudao.test.icbc.IcbcTenantTestConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link TaxDeclarationServiceImpl} 的单元测试（issue #13）。
 *
 * <p>断言的是代办税费申报的外部可观察行为：按月聚合、合计金额、10 万元免征线与
 * 1% / 3% 分列、缺项检查、申报缴款状态机、缴款凭证与发票关联、申报期与暂停资格预警。
 */
@Import({TaxDeclarationServiceImpl.class, TaxSupplementServiceImpl.class,
        NaturalPersonQuotaServiceImpl.class, UnitTestConfiguration.class, IcbcTenantTestConfiguration.class})
@Sql(scripts = "/sql/create_tables.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Transactional
public class TaxDeclarationServiceImplTest extends BaseDbUnitTest {

    private static final Long TENANT_ID = 1L;
    private static final Long OTHER_TENANT_ID = 2L;
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    @Resource
    private TaxDeclarationService taxDeclarationService;
    @Resource
    private PayeeInfoMapper payeeInfoMapper;
    @Resource
    private InvoiceOrderMapper invoiceOrderMapper;
    @Resource
    private RedInvoiceMapper redInvoiceMapper;
    @Resource
    private TaxDeclarationMapper declarationMapper;
    @Resource
    private TaxDeclarationItemMapper itemMapper;
    @Resource
    private TaxSupplementMapper supplementMapper;

    @BeforeEach
    public void setUp() {
        TenantContextHolder.setTenantId(TENANT_ID);
    }

    @AfterEach
    public void tearDown() {
        TenantContextHolder.clear();
    }

    // ==================== 按月聚合与合计金额 ====================

    @Test
    public void testGenerate_computesVatSurchargeAndIit() {
        PayeeInfoDO payee = insertPayee(TENANT_ID, "张三", "110101199001011234", "13800000001");
        String month = lastMonth();
        insertBlue(TENANT_ID, "ORDER_A", payee, new BigDecimal("120000.00"), month, "0.01",
                InvoiceIssueStatusEnum.ISSUED, TaxStatusEnum.SUCCESS);

        TaxDeclarationRespVO declaration = taxDeclarationService.generate(month);

        assertEquals(month, declaration.getPeriodMonth());
        assertEquals(1, declaration.getSellerCount());
        assertEquals(0, new BigDecimal("120000.00").compareTo(declaration.getTotalSalesAmount()));
        // 增值税 120000 × 1% = 1200；附加税费 1200 × 6% = 72；个税 120000 × 0.5% = 600
        assertEquals(0, new BigDecimal("1200.00").compareTo(declaration.getVatAmount()));
        assertEquals(0, new BigDecimal("72.00").compareTo(declaration.getSurchargeAmount()));
        assertEquals(0, new BigDecimal("600.00").compareTo(declaration.getIitAmount()));
        assertEquals(0, new BigDecimal("1872.00").compareTo(declaration.getTotalTaxAmount()));
        assertEquals(TaxDeclarationStatusEnum.PENDING.getStatus(), declaration.getStatus());
        assertEquals(1, declaration.getItems().size());
        assertEquals(Boolean.TRUE, declaration.getItems().get(0).getOverExempt());
    }

    @Test
    public void testGenerate_splitsOnePercentAndThreePercent() {
        PayeeInfoDO payee = insertPayee(TENANT_ID, "李四", "110101199002022345", "13800000002");
        String month = lastMonth();
        insertBlue(TENANT_ID, "ORDER_ONE", payee, new BigDecimal("100000.00"), month, "0.01",
                InvoiceIssueStatusEnum.ISSUED, TaxStatusEnum.SUCCESS);
        insertBlue(TENANT_ID, "ORDER_THREE", payee, new BigDecimal("50000.00"), month, "0.03",
                InvoiceIssueStatusEnum.ISSUED, TaxStatusEnum.SUCCESS);

        TaxDeclarationRespVO declaration = taxDeclarationService.generate(month);

        assertEquals(0, new BigDecimal("100000.00").compareTo(declaration.getAmountAtOnePercent()));
        assertEquals(0, new BigDecimal("50000.00").compareTo(declaration.getAmountAtThreePercent()));
        // 增值税 100000×1% + 50000×3% = 2500
        assertEquals(0, new BigDecimal("2500.00").compareTo(declaration.getVatAmount()));
        assertEquals(0, new BigDecimal("150.00").compareTo(declaration.getSurchargeAmount()));
        assertEquals(0, new BigDecimal("750.00").compareTo(declaration.getIitAmount()));
        assertEquals(0, new BigDecimal("3400.00").compareTo(declaration.getTotalTaxAmount()));
    }

    @Test
    public void testGenerate_vatExemptUnderTenThousandLine() {
        PayeeInfoDO payee = insertPayee(TENANT_ID, "王五", "110101199003033456", "13800000003");
        String month = lastMonth();
        insertBlue(TENANT_ID, "ORDER_SMALL", payee, new BigDecimal("80000.00"), month, "0.01",
                InvoiceIssueStatusEnum.ISSUED, TaxStatusEnum.SUCCESS);

        TaxDeclarationRespVO declaration = taxDeclarationService.generate(month);

        assertTrue(declaration.getItems().get(0).getVatExempt());
        assertEquals(0, BigDecimal.ZERO.compareTo(declaration.getVatAmount()));
        assertEquals(0, BigDecimal.ZERO.compareTo(declaration.getSurchargeAmount()));
        // 免征增值税不免个税
        assertEquals(0, new BigDecimal("400.00").compareTo(declaration.getIitAmount()));
    }

    @Test
    public void testGenerate_tenThousandLineJudgedCrossTenant() {
        // 同一个自然人在两个租户各开 6 万：单看本租户未超 10 万，跨租户合并后 12 万要计税
        PayeeInfoDO payeeHere = insertPayee(TENANT_ID, "赵六", "110101199004044567", "13800000004");
        PayeeInfoDO payeeThere = insertPayee(OTHER_TENANT_ID, "赵六", "110101199004044567", "13800000004");
        String month = lastMonth();
        insertBlue(TENANT_ID, "ORDER_HERE", payeeHere, new BigDecimal("60000.00"), month, "0.01",
                InvoiceIssueStatusEnum.ISSUED, TaxStatusEnum.SUCCESS);
        insertBlue(OTHER_TENANT_ID, "ORDER_THERE", payeeThere, new BigDecimal("60000.00"), month, "0.01",
                InvoiceIssueStatusEnum.ISSUED, TaxStatusEnum.SUCCESS);

        TaxDeclarationRespVO declaration = taxDeclarationService.generate(month);

        TaxDeclarationItemRespVO item = declaration.getItems().get(0);
        assertEquals(0, new BigDecimal("120000.00").compareTo(item.getCrossTenantMonthAmount()));
        assertEquals(Boolean.TRUE, item.getOverExempt());
        assertFalse(item.getVatExempt());
        // 本租户只按自己的 6 万计税
        assertEquals(0, new BigDecimal("60000.00").compareTo(item.getSalesAmount()));
        assertEquals(0, new BigDecimal("600.00").compareTo(item.getVatAmount()));
    }

    @Test
    public void testGenerate_redOffsetReducesSalesInSameMonth() {
        PayeeInfoDO payee = insertPayee(TENANT_ID, "钱七", "110101199005055678", "13800000005");
        String month = lastMonth();
        InvoiceOrderDO blue = insertBlue(TENANT_ID, "ORDER_BLUE", payee, new BigDecimal("120000.00"), month,
                "0.01", InvoiceIssueStatusEnum.ISSUED, TaxStatusEnum.SUCCESS);
        insertRed(TENANT_ID, "RED_OK", blue, new BigDecimal("20000.00"), month, RedOffsetStatusEnum.SUCCESS);

        TaxDeclarationRespVO declaration = taxDeclarationService.generate(month);

        // 12 万 − 2 万 = 10 万，未超免征线（＞10 万才算超），增值税为 0
        assertEquals(0, new BigDecimal("100000.00").compareTo(declaration.getTotalSalesAmount()));
        assertEquals(0, BigDecimal.ZERO.compareTo(declaration.getVatAmount()));
        assertEquals(0, new BigDecimal("500.00").compareTo(declaration.getIitAmount()));
    }

    @Test
    public void testGenerate_isIdempotent() {
        PayeeInfoDO payee = insertPayee(TENANT_ID, "孙八", "110101199006066789", "13800000006");
        String month = lastMonth();
        insertBlue(TENANT_ID, "ORDER_IDEMPOTENT", payee, new BigDecimal("120000.00"), month, "0.01",
                InvoiceIssueStatusEnum.ISSUED, TaxStatusEnum.SUCCESS);

        TaxDeclarationRespVO first = taxDeclarationService.generate(month);
        TaxDeclarationRespVO second = taxDeclarationService.generate(month);

        assertEquals(first.getId(), second.getId());
        assertEquals(first.getDeclarationNo(), second.getDeclarationNo());
        assertEquals(1, itemMapper.selectByDeclarationId(first.getId()).size());
    }

    // ==================== 缺项检查 ====================

    @Test
    public void testPrecheck_pointsOutMissingSellerIdentity() {
        PayeeInfoDO payee = insertPayee(TENANT_ID, "周九", null, "13800000007");
        String month = lastMonth();
        insertBlue(TENANT_ID, "ORDER_NO_ID", payee, new BigDecimal("120000.00"), month, "0.01",
                InvoiceIssueStatusEnum.ISSUED, TaxStatusEnum.SUCCESS);

        TaxDeclarationPrecheckRespVO precheck = taxDeclarationService.precheck(month);

        assertFalse(precheck.getReady());
        assertTrue(precheck.getMissing().stream().anyMatch(item -> "SELLER_INFO_MISSING".equals(item.getType())));
        assertTrue(precheck.getMessage().contains("缺"), "实际：" + precheck.getMessage());
    }

    @Test
    public void testPrecheck_readyWhenDataComplete() {
        PayeeInfoDO payee = insertPayee(TENANT_ID, "吴十", "110101199008088901", "13800000008");
        String month = lastMonth();
        insertBlue(TENANT_ID, "ORDER_OK", payee, new BigDecimal("120000.00"), month, "0.01",
                InvoiceIssueStatusEnum.ISSUED, TaxStatusEnum.SUCCESS);

        TaxDeclarationPrecheckRespVO precheck = taxDeclarationService.precheck(month);

        assertTrue(precheck.getReady());
        assertEquals(0, precheck.getMissingCount());
    }

    // ==================== 申报 / 缴款状态机 ====================

    @Test
    public void testDeclareAndPay_archivesVoucherAndLinksInvoices() {
        PayeeInfoDO payee = insertPayee(TENANT_ID, "郑十一", "110101199009099012", "13800000009");
        String month = lastMonth();
        insertBlue(TENANT_ID, "ORDER_PAY", payee, new BigDecimal("120000.00"), month, "0.01",
                InvoiceIssueStatusEnum.ISSUED, TaxStatusEnum.SUCCESS);
        taxDeclarationService.generate(month);

        TaxDeclarationDeclareReqVO declareReq = new TaxDeclarationDeclareReqVO();
        declareReq.setPeriodMonth(month);
        declareReq.setDeclaredBy("财务小李");
        TaxDeclarationRespVO declared = taxDeclarationService.declare(declareReq);
        assertEquals(TaxDeclarationStatusEnum.DECLARED.getStatus(), declared.getStatus());
        assertNotNull(declared.getDeclaredAt());

        TaxDeclarationPayReqVO payReq = new TaxDeclarationPayReqVO();
        payReq.setPeriodMonth(month);
        payReq.setVoucherNo("VOUCHER-001");
        payReq.setPaymentMethod("电子税务局批量扣款");
        TaxDeclarationRespVO paid = taxDeclarationService.recordPayment(payReq);

        assertEquals(TaxDeclarationStatusEnum.PAID.getStatus(), paid.getStatus());
        assertEquals("VOUCHER-001", paid.getVoucherNo());
        assertEquals(0, new BigDecimal("1872.00").compareTo(paid.getPaidAmount()));
        assertEquals(1, paid.getInvoiceCount());
        assertEquals(TaxDeclarationStatusEnum.PAID.getStatus(), paid.getItems().get(0).getStatus());
        assertEquals(0, new BigDecimal("1872.00").compareTo(paid.getItems().get(0).getPaidAmount()));
    }

    @Test
    public void testPay_requiresDeclaredFirst() {
        PayeeInfoDO payee = insertPayee(TENANT_ID, "王十二", "110101199010100123", "13800000010");
        String month = lastMonth();
        insertBlue(TENANT_ID, "ORDER_NOT_DECLARED", payee, new BigDecimal("120000.00"), month, "0.01",
                InvoiceIssueStatusEnum.ISSUED, TaxStatusEnum.SUCCESS);
        taxDeclarationService.generate(month);

        TaxDeclarationPayReqVO payReq = new TaxDeclarationPayReqVO();
        payReq.setPeriodMonth(month);
        assertServiceException(() -> taxDeclarationService.recordPayment(payReq),
                TAX_DECLARATION_STATUS_INVALID, month, "缴款",
                TaxDeclarationStatusEnum.nameOf(TaxDeclarationStatusEnum.PENDING.getStatus()), "请先报送报告表");
    }

    @Test
    public void testDeclare_blocksWhenDataMissing() {
        PayeeInfoDO payee = insertPayee(TENANT_ID, "冯十三", null, "13800000011");
        String month = lastMonth();
        insertBlue(TENANT_ID, "ORDER_MISSING", payee, new BigDecimal("120000.00"), month, "0.01",
                InvoiceIssueStatusEnum.ISSUED, TaxStatusEnum.SUCCESS);
        taxDeclarationService.generate(month);

        TaxDeclarationDeclareReqVO reqVO = new TaxDeclarationDeclareReqVO();
        reqVO.setPeriodMonth(month);
        assertServiceException(() -> taxDeclarationService.declare(reqVO), TAX_DECLARATION_NOT_READY,
                month, "1 个出售者缺姓名或身份证号，不能申报");
    }

    @Test
    public void testGenerate_paidDeclarationIsLockedAndDifferenceBecomesSupplement() {
        PayeeInfoDO payee = insertPayee(TENANT_ID, "陈十四", "110101199012120345", "13800000012");
        String month = lastMonth();
        insertBlue(TENANT_ID, "ORDER_BASE", payee, new BigDecimal("120000.00"), month, "0.01",
                InvoiceIssueStatusEnum.ISSUED, TaxStatusEnum.SUCCESS);
        taxDeclarationService.generate(month);
        TaxDeclarationDeclareReqVO declareReq = new TaxDeclarationDeclareReqVO();
        declareReq.setPeriodMonth(month);
        taxDeclarationService.declare(declareReq);
        TaxDeclarationPayReqVO payReq = new TaxDeclarationPayReqVO();
        payReq.setPeriodMonth(month);
        taxDeclarationService.recordPayment(payReq);

        // 缴款后又开来一张票：已缴的申报单不改写，差额转补缴
        insertBlue(TENANT_ID, "ORDER_EXTRA", payee, new BigDecimal("60000.00"), month, "0.01",
                InvoiceIssueStatusEnum.ISSUED, TaxStatusEnum.SUCCESS);
        TaxDeclarationRespVO regenerated = taxDeclarationService.generate(month);

        assertEquals(TaxDeclarationStatusEnum.PAID.getStatus(), regenerated.getStatus());
        assertEquals(0, new BigDecimal("1872.00").compareTo(regenerated.getTotalTaxAmount()));
        List<TaxSupplementDO> pending = supplementMapper.selectPendingByDeclarationId(regenerated.getId());
        assertEquals(1, pending.size());
        // 新增 6 万应补：增值税 600 + 附加 36 + 个税 300 = 936
        assertEquals(0, new BigDecimal("936.00").compareTo(pending.get(0).getAmount()));
    }

    @Test
    public void testGenerate_keepsDeclaredStatusWhenAmountsUnchanged() {
        PayeeInfoDO payee = insertPayee(TENANT_ID, "许十六", "110101199204040789", "13800000016");
        String month = lastMonth();
        insertBlue(TENANT_ID, "ORDER_KEEP_DECLARED", payee, new BigDecimal("120000.00"), month, "0.01",
                InvoiceIssueStatusEnum.ISSUED, TaxStatusEnum.SUCCESS);
        taxDeclarationService.generate(month);
        TaxDeclarationDeclareReqVO declareReq = new TaxDeclarationDeclareReqVO();
        declareReq.setPeriodMonth(month);
        declareReq.setDeclaredBy("财务小李");
        TaxDeclarationRespVO declared = taxDeclarationService.declare(declareReq);

        // 夜间定时任务每日刷新，金额未变时不应把已报送的申报单打回待申报
        TaxDeclarationRespVO regenerated = taxDeclarationService.generate(month);

        assertEquals(TaxDeclarationStatusEnum.DECLARED.getStatus(), regenerated.getStatus());
        assertEquals("财务小李", regenerated.getDeclaredBy());
        assertEquals(declared.getDeclaredAt(), regenerated.getDeclaredAt());
    }

    @Test
    public void testGenerate_declaredResetToPendingWhenAmountsChange() {
        PayeeInfoDO payee = insertPayee(TENANT_ID, "何十七", "110101199205050890", "13800000017");
        String month = lastMonth();
        insertBlue(TENANT_ID, "ORDER_CHANGE", payee, new BigDecimal("120000.00"), month, "0.01",
                InvoiceIssueStatusEnum.ISSUED, TaxStatusEnum.SUCCESS);
        taxDeclarationService.generate(month);
        TaxDeclarationDeclareReqVO declareReq = new TaxDeclarationDeclareReqVO();
        declareReq.setPeriodMonth(month);
        taxDeclarationService.declare(declareReq);

        // 报送后又开来一张票：金额变了，退回待申报，避免用对不上的报告表去缴款
        insertBlue(TENANT_ID, "ORDER_CHANGE_EXTRA", payee, new BigDecimal("50000.00"), month, "0.01",
                InvoiceIssueStatusEnum.ISSUED, TaxStatusEnum.SUCCESS);
        TaxDeclarationRespVO regenerated = taxDeclarationService.generate(month);

        assertEquals(TaxDeclarationStatusEnum.PENDING.getStatus(), regenerated.getStatus());
        assertNull(regenerated.getDeclaredAt());
    }

    // ==================== 预警 ====================

    @Test
    public void testGetWarnings_deadlineApproaching() {
        TaxDeclarationDO declaration = TaxDeclarationDO.builder()
                .declarationNo("TAXDECL-" + lastMonth())
                .periodMonth(lastMonth())
                .declarationDeadline(LocalDate.now().plusDays(3))
                .status(TaxDeclarationStatusEnum.PENDING.getStatus())
                .totalTaxAmount(new BigDecimal("100.00"))
                .dataReady(true)
                .missingDataCount(0)
                .build();
        declarationMapper.insert(declaration);

        List<TaxDeclarationWarningRespVO> warnings = taxDeclarationService.getWarnings();

        assertTrue(warnings.stream().anyMatch(item ->
                TaxDeclarationWarningTypeEnum.DEADLINE_APPROACHING.getCode().equals(item.getType())));
        assertTrue(warnings.stream().noneMatch(TaxDeclarationWarningRespVO::getSuspensionRisk));
    }

    @Test
    public void testGetWarnings_overdueSuspensionRisk() {
        String oldMonth = LocalDate.now().minusMonths(2).format(MONTH_FORMATTER);
        TaxDeclarationDO declaration = TaxDeclarationDO.builder()
                .declarationNo("TAXDECL-" + oldMonth)
                .periodMonth(oldMonth)
                .declarationDeadline(LocalDate.now().minusDays(7))
                .status(TaxDeclarationStatusEnum.PENDING.getStatus())
                .totalTaxAmount(new BigDecimal("500.00"))
                .dataReady(true)
                .missingDataCount(0)
                .build();
        declarationMapper.insert(declaration);

        List<TaxDeclarationWarningRespVO> warnings = taxDeclarationService.getWarnings();

        TaxDeclarationWarningRespVO overdue = warnings.stream().filter(item ->
                TaxDeclarationWarningTypeEnum.OVERDUE_SUSPENSION_RISK.getCode().equals(item.getType()))
                .findFirst().orElseThrow();
        assertTrue(overdue.getSuspensionRisk());
        assertEquals(Boolean.TRUE, overdue.getOverdue());
        assertTrue(overdue.getMessage().contains("暂停"), "实际：" + overdue.getMessage());
    }

    @Test
    public void testGetWarnings_paidDeclarationProducesNoWarning() {
        PayeeInfoDO payee = insertPayee(TENANT_ID, "袁十五", "110101199101010456", "13800000013");
        String month = lastMonth();
        insertBlue(TENANT_ID, "ORDER_PAID_NOWARN", payee, new BigDecimal("120000.00"), month, "0.01",
                InvoiceIssueStatusEnum.ISSUED, TaxStatusEnum.SUCCESS);
        taxDeclarationService.generate(month);
        TaxDeclarationDeclareReqVO declareReq = new TaxDeclarationDeclareReqVO();
        declareReq.setPeriodMonth(month);
        taxDeclarationService.declare(declareReq);
        TaxDeclarationPayReqVO payReq = new TaxDeclarationPayReqVO();
        payReq.setPeriodMonth(month);
        taxDeclarationService.recordPayment(payReq);

        assertTrue(taxDeclarationService.getWarnings().isEmpty());
    }

    // ==================== 分页 ====================

    @Test
    public void testGetItemPage_filterByOverExempt() {
        PayeeInfoDO big = insertPayee(TENANT_ID, "大户", "110101199202020567", "13800000014");
        PayeeInfoDO small = insertPayee(TENANT_ID, "小户", "110101199203030678", "13800000015");
        String month = lastMonth();
        insertBlue(TENANT_ID, "ORDER_BIG", big, new BigDecimal("120000.00"), month, "0.01",
                InvoiceIssueStatusEnum.ISSUED, TaxStatusEnum.SUCCESS);
        insertBlue(TENANT_ID, "ORDER_SMALL", small, new BigDecimal("30000.00"), month, "0.01",
                InvoiceIssueStatusEnum.ISSUED, TaxStatusEnum.SUCCESS);
        taxDeclarationService.generate(month);

        TaxDeclarationItemPageReqVO reqVO = new TaxDeclarationItemPageReqVO();
        reqVO.setPeriodMonth(month);
        reqVO.setOverExempt(true);
        PageResult<TaxDeclarationItemRespVO> page = taxDeclarationService.getItemPage(reqVO);

        assertEquals(1, page.getTotal());
        assertEquals("大户", page.getList().get(0).getSellerName());
    }

    // ==================== 造数 ====================

    private String lastMonth() {
        return LocalDate.now().minusMonths(1).format(MONTH_FORMATTER);
    }

    private PayeeInfoDO insertPayee(Long tenantId, String name, String idCardNo, String mobile) {
        return TenantUtils.execute(tenantId, () -> {
            // 生产真实形状：partner_payee_id 与 payee_no 对每份档案各自生成，
            // 同一自然人跨租户的两份档案编号不同；用手机号派生会被全局唯一键拒（#99）
            String unique = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            PayeeInfoDO payee = PayeeInfoDO.builder()
                    .partnerPayeeId("PAYEE_" + unique)
                    .name(name)
                    .mobile(mobile)
                    .idCardNo(idCardNo)
                    .payeeNo("PAYEE_NO_" + unique)
                    .build();
            payeeInfoMapper.insert(payee);
            return payee;
        });
    }

    private InvoiceOrderDO insertBlue(Long tenantId, String partnerOrderId, PayeeInfoDO payee,
                                      BigDecimal amount, String periodMonth, String taxRate,
                                      InvoiceIssueStatusEnum invoiceStatus, TaxStatusEnum taxStatus) {
        return TenantUtils.execute(tenantId, () -> {
            LocalDateTime invoiceDate = LocalDate.parse(periodMonth + "-05").atTime(10, 0);
            InvoiceOrderDO order = InvoiceOrderDO.builder()
                    .orderNo("INV_" + partnerOrderId)
                    .partnerOrderId(partnerOrderId)
                    .payeeId(payee.getId())
                    .payeeNo(payee.getPartnerPayeeId())
                    .payerNo("PAYER_1")
                    .totalAmount(amount)
                    .invoiceAmount(amount)
                    .taxRate(taxRate == null ? null : new BigDecimal(taxRate))
                    .invoiceType(1)
                    .businessType("SCRAP")
                    .orderStatus(0)
                    .invoiceStatus(invoiceStatus.getStatus())
                    .paymentStatus(0)
                    .taxStatus(taxStatus == null ? TaxStatusEnum.SUCCESS.getStatus() : taxStatus.getStatus())
                    .preInvoiceStatus(PreInvoiceStatusEnum.SUCCESS.getStatus())
                    .build();
            if (InvoiceIssueStatusEnum.ISSUED.equals(invoiceStatus)) {
                order.setInvoiceDate(invoiceDate);
                order.setInvoiceNo("INVNO_" + partnerOrderId);
            }
            order.setCreateTime(invoiceDate);
            invoiceOrderMapper.insert(order);
            return order;
        });
    }

    private void insertRed(Long tenantId, String redOffsetNo, InvoiceOrderDO blue, BigDecimal amount,
                           String periodMonth, RedOffsetStatusEnum status) {
        TenantUtils.execute(tenantId, () -> redInvoiceMapper.insert(RedInvoiceDO.builder()
                .redOffsetNo(redOffsetNo)
                .invoiceOrderId(blue.getId())
                .partnerOrderId(blue.getPartnerOrderId())
                .reason("01")
                .amount(amount)
                .redOffsetStatus(status.getStatus())
                .redOffsetStatusCode(status.getCode())
                .redInvoiceDate(LocalDate.parse(periodMonth + "-06").atTime(11, 0))
                .build()));
    }

}
