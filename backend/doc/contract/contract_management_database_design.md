# 合同管理模块 - 数据库设计文档 (Database Design Document V2.0)

## 1. 引言

本文档定义了"危废再生资源全流程追溯SaaS平台"中合同管理模块相关的数据库表结构、字段及关系。旨在为数据库设计和开发提供依据。

## 2. 模块职责定位

### 2.1 合同模块职责
- **核心职责**：管理各类业务合同的全生命周期
- **主要功能**：
  - 合同模板管理（创建、维护、版本控制）
  - 合同创建与协商（电子与纸质）
  - 多方电子签章集成（e签宝等）
  - 合同存储、检索与版本控制
  - 合同状态管理与提醒
  - 与其他业务模块的集成

### 2.2 与其他模块的边界
- **合同模块负责**：合同创建、签署、存储、查询、状态管理
- **业务模块负责**：基于合同开展具体业务、合同有效性校验
- **接口协作**：
  - 业务模块查询合同状态和有效性
  - 合同模块提供合同关联和校验服务
  - 合同模块推送状态变更通知

## 3. 核心表结构设计

### 3.1 合同类型表 (`contract_types`)

```sql
CREATE TABLE `contract_type` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '合同类型ID',
  `type_name` varchar(100) NOT NULL COMMENT '类型名称',
  `type_code` varchar(50) NOT NULL COMMENT '类型编码',
  `type_description` text COMMENT '类型描述',
  `is_system_defined` bit(1) NOT NULL DEFAULT b'1' COMMENT '是否为系统预定义类型',
  `is_active` bit(1) NOT NULL DEFAULT b'1' COMMENT '是否启用',
  `sort_order` int(11) DEFAULT '0' COMMENT '排序顺序',
  `tenant_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '租户ID',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_type_code` (`type_code`, `deleted`, `tenant_id`),
  KEY `idx_tenant_id_deleted` (`tenant_id`, `deleted`),
  KEY `idx_is_active` (`is_active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合同类型表';
