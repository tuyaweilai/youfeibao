package cn.iocoder.yudao.module.icbc.controller.admin.platform;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestBody;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 一致性测试：{@link PlatformCardRecognitionController} 上**带请求体**的接口必须关掉请求体记录
 * （{@code @ApiAccessLog(requestEnable = false)}）。
 *
 * <p><b>这条断言为哪条验收而存在</b>（#103）：保存与自检的请求体都会携带腾讯云 SecretId / SecretKey，
 * 而平台的 {@code ApiAccessLogFilter} 默认把 {@code /admin-api} 的 JSON 请求体写进
 * {@code infra_api_access_log.request_params}。注意**这不是「不脱敏」的洞**：{@code secretId} /
 * {@code secretKey} 早在 #98 就进了 {@code ApiLogSanitizer.SANITIZE_KEYS}，按键名会被打码。
 * 这条注解治的是另一件事——**密钥只该走「整段不记」**，不该把安全寄托在「字段名碰巧命中」上：
 * 字段一改名，按键名的名单就绕过了，而这份名单是手工维护的。
 *
 * <p>照 {@code OnboardingWizardAccessLogAnnotationTest} 的反射式写法：读注解、断言 {@code requestEnable == false}。
 * 往这个控制器加「请求体带密钥 / PII」的接口而忘了加注解，这条测试就红。
 */
public class PlatformCardRecognitionAccessLogAnnotationTest {

    @Test
    public void testRequestBodyEndpointsDisableRequestLog() {
        Set<String> guardedMethods = new TreeSet<>();
        for (Method method : PlatformCardRecognitionController.class.getDeclaredMethods()) {
            if (!hasRequestBody(method)) {
                continue;
            }
            guardedMethods.add(method.getName());
            ApiAccessLog annotation = method.getAnnotation(ApiAccessLog.class);
            assertNotNull(annotation, () -> String.format(
                    "%s 的请求体带密钥，必须加 @ApiAccessLog(requestEnable = false)（#103）", method.getName()));
            assertFalse(annotation.requestEnable(), () -> String.format(
                    "%s 必须关掉请求体记录（requestEnable = false），否则密钥会写进 infra_api_access_log",
                    method.getName()));
        }
        assertEquals(new TreeSet<>(Arrays.asList("saveConfig", "checkConnectivity")), guardedMethods,
                "带请求体的接口集合变了：新增的接口若也带密钥 / PII，请一并加注解并更新这条断言");
    }

    private boolean hasRequestBody(Method method) {
        for (Parameter parameter : method.getParameters()) {
            if (parameter.isAnnotationPresent(RequestBody.class)) {
                return true;
            }
        }
        return false;
    }

}
