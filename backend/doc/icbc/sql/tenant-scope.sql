-- ========================================
-- 反向开票：工行相关表加租户维度
-- ========================================
--
-- 背景：yudao 的多租户拦截器对未列入 `yudao.tenant.ignore-tables` 的表一律追加
-- `tenant_id = ?`。反向开票业务表（出售者档案、收购单、发票、证据、资金流、
-- 工行调用日志与通知）属于某个回收企业租户，必须带 `tenant_id`，否则线上查询
-- 会直接报「未知列 tenant_id」。
--
-- 与之对应的 Java 侧改动：这些 DO 一律继承 `TenantBaseDO`。
--
-- 注意：`icbc_callback_notify` 与 `icbc_api_log` 在 Service 层用 `@TenantIgnore`
-- 写入/读取（通知先于租户上下文到达、平台运营看全量日志），它们仍要有
-- `tenant_id` 列，但允许为 0。

ALTER TABLE `icbc_payee_info`
    ADD COLUMN `tenant_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '租户编号' AFTER `deleted`,
    ADD INDEX `idx_tenant_id` (`tenant_id`);

ALTER TABLE `icbc_payer_info`
    ADD COLUMN `tenant_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '租户编号' AFTER `deleted`,
    ADD INDEX `idx_tenant_id` (`tenant_id`);

ALTER TABLE `icbc_invoice_order`
    ADD COLUMN `tenant_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '租户编号' AFTER `deleted`,
    ADD INDEX `idx_tenant_id` (`tenant_id`);

ALTER TABLE `icbc_order_item`
    ADD COLUMN `tenant_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '租户编号' AFTER `deleted`,
    ADD INDEX `idx_tenant_id` (`tenant_id`);

ALTER TABLE `icbc_payment_order`
    ADD COLUMN `tenant_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '租户编号' AFTER `deleted`,
    ADD INDEX `idx_tenant_id` (`tenant_id`);

ALTER TABLE `icbc_invoice_download`
    ADD COLUMN `tenant_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '租户编号' AFTER `deleted`,
    ADD INDEX `idx_tenant_id` (`tenant_id`);

ALTER TABLE `icbc_invoice_file`
    ADD COLUMN `tenant_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '租户编号' AFTER `deleted`,
    ADD INDEX `idx_tenant_id` (`tenant_id`);

ALTER TABLE `icbc_api_log`
    ADD COLUMN `tenant_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '租户编号' AFTER `deleted`,
    ADD INDEX `idx_tenant_id` (`tenant_id`);

ALTER TABLE `icbc_callback_notify`
    ADD COLUMN `tenant_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '租户编号' AFTER `deleted`,
    ADD INDEX `idx_tenant_id` (`tenant_id`);

-- 出售者的唯一键从「全局唯一」改为「租户内唯一」：
-- 同一个自然人可以同时给两家回收企业供货，各自建档，彼此不可见。
-- 全局唯一会让第二家开不出票。先删旧唯一键，再建复合唯一键。
ALTER TABLE `icbc_payee_info`
    DROP INDEX `uk_id_card_no`,
    ADD UNIQUE KEY `uk_payee_tenant_id_card_no` (`tenant_id`, `id_card_no`),
    ADD UNIQUE KEY `uk_payee_tenant_mobile` (`tenant_id`, `mobile`);
