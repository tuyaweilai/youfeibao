# 危险废物转移模块数据库设计文档 (V3.0)

## 1. 设计原则

本数据库设计遵循项目提供的 `database-design-rules.mdc` 规范，包括：
- 表名与字段名采用小写字母和下划线命名法。
- 公共字段（`id`, `tenant_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`）的包含。
- 明确的字段类型、注释和索引策略。
- 存储引擎 InnoDB，字符集 utf8mb4。
- 禁止使用外键约束，在应用层面控制。
- 字段默认值不使用 NULL，尽量使用空字符串、0或其他业务默认值。

## 2. 模块职责定位

### 2.1 危废转移模块职责
- **核心职责**：管理危废转移的业务逻辑和交易流程
- **主要功能**：
  - 预约管理（创建、审核、分配）
  - 订单管理（创建、价格、状态）
  - 付款配置和结算管理
  - 过磅数据处理和分摊
  - 价格管理

### 2.2 与物流模块的边界
- **危废模块负责**：业务决策、金额计算、付款管理
- **物流模块负责**：运输执行、节点上报、过磅操作
- **接口协作**：通过API进行数据交换，避免直接数据库耦合

## 3. 公共字段说明

每个业务表都应包含以下公共字段：

| 字段名 | 类型 | 非空 | 默认值 | 注释 |
|--------|------|------|--------|------|
| `id` | `bigint(20)` | 是 | | 主键ID |
| `tenant_id` | `bigint(20)` | 是 | `0` | 租户ID |
| `creator` | `varchar(64)` | 否 | `''` | 创建者 |
| `create_time` | `datetime` | 是 | `CURRENT_TIMESTAMP` | 创建时间 |
| `updater` | `varchar(64)` | 否 | `''` | 更新者 |
| `update_time` | `datetime` | 是 | `CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP` | 更新时间 |
| `deleted` | `bit(1)` | 是 | `b'0'` | 是否删除 (0:未删除, 1:已删除) |

## 4. 核心表结构设计

### 4.1 订单主表 (`waste_transfer_order`) - 核心业务表

```sql
CREATE TABLE `waste_transfer_order` (
  `id` bigint(20) NOT NULL COMMENT '订单ID',
  `order_no` varchar(64) NOT NULL COMMENT '订单号 (系统生成, 唯一)',
  `appointment_id` bigint(20) DEFAULT NULL COMMENT '关联预约单ID',
  `quotation_id` bigint(20) DEFAULT NULL COMMENT '关联报价记录ID',
  `producing_enterprise_id` bigint(20) NOT NULL COMMENT '产废企业ID',
  `producing_store_id` bigint(20) DEFAULT NULL COMMENT '产废门店ID',
  `recycling_enterprise_id` bigint(20) NOT NULL COMMENT '回收企业ID',
  
  -- 物流关联（通过接口协作）
  `transport_task_id` bigint(20) DEFAULT NULL COMMENT '关联的物流运输任务ID',
  `logistics_status` tinyint(4) DEFAULT NULL COMMENT '物流状态快照',
  
  -- 废物信息
  `waste_code` varchar(50) NOT NULL COMMENT '危险废物代码',
  `waste_name` varchar(100) NOT NULL COMMENT '危险废物名称',
  `estimated_quantity` decimal(10,2) NOT NULL COMMENT '预估数量',
  `allocated_quantity` decimal(10,2) DEFAULT NULL COMMENT '基于过磅的分摊数量',
  `quantity_unit` varchar(10) NOT NULL COMMENT '数量单位',
  `packaging_type` varchar(50) DEFAULT '' COMMENT '包装方式',
  
  -- 价格信息
  `unit_price` decimal(10,2) NOT NULL COMMENT '单价',
  `estimated_amount` decimal(12,2) NOT NULL COMMENT '预估总金额',
  `final_amount` decimal(12,2) DEFAULT NULL COMMENT '最终结算金额',
  
  -- 业务状态
  `business_status` tinyint(4) NOT NULL DEFAULT '0' COMMENT '业务状态 (0:待确认, 1:已确认, 2:待结算, 3:已结算, 4:已完成, 5:已取消)',
  
  -- 付款管理
  `payment_config_id` bigint(20) DEFAULT NULL COMMENT '关联的产废企业付款配置ID',
  `payment_method_type` tinyint(4) DEFAULT NULL COMMENT '实际付款方式 (1:对公结算, 2:个人结算)',
  `payment_status` tinyint(4) DEFAULT '0' COMMENT '付款状态 (0:未付款, 1:已付款, 2:付款失败, 3:待凭证上传, 4:凭证已上传, 5:凭证已确认)',
  `payment_completed_time` datetime DEFAULT NULL COMMENT '付款完成时间',
  `payment_voucher_id` bigint(20) DEFAULT NULL COMMENT '对公付款凭证ID',
  
  -- 分摊信息
  `allocation_ratio` decimal(8,5) DEFAULT NULL COMMENT '在车辆总重量中的分摊比例',
  `allocation_completed_time` datetime DEFAULT NULL COMMENT '订单分摊完成时间',
  `variance_from_estimate` decimal(10,2) DEFAULT NULL COMMENT '与预估量的差异',
  `variance_rate` decimal(5,2) DEFAULT NULL COMMENT '差异率 (%)',
  
  `source_type` tinyint(4) NOT NULL DEFAULT '0' COMMENT '订单来源类型 (0:预约转订单, 1:扫街临时订单, 2:补单)',
  `related_order_id` bigint(20) DEFAULT NULL COMMENT '关联订单ID (用于补单场景)',
  `user_remark` varchar(255) DEFAULT '' COMMENT '用户备注',
  `internal_remark` varchar(255) DEFAULT '' COMMENT '内部备注',
  
  `tenant_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '租户ID',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no_tenant_id` (`order_no`, `tenant_id`, `deleted`),
  KEY `idx_appointment_id` (`appointment_id`),
  KEY `idx_producing_enterprise_id` (`producing_enterprise_id`),
  KEY `idx_recycling_enterprise_id` (`recycling_enterprise_id`),
  KEY `idx_transport_task_id` (`transport_task_id`),
  KEY `idx_business_status` (`business_status`),
  KEY `idx_payment_status` (`payment_status`),
  KEY `idx_tenant_id_deleted_status` (`tenant_id`, `deleted`, `business_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='危废转移订单主表';
