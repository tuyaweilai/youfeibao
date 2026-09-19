package cn.iocoder.yudao.module.icbc.service.tax;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.icbc.UnitTestConfiguration;
import cn.iocoder.yudao.module.icbc.controller.admin.tax.vo.SellerSettlementStatementRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.tax.vo.SettlementReminderHandleReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.tax.vo.SettlementReminderPageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.tax.vo.SettlementReminderRespVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.invoice.InvoiceOrderDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payee.PayeeInfoDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.invoice.InvoiceOrderMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.payee.PayeeInfoMapper;
import cn.iocoder.yudao.module.icbc.enums.InvoiceIssueStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.PreInvoiceStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.SettlementReminderStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.TaxStatusEnum;
import cn.iocoder.yudao.module.icbc.service.tax.impl.AnnualSettlementServiceImpl;
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

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.PAYEE_NOT_EXISTS;
import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link AnnualSettlementServiceImpl} 的单元测试（issue #13）：
 * 汇算清缴提醒的生成与幂等、出售者能取得自己的开票与已缴税款信息。
 */
@Import({AnnualSettlementServiceImpl.class, UnitTestConfiguration.class, IcbcTenantTestConfiguration.class})
@Sql(scripts = "/sql/create_tables.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Transactional
public class AnnualSettlementServiceImplTest extends BaseDbUnitTest {

    private static final Long TENANT_ID = 1L;

    @Resource
    private AnnualSettlementService annualSettlementService;
    @Resource
    private PayeeInfoMapper payeeInfoMapper;
    @Resource
    private InvoiceOrderMapper invoiceOrderMapper;

    @BeforeEach
    public void setUp() {
        TenantContextHolder.setTenantId(TENANT_ID);
    }

    @AfterEach
    public void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    public void testRemind_createsReminderWithStatementAndIsIdempotent() {
        int taxYear = LocalDate.now().getYear();
        PayeeInfoDO payee = insertPayee("张三", "110101199001011234", "13800000001");
        insertInvoice("ORDER_1", payee, new BigDecimal("120000.00"), new BigDecimal("1872.00"), taxYear);
        insertInvoice("ORDER_2", payee, new BigDecimal("80000.00"), new BigDecimal("400.00"), taxYear);

        int created = annualSettlementService.remind(taxYear);

        assertEquals(1, created);
        assertEquals(0, annualSettlementService.remind(taxYear), "重复执行应幂等");

        SettlementReminderPageReqVO pageReq = new SettlementReminderPageReqVO();
        pageReq.setTaxYear(taxYear);
        PageResult<SettlementReminderRespVO> page = annualSettlementService.getPage(pageReq);
        assertEquals(1, page.getTotal());
        SettlementReminderRespVO reminder = page.getList().get(0);
        assertEquals(SettlementReminderStatusEnum.PENDING.getStatus(), reminder.getStatus());
        assertEquals(2, reminder.getInvoiceCount());
        assertEquals(0, new BigDecimal("200000.00").compareTo(reminder.getInvoicedAmount()));
        assertEquals(0, new BigDecimal("2272.00").compareTo(reminder.getPaidTaxAmount()));
        // 个税预缴口径：销售额 × 0.5%
        assertEquals(0, new BigDecimal("1000.00").compareTo(reminder.getIitAmount()));
        // 截止日 = 次年 3 月 31 日
        assertEquals(LocalDate.of(taxYear + 1, 3, 31), reminder.getDeadline());
    }

    @Test
    public void testGetStatement_returnsInvoicingAndPaidTax() {
        int taxYear = LocalDate.now().getYear();
        PayeeInfoDO payee = insertPayee("李四", "110101199002022345", "13800000002");
        insertInvoice("ORDER_STATEMENT", payee, new BigDecimal("120000.00"), new BigDecimal("1872.00"), taxYear);

        SellerSettlementStatementRespVO statement = annualSettlementService.getStatement(payee.getId(), taxYear);

        assertEquals(taxYear, statement.getTaxYear());
        assertEquals("李四", statement.getSellerName());
        assertNotNull(statement.getIdCardMasked());
        assertTrue(statement.getIdCardMasked().contains("*"));
        assertEquals(1, statement.getInvoiceCount());
        assertEquals(0, new BigDecimal("120000.00").compareTo(statement.getInvoicedAmount()));
        assertEquals(0, new BigDecimal("1872.00").compareTo(statement.getPaidTaxAmount()));
        assertEquals(1, statement.getMonths().size());
        assertTrue(statement.getMessage().contains("汇算清缴"), "实际：" + statement.getMessage());
    }

    @Test
    public void testGetStatement_noInvoiceReturnsEmptyStatement() {
        PayeeInfoDO payee = insertPayee("王五", "110101199003033456", "13800000003");

        SellerSettlementStatementRespVO statement =
                annualSettlementService.getStatement(payee.getId(), LocalDate.now().getYear());

        assertEquals(0, statement.getInvoiceCount());
        assertEquals(0, BigDecimal.ZERO.compareTo(statement.getInvoicedAmount()));
        assertTrue(statement.getMessage().contains("无需汇算清缴"), "实际：" + statement.getMessage());
    }

    @Test
    public void testGetStatement_payeeNotExists() {
        assertServiceException(() -> annualSettlementService.getStatement(99999L, 2026), PAYEE_NOT_EXISTS);
    }

    @Test
    public void testHandle_marksReminded() {
        int taxYear = LocalDate.now().getYear();
        PayeeInfoDO payee = insertPayee("赵六", "110101199004044567", "13800000004");
        insertInvoice("ORDER_HANDLE", payee, new BigDecimal("120000.00"), new BigDecimal("1872.00"), taxYear);
        annualSettlementService.remind(taxYear);
        SettlementReminderPageReqVO pageReq = new SettlementReminderPageReqVO();
        Long id = annualSettlementService.getPage(pageReq).getList().get(0).getId();

        SettlementReminderHandleReqVO handleReq = new SettlementReminderHandleReqVO();
        handleReq.setId(id);
        handleReq.setStatus(SettlementReminderStatusEnum.REMINDED.getStatus());
        handleReq.setHandleRemark("已生成免登录链接发给出售者");
        SettlementReminderRespVO handled = annualSettlementService.handle(handleReq);

        assertEquals(SettlementReminderStatusEnum.REMINDED.getStatus(), handled.getStatus());
        assertEquals("已生成免登录链接发给出售者", handled.getHandleRemark());
    }

    // ==================== 造数 ====================

    private PayeeInfoDO insertPayee(String name, String idCardNo, String mobile) {
        return TenantUtils.execute(TENANT_ID, () -> {
            PayeeInfoDO payee = PayeeInfoDO.builder()
                    .partnerPayeeId("PARTNER_" + mobile)
                    .name(name)
                    .mobile(mobile)
                    .idCardNo(idCardNo)
                    .payeeNo("PAYEE_" + mobile)
                    .build();
            payeeInfoMapper.insert(payee);
            return payee;
        });
    }

    private void insertInvoice(String partnerOrderId, PayeeInfoDO payee, BigDecimal amount,
                               BigDecimal paidTax, int taxYear) {
        TenantUtils.execute(TENANT_ID, () -> {
            LocalDateTime invoiceDate = LocalDate.of(taxYear, 6, 5).atTime(10, 0);
            InvoiceOrderDO order = InvoiceOrderDO.builder()
                    .orderNo("INV_" + partnerOrderId)
                    .partnerOrderId(partnerOrderId)
                    .payeeId(payee.getId())
                    .payeeNo(payee.getPartnerPayeeId())
                    .payerNo("PAYER_1")
                    .totalAmount(amount)
                    .invoiceAmount(amount)
                    .taxRate(new BigDecimal("0.01"))
                    .invoiceType(1)
                    .businessType("SCRAP")
                    .orderStatus(0)
                    .invoiceStatus(InvoiceIssueStatusEnum.ISSUED.getStatus())
                    .paymentStatus(0)
                    .taxStatus(TaxStatusEnum.SUCCESS.getStatus())
                    .preInvoiceStatus(PreInvoiceStatusEnum.SUCCESS.getStatus())
                    .invoiceDate(invoiceDate)
                    .invoiceNo("INVNO_" + partnerOrderId)
                    .taxRealAmount(paidTax)
                    .build();
            order.setCreateTime(invoiceDate);
            invoiceOrderMapper.insert(order);
        });
    }

}
