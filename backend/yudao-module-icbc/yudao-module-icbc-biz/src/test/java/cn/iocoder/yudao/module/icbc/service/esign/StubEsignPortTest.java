package cn.iocoder.yudao.module.icbc.service.esign;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link StubEsignPort} 的契约测试。
 *
 * <p>锁住的是 ADR 0036 / 0037 的降级承诺：租户未开通电子签章时**签章不可用而不是抛异常**，
 * 业务层据此把协议落 {@code PAPER}、向导照常走完。唯一的例外是回调验签——伪造或错配的通知
 * 必须明确失败，不能被静默吞掉。
 */
public class StubEsignPortTest {

    private static final Long TENANT_ID = 1L;

    private final EsignPort port = new StubEsignPort();

    @Test
    public void testNotAvailable() {
        assertFalse(port.isAvailable(TENANT_ID));
    }

    @Test
    public void testOperationsReturnEmptyInsteadOfThrowing() {
        EsignPort.EsignTask task = port.initiate(TENANT_ID, EsignPort.EsignRequest.builder()
                .payeeId(9L)
                .build());
        assertNull(task.getSignTaskId());

        assertNull(port.createSignUrl(TENANT_ID, "task-1", EsignPort.EsignSigner.builder()
                .name("张三")
                .mobile("13800138000")
                .build()));

        assertTrue(port.listSignedDocuments(TENANT_ID, "task-1").isEmpty());
    }

    @Test
    public void testCallbackFailsLoudly() {
        assertThrows(UnsupportedOperationException.class,
                () -> port.parseCallback("signature", "timestamp", "nonce", "{}"));
    }

}
