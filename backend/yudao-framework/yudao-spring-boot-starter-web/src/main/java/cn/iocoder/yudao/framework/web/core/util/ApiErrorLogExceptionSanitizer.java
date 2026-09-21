package cn.iocoder.yudao.framework.web.core.util;

import cn.hutool.core.util.StrUtil;

import java.sql.SQLIntegrityConstraintViolationException;
import java.util.regex.Pattern;

/**
 * 异常**文本**（message / root cause message / stack trace）的脱敏器（#102）。
 *
 * <p><b>它治的是「值」，不是「键名」</b>：{@link ApiLogSanitizer} 按字段名删键，
 * 管不到被拼进 message 里的值——MySQL 的唯一键冲突报文自带键值：
 * <pre>Duplicate entry '1-110101199001011299-0' for key 'uk_id_card_no'</pre>
 * 而 {@link cn.iocoder.yudao.framework.web.core.handler.GlobalExceptionHandler} 会把同一段文本写进
 * {@code exception_message} / {@code exception_root_cause_message} / {@code exception_stack_trace}
 * 三个字段（栈的第一行与 {@code Caused by:} 行都是 {@code Throwable#toString()}，带着同一条 message），
 * 所以三个字段都要过这里。
 *
 * <p><b>为什么不做通用正则脱敏</b>（#102 明确否掉的 B 方案）：一条面向所有异常消息的「把看起来像
 * 身份证号 / 手机号的串抹掉」的正则，既会误伤合法文案（单号、编码、金额），又给了「换个格式就绕过」
 * 的假安全感。这里只处理**已知会带值的那一类**——数据库完整性约束报文的两种方言：
 * <ul>
 *   <li>MySQL：{@code Duplicate entry '...' for key '...'}、{@code Incorrect string/integer/... value: '...' for column ...}</li>
 *   <li>H2（单测用）：{@code Unique index or primary key violation: "... VALUES (...)"; SQL statement: ...}</li>
 * </ul>
 * 其余文本**原样保留**：约束名 / 索引名 / 列名 / 类名 / 栈帧都是安全的，正是定位问题要用的信息。
 *
 * <p><b>只在 DB 约束异常上生效</b>：{@link #isDbConstraintViolation(Throwable)} 顺着 cause 链判断，
 * 命中才调用 {@link #sanitize(String)}。这样本类不是「所有消息都过一遍」的通用脱敏。
 * 判断用类名字符串（而非直接 {@code instanceof}）是因为 {@code yudao-spring-boot-starter-web} 的
 * 编译期 classpath 上没有 spring-tx / spring-jdbc（不引 {@code DataIntegrityViolationException} 的
 * 编译依赖），而 {@link SQLIntegrityConstraintViolationException} 属于 JDK，可以直接判。
 *
 * <p><b>还剩哪些值会漏（#102 如实列，别护着自己）</b>：
 * <ul>
 *   <li><b>含转义单引号的值只吃掉一半</b>：{@code Duplicate entry 'O\'Brien' for key 'x'} 里
 *       {@code [^']*} 在 {@code \'} 处停下，姓名类会漏出尾部；数字型 PII（身份证 / 手机号 / 卡号）
 *       不含单引号，不受影响。要治得把转义也写进正则，代价是正则更脆。</li>
 *   <li><b>类型不匹配的路径 / 查询参数回给响应</b>：{@code methodArgumentTypeMismatchExceptionHandler}
 *       用 {@code ex.getMessage()} 拼返回文案，里面的原值（{@code failed to convert value of type ...}）
 *       会到调用方；本类只治落库的三个文本字段，管不到这里。</li>
 *   <li><b>{@code @Valid} 校验失败的文件日志</b>：{@code methodArgumentNotValidExceptionExceptionHandler} /
 *       {@code bindExceptionHandler} 做 {@code log.warn(..., ex)}，而 Spring 那条异常的消息里带
 *       {@code rejected value [<原值>]}——返回给用户的文案走 {@code getDefaultMessage()}、不含值，
 *       但控制台 / 文件日志会带（#102 的 C-4，结论：本票不治，原因见提交与 handoff）。</li>
 *   <li><b>非 DB 约束来源的值</b>：第三方 SDK 报文、我们自己 {@code exception(CODE, 拼值)} 且未改的文案、
 *       非 JDBC 的异常——按设计不覆盖（{@link #isDbConstraintViolation(Throwable)} 不命中就不调用），
 *       要在各自的发生位置治。icbc / member / system 全仓 grep 后只剩已改的
 *       {@code member.USER_MOBILE_USED} 一处（见该票）。</li>
 * </ul>
 */
