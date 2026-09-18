# EnterpriseInfoService 单元测试方案

## 一、测试目标
- 验证 `EnterpriseInfoService` 及其实现类（如 `EnterpriseInfoServiceImpl`）的核心业务逻辑正确性。
- 保证企业信息相关的增删改查、审核等功能在不同场景下均能正确响应。
- 提高代码质量和重构安全性。

## 二、测试范围
- 企业信息创建（createEnterpriseInfo）
- 企业信息更新（updateEnterpriseInfo）
- 企业信息删除（deleteEnterpriseInfo）
- 企业信息查询（getEnterpriseInfo, getEnterpriseInfoList, pageEnterpriseInfo, getEnterpriseInfoByCreditCode, getEnterpriseInfoSimpleList）
- 企业入驻审核（auditEnterpriseInfo）
- 业务异常场景（如主键冲突、唯一约束、状态非法等）

## 三、测试方法
- 使用 JUnit5 作为测试框架
- 使用 Mockito 对 Mapper、外部依赖进行 Mock
- 每个方法设计正向和逆向（异常）用例
- 测试用例互相独立，数据隔离

## 四、用例设计
### 1. 创建企业信息
- 正常创建，断言返回主键ID，Mapper被调用
- 重复信用代码/企业名，断言抛出业务异常

### 2. 更新企业信息
- 正常更新，断言无异常，Mapper被调用
- 更新不存在企业，断言抛出异常
- 更新为已存在信用代码/企业名，断言抛出异常

### 3. 删除企业信息
- 正常删除，断言Mapper被调用
- 删除不存在企业，断言抛出异常

### 4. 查询企业信息
- 查询单个企业，断言返回正确对象
- 查询列表/分页，断言返回集合、分页信息
- 查询不存在企业，断言返回null或空集合

### 5. 企业入驻审核
- 正常审核通过/拒绝，断言状态变更
- 审核非法状态，断言抛出异常

### 6. 业务异常
- 非法参数、空指针、唯一约束冲突等

## 五、Mock依赖
- Mock `EnterpriseInfoMapper`，控制 insert/update/delete/select 行为
- 如有依赖其他 Service/Mapper，也需 Mock

## 六、断言点
- 方法返回值（如ID、对象、集合）
- 依赖方法调用次数（verify）
- 抛出异常类型与消息
- 状态变更（如审核状态）

## 七、命名规范
- 测试类名：被测类名+Test，如 `EnterpriseInfoServiceImplTest`
- 测试方法名：`方法名_条件_预期结果`，如 `createEnterpriseInfo_参数正确_创建成功`

## 八、覆盖率要求
- 业务主流程、分支、异常分支均需覆盖
- 推荐核心业务逻辑测试覆盖率不低于80%

## 九、最佳实践
- 测试用例应简洁明了，关注业务逻辑
- 避免真实数据库操作，全部Mock
- 测试数据独立，避免相互依赖
- 及时补充新功能、新分支的测试用例

## 十、示例代码片段
```java
@Test
void createEnterpriseInfo_参数正确_创建成功() {
    EnterpriseInfoCreateReqVO reqVO = new EnterpriseInfoCreateReqVO();
    reqVO.setName("测试企业");
    reqVO.setCreditCode("123456789012345678");
    // ... 其他字段
    when(enterpriseInfoMapper.insert(any(EnterpriseInfoDO.class))).thenReturn(1);
    Long id = enterpriseInfoService.createEnterpriseInfo(reqVO);
    assertNotNull(id);
    verify(enterpriseInfoMapper, times(1)).insert(any(EnterpriseInfoDO.class));
}
```

---

如需详细用例代码或自动生成测试类，请补充具体需求。 