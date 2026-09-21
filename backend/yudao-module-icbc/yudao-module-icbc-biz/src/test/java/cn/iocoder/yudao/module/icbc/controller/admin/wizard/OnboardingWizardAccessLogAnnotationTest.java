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
 * 一致性测试：{@link OnboardingWizardController} 上**带请求体**的接口必须关掉请求体记录
 * （{@code @ApiAccessLog(requestEnable = false)}）。
 *
 * <p><b>这条断言为哪条验收而存在</b>（#91 复审 ST-A / SP-A）：本票验收写着「证件与银行卡的影像
 * 不落库」，ADR 0037 决策 5 也写着「图片识别完即弃」，而平台的 {@code ApiAccessLogFilter} 默认
 * 把 {@code /admin-api} 的 JSON 请求体截 8000 字符写进 {@code infra_api_access_log.request_params}
 * ——本控制器的请求体要么是**证件 / 银行卡影像本身**（三枚识别的 {@code imageBase64}，现场端按
 * 10M 上限传），要么是**完整 PII**（{@code submit} 带姓名 / 身份证号 / 手机号 / 住址 / 银行卡号）。
 * 关掉请求体记录的唯一途径是本注解，而 {@code yudao.access-log.enable=false} 只在
 * {@code application-local.yaml} 里配过，dev / 生产默认开启——不锁就会把影像片段与 PII
 * 落进后台可查、可导出的运维日志表。
 *
 * <p>#98 之后平台的默认脱敏名单（{@code ApiLogSanitizer.SANITIZE_KEYS}，#101 从 {@code ApiAccessLogFilter}
 * 抽到 web 包与异常日志共用）已经覆盖
 * {@code imageBase64} / {@code idCardNo} / {@code mobile} / {@code bankCardNo} / {@code address}
 * 这类**字段名**，但这条注解**不撤**：两个机制管的不是一件事——默认名单治「记了也不出 PII」，
 * 注解治「整段别记」（影像按 10M 上限进来，本身就值得连解析都不做；且姓名 {@code name}
 * 按 #98 的口径不进名单）。
 *
 * <p>注释会漂，注解不会：以后往这个控制器加「请求体带影像或 PII」的接口，忘了加注解这条测试就红。
 * 反过来，若某接口真的不再带敏感请求体，应当把它显式迁出这条规则并更新下方集合，而不是悄悄放开。
 */
public class OnboardingWizardAccessLogAnnotationTest {

    @Test
    public void testRequestBodyEndpointsDisableRequestLog() {
        Set<String> guardedMethods = new TreeSet<>();
        for (Method method : OnboardingWizardController.class.getDeclaredMethods()) {
            if (!hasRequestBody(method)) {
                continue;
            }
            guardedMethods.add(method.getName());
            ApiAccessLog annotation = method.getAnnotation(ApiAccessLog.class);
            assertNotNull(annotation, () -> String.format(
                    "%s 的请求体带影像或 PII，必须加 @ApiAccessLog(requestEnable = false)（#91 复审 ST-A）",
                    method.getName()));
            assertFalse(annotation.requestEnable(), () -> String.format(
                    "%s 必须关掉请求体记录（requestEnable = false），否则影像 / PII 会写进 infra_api_access_log",
                    method.getName()));
        }
        assertEquals(new TreeSet<>(Arrays.asList("recognizeIdCardFront", "recognizeIdCardBack", "recognizeBankCard", "submit")),
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
