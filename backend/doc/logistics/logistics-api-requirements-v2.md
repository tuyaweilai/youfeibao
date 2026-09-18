# 物流运输 API 需求清单（复审改进版 V2.1）

## 1. 引言

本文档是对"危废再生资源全流程追溯SaaS平台"中物流运输模块API接口需求的复审改进版本。基于业务流程分析、数据库设计和开发提示词，对原有API清单进行了全面梳理、补充和优化。

## 2. API 设计原则和规范

### 2.1 命名约定
-   **基路径：** `/api/v1/logistics`
-   **认证：** 所有接口均需JWT Token认证
-   **请求/响应格式：** JSON，统一使用驼峰命名
-   **错误处理：** 标准HTTP状态码 + 统一错误响应格式
-   **分页：** 统一使用 `pageNo`, `pageSize` 参数
-   **排序：** 统一使用 `sortField`, `sortOrder` 参数

### 2.2 响应格式规范
```json
{
  "code": 200,
  "data": {},
  "msg": "操作成功",
  "timestamp": "2024-01-01T10:00:00Z"
}
```

### 2.3 错误响应格式
```json
{
  "code": 400,
  "data": null,
  "msg": "参数校验失败",
  "timestamp": "2024-01-01T10:00:00Z",
  "details": ["字段xxx不能为空"]
}
```

## 3. API 清单（按业务模块分组）

### 3.1 运输任务管理模块

#### 3.1.1 任务创建与分配（回收企业/平台管理员）

| 序号 | API 端点 | HTTP 方法 | 描述 | 权限角色 |
|------|----------|-----------|------|----------|
| 1 | `/transport-tasks` | `POST` | 创建运输任务（危废模块调用） | SYSTEM |
| 2 | `/transport-tasks/{taskId}/assign` | `PUT` | 分配任务给物流公司/车辆/司机 | RECYCLER_ADMIN |
| 3 | `/transport-tasks/{taskId}/reassign` | `PUT` | 重新分配任务 | RECYCLER_ADMIN |
| 4 | `/transport-tasks/{taskId}/cancel` | `PUT` | 取消运输任务 | RECYCLER_ADMIN |
| 5 | `/transport-tasks` | `GET` | 查询运输任务列表 | RECYCLER_ADMIN, LOGISTICS_DISPATCHER |
| 6 | `/transport-tasks/{taskId}` | `GET` | 查询任务详情 | ALL_ROLES |

**详细接口设计：**

**1. 创建运输任务**
```
POST /api/v1/logistics/transport-tasks
Content-Type: application/json
Authorization: Bearer {token}

请求体：
{
  "orderId": 67890,
  "orderNo": "WO202401010001",
  "wasteCode": "HW08",
  "wasteName": "废矿物油",
  "estimatedQuantity": 2.5,
  "quantityUnit": "吨",
  "pickupInfo": {
    "enterpriseId": 12345,
    "enterpriseName": "XX化工厂",
    "address": "上海市浦东新区XX路XX号",
    "contactName": "张三",
    "contactPhone": "13800138000",
    "expectedTime": "2024-01-01T09:00:00"
  },
  "deliveryInfo": {
    "enterpriseId": 54321,
    "enterpriseName": "XX回收公司",
    "address": "上海市嘉定区XX路XX号",
    "contactName": "李四",
    "contactPhone": "13900139000",
    "expectedTime": "2024-01-01T14:00:00"
  },
  "specialRequirements": "需要防泄漏包装",
  "isTemporary": false
}

响应：
{
  "code": 200,
  "data": {
    "taskId": 12345,
    "taskNo": "TT202401010001",
    "status": "PENDING_ASSIGN"
  },
  "msg": "运输任务创建成功"
}
```

**2. 分配运输任务**
```
PUT /api/v1/logistics/transport-tasks/{taskId}/assign

请求体：
{
  "logisticsCompanyId": 301,
  "vehicleId": 101,
  "driverId": 201,
  "plannedPickupTime": "2024-01-01T08:30:00",
  "plannedDeliveryTime": "2024-01-01T15:00:00",
  "assignReason": "就近分配"
}
```

#### 3.1.2 司机任务操作（物流司机）

