-- 危险废物转移模块缺失表生成脚本
-- 基于 waste_transfer_database_design.md 设计文档
-- 执行日期：2024-12-02
-- 说明：生成设计文档中定义但数据库中缺失的表

-- ========================================
-- 当前数据库已存在的表：
-- 1. waste_transfer_appointment (预约单表)
-- 2. waste_recycler_assignment_rule (回收企业分配规则表) 
-- 3. waste_appointment_assignment_history
-- 4. waste_appointment_status_log
-- ========================================

-- ========================================
-- 需要生成的核心业务表
-- ========================================

-- 1. 订单主表
CREATE TABLE IF NOT EXISTS `waste_transfer_order` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '订单ID',
  `order_no` varchar(64) NOT NULL COMMENT '订单号 (系统生成, 唯一)',
  `appointment_id` bigint(20) DEFAULT NULL COMMENT '关联预约单ID',
  `quotation_id` bigint(20) DEFAULT NULL COMMENT '关联报价记录ID',
  `producing_enterprise_id` bigint(20) NOT NULL COMMENT '产废企业ID',
  `producing_store_id` bigint(20) DEFAULT NULL COMMENT '产废门店ID',
  `recycling_enterprise_id` bigint(20) NOT NULL COMMENT '回收企业ID',
  
  -- 物流关联（通过接口协作）
  `transport_task_id` bigint(20) DEFAULT NULL COMMENT '关联的物流运输任务ID',
  `vehicle_weighing_id` bigint(20) DEFAULT NULL COMMENT '关联的车辆过磅ID',
  `logistics_status` tinyint(4) DEFAULT NULL COMMENT '物流状态快照',
  
  -- 废物信息
  `waste_code` varchar(50) NOT NULL COMMENT '危险废物代码',
  `waste_name` varchar(100) NOT NULL COMMENT '危险废物名称',
  `estimated_quantity` decimal(10,2) NOT NULL COMMENT '预估数量',
  `confirmed_quantity` decimal(10,2) DEFAULT NULL COMMENT '收运员确认数量',
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
  `pickup_confirmed_time` datetime DEFAULT NULL COMMENT '收货确认时间',
  `pickup_confirmed_by` varchar(64) DEFAULT NULL COMMENT '收货确认人',
  `pickup_confirmed_location` varchar(200) DEFAULT NULL COMMENT '收货确认GPS位置',
  
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
  `deleted` bit(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no_tenant_id` (`order_no`, `tenant_id`, `deleted`),
  KEY `idx_appointment_id` (`appointment_id`),
  KEY `idx_producing_enterprise_id` (`producing_enterprise_id`),
  KEY `idx_recycling_enterprise_id` (`recycling_enterprise_id`),
  KEY `idx_transport_task_id` (`transport_task_id`),
  KEY `idx_vehicle_weighing_id` (`vehicle_weighing_id`),
  KEY `idx_business_status` (`business_status`),
  KEY `idx_payment_status` (`payment_status`),
  KEY `idx_pickup_confirmed_time` (`pickup_confirmed_time`),
  KEY `idx_tenant_id_deleted_status` (`tenant_id`, `deleted`, `business_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='危废转移订单主表';

-- 2. 车辆过磅记录表
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
  `deleted` bit(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
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

-- 3. 订单分摊记录表
CREATE TABLE IF NOT EXISTS `waste_order_allocation_record` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '分摊记录ID',
  `order_id` bigint(20) NOT NULL COMMENT '订单ID',
  `vehicle_weighing_id` bigint(20) DEFAULT NULL COMMENT '关联车辆过磅记录ID',
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
  `is_manual_adjustment` bit(1) DEFAULT 0 COMMENT '是否人工调整',
  `adjustment_reason` varchar(255) DEFAULT '' COMMENT '人工调整原因',
  `remark` varchar(255) DEFAULT '' COMMENT '分摊备注',
  
  `tenant_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '租户ID',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_batch` (`order_id`, `weighing_batch_no`, `deleted`),
  KEY `idx_vehicle_weighing_id` (`vehicle_weighing_id`),
  KEY `idx_weighing_batch_no` (`weighing_batch_no`),
  KEY `idx_allocation_method` (`allocation_method`),
  KEY `idx_allocation_time` (`allocation_time`),
  KEY `idx_allocation_time_method` (`allocation_time`, `allocation_method`),
  KEY `idx_is_manual_adjustment` (`is_manual_adjustment`),
  KEY `idx_tenant_id_deleted` (`tenant_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='订单过磅分摊记录表';

-- 4. 订单状态历史表  
CREATE TABLE IF NOT EXISTS `waste_order_status_history` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '状态历史ID',
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
  `milestone_flag` bit(1) DEFAULT 0 COMMENT '是否关键里程碑',
  
  -- 扩展的业务数据
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
  `deleted` bit(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
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

-- 5. 产废企业付款配置表
CREATE TABLE IF NOT EXISTS `waste_producer_payment_config` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '配置ID',
  `producing_enterprise_id` bigint(20) NOT NULL COMMENT '产废企业ID',
  `producing_store_id` bigint(20) DEFAULT NULL COMMENT '产废门店ID (可细化到门店级别)',
  `payment_method` tinyint(4) NOT NULL DEFAULT '1' COMMENT '结算方式 (1:对公结算, 2:个人结算)',
  `is_default_config` bit(1) DEFAULT 0 COMMENT '是否为企业默认配置',
  
  -- 对公结算配置
  `company_bank_name` varchar(100) DEFAULT '' COMMENT '对公开户银行',
  `company_bank_account` varchar(50) DEFAULT '' COMMENT '对公银行账号',
  `company_account_name` varchar(100) DEFAULT '' COMMENT '对公账户名称',
  `tax_number` varchar(50) DEFAULT '' COMMENT '税号',
  `company_address` varchar(200) DEFAULT '' COMMENT '公司地址',
  `company_phone` varchar(20) DEFAULT '' COMMENT '公司电话',
  `invoice_required` bit(1) DEFAULT 0 COMMENT '是否需要开票',
  `invoice_title` varchar(100) DEFAULT '' COMMENT '发票抬头',
  
  -- 个人结算配置  
  `personal_payee_name` varchar(64) DEFAULT '' COMMENT '个人收款人姓名',
  `personal_payee_phone` varchar(20) DEFAULT '' COMMENT '个人收款人电话',
  `personal_payee_id_card` varchar(18) DEFAULT '' COMMENT '个人收款人身份证号',
  `personal_bank_name` varchar(100) DEFAULT '' COMMENT '个人开户银行',
  `personal_bank_account` varchar(50) DEFAULT '' COMMENT '个人银行账号',
  `personal_account_name` varchar(64) DEFAULT '' COMMENT '个人账户名称',
  `relationship_to_enterprise` varchar(50) DEFAULT '' COMMENT '与企业关系 (法人/财务/授权人等)',
  
  -- 通用配置
  `auto_payment_enabled` bit(1) DEFAULT 0 COMMENT '是否启用自动付款',
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
  `deleted` bit(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_producing_enterprise_id` (`producing_enterprise_id`),
  KEY `idx_producing_store_id` (`producing_store_id`),
  KEY `idx_payment_method` (`payment_method`),
  KEY `idx_is_default_config` (`is_default_config`),
  KEY `idx_config_status` (`config_status`),
  KEY `idx_tenant_id_deleted` (`tenant_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='产废企业付款配置表';

-- 6. 对公付款凭证表
CREATE TABLE IF NOT EXISTS `waste_company_payment_voucher` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '凭证ID',
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
  
  -- 确认状态
  `voucher_status` tinyint(4) NOT NULL DEFAULT '0' COMMENT '凭证状态 (0:待确认, 1:已确认, 2:有争议, 3:已退回)',
  `confirmed_by_producer` bit(1) DEFAULT 0 COMMENT '产废企业是否确认',
  `confirmed_by_recycling` bit(1) DEFAULT 0 COMMENT '回收企业是否确认',
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
  `deleted` bit(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_voucher_no_tenant_id` (`voucher_no`, `tenant_id`, `deleted`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_payment_type` (`payment_type`),
  KEY `idx_payment_date` (`payment_date`),
  KEY `idx_voucher_status` (`voucher_status`),
  KEY `idx_operator_id` (`operator_id`),
  KEY `idx_tenant_id_deleted` (`tenant_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='对公付款凭证表';

-- 7. 预约报价记录表
CREATE TABLE IF NOT EXISTS `waste_appointment_quotation` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '报价记录ID',
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
  `deleted` bit(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_appointment_id` (`appointment_id`),
  KEY `idx_recycling_enterprise_id` (`recycling_enterprise_id`),
  KEY `idx_status` (`status`),
  KEY `idx_valid_until` (`valid_until`),
  KEY `idx_appointment_status_time` (`appointment_id`, `status`, `create_time`),
  KEY `idx_tenant_id_deleted_status` (`tenant_id`, `deleted`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='预约报价记录表';

-- 验证表创建
SELECT 
    TABLE_NAME as '表名',
    TABLE_COMMENT as '表注释',
    CREATE_TIME as '创建时间'
FROM information_schema.TABLES 
WHERE TABLE_SCHEMA = DATABASE() 
    AND TABLE_NAME LIKE 'waste_%'
ORDER BY TABLE_NAME; 