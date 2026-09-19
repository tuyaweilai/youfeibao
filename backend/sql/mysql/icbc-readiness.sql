-- 连接字符集：文件里有中文种子数据。客户端默认不是 utf8mb4 时（例如没有 LANG 的
-- `docker exec -i <mysql容器> mysql`，其默认是 latin1），中文会被双重编码存进库，
-- 界面上就是乱码。这里显式声明，导入时不必再依赖客户端参数。
SET NAMES utf8mb4;

-- ========================================
-- 租户开票就绪：三层资质、编码配置、企业授权
-- 幂等：CREATE TABLE IF NOT EXISTS
-- ========================================

-- 1. 租户三层资质
CREATE TABLE IF NOT EXISTS `icbc_qualification` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `type` varchar(20) NOT NULL COMMENT '资质层：TAX-税务侧反向开票资格，INDUSTRY-行业侧，PUBLIC_SECURITY-公安侧备案',
  `name` varchar(100) NOT NULL COMMENT '资质名称',
  `issuing_authority` varchar(200) DEFAULT NULL COMMENT '发证机关',
  `cert_no` varchar(100) DEFAULT NULL COMMENT '证书编号',
  `valid_from` date DEFAULT NULL COMMENT '有效期起',
  `valid_to` date DEFAULT NULL COMMENT '有效期止',
  `file_url` varchar(500) DEFAULT NULL COMMENT '证照扫描件',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态：0-待核实，1-有效，2-失效，3-吊销',
  `audit_remark` varchar(500) DEFAULT NULL COMMENT '核实意见',
  `tenant_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '租户编号',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='租户三层资质';

-- 2. 品类与税收分类编码配置
CREATE TABLE IF NOT EXISTS `icbc_goods_config` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(100) NOT NULL COMMENT '品类名称',
  `unit` varchar(20) DEFAULT NULL COMMENT '计量单位',
  `tax_rate` decimal(5,4) DEFAULT NULL COMMENT '税率',
  `tax_method` varchar(20) DEFAULT NULL COMMENT '计税方法：SIMPLE-简易计税，GENERAL-一般计税',
  `merged_code` varchar(19) DEFAULT NULL COMMENT '商品和服务税收分类合并编码',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态：0-启用，1-停用',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `tenant_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '租户编号',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='品类与税收分类编码配置';

-- 3. 工行企业授权
CREATE TABLE IF NOT EXISTS `icbc_enterprise_auth` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `out_vendor_id` varchar(40) DEFAULT NULL COMMENT '付方编号（回收企业 / 子商户）',
  `site_type` varchar(10) DEFAULT NULL COMMENT '工行入参 siteType',
  `user_type` varchar(10) DEFAULT NULL COMMENT '工行入参 userType',
  `auth_status` tinyint NOT NULL DEFAULT '0' COMMENT '授权状态：0-未授权，1-已授权，2-已失效',
  `auth_time` datetime DEFAULT NULL COMMENT '授权时间',
  `expire_time` datetime DEFAULT NULL COMMENT '授权有效期止',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `tenant_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '租户编号',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工行企业授权记录';

-- 兼容已存在的库：补 `tax_method` 列（幂等）
SET @col_exists := (
  SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'icbc_goods_config' AND COLUMN_NAME = 'tax_method'
);
SET @ddl := IF(@col_exists = 0,
  'ALTER TABLE `icbc_goods_config` ADD COLUMN `tax_method` varchar(20) DEFAULT NULL COMMENT ''计税方法：SIMPLE-简易计税，GENERAL-一般计税'' AFTER `tax_rate`',
  'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 4. 平台级报废产品税收分类编码表（全局，无租户维度）
CREATE TABLE IF NOT EXISTS `icbc_scrap_code` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(100) NOT NULL COMMENT '报废产品名称',
  `merged_code` varchar(19) NOT NULL COMMENT '商品和服务税收分类合并编码',
  `unit` varchar(20) DEFAULT NULL COMMENT '计量单位',
  `tax_rate` decimal(5,4) DEFAULT NULL COMMENT '税率',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态：0-启用，1-停用',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_merged_code` (`merged_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='平台级报废产品税收分类编码表';

-- 5. 资质到期预警（租户级）
CREATE TABLE IF NOT EXISTS `icbc_expiry_warning` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `qualification_id` bigint unsigned NOT NULL COMMENT '资质编号',
  `type` varchar(20) DEFAULT NULL COMMENT '资质层',
  `name` varchar(100) DEFAULT NULL COMMENT '资质名称',
  `valid_to` date DEFAULT NULL COMMENT '有效期止',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态：0-待处理，1-已处理',
  `warned_at` datetime DEFAULT NULL COMMENT '预警时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `tenant_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '租户编号',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_qualification_id` (`qualification_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='资质到期预警';
