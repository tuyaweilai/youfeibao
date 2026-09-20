-- 连接字符集：文件里有中文注释与菜单种子。客户端默认不是 utf8mb4 时（例如没有 LANG 的
-- `docker exec -i <mysql容器> mysql`，其默认是 latin1），中文会被双重编码存进库，
-- 界面上就是乱码。这里显式声明，导入时不必再依赖客户端参数。
SET NAMES utf8mb4;

-- ========================================
-- 待入库 → 入库单 → 库存流水（#52 T14，ADR 0027 / 0028）
--
-- 入库是收购单派生的**单向动作**：验收后的货进待入库，仓管选仓库 / 库位 / 批次确认实际入库量。
-- 库存写入只有一条路径：过账时经 `StockApi` 写 RECEIPT_IN(90) 流水并增量余额；作废已过账的单
-- 按相反方向冲销，业务类型用 RECEIPT_IN_CANCEL(91)。**icbc 不直接碰 `erp_stock*`**。
--
-- 三条约定的落点：
--   * 一张收购单可拆多个库位 / 分多次入库：明细在 icbc_stock_in_item，多次入库就是多张入库单；
--     累计上限由 StockApi 的 maxCount 兜底（业务编号传收购单编号，跨入库单生效）；
--   * 只有过账的入库才增加正式库存：status=0 待过账时不动库存（规格 #38 实现决策第 9 条）；
--   * 可入库实物量只有一个取数点：确认时的值快照进 available_quantity，
--     取数在 StockInService#resolveAvailableQuantity 一处（#53 的接收量落地后只改那里）。
--
-- 幂等：CREATE TABLE IF NOT EXISTS。两张表都是租户表（带 tenant_id），
-- 不进 yudao.tenant.ignore-tables。菜单与按钮权限在 icbc-menu.sql 里幂等维护。
-- ========================================

CREATE TABLE IF NOT EXISTS `icbc_stock_in` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `stock_in_no` varchar(64) NOT NULL COMMENT '入库单号（平台生成，唯一）',
  `acquisition_id` bigint unsigned NOT NULL COMMENT '来源收购单编号',
  `acquisition_no` varchar(64) DEFAULT NULL COMMENT '收购单号快照',
  `payee_id` bigint unsigned DEFAULT NULL COMMENT '出售者（收方）档案编号',
  `seller_name` varchar(100) DEFAULT NULL COMMENT '出售者姓名快照',
  `goods_config_id` bigint unsigned DEFAULT NULL COMMENT '品类配置编号',
  `category_name` varchar(100) DEFAULT NULL COMMENT '品类名称快照',
  `unit` varchar(20) DEFAULT NULL COMMENT '计量单位快照',
  `available_quantity` decimal(14,4) DEFAULT NULL COMMENT '可入库实物量快照（实物口径：净重；取数在 StockInService#resolveAvailableQuantity）',
  `total_quantity` decimal(14,4) DEFAULT NULL COMMENT '本次入库合计（各明细之和）',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态：0-待过账，1-已过账，2-已作废',
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
  UNIQUE KEY `uk_stock_in_no` (`stock_in_no`),
  KEY `idx_stock_in_acquisition` (`tenant_id`, `acquisition_id`),
  KEY `idx_stock_in_status` (`tenant_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='入库单（收购单派生的单向动作）';

CREATE TABLE IF NOT EXISTS `icbc_stock_in_item` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `stock_in_id` bigint unsigned NOT NULL COMMENT '入库单编号',
  `warehouse_id` bigint unsigned NOT NULL COMMENT '仓库编号（erp_warehouse.id）',
  `location_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '库位编号（0 = 未指定）',
  `batch_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '批次编号（0 = 未指定）',
  `quantity` decimal(14,4) NOT NULL COMMENT '入库数量（正数）',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `tenant_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '租户编号',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_stock_in_item_stock_in` (`tenant_id`, `stock_in_id`),
  KEY `idx_stock_in_item_warehouse` (`tenant_id`, `warehouse_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='入库单明细（仓库 / 库位 / 批次 + 数量）';
