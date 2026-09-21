package cn.iocoder.yudao.framework.web.core.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;

import java.util.Iterator;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString;

/**
 * API 日志请求参数的共用脱敏器：访问日志（{@code infra_api_access_log}）与异常日志
 * （{@code infra_api_error_log}）走**同一份名单、同一段递归逻辑**。
 *
 * <p><b>为什么放在 {@code framework/web} 而不是 {@code framework/apilog}</b>（#101）：两条日志路径分属
 * {@link cn.iocoder.yudao.framework.apilog.core.filter.ApiAccessLogFilter}（apilog）与
 * {@link cn.iocoder.yudao.framework.web.core.handler.GlobalExceptionHandler}（web）。apilog 已经依赖
 * web（Filter 继承 web 的 {@code ApiRequestFilter}），若把共用脱敏器放进 apilog，就变成 web 反向依赖
 * apilog，形成包级循环。放 web 是最底层、两边都能依赖的位置。
 *
 * <p><b>名单只有这一处</b>：{@link #SANITIZE_KEYS}。改名单不会漏掉任何一条日志路径。
 * 守卫测试（icbc 的 {@code ApiAccessLogPiiCoverageTest}）直接从本文件源码解析该名单。
 *
 * <p><b>已知残留</b>：脱敏按**键名**递归做，不认值的内容——{@code name} / {@code code} 这类太通用的
 * 字段名进不了名单（商品名 / 企业名 / 菜单名都叫 name；短信验证码在部分接口就叫 code），
 * 因此**姓名与个别验证码仍会进日志**，这是字段名粒度脱敏的天生上限，不是本类能解决的。
 *
 * @see cn.iocoder.yudao.framework.apilog.core.filter.ApiAccessLogFilter
 * @see cn.iocoder.yudao.framework.web.core.handler.GlobalExceptionHandler
 */
@Slf4j
public class ApiLogSanitizer {

    // ========== 默认脱敏名单 ==========
    // 1. 上游默认四个键：password、token、accessToken、refreshToken。
    // 2. 本地追加（#98「平台访问日志的 PII 面级收口」）：平台的 ApiAccessLogFilter 默认把 /admin-api 与
    //    /app-api 的 JSON 请求体写进 infra_api_access_log.request_params，而上游只脱敏上面那四个键，
    //    于是带 PII 的请求体（身份证号、银行卡号、手机号、住址、证件影像 base64……）会被整段留库。这是
    //    本地偏离**：默认安全优先——以后任何人写的新接口，只要字段名命中就自动脱敏，不靠每个接口记得
    //    加注解（逐接口加注解要改 34+ 处，还会永久腐化）。上游默认名单不含 PII，这条本身值得提 PR。
    // 3. 有意**不加** name / *Name：商品名、企业名、菜单名、自然人姓名都叫 name，加了会把访问日志废掉。
    //    代价是**姓名仍会进访问日志**，这是 #98 写在票面上的已知残留，不在这里假装解决。
    // 4. 脱敏按**键名**递归做（数组、嵌套对象都走），不认值的内容——换个字段名就绕过了。所以新增请求
    //    DTO 时字段名要往这份名单里的写法靠；守卫测试：icbc 的 ApiAccessLogPiiCoverageTest（扫控制器 +
    //    请求 DTO 字段，落在名单外就红），行为测试：ApiLogSanitizerTest。
    // 5. #101 起，异常日志的请求参数也走这份名单（此前 GlobalExceptionHandler 自己拼 requestParams 且不脱敏）。
    public static final String[] SANITIZE_KEYS = new String[]{
            // 上游默认
            "password", "token", "accessToken", "refreshToken",
            // #98：证件 / 影像
            "idCardNo", "idNo", "certNo", "imageBase64",
            // #98：联系方式
            "mobile", "contactMobile", "driverMobile", "telephone", "phone", "payPhoneno",
            "sellerTelephone", "email",
            // #98：银行卡 / 账户
            "bankCardNo", "newBankCardNo", "bankAccount", "receiverAccount",
            "cardNumber", "drawerCardNumber", "payerAcctNum", "taxPayerAccountNo",
            // #98：住址
            "address", "sellerAddress",
            // #98 顺带：与 password / token 同类的短期凭据与密钥（扫请求体时发现的，漏在日志里更糟）
            "verifiedCode", "secretKey", "secretId", "callbackSignKey", "consoleToken"};

