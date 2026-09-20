-- 连接字符集：文件里有中文种子数据。客户端默认不是 utf8mb4 时（例如没有 LANG 的
-- `docker exec -i <mysql容器> mysql`，其默认是 latin1），中文会被双重编码存进库，
-- 界面上就是乱码。这里显式声明，导入时不必再依赖客户端参数。
SET NAMES utf8mb4;

-- ========================================
-- 采购订单履约五口径与执行进度（#47 T09，见 docs/adr/0027、0028 与 CONTEXT.md「采购订单」）
--
-- 一张订单的履约必须分口径说清楚：计划 / 验收 / 入库 / 结算 / 未履行（用户故事 21）。
-- 本票把五口径与三类异常落地：
--   1. icbc_purchase_setting    采购履约配置（租户级单行）：完成比例采用哪个口径；超量 / 过期 /
--                               跨场站交货按「拦截」还是「提交授权审核」处理。
--   2. icbc_purchase_exception  履约异常授权单：提交 → 审核 → 已通过的授权成为交货门禁的放行依据。
--
-- 三件要点：
--   * 五口径的取数在 PurchaseOrderService#getProgress 一处：计划 ← 明细计划量；验收 ← 成交记录数量
--     （退货记负，自动扣回）；结算 ← 成交记录中来源收购单已归入结算单的那部分；入库 ← 待 #52 接入
--     （不出数字，标「待接入」）；未履行 ← 计划 − 本单履约口径量。
--   * 「完成比例采用哪个口径」落在租户级配置上（采购合同 #45 没有承载该字段）。默认按验收口径，
--     只允许选能取到数的口径（入库 #52 未落地，不可选）。
--   * 授权单只放宽它自己那一件事（追加量 / 有效期 / 场站），不改订单状态、不改已发生的业务。
--     企业配置未设置时默认「拦截」——不配置不等于放行。
--
-- 两张都是租户表（带 tenant_id），不进 yudao.tenant.ignore-tables。
-- 幂等：CREATE TABLE IF NOT EXISTS / CREATE INDEX IF NOT EXISTS。菜单在 icbc-menu.sql 里幂等维护。
-- ========================================

CREATE TABLE IF NOT EXISTS `icbc_purchase_setting` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `performance_basis` varchar(32) NOT NULL DEFAULT 'ACCEPTED' COMMENT '完成比例采用的履约口径：ACCEPTED-验收口径，SETTLED-结算口径',
  `over_quantity_rule` varchar(16) NOT NULL DEFAULT 'BLOCK' COMMENT '超量交货处理方式：BLOCK-拦截，APPROVAL-提交授权审核',
  `expired_rule` varchar(16) NOT NULL DEFAULT 'BLOCK' COMMENT '过期交货处理方式：BLOCK-拦截，APPROVAL-提交授权审核',
  `cross_station_rule` varchar(16) NOT NULL DEFAULT 'BLOCK' COMMENT '跨场站交货处理方式：BLOCK-拦截，APPROVAL-提交授权审核',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `tenant_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '租户编号',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_purchase_setting_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='采购履约配置（租户级，完成比例口径与三类异常的处理方式）';

CREATE TABLE IF NOT EXISTS `icbc_purchase_exception` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `exception_no` varchar(64) NOT NULL COMMENT '授权单号（平台生成，租户内唯一）',
  `order_id` bigint unsigned NOT NULL COMMENT '采购订单编号',
  `order_no` varchar(64) DEFAULT NULL COMMENT '采购订单号快照',
  `item_id` bigint unsigned DEFAULT NULL COMMENT '采购订单明细编号（超量交货必填）',
  `category_name` varchar(100) DEFAULT NULL COMMENT '品类名称快照',
  `exception_type` varchar(32) NOT NULL COMMENT '异常类型：OVER_QUANTITY-超量，EXPIRED-过期，CROSS_STATION-跨场站',
  `station_id` bigint unsigned DEFAULT NULL COMMENT '本次交货场站编号（跨场站交货必填）',
  `station_name` varchar(100) DEFAULT NULL COMMENT '本次交货场站名称快照',
  `requested_quantity` decimal(16,4) NOT NULL COMMENT '本次申请的交货量',
  `approved_quantity` decimal(16,4) DEFAULT NULL COMMENT '审核通过的追加量（超量交货的授权上限）',
  `valid_until` date DEFAULT NULL COMMENT '授权有效期止（含当日；空表示不设有效期）',
  `reason` varchar(500) NOT NULL COMMENT '提交原因',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态：0-待审核，1-已通过，2-已拒绝',
  `requested_by` bigint DEFAULT NULL COMMENT '提交人',
  `requested_time` datetime DEFAULT NULL COMMENT '提交时间',
  `reviewed_by` bigint DEFAULT NULL COMMENT '审核人',
  `reviewed_time` datetime DEFAULT NULL COMMENT '审核时间',
  `review_remark` varchar(500) DEFAULT NULL COMMENT '审核意见',
  `tenant_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '租户编号',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_purchase_exception_no` (`tenant_id`, `exception_no`),
  KEY `idx_purchase_exception_order` (`tenant_id`, `order_id`, `exception_type`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='采购订单履约异常授权单（超量 / 过期 / 跨场站）';
