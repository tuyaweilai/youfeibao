package cn.iocoder.yudao.module.icbc.service.tax;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.icbc.UnitTestConfiguration;
import cn.iocoder.yudao.module.icbc.controller.admin.tax.vo.TaxSupplementCreateReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.tax.vo.TaxSupplementPageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.tax.vo.TaxSupplementPayReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.tax.vo.TaxSupplementRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.tax.vo.TaxSupplementSummaryRespVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payee.PayeeInfoDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.payee.PayeeInfoMapper;
import cn.iocoder.yudao.module.icbc.enums.TaxSupplementStatusEnum;
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

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.TAX_SUPPLEMENT_STATUS_INVALID;
import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link TaxSupplementServiceImpl} 的单元测试（issue #13）：
 * 待补缴累计金额按 1% 与 3% 分列、补缴可缴清且不重复缴款。
 */
@Import({TaxSupplementServiceImpl.class, UnitTestConfiguration.class, IcbcTenantTestConfiguration.class})
@Sql(scripts = "/sql/create_tables.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Transactional
public class TaxSupplementServiceImplTest extends BaseDbUnitTest {

    private static final Long TENANT_ID = 1L;

    @Resource
    private TaxSupplementService taxSupplementService;
    @Resource
    private PayeeInfoMapper payeeInfoMapper;

    @BeforeEach
    public void setUp() {
        TenantContextHolder.setTenantId(TENANT_ID);
    }

    @AfterEach
    public void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    public void testCreateAndSummary_splitsOnePercentAndThreePercent() {
        PayeeInfoDO payee = insertPayee();
        TaxSupplementCreateReqVO createReq = new TaxSupplementCreateReqVO();
        createReq.setPeriodMonth("2026-08");
        createReq.setPayeeId(payee.getId());
        createReq.setReason("跨期红冲调整");
        createReq.setAmountAtOnePercent(new BigDecimal("100.00"));
        createReq.setAmountAtThreePercent(new BigDecimal("300.00"));

        TaxSupplementRespVO created = taxSupplementService.create(createReq);

        assertEquals(TaxSupplementStatusEnum.PENDING.getStatus(), created.getStatus());
        assertEquals(0, new BigDecimal("400.00").compareTo(created.getAmount()));
        assertEquals("张三", created.getSellerName());
        assertNotNull(created.getSupplementNo());

        TaxSupplementSummaryRespVO summary = taxSupplementService.getSummary();
        assertEquals(1, summary.getPendingCount());
        assertEquals(0, new BigDecimal("400.00").compareTo(summary.getPendingAmount()));
        assertEquals(0, new BigDecimal("100.00").compareTo(summary.getPendingAmountAtOnePercent()));
        assertEquals(0, new BigDecimal("300.00").compareTo(summary.getPendingAmountAtThreePercent()));
        assertTrue(summary.getMessage().contains("1%"), "实际：" + summary.getMessage());
    }

    @Test
    public void testGetPage_filterByStatus() {
        TaxSupplementCreateReqVO createReq = new TaxSupplementCreateReqVO();
        createReq.setPeriodMonth("2026-08");
        createReq.setReason("补缴");
        createReq.setAmountAtThreePercent(new BigDecimal("300.00"));
        taxSupplementService.create(createReq);

        TaxSupplementPageReqVO pageReq = new TaxSupplementPageReqVO();
        pageReq.setStatus(TaxSupplementStatusEnum.PENDING.getStatus());
        PageResult<TaxSupplementRespVO> page = taxSupplementService.getPage(pageReq);

        assertEquals(1, page.getTotal());
        assertEquals(0, new BigDecimal("300.00").compareTo(page.getList().get(0).getAmount()));
    }

    @Test
    public void testRecordPayment_isSingleShot() {
        TaxSupplementCreateReqVO createReq = new TaxSupplementCreateReqVO();
        createReq.setPeriodMonth("2026-08");
        createReq.setReason("补缴");
        createReq.setAmountAtThreePercent(new BigDecimal("300.00"));
        TaxSupplementRespVO created = taxSupplementService.create(createReq);

        TaxSupplementPayReqVO payReq = new TaxSupplementPayReqVO();
        payReq.setId(created.getId());
        payReq.setVoucherNo("V-1");
        TaxSupplementRespVO paid = taxSupplementService.recordPayment(payReq);

        assertEquals(TaxSupplementStatusEnum.PAID.getStatus(), paid.getStatus());
        assertEquals(0, new BigDecimal("300.00").compareTo(paid.getPaidAmount()));
        assertEquals("V-1", paid.getVoucherNo());
        assertEquals(0, taxSupplementService.getSummary().getPendingCount());

        assertServiceException(() -> taxSupplementService.recordPayment(payReq),
                TAX_SUPPLEMENT_STATUS_INVALID, "缴款",
                TaxSupplementStatusEnum.nameOf(TaxSupplementStatusEnum.PAID.getStatus()),
                "已补缴的记录不能重复缴款");
    }

    private PayeeInfoDO insertPayee() {
        return TenantUtils.execute(TENANT_ID, () -> {
            PayeeInfoDO payee = PayeeInfoDO.builder()
                    .partnerPayeeId("PARTNER_SUP")
                    .name("张三")
                    .mobile("13800000001")
                    .idCardNo("110101199001011234")
                    .build();
            payeeInfoMapper.insert(payee);
            return payee;
        });
    }

}
