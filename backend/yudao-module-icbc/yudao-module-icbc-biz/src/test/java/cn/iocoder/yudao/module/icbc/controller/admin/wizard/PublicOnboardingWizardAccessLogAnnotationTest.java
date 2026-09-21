package cn.iocoder.yudao.module.icbc.controller.admin.wizard;

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
 * 一致性测试：{@link PublicOnboardingWizardController} 上**带请求体**的接口必须关掉请求体记录
 * （{@code @ApiAccessLog(requestEnable = false)}）。
 *
 * <p>公开端点走的是同一条 {@code /admin-api} 访问日志过滤链（平台的 {@code ApiAccessLogFilter}
 * 默认把 JSON 请求体写进 {@code infra_api_access_log.request_params}），所以自填壳的影像与 PII
 * 与代录壳受同一条约束：三枚识别传的是证件 / 银行卡影像本身，{@code submit} 带姓名 / 身份证号 /
 * 手机号 / 住址 / 银行卡号。注释会漂，注解不会——以后往这个控制器加带敏感请求体的接口，忘了加注解
 * 这条测试就红（与代录壳的 {@code OnboardingWizardAccessLogAnnotationTest} 同一口径）。
 */
public class PublicOnboardingWizardAccessLogAnnotationTest {

    @Test
    public void testRequestBodyEndpointsDisableRequestLog() {
        Set<String> guardedMethods = new TreeSet<>();
        for (Method method : PublicOnboardingWizardController.class.getDeclaredMethods()) {
            if (!hasRequestBody(method)) {
                continue;
            }
            guardedMethods.add(method.getName());
            ApiAccessLog annotation = method.getAnnotation(ApiAccessLog.class);
            assertNotNull(annotation, () -> String.format(
                    "%s 的请求体带影像或 PII，必须加 @ApiAccessLog(requestEnable = false)（#94）",
                    method.getName()));
            assertFalse(annotation.requestEnable(), () -> String.format(
                    "%s 必须关掉请求体记录（requestEnable = false），否则影像 / PII 会写进 infra_api_access_log",
                    method.getName()));
        }
        assertEquals(new TreeSet<>(Arrays.asList(
                        "recognizeIdCardFront", "recognizeIdCardBack", "recognizeBankCard", "submit")),
                guardedMethods,
                "带请求体的接口集合变了：新增的接口若也带影像 / PII，请一并加注解并更新这条断言");
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
