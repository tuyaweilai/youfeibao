-- 连接字符集：注释里有中文。
SET NAMES utf8mb4;

-- ========================================
-- 付方编号加宽：varchar(20) → varchar(64)
--
-- 起因：`icbc_invoice_order.payer_no` / `icbc_payment_order.payer_no` 装的是工行报文里的
-- `outVendorId`（付方平台外部编号 / 子商户编号），也就是付方档案的 `partnerPayerId`
-- （形如 `PAYER_3412be5815c147eeb5f87938d3e37320`，38 字符），而这两列只有 20 字符。
-- 后果：**预下单只要成功，写订单就会撞「Data too long for column 'payer_no'」**
-- （严格模式下直接失败，非严格模式下静默截断，后者更糟）。
--
-- 单元测试一直没发现，是因为测试建表里 `icbc_invoice_order.payer_no` 写的是 varchar(64)
-- （`icbc_payment_order` 写的是 20）——测试与生产建表不一致，把问题盖住了。两处已一并对齐。
--
-- 幂等：先看当前长度，短于 64 才改。
-- ========================================

SET @needs := (
  SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'icbc_invoice_order'
    AND COLUMN_NAME = 'payer_no' AND CHARACTER_MAXIMUM_LENGTH < 64
);
SET @ddl := IF(@needs > 0,
  'ALTER TABLE `icbc_invoice_order` MODIFY COLUMN `payer_no` varchar(64) NOT NULL COMMENT ''付方编号（付方平台外部编号 / 工行子商户编号 = 回收企业，即付方档案的 partnerPayerId）''',
  'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @needs := (
  SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'icbc_payment_order'
    AND COLUMN_NAME = 'payer_no' AND CHARACTER_MAXIMUM_LENGTH < 64
);
SET @ddl := IF(@needs > 0,
  'ALTER TABLE `icbc_payment_order` MODIFY COLUMN `payer_no` varchar(64) DEFAULT NULL COMMENT ''付方编号（与 icbc_invoice_order.payer_no 同口径）''',
  'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
