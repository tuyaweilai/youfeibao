package cn.iocoder.yudao.framework.web.core.handler;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.web.config.WebProperties;
import cn.iocoder.yudao.framework.web.core.util.WebFrameworkUtils;
import cn.iocoder.yudao.module.infra.api.logger.ApiErrorLogApi;
import cn.iocoder.yudao.module.infra.api.logger.dto.ApiErrorLogCreateReqDTO;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

/**
 * {@link GlobalExceptionHandler}（异常路径）的**行为**测试：真的抛一个异常，断言最终要写进
 * {@code infra_api_error_log} 的那条记录（{@link ApiErrorLogCreateReqDTO}，即
 * {@code ApiErrorLogApi#createApiErrorLogAsync} 的实参）里的请求参数已经脱敏。
 *
 * <p><b>为什么这条测试要存在</b>（#101）：#98 只把 {@link cn.iocoder.yudao.framework.apilog.core.filter.ApiAccessLogFilter}
 * 那条正常路径收口了；异常路径走的 {@link GlobalExceptionHandler#createExceptionLog} 自己拼
 * {@code {"query":..., "body":...}} 且不脱敏，于是任何抛异常的请求都会把请求体整段写进
 * {@code infra_api_error_log}（证件影像 base64 截 8000 字符后照样写）。这条测试在修复前**实测为红**。
 *
 * <p>断言的是「实际输入 → 实际输出」：用真实 {@link MockHttpServletRequest} 喂一份带 PII 的
 * JSON body + 查询串，调真实的 {@code defaultExceptionHandler}，再对捕获到的 DTO 断言。
 * 不是断言「名单里有这个字符串」。
 */
@ExtendWith(MockitoExtension.class)
public class GlobalExceptionHandlerErrorLogSanitizeTest {

    /** 与 #98 的脱敏行为测试（今 {@code ApiLogSanitizerTest}）用同一份 PII 混合请求体：PII 在顶层 / 嵌套对象 / 数组三层。 */
    private static final String REQUEST_BODY = "{"
            + "\"name\":\"张三\","
            + "\"idCardNo\":\"110101199003078888\","
            + "\"mobile\":\"13800138000\","
            + "\"bankCardNo\":\"6222020000000000000\","
            + "\"address\":\"北京市海淀区上地十街10号\","
            + "\"ext\":{\"idNo\":\"110101199003078888\",\"pageNo\":1,"
            + "\"records\":[{\"name\":\"李四\",\"imageBase64\":\"aGVsbG8=\",\"amount\":12.5}]}}";

    @Mock
    private ApiErrorLogApi apiErrorLogApi;

    @BeforeEach
    public void setUp() {
        // GlobalExceptionHandler 读用户类型时会用到 WebFrameworkUtils 的静态 WebProperties
        new WebFrameworkUtils(new WebProperties());
    }

