-- 添加部门与企业关联表
CREATE TABLE IF NOT EXISTS `enterprise_dept_link` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `enterprise_id` bigint NOT NULL COMMENT '企业ID',
  `dept_id` bigint NOT NULL COMMENT '部门ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dept_tenant` (`dept_id`, `tenant_id`) COMMENT '部门和租户唯一索引',
  KEY `idx_enterprise_id` (`enterprise_id`) COMMENT '企业ID索引'
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='部门与企业关联表'; 