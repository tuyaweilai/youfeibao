package cn.iocoder.yudao.test.icbc;

import cn.iocoder.yudao.framework.mybatis.core.util.MyBatisUtils;
import cn.iocoder.yudao.framework.tenant.config.TenantProperties;
import cn.iocoder.yudao.framework.tenant.core.db.TenantDatabaseInterceptor;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.Set;

/**
 * 让 icbc 单元测试真正打开 MyBatis-Plus 的多租户拦截器。
 *
 * <p>{@code BaseDbUnitTest} 默认只装配 DB 相关配置，不带 {@code YudaoTenantAutoConfiguration}，
 * 所以普通的 icbc 单测里租户过滤是关着的。凡是「验证租户隔离」的用例，
 * 都要额外 {@code @Import} 本配置，让租户拦截器上线。
 *
 * <p>注意：本类刻意放在 {@code cn.iocoder.yudao.test.icbc} 而不是
 * {@code cn.iocoder.yudao.module.icbc} 下——后者的 {@code UnitTestConfiguration}
 * 会组件扫描整个模块，放在那里会被所有 icbc 单测无条件加载，租户过滤就会意外打开。
 */
@TestConfiguration
public class IcbcTenantTestConfiguration {

    @Bean
    public static BeanPostProcessor tenantLineInnerInterceptorPostProcessor() {
        return new BeanPostProcessor() {

            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (bean instanceof MybatisPlusInterceptor) {
                    TenantProperties properties = new TenantProperties();
                    // 与生产 application.yaml 的 yudao.tenant.ignore-tables 保持一致：
                    // 这几张是无租户隔离语义的全局表（报废产品编码、公开令牌、平台计费台账）
                    properties.setIgnoreTables(Set.of(
                            "icbc_scrap_code", "icbc_public_token", "icbc_billing_ledger"));
                    MyBatisUtils.addInterceptor((MybatisPlusInterceptor) bean,
                            new TenantLineInnerInterceptor(new TenantDatabaseInterceptor(properties)), 0);
                }
                return bean;
            }

        };
    }

}
