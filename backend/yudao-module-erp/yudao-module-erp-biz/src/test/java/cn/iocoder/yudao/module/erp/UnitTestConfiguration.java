package cn.iocoder.yudao.module.erp;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import javax.sql.DataSource;

/**
 * ERP 单元测试的配置类。
 *
 * <p>刻意<b>不做组件扫描</b>：ERP 模块的 Bean 很多且互相依赖（采购 / 销售 / 库存 / 财务），
 * 全量扫描会让每个用例都要装配一整棵对象图。用例只 {@code @Import} 自己需要的 Service。
 */
@TestConfiguration
public class UnitTestConfiguration {

    /**
     * 创建内存数据库，用于测试。
     *
     * <p><b>必须 generateUniqueName(true)</b>：内嵌 H2 默认数据库名固定为 `testdb`（`DB_CLOSE_DELAY=-1`），
     * 多个测试上下文又会共享同一个库；而 Spring 测试上下文缓存淘汰某个上下文时会执行 `SHUTDOWN`，
     * 把共享的库删掉，另一个仍在缓存中的上下文随即变成「空库」，表现为莫名其妙的 BadSqlGrammar。
     * 每个上下文用独立的库名，上下文之间互不干扰。
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