    @Test
    public void testExceptionLogRequestParamsAreSanitized() {
        // 准备：真实请求，body 与查询串都带 PII
        MockHttpServletRequest request = newRequest();
        request.addParameter("mobile", "13800138000");
        request.addParameter("pageNo", "1");

        // 调用：真的走异常处理器（入参是一个真实的异常，强制它落 infra_api_error_log）
        new GlobalExceptionHandler("test-app", apiErrorLogApi)
                .defaultExceptionHandler(request, new RuntimeException("boom"));

        // 断言：写进 infra_api_error_log 的 requestParams 是 {"query":..., "body":...} 同一形状，但 PII 键全没了
        String requestParams = captureErrorLogRequestParams();
        JsonNode params = JsonUtils.parseTree(requestParams);
        assertTrue(params.has("query"), () -> "requestParams 形状变了，缺 query：" + requestParams);
        assertTrue(params.has("body"), () -> "requestParams 形状变了，缺 body：" + requestParams);

        // 1）查询串那半：mobile 没了，业务字段 pageNo 还在（异常日志的 query 保持「对象」形状）
        JsonNode queryNode = params.get("query");
        assertFalse(queryNode.has("mobile"), () -> "查询串里的 mobile 没脱敏：" + queryNode);
        assertFalse(requestParams.contains("13800138000"), () -> "查询串里的手机号值还在：" + requestParams);
        assertTrue(queryNode.has("pageNo"), () -> "业务字段 pageNo 不该被脱敏：" + queryNode);

        // 2）body 那半：顶层 / 嵌套对象 / 数组三层里的 PII 键都没了
        String bodyJson = params.get("body").asText();
        for (String key : new String[]{"idCardNo", "mobile", "bankCardNo", "address", "idNo", "imageBase64"}) {
            assertFalse(bodyJson.contains("\"" + key + "\""), () -> "异常日志 body 里的 " + key + " 没脱敏：" + bodyJson);
        }
        for (String value : new String[]{"110101199003078888", "13800138000", "6222020000000000000",
                "北京市海淀区上地十街10号", "aGVsbG8="}) {
            assertFalse(requestParams.contains(value), () -> "异常日志里还留着 PII 值 " + value + "：" + requestParams);
        }
        // 3）该留的还在：name（有意不脱敏）、嵌套的 pageNo / amount
        assertTrue(bodyJson.contains("\"name\":\"张三\""), () -> "name 不该被脱敏：" + bodyJson);
        assertTrue(bodyJson.contains("\"name\":\"李四\""), () -> "数组元素里的 name 不该被脱敏：" + bodyJson);
        assertTrue(bodyJson.contains("\"pageNo\""), () -> "pageNo 不该被脱敏：" + bodyJson);
        assertTrue(bodyJson.contains("\"amount\""), () -> "amount 不该被脱敏：" + bodyJson);
    }

    @Test
    public void testExceptionLogRequestParamsSkippedWhenRequestLogDisabled() {
        // 准备：某接口 @ApiAccessLog(requestEnable = false)（向导 / 自填壳 / signUrl 那 9 处），
        // ApiAccessLogInterceptor 会把这个开关放到请求属性里（WebFrameworkUtils）
        MockHttpServletRequest request = newRequest();
        request.addParameter("signUrl", "https://esign.example.com?token=super-secret");
        WebFrameworkUtils.setRequestLogEnabled(request, false);

        // 调用
        new GlobalExceptionHandler("test-app", apiErrorLogApi)
                .defaultExceptionHandler(request, new RuntimeException("boom"));

        // 断言：整段请求参数都不记（infra_api_error_log.request_params 是 NOT NULL，所以写空串）
        String requestParams = captureErrorLogRequestParams();
        assertEquals("", requestParams, () -> "requestEnable=false 的接口，异常日志不该记请求参数：" + requestParams);
    }

    @Test
    public void testExceptionLogAppliesAnnotationSanitizeKeys() {
        // 准备：@ApiAccessLog(sanitizeKeys = ...) 是方法级的那一份，异常路径也要生效
        MockHttpServletRequest request = newRequest();
        request.addParameter("customSecret", "s3cret");
        WebFrameworkUtils.setRequestLogSanitizeKeys(request, new String[]{"customSecret"});

        // 调用
        new GlobalExceptionHandler("test-app", apiErrorLogApi)
                .defaultExceptionHandler(request, new RuntimeException("boom"));

        // 断言：注解级键被删
        String requestParams = captureErrorLogRequestParams();
        assertFalse(requestParams.contains("customSecret"), () -> "注解级 sanitizeKeys 没生效：" + requestParams);
        assertFalse(requestParams.contains("s3cret"), () -> "注解级 sanitizeKeys 没生效：" + requestParams);
    }

    // ========== 辅助 ==========

    private MockHttpServletRequest newRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/admin-api/icbc/test");
        request.setContentType(MediaType.APPLICATION_JSON_VALUE);
        request.setContent(REQUEST_BODY.getBytes(StandardCharsets.UTF_8));
        return request;
    }

    private String captureErrorLogRequestParams() {
        ArgumentCaptor<ApiErrorLogCreateReqDTO> captor = ArgumentCaptor.forClass(ApiErrorLogCreateReqDTO.class);
        verify(apiErrorLogApi).createApiErrorLogAsync(captor.capture());
        String requestParams = captor.getValue().getRequestParams();
        assertTrue(requestParams != null, "异常日志的 requestParams 不该为 null（infra_api_error_log 该列 NOT NULL）");
        return requestParams;
    }

}