| 序号 | API 端点 | HTTP 方法 | 描述 | 权限角色 |
|------|----------|-----------|------|----------|
| 7 | `/driver/tasks` | `GET` | 司机查询分配给自己的任务列表 | DRIVER |
| 8 | `/driver/tasks/{taskId}` | `GET` | 司机查询任务详情 | DRIVER |
| 9 | `/driver/tasks/{taskId}/accept` | `POST` | 司机接受任务 | DRIVER |
| 10 | `/driver/tasks/{taskId}/reject` | `POST` | 司机拒绝任务 | DRIVER |
| 11 | `/driver/tasks/{taskId}/start` | `POST` | 司机开始执行任务 | DRIVER |

### 3.2 运输节点跟踪模块

#### 3.2.1 节点上报（物流司机）

| 序号 | API 端点 | HTTP 方法 | 描述 | 权限角色 |
|------|----------|-----------|------|----------|
| 12 | `/transport-tasks/{taskId}/nodes` | `POST` | 上报运输节点 | DRIVER |
| 13 | `/transport-tasks/{taskId}/nodes` | `GET` | 查询运输轨迹 | ALL_ROLES |
| 14 | `/transport-tasks/{taskId}/location` | `POST` | 上报实时位置（在途） | DRIVER |
| 15 | `/transport-tasks/{taskId}/exception` | `POST` | 上报异常情况 | DRIVER |

**详细接口设计：**

**12. 上报运输节点**
```
POST /api/v1/logistics/transport-tasks/{taskId}/nodes

请求体：
{
  "nodeType": "LOAD_COMPLETED",
  "nodeTime": "2024-01-01T09:30:00",
  "location": {
    "address": "上海市浦东新区XX路XX号",
    "latitude": 31.2304,
    "longitude": 121.4737
  },
  "actualQuantity": 2.6,
  "photos": [
    "https://oss.example.com/photos/load1.jpg",
    "https://oss.example.com/photos/load2.jpg"
  ],
  "remark": "已完成装货，共10桶废矿物油",
  "additionalData": {
    "containerCount": 10,
    "sealNumber": "SEAL001"
  }
}

响应：
{
  "code": 200,
  "data": {
    "nodeId": 98765,
    "taskStatus": "IN_TRANSIT",
    "nextNodeType": "ARRIVE_DELIVERY"
  },
  "msg": "节点上报成功"
}
```

#### 3.2.2 轨迹查询（多角色）

| 序号 | API 端点 | HTTP 方法 | 描述 | 权限角色 |
|------|----------|-----------|------|----------|
| 16 | `/transport-tasks/{taskId}/track` | `GET` | 查询完整运输轨迹 | ALL_ROLES |
| 17 | `/vehicles/{vehicleId}/track` | `GET` | 查询车辆实时轨迹 | RECYCLER_ADMIN, LOGISTICS_DISPATCHER |
| 18 | `/transport-tasks/track/batch` | `POST` | 批量查询任务轨迹 | RECYCLER_ADMIN |

### 3.3 车辆过磅管理模块

#### 3.3.1 过磅操作（过磅员）

| 序号 | API 端点 | HTTP 方法 | 描述 | 权限角色 |
|------|----------|-----------|------|----------|
| 19 | `/weighing/records` | `POST` | 录入过磅数据 | WEIGHING_OPERATOR |
| 20 | `/weighing/records/{recordId}` | `GET` | 查询过磅记录详情 | ALL_ROLES |
| 21 | `/weighing/records` | `GET` | 查询过磅记录列表 | RECYCLER_ADMIN, WEIGHING_OPERATOR |
| 22 | `/weighing/records/{recordId}/correct` | `POST` | 过磅数据修正 | WEIGHING_OPERATOR |
| 23 | `/weighing/records/{recordId}/push` | `POST` | 手动推送过磅数据 | WEIGHING_OPERATOR |

**详细接口设计：**

