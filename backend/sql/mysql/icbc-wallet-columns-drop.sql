-- 连接字符集：文件里有中文注释。客户端默认不是 utf8mb4 时（例如没有 LANG 的
-- `docker exec -i <mysql容器> mysql`，其默认是 latin1），中文会被双重编码存进库。
SET NAMES utf8mb4;

-- ========================================
-- 一次性迁移：清掉「电子钱包 / 电子账户 / 开户状态」的残留列（#87，见 ADR 0035）
--
-- 收方入驻改走数据接口后（#83），工行只回审核结论，不再有账户开通这条线；状态机也只剩
-- 审核三态。于是这些当年「原样透传」的列已经没有产生它们的路径，留着只会误导：
--
--   * `icbc_payee_info`：`icbc_medium_id`（工行电子账户账号）、`icbc_openacct_status`（开户状态）
--   * `icbc_payee_bank_card_change`：`icbc_medium_id`、`icbc_openacct_status`
--   * `icbc_payer_info`：`icbc_medium_id` / `icbc_openacct_status` —— 付方侧同名的死列。
--     当初（ADR 0010 的第一轮清理）把它们降级为「工行返回的不透明字段」，但之后再没有任何
--     代码写过它们（付方回调只带 payerStatus），所以一并清掉，接口里也不再出现「开户状态」。
--
-- **不可逆**：列与列里的历史值都会被删掉。执行前请确认这些列已无写入方（本仓库当前代码
-- 已不引用它们；建表脚本 `icbc_payee_info.sql` / `icbc-bank-card-change.sql` / `icbc_payer_info.sql`
-- 也已是「没有这些列」的最终形状，新库直接导建表脚本即可，不需要跑本文件）。
--
-- 幂等：DROP COLUMN 按 information_schema 判存在再执行，重复跑安全。
-- 注意 `--` 后必须跟空格才是注释（本项目已两次踩过 1064）。
-- ========================================

-- 1. 收方档案
SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
             AND TABLE_NAME = 'icbc_payee_info' AND COLUMN_NAME = 'icbc_medium_id');
SET @ddl := IF(@col = 1, 'ALTER TABLE `icbc_payee_info` DROP COLUMN `icbc_medium_id`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
             AND TABLE_NAME = 'icbc_payee_info' AND COLUMN_NAME = 'icbc_openacct_status');
SET @ddl := IF(@col = 1, 'ALTER TABLE `icbc_payee_info` DROP COLUMN `icbc_openacct_status`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2. 换卡单
SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
             AND TABLE_NAME = 'icbc_payee_bank_card_change' AND COLUMN_NAME = 'icbc_medium_id');
SET @ddl := IF(@col = 1, 'ALTER TABLE `icbc_payee_bank_card_change` DROP COLUMN `icbc_medium_id`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
             AND TABLE_NAME = 'icbc_payee_bank_card_change' AND COLUMN_NAME = 'icbc_openacct_status');
SET @ddl := IF(@col = 1, 'ALTER TABLE `icbc_payee_bank_card_change` DROP COLUMN `icbc_openacct_status`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3. 付方档案（同名的死列，一并清掉）
SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
             AND TABLE_NAME = 'icbc_payer_info' AND COLUMN_NAME = 'icbc_medium_id');
SET @ddl := IF(@col = 1, 'ALTER TABLE `icbc_payer_info` DROP COLUMN `icbc_medium_id`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
             AND TABLE_NAME = 'icbc_payer_info' AND COLUMN_NAME = 'icbc_openacct_status');
SET @ddl := IF(@col = 1, 'ALTER TABLE `icbc_payer_info` DROP COLUMN `icbc_openacct_status`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
