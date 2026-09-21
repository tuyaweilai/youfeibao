package cn.iocoder.yudao.framework.web.core.util;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link ApiLogSanitizer} 默认脱敏名单的**行为**测试。
 *
 * <p>本文件原来是 {@code ApiAccessLogFilterSanitizeTest}（#98）。#101 把默认名单与递归脱敏逻辑从
 * {@code ApiAccessLogFilter} 抽到 {@link ApiLogSanitizer}（访问日志与异常日志共用），测试随之搬过来，
 * 断言方式不变：只断言「名单里有 idCardNo 这个字符串」等于没测，这里拿一份带 PII 的请求体进去，
 * 断言实际输出里 PII 键**没了**、业务字段（{@code name} / {@code pageNo} / {@code amount}）**还在**。
 *
 * <p>{@code ApiAccessLogFilter} 与 {@link cn.iocoder.yudao.framework.web.core.handler.GlobalExceptionHandler}
 * 调的**就是**这几个方法，没有第二条脱敏路径。异常路径的端到端行为测试见
 * {@code GlobalExceptionHandlerErrorLogSanitizeTest}。
 *
 * <p>注意平台的闸门命令是 {@code mvn -o -pl yudao-module-icbc-api,yudao-module-icbc-biz test}——框架模块
 * 不在那个 reactor 里，它跑的是 {@code ~/.m2} 里的旧 jar。所以「名单被改窄」这件事由 icbc 侧的
 * {@code ApiAccessLogPiiCoverageTest}（从源码读名单）在闸门里守，这里守的是**脱敏行为本身**：
 * {@code mvn -o -pl yudao-framework/yudao-spring-boot-starter-web test}。
 */
public class ApiLogSanitizerTest {

    /** 一份带 PII 的请求体：PII 与业务字段混在一起，且 PII 出现在嵌套对象与数组元素里。 */
    private static final String REQUEST_BODY = "{"
            + "\"name\":\"张三\","
            + "\"idCardNo\":\"110101199003078888\","
            + "\"mobile\":\"13800138000\","
            + "\"bankCardNo\":\"6222020000000000000\","
            + "\"address\":\"北京市海淀区上地十街10号\","
            + "\"email\":\"zhangsan@example.com\","
            + "\"imageBase64\":\"aGVsbG8=\","
            + "\"ext\":{\"idNo\":\"110101199003078888\",\"pageNo\":1,"
            + "\"records\":[{\"name\":\"李四\",\"mobile\":\"13900139000\",\"amount\":12.5}]}}";

    @Test
    public void testSanitizeJsonRemovesPiiKeysRecursively() {
        // 调用：与 doFilter / 异常日志的默认路径一致（注解没写 sanitizeKeys 时传空数组，走默认名单）
        String sanitized = ApiLogSanitizer.sanitizeJson(REQUEST_BODY, new String[0]);

        // 断言：PII 键在顶层、嵌套对象、数组元素三个层次上都消失；业务字段原样保留
        assertEquals("{\"name\":\"张三\",\"ext\":{\"pageNo\":1,\"records\":[{\"name\":\"李四\",\"amount\":12.5}]}}",
                sanitized);
    }

    @Test
    public void testSanitizeJsonKeepsPiiValuesOutOfNothingButTheKey() {
        // 反例守卫：把 PII 换个字段名（或塞进数组）依然按「键名」处理，值是明文就还在——
        // 这正是本票的已知边界：脱敏按字段名，不认值的内容
        String sanitized = ApiLogSanitizer.sanitizeJson(
                "{\"whatever\":\"110101199003078888\",\"mobile\":\"13800138000\"}", new String[0]);
        assertEquals("{\"whatever\":\"110101199003078888\"}", sanitized);
    }

    @Test
    public void testSanitizeJsonAlsoAppliesAnnotationLevelKeys() {
        // @ApiAccessLog(sanitizeKeys = ...) 是每个方法自己指定的那一份，两边的键都要生效
        String sanitized = ApiLogSanitizer.sanitizeJson(
                "{\"customSecret\":\"s3cret\",\"mobile\":\"13800138000\",\"pageNo\":1}",
                new String[]{"customSecret"});
        assertEquals("{\"pageNo\":1}", sanitized);
    }

    @Test
    public void testSanitizeMapRemovesPiiQueryParams() {
        // 查询串走 sanitizeMap，用的是同一份名单
        Map<String, Object> query = new HashMap<>();
        query.put("mobile", "13800138000");
        query.put("idCardNo", "110101199003078888");
        query.put("pageNo", 1);
        query.put("customSecret", "s3cret");

        String sanitized = ApiLogSanitizer.sanitizeMap(query, new String[]{"customSecret"});
        assertEquals("{\"pageNo\":1}", sanitized);
    }

    @Test
    public void testSanitizeMapInPlaceKeepsQueryAsObjectShape() {
        // 异常日志保留「query 是个对象」的原形状（#101），就地脱敏后对象里只剩业务字段
        Map<String, Object> query = new HashMap<>();
        query.put("mobile", "13800138000");
        query.put("pageNo", 1);

        ApiLogSanitizer.sanitizeMapInPlace(query, null);
        assertEquals(1, query.size());
        assertEquals(1, query.get("pageNo"));
        assertFalse(query.containsKey("mobile"));
    }

    @Test
    public void testSanitizeJsonRemovesCredentialKeysFoundInEsignAndPaymentRequests() {
        // 扫请求体时顺手发现的同类字段：租户电子签密钥（PlatformEsignController.saveConfig）与
        // 支付 / 开票验证码（PaymentController.applyPayment）—— 改前它们和密码一样是明文进日志的
        String sanitized = ApiLogSanitizer.sanitizeJson(
                "{\"appId\":\"console-app\",\"secretId\":\"AKIDxxx\",\"secretKey\":\"SKxxx\","
                        + "\"callbackSignKey\":\"CSKxxx\",\"consoleToken\":\"console-token\","
                        + "\"verifiedCode\":\"123456\"}", new String[0]);
        // appId 是标识符不是密钥，保留；其余全删
        assertEquals("{\"appId\":\"console-app\"}", sanitized);
    }

    @Test
    public void testSanitizeResponseAppliesSameKeysButKeepsCodeAndMsg() {
        // 响应体（responseEnable = true 时）走同一份名单；code / msg 不能被动，否则错误信息会被吃掉
        Map<String, Object> data = new HashMap<>();
        data.put("mobile", "13800138000");
        data.put("name", "张三");
        CommonResult<Map<String, Object>> result = CommonResult.success(data);

        String sanitized = ApiLogSanitizer.sanitizeJson(result, new String[0]);
        JsonNode node = JsonUtils.parseTree(sanitized);
        assertEquals(0, node.get("code").asInt());
        assertEquals("", node.get("msg").asText());
        assertEquals("张三", node.get("data").get("name").asText());
        assertFalse(node.get("data").has("mobile"), () -> "响应体里的 mobile 没被脱敏：" + sanitized);
    }

    @Test
    public void testNameIsDeliberatelyNotSanitized() {
        // #98 / #101 的已知残留：name 太通用（商品名 / 企业名 / 菜单名都叫 name），所以姓名仍会进访问日志。
        // 这条断言把「有意不脱敏」写下来，免得后人以为漏了
        String sanitized = ApiLogSanitizer.sanitizeJson("{\"name\":\"张三\"}", new String[0]);
        assertTrue(sanitized.contains("\"name\":\"张三\""), () -> "name 不该被脱敏（有意为之）：" + sanitized);
    }

}
