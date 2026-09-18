package cn.iocoder.yudao.module.icbc.dal;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.icbc.UnitTestConfiguration;
import cn.iocoder.yudao.module.icbc.dal.dataobject.download.InvoiceDownloadDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.download.InvoiceFileDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.invoice.InvoiceOrderDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payee.PayeeInfoDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payment.PaymentOrderDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.download.InvoiceDownloadMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.download.InvoiceFileMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.invoice.InvoiceOrderMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.payee.PayeeInfoMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.payment.PaymentOrderMapper;
import cn.iocoder.yudao.module.icbc.service.platform.PlatformInvoiceQueryService;
import cn.iocoder.yudao.module.icbc.service.platform.impl.PlatformInvoiceQueryServiceImpl;
import cn.iocoder.yudao.test.icbc.IcbcTenantTestConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import javax.annotation.Resource;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 租户隔离的集成测试。
 *
 * <p>打开 MyBatis-Plus 的租户拦截器后，验证：
 * <ul>
 *   <li>一个租户写进去的出售者档案、收购单，另一个租户读不到；</li>
 *   <li>写入时租户编号由当前上下文自动打上；</li>
 *   <li>只有平台运营能跨租户读全量。</li>
 * </ul>
 */
@Import({UnitTestConfiguration.class, IcbcTenantTestConfiguration.class,
        PlatformInvoiceQueryServiceImpl.class})
public class IcbcTenantIsolationTest extends BaseDbUnitTest {

    @Resource
    private PayeeInfoMapper payeeInfoMapper;
    @Resource
    private InvoiceOrderMapper invoiceOrderMapper;
    @Resource
    private PaymentOrderMapper paymentOrderMapper;
    @Resource
    private InvoiceDownloadMapper invoiceDownloadMapper;
    @Resource
    private InvoiceFileMapper invoiceFileMapper;
    @Resource
    private PlatformInvoiceQueryService platformInvoiceQueryService;

    @AfterEach
    public void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    public void testPayeeIsolatedByTenant() {
        // 租户 1 登记一个出售者
        Long payeeId = TenantUtils.execute(1L, () -> {
            PayeeInfoDO payee = newPayee("张三", "110101199001011234", "13800000001");
            payeeInfoMapper.insert(payee);
            return payee.getId();
        });
        assertNotNull(payeeId);

        // 租户 1 读得到
        TenantUtils.execute(1L, () -> assertNotNull(payeeInfoMapper.selectById(payeeId)));
        // 租户 2 读不到
        TenantUtils.execute(2L, () -> {
            assertNull(payeeInfoMapper.selectById(payeeId));
            assertTrue(payeeInfoMapper.selectList().isEmpty());
        });
    }

    @Test
    public void testPayeeSameNaturalPersonCanExistInTwoTenants() {
        // 同一个自然人，在两个租户各自建档，互不冲突也互不可见
        Long payeeId1 = TenantUtils.execute(1L, () -> {
            PayeeInfoDO payee = newPayee("李四", "110101199001011235", "13800000002");
            payeeInfoMapper.insert(payee);
            return payee.getId();
        });
        Long payeeId2 = TenantUtils.execute(2L, () -> {
            PayeeInfoDO payee = newPayee("李四", "110101199001011235", "13800000002");
            payeeInfoMapper.insert(payee);
            return payee.getId();
        });

        assertNotNull(payeeId1);
        assertNotNull(payeeId2);
        TenantUtils.execute(1L, () -> {
            assertNotNull(payeeInfoMapper.selectById(payeeId1));
            assertNull(payeeInfoMapper.selectById(payeeId2));
        });
        TenantUtils.execute(2L, () -> {
            assertNotNull(payeeInfoMapper.selectById(payeeId2));
            assertNull(payeeInfoMapper.selectById(payeeId1));
        });
    }

    @Test
    public void testInvoiceOrderIsolatedByTenant() {
        Long orderId1 = insertOrder(1L, "ORDER_TENANT_1", "PARTNER_TENANT_1");
        Long orderId2 = insertOrder(2L, "ORDER_TENANT_2", "PARTNER_TENANT_2");

        TenantUtils.execute(1L, () -> {
            assertNotNull(invoiceOrderMapper.selectById(orderId1));
            assertNull(invoiceOrderMapper.selectById(orderId2));
            assertEquals(1, invoiceOrderMapper.selectList().size());
        });
        TenantUtils.execute(2L, () -> {
            assertNotNull(invoiceOrderMapper.selectById(orderId2));
            assertNull(invoiceOrderMapper.selectById(orderId1));
            assertEquals(1, invoiceOrderMapper.selectList().size());
        });
    }

