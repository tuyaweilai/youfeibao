package cn.iocoder.yudao.module.icbc.service.quota;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.icbc.UnitTestConfiguration;
import cn.iocoder.yudao.module.icbc.controller.admin.quota.vo.SellerQuotaCheckRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.quota.vo.SellerQuotaGuidanceHandleReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.quota.vo.SellerQuotaGuidancePageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.quota.vo.SellerQuotaMonthRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.quota.vo.SellerQuotaRespVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.invoice.InvoiceOrderDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.invoice.RedInvoiceDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payee.PayeeInfoDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.quota.SellerQuotaGuidanceDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.invoice.InvoiceOrderMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.invoice.RedInvoiceMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.payee.PayeeInfoMapper;
import cn.iocoder.yudao.module.icbc.enums.InvoiceIssueStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.PreInvoiceStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.RedOffsetStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.SellerQuotaGuidanceStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.SellerQuotaTriggerSceneEnum;
import cn.iocoder.yudao.module.icbc.service.quota.impl.NaturalPersonQuotaServiceImpl;
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
 * {@link NaturalPersonQuotaServiceImpl} 的单元测试（issue #12）。
 *
 * <p>断言的是额度台账的外部可观察行为：连续 12 个月滚动窗口、跨租户合并、在途与红冲的口径、
 * 「出售者 × 月」的 10 万元免征线、1% 与 3% 分列，以及超限时的拒绝理由与经营主体登记引导。
 */
