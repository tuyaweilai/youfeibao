-- 连接字符集：文件里有中文种子数据。客户端默认不是 utf8mb4 时（例如没有 LANG 的
-- `docker exec -i <mysql容器> mysql`，其默认是 latin1），中文会被双重编码存进库，
-- 界面上就是乱码。这里显式声明，导入时不必再依赖客户端参数。
SET NAMES utf8mb4;

-- ========================================
-- 采购订单（#46 T08，见 docs/adr/0027 与 CONTEXT.md「采购订单」）
--
-- 采购订单是回收企业内部的采购**执行依据**：向谁买哪些品类、多少量、什么价、在哪段时间、
-- 哪个场站，一单多条品类明细、一条明细可分多次收货。它不是交易对方下的单（那是「到站预约」），
-- 零散收购也可以不挂订单（报表标「直接收购」，见 #51）。
--
-- 采购履约链建在 icbc 模块（#38 规格第 1 条，修订 ADR 0025/0027 的 erp 侧）：
--   1. icbc_purchase_order        订单主体（对手方双外键、可选合同与场站、状态、计划量额快照）
--   2. icbc_purchase_order_item   品类明细（一条明细可分多次收货；固定单价 / 按交货日价格表）
--   3. icbc_purchase_order_price  交货日价格表（按交货日不晚于当日的最新一条生效）
--   4. icbc_purchase_order_deal   成交记录（每次成交留价格快照与调整原因，只追加）
--
-- 对手方沿用 ADR 0029 的「主体类型六态 + 双可空 id」，由 chk_purchase_order_counterparty 与
-- Service 双重保证恰好一个非空；合同必须已审核生效（门禁在 PurchaseContractService）。
-- 四张都是租户表（带 tenant_id），不进 yudao.tenant.ignore-tables。
-- 幂等：CREATE TABLE IF NOT EXISTS。菜单与按钮权限在 icbc-menu.sql 里幂等维护。
-- ========================================

CREATE TABLE IF NOT EXISTS `icbc_purchase_order` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_no` varchar(64) NOT NULL COMMENT '采购订单号（平台生成，租户内唯一）',
  `contract_id` bigint unsigned DEFAULT NULL COMMENT '关联采购合同编号（可空）',
  `contract_no` varchar(64) DEFAULT NULL COMMENT '采购合同号快照',
  `counterparty_type` tinyint NOT NULL COMMENT '交易对方主体类型：1-自然人出售者，2-个体工商户，3-个人独资企业，4-合伙企业，5-企业法人，6-农民专业合作社',
  `payee_id` bigint unsigned DEFAULT NULL COMMENT '自然人出售者档案编号（主体类型为自然人时非空）',
  `supplier_id` bigint unsigned DEFAULT NULL COMMENT '单位供货方编号（主体类型为非自然人时非空，指向 erp_supplier）',
  `counterparty_name` varchar(200) DEFAULT NULL COMMENT '交易对方名称快照',
  `station_id` bigint unsigned DEFAULT NULL COMMENT '执行场站编号（可空）',
  `station_name` varchar(100) DEFAULT NULL COMMENT '执行场站名称快照',
  `start_date` date NOT NULL COMMENT '执行开始日期',
  `end_date` date NOT NULL COMMENT '执行结束日期（早于今天即视为过期）',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态：0-草稿，1-执行中，2-暂停，3-完成，4-关闭',
  `total_quantity` decimal(16,4) DEFAULT NULL COMMENT '计划总量（明细汇总快照）',
  `total_amount` decimal(16,2) DEFAULT NULL COMMENT '计划总金额（明细汇总快照）',
  `suspend_reason` varchar(500) DEFAULT NULL COMMENT '暂停原因',
  `suspended_time` datetime DEFAULT NULL COMMENT '暂停时间',
  `completed_time` datetime DEFAULT NULL COMMENT '完成时间',
  `closed_by` bigint DEFAULT NULL COMMENT '关闭人',
  `closed_time` datetime DEFAULT NULL COMMENT '关闭时间',
  `close_reason` varchar(500) DEFAULT NULL COMMENT '关闭原因',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `tenant_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '租户编号',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_purchase_order_no` (`tenant_id`, `order_no`),
  KEY `idx_purchase_order_counterparty` (`tenant_id`, `counterparty_type`, `counterparty_name`),
  KEY `idx_purchase_order_status` (`tenant_id`, `status`, `end_date`),
  KEY `idx_purchase_order_contract` (`tenant_id`, `contract_id`),
  CONSTRAINT `chk_purchase_order_counterparty`
    CHECK ((`payee_id` IS NULL) <> (`supplier_id` IS NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='采购订单（采购执行依据）';

-- 明细整组重建（逻辑删旧行 + 插新行），因此不加 (tenant_id, order_id, goods_config_id) 唯一键：
-- 逻辑删除的行仍在表里，唯一键会让「先移除再重新加入同一品类」直接报 DuplicateKey。
-- 历史约定由成交记录与订单快照保证。
CREATE TABLE IF NOT EXISTS `icbc_purchase_order_item` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键（分次收货与成交记录引用它）',
  `order_id` bigint unsigned NOT NULL COMMENT '采购订单编号',
  `goods_config_id` bigint unsigned NOT NULL COMMENT '品类编号（icbc_goods_config）',
  `category_name` varchar(100) DEFAULT NULL COMMENT '品类名称快照',
  `unit` varchar(32) DEFAULT NULL COMMENT '计量单位快照',
  `quantity` decimal(16,4) NOT NULL COMMENT '计划量',
  `price_mode` tinyint NOT NULL DEFAULT '1' COMMENT '定价方式：1-固定单价，2-按交货日价格表',
  `unit_price` decimal(16,4) DEFAULT NULL COMMENT '参考单价（固定单价的成交价；价格表方式的兜底价）',
  `amount` decimal(16,2) DEFAULT NULL COMMENT '计划金额 = 计划量 × 参考单价',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注（等级 / 规格等）',
  `tenant_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '租户编号',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_purchase_order_item_order` (`tenant_id`, `order_id`),
  KEY `idx_purchase_order_item_goods` (`tenant_id`, `goods_config_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='采购订单品类明细';

