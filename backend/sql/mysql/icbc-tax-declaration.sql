-- 连接字符集：文件里有中文种子数据。客户端默认不是 utf8mb4 时（例如没有 LANG 的
-- `docker exec -i <mysql容器> mysql`，其默认是 latin1），中文会被双重编码存进库，
-- 界面上就是乱码。这里显式声明，导入时不必再依赖客户端参数。
SET NAMES utf8mb4;

-- ========================================
-- 代办税费申报（#13）
-- 1. icbc_tax_declaration        本租户 × 本申报月的申报单（清单与合计金额）
-- 2. icbc_tax_declaration_item   每个出售者一条明细（含是否超 10 万元）
-- 3. icbc_tax_declaration_invoice 申报单与发票的关联（缴款凭证对应哪些票）
-- 4. icbc_tax_supplement         需补缴税费（按 1% / 3% 分列）
-- 5. icbc_settlement_reminder    出售者汇算清缴提醒与对账
-- 幂等：全部 IF NOT EXISTS；菜单与按钮权限在 icbc-menu.sql 里幂等维护
-- ========================================

CREATE TABLE IF NOT EXISTS `icbc_tax_declaration` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `declaration_no` varchar(64) DEFAULT NULL COMMENT '申报单号',
  `period_month` varchar(7) NOT NULL COMMENT '申报月 yyyy-MM',
  `declaration_deadline` date DEFAULT NULL COMMENT '申报期截止日（次月 15 日）',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态：0-待申报，1-已申报待缴款，2-已缴款',
  `seller_count` int DEFAULT NULL COMMENT '涉及出售者数',
  `over_exempt_seller_count` int DEFAULT NULL COMMENT '当月销售额超 10 万元、需单独列出的出售者数',
  `total_sales_amount` decimal(14,2) DEFAULT NULL COMMENT '当月净销售额合计（元）',
  `amount_at_one_percent` decimal(14,2) DEFAULT NULL COMMENT '按 3% 减按 1% 的销售额（元）',
  `amount_at_three_percent` decimal(14,2) DEFAULT NULL COMMENT '放弃减按、按 3% 的销售额（元）',
  `other_amount` decimal(14,2) DEFAULT NULL COMMENT '征收率未识别的销售额（元）',
  `vat_amount` decimal(14,2) DEFAULT NULL COMMENT '应缴增值税合计（元）',
  `surcharge_amount` decimal(14,2) DEFAULT NULL COMMENT '应缴附加税费合计（元）',
  `iit_amount` decimal(14,2) DEFAULT NULL COMMENT '应缴个人所得税合计（元）',
  `total_tax_amount` decimal(14,2) DEFAULT NULL COMMENT '应缴税费合计（元）',
  `paid_amount` decimal(14,2) DEFAULT NULL COMMENT '实缴金额（元）',
  `declared_at` datetime DEFAULT NULL COMMENT '申报时间',
  `declared_by` varchar(64) DEFAULT NULL COMMENT '申报人',
  `declared_remark` varchar(500) DEFAULT NULL COMMENT '申报备注',
  `paid_at` datetime DEFAULT NULL COMMENT '缴款时间',
  `payment_method` varchar(64) DEFAULT NULL COMMENT '缴纳方式',
  `voucher_no` varchar(64) DEFAULT NULL COMMENT '缴款凭证号',
  `voucher_file_url` varchar(500) DEFAULT NULL COMMENT '缴款凭证文件地址',
  `data_ready` bit(1) DEFAULT b'1' COMMENT '申报数据是否齐备',
  `missing_data_count` int DEFAULT NULL COMMENT '缺项数量',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tax_declaration_period` (`tenant_id`, `period_month`),
  KEY `idx_tax_declaration_deadline` (`declaration_deadline`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='代办税费申报单（#13）';

CREATE TABLE IF NOT EXISTS `icbc_tax_declaration_item` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `declaration_id` bigint DEFAULT NULL COMMENT '申报单编号',
  `period_month` varchar(7) DEFAULT NULL COMMENT '申报月 yyyy-MM',
  `payee_id` bigint DEFAULT NULL COMMENT '出售者档案编号',
  `seller_name` varchar(100) DEFAULT NULL COMMENT '出售者姓名',
  `id_card_no` varchar(32) DEFAULT NULL COMMENT '身份证号码',
  `invoice_count` int DEFAULT NULL COMMENT '当月本租户开票张数',
  `sales_amount` decimal(14,2) DEFAULT NULL COMMENT '当月本租户净销售额（元）',
  `amount_at_one_percent` decimal(14,2) DEFAULT NULL COMMENT '按 3% 减按 1% 的销售额（元）',
  `amount_at_three_percent` decimal(14,2) DEFAULT NULL COMMENT '放弃减按、按 3% 的销售额（元）',
  `other_amount` decimal(14,2) DEFAULT NULL COMMENT '征收率未识别的销售额（元）',
  `cross_tenant_month_amount` decimal(14,2) DEFAULT NULL COMMENT '该自然人当月在平台各租户的净销售额合计（10 万元免征线口径）',
  `vat_exempt` bit(1) DEFAULT NULL COMMENT '增值税是否免征（未超 10 万元）',
  `over_exempt` bit(1) DEFAULT NULL COMMENT '当月销售额（跨租户）是否超 10 万元',
  `vat_amount` decimal(14,2) DEFAULT NULL COMMENT '应缴增值税（元）',
  `surcharge_amount` decimal(14,2) DEFAULT NULL COMMENT '应缴附加税费（元）',
  `iit_amount` decimal(14,2) DEFAULT NULL COMMENT '应缴个人所得税（元）',
  `total_tax_amount` decimal(14,2) DEFAULT NULL COMMENT '应缴税费合计（元）',
  `paid_amount` decimal(14,2) DEFAULT NULL COMMENT '实缴金额（元）',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态，同申报单状态线',
  `paid_at` datetime DEFAULT NULL COMMENT '缴款时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_tax_item_declaration` (`declaration_id`),
  KEY `idx_tax_item_payee` (`payee_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='代办税费申报明细（#13）';

CREATE TABLE IF NOT EXISTS `icbc_tax_declaration_invoice` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `declaration_id` bigint DEFAULT NULL COMMENT '申报单编号',
  `period_month` varchar(7) DEFAULT NULL COMMENT '申报月 yyyy-MM',
  `invoice_order_id` bigint DEFAULT NULL COMMENT '开票订单编号',
  `partner_order_id` varchar(64) DEFAULT NULL COMMENT '合作方订单号',
  `invoice_no` varchar(64) DEFAULT NULL COMMENT '发票号码',
  `payee_id` bigint DEFAULT NULL COMMENT '出售者档案编号',
  `direction` varchar(8) DEFAULT NULL COMMENT '方向：BLUE-蓝票计入，RED-红票冲减',
  `amount` decimal(14,2) DEFAULT NULL COMMENT '金额（元）',
  `tax_rate` decimal(5,4) DEFAULT NULL COMMENT '适用征收率',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_tax_invoice_declaration` (`declaration_id`),
  KEY `idx_tax_invoice_order` (`invoice_order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='申报单与发票关联（#13）';

CREATE TABLE IF NOT EXISTS `icbc_tax_supplement` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `supplement_no` varchar(64) DEFAULT NULL COMMENT '补缴单号',
  `declaration_id` bigint DEFAULT NULL COMMENT '关联申报单编号',
  `period_month` varchar(7) DEFAULT NULL COMMENT '所属申报月 yyyy-MM',
  `payee_id` bigint DEFAULT NULL COMMENT '出售者档案编号',
  `seller_name` varchar(100) DEFAULT NULL COMMENT '出售者姓名',
  `reason` varchar(500) DEFAULT NULL COMMENT '补缴原因',
  `amount_at_one_percent` decimal(14,2) DEFAULT NULL COMMENT '按 3% 减按 1% 部分的补缴金额（元）',
  `amount_at_three_percent` decimal(14,2) DEFAULT NULL COMMENT '放弃减按、按 3% 部分的补缴金额（元）',
  `amount` decimal(14,2) DEFAULT NULL COMMENT '应补缴金额合计（元）',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态：0-待补缴，1-已补缴',
  `paid_amount` decimal(14,2) DEFAULT NULL COMMENT '实缴金额（元）',
  `paid_at` datetime DEFAULT NULL COMMENT '缴款时间',
  `voucher_no` varchar(64) DEFAULT NULL COMMENT '缴款凭证号',
  `voucher_file_url` varchar(500) DEFAULT NULL COMMENT '缴款凭证文件地址',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_tax_supplement_declaration` (`declaration_id`),
  KEY `idx_tax_supplement_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='需补缴税费（#13）';

CREATE TABLE IF NOT EXISTS `icbc_settlement_reminder` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `payee_id` bigint DEFAULT NULL COMMENT '出售者档案编号',
  `seller_name` varchar(100) DEFAULT NULL COMMENT '出售者姓名',
  `id_card_no` varchar(32) DEFAULT NULL COMMENT '身份证号码',
  `tax_year` int DEFAULT NULL COMMENT '纳税年度',
  `deadline` date DEFAULT NULL COMMENT '汇算清缴截止日（次年 3 月 31 日）',
  `invoice_count` int DEFAULT NULL COMMENT '当年开票张数',
  `invoiced_amount` decimal(14,2) DEFAULT NULL COMMENT '当年开票金额（元）',
  `paid_tax_amount` decimal(14,2) DEFAULT NULL COMMENT '当年已预缴税费合计（元）',
  `iit_amount` decimal(14,2) DEFAULT NULL COMMENT '当年已预缴个人所得税（元）',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态：0-待提醒，1-已提醒',
  `reminded_at` datetime DEFAULT NULL COMMENT '提醒时间',
  `handle_remark` varchar(500) DEFAULT NULL COMMENT '处理说明',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_settlement_reminder_payee` (`payee_id`),
  KEY `idx_settlement_reminder_tax_year` (`tax_year`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='出售者汇算清缴提醒（#13）';
