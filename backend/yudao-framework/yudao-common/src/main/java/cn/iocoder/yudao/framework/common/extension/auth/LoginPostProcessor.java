package cn.iocoder.yudao.framework.common.extension.auth;

import cn.iocoder.yudao.framework.common.event.auth.LoginEvent;

/**
 * 登录后处理器 SPI 接口
 * 用于扩展登录成功后的处理逻辑
 * 
 * @author ruoyi-vue-pro
 */
public interface LoginPostProcessor {

    /**
     * 处理器名称
     */
    String getName();

    /**
     * 处理优先级，数字越大优先级越高
     */
    default int getOrder() {
        return 0;
    }

    /**
     * 是否启用
     */
    default boolean isEnabled() {
        return true;
    }

    /**
     * 支持的用户类型
     */
    default boolean supports(Integer userType) {
        return true;
    }

    /**
     * 处理登录事件
     * 
     * @param event 登录事件
     * @return 是否继续执行后续处理器
     */
    boolean process(LoginEvent event);

    /**
     * 增强登录响应
     * 
     * @param respVO 登录响应
     * @param event 登录事件
     */
    default void enhanceResponse(Object respVO, LoginEvent event) {
        // 默认实现为空
    }
} 