package cn.iocoder.yudao.module.icbc.controller.admin.esign;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.icbc.controller.admin.esign.vo.EsignCallbackRespVO;
import cn.iocoder.yudao.module.icbc.service.esign.EsignCallbackService;
import cn.iocoder.yudao.module.icbc.service.esign.EsignPort;
import cn.iocoder.yudao.module.icbc.service.esign.FrameworkAgreementEsignService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.mock.web.MockHttpServletRequest;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * {@link EsignCallbackController} 的接线测试（#95 SP-3）。
 *
 * <p><b>为什么必须有这条测试</b>：AC「签署完成回调把协议推到生效」的落地不是 service 实现本身，
 * 而是控制器在验签后**把归一化通知交给业务层**这一步调用。把
 * {@code frameworkAgreementEsignService.applyFinishedCallback(callback)} 这一行删掉，
 * 全部 service 单测照样绿，而整条 AC 失效。这里从控制器进去，断言通知确实被转交。
 */
public class EsignCallbackControllerWiringTest extends BaseMockitoUnitTest {

    @InjectMocks
    private EsignCallbackController controller;

    @Mock
    private EsignCallbackService esignCallbackService;
    @Mock
    private FrameworkAgreementEsignService frameworkAgreementEsignService;

    @Test
    public void testReceiveNotify_handsParsedCallbackToBusinessLayer() throws Exception {
        LocalDateTime signedAt = LocalDateTime.of(2026, 9, 21, 10, 0);
        EsignPort.EsignCallback callback = EsignPort.EsignCallback.builder()
                .tenantId(1L)
                .signTaskId("TASK-WIRING-1")
                .finished(true)
                .signedAt(signedAt)
                .build();
        when(esignCallbackService.handle(any(), any(), any(), any())).thenReturn(callback);

        MockHttpServletRequest request = new MockHttpServletRequest("POST",
                "/admin-api/icbc/esign/callback/notify");
        request.setContent("{\"EventType\":\"SIGN\"}".getBytes(StandardCharsets.UTF_8));
        request.addHeader(EsignCallbackController.HEADER_SIGNATURE, "sig");
        request.addHeader(EsignCallbackController.HEADER_TIMESTAMP, "1700000000");
        request.addHeader(EsignCallbackController.HEADER_NONCE, "nonce");

        CommonResult<EsignCallbackRespVO> result = controller.receiveNotify(request);

        // 关键接线：验签通过后的通知必须原样交给业务层收敛协议状态
        ArgumentCaptor<EsignPort.EsignCallback> captor = ArgumentCaptor.forClass(EsignPort.EsignCallback.class);
        verify(frameworkAgreementEsignService).applyFinishedCallback(captor.capture());
        assertSame(callback, captor.getValue(), "交给业务层的必须是验签后解析出的那一份通知");
        // 回执里带反查出的租户与任务号，供后续追踪
        assertEquals(1L, result.getData().getTenantId());
        assertEquals("TASK-WIRING-1", result.getData().getSignTaskId());
    }

    @Test
    public void testReceiveNotify_verifyFailureDoesNotTouchBusinessLayer() throws Exception {
        // 验签失败时 handle 会抛错，业务层一律不该被触到（伪造通知不能推协议状态）
        when(esignCallbackService.handle(any(), any(), any(), any()))
                .thenThrow(new EsignPort.EsignCallbackRejectedException("验签失败"));

        MockHttpServletRequest request = new MockHttpServletRequest("POST",
                "/admin-api/icbc/esign/callback/notify");
        request.setContent("{}".getBytes(StandardCharsets.UTF_8));

        org.junit.jupiter.api.Assertions.assertThrows(EsignPort.EsignCallbackRejectedException.class,
                () -> controller.receiveNotify(request));
        verifyNoInteractions(frameworkAgreementEsignService);
    }

}
