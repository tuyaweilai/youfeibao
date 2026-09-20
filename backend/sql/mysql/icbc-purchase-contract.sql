-- 连接字符集：文件里有中文种子数据。客户端默认不是 utf8mb4 时（例如没有 LANG 的
-- `docker exec -i <mysql容器> mysql`，其默认是 latin1），中文会被双重编码存进库，
-- 界面上就是乱码。这里显式声明，导入时不必再依赖客户端参数。
SET NAMES utf8mb4;

-- ========================================
-- 采购合同（#45 T07，见 docs/adr/0027 与 CONTEXT.md「采购合同」）
--
-- 1. icbc_purchase_contract：合同主体（双方、有效期、数量约定、计量与质量标准、价格规则、
--    运输责任、付款条款、附件、状态与审核留痕）
-- 2. icbc_purchase_contract_category：适用品类（多行；落名称 / 单位快照）
-- 3. icbc_purchase_contract_version：每次送审落一版整份合同快照（只追加，不覆盖）
--
-- 采购履约链建在 icbc 模块（#38 规格第 1 条，修订 ADR 0027 的「erp_purchase_contract」）：
-- 合同要承载自然人出售者与单位供货方两种对手方，而 erp 侧看不到 icbc_payee_info；
-- 依赖方向恒为 icbc → erp，故合同落在 icbc 侧，对手方用「主体类型 + 双可空 id」承载。
--
-- 交易对方的「恰好一个非空」由 chk_contract_counterparty 与 Service 双重保证。
-- 幂等：CREATE TABLE IF NOT EXISTS。三张都是租户表（带 tenant_id），不进 ignore-tables。
-- 菜单与按钮权限在 icbc-menu.sql 里幂等维护。
-- ========================================

CREATE TABLE IF NOT EXISTS `icbc_purchase_contract` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `contract_no` varchar(64) NOT NULL COMMENT '合同编号（平台生成，租户内唯一）',
  `name` varchar(200) NOT NULL COMMENT '合同名称',
  `counterparty_type` tinyint NOT NULL COMMENT '交易对方主体类型：1-自然人出售者，2-个体工商户，3-个人独资企业，4-合伙企业，5-企业法人，6-农民专业合作社',
  `payee_id` bigint unsigned DEFAULT NULL COMMENT '自然人出售者档案编号（主体类型为自然人时非空）',
  `supplier_id` bigint unsigned DEFAULT NULL COMMENT '单位供货方编号（主体类型为非自然人时非空，指向 erp_supplier）',
  `counterparty_name` varchar(200) DEFAULT NULL COMMENT '交易对方名称快照',
  `start_date` date NOT NULL COMMENT '有效期起',
  `end_date` date NOT NULL COMMENT '有效期止（早于今天即视为过期）',
  `quantity_agreement` varchar(500) DEFAULT NULL COMMENT '数量约定',
  `measure_standard` varchar(500) DEFAULT NULL COMMENT '计量标准',
  `quality_standard` varchar(500) DEFAULT NULL COMMENT '质量标准',
  `price_rule` varchar(500) DEFAULT NULL COMMENT '价格规则',
  `transport_responsibility` varchar(500) DEFAULT NULL COMMENT '运输责任',
  `payment_terms` varchar(500) DEFAULT NULL COMMENT '付款条款',
  `attachment_urls` varchar(2000) DEFAULT NULL COMMENT '附件地址（多个用英文逗号分隔）',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态：0-草稿，1-待审核，2-生效，3-关闭',
  `version_no` int NOT NULL DEFAULT '0' COMMENT '已送审的最新版本号（0 表示尚未送审）',
  `submitted_by` bigint DEFAULT NULL COMMENT '最近一次送审人',
  `submitted_time` datetime DEFAULT NULL COMMENT '最近一次送审时间',
  `audited_by` bigint DEFAULT NULL COMMENT '审核人',
  `audited_time` datetime DEFAULT NULL COMMENT '审核时间',
  `audit_remark` varchar(500) DEFAULT NULL COMMENT '审核意见',
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
  UNIQUE KEY `uk_purchase_contract_no` (`tenant_id`, `contract_no`),
  KEY `idx_purchase_contract_counterparty` (`tenant_id`, `counterparty_type`, `counterparty_name`),
  KEY `idx_purchase_contract_status` (`tenant_id`, `status`, `end_date`),
  CONSTRAINT `chk_purchase_contract_counterparty`
    CHECK ((`payee_id` IS NULL) <> (`supplier_id` IS NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='采购合同（采购条款）';

-- 适用品类行是"当前生效集合"：修改合同时整组重建（逻辑删除旧行 + 插入新行）。
-- 因此这里**不加** (tenant_id, contract_id, goods_config_id) 唯一键：逻辑删除的行仍在表里，
-- 唯一键会让"先移除再重新加入同一品类"直接报 DuplicateKey。历史约定由版本快照保证。
CREATE TABLE IF NOT EXISTS `icbc_purchase_contract_category` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `contract_id` bigint unsigned NOT NULL COMMENT '采购合同编号',
  `goods_config_id` bigint unsigned NOT NULL COMMENT '品类编号（icbc_goods_config）',
  `category_name` varchar(100) DEFAULT NULL COMMENT '品类名称快照',
  `unit` varchar(32) DEFAULT NULL COMMENT '计量单位快照',
  `tenant_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '租户编号',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_purchase_contract_category_contract` (`contract_id`),
  KEY `idx_purchase_contract_category_goods` (`tenant_id`, `contract_id`, `goods_config_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='采购合同适用品类';
CREATE TABLE IF NOT EXISTS `icbc_purchase_contract_version` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `contract_id` bigint unsigned NOT NULL COMMENT '采购合同编号',
  `version_no` int NOT NULL COMMENT '版本号，从 1 递增',
  `snapshot_json` mediumtext COMMENT '整份合同快照 JSON（含适用品类）',
  `snapshot_hash` varchar(64) DEFAULT NULL COMMENT '快照哈希（SHA-256）',
  `change_reason` varchar(500) DEFAULT NULL COMMENT '本版变更原因',
  `changed_by` varchar(64) DEFAULT NULL COMMENT '送审人',
  `audit_status` tinyint NOT NULL DEFAULT '0' COMMENT '本版审核状态：0-待审核，1-已通过，2-已驳回',
  `audited_by` bigint DEFAULT NULL COMMENT '本版审核人',
  `audited_time` datetime DEFAULT NULL COMMENT '本版审核时间',
  `audit_remark` varchar(500) DEFAULT NULL COMMENT '本版审核意见',
  `tenant_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '租户编号',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_purchase_contract_version` (`tenant_id`, `contract_id`, `version_no`),
  KEY `idx_purchase_contract_version_contract` (`contract_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='采购合同版本快照';
