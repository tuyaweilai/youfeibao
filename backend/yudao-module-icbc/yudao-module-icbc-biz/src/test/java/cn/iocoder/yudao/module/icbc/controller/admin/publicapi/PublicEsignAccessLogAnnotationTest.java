package cn.iocoder.yudao.module.icbc.controller.admin.publicapi;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 一致性测试：{@link PublicEsignController#signUrl} 把**公开令牌放在查询串里**，
 * 必须显式关掉访问日志的请求参数记录（{@code @ApiAccessLog(requestEnable = false)}）。
 *
 * <p>框架当前的 {@code ApiAccessLogFilter} 恰好会把查询串里的 {@code token} 脱敏、且记录的是
 * 不带查询串的 {@code requestURI}，所以现在未必真会写进日志表；但这是两个框架内部细节，接口不该
 * 依赖它们。这条测试钉住的是「声明本身」：谁把这个接口改回默认记录，它就红。
 */public class PublicEsignAccessLogAnnotationTest {

    @Test
    public void testSignUrlTokenEndpointDisablesRequestLog() throws NoSuchMethodException {
        Method method = PublicEsignController.class.getDeclaredMethod("signUrl", String.class);
        ApiAccessLog annotation = method.getAnnotation(ApiAccessLog.class);
        assertNotNull(annotation, "带令牌查询串的接口必须显式声明 @ApiAccessLog");
        assertFalse(annotation.requestEnable(),
                "signUrl 的令牌在查询串里，必须 requestEnable = false，否则令牌会写进 infra_api_access_log");
    }

}
