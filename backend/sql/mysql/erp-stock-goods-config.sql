-- T04（#42）：库存域以「品类」为维度，而不是 ERP 自己的产品。
--
-- 叠加在 `erp.sql` 之上，**不改上游 DDL**（`erp.sql` 保持逐字导出，便于与上游比对）。
-- 必须排在 `erp.sql` 之后导入（见 README 导入顺序）。
--
-- 做的事：
--   1. 12 张表的 `product_id` 换成 `goods_config_id`（指向 `icbc_goods_config.id`），
--      10 张明细表的 `product_unit_id` 去掉（单位由品类主数据给出，ERP 不再冗余）。
--   2. `erp_warehouse` 加 `station_id`（仓库归属场站）。
--   3. `erp_stock` 对 `(goods_config_id, warehouse_id)` 加唯一约束，修掉上游
--      「查不到就插一行 0」在并发下产生重复余额行的隐患。
--   4. 删掉 `erp_product` / `erp_product_category` / `erp_product_unit` 三张产品表。
--
-- 幂等：每步先查 information_schema 再执行；重复导入不报错。

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS `erp_t04_rename_product_id`;
DROP PROCEDURE IF EXISTS `erp_t04_drop_product_unit_id`;
DROP PROCEDURE IF EXISTS `erp_t04_add_station_id`;
DROP PROCEDURE IF EXISTS `erp_t04_add_stock_unique`;
DROP PROCEDURE IF EXISTS `erp_t04_rename_index`;
DROP PROCEDURE IF EXISTS `erp_t04_drop_index`;

DELIMITER $$
-- 1.1 product_id -> goods_config_id（列上的索引由 MySQL 自动跟随改名后的列）
CREATE PROCEDURE `erp_t04_rename_product_id`(IN p_table VARCHAR(64))
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema = DATABASE() AND table_name = p_table AND column_name = 'product_id') THEN
        SET @sql = CONCAT('ALTER TABLE `', p_table, '` CHANGE COLUMN `product_id` `goods_config_id` ',
                          'bigint NOT NULL COMMENT ''品类编号（icbc_goods_config.id）''');
        PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
    END IF;
END$$

-- 1.2 去掉 product_unit_id
CREATE PROCEDURE `erp_t04_drop_product_unit_id`(IN p_table VARCHAR(64))
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema = DATABASE() AND table_name = p_table AND column_name = 'product_unit_id') THEN
        SET @sql = CONCAT('ALTER TABLE `', p_table, '` DROP COLUMN `product_unit_id`');
        PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
    END IF;
END$$

-- 2. erp_warehouse.station_id
CREATE PROCEDURE `erp_t04_add_station_id`()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = DATABASE() AND table_name = 'erp_warehouse' AND column_name = 'station_id') THEN
        ALTER TABLE `erp_warehouse`
            ADD COLUMN `station_id` bigint NULL COMMENT '归属场站编号（icbc_station.id）' AFTER `remark`;
    END IF;
END$$

-- 3. erp_stock 唯一约束
CREATE PROCEDURE `erp_t04_add_stock_unique`()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics
                   WHERE table_schema = DATABASE() AND table_name = 'erp_stock'
                     AND index_name = 'uk_goods_config_warehouse') THEN
        ALTER TABLE `erp_stock`
            ADD UNIQUE INDEX `uk_goods_config_warehouse`(`goods_config_id` ASC, `warehouse_id` ASC) USING BTREE;
    END IF;
END$$

-- 4. 索引改名：列改名后，跟着列走的旧索引名不再反映语义
CREATE PROCEDURE `erp_t04_rename_index`(IN p_table VARCHAR(64), IN p_old VARCHAR(64), IN p_new VARCHAR(64))
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.statistics
               WHERE table_schema = DATABASE() AND table_name = p_table AND index_name = p_old)
       AND NOT EXISTS (SELECT 1 FROM information_schema.statistics
                       WHERE table_schema = DATABASE() AND table_name = p_table AND index_name = p_new) THEN
        SET @sql = CONCAT('ALTER TABLE `', p_table, '` RENAME INDEX `', p_old, '` TO `', p_new, '`');
        PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
    END IF;
END$$

-- 5. 删除被唯一约束取代的旧索引
CREATE PROCEDURE `erp_t04_drop_index`(IN p_table VARCHAR(64), IN p_index VARCHAR(64))
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.statistics
               WHERE table_schema = DATABASE() AND table_name = p_table AND index_name = p_index) THEN
        SET @sql = CONCAT('ALTER TABLE `', p_table, '` DROP INDEX `', p_index, '`');
        PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

