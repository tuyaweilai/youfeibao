-- 连接字符集：文件里有中文注释。客户端默认不是 utf8mb4 时（例如没有 LANG 的
-- `docker exec -i <mysql容器> mysql`，其默认是 latin1），中文会被双重编码存进库。
SET NAMES utf8mb4;

-- ========================================
-- 换卡单补「是否我行卡」（#86，见 ADR 0035）
--
-- 换卡走工行的**收方修改数据接口**（`edpreceive/update`），它允许修改的字段里就有
-- 「是否我行用户」（0-非我行用户 / 1-我行用户），且必须与新卡号成对出现。
-- 这个字段原本借入驻提交 VO（`SellerOnboardingSubmitReqVO.accountCode`）传，属于发起侧的字段
-- 却从入驻入参里读；现在落到换卡单上，发起时定下来（#86 的字段补齐）。
--
-- 幂等：ADD COLUMN 按 information_schema 判存在再执行。已建库的环境跑一次即可；
-- `icbc-bank-card-change.sql` 的建表语句也已同步加上这一列（新库直接是最终形状）。
-- 注意 `--` 后必须跟空格才是注释（本项目已两次踩过 1064）。
-- ========================================

SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
             AND TABLE_NAME = 'icbc_payee_bank_card_change' AND COLUMN_NAME = 'account_code');
SET @ddl := IF(@col = 0, 'ALTER TABLE `icbc_payee_bank_card_change` ADD COLUMN `account_code` varchar(2) DEFAULT NULL COMMENT ''是否我行用户：0-非我行用户，1-我行用户（为空按 1 上送）'' AFTER `new_bank_branch`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
