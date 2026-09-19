package cn.iocoder.yudao.module.icbc.service.billing;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.icbc.UnitTestConfiguration;
import cn.iocoder.yudao.module.icbc.controller.admin.billing.vo.IcbcBillingLedgerPageReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.billing.IcbcBillingLedgerDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.invoice.InvoiceOrderDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.invoice.RedInvoiceDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.billing.IcbcBillingLedgerMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.invoice.InvoiceOrderMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.invoice.RedInvoiceMapper;
import cn.iocoder.yudao.module.icbc.enums.InvoiceIssueStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.PaymentStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.RedOffsetStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.TaxStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.UploadStatusEnum;
import cn.iocoder.yudao.module.icbc.service.billing.impl.PlatformBillingServiceImpl;
import cn.iocoder.yudao.test.icbc.IcbcTenantTestConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.BILLING_PERIOD_MONTH_INVALID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 平台计费计量的测试（#16）。
 *
 * <p>口径：成功开具的报废产品收购发票张数 - 被成功红冲的张数；红票本身不计。
 * 计量台账是平台自己的账，跨租户；租户侧没有入口。
 */
@Import({UnitTestConfiguration.class, IcbcTenantTestConfiguration.class, PlatformBillingServiceImpl.class})
@TestPropertySource(properties = "icbc.billing.unit-price=10.00")
@Sql(scripts = "/sql/create_tables.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/sql/clean.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
public class PlatformBillingServiceImplTest extends BaseDbUnitTest {

    private static final String PERIOD = "2026-09";

    @Resource
    private PlatformBillingService platformBillingService;

    @Resource
    private InvoiceOrderMapper invoiceOrderMapper;

    @Resource
    private RedInvoiceMapper redInvoiceMapper;

    @Resource
    private IcbcBillingLedgerMapper billingLedgerMapper;

    @AfterEach
    public void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    public void testGenerateCountsOnlyIssuedScrapInPeriod() {
        insertIssuedInvoice(1L, "ORDER_A", "2026-09-05 10:00:00");
        insertIssuedInvoice(1L, "ORDER_B", "2026-09-30 23:59:00");
        // 非本期
        insertIssuedInvoice(1L, "ORDER_C", "2026-08-31 23:59:00");
        // 未开票
        insertInvoice(1L, "ORDER_D", null, InvoiceIssueStatusEnum.NOT_ISSUED.getStatus(),
                "SCRAP", "2026-09-10 10:00:00");
        // 农产品收购，不计入报废产品计费
        insertInvoice(1L, "ORDER_E", "55555555555555555555", InvoiceIssueStatusEnum.ISSUED.getStatus(),
                "AGRICULTURAL", "2026-09-10 10:00:00");

        IcbcBillingLedgerDO ledger = platformBillingService.generate(1L, PERIOD);

        assertEquals(2, ledger.getIssuedCount());
        assertEquals(0, ledger.getReversedCount());
        assertEquals(2, ledger.getBillableCount());
        assertEquals(0, new BigDecimal("20.00").compareTo(ledger.getAmount()));
        assertEquals(0, new BigDecimal("10.00").compareTo(ledger.getUnitPrice()));
        assertEquals(1L, ledger.getTenantId());
        assertEquals(PERIOD, ledger.getPeriodMonth());
    }

    @Test
    public void testRedOffsetDeductsBlueAndRedIsNeverCounted() {
        insertIssuedInvoice(1L, "ORDER_KEEP", "2026-09-05 10:00:00");
        insertIssuedInvoice(1L, "ORDER_REVERSED", "2026-09-06 10:00:00");
        // 红票上传成功：对应的蓝票不再计费，红票本身也不计
        insertRedInvoice(1L, "RED_1", "ORDER_REVERSED", RedOffsetStatusEnum.SUCCESS.getStatus());
        // 红冲还在进行中：蓝票仍计费
        insertIssuedInvoice(1L, "ORDER_PENDING_RED", "2026-09-07 10:00:00");
        insertRedInvoice(1L, "RED_2", "ORDER_PENDING_RED", RedOffsetStatusEnum.APPLYING.getStatus());

        IcbcBillingLedgerDO ledger = platformBillingService.generate(1L, PERIOD);

        assertEquals(3, ledger.getIssuedCount());
        assertEquals(1, ledger.getReversedCount());
        assertEquals(2, ledger.getBillableCount());
        assertEquals(0, new BigDecimal("20.00").compareTo(ledger.getAmount()));
    }

    @Test
    public void testGenerateIsIdempotent() {
        insertIssuedInvoice(1L, "ORDER_X", "2026-09-05 10:00:00");

        IcbcBillingLedgerDO first = platformBillingService.generate(1L, PERIOD);
        // 同一租户 × 期间重复计量：覆盖同一条，不产生第二条
        IcbcBillingLedgerDO second = platformBillingService.generate(1L, PERIOD);

        assertEquals(first.getId(), second.getId());
        assertEquals(1, billingLedgerMapper.selectListByPeriod(PERIOD).size());
    }

    @Test
    public void testGenerateAllSpansTenants() {
        insertIssuedInvoice(1L, "ORDER_T1", "2026-09-05 10:00:00");
        insertIssuedInvoice(2L, "ORDER_T2", "2026-09-06 10:00:00");

        List<IcbcBillingLedgerDO> ledgers = platformBillingService.generate(PERIOD);

        assertEquals(2, ledgers.size());
        assertTrue(ledgers.stream().anyMatch(ledger -> ledger.getTenantId().equals(1L)));
        assertTrue(ledgers.stream().anyMatch(ledger -> ledger.getTenantId().equals(2L)));
        // 台账落在全局表：不按租户过滤
        assertEquals(2, billingLedgerMapper.selectListByPeriod(PERIOD).size());
    }

    @Test
    public void testPageFiltersByTenantAndPeriod() {
        insertIssuedInvoice(1L, "ORDER_P1", "2026-09-05 10:00:00");
        insertIssuedInvoice(2L, "ORDER_P2", "2026-08-05 10:00:00");
        platformBillingService.generate(PERIOD);
        platformBillingService.generate("2026-08");

        IcbcBillingLedgerPageReqVO reqVO = new IcbcBillingLedgerPageReqVO();
        reqVO.setTenantId(1L);
        reqVO.setPeriodMonth(PERIOD);
        PageResult<IcbcBillingLedgerDO> page = platformBillingService.getPage(reqVO);

        assertEquals(1, page.getTotal());
        assertEquals(1L, page.getList().get(0).getTenantId());
    }

    @Test
    public void testInvalidPeriodRejected() {
        assertServiceException(() -> platformBillingService.generate(1L, "2026/09"),
                BILLING_PERIOD_MONTH_INVALID, "2026/09");
    }

    // ==================== 造数 ====================

    private void insertIssuedInvoice(Long tenantId, String partnerOrderId, String invoiceDate) {
        insertInvoice(tenantId, partnerOrderId, "INV_" + partnerOrderId,
                InvoiceIssueStatusEnum.ISSUED.getStatus(), "SCRAP", invoiceDate);
    }

    private void insertInvoice(Long tenantId, String partnerOrderId, String invoiceNo, Integer invoiceStatus,
                               String businessType, String invoiceDate) {
        TenantUtils.execute(tenantId, () -> invoiceOrderMapper.insert(InvoiceOrderDO.builder()
                .orderNo("INV_" + partnerOrderId)
                .partnerOrderId(partnerOrderId)
                .payeeNo("PAYEE_" + partnerOrderId)
                .payerNo("PAYER_1")
                .totalAmount(new BigDecimal("1000.00"))
                .invoiceType(1)
                .businessType(businessType)
                .orderStatus(3)
                .invoiceStatus(invoiceStatus)
                .paymentStatus(PaymentStatusEnum.SUCCESS.getStatus())
                .taxStatus(TaxStatusEnum.SUCCESS.getStatus())
                .uploadStatus(UploadStatusEnum.SUCCESS.getStatus())
                .invoiceNo(invoiceNo)
                .invoiceDate(LocalDateTime.parse(invoiceDate.replace(' ', 'T')))
                .build()));
    }

    private void insertRedInvoice(Long tenantId, String redOffsetNo, String partnerOrderId, Integer redOffsetStatus) {
        TenantUtils.execute(tenantId, () -> redInvoiceMapper.insert(RedInvoiceDO.builder()
                .redOffsetNo(redOffsetNo)
                .partnerOrderId(partnerOrderId)
                .reason("02")
                .amount(new BigDecimal("1000.00"))
                .redOffsetStatus(redOffsetStatus)
                .build()));
    }

}
