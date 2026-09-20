-- 连接字符集：文件里有中文注释。客户端默认不是 utf8mb4 时（例如没有 LANG 的
-- `docker exec -i <mysql容器> mysql`，其默认是 latin1），中文会被双重编码存进库。
SET NAMES utf8mb4;

-- ========================================
-- 物流域：承运合同与运费对账（V8 #75，见 ADR 0032 与 CONTEXT.md「承运合同」「运费」）
--
-- 运力吃紧时找第三方：承运合同按线路或品类定运价与计费方式，运费按趟次汇集对账。
-- 运费是**另一笔账**，不改变收购单金额与发票金额（CONTEXT.md「运费」）。
--
--   1. logistics_carrier_contract  承运合同（运价、计费方式、附加费与承担方）
--   2. logistics_freight_order     承运商运费单（一趟一张的应付与对账）
--   3. logistics_transport_cost    运输费用（自有车的路桥 / 燃油等内部成本，按实际承担方）
--
-- 三张都是租户表（带 tenant_id），不进 yudao.tenant.ignore-tables，也不引用任何 icbc 表。
--
-- 幂等：CREATE TABLE IF NOT EXISTS。菜单在 logistics-menu.sql 里幂等维护。
-- 注意 `--` 后必须跟空格才是注释（本项目已两次踩过 1064）。
-- ========================================

-- ---------------------------------------------------------------------
-- 1. 承运合同：运价与计费方式的约定，是运费对账的依据
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `logistics_carrier_contract` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `contract_no` varchar(64) NOT NULL COMMENT '合同编号（租户内唯一，对外可见）',
  `carrier_id` bigint unsigned NOT NULL COMMENT '承运商编号',
  `carrier_name` varchar(100) DEFAULT NULL COMMENT '承运商名称快照（改名或删档不影响历史运单）',
  `effective_from` date NOT NULL COMMENT '生效日期',
  `effective_to` date DEFAULT NULL COMMENT '失效日期（为空表示长期）',
  `route` varchar(255) DEFAULT NULL COMMENT '适用线路（与适用品类至少填一个）',
  `goods_config_id` bigint unsigned DEFAULT NULL COMMENT '适用品类编号（icbc 侧编号，可空）',
  `category_name` varchar(64) DEFAULT NULL COMMENT '适用品类名称快照',
  `billing_mode` tinyint NOT NULL COMMENT '计费方式：1-按车，2-按吨，3-按公里',
  `unit_price` decimal(16,4) NOT NULL COMMENT '运价（按计费方式：每趟 / 每吨 / 每公里）',
  `surcharges` text COMMENT '附加费列表 JSON：[{"name":"路桥附加费","amount":100.00,"bearer":2}]，bearer 1-承运商承担 2-本企业承担',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态：0-生效，1-已停用',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `tenant_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '租户编号',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_carrier_contract_tenant_no` (`tenant_id`, `contract_no`),
  KEY `idx_carrier_contract_tenant_carrier` (`tenant_id`, `carrier_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='物流 - 承运合同';

-- ---------------------------------------------------------------------
-- 2. 承运商运费单：一趟一张，运价快照自合同；差异不抹平
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `logistics_freight_order` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `freight_no` varchar(64) NOT NULL COMMENT '运费单号（租户内唯一，对外可见）',
  `task_id` bigint unsigned NOT NULL COMMENT '运输任务编号（一趟一张）',
  `task_no` varchar(64) DEFAULT NULL COMMENT '运输任务单号（冗余）',
  `carrier_id` bigint unsigned NOT NULL COMMENT '承运商编号（承运商运费必有）',
  `carrier_name` varchar(100) DEFAULT NULL COMMENT '承运商名称快照',
  `contract_id` bigint unsigned NOT NULL COMMENT '承运合同编号',
  `contract_no` varchar(64) DEFAULT NULL COMMENT '承运合同号快照',
  `billing_mode` tinyint NOT NULL COMMENT '计费方式（合同快照）：1-按车，2-按吨，3-按公里',
  `bill_quantity` decimal(16,4) NOT NULL COMMENT '计费量（按车 = 趟数、按吨 = 吨数、按公里 = 公里数）',
  `bill_unit_price` decimal(16,4) DEFAULT NULL COMMENT '运价（合同快照）',
  `base_amount` decimal(16,2) DEFAULT NULL COMMENT '基础运费（计费量 × 运价）',
  `surcharge_amount` decimal(16,2) DEFAULT NULL COMMENT '附加费净额（本企业承担 − 承运商承担）',
  `expected_amount` decimal(16,2) DEFAULT NULL COMMENT '应有应付（基础运费 + 附加费净额）',
  `actual_amount` decimal(16,2) DEFAULT NULL COMMENT '实际应付（对账确认；确认前可空）',
  `variance_amount` decimal(16,2) DEFAULT NULL COMMENT '差异（实际 − 应有）',
  `variance_reason` varchar(500) DEFAULT NULL COMMENT '差异原因（差异非零必填，不抹平）',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态：0-待确认应付，1-已确认应付，2-已登记付款凭证',
  `confirm_by` bigint unsigned DEFAULT NULL COMMENT '确认应付人（系统用户编号）',
  `confirm_by_name` varchar(64) DEFAULT NULL COMMENT '确认应付人姓名',
  `confirm_time` datetime DEFAULT NULL COMMENT '确认应付时间',
  `confirm_remark` varchar(500) DEFAULT NULL COMMENT '确认应付备注',
  `payment_voucher_no` varchar(64) DEFAULT NULL COMMENT '外部付款凭证号',
  `payment_voucher_url` varchar(500) DEFAULT NULL COMMENT '外部付款凭证附件 URL',
  `payment_amount` decimal(16,2) DEFAULT NULL COMMENT '实付金额',
  `paid_at` datetime DEFAULT NULL COMMENT '付款时间',
  `payment_remark` varchar(500) DEFAULT NULL COMMENT '付款备注',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `tenant_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '租户编号',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_freight_order_tenant_no` (`tenant_id`, `freight_no`),
  KEY `idx_freight_order_tenant_task` (`tenant_id`, `task_id`),
  KEY `idx_freight_order_tenant_carrier` (`tenant_id`, `carrier_id`),
  KEY `idx_freight_order_tenant_status` (`tenant_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='物流 - 承运商运费单';

-- ---------------------------------------------------------------------
-- 3. 运输费用（内部成本）：自有车的路桥 / 燃油等，按实际承担方记；不进运费单与收购单
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `logistics_transport_cost` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `task_id` bigint unsigned NOT NULL COMMENT '运输任务编号',
  `task_no` varchar(64) DEFAULT NULL COMMENT '运输任务单号（冗余）',
  `cost_type` tinyint NOT NULL COMMENT '费用类型：1-路桥费，2-燃油费，3-其他',
  `name` varchar(100) DEFAULT NULL COMMENT '费用名称',
  `amount` decimal(16,2) NOT NULL COMMENT '金额（元）',
  `bearer` tinyint NOT NULL COMMENT '承担方：1-承运商承担，2-本企业承担',
  `occur_date` date DEFAULT NULL COMMENT '发生日期',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `tenant_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '租户编号',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_transport_cost_tenant_task` (`tenant_id`, `task_id`),
  KEY `idx_transport_cost_tenant_type` (`tenant_id`, `cost_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='物流 - 运输费用（内部成本）';