    /**
     * 脱敏查询串（Map 形式），返回脱敏后的 JSON 字符串；空 map 返回 {@code null}
     *
     * @param sanitizeKeys 方法级额外脱敏字段（来自 {@code @ApiAccessLog(sanitizeKeys = ...)}），可为空
     */
    public static String sanitizeMap(Map<String, ?> map, String[] sanitizeKeys) {
        if (CollUtil.isEmpty(map)) {
            return null;
        }
        sanitizeMapInPlace(map, sanitizeKeys);
        return JsonUtils.toJsonString(map);
    }

    /**
     * 就地脱敏查询串（Map 形式），不改变调用方对「query 是个对象」这个形状的预期。
     * 异常日志（{@code infra_api_error_log}）历史上就是 {@code {"query":{...},"body":"..."}}，
     * 与访问日志把 query 序列化成字符串的形状不同，这里保留异常日志的原形状。
     */
    public static void sanitizeMapInPlace(Map<String, ?> map, String[] sanitizeKeys) {
        if (CollUtil.isEmpty(map)) {
            return;
        }
        if (sanitizeKeys != null) {
            MapUtil.removeAny(map, sanitizeKeys);
        }
        MapUtil.removeAny(map, SANITIZE_KEYS);
    }

    /**
     * 脱敏 JSON 请求体，返回脱敏后的 JSON 字符串；空字符串返回 {@code null}
     */
    public static String sanitizeJson(String jsonString, String[] sanitizeKeys) {
        if (StrUtil.isEmpty(jsonString)) {
            return null;
        }
        try {
            JsonNode rootNode = JsonUtils.parseTree(jsonString);
            sanitizeJson(rootNode, sanitizeKeys);
            return JsonUtils.toJsonString(rootNode);
        } catch (Exception e) {
            // 脱敏失败的情况下，直接忽略异常，避免影响用户请求
            log.error("[sanitizeJson][脱敏({}) 发生异常]", jsonString, e);
            return jsonString;
        }
    }

    /**
     * 脱敏响应结果（{@link CommonResult}），只处理 {@code data} 字段，不处理 {@code code} / {@code msg}
     */
    public static String sanitizeJson(CommonResult<?> commonResult, String[] sanitizeKeys) {
        if (commonResult == null) {
            return null;
        }
        String jsonString = toJsonString(commonResult);
        try {
            JsonNode rootNode = JsonUtils.parseTree(jsonString);
            sanitizeJson(rootNode.get("data"), sanitizeKeys); // 只处理 data 字段，不处理 code、msg 字段，避免错误被脱敏掉
            return JsonUtils.toJsonString(rootNode);
        } catch (Exception e) {
            // 脱敏失败的情况下，直接忽略异常，避免影响用户请求
            log.error("[sanitizeJson][脱敏({}) 发生异常]", jsonString, e);
            return jsonString;
        }
    }

    private static void sanitizeJson(JsonNode node, String[] sanitizeKeys) {
        // 情况一：数组，遍历处理
        if (node.isArray()) {
            for (JsonNode childNode : node) {
                sanitizeJson(childNode, sanitizeKeys);
            }
            return;
        }
        // 情况二：非 Object，只是某个值，直接返回
        if (!node.isObject()) {
            return;
        }
        //  情况三：Object，遍历处理
        Iterator<Map.Entry<String, JsonNode>> iterator = node.fields();
        while (iterator.hasNext()) {
            Map.Entry<String, JsonNode> entry = iterator.next();
            if (ArrayUtil.contains(sanitizeKeys, entry.getKey())
                || ArrayUtil.contains(SANITIZE_KEYS, entry.getKey())) {
                iterator.remove();
                continue;
            }
            sanitizeJson(entry.getValue(), sanitizeKeys);
        }
    }

}
