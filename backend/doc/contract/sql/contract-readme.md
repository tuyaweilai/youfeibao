# 合同管理模块数据库说明文档

## 1. 概述

本目录包含"危废再生资源全流程追溯SaaS平台"中合同管理模块的数据库脚本文件。合同管理模块作为独立的业务模块，为其他模块提供合同相关的基础服务。

## 2. 文件说明

| 文件名 | 描述 |
|---------|---------|
| `contract-management-schema.sql` | 合同管理模块完整数据库脚本，包含所有表创建和基础数据插入 |
| `contract-readme.md` | 本说明文档 |

## 3. 执行说明

### 3.1 执行脚本

```bash
mysql -u用户名 -p密码 数据库名 < contract-management-schema.sql
```

## 4. 表结构说明

### 4.1 核心业务表

| 表名 | 描述 | 主要字段 |
|------|------|----------|
| `contracts` | 合同主表 | 合同ID、合同编号、合同名称、合同类型、状态、负责企业、金额 |
| `contract_versions` | 合同版本表 | 版本ID、合同ID、版本号、生效日期、失效日期、合同内容、电子签章状态 |
| `contract_parties` | 合同参与方表 | 参与方ID、版本ID、企业ID、角色、签署人信息、签署状态 |

### 4.2 基础配置表

| 表名 | 描述 | 主要字段 |
|------|------|----------|
| `contract_types` | 合同类型表 | 类型ID、类型名称、类型编码、描述、是否系统预定义 |
| `contract_templates` | 合同模板表 | 模板ID、模板名称、模板编码、合同类型、模板内容、版本 |

### 4.3 扩展功能表

| 表名 | 描述 | 主要字段 |
|------|------|----------|
| `contract_attachments` | 合同附件表 | 附件ID、版本ID、文件信息、附件类型、下载次数 |
| `contract_linked_objects` | 合同关联对象表 | 关联ID、合同ID、对象ID、对象类型、关联类型、关联状态 |
| `contract_operation_logs` | 合同操作日志表 | 日志ID、合同ID、操作类型、操作描述、操作人、操作时间 |

## 5. 数据库设计规范

1. **表命名规范**：所有表名采用 `contract_` 前缀，使用小写字母和下划线命名法
2. **字段命名规范**：所有字段名使用小写字母和下划线命名法，避免使用驼峰命名
3. **必备字段**：每个表都包含标准的创建时间、更新时间、创建者、更新者、删除标志、租户ID字段
4. **字段类型规范**：
   - 主键使用 `bigint(20)` 类型并自增
   - 状态字段使用 `tinyint(4)` 类型
   - 金额字段使用 `decimal(15,2)` 类型
   - 时间字段使用 `datetime` 类型
   - 文本字段根据长度选择 `varchar` 或 `text` 类型
5. **索引规范**：
   - 主键索引必须有
   - 唯一约束使用 `uk_` 前缀
   - 普通索引使用 `idx_` 前缀
   - 外键字段、状态字段、时间字段都应添加索引
6. **字符集**：统一使用 `utf8mb4` 字符集
7. **存储引擎**：统一使用 InnoDB 存储引擎

## 6. 枚举值说明

### 6.1 合同状态 (contracts.status)
- 0: 草稿 (DRAFT)
- 1: 待审核 (PENDING_APPROVAL)
- 2: 审核通过 (APPROVED)
- 3: 待签署 (PENDING_SIGNATURE)
- 4: 签署中 (SIGNING)
- 5: 已生效 (ACTIVE)
- 6: 已履行 (FULFILLED)
- 7: 即将到期 (EXPIRING_SOON)
- 8: 已到期 (EXPIRED)
- 9: 已解除 (TERMINATED)
- 10: 已作废 (VOIDED)
- 11: 已归档 (ARCHIVED)

### 6.2 签署状态 (contract_parties.sign_status)
- 0: 待签署 (PENDING)
- 1: 已签署 (SIGNED)
- 2: 已拒绝 (DECLINED)
- 3: 已过期 (EXPIRED)