    @Test
    public void testInsertStampsCurrentTenant() {
        Long orderId = insertOrder(7L, "ORDER_TENANT_7", "PARTNER_TENANT_7");

        InvoiceOrderDO raw = TenantUtils.executeIgnore(() -> invoiceOrderMapper.selectById(orderId));
        assertNotNull(raw);
        assertEquals(7L, raw.getTenantId());
    }

    @Test
    public void testPlatformOperatorSpansAllTenants() {
        insertOrder(1L, "ORDER_PLATFORM_1", "PARTNER_PLATFORM_1");
        insertOrder(2L, "ORDER_PLATFORM_2", "PARTNER_PLATFORM_2");

        // 平台运营：跨租户读全量
        assertEquals(2, platformInvoiceQueryService.getPlatformInvoiceList().size());
        // 普通租户：只能读到自己的
        TenantUtils.execute(1L, () -> assertEquals(1, invoiceOrderMapper.selectList().size()));
        TenantUtils.execute(2L, () -> assertEquals(1, invoiceOrderMapper.selectList().size()));
    }

    @Test
    public void testPaymentIsolatedByTenant() {
        Long paymentId = TenantUtils.execute(1L, () -> {
            PaymentOrderDO payment = new PaymentOrderDO();
            payment.setOrderNo("PAY_ORDER_1");
            payment.setPartnerOrderId("PAY_PARTNER_1");
            payment.setPaymentStatus(0);
            paymentOrderMapper.insert(payment);
            return payment.getId();
        });

        TenantUtils.execute(1L, () -> assertNotNull(paymentOrderMapper.selectById(paymentId)));
        TenantUtils.execute(2L, () -> assertNull(paymentOrderMapper.selectById(paymentId)));
    }

    @Test
    public void testEvidenceIsolatedByTenant() {
        // 租户 1 下载发票并生成证据文件
        Long fileId = TenantUtils.execute(1L, () -> {
            InvoiceDownloadDO download = new InvoiceDownloadDO();
            download.setInvoiceOrderId(1L);
            download.setPartnerOrderId("DL_PARTNER_1");
            download.setDownloadStatus(2);
            invoiceDownloadMapper.insert(download);

            InvoiceFileDO file = new InvoiceFileDO();
            file.setDownloadId(download.getId());
            file.setInvoiceNumber("INV_FILE_1");
            file.setFileType("PDF");
            file.setFilePath("/tmp/inv-1.pdf");
            file.setFileName("inv-1.pdf");
            invoiceFileMapper.insert(file);
            return file.getId();
        });

        TenantUtils.execute(1L, () -> assertNotNull(invoiceFileMapper.selectById(fileId)));
        TenantUtils.execute(2L, () -> {
            assertNull(invoiceFileMapper.selectById(fileId));
            assertTrue(invoiceFileMapper.selectList().isEmpty());
            assertTrue(invoiceDownloadMapper.selectList().isEmpty());
        });
    }

    private static PayeeInfoDO newPayee(String name, String idCardNo, String mobile) {
        PayeeInfoDO payee = new PayeeInfoDO();
        payee.setPayeeNo("PAYEE_" + mobile);
        payee.setPartnerPayeeId("PARTNER_" + mobile);
        payee.setName(name);
        payee.setIdCardNo(idCardNo);
        payee.setMobile(mobile);
        return payee;
    }

    private Long insertOrder(Long tenantId, String orderNo, String partnerOrderId) {
        return TenantUtils.execute(tenantId, () -> {
            InvoiceOrderDO order = new InvoiceOrderDO();
            order.setOrderNo(orderNo);
            order.setPartnerOrderId(partnerOrderId);
            order.setPayeeId(1L);
            order.setPayeeNo("PAYEE_1");
            order.setPayerId(1L);
            order.setPayerNo("PAYER_1");
            order.setTotalAmount(new BigDecimal("100.00"));
            order.setInvoiceType(1);
            order.setOrderStatus(0);
            order.setInvoiceStatus(0);
            order.setPaymentStatus(0);
            order.setTaxStatus(0);
            invoiceOrderMapper.insert(order);
            return order.getId();
        });
    }

}
