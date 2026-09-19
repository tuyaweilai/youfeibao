-- 连接字符集：文件里有中文种子数据。客户端默认不是 utf8mb4 时（例如没有 LANG 的
-- `docker exec -i <mysql容器> mysql`，其默认是 latin1），中文会被双重编码存进库，
-- 界面上就是乱码。这里显式声明，导入时不必再依赖客户端参数。
SET NAMES utf8mb4;

-- e签宝认证流程表
CREATE TABLE `enterprise_esign_auth_flow` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `auth_flow_id` varchar(64) NOT NULL COMMENT 'e签宝认证流程ID',
  `enterprise_id` bigint(20) DEFAULT NULL COMMENT '企业ID（企业认证时使用）',
  `user_id` bigint(20) DEFAULT NULL COMMENT '用户ID（个人认证时使用）',
  `auth_type` tinyint(4) NOT NULL COMMENT '认证类型（1:企业认证, 2:个人认证）',
  `auth_status` tinyint(4) NOT NULL DEFAULT '0' COMMENT '认证状态（0:待认证, 1:认证中, 2:认证成功, 3:认证失败）',
  `auth_url` varchar(500) DEFAULT NULL COMMENT '认证链接',
  `auth_short_url` varchar(500) DEFAULT NULL COMMENT '认证短链接',
  `notify_status` tinyint(4) NOT NULL DEFAULT '0' COMMENT '回调状态（0:未回调, 1:已回调）',
  `notify_time` datetime DEFAULT NULL COMMENT '回调时间',
  `notify_data` text COMMENT '回调原始数据',
  `error_message` varchar(500) DEFAULT NULL COMMENT '错误信息',
  `org_id` varchar(64) DEFAULT NULL COMMENT 'e签宝组织ID',
  `person_id` varchar(64) DEFAULT NULL COMMENT 'e签宝个人ID',
  `tenant_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '租户ID',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_auth_flow_id` (`auth_flow_id`),
  KEY `idx_enterprise_id` (`enterprise_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_auth_type_status` (`auth_type`, `auth_status`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='e签宝认证流程表'; 