package cn.iocoder.yudao.module.icbc.service.platform;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.icbc.UnitTestConfiguration;
import cn.iocoder.yudao.module.icbc.controller.admin.evidence.vo.EvidenceCompletenessItemRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.evidence.vo.EvidenceCompletenessSummaryRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.platform.vo.PlatformExceptionInvoiceRespVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.invoice.InvoiceOrderDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.invoice.RedInvoiceDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.invoice.InvoiceOrderMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.invoice.RedInvoiceMapper;
import cn.iocoder.yudao.module.icbc.enums.InvoiceIssueStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.PaymentStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.RedOffsetStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.TaxStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.UploadStatusEnum;
import cn.iocoder.yudao.module.icbc.service.evidence.impl.EvidencePackageWriter;
import cn.iocoder.yudao.module.icbc.service.evidence.impl.InvoiceEvidenceServiceImpl;
import cn.iocoder.yudao.module.icbc.service.platform.impl.PlatformEvidenceServiceImpl;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 平台运营全平台证据与异常票的测试（#15）。
 *
 * <p>平台运营要能看到全平台的五流齐备率，以及「哪几张票经不起查」：
 * 状态线异常或已开票但五流不齐。
 */
@Import({UnitTestConfiguration.class, IcbcTenantTestConfiguration.class,
        InvoiceEvidenceServiceImpl.class, EvidencePackageWriter.class, PlatformEvidenceServiceImpl.class})
@Sql(scripts = "/sql/create_tables.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/sql/clean.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
public class PlatformEvidenceServiceImplTest extends BaseDbUnitTest {

    @Resource
    private PlatformEvidenceService platformEvidenceService;

    @Resource
    private InvoiceOrderMapper invoiceOrderMapper;

    @Resource
    private RedInvoiceMapper redInvoiceMapper;

    @AfterEach
    public void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    public void testPlatformCompletenessSpansTenants() {
        insertOrder(1L, "ORDER_T1", "INV_T1", null);
        insertOrder(2L, "ORDER_T2", null, null);

        EvidenceCompletenessSummaryRespVO summary = platformEvidenceService.getPlatformCompleteness();

        assertEquals(2, summary.getInvoiceCount());
        Set<Long> tenantIds = summary.getItems().stream()
                .map(EvidenceCompletenessItemRespVO::getTenantId).collect(Collectors.toSet());
        assertTrue(tenantIds.containsAll(Set.of(1L, 2L)));
    }

    @Test
    public void testExceptionListFlagsAllStatusLines() {
        insertOrder(1L, "ORDER_FAILED", null, order -> {
            order.setInvoiceStatus(InvoiceIssueStatusEnum.FAILED.getStatus());
            order.setTaxStatus(TaxStatusEnum.FAILED.getStatus());
            order.setUploadStatus(UploadStatusEnum.FAILED.getStatus());
            order.setPaymentStatus(PaymentStatusEnum.FAILED.getStatus());
        });

        List<PlatformExceptionInvoiceRespVO> list = platformEvidenceService.getExceptionInvoiceList();

        PlatformExceptionInvoiceRespVO vo = findByPartnerOrderId(list, "ORDER_FAILED");
        assertTrue(vo.getReasons().contains("开票失败"));
        assertTrue(vo.getReasons().contains("缴税失败"));
        assertTrue(vo.getReasons().contains("上传失败"));
        assertTrue(vo.getReasons().contains("付款异常：支付失败"));
        assertEquals(1L, vo.getTenantId());
    }

    @Test
    public void testExceptionListFlagsRedOffsetFailure() {
        insertOrder(1L, "ORDER_RED", null, null);
        TenantUtils.execute(1L, () -> redInvoiceMapper.insert(RedInvoiceDO.builder()
                .redOffsetNo("RED_FAILED")
                .partnerOrderId("ORDER_RED")
                .reason("01")
                .amount(new BigDecimal("1000.00"))
                .redOffsetStatus(RedOffsetStatusEnum.UPLOAD_FAILED.getStatus())
                .build()));

        List<PlatformExceptionInvoiceRespVO> list = platformEvidenceService.getExceptionInvoiceList();

        PlatformExceptionInvoiceRespVO vo = findByPartnerOrderId(list, "ORDER_RED");
        assertTrue(vo.getReasons().contains("红冲异常：上传失败"));
    }

    @Test
    public void testExceptionListFlagsIssuedInvoiceWithIncompleteEvidence() {
        insertOrder(1L, "ORDER_ISSUED_INCOMPLETE", "INV_INCOMPLETE", null);

        List<PlatformExceptionInvoiceRespVO> list = platformEvidenceService.getExceptionInvoiceList();

        PlatformExceptionInvoiceRespVO vo = findByPartnerOrderId(list, "ORDER_ISSUED_INCOMPLETE");
        assertTrue(vo.getReasons().stream().anyMatch(reason -> reason.startsWith("五流不齐")),
                "实际：" + vo.getReasons());
        assertFalse(vo.getMissingFlows().isEmpty());
    }

    @Test
    public void testExceptionListExcludesHealthyInvoice() {
        // 未开票、无任何状态异常、也未到谈证据齐备的阶段：不算异常票
        insertOrder(1L, "ORDER_HEALTHY", null, null);

        assertTrue(platformEvidenceService.getExceptionInvoiceList().stream()
                .noneMatch(vo -> "ORDER_HEALTHY".equals(vo.getPartnerOrderId())));
    }

    private PlatformExceptionInvoiceRespVO findByPartnerOrderId(List<PlatformExceptionInvoiceRespVO> list,
                                                                String partnerOrderId) {
        return list.stream().filter(vo -> partnerOrderId.equals(vo.getPartnerOrderId()))
                .findFirst().orElseThrow(() -> new AssertionError("异常票清单里没有 " + partnerOrderId));
    }

    private void insertOrder(Long tenantId, String partnerOrderId, String invoiceNo,
                             java.util.function.Consumer<InvoiceOrderDO> customizer) {
        TenantUtils.execute(tenantId, () -> {
            InvoiceOrderDO order = InvoiceOrderDO.builder()
                    .orderNo("INV_" + partnerOrderId)
                    .partnerOrderId(partnerOrderId)
                    .payeeNo("PAYEE_" + partnerOrderId)
                    .payerNo("PAYER_1")
                    .totalAmount(new BigDecimal("1000.00"))
                    .invoiceType(1)
                    .businessType("SCRAP")
                    .orderStatus(3)
                    .invoiceStatus(invoiceNo != null ? InvoiceIssueStatusEnum.ISSUED.getStatus()
                            : InvoiceIssueStatusEnum.NOT_ISSUED.getStatus())
                    .paymentStatus(PaymentStatusEnum.PENDING.getStatus())
                    .taxStatus(TaxStatusEnum.NOT_TAXED.getStatus())
                    .uploadStatus(UploadStatusEnum.NOT_UPLOADED.getStatus())
                    .invoiceNo(invoiceNo)
                    .build();
            if (customizer != null) {
                customizer.accept(order);
            }
            invoiceOrderMapper.insert(order);
        });
    }

}
