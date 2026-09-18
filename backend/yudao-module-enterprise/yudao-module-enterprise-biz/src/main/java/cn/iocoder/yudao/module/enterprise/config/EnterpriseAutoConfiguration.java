package cn.iocoder.yudao.module.enterprise.config;

import cn.iocoder.yudao.framework.common.annotation.ConditionalOnEnterpriseModule;
import cn.iocoder.yudao.module.enterprise.service.auth.EnterpriseBindingLoginProcessor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 企业模块自动配置
 * 
 * @author ruoyi-vue-pro
 */
@Configuration
@ConditionalOnEnterpriseModule
@EnableConfigurationProperties(EnterpriseProperties.class)
public class EnterpriseAutoConfiguration {

    /**
     * 企业绑定登录处理器
     */
    @Bean
    @ConditionalOnEnterpriseModule(value = "binding.auto-binding")
    public EnterpriseBindingLoginProcessor enterpriseBindingLoginProcessor() {
        return new EnterpriseBindingLoginProcessor();
    }
} 