### 6.3 电子签章状态 (contract_versions.esignature_status)
- 0: 未发起 (NOT_INITIATED)
- 1: 进行中 (IN_PROGRESS)
- 2: 已完成 (COMPLETED)
- 3: 已失败 (FAILED)

### 6.4 附件类型 (contract_attachments.attachment_type)
- 0: 合同正文 (MAIN_CONTRACT)
- 1: 扫描件 (SCANNED_COPY)
- 2: 补充文件 (SUPPLEMENTARY_DOCUMENT)
- 3: 签署凭证 (SIGNATURE_CERTIFICATE)

### 6.5 关联类型 (contract_linked_objects.link_type)
- 0: 业务关联 (BUSINESS_LINK)
- 1: 依赖关联 (DEPENDENCY_LINK)
- 2: 参考关联 (REFERENCE_LINK)

### 6.6 操作类型 (contract_operation_logs.operation_type)
- 1: 创建 (CREATE)
- 2: 修改 (UPDATE)
- 3: 提交审核 (SUBMIT_FOR_APPROVAL)
- 4: 审核 (APPROVE_OR_REJECT)
- 5: 签署 (SIGN)
- 6: 激活 (ACTIVATE)
- 7: 终止 (TERMINATE)
- 8: 归档 (ARCHIVE)

## 7. 系统预定义数据

### 7.1 合同类型
脚本会自动插入以下系统预定义的合同类型：

1. **危险废物转移合同** (WASTE_TRANSFER)
   - 产废企业与回收企业之间的危险废物转移合同

2. **物流服务合同** (LOGISTICS_SERVICE)
   - 回收企业与物流企业之间的物流服务合同

3. **废物处置合同** (WASTE_DISPOSAL)
   - 回收企业与处置企业之间的废物处置合同

4. **平台服务协议** (PLATFORM_SERVICE)
   - 企业与平台之间的服务协议

5. **采购合同** (PROCUREMENT)
   - 企业间的采购合同

6. **技术服务合同** (TECHNICAL_SERVICE)
   - 技术服务相关合同

### 7.2 系统配置
脚本会自动插入以下系统配置：

- `contract.number.prefix`: 合同编号生成前缀 (CT)
- `contract.expiry.remind.days`: 合同到期自动提醒天数 (30,15,7)
- `contract.esignature.provider`: 默认电子签章服务商 (esign)
- `contract.file.storage.path`: 合同文件存储相对路径 (/contract/files/)
- `contract.approval.enabled`: 是否启用合同审核流程 (true)

## 8. 与其他模块的关联

### 8.1 企业关联
- 合同模块通过 `enterprise_id` 字段关联系统现有的企业表
- 不维护独立的企业信息，避免数据冗余

### 8.2 用户关联
- 签署人信息通过 `signatory_user_id` 字段关联系统用户表
- 操作日志通过 `operator_id` 字段关联操作人信息

### 8.3 业务关联
- 通过 `contract_linked_objects` 表关联其他业务对象
- 支持关联订单、预约、项目等各种业务实体
- 提供灵活的关联类型和状态管理

## 9. 扩展性设计

### 9.1 版本控制
- 合同支持多版本管理，每个版本独立存储
- 支持版本间的变更说明和追溯

### 9.2 电子签章集成
- 预留电子签章服务商接口字段
- 支持第三方电子签章平台集成

### 9.3 多租户支持
- 所有表都包含 `tenant_id` 字段
- 支持多租户数据隔离

### 9.4 审计追溯
- 完整的操作日志记录
- 支持合同全生命周期追溯

## 10. 注意事项

1. **数据一致性**：合同状态变更需要严格按照状态机流转
2. **权限控制**：不同角色对合同的操作权限需要在应用层控制
3. **文件管理**：合同文件和附件的存储路径需要与文件服务配置一致
4. **电子签章**：集成第三方电子签章服务时需要处理回调和状态同步
5. **到期提醒**：需要配置定时任务处理合同到期提醒功能 