**19. 录入过磅数据**
```
POST /api/v1/logistics/weighing/records

请求体：
{
  "weighingBatchNo": "WB202401010001",
  "vehicleId": 101,
  "vehicleNo": "沪A12345",
  "driverId": 201,
  "driverName": "王五",
  "weighingTime": "2024-01-01T10:00:00",
  "grossWeight": 15.5,
  "tareWeight": 10.6,
  "netWeight": 4.9,
  "weighingLocation": "XX回收站过磅点",
  "relatedOrderIds": [67890, 67891, 67892],
  "weighingPhotos": [
    "https://oss.example.com/weighing/slip1.jpg"
  ],
  "operatorId": 301,
  "operatorName": "过磅员张三",
  "remark": "正常过磅"
}

响应：
{
  "code": 200,
  "data": {
    "recordId": 55555,
    "weighingNo": "WR202401010001",
    "pushStatus": "PENDING",
    "anomalyDetected": false
  },
  "msg": "过磅数据录入成功"
}
```

#### 3.3.2 异常检测与处理

| 序号 | API 端点 | HTTP 方法 | 描述 | 权限角色 |
|------|----------|-----------|------|----------|
| 24 | `/weighing/anomalies` | `GET` | 查询过磅异常列表 | RECYCLER_ADMIN |
| 25 | `/weighing/anomalies/{anomalyId}` | `GET` | 查询异常详情 | RECYCLER_ADMIN |
| 26 | `/weighing/anomalies/{anomalyId}/handle` | `POST` | 处理过磅异常 | RECYCLER_ADMIN |

### 3.4 现金代付管理模块

#### 3.4.1 代付记录（物流司机）

| 序号 | API 端点 | HTTP 方法 | 描述 | 权限角色 |
|------|----------|-----------|------|----------|
| 27 | `/cash-advances` | `POST` | 记录现金代付 | DRIVER |
| 28 | `/cash-advances` | `GET` | 查询代付记录列表 | DRIVER, RECYCLER_FINANCE |
| 29 | `/cash-advances/{recordId}` | `GET` | 查询代付记录详情 | DRIVER, RECYCLER_FINANCE |
| 30 | `/cash-advances/{recordId}/voucher` | `POST` | 上传支付凭证 | DRIVER |

**详细接口设计：**

**27. 记录现金代付**
```
POST /api/v1/logistics/cash-advances

请求体：
{
  "taskId": 12345,
  "orderId": 67890,
  "paymentAmount": 800.00,
  "paymentTime": "2024-01-01T09:00:00",
  "paymentLocation": "上海市浦东新区XX路XX号",
  "payeeName": "张三",
  "payeePhone": "13800138000",
  "paymentMethod": "CASH",
  "paymentPhotos": [
    "https://oss.example.com/payment/photo1.jpg"
  ],
  "receiptPhotos": [
    "https://oss.example.com/receipt/photo1.jpg"
  ],
  "remark": "现场代付废料款"
}
```

#### 3.4.2 对账管理（回收企业财务）

| 序号 | API 端点 | HTTP 方法 | 描述 | 权限角色 |
|------|----------|-----------|------|----------|
| 31 | `/cash-advances/{recordId}/confirm` | `POST` | 确认代付记录 | RECYCLER_FINANCE |
| 32 | `/cash-advances/reconcile` | `POST` | 批量对账 | RECYCLER_FINANCE |
| 33 | `/cash-advances/reconcile/report` | `GET` | 生成对账报告 | RECYCLER_FINANCE |

### 3.5 临时订单管理模块（扫街回收）

#### 3.5.1 临时订单创建（物流司机）

| 序号 | API 端点 | HTTP 方法 | 描述 | 权限角色 |
|------|----------|-----------|------|----------|
| 34 | `/temporary-orders` | `POST` | 创建临时回收订单 | DRIVER |
| 35 | `/temporary-orders/{orderId}/payment-voucher` | `POST` | 生成支付凭证 | DRIVER |
| 36 | `/temporary-orders` | `GET` | 查询临时订单列表 | DRIVER, RECYCLER_ADMIN |

**详细接口设计：**

