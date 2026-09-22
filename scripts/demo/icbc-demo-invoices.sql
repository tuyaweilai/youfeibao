-- ========================================
-- 本地演示数据：一组「已付款 + 已开票 + 已下载 PDF」的收购单
--
-- 用途：后台「财务票务 / 发票下载与证据」列表、业务追溯（发票流证据）、自然人端发票记录
--       都可以用这几条来自检；不再靠手抄订单号造数据。
--
-- 卖家：周忍法（payee 2 / naturalPerson 2，租户 1）；付款方：付方档案 1。
-- 生成物：icbc_acquisition（已开票）+ icbc_invoice_order（已开票 / 已缴税 / 已上传）
--        + icbc_payment_order（支付成功 + 回单）+ icbc_invoice_download / icbc_invoice_file
--        （指向 /tmp/icbc-demo 下由 make-demo-invoice-pdf.py 生成的演示 PDF）。
--
-- PDF 必须先由 `python3 scripts/demo/make-demo-invoice-pdf.py` 生成，
-- 或者直接跑 `bash scripts/demo/seed-icbc-demo-invoices.sh`（它会把 file_size 校准成磁盘真实大小：
-- downloadFile 拿 file_size 当 Content-Length，对不上浏览器会截断下载）。
--
-- 幂等：每条 INSERT 都按业务号判重，重复执行不会重复插入。
-- ========================================

SET NAMES utf8mb4;

-- ---------- 收购单 #1 / #2：早就有的两条（废钢 / 废铝）----------
INSERT INTO `icbc_acquisition`
(`acquisition_no`, `payee_id`, `seller_subject_type`, `partner_payee_id`, `seller_name`, `seller_mobile`,
 `goods_config_id`, `category_name`, `unit`, `tax_rate`, `tax_method`, `merged_code`,
 `quantity`, `unit_price`, `amount`, `gross_weight`, `tare_weight`, `net_weight`,
 `deduction`, `deduction_method`, `settlement_weight`, `accepted_weight`, `rejected_weight`, `residual_weight`,
 `weight_diff`, `document_status`, `station_id`, `settlement_id`, `trade_address`, `trade_time`, `settlement_method`,
 `status`, `invoice_partner_order_id`, `source`, `tenant_id`, `creator`, `updater`)
SELECT 'ACQ20260922DEMO01', 2, 1, 'PAYEE_1789828661728_CC25247A', '周忍法', '13996130884',
       1, '废钢', '吨', 0.0100, 'SIMPLE', '1090101010000000000',
       11.85, 2600.00, 30420.00, 18.20, 6.35, 11.85,
       0.15, 'WEIGHT', 11.70, 11.70, 0.00, 0.00,
       0.00, 'COMPLETE', 1, 4, '重庆市渝北区双凤桥街道空港大道 100 号',
       DATE_SUB(NOW(), INTERVAL 6 DAY), '银行转账，过磅后 3 日内结清',
       3, 'ACQ20260922DEMO01', 'ONLINE', 1, '1', '1'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `icbc_acquisition` WHERE `acquisition_no` = 'ACQ20260922DEMO01');

INSERT INTO `icbc_acquisition`
(`acquisition_no`, `payee_id`, `seller_subject_type`, `partner_payee_id`, `seller_name`, `seller_mobile`,
 `goods_config_id`, `category_name`, `unit`, `tax_rate`, `tax_method`, `merged_code`,
 `quantity`, `unit_price`, `amount`, `gross_weight`, `tare_weight`, `net_weight`,
 `deduction`, `deduction_method`, `settlement_weight`, `accepted_weight`, `rejected_weight`, `residual_weight`,
 `weight_diff`, `document_status`, `station_id`, `settlement_id`, `trade_address`, `trade_time`, `settlement_method`,
 `status`, `invoice_partner_order_id`, `source`, `tenant_id`, `creator`, `updater`)
