package cn.iocoder.yudao.module.enterprise.service.auth;

import cn.iocoder.yudao.framework.common.annotation.ConditionalOnEnterpriseModule;
import cn.iocoder.yudao.framework.common.event.auth.LoginEvent;
import cn.iocoder.yudao.framework.common.extension.auth.LoginPostProcessor;
import cn.iocoder.yudao.module.enterprise.api.binding.EnterpriseBindingApi;
import cn.iocoder.yudao.module.enterprise.api.binding.dto.EnterpriseBindingResultVO;
import cn.iocoder.yudao.module.enterprise.enums.binding.EnterpriseBindingStatusEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

import static cn.iocoder.yudao.framework.common.enums.UserTypeEnum.ADMIN;

/**
 * 企业绑定登录处理器
 * 在用户登录成功后自动处理企业绑定逻辑
 * 
 * @author ruoyi-vue-pro
 */
@Component
@ConditionalOnEnterpriseModule
@Order(100)
@Slf4j
public class EnterpriseBindingLoginProcessor implements LoginPostProcessor {

    @Autowired
    private EnterpriseBindingApi enterpriseBindingApi;

    /**
     * 存储当前线程的绑定结果
     */
    private static final ThreadLocal<EnterpriseBindingResultVO> BINDING_RESULT_HOLDER = new ThreadLocal<>();

    @Override
    public String getName() {
        return "企业绑定登录处理器";
    }

    @Override
    public int getOrder() {
        return 100;
    }

    @Override
    public boolean supports(Integer userType) {
        return ADMIN.getValue().equals(userType);
    }

    @Override
    public boolean process(LoginEvent event) {
        try {
            log.info("[企业绑定处理器] 开始处理用户企业绑定, userId: {}, deptId: {}, tenantId: {}, roles: {}", 
                    event.getUserId(), event.getDeptId(), event.getTenantId(), event.getRoleCodes());

            // 调用企业绑定API
            EnterpriseBindingResultVO bindingResult = enterpriseBindingApi.checkAndBindUserToEnterprise(
                    event.getUserId(),
                    event.getDeptId(),
                    event.getTenantId(),
                    event.getRoleCodes()
            );

            // 将结果存储到ThreadLocal中，供响应增强使用
            BINDING_RESULT_HOLDER.set(bindingResult);

            log.info("[企业绑定处理器] 企业绑定结果: status={}, enterpriseId={}, message={}",
                    bindingResult.getEnterpriseBindingStatus(), 
                    bindingResult.getBoundEnterpriseId(),
                    bindingResult.getEnterpriseBindingMessage());

            return true; // 继续执行后续处理器
            
        } catch (Exception e) {
            log.error("[企业绑定处理器] 企业绑定过程发生异常: {}", e.getMessage(), e);
            
            // 创建错误绑定结果
            EnterpriseBindingResultVO errorResult = EnterpriseBindingResultVO.builder()
                    .enterpriseBindingStatus(EnterpriseBindingStatusEnum.BINDING_ERROR.getCode())
                    .enterpriseBindingMessage("企业绑定过程发生错误，请联系管理员")
                    .build();
            
            BINDING_RESULT_HOLDER.set(errorResult);
            
            // 不中断登录流程
            return true;
        }
    }

    @Override
    public void enhanceResponse(Object respObj, LoginEvent event) {
        if (respObj == null) {
            return;
        }

        try {
            // 获取存储的绑定结果
            EnterpriseBindingResultVO bindingResult = BINDING_RESULT_HOLDER.get();
            if (bindingResult == null) {
                return;
            }

            // 通过反射设置企业绑定信息
            setFieldByReflection(respObj, "setEnterpriseBindingStatus", Integer.class, bindingResult.getEnterpriseBindingStatus());
            setFieldByReflection(respObj, "setBoundEnterpriseId", Long.class, bindingResult.getBoundEnterpriseId());
            setFieldByReflection(respObj, "setBoundEnterpriseName", String.class, bindingResult.getBoundEnterpriseName());
            setFieldByReflection(respObj, "setEnterpriseBindingMessage", String.class, bindingResult.getEnterpriseBindingMessage());
                
                log.debug("[企业绑定处理器] 已增强登录响应，添加企业绑定信息");

        } catch (Exception e) {
            log.error("[企业绑定处理器] 设置企业绑定信息失败", e);
        } finally {
            // 清理ThreadLocal
            BINDING_RESULT_HOLDER.remove();
        }
    }

    /**
     * 通过反射设置字段值
     */
    private void setFieldByReflection(Object obj, String methodName, Class<?> paramType, Object value) {
        if (value == null) {
            return;
        }
        
        try {
            Method method = obj.getClass().getMethod(methodName, paramType);
            method.invoke(obj, value);
        } catch (NoSuchMethodException e) {
            log.debug("[企业绑定处理器] 响应对象不支持方法: {}", methodName);
        } catch (Exception e) {
            log.warn("[企业绑定处理器] 调用方法 {} 失败: {}", methodName, e.getMessage());
        }
    }
} 