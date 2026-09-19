-- 连接字符集：文件里有中文种子数据。客户端默认不是 utf8mb4 时（例如没有 LANG 的
-- `docker exec -i <mysql容器> mysql`，其默认是 latin1），中文会被双重编码存进库，
-- 界面上就是乱码。这里显式声明，导入时不必再依赖客户端参数。
SET NAMES utf8mb4;

-- ========================================
-- 开票、缴税与上传状态（#10）
-- icbc_invoice_order 补上传状态、缴税字段
-- 幂等：字段用 information_schema 判断后再 ALTER
-- ========================================

-- 1. 上传状态：与开票、缴税并列的第三条独立状态线
SET @col_exists := (
  SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'icbc_invoice_order' AND COLUMN_NAME = 'upload_status'
);
SET @ddl := IF(@col_exists = 0,
  'ALTER TABLE `icbc_invoice_order` ADD COLUMN `upload_status` tinyint DEFAULT NULL COMMENT ''发票上传状态：0-未上传，1-处理中，2-已受理，3-上传中，4-上传成功，5-上传失败'' AFTER `tax_status`',
  'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. 实缴税额（缴税凭证）
SET @col_exists := (
  SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'icbc_invoice_order' AND COLUMN_NAME = 'tax_real_amount'
);
SET @ddl := IF(@col_exists = 0,
  'ALTER TABLE `icbc_invoice_order` ADD COLUMN `tax_real_amount` decimal(14,2) DEFAULT NULL COMMENT ''实缴税额（工行 taxRealAmount）'' AFTER `tax_amount`',
  'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3. 缴税时间（工行 tradeTime）
SET @col_exists := (
  SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'icbc_invoice_order' AND COLUMN_NAME = 'tax_time'
);
SET @ddl := IF(@col_exists = 0,
  'ALTER TABLE `icbc_invoice_order` ADD COLUMN `tax_time` datetime DEFAULT NULL COMMENT ''缴税时间（工行 tradeTime）'' AFTER `tax_real_amount`',
  'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 4. 税费缴纳方式：0-自然人自行办理，1-企业委托扣缴
SET @col_exists := (
  SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'icbc_invoice_order' AND COLUMN_NAME = 'tax_payment_method'
);
SET @ddl := IF(@col_exists = 0,
  'ALTER TABLE `icbc_invoice_order` ADD COLUMN `tax_payment_method` varchar(4) DEFAULT NULL COMMENT ''税费缴纳方式：0-自然人自行办理，1-企业委托扣缴'' AFTER `tax_time`',
  'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 5. 应征凭证序号（缴税凭证编号）
SET @col_exists := (
  SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'icbc_invoice_order' AND COLUMN_NAME = 'tax_voucher_no'
);
SET @ddl := IF(@col_exists = 0,
  'ALTER TABLE `icbc_invoice_order` ADD COLUMN `tax_voucher_no` varchar(64) DEFAULT NULL COMMENT ''应征凭证序号（缴税凭证编号）'' AFTER `tax_payment_method`',
  'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 6. 开票状态语义收敛：2 由「开票成功」改为「已开票」，与新枚举一致
ALTER TABLE `icbc_invoice_order`
  MODIFY COLUMN `invoice_status` tinyint NOT NULL DEFAULT 0 COMMENT '开票状态：0-未开票，1-开票中，2-已开票，3-开票失败';
