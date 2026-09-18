# 危险废物转移模块详细实施计划

**版本：** 1.0  
**制定日期：** 2024年1月  
**项目周期：** 8-10周  

## 1. 项目概览

### 1.1 项目目标
基于需求文档实现危险废物转移模块的完整功能，包括预约管理、订单管理、回收企业分配、价格管理、临时订单处理等核心业务流程。

### 1.2 技术栈
- **后端框架**：Spring Boot 2.7.18 + JDK 8
- **数据库**：MySQL 8.0
- **ORM框架**：MyBatis Plus
- **缓存**：Redis
- **消息队列**：RocketMQ/RabbitMQ
- **权限管理**：Spring Security + JWT
- **API文档**：Swagger/OpenAPI 3
- **构建工具**：Maven 3.6+

### 1.3 关键用户故事
| 优先级 | 用户故事ID | 描述 | 预估工作量 |
|--------|------------|------|------------|
| P0 | US-003 | 产废企业发起预约 | 5人天 |
| P0 | US-004 | 回收企业处理预约，生成订单 | 4人天 |
| P0 | US-028 | 系统自动分配回收企业 | 3人天 |
| P1 | US-017 | 收运员创建临时订单 | 3人天 |
| P1 | US-005 | 回收企业指派物流 | 2人天 |
| P1 | US-027 | 配置回收企业分配规则 | 4人天 |
| P1 | US-029 | 管理员手动变更回收企业 | 2人天 |
| P2 | US-037/038 | 价格策略和报价功能 | 6人天 |
| P2 | US-023 | 补单管理 | 3人天 |

## 2. 系统架构设计

### 2.1 模块结构
```
yudao-module-waste/
├── yudao-module-waste-api/           # API接口定义
│   ├── enums/                        # 枚举定义
│   ├── dto/                          # DTO对象
│   └── api/                          # Feign接口
├── yudao-module-waste-biz/           # 业务实现
│   ├── controller/                   # 控制器层
│   │   ├── admin/                    # 管理后台接口
│   │   └── app/                      # 移动端接口
│   ├── service/                      # 服务层
│   │   ├── appointment/              # 预约服务
│   │   ├── order/                    # 订单服务
│   │   ├── price/                    # 价格服务
│   │   └── assignment/               # 分配服务
│   ├── dal/                          # 数据访问层
│   │   ├── dataobject/               # DO对象
│   │   └── mysql/                    # Mapper接口
│   ├── convert/                      # 转换器
│   ├── enums/                        # 业务枚举
│   └── framework/                    # 框架配置
└── sql/                              # 数据库脚本
```

### 2.2 核心领域模型
```
预约管理域 (Appointment Domain)
├── WasteTransferAppointment          # 预约单聚合根
├── RecyclerAssignmentRule            # 回收企业分配规则
└── AppointmentStatusHistory          # 预约状态历史

订单管理域 (Order Domain)  
├── WasteTransferOrder                # 订单聚合根
├── OrderStatusHistory                # 订单状态历史
└── TemporaryOrder                    # 临时订单

价格管理域 (Price Domain)
├── PriceStrategy                     # 价格策略
├── QuotationRecord                   # 报价记录
└── CustomerPrice                     # 客户专属价格

分配管理域 (Assignment Domain)
├── AutoAssignmentEngine              # 自动分配引擎
└── ManualAssignmentRecord            # 手动分配记录
```

## 3. 开发阶段规划

### 阶段1：基础设施搭建 (第1周)

#### 3.1.1 项目结构初始化
- [x] 创建waste模块maven工程结构
- [x] 配置模块依赖关系
- [x] 启用waste模块到主项目

#### 3.1.2 数据库设计与建表
**任务清单：**
- [ ] 设计数据库表结构（基于数据库设计文档）
- [ ] 编写建表SQL脚本
- [ ] 创建索引和约束
- [ ] 初始化基础数据

**核心表：**
1. `waste_transfer_appointment` - 预约单表
2. `waste_transfer_order` - 订单主表  
3. `waste_recycler_assignment_rule` - 分配规则表
4. `waste_appointment_quotation` - 报价记录表
5. `waste_price_strategy` - 价格策略表

#### 3.1.3 基础枚举和常量定义
- [ ] 预约状态枚举 (WasteAppointmentStatusEnum)
- [ ] 订单状态枚举 (WasteOrderStatusEnum)
- [ ] 分配方式枚举 (AssignmentMethodEnum)
- [ ] 订单来源类型枚举 (OrderSourceTypeEnum)
- [ ] 错误码常量定义 (ErrorCodeConstants)

#### 3.1.4 菜单权限初始化
- [ ] 创建菜单结构SQL
- [ ] 定义权限编码
- [ ] 配置角色权限关系

### 阶段2：预约管理核心功能 (第2-3周)

