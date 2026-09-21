package cn.iocoder.yudao.framework.web.core.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link ApiErrorLogExceptionSanitizer#sanitize(String)} 的文本契约测试（#102）。
 *
 * <p>端到端「真撞键 → 落 infra_api_error_log」的用例在同模块同包的
 * {@code GlobalExceptionHandlerErrorLogValueSanitizeTest}；这条只锁两种方言的文本转换本身
 * （H2 那半不能在单测里造出 MySQL 报文，反过来也一样）。
 */
public class ApiErrorLogExceptionSanitizerTest {

    @Test
    public void testMysqlDuplicateEntryKeepsKeyDropsValue() {
        String raw = "### Error updating database.  Cause: java.sql.SQLIntegrityConstraintViolationException: "
                + "Duplicate entry '1-110101199001011299-0' for key 'uk_id_card_no'";

        String sanitized = ApiErrorLogExceptionSanitizer.sanitize(raw);

        assertFalse(sanitized.contains("110101199001011299"), () -> "身份证号还在：" + sanitized);
        assertTrue(sanitized.contains("uk_id_card_no"), () -> "约束名被抹了，定位不了：" + sanitized);
        assertTrue(sanitized.contains("Duplicate entry '***' for key 'uk_id_card_no'"), () -> sanitized);
        // 幂等：再跑一次结果不变（避免重复点击 / 重放写日志时二次变换出新花样）
        assertEquals(sanitized, ApiErrorLogExceptionSanitizer.sanitize(sanitized));
    }

    @Test
    public void testMysqlIncorrectValueDropsValue() {
        String rawString = "Incorrect string value: '\\xE5\\xBC\\xA0\\xE4\\xB8\\x89' for column 'name' at row 1";
        String sanitizedString = ApiErrorLogExceptionSanitizer.sanitize(rawString);
        assertFalse(sanitizedString.contains("\\xE5"), () -> "值的原始字节还在：" + sanitizedString);
        assertTrue(sanitizedString.contains("for column 'name'"), () -> "列名被抹了：" + sanitizedString);

        // 同一个位置的另一种值类型（手机号误送进数值列，报文同样带值）
        String rawInteger = "Incorrect integer value: '13800138000' for column 'x' at row 1";
        String sanitizedInteger = ApiErrorLogExceptionSanitizer.sanitize(rawInteger);
        assertFalse(sanitizedInteger.contains("13800138000"), () -> "手机号还在：" + sanitizedInteger);
        assertTrue(sanitizedInteger.contains("for column 'x'"), () -> "列名被抹了：" + sanitizedInteger);
    }

    @Test
    public void testH2UniqueIndexKeepsIndexDropsValues() {
        String raw = "Unique index or primary key violation: \"PUBLIC.UK_PAYEE_ID_CARD_NO_INDEX_5 ON "
                + "PUBLIC.ICBC_PAYEE_INFO(TENANT_ID NULLS FIRST, ID_CARD_NO NULLS FIRST, DELETED NULLS FIRST) "
                + "VALUES ( /* key:2681670723263666 */ CAST(1 AS BIGINT), '110101199001011299', FALSE)\"; "
                + "SQL statement:\nINSERT INTO icbc_payee_info ( id, id_card_no ) VALUES ( ?, ? ) [23505-214]";

        String sanitized = ApiErrorLogExceptionSanitizer.sanitize(raw);

        assertFalse(sanitized.contains("110101199001011299"), () -> "身份证号还在：" + sanitized);
        assertTrue(sanitized.contains("UK_PAYEE_ID_CARD_NO_INDEX_5"), () -> "索引名被抹了：" + sanitized);
        // 值里的内部括号（CAST(...)）不能把替换截断，值必须整段消失
        assertTrue(sanitized.contains("VALUES (***)\""), () -> "H2 值段没有被整段替换：" + sanitized);
        assertEquals(sanitized, ApiErrorLogExceptionSanitizer.sanitize(sanitized));
    }

    @Test
    public void testNonConstraintTextUnchanged() {
        String raw = "请求参数不正确:手机号不能为空，values 是业务字段名";
        assertEquals(raw, ApiErrorLogExceptionSanitizer.sanitize(raw));
    }

    /**
     * #102 修票纠正：MySQL 会把值里的单引号转义成 {@code \'}，而 {@code (Duplicate entry ')[^']*(' for key)}
     * 的 {@code [^']*} 过不去那个 {@code \'}、后面又要求紧跟 {@code ' for key}，于是**整条不匹配**——
     * 这个值一个字都没被替换（不是「只吃掉一半 / 漏出尾部」）。把这条实际边界钉住，别被读成低风险。
     */
    @Test
    public void testEscapedSingleQuoteValueIsLeftUntouched() {
        String raw = "Duplicate entry 'O\\'Brien' for key 'uk_name'";
        assertEquals(raw, ApiErrorLogExceptionSanitizer.sanitize(raw));
    }

}
