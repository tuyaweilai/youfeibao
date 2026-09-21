-- 连接字符集：文件里有中文注释。客户端默认不是 utf8mb4 时（例如没有 LANG 的
-- `docker exec -i <mysql容器> mysql`，其默认是 latin1），中文会被双重编码存进库。
SET NAMES utf8mb4;

-- ========================================
-- 收方档案补「是否我行卡」（#91，建档向导）
--
-- 建档向导在银行卡确认页把「是否我行卡」定下来（`1`-我行用户 / `0`-非我行用户），本人确认过的值
-- 要一直带到收方入驻那一步；原先只有一个从没人确认过的缺省常量 `DEFAULT_ACCOUNT_CODE = "1"`。
-- 把它存在收方档案上，收方入驻发起时优先取它，缺省才回退到 1（原来那个猜的值）。
--
-- 幂等：ADD COLUMN 按 information_schema 判存在再执行。已建库的环境跑一次即可；
-- `icbc_payee_info.sql` 的建表语句也已同步加上这一列（新库直接是最终形状）。
-- 注意 `--` 后必须跟空格才是注释（本项目已两次踩过 1064）。
-- ========================================

SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
             AND TABLE_NAME = 'icbc_payee_info' AND COLUMN_NAME = 'account_code');
SET @ddl := IF(@col = 0, 'ALTER TABLE `icbc_payee_info` ADD COLUMN `account_code` varchar(2) DEFAULT NULL COMMENT ''是否我行用户：0-非我行用户，1-我行用户（建档向导确认页定下来的值，#91）'' AFTER `id_validity_period`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
