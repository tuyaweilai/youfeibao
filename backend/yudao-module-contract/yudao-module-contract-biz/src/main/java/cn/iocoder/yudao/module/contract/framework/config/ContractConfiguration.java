package cn.iocoder.yudao.module.contract.framework.config;

import cn.iocoder.yudao.framework.swagger.config.YudaoSwaggerAutoConfiguration;
import org.springdoc.core.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * 合同模块配置类
 *
 * @author 芋道源码
 */
@Configuration
@ComponentScan("cn.iocoder.yudao.module.contract")
public class ContractConfiguration {

    /**
     * 合同模块的 API 分组
     */
    @Bean
    public GroupedOpenApi contractGroupedOpenApi() {
        return YudaoSwaggerAutoConfiguration.buildGroupedOpenApi("contract");
    }

} 