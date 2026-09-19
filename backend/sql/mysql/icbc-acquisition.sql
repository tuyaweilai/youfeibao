-- ========================================
-- 收购登记（#7）：一笔收购的合同流 / 货物流 / 信息流骨架
-- 幂等：CREATE TABLE IF NOT EXISTS
-- ========================================

CREATE TABLE IF NOT EXISTS `icbc_acquisition` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `acquisition_no` varchar(64) NOT NULL COMMENT '收购单号（合同编号）',
  `client_request_id` varchar(64) DEFAULT NULL COMMENT '客户端幂等键（离线补传去重）',
  `payee_id` bigint unsigned NOT NULL COMMENT '出售者（收方）档案编号',
  `partner_payee_id` varchar(64) DEFAULT NULL COMMENT '出售者外部编号快照',
  `seller_name` varchar(100) DEFAULT NULL COMMENT '出售者姓名快照',
  `seller_mobile` varchar(32) DEFAULT NULL COMMENT '出售者联系方式快照',
  `goods_config_id` bigint unsigned DEFAULT NULL COMMENT '品类配置编号',
  `category_name` varchar(100) DEFAULT NULL COMMENT '品类名称',
  `unit` varchar(20) DEFAULT NULL COMMENT '计量单位（按品类带出）',
  `tax_rate` decimal(5,4) DEFAULT NULL COMMENT '税率（按品类带出）',
  `tax_method` varchar(20) DEFAULT NULL COMMENT '计税方法：SIMPLE/GENERAL（按品类带出）',
  `merged_code` varchar(19) DEFAULT NULL COMMENT '商品和服务税收分类合并编码（按品类带出）',
  `specification` varchar(100) DEFAULT NULL COMMENT '规格',
  `quantity` decimal(14,4) DEFAULT NULL COMMENT '数量',
  `unit_price` decimal(14,2) DEFAULT NULL COMMENT '含税单价（元）',
  `amount` decimal(14,2) DEFAULT NULL COMMENT '金额（元）',
  `gross_weight` decimal(14,4) DEFAULT NULL COMMENT '毛重',
  `tare_weight` decimal(14,4) DEFAULT NULL COMMENT '皮重',
  `net_weight` decimal(14,4) DEFAULT NULL COMMENT '净重',
  `deduction` decimal(14,4) DEFAULT NULL COMMENT '扣杂原始值（按重量时为重量，按比例时为 0~1）',
  `deduction_method` varchar(10) DEFAULT NULL COMMENT '扣杂录法：WEIGHT-按重量，RATIO-按比例',
  `settlement_weight` decimal(14,4) DEFAULT NULL COMMENT '结算重量 = 毛重 − 皮重 − 扣杂（唯一计价基准）',
  `adjustment_amount` decimal(14,2) DEFAULT NULL COMMENT '调整项（元，可正可负）',
  `adjustment_reason` varchar(200) DEFAULT NULL COMMENT '调整原因（运费/补贴/折让）',
  `quantity_note` varchar(200) DEFAULT NULL COMMENT '数量口径说明',
  `driver_name` varchar(50) DEFAULT NULL COMMENT '司机姓名（运输信息）',
  `driver_mobile` varchar(32) DEFAULT NULL COMMENT '司机手机号（运输信息）',
  `weight_ticket_no` varchar(64) DEFAULT NULL COMMENT '磅单号',
  `weight_ticket_image_url` varchar(500) DEFAULT NULL COMMENT '磅单照片地址',
  `weight_ticket_plate_no` varchar(32) DEFAULT NULL COMMENT '磅单识别车牌',
  `vehicle_plate_no` varchar(32) DEFAULT NULL COMMENT '车头车尾识别车牌',
  `plate_matched` tinyint(1) DEFAULT NULL COMMENT '车牌比对：1-一致，0-不一致，NULL-无法比对',
  `vehicle_front_image_url` varchar(500) DEFAULT NULL COMMENT '车头照片地址',
  `vehicle_rear_image_url` varchar(500) DEFAULT NULL COMMENT '车尾照片地址',
  `trade_address` varchar(255) DEFAULT NULL COMMENT '交易地点',
  `trade_time` datetime DEFAULT NULL COMMENT '交易时间',
  `settlement_method` varchar(200) DEFAULT NULL COMMENT '结算方式',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态：0-已登记，1-待付款，2-已付款，3-已开票，9-已取消',
  `invoice_partner_order_id` varchar(64) DEFAULT NULL COMMENT '关联开票合作方订单号',
  `source` varchar(20) DEFAULT NULL COMMENT '登记来源：ONLINE/OFFLINE_SYNC',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `tenant_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '租户编号',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_acquisition_no` (`acquisition_no`),
  UNIQUE KEY `uk_acquisition_client_request` (`tenant_id`, `client_request_id`),
  KEY `idx_acquisition_payee_id` (`payee_id`),
  KEY `idx_acquisition_invoice_order` (`invoice_partner_order_id`),
  KEY `idx_acquisition_trade_time` (`trade_time`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收购登记单';

-- ========================================
-- 计价模型（#32，见 ADR 0019）：结算重量是唯一计价基准，扣杂独立成字段。
-- 幂等：仅当列不存在时 ALTER；历史数据按扣杂 = 0 兼容（旧单金额不回算）。
-- ========================================

SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
             AND TABLE_NAME = 'icbc_acquisition' AND COLUMN_NAME = 'deduction');
