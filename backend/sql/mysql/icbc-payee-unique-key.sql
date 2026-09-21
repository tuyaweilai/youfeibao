-- 连接字符集：文件里有中文注释。客户端默认不是 utf8mb4 时（例如没有 LANG 的
-- `docker exec -i <mysql容器> mysql`，其默认是 latin1），中文会被双重编码存进库。
SET NAMES utf8mb4;

-- ========================================
-- ⚠️ 必须先指定数据库！用法：
--     mysql -uroot -p <库名> < icbc-payee-unique-key.sql
--     docker exec -i <mysql容器> mysql -uroot -p <库名> < icbc-payee-unique-key.sql
--
-- 为什么反复强调：下面的守卫写的是 `TABLE_SCHEMA = DATABASE()`。**不带库名**时
-- `DATABASE()` 是 NULL，守卫恒假，DROP / ADD 全部走 `SELECT 1`——脚本退出码 0、
-- 唯一键一个都没换，而本文件存在的唯一理由就是给「已经建过库的环境」换这两个键，
-- 静默不干活是最坏的结果。所以同 icbc-wallet-columns-drop.sql，开头加一道硬失败守卫：
-- 没选库就当场报错，不装作成功。
-- ========================================
SET @guard := IF(DATABASE() IS NULL,
    'SELECT 1 FROM `__请指定数据库：mysql -uroot -p 库名 < 本文件` . `t`',
    'DO 0');
PREPARE guard_stmt FROM @guard; EXECUTE guard_stmt; DEALLOCATE PREPARE guard_stmt;

-- ========================================
-- 收方档案唯一键补 tenant_id（#97，ADR 0017 / CONTEXT「收方档案」）
--
-- 收方档案是「自然人 × 回收企业」这一层：同一自然人在两家回收企业就该有两份档案。
-- 服务层 `PayeeInfoServiceImpl#validateIdCardNoUnique` / `#validateMobileUnique` 走
-- `selectByIdCardNo` / `selectByMobile`，MyBatis-Plus 的租户插件已经给它加了 `tenant_id` 条件——
-- 也就是「本租户内一张身份证 / 一个手机号一份档案」这条规则**早就实现了**。
-- 只有建表脚本的键没跟上：`(id_card_no, deleted)` / `(mobile, deleted)` 是全局的，
-- 第二家企业建档时 INSERT 直接撞键（500），而 Service 层按租户查又看不到别家的行。
--
-- 改成 `(tenant_id, id_card_no, deleted)` / `(tenant_id, mobile, deleted)`。
-- 旧键**严格强于**新键（列更少 = 更严），所以这是纯放开：不可能有既存行违反新键，
-- 迁移不做任何数据校验也不会失败。
--
-- `deleted` 保留，语义与改前**完全一致**：同一租户「软删 → 重建 → 再软删」时第二次软删
-- 仍会撞键（两条 `deleted=1` 同键值），这个既有毛病原样保留，不在本票处理。
--
-- 幂等：按 information_schema.STATISTICS 判索引的存在与列组成——旧形状先 DROP，再 ADD 新形状；
-- 已经是新形状或键不存在时都跳过。重复跑安全。
-- 新库直接导 `icbc_payee_info.sql`（已是最终形状），不需要跑本文件。
-- 注意 `--` 后必须跟空格才是注释（本项目已两次踩过 1064）。
-- ========================================

-- 1. 身份证号唯一键：先 DROP 不含 tenant_id 的旧形状，再 ADD 新形状
SET @cols := (SELECT GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) FROM information_schema.STATISTICS
              WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'icbc_payee_info' AND INDEX_NAME = 'uk_id_card_no');
SET @ddl := IF(@cols IS NOT NULL AND FIND_IN_SET('tenant_id', @cols) = 0,
    'ALTER TABLE `icbc_payee_info` DROP INDEX `uk_id_card_no`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @tbl := (SELECT COUNT(1) FROM information_schema.TABLES
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'icbc_payee_info');
SET @cols := (SELECT GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) FROM information_schema.STATISTICS
              WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'icbc_payee_info' AND INDEX_NAME = 'uk_id_card_no');
SET @ddl := IF(@tbl = 1 AND @cols IS NULL,
    'ALTER TABLE `icbc_payee_info` ADD UNIQUE KEY `uk_id_card_no` (`tenant_id`, `id_card_no`, `deleted`)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2. 手机号唯一键：同上
SET @cols := (SELECT GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) FROM information_schema.STATISTICS
              WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'icbc_payee_info' AND INDEX_NAME = 'uk_mobile');
SET @ddl := IF(@cols IS NOT NULL AND FIND_IN_SET('tenant_id', @cols) = 0,
    'ALTER TABLE `icbc_payee_info` DROP INDEX `uk_mobile`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @tbl := (SELECT COUNT(1) FROM information_schema.TABLES
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'icbc_payee_info');
SET @cols := (SELECT GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) FROM information_schema.STATISTICS
              WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'icbc_payee_info' AND INDEX_NAME = 'uk_mobile');
SET @ddl := IF(@tbl = 1 AND @cols IS NULL,
    'ALTER TABLE `icbc_payee_info` ADD UNIQUE KEY `uk_mobile` (`tenant_id`, `mobile`, `deleted`)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
