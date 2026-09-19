package cn.iocoder.yudao.module.icbc.dal;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.icbc.UnitTestConfiguration;
import cn.iocoder.yudao.module.icbc.controller.admin.naturalperson.vo.NaturalPersonConflictRespVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.acquisition.IcbcAcquisitionDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.appointment.IcbcAppointmentDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.download.InvoiceDownloadDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.download.InvoiceFileDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.invoice.InvoiceOrderDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.naturalperson.IcbcNaturalPersonDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payee.IcbcPayeeBankCardChangeDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payee.PayeeInfoDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payment.PaymentOrderDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.download.InvoiceDownloadMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.acquisition.IcbcAcquisitionMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.appointment.IcbcAppointmentMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.download.InvoiceFileMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.invoice.InvoiceOrderMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.payee.PayeeBankCardChangeMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.payee.PayeeInfoMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.payment.PaymentOrderMapper;
import cn.iocoder.yudao.module.icbc.service.naturalperson.NaturalPersonService;
import cn.iocoder.yudao.module.icbc.service.naturalperson.impl.NaturalPersonServiceImpl;
import cn.iocoder.yudao.module.icbc.service.payee.PayeeInfoService;
import cn.iocoder.yudao.module.icbc.service.payee.PayeeInfoServiceImpl;
import cn.iocoder.yudao.module.icbc.service.platform.PlatformInvoiceQueryService;
import cn.iocoder.yudao.module.icbc.service.platform.impl.PlatformInvoiceQueryServiceImpl;
import cn.iocoder.yudao.test.icbc.IcbcTenantTestConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
        PlatformInvoiceQueryServiceImpl.class, PayeeInfoServiceImpl.class, NaturalPersonServiceImpl.class})