```

### 3.2 合同模板表 (`contract_templates`)

```sql
CREATE TABLE `contract_templates` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '模板ID',
  `template_name` varchar(255) NOT NULL COMMENT '模板名称',
  `template_code` varchar(100) NOT NULL COMMENT '模板编码',
  `contract_type_id` bigint(20) NOT NULL COMMENT '合同类型ID',
  `template_content` longtext COMMENT '模板内容',
  `template_file_url` varchar(512) DEFAULT '' COMMENT '模板文件URL',
  `version` varchar(50) DEFAULT '1.0' COMMENT '模板版本',
  `is_active` bit(1) NOT NULL DEFAULT b'1' COMMENT '是否启用',
  `is_default` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否默认模板',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `tenant_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '租户ID',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_template_code` (`template_code`, `deleted`, `tenant_id`),
  KEY `idx_contract_type_id` (`contract_type_id`),
  KEY `idx_is_active` (`is_active`),
  KEY `idx_tenant_id_deleted` (`tenant_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合同模板表';
```

### 3.3 合同主表 (`contracts`)

```sql
CREATE TABLE `contracts` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '合同ID',
  `contract_no` varchar(64) NOT NULL COMMENT '合同编号',
  `contract_uuid` varchar(36) NOT NULL COMMENT '合同UUID',
  `contract_name` varchar(255) NOT NULL COMMENT '合同名称',
  `contract_type_id` bigint(20) NOT NULL COMMENT '合同类型ID',
  `template_id` bigint(20) DEFAULT NULL COMMENT '使用的模板ID',
  `current_version_id` bigint(20) DEFAULT NULL COMMENT '当前激活版本ID',
  `status` tinyint(4) NOT NULL DEFAULT '0' COMMENT '合同状态(0:草稿,1:待审核,2:审核通过,3:待签署,4:签署中,5:已生效,6:已履行,7:即将到期,8:已到期,9:已解除,10:已作废,11:已归档)',
  `is_electronic` bit(1) NOT NULL DEFAULT b'1' COMMENT '是否为电子合同',
  `primary_owner_enterprise_id` bigint(20) NOT NULL COMMENT '合同主要负责企业ID',
  `total_amount` decimal(15,2) DEFAULT NULL COMMENT '合同总金额',
  `currency` varchar(10) DEFAULT 'CNY' COMMENT '币种',
  `priority_level` tinyint(4) DEFAULT '0' COMMENT '优先级(0:普通,1:重要,2:紧急)',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `tenant_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '租户ID',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_contract_no` (`contract_no`, `deleted`, `tenant_id`),
  UNIQUE KEY `uk_contract_uuid` (`contract_uuid`),
  KEY `idx_contract_type_id` (`contract_type_id`),
  KEY `idx_status` (`status`),
  KEY `idx_primary_owner_enterprise_id` (`primary_owner_enterprise_id`),
  KEY `idx_current_version_id` (`current_version_id`),
  KEY `idx_tenant_id_deleted` (`tenant_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合同主表';
```

### 3.4 合同版本表 (`contract_versions`)

```sql
CREATE TABLE `contract_versions` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '版本ID',
  `contract_id` bigint(20) NOT NULL COMMENT '合同ID',
  `version_number` varchar(20) NOT NULL DEFAULT '1.0' COMMENT '版本号',
  `effective_date` date DEFAULT NULL COMMENT '生效日期',
  `expiry_date` date DEFAULT NULL COMMENT '失效日期',
  `signing_date` date DEFAULT NULL COMMENT '签署完成日期',
  `termination_date` date DEFAULT NULL COMMENT '终止日期',
  `reason_for_termination` text COMMENT '终止原因',
  `description_of_changes` text COMMENT '版本变更说明',
  `contract_content` longtext COMMENT '合同内容',
  `contract_file_url` varchar(512) DEFAULT '' COMMENT '合同文件URL',
  `esignature_provider` varchar(50) DEFAULT '' COMMENT '电子签章服务商',
  `esignature_process_id` varchar(100) DEFAULT '' COMMENT '第三方签署流程ID',
  `esignature_status` tinyint(4) DEFAULT '0' COMMENT '电子签章状态(0:未发起,1:进行中,2:已完成,3:已失败)',
  `auto_remind_days` varchar(100) DEFAULT '30,15,7' COMMENT '自动提醒天数(逗号分隔)',
  `tenant_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '租户ID',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_contract_id` (`contract_id`),
  KEY `idx_effective_date` (`effective_date`),
  KEY `idx_expiry_date` (`expiry_date`),
  KEY `idx_esignature_status` (`esignature_status`),
  KEY `idx_tenant_id_deleted` (`tenant_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合同版本表';
```

### 3.5 合同参与方表 (`contract_parties`)

```sql
CREATE TABLE `contract_parties` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '参与方ID',
  `version_id` bigint(20) NOT NULL COMMENT '合同版本ID',
  `enterprise_id` bigint(20) NOT NULL COMMENT '参与企业ID',
  `enterprise_name` varchar(255) NOT NULL COMMENT '企业名称',
  `role_in_contract` varchar(100) NOT NULL COMMENT '合同中的角色',
  `signatory_name` varchar(100) DEFAULT '' COMMENT '签署人姓名',
  `signatory_email` varchar(255) DEFAULT '' COMMENT '签署人邮箱',
  `signatory_phone` varchar(20) DEFAULT '' COMMENT '签署人电话',
  `signatory_user_id` bigint(20) DEFAULT NULL COMMENT '签署人用户ID',
  `sign_status` tinyint(4) NOT NULL DEFAULT '0' COMMENT '签署状态(0:待签署,1:已签署,2:已拒绝,3:已过期)',
  `signed_at` datetime DEFAULT NULL COMMENT '签署时间',
  `sign_ip` varchar(50) DEFAULT '' COMMENT '签署IP地址',
  `esignature_individual_id` varchar(100) DEFAULT '' COMMENT '电子签章个体ID',
  `order_in_sign_flow` int(11) DEFAULT '0' COMMENT '签署顺序',
  `is_required` bit(1) NOT NULL DEFAULT b'1' COMMENT '是否必须签署',
  `tenant_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '租户ID',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_version_id` (`version_id`),
  KEY `idx_enterprise_id` (`enterprise_id`),
  KEY `idx_sign_status` (`sign_status`),
  KEY `idx_signatory_user_id` (`signatory_user_id`),
  KEY `idx_tenant_id_deleted` (`tenant_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合同参与方表';
```

### 3.6 合同附件表 (`contract_attachments`)

```sql
CREATE TABLE `contract_attachments` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '附件ID',
  `version_id` bigint(20) NOT NULL COMMENT '合同版本ID',
  `file_name` varchar(255) NOT NULL COMMENT '文件名',
  `original_file_name` varchar(255) NOT NULL COMMENT '原始文件名',
  `file_path` varchar(512) NOT NULL COMMENT '文件路径',
  `file_url` varchar(512) NOT NULL COMMENT '文件URL',
  `file_size` bigint(20) DEFAULT NULL COMMENT '文件大小(字节)',
  `file_type` varchar(100) DEFAULT '' COMMENT '文件类型(MIME)',
  `attachment_type` tinyint(4) NOT NULL DEFAULT '0' COMMENT '附件类型(0:合同正文,1:扫描件,2:补充文件,3:签署凭证)',
  `is_public` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否公开',
  `download_count` int(11) DEFAULT '0' COMMENT '下载次数',
  `tenant_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '租户ID',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_version_id` (`version_id`),
  KEY `idx_attachment_type` (`attachment_type`),
  KEY `idx_tenant_id_deleted` (`tenant_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合同附件表';
```

### 3.7 合同关联对象表 (`contract_linked_objects`)

```sql
CREATE TABLE `contract_linked_objects` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '关联ID',
  `contract_id` bigint(20) NOT NULL COMMENT '合同ID',
  `version_id` bigint(20) NOT NULL COMMENT '合同版本ID',
  `object_id` bigint(20) NOT NULL COMMENT '关联对象ID',
  `object_type` varchar(50) NOT NULL COMMENT '关联对象类型',
  `object_no` varchar(100) DEFAULT '' COMMENT '关联对象编号',
  `link_type` tinyint(4) NOT NULL DEFAULT '0' COMMENT '关联类型(0:业务关联,1:依赖关联,2:参考关联)',
  `link_status` tinyint(4) NOT NULL DEFAULT '0' COMMENT '关联状态(0:有效,1:失效,2:暂停)',
  `link_description` varchar(500) DEFAULT '' COMMENT '关联说明',
  `tenant_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '租户ID',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_contract_id` (`contract_id`),
  KEY `idx_version_id` (`version_id`),
  KEY `idx_object_id_type` (`object_id`, `object_type`),
  KEY `idx_link_status` (`link_status`),
  KEY `idx_tenant_id_deleted` (`tenant_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合同关联对象表';
```

### 3.8 合同操作日志表 (`contract_operation_logs`)

```sql
CREATE TABLE `contract_operation_logs` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `contract_id` bigint(20) NOT NULL COMMENT '合同ID',
  `version_id` bigint(20) DEFAULT NULL COMMENT '合同版本ID',
  `operation_type` tinyint(4) NOT NULL COMMENT '操作类型(1:创建,2:修改,3:提交审核,4:审核,5:签署,6:激活,7:终止,8:归档)',
  `operation_description` varchar(500) NOT NULL COMMENT '操作描述',
  `old_status` tinyint(4) DEFAULT NULL COMMENT '操作前状态',
  `new_status` tinyint(4) DEFAULT NULL COMMENT '操作后状态',
  `operation_data` text COMMENT '操作数据(JSON)',
  `operator_id` bigint(20) NOT NULL COMMENT '操作人ID',
  `operator_name` varchar(64) NOT NULL COMMENT '操作人姓名',
  `operator_ip` varchar(50) DEFAULT '' COMMENT '操作IP',
  `tenant_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '租户ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_contract_id` (`contract_id`),
  KEY `idx_version_id` (`version_id`),
  KEY `idx_operation_type` (`operation_type`),
  KEY `idx_operator_id` (`operator_id`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合同操作日志表';
```

## 4. 枚举值定义

### 4.1 合同状态 (contracts.status)
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

### 4.2 签署状态 (contract_parties.sign_status)
- 0: 待签署 (PENDING)
- 1: 已签署 (SIGNED)
- 2: 已拒绝 (DECLINED)
- 3: 已过期 (EXPIRED)

### 4.3 电子签章状态 (contract_versions.esignature_status)
- 0: 未发起 (NOT_INITIATED)
- 1: 进行中 (IN_PROGRESS)
- 2: 已完成 (COMPLETED)
- 3: 已失败 (FAILED)

### 4.4 附件类型 (contract_attachments.attachment_type)
- 0: 合同正文 (MAIN_CONTRACT)
- 1: 扫描件 (SCANNED_COPY)
- 2: 补充文件 (SUPPLEMENTARY_DOCUMENT)
- 3: 签署凭证 (SIGNATURE_CERTIFICATE)

### 4.5 关联类型 (contract_linked_objects.link_type)
- 0: 业务关联 (BUSINESS_LINK)
- 1: 依赖关联 (DEPENDENCY_LINK)
- 2: 参考关联 (REFERENCE_LINK)

### 4.6 操作类型 (contract_operation_logs.operation_type)
- 1: 创建 (CREATE)
- 2: 修改 (UPDATE)
- 3: 提交审核 (SUBMIT_FOR_APPROVAL)
- 4: 审核 (APPROVE_OR_REJECT)
- 5: 签署 (SIGN)
- 6: 激活 (ACTIVATE)
- 7: 终止 (TERMINATE)
- 8: 归档 (ARCHIVE)

## 5. 注意事项

1. **表命名规范**: 所有表名采用 `contract_` 前缀，使用小写字母和下划线命名法
2. **字段命名规范**: 所有字段名使用小写字母和下划线命名法，避免使用驼峰命名
3. **必备字段**: 每个表都包含标准的创建时间、更新时间、创建者、更新者、删除标志、租户ID字段
4. **字段类型规范**: 
   - 主键使用 `bigint(20)` 类型并自增
   - 状态字段使用 `tinyint(4)` 类型
   - 金额字段使用 `decimal(15,2)` 类型
   - 时间字段使用 `datetime` 类型
   - 文本字段根据长度选择 `varchar` 或 `text` 类型
5. **索引规范**: 
   - 主键索引必须有
   - 唯一约束使用 `uk_` 前缀
   - 普通索引使用 `idx_` 前缀
   - 外键字段、状态字段、时间字段都应添加索引
6. **枚举值**: 所有状态字段使用数字枚举，在注释中说明各个值的含义
7. **字符集**: 统一使用 `utf8mb4` 字符集
8. **存储引擎**: 统一使用 InnoDB 存储引擎
9. **外键关联**: 
   - `enterprise_id` 关联系统企业表
   - `user_id` 关联系统用户表
   - `tenant_id` 支持多租户隔离

## 6. 关系说明

*   `contracts` 与 `contract_versions`: 一对多 (一个合同有多个版本，但通常只有一个当前激活版本)
*   `contract_versions` 与 `contract_parties`: 一对多 (一个合同版本有多个参与方)
*   `contract_versions` 与 `contract_attachments`: 一对多 (一个合同版本有多个附件)
*   `contracts` 与 `contract_types`: 多对一
*   `contract_templates` 与 `contract_types`: 多对一
*   `contract_versions` 与 `contract_linked_objects`: 一对多
*   `contracts` 与 `contract_operation_logs`: 一对多 