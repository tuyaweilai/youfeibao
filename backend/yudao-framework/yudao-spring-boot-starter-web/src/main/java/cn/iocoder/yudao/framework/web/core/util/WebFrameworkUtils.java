package cn.iocoder.yudao.framework.web.core.util;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.iocoder.yudao.framework.common.enums.TerminalEnum;
import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.servlet.ServletUtils;
import cn.iocoder.yudao.framework.web.config.WebProperties;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

/**
 * 专属于 web 包的工具类
 *
 * @author 芋道源码
 */
public class WebFrameworkUtils {

    private static final String REQUEST_ATTRIBUTE_LOGIN_USER_ID = "login_user_id";
    private static final String REQUEST_ATTRIBUTE_LOGIN_USER_TYPE = "login_user_type";

    private static final String REQUEST_ATTRIBUTE_COMMON_RESULT = "common_result";

    // ========== 请求日志（请求参数记录）的开关与脱敏名单 ==========
    // 由 ApiAccessLogInterceptor 在 preHandle 时按 @ApiAccessLog 解析后写入请求属性，
    // 供 ApiAccessLogFilter 与 GlobalExceptionHandler 复用。放在 web 包（而不是 apilog 包）的原因：
    // web 是最底层，apilog 已经依赖 web（ApiAccessLogFilter extends ApiRequestFilter），
    // 反向让 web 引用 apilog 的注解会形成包级循环。见 #101。

    private static final String REQUEST_ATTRIBUTE_REQUEST_LOG_ENABLE = "request_log_enable";

    private static final String REQUEST_ATTRIBUTE_REQUEST_LOG_SANITIZE_KEYS = "request_log_sanitize_keys";

    public static final String HEADER_TENANT_ID = "tenant-id";

    /**
     * 终端的 Header
     *
     * @see cn.iocoder.yudao.framework.common.enums.TerminalEnum
     */
    public static final String HEADER_TERMINAL = "terminal";

    private static WebProperties properties;

    public WebFrameworkUtils(WebProperties webProperties) {
        WebFrameworkUtils.properties = webProperties;
    }

    /**
     * 获得租户编号，从 header 中
     * 考虑到其它 framework 组件也会使用到租户编号，所以不得不放在 WebFrameworkUtils 统一提供
     *
     * @param request 请求
     * @return 租户编号
     */
    public static Long getTenantId(HttpServletRequest request) {
        String tenantId = request.getHeader(HEADER_TENANT_ID);
        return NumberUtil.isNumber(tenantId) ? Long.valueOf(tenantId) : null;
    }

    public static void setLoginUserId(ServletRequest request, Long userId) {
        request.setAttribute(REQUEST_ATTRIBUTE_LOGIN_USER_ID, userId);
    }

    /**
     * 设置用户类型
     *
     * @param request 请求
     * @param userType 用户类型
     */
    public static void setLoginUserType(ServletRequest request, Integer userType) {
        request.setAttribute(REQUEST_ATTRIBUTE_LOGIN_USER_TYPE, userType);
    }

    /**
     * 获得当前用户的编号，从请求中
     * 注意：该方法仅限于 framework 框架使用！！！
     *
     * @param request 请求
     * @return 用户编号
     */
    public static Long getLoginUserId(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        return (Long) request.getAttribute(REQUEST_ATTRIBUTE_LOGIN_USER_ID);
    }

    /**
     * 获得当前用户的类型
     * 注意：该方法仅限于 web 相关的 framework 组件使用！！！
     *
     * @param request 请求
     * @return 用户编号
     */
    public static Integer getLoginUserType(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        // 1. 优先，从 Attribute 中获取
        Integer userType = (Integer) request.getAttribute(REQUEST_ATTRIBUTE_LOGIN_USER_TYPE);
        if (userType != null) {
            return userType;
        }
        // 2. 其次，基于 URL 前缀的约定
        if (request.getServletPath().startsWith(properties.getAdminApi().getPrefix())) {
            return UserTypeEnum.ADMIN.getValue();
        }
        if (request.getServletPath().startsWith(properties.getAppApi().getPrefix())) {
            return UserTypeEnum.MEMBER.getValue();
        }
        return null;
    }

    public static Integer getLoginUserType() {
        HttpServletRequest request = getRequest();
        return getLoginUserType(request);
    }

    public static Long getLoginUserId() {
        HttpServletRequest request = getRequest();
        return getLoginUserId(request);
    }

    public static Integer getTerminal() {
        HttpServletRequest request = getRequest();
        if (request == null) {
            return TerminalEnum.UNKNOWN.getTerminal();
        }
        String terminalValue = request.getHeader(HEADER_TERMINAL);
        return NumberUtil.parseInt(terminalValue, TerminalEnum.UNKNOWN.getTerminal());
    }

    public static void setCommonResult(ServletRequest request, CommonResult<?> result) {
        request.setAttribute(REQUEST_ATTRIBUTE_COMMON_RESULT, result);
    }

    public static CommonResult<?> getCommonResult(ServletRequest request) {
        return (CommonResult<?>) request.getAttribute(REQUEST_ATTRIBUTE_COMMON_RESULT);
    }

    /**
     * 设置「是否记录请求参数」的开关（来自 {@code @ApiAccessLog(requestEnable = ...)}）
     */
    public static void setRequestLogEnabled(ServletRequest request, boolean enabled) {
        request.setAttribute(REQUEST_ATTRIBUTE_REQUEST_LOG_ENABLE, enabled);
    }

    /**
     * 是否记录请求参数。请求属性未设置时默认记录（与 {@code @ApiAccessLog.requestEnable} 的默认值一致）。
     */
    public static boolean isRequestLogEnabled(HttpServletRequest request) {
        if (request == null) {
            return true;
        }
        Object value = request.getAttribute(REQUEST_ATTRIBUTE_REQUEST_LOG_ENABLE);
        return !(value instanceof Boolean) || (Boolean) value;
    }

    /**
     * 设置方法级额外的脱敏字段（来自 {@code @ApiAccessLog(sanitizeKeys = ...)}）
     */
    public static void setRequestLogSanitizeKeys(ServletRequest request, String[] sanitizeKeys) {
        request.setAttribute(REQUEST_ATTRIBUTE_REQUEST_LOG_SANITIZE_KEYS, sanitizeKeys);
    }

    /**
     * 方法级额外的脱敏字段，未设置时返回 {@code null}（表示只走默认名单）
     */
    public static String[] getRequestLogSanitizeKeys(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        Object value = request.getAttribute(REQUEST_ATTRIBUTE_REQUEST_LOG_SANITIZE_KEYS);
        return value instanceof String[] ? (String[]) value : null;
    }

    public static HttpServletRequest getRequest() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (!(requestAttributes instanceof ServletRequestAttributes)) {
            return null;
        }
        ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) requestAttributes;
        return servletRequestAttributes.getRequest();
    }

}