CREATE TABLE IF NOT EXISTS `icbc_purchase_order_price` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_id` bigint unsigned NOT NULL COMMENT '采购订单编号',
  `item_id` bigint unsigned NOT NULL COMMENT '采购订单明细编号',
  `delivery_date` date NOT NULL COMMENT '生效交货日',
  `unit_price` decimal(16,4) NOT NULL COMMENT '该日生效单价',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `tenant_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '租户编号',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_purchase_order_price_item` (`tenant_id`, `item_id`, `delivery_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='采购订单交货日价格表';

CREATE TABLE IF NOT EXISTS `icbc_purchase_order_deal` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_id` bigint unsigned NOT NULL COMMENT '采购订单编号',
  `item_id` bigint unsigned NOT NULL COMMENT '采购订单明细编号（一条明细可分多次成交）',
  `deal_no` varchar(64) NOT NULL COMMENT '成交单号（平台生成）',
  `deal_time` datetime DEFAULT NULL COMMENT '成交时间',
  `delivery_date` date DEFAULT NULL COMMENT '交货日（按交货日价格表取价用）',
  `quantity` decimal(16,4) NOT NULL COMMENT '成交数量',
  `unit_price` decimal(16,4) NOT NULL COMMENT '成交单价（价格快照）',
  `reference_unit_price` decimal(16,4) DEFAULT NULL COMMENT '参考单价',
  `price_adjusted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '成交价是否做过调整',
  `adjust_reason` varchar(500) DEFAULT NULL COMMENT '调整原因（调整时必须填）',
  `source_type` varchar(32) DEFAULT NULL COMMENT '关联业务来源类型（可空，如 ACQUISITION）',
  `source_id` bigint unsigned DEFAULT NULL COMMENT '关联业务来源编号',
  `source_no` varchar(64) DEFAULT NULL COMMENT '关联业务来源单号',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `tenant_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '租户编号',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_purchase_order_deal_no` (`tenant_id`, `deal_no`),
  KEY `idx_purchase_order_deal_order` (`tenant_id`, `order_id`),
  KEY `idx_purchase_order_deal_item` (`tenant_id`, `item_id`),
  KEY `idx_purchase_order_deal_source` (`tenant_id`, `source_type`, `source_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='采购订单成交记录（价格快照）';