@Import({NaturalPersonQuotaServiceImpl.class, UnitTestConfiguration.class, IcbcTenantTestConfiguration.class})
@Sql(scripts = "/sql/create_tables.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Transactional
public class NaturalPersonQuotaServiceImplTest extends BaseDbUnitTest {

    private static final Long TENANT_ID = 1L;
    private static final Long OTHER_TENANT_ID = 2L;
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    @Resource
    private NaturalPersonQuotaService naturalPersonQuotaService;
    @Resource
    private PayeeInfoMapper payeeInfoMapper;
    @Resource
    private InvoiceOrderMapper invoiceOrderMapper;
    @Resource
    private RedInvoiceMapper redInvoiceMapper;

    @BeforeEach
    public void setUp() {
        TenantContextHolder.setTenantId(TENANT_ID);
    }

    @AfterEach
    public void tearDown() {
        TenantContextHolder.clear();
    }

    // ==================== 滚动窗口 ====================

    @Test
    public void testRollingWindow_onlyCountsLastTwelveMonths() {
        PayeeInfoDO payee = insertPayee(TENANT_ID, "张三", "110101199001011234", "13800000001");
        insertInvoiceOrder(TENANT_ID, "ORDER_IN", payee, new BigDecimal("100000.00"),
                LocalDateTime.now().minusMonths(11), "0.01", InvoiceIssueStatusEnum.ISSUED, null);
        // 13 个月前：已滚出窗口，不占额度
        insertInvoiceOrder(TENANT_ID, "ORDER_OUT", payee, new BigDecimal("900000.00"),
                LocalDateTime.now().minusMonths(13), "0.01", InvoiceIssueStatusEnum.ISSUED, null);

        SellerQuotaRespVO quota = naturalPersonQuotaService.getQuota(payee.getId());

        assertEquals(0, new BigDecimal("100000.00").compareTo(quota.getUsedAmount()));
        assertEquals(0, new BigDecimal("4900000.00").compareTo(quota.getRemainingAmount()));
        assertEquals(Boolean.FALSE, quota.getQuotaExceeded());
        assertNotNull(quota.getWindowStart());
        assertNotNull(quota.getWindowEnd());
    }

    // ==================== 跨租户合并 ====================

    @Test
    public void testCrossTenantMerging_sameNaturalPersonSalesAddUp() {
        PayeeInfoDO payeeInTenant1 = insertPayee(TENANT_ID, "李四", "110101199002022345", "13800000002");
        // 同一个自然人（同身份证）在另一个租户也建了档、也开了票
        PayeeInfoDO payeeInTenant2 = insertPayee(OTHER_TENANT_ID, "李四", "110101199002022345", "13800000002");
        insertInvoiceOrder(TENANT_ID, "ORDER_T1", payeeInTenant1, new BigDecimal("600000.00"),
                LocalDateTime.now().minusMonths(2), "0.01", InvoiceIssueStatusEnum.ISSUED, null);
        insertInvoiceOrder(OTHER_TENANT_ID, "ORDER_T2", payeeInTenant2, new BigDecimal("400000.00"),
                LocalDateTime.now().minusMonths(1), "0.01", InvoiceIssueStatusEnum.ISSUED, null);

        SellerQuotaRespVO quota = naturalPersonQuotaService.getQuota(payeeInTenant1.getId());

        // 额度是自然人的：另一租户的 40 万也要算进来
        assertEquals(0, new BigDecimal("1000000.00").compareTo(quota.getUsedAmount()));
        assertEquals(0, new BigDecimal("4000000.00").compareTo(quota.getRemainingAmount()));
    }

    @Test
    public void testDifferentNaturalPersonIsNotMerged() {
        PayeeInfoDO zhang = insertPayee(TENANT_ID, "张三", "110101199001011234", "13800000001");
        PayeeInfoDO wang = insertPayee(OTHER_TENANT_ID, "王五", "110101199003033456", "13800000003");
        insertInvoiceOrder(OTHER_TENANT_ID, "ORDER_W", wang, new BigDecimal("900000.00"),
                LocalDateTime.now().minusMonths(1), "0.01", InvoiceIssueStatusEnum.ISSUED, null);

        assertEquals(0, BigDecimal.ZERO.compareTo(
                naturalPersonQuotaService.getQuota(zhang.getId()).getUsedAmount()));
    }

    // ==================== 口径：什么才算占额度 ====================

    @Test
    public void testUnissuedAndFailedInvoicesDoNotOccupyQuota() {
        PayeeInfoDO payee = insertPayee(TENANT_ID, "赵六", "110101199004044567", "13800000004");
        // 只是预下单：还没开票，不占额度
        insertInvoiceOrder(TENANT_ID, "ORDER_INITIAL", payee, new BigDecimal("700000.00"),
                LocalDateTime.now().minusDays(1), "0.01",
                InvoiceIssueStatusEnum.NOT_ISSUED, PreInvoiceStatusEnum.IN_PROGRESS);
        // 预开票成功、已付款待开票：在途，占额度
        insertInvoiceOrder(TENANT_ID, "ORDER_PENDING", payee, new BigDecimal("50000.00"),
                LocalDateTime.now().minusDays(1), "0.01",
                InvoiceIssueStatusEnum.NOT_ISSUED, PreInvoiceStatusEnum.SUCCESS);
        // 开票失败：没有销售额，不占额度
        insertInvoiceOrder(TENANT_ID, "ORDER_FAILED", payee, new BigDecimal("800000.00"),
                LocalDateTime.now().minusDays(1), "0.01",
                InvoiceIssueStatusEnum.FAILED, PreInvoiceStatusEnum.SUCCESS);

        SellerQuotaRespVO quota = naturalPersonQuotaService.getQuota(payee.getId());

        assertEquals(0, BigDecimal.ZERO.compareTo(quota.getIssuedAmount()));
        assertEquals(0, new BigDecimal("50000.00").compareTo(quota.getPendingAmount()));
        assertEquals(0, new BigDecimal("50000.00").compareTo(quota.getUsedAmount()));
    }

    @Test
    public void testRedOffsetReducesUsedOnlyForInWindowBlueInvoice() {
        PayeeInfoDO payee = insertPayee(TENANT_ID, "钱七", "110101199005055678", "13800000005");
        InvoiceOrderDO blue = insertInvoiceOrder(TENANT_ID, "ORDER_BLUE", payee, new BigDecimal("1000000.00"),
                LocalDateTime.now().minusMonths(2), "0.01", InvoiceIssueStatusEnum.ISSUED, null);
        // 红冲成功的 40 万：销售额冲回去，额度也要放出来
        insertRedInvoice(TENANT_ID, "RED_OK", blue, new BigDecimal("400000.00"),
                LocalDateTime.now().minusMonths(1), RedOffsetStatusEnum.SUCCESS);
        // 红冲没成功（上传失败）：不算
        insertRedInvoice(TENANT_ID, "RED_FAILED", blue, new BigDecimal("100000.00"),
                LocalDateTime.now().minusMonths(1), RedOffsetStatusEnum.UPLOAD_FAILED);
        // 蓝票已滚出窗口的旧票被红冲：本就未计入额度，不能再放一次额度
        InvoiceOrderDO oldBlue = insertInvoiceOrder(TENANT_ID, "ORDER_OLD_BLUE", payee, new BigDecimal("500000.00"),
                LocalDateTime.now().minusMonths(13), "0.01", InvoiceIssueStatusEnum.ISSUED, null);
        insertRedInvoice(TENANT_ID, "RED_OLD", oldBlue, new BigDecimal("500000.00"),
                LocalDateTime.now().minusDays(1), RedOffsetStatusEnum.SUCCESS);

        SellerQuotaRespVO quota = naturalPersonQuotaService.getQuota(payee.getId());

        assertEquals(0, new BigDecimal("1000000.00").compareTo(quota.getIssuedAmount()));
        assertEquals(0, new BigDecimal("400000.00").compareTo(quota.getRedOffsetAmount()));
        assertEquals(0, new BigDecimal("600000.00").compareTo(quota.getUsedAmount()));
    }

    // ==================== 出售者 × 月：10 万元免征线 ====================

    @Test
    public void testMonthlyLedgerFlagsTenThousandExemptLine() {
        PayeeInfoDO payee = insertPayee(TENANT_ID, "孙八", "110101199006066789", "13800000006");
        insertInvoiceOrder(TENANT_ID, "ORDER_THIS_MONTH", payee, new BigDecimal("120000.00"),
                LocalDateTime.now(), "0.01", InvoiceIssueStatusEnum.ISSUED, null);
        insertInvoiceOrder(TENANT_ID, "ORDER_LAST_MONTH", payee, new BigDecimal("30000.00"),
                LocalDateTime.now().minusMonths(1), "0.01", InvoiceIssueStatusEnum.ISSUED, null);

        SellerQuotaRespVO quota = naturalPersonQuotaService.getQuota(payee.getId());

        String thisMonth = LocalDate.now().format(MONTH_FORMATTER);
        String lastMonth = LocalDate.now().minusMonths(1).format(MONTH_FORMATTER);
        assertEquals(0, new BigDecimal("120000.00").compareTo(quota.getCurrentMonthAmount()));
        assertEquals(Boolean.TRUE, quota.getCurrentMonthOverExempt());
        assertEquals(0, new BigDecimal("100000.00").compareTo(quota.getMonthlyExemptAmount()));

        SellerQuotaMonthRespVO current = monthOf(quota, thisMonth);
        assertEquals(0, new BigDecimal("120000.00").compareTo(current.getNetAmount()));
        assertEquals(Boolean.TRUE, current.getOverMonthlyExempt());
        assertEquals(Boolean.FALSE, monthOf(quota, lastMonth).getOverMonthlyExempt());
        // 本月无销售额也要出现在台账里，界面才能显示「本月」
        assertNotNull(monthOf(quota, thisMonth));
    }

    @Test
    public void testMonthlyLedgerAlwaysContainsCurrentMonthEvenWithoutSales() {
        PayeeInfoDO payee = insertPayee(TENANT_ID, "周九", "110101199007077890", "13800000007");
        insertInvoiceOrder(TENANT_ID, "ORDER_OLD_MONTH", payee, new BigDecimal("10000.00"),
                LocalDateTime.now().minusMonths(3), "0.01", InvoiceIssueStatusEnum.ISSUED, null);

        SellerQuotaRespVO quota = naturalPersonQuotaService.getQuota(payee.getId());

        assertEquals(0, BigDecimal.ZERO.compareTo(quota.getCurrentMonthAmount()));
        assertEquals(0, BigDecimal.ZERO.compareTo(
                monthOf(quota, LocalDate.now().format(MONTH_FORMATTER)).getNetAmount()));
    }

    // ==================== 1% / 3% 分列 ====================

    @Test
    public void testSalesAreSplitByTaxRateReductionOrWaiver() {
        PayeeInfoDO payee = insertPayee(TENANT_ID, "吴十", "110101199008088901", "13800000008");
        // 3% 减按 1%
        insertInvoiceOrder(TENANT_ID, "ORDER_ONE", payee, new BigDecimal("100000.00"),
                LocalDateTime.now().minusMonths(1), "0.01", InvoiceIssueStatusEnum.ISSUED, null);
        // 放弃减按，按 3%
        insertInvoiceOrder(TENANT_ID, "ORDER_THREE", payee, new BigDecimal("200000.00"),
                LocalDateTime.now().minusMonths(1), "0.03", InvoiceIssueStatusEnum.ISSUED, null);
        // 历史数据没有征收率：计入总额，但不进 1% / 3% 任何一列
        insertInvoiceOrder(TENANT_ID, "ORDER_UNKNOWN", payee, new BigDecimal("50000.00"),
                LocalDateTime.now().minusMonths(1), null, InvoiceIssueStatusEnum.ISSUED, null);

        SellerQuotaRespVO quota = naturalPersonQuotaService.getQuota(payee.getId());

        assertEquals(0, new BigDecimal("100000.00").compareTo(quota.getAmountAtOnePercent()));
        assertEquals(0, new BigDecimal("200000.00").compareTo(quota.getAmountAtThreePercent()));
        assertEquals(0, new BigDecimal("50000.00").compareTo(quota.getOtherAmount()));
        assertEquals(0, new BigDecimal("350000.00").compareTo(quota.getUsedAmount()));
    }

    // ==================== 硬校验 ====================

    @Test
    public void testCheckQuota_passesAndWarnsAboutExemptLine() {
        PayeeInfoDO payee = insertPayee(TENANT_ID, "郑十一", "110101199009099012", "13800000009");

        SellerQuotaCheckRespVO check = naturalPersonQuotaService.checkQuota(payee.getId(),
                new BigDecimal("120000.00"));

        assertTrue(check.getPassed());
        assertFalse(check.getQuotaExceeded());
        assertEquals(Boolean.TRUE, check.getMonthlyOverExempt());
        assertTrue(check.getMessage().contains("10 万元免征线"), "实际：" + check.getMessage());
        assertNull(check.getRemedy());
    }

    @Test
    public void testCheckQuota_thisInvoiceWouldExceedCap() {
        PayeeInfoDO payee = insertPayee(TENANT_ID, "王十二", "110101199010100123", "13800000010");
        insertInvoiceOrder(TENANT_ID, "ORDER_NEAR_CAP", payee, new BigDecimal("4999000.00"),
                LocalDateTime.now().minusMonths(1), "0.01", InvoiceIssueStatusEnum.ISSUED, null);

        SellerQuotaCheckRespVO check = naturalPersonQuotaService.checkQuota(payee.getId(),
                new BigDecimal("5000.00"));

        assertFalse(check.getPassed());
        assertTrue(check.getQuotaExceeded());
        assertEquals(0, new BigDecimal("1000.00").compareTo(check.getRemainingAmount()));
        assertTrue(check.getMessage().contains("500 万"), "实际：" + check.getMessage());
        assertNotNull(check.getRemedy());
    }

    @Test
    public void testCheckQuota_alreadyOverCap() {
        PayeeInfoDO payee = insertPayee(TENANT_ID, "冯十三", "110101199011110234", "13800000011");
        insertInvoiceOrder(TENANT_ID, "ORDER_OVER_CAP", payee, new BigDecimal("5100000.00"),
                LocalDateTime.now().minusMonths(1), "0.01", InvoiceIssueStatusEnum.ISSUED, null);

        SellerQuotaCheckRespVO check = naturalPersonQuotaService.checkQuota(payee.getId(),
                new BigDecimal("100.00"));

        assertFalse(check.getPassed());
        assertTrue(check.getQuotaExceeded());
        assertEquals(0, BigDecimal.ZERO.compareTo(check.getRemainingAmount()));
        assertTrue(check.getMessage().contains("超过 500 万元上限"), "实际：" + check.getMessage());
        assertNotNull(check.getRemedy());
    }

    @Test
    public void testCheckQuota_payeeNotExists() {
        assertServiceException(() -> naturalPersonQuotaService.checkQuota(99999L, BigDecimal.TEN),
                PAYEE_NOT_EXISTS);
        assertServiceException(() -> naturalPersonQuotaService.getQuota(99999L), PAYEE_NOT_EXISTS);
    }

    // ==================== 经营主体登记引导 ====================

    @Test
    public void testRecordGuidance_isIdempotentAndHandled() {
        PayeeInfoDO payee = insertPayee(TENANT_ID, "陈十四", "110101199012120345", "13800000012");
        insertInvoiceOrder(TENANT_ID, "ORDER_GUIDE", payee, new BigDecimal("5100000.00"),
                LocalDateTime.now().minusMonths(1), "0.01", InvoiceIssueStatusEnum.ISSUED, null);

        naturalPersonQuotaService.recordGuidance(payee.getId(),
                SellerQuotaTriggerSceneEnum.INVOICE_APPLICATION.getCode(), "ACQ_GUIDE_1");
        // 再次被拒：只刷新，不新增（否则被拒一次就多一条）
        naturalPersonQuotaService.recordGuidance(payee.getId(),
                SellerQuotaTriggerSceneEnum.INVOICE_APPLICATION.getCode(), "ACQ_GUIDE_2");

        SellerQuotaGuidancePageReqVO pageReqVO = new SellerQuotaGuidancePageReqVO();
        PageResult<SellerQuotaGuidanceDO> page = naturalPersonQuotaService.getGuidancePage(pageReqVO);
        assertEquals(1, page.getTotal());
        SellerQuotaGuidanceDO guidance = page.getList().get(0);
        assertEquals(SellerQuotaGuidanceStatusEnum.PENDING.getStatus(), guidance.getStatus());
        assertEquals("ACQ_GUIDE_2", guidance.getTriggerBizNo());
        assertEquals(0, new BigDecimal("5100000.00").compareTo(guidance.getUsedAmount()));

        // 已引导
        SellerQuotaGuidanceHandleReqVO informed = new SellerQuotaGuidanceHandleReqVO();
        informed.setId(guidance.getId());
        informed.setStatus(SellerQuotaGuidanceStatusEnum.INFORMED.getStatus());
        informed.setHandleRemark("已电话告知，去办个体工商户");
        naturalPersonQuotaService.handleGuidance(informed);
        assertEquals(SellerQuotaGuidanceStatusEnum.INFORMED.getStatus(),
                naturalPersonQuotaService.getGuidancePage(pageReqVO).getList().get(0).getStatus());

        // 已办结
        SellerQuotaGuidanceHandleReqVO resolved = new SellerQuotaGuidanceHandleReqVO();
        resolved.setId(guidance.getId());
        resolved.setStatus(SellerQuotaGuidanceStatusEnum.RESOLVED.getStatus());
        naturalPersonQuotaService.handleGuidance(resolved);

        // 办结后不能再被改回「已引导」
        assertServiceException(() -> naturalPersonQuotaService.handleGuidance(informed),
                SELLER_QUOTA_GUIDANCE_STATUS_INVALID, SellerQuotaGuidanceStatusEnum.nameOf(
                        SellerQuotaGuidanceStatusEnum.INFORMED.getStatus()));
    }

    @Test
    public void testHandleGuidance_notExists() {
        SellerQuotaGuidanceHandleReqVO reqVO = new SellerQuotaGuidanceHandleReqVO();
        reqVO.setId(12345L);
        reqVO.setStatus(SellerQuotaGuidanceStatusEnum.INFORMED.getStatus());
        assertServiceException(() -> naturalPersonQuotaService.handleGuidance(reqVO),
                SELLER_QUOTA_GUIDANCE_NOT_EXISTS);
    }

    // ==================== 造数 ====================

    private SellerQuotaMonthRespVO monthOf(SellerQuotaRespVO quota, String month) {
        List<SellerQuotaMonthRespVO> months = quota.getMonths();
        assertNotNull(months);
        return months.stream().filter(item -> month.equals(item.getMonth())).findFirst()
                .orElseThrow(() -> new AssertionError("台账里没有 " + month + "，实际：" + months.stream()
                        .map(SellerQuotaMonthRespVO::getMonth).toList()));
    }

    private PayeeInfoDO insertPayee(Long tenantId, String name, String idCardNo, String mobile) {
        return TenantUtils.execute(tenantId, () -> {
            PayeeInfoDO payee = PayeeInfoDO.builder()
                    // 生产真实形状：partner_payee_id 由 generatePartnerPayeeId() 每个档案各自生成，
                    // 同一自然人跨租户的两份档案编号不同；用手机号派生会被全局唯一键拒（#99）
                    .partnerPayeeId("PAYEE_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                    .name(name)
                    .mobile(mobile)
                    .idCardNo(idCardNo)
                    .build();
            payeeInfoMapper.insert(payee);
            return payee;
        });
    }

    private InvoiceOrderDO insertInvoiceOrder(Long tenantId, String partnerOrderId, PayeeInfoDO payee,
                                              BigDecimal amount, LocalDateTime effectiveTime, String taxRate,
                                              InvoiceIssueStatusEnum invoiceStatus,
                                              PreInvoiceStatusEnum preInvoiceStatus) {
        return TenantUtils.execute(tenantId, () -> {
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
                    .taxStatus(0)
                    .preInvoiceStatus(preInvoiceStatus == null
                            ? PreInvoiceStatusEnum.INITIAL.getStatus() : preInvoiceStatus.getStatus())
                    .build();
            order.setCreateTime(effectiveTime);
            if (InvoiceIssueStatusEnum.ISSUED.equals(invoiceStatus)) {
                order.setInvoiceDate(effectiveTime);
            }
            invoiceOrderMapper.insert(order);
            return order;
        });
    }

    private void insertRedInvoice(Long tenantId, String redOffsetNo, InvoiceOrderDO blueInvoice,
                                  BigDecimal amount, LocalDateTime redInvoiceDate,
                                  RedOffsetStatusEnum status) {
        TenantUtils.execute(tenantId, () -> redInvoiceMapper.insert(RedInvoiceDO.builder()
                .redOffsetNo(redOffsetNo)
                .invoiceOrderId(blueInvoice.getId())
                .partnerOrderId(blueInvoice.getPartnerOrderId())
                .reason("01")
                .amount(amount)
                .redOffsetStatus(status.getStatus())
                .redOffsetStatusCode(status.getCode())
                .redInvoiceDate(redInvoiceDate)
                .build()));
    }

}
