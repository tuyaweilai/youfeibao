package cn.iocoder.yudao.module.icbc.service.inputinvoice;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.icbc.UnitTestConfiguration;
import cn.iocoder.yudao.module.icbc.controller.admin.inputinvoice.vo.InputInvoiceLinkReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.inputinvoice.vo.InputInvoiceLinkRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.inputinvoice.vo.InputInvoicePageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.inputinvoice.vo.InputInvoiceRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.inputinvoice.vo.InputInvoiceSaveReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.inputinvoice.IcbcInputInvoiceDO;
import cn.iocoder.yudao.module.icbc.enums.InputInvoiceBizTypeEnum;
import cn.iocoder.yudao.module.icbc.enums.InputInvoiceStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.InputInvoiceTypeEnum;
import cn.iocoder.yudao.module.icbc.service.inputinvoice.impl.InputInvoiceServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link InputInvoiceServiceImpl} 的单元测试（#49 T11）。
 *
 * <p>覆盖验收：登记票面事实（票种 / 号码 / 金额 / 税额 / 开票日期 / 销方）、按「销方 + 号码」
 * 唯一、勾稽到单据且金额不超单据金额、状态（已登记 / 部分勾稽 / 已勾稽）。勾稽金额上限按调用方
 * 给出的 {@code bizAmount} 校验，测试里不依赖采购订单 / 入库单的实现。
 */