SELECT 'ACQ20260922DEMO02', 2, 1, 'PAYEE_1789828661728_CC25247A', '周忍法', '13996130884',
       1, '废铝', '吨', 0.0100, 'SIMPLE', '1090101010000000000',
       2.20, 13000.00, 27950.00, 3.40, 1.20, 2.20,
       0.05, 'WEIGHT', 2.15, 2.15, 0.00, 0.00,
       0.00, 'COMPLETE', 1, 4, '重庆市渝北区双凤桥街道空港大道 100 号',
       DATE_SUB(NOW(), INTERVAL 3 DAY), '银行转账，过磅后 3 日内结清',
       3, 'ACQ20260922DEMO02', 'ONLINE', 1, '1', '1'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `icbc_acquisition` WHERE `acquisition_no` = 'ACQ20260922DEMO02');

-- ---------- 收购单 #3 / #4：本次新增（让下载列表有 4 张可下载的票）----------
INSERT INTO `icbc_acquisition`
(`acquisition_no`, `payee_id`, `seller_subject_type`, `partner_payee_id`, `seller_name`, `seller_mobile`,
 `goods_config_id`, `category_name`, `unit`, `tax_rate`, `tax_method`, `merged_code`,
 `quantity`, `unit_price`, `amount`, `gross_weight`, `tare_weight`, `net_weight`,
 `deduction`, `deduction_method`, `settlement_weight`, `accepted_weight`, `rejected_weight`, `residual_weight`,
 `weight_diff`, `document_status`, `station_id`, `settlement_id`, `trade_address`, `trade_time`, `settlement_method`,
 `status`, `invoice_partner_order_id`, `source`, `tenant_id`, `creator`, `updater`)
SELECT 'ACQ20260922DEMO04', 2, 1, 'PAYEE_1789828661728_CC25247A', '周忍法', '13996130884',
       1, '废钢', '吨', 0.0100, 'SIMPLE', '1090101010000000000',
       8.00, 2500.00, 20000.00, 12.60, 4.45, 8.15,
       0.15, 'WEIGHT', 8.00, 8.00, 0.00, 0.00,
       0.00, 'COMPLETE', 1, 4, '重庆市渝北区双凤桥街道空港大道 100 号',
       DATE_SUB(NOW(), INTERVAL 4 DAY), '银行转账，过磅后 3 日内结清',
       3, 'ACQ20260922DEMO04', 'ONLINE', 1, '1', '1'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `icbc_acquisition` WHERE `acquisition_no` = 'ACQ20260922DEMO04');

INSERT INTO `icbc_acquisition`
(`acquisition_no`, `payee_id`, `seller_subject_type`, `partner_payee_id`, `seller_name`, `seller_mobile`,
 `goods_config_id`, `category_name`, `unit`, `tax_rate`, `tax_method`, `merged_code`,
 `quantity`, `unit_price`, `amount`, `gross_weight`, `tare_weight`, `net_weight`,
 `deduction`, `deduction_method`, `settlement_weight`, `accepted_weight`, `rejected_weight`, `residual_weight`,
 `weight_diff`, `document_status`, `station_id`, `settlement_id`, `trade_address`, `trade_time`, `settlement_method`,
 `status`, `invoice_partner_order_id`, `source`, `tenant_id`, `creator`, `updater`)
SELECT 'ACQ20260922DEMO05', 2, 1, 'PAYEE_1789828661728_CC25247A', '周忍法', '13996130884',
       1, '废钢', '吨', 0.0100, 'SIMPLE', '1090101010000000000',
       25.00, 2400.00, 60000.00, 30.20, 5.05, 25.15,
       0.15, 'WEIGHT', 25.00, 25.00, 0.00, 0.00,
       0.00, 'COMPLETE', 1, 4, '重庆市渝北区双凤桥街道空港大道 100 号',
       DATE_SUB(NOW(), INTERVAL 2 DAY), '银行转账，过磅后 3 日内结清',
       3, 'ACQ20260922DEMO05', 'ONLINE', 1, '1', '1'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `icbc_acquisition` WHERE `acquisition_no` = 'ACQ20260922DEMO05');

