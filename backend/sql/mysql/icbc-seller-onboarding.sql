-- ========================================
-- 出售者建档（#6）：框架收购协议、首次授权
-- 幂等：CREATE TABLE IF NOT EXISTS
-- 实人认证 / 收方入驻相关字段已加在 icbc_payee_info.sql 的建表语句里。
-- ========================================

-- 1. 框架收购协议：每个出售者一份、长期有效；重新签署时旧协议作废留痕
CREATE TABLE IF NOT EXISTS `icbc_framework_agreement` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `payee_id` bigint unsigned NOT NULL COMMENT '出售者（收方）编号',
  `agreement_no` varchar(64) NOT NULL COMMENT '协议编号',
  `product_name` varchar(200) NOT NULL COMMENT '货物名称',
  `quantity` varchar(100) NOT NULL COMMENT '数量',
  `specification` varchar(100) NOT NULL COMMENT '规格',
  `recycle_period` varchar(100) NOT NULL COMMENT '回收期次',
  `settlement_method` varchar(200) NOT NULL COMMENT '结算方式',
  `sign_method` varchar(20) DEFAULT NULL COMMENT '签署方式：ELECTRONIC-电子签章，PAPER-纸质签署',
  `signed_at` datetime DEFAULT NULL COMMENT '签署时间',
  `file_url` varchar(500) DEFAULT NULL COMMENT '协议文件地址',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态：0-待签署，1-生效，2-作废',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `tenant_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '租户编号',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_agreement_no` (`agreement_no`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_payee_id` (`payee_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='框架收购协议';

-- 2. 出售者首次授权：反向开票 + 代办税费，两份授权分开留痕
CREATE TABLE IF NOT EXISTS `icbc_seller_authorization` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `payee_id` bigint unsigned NOT NULL COMMENT '出售者（收方）编号',
  `reverse_invoice_authorized` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否授权反向开票',
  `tax_agency_authorized` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否授权代办税费',
  `authorized_at` datetime DEFAULT NULL COMMENT '授权时间',
  `channel` varchar(20) DEFAULT NULL COMMENT '授权渠道：ONSITE-收购现场，ICBC_H5-工行页面',
  `operator` varchar(64) DEFAULT NULL COMMENT '现场办理人（收货员）',
  `evidence_url` varchar(500) DEFAULT NULL COMMENT '留痕附件地址',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `tenant_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '租户编号',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_payee_id` (`payee_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='出售者首次授权';
