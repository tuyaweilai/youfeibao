package cn.iocoder.yudao.module.icbc.service.evidence;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.icbc.controller.admin.evidence.vo.*;
import cn.iocoder.yudao.module.icbc.dal.dataobject.acquisition.IcbcAcquisitionDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.download.InvoiceDownloadDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.download.InvoiceFileDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.invoice.InvoiceOrderDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.invoice.OrderItemDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payee.PayeeInfoDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payment.PaymentOrderDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.download.InvoiceDownloadMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.acquisition.IcbcAcquisitionMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.agreement.IcbcFrameworkAgreementMapper;
import cn.iocoder.yudao.module.icbc.dal.dataobject.agreement.IcbcFrameworkAgreementDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.download.InvoiceFileMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.invoice.InvoiceOrderMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.invoice.OrderItemMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.payee.PayeeInfoMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.payment.PaymentOrderMapper;
import cn.iocoder.yudao.module.icbc.service.evidence.impl.EvidencePackageWriter;
import cn.iocoder.yudao.module.icbc.service.evidence.impl.InvoiceEvidenceServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link InvoiceEvidenceServiceImpl} 的单元测试。
 *
 * <p>从 Service 接口进去，用真实 Mapper 读真库，验证的是「一张票的五流能不能被
 * 正确聚合、缺哪一流能不能被指出」这类行为，不碰实现细节。
 */
