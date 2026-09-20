-- 连接字符集：文件里有中文种子数据。客户端默认不是 utf8mb4 时（例如没有 LANG 的
-- `docker exec -i <mysql容器> mysql`，其默认是 latin1），中文会被双重编码存进库，
-- 界面上就是乱码。这里显式声明，导入时不必再依赖客户端参数。
SET NAMES utf8mb4;

-- ========================================
-- 交接批次与有效磅次（#50 T12）
--
-- 一个交易对方的一次**物理交接**记为一个交接批次（icbc_handover_batch）；
-- 过磅保留每一次原始读数（icbc_weighing），**只有被选定的那一次参与计量**，
-- 其余留档但不参与。收购单通过 handover_batch_id / weighing_id 引用有效磅次的值与版本。
--
-- 两条刻意的不变量：
--   * **不按「车牌 + 日期」去重**：同一车同一天两次送货就是两个批次，磅单与收购单各归各；
--   * **预约与采购订单可空**：临时上门的散户不被流程挡住，零散收购标为「直接收购」。
--
-- 幂等：CREATE TABLE IF NOT EXISTS + 仅当列不存在时 ALTER。两张表都是租户表（带 tenant_id），
-- 不进 yudao.tenant.ignore-tables。菜单与按钮权限在 icbc-menu.sql 里幂等维护。
-- ========================================

CREATE TABLE IF NOT EXISTS `icbc_handover_batch` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `batch_no` varchar(64) NOT NULL COMMENT '批次号（平台生成，唯一）',
  `payee_id` bigint unsigned NOT NULL COMMENT '出售者（交易对方）档案编号',
  `seller_name` varchar(100) DEFAULT NULL COMMENT '出售者姓名快照',
  `seller_mobile` varchar(32) DEFAULT NULL COMMENT '出售者联系方式快照',
  `station_id` bigint unsigned DEFAULT NULL COMMENT '场站编号（到场收货时填；上门回收为空）',
  `station_name` varchar(100) DEFAULT NULL COMMENT '场站名称快照',
  `visit_address` varchar(500) DEFAULT NULL COMMENT '上门地址（上门回收时填；到场收货为空）',
  `occur_time` datetime DEFAULT NULL COMMENT '交接时间',
  `source_type` varchar(20) DEFAULT NULL COMMENT '来源方式：APPOINTMENT-预约到站，WALK_IN-直接到场，ON_SITE-上门回收',
  `driver_name` varchar(50) DEFAULT NULL COMMENT '司机姓名（运输信息）',
  `driver_mobile` varchar(32) DEFAULT NULL COMMENT '司机手机号（运输信息）',
  `plate_no` varchar(32) DEFAULT NULL COMMENT '车牌号',
  `appointment_id` bigint unsigned DEFAULT NULL COMMENT '关联的到站预约编号（可空；预约不是订单）',
  `purchase_order_id` bigint unsigned DEFAULT NULL COMMENT '关联的采购订单编号（可空；不填即直接收购）',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `tenant_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '租户编号',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_handover_batch_no` (`batch_no`),
  KEY `idx_handover_batch_payee` (`tenant_id`, `payee_id`),
  KEY `idx_handover_batch_station` (`tenant_id`, `station_id`),
  KEY `idx_handover_batch_occur_time` (`tenant_id`, `occur_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='交接批次（一次物理交接）';

CREATE TABLE IF NOT EXISTS `icbc_weighing` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `batch_id` bigint unsigned NOT NULL COMMENT '交接批次编号',
  `seq_no` int NOT NULL COMMENT '第几次磅次（计量结果引用它的值与此版本号）',
  `gross_weight` decimal(14,4) DEFAULT NULL COMMENT '毛重',
  `tare_weight` decimal(14,4) DEFAULT NULL COMMENT '皮重',
  `net_weight` decimal(14,4) DEFAULT NULL COMMENT '净重 = 毛重 − 皮重',
  `weigh_time` datetime DEFAULT NULL COMMENT '过磅时间',
  `weight_ticket_no` varchar(64) DEFAULT NULL COMMENT '磅单号',
  `weight_ticket_image_url` varchar(500) DEFAULT NULL COMMENT '磅单照片地址',
  `plate_no` varchar(32) DEFAULT NULL COMMENT '磅单上的车牌号',
  `effective` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否有效磅次：1-参与计量，0-留档不参与',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注（如复磅原因）',
  `tenant_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '租户编号',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_weighing_batch_seq` (`batch_id`, `seq_no`),
  KEY `idx_weighing_batch_effective` (`tenant_id`, `batch_id`, `effective`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='磅次（一次过磅的原始读数）';

-- ========================================
-- 收购单挂交接批次与有效磅次（#50）
-- 计量结果引用的是有效磅次的值（毛重 / 皮重 / 净重已快照进收购单）与版本（weighing_seq_no）。
-- 幂等：仅当列不存在时 ALTER；历史数据为空，表示人工录入重量，行为不变。
-- ========================================

SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
             AND TABLE_NAME = 'icbc_acquisition' AND COLUMN_NAME = 'handover_batch_id');
SET @ddl := IF(@col = 0, 'ALTER TABLE `icbc_acquisition` ADD COLUMN `handover_batch_id` bigint unsigned DEFAULT NULL COMMENT ''交接批次编号（#50）'' AFTER `settlement_id`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
             AND TABLE_NAME = 'icbc_acquisition' AND COLUMN_NAME = 'weighing_id');
SET @ddl := IF(@col = 0, 'ALTER TABLE `icbc_acquisition` ADD COLUMN `weighing_id` bigint unsigned DEFAULT NULL COMMENT ''有效磅次编号（计量结果引用的就是它）'' AFTER `handover_batch_id`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
             AND TABLE_NAME = 'icbc_acquisition' AND COLUMN_NAME = 'weighing_seq_no');
SET @ddl := IF(@col = 0, 'ALTER TABLE `icbc_acquisition` ADD COLUMN `weighing_seq_no` int DEFAULT NULL COMMENT ''有效磅次是第几次（版本号快照）'' AFTER `weighing_id`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx := (SELECT COUNT(1) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE()
             AND TABLE_NAME = 'icbc_acquisition' AND INDEX_NAME = 'idx_acquisition_handover_batch');
SET @ddl := IF(@idx = 0, 'ALTER TABLE `icbc_acquisition` ADD INDEX `idx_acquisition_handover_batch` (`handover_batch_id`)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
