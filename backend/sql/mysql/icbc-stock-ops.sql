-- 连接字符集：文件里有中文注释与菜单种子。客户端默认不是 utf8mb4 时（例如没有 LANG 的
-- `docker exec -i <mysql容器> mysql`，其默认是 latin1），中文会被双重编码存进库，
-- 界面上就是乱码。这里显式声明，导入时不必再依赖客户端参数。
SET NAMES utf8mb4;

-- ========================================
-- 非销售出库 / 跨仓调拨 / 盘点调整 / 期初导入（#54 T16，ADR 0025 / 0027）
--
-- 四类仓管自主发起的库存作业，写入路径只有一条：过账时经 `StockApi` 写 `erp_stock_record`
-- 流水并增量 `erp_stock` 余额。**icbc 不直接碰 `erp_stock*`**（ADR 0027）。
--
--   * 非销售出库（`icbc_stock_out`）：报损 / 退货出库 / 内部领用三种，**不挂客户**——一期不做
--     销售出库（ADR 0004）。过账写 SCRAP_OUT(100) / RETURN_OUT(102) / INTERNAL_USE_OUT(104)，
--     作废已过账的按相反方向冲销（*_CANCEL）。
--   * 跨仓调拨（`icbc_stock_move`）：源减目标加，过账经 `StockApi#move` 一次写两条流水
--     MOVE_OUT(32) / MOVE_IN(30)；作废按相反方向调回（MOVE_OUT_CANCEL / MOVE_IN_CANCEL）。
--   * 盘点调整（`icbc_stock_check`）：登记实盘数，过账经 `StockApi#adjustTo` 由 ERP 在同一事务里
--     算差额并写 CHECK_MORE_IN(40) / CHECK_LESS_OUT(42)，差额与账面数落明细；账实相符不写流水。
--     作废按**记录的差额**冲销，不重算。
--   * 期初导入（`icbc_stock_opening`）：一个「品类 + 仓库 + 库位 + 批次」一行，导入即过账
--     （OPENING_IN(110)），同一维度只允许一条生效期初；作废冲销 OPENING_IN_CANCEL(111)。
--
-- 余额与流水始终一致：每一笔余额变更都对应一条流水，冲销是反向流水而不是改余额。
-- 幂等：CREATE TABLE IF NOT EXISTS。七张表都是租户表（带 tenant_id），不进 yudao.tenant.ignore-tables。
-- 菜单与按钮权限在 icbc-menu.sql 里幂等维护。
-- ========================================

