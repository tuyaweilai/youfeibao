package cn.iocoder.yudao.module.icbc.service.esign;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.icbc.UnitTestConfiguration;
import cn.iocoder.yudao.module.icbc.service.esign.impl.EsignCallbackServiceImpl;
import cn.iocoder.yudao.module.system.api.tenant.TenantApi;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

import javax.annotation.Resource;
import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * {@link EsignCallbackServiceImpl} 的单元测试（#92）。
 *
 * <p>锁住端口契约里唯一一条「必须明确失败」：验签失败、端口返回 null、子客编号反查不到租户，
 * 三种情况都抛错，绝不静默返回空通知。真实验签在端口实现里（本层只翻译失败）。
 *
 * <p>同时锁住「异常口径不能过宽」：只有端口的 {@link EsignPort.EsignCallbackRejectedException}
 * 被翻译成「回调被拒绝」，NPE / DB 异常照实向上抛，不把真 bug 伪装成伪造攻击。
 */
@Import({EsignCallbackServiceImpl.class, UnitTestConfiguration.class})
public class EsignCallbackServiceTest extends BaseDbUnitTest {

    @Resource
    private EsignCallbackService esignCallbackService;

    @MockBean
    private EsignPort esignPort;
    @MockBean
    private TenantApi tenantApi;

    @Test
    public void testHandle_returnsParsedCallbackWithTenant() {
        LocalDateTime signedAt = LocalDateTime.of(2026, 9, 21, 10, 0);
        when(esignPort.parseCallback(any(), any(), any(), any())).thenReturn(EsignPort.EsignCallback.builder()
                .tenantId(1L)
                .signTaskId("TASK-1")
                .finished(true)
                .signedAt(signedAt)
                .build());

        EsignPort.EsignCallback callback = esignCallbackService.handle("sig", "ts", "nonce", "{}");
        assertEquals(1L, callback.getTenantId());
        assertEquals("TASK-1", callback.getSignTaskId());
        assertTrue(callback.isFinished());
        assertEquals(signedAt, callback.getSignedAt());
    }

    @Test
    public void testHandle_verifyFailure_failsLoudly() {
        when(esignPort.parseCallback(any(), any(), any(), any()))
                .thenThrow(new EsignPort.EsignCallbackRejectedException("电子签章未开通，不应收到签署状态通知"));
        assertServiceException(() -> esignCallbackService.handle("sig", "ts", "nonce", "{}"),
                ESIGN_CALLBACK_REJECTED, "电子签章未开通，不应收到签署状态通知");
    }

    @Test
    public void testHandle_unexpectedRuntimeException_propagatesUnchanged() {
        // 端口内部的 NPE / DB 异常不是「拒绝」，不能被翻译成伪造攻击
        NullPointerException bug = new NullPointerException("端口内部空指针");
        when(esignPort.parseCallback(any(), any(), any(), any())).thenThrow(bug);
        NullPointerException thrown = assertThrows(NullPointerException.class,
                () -> esignCallbackService.handle("sig", "ts", "nonce", "{}"));
        assertSame(bug, thrown);
    }

    @Test
    public void testHandle_nullCallback_failsLoudly() {
        when(esignPort.parseCallback(any(), any(), any(), any())).thenReturn(null);
        assertServiceException(() -> esignCallbackService.handle("sig", "ts", "nonce", "{}"),
                ESIGN_CALLBACK_VERIFY_FAILED);
    }

    @Test
    public void testHandle_unresolvedTenant_failsLoudly() {
        when(esignPort.parseCallback(any(), any(), any(), any())).thenReturn(EsignPort.EsignCallback.builder()
                .signTaskId("TASK-1")
                .finished(false)
                .unfinishedReason("已过期")
                .build());
        assertServiceException(() -> esignCallbackService.handle("sig", "ts", "nonce", "{}"),
                ESIGN_SUB_CUSTOMER_NOT_RESOLVED, "（空）");
    }

}
