-- 连接字符集：文件里有中文种子数据。客户端默认不是 utf8mb4 时（例如没有 LANG 的
-- `docker exec -i <mysql容器> mysql`，其默认是 latin1），中文会被双重编码存进库，
-- 界面上就是乱码。这里显式声明，导入时不必再依赖客户端参数。
SET NAMES utf8mb4;

-- ========================================
-- 自然人端：企业授权自助撤销留痕（#34）
--
-- 「企业授权」是自然人主体对某一家回收企业的开票与代办税费授权（CONTEXT.md），
-- 按企业分别管理、可自助撤销；**撤销只拦未来，已开出的票不追溯**（ADR 0017）。
-- 撤销是一个需要留痕的动作，所以在首次授权表上补撤销时间与原因。
--
-- 幂等：仅当列不存在时 ALTER。
-- ========================================

SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
             AND TABLE_NAME = 'icbc_seller_authorization' AND COLUMN_NAME = 'revoked_at');
SET @ddl := IF(@col = 0, 'ALTER TABLE `icbc_seller_authorization` ADD COLUMN `revoked_at` datetime DEFAULT NULL COMMENT ''自助撤销时间（撤销只拦未来，不追溯已开票）'' AFTER `authorized_at`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
             AND TABLE_NAME = 'icbc_seller_authorization' AND COLUMN_NAME = 'revoke_reason');
SET @ddl := IF(@col = 0, 'ALTER TABLE `icbc_seller_authorization` ADD COLUMN `revoke_reason` varchar(500) DEFAULT NULL COMMENT ''撤销原因'' AFTER `revoked_at`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 「我收到了」：自然人自行确认，**不改动银行状态**（payStatus 仍是唯一权威，ADR 0021）。
-- 他点了确认而银行状态未成功，本身是一个高价值的转人工信号，所以单独留痕时间与 IP。
SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
             AND TABLE_NAME = 'icbc_payment_order' AND COLUMN_NAME = 'seller_received_confirmed_at');
SET @ddl := IF(@col = 0, 'ALTER TABLE `icbc_payment_order` ADD COLUMN `seller_received_confirmed_at` datetime DEFAULT NULL COMMENT ''自然人自行确认收到的时间（不改银行状态）'' AFTER `receipt_time`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
             AND TABLE_NAME = 'icbc_payment_order' AND COLUMN_NAME = 'seller_received_confirm_ip');
SET @ddl := IF(@col = 0, 'ALTER TABLE `icbc_payment_order` ADD COLUMN `seller_received_confirm_ip` varchar(64) DEFAULT NULL COMMENT ''自然人自行确认收到时的 IP'' AFTER `seller_received_confirmed_at`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