-- ---------- 开票单：已开票 + 已缴税 + 已上传（order_status 4）----------
INSERT INTO `icbc_invoice_order`
(`order_no`, `partner_order_id`, `acquisition_id`, `payee_id`, `payee_no`, `payer_id`, `payer_no`,
 `total_amount`, `tax_rate`, `invoice_type`, `business_type`, `order_status`,
 `invoice_status`, `payment_status`, `tax_status`, `upload_status`,
 `tax_real_amount`, `tax_time`, `tax_payment_method`, `tax_voucher_no`,
 `confirm_status`, `pre_invoice_status`, `pre_order_time`,
 `invoice_no`, `invoice_code`, `invoice_date`, `invoice_amount`, `tax_amount`,
 `tenant_id`, `creator`, `updater`)
SELECT 'INV20260922DEMO01', a.`acquisition_no`, a.`id`, a.`payee_id`, 'NP62F532FBB43F11F18D6C863246C1563F',
       1, 'PAYER_3412be5815c147eeb5f87938d3e37320',
       30420.00, 0.0100, 1, 'SCRAP', 4,
       2, 2, 2, 4,
       301.19, DATE_SUB(NOW(), INTERVAL 4 DAY), '1', 'TAX20260922DEMO01',
       2, 2, DATE_SUB(NOW(), INTERVAL 6 DAY),
       '25500123456789012341', '050001900111', DATE_SUB(NOW(), INTERVAL 4 DAY), 30420.00, 301.19,
       1, '1', '1'
FROM `icbc_acquisition` a
WHERE a.`acquisition_no` = 'ACQ20260922DEMO01'
  AND NOT EXISTS (SELECT 1 FROM `icbc_invoice_order` WHERE `partner_order_id` = 'ACQ20260922DEMO01');

INSERT INTO `icbc_invoice_order`
(`order_no`, `partner_order_id`, `acquisition_id`, `payee_id`, `payee_no`, `payer_id`, `payer_no`,
 `total_amount`, `tax_rate`, `invoice_type`, `business_type`, `order_status`,
 `invoice_status`, `payment_status`, `tax_status`, `upload_status`,
 `tax_real_amount`, `tax_time`, `tax_payment_method`, `tax_voucher_no`,
 `confirm_status`, `pre_invoice_status`, `pre_order_time`,
 `invoice_no`, `invoice_code`, `invoice_date`, `invoice_amount`, `tax_amount`,
 `tenant_id`, `creator`, `updater`)
SELECT 'INV20260922DEMO02', a.`acquisition_no`, a.`id`, a.`payee_id`, 'NP62F532FBB43F11F18D6C863246C1563F',
       1, 'PAYER_3412be5815c147eeb5f87938d3e37320',
       27950.00, 0.0100, 1, 'SCRAP', 4,
       2, 2, 2, 4,
       276.73, DATE_SUB(NOW(), INTERVAL 2 DAY), '1', 'TAX20260922DEMO02',
       2, 2, DATE_SUB(NOW(), INTERVAL 3 DAY),
       '25500123456789012342', '050001900111', DATE_SUB(NOW(), INTERVAL 2 DAY), 27950.00, 276.73,
       1, '1', '1'
FROM `icbc_acquisition` a
WHERE a.`acquisition_no` = 'ACQ20260922DEMO02'
  AND NOT EXISTS (SELECT 1 FROM `icbc_invoice_order` WHERE `partner_order_id` = 'ACQ20260922DEMO02');

INSERT INTO `icbc_invoice_order`
(`order_no`, `partner_order_id`, `acquisition_id`, `payee_id`, `payee_no`, `payer_id`, `payer_no`,
 `total_amount`, `tax_rate`, `invoice_type`, `business_type`, `order_status`,
 `invoice_status`, `payment_status`, `tax_status`, `upload_status`,
 `tax_real_amount`, `tax_time`, `tax_payment_method`, `tax_voucher_no`,
 `confirm_status`, `pre_invoice_status`, `pre_order_time`,
 `invoice_no`, `invoice_code`, `invoice_date`, `invoice_amount`, `tax_amount`,
 `tenant_id`, `creator`, `updater`)
