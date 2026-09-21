package cn.iocoder.yudao.framework.apilog.core.interceptor;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.web.core.util.WebFrameworkUtils;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.method.HandlerMethod;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link ApiAccessLogInterceptor#applyRequestLogConfig} 的接线测试（#101）：把 {@code @ApiAccessLog} 的
 * {@code requestEnable} / {@code sanitizeKeys} 解析到请求属性，供异常日志
 * （{@code GlobalExceptionHandler}）复用。
 *
 * <p>单测 {@code GlobalExceptionHandler} 时是手工写请求属性的；这条测试补上「属性到底由谁按注解写进去」，
 * 否则接线断了也不会红。
 */
public class ApiAccessLogInterceptorRequestLogConfigTest {

    @Test
    public void testResolvesRequestLogDisabled() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        HandlerMethod handlerMethod = new HandlerMethod(new SampleHandler(),
                SampleHandler.class.getDeclaredMethod("requestLogDisabled"));

        ApiAccessLogInterceptor.applyRequestLogConfig(request, handlerMethod);

        assertFalse(WebFrameworkUtils.isRequestLogEnabled(request), "requestEnable=false 没写进请求属性");
    }

    @Test
    public void testResolvesSanitizeKeysAndDefaultsToEnabled() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        HandlerMethod handlerMethod = new HandlerMethod(new SampleHandler(),
                SampleHandler.class.getDeclaredMethod("customSanitizeKeys"));

        ApiAccessLogInterceptor.applyRequestLogConfig(request, handlerMethod);

        assertTrue(WebFrameworkUtils.isRequestLogEnabled(request), "requestEnable 默认 true");
        assertArrayEquals(new String[]{"customSecret"}, WebFrameworkUtils.getRequestLogSanitizeKeys(request));
    }

    @Test
    public void testWithoutAnnotationDefaultsToEnabled() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        HandlerMethod handlerMethod = new HandlerMethod(new SampleHandler(),
                SampleHandler.class.getDeclaredMethod("noAnnotation"));

        ApiAccessLogInterceptor.applyRequestLogConfig(request, handlerMethod);

        assertTrue(WebFrameworkUtils.isRequestLogEnabled(request));
        assertNull(WebFrameworkUtils.getRequestLogSanitizeKeys(request));
    }

    @SuppressWarnings("unused")
    static class SampleHandler {

        @ApiAccessLog(requestEnable = false)
        public void requestLogDisabled() {
        }

        @ApiAccessLog(sanitizeKeys = {"customSecret"})
        public void customSanitizeKeys() {
        }

        public void noAnnotation() {
        }

    }

}
