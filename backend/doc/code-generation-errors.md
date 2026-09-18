# 代码生成错误记录

在生成物流模块车辆信息管理功能代码时，遇到了几个常见错误，记录如下以避免将来重复出现：

## 1. 注解路径和名称错误

**问题描述**：
Controller类中引用了不存在的注解路径`cn.iocoder.yudao.framework.operatelog.core.annotations.OperateLog`和`framework.operatelog.core.enums.OperateTypeEnum`

**解决方法**：
- 使用正确的API日志注解：`cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog`
- 使用正确的操作类型枚举：`cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum`
- 注解参数从`type = EXPORT`改为`operateType = EXPORT`

**避免方法**：
- 在生成代码前检查项目中已存在的类似功能模块，查看其正确使用的注解
- 使用IDE的自动导入功能，确保引入存在的类
- 遵循项目中统一的日志和操作记录实现方式

## 2. 工具类方法使用错误

**问题描述**：
在转换器类中使用了`CollectionUtils.isEmpty(list)`检查集合是否为空，但该方法在项目自定义的CollectionUtils中不存在

**解决方法**：
- 使用Hutool工具库提供的方法：`cn.hutool.core.collection.CollUtil.isEmpty(list)`

**避免方法**：
- 熟悉项目使用的工具类库（如Hutool）提供的常用方法
- 检查集合为空的判断统一使用`CollUtil.isEmpty()`而非自定义的CollectionUtils
- 参考项目中已有代码的实现方式，保持一致性

## 3. 测试类缺少必要的导入

**问题描述**：
单元测试类中使用了`assertServiceException`断言方法但没有导入相应的静态方法

**解决方法**：
- 添加正确的静态导入：`import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;`

**避免方法**：
- 编写测试类时参考项目中已有的测试类实现
- 确保所有断言方法都正确导入
- 使用IDE的代码分析功能及早发现未解析的方法引用

## 4. 对象映射警告（非阻断性）

**问题描述**：
MapStruct对象转换时提示未映射的属性："Unmapped target properties: id, tenantId, enterpriseName"

**解决方法**：
- 这是警告而非错误，不影响编译，可在后续完善映射关系
- 可以在@Mapping注解中显式指定忽略这些属性，或提供映射实现

**避免方法**：
- 确保VO/DO对象的字段名称一致，减少手动映射需求
- 在转换器接口中正确处理所有字段的映射关系
- 对于特殊字段（如租户ID等系统字段），考虑统一处理或显式忽略

## 5. 单元测试配置错误

**问题描述**：
运行单元测试时出现错误：`Could not resolve placeholder 'yudao.info.base-package' in value "${yudao.info.base-package}"`

**解决方法**：
1. 在测试模块中创建必要的配置文件：
   - 在`src/test/resources`目录下创建`application-unit-test.yaml`文件
   - 在配置文件中添加`yudao.info.base-package: cn.iocoder.yudao`配置项
   - 同时配置测试环境需要的其他配置（如数据源、Redis等）

2. 创建测试SQL脚本：
   - 在`src/test/resources/sql`目录下创建`create_tables.sql`文件
   - 添加单元测试需要的表结构创建语句
   - 同时创建`clean.sql`文件用于清理测试数据

3. 修改测试类：
   - 在测试类上添加`@ActiveProfiles("unit-test")`注解，指定使用unit-test配置文件

**避免方法**：
- 创建新模块时，从现有模块复制完整的测试配置结构
- 确保测试资源目录结构符合项目规范：
  ```
  src/test/resources/
  ├── application-unit-test.yaml  # 单元测试配置文件
  └── sql/
      ├── create_tables.sql       # 测试表结构创建脚本
      └── clean.sql               # 测试数据清理脚本
  ```
- 参考其他模块的测试类实现，确保注解和配置完整

## 6. 单元测试SQL脚本缺失

**问题描述**：
运行单元测试时出现错误：`Cannot read SQL script from class path resource [sql/clean.sql]; nested exception is java.io.FileNotFoundException: class path resource [sql/clean.sql] cannot be opened because it does not exist`

**解决方法**：
- 在`src/test/resources/sql`目录下创建`clean.sql`文件，用于测试执行后清理数据
- 通常包含清理测试表的SQL语句，如：`TRUNCATE TABLE table_name;`

**避免方法**：
- 单元测试时确保创建完整的SQL脚本：
  - `create_tables.sql`: 创建测试所需表结构
  - `clean.sql`: 清理测试数据（每个测试方法执行后会自动调用）
- 特别注意SQL脚本的位置和命名，Spring测试框架会按照固定规则查找这些文件

## 7. 测试注解冲突错误

**问题描述**：
运行单元测试时出现错误：`Configuration error: found multiple declarations of @BootstrapWith for test class: [@org.springframework.test.context.BootstrapWith(value=org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTestContextBootstrapper.class), @org.springframework.test.context.BootstrapWith(value=org.springframework.boot.test.context.SpringBootTestContextBootstrapper.class)]`

