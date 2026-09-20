-- 连接字符集：文件里有中文种子数据。客户端默认不是 utf8mb4 时（例如没有 LANG 的
-- `docker exec -i <mysql容器> mysql`，其默认是 latin1），中文会被双重编码存进库，
-- 界面上就是乱码。这里显式声明，导入时不必再依赖客户端参数。
SET NAMES utf8mb4;

-- ========================================
-- 收购单关联采购安排与「直接收购」（#51 T13，见 docs/adr/0027 与 CONTEXT.md「采购订单」）
--
-- 每张收购单可选关联一条采购订单明细（执行中且未过期的采购订单），也可以不关联：
--   * 关联：收购单挂 purchase_order_id + purchase_order_item_id，采购进度（#47）能按明细归集；
--   * 不关联：报表 / 列表标为「直接收购」，**不是失败也不是缺失**，零散散户不必虚造订单。
--
-- 为什么用 NOT NULL DEFAULT 0 而不是 NULL：
--   NULL 在唯一索引里互不相等，会给后续「同一业务单据只写一次」的幂等约束埋坑（ADR 0027 / #43 的教训）；
--   而 0 是稳定哨兵值，语义就是「未关联」。历史数据与不传该字段的调用方自然落到 0。
--
-- 幂等：仅当列不存在时 ALTER；已进 README 导入顺序。菜单与按钮权限不新增
--   （复用既有的 icbc:acquisition:* 与 icbc:purchase-order:query）。
-- ========================================

SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
             AND TABLE_NAME = 'icbc_acquisition' AND COLUMN_NAME = 'purchase_order_id');
SET @ddl := IF(@col = 0, 'ALTER TABLE `icbc_acquisition` ADD COLUMN `purchase_order_id` bigint unsigned NOT NULL DEFAULT 0 COMMENT ''关联的采购订单编号（0 = 未关联，即直接收购，#51）'' AFTER `settlement_id`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
             AND TABLE_NAME = 'icbc_acquisition' AND COLUMN_NAME = 'purchase_order_item_id');
SET @ddl := IF(@col = 0, 'ALTER TABLE `icbc_acquisition` ADD COLUMN `purchase_order_item_id` bigint unsigned NOT NULL DEFAULT 0 COMMENT ''关联的采购订单明细编号（0 = 未关联，#51）'' AFTER `purchase_order_id`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx := (SELECT COUNT(1) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE()
             AND TABLE_NAME = 'icbc_acquisition' AND INDEX_NAME = 'idx_acquisition_purchase_order');
SET @ddl := IF(@idx = 0, 'ALTER TABLE `icbc_acquisition` ADD INDEX `idx_acquisition_purchase_order` (`tenant_id`, `purchase_order_id`)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx := (SELECT COUNT(1) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE()
             AND TABLE_NAME = 'icbc_acquisition' AND INDEX_NAME = 'idx_acquisition_purchase_order_item');
SET @ddl := IF(@idx = 0, 'ALTER TABLE `icbc_acquisition` ADD INDEX `idx_acquisition_purchase_order_item` (`tenant_id`, `purchase_order_item_id`)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