@Import({InputInvoiceServiceImpl.class, UnitTestConfiguration.class})
@Sql(scripts = "/sql/create_tables.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Transactional
@Rollback
public class InputInvoiceServiceTest extends BaseDbUnitTest {

    @Resource
    private InputInvoiceService inputInvoiceService;

    // ==================== 登记 ====================

    @Test
    public void testCreate_registersInvoiceFacts() {
        InputInvoiceSaveReqVO reqVO = newInvoiceReq("10000001", "某某钢铁有限公司", "91110000MA001",
                new BigDecimal("1000.00"), new BigDecimal("130.00"));
        reqVO.setInvoiceCode("011002100111");
        reqVO.setRemark("6 月采购废钢");

        Long id = inputInvoiceService.createInvoice(reqVO);

        IcbcInputInvoiceDO invoice = inputInvoiceService.getInvoice(id);
        assertEquals("10000001", invoice.getInvoiceNo());
        assertEquals("011002100111", invoice.getInvoiceCode());
        assertEquals(InputInvoiceTypeEnum.SPECIAL.getType(), invoice.getInvoiceType());
        assertEquals(LocalDate.of(2026, 6, 1), invoice.getInvoiceDate());
        assertEquals("某某钢铁有限公司", invoice.getSellerName());
        assertEquals("91110000MA001", invoice.getSellerTaxNo());
        assertEquals(new BigDecimal("1000.00"), invoice.getAmount());
        assertEquals(new BigDecimal("130.00"), invoice.getTaxAmount());
        assertEquals(new BigDecimal("1130.00"), invoice.getTotalAmount());
        assertEquals(0, BigDecimal.ZERO.compareTo(invoice.getLinkedAmount()));
        assertEquals(InputInvoiceStatusEnum.REGISTERED.getStatus(), invoice.getStatus());
    }

    @Test
    public void testCreate_duplicateBySellerAndInvoiceNoRejected() {
        inputInvoiceService.createInvoice(newInvoiceReq("10000002", "甲公司", "91110000MA002",
                new BigDecimal("100.00"), BigDecimal.ZERO));

        assertServiceException(() -> inputInvoiceService.createInvoice(newInvoiceReq("10000002", "甲公司",
                        "91110000MA002", new BigDecimal("200.00"), BigDecimal.ZERO)),
                INPUT_INVOICE_DUPLICATED, "甲公司", "10000002");
    }

    @Test
    public void testCreate_sameInvoiceNoDifferentSellerAllowed() {
        Long first = inputInvoiceService.createInvoice(newInvoiceReq("10000003", "甲公司", "91110000MA003",
                new BigDecimal("100.00"), BigDecimal.ZERO));
        Long second = inputInvoiceService.createInvoice(newInvoiceReq("10000003", "乙公司", "91110000MA004",
                new BigDecimal("100.00"), BigDecimal.ZERO));

        assertNotEquals(first, second);
    }

    @Test
    public void testCreate_taxNoIsTheSellerIdentityKey() {
        // 同名但税号不同：视为两张票，允许
        inputInvoiceService.createInvoice(newInvoiceReq("10000004", "同名公司", "91110000MA005",
                new BigDecimal("100.00"), BigDecimal.ZERO));
        // 同税号同号：拒绝
        assertServiceException(() -> inputInvoiceService.createInvoice(newInvoiceReq("10000004", "同名公司",
                        "91110000MA005", new BigDecimal("100.00"), BigDecimal.ZERO)),
                INPUT_INVOICE_DUPLICATED, "同名公司", "10000004");
    }

    @Test
    public void testCreate_invalidTypeRejected() {
        InputInvoiceSaveReqVO reqVO = newInvoiceReq("10000005", "甲公司", null,
                new BigDecimal("100.00"), BigDecimal.ZERO);
        reqVO.setInvoiceType(9);
        assertServiceException(() -> inputInvoiceService.createInvoice(reqVO),
                INPUT_INVOICE_TYPE_INVALID, 9);
    }

    @Test
    public void testCreate_invalidAmountRejected() {
        InputInvoiceSaveReqVO reqVO = newInvoiceReq("10000006", "甲公司", null,
                new BigDecimal("-1.00"), BigDecimal.ZERO);
        assertServiceException(() -> inputInvoiceService.createInvoice(reqVO), INPUT_INVOICE_AMOUNT_INVALID);
    }

    // ==================== 勾稽 ====================

    @Test
    public void testLink_partialThenFullUpdatesStatus() {
        Long invoiceId = createInvoice("1001", new BigDecimal("1000.00"), new BigDecimal("0.00"));

        InputInvoiceLinkRespVO first = inputInvoiceService.linkToBiz(linkReq(invoiceId,
                InputInvoiceBizTypeEnum.PURCHASE_ORDER.getType(), 501L, "PO20260601",
                new BigDecimal("1000.00"), new BigDecimal("400.00")));
        assertEquals("采购订单", first.getBizTypeName());

        IcbcInputInvoiceDO partial = inputInvoiceService.getInvoice(invoiceId);
        assertEquals(InputInvoiceStatusEnum.PARTIALLY_LINKED.getStatus(), partial.getStatus());
        assertEquals(0, new BigDecimal("400.00").compareTo(partial.getLinkedAmount()));

        inputInvoiceService.linkToBiz(linkReq(invoiceId, InputInvoiceBizTypeEnum.PURCHASE_ORDER.getType(),
                502L, "PO20260602", new BigDecimal("600.00"), new BigDecimal("600.00")));

        IcbcInputInvoiceDO full = inputInvoiceService.getInvoice(invoiceId);
        assertEquals(InputInvoiceStatusEnum.LINKED.getStatus(), full.getStatus());
        assertEquals(0, new BigDecimal("1000.00").compareTo(full.getLinkedAmount()));
    }

    @Test
    public void testLink_exceedBizAmountRejected() {
        Long invoiceId = createInvoice("1002", new BigDecimal("1000.00"), BigDecimal.ZERO);
        assertServiceException(() -> inputInvoiceService.linkToBiz(linkReq(invoiceId,
                        InputInvoiceBizTypeEnum.PURCHASE_ORDER.getType(), 601L, "PO601",
                        new BigDecimal("100.00"), new BigDecimal("120.00"))),
                INPUT_INVOICE_LINK_EXCEED_BIZ_AMOUNT, new BigDecimal("120.00"),
                new BigDecimal("100.00"), BigDecimal.ZERO);
    }

    @Test
    public void testLink_cumulativeOnSameBizAcrossInvoicesRejected() {
        Long first = createInvoice("1003", new BigDecimal("1000.00"), BigDecimal.ZERO);
        Long second = createInvoice("1004", new BigDecimal("1000.00"), BigDecimal.ZERO);

        // 两张票各勾到同一张单据，累计不得超过单据金额 1000
        inputInvoiceService.linkToBiz(linkReq(first, InputInvoiceBizTypeEnum.PURCHASE_ORDER.getType(),
                701L, "PO701", new BigDecimal("1000.00"), new BigDecimal("800.00")));
        assertServiceException(() -> inputInvoiceService.linkToBiz(linkReq(second,
                        InputInvoiceBizTypeEnum.PURCHASE_ORDER.getType(), 701L, "PO701",
                        new BigDecimal("1000.00"), new BigDecimal("300.00"))),
                INPUT_INVOICE_LINK_EXCEED_BIZ_AMOUNT, new BigDecimal("300.00"),
                new BigDecimal("1000.00"), new BigDecimal("800.00"));
    }

    @Test
    public void testLink_exceedInvoiceAmountRejected() {
        Long invoiceId = createInvoice("1005", new BigDecimal("1000.00"), BigDecimal.ZERO);
        assertServiceException(() -> inputInvoiceService.linkToBiz(linkReq(invoiceId,
                        InputInvoiceBizTypeEnum.ACQUISITION.getType(), 801L, "ACQ801",
                        new BigDecimal("5000.00"), new BigDecimal("1200.00"))),
                INPUT_INVOICE_LINK_EXCEED_INVOICE_AMOUNT, new BigDecimal("1200.00"),
                new BigDecimal("1000.00"), new BigDecimal("0.00"));
    }

    @Test
    public void testLink_duplicatePairRejected() {
        Long invoiceId = createInvoice("1006", new BigDecimal("1000.00"), BigDecimal.ZERO);
        inputInvoiceService.linkToBiz(linkReq(invoiceId, InputInvoiceBizTypeEnum.ACQUISITION.getType(),
                901L, "ACQ901", new BigDecimal("1000.00"), new BigDecimal("100.00")));
        assertServiceException(() -> inputInvoiceService.linkToBiz(linkReq(invoiceId,
                        InputInvoiceBizTypeEnum.ACQUISITION.getType(), 901L, "ACQ901",
                        new BigDecimal("1000.00"), new BigDecimal("100.00"))),
                INPUT_INVOICE_LINK_ALREADY_EXISTS, "ACQ901");
    }

    @Test
    public void testLink_invalidBizTypeRejected() {
        Long invoiceId = createInvoice("1007", new BigDecimal("1000.00"), BigDecimal.ZERO);
        assertServiceException(() -> inputInvoiceService.linkToBiz(linkReq(invoiceId, "SALE_ORDER",
                        902L, "SO902", new BigDecimal("1000.00"), new BigDecimal("10.00"))),
                INPUT_INVOICE_LINK_BIZ_TYPE_INVALID, "SALE_ORDER");
    }

    @Test
    public void testLink_zeroAmountRejected() {
        Long invoiceId = createInvoice("1008", new BigDecimal("1000.00"), BigDecimal.ZERO);
        assertServiceException(() -> inputInvoiceService.linkToBiz(linkReq(invoiceId,
                        InputInvoiceBizTypeEnum.ACQUISITION.getType(), 903L, "ACQ903",
                        new BigDecimal("1000.00"), BigDecimal.ZERO)),
                INPUT_INVOICE_LINK_AMOUNT_INVALID);
    }

    @Test
    public void testUnlink_recomputesStatus() {
        Long invoiceId = createInvoice("1009", new BigDecimal("1000.00"), BigDecimal.ZERO);
        InputInvoiceLinkRespVO link = inputInvoiceService.linkToBiz(linkReq(invoiceId,
                InputInvoiceBizTypeEnum.PURCHASE_ORDER.getType(), 1001L, "PO1001",
                new BigDecimal("1000.00"), new BigDecimal("1000.00")));
        assertEquals(InputInvoiceStatusEnum.LINKED.getStatus(),
                inputInvoiceService.getInvoice(invoiceId).getStatus());

        inputInvoiceService.unlink(link.getId());

        IcbcInputInvoiceDO invoice = inputInvoiceService.getInvoice(invoiceId);
        assertEquals(InputInvoiceStatusEnum.REGISTERED.getStatus(), invoice.getStatus());
        assertEquals(0, BigDecimal.ZERO.compareTo(invoice.getLinkedAmount()));
        assertTrue(inputInvoiceService.getLinkList(invoiceId).isEmpty());
    }

    @Test
    public void testUnlink_notExistsRejected() {
        assertServiceException(() -> inputInvoiceService.unlink(999999L), INPUT_INVOICE_LINK_NOT_EXISTS);
    }

    // ==================== 修改 / 删除门禁 ====================

    @Test
    public void testUpdate_okWhenNotLinked() {
        Long invoiceId = createInvoice("1010", new BigDecimal("1000.00"), BigDecimal.ZERO);
        InputInvoiceSaveReqVO update = newInvoiceReq("1010", "甲公司改名", "91110000MA010",
                new BigDecimal("1100.00"), new BigDecimal("100.00"));
        update.setId(invoiceId);

        inputInvoiceService.updateInvoice(update);

        IcbcInputInvoiceDO invoice = inputInvoiceService.getInvoice(invoiceId);
        assertEquals("甲公司改名", invoice.getSellerName());
        assertEquals(0, new BigDecimal("1200.00").compareTo(invoice.getTotalAmount()));
    }

    @Test
    public void testUpdate_linkedInvoiceRejected() {
        Long invoiceId = createInvoice("1011", new BigDecimal("1000.00"), BigDecimal.ZERO);
        inputInvoiceService.linkToBiz(linkReq(invoiceId, InputInvoiceBizTypeEnum.PURCHASE_ORDER.getType(),
                1101L, "PO1101", new BigDecimal("1000.00"), new BigDecimal("100.00")));

        InputInvoiceSaveReqVO update = newInvoiceReq("1011", "甲公司", null,
                new BigDecimal("1000.00"), BigDecimal.ZERO);
        update.setId(invoiceId);
        assertServiceException(() -> inputInvoiceService.updateInvoice(update),
                INPUT_INVOICE_STATUS_NOT_ALLOW, InputInvoiceStatusEnum.PARTIALLY_LINKED.getName());
    }

    @Test
    public void testDelete_onlyWhenNotLinked() {
        Long invoiceId = createInvoice("1012", new BigDecimal("1000.00"), BigDecimal.ZERO);
        inputInvoiceService.deleteInvoice(invoiceId);
        assertServiceException(() -> inputInvoiceService.getInvoice(invoiceId), INPUT_INVOICE_NOT_EXISTS);

        Long linkedId = createInvoice("1013", new BigDecimal("1000.00"), BigDecimal.ZERO);
        inputInvoiceService.linkToBiz(linkReq(linkedId, InputInvoiceBizTypeEnum.PURCHASE_ORDER.getType(),
                1201L, "PO1201", new BigDecimal("1000.00"), new BigDecimal("100.00")));
        assertServiceException(() -> inputInvoiceService.deleteInvoice(linkedId),
                INPUT_INVOICE_STATUS_NOT_ALLOW, InputInvoiceStatusEnum.PARTIALLY_LINKED.getName());
    }

    // ==================== 查询 ====================

    @Test
    public void testGet_notExistsRejected() {
        assertServiceException(() -> inputInvoiceService.getInvoice(999999L), INPUT_INVOICE_NOT_EXISTS);
    }

    @Test
    public void testDetail_carriesLinks() {
        Long invoiceId = createInvoice("1014", new BigDecimal("1000.00"), BigDecimal.ZERO);
        inputInvoiceService.linkToBiz(linkReq(invoiceId, InputInvoiceBizTypeEnum.ACQUISITION.getType(),
                1301L, "ACQ1301", new BigDecimal("1000.00"), new BigDecimal("300.00")));

        InputInvoiceRespVO detail = inputInvoiceService.getDetail(invoiceId);
        assertEquals("增值税专用发票", detail.getInvoiceTypeName());
        assertEquals("部分勾稽", detail.getStatusName());
        assertEquals(0, new BigDecimal("700.00").compareTo(detail.getRemainingAmount()));
        assertEquals(1, detail.getLinks().size());
        assertEquals("收购单", detail.getLinks().get(0).getBizTypeName());
        assertEquals("ACQ1301", detail.getLinks().get(0).getBizNo());
    }

    @Test
    public void testPage_filterByStatusAndSeller() {
        Long linkedId = createInvoice("1015", new BigDecimal("1000.00"), BigDecimal.ZERO, "甲公司");
        createInvoice("1016", new BigDecimal("1000.00"), BigDecimal.ZERO, "乙公司");
        inputInvoiceService.linkToBiz(linkReq(linkedId, InputInvoiceBizTypeEnum.PURCHASE_ORDER.getType(),
                1401L, "PO1401", new BigDecimal("1000.00"), new BigDecimal("1000.00")));

        InputInvoicePageReqVO page = new InputInvoicePageReqVO();
        page.setStatus(InputInvoiceStatusEnum.LINKED.getStatus());
        PageResult<InputInvoiceRespVO> linkedPage = inputInvoiceService.getInvoicePage(page);
        assertEquals(1, linkedPage.getTotal());
        assertEquals(linkedId, linkedPage.getList().get(0).getId());

        InputInvoicePageReqVO bySeller = new InputInvoicePageReqVO();
        bySeller.setSellerName("乙");
        assertEquals(1, inputInvoiceService.getInvoicePage(bySeller).getTotal());
    }

    // ==================== 辅助 ====================

    private Long createInvoice(String invoiceNo, BigDecimal amount, BigDecimal taxAmount) {
        return createInvoice(invoiceNo, amount, taxAmount, "销方" + invoiceNo);
    }

    private Long createInvoice(String invoiceNo, BigDecimal amount, BigDecimal taxAmount, String sellerName) {
        return inputInvoiceService.createInvoice(newInvoiceReq(invoiceNo, sellerName, null, amount, taxAmount));
    }

    private InputInvoiceSaveReqVO newInvoiceReq(String invoiceNo, String sellerName, String sellerTaxNo,
                                                BigDecimal amount, BigDecimal taxAmount) {
        InputInvoiceSaveReqVO reqVO = new InputInvoiceSaveReqVO();
        reqVO.setInvoiceNo(invoiceNo);
        reqVO.setInvoiceType(InputInvoiceTypeEnum.SPECIAL.getType());
        reqVO.setInvoiceDate(LocalDate.of(2026, 6, 1));
        reqVO.setSellerName(sellerName);
        reqVO.setSellerTaxNo(sellerTaxNo);
        reqVO.setAmount(amount);
        reqVO.setTaxAmount(taxAmount);
        return reqVO;
    }

    private InputInvoiceLinkReqVO linkReq(Long invoiceId, String bizType, Long bizId, String bizNo,
                                          BigDecimal bizAmount, BigDecimal linkedAmount) {
        InputInvoiceLinkReqVO reqVO = new InputInvoiceLinkReqVO();
        reqVO.setInvoiceId(invoiceId);
        reqVO.setBizType(bizType);
        reqVO.setBizId(bizId);
        reqVO.setBizNo(bizNo);
        reqVO.setBizAmount(bizAmount);
        reqVO.setLinkedAmount(linkedAmount);
        return reqVO;
    }

}
