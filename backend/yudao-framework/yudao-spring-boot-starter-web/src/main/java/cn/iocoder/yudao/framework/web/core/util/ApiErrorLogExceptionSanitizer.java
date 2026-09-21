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
 *   <li><b>含单引号（MySQL 转义成 {@code \'}）的值整段不匹配、整段留着</b>：
 *       {@code Duplicate entry 'O\'Brien' for key 'x'} 里 {@code [^']*} 过不去第一个 {@code \'}，
 *       紧接着又要求 {@code ' for key}，于是**整条不匹配、这个值一个字都没被替换**（不是
 *       「只吃掉一半 / 漏出尾部」——原注释方向写反了，本票纠正）。姓名类会整段漏出；数字型 PII
 *       （身份证 / 手机号 / 卡号）不含单引号，不受影响。要治得把转义也写进正则，代价是正则更脆。</li>
 *   <li><b>类型不匹配的路径 / 查询参数回给响应</b>：{@code methodArgumentTypeMismatchExceptionHandler}
 *       用 {@code ex.getMessage()} 拼返回文案，里面的原值（{@code failed to convert value of type ...}）
 *       会到调用方；本类只治落库的三个文本字段，管不到这里。</li>
 *   <li><b>{@code @Valid} / {@code BindException} 失败会把原值写进日志（现实可达，不是「低风险」）</b>：
 *       这两个 handler 做 {@code log.warn(..., ex)}，而 Spring 的 {@code MethodArgumentNotValidException}
 *       消息里带 {@code rejected value [<原值>]}，经 logback 写进**控制台 + 保留 30 天的 FILE appender
 *       （{@code logback-spring.xml} {@code maxHistory=30}）+ SkyWalking GRPC 日志中心**。挂校验注解、
 *       且能对**非空值**失败（{@code @Pattern} / {@code @Size} / {@code @Mobile} / {@code @Length}）的
 *       PII 字段至少有：{@code OnboardingWizardSubmitReqVO}（身份证 / 手机号 / 银行卡号 {@code @Pattern}）、
 *       {@code SellerSmsLoginReqVO} / {@code SellerSmsSendReqVO}（**免登录**手机号 {@code @Pattern}）、
 *       {@code SellerBankCardChangeReqVO}（银行卡号 {@code @NotBlank}/{@code @Size}）、{@code PayeeInfoSaveReqVO} /
 *       {@code PayeeAddReqVO} / {@code PayerInfoSaveReqVO} / {@code PayerAddReqVO}（手机号 / 卡号）、
 *       {@code SellerContactFallbackReqVO}（手机号）、{@code InvoicePreOrderReqVO}（收方地址 / 电话 {@code @Size}），
 *       以及 member app 的登录 / 改手机号（{@code @Mobile}）、system 的 admin 登录 / 用户保存（{@code @Mobile}）。
 *       返回给用户的文案走 {@code getDefaultMessage()}、不含值；这条**根本不写 {@code infra_api_error_log}**，
 *       走的是文件 / 控制台 / 日志中心——不在本票（只治落库三字段）的射程内，是**留给下游票的泄漏点**。</li>
 *   <li><b>非 DB 约束来源的值</b>：第三方 SDK 报文、我们自己 {@code exception(CODE, 拼值)} 且未改的文案、
 *       非 JDBC 的异常——按设计不覆盖（{@link #isDbConstraintViolation(Throwable)} 不命中就不调用），
 *       要在各自的发生位置治。按字段名 grep：手机号 / 身份证 / 银行卡 / 住址只命中已改的
 *       {@code member.USER_MOBILE_USED}；但**自然人姓名**这类不含固定字段名的值会漏掉 grep，例如
 *       {@code AcquisitionServiceImpl:373} 把 {@code order.getCounterpartyName()} 拼进
 *       {@code ACQUISITION_PURCHASE_ORDER_COUNTERPARTY_MISMATCH}（ADR 0027 下对手方可以是自然人出售者）。
 *       走 HTTP 控制器时它是 {@code ServiceException}，由 {@code serviceExceptionHandler} 处理——只 warn
 *       第一层栈帧、**不写** {@code infra_api_error_log}，值回在**响应体** {@code CommonResult.msg} 里，
 *       **不进那三个字段**（这条路径本票不覆盖；要治得逐个改文案）。</li>
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
