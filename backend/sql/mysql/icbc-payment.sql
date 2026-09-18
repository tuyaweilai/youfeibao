-- ========================================
-- 付款：付方支付与资金流回单归档（#9）
-- 幂等：字段与索引用 information_schema 判断后再 ALTER
-- ========================================

-- 1. icbc_payment_order 补来源收购单 / 开票单、工行 payStatus、回单与重试次数
SET @col_exists := (
  SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'icbc_payment_order' AND COLUMN_NAME = 'acquisition_id'
);
SET @ddl := IF(@col_exists = 0,
  'ALTER TABLE `icbc_payment_order` ADD COLUMN `acquisition_id` bigint DEFAULT NULL COMMENT ''来源收购单编号'' AFTER `partner_order_id`',
  'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists := (
  SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'icbc_payment_order' AND COLUMN_NAME = 'invoice_order_id'
);
SET @ddl := IF(@col_exists = 0,
  'ALTER TABLE `icbc_payment_order` ADD COLUMN `invoice_order_id` bigint DEFAULT NULL COMMENT ''来源开票订单编号'' AFTER `acquisition_id`',
  'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists := (
  SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'icbc_payment_order' AND COLUMN_NAME = 'pay_status'
);
SET @ddl := IF(@col_exists = 0,
  'ALTER TABLE `icbc_payment_order` ADD COLUMN `pay_status` varchar(8) DEFAULT NULL COMMENT ''工行原始支付状态码：-1/00/01/02/03/04/05/06/07/12/25'' AFTER `payment_status`',
  'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists := (
  SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'icbc_payment_order' AND COLUMN_NAME = 'actually_received_amount'
);
SET @ddl := IF(@col_exists = 0,
  'ALTER TABLE `icbc_payment_order` ADD COLUMN `actually_received_amount` decimal(14,2) DEFAULT NULL COMMENT ''实际到账金额'' AFTER `payment_serial_no`',
  'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists := (
  SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'icbc_payment_order' AND COLUMN_NAME = 'receipt_no'
);
SET @ddl := IF(@col_exists = 0,
  'ALTER TABLE `icbc_payment_order` ADD COLUMN `receipt_no` varchar(64) DEFAULT NULL COMMENT ''转账回单号'' AFTER `actually_received_amount`',
  'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists := (
  SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'icbc_payment_order' AND COLUMN_NAME = 'receipt_time'
);
SET @ddl := IF(@col_exists = 0,
  'ALTER TABLE `icbc_payment_order` ADD COLUMN `receipt_time` datetime DEFAULT NULL COMMENT ''转账回单归档时间'' AFTER `receipt_no`',
  'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists := (
  SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'icbc_payment_order' AND COLUMN_NAME = 'receipt_file_url'
);
SET @ddl := IF(@col_exists = 0,
  'ALTER TABLE `icbc_payment_order` ADD COLUMN `receipt_file_url` varchar(500) DEFAULT NULL COMMENT ''转账回单文件地址'' AFTER `receipt_time`',
  'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists := (
  SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'icbc_payment_order' AND COLUMN_NAME = 'retry_count'
);
SET @ddl := IF(@col_exists = 0,
  'ALTER TABLE `icbc_payment_order` ADD COLUMN `retry_count` int DEFAULT 0 COMMENT ''重新发起次数'' AFTER `receipt_file_url`',
  'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'icbc_payment_order' AND INDEX_NAME = 'idx_payment_acquisition_id'
);
SET @ddl := IF(@idx_exists = 0,
  'ALTER TABLE `icbc_payment_order` ADD INDEX `idx_payment_acquisition_id` (`acquisition_id`)',
  'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
