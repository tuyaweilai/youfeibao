-- 连接字符集：文件里有中文注释。
SET NAMES utf8mb4;

-- ========================================
-- 档案做全与派车门禁（V3 #70，见 ADR 0030 与 CONTEXT.md「物流与运力」）
--
--   1. logistics_vehicle  补：行驶证 / 保险到期日、照片、GPS 设备号
--   2. logistics_driver   补：驾驶证与从业资格证号码 / 类型 / 到期日、所属承运商
--   3. logistics_carrier  新增：承运商档案（运力吃紧时找的第三方公司）
--   4. logistics_transport_task 补：授权放行留痕（原因 / 授权人 / 时间）
--
-- **门禁与逃生门的形状**：证件过期或被停用的车辆与司机**不得派出**；确需放行时由管理员带着
-- `override_reason` 再次提交，服务端校验调用者持有 `logistics:transport-task:override` 权限
-- （只挂在管理员角色上），并把原因、授权人、时间落到任务上。**没有这条留痕就不放行**——
-- 与 ADR 0018 的线下签字件同一个模式：门禁可以绕，但绕过必须留名。
--
-- 幂等：`--` 后必须跟空格才是 MySQL 注释；列与索引按 information_schema 判存在再执行
-- （与 `icbc-acquisition-purchase-link.sql`、`logistics-transport-cargo.sql` 同一手法）。
-- ========================================

-- ---------- 车辆 ----------
SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
             AND TABLE_NAME = 'logistics_vehicle' AND COLUMN_NAME = 'driving_license_expiry_date');
SET @ddl := IF(@col = 0, 'ALTER TABLE `logistics_vehicle` ADD COLUMN `driving_license_expiry_date` date DEFAULT NULL COMMENT ''行驶证到期日（过期不得派出）'' AFTER `capacity_ton`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
             AND TABLE_NAME = 'logistics_vehicle' AND COLUMN_NAME = 'insurance_expiry_date');
SET @ddl := IF(@col = 0, 'ALTER TABLE `logistics_vehicle` ADD COLUMN `insurance_expiry_date` date DEFAULT NULL COMMENT ''保险到期日（过期不得派出）'' AFTER `driving_license_expiry_date`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
             AND TABLE_NAME = 'logistics_vehicle' AND COLUMN_NAME = 'photos');
SET @ddl := IF(@col = 0, 'ALTER TABLE `logistics_vehicle` ADD COLUMN `photos` text COMMENT ''车辆照片 URL 列表（JSON 数组文本）'' AFTER `insurance_expiry_date`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
             AND TABLE_NAME = 'logistics_vehicle' AND COLUMN_NAME = 'gps_device_id');
SET @ddl := IF(@col = 0, 'ALTER TABLE `logistics_vehicle` ADD COLUMN `gps_device_id` varchar(64) DEFAULT NULL COMMENT ''车载定位设备号（一期只登记，轨迹接入另行立项）'' AFTER `photos`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------- 承运商 ----------
CREATE TABLE IF NOT EXISTS `logistics_carrier` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(100) NOT NULL COMMENT '承运商名称（租户内唯一）',
  `contact_name` varchar(64) DEFAULT NULL COMMENT '联系人',
  `contact_mobile` varchar(32) DEFAULT NULL COMMENT '联系电话',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态：0-合作中，1-已停用',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `tenant_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '租户编号',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_carrier_tenant_name` (`tenant_id`, `name`),
  KEY `idx_carrier_tenant_status` (`tenant_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='物流 - 承运商档案';

-- ---------- 司机：证件与所属承运商 ----------
SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
             AND TABLE_NAME = 'logistics_driver' AND COLUMN_NAME = 'carrier_id');
SET @ddl := IF(@col = 0, 'ALTER TABLE `logistics_driver` ADD COLUMN `carrier_id` bigint unsigned DEFAULT NULL COMMENT ''所属承运商编号（来源为承运商时非空）'' AFTER `source`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
             AND TABLE_NAME = 'logistics_driver' AND COLUMN_NAME = 'driving_license_no');
SET @ddl := IF(@col = 0, 'ALTER TABLE `logistics_driver` ADD COLUMN `driving_license_no` varchar(64) DEFAULT NULL COMMENT ''驾驶证号码'' AFTER `carrier_id`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
             AND TABLE_NAME = 'logistics_driver' AND COLUMN_NAME = 'driving_license_type');
SET @ddl := IF(@col = 0, 'ALTER TABLE `logistics_driver` ADD COLUMN `driving_license_type` varchar(32) DEFAULT NULL COMMENT ''准驾车型（如 A2 / B2）'' AFTER `driving_license_no`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
             AND TABLE_NAME = 'logistics_driver' AND COLUMN_NAME = 'driving_license_expiry_date');
SET @ddl := IF(@col = 0, 'ALTER TABLE `logistics_driver` ADD COLUMN `driving_license_expiry_date` date DEFAULT NULL COMMENT ''驾驶证到期日（过期不得派出）'' AFTER `driving_license_type`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
             AND TABLE_NAME = 'logistics_driver' AND COLUMN_NAME = 'qualification_cert_no');
SET @ddl := IF(@col = 0, 'ALTER TABLE `logistics_driver` ADD COLUMN `qualification_cert_no` varchar(64) DEFAULT NULL COMMENT ''从业资格证号码'' AFTER `driving_license_expiry_date`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
             AND TABLE_NAME = 'logistics_driver' AND COLUMN_NAME = 'qualification_cert_expiry_date');
SET @ddl := IF(@col = 0, 'ALTER TABLE `logistics_driver` ADD COLUMN `qualification_cert_expiry_date` date DEFAULT NULL COMMENT ''从业资格证到期日（过期不得派出）'' AFTER `qualification_cert_no`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx := (SELECT COUNT(1) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE()
             AND TABLE_NAME = 'logistics_driver' AND INDEX_NAME = 'idx_driver_tenant_carrier');
SET @ddl := IF(@idx = 0, 'ALTER TABLE `logistics_driver` ADD INDEX `idx_driver_tenant_carrier` (`tenant_id`, `carrier_id`)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------- 任务：授权放行留痕 ----------
SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
             AND TABLE_NAME = 'logistics_transport_task' AND COLUMN_NAME = 'override_reason');
SET @ddl := IF(@col = 0, 'ALTER TABLE `logistics_transport_task` ADD COLUMN `override_reason` varchar(500) DEFAULT NULL COMMENT ''授权放行原因（证件过期时由管理员带着原因放行，留痕）'' AFTER `assign_time`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
             AND TABLE_NAME = 'logistics_transport_task' AND COLUMN_NAME = 'override_by');
SET @ddl := IF(@col = 0, 'ALTER TABLE `logistics_transport_task` ADD COLUMN `override_by` bigint unsigned DEFAULT NULL COMMENT ''授权放行人（系统用户编号）'' AFTER `override_reason`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
             AND TABLE_NAME = 'logistics_transport_task' AND COLUMN_NAME = 'override_time');
SET @ddl := IF(@col = 0, 'ALTER TABLE `logistics_transport_task` ADD COLUMN `override_time` datetime DEFAULT NULL COMMENT ''授权放行时间'' AFTER `override_by`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
