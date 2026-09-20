-- 连接字符集：文件里有中文注释。客户端默认不是 utf8mb4 时（例如没有 LANG 的
-- `docker exec -i <mysql容器> mysql`，其默认是 latin1），中文会被双重编码存进库。
SET NAMES utf8mb4;

-- ========================================
-- 交接登记 → 回场复磅 → 收购单（V6 #73，见 ADR 0030 / 0031 / 0032）
--
-- 上门提货的闭环在两侧之间挂接：物流侧的**交接登记**（`logistics_transport_handover`，
-- 现场参考量与照片凭证）在**回场复磅**时由磅房在 icbc 侧建成交接批次（`icbc_handover_batch`），
-- 再按有效磅次生成收购单。本文件给两张 icbc 表补上这一次挂接所需的列：
--
-- 1. `icbc_handover_batch`：
--    * `logistics_handover_id` —— 指向物流侧交接登记，两侧唯一挂接点（物流只存 icbc 侧编号，
--      icbc 反查物流读取面，方向恒为 icbc → 物流，ADR 0032）；
--    * `driver_id` / `vehicle_id` —— 司机与车辆**引用 + 快照并存**（ADR 0032 第 7 条）：
--      引用让「这台车跑了多少趟」算得出来，快照让一票一档不被档案改名或删档污染；
--    * `document_status` / `document_gap` —— 要件是否齐备（待补档继承到收购单）；
--    * `reference_quantity` / `reference_unit_price` —— 现场参考值与参考单价快照，
--      磅房在批次详情里就能看到，生成收购单时作为单价默认值（不看物流也能生成）。
--
-- 2. `icbc_acquisition`：把上面这些落到收购事实上（`logistics_handover_id` / `driver_id` /
--    `vehicle_id` / `document_status` / `document_gap` / `reference_quantity` /
--    `reference_unit_price` / `reference_fix_reason` / `document_completed_*`）。
--    * 待补档的收购单被付款与开票门禁拦住，**不进开票申请、不进台账口径、不计入额度**；
--      补档后（`complete-documents`）放行，留办理人与时间。
--    * 单价默认取现场参考价；收货员修正参考价或参考量时**必须填原因**（`reference_fix_reason`），
--      改动被解释而不是被抹平。
--
-- 幂等：ADD COLUMN / ADD INDEX 都按 information_schema 判存在再执行。
-- 注意 `--` 后必须跟空格才是注释（本项目已两次踩过 1064）。
-- ========================================

-- ---------------------------------------------------------------------
-- 1. icbc_handover_batch
-- ---------------------------------------------------------------------

SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
             AND TABLE_NAME = 'icbc_handover_batch' AND COLUMN_NAME = 'logistics_handover_id');
SET @ddl := IF(@col = 0, 'ALTER TABLE `icbc_handover_batch` ADD COLUMN `logistics_handover_id` bigint unsigned DEFAULT NULL COMMENT ''物流侧交接登记编号（回场复磅时按现场交接登记建批次）'' AFTER `batch_no`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
             AND TABLE_NAME = 'icbc_handover_batch' AND COLUMN_NAME = 'driver_id');
SET @ddl := IF(@col = 0, 'ALTER TABLE `icbc_handover_batch` ADD COLUMN `driver_id` bigint unsigned DEFAULT NULL COMMENT ''司机编号（物流侧编号，与姓名快照并存）'' AFTER `driver_mobile`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
             AND TABLE_NAME = 'icbc_handover_batch' AND COLUMN_NAME = 'vehicle_id');
SET @ddl := IF(@col = 0, 'ALTER TABLE `icbc_handover_batch` ADD COLUMN `vehicle_id` bigint unsigned DEFAULT NULL COMMENT ''车辆编号（物流侧编号，与车牌快照并存）'' AFTER `driver_id`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
             AND TABLE_NAME = 'icbc_handover_batch' AND COLUMN_NAME = 'document_status');
SET @ddl := IF(@col = 0, 'ALTER TABLE `icbc_handover_batch` ADD COLUMN `document_status` varchar(16) DEFAULT NULL COMMENT ''要件状态：COMPLETE-已齐，PENDING-待补档（缺身份证或银行卡）'' AFTER `vehicle_id`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
             AND TABLE_NAME = 'icbc_handover_batch' AND COLUMN_NAME = 'document_gap');
SET @ddl := IF(@col = 0, 'ALTER TABLE `icbc_handover_batch` ADD COLUMN `document_gap` varchar(128) DEFAULT NULL COMMENT ''缺什么（待补档时说明）'' AFTER `document_status`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
             AND TABLE_NAME = 'icbc_handover_batch' AND COLUMN_NAME = 'reference_quantity');
SET @ddl := IF(@col = 0, 'ALTER TABLE `icbc_handover_batch` ADD COLUMN `reference_quantity` decimal(16,4) DEFAULT NULL COMMENT ''现场参考量快照'' AFTER `document_gap`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
             AND TABLE_NAME = 'icbc_handover_batch' AND COLUMN_NAME = 'reference_unit_price');
