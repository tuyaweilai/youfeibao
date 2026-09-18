-- 危险废物转移模块价格管理表生成脚本（修复版）
-- 基于 waste_transfer_database_design.md 设计文档
-- 执行日期：2024-12-02

-- ========================================
-- 价格管理相关表
-- ========================================

-- 1. 市场价格基准表
CREATE TABLE IF NOT EXISTS `waste_price_benchmark` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '价格基准ID',
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

-- 2. 回收企业报价配置表
CREATE TABLE IF NOT EXISTS `waste_recycler_price_config` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '价格配置ID',
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

-- 3. 客户专属价格配置表
CREATE TABLE IF NOT EXISTS `waste_recycler_customer_price` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '客户专属价格ID',
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

-- 4. 回收企业业务模式配置表
CREATE TABLE IF NOT EXISTS `waste_recycler_business_config` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '配置ID',
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

-- 5. 价格调整记录表
CREATE TABLE IF NOT EXISTS `waste_order_price_adjustment` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '价格调整记录ID',
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

-- 验证新创建的表
SELECT 
    TABLE_NAME as '表名',
    TABLE_COMMENT as '表注释',
    CREATE_TIME as '创建时间'
FROM information_schema.TABLES 
WHERE TABLE_SCHEMA = DATABASE() 
    AND TABLE_NAME LIKE 'waste_%'
    AND CREATE_TIME >= DATE_SUB(NOW(), INTERVAL 1 MINUTE)
ORDER BY TABLE_NAME; 