package cn.iocoder.yudao.module.enterprise.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 企业模块配置属性
 * 
 * @author ruoyi-vue-pro
 */
@Data
@Component
@ConfigurationProperties(prefix = "yudao.enterprise")
public class EnterpriseProperties {

    /**
     * 是否启用企业模块
     */
    private Boolean enabled = false;

    /**
     * 企业绑定配置
     */
    private Binding binding = new Binding();

    /**
     * 企业认证配置
     */
    private Authentication authentication = new Authentication();

    @Data
    public static class Binding {
        /**
         * 是否启用自动绑定
         */
        private Boolean autoBinding = true;

        /**
         * 绑定失败是否中断登录
         */
        private Boolean failureInterruptLogin = false;

        /**
         * 绑定超时时间（秒）
         */
        private Integer timeoutSeconds = 30;
    }

    @Data
    public static class Authentication {
        /**
         * 是否启用企业认证
         */
        private Boolean enabled = true;

        /**
         * 认证提供商
         */
        private String provider = "esign";
    }
} 