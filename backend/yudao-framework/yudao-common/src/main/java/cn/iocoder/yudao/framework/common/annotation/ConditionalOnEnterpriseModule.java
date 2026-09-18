package cn.iocoder.yudao.framework.common.annotation;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

/**
 * 企业模块条件注解
 * 当启用企业模块时，才装配相关的 Bean
 * 
 * @author ruoyi-vue-pro
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ConditionalOnProperty(prefix = "yudao.enterprise", name = "enabled", havingValue = "true", matchIfMissing = false)
public @interface ConditionalOnEnterpriseModule {

    /**
     * 别名，用于指定具体的企业功能模块
     */
    @AliasFor(annotation = ConditionalOnProperty.class, attribute = "name")
    String[] value() default "enabled";

    /**
     * 是否必须匹配
     */
    @AliasFor(annotation = ConditionalOnProperty.class, attribute = "matchIfMissing")
    boolean matchIfMissing() default false;
} 