# 合同管理模块 - API需求清单 (API Requirements List V2.1)

## 1. 引言

本文档是对"危废再生资源全流程追溯SaaS平台"中合同管理模块API接口需求的详细清单。基于业务流程分析、数据库设计和用户故事需求，提供完整的API接口规范。

## 2. API 设计原则和规范

### 2.1 命名约定
-   **基路径：** `/api/v1/contracts`
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

### 3.1 合同类型管理模块

#### 3.1.1 合同类型操作（平台管理员）

| 序号 | API 端点 | HTTP 方法 | 描述 | 权限角色 |
|------|----------|-----------|------|----------|
| 1 | `/types` | `GET` | 获取合同类型列表 | ALL_ROLES |
| 2 | `/types` | `POST` | 创建新的合同类型 | PLATFORM_ADMIN |
| 3 | `/types/{typeId}` | `GET` | 获取合同类型详情 | ALL_ROLES |
| 4 | `/types/{typeId}` | `PUT` | 更新合同类型 | PLATFORM_ADMIN |
| 5 | `/types/{typeId}/status` | `PUT` | 修改类型状态 | PLATFORM_ADMIN |
| 6 | `/types/{typeId}` | `DELETE` | 删除合同类型 | PLATFORM_ADMIN |

**详细接口设计：**

**1. 获取合同类型列表**
```
GET /api/v1/contracts/types
Content-Type: application/json
Authorization: Bearer {token}

查询参数：
?isActive=true&isSystemDefined=true&pageNo=1&pageSize=10

响应：
{
  "code": 200,
  "data": {
    "total": 5,
    "items": [
      {
        "id": 1,
        "typeName": "危险废物转移合同",
        "typeCode": "HAZARDOUS_WASTE_TRANSFER",
        "typeDescription": "用于危险废物转移的合同类型",
        "isSystemDefined": true,
        "isActive": true,
        "sortOrder": 1
      }
    ]
  },
  "msg": "查询成功"
}
```

**2. 创建合同类型**
```
POST /api/v1/contracts/types

请求体：
{
  "typeName": "物流服务合同",
  "typeCode": "LOGISTICS_SERVICE",
  "typeDescription": "物流运输服务合同",
  "isSystemDefined": false,
  "sortOrder": 10
}

响应：
{
  "code": 200,
  "data": {
    "id": 6,
    "typeName": "物流服务合同",
    "typeCode": "LOGISTICS_SERVICE"
  },
  "msg": "创建成功"
}
```

### 3.2 合同模板管理模块

#### 3.2.1 模板操作（平台管理员）

| 序号 | API 端点 | HTTP 方法 | 描述 | 权限角色 |
|------|----------|-----------|------|----------|
| 7 | `/templates` | `GET` | 获取合同模板列表 | ALL_ROLES |
| 8 | `/templates` | `POST` | 创建新合同模板 | PLATFORM_ADMIN |
| 9 | `/templates/{templateId}` | `GET` | 获取模板详情 | ALL_ROLES |
| 10 | `/templates/{templateId}` | `PUT` | 更新合同模板 | PLATFORM_ADMIN |
| 11 | `/templates/{templateId}/status` | `PUT` | 修改模板状态 | PLATFORM_ADMIN |
| 12 | `/templates/{templateId}` | `DELETE` | 删除合同模板 | PLATFORM_ADMIN |
| 13 | `/templates/{templateId}/copy` | `POST` | 复制模板 | PLATFORM_ADMIN |

### 3.3 合同管理模块

#### 3.3.1 合同创建与基础操作（企业用户）

| 序号 | API 端点 | HTTP 方法 | 描述 | 权限角色 |
|------|----------|-----------|------|----------|
| 14 | `/` | `POST` | 创建新合同 | ENTERPRISE_USER |
| 15 | `/` | `GET` | 获取合同列表 | ENTERPRISE_USER |
| 16 | `/{contractIdOrUUID}` | `GET` | 获取合同详情 | ENTERPRISE_USER |
| 17 | `/{contractIdOrUUID}` | `PUT` | 更新合同信息 | ENTERPRISE_USER |
| 18 | `/{contractIdOrUUID}` | `DELETE` | 删除合同 | ENTERPRISE_USER |
| 19 | `/{contractIdOrUUID}/copy` | `POST` | 复制合同 | ENTERPRISE_USER |

**详细接口设计：**

