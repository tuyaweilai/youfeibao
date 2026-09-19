-- ========================================
-- 场站与场站二维码（#34，见 docs/adr/0017 与 CONTEXT.md「场站」）
--
-- 场站是回收企业实际发生收购的固定场所，是交易地点与收货二维码的粒度。
-- 二维码一码一场站，印在磅房 / 墙上：码内不带任何令牌（公开且长期贴），
-- 只编码 station_code，由服务端解析出企业与场站的公开信息。
--
-- 幂等：CREATE TABLE IF NOT EXISTS。租户表（带 tenant_id），不进 ignore-tables。
-- 菜单与按钮权限在 icbc-menu.sql 里幂等维护。
-- ========================================

CREATE TABLE IF NOT EXISTS `icbc_station` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `station_code` varchar(64) NOT NULL COMMENT '场站码（全局唯一，二维码只编码它）',
  `name` varchar(100) NOT NULL COMMENT '场站名称',
  `address` varchar(255) DEFAULT NULL COMMENT '场站地址',
  `contact_mobile` varchar(32) DEFAULT NULL COMMENT '场站联系电话（公开，用于联系客服）',
  `open_status` tinyint NOT NULL DEFAULT '1' COMMENT '是否在收货：1-在收货，0-暂停收货',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `tenant_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '租户编号',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_station_code` (`station_code`),
  KEY `idx_station_tenant` (`tenant_id`, `open_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='场站（收货二维码的粒度）';