**解决方法**：
- 不要同时使用`@WebMvcTest`注解和继承`BaseDbAndRedisUnitTest`基类
- 如果需要数据库和Redis支持，只继承`BaseDbAndRedisUnitTest`基类即可
- 如果只需要Web层测试，使用`@WebMvcTest`注解，但不要继承`BaseDbAndRedisUnitTest`

**避免方法**：
- 明确测试类型：
  - 单元测试（需要DB+Redis）：继承`BaseDbAndRedisUnitTest`
  - Web层测试（不需要DB）：使用`@WebMvcTest`
  - 不要混合使用两种测试方式
- 参考项目中已有的测试类实现，选择合适的测试基类

## 8. 测试SQL初始化配置缺失

**问题描述**：
运行单元测试时出现错误：`Table "CONTRACT_TYPES" not found (this database is empty)`，表明测试表结构没有被正确创建

**解决方法**：
1. 在`application-unit-test.yaml`配置文件中添加SQL初始化配置：
   ```yaml
   spring:
     sql:
       init:
         schema-locations: classpath:sql/create_tables.sql
         mode: always
   ```

2. 修改`clean.sql`文件，使用容错的清理语句：
   ```sql
   -- 使用DELETE而不是TRUNCATE，避免表不存在的错误
   DELETE FROM contract_types WHERE 1=1;
   DELETE FROM contract_templates WHERE 1=1;
   DELETE FROM contracts WHERE 1=1;
   ```

**避免方法**：
- 确保测试配置文件包含SQL初始化配置
- 使用H2数据库兼容的SQL语法编写测试表结构
- 清理脚本使用容错的SQL语句，避免表不存在时的错误
- 测试前确保`create_tables.sql`和`clean.sql`都存在且语法正确

**问题描述**：
- 1. ID自增策略问题
- 问题：AppointmentDO的ID字段没有指定自增策略，导致插入后ID为null
**解决方法**：
- 解决：在@TableId注解中添加type = IdType.AUTO，确保数据库自动生成ID
**问题描述**：
- 2. 数据库兼容性问题
- 问题：H2数据库无法识别MySQL特有的bit类型默认值格式b'0'
**解决方法**：
- 解决：修改create_tables.sql中的bit字段默认值从b'0'改为FALSE或0
**问题描述**：
- 3. 数值范围问题
- 问题：随机生成的business_mode和priority_level字段值超出H2数据库字段范围
**解决方法**：
- 解决：修改表结构将priority_level改为SMALLINT类型，并在测试中设置固定值确保在合法范围内
**问题描述**：
- 4. 字段约束问题
- 问题：测试中试图调用不存在的setProducerEnterpriseName方法
**解决方法**：
- 解决：修改测试数据库表结构，将producer_enterprise_name字段设为可空，并在测试中移除对不存在方法的调用
**问题描述**：
- 5. BigDecimal精度问题
- 问题：assertPojoEquals比较BigDecimal字段时精度不匹配
**解决方法**：
- 解决：在测试中设置固定的BigDecimal值，确保精度一致

## 9. H2数据库TINYINT字段范围错误

**问题描述**：
运行单元测试时出现错误：`Numeric value out of range: "-1550466523" in column "SIGN_METHOD"`，表明随机生成的Integer值超出了H2数据库TINYINT字段的范围（-128到127）

**解决方法**：
1. 修改测试代码，为TINYINT类型字段设置合法范围的值：
   ```java
   // 错误的做法：使用randomPojo可能生成超范围值
   ContractDO dbContract = randomPojo(ContractDO.class);
   
   // 正确的做法：为TINYINT字段设置合法值
   ContractDO dbContract = randomPojo(ContractDO.class, o -> {
       o.setSignMethod(1); // 设置在TINYINT范围内的固定值
       o.setStatus(ContractStatusEnum.DRAFT.getStatus()); // 使用枚举值
   });
   ```

2. 或者修改数据库表结构，将TINYINT改为INT类型：
   ```sql
   -- 将TINYINT改为INT以支持更大范围的值
   sign_method INT DEFAULT 1 COMMENT '签署方式：1-线上签署 2-线下签署',
   ```

**避免方法**：
- 在使用randomPojo生成测试数据时，注意为数值范围受限的字段（如TINYINT、SMALLINT）设置合法值
- 优先使用枚举值或预定义常量，而不是随机值
- 确保测试SQL表结构与生产环境保持一致，特别是字段类型和约束
- 对于状态类字段，建议使用枚举类的值而不是随机数

## 总结

按照以上记录避免相似错误，可以提高代码生成的质量和效率。在使用代码生成工具或手动编写代码时，应当特别注意项目中已有的规范和实现方式，确保新增代码与现有代码保持一致。

### 测试配置最佳实践

1. **测试基类选择**：
   - 需要数据库+Redis：继承`BaseDbAndRedisUnitTest`
   - 只需要Web层：使用`@WebMvcTest`
   - 不要混合使用

2. **测试配置文件**：
   - 必须包含`yudao.info.base-package`配置
   - 必须包含SQL初始化配置
   - 使用H2内存数据库进行测试

3. **测试SQL脚本**：
   - `create_tables.sql`：使用H2兼容语法
   - `clean.sql`：使用容错的清理语句
   - 确保脚本位置正确：`src/test/resources/sql/` 