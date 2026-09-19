-- 连接字符集：文件里有中文种子数据。客户端默认不是 utf8mb4 时（例如没有 LANG 的
-- `docker exec -i <mysql容器> mysql`，其默认是 latin1），中文会被双重编码存进库，
-- 界面上就是乱码。这里显式声明，导入时不必再依赖客户端参数。
SET NAMES utf8mb4;

-- ========================================
-- 自然人身份层升为平台级并引入注册（#31，见 docs/adr/0017）
--
-- 1. 新建 icbc_natural_person（平台级自然人主体，**无 tenant_id**）
-- 2. 新建 icbc_natural_person_login（自然人主体 ↔ 登录凭证绑定，**无 tenant_id**）
-- 3. icbc_payee_info 补 natural_person_id，并回填既有数据
-- 4. 人工核对清单：同一身份证在不同租户的姓名 / 手机号不一致者
--
-- 幂等：建表用 IF NOT EXISTS；字段用 information_schema 判断后再 ALTER；
--       回填只处理 natural_person_id 为空的收方档案。
-- 两张新表已登记进 yudao-server 的 yudao.tenant.ignore-tables，否则会被拼上 tenant_id。
-- 菜单与按钮权限在 icbc-menu.sql 里幂等维护。
-- ========================================

-- 1. 平台级自然人主体
CREATE TABLE IF NOT EXISTS `icbc_natural_person` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `out_user_id` varchar(64) NOT NULL COMMENT '平台级外部用户编号（工行 outUserId），生成后不可变',
  `name` varchar(100) DEFAULT NULL COMMENT '身份登记姓名（最近一次）',
  `id_card_no` varchar(32) NOT NULL COMMENT '身份证件号码：身份的唯一锚点',
  `mobile` varchar(32) DEFAULT NULL COMMENT '身份登记手机号（最近一次，不等于登录凭证）',
  `real_name_status` tinyint DEFAULT '0' COMMENT '实人认证状态：0-未认证，1-认证中，2-认证通过，3-认证未通过',
  `real_name_msg` varchar(500) DEFAULT NULL COMMENT '实人认证失败原因',
  `real_name_time` datetime DEFAULT NULL COMMENT '实人认证通过时间',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态：0-正常，1-已停用（平台运营人工处置）',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_natural_person_id_card_no` (`id_card_no`),
  UNIQUE KEY `uk_natural_person_out_user_id` (`out_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='平台级自然人主体';

-- MySQL 8 建表默认 utf8mb4_0900_ai_ci，而本库快照（icbc_payee_info 等）是 utf8mb4_unicode_ci；
-- 两者直接比较（回填与运行时的身份证号关联）会报 "Illegal mix of collations"。
-- 老库若已按 0900_ai_ci 建过表，这里幂等地改成 unicode_ci。
SET @col_exists := (
  SELECT COUNT(1) FROM information_schema.TABLES
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'icbc_natural_person'
    AND TABLE_COLLATION <> 'utf8mb4_unicode_ci'
);
SET @ddl := IF(@col_exists = 1,
  'ALTER TABLE `icbc_natural_person` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci',
  'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. 自然人主体与登录凭证的绑定（多对多：一个手机号可被多个主体复用）
CREATE TABLE IF NOT EXISTS `icbc_natural_person_login` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `natural_person_id` bigint NOT NULL COMMENT '自然人主体编号',
  `member_user_id` bigint NOT NULL COMMENT '登录凭证：平台租户下的会员用户编号',
  `bound_at` datetime DEFAULT NULL COMMENT '绑定时间',
  `bind_source` varchar(20) DEFAULT NULL COMMENT '绑定来源：REGISTER-本人注册，OPS_CLAIM-平台运营人工认领',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注（人工认领时记录核实过程）',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_natural_person_login` (`natural_person_id`, `member_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='自然人主体与登录凭证的绑定';

SET @col_exists := (
  SELECT COUNT(1) FROM information_schema.TABLES
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'icbc_natural_person_login'
    AND TABLE_COLLATION <> 'utf8mb4_unicode_ci'
);
SET @ddl := IF(@col_exists = 1,
  'ALTER TABLE `icbc_natural_person_login` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci',
  'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3. 收方档案挂到自然人主体
SET @col_exists := (
  SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'icbc_payee_info' AND COLUMN_NAME = 'natural_person_id'
);
SET @ddl := IF(@col_exists = 0,
  'ALTER TABLE `icbc_payee_info` ADD COLUMN `natural_person_id` bigint DEFAULT NULL COMMENT ''自然人主体编号（平台级身份，ADR 0017）'' AFTER `partner_payee_id`',
  'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'icbc_payee_info' AND INDEX_NAME = 'idx_payee_natural_person'
);
SET @ddl := IF(@idx_exists = 0,
  'ALTER TABLE `icbc_payee_info` ADD INDEX `idx_payee_natural_person` (`natural_person_id`)',
  'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 4. 回填：按身份证件号码把既有收方档案合并到同一个自然人主体上。
--    同一个人在不同回收企业各有一条收方档案，这是**预期**情形（身份跨企业，收方档案按租户）。
--    out_user_id 用与运行时同一格式（NP + 32 位十六进制）生成，避免与既有值冲突。
INSERT INTO `icbc_natural_person` (`out_user_id`, `name`, `id_card_no`, `mobile`, `real_name_status`,
                                   `real_name_msg`, `real_name_time`, `status`, `creator`, `create_time`)
SELECT
    UPPER(CONCAT('NP', REPLACE(UUID(), '-', ''))),
    MAX(p.`name`),
    p.`id_card_no`,
    MAX(p.`mobile`),
    MAX(p.`real_name_status`),
    NULL,
    MAX(p.`real_name_time`),
    0,
    'migration',
    NOW()
FROM `icbc_payee_info` p
LEFT JOIN `icbc_natural_person` n ON n.`id_card_no` = p.`id_card_no`
WHERE p.`deleted` = 0 AND p.`id_card_no` IS NOT NULL AND p.`id_card_no` <> '' AND n.`id` IS NULL
GROUP BY p.`id_card_no`;

UPDATE `icbc_payee_info` p
JOIN `icbc_natural_person` n ON n.`id_card_no` = p.`id_card_no`
SET p.`natural_person_id` = n.`id`
WHERE p.`natural_person_id` IS NULL;

-- 5. 人工核对清单：同一身份证在不同租户的姓名 / 手机号不一致。
--    这类身份**不自动合并、不覆盖**（ADR 0017）。迁移本身不做任何合并动作；
--    平台运营在「自然人主体」页点「身份冲突清单」查看（GET /icbc/platform/natural-person/conflicts），
--    核实后在该页人工认领 / 解绑 / 停用。下面的查询与该接口同口径，仅作为 DBA 兜底核对。
--
-- SELECT p.`id_card_no`,
--        COUNT(DISTINCT p.`name`)   AS name_kinds,
--        COUNT(DISTINCT p.`mobile`) AS mobile_kinds,
--        GROUP_CONCAT(DISTINCT CONCAT(p.`tenant_id`, ':', p.`name`, '/', p.`mobile`) ORDER BY p.`tenant_id`) AS detail
-- FROM `icbc_payee_info` p
-- WHERE p.`deleted` = 0 AND p.`id_card_no` IS NOT NULL AND p.`id_card_no` <> ''
-- GROUP BY p.`id_card_no`
-- HAVING name_kinds > 1 OR mobile_kinds > 1;

-- 6. 通知幂等键加宽：平台级 out_user_id（NP + 32 位十六进制）比原来的收方档案编号长，
--    而 notifyId 是「类型:业务号:内容摘要」拼出来的（见 IcbcNotifyParser），64 位装不下。
--    这也是既有实现的潜在缺陷：业务号接近 64 位时同样会溢出。
SET @col_len := (
  SELECT CHARACTER_MAXIMUM_LENGTH FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'icbc_callback_notify' AND COLUMN_NAME = 'notify_id'
);
SET @ddl := IF(@col_len IS NOT NULL AND @col_len < 128,
  'ALTER TABLE `icbc_callback_notify` MODIFY COLUMN `notify_id` varchar(128) NOT NULL COMMENT ''通知ID（类型:业务号:内容摘要）''',
  'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
