package cn.iocoder.yudao.module.icbc.controller.admin.publicapi;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 一致性测试：{@link PublicEsignController#signUrl} 把**公开令牌放在查询串里**，
 * 必须关掉访问日志的请求参数记录（{@code @ApiAccessLog(requestEnable = false)}）。
 *
 * <p>平台 {@code ApiAccessLogFilter} 默认把 {@code query} 原样写进 {@code infra_api_access_log}；
 * 令牌是通往本人档案与一次性签署链接的凭证，不该落到运维日志表里。注释会漂，注解不会：
 * 谁把这个接口改回默认记录，这条测试就红。
 */
public class PublicEsignAccessLogAnnotationTest {

    @Test
    public void testSignUrlTokenEndpointDisablesRequestLog() throws NoSuchMethodException {
        Method method = PublicEsignController.class.getDeclaredMethod("signUrl", String.class);
        ApiAccessLog annotation = method.getAnnotation(ApiAccessLog.class);
        assertNotNull(annotation, "带令牌查询串的接口必须显式声明 @ApiAccessLog");
        assertFalse(annotation.requestEnable(),
                "signUrl 的令牌在查询串里，必须 requestEnable = false，否则令牌会写进 infra_api_access_log");
    }

}
