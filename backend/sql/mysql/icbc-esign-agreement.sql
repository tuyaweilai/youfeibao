-- 连接字符集：文件里有中文注释。客户端默认不是 utf8mb4 时（例如没有 LANG 的
-- `docker exec -i <mysql容器> mysql`，其默认是 latin1），中文会被双重编码存进库。
SET NAMES utf8mb4;

-- ========================================
-- 框架收购协议补「第三方签署任务号」与「告知函文件地址」（#95，见 ADR 0036）
--
-- 合同组电子签署发起成功后，把第三方的**合同组任务号**写在协议上：
-- 第三方回调只带子客编号（反查出租户）与任务号，不带我们的协议编号，
-- 所以「合同组整体签完」这条通知靠 `sign_task_id` 定位回具体协议。
-- 纸质协议（`sign_method = PAPER`）这一列为空。
--
-- 幂等：ADD COLUMN 按 information_schema 判存在再执行。已建库的环境跑一次即可；
-- `icbc-seller-onboarding.sql` 的建表语句也已同步加上这一列（新库直接是最终形状）。
-- 注意 `--` 后必须跟空格才是注释（本项目已两次踩过 1064）。
-- ========================================

SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
             AND TABLE_NAME = 'icbc_framework_agreement' AND COLUMN_NAME = 'sign_task_id');
SET @ddl := IF(@col = 0, 'ALTER TABLE `icbc_framework_agreement` ADD COLUMN `sign_task_id` varchar(64) DEFAULT NULL COMMENT ''第三方签署任务号（合同组任务号）：回调据此定位协议；纸质为空'' AFTER `sign_method`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx := (SELECT COUNT(1) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE()
             AND TABLE_NAME = 'icbc_framework_agreement' AND INDEX_NAME = 'idx_sign_task_id');
SET @ddl := IF(@idx = 0, 'ALTER TABLE `icbc_framework_agreement` ADD INDEX `idx_sign_task_id` (`sign_task_id`)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 合同组里的**第二份文书**（反向发票合规告知函）单独落址：`file_url` 只放主文书
-- （框架收购协议），告知函放 `notice_file_url`。两份文书在证据链上分别成条
-- （ADR 0036 决策 3：两份文书要能分别引用，所以不拼成一个 PDF），同归
-- FRAMEWORK_AGREEMENT 这一类型 / 合同流，不新增证据类型、不新增第六流。
SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
             AND TABLE_NAME = 'icbc_framework_agreement' AND COLUMN_NAME = 'notice_file_url');
SET @ddl := IF(@col = 0, 'ALTER TABLE `icbc_framework_agreement` ADD COLUMN `notice_file_url` varchar(500) DEFAULT NULL COMMENT ''反向发票合规告知函文件地址（合同组第二份文书，与协议分别成条进证据链）'' AFTER `file_url`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
