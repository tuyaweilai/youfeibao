-- ========================================
-- 工商银行接口日志与回调管理数据库表结构
-- ========================================

-- 1. 工行接口调用日志表
CREATE TABLE `icbc_api_log` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `msg_id` varchar(64) NOT NULL COMMENT '消息通讯唯一编号',
  `api_name` varchar(100) NOT NULL COMMENT '接口名称',
  `api_url` varchar(200) NOT NULL COMMENT '接口URL',
  `method` varchar(10) NOT NULL COMMENT '请求方法',
  `request_params` text COMMENT '请求参数（脱敏后）',
  `response_data` text COMMENT '响应数据',
  `return_code` varchar(20) DEFAULT NULL COMMENT '工行返回码',
  `return_msg` varchar(500) DEFAULT NULL COMMENT '工行返回消息',
  `status` tinyint unsigned NOT NULL COMMENT '调用状态：1-成功，2-失败',
  `cost_time` int unsigned DEFAULT NULL COMMENT '耗时（毫秒）',
  `business_id` varchar(64) DEFAULT NULL COMMENT '业务ID（订单号等）',
  `business_type` varchar(50) DEFAULT NULL COMMENT '业务类型',
  `error_msg` varchar(1000) DEFAULT NULL COMMENT '错误信息',
  `tenant_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '租户ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_msg_id` (`msg_id`),
  KEY `idx_api_name` (`api_name`),
  KEY `idx_business_id` (`business_id`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工行接口调用日志表';

-- 2. 工行回调通知表
CREATE TABLE `icbc_callback_notify` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `notify_id` varchar(128) NOT NULL COMMENT '通知ID',
  `notify_type` varchar(50) NOT NULL COMMENT '通知类型：PAYEE_AUDIT-收方审核，INVOICE_STATUS-发票状态',
  `business_id` varchar(64) NOT NULL COMMENT '业务ID',
  `notify_data` text NOT NULL COMMENT '通知数据',
  `sign` varchar(1000) DEFAULT NULL COMMENT '签名',
  `process_status` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '处理状态：0-待处理，1-处理成功，2-处理失败',
  `process_msg` varchar(500) DEFAULT NULL COMMENT '处理结果信息',
  `process_time` datetime DEFAULT NULL COMMENT '处理时间',
  `retry_count` int unsigned NOT NULL DEFAULT '0' COMMENT '重试次数',
  `tenant_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '租户ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_notify_id` (`notify_id`),
  KEY `idx_notify_type` (`notify_type`),
  KEY `idx_business_id` (`business_id`),
  KEY `idx_process_status` (`process_status`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工行回调通知表'; 