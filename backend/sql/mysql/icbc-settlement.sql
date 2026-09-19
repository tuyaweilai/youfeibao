-- ========================================
-- 结算单与确认门禁（#33，见 docs/adr/0018 / 0022 / 0024）
--
-- 1. icbc_settlement：一次到场批次一张结算单（多张收购单聚合），承载确认 / 异议 / 线下签字
-- 2. icbc_settlement_version：整单快照（只追加、不覆盖），当前生效版本用结算单指针指
--
-- 幂等：CREATE TABLE IF NOT EXISTS。两张表都是租户表（带 tenant_id），不进 ignore-tables。
-- 菜单与按钮权限在 icbc-menu.sql 里幂等维护。
-- ========================================

CREATE TABLE IF NOT EXISTS `icbc_settlement` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `settlement_no` varchar(64) NOT NULL COMMENT '结算单号',
  `payee_id` bigint unsigned NOT NULL COMMENT '出售者（收方）档案编号',
  `natural_person_id` bigint unsigned DEFAULT NULL COMMENT '自然人主体编号（平台级身份）',
  `seller_name` varchar(100) DEFAULT NULL COMMENT '出售者姓名快照',
  `seller_mobile` varchar(32) DEFAULT NULL COMMENT '出售者联系方式快照',
  `station_id` bigint unsigned DEFAULT NULL COMMENT '场站编号（一次到场批次 = 同出售者 + 同场站）',
  `station_name` varchar(100) DEFAULT NULL COMMENT '场站名称快照',
  `batch_key` varchar(64) DEFAULT NULL COMMENT '离线批次键（现场端同一批用同一个值）',
  `generate_time` datetime DEFAULT NULL COMMENT '生成时间（结束本次收货）',
  `generated_by` bigint DEFAULT NULL COMMENT '生成人（收货员用户编号）',
  `current_version_id` bigint unsigned DEFAULT NULL COMMENT '当前生效版本编号',
  `current_version_no` int DEFAULT NULL COMMENT '当前生效版本号',
  `confirm_status` tinyint NOT NULL DEFAULT '0' COMMENT '确认状态：0-待确认，1-已确认，2-有异议，3-需线下签字确认，4-已线下签字确认',
  `confirm_time` datetime DEFAULT NULL COMMENT '确认时间',
  `confirm_ip` varchar(64) DEFAULT NULL COMMENT '确认时 IP',
  `confirm_device` varchar(255) DEFAULT NULL COMMENT '确认时设备（User-Agent）',
  `confirm_hash` varchar(64) DEFAULT NULL COMMENT '确认时结算单快照哈希',
  `confirm_member_user_id` bigint DEFAULT NULL COMMENT '确认人（登录凭证）',
  `dispute_reason` varchar(10) DEFAULT NULL COMMENT '异议原因枚举：01/02/03/04/05/99',
  `dispute_note` varchar(500) DEFAULT NULL COMMENT '异议说明',
  `dispute_time` datetime DEFAULT NULL COMMENT '最近一次异议时间',
  `dispute_count` int NOT NULL DEFAULT '0' COMMENT '累计异议次数',
  `enterprise_reply_note` varchar(500) DEFAULT NULL COMMENT '企业处理说明（不改但附说明）',
  `enterprise_reply_time` datetime DEFAULT NULL COMMENT '企业最近一次处理时间',
  `deadline_time` datetime DEFAULT NULL COMMENT '下一步动作截止时间',
  `offline_sign_file_url` varchar(500) DEFAULT NULL COMMENT '线下签字确认书附件地址',
  `offline_sign_handler` varchar(100) DEFAULT NULL COMMENT '线下签字办理人',
  `offline_sign_time` datetime DEFAULT NULL COMMENT '线下签字确认时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `tenant_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '租户编号',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_settlement_no` (`settlement_no`),
  KEY `idx_settlement_payee` (`tenant_id`, `payee_id`),
  KEY `idx_settlement_natural_person` (`tenant_id`, `natural_person_id`),
  KEY `idx_settlement_confirm_status` (`tenant_id`, `confirm_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='结算单';

CREATE TABLE IF NOT EXISTS `icbc_settlement_version` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `settlement_id` bigint unsigned NOT NULL COMMENT '结算单编号',
  `version_no` int NOT NULL COMMENT '版本号，从 1 递增',
  `snapshot_json` mediumtext COMMENT '整单快照 JSON（结算重量 / 单价 / 调整项 / 逐条收购单明细）',
  `snapshot_hash` varchar(64) DEFAULT NULL COMMENT '快照哈希（SHA-256）',
  `total_settlement_weight` decimal(14,4) DEFAULT NULL COMMENT '本版合计结算重量',
  `total_amount` decimal(14,2) DEFAULT NULL COMMENT '本版合计金额',
  `acquisition_count` int DEFAULT NULL COMMENT '本版收购单条数',
  `change_reason` varchar(500) DEFAULT NULL COMMENT '变更原因',
  `changed_by` varchar(64) DEFAULT NULL COMMENT '变更人',
  `source` varchar(32) DEFAULT NULL COMMENT '来源：GENERATE/ENTERPRISE_CHANGE/ENTERPRISE_REPLY',
  `tenant_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '租户编号',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_settlement_version` (`tenant_id`, `settlement_id`, `version_no`),
  KEY `idx_settlement_version_settlement` (`settlement_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='结算单版本';

-- ========================================
-- 场站维度（#33 / #34，ADR 0018）：一次到场批次 = 同出售者 + 同场站；
-- 自然人端「扫码进入」按该场站匹配待确认结算单。幂等：仅当列不存在时 ALTER。
-- ========================================
SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
             AND TABLE_NAME = 'icbc_settlement' AND COLUMN_NAME = 'station_id');
SET @ddl := IF(@col = 0, 'ALTER TABLE `icbc_settlement` ADD COLUMN `station_id` bigint unsigned DEFAULT NULL COMMENT ''场站编号（一次到场批次 = 同出售者 + 同场站）'' AFTER `seller_mobile`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
             AND TABLE_NAME = 'icbc_settlement' AND COLUMN_NAME = 'station_name');
SET @ddl := IF(@col = 0, 'ALTER TABLE `icbc_settlement` ADD COLUMN `station_name` varchar(100) DEFAULT NULL COMMENT ''场站名称快照'' AFTER `station_id`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx := (SELECT COUNT(1) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE()
             AND TABLE_NAME = 'icbc_settlement' AND INDEX_NAME = 'idx_settlement_station');
SET @ddl := IF(@idx = 0, 'ALTER TABLE `icbc_settlement` ADD INDEX `idx_settlement_station` (`tenant_id`, `station_id`)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