#### 3.2.1 预约基础功能 (US-003)
**数据层实现：**
- [ ] WasteTransferAppointmentDO 数据对象
- [ ] WasteTransferAppointmentMapper 数据访问接口
- [ ] 基础CRUD方法实现

**服务层实现：**
- [ ] WasteTransferAppointmentService 接口定义
- [ ] WasteTransferAppointmentServiceImpl 实现类
- [ ] 预约创建逻辑
- [ ] 预约号生成算法
- [ ] 预约状态流转管理

**控制层实现：**
- [ ] WasteTransferAppointmentController
- [ ] 创建预约API (POST /waste/appointment/create)
- [ ] 查询预约API (GET /waste/appointment/get/{id})
- [ ] 分页查询API (GET /waste/appointment/page)
- [ ] 更新预约API (PUT /waste/appointment/update)
- [ ] 取消预约API (PUT /waste/appointment/cancel)

**VO对象设计：**
- [ ] WasteTransferAppointmentUpdateReqVO  
- [ ] WasteTransferAppointmentPageReqVO
- [ ] WasteTransferAppointmentRespVO

#### 3.2.2 回收企业处理预约 (US-004)
**业务功能：**
- [ ] 回收企业查看待处理预约列表
- [ ] 预约详情查看
- [ ] 接受预约功能
- [ ] 拒绝预约功能（含拒绝原因）
- [ ] 预约状态变更通知

**API接口：**
- [ ] 接受预约API (PUT /waste/appointment/accept)
- [ ] 拒绝预约API (PUT /waste/appointment/reject)
- [ ] 回收企业预约列表API

#### 3.2.3 自动分配回收企业 (US-028)
**核心组件：**
- [ ] AutoAssignmentEngine 自动分配引擎
- [ ] RecyclerAssignmentRule 分配规则实体
- [ ] 区域匹配算法
- [ ] 负载均衡算法
- [ ] 分配失败处理机制

**分配规则配置：**
- [ ] 基于区域的分配规则
- [ ] 基于废物类型的分配规则
- [ ] 企业产能考虑
- [ ] 默认分配策略

### 阶段3：订单管理功能 (第4-5周)

#### 3.3.1 订单生成和基础管理
**数据层：**
- [ ] WasteTransferOrderDO 订单数据对象
- [ ] WasteTransferOrderMapper 数据访问接口
- [ ] 订单状态历史表设计

**服务层：**
- [ ] WasteTransferOrderService 订单服务接口
- [ ] 从预约生成订单逻辑
- [ ] 订单号生成算法
- [ ] 订单状态流转管理
- [ ] 订单查询和筛选

**API接口：**
- [ ] 订单详情查询API
- [ ] 订单分页查询API  
- [ ] 订单状态更新API
- [ ] 订单取消API

#### 3.3.2 临时订单创建 (US-017)
**移动端接口：**
- [ ] 临时订单创建API (POST /waste/order/create-temp)
- [ ] GPS位置记录
- [ ] 现场照片上传
- [ ] 产废方信息录入

**业务逻辑：**
- [ ] 临时订单与预约订单区分
- [ ] 订单来源类型标记
- [ ] 收运员信息关联
- [ ] 临时订单审核流程

#### 3.3.3 物流指派功能 (US-005)
**功能实现：**
- [ ] 物流企业选择
- [ ] 运输任务创建
- [ ] 物流状态跟踪
- [ ] 物流费用计算

### 阶段4：回收企业分配管理 (第6周)

#### 3.4.1 分配规则配置 (US-027)
**管理功能：**
- [ ] 分配规则CRUD管理
- [ ] 规则优先级配置
- [ ] 区域映射管理
- [ ] 规则有效性验证

**API接口：**
- [ ] 分配规则管理API
- [ ] 规则测试API
- [ ] 规则历史查询API

#### 3.4.2 手动分配管理 (US-029)
**管理员功能：**
- [ ] 手动变更回收企业
- [ ] 分配记录日志
- [ ] 变更原因记录
- [ ] 相关方通知

### 阶段5：价格管理和报价功能 (第7周)

#### 3.5.1 价格策略配置
**数据模型：**
- [ ] 基础价格配置
- [ ] 区域价格差异
- [ ] 客户专属价格
- [ ] 价格有效期管理

#### 3.5.2 报价流程实现
**竞价模式：**
- [ ] 多方报价功能
- [ ] 报价时间限制
- [ ] 报价比较和选择

**非竞价模式：**
- [ ] 自动报价生成
- [ ] 价格策略应用
- [ ] 固定价格展示

### 阶段6：补单和高级功能 (第8周)

#### 3.6.1 补单管理 (US-023)
- [ ] 补单创建流程
- [ ] 原订单关联
- [ ] 费用调整逻辑
- [ ] 补单审核流程

#### 3.6.2 系统集成
- [ ] 与企业模块集成
- [ ] 与用户权限集成
- [ ] 与消息通知集成
- [ ] 与工作流集成（可选）