CALL `erp_t04_rename_product_id`('erp_purchase_in_items');
CALL `erp_t04_rename_product_id`('erp_purchase_order_items');
CALL `erp_t04_rename_product_id`('erp_purchase_return_items');
CALL `erp_t04_rename_product_id`('erp_sale_order_items');
CALL `erp_t04_rename_product_id`('erp_sale_out_items');
CALL `erp_t04_rename_product_id`('erp_sale_return_items');
CALL `erp_t04_rename_product_id`('erp_stock');
CALL `erp_t04_rename_product_id`('erp_stock_record');
CALL `erp_t04_rename_product_id`('erp_stock_check_item');
CALL `erp_t04_rename_product_id`('erp_stock_in_item');
CALL `erp_t04_rename_product_id`('erp_stock_move_item');
CALL `erp_t04_rename_product_id`('erp_stock_out_item');

CALL `erp_t04_drop_product_unit_id`('erp_purchase_in_items');
CALL `erp_t04_drop_product_unit_id`('erp_purchase_order_items');
CALL `erp_t04_drop_product_unit_id`('erp_purchase_return_items');
CALL `erp_t04_drop_product_unit_id`('erp_sale_order_items');
CALL `erp_t04_drop_product_unit_id`('erp_sale_out_items');
CALL `erp_t04_drop_product_unit_id`('erp_sale_return_items');
CALL `erp_t04_drop_product_unit_id`('erp_stock_check_item');
CALL `erp_t04_drop_product_unit_id`('erp_stock_in_item');
CALL `erp_t04_drop_product_unit_id`('erp_stock_move_item');
CALL `erp_t04_drop_product_unit_id`('erp_stock_out_item');

CALL `erp_t04_add_station_id`();
CALL `erp_t04_add_stock_unique`();

CALL `erp_t04_rename_index`('erp_purchase_in_items', 'idx_product_id_warehouse_id_in_id', 'idx_goods_config_id_warehouse_id_in_id');
CALL `erp_t04_rename_index`('erp_purchase_order_items', 'idx_product_id_order_id', 'idx_goods_config_id_order_id');
CALL `erp_t04_rename_index`('erp_purchase_return_items', 'idx_product_id_warehouse_id_return_id', 'idx_goods_config_id_warehouse_id_return_id');
CALL `erp_t04_rename_index`('erp_sale_order_items', 'idx_product_id_order_id', 'idx_goods_config_id_order_id');
CALL `erp_t04_rename_index`('erp_sale_out_items', 'idx_product_id_warehouse_id_out_id', 'idx_goods_config_id_warehouse_id_out_id');
CALL `erp_t04_rename_index`('erp_sale_return_items', 'idx_product_id_warehouse_id_return_id', 'idx_goods_config_id_warehouse_id_return_id');
CALL `erp_t04_rename_index`('erp_stock_record', 'idx_product_id_warehouse_id', 'idx_goods_config_id_warehouse_id');
CALL `erp_t04_rename_index`('erp_stock_check_item', 'idx_product_id_warehouse_id_check_id', 'idx_goods_config_id_warehouse_id_check_id');
CALL `erp_t04_rename_index`('erp_stock_in_item', 'idx_product_id_warehouse_id_in_id', 'idx_goods_config_id_warehouse_id_in_id');
CALL `erp_t04_rename_index`('erp_stock_move_item', 'idx_product_id_from_warehouse_id_move_id', 'idx_goods_config_id_from_warehouse_id_move_id');
CALL `erp_t04_rename_index`('erp_stock_out_item', 'idx_product_id_warehouse_id_out_id', 'idx_goods_config_id_warehouse_id_out_id');

-- erp_stock 的旧普通索引已被 uk_goods_config_warehouse 取代
CALL `erp_t04_drop_index`('erp_stock', 'idx_product_id_warehouse_id');

DROP PROCEDURE IF EXISTS `erp_t04_rename_product_id`;
DROP PROCEDURE IF EXISTS `erp_t04_drop_product_unit_id`;
DROP PROCEDURE IF EXISTS `erp_t04_add_station_id`;
DROP PROCEDURE IF EXISTS `erp_t04_add_stock_unique`;
DROP PROCEDURE IF EXISTS `erp_t04_rename_index`;
DROP PROCEDURE IF EXISTS `erp_t04_drop_index`;

-- 6. 产品域三张表不导入
DROP TABLE IF EXISTS `erp_product`;
DROP TABLE IF EXISTS `erp_product_category`;
DROP TABLE IF EXISTS `erp_product_unit`;