CREATE TABLE IF NOT EXISTS `icbc_stock_out` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `stock_out_no` varchar(64) NOT NULL COMMENT '出库单号（平台生成，唯一）',
  `out_type` tinyint NOT NULL COMMENT '出库类型：10-报损，20-退货出库，30-内部领用',
  `total_quantity` decimal(14,4) DEFAULT NULL COMMENT '出库合计（各明细之和）',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态：0-待过账，1-已过账，2-已作废',
  `posted_time` datetime DEFAULT NULL COMMENT '过账时间',
  `cancel_reason` varchar(500) DEFAULT NULL COMMENT '作废原因',
  `cancelled_time` datetime DEFAULT NULL COMMENT '作废时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注（出库事由）',
  `tenant_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '租户编号',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_stock_out_no` (`stock_out_no`),
  KEY `idx_stock_out_status` (`tenant_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='非销售出库单（报损 / 退货出库 / 内部领用，不挂客户）';

CREATE TABLE IF NOT EXISTS `icbc_stock_out_item` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `stock_out_id` bigint unsigned NOT NULL COMMENT '出库单编号',
  `goods_config_id` bigint unsigned NOT NULL COMMENT '品类编号（icbc_goods_config.id）',
  `warehouse_id` bigint unsigned NOT NULL COMMENT '仓库编号（erp_warehouse.id）',
  `location_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '库位编号（0 = 未指定）',
  `batch_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '批次编号（0 = 未指定）',
  `quantity` decimal(14,4) NOT NULL COMMENT '出库数量（正数）',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `tenant_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '租户编号',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_stock_out_item_out` (`tenant_id`, `stock_out_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='非销售出库单明细';

CREATE TABLE IF NOT EXISTS `icbc_stock_move` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `move_no` varchar(64) NOT NULL COMMENT '调拨单号（平台生成，唯一）',
  `total_quantity` decimal(14,4) DEFAULT NULL COMMENT '调拨合计（各明细之和）',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态：0-待过账，1-已过账，2-已作废',
  `posted_time` datetime DEFAULT NULL COMMENT '过账时间',
  `cancel_reason` varchar(500) DEFAULT NULL COMMENT '作废原因',
  `cancelled_time` datetime DEFAULT NULL COMMENT '作废时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注（调拨事由）',
  `tenant_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '租户编号',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_stock_move_no` (`move_no`),
  KEY `idx_stock_move_status` (`tenant_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='跨仓调拨单（源减目标加）';

CREATE TABLE IF NOT EXISTS `icbc_stock_move_item` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `move_id` bigint unsigned NOT NULL COMMENT '调拨单编号',
  `goods_config_id` bigint unsigned NOT NULL COMMENT '品类编号（icbc_goods_config.id）',
  `from_warehouse_id` bigint unsigned NOT NULL COMMENT '源仓库编号（erp_warehouse.id）',
  `from_location_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '源库位编号（0 = 未指定）',
  `from_batch_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '源批次编号（0 = 未指定）',
  `to_warehouse_id` bigint unsigned NOT NULL COMMENT '目标仓库编号（erp_warehouse.id）',
  `to_location_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '目标库位编号（0 = 未指定）',
  `to_batch_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '目标批次编号（0 = 未指定）',
  `quantity` decimal(14,4) NOT NULL COMMENT '调拨数量（正数）',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `tenant_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '租户编号',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_stock_move_item_move` (`tenant_id`, `move_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='跨仓调拨单明细';

CREATE TABLE IF NOT EXISTS `icbc_stock_check` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `check_no` varchar(64) NOT NULL COMMENT '盘点单号（平台生成，唯一）',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态：0-待过账，1-已过账，2-已作废',
  `posted_time` datetime DEFAULT NULL COMMENT '过账时间',
  `cancel_reason` varchar(500) DEFAULT NULL COMMENT '作废原因',
  `cancelled_time` datetime DEFAULT NULL COMMENT '作废时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注（盘点范围 / 事由）',
  `tenant_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '租户编号',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_stock_check_no` (`check_no`),
  KEY `idx_stock_check_status` (`tenant_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='盘点单（把余额对齐到实盘数）';

CREATE TABLE IF NOT EXISTS `icbc_stock_check_item` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `check_id` bigint unsigned NOT NULL COMMENT '盘点单编号',
  `goods_config_id` bigint unsigned NOT NULL COMMENT '品类编号（icbc_goods_config.id）',
  `warehouse_id` bigint unsigned NOT NULL COMMENT '仓库编号（erp_warehouse.id）',
  `location_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '库位编号（0 = 未指定）',
  `batch_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '批次编号（0 = 未指定）',
  `actual_quantity` decimal(14,4) NOT NULL COMMENT '盘点实盘数（录入值）',
  `book_quantity` decimal(14,4) DEFAULT NULL COMMENT '账面数量（过账时快照）',
  `difference_quantity` decimal(14,4) DEFAULT NULL COMMENT '差额 = 实盘 − 账面（正数盘盈，负数盘亏）',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `tenant_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '租户编号',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_stock_check_item_check` (`tenant_id`, `check_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='盘点单明细';

CREATE TABLE IF NOT EXISTS `icbc_stock_opening` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `opening_no` varchar(64) NOT NULL COMMENT '导入批次号（一次导入一个批次号）',
  `goods_config_id` bigint unsigned NOT NULL COMMENT '品类编号（icbc_goods_config.id）',
  `warehouse_id` bigint unsigned NOT NULL COMMENT '仓库编号（erp_warehouse.id）',
  `location_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '库位编号（0 = 未指定）',
  `batch_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '批次编号（0 = 未指定）',
  `quantity` decimal(14,4) NOT NULL COMMENT '期初数量（正数）',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1-已过账，2-已作废（导入即过账）',
  `posted_time` datetime DEFAULT NULL COMMENT '过账时间',
  `cancel_reason` varchar(500) DEFAULT NULL COMMENT '作废原因',
  `cancelled_time` datetime DEFAULT NULL COMMENT '作废时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `tenant_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '租户编号',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_stock_opening_dimension` (`tenant_id`, `goods_config_id`, `warehouse_id`, `location_id`, `batch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='期初（一个维度一行，导入即过账）';

-- ========================================
-- #54 收口：期初「同一维度只允许一条生效」的并发兜底
-- 生效中 active_key = 1；作废 active_key = NULL（唯一索引里 NULL 互不相等，可留多条作废记录）。
-- 服务层已有前置校验，这里加数据库约束，避免并发导入各写一条。
-- 幂等：先查 information_schema。
-- ========================================
DROP PROCEDURE IF EXISTS `icbc_t54_add_opening_active_key`;
DROP PROCEDURE IF EXISTS `icbc_t54_add_opening_unique`;

DELIMITER $$
CREATE PROCEDURE `icbc_t54_add_opening_active_key`()
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                 WHERE table_schema = DATABASE() AND table_name = 'icbc_stock_opening'
                   AND column_name = 'active_key') THEN
    ALTER TABLE `icbc_stock_opening`
      ADD COLUMN `active_key` tinyint NULL COMMENT '生效标记：1=生效中，NULL=已作废（唯一索引里 NULL 互不相等）' AFTER `status`;
    UPDATE `icbc_stock_opening` SET `active_key` = CASE WHEN `status` = 1 THEN 1 ELSE NULL END;
  END IF;
END$$

CREATE PROCEDURE `icbc_t54_add_opening_unique`()
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.statistics
                 WHERE table_schema = DATABASE() AND table_name = 'icbc_stock_opening'
                   AND index_name = 'uk_stock_opening_dimension_active') THEN
    ALTER TABLE `icbc_stock_opening`
      ADD UNIQUE KEY `uk_stock_opening_dimension_active`
        (`tenant_id`, `goods_config_id`, `warehouse_id`, `location_id`, `batch_id`, `active_key`);
  END IF;
END$$
DELIMITER ;

CALL `icbc_t54_add_opening_active_key`();
CALL `icbc_t54_add_opening_unique`();

DROP PROCEDURE IF EXISTS `icbc_t54_add_opening_active_key`;
DROP PROCEDURE IF EXISTS `icbc_t54_add_opening_unique`;
