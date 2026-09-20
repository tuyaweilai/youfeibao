package cn.iocoder.yudao.module.logistics;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import javax.sql.DataSource;

/**
 * 物流模块单元测试的配置类。
 *
 * <p><b>必须 generateUniqueName(true)</b>：内嵌 H2 默认数据库名固定为 `testdb`（`DB_CLOSE_DELAY=-1`），
 * 多个测试上下文会共享同一个库；而 Spring 测试上下文缓存淘汰某个上下文时会执行 `SHUTDOWN`，
 * 把共享的库删掉，另一个仍在缓存中的上下文随即变成「空库」，表现为莫名其妙的 BadSqlGrammar。
 * 每个上下文用独立的库名，上下文之间互不干扰。
 *
 * <p>`@ComponentScan` 只扫本模块：物流模块**不依赖 icbc**（ADR 0032），测试上下文里也不应该有它的 bean。
 */
@TestConfiguration
@ComponentScan(basePackages = "cn.iocoder.yudao.module.logistics")
public class UnitTestConfiguration {

    /**
     * 创建内存数据库，用于测试
     */
    @Bean
    @Primary
    public DataSource dataSource() {
        return new EmbeddedDatabaseBuilder()
                .generateUniqueName(true)
                .setType(EmbeddedDatabaseType.H2)
                .addScript("/sql/create_tables.sql")
                .build();
    }

}
