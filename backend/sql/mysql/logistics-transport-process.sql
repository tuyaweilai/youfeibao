-- 连接字符集：文件里有中文注释。客户端默认不是 utf8mb4 时（例如没有 LANG 的
-- `docker exec -i <mysql容器> mysql`，其默认是 latin1），中文会被双重编码存进库。
SET NAMES utf8mb4;

-- ========================================
-- 物流域：运输过程做全（V4 #71，见 ADR 0030 / 0031 / 0032）
--
-- 本文件做两件事：
--   1. 运输节点补「异常」独立标记，并把 `node_type` 放开为可空；
--   2. 新建运输任务改派承接记录表。
--
-- **异常不是任务状态**（#59 Implementation Decisions 第 17 条）：车辆故障、道路封闭这类事
-- 只标在事实（节点）上，任务状态机照旧只走「待分配 → 已分配 → 已接单 → 执行中 → 已完成」。
-- 异常事件没有「走到哪一步」，所以 `node_type` 必须可空——它是一条独立事实，不是某一步的附属。
--
-- **改派保留承接关系、不覆盖原记录**：换车换人时把「原车原人 → 新车新人 + 原因」写进
-- `logistics_transport_task_reassign`，任务行上的快照只是「当前是谁」，历史在这里。
--
-- 幂等：ADD COLUMN 按 information_schema 判存在再执行（MySQL 8 不支持 ADD COLUMN IF NOT EXISTS），
-- MODIFY COLUMN 只在列还是 NOT NULL 时执行；建表用 CREATE TABLE IF NOT EXISTS。
-- 注意 `--` 后必须跟空格才是注释：中文全角括号紧跟 `--` 会被 MySQL 当成语句，报 1064。
-- ========================================

-- ---- 1. 运输节点：node_type 放开为可空（异常事件没有节点类型） ----
SET @nullable := (SELECT IS_NULLABLE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'logistics_transport_node' AND COLUMN_NAME = 'node_type');
SET @ddl := IF(@nullable = 'NO',
    'ALTER TABLE `logistics_transport_node` MODIFY COLUMN `node_type` tinyint NULL COMMENT ''节点类型：1-到达提货点，2-交接完成，3-起运，4-到达场站，5-卸货完成；空=异常事件（异常是独立标记，不是节点类型）''',
    'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---- 2. 运输节点：异常字段 ----
SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
             AND TABLE_NAME = 'logistics_transport_node' AND COLUMN_NAME = 'abnormal_type');
SET @ddl := IF(@col = 0, 'ALTER TABLE `logistics_transport_node` ADD COLUMN `abnormal_type` tinyint DEFAULT NULL COMMENT ''异常类型：1-车辆故障，2-交通事故，3-天气延误，4-道路封闭，5-货物损坏，6-对方不在，7-地址错误，8-其他'' AFTER `operator_name`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
             AND TABLE_NAME = 'logistics_transport_node' AND COLUMN_NAME = 'abnormal_reason');
SET @ddl := IF(@col = 0, 'ALTER TABLE `logistics_transport_node` ADD COLUMN `abnormal_reason` varchar(500) DEFAULT NULL COMMENT ''异常说明（必填）'' AFTER `abnormal_type`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
             AND TABLE_NAME = 'logistics_transport_node' AND COLUMN_NAME = 'abnormal_resolved');
SET @ddl := IF(@col = 0, 'ALTER TABLE `logistics_transport_node` ADD COLUMN `abnormal_resolved` tinyint unsigned NOT NULL DEFAULT ''0'' COMMENT ''异常是否已解决'' AFTER `abnormal_reason`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
             AND TABLE_NAME = 'logistics_transport_node' AND COLUMN_NAME = 'abnormal_resolved_at');
SET @ddl := IF(@col = 0, 'ALTER TABLE `logistics_transport_node` ADD COLUMN `abnormal_resolved_at` datetime DEFAULT NULL COMMENT ''异常解决时间'' AFTER `abnormal_resolved`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
             AND TABLE_NAME = 'logistics_transport_node' AND COLUMN_NAME = 'abnormal_resolved_by');
SET @ddl := IF(@col = 0, 'ALTER TABLE `logistics_transport_node` ADD COLUMN `abnormal_resolved_by` bigint unsigned DEFAULT NULL COMMENT ''异常解决人（系统用户编号）'' AFTER `abnormal_resolved_at`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
             AND TABLE_NAME = 'logistics_transport_node' AND COLUMN_NAME = 'abnormal_resolved_name');
SET @ddl := IF(@col = 0, 'ALTER TABLE `logistics_transport_node` ADD COLUMN `abnormal_resolved_name` varchar(64) DEFAULT NULL COMMENT ''异常解决人姓名快照'' AFTER `abnormal_resolved_by`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
             AND TABLE_NAME = 'logistics_transport_node' AND COLUMN_NAME = 'abnormal_resolved_remark');
SET @ddl := IF(@col = 0, 'ALTER TABLE `logistics_transport_node` ADD COLUMN `abnormal_resolved_remark` varchar(500) DEFAULT NULL COMMENT ''异常解决说明'' AFTER `abnormal_resolved_name`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---- 3. 运输任务改派承接记录 ----
-- 与车辆车牌不建唯一键同一理由：改派是**同一任务可以发生多次**的动作，只追加、不覆盖；
-- 任务行上的 `vehicle_id` / `driver_id` 与快照只表示「当前是谁」。
CREATE TABLE IF NOT EXISTS `logistics_transport_task_reassign` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `task_id` bigint unsigned NOT NULL COMMENT '运输任务编号',
  `task_no` varchar(64) NOT NULL COMMENT '运输任务单号（冗余）',
  `prev_vehicle_id` bigint unsigned DEFAULT NULL COMMENT '原车辆编号',
  `prev_plate_no` varchar(32) DEFAULT NULL COMMENT '原车牌号快照',
  `prev_driver_id` bigint unsigned DEFAULT NULL COMMENT '原司机编号',
  `prev_driver_name` varchar(64) DEFAULT NULL COMMENT '原司机姓名快照',
  `prev_driver_mobile` varchar(32) DEFAULT NULL COMMENT '原司机手机号快照',
  `vehicle_id` bigint unsigned NOT NULL COMMENT '新车辆编号',
  `plate_no` varchar(32) DEFAULT NULL COMMENT '新车牌号快照',
  `driver_id` bigint unsigned NOT NULL COMMENT '新司机编号',
  `driver_name` varchar(64) DEFAULT NULL COMMENT '新司机姓名快照',
  `driver_mobile` varchar(32) DEFAULT NULL COMMENT '新司机手机号快照',
  `reason` varchar(500) NOT NULL COMMENT '改派原因（必填）',
  `operator_id` bigint unsigned DEFAULT NULL COMMENT '改派人（系统用户编号）',
  `operator_name` varchar(64) DEFAULT NULL COMMENT '改派人姓名快照',
  `reassign_time` datetime NOT NULL COMMENT '改派时间',
  `tenant_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '租户编号',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_reassign_tenant_task` (`tenant_id`, `task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='物流 - 运输任务改派承接记录';
