-- T06（#44）：单位供货方档案。
--
-- 单位供货方与自然人出售者是「交易对方」的两种，主体类型六态、判定规则是「是否属于自然人」
-- （ADR 0029）。自然人出售者的档案在 `icbc_payee_info`，不进 `erp_supplier`；单位供货方的
-- 档案就落在 ERP 供应商表上，这里补齐它缺的字段（ADR 0027 的双外键承载）。
--
-- 叠加在 `erp.sql` 之上，**不改上游 DDL**（`erp.sql` 保持逐字导出，便于与上游比对）。
-- 必须排在 `erp.sql` 之后导入（见 README 导入顺序），与 `erp-stock-*.sql` 之间无先后依赖。
--
-- 做的事：给 `erp_supplier` 加 `subject_type`（主体类型）、`taxpayer_qualification`（纳税人资格）、
-- `address`（地址）。税号 / 开户行 / 开户账号 / 联系人 / 联系电话沿用上游已有列。
--
-- 幂等：每列先查 information_schema 再执行；重复导入不报错。

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS `erp_t06_add_column`;

DELIMITER $$
CREATE PROCEDURE `erp_t06_add_column`(IN p_table VARCHAR(64), IN p_column VARCHAR(64), IN p_definition VARCHAR(255))
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = DATABASE() AND table_name = p_table AND column_name = p_column) THEN
        SET @sql = CONCAT('ALTER TABLE `', p_table, '` ADD COLUMN `', p_column, '` ', p_definition);
        PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

-- 主体类型：NULL 容忍历史数据；新建 / 修改由服务层强制填写，且不能是自然人。
CALL `erp_t06_add_column`('erp_supplier', 'subject_type', 'tinyint NULL COMMENT ''主体类型（SellerSubjectTypeEnum：1 自然人 / 2 个体工商户 / 3 个人独资企业 / 4 合伙企业 / 5 企业法人 / 6 农民专业合作社）'' AFTER `sort`');
CALL `erp_t06_add_column`('erp_supplier', 'taxpayer_qualification', 'tinyint NULL COMMENT ''纳税人资格（TaxpayerQualificationEnum：1 一般纳税人 / 2 小规模纳税人）'' AFTER `subject_type`');
CALL `erp_t06_add_column`('erp_supplier', 'address', 'varchar(255) NULL COMMENT ''地址'' AFTER `taxpayer_qualification`');

DROP PROCEDURE IF EXISTS `erp_t06_add_column`;
