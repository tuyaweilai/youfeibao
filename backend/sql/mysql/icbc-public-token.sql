-- 公开令牌表（给没有账号的自然人用的短期单用途令牌）
-- 本表不带租户维度：令牌必须先于租户上下文被读到，才知道把请求放到哪个租户下执行。
-- 已登记进 yudao.tenant.ignore-tables；租户编号以普通列 tenant_id 显式存储。
CREATE TABLE IF NOT EXISTS `icbc_public_token` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `jti` varchar(64) NOT NULL COMMENT '令牌唯一编号（签名 payload 的 jti）',
    `purpose` varchar(40) NOT NULL COMMENT '用途：INVOICE_DOWNLOAD/CONTACT_LEAD/QUOTA_QUERY',
    `tenant_id` bigint NOT NULL COMMENT '令牌解析出的租户编号',
    `business_key` varchar(64) NOT NULL COMMENT '绑定业务键：订单号或收方ID',
    `max_uses` int NOT NULL DEFAULT 1 COMMENT '允许使用次数',
    `used_count` int NOT NULL DEFAULT 0 COMMENT '已使用次数',
    `expires_time` datetime NOT NULL COMMENT '过期时间',
    `last_used_time` datetime DEFAULT NULL COMMENT '最后使用时间',
    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
    `creator` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_jti` (`jti`),
    KEY `idx_tenant_id` (`tenant_id`),
    KEY `idx_business_key` (`business_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='公开令牌表';

-- 收方入驻失败后留下的联系方式（租户级）
CREATE TABLE IF NOT EXISTS `icbc_contact_lead` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `payee_id` bigint NOT NULL COMMENT '关联收方ID',
    `name` varchar(100) NOT NULL COMMENT '出售者姓名',
    `mobile` varchar(32) NOT NULL COMMENT '联系方式',
    `remark` varchar(500) DEFAULT NULL COMMENT '备注 / 失败原因',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    `creator` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    PRIMARY KEY (`id`),
    KEY `idx_payee_id` (`payee_id`),
    KEY `idx_tenant_id` (`tenant_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收方入驻失败留联系方式表';