**14. 创建新合同**
```
POST /api/v1/contracts/

请求体：
{
  "contractName": "XX化工厂废矿物油转移合同",
  "contractTypeId": 1,
  "templateId": 5,
  "primaryOwnerEnterpriseId": 12345,
  "totalAmount": 50000.00,
  "currency": "CNY",
  "priorityLevel": 1,
  "isElectronic": true,
  "parties": [
    {
      "enterpriseId": 12345,
      "enterpriseName": "XX化工厂",
      "roleInContract": "产废方",
      "signatoryName": "张三",
      "signatoryEmail": "zhangsan@example.com",
      "signatoryPhone": "13800138000",
      "orderInSignFlow": 1,
      "isRequired": true
    },
    {
      "enterpriseId": 54321,
      "enterpriseName": "XX回收公司",
      "roleInContract": "回收方",
      "signatoryName": "李四",
      "signatoryEmail": "lisi@example.com",
      "signatoryPhone": "13900139000",
      "orderInSignFlow": 2,
      "isRequired": true
    }
  ],
  "versionInfo": {
    "effectiveDate": "2024-01-01",
    "expiryDate": "2024-12-31",
    "contractContent": "合同正文内容...",
    "autoRemindDays": "30,15,7"
  },
  "remark": "年度框架合同"
}

响应：
{
  "code": 200,
  "data": {
    "contractId": 12345,
    "contractNo": "CT202401010001",
    "contractUUID": "550e8400-e29b-41d4-a716-446655440000",
    "status": 0,
    "currentVersionId": 67890
  },
  "msg": "合同创建成功"
}
```

#### 3.3.2 合同审批流程（审批人）

| 序号 | API 端点 | HTTP 方法 | 描述 | 权限角色 |
|------|----------|-----------|------|----------|
| 20 | `/{contractIdOrUUID}/submit-for-approval` | `POST` | 提交审批 | ENTERPRISE_USER |
| 21 | `/{contractIdOrUUID}/approve` | `POST` | 审批通过 | ENTERPRISE_ADMIN |
| 22 | `/{contractIdOrUUID}/reject` | `POST` | 审批拒绝 | ENTERPRISE_ADMIN |
| 23 | `/pending-approval` | `GET` | 获取待审批合同列表 | ENTERPRISE_ADMIN |

#### 3.3.3 合同状态管理

| 序号 | API 端点 | HTTP 方法 | 描述 | 权限角色 |
|------|----------|-----------|------|----------|
| 24 | `/{contractIdOrUUID}/activate` | `POST` | 激活合同 | ENTERPRISE_ADMIN |
| 25 | `/{contractIdOrUUID}/terminate` | `POST` | 终止合同 | ENTERPRISE_ADMIN |
| 26 | `/{contractIdOrUUID}/archive` | `POST` | 归档合同 | ENTERPRISE_ADMIN |
| 27 | `/{contractIdOrUUID}/void` | `POST` | 作废合同 | ENTERPRISE_ADMIN |

### 3.4 合同版本管理模块

#### 3.4.1 版本操作

| 序号 | API 端点 | HTTP 方法 | 描述 | 权限角色 |
|------|----------|-----------|------|----------|
| 28 | `/{contractIdOrUUID}/versions` | `GET` | 获取版本列表 | ENTERPRISE_USER |
| 29 | `/{contractIdOrUUID}/versions` | `POST` | 创建新版本 | ENTERPRISE_USER |
| 30 | `/{contractIdOrUUID}/versions/{versionId}` | `GET` | 获取版本详情 | ENTERPRISE_USER |
| 31 | `/{contractIdOrUUID}/versions/{versionId}` | `PUT` | 更新版本信息 | ENTERPRISE_USER |
| 32 | `/{contractIdOrUUID}/versions/{versionId}/activate` | `POST` | 激活版本 | ENTERPRISE_ADMIN |
| 33 | `/{contractIdOrUUID}/versions/compare` | `POST` | 版本对比 | ENTERPRISE_USER |

### 3.5 合同签署管理模块

#### 3.5.1 电子签署流程

| 序号 | API 端点 | HTTP 方法 | 描述 | 权限角色 |
|------|----------|-----------|------|----------|
| 34 | `/{contractIdOrUUID}/versions/{versionId}/initiate-signing` | `POST` | 发起电子签署 | ENTERPRISE_USER |
| 35 | `/{contractIdOrUUID}/versions/{versionId}/signing-status` | `GET` | 查询签署状态 | ENTERPRISE_USER |
| 36 | `/{contractIdOrUUID}/versions/{versionId}/sign` | `POST` | 执行签署 | SIGNATORY |
| 37 | `/{contractIdOrUUID}/versions/{versionId}/decline` | `POST` | 拒绝签署 | SIGNATORY |
| 38 | `/esignature/webhook` | `POST` | 电子签章回调 | SYSTEM |
| 39 | `/my-pending-signatures` | `GET` | 我的待签署列表 | SIGNATORY |

