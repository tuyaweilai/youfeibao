-- 危险废物转移模块数据库修复脚本
-- 基于评审报告：waste_transfer_database_design_review.md
-- 执行日期：2024-12-02
-- 说明：修复设计文档中发现的严重问题

-- ========================================
-- 1. 补充缺失的核心表
-- ========================================

-- 1.1 车辆过磅记录表（核心业务表）
CREATE TABLE IF NOT EXISTS `waste_vehicle_weighing_record` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '过磅记录ID',
  `weighing_no` varchar(64) NOT NULL COMMENT '过磅批次号',
  `transport_task_id` bigint(20) NOT NULL COMMENT '运输任务ID',
  `vehicle_id` bigint(20) NOT NULL COMMENT '车辆ID',
  `driver_id` bigint(20) NOT NULL COMMENT '司机ID',
  `recycling_enterprise_id` bigint(20) NOT NULL COMMENT '回收企业ID',
  
  -- 重量数据
  `tare_weight` decimal(10,2) DEFAULT NULL COMMENT '车辆皮重(kg)',
  `gross_weight` decimal(10,2) DEFAULT NULL COMMENT '车辆毛重(kg)', 
  `net_weight` decimal(10,2) DEFAULT NULL COMMENT '车辆净重(kg)',
  `actual_quantity` decimal(10,2) DEFAULT NULL COMMENT '实际过磅数量',
  
  -- 预估对比
  `estimated_total_quantity` decimal(10,2) NOT NULL COMMENT '出车预估总量',
  `confirmed_total_quantity` decimal(10,2) DEFAULT NULL COMMENT '收运员确认总量',
  `planned_collection_count` int(11) NOT NULL COMMENT '计划收集订单数',
  `actual_collection_count` int(11) DEFAULT NULL COMMENT '实际收集订单数',
  
  -- 差异分析
  `variance_estimate_vs_confirmed` decimal(10,2) DEFAULT NULL COMMENT '预估vs确认差异',
  `variance_confirmed_vs_actual` decimal(10,2) DEFAULT NULL COMMENT '确认vs过磅差异',
  `variance_estimate_vs_actual` decimal(10,2) DEFAULT NULL COMMENT '预估vs过磅差异',
  `total_variance_rate` decimal(5,2) DEFAULT NULL COMMENT '总体差异率(%)',
  
  -- 时间记录
  `departure_time` datetime NOT NULL COMMENT '出车时间',
  `weighing_time` datetime DEFAULT NULL COMMENT '过磅时间',
  
  -- 状态管理
  `weighing_status` tinyint(4) NOT NULL DEFAULT '0' COMMENT '过磅状态(0:未过磅,1:已过磅,2:异常,3:需人工审核)',
  `allocation_status` tinyint(4) NOT NULL DEFAULT '0' COMMENT '分摊状态(0:未分摊,1:已分摊,2:分摊异常)',
  `weighing_operator` varchar(64) DEFAULT '' COMMENT '过磅操作员',
  
  -- 公共字段
  `tenant_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '租户ID',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_weighing_no_tenant_id` (`weighing_no`, `tenant_id`, `deleted`),
  KEY `idx_transport_task_id` (`transport_task_id`),
  KEY `idx_vehicle_id` (`vehicle_id`),
  KEY `idx_driver_id` (`driver_id`),
  KEY `idx_recycling_enterprise_id` (`recycling_enterprise_id`),
  KEY `idx_weighing_status` (`weighing_status`),
  KEY `idx_allocation_status` (`allocation_status`),
  KEY `idx_weighing_time` (`weighing_time`),
  KEY `idx_tenant_id_deleted` (`tenant_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='车辆过磅记录表';

-- 1.2 订单过磅分摊记录表（补充）
CREATE TABLE IF NOT EXISTS `waste_order_allocation_record` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '订单过磅分摊记录ID',
  `order_id` bigint(20) NOT NULL COMMENT '订单ID',
  `transport_task_id` bigint(20) DEFAULT NULL COMMENT '运输任务ID',
  `vehicle_weighing_id` bigint(20) DEFAULT NULL COMMENT '关联车辆过磅记录ID',
  `allocation_time` datetime NOT NULL COMMENT '分摊时间',
  `allocation_method` varchar(50) DEFAULT NULL COMMENT '分摊方法',
  `allocation_quantity` decimal(10,2) DEFAULT NULL COMMENT '分摊数量',
  `allocation_status` tinyint(4) NOT NULL DEFAULT '0' COMMENT '分摊状态(0:未分摊,1:已分摊,2:分摊异常)',
  `allocation_operator` varchar(64) DEFAULT NULL COMMENT '分摊操作员',
  
  -- 公共字段
  `tenant_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '租户ID',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_transport_task_id` (`transport_task_id`),
  KEY `idx_vehicle_weighing_id` (`vehicle_weighing_id`),
  KEY `idx_allocation_time_method` (`allocation_time`, `allocation_method`),
  KEY `idx_allocation_status` (`allocation_status`),
  KEY `idx_tenant_id_deleted` (`tenant_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='订单过磅分摊记录表';

-- 1.3 付款状态变更历史表（补充）
CREATE TABLE IF NOT EXISTS `waste_payment_status_history` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '付款状态历史ID',
  `order_id` bigint(20) NOT NULL COMMENT '订单ID',
  `status_from` tinyint(4) DEFAULT NULL COMMENT '变更前状态',
  `status_to` tinyint(4) NOT NULL COMMENT '变更后状态',
  `status_name` varchar(50) NOT NULL COMMENT '状态名称',
  `change_reason` varchar(200) DEFAULT '' COMMENT '变更原因',
  `operator_type` tinyint(4) NOT NULL COMMENT '操作者类型 (1:产废企业, 2:回收企业, 3:司机, 4:系统自动, 5:平台管理员)',
  `operator_id` bigint(20) DEFAULT NULL COMMENT '操作者ID',
  `operator_name` varchar(64) DEFAULT '' COMMENT '操作者姓名',
  `change_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '变更时间',
  `payment_amount` decimal(12,2) DEFAULT NULL COMMENT '涉及金额',
  `payment_method` tinyint(4) DEFAULT NULL COMMENT '付款方式',
  `voucher_id` bigint(20) DEFAULT NULL COMMENT '关联凭证ID',
  `business_data` text DEFAULT NULL COMMENT '业务数据JSON',
  
  -- 公共字段
  `tenant_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '租户ID',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_status_to` (`status_to`),
  KEY `idx_operator_type_id` (`operator_type`, `operator_id`),
  KEY `idx_change_time` (`change_time`),
  KEY `idx_tenant_id_deleted` (`tenant_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='付款状态变更历史表';

-- 1.4 业务操作审计日志表（补充）
CREATE TABLE IF NOT EXISTS `waste_operation_audit_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '审计日志ID',
  `operation_type` varchar(50) NOT NULL COMMENT '操作类型 (CREATE/UPDATE/DELETE/CONFIRM/ALLOCATE等)',
  `target_table` varchar(50) NOT NULL COMMENT '目标表名',
  `target_id` bigint(20) NOT NULL COMMENT '目标记录ID',
  `target_no` varchar(64) DEFAULT NULL COMMENT '目标业务单号（订单号/预约号等）',
  `operation_description` varchar(200) DEFAULT '' COMMENT '操作描述',
  `operation_data` text DEFAULT NULL COMMENT '操作数据JSON (记录变更前后的关键字段)',
  `operator_type` tinyint(4) NOT NULL COMMENT '操作者类型 (1:产废企业, 2:回收企业, 3:司机, 4:系统自动, 5:平台管理员)',
  `operator_id` bigint(20) DEFAULT NULL COMMENT '操作者ID',
  `operator_name` varchar(64) DEFAULT '' COMMENT '操作者姓名',
  `operator_ip` varchar(50) DEFAULT NULL COMMENT '操作者IP',
  `user_agent` varchar(255) DEFAULT NULL COMMENT '用户代理信息',
  `operation_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  `risk_level` tinyint(4) DEFAULT '1' COMMENT '风险等级 (1:低, 2:中, 3:高)',
  
  -- 公共字段
  `tenant_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '租户ID',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_operation_type` (`operation_type`),
  KEY `idx_target_table_id` (`target_table`, `target_id`),
  KEY `idx_target_no` (`target_no`),
  KEY `idx_operator_type_id` (`operator_type`, `operator_id`),
  KEY `idx_operation_time` (`operation_time`),
  KEY `idx_risk_level` (`risk_level`),
  KEY `idx_tenant_id_deleted` (`tenant_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='业务操作审计日志表';

-- ========================================
-- 2. 性能优化索引
-- ========================================

-- 2.1 为 waste_transfer_order 表添加复合索引
ALTER TABLE waste_transfer_order 
ADD KEY idx_enterprise_status_time (producing_enterprise_id, business_status, create_time);

ALTER TABLE waste_transfer_order 
ADD KEY idx_payment_status_time (payment_status, payment_completed_time);

ALTER TABLE waste_transfer_order 
ADD KEY idx_recycling_status_time (recycling_enterprise_id, business_status, create_time);

-- 2.2 为 waste_order_allocation_record 表添加复合索引
ALTER TABLE waste_order_allocation_record 
ADD KEY idx_allocation_time_method (allocation_time, allocation_method);

-- 2.3 为 waste_appointment_quotation 表添加复合索引  
ALTER TABLE waste_appointment_quotation 
ADD KEY idx_appointment_status_time (appointment_id, status, create_time);

-- ========================================
-- 3. 修复订单主表缺失字段
-- ========================================

-- 3.1 为订单表添加一些可能缺失的重要字段
ALTER TABLE waste_transfer_order 
ADD COLUMN IF NOT EXISTS vehicle_weighing_id bigint(20) DEFAULT NULL COMMENT '关联的车辆过磅ID' AFTER transport_task_id;

ALTER TABLE waste_transfer_order 
ADD COLUMN IF NOT EXISTS pickup_confirmed_time datetime DEFAULT NULL COMMENT '收货确认时间' AFTER payment_completed_time;

ALTER TABLE waste_transfer_order 
ADD COLUMN IF NOT EXISTS pickup_confirmed_by varchar(64) DEFAULT NULL COMMENT '收货确认人' AFTER pickup_confirmed_time;

ALTER TABLE waste_transfer_order 
ADD COLUMN IF NOT EXISTS pickup_confirmed_location varchar(200) DEFAULT NULL COMMENT '收货确认GPS位置' AFTER pickup_confirmed_by;

ALTER TABLE waste_transfer_order 
ADD COLUMN IF NOT EXISTS confirmed_quantity decimal(10,2) DEFAULT NULL COMMENT '收运员确认数量' AFTER estimated_quantity;

-- 3.2 为新增字段添加索引
ALTER TABLE waste_transfer_order 
ADD KEY idx_vehicle_weighing_id (vehicle_weighing_id);

ALTER TABLE waste_transfer_order 
ADD KEY idx_pickup_confirmed_time (pickup_confirmed_time);

-- ========================================
-- 4. 修复分摊记录表字段
-- ========================================

-- 4.1 修正分摊记录表的关联字段名
ALTER TABLE waste_order_allocation_record 
ADD COLUMN IF NOT EXISTS vehicle_weighing_id bigint(20) DEFAULT NULL COMMENT '关联车辆过磅记录ID' AFTER order_id;

ALTER TABLE waste_order_allocation_record 
ADD KEY idx_vehicle_weighing_id (vehicle_weighing_id);

-- ========================================
-- 验证脚本
-- ========================================

-- 验证新建表是否成功
SELECT 
    table_name,
    table_comment
FROM information_schema.tables 
WHERE table_schema = DATABASE() 
    AND table_name IN (
        'waste_vehicle_weighing_record',
        'waste_order_allocation_record',
        'waste_payment_status_history',
        'waste_operation_audit_log'
    );

-- 验证索引是否添加成功
SELECT 
    table_name,
    index_name,
    column_name
FROM information_schema.statistics 
WHERE table_schema = DATABASE() 
    AND table_name = 'waste_transfer_order'
    AND index_name LIKE 'idx_%'
ORDER BY table_name, index_name, seq_in_index;

-- ========================================
-- 修复完成总结
-- ========================================

/*
修复内容总结：

1. ✅ 补充了 4 个缺失的核心业务表
   - waste_vehicle_weighing_record (车辆过磅记录表)
   - waste_order_allocation_record (订单过磅分摊记录表)
   - waste_payment_status_history (付款状态变更历史表)
   - waste_operation_audit_log (操作审计日志表)

2. ✅ 添加了性能优化索引
   - 企业维度查询索引
   - 付款状态查询索引
   - 分摊时间查询索引

3. ✅ 修复了订单主表缺失字段
   - 车辆过磅关联字段
   - 收货确认相关字段
   - 确认数量字段

4. ✅ 完善了表关联关系
   - 分摊记录表与过磅记录表的关联
   - 各种状态历史表的索引优化

经过修复后，数据库结构已经完整支持危废转移的核心业务流程！
*/ 