**34. 创建临时回收订单**
```
POST /api/v1/logistics/temporary-orders

请求体：
{
  "wasteType": "HW08",
  "wasteName": "废矿物油",
  "estimatedQuantity": 2.5,
  "quantityUnit": "吨",
  "pickupLocation": {
    "address": "上海市浦东新区XX路XX号",
    "latitude": 31.2304,
    "longitude": 121.4737
  },
  "producerInfo": {
    "name": "张三",
    "phone": "13800138000",
    "idCard": "310101199001010001"
  },
  "discoveryPhotos": [
    "https://oss.example.com/discovery/photo1.jpg"
  ],
  "remark": "个人车主，废机油约10桶",
  "estimatedValue": 400.00
}

响应：
{
  "code": 200,
  "data": {
    "orderId": 67890,
    "orderNo": "WO202401010001",
    "taskId": 12345,
    "taskNo": "TT202401010001",
    "paymentVoucherUrl": "https://pay.example.com/voucher/abc123"
  },
  "msg": "临时订单创建成功"
}
```

### 3.6 在途量监控模块

#### 3.6.1 在途量查询（回收企业管理员）

| 序号 | API 端点 | HTTP 方法 | 描述 | 权限角色 |
|------|----------|-----------|------|----------|
| 37 | `/in-transit/vehicles/{vehicleId}` | `GET` | 查询车辆在途量 | RECYCLER_ADMIN |
| 38 | `/in-transit/summary` | `GET` | 在途量概览统计 | RECYCLER_ADMIN |
| 39 | `/in-transit/alerts` | `GET` | 在途量预警列表 | RECYCLER_ADMIN |
| 40 | `/in-transit/vehicles` | `GET` | 在途车辆列表 | RECYCLER_ADMIN |

#### 3.6.2 在途量核销（系统内部）

| 序号 | API 端点 | HTTP 方法 | 描述 | 权限角色 |
|------|----------|-----------|------|----------|
| 41 | `/in-transit/{taskId}/write-off` | `POST` | 核销在途量 | SYSTEM |
| 42 | `/in-transit/batch-write-off` | `POST` | 批量核销在途量 | SYSTEM |

### 3.7 基础数据管理模块

#### 3.7.1 物流公司管理（平台管理员）

| 序号 | API 端点 | HTTP 方法 | 描述 | 权限角色 |
|------|----------|-----------|------|----------|
| 43 | `/companies` | `POST` | 新增物流公司 | PLATFORM_ADMIN |
| 44 | `/companies/{companyId}` | `PUT` | 修改物流公司信息 | PLATFORM_ADMIN |
| 45 | `/companies/{companyId}` | `GET` | 查询物流公司详情 | ALL_ROLES |
| 46 | `/companies` | `GET` | 查询物流公司列表 | ALL_ROLES |
| 47 | `/companies/{companyId}/status` | `PUT` | 修改公司状态 | PLATFORM_ADMIN |

#### 3.7.2 车辆管理（物流公司管理员）

| 序号 | API 端点 | HTTP 方法 | 描述 | 权限角色 |
|------|----------|-----------|------|----------|
| 48 | `/vehicles` | `POST` | 新增车辆 | LOGISTICS_ADMIN |
| 49 | `/vehicles/{vehicleId}` | `PUT` | 修改车辆信息 | LOGISTICS_ADMIN |
| 50 | `/vehicles/{vehicleId}` | `GET` | 查询车辆详情 | LOGISTICS_ADMIN, LOGISTICS_DISPATCHER |
| 51 | `/vehicles` | `GET` | 查询车辆列表 | LOGISTICS_ADMIN, LOGISTICS_DISPATCHER |
| 52 | `/vehicles/{vehicleId}/status` | `PUT` | 修改车辆状态 | LOGISTICS_ADMIN |

#### 3.7.3 司机管理（物流公司管理员）

| 序号 | API 端点 | HTTP 方法 | 描述 | 权限角色 |
|------|----------|-----------|------|----------|
| 53 | `/drivers` | `POST` | 新增司机 | LOGISTICS_ADMIN |
| 54 | `/drivers/{driverId}` | `PUT` | 修改司机信息 | LOGISTICS_ADMIN |
| 55 | `/drivers/{driverId}` | `GET` | 查询司机详情 | LOGISTICS_ADMIN, LOGISTICS_DISPATCHER |
| 56 | `/drivers` | `GET` | 查询司机列表 | LOGISTICS_ADMIN, LOGISTICS_DISPATCHER |
| 57 | `/drivers/{driverId}/status` | `PUT` | 修改司机状态 | LOGISTICS_ADMIN |
| 58 | `/drivers/by-enterprise` | `GET` | 查询企业下的司机列表 | LOGISTICS_ADMIN, LOGISTICS_DISPATCHER |