SELECT 'INV20260922DEMO04', a.`acquisition_no`, a.`id`, a.`payee_id`, 'NP62F532FBB43F11F18D6C863246C1563F',
       1, 'PAYER_3412be5815c147eeb5f87938d3e37320',
       20000.00, 0.0100, 1, 'SCRAP', 4,
       2, 2, 2, 4,
       198.02, DATE_SUB(NOW(), INTERVAL 2 DAY), '1', 'TAX20260922DEMO04',
       2, 2, DATE_SUB(NOW(), INTERVAL 4 DAY),
       '25500123456789012343', '050001900111', DATE_SUB(NOW(), INTERVAL 2 DAY), 20000.00, 198.02,
       1, '1', '1'
FROM `icbc_acquisition` a
WHERE a.`acquisition_no` = 'ACQ20260922DEMO04'
  AND NOT EXISTS (SELECT 1 FROM `icbc_invoice_order` WHERE `partner_order_id` = 'ACQ20260922DEMO04');

INSERT INTO `icbc_invoice_order`
(`order_no`, `partner_order_id`, `acquisition_id`, `payee_id`, `payee_no`, `payer_id`, `payer_no`,
 `total_amount`, `tax_rate`, `invoice_type`, `business_type`, `order_status`,
 `invoice_status`, `payment_status`, `tax_status`, `upload_status`,
 `tax_real_amount`, `tax_time`, `tax_payment_method`, `tax_voucher_no`,
 `confirm_status`, `pre_invoice_status`, `pre_order_time`,
 `invoice_no`, `invoice_code`, `invoice_date`, `invoice_amount`, `tax_amount`,
 `tenant_id`, `creator`, `updater`)
SELECT 'INV20260922DEMO05', a.`acquisition_no`, a.`id`, a.`payee_id`, 'NP62F532FBB43F11F18D6C863246C1563F',
       1, 'PAYER_3412be5815c147eeb5f87938d3e37320',
       60000.00, 0.0100, 1, 'SCRAP', 4,
       2, 2, 2, 4,
       594.06, DATE_SUB(NOW(), INTERVAL 1 DAY), '1', 'TAX20260922DEMO05',
       2, 2, DATE_SUB(NOW(), INTERVAL 2 DAY),
       '25500123456789012344', '050001900111', DATE_SUB(NOW(), INTERVAL 1 DAY), 60000.00, 594.06,
       1, '1', '1'
FROM `icbc_acquisition` a
WHERE a.`acquisition_no` = 'ACQ20260922DEMO05'
  AND NOT EXISTS (SELECT 1 FROM `icbc_invoice_order` WHERE `partner_order_id` = 'ACQ20260922DEMO05');

-- ---------- 支付单：支付成功 + 转账回单（资金流证据）----------
INSERT INTO `icbc_payment_order`
(`order_no`, `partner_order_id`, `acquisition_id`, `invoice_order_id`, `icbc_order_no`,
 `payee_no`, `payer_no`, `payment_amount`, `payment_status`, `pay_status`,
 `payment_time`, `payment_serial_no`, `actually_received_amount`, `receipt_no`, `receipt_time`,
 `retry_count`, `tenant_id`, `creator`, `updater`)
SELECT 'PAY20260922DEMO01', a.`acquisition_no`, a.`id`, o.`id`, 'ICBC20260922DEMO01',
       'NP62F532FBB43F11F18D6C863246C1563F', 'PAYER_3412be5815c147eeb5f87938d3e37320',
       30420.00, 2, '02',
       DATE_SUB(NOW(), INTERVAL 5 DAY), 'SER20260922DEMO01', 30420.00,
       'REC20260922DEMO01', DATE_SUB(NOW(), INTERVAL 5 DAY),
       0, 1, '1', '1'