SET @ddl := IF(@col = 0, 'ALTER TABLE `icbc_handover_batch` ADD COLUMN `reference_unit_price` decimal(16,4) DEFAULT NULL COMMENT ''现场参考单价快照（生成收购单时的单价默认值）'' AFTER `reference_quantity`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx := (SELECT COUNT(1) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE()
             AND TABLE_NAME = 'icbc_handover_batch' AND INDEX_NAME = 'idx_handover_batch_logistics');
SET @ddl := IF(@idx = 0, 'ALTER TABLE `icbc_handover_batch` ADD INDEX `idx_handover_batch_logistics` (`logistics_handover_id`)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------
-- 2. icbc_acquisition
-- ---------------------------------------------------------------------

SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
             AND TABLE_NAME = 'icbc_acquisition' AND COLUMN_NAME = 'logistics_handover_id');
SET @ddl := IF(@col = 0, 'ALTER TABLE `icbc_acquisition` ADD COLUMN `logistics_handover_id` bigint unsigned DEFAULT NULL COMMENT ''物流侧交接登记编号（本笔收购的现场交接来源）'' AFTER `batch_key`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
             AND TABLE_NAME = 'icbc_acquisition' AND COLUMN_NAME = 'driver_id');
SET @ddl := IF(@col = 0, 'ALTER TABLE `icbc_acquisition` ADD COLUMN `driver_id` bigint unsigned DEFAULT NULL COMMENT ''司机编号（物流侧编号，与姓名快照并存）'' AFTER `logistics_handover_id`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
             AND TABLE_NAME = 'icbc_acquisition' AND COLUMN_NAME = 'vehicle_id');
SET @ddl := IF(@col = 0, 'ALTER TABLE `icbc_acquisition` ADD COLUMN `vehicle_id` bigint unsigned DEFAULT NULL COMMENT ''车辆编号（物流侧编号，与车牌快照并存）'' AFTER `driver_id`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
             AND TABLE_NAME = 'icbc_acquisition' AND COLUMN_NAME = 'document_status');
SET @ddl := IF(@col = 0, 'ALTER TABLE `icbc_acquisition` ADD COLUMN `document_status` varchar(16) DEFAULT NULL COMMENT ''要件状态：COMPLETE-已齐，PENDING-待补档（付款与开票门禁拦住，不进开票申请与额度）'' AFTER `vehicle_id`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
             AND TABLE_NAME = 'icbc_acquisition' AND COLUMN_NAME = 'document_gap');
SET @ddl := IF(@col = 0, 'ALTER TABLE `icbc_acquisition` ADD COLUMN `document_gap` varchar(128) DEFAULT NULL COMMENT ''缺什么（待补档时说明）'' AFTER `document_status`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
             AND TABLE_NAME = 'icbc_acquisition' AND COLUMN_NAME = 'reference_quantity');
SET @ddl := IF(@col = 0, 'ALTER TABLE `icbc_acquisition` ADD COLUMN `reference_quantity` decimal(16,4) DEFAULT NULL COMMENT ''现场参考量快照（不是计量事实）'' AFTER `document_gap`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
             AND TABLE_NAME = 'icbc_acquisition' AND COLUMN_NAME = 'reference_unit_price');
SET @ddl := IF(@col = 0, 'ALTER TABLE `icbc_acquisition` ADD COLUMN `reference_unit_price` decimal(16,4) DEFAULT NULL COMMENT ''现场参考单价快照（未修正时即成交单价）'' AFTER `reference_quantity`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
             AND TABLE_NAME = 'icbc_acquisition' AND COLUMN_NAME = 'reference_fix_reason');
SET @ddl := IF(@col = 0, 'ALTER TABLE `icbc_acquisition` ADD COLUMN `reference_fix_reason` varchar(500) DEFAULT NULL COMMENT ''修正现场参考价 / 参考量的原因（修正必填）'' AFTER `reference_unit_price`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
             AND TABLE_NAME = 'icbc_acquisition' AND COLUMN_NAME = 'document_completed_at');
SET @ddl := IF(@col = 0, 'ALTER TABLE `icbc_acquisition` ADD COLUMN `document_completed_at` datetime DEFAULT NULL COMMENT ''补档完成时间'' AFTER `reference_fix_reason`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
             AND TABLE_NAME = 'icbc_acquisition' AND COLUMN_NAME = 'document_completed_by');
SET @ddl := IF(@col = 0, 'ALTER TABLE `icbc_acquisition` ADD COLUMN `document_completed_by` bigint unsigned DEFAULT NULL COMMENT ''补档办理人（系统用户编号）'' AFTER `document_completed_at`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
             AND TABLE_NAME = 'icbc_acquisition' AND COLUMN_NAME = 'document_complete_remark');
SET @ddl := IF(@col = 0, 'ALTER TABLE `icbc_acquisition` ADD COLUMN `document_complete_remark` varchar(500) DEFAULT NULL COMMENT ''补档说明'' AFTER `document_completed_by`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx := (SELECT COUNT(1) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE()
             AND TABLE_NAME = 'icbc_acquisition' AND INDEX_NAME = 'idx_acquisition_logistics_handover');
SET @ddl := IF(@idx = 0, 'ALTER TABLE `icbc_acquisition` ADD INDEX `idx_acquisition_logistics_handover` (`logistics_handover_id`)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
