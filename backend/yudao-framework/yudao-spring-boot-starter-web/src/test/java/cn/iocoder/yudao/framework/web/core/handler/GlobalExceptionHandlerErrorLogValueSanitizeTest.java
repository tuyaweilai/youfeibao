package cn.iocoder.yudao.framework.web.core.handler;

import cn.iocoder.yudao.framework.web.config.WebProperties;
import cn.iocoder.yudao.framework.web.core.util.WebFrameworkUtils;
import cn.iocoder.yudao.module.infra.api.logger.ApiErrorLogApi;
import cn.iocoder.yudao.module.infra.api.logger.dto.ApiErrorLogCreateReqDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

/**
 * #102 的行为测试：异常日志里的**值**不外泄。
 *
 * <p>{@link GlobalExceptionHandlerErrorLogSanitizeTest} 测的是 #101 的「请求参数按字段名脱敏」；
 * 这条测的是「文本字段里的 PII 值」：{@code exception_message} / {@code exception_root_cause_message}
 * / {@code exception_stack_trace} 三个字段都带同一段文本，而 MySQL 的唯一键冲突报文自带键值：
 * {@code Duplicate entry '1-110101199001011299-0' for key 'uk_id_card_no'}。
 *
 * <p><b>真撞一次键</b>：用内存 H2 建一张与生产 {@code icbc_payee_info} 同约束名的表，插两行同一个
 * 身份证号，由 Spring 的 SQLExceptionTranslator 翻成真实的 {@code DuplicateKeyException}（H2 的
 * 23505 会翻成它），再走 {@link GlobalExceptionHandler}，断言写进 {@code infra_api_error_log} 的
 * 载荷（{@link ApiErrorLogApi#createApiErrorLogAsync} 的实参）三个字段都不含那个值、且约束名还在。
 *
 * <p><b>为什么在 framework/web 而不是 icbc 模块</b>：这条路径（{@code GlobalExceptionHandler}）属于
 * 本模块；icbc 的测试跑的是 ~/.m2 里的**旧 jar**，改本模块的代码它看不见（fleet 的闸门会另编本模块）。
 * 本模块 classpath 上没有 icbc 的建表脚本，所以这里用同约束名的最小表替身。
 */
@ExtendWith(MockitoExtension.class)
public class GlobalExceptionHandlerErrorLogValueSanitizeTest {

    private static final String ID_CARD_NO = "110101199001011299";

    @Mock
    private ApiErrorLogApi apiErrorLogApi;

    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    public void setUp() {
        // GlobalExceptionHandler 基于 URL 前缀判断用户类型时会用到静态 WebProperties
        new WebFrameworkUtils(new WebProperties());
        // 每个用例独立库名：内嵌 H2 默认库名固定，会被别的上下文 SHUTDOWN 掉（同 UnitTestConfiguration 的注释）
        jdbcTemplate = new JdbcTemplate(new EmbeddedDatabaseBuilder()
                .generateUniqueName(true)
                .setType(EmbeddedDatabaseType.H2)
                .build());
        jdbcTemplate.execute("CREATE TABLE icbc_payee_info (id BIGINT PRIMARY KEY, id_card_no VARCHAR(32) NOT NULL, "
                + "CONSTRAINT uk_payee_id_card_no UNIQUE (id_card_no))");
    }

    @Test
    public void testDuplicateKeyValueDoesNotLeakIntoApiErrorLog() {
        // 准备：第一行占住这个身份证号
        jdbcTemplate.update("INSERT INTO icbc_payee_info (id, id_card_no) VALUES (?, ?)", 1L, ID_CARD_NO);

        // 调用：真的撞一次唯一键，Spring 把它翻成 DuplicateKeyException（H2 23505）
        DuplicateKeyException duplicateKey = assertThrows(DuplicateKeyException.class,
                () -> jdbcTemplate.update("INSERT INTO icbc_payee_info (id, id_card_no) VALUES (?, ?)", 2L, ID_CARD_NO));
        // 兜底：异常原文里确实带着那个值（否则这条测试没测到东西）
        assertTrue(duplicateKey.getMessage().contains(ID_CARD_NO),
                () -> "撞键异常原文里没有身份证号，测试前提不成立：" + duplicateKey.getMessage());

        // 调用：异常处理器把这条异常落成 infra_api_error_log
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/admin-api/icbc/payee/create");
        new GlobalExceptionHandler("test-app", apiErrorLogApi).defaultExceptionHandler(request, duplicateKey);

        // 断言：写进 infra_api_error_log 的三个文本字段都不含那个值
        ApiErrorLogCreateReqDTO errorLog = captureErrorLog();
        assertNoPii(errorLog.getExceptionMessage(), "exception_message");
        assertNoPii(errorLog.getExceptionRootCauseMessage(), "exception_root_cause_message");
        assertNoPii(errorLog.getExceptionStackTrace(), "exception_stack_trace");
        // 仍然能定位：约束名与类名是安全的，必须留着
        assertTrue(errorLog.getExceptionMessage().toLowerCase().contains("uk_payee_id_card_no"),
                () -> "脱敏把约束名也抹了，定位不了是哪个约束：" + errorLog.getExceptionMessage());
        assertTrue(errorLog.getExceptionStackTrace().toLowerCase().contains("uk_payee_id_card_no"),
                () -> "栈里丢了约束名，定位不了是哪个约束：" + errorLog.getExceptionStackTrace());
    }

    /**
     * 生产库是 MySQL，报文形状与 H2 不同。单测跑不到 MySQL，这里用生产同形状的报文把同一段处理再过一遍
     * 写库载荷（先前的红测就落在这个形状上）。
     */
    @Test
    public void testMysqlDuplicateEntryValueDoesNotLeakIntoApiErrorLog() {
        DuplicateKeyException duplicateKey = new DuplicateKeyException(
                "### Error updating database.  Cause: java.sql.SQLIntegrityConstraintViolationException: "
                        + "Duplicate entry '1-" + ID_CARD_NO + "-0' for key 'uk_id_card_no'");

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/admin-api/icbc/payee/create");
        new GlobalExceptionHandler("test-app", apiErrorLogApi).defaultExceptionHandler(request, duplicateKey);

        ApiErrorLogCreateReqDTO errorLog = captureErrorLog();
        assertNoPii(errorLog.getExceptionMessage(), "exception_message");
        assertNoPii(errorLog.getExceptionRootCauseMessage(), "exception_root_cause_message");
        assertNoPii(errorLog.getExceptionStackTrace(), "exception_stack_trace");
        assertTrue(errorLog.getExceptionMessage().contains("uk_id_card_no"),
                () -> "脱敏把约束名也抹了：" + errorLog.getExceptionMessage());
    }

    // ========== 辅助 ==========

    private ApiErrorLogCreateReqDTO captureErrorLog() {
        ArgumentCaptor<ApiErrorLogCreateReqDTO> captor = ArgumentCaptor.forClass(ApiErrorLogCreateReqDTO.class);
        verify(apiErrorLogApi).createApiErrorLogAsync(captor.capture());
        return captor.getValue();
    }

    private static void assertNoPii(String text, String field) {
        assertTrue(text != null, () -> field + " 不该为 null");
        assertFalse(text.contains(ID_CARD_NO), () -> field + " 里还留着身份证号：" + text);
    }

}