### 阶段7：测试和优化 (第9周)

#### 3.7.1 功能测试
- [ ] 单元测试覆盖率 >80%
- [ ] 集成测试用例
- [ ] API接口测试
- [ ] 业务流程测试

#### 3.7.2 性能优化
- [ ] 数据库查询优化
- [ ] 缓存策略优化
- [ ] 接口响应时间优化
- [ ] 并发处理优化

### 阶段8：部署和上线 (第10周)

#### 3.8.1 部署准备
- [ ] 生产环境数据库脚本
- [ ] 配置文件优化
- [ ] 日志配置
- [ ] 监控配置

#### 3.8.2 上线部署
- [ ] 灰度发布
- [ ] 数据迁移
- [ ] 功能验证
- [ ] 性能监控

## 4. 技术实现细节

### 4.1 数据库设计原则
- 使用统一的公共字段（id, tenant_id, creator, create_time, updater, update_time, deleted）
- 所有金额字段使用 DECIMAL(12,2) 确保精度
- 建立合适的索引以支持查询性能
- 使用软删除，不物理删除数据

### 4.2 状态机设计
```java
// 预约状态流转
PENDING -> WAITING_RECYCLER_CONFIRM -> ACCEPTED/REJECTED -> ORDER_GENERATED
        -> CANCELLED

// 订单状态流转  
PENDING -> ASSIGNED_LOGISTICS -> IN_TRANSIT -> DELIVERED -> WEIGHED -> COMPLETED
        -> CANCELLED/EXCEPTION
```

### 4.3 分配算法设计
```java
public interface RecyclerAssignmentStrategy {
    // 基于区域的分配策略
    RegionBasedAssignmentStrategy
    // 基于负载的分配策略  
    LoadBalancedAssignmentStrategy
    // 基于废物类型的分配策略
    WasteTypeBasedAssignmentStrategy
}
```

### 4.4 价格计算引擎
```java
public interface PriceCalculationEngine {
    // 价格策略链
    CustomerSpecificPrice -> RegionalPrice -> BasePrice
    // 动态价格调整
    SeasonalAdjustment, VolumeDiscount
}
```

## 5. 关键风险与应对

### 5.1 技术风险
| 风险项 | 风险等级 | 应对措施 |
|--------|----------|----------|
| 数据一致性问题 | 高 | 使用分布式事务，设计补偿机制 |
| 性能瓶颈 | 中 | 读写分离，合理使用缓存 |
| 并发冲突 | 中 | 乐观锁，分布式锁 |

### 5.2 业务风险
| 风险项 | 风险等级 | 应对措施 |
|--------|----------|----------|
| 需求变更 | 中 | 模块化设计，预留扩展点 |
| 集成复杂度 | 高 | 渐进式集成，充分测试 |
| 数据迁移 | 中 | 制定详细迁移方案 |

## 6. 质量保证

### 6.1 代码质量
- 遵循项目编码规范
- 代码评审机制
- 静态代码分析
- 单元测试覆盖率 >80%

### 6.2 接口质量
- API文档完整性
- 参数校验完备性
- 错误处理规范性
- 响应格式统一性

### 6.3 数据质量
- 数据校验规则
- 数据备份策略
- 数据一致性检查
- 业务规则验证

## 7. 交付物清单

### 7.1 代码交付物
- [ ] 完整的Java源代码
- [ ] SQL数据库脚本
- [ ] Maven配置文件
- [ ] 配置文件模板

### 7.2 文档交付物
- [ ] API接口文档
- [ ] 数据库设计文档
- [ ] 部署指南
- [ ] 用户操作手册
- [ ] 运维手册

### 7.3 测试交付物
- [ ] 单元测试用例
- [ ] 集成测试用例
- [ ] 性能测试报告
- [ ] 安全测试报告

## 8. 项目里程碑

| 里程碑 | 时间节点 | 关键交付物 | 验收标准 |
|--------|----------|------------|----------|
| M1 | 第1周末 | 基础设施搭建完成 | 数据库建表完成，模块可启动 |
| M2 | 第3周末 | 预约管理功能完成 | 预约CRUD功能正常，自动分配可用 |
| M3 | 第5周末 | 订单管理功能完成 | 订单流转正常，临时订单可创建 |
| M4 | 第7周末 | 价格管理功能完成 | 报价流程通畅，价格策略生效 |
| M5 | 第9周末 | 测试完成 | 功能测试通过，性能达标 |
| M6 | 第10周末 | 上线部署完成 | 生产环境运行稳定 |

## 9. 总结

本实施计划基于详细的需求分析，采用分阶段、迭代的开发方式，确保关键功能优先交付，同时保证代码质量和系统稳定性。通过合理的架构设计和技术选型，能够支撑业务的长期发展和扩展需求。 