@Sql(scripts = "/sql/create_tables.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/sql/clean.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
public class IcbcTenantIsolationTest extends BaseDbUnitTest {

    @Resource
    private PayeeInfoService payeeInfoService;
    @Resource
    private NaturalPersonService naturalPersonService;

    @Resource
    private PayeeInfoMapper payeeInfoMapper;
    @Resource
    private PayeeBankCardChangeMapper payeeBankCardChangeMapper;
    @Resource
    private InvoiceOrderMapper invoiceOrderMapper;
    @Resource
    private PaymentOrderMapper paymentOrderMapper;
    @Resource
    private InvoiceDownloadMapper invoiceDownloadMapper;
    @Resource
    private InvoiceFileMapper invoiceFileMapper;
    @Resource
    private IcbcAcquisitionMapper acquisitionMapper;
    @Resource
    private IcbcAppointmentMapper appointmentMapper;
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
    public void testSameNaturalPersonAcrossTenantsSharesOneIdentity() {
        // ADR 0017：同一个人在两家回收企业各有一条收方档案，但只有一个自然人主体、一个 outUserId
        String idCardNo = "110101199001011236";
        String mobile = "13800000003";
        Long payeeId1 = TenantUtils.execute(1L, () -> payeeInfoService.createPayeeInfo(
                payeeSaveReq("赵五", idCardNo, mobile)));
        Long payeeId2 = TenantUtils.execute(2L, () -> payeeInfoService.createPayeeInfo(
                payeeSaveReq("赵五", idCardNo, mobile)));

        Long personId1 = TenantUtils.execute(1L, () -> payeeInfoMapper.selectById(payeeId1).getNaturalPersonId());
        Long personId2 = TenantUtils.execute(2L, () -> payeeInfoMapper.selectById(payeeId2).getNaturalPersonId());
        assertEquals(personId1, personId2);

        // 实人认证记在主体上：一次通过，两家企业的建档总览都看得到
        TenantUtils.executeIgnore(() -> {
            naturalPersonService.applyRealNameResult(personId1, true, null);
            return null;
        });
        PayeeInfoDO payee1 = TenantUtils.execute(1L, () -> payeeInfoMapper.selectById(payeeId1));
        assertEquals(personId1, payee1.getNaturalPersonId());
        // 收方档案是租户级的：别家企业的档案在本租户看不到
        TenantUtils.execute(2L, () -> assertNull(payeeInfoMapper.selectById(payeeId1)));
    }

    private cn.iocoder.yudao.module.icbc.controller.admin.payee.vo.PayeeInfoSaveReqVO payeeSaveReq(
            String name, String idCardNo, String mobile) {
        cn.iocoder.yudao.module.icbc.controller.admin.payee.vo.PayeeInfoSaveReqVO reqVO =
                new cn.iocoder.yudao.module.icbc.controller.admin.payee.vo.PayeeInfoSaveReqVO();
        reqVO.setName(name);
        reqVO.setIdCardNo(idCardNo);
        reqVO.setMobile(mobile);
        return reqVO;
    }

    @Test
    public void testAppointmentIsolatedByTenant() {
        // 预约是租户表：他扫码约的是某一家企业的场站，别家企业看不到
        Long appointmentId = TenantUtils.execute(1L, () -> {
            IcbcAppointmentDO appointment = IcbcAppointmentDO.builder()
                    .appointmentNo("APT_TENANT_1")
                    .naturalPersonId(1L)
                    .stationId(1L)
                    .goodsConfigId(1L)
                    .status(0)
                    .build();
            appointmentMapper.insert(appointment);
            return appointment.getId();
        });

        TenantUtils.execute(1L, () -> assertNotNull(appointmentMapper.selectById(appointmentId)));
        TenantUtils.execute(2L, () -> {
            assertNull(appointmentMapper.selectById(appointmentId));
            assertTrue(appointmentMapper.selectList().isEmpty());
        });
        // 自然人本人跨企业看自己的预约：显式开阀读取
        assertNotNull(TenantUtils.executeIgnore(() -> appointmentMapper.selectById(appointmentId)));
    }

    @Test
    public void testBankCardChangeIsolatedByTenant() {
        // 换卡是租户表：在 A 企业换的卡，B 企业看不到（ADR 0017 的交易可见性边界）
        Long changeId = TenantUtils.execute(1L, () -> {
            IcbcPayeeBankCardChangeDO change = IcbcPayeeBankCardChangeDO.builder()
                    .changeNo("BC_TENANT_1")
                    .payeeId(1L)
                    .naturalPersonId(1L)
                    .status(0)
                    .newBankCardNo("6222029999888877")
                    .requestedAt(java.time.LocalDateTime.now())
                    .build();
            payeeBankCardChangeMapper.insert(change);
            return change.getId();
        });

        TenantUtils.execute(1L, () -> assertNotNull(payeeBankCardChangeMapper.selectById(changeId)));
        TenantUtils.execute(2L, () -> {
            assertNull(payeeBankCardChangeMapper.selectById(changeId));
            assertTrue(payeeBankCardChangeMapper.selectList().isEmpty());
        });
        // 自然人本人在自己的资料里看得到：显式开阀读取
        assertNotNull(TenantUtils.executeIgnore(() -> payeeBankCardChangeMapper.selectById(changeId)));
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

    @Test
    public void testAcquisitionIsolatedByTenant() {
        Long acquisitionId = TenantUtils.execute(1L, () -> {
            IcbcAcquisitionDO acquisition = new IcbcAcquisitionDO();
            acquisition.setAcquisitionNo("ACQ_TENANT_1");
            acquisition.setPayeeId(1L);
            acquisition.setStatus(0);
            acquisitionMapper.insert(acquisition);
            return acquisition.getId();
        });

        TenantUtils.execute(1L, () -> assertNotNull(acquisitionMapper.selectById(acquisitionId)));
        TenantUtils.execute(2L, () -> {
            assertNull(acquisitionMapper.selectById(acquisitionId));
            assertTrue(acquisitionMapper.selectList().isEmpty());
        });
    }

    @Test
    public void testIdentityConflictListSpansTenantsAndMasksPersonalData() {
        // 同一身份证在两家企业：姓名一致、手机号不一致 → 命中 ADR 0017「不自动合并」的人工清单
        TenantUtils.execute(1L, () -> payeeInfoMapper.insert(
                newPayee("张三", "110101199001011234", "13800000011")));
        TenantUtils.execute(2L, () -> payeeInfoMapper.insert(
                newPayee("张三", "110101199001011234", "13800000012")));
        // 同一身份证、姓名与手机号完全一致：跨企业卖货的正常情形，不算冲突
        TenantUtils.execute(1L, () -> payeeInfoMapper.insert(
                newPayee("李四", "110101199001011235", "13800000021")));
        TenantUtils.execute(2L, () -> payeeInfoMapper.insert(
                newPayee("李四", "110101199001011235", "13800000021")));

        List<NaturalPersonConflictRespVO> conflicts = naturalPersonService.getIdentityConflictList();

        assertEquals(1, conflicts.size());
        NaturalPersonConflictRespVO conflict = conflicts.get(0);
        assertEquals(2, conflict.getRecordCount());
        // 脱敏展示：平台运营看到的是「是谁」，不是可复制走的原始证件号
        assertNotEquals("110101199001011234", conflict.getIdCardNo());
        assertTrue(conflict.getIdCardNo().startsWith("110101"));
        // 跨租户可见：两条档案分属租户 1 与 2
        Set<Long> tenantIds = conflict.getRecords().stream()
                .map(NaturalPersonConflictRespVO.Record::getTenantId).collect(Collectors.toSet());
        assertEquals(Set.of(1L, 2L), tenantIds);
        assertTrue(conflict.getRecords().stream().allMatch(r -> r.getMobile().contains("*")));
        // 尚未建档时不给主体编号
        assertNull(conflict.getNaturalPersonId());
    }

    @Test
    public void testIdentityConflictListFillsNaturalPersonWhenRegistered() {
        // 按身份证已建档的主体：清单要能直接给出编号，便于运营跳过去处置
        Long personId = TenantUtils.executeIgnore(() -> naturalPersonService.register(
                registerReq("王六", "110101199001011237", "13800000031")).getId());
        TenantUtils.execute(1L, () -> payeeInfoMapper.insert(
                newPayee("王六", "110101199001011237", "13800000031")));
        TenantUtils.execute(2L, () -> payeeInfoMapper.insert(
                newPayee("王六", "110101199001011237", "13800000032")));

        List<NaturalPersonConflictRespVO> conflicts = naturalPersonService.getIdentityConflictList();

        assertEquals(1, conflicts.size());
        assertEquals(personId, conflicts.get(0).getNaturalPersonId());
        assertNotNull(conflicts.get(0).getOutUserId());
    }

    @Test
    public void testIdentityConflictListEmptyWhenNoConflict() {
        // 同一身份证跨企业但信息一致：不该出现在人工清单里
        TenantUtils.execute(1L, () -> payeeInfoMapper.insert(
                newPayee("赵七", "110101199001011238", "13800000041")));
        TenantUtils.execute(2L, () -> payeeInfoMapper.insert(
                newPayee("赵七", "110101199001011238", "13800000041")));

        assertTrue(naturalPersonService.getIdentityConflictList().isEmpty());
    }

    private cn.iocoder.yudao.module.icbc.controller.admin.naturalperson.vo.NaturalPersonRegisterReqVO registerReq(
            String name, String idCardNo, String mobile) {
        cn.iocoder.yudao.module.icbc.controller.admin.naturalperson.vo.NaturalPersonRegisterReqVO reqVO =
                new cn.iocoder.yudao.module.icbc.controller.admin.naturalperson.vo.NaturalPersonRegisterReqVO();
        reqVO.setName(name);
        reqVO.setIdCardNo(idCardNo);
        reqVO.setMobile(mobile);
        return reqVO;
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
