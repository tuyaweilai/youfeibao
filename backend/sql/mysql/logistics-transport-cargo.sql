-- 连接字符集：文件里有中文注释。
SET NAMES utf8mb4;

-- ========================================
-- 运输任务加「货物计划提示」三个字段（V2c #79）
--
-- 给司机看的「这趟去拉什么」：`cargo_name` / `estimated_quantity` / `quantity_unit`。
-- **这是计划提示，不是品类权威**：权威品类在交接登记与收购单上（`goods_config_id`，ADR 0028）。
-- 物流模块不引用 icbc 的品类配置（ADR 0032），所以这里只能是文本 + 约量；
-- 页面与接口文案都要说清这一点，别有人拿它当台账口径。
--
-- 幂等：MySQL 8 不支持 `ADD COLUMN IF NOT EXISTS`，所以按 information_schema 判存在再执行
-- （与 `icbc-acquisition-purchase-link.sql` 同一手法）。
-- 注意 `--` 后必须跟空格才是注释：中文全角括号紧跟 `--` 会被 MySQL 当成语句，报 1064。
-- ========================================

SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
             AND TABLE_NAME = 'logistics_transport_task' AND COLUMN_NAME = 'cargo_name');
SET @ddl := IF(@col = 0, 'ALTER TABLE `logistics_transport_task` ADD COLUMN `cargo_name` varchar(100) DEFAULT NULL COMMENT ''货物名称（计划提示，不是品类权威）'' AFTER `purchase_order_no`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
             AND TABLE_NAME = 'logistics_transport_task' AND COLUMN_NAME = 'estimated_quantity');
SET @ddl := IF(@col = 0, 'ALTER TABLE `logistics_transport_task` ADD COLUMN `estimated_quantity` decimal(16,4) DEFAULT NULL COMMENT ''约量（计划提示）'' AFTER `cargo_name`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
             AND TABLE_NAME = 'logistics_transport_task' AND COLUMN_NAME = 'quantity_unit');
SET @ddl := IF(@col = 0, 'ALTER TABLE `logistics_transport_task` ADD COLUMN `quantity_unit` varchar(16) DEFAULT NULL COMMENT ''约量单位'' AFTER `estimated_quantity`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
