-- 连接字符集：文件里有中文种子数据。客户端默认不是 utf8mb4 时（例如没有 LANG 的
-- `docker exec -i <mysql容器> mysql`，其默认是 latin1），中文会被双重编码存进库，
-- 界面上就是乱码。这里显式声明，导入时不必再依赖客户端参数。
SET NAMES utf8mb4;

-- ========================================
-- 结算确认后自动预下单（#106，ADR 0039）
-- 幂等：字段用 information_schema 判断后再 ALTER
-- ========================================

-- 1. 本企业开票参数：开票人实名与应税行为发生地
--    这三项是工行预下单的必输项，过去只有开票员在 PC 表单上手填；
--    自动预下单发生在自然人的手机上，那一刻没有开票员在场，所以必须落成租户配置。
--    落在付方档案上：它天然一租户一行，语义就是「本企业的开票主体」。
SET @col_exists := (
  SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'icbc_payer_info' AND COLUMN_NAME = 'drawer_name'
);
SET @ddl := IF(@col_exists = 0,
  'ALTER TABLE `icbc_payer_info` ADD COLUMN `drawer_name` varchar(200) DEFAULT NULL COMMENT ''开票人姓名（须与工行税务登记的开票员为同一实名主体）'' AFTER `taxpayer_type`',
  'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists := (
  SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'icbc_payer_info' AND COLUMN_NAME = 'drawer_card_number'
);
SET @ddl := IF(@col_exists = 0,
  'ALTER TABLE `icbc_payer_info` ADD COLUMN `drawer_card_number` varchar(30) DEFAULT NULL COMMENT ''开票人证件号码'' AFTER `drawer_name`',
  'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists := (
  SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'icbc_payer_info' AND COLUMN_NAME = 'area_code'
);
SET @ddl := IF(@col_exists = 0,
  'ALTER TABLE `icbc_payer_info` ADD COLUMN `area_code` varchar(11) DEFAULT NULL COMMENT ''应税行为发生地（省级税务机关代码，如 110000）'' AFTER `drawer_card_number`',
  'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. 自然人确认页面的表单 HTML
--    工行预下单是 UI 接口，返回一段自动提交到工行的表单；过去只当场回给调用方，
--    自然人在手机上关掉页面就再也拿不到了。落库后可以按需重开（同一会话内重开是安全的；
--    跨天重开依赖工行的签名时效，超时要重新发起预下单，见 isRedo 待办）。
SET @col_exists := (
  SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'icbc_invoice_order' AND COLUMN_NAME = 'confirm_page_html'
);
SET @ddl := IF(@col_exists = 0,
  'ALTER TABLE `icbc_invoice_order` ADD COLUMN `confirm_page_html` text COMMENT ''自然人确认页面的工行表单 HTML（预下单返回，供按需重开）'' AFTER `pre_order_time`',
  'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
