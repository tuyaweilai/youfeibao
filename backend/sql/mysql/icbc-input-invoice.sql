-- 连接字符集：文件里有中文种子数据（注释与单据类型名）。客户端默认不是 utf8mb4 时
-- （例如没有 LANG 的 `docker exec -i <mysql容器> mysql`，其默认是 latin1），中文会被
-- 双重编码存进库。这里显式声明，导入时不必再依赖客户端参数。
SET NAMES utf8mb4;

-- ========================================
-- 进项收票登记与勾稽（#49 T11，ADR 0029）
--
-- 单位供货方（企业 / 个体工商户 / 个人独资企业 / 合伙企业 / 农民专业合作社）向回收企业
-- 开具增值税发票后，由财务登记票面事实（票种 / 号码 / 金额 / 税额 / 开票日期 / 销方），
-- 再勾稽到采购单据，让**票、货、款三者对得上**。自然人出售者不在本链路里（他们走反向开票）。
--
-- 两条设计约定：
--   * **登记唯一**：按「销方 + 发票号码」唯一。seller_key = 有税号用税号，否则用销方名称，
--     与 invoice_no 一起构成唯一键 uk_input_invoice_seller_no；
--   * **勾稽用通用关联表**：icbc_input_invoice_link(biz_type, biz_id, biz_no, biz_amount, linked_amount)，
--     biz_type 是稳定编码（ACQUISITION / PURCHASE_ORDER / STOCK_IN）。金额上限按调用方给出的
--     biz_amount 校验，本表不反向依赖采购订单 / 入库单的实现（第二轮并行约定）。
--
-- 幂等：CREATE TABLE IF NOT EXISTS。两张表都是租户表（带 tenant_id），不进
-- yudao.tenant.ignore-tables。菜单与按钮权限在 icbc-menu.sql 里幂等维护。
-- ========================================

CREATE TABLE IF NOT EXISTS `icbc_input_invoice` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `invoice_no` varchar(64) NOT NULL COMMENT '发票号码（销方 + 号码唯一）',
  `invoice_code` varchar(32) DEFAULT NULL COMMENT '发票代码（数电票可空）',
  `invoice_type` tinyint NOT NULL COMMENT '票种：1-专用发票，2-普通发票',
  `invoice_date` date NOT NULL COMMENT '开票日期',
  `seller_name` varchar(200) NOT NULL COMMENT '销方名称',
  `seller_tax_no` varchar(64) DEFAULT NULL COMMENT '销方纳税人识别号（为空时按名称参与唯一性判定）',
  `seller_key` varchar(200) NOT NULL COMMENT '销方唯一标识：有税号用税号，否则用名称（供唯一索引使用）',
  `amount` decimal(18,2) NOT NULL COMMENT '不含税金额',
  `tax_amount` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '税额',
  `total_amount` decimal(18,2) NOT NULL COMMENT '价税合计 = 不含税金额 + 税额（勾稽上限）',
  `linked_amount` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '已勾稽金额合计',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态：0-已登记，1-部分勾稽，2-已勾稽',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `tenant_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '租户编号',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_input_invoice_seller_no` (`tenant_id`, `seller_key`, `invoice_no`),
  KEY `idx_input_invoice_seller` (`tenant_id`, `seller_name`),
  KEY `idx_input_invoice_date` (`tenant_id`, `invoice_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='进项发票（单位供货方开给回收企业）';

CREATE TABLE IF NOT EXISTS `icbc_input_invoice_link` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `invoice_id` bigint unsigned NOT NULL COMMENT '进项发票编号',
  `biz_type` varchar(32) NOT NULL COMMENT '单据类型：ACQUISITION-收购单，PURCHASE_ORDER-采购订单，STOCK_IN-入库单',
  `biz_id` bigint unsigned NOT NULL COMMENT '单据编号',
  `biz_no` varchar(64) DEFAULT NULL COMMENT '单据号快照',
  `biz_amount` decimal(18,2) NOT NULL COMMENT '单据金额（调用方传入，作为勾稽金额上限）',
  `linked_amount` decimal(18,2) NOT NULL COMMENT '本次勾稽金额',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `tenant_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '租户编号',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_input_invoice_link` (`tenant_id`, `invoice_id`, `biz_type`, `biz_id`),
  KEY `idx_input_invoice_link_biz` (`tenant_id`, `biz_type`, `biz_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='进项发票勾稽关联（通用关联表）';
