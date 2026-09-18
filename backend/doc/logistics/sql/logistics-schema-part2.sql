-- 物流运输模块数据库表结构脚本（第二部分）
-- 创建日期：2024年05月

-- 创建车辆过磅记录表
CREATE TABLE IF NOT EXISTS `logistics_vehicle_weighing` (
  `id` bigint(20) NOT NULL COMMENT '过磅记录ID',
  `weighing_no` varchar(64) NOT NULL COMMENT '过磅单号',
  `weighing_batch_no` varchar(64) NOT NULL COMMENT '过磅批次号（用于危废模块关联）',
  `vehicle_id` bigint(20) NOT NULL COMMENT '车辆ID',
  `vehicle_no` varchar(20) NOT NULL COMMENT '车牌号',
  `driver_id` bigint(20) NOT NULL COMMENT '司机ID',
  `driver_name` varchar(64) NOT NULL COMMENT '司机姓名',
  
  -- 过磅数据
  `weighing_time` datetime NOT NULL COMMENT '过磅时间',
  `gross_weight` decimal(10,2) NOT NULL COMMENT '毛重(吨)',
  `tare_weight` decimal(10,2) NOT NULL COMMENT '皮重(吨)',
  `net_weight` decimal(10,2) NOT NULL COMMENT '净重(吨)',
  
  -- 关联订单
  `related_order_ids` text COMMENT '关联的订单IDs(JSON数组)',
  `order_count` int(11) NOT NULL DEFAULT '0' COMMENT '订单数量',
  
  -- 操作信息
  `weighing_location` varchar(200) NOT NULL COMMENT '过磅地点',
  `operator_id` bigint(20) NOT NULL COMMENT '过磅员ID',
  `operator_name` varchar(64) NOT NULL COMMENT '过磅员姓名',
  `weighing_photos` text COMMENT '过磅单照片URLs(JSON数组)',
  
  -- 推送状态（推送给危废模块）
  `push_status` tinyint(4) NOT NULL DEFAULT '0' COMMENT '推送状态(0:待推送,1:已推送,2:推送失败)',
  `push_time` datetime DEFAULT NULL COMMENT '推送时间',
  `push_response` text COMMENT '推送响应',
  
  -- 异常检测
  `is_anomaly` bit(1) DEFAULT b'0' COMMENT '是否异常',
  `anomaly_type` tinyint(4) DEFAULT NULL COMMENT '异常类型',
  `anomaly_reason` varchar(500) DEFAULT '' COMMENT '异常原因',
  
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `tenant_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '租户ID',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_weighing_no` (`weighing_no`),
  UNIQUE KEY `uk_weighing_batch_no` (`weighing_batch_no`),
  KEY `idx_vehicle_id` (`vehicle_id`),
  KEY `idx_weighing_time` (`weighing_time`),
  KEY `idx_push_status` (`push_status`),
  KEY `idx_tenant_id_deleted` (`tenant_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='车辆过磅记录表';

-- 创建司机现金代付记录表
CREATE TABLE IF NOT EXISTS `logistics_cash_advance` (
  `id` bigint(20) NOT NULL COMMENT '代付记录ID',
  `task_id` bigint(20) NOT NULL COMMENT '运输任务ID',
  `order_id` bigint(20) NOT NULL COMMENT '订单ID',
  `driver_id` bigint(20) NOT NULL COMMENT '司机ID',
  `driver_name` varchar(64) NOT NULL COMMENT '司机姓名',
  
  -- 支付信息
  `payment_amount` decimal(10,2) NOT NULL COMMENT '支付金额',
  `payment_time` datetime NOT NULL COMMENT '支付时间',
  `payment_location` varchar(500) DEFAULT '' COMMENT '支付地点',
  `payment_method` varchar(20) NOT NULL DEFAULT 'CASH' COMMENT '支付方式(CASH:现金,WECHAT:微信,ALIPAY:支付宝)',
  `payee_name` varchar(64) NOT NULL COMMENT '收款人姓名',
  `payee_phone` varchar(20) DEFAULT '' COMMENT '收款人电话',
  
  -- 凭证信息
  `payment_photos` text COMMENT '支付凭证照片URLs(JSON数组)',
  `receipt_photos` text COMMENT '收据照片URLs(JSON数组)',
  
  -- 通知状态（通知危废模块）
  `notify_status` tinyint(4) NOT NULL DEFAULT '0' COMMENT '通知状态(0:待通知,1:已通知,2:通知失败)',
  `notify_time` datetime DEFAULT NULL COMMENT '通知时间',
  
  -- 对账状态
  `reconcile_status` tinyint(4) NOT NULL DEFAULT '0' COMMENT '对账状态(0:待对账,1:已确认,2:有异议)',
  `reconcile_time` datetime DEFAULT NULL COMMENT '对账时间',
  `reconcile_remark` varchar(500) DEFAULT '' COMMENT '对账备注',
  `reconcile_operator_id` bigint(20) DEFAULT NULL COMMENT '对账操作人ID',
  `reconcile_operator_name` varchar(64) DEFAULT NULL COMMENT '对账操作人姓名',
  
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `tenant_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '租户ID',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_task_id` (`task_id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_driver_id` (`driver_id`),
  KEY `idx_notify_status` (`notify_status`),
  KEY `idx_reconcile_status` (`reconcile_status`),
  KEY `idx_tenant_id_deleted` (`tenant_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='司机现金代付记录表'; 