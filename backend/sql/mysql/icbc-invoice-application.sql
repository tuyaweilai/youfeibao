-- ========================================
-- 开票申请（#8）：预下单与自然人确认
-- 幂等：字段与索引用 information_schema 判断后再 ALTER
-- ========================================

-- 1. icbc_invoice_order 补来源收购单、自然人确认状态、预开票状态
SET @col_exists := (
  SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'icbc_invoice_order' AND COLUMN_NAME = 'acquisition_id'
);
SET @ddl := IF(@col_exists = 0,
  'ALTER TABLE `icbc_invoice_order` ADD COLUMN `acquisition_id` bigint DEFAULT NULL COMMENT ''来源收购单编号（一个收购单对应一张票）'' AFTER `partner_order_id`',
  'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists := (
  SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'icbc_invoice_order' AND COLUMN_NAME = 'confirm_status'
);
SET @ddl := IF(@col_exists = 0,
  'ALTER TABLE `icbc_invoice_order` ADD COLUMN `confirm_status` tinyint NOT NULL DEFAULT 0 COMMENT ''自然人确认状态：0-未确认，1-自然人确认完成，2-全部确认完成'' AFTER `tax_status`',
  'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists := (
  SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'icbc_invoice_order' AND COLUMN_NAME = 'pre_invoice_status'
);
SET @ddl := IF(@col_exists = 0,
  'ALTER TABLE `icbc_invoice_order` ADD COLUMN `pre_invoice_status` tinyint NOT NULL DEFAULT 0 COMMENT ''预开票状态：0-初始，1-预开票中，2-预开票成功，3-预开票失败，4-预开票取消'' AFTER `confirm_status`',
  'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists := (
  SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'icbc_invoice_order' AND COLUMN_NAME = 'pre_order_time'
);
SET @ddl := IF(@col_exists = 0,
  'ALTER TABLE `icbc_invoice_order` ADD COLUMN `pre_order_time` datetime DEFAULT NULL COMMENT ''预下单发起时间'' AFTER `pre_invoice_status`',
  'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'icbc_invoice_order' AND INDEX_NAME = 'idx_acquisition_id'
);
SET @ddl := IF(@idx_exists = 0,
  'ALTER TABLE `icbc_invoice_order` ADD INDEX `idx_acquisition_id` (`acquisition_id`)',
  'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. 菜单与按钮权限：开票申请（预下单与自然人确认）
DELETE FROM `system_menu` WHERE `permission` IN ('icbc:invoice-application:apply', 'icbc:invoice-application:query');

INSERT INTO `system_menu`
  (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
   `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
  ('开票申请', 'icbc:invoice-application:query', 2, 6, 5100, 'invoice-application', '',
   'icbc/invoiceApplication/index', 'IcbcInvoiceApplication', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0');

SET @menu_id = LAST_INSERT_ID();

INSERT INTO `system_menu`
  (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
   `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
  ('发起开票申请', 'icbc:invoice-application:apply', 3, 1, @menu_id, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0');