### 3.8 统计分析模块

#### 3.8.1 运输统计（多角色）

| 序号 | API 端点 | HTTP 方法 | 描述 | 权限角色 |
|------|----------|-----------|------|----------|
| 58 | `/statistics/transport/summary` | `GET` | 运输任务统计概览 | RECYCLER_ADMIN, LOGISTICS_ADMIN |
| 59 | `/statistics/transport/efficiency` | `GET` | 运输效率分析 | RECYCLER_ADMIN, LOGISTICS_ADMIN |
| 60 | `/statistics/transport/loss-rate` | `GET` | 运输损耗率统计 | RECYCLER_ADMIN |
| 61 | `/statistics/vehicle/count` | `GET` | 车辆数量统计 | LOGISTICS_ADMIN |
| 62 | `/statistics/vehicle/utilization` | `GET` | 车辆利用率统计 | LOGISTICS_ADMIN |

## 4. 新增和改进的API接口

### 4.1 新增接口

1. **批量操作接口**：
   - `/transport-tasks/batch-assign` - 批量分配任务
   - `/weighing/records/batch-push` - 批量推送过磅数据
   - `/cash-advances/batch-confirm` - 批量确认代付记录

2. **实时通知接口**：
   - `/notifications/subscribe` - 订阅实时通知
   - `/notifications/unsubscribe` - 取消订阅通知

3. **数据导出接口**：
   - `/export/transport-tasks` - 导出运输任务数据
   - `/export/weighing-records` - 导出过磅记录
   - `/export/cash-advances` - 导出代付记录

4. **系统配置接口**：
   - `/config/anomaly-thresholds` - 异常检测阈值配置
   - `/config/notification-rules` - 通知规则配置

### 4.2 改进的接口

1. **增强查询条件**：所有列表查询接口都支持更丰富的筛选条件
2. **统一分页格式**：所有分页接口使用统一的分页参数和响应格式
3. **增加批量操作**：支持批量处理提高操作效率
4. **完善权限控制**：明确每个接口的权限角色要求
5. **增强错误处理**：提供更详细的错误信息和处理建议

## 5. 数据传输对象（DTO）规范

### 5.1 请求DTO命名规范
- 创建：`Create{Entity}ReqVO`
- 更新：`Update{Entity}ReqVO`
- 查询：`{Entity}PageReqVO`

### 5.2 响应DTO命名规范
- 详情：`{Entity}RespVO`
- 列表：`{Entity}PageRespVO`
- 简要：`{Entity}SimpleRespVO`

## 6. 接口版本管理

### 6.1 版本策略
- 使用URL路径版本控制：`/api/v1/logistics`
- 向后兼容原则：新版本保持对旧版本的兼容
- 废弃通知：提前通知接口废弃计划

### 6.2 版本升级路径
- v1.0：基础功能实现
- v1.1：增加批量操作和统计分析
- v2.0：支持第三方物流平台对接

## 7. 安全性要求

### 7.1 认证授权
- JWT Token认证
- 基于角色的权限控制（RBAC）
- API访问频率限制

### 7.2 数据安全
- 敏感数据加密传输
- 请求参数校验
- SQL注入防护
- XSS攻击防护

## 8. 性能要求

### 8.1 响应时间
- 查询接口：< 500ms
- 创建/更新接口：< 1s
- 批量操作接口：< 3s
- 统计分析接口：< 2s

### 8.2 并发处理
- 支持1000+并发请求
- 数据库连接池优化
- 缓存策略应用

## 9. 监控和日志

### 9.1 接口监控
- 响应时间监控
- 错误率监控
- 调用量统计
- 性能瓶颈分析

### 9.2 操作日志
- 关键操作审计日志
- 数据变更记录
- 异常操作告警
- 日志归档策略

## 10. 测试要求

### 10.1 单元测试
- 业务逻辑测试覆盖率 > 80%
- 边界条件测试
- 异常情况测试

### 10.2 集成测试
- API接口测试
- 数据库事务测试
- 第三方服务集成测试

### 10.3 性能测试
- 压力测试
- 负载测试
- 稳定性测试 