**详细接口设计：**

**34. 发起电子签署**
```
POST /api/v1/contracts/{contractIdOrUUID}/versions/{versionId}/initiate-signing

请求体：
{
  "esignProvider": "e签宝",
  "signingMode": "sequential",
  "signatories": [
    {
      "partyId": 1,
      "email": "zhangsan@example.com",
      "name": "张三",
      "phone": "13800138000",
      "order": 1
    },
    {
      "partyId": 2,
      "email": "lisi@example.com",
      "name": "李四",
      "phone": "13900139000",
      "order": 2
    }
  ],
  "expireHours": 72,
  "notifyMessage": "请及时签署合同"
}

响应：
{
  "code": 200,
  "data": {
    "processId": "ESP202401010001",
    "signingUrls": [
      {
        "partyId": 1,
        "signingUrl": "https://esign.example.com/sign/abc123"
      }
    ]
  },
  "msg": "电子签署流程已发起"
}
```

### 3.6 合同附件管理模块

#### 3.6.1 附件操作

| 序号 | API 端点 | HTTP 方法 | 描述 | 权限角色 |
|------|----------|-----------|------|----------|
| 40 | `/{contractIdOrUUID}/versions/{versionId}/attachments` | `POST` | 上传附件 | ENTERPRISE_USER |
| 41 | `/{contractIdOrUUID}/versions/{versionId}/attachments` | `GET` | 获取附件列表 | ENTERPRISE_USER |
| 42 | `/{contractIdOrUUID}/versions/{versionId}/attachments/{attachmentId}` | `GET` | 下载附件 | ENTERPRISE_USER |
| 43 | `/{contractIdOrUUID}/versions/{versionId}/attachments/{attachmentId}` | `DELETE` | 删除附件 | ENTERPRISE_USER |
| 44 | `/{contractIdOrUUID}/versions/{versionId}/attachments/batch-download` | `POST` | 批量下载附件 | ENTERPRISE_USER |

### 3.7 合同关联管理模块

#### 3.7.1 业务对象关联

| 序号 | API 端点 | HTTP 方法 | 描述 | 权限角色 |
|------|----------|-----------|------|----------|
| 45 | `/linked-objects` | `POST` | 创建业务关联 | SYSTEM |
| 46 | `/linked-objects` | `GET` | 查询关联关系 | ENTERPRISE_USER |
| 47 | `/linked-objects/{linkId}` | `PUT` | 更新关联状态 | SYSTEM |
| 48 | `/linked-objects/{linkId}` | `DELETE` | 删除关联关系 | SYSTEM |
| 49 | `/validate-business-contract` | `POST` | 校验业务合同有效性 | SYSTEM |

**详细接口设计：**

**49. 校验业务合同有效性**
```
POST /api/v1/contracts/validate-business-contract

请求体：
{
  "businessType": "WASTE_TRANSFER",
  "partyEnterpriseIds": [12345, 54321],
  "businessDate": "2024-01-15",
  "wasteTypes": ["HW08"],
  "additionalCriteria": {
    "region": "上海市",
    "quantity": 10.5
  }
}

响应：
{
  "code": 200,
  "data": {
    "isValid": true,
    "validContracts": [
      {
        "contractId": 12345,
        "contractNo": "CT202401010001",
        "contractName": "XX化工厂废矿物油转移合同",
        "effectiveDate": "2024-01-01",
        "expiryDate": "2024-12-31",
        "matchScore": 95
      }
    ],
    "validationDetails": {
      "hasValidContract": true,
      "contractCount": 1,
      "expiringContracts": [],
      "recommendations": []
    }
  },
  "msg": "校验完成"
}
```

### 3.8 合同查询与统计模块

#### 3.8.1 高级查询

| 序号 | API 端点 | HTTP 方法 | 描述 | 权限角色 |
|------|----------|-----------|------|----------|
| 50 | `/search` | `POST` | 高级搜索合同 | ENTERPRISE_USER |
| 51 | `/expiring-soon` | `GET` | 即将到期合同 | ENTERPRISE_USER |
| 52 | `/my-contracts` | `GET` | 我的合同列表 | ENTERPRISE_USER |
| 53 | `/enterprise/{enterpriseId}/contracts` | `GET` | 企业合同列表 | ENTERPRISE_ADMIN |

