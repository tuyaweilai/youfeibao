-- ========================================
-- 额度风控与跨租户合并（#12）
-- 1. icbc_invoice_order 补适用征收率（额度台账按它把销售额按 1% / 3% 分列）
-- 2. 新建 icbc_seller_quota_guidance（出售者额度超限后的经营主体登记引导）
-- 幂等：字段用 information_schema 判断后再 ALTER；建表用 IF NOT EXISTS
-- 菜单与按钮权限在 icbc-menu.sql 里幂等维护
-- ========================================

-- 1. 适用征收率
SET @col_exists := (
  SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'icbc_invoice_order' AND COLUMN_NAME = 'tax_rate'
);
SET @ddl := IF(@col_exists = 0,
  'ALTER TABLE `icbc_invoice_order` ADD COLUMN `tax_rate` decimal(5,4) DEFAULT NULL COMMENT ''适用征收率（0.01=3%减按1%，0.03=放弃减按）'' AFTER `total_amount`',
  'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. 额度超限的引导记录
CREATE TABLE IF NOT EXISTS `icbc_seller_quota_guidance` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `payee_id` bigint NOT NULL COMMENT '出售者档案编号',
  `seller_name` varchar(100) DEFAULT NULL COMMENT '出售者姓名',
  `id_card_no` varchar(32) DEFAULT NULL COMMENT '身份证号码',
  `trigger_scene` varchar(32) DEFAULT NULL COMMENT '触发场景：INVOICE_APPLICATION-开票申请被拒，ACQUISITION-收购登记时已超',
  `trigger_biz_no` varchar(64) DEFAULT NULL COMMENT '触发业务单号（收购单号 / 合作方订单号）',
  `used_amount` decimal(14,2) DEFAULT NULL COMMENT '触发时连续 12 个月累计已用额度（元）',
  `cap_amount` decimal(14,2) DEFAULT NULL COMMENT '触发时窗口上限（元），500 万',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '引导状态：0-待引导，1-已引导，2-已办结',
  `triggered_at` datetime DEFAULT NULL COMMENT '首次触发时间',
  `last_triggered_at` datetime DEFAULT NULL COMMENT '最近一次触发时间',
  `handled_at` datetime DEFAULT NULL COMMENT '处理时间',
  `handle_remark` varchar(500) DEFAULT NULL COMMENT '处理说明',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_quota_guidance_payee_id` (`payee_id`),
  KEY `idx_quota_guidance_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='出售者额度超限的经营主体登记引导表';