@Import({InvoiceEvidenceServiceImpl.class, EvidencePackageWriter.class})
@Sql(scripts = "/sql/create_tables.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Transactional
public class InvoiceEvidenceServiceImplTest extends BaseDbUnitTest {

    @Resource
    private InvoiceEvidenceService invoiceEvidenceService;
    @Resource
    private InvoiceOrderMapper invoiceOrderMapper;
    @Resource
    private OrderItemMapper orderItemMapper;
    @Resource
    private PayeeInfoMapper payeeInfoMapper;
    @Resource
    private PaymentOrderMapper paymentOrderMapper;
    @Resource
    private InvoiceDownloadMapper invoiceDownloadMapper;
    @Resource
    private InvoiceFileMapper invoiceFileMapper;
    @Resource
    private IcbcAcquisitionMapper acquisitionMapper;
    @Resource
    private IcbcFrameworkAgreementMapper frameworkAgreementMapper;

    @Test
    public void testGetEvidenceChain_acquisitionSuppliesContractGoodsAndInfo() {
        // 一笔收购登记单挂到这张票上：合同流 / 货物流 / 信息流应自动成形，无需人工补录
        PayeeInfoDO payee = insertPayee("秦十五", "13300133000", "北京市朝阳区");
        InvoiceOrderDO order = insertOrder("ORDER_ACQ", "INV_ACQ", payee.getId(), "55555555555555555555");
        insertAcquisition(order.getPartnerOrderId(), "ACQ20261201000001", payee.getId());

        // 调用
        EvidenceChainRespVO chain = invoiceEvidenceService.getEvidenceChain("ORDER_ACQ");

        // 断言：三流来自收购登记单
        assertTrue(flowPresent(chain, "CONTRACT"));
        assertTrue(flowPresent(chain, "GOODS"));
        assertTrue(flowPresent(chain, "INFO"));
        assertTrue(flowSources(chain, "CONTRACT").stream()
                .anyMatch(source -> "单笔收购确认书".equals(source.getTitle())));
        assertTrue(flowSources(chain, "GOODS").stream()
                .anyMatch(source -> "过磅单".equals(source.getTitle())));
        assertTrue(flowSources(chain, "GOODS").stream()
                .anyMatch(source -> "车头照片".equals(source.getTitle())));
        assertTrue(flowSources(chain, "INFO").stream()
                .anyMatch(source -> "ACQ20261201000001".equals(source.getRef())));
    }

    @Test
    public void testGetEvidenceChain_effectiveFrameworkAgreementLandsInContractFlow() {
        // 出售者有一份**生效中**的框架收购协议：#95 之后它挂进该出售者的合同流（两份文书一个合同组）
        PayeeInfoDO payee = insertPayee("秦十六", "13300133001", "北京市海淀区");
        InvoiceOrderDO order = insertOrder("ORDER_FW", "INV_FW", payee.getId(), null);
        insertItem(order);
        IcbcFrameworkAgreementDO agreement = IcbcFrameworkAgreementDO.builder()
                .payeeId(payee.getId()).agreementNo("FW202609210001")
                .productName("废钢").quantity("5 吨").specification("重型")
                .recyclePeriod("2026 年 9 月第 1 期").settlementMethod("银行转账")
                .signMethod("ELECTRONIC").status(1)
                .fileUrl("https://esign/signed/agreement.pdf")
                .noticeFileUrl("https://esign/signed/notice.pdf")
                .build();
        frameworkAgreementMapper.insert(agreement);

        EvidenceChainRespVO chain = invoiceEvidenceService.getEvidenceChain("ORDER_FW");

        assertTrue(flowPresent(chain, "CONTRACT"));
        assertTrue(flowSources(chain, "CONTRACT").stream()
                        .anyMatch(source -> "框架收购协议".equals(source.getTitle())),
                "生效协议要出现在合同流里（挂进该出售者的证据链）");
        assertTrue(flowSources(chain, "CONTRACT").stream()
                        .anyMatch(source -> "https://esign/signed/agreement.pdf".equals(source.getUrl())),
                "已签文件地址要带得出来，供查验时下载");
        // SP-2：告知函单独成条（ADR 0036 决策 3：两份文书要能分别引用），同归 FRAMEWORK_AGREEMENT 类型
        assertTrue(flowSources(chain, "CONTRACT").stream()
                        .anyMatch(source -> "反向发票合规告知函".equals(source.getTitle())
                                && "https://esign/signed/notice.pdf".equals(source.getUrl())),
                "告知函要以独立条目进合同流，而不是被主文书吞掉");
        assertEquals(2, flowSources(chain, "CONTRACT").stream()
                        .filter(source -> "FRAMEWORK_AGREEMENT".equals(source.getSourceType())).count(),
                "两份文书各自成条，不新增证据类型");
    }

    @Test
    public void testGetLedgerRows_prefersAcquisitionTradeFields() {
        PayeeInfoDO payee = insertPayee("尤十六", "13200132000", "上海市");
        InvoiceOrderDO order = insertOrder("ORDER_ACQ_LEDGER", "INV_ACQ_LEDGER", payee.getId(), null);
        insertAcquisition(order.getPartnerOrderId(), "ACQ20261201000002", payee.getId());

        AcquisitionLedgerReqVO reqVO = new AcquisitionLedgerReqVO();
        reqVO.setPartnerOrderId("ORDER_ACQ_LEDGER");
        List<AcquisitionLedgerRespVO> rows = invoiceEvidenceService.getLedgerRows(reqVO);

        assertEquals(1, rows.size());
        AcquisitionLedgerRespVO row = rows.get(0);
        assertEquals(LocalDateTime.of(2026, 12, 1, 10, 0), row.getTradeTime());
        assertEquals("北京市朝阳区回收站", row.getTradeAddress());
        assertEquals("废钢", row.getProductName());
        assertEquals("吨", row.getUnit());
        assertEquals(0, new BigDecimal("5").compareTo(row.getQuantity()));
        assertEquals(0, new BigDecimal("500.00").compareTo(row.getAmount()));
    }

    @Test
    public void testGetEvidenceChain_allFiveFlowsPresent() {
        // 一张走完全程的票：有收购单与明细、支付成功、发票与原件，再补录合同与货物证据
        PayeeInfoDO payee = insertPayee("张三", "13800138000", "北京市朝阳区");
        InvoiceOrderDO order = insertOrder("ORDER_FULL", "INV_FULL", payee.getId(), "12345678901234567890");
        insertItem(order);
        insertPayment("ORDER_FULL", "PAY20240101001");
        insertDownloadAndFile("ORDER_FULL", order.getId(), "12345678901234567890");
        invoiceEvidenceService.attachEvidence(attachReq("ORDER_FULL", "FRAMEWORK_AGREEMENT"));
        invoiceEvidenceService.attachEvidence(attachReq("ORDER_FULL", "WEIGHBRIDGE_TICKET"));

        // 调用
        EvidenceChainRespVO chain = invoiceEvidenceService.getEvidenceChain("ORDER_FULL");

        // 断言：五流齐备
        assertEquals(5, chain.getPresentCount());
        assertEquals(5, chain.getTotalCount());
        assertTrue(chain.getComplete());
        assertEquals(0, new BigDecimal("100.00").compareTo(chain.getCompletenessRate()));
        assertTrue(flowPresent(chain, "CONTRACT"));
        assertTrue(flowPresent(chain, "GOODS"));
        assertTrue(flowPresent(chain, "CAPITAL"));
        assertTrue(flowPresent(chain, "INVOICE"));
        assertTrue(flowPresent(chain, "INFO"));
        // 资金流取的是支付成功后归档的转账回单
        assertEquals("转账回单", flowSources(chain, "CAPITAL").get(0).getTitle());
        assertEquals("PAY20240101001", flowSources(chain, "CAPITAL").get(0).getRef());
    }

    @Test
    public void testGetEvidenceChain_onlyLedgerMissingTheOtherFour() {
        // 只有收购单与明细：信息流在，其余四流缺
        PayeeInfoDO payee = insertPayee("李四", "13900139000", "上海市浦东新区");
        InvoiceOrderDO order = insertOrder("ORDER_PARTIAL", "INV_PARTIAL", payee.getId(), null);
        insertItem(order);

        // 调用
        EvidenceChainRespVO chain = invoiceEvidenceService.getEvidenceChain("ORDER_PARTIAL");

        // 断言
        assertEquals(1, chain.getPresentCount());
        assertFalse(chain.getComplete());
        assertEquals(0, new BigDecimal("20.00").compareTo(chain.getCompletenessRate()));
        assertTrue(flowPresent(chain, "INFO"));
        assertTrue(flowSources(chain, "INFO").get(0).getTitle().contains("台账"));
        assertFalse(flowPresent(chain, "CONTRACT"));
        assertFalse(flowPresent(chain, "GOODS"));
        assertFalse(flowPresent(chain, "CAPITAL"));
        assertFalse(flowPresent(chain, "INVOICE"));
    }

    @Test
    public void testAttachAndDeleteEvidence_movesGoodsFlow() {
        // 没有任何货物证据时货物流缺失
        InvoiceOrderDO order = insertOrder("ORDER_GOODS", "INV_GOODS", null, null);
        insertItem(order);
        assertFalse(flowPresent(invoiceEvidenceService.getEvidenceChain("ORDER_GOODS"), "GOODS"));

        // 补录过磅单后货物流齐备
        Long evidenceId = invoiceEvidenceService.attachEvidence(attachReq("ORDER_GOODS", "WEIGHBRIDGE_TICKET"));
        EvidenceChainRespVO afterAttach = invoiceEvidenceService.getEvidenceChain("ORDER_GOODS");
        assertTrue(flowPresent(afterAttach, "GOODS"));
        assertEquals(1, afterAttach.getAttachments().size());

        // 删除后回到缺失
        invoiceEvidenceService.deleteEvidence(evidenceId);
        assertFalse(flowPresent(invoiceEvidenceService.getEvidenceChain("ORDER_GOODS"), "GOODS"));
    }

    @Test
    public void testAttachEvidence_invalidType() {
        insertOrder("ORDER_BAD_TYPE", "INV_BAD_TYPE", null, null);
        assertServiceException(
                () -> invoiceEvidenceService.attachEvidence(attachReq("ORDER_BAD_TYPE", "NOT_A_TYPE")),
                EVIDENCE_TYPE_INVALID);
    }

    @Test
    public void testAttachEvidence_orderNotExists() {
        assertServiceException(
                () -> invoiceEvidenceService.attachEvidence(attachReq("ORDER_MISSING", "FRAMEWORK_AGREEMENT")),
                INVOICE_ORDER_NOT_EXISTS);
    }

    @Test
    public void testDeleteEvidence_notExists() {
        assertServiceException(() -> invoiceEvidenceService.deleteEvidence(999L), EVIDENCE_NOT_EXISTS);
    }

    @Test
    public void testGetCompleteness_batchRate() {
        // 一张齐备、一张只有信息流 → 6/10 = 60%
        PayeeInfoDO payee = insertPayee("王五", "13700137000", "广州市天河区");
        InvoiceOrderDO full = insertOrder("ORDER_C1", "INV_C1", payee.getId(), "11111111111111111111");
        insertItem(full);
        insertPayment("ORDER_C1", "PAY_C1");
        insertDownloadAndFile("ORDER_C1", full.getId(), "11111111111111111111");
        invoiceEvidenceService.attachEvidence(attachReq("ORDER_C1", "FRAMEWORK_AGREEMENT"));
        invoiceEvidenceService.attachEvidence(attachReq("ORDER_C1", "WEIGHBRIDGE_TICKET"));

        InvoiceOrderDO partial = insertOrder("ORDER_C2", "INV_C2", payee.getId(), null);
        insertItem(partial);

        EvidenceScopeReqVO scope = new EvidenceScopeReqVO();
        scope.setPartnerOrderIds(List.of("ORDER_C1", "ORDER_C2"));

        // 调用
        EvidenceCompletenessSummaryRespVO summary = invoiceEvidenceService.getCompleteness(scope);

        // 断言
        assertEquals(2, summary.getInvoiceCount());
        assertEquals(1, summary.getCompleteCount());
        assertEquals(0, new BigDecimal("60.00").compareTo(summary.getCompletenessRate()));
        assertEquals(2, summary.getItems().size());
        EvidenceCompletenessItemRespVO partialItem = summary.getItems().stream()
                .filter(item -> "ORDER_C2".equals(item.getPartnerOrderId())).findFirst().orElseThrow();
        assertEquals(List.of("合同流", "货物流", "资金流", "发票流"), partialItem.getMissingFlows());
    }

    @Test
    public void testGetLedgerRows_containsRequiredFields() {
        PayeeInfoDO payee = insertPayee("赵六", "13600136000", "成都市武侯区");
        InvoiceOrderDO order = insertOrder("ORDER_LEDGER", "INV_LEDGER", payee.getId(), "22222222222222222222");
        order.setInvoiceDate(LocalDateTime.of(2024, 12, 1, 10, 30));
        invoiceOrderMapper.updateById(order);
        insertItem(order);

        AcquisitionLedgerReqVO reqVO = new AcquisitionLedgerReqVO();
        reqVO.setPartnerOrderId("ORDER_LEDGER");

        // 调用
        List<AcquisitionLedgerRespVO> rows = invoiceEvidenceService.getLedgerRows(reqVO);

        // 断言：字段逐项是独立可知的字面值
        assertEquals(1, rows.size());
        AcquisitionLedgerRespVO row = rows.get(0);
        assertEquals(LocalDateTime.of(2024, 12, 1, 10, 30), row.getTradeTime());
        assertEquals("成都市武侯区", row.getTradeAddress());
        assertEquals("赵六", row.getSellerName());
        assertEquals("13600136000", row.getSellerMobile());
        assertEquals("废铁", row.getProductName());
        assertEquals("吨", row.getUnit());
        assertEquals(0, new BigDecimal("100").compareTo(row.getQuantity()));
        assertEquals(0, new BigDecimal("10.00").compareTo(row.getUnitPrice()));
        assertEquals(0, new BigDecimal("1000.00").compareTo(row.getAmount()));
        assertEquals("22222222222222222222", row.getInvoiceNo());
        assertEquals("ORDER_LEDGER", row.getPartnerOrderId());
    }

    @Test
    public void testGetEvidencePage_filtersByPartnerOrderId() {
        insertOrder("ORDER_PAGE_A", "INV_A", null, null);
        insertOrder("ORDER_PAGE_B", "INV_B", null, null);

        EvidencePageReqVO pageReqVO = new EvidencePageReqVO();
        pageReqVO.setPartnerOrderId("ORDER_PAGE_B");

        PageResult<EvidenceChainRespVO> page = invoiceEvidenceService.getEvidencePage(pageReqVO);

        assertEquals(1, page.getList().size());
        assertEquals("ORDER_PAGE_B", page.getList().get(0).getPartnerOrderId());
    }

    @Test
    public void testExportEvidencePackage_zipContainsManifestLedgerAndEvidenceList() throws Exception {
        PayeeInfoDO payee = insertPayee("孙七", "13500135000", "杭州市西湖区");
        InvoiceOrderDO order = insertOrder("ORDER_ZIP", "INV_ZIP", payee.getId(), "33333333333333333333");
        insertItem(order);

        MockHttpServletResponse response = new MockHttpServletResponse();
        invoiceEvidenceService.exportEvidencePackage("ORDER_ZIP", response);

        byte[] bytes = response.getContentAsByteArray();
        assertTrue(bytes.length > 0);
        assertEquals("application/zip", response.getContentType());
        Set<String> entries = new HashSet<>();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                entries.add(entry.getName());
            }
        }
        assertTrue(entries.contains("ORDER_ZIP/证据链.json"), "应含证据链清单，实际：" + entries);
        assertTrue(entries.contains("ORDER_ZIP/收购台账.csv"), "应含收购台账，实际：" + entries);
        assertTrue(entries.contains("ORDER_ZIP/证据清单.csv"), "应含证据清单，实际：" + entries);
    }

    @Test
    public void testExportEvidencePackageBatch_eachInvoiceGetsItsOwnFolder() throws Exception {
        insertOrder("ORDER_B1", "INV_B1", null, null);
        insertOrder("ORDER_B2", "INV_B2", null, null);

        EvidenceScopeReqVO scope = new EvidenceScopeReqVO();
        scope.setPartnerOrderIds(List.of("ORDER_B1", "ORDER_B2"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        invoiceEvidenceService.exportEvidencePackageBatch(scope, response);

        Set<String> entries = new HashSet<>();
        try (ZipInputStream zis = new ZipInputStream(
                new ByteArrayInputStream(response.getContentAsByteArray()))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                entries.add(entry.getName());
            }
        }
        assertTrue(entries.contains("ORDER_B1/证据链.json"), "实际：" + entries);
        assertTrue(entries.contains("ORDER_B2/证据链.json"), "实际：" + entries);
    }

    @Test
    public void testExportAcquisitionLedger_writesExcel() throws Exception {
        PayeeInfoDO payee = insertPayee("周八", "13400134000", "南京市鼓楼区");
        InvoiceOrderDO order = insertOrder("ORDER_XLS", "INV_XLS", payee.getId(), "44444444444444444444");
        insertItem(order);

        AcquisitionLedgerReqVO reqVO = new AcquisitionLedgerReqVO();
        reqVO.setPartnerOrderId("ORDER_XLS");
        MockHttpServletResponse response = new MockHttpServletResponse();
        invoiceEvidenceService.exportAcquisitionLedger(reqVO, response);

        assertTrue(response.getContentAsByteArray().length > 0);
        assertTrue(response.getContentType().contains("ms-excel"),
                "实际：" + response.getContentType());
    }

    // ==================== 造数 ====================

    private PayeeInfoDO insertPayee(String name, String mobile, String address) {
        PayeeInfoDO payee = PayeeInfoDO.builder()
                .name(name).mobile(mobile).address(address)
                .idCardNo("11010119900101" + mobile.substring(7))
                .partnerPayeeId("PARTNER_" + mobile)
                .build();
        payeeInfoMapper.insert(payee);
        return payee;
    }

    private InvoiceOrderDO insertOrder(String partnerOrderId, String orderNo, Long payeeId, String invoiceNo) {
        InvoiceOrderDO order = InvoiceOrderDO.builder()
                .orderNo(orderNo)
                .partnerOrderId(partnerOrderId)
                .payeeId(payeeId)
                .payeeNo("PAYEE_" + partnerOrderId)
                .payerNo("PAYER_1")
                .totalAmount(new BigDecimal("1000.00"))
                .invoiceType(1)
                .businessType("SCRAP")
                .orderStatus(3)
                .invoiceStatus(invoiceNo != null ? 2 : 0)
                .paymentStatus(0)
                .taxStatus(0)
                .invoiceNo(invoiceNo)
                .build();
        invoiceOrderMapper.insert(order);
        return order;
    }

    private void insertItem(InvoiceOrderDO order) {
        OrderItemDO item = OrderItemDO.builder()
                .orderId(order.getId())
                .orderNo(order.getOrderNo())
                .itemName("废铁")
                .unit("吨")
                .quantity(new BigDecimal("100"))
                .unitPrice(new BigDecimal("10.00"))
                .amount(new BigDecimal("1000.00"))
                .taxRate(new BigDecimal("0.01"))
                .build();
        orderItemMapper.insert(item);
    }

    private void insertPayment(String partnerOrderId, String serialNo) {
        PaymentOrderDO payment = PaymentOrderDO.builder()
                .orderNo("PAY_" + partnerOrderId)
                .partnerOrderId(partnerOrderId)
                .paymentAmount(new BigDecimal("1000.00"))
                .paymentStatus(2)
                .paymentSerialNo(serialNo)
                .paymentTime(LocalDateTime.of(2024, 12, 1, 11, 0))
                .build();
        paymentOrderMapper.insert(payment);
    }

    private void insertAcquisition(String invoicePartnerOrderId, String acquisitionNo, Long payeeId) {
        IcbcAcquisitionDO acquisition = IcbcAcquisitionDO.builder()
                .acquisitionNo(acquisitionNo)
                .clientRequestId(acquisitionNo)
                .payeeId(payeeId)
                .partnerPayeeId("PARTNER_ACQ")
                .sellerName("张三")
                .sellerMobile("13800138000")
                .categoryName("废钢")
                .unit("吨")
                .taxRate(new BigDecimal("0.01"))
                .mergedCode("1090101010000000000")
                .quantity(new BigDecimal("5"))
                .unitPrice(new BigDecimal("100.00"))
                .amount(new BigDecimal("500.00"))
                .grossWeight(new BigDecimal("18000"))
                .tareWeight(new BigDecimal("5500"))
                .netWeight(new BigDecimal("12500"))
                .weightTicketNo("WD20261201001")
                .weightTicketImageUrl("https://cdn.example.com/weight/wd.jpg")
                .weightTicketPlateNo("京A12345")
                .vehiclePlateNo("京A12345")
                .plateMatched(true)
                .vehicleFrontImageUrl("https://cdn.example.com/vehicle/front.jpg")
                .tradeAddress("北京市朝阳区回收站")
                .tradeTime(LocalDateTime.of(2026, 12, 1, 10, 0))
                .settlementMethod("银行转账")
                .status(0)
                .invoicePartnerOrderId(invoicePartnerOrderId)
                .source("ONLINE")
                .build();
        acquisitionMapper.insert(acquisition);
    }

    private void insertDownloadAndFile(String partnerOrderId, Long invoiceOrderId, String invoiceNumber) {
        InvoiceDownloadDO download = InvoiceDownloadDO.builder()
                .invoiceOrderId(invoiceOrderId)
                .partnerOrderId(partnerOrderId)
                .orderNumber("ICBC_" + partnerOrderId)
                .invoiceNumber(invoiceNumber)
                .downloadStatus(2)
                .retryCount(0)
                .build();
        invoiceDownloadMapper.insert(download);
        InvoiceFileDO file = InvoiceFileDO.builder()
                .downloadId(download.getId())
                .invoiceNumber(invoiceNumber)
                .fileType("PDF")
                .filePath("/tmp/" + partnerOrderId + ".pdf")
                .fileName(partnerOrderId + ".pdf")
                .fileSize(1024L)
                .fileMd5("d41d8cd98f00b204e9800998ecf8427e")
                .accessCount(0)
                .build();
        invoiceFileMapper.insert(file);
    }

    private EvidenceAttachReqVO attachReq(String partnerOrderId, String evidenceType) {
        EvidenceAttachReqVO reqVO = new EvidenceAttachReqVO();
        reqVO.setPartnerOrderId(partnerOrderId);
        reqVO.setEvidenceType(evidenceType);
        reqVO.setFileUrl("https://example.com/" + evidenceType + ".png");
        reqVO.setFileName(evidenceType + ".png");
        reqVO.setOccurredTime(LocalDateTime.of(2024, 12, 1, 9, 0));
        reqVO.setRemark("现场补录");
        return reqVO;
    }

    // ==================== 断言辅助 ====================

    private boolean flowPresent(EvidenceChainRespVO chain, String flow) {
        return chain.getFlows().stream()
                .filter(item -> flow.equals(item.getFlow()))
                .findFirst()
                .map(EvidenceFlowRespVO::getPresent)
                .orElse(false);
    }

    private List<EvidenceSourceRespVO> flowSources(EvidenceChainRespVO chain, String flow) {
        return chain.getFlows().stream()
                .filter(item -> flow.equals(item.getFlow()))
                .findFirst()
                .map(EvidenceFlowRespVO::getSources)
                .orElse(Collections.emptyList());
    }

}