#### 3.8.2 统计分析

| 序号 | API 端点 | HTTP 方法 | 描述 | 权限角色 |
|------|----------|-----------|------|----------|
| 54 | `/statistics/summary` | `GET` | 合同统计概览 | ENTERPRISE_ADMIN |
| 55 | `/statistics/by-type` | `GET` | 按类型统计 | ENTERPRISE_ADMIN |
| 56 | `/statistics/by-status` | `GET` | 按状态统计 | ENTERPRISE_ADMIN |
| 57 | `/statistics/signing-efficiency` | `GET` | 签署效率统计 | ENTERPRISE_ADMIN |

### 3.9 合同提醒与通知模块

#### 3.9.1 提醒管理

| 序号 | API 端点 | HTTP 方法 | 描述 | 权限角色 |
|------|----------|-----------|------|----------|
| 58 | `/reminders` | `GET` | 获取提醒列表 | ENTERPRISE_USER |
| 59 | `/reminders/{reminderId}/mark-read` | `POST` | 标记提醒已读 | ENTERPRISE_USER |
| 60 | `/reminders/settings` | `GET` | 获取提醒设置 | ENTERPRISE_USER |
| 61 | `/reminders/settings` | `PUT` | 更新提醒设置 | ENTERPRISE_USER |

### 3.10 数据导出模块

#### 3.10.1 导出功能

| 序号 | API 端点 | HTTP 方法 | 描述 | 权限角色 |
|------|----------|-----------|------|----------|
| 62 | `/export/contracts` | `POST` | 导出合同数据 | ENTERPRISE_ADMIN |
| 63 | `/export/templates` | `POST` | 导出模板数据 | PLATFORM_ADMIN |
| 64 | `/export/statistics` | `POST` | 导出统计报表 | ENTERPRISE_ADMIN |

## 4. 数据传输对象（DTO）规范

### 4.1 请求DTO命名规范
- 创建：`Create{Entity}ReqVO`
- 更新：`Update{Entity}ReqVO`
- 查询：`{Entity}PageReqVO`
- 搜索：`{Entity}SearchReqVO`

### 4.2 响应DTO命名规范
- 详情：`{Entity}RespVO`
- 列表：`{Entity}PageRespVO`
- 简要：`{Entity}SimpleRespVO`
- 统计：`{Entity}StatisticsRespVO`

## 5. 权限角色定义

### 5.1 角色类型
- **PLATFORM_ADMIN**: 平台管理员
- **ENTERPRISE_ADMIN**: 企业管理员
- **ENTERPRISE_USER**: 企业用户
- **SIGNATORY**: 合同签署人
- **SYSTEM**: 系统内部调用

### 5.2 权限矩阵
| 功能模块 | PLATFORM_ADMIN | ENTERPRISE_ADMIN | ENTERPRISE_USER | SIGNATORY |
|----------|----------------|------------------|-----------------|-----------|
| 合同类型管理 | ✓ | ✗ | ✗ | ✗ |
| 合同模板管理 | ✓ | ✗ | ✗ | ✗ |
| 合同创建 | ✓ | ✓ | ✓ | ✗ |
| 合同审批 | ✓ | ✓ | ✗ | ✗ |
| 合同签署 | ✓ | ✓ | ✓ | ✓ |
| 合同查询 | ✓ | ✓ | ✓ | ✓ |

## 6. 安全性要求

### 6.1 认证授权
- JWT Token认证
- 基于角色的权限控制（RBAC）
- API访问频率限制
- 敏感操作二次验证

### 6.2 数据安全
- 合同内容加密存储
- 文件上传安全检查
- 签署过程防篡改
- 操作日志完整记录

## 7. 性能要求

### 7.1 响应时间
- 查询接口：< 500ms
- 创建/更新接口：< 1s
- 文件上传接口：< 5s
- 统计分析接口：< 2s

### 7.2 并发处理
- 支持500+并发请求
- 文件上传支持断点续传
- 大文件异步处理

## 8. 监控和日志

### 8.1 接口监控
- 响应时间监控
- 错误率监控
- 调用量统计
- 签署成功率监控

### 8.2 业务日志
- 合同操作审计日志
- 签署过程记录
- 文件访问日志
- 异常操作告警 