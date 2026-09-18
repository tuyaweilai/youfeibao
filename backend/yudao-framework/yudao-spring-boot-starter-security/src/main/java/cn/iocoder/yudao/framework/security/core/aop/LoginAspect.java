package cn.iocoder.yudao.framework.security.core.aop;

import cn.iocoder.yudao.framework.common.event.auth.LoginEvent;
import cn.iocoder.yudao.framework.security.core.service.LoginPostProcessorManager;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.enums.UserTypeEnum.ADMIN;

/**
 * 登录切面
 * 处理登录成功后的事件发布和扩展处理
 * 
 * @author ruoyi-vue-pro
 */
@Aspect
@Component
@Slf4j
public class LoginAspect {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private LoginPostProcessorManager postProcessorManager;

    /**
     * 登录成功后处理的切点
     */
    @Pointcut("execution(* cn.iocoder.yudao.module.system.service.auth.AdminAuthServiceImpl.createTokenAfterLoginSuccess(..))")
    public void loginSuccessPointcut() {}

    /**
     * 环绕通知 - 处理登录成功后的扩展逻辑
     */
    @Around("loginSuccessPointcut()")
    public Object aroundLoginSuccess(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        Object user = args[0];
        Object logType = args[1];

        // 执行原始方法
        Object result = joinPoint.proceed();

        try {
            // 创建登录事件
            LoginEvent loginEvent = new LoginEvent(
                    this,
                    getUserId(user),
                    getUsername(user),
                    getDeptId(user),
                    getTenantId(user),
                    getRoleCodes(user),
                    getLogType(logType),
                    ADMIN.getValue()
            );

            // 发布登录事件
            eventPublisher.publishEvent(loginEvent);

            // 处理登录后逻辑
            postProcessorManager.processLogin(loginEvent);

            // 增强响应
            postProcessorManager.enhanceResponse(result, loginEvent);

        } catch (Exception e) {
            log.error("登录后处理发生异常", e);
            // 不影响登录结果
        }

        return result;
    }

    private Long getUserId(Object user) {
        try {
            Method method = user.getClass().getMethod("getId");
            Object result = method.invoke(user);
            return result instanceof Long ? (Long) result : null;
        } catch (Exception e) {
            log.error("获取用户ID失败", e);
            return null;
        }
    }

    private String getUsername(Object user) {
        try {
            Method method = user.getClass().getMethod("getUsername");
            Object result = method.invoke(user);
            return result instanceof String ? (String) result : null;
        } catch (Exception e) {
            log.error("获取用户名失败", e);
            return null;
        }
    }

    private Long getDeptId(Object user) {
        try {
            Method method = user.getClass().getMethod("getDeptId");
            Object result = method.invoke(user);
            return result instanceof Long ? (Long) result : null;
        } catch (Exception e) {
            log.error("获取部门ID失败", e);
            return null;
        }
    }

    private Long getTenantId(Object user) {
        try {
            Method method = user.getClass().getMethod("getTenantId");
            Object result = method.invoke(user);
            return result instanceof Long ? (Long) result : null;
        } catch (Exception e) {
            log.error("获取租户ID失败", e);
            return null;
        }
    }

    private Set<String> getRoleCodes(Object user) {
        try {
            Method method = user.getClass().getMethod("getRoleCodes");
            Object result = method.invoke(user);
            if (result instanceof Set) {
                return (Set<String>) result;
            }
            return new HashSet<>();
        } catch (Exception e) {
            log.error("获取角色编码失败", e);
            return new HashSet<>();
        }
    }

    private Integer getLogType(Object logType) {
        if (logType instanceof Integer) {
            return (Integer) logType;
        }
        try {
            Method method = logType.getClass().getMethod("getType");
            Object result = method.invoke(logType);
            return result instanceof Integer ? (Integer) result : null;
        } catch (Exception e) {
            log.error("获取日志类型失败", e);
            return null;
        }
    }
} 