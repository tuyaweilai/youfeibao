-- 危废转移预约管理模块数据库表结构脚本
-- 创建日期：2024年12月

-- 创建危废转移预约表
CREATE TABLE IF NOT EXISTS `waste_transfer_appointment` (
  `id` bigint(20) NOT NULL COMMENT '预约ID',
  `appointment_no` varchar(64) NOT NULL COMMENT '预约单号',
  
  -- 产废企业信息
  `producer_enterprise_id` bigint(20) NOT NULL COMMENT '产废企业ID',
  `producer_enterprise_name` varchar(100) NOT NULL COMMENT '产废企业名称',
  `producer_contact_name` varchar(64) NOT NULL COMMENT '产废企业联系人',
  `producer_contact_phone` varchar(20) NOT NULL COMMENT '产废企业联系电话',
  
  -- 回收企业信息
  `recycler_enterprise_id` bigint(20) DEFAULT NULL COMMENT '回收企业ID',
  `recycler_enterprise_name` varchar(100) DEFAULT NULL COMMENT '回收企业名称',
  `assignment_type` tinyint(4) NOT NULL DEFAULT '0' COMMENT '分配方式(0:自动分配,1:手动指定)',
  `assignment_time` datetime DEFAULT NULL COMMENT '分配时间',
  `assignment_operator` varchar(64) DEFAULT NULL COMMENT '分配操作人',
  
  -- 废物信息
  `waste_code` varchar(50) NOT NULL COMMENT '危险废物代码',
  `waste_name` varchar(100) NOT NULL COMMENT '危险废物名称',
  `waste_category` varchar(50) NOT NULL COMMENT '废物类别',
  `estimated_quantity` decimal(10,2) NOT NULL COMMENT '预估数量',
  `quantity_unit` varchar(10) NOT NULL COMMENT '数量单位',
  `waste_description` varchar(500) DEFAULT '' COMMENT '废物描述',
  
  -- 地址信息
  `pickup_address` varchar(500) NOT NULL COMMENT '取货地址',
  `pickup_latitude` decimal(10,7) DEFAULT NULL COMMENT '取货地址纬度',
  `pickup_longitude` decimal(10,7) DEFAULT NULL COMMENT '取货地址经度',
  `delivery_address` varchar(500) DEFAULT NULL COMMENT '送货地址',
  `delivery_latitude` decimal(10,7) DEFAULT NULL COMMENT '送货地址纬度',
  `delivery_longitude` decimal(10,7) DEFAULT NULL COMMENT '送货地址经度',
  
  -- 时间信息
  `expected_pickup_time` datetime NOT NULL COMMENT '期望取货时间',
  `expected_delivery_time` datetime DEFAULT NULL COMMENT '期望送达时间',
  
  -- 状态信息
  `appointment_status` tinyint(4) NOT NULL DEFAULT '0' COMMENT '预约状态(0:待处理,1:待回收方确认,2:已确认,3:已拒绝,4:已生成订单,5:已取消)',
  `confirm_time` datetime DEFAULT NULL COMMENT '确认时间',
  `reject_time` datetime DEFAULT NULL COMMENT '拒绝时间',
  `reject_reason` varchar(500) DEFAULT '' COMMENT '拒绝原因',
  `cancel_time` datetime DEFAULT NULL COMMENT '取消时间',
  `cancel_reason` varchar(500) DEFAULT '' COMMENT '取消原因',
  
  -- 业务信息
  `business_mode` tinyint(4) NOT NULL DEFAULT '0' COMMENT '业务模式(0:独立运营,1:平台竞价,2:混合模式)',
  `is_urgent` bit(1) DEFAULT b'0' COMMENT '是否紧急',
  `priority_level` tinyint(4) DEFAULT '0' COMMENT '优先级(0:普通,1:高,2:紧急)',
  
  -- 关联信息
  `order_id` bigint(20) DEFAULT NULL COMMENT '关联订单ID',
  `contract_id` bigint(20) DEFAULT NULL COMMENT '关联合同ID',
  
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `tenant_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '租户ID',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_appointment_no` (`appointment_no`),
  KEY `idx_producer_enterprise_id` (`producer_enterprise_id`),
  KEY `idx_recycler_enterprise_id` (`recycler_enterprise_id`),
  KEY `idx_appointment_status` (`appointment_status`),
  KEY `idx_waste_code` (`waste_code`),
  KEY `idx_expected_pickup_time` (`expected_pickup_time`),
  KEY `idx_tenant_id_deleted` (`tenant_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='危废转移预约表';

-- 创建回收企业分配规则表
CREATE TABLE IF NOT EXISTS `waste_recycler_assignment_rule` (
  `id` bigint(20) NOT NULL COMMENT '规则ID',
  `rule_name` varchar(100) NOT NULL COMMENT '规则名称',
  `rule_type` tinyint(4) NOT NULL COMMENT '规则类型(0:区域匹配,1:废物类型匹配,2:负载均衡,3:默认分配)',
  `priority` int(11) NOT NULL DEFAULT '0' COMMENT '优先级(数值越大优先级越高)',
  `is_enabled` bit(1) NOT NULL DEFAULT b'1' COMMENT '是否启用',
  
  -- 规则条件
  `waste_codes` text COMMENT '适用的废物代码(JSON数组)',
  `waste_categories` text COMMENT '适用的废物类别(JSON数组)',
  `regions` text COMMENT '适用的区域(JSON数组)',
  `min_quantity` decimal(10,2) DEFAULT NULL COMMENT '最小数量限制',
  `max_quantity` decimal(10,2) DEFAULT NULL COMMENT '最大数量限制',
  
  -- 分配目标
  `target_recycler_ids` text COMMENT '目标回收企业ID列表(JSON数组)',
  `assignment_strategy` tinyint(4) NOT NULL DEFAULT '0' COMMENT '分配策略(0:轮询,1:随机,2:负载最小,3:距离最近)',
  
  -- 规则配置
  `rule_config` text COMMENT '规则配置(JSON对象)',
  `effective_start_time` datetime DEFAULT NULL COMMENT '生效开始时间',
  `effective_end_time` datetime DEFAULT NULL COMMENT '生效结束时间',
  
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `tenant_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '租户ID',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_rule_type` (`rule_type`),
  KEY `idx_priority` (`priority`),
  KEY `idx_is_enabled` (`is_enabled`),
  KEY `idx_tenant_id_deleted` (`tenant_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='回收企业分配规则表';

-- 创建预约分配历史表
CREATE TABLE IF NOT EXISTS `waste_appointment_assignment_history` (
  `id` bigint(20) NOT NULL COMMENT '历史记录ID',
  `appointment_id` bigint(20) NOT NULL COMMENT '预约ID',
  `appointment_no` varchar(64) NOT NULL COMMENT '预约单号',
  
  -- 分配信息
  `assignment_type` tinyint(4) NOT NULL COMMENT '分配方式(0:自动分配,1:手动指定)',
  `old_recycler_id` bigint(20) DEFAULT NULL COMMENT '原回收企业ID',
  `old_recycler_name` varchar(100) DEFAULT NULL COMMENT '原回收企业名称',
  `new_recycler_id` bigint(20) DEFAULT NULL COMMENT '新回收企业ID',
  `new_recycler_name` varchar(100) DEFAULT NULL COMMENT '新回收企业名称',
  
  -- 分配规则
  `applied_rule_id` bigint(20) DEFAULT NULL COMMENT '应用的分配规则ID',
  `applied_rule_name` varchar(100) DEFAULT NULL COMMENT '应用的分配规则名称',
  `assignment_reason` varchar(500) DEFAULT '' COMMENT '分配原因',
  
  -- 操作信息
  `operator_id` bigint(20) DEFAULT NULL COMMENT '操作人ID',
  `operator_name` varchar(64) DEFAULT NULL COMMENT '操作人姓名',
  `operation_time` datetime NOT NULL COMMENT '操作时间',
  `operation_type` tinyint(4) NOT NULL COMMENT '操作类型(0:首次分配,1:重新分配,2:手动变更)',
  
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `tenant_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '租户ID',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_appointment_id` (`appointment_id`),
  KEY `idx_appointment_no` (`appointment_no`),
  KEY `idx_operation_time` (`operation_time`),
  KEY `idx_tenant_id_deleted` (`tenant_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预约分配历史表';

-- 创建预约状态变更日志表
CREATE TABLE IF NOT EXISTS `waste_appointment_status_log` (
  `id` bigint(20) NOT NULL COMMENT '日志ID',
  `appointment_id` bigint(20) NOT NULL COMMENT '预约ID',
  `appointment_no` varchar(64) NOT NULL COMMENT '预约单号',
  
  -- 状态变更
  `old_status` tinyint(4) DEFAULT NULL COMMENT '原状态',
  `new_status` tinyint(4) NOT NULL COMMENT '新状态',
  `status_change_time` datetime NOT NULL COMMENT '状态变更时间',
  `change_reason` varchar(500) DEFAULT '' COMMENT '变更原因',
  
  -- 操作信息
  `operator_id` bigint(20) DEFAULT NULL COMMENT '操作人ID',
  `operator_name` varchar(64) DEFAULT NULL COMMENT '操作人姓名',
  `operator_type` tinyint(4) NOT NULL COMMENT '操作人类型(0:系统,1:产废企业,2:回收企业,3:管理员)',
  
  -- 附加信息
  `additional_data` text COMMENT '附加数据(JSON对象)',
  
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `tenant_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '租户ID',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_appointment_id` (`appointment_id`),
  KEY `idx_appointment_no` (`appointment_no`),
  KEY `idx_status_change_time` (`status_change_time`),
  KEY `idx_tenant_id_deleted` (`tenant_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预约状态变更日志表'; 