public class ApiErrorLogExceptionSanitizer {

    /** 值占位符：一眼能看出「这里原来有个值，被有意去掉了」 */
    public static final String MASK = "***";

    /**
     * MySQL 唯一键冲突：{@code Duplicate entry '1-110101199001011299-0' for key 'uk_id_card_no'}
     * ——单引号里是值（复合唯一键是各列拼起来的一整串），{@code for key} 后面是安全的约束名。
     */
    private static final Pattern MYSQL_DUPLICATE_ENTRY =
            Pattern.compile("(Duplicate entry ')[^']*(' for key)");

    /**
     * MySQL 值类型／字符集报错：{@code Incorrect string value: '\xE5...' for column 'name'}、
     * {@code Incorrect integer value: '13800138000' for column 'x'}、{@code Incorrect datetime value: '...'}
     * ——单引号里是值的原始字节 / 原文。
     */
    private static final Pattern MYSQL_INCORRECT_VALUE =
            Pattern.compile("(Incorrect \\w+ value: ')[^']*(')");

    /**
     * H2 唯一索引冲突：{@code "... ON PUBLIC.TABLE(...) VALUES (1, '110101...', FALSE)"; SQL statement: ...}
     * ——{@code VALUES} 括号里是各列的实例值；括号前的索引名 / 表名 / 列名保留。
     * 必须锚在 H2 的固定前缀 {@code Unique index or primary key violation: "} 上，并用 {@code [^"]*}
     * 限定在带引号的报文段内：否则会先匹到前面 {@code SQL [INSERT ... VALUES (?, ?)]} 里的那个
     * {@code VALUES (}，再用跨行 {@code .*?} 一路吞到违规段的 {@code )"}，把中间的索引名一起抹掉。
     */
    private static final Pattern H2_UNIQUE_VALUES = Pattern.compile(
            "(Unique index or primary key violation: \"[^\"]*VALUES \\()[^\"]*(\\)\")");

    /**
     * 异常（含 cause 链）是不是数据库完整性约束异常。命中才值得过 {@link #sanitize(String)}。
     */
    public static boolean isDbConstraintViolation(Throwable e) {
        for (Throwable current = e; current != null; current = current.getCause()) {
            String className = current.getClass().getName();
            if ("org.springframework.dao.DataIntegrityViolationException".equals(className)
                    || "org.springframework.dao.DuplicateKeyException".equals(className)
                    || current instanceof SQLIntegrityConstraintViolationException) {
                return true;
            }
            if (current.getCause() == current) { // 自引用的 cause 链，避免死循环
                break;
            }
        }
        return false;
    }

    /**
     * 把数据库约束报文里的**值**换成 {@link #MASK}，其余文本原样保留。
     * 幂等：已脱敏的文本再跑一次结果不变。
     */
    public static String sanitize(String text) {
        if (StrUtil.isEmpty(text)) {
            return text;
        }
        String result = MYSQL_DUPLICATE_ENTRY.matcher(text).replaceAll("$1" + MASK + "$2");
        result = MYSQL_INCORRECT_VALUE.matcher(result).replaceAll("$1" + MASK + "$2");
        result = H2_UNIQUE_VALUES.matcher(result).replaceAll("$1" + MASK + "$2");
        return result;
    }

}
