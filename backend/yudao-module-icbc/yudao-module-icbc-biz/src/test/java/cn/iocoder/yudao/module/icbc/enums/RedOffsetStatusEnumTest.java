package cn.iocoder.yudao.module.icbc.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link RedOffsetStatusEnum} 的单元测试。
 *
 * <p>红冲是一条 00–11 的状态线（#14）：哪一步算「红票真正开出」、哪一步还能撤销，
 * 是产品语义的核心，在这里被锁死。
 */
public class RedOffsetStatusEnumTest {

    @Test
    public void testCodeMapping() {
        assertEquals(RedOffsetStatusEnum.APPLYING, RedOffsetStatusEnum.ofCode("01").orElseThrow());
        assertEquals(RedOffsetStatusEnum.APPLIED, RedOffsetStatusEnum.ofCode("02").orElseThrow());
        assertEquals(RedOffsetStatusEnum.SUCCESS, RedOffsetStatusEnum.ofCode("07").orElseThrow());
        assertEquals(RedOffsetStatusEnum.REVOKED, RedOffsetStatusEnum.ofCode("10").orElseThrow());
        assertEquals(RedOffsetStatusEnum.REVOKE_FAILED, RedOffsetStatusEnum.ofCode("11").orElseThrow());
        // 未识别状态码收敛到初始，不丢原始码
        assertEquals(RedOffsetStatusEnum.INITIAL.getStatus(), RedOffsetStatusEnum.toStatus("99"));
    }

    @Test
    public void testOnlyUploadSuccessMeansRedInvoiceIssued() {
        assertTrue(RedOffsetStatusEnum.SUCCESS.isRedInvoiceIssued());
        assertFalse(RedOffsetStatusEnum.APPLIED.isRedInvoiceIssued());
        assertFalse(RedOffsetStatusEnum.UPLOADING.isRedInvoiceIssued());
    }

    @Test
    public void testRevocableOnlyBeforeRedInvoiceUploaded() {
        assertTrue(RedOffsetStatusEnum.INITIAL.isRevocable());
        assertTrue(RedOffsetStatusEnum.APPLIED.isRevocable());
        assertTrue(RedOffsetStatusEnum.UPLOADING.isRevocable());
        // 撤销失败可再次撤销
        assertTrue(RedOffsetStatusEnum.REVOKE_FAILED.isRevocable());
        // 红票已上传成功 / 撤销中 / 已撤销不可再撤销
        assertFalse(RedOffsetStatusEnum.SUCCESS.isRevocable());
        assertFalse(RedOffsetStatusEnum.REVOKING.isRevocable());
        assertFalse(RedOffsetStatusEnum.REVOKED.isRevocable());
    }

    @Test
    public void testTerminalDoesNotRegress() {
        // 已红冲成功后，旧的「上传中」不回退
        assertFalse(RedOffsetStatusEnum.shouldApply(
                RedOffsetStatusEnum.SUCCESS.getStatus(), RedOffsetStatusEnum.UPLOADING.getStatus()));
        // 已撤销后，旧的「申请中」不回退
        assertFalse(RedOffsetStatusEnum.shouldApply(
                RedOffsetStatusEnum.REVOKED.getStatus(), RedOffsetStatusEnum.APPLYING.getStatus()));
        // 幂等：同终态可重复写
        assertTrue(RedOffsetStatusEnum.shouldApply(
                RedOffsetStatusEnum.SUCCESS.getStatus(), RedOffsetStatusEnum.SUCCESS.getStatus()));
        // 进行中状态可以正常推进
        assertTrue(RedOffsetStatusEnum.shouldApply(
                RedOffsetStatusEnum.APPLYING.getStatus(), RedOffsetStatusEnum.APPLIED.getStatus()));
    }

    @Test
    public void testFailureStatesExposeNextAction() {
        assertTrue(RedOffsetStatusEnum.APPLY_FAILED.isException());
        assertTrue(RedOffsetStatusEnum.UPLOAD_FAILED.isException());
        assertNotNull(RedOffsetStatusEnum.APPLY_FAILED.getNextAction());
        assertNull(RedOffsetStatusEnum.SUCCESS.getNextAction());
        assertNull(RedOffsetStatusEnum.REVOKED.getNextAction());
    }
}
