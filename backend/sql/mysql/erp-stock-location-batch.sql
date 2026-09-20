-- T05（#43）：仓库之下补库位与批次，让库存按「品类 + 仓库 + 库位 + 批次」四个维度归属。
--
-- 叠加在 `erp.sql` + `erp-stock-goods-config.sql` 之上，**不改上游 DDL**。
-- 必须排在 `erp-stock-goods-config.sql` 之后导入（见 README 导入顺序）。
--
-- 做的事：
--   1. 新建 `erp_stock_location`（库位）与 `erp_stock_batch`（批次）两张租户表。
--   2. `erp_stock` / `erp_stock_record` 各加 `location_id` / `batch_id`。
--      用 `NOT NULL DEFAULT 0` 而不是 NULL：NULL 在唯一索引里互不相等，会让同一
--      (品类, 仓库, 未指定, 未指定) 出现多行余额；0 表示「未指定库位 / 批次」。
--   3. `erp_stock` 的唯一约束从 `(goods_config_id, warehouse_id)` 扩成
--      `(goods_config_id, warehouse_id, location_id, batch_id)`（AC 明确「并保持唯一」）。
--      旧数据满足新约束（旧约束是新约束的真子集），直接换即可。
--   4. 补 `erp_stock_record` 的 4 维索引，供按库位 / 批次查流水。
--
-- 幂等：先查 information_schema 再执行；重复导入不报错。

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS `erp_t05_add_column`;
DROP PROCEDURE IF EXISTS `erp_t05_drop_index`;
DROP PROCEDURE IF EXISTS `erp_t05_add_stock_unique`;

DELIMITER $$
-- 1.1 加列（列不存在才加）
CREATE PROCEDURE `erp_t05_add_column`(IN p_table VARCHAR(64), IN p_column VARCHAR(64), IN p_after VARCHAR(64), IN p_comment VARCHAR(255))
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = DATABASE() AND table_name = p_table AND column_name = p_column) THEN
        SET @sql = CONCAT('ALTER TABLE `', p_table, '` ADD COLUMN `', p_column,
                          '` bigint NOT NULL DEFAULT 0 COMMENT ''', p_comment, ''' AFTER `', p_after, '`');
        PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
    END IF;
END$$

-- 1.2 删索引
CREATE PROCEDURE `erp_t05_drop_index`(IN p_table VARCHAR(64), IN p_index VARCHAR(64))
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.statistics
               WHERE table_schema = DATABASE() AND table_name = p_table AND index_name = p_index) THEN
        SET @sql = CONCAT('ALTER TABLE `', p_table, '` DROP INDEX `', p_index, '`');
        PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
    END IF;
END$$

-- 1.3 erp_stock 4 维唯一约束
CREATE PROCEDURE `erp_t05_add_stock_unique`()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics
                   WHERE table_schema = DATABASE() AND table_name = 'erp_stock'
                     AND index_name = 'uk_goods_config_warehouse_location_batch') THEN
        ALTER TABLE `erp_stock`
            ADD UNIQUE INDEX `uk_goods_config_warehouse_location_batch`
                (`goods_config_id` ASC, `warehouse_id` ASC, `location_id` ASC, `batch_id` ASC) USING BTREE;
    END IF;
END$$
DELIMITER ;

-- 1. 库位表
CREATE TABLE IF NOT EXISTS `erp_stock_location` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
    `warehouse_id` bigint NOT NULL COMMENT '仓库编号（erp_warehouse.id）',
    `name` varchar(64) NOT NULL COMMENT '库位名称',
    `sort` bigint NOT NULL DEFAULT 0 COMMENT '排序',
    `remark` varchar(255) DEFAULT NULL COMMENT '备注',
    `status` tinyint NOT NULL DEFAULT 0 COMMENT '开启状态（0 开启 1 停用）',
    `creator` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    KEY `idx_warehouse_id` (`warehouse_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'ERP 库位';

-- 2. 批次表
CREATE TABLE IF NOT EXISTS `erp_stock_batch` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
    `batch_no` varchar(64) NOT NULL COMMENT '批次号',
    `goods_config_id` bigint DEFAULT NULL COMMENT '品类编号（icbc_goods_config.id，可空表示通用批次）',
    `in_time` datetime DEFAULT NULL COMMENT '入库时间',
    `remark` varchar(255) DEFAULT NULL COMMENT '备注',
    `status` tinyint NOT NULL DEFAULT 0 COMMENT '开启状态（0 开启 1 停用）',
    `creator` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_batch_no` (`tenant_id`, `batch_no`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'ERP 批次';

-- 3. erp_stock / erp_stock_record 加库位与批次
CALL `erp_t05_add_column`('erp_stock', 'location_id', 'warehouse_id', '库位编号（erp_stock_location.id，0 表示未指定）');
CALL `erp_t05_add_column`('erp_stock', 'batch_id', 'location_id', '批次编号（erp_stock_batch.id，0 表示未指定）');
CALL `erp_t05_add_column`('erp_stock_record', 'location_id', 'warehouse_id', '库位编号（erp_stock_location.id，0 表示未指定）');
CALL `erp_t05_add_column`('erp_stock_record', 'batch_id', 'location_id', '批次编号（erp_stock_batch.id，0 表示未指定）');

-- 4. 唯一约束：旧 (goods_config_id, warehouse_id) 扩成 4 维
CALL `erp_t05_drop_index`('erp_stock', 'uk_goods_config_warehouse');
CALL `erp_t05_add_stock_unique`();
-- 旧普通索引若还在，删掉（4 维唯一约束已覆盖前缀）
CALL `erp_t05_drop_index`('erp_stock', 'idx_goods_config_id_warehouse_id');

DROP PROCEDURE IF EXISTS `erp_t05_add_column`;
DROP PROCEDURE IF EXISTS `erp_t05_drop_index`;
DROP PROCEDURE IF EXISTS `erp_t05_add_stock_unique`;