SET @ddl := IF(@col = 0, 'ALTER TABLE `icbc_acquisition` ADD COLUMN `deduction` decimal(14,4) DEFAULT NULL COMMENT ''扣杂原始值（按重量时为重量，按比例时为 0~1）'' AFTER `net_weight`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
             AND TABLE_NAME = 'icbc_acquisition' AND COLUMN_NAME = 'deduction_method');
SET @ddl := IF(@col = 0, 'ALTER TABLE `icbc_acquisition` ADD COLUMN `deduction_method` varchar(10) DEFAULT NULL COMMENT ''扣杂录法：WEIGHT-按重量，RATIO-按比例'' AFTER `deduction`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
             AND TABLE_NAME = 'icbc_acquisition' AND COLUMN_NAME = 'settlement_weight');
SET @ddl := IF(@col = 0, 'ALTER TABLE `icbc_acquisition` ADD COLUMN `settlement_weight` decimal(14,4) DEFAULT NULL COMMENT ''结算重量 = 毛重 − 皮重 − 扣杂（唯一计价基准）'' AFTER `deduction_method`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
             AND TABLE_NAME = 'icbc_acquisition' AND COLUMN_NAME = 'adjustment_amount');
SET @ddl := IF(@col = 0, 'ALTER TABLE `icbc_acquisition` ADD COLUMN `adjustment_amount` decimal(14,2) DEFAULT NULL COMMENT ''调整项（元，可正可负）'' AFTER `settlement_weight`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
             AND TABLE_NAME = 'icbc_acquisition' AND COLUMN_NAME = 'adjustment_reason');
SET @ddl := IF(@col = 0, 'ALTER TABLE `icbc_acquisition` ADD COLUMN `adjustment_reason` varchar(200) DEFAULT NULL COMMENT ''调整原因（运费/补贴/折让）'' AFTER `adjustment_amount`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
             AND TABLE_NAME = 'icbc_acquisition' AND COLUMN_NAME = 'quantity_note');
SET @ddl := IF(@col = 0, 'ALTER TABLE `icbc_acquisition` ADD COLUMN `quantity_note` varchar(200) DEFAULT NULL COMMENT ''数量口径说明'' AFTER `adjustment_reason`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
             AND TABLE_NAME = 'icbc_acquisition' AND COLUMN_NAME = 'driver_name');
SET @ddl := IF(@col = 0, 'ALTER TABLE `icbc_acquisition` ADD COLUMN `driver_name` varchar(50) DEFAULT NULL COMMENT ''司机姓名（运输信息）'' AFTER `quantity_note`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
             AND TABLE_NAME = 'icbc_acquisition' AND COLUMN_NAME = 'driver_mobile');
SET @ddl := IF(@col = 0, 'ALTER TABLE `icbc_acquisition` ADD COLUMN `driver_mobile` varchar(32) DEFAULT NULL COMMENT ''司机手机号（运输信息）'' AFTER `driver_name`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 发票明细的数量口径说明（ADR 0019）：结算重量计价后发票「数量」与磅单「净重」不再相等
SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
             AND TABLE_NAME = 'icbc_order_item' AND COLUMN_NAME = 'quantity_note');
SET @ddl := IF(@col = 0, 'ALTER TABLE `icbc_order_item` ADD COLUMN `quantity_note` varchar(200) DEFAULT NULL COMMENT ''数量口径说明（结算重量计价后发票数量与磅单净重的差异）'' AFTER `tax_amount`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