FROM `icbc_acquisition` a
JOIN `icbc_invoice_order` o ON o.`partner_order_id` = a.`acquisition_no`
WHERE a.`acquisition_no` = 'ACQ20260922DEMO01'
  AND NOT EXISTS (SELECT 1 FROM `icbc_payment_order` WHERE `partner_order_id` = 'ACQ20260922DEMO01');

INSERT INTO `icbc_payment_order`
(`order_no`, `partner_order_id`, `acquisition_id`, `invoice_order_id`, `icbc_order_no`,
 `payee_no`, `payer_no`, `payment_amount`, `payment_status`, `pay_status`,
 `payment_time`, `payment_serial_no`, `actually_received_amount`, `receipt_no`, `receipt_time`,
 `retry_count`, `tenant_id`, `creator`, `updater`)
SELECT 'PAY20260922DEMO02', a.`acquisition_no`, a.`id`, o.`id`, 'ICBC20260922DEMO02',
       'NP62F532FBB43F11F18D6C863246C1563F', 'PAYER_3412be5815c147eeb5f87938d3e37320',
       27950.00, 2, '02',
       DATE_SUB(NOW(), INTERVAL 3 DAY), 'SER20260922DEMO02', 27950.00,
       'REC20260922DEMO02', DATE_SUB(NOW(), INTERVAL 3 DAY),
       0, 1, '1', '1'
FROM `icbc_acquisition` a
JOIN `icbc_invoice_order` o ON o.`partner_order_id` = a.`acquisition_no`
WHERE a.`acquisition_no` = 'ACQ20260922DEMO02'
  AND NOT EXISTS (SELECT 1 FROM `icbc_payment_order` WHERE `partner_order_id` = 'ACQ20260922DEMO02');

INSERT INTO `icbc_payment_order`
(`order_no`, `partner_order_id`, `acquisition_id`, `invoice_order_id`, `icbc_order_no`,
 `payee_no`, `payer_no`, `payment_amount`, `payment_status`, `pay_status`,
 `payment_time`, `payment_serial_no`, `actually_received_amount`, `receipt_no`, `receipt_time`,
 `retry_count`, `tenant_id`, `creator`, `updater`)
SELECT 'PAY20260922DEMO04', a.`acquisition_no`, a.`id`, o.`id`, 'ICBC20260922DEMO04',
       'NP62F532FBB43F11F18D6C863246C1563F', 'PAYER_3412be5815c147eeb5f87938d3e37320',
       20000.00, 2, '02',
       DATE_SUB(NOW(), INTERVAL 3 DAY), 'SER20260922DEMO04', 20000.00,
       'REC20260922DEMO04', DATE_SUB(NOW(), INTERVAL 3 DAY),
       0, 1, '1', '1'
FROM `icbc_acquisition` a
JOIN `icbc_invoice_order` o ON o.`partner_order_id` = a.`acquisition_no`
WHERE a.`acquisition_no` = 'ACQ20260922DEMO04'
  AND NOT EXISTS (SELECT 1 FROM `icbc_payment_order` WHERE `partner_order_id` = 'ACQ20260922DEMO04');

INSERT INTO `icbc_payment_order`
(`order_no`, `partner_order_id`, `acquisition_id`, `invoice_order_id`, `icbc_order_no`,
 `payee_no`, `payer_no`, `payment_amount`, `payment_status`, `pay_status`,
 `payment_time`, `payment_serial_no`, `actually_received_amount`, `receipt_no`, `receipt_time`,
 `retry_count`, `tenant_id`, `creator`, `updater`)
