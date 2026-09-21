-- 连接字符集：文件里有中文注释。客户端默认不是 utf8mb4 时（例如没有 LANG 的
-- `docker exec -i <mysql容器> mysql`，其默认是 latin1），中文会被双重编码存进库。
SET NAMES utf8mb4;

-- ========================================
-- 自然人主体补「证件签发日期 / 证件截止日期」（#91 修票，SP-1）
--
-- 建模口径（#81 决策 5、CONTEXT「收方档案」词条、ADR 0017）：姓名、证件号、证件有效期这类
-- **平台级身份字段由自然人主体持有**，收方档案上的同名列只是「本次登记确认的值」。
-- #91 第一版只把这两个日期写进了 icbc_payee_info，主体上没有，于是同一个人在第二家回收企业
-- 建档时证件有效期不会复用，与领域模型相反。
--
-- 语义与收方档案上的同名列一致：`yyyy-MM-dd`，长期有效传 `9999-12-30`。
-- 回填只处理既有主体里空缺、而收方档案里有的情形（取该主体名下档案里最小的那个值，日期串按字典序即时间序）；
-- 已有值一律不动。
--
-- 幂等：ADD COLUMN 按 information_schema 判存在再执行。已建库的环境跑一次即可；
-- `icbc-natural-person.sql` 的建表语句也已同步加上这两列（新库直接是最终形状）。
-- 注意 `--` 后必须跟空格才是注释（本项目已两次踩过 1064）。
-- ========================================

SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
             AND TABLE_NAME = 'icbc_natural_person' AND COLUMN_NAME = 'id_sign_date');
SET @ddl := IF(@col = 0, 'ALTER TABLE `icbc_natural_person` ADD COLUMN `id_sign_date` varchar(10) DEFAULT NULL COMMENT ''证件签发日期 yyyy-MM-dd（平台级身份字段，跨企业复用，#91）'' AFTER `mobile`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
             AND TABLE_NAME = 'icbc_natural_person' AND COLUMN_NAME = 'id_validity_period');
SET @ddl := IF(@col = 0, 'ALTER TABLE `icbc_natural_person` ADD COLUMN `id_validity_period` varchar(10) DEFAULT NULL COMMENT ''证件截止日期 yyyy-MM-dd，永久有效传 9999-12-30（同上）'' AFTER `id_sign_date`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 回填：主体上空缺、而它名下的收方档案里填过 —— 取该主体所有档案里最小的那个值（日期串按字典序即时间序）。
-- 两列分开判空，已有值不覆盖（与 ADR 0017 的「不覆盖」同一条精神）。
UPDATE `icbc_natural_person` n
JOIN (
    SELECT p.`natural_person_id`,
           MIN(NULLIF(p.`id_sign_date`, '')) AS `id_sign_date`,
           MIN(NULLIF(p.`id_validity_period`, '')) AS `id_validity_period`
    FROM `icbc_payee_info` p
    WHERE p.`deleted` = 0 AND p.`natural_person_id` IS NOT NULL
    GROUP BY p.`natural_person_id`
) f ON f.`natural_person_id` = n.`id`
SET n.`id_sign_date` = IF(n.`id_sign_date` IS NULL OR n.`id_sign_date` = '', f.`id_sign_date`, n.`id_sign_date`),
    n.`id_validity_period` = IF(n.`id_validity_period` IS NULL OR n.`id_validity_period` = '', f.`id_validity_period`, n.`id_validity_period`)
WHERE (n.`id_sign_date` IS NULL OR n.`id_sign_date` = '' OR n.`id_validity_period` IS NULL OR n.`id_validity_period` = '');