```

### 4.2 订单分摊记录表 (`waste_order_allocation_record`)

```sql
CREATE TABLE `waste_order_allocation_record` (
  `id` bigint(20) NOT NULL COMMENT '分摊记录ID',
  `order_id` bigint(20) NOT NULL COMMENT '订单ID',
  `weighing_batch_no` varchar(64) NOT NULL COMMENT '过磅批次号（来自物流模块）',
  `vehicle_net_weight` decimal(10,2) NOT NULL COMMENT '车辆净重（来自物流模块）',
  `allocation_method` tinyint(4) NOT NULL DEFAULT '1' COMMENT '分摊方法 (1:按预估量比例, 2:按确认量比例, 3:人工指定, 4:平均分摊)',
  
  -- 分摊计算过程
  `estimated_quantity` decimal(10,2) NOT NULL COMMENT '订单预估量',
  `allocation_ratio` decimal(8,5) NOT NULL COMMENT '分摊比例',
  `allocated_quantity` decimal(10,2) NOT NULL COMMENT '分摊后数量',
  
  -- 金额计算
  `unit_price` decimal(10,2) NOT NULL COMMENT '单价',
  `estimated_amount` decimal(12,2) NOT NULL COMMENT '预估金额',
  `allocated_amount` decimal(12,2) NOT NULL COMMENT '分摊后金额',
  `amount_adjustment` decimal(12,2) NOT NULL COMMENT '金额调整 = 分摊金额 - 预估金额',
  
  -- 分摊详情
  `allocation_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '分摊时间',
  `allocation_operator` varchar(64) DEFAULT '' COMMENT '分摊操作员',
  `is_manual_adjustment` bit(1) DEFAULT b'0' COMMENT '是否人工调整',
  `adjustment_reason` varchar(255) DEFAULT '' COMMENT '人工调整原因',
  `remark` varchar(255) DEFAULT '' COMMENT '分摊备注',
  
  `tenant_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '租户ID',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_batch` (`order_id`, `weighing_batch_no`, `deleted`),
  KEY `idx_weighing_batch_no` (`weighing_batch_no`),
  KEY `idx_allocation_method` (`allocation_method`),
  KEY `idx_allocation_time` (`allocation_time`),
  KEY `idx_is_manual_adjustment` (`is_manual_adjustment`),
  KEY `idx_tenant_id_deleted` (`tenant_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='订单过磅分摊记录表';
```

### 4.3 订单状态历史表 (`waste_order_status_history`)

```sql
CREATE TABLE `waste_order_status_history` (
  `id` bigint(20) NOT NULL COMMENT '状态历史ID',
  `order_id` bigint(20) NOT NULL COMMENT '订单ID',
  `status_from` tinyint(4) DEFAULT NULL COMMENT '变更前状态',
  `status_to` tinyint(4) NOT NULL COMMENT '变更后状态',
  `status_name` varchar(50) NOT NULL COMMENT '状态名称',
  `change_type` tinyint(4) NOT NULL DEFAULT '1' COMMENT '变更类型 (1:正常流转, 2:异常处理, 3:人工干预, 4:系统回滚)',
  `change_reason` varchar(200) DEFAULT '' COMMENT '状态变更原因',
  `operator_type` tinyint(4) NOT NULL COMMENT '操作者类型 (1:产废企业, 2:回收企业, 3:物流企业, 4:系统自动, 5:平台管理员)',
  `operator_id` bigint(20) DEFAULT NULL COMMENT '操作者ID',
  `operator_name` varchar(64) DEFAULT '' COMMENT '操作者姓名',
  `operator_ip` varchar(50) DEFAULT '' COMMENT '操作者IP地址',
  `change_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '状态变更时间',
  `duration_seconds` int(11) DEFAULT NULL COMMENT '在前一状态的停留时长(秒)',
  `milestone_flag` bit(1) DEFAULT b'0' COMMENT '是否关键里程碑',
  
  -- 🔥 扩展的业务数据
  `payment_method` tinyint(4) DEFAULT NULL COMMENT '付款方式 (状态为已确认收货时)',
  `cash_amount` decimal(10,2) DEFAULT NULL COMMENT '现金金额 (现金付款时)',
  `online_amount` decimal(10,2) DEFAULT NULL COMMENT '线上金额 (线上付款时)',
  `vehicle_weighing_id` bigint(20) DEFAULT NULL COMMENT '关联的车辆过磅ID',
  `allocation_ratio` decimal(8,5) DEFAULT NULL COMMENT '分摊比例 (分摊完成时)',
  
  `business_data` text DEFAULT NULL COMMENT '业务相关数据 (JSON格式)',
  `location_info` varchar(200) DEFAULT '' COMMENT 'GPS位置信息',
  `remark` varchar(500) DEFAULT '' COMMENT '备注说明',
  `tenant_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '租户ID',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_status_to` (`status_to`),
  KEY `idx_change_type` (`change_type`),
  KEY `idx_operator_type_id` (`operator_type`, `operator_id`),
  KEY `idx_change_time` (`change_time`),
  KEY `idx_milestone_flag` (`milestone_flag`),
  KEY `idx_payment_method` (`payment_method`),
  KEY `idx_vehicle_weighing_id` (`vehicle_weighing_id`),
  KEY `idx_tenant_id_deleted` (`tenant_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='订单状态变更历史表';
```

### 4.4 产废企业付款配置表 (`waste_producer_payment_config`)

```sql
CREATE TABLE `waste_order_status_history` (
  `id` bigint(20) NOT NULL COMMENT '状态历史ID',
  `order_id` bigint(20) NOT NULL COMMENT '订单ID',
  `status_from` tinyint(4) DEFAULT NULL COMMENT '变更前状态',
  `status_to` tinyint(4) NOT NULL COMMENT '变更后状态',
  `status_name` varchar(50) NOT NULL COMMENT '状态名称',
  `change_type` tinyint(4) NOT NULL DEFAULT '1' COMMENT '变更类型 (1:正常流转, 2:异常处理, 3:人工干预, 4:系统回滚)',
  `change_reason` varchar(200) DEFAULT '' COMMENT '状态变更原因',
  `operator_type` tinyint(4) NOT NULL COMMENT '操作者类型 (1:产废企业, 2:回收企业, 3:物流企业, 4:系统自动, 5:平台管理员)',
  `operator_id` bigint(20) DEFAULT NULL COMMENT '操作者ID',
  `operator_name` varchar(64) DEFAULT '' COMMENT '操作者姓名',
  `operator_ip` varchar(50) DEFAULT '' COMMENT '操作者IP地址',
  `change_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '状态变更时间',
  `duration_seconds` int(11) DEFAULT NULL COMMENT '在前一状态的停留时长(秒)',
  `milestone_flag` bit(1) DEFAULT b'0' COMMENT '是否关键里程碑',
  
  -- 🔥 扩展的业务数据
  `payment_method` tinyint(4) DEFAULT NULL COMMENT '付款方式 (状态为已确认收货时)',
  `cash_amount` decimal(10,2) DEFAULT NULL COMMENT '现金金额 (现金付款时)',
  `online_amount` decimal(10,2) DEFAULT NULL COMMENT '线上金额 (线上付款时)',
  `vehicle_weighing_id` bigint(20) DEFAULT NULL COMMENT '关联的车辆过磅ID',
  `allocation_ratio` decimal(8,5) DEFAULT NULL COMMENT '分摊比例 (分摊完成时)',
  
  `business_data` text DEFAULT NULL COMMENT '业务相关数据 (JSON格式)',
  `location_info` varchar(200) DEFAULT '' COMMENT 'GPS位置信息',
  `remark` varchar(500) DEFAULT '' COMMENT '备注说明',
  `tenant_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '租户ID',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_status_to` (`status_to`),
  KEY `idx_change_type` (`change_type`),
  KEY `idx_operator_type_id` (`operator_type`, `operator_id`),
  KEY `idx_change_time` (`change_time`),
  KEY `idx_milestone_flag` (`milestone_flag`),
  KEY `idx_payment_method` (`payment_method`),
  KEY `idx_vehicle_weighing_id` (`vehicle_weighing_id`),
  KEY `idx_tenant_id_deleted` (`tenant_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='订单状态变更历史表';
```

### 4.6 产废企业付款配置表 (`waste_producer_payment_config`) - 新增核心表

```sql
CREATE TABLE `waste_producer_payment_config` (
  `id` bigint(20) NOT NULL COMMENT '配置ID',
  `producing_enterprise_id` bigint(20) NOT NULL COMMENT '产废企业ID',
  `producing_store_id` bigint(20) DEFAULT NULL COMMENT '产废门店ID (可细化到门店级别)',
  `payment_method` tinyint(4) NOT NULL DEFAULT '1' COMMENT '结算方式 (1:对公结算, 2:个人结算)',
  `is_default_config` bit(1) DEFAULT b'0' COMMENT '是否为企业默认配置',
  
  -- 🔥 对公结算配置
  `company_bank_name` varchar(100) DEFAULT '' COMMENT '对公开户银行',
  `company_bank_account` varchar(50) DEFAULT '' COMMENT '对公银行账号',
  `company_account_name` varchar(100) DEFAULT '' COMMENT '对公账户名称',
  `tax_number` varchar(50) DEFAULT '' COMMENT '税号',
  `company_address` varchar(200) DEFAULT '' COMMENT '公司地址',
  `company_phone` varchar(20) DEFAULT '' COMMENT '公司电话',
  `invoice_required` bit(1) DEFAULT b'0' COMMENT '是否需要开票',
  `invoice_title` varchar(100) DEFAULT '' COMMENT '发票抬头',
  
  -- 🔥 个人结算配置  
  `personal_payee_name` varchar(64) DEFAULT '' COMMENT '个人收款人姓名',
  `personal_payee_phone` varchar(20) DEFAULT '' COMMENT '个人收款人电话',
  `personal_payee_id_card` varchar(18) DEFAULT '' COMMENT '个人收款人身份证号',
  `personal_bank_name` varchar(100) DEFAULT '' COMMENT '个人开户银行',
  `personal_bank_account` varchar(50) DEFAULT '' COMMENT '个人银行账号',
  `personal_account_name` varchar(64) DEFAULT '' COMMENT '个人账户名称',
  `relationship_to_enterprise` varchar(50) DEFAULT '' COMMENT '与企业关系 (法人/财务/授权人等)',
  
  -- 🔥 通用配置
  `auto_payment_enabled` bit(1) DEFAULT b'0' COMMENT '是否启用自动付款',
  `payment_delay_hours` int(11) DEFAULT '0' COMMENT '付款延迟时间(小时) 0=立即',
  `min_payment_amount` decimal(10,2) DEFAULT '0.00' COMMENT '最小付款金额阈值',
  `max_payment_amount` decimal(10,2) DEFAULT '999999.99' COMMENT '最大付款金额阈值',
  
  `config_status` tinyint(4) NOT NULL DEFAULT '1' COMMENT '配置状态 (1:有效, 2:无效, 3:待审核)',
  `approved_by` varchar(64) DEFAULT '' COMMENT '审核人',
  `approved_time` datetime DEFAULT NULL COMMENT '审核时间',
  `remark` varchar(255) DEFAULT '' COMMENT '配置备注',
  
  `tenant_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '租户ID',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_producing_enterprise_id` (`producing_enterprise_id`),
  KEY `idx_producing_store_id` (`producing_store_id`),
  KEY `idx_payment_method` (`payment_method`),
  KEY `idx_is_default_config` (`is_default_config`),
  KEY `idx_config_status` (`config_status`),
  KEY `idx_tenant_id_deleted` (`tenant_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='产废企业付款配置表';
```

### 4.7 对公付款凭证表 (`waste_company_payment_voucher`) - 新增

```sql
CREATE TABLE `waste_company_payment_voucher` (
  `id` bigint(20) NOT NULL COMMENT '凭证ID',
  `order_id` bigint(20) NOT NULL COMMENT '订单ID',
  `voucher_no` varchar(64) NOT NULL COMMENT '凭证单号',
  `payment_type` tinyint(4) NOT NULL DEFAULT '1' COMMENT '付款类型 (1:银行转账, 2:网银支付, 3:现金支付, 4:支票)',
  `payment_amount` decimal(12,2) NOT NULL COMMENT '付款金额',
  `payment_date` date NOT NULL COMMENT '付款日期',
  `payment_bank` varchar(100) DEFAULT '' COMMENT '付款银行',
  `payment_account` varchar(50) DEFAULT '' COMMENT '付款账号',
  `payee_bank` varchar(100) DEFAULT '' COMMENT '收款银行',
  `payee_account` varchar(50) DEFAULT '' COMMENT '收款账号',
  `payee_name` varchar(100) DEFAULT '' COMMENT '收款户名',
  `transaction_no` varchar(100) DEFAULT '' COMMENT '银行流水号/交易号',
  `transfer_voucher_url` varchar(500) DEFAULT '' COMMENT '转账凭证图片URL',
  `bank_receipt_url` varchar(500) DEFAULT '' COMMENT '银行回单图片URL',
  
  -- 🔥 确认状态
  `voucher_status` tinyint(4) NOT NULL DEFAULT '0' COMMENT '凭证状态 (0:待确认, 1:已确认, 2:有争议, 3:已退回)',
  `confirmed_by_producer` bit(1) DEFAULT b'0' COMMENT '产废企业是否确认',
  `confirmed_by_recycling` bit(1) DEFAULT b'0' COMMENT '回收企业是否确认',
  `producer_confirm_time` datetime DEFAULT NULL COMMENT '产废企业确认时间',
  `recycling_confirm_time` datetime DEFAULT NULL COMMENT '回收企业确认时间',
  `dispute_reason` varchar(255) DEFAULT '' COMMENT '争议原因',
  
  `operator_id` bigint(20) DEFAULT NULL COMMENT '操作员ID',
  `operator_name` varchar(64) DEFAULT '' COMMENT '操作员姓名',
  `remark` varchar(255) DEFAULT '' COMMENT '备注',
  
  `tenant_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '租户ID',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_voucher_no_tenant_id` (`voucher_no`, `tenant_id`, `deleted`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_payment_type` (`payment_type`),
  KEY `idx_payment_date` (`payment_date`),
  KEY `idx_voucher_status` (`voucher_status`),
  KEY `idx_operator_id` (`operator_id`),
  KEY `idx_tenant_id_deleted` (`tenant_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='对公付款凭证表';
```

### 4.8 预约单表 (`waste_transfer_appointment`) - 恢复核心预约功能

```sql
CREATE TABLE `waste_transfer_appointment` (
  `id` bigint(20) NOT NULL COMMENT '预约单ID',
  `appointment_no` varchar(64) NOT NULL COMMENT '预约单号 (系统生成, 唯一)',
  `producing_enterprise_id` bigint(20) NOT NULL COMMENT '产废企业ID (关联 enterprise_info 表)',
  `producing_store_id` bigint(20) DEFAULT NULL COMMENT '产废门店ID (关联 enterprise_store 表, 若适用)',
  `producing_contact_name` varchar(64) NOT NULL COMMENT '产废方联系人',
  `producing_contact_phone` varchar(20) NOT NULL COMMENT '产废方联系电话',
  `producing_address_detail` varchar(255) NOT NULL COMMENT '产废方详细地址',
  `waste_code` varchar(50) NOT NULL COMMENT '危险废物代码 (国标)',
  `waste_name` varchar(100) NOT NULL COMMENT '危险废物名称',
  `waste_category_id` bigint(20) DEFAULT NULL COMMENT '废物类别ID (关联字典或废物分类表)',
  `estimated_quantity` decimal(10,2) NOT NULL COMMENT '预估数量',
  `quantity_unit` varchar(10) NOT NULL COMMENT '数量单位 (如:吨,千克,桶)',
  `packaging_type` varchar(50) DEFAULT '' COMMENT '包装方式',
  `expected_collection_time_start` datetime NOT NULL COMMENT '期望上门收集开始时间',
  `expected_collection_time_end` datetime NOT NULL COMMENT '期望上门收集结束时间',
  `assigned_recycling_enterprise_id` bigint(20) DEFAULT NULL COMMENT '已指派/选定的回收企业ID (关联 enterprise_info 表)',
  `assignment_method` tinyint(4) DEFAULT '0' COMMENT '回收方分配方式 (0:未分配, 1:用户选择, 2:系统规则分配, 3:管理员手动指定)',
  `status` tinyint(4) NOT NULL DEFAULT '0' COMMENT '预约状态 (0:待处理, 1:待回收方确认, 2:回收方已确认/待生成订单, 3:回收方已拒绝, 4:已取消, 5:已生成订单)',
  `user_remark` varchar(255) DEFAULT '' COMMENT '用户备注',
  `rejection_reason` varchar(255) DEFAULT '' COMMENT '回收方拒绝原因',
  `cancellation_reason` varchar(255) DEFAULT '' COMMENT '用户/管理员取消原因',
  `tenant_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '租户ID',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者 (产废企业用户ID)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_appointment_no_tenant_id` (`appointment_no`, `tenant_id`, `deleted`),
  KEY `idx_producing_enterprise_id` (`producing_enterprise_id`),
  KEY `idx_assigned_recycling_enterprise_id` (`assigned_recycling_enterprise_id`),
  KEY `idx_status` (`status`),
  KEY `idx_expected_collection_time_start` (`expected_collection_time_start`),
  KEY `idx_tenant_id_deleted_status` (`tenant_id`, `deleted`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='危废转移预约单';
```

### 4.9 预约报价记录表 (`waste_appointment_quotation`) - 恢复报价功能

```sql
CREATE TABLE `waste_appointment_quotation` (
  `id` bigint(20) NOT NULL COMMENT '报价记录ID',
  `appointment_id` bigint(20) NOT NULL COMMENT '预约单ID',
  `recycling_enterprise_id` bigint(20) NOT NULL COMMENT '回收企业ID',
  `quoted_price` decimal(10,2) NOT NULL COMMENT '报价金额',
  `price_unit` varchar(20) NOT NULL COMMENT '价格单位',
  `total_amount` decimal(12,2) NOT NULL COMMENT '总金额 (报价*数量)',
  `quotation_remark` varchar(500) DEFAULT '' COMMENT '报价说明 (如:包含运费,质量要求等)',
  `valid_until` datetime NOT NULL COMMENT '报价有效期至',
  `status` tinyint(4) NOT NULL DEFAULT '0' COMMENT '报价状态 (0:待确认, 1:已被接受, 2:已被拒绝, 3:已失效)',
  `accepted_time` datetime DEFAULT NULL COMMENT '接受时间',
  `rejection_reason` varchar(255) DEFAULT '' COMMENT '拒绝原因',
  `contract_id` bigint(20) DEFAULT NULL COMMENT '关联生成的合同ID',
  `tenant_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '租户ID',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者 (回收企业用户)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_appointment_id` (`appointment_id`),
  KEY `idx_recycling_enterprise_id` (`recycling_enterprise_id`),
  KEY `idx_status` (`status`),
  KEY `idx_valid_until` (`valid_until`),
  KEY `idx_tenant_id_deleted_status` (`tenant_id`, `deleted`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='预约报价记录表';
```

### 4.10 回收企业分配规则表 (`waste_recycler_assignment_rule`) - 恢复自动分配功能

```sql
CREATE TABLE `waste_recycler_assignment_rule` (
  `id` bigint(20) NOT NULL COMMENT '规则ID',
  `rule_name` varchar(100) NOT NULL COMMENT '规则名称',
  `priority` int(11) NOT NULL DEFAULT '0' COMMENT '规则优先级 (数字越小优先级越高)',
  `is_enabled` bit(1) NOT NULL DEFAULT b'1' COMMENT '是否启用 (0:否, 1:是)',
  `match_province_code` varchar(20) DEFAULT '' COMMENT '匹配条件: 产废方省编码',
  `match_city_code` varchar(20) DEFAULT '' COMMENT '匹配条件: 产废方市编码',
  `match_district_code` varchar(20) DEFAULT '' COMMENT '匹配条件: 产废方区编码',
  `match_waste_category_id` bigint(20) DEFAULT NULL COMMENT '匹配条件: 废物类别ID',
  `assigned_recycling_enterprise_id` bigint(20) NOT NULL COMMENT '分配的回收企业ID',
  `description` varchar(255) DEFAULT '' COMMENT '规则描述',
  `tenant_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '租户ID',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_priority` (`priority`),
  KEY `idx_is_enabled` (`is_enabled`),
  KEY `idx_assigned_recycling_enterprise_id` (`assigned_recycling_enterprise_id`),
  KEY `idx_tenant_id_deleted_enabled` (`tenant_id`, `deleted`, `is_enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='回收企业分配规则表';
```

### 4.11 市场价格基准表 (`waste_price_benchmark`) - 恢复价格参考功能

```sql
CREATE TABLE `waste_price_benchmark` (
  `id` bigint(20) NOT NULL COMMENT '价格基准ID',
  `waste_code` varchar(50) NOT NULL COMMENT '危险废物代码',
  `waste_name` varchar(100) NOT NULL COMMENT '危险废物名称',
  `price` decimal(10,2) NOT NULL COMMENT '基准价格',
  `price_unit` varchar(20) NOT NULL COMMENT '价格单位 (如:元/桶,元/吨,元/千克)',
  `region_code` varchar(20) DEFAULT '' COMMENT '适用地区编码 (空为全国通用)',
  `region_name` varchar(100) DEFAULT '' COMMENT '适用地区名称',
  `effective_date` date NOT NULL COMMENT '生效日期',
  `expire_date` date DEFAULT NULL COMMENT '失效日期',
  `price_source` varchar(100) DEFAULT '' COMMENT '价格来源 (如:市场调研,政府指导价等)',
  `status` tinyint(4) NOT NULL DEFAULT '1' COMMENT '状态 (0:草稿, 1:生效中, 2:已失效)',
  `remark` varchar(255) DEFAULT '' COMMENT '备注说明',
  `tenant_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '租户ID',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_waste_code` (`waste_code`),
  KEY `idx_region_code` (`region_code`),
  KEY `idx_status_effective_date` (`status`, `effective_date`),
  KEY `idx_tenant_id_deleted_status` (`tenant_id`, `deleted`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='危险废物市场价格基准表';
```

### 4.12 回收企业报价配置表 (`waste_recycler_price_config`) - 恢复价格配置功能

```sql
CREATE TABLE `waste_recycler_price_config` (
  `id` bigint(20) NOT NULL COMMENT '价格配置ID',
  `recycling_enterprise_id` bigint(20) NOT NULL COMMENT '回收企业ID',
  `waste_code` varchar(50) NOT NULL COMMENT '危险废物代码',
  `waste_name` varchar(100) NOT NULL COMMENT '危险废物名称',
  `purchase_price` decimal(10,2) NOT NULL COMMENT '收购价格',
  `price_unit` varchar(20) NOT NULL COMMENT '价格单位',
  `min_quantity` decimal(10,2) DEFAULT NULL COMMENT '最小收购数量',
  `max_quantity` decimal(10,2) DEFAULT NULL COMMENT '最大收购数量',
  `region_code` varchar(20) DEFAULT '' COMMENT '服务地区编码',
  `region_name` varchar(100) DEFAULT '' COMMENT '服务地区名称',
  `effective_date` date NOT NULL COMMENT '生效日期',
  `expire_date` date DEFAULT NULL COMMENT '失效日期',
  `is_negotiable` bit(1) DEFAULT b'0' COMMENT '是否可议价 (0:固定价格, 1:可议价)',
  `status` tinyint(4) NOT NULL DEFAULT '1' COMMENT '状态 (0:停用, 1:启用)',
  `remark` varchar(255) DEFAULT '' COMMENT '备注说明',
  `tenant_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '租户ID',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_recycler_waste_region` (`recycling_enterprise_id`, `waste_code`, `region_code`, `deleted`),
  KEY `idx_waste_code` (`waste_code`),
  KEY `idx_region_code` (`region_code`),
  KEY `idx_status_effective_date` (`status`, `effective_date`),
  KEY `idx_tenant_id_deleted_status` (`tenant_id`, `deleted`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='回收企业价格配置表';
```

### 4.13 客户专属价格配置表 (`waste_recycler_customer_price`) - 恢复VIP定价功能

```sql
CREATE TABLE `waste_recycler_customer_price` (
  `id` bigint(20) NOT NULL COMMENT '客户专属价格ID',
  `recycling_enterprise_id` bigint(20) NOT NULL COMMENT '回收企业ID',
  `customer_enterprise_id` bigint(20) NOT NULL COMMENT '客户企业ID (产废企业)',
  `waste_code` varchar(50) NOT NULL COMMENT '危险废物代码',
  `waste_name` varchar(100) NOT NULL COMMENT '危险废物名称',
  `price_type` tinyint(4) NOT NULL DEFAULT '1' COMMENT '价格类型 (1:VIP专属价格, 2:合同约定价格, 3:批量优惠价格)',
  `special_price` decimal(10,2) NOT NULL COMMENT '专属价格',
  `price_unit` varchar(20) NOT NULL COMMENT '价格单位',
  `min_quantity` decimal(10,2) DEFAULT NULL COMMENT '最小适用数量',
  `max_quantity` decimal(10,2) DEFAULT NULL COMMENT '最大适用数量',
  `effective_date` date NOT NULL COMMENT '生效日期',
  `expire_date` date DEFAULT NULL COMMENT '失效日期',
  `contract_id` bigint(20) DEFAULT NULL COMMENT '关联合同ID (如果是合同约定价格)',
  `price_advantage_desc` varchar(200) DEFAULT '' COMMENT '价格优势描述 (如:比基础价格优惠5%)',
  `status` tinyint(4) NOT NULL DEFAULT '1' COMMENT '状态 (0:停用, 1:启用)',
  `remark` varchar(255) DEFAULT '' COMMENT '备注说明',
  `tenant_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '租户ID',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_recycler_customer_waste` (`recycling_enterprise_id`, `customer_enterprise_id`, `waste_code`, `deleted`),
  KEY `idx_customer_enterprise_id` (`customer_enterprise_id`),
  KEY `idx_waste_code` (`waste_code`),
  KEY `idx_price_type` (`price_type`),
  KEY `idx_status_effective_date` (`status`, `effective_date`),
  KEY `idx_tenant_id_deleted_status` (`tenant_id`, `deleted`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='回收企业客户专属价格配置表';
```

### 4.14 回收企业业务模式配置表 (`waste_recycler_business_config`) - 恢复业务模式管理

```sql
CREATE TABLE `waste_recycler_business_config` (
  `id` bigint(20) NOT NULL COMMENT '配置ID',
  `recycling_enterprise_id` bigint(20) NOT NULL COMMENT '回收企业ID',
  `business_mode` tinyint(4) NOT NULL DEFAULT '1' COMMENT '业务模式 (1:独立运营模式, 2:平台竞价模式, 3:混合模式)',
  `default_quotation_mode` tinyint(4) NOT NULL DEFAULT '1' COMMENT '默认报价模式 (1:直接报价, 2:竞价)',
  `allow_client_mode_selection` bit(1) DEFAULT b'0' COMMENT '是否允许产废企业选择交易模式 (0:否, 1:是)',
  `quotation_timeout_hours` int(11) DEFAULT '24' COMMENT '竞价模式下报价超时时间(小时)',
  `auto_accept_single_quotation` bit(1) DEFAULT b'0' COMMENT '单一报价时是否自动接受 (0:否, 1:是)',
  `enable_price_negotiation` bit(1) DEFAULT b'0' COMMENT '是否启用价格协商 (0:否, 1:是)',
  `is_enabled` bit(1) NOT NULL DEFAULT b'1' COMMENT '配置是否启用 (0:否, 1:是)',
  `remark` varchar(255) DEFAULT '' COMMENT '配置说明',
  `tenant_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '租户ID',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_recycling_enterprise_id` (`recycling_enterprise_id`, `deleted`),
  KEY `idx_business_mode` (`business_mode`),
  KEY `idx_is_enabled` (`is_enabled`),
  KEY `idx_tenant_id_deleted_enabled` (`tenant_id`, `deleted`, `is_enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='回收企业业务模式配置表';
```

### 4.15 价格调整记录表 (`waste_order_price_adjustment`) - 恢复价格调整功能

```sql
CREATE TABLE `waste_order_price_adjustment` (
  `id` bigint(20) NOT NULL COMMENT '价格调整记录ID',
  `order_id` bigint(20) NOT NULL COMMENT '订单ID',
  `adjustment_type` tinyint(4) NOT NULL COMMENT '调整类型 (1:数量差异调整, 2:质量差异调整, 3:其他调整)',
  `original_price` decimal(10,2) NOT NULL COMMENT '原始单价',
  `adjusted_price` decimal(10,2) NOT NULL COMMENT '调整后单价',
  `original_quantity` decimal(10,2) NOT NULL COMMENT '原始数量',
  `adjusted_quantity` decimal(10,2) NOT NULL COMMENT '调整后数量',
  `original_amount` decimal(12,2) NOT NULL COMMENT '原始总金额',
  `adjusted_amount` decimal(12,2) NOT NULL COMMENT '调整后总金额',
  `adjustment_amount` decimal(12,2) NOT NULL COMMENT '调整金额 (正数为增加,负数为减少)',
  `adjustment_reason` varchar(500) NOT NULL COMMENT '调整原因',
  `status` tinyint(4) NOT NULL DEFAULT '0' COMMENT '调整状态 (0:待确认, 1:已确认, 2:已拒绝)',
  `confirmed_by` varchar(64) DEFAULT '' COMMENT '确认人',
  `confirmed_time` datetime DEFAULT NULL COMMENT '确认时间',
  `related_weighing_id` bigint(20) DEFAULT NULL COMMENT '关联的过磅记录ID',
  `tenant_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '租户ID',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_status` (`status`),
  KEY `idx_related_weighing_id` (`related_weighing_id`),
  KEY `idx_tenant_id_deleted_status` (`tenant_id`, `deleted`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='订单价格调整记录表';
```

## 5. 核心业务逻辑实现

### 5.1 收货确认与付款配置获取

```sql
-- 收运员确认收货时首先获取产废企业的付款配置
SELECT 
  pc.payment_method,
  pc.personal_payee_name,
  pc.personal_payee_phone,
  pc.personal_bank_account,
  pc.auto_payment_enabled,
  pc.payment_delay_hours,
  CASE 
    WHEN pc.payment_method = 1 THEN '对公结算'
    WHEN pc.payment_method = 2 AND pc.personal_payee_name != '' THEN '个人结算-已配置收款人'
    WHEN pc.payment_method = 2 AND pc.personal_payee_name = '' THEN '个人结算-需填写收款人'
    ELSE '未配置'
  END as payment_config_status
FROM waste_producer_payment_config pc
WHERE pc.producing_enterprise_id = #{producingEnterpriseId}
  AND pc.is_default_config = 1
  AND pc.config_status = 1;

-- 收运员确认收货的业务逻辑
UPDATE waste_transfer_order 
SET status = 3, -- 已确认收货
    pickup_confirmed_time = NOW(),
    pickup_confirmed_by = #{driverId},
    pickup_confirmed_location = #{gpsLocation},
    payment_config_id = #{paymentConfigId},
    payment_method_type = #{paymentMethodType}, -- 从配置获取：1对公/2个人
    confirmed_quantity = #{confirmedQuantity},
    -- 仅当个人结算且现场选择了付款方式时更新以下字段
    onsite_payment_choice = CASE WHEN #{paymentMethodType} = 2 THEN #{onsiteChoice} ELSE NULL END,
    onsite_cash_amount = CASE WHEN #{onsiteChoice} = 1 THEN #{cashAmount} ELSE NULL END,
    payment_status = CASE 
        WHEN #{paymentMethodType} = 1 THEN 3 -- 对公结算，待凭证上传
        WHEN #{paymentMethodType} = 2 AND #{onsiteChoice} = 1 THEN 1 -- 个人现金已付
        WHEN #{paymentMethodType} = 2 AND #{onsiteChoice} = 2 THEN 1 -- 个人立即付款
        WHEN #{paymentMethodType} = 2 AND #{onsiteChoice} = 3 THEN 0 -- 个人待付款
        ELSE 0
    END
WHERE id = #{orderId} AND status = 2;

-- 如果是个人结算且选择现金已付，插入现金付款记录
INSERT INTO waste_cash_payment_record (
  order_id, transport_task_id, payment_amount, payment_time,
  payer_driver_id, payer_name, payee_name, payee_phone, payment_location
) 
SELECT #{orderId}, #{transportTaskId}, #{cashAmount}, NOW(),
       #{driverId}, #{driverName}, #{payeeName}, #{payeePhone}, #{gpsLocation}
WHERE #{paymentMethodType} = 2 AND #{onsiteChoice} = 1;
```

### 5.2 对公付款凭证上传

```sql
-- 回收企业财务上传对公付款凭证
INSERT INTO waste_company_payment_voucher (
  order_id, voucher_no, payment_type, payment_amount, payment_date,
  payment_bank, payment_account, payee_bank, payee_account, payee_name,
  transaction_no, transfer_voucher_url, operator_id, operator_name
) VALUES (
  #{orderId}, #{voucherNo}, #{paymentType}, #{paymentAmount}, #{paymentDate},
  #{paymentBank}, #{paymentAccount}, #{payeeBank}, #{payeeAccount}, #{payeeName},
  #{transactionNo}, #{voucherUrl}, #{operatorId}, #{operatorName}
);

-- 更新订单付款状态
UPDATE waste_transfer_order 
SET payment_status = 4, -- 凭证已上传
    payment_voucher_id = #{voucherId},
    payment_completed_time = NOW()
WHERE id = #{orderId} AND payment_method_type = 1;

-- 产废企业确认对公付款凭证
UPDATE waste_company_payment_voucher 
SET voucher_status = 1, -- 已确认
    confirmed_by_producer = 1,
    producer_confirm_time = NOW()
WHERE id = #{voucherId};

-- 同时更新订单状态
UPDATE waste_transfer_order 
SET payment_status = 5 -- 凭证已确认
WHERE payment_voucher_id = #{voucherId};
```

### 5.3 个人收款人信息补充

```sql
-- 当个人结算但未预设收款人时，回收企业财务填写收款人信息
UPDATE waste_transfer_order 
SET personal_payee_info = JSON_OBJECT(
      'payee_name', #{payeeName},
      'payee_phone', #{payeePhone},
      'payee_id_card', #{payeeIdCard},
      'bank_name', #{bankName},
      'bank_account', #{bankAccount},
      'relationship', #{relationship},
      'filled_by', #{operatorId},
      'filled_time', NOW()
    ),
    payment_status = CASE 
      WHEN onsite_payment_choice IS NULL THEN 0 -- 待付款
      ELSE payment_status -- 保持现状
    END
WHERE id = #{orderId} 
  AND payment_method_type = 2 
  AND personal_payee_info IS NULL;

-- 查询需要补充收款人信息的订单
SELECT 
  o.id,
  o.order_no,
  o.producing_enterprise_id,
  o.estimated_amount,
  o.final_amount,
  o.onsite_payment_choice,
  o.payment_status,
  CASE 
    WHEN o.personal_payee_info IS NULL THEN '需要填写收款人信息'
    WHEN o.onsite_payment_choice IS NULL THEN '需要确定付款方式'
    ELSE '信息完整'
  END as action_required
FROM waste_transfer_order o
WHERE o.payment_method_type = 2 
  AND o.status = 3 
  AND (o.personal_payee_info IS NULL OR o.onsite_payment_choice IS NULL)
ORDER BY o.pickup_confirmed_time;
```

### 5.4 车辆过磅与核准逻辑

```sql
-- 1. 创建车辆过磅记录
INSERT INTO waste_vehicle_weighing_record (
  weighing_no, transport_task_id, vehicle_id, driver_id, recycling_enterprise_id,
  departure_time, estimated_total_quantity, planned_collection_count,
  actual_collection_count, confirmed_total_quantity
) VALUES (
  #{weighingNo}, #{transportTaskId}, #{vehicleId}, #{driverId}, #{recyclingEnterpriseId},
  #{departureTime}, #{estimatedTotal}, #{plannedCount},
  #{actualCount}, #{confirmedTotal}
);

-- 2. 完成过磅，更新过磅数据
UPDATE waste_vehicle_weighing_record 
SET weighing_time = NOW(),
    tare_weight = #{tareWeight},
    gross_weight = #{grossWeight},
    net_weight = #{netWeight},
    actual_quantity = #{actualQuantity},
    variance_estimate_vs_confirmed = (confirmed_total_quantity - estimated_total_quantity),
    variance_confirmed_vs_actual = (#{actualQuantity} - confirmed_total_quantity),
    variance_estimate_vs_actual = (#{actualQuantity} - estimated_total_quantity),
    total_variance_rate = ((#{actualQuantity} - estimated_total_quantity) / estimated_total_quantity * 100),
    weighing_status = 1, -- 过磅完成
    weighing_operator = #{operatorId}
WHERE id = #{weighingId};

-- 3. 更新相关订单状态为"车辆已过磅"
UPDATE waste_transfer_order 
SET status = 5, -- 车辆已过磅
    vehicle_weighing_id = #{weighingId}
WHERE current_transport_task_id = #{transportTaskId} 
  AND status = 4; -- 待车辆过磅
```

### 5.5 订单分摊逻辑

```sql
-- 自动按比例分摊车辆总重量到各个订单
-- 1. 获取该车辆的所有订单和过磅信息
SELECT o.id, o.estimated_quantity, o.confirmed_quantity, w.actual_quantity as vehicle_actual_quantity
FROM waste_transfer_order o
JOIN waste_vehicle_weighing_record w ON o.vehicle_weighing_id = w.id
WHERE w.id = #{weighingId} AND o.status = 5;

-- 2. 计算分摊比例和分摊量
-- 分摊比例 = 订单预估量 / 所有订单预估量总和
-- 分摊量 = 车辆实际过磅量 * 分摊比例

-- 3. 插入分摊记录
INSERT INTO waste_order_allocation_record (
  order_id, vehicle_weighing_id, allocation_method,
  estimated_quantity, confirmed_quantity, allocation_ratio, allocated_quantity,
  unit_price, estimated_amount, allocated_amount, amount_adjustment,
  allocation_operator
) VALUES (
  #{orderId}, #{weighingId}, 1, -- 按预估量比例分摊
  #{estimatedQuantity}, #{confirmedQuantity}, #{allocationRatio}, #{allocatedQuantity},
  #{unitPrice}, #{estimatedAmount}, #{allocatedAmount}, #{amountAdjustment},
  #{operatorId}
);

-- 4. 更新订单的分摊数量和最终金额
UPDATE waste_transfer_order 
SET allocated_quantity = #{allocatedQuantity},
    final_amount = #{allocatedAmount},
    allocation_ratio = #{allocationRatio},
    allocation_completed_time = NOW(),
    variance_from_estimate = (#{allocatedQuantity} - estimated_quantity),
    variance_rate = ((#{allocatedQuantity} - estimated_quantity) / estimated_quantity * 100),
    status = 6 -- 订单分摊完成
WHERE id = #{orderId};

-- 5. 更新车辆过磅记录的分摊状态
UPDATE waste_vehicle_weighing_record 
SET allocation_status = 1 -- 分摊完成
WHERE id = #{weighingId};
```

### 5.6 三重核准查询

```sql
-- 车辆级别的三重核准对比查询
SELECT 
  w.weighing_no,
  w.vehicle_id,
  w.departure_time,
  w.estimated_total_quantity as "出车预估量",
  w.confirmed_total_quantity as "收运员确认量",
  w.actual_quantity as "实际过磅量",
  w.variance_estimate_vs_confirmed as "预估vs确认差异",
  w.variance_confirmed_vs_actual as "确认vs过磅差异", 
  w.variance_estimate_vs_actual as "预估vs过磅差异",
  w.total_variance_rate as "总体差异率(%)",
  COUNT(o.id) as "实际收集订单数",
  w.planned_collection_count as "计划收集订单数"
FROM waste_vehicle_weighing_record w
LEFT JOIN waste_transfer_order o ON w.id = o.vehicle_weighing_id
WHERE w.weighing_time >= DATE_SUB(NOW(), INTERVAL 7 DAY)
GROUP BY w.id
ORDER BY w.weighing_time DESC;

-- 订单级别的分摊明细查询
SELECT 
  o.order_no,
  o.estimated_quantity as "订单预估量",
  o.confirmed_quantity as "收运员确认量",
  o.allocated_quantity as "分摊后量",
  o.variance_from_estimate as "分摊差异",
  o.variance_rate as "差异率(%)",
  o.estimated_amount as "预估金额",
  o.final_amount as "最终金额",
  a.allocation_ratio as "分摊比例",
  a.amount_adjustment as "金额调整"
FROM waste_transfer_order o
LEFT JOIN waste_order_allocation_record a ON o.id = a.order_id
WHERE o.vehicle_weighing_id = #{weighingId}
ORDER BY a.allocation_ratio DESC;
```

## 6. 业务场景示例

### 6.1 完整的一车多单收运流程

```sql
-- 场景：一辆车收集3个订单，总计5吨废矿物油

-- 1. 出车时记录预估信息
-- 订单A: 2吨，订单B: 1.5吨，订单C: 1.5吨，总计5吨

-- 2. 收运过程中，收运员依次确认收货
-- 订单A: 确认收到2.1吨，选择"立即付款"
-- 订单B: 确认收到1.4吨，选择"现金已付"，现金800元
-- 订单C: 确认收到1.6吨，选择"待付款"

-- 3. 车辆回到回收企业过磅
-- 实际过磅：净重4.9吨

-- 4. 系统自动分摊
-- 预估总量: 5吨，实际过磅: 4.9吨，总差异: -2%
-- 订单A分摊: 4.9 * (2/5) = 1.96吨，差异-2%
-- 订单B分摊: 4.9 * (1.5/5) = 1.47吨，差异-2%  
-- 订单C分摊: 4.9 * (1.5/5) = 1.47吨，差异-2%

-- 5. 最终结算
-- 订单A: 1.96吨 * 500元/吨 = 980元 (已线上支付)
-- 订单B: 1.47吨 * 500元/吨 = 735元 (已现金支付800元，多付65元需退还)
-- 订单C: 1.47吨 * 500元/吨 = 735元 (待管理员确认付款)
```

### 6.2 异常情况处理

```sql
-- 场景1: 过磅差异过大 (>10%)
-- 触发人工审核，暂停自动分摊
UPDATE waste_vehicle_weighing_record 
SET weighing_status = 3, -- 异常状态
    allocation_status = 0 -- 暂停分摊
WHERE id = #{weighingId} AND ABS(total_variance_rate) > 10;

-- 场景2: 现金支付金额与最终分摊不符
-- 生成对账差异记录，需要人工处理
SELECT 
  o.order_no,
  c.payment_amount as "现金支付金额",
  o.final_amount as "最终应付金额",
  (c.payment_amount - o.final_amount) as "差额"
FROM waste_transfer_order o
JOIN waste_cash_payment_record c ON o.id = c.order_id
WHERE o.pickup_payment_method = 1 
  AND ABS(c.payment_amount - o.final_amount) > 10; -- 差额超过10元
```

## 7. 核心优势总结

这种重新设计的方案确保了：

- ✅ **符合液体危废特性**：车辆级收集和过磅，而非单个订单过磅
- ✅ **灵活的付款方式**：现场支持现金已付/立即付款/待付款三种选择
- ✅ **三重核准机制**：预估量 vs 确认量 vs 过磅量的完整对比
- ✅ **公平的分摊算法**：基于预估量比例分摊车辆总重量
- ✅ **完整的对账支持**：现金支付与最终分摊的差异处理
- ✅ **异常处理机制**：过大差异触发人工审核，确保数据准确性
- ✅ **业务流程清晰**：从收货确认到过磅分摊的完整状态流转
- ✅ **数据可追溯**：每个环节都有详细记录和时间戳

通过这种设计，系统能够准确反映液体废矿物油收运的真实业务流程，确保各方责任明确，结算公平合理。