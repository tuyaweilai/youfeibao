package cn.iocoder.yudao.module.icbc.apilog;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 守卫测试：icbc 的控制器方法（{@code /admin-api} 与 {@code /app-api} 两个前缀都走同一条
 * {@code ApiAccessLogFilter}，所以两边都扫）带 {@link RequestBody}，且请求 DTO 命中
 * {@link #WATCHED_SENSITIVE_FIELDS} 的字段名时，必须满足二者之一——
 *
 * <ol>
 *     <li>该字段名在平台的默认脱敏名单里（{@code ApiLogSanitizer.SANITIZE_KEYS}）；或</li>
 *     <li>该方法 {@code @ApiAccessLog(requestEnable = false)}，整段请求不记。</li>
 * </ol>
 *
 * <p>两个机制管的不是一件事（#98）：默认名单治「记了也不出 PII」，逐接口的
 * {@code requestEnable = false} 治「整段别记」（证件影像 10MB 的解析开销、令牌在查询串里）。
 * 所以这条测试**两边都认**，不逼任何一边消失。
 *
 * <p><b>为什么扫 icbc 而不是全平台</b>：这条测试跑在 {@code yudao-module-icbc-biz} 的测试
 * classpath 上，那里只有 icbc 的控制器（icbc → system / member / erp 只依赖对方的 {@code -api}，
 * 那几个模块的 {@code -biz} 不在 reactor 里，扫不到）。全平台的部分由**默认名单本身**兜住——
 * {@code ApiAccessLogFilter} 对所有 {@code /admin-api} 、{@code /app-api} 请求生效，不是逐模块配置。
 * 这里扫 icbc 全部控制器（{@code controller.app} + {@code controller.admin}）是当**探针**：名单被改窄、
 * 或 icbc 新加一个带敏感请求体的接口，这条测试就会红。（扫全平台要放到依赖全部模块的
 * {@code yudao-server} 里，本票不做，见报告。）
 *
 * <p><b>为什么从源码读默认名单，而不是反射读常量</b>：闸门与本地都是
 * {@code mvn -o -pl yudao-module-icbc-api,yudao-module-icbc-biz test}——框架模块**不在这个
 * reactor 里**，{@code ApiAccessLogFilter} 是从 {@code ~/.m2} 里的旧 jar 解析的。反射读到的会是
 * 改动前的名单，这条测试就会对着一个过期对象下判断（改前红、改完还红）。所以名单的唯一事实来源
 * 取**框架模块的源码文件**，与 {@code IcbcUniqueKeySchemaParityTest}「直接比对两份脚本」同一手法。
 * 名单所在文件被挪走时，{@link #testDefaultSanitizeKeysSourceIsParsable()} 会红，不会静默放过。
 */
public class ApiAccessLogPiiCoverageTest {

    /**
     * 被盯的敏感字段名：平台请求 VO / DTO 里实际出现过的「个人身份 / 联系方式 / 住址 / 影像 / 账户」字段名，
     * 外加几个与 {@code password} / {@code token} 同类的**短期凭据与密钥**（扫请求体时顺手发现的：
     * 支付 / 开票验证码、电子签控制台令牌、租户电子签密钥——漏在访问日志里比 PII 更糟）。
     *
     * <p>这里**故意不含 {@code name}**（商品名 / 企业名 / 菜单名都叫 {@code name}）——所以
     * {@code sellerName} / {@code naturalPersonName} 这类姓名**不在覆盖范围内**，姓名仍会进访问日志，
     * 这是 #98 写在票面上的已知残留，不在这里假装解决。
     */
    private static final Set<String> WATCHED_SENSITIVE_FIELDS = new TreeSet<>(Arrays.asList(
            // 证件 / 影像
            "idCardNo", "idNo", "certNo", "imageBase64",
            // 联系方式
            "mobile", "contactMobile", "driverMobile", "telephone", "phone", "payPhoneno", "sellerTelephone", "email",
            // 银行卡 / 账户
            "bankCardNo", "newBankCardNo", "bankAccount", "receiverAccount",
            "cardNumber", "drawerCardNumber", "payerAcctNum", "taxPayerAccountNo",
            // 住址
            "address", "sellerAddress",
            // 短期凭据 / 密钥
            "verifiedCode", "secretKey", "secretId", "callbackSignKey", "consoleToken"));

    /** 默认名单所在的框架源码：{@code ApiLogSanitizer.SANITIZE_KEYS}（#101 从 ApiAccessLogFilter 抽出来）。 */
    private static final String SANITIZE_KEYS_SOURCE = String.join("/",
            "yudao-framework", "yudao-spring-boot-starter-web", "src", "main", "java",
            "cn/iocoder/yudao/framework/web/core/util/ApiLogSanitizer.java");

    private static final Pattern SANITIZE_KEYS_PATTERN =
            Pattern.compile("SANITIZE_KEYS\\s*=\\s*new\\s+String\\[\\]\\s*\\{([^}]*)\\}", Pattern.DOTALL);

    private static final String[] UPSTREAM_SANITIZE_KEYS =
            new String[]{"password", "token", "accessToken", "refreshToken"};

    @Test
    public void testPiiRequestBodyFieldsAreSanitizedOrNotLogged() throws Exception {
        Set<String> defaultSanitizeKeys = readDefaultSanitizeKeys();
        List<String> offenders = new ArrayList<>();
        for (Class<?> controller : scanControllers()) {
            for (Method method : controller.getDeclaredMethods()) {
                Class<?> requestBodyType = findRequestBodyType(method);
                if (requestBodyType == null) {
                    continue;
                }
                // 该接口是否整段关了请求记录
                ApiAccessLog annotation = method.getAnnotation(ApiAccessLog.class);
                boolean requestLogDisabled = annotation != null && !annotation.requestEnable();
                for (String fieldName : collectFieldNames(requestBodyType)) {
                    if (!WATCHED_SENSITIVE_FIELDS.contains(fieldName) || defaultSanitizeKeys.contains(fieldName)
                            || requestLogDisabled) {
                        continue;
                    }
                    offenders.add(String.format("%s#%s ← %s.%s", controller.getSimpleName(), method.getName(),
                            requestBodyType.getSimpleName(), fieldName));
                }
            }
        }
        assertEquals(new ArrayList<>(), offenders, () -> "以下接口的请求体带敏感字段，但既不在默认脱敏名单里、"
                + "也没关掉请求记录（#98）：\n  " + String.join("\n  ", offenders)
                + "\n修法二选一：①把字段名加进 ApiLogSanitizer.SANITIZE_KEYS（首选，全平台生效）；"
                + "②把该字段名加进本测试的 WATCHED_SENSITIVE_FIELDS，并给接口加 @ApiAccessLog(requestEnable = false)。");
    }

    @Test
    public void testDefaultSanitizeKeysSourceIsParsable() throws IOException {
        Path source = findSanitizeKeysSource();
        Set<String> keys = parseSanitizeKeys(Files.readString(source, StandardCharsets.UTF_8));
        // 上游那四个必须还在（本地只追加，不改上游口径）
        for (String upstreamKey : UPSTREAM_SANITIZE_KEYS) {
            assertTrue(keys.contains(upstreamKey), () -> "默认脱敏名单丢了上游的 " + upstreamKey + "：" + source);
        }
        // 票面上的硬口径：不许加 name
        assertFalse(keys.contains("name"), () -> "name 太通用（商品名 / 企业名 / 菜单名都叫 name），不许进默认名单：" + source);
        assertTrue(keys.size() >= UPSTREAM_SANITIZE_KEYS.length, () -> "默认脱敏名单解析结果可疑：" + keys);
    }

    // ========== 控制器与请求体枚举 ==========

    private List<Class<?>> scanControllers() throws ClassNotFoundException {
        List<Class<?>> controllers = new ArrayList<>();
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
        for (BeanDefinition beanDefinition :
                scanner.findCandidateComponents("cn.iocoder.yudao.module.icbc.controller")) {
            controllers.add(Class.forName(beanDefinition.getBeanClassName()));
        }
        assertFalse(controllers.isEmpty(), "一个 icbc 控制器都没扫到：扫描包名或测试 classpath 变了，这条测试已经失效");
        return controllers;
    }

    private Class<?> findRequestBodyType(Method method) {
        for (Parameter parameter : method.getParameters()) {
            if (parameter.isAnnotationPresent(RequestBody.class)) {
                return parameter.getType();
            }
        }
        return null;
    }

    /**
     * 收集请求 DTO 的全部字段名：含父类（{@code extends} 的公共字段）与嵌套 DTO（{@code List<X>}、
     * 数组、直接嵌套），与 {@code ApiAccessLogFilter} 递归脱敏的覆盖范围对齐。
     * 只下钻 {@code cn.iocoder.yudao} 自己的类型，避免钻进 JDK / 三方类型。
     */
    private Set<String> collectFieldNames(Class<?> root) {
        Set<String> names = new TreeSet<>();
        Set<Class<?>> visited = new HashSet<>();
        Deque<Class<?>> queue = new ArrayDeque<>();
        queue.add(root);
        while (!queue.isEmpty()) {
            Class<?> type = queue.poll();
            for (Class<?> current = type; current != null && current != Object.class;
                 current = current.getSuperclass()) {
                if (!visited.add(current)) {
                    continue;
                }
                for (Field field : current.getDeclaredFields()) {
                    if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) {
                        continue;
                    }
                    names.add(field.getName());
                    for (Class<?> nested : nestedClasses(field.getGenericType())) {
                        if (nested.getName().startsWith("cn.iocoder.yudao.")) {
                            queue.add(nested);
                        }
                    }
                }
            }
        }
        return names;
    }

    private List<Class<?>> nestedClasses(Type type) {
        List<Class<?>> classes = new ArrayList<>();
        if (type instanceof ParameterizedType) {
            for (Type argument : ((ParameterizedType) type).getActualTypeArguments()) {
                classes.addAll(nestedClasses(argument));
            }
        } else if (type instanceof GenericArrayType) {
            classes.addAll(nestedClasses(((GenericArrayType) type).getGenericComponentType()));
        } else if (type instanceof Class) {
            Class<?> clazz = (Class<?>) type;
            if (clazz.isArray()) {
                classes.add(clazz.getComponentType());
            } else {
                classes.add(clazz);
            }
        }
        return classes;
    }

    // ========== 默认脱敏名单：从源码读 ==========

    private Set<String> readDefaultSanitizeKeys() throws IOException {
        return parseSanitizeKeys(Files.readString(findSanitizeKeysSource(), StandardCharsets.UTF_8));
    }

    private Path findSanitizeKeysSource() {
        Path directory = Paths.get("").toAbsolutePath();
        for (int i = 0; i < 8 && directory != null; i++, directory = directory.getParent()) {
            Path candidate = directory.resolve(SANITIZE_KEYS_SOURCE);
            if (Files.exists(candidate)) {
                return candidate;
            }
        }
        throw new AssertionError("从 " + Paths.get("").toAbsolutePath() + " 逐级向上都找不到 " + SANITIZE_KEYS_SOURCE
                + "：默认脱敏名单的唯一事实来源没了，这条守卫测试必须跟着改，不能静默通过");
    }

    private Set<String> parseSanitizeKeys(String source) {
        Matcher matcher = SANITIZE_KEYS_PATTERN.matcher(source);
        assertTrue(matcher.find(), "源码里找不到 SANITIZE_KEYS = new String[]{...}：" + SANITIZE_KEYS_SOURCE);
        Set<String> keys = new TreeSet<>();
        Matcher keyMatcher = Pattern.compile("\"([^\"]+)\"").matcher(matcher.group(1));
        while (keyMatcher.find()) {
            keys.add(keyMatcher.group(1));
        }
        assertFalse(keys.isEmpty(), "SANITIZE_KEYS 解析出来是空的：" + SANITIZE_KEYS_SOURCE);
        return keys;
    }

}