SELECT 'PAY20260922DEMO05', a.`acquisition_no`, a.`id`, o.`id`, 'ICBC20260922DEMO05',
       'NP62F532FBB43F11F18D6C863246C1563F', 'PAYER_3412be5815c147eeb5f87938d3e37320',
       60000.00, 2, '02',
       DATE_SUB(NOW(), INTERVAL 2 DAY), 'SER20260922DEMO05', 60000.00,
       'REC20260922DEMO05', DATE_SUB(NOW(), INTERVAL 2 DAY),
       0, 1, '1', '1'
FROM `icbc_acquisition` a
JOIN `icbc_invoice_order` o ON o.`partner_order_id` = a.`acquisition_no`
WHERE a.`acquisition_no` = 'ACQ20260922DEMO05'
  AND NOT EXISTS (SELECT 1 FROM `icbc_payment_order` WHERE `partner_order_id` = 'ACQ20260922DEMO05');

-- ---------- 下载记录与文件：指向 /tmp/icbc-demo 下的演示 PDF ----------
-- file_size 先给 0，由 seed-icbc-demo-invoices.sh 按磁盘真实大小回填（单独跑本 SQL 时请一并跑那个 sh）
INSERT INTO `icbc_invoice_download`
(`invoice_order_id`, `partner_order_id`, `order_number`, `invoice_number`,
 `file_path`, `file_name`, `file_size`, `download_status`, `download_time`,
 `retry_count`, `tenant_id`, `creator`, `updater`)
SELECT o.`id`, o.`partner_order_id`, o.`order_no`, o.`invoice_no`,
       CONCAT('/tmp/icbc-demo/', o.`partner_order_id`, '-invoice.pdf'),
       CONCAT(o.`partner_order_id`, '-invoice.pdf'), 0,
       2, DATE_SUB(NOW(), INTERVAL 4 DAY), 0, 1, '1', '1'
FROM `icbc_invoice_order` o
WHERE o.`partner_order_id` IN
      ('ACQ20260922DEMO01', 'ACQ20260922DEMO02', 'ACQ20260922DEMO04', 'ACQ20260922DEMO05')
  AND NOT EXISTS (SELECT 1 FROM `icbc_invoice_download` d WHERE d.`partner_order_id` = o.`partner_order_id`);

INSERT INTO `icbc_invoice_file`
(`download_id`, `invoice_number`, `file_type`, `file_path`, `file_name`, `file_size`, `upload_time`,
 `tenant_id`, `creator`, `updater`)
SELECT d.`id`, d.`invoice_number`, 'PDF', d.`file_path`, d.`file_name`, d.`file_size`,
       d.`download_time`, 1, '1', '1'
FROM `icbc_invoice_download` d
WHERE d.`partner_order_id` IN
      ('ACQ20260922DEMO01', 'ACQ20260922DEMO02', 'ACQ20260922DEMO04', 'ACQ20260922DEMO05')
  AND NOT EXISTS (SELECT 1 FROM `icbc_invoice_file` f WHERE f.`download_id` = d.`id` AND f.`file_type` = 'PDF');

-- ---------- 自检：每笔收购应有 开票单 + 支付单 + 下载记录 + 1 个 PDF 文件 ----------
SELECT a.`acquisition_no`, a.`category_name`, a.`amount`, a.`status` AS acquisition_status,
       o.`order_status`, o.`invoice_status`, o.`payment_status`, o.`invoice_no`,
       p.`payment_status` AS payment_platform_status, p.`receipt_no`,
       d.`download_status`, d.`file_name`,
       (SELECT COUNT(1) FROM `icbc_invoice_file` f WHERE f.`download_id` = d.`id`) AS pdf_files
FROM `icbc_acquisition` a
JOIN `icbc_invoice_order` o ON o.`partner_order_id` = a.`acquisition_no`
JOIN `icbc_payment_order` p ON p.`partner_order_id` = a.`acquisition_no`
LEFT JOIN `icbc_invoice_download` d ON d.`partner_order_id` = a.`acquisition_no`
WHERE a.`acquisition_no` LIKE 'ACQ20260922DEMO%'
ORDER BY a.`acquisition_no`;
