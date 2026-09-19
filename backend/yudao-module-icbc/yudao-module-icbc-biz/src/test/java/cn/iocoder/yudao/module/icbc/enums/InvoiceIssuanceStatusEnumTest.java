package cn.iocoder.yudao.module.icbc.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 开票 / 缴税 / 上传三类状态枚举的映射与语义测试（issue #10）。
 */
public class InvoiceIssuanceStatusEnumTest {

    @Test
    public void testTaxStatusMapping() {
        assertEquals(TaxStatusEnum.NOT_TAXED.getStatus(), TaxStatusEnum.toStatus("00"));
        assertEquals(TaxStatusEnum.TAXING.getStatus(), TaxStatusEnum.toStatus("01"));
        assertEquals(TaxStatusEnum.TAXING.getStatus(), TaxStatusEnum.toStatus("02"));
        assertEquals(TaxStatusEnum.TAXING.getStatus(), TaxStatusEnum.toStatus("03"));
        assertEquals(TaxStatusEnum.SUCCESS.getStatus(), TaxStatusEnum.toStatus("04"));
        assertEquals(TaxStatusEnum.FAILED.getStatus(), TaxStatusEnum.toStatus("05"));
        assertEquals(TaxStatusEnum.ABNORMAL_AMOUNT.getStatus(), TaxStatusEnum.toStatus("97"));
        assertEquals(TaxStatusEnum.ABNORMAL_UNKNOWN.getStatus(), TaxStatusEnum.toStatus("98"));
        assertEquals(TaxStatusEnum.NOT_REQUIRED.getStatus(), TaxStatusEnum.toStatus("99"));
        // 未识别的码不覆盖本地快照
        assertNull(TaxStatusEnum.toStatus("XX"));
        assertNull(TaxStatusEnum.toStatus(null));
    }

    @Test
    public void testTaxPaidAndException() {
        assertTrue(TaxStatusEnum.isPaid(TaxStatusEnum.SUCCESS.getStatus()));
        assertTrue(TaxStatusEnum.isPaid(TaxStatusEnum.NOT_REQUIRED.getStatus()));
        assertFalse(TaxStatusEnum.isPaid(TaxStatusEnum.TAXING.getStatus()));
        assertTrue(TaxStatusEnum.isException(TaxStatusEnum.FAILED.getStatus()));
        assertTrue(TaxStatusEnum.isException(TaxStatusEnum.ABNORMAL_AMOUNT.getStatus()));
        assertTrue(TaxStatusEnum.isException(TaxStatusEnum.ABNORMAL_UNKNOWN.getStatus()));
        assertFalse(TaxStatusEnum.isException(TaxStatusEnum.SUCCESS.getStatus()));
        // 成功 / 无需缴税没有下一步动作，异常 / 进行中有
        assertNull(TaxStatusEnum.nextActionOf(TaxStatusEnum.SUCCESS.getStatus()));
        assertNull(TaxStatusEnum.nextActionOf(TaxStatusEnum.NOT_REQUIRED.getStatus()));
        assertNotNull(TaxStatusEnum.nextActionOf(TaxStatusEnum.FAILED.getStatus()));
        assertNotNull(TaxStatusEnum.nextActionOf(TaxStatusEnum.TAXING.getStatus()));
    }

    @Test
    public void testUploadStatusMapping() {
        assertEquals(UploadStatusEnum.NOT_UPLOADED.getStatus(), UploadStatusEnum.toStatus("00"));
        assertEquals(UploadStatusEnum.PROCESSING.getStatus(), UploadStatusEnum.toStatus("01"));
        assertEquals(UploadStatusEnum.ACCEPTED.getStatus(), UploadStatusEnum.toStatus("02"));
        assertEquals(UploadStatusEnum.UPLOADING.getStatus(), UploadStatusEnum.toStatus("03"));
        assertEquals(UploadStatusEnum.SUCCESS.getStatus(), UploadStatusEnum.toStatus("04"));
        assertEquals(UploadStatusEnum.FAILED.getStatus(), UploadStatusEnum.toStatus("05"));
        assertNull(UploadStatusEnum.toStatus("99"));
        assertTrue(UploadStatusEnum.isSuccess(UploadStatusEnum.SUCCESS.getStatus()));
        assertTrue(UploadStatusEnum.isException(UploadStatusEnum.FAILED.getStatus()));
        assertNull(UploadStatusEnum.nextActionOf(UploadStatusEnum.SUCCESS.getStatus()));
        assertNotNull(UploadStatusEnum.nextActionOf(UploadStatusEnum.FAILED.getStatus()));
    }

    @Test
    public void testInvoiceIssueStatus() {
        assertTrue(InvoiceIssueStatusEnum.isIssued(InvoiceIssueStatusEnum.ISSUED.getStatus()));
        assertFalse(InvoiceIssueStatusEnum.isIssued(InvoiceIssueStatusEnum.ISSUING.getStatus()));
        assertTrue(InvoiceIssueStatusEnum.isException(InvoiceIssueStatusEnum.FAILED.getStatus()));
        assertNull(InvoiceIssueStatusEnum.nextActionOf(InvoiceIssueStatusEnum.ISSUED.getStatus()));
        assertNotNull(InvoiceIssueStatusEnum.nextActionOf(InvoiceIssueStatusEnum.FAILED.getStatus()));
        assertEquals("已开票", InvoiceIssueStatusEnum.nameOf(InvoiceIssueStatusEnum.ISSUED.getStatus()));
    }
}
