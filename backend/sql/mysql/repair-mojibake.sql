-- ========================================
-- 修复「中文种子数据被双重编码」——界面上表现为菜单名、模板、任务名等**乱码**。
--
-- 症状：库里存的是 `åå‘å¼€ç¥¨` 而不是 `反向开票`（`select hex(name)` 是 `C3A5C28F…`，
-- 而不是 `E58F8DE59091…`），菜单树里整片是 `å...`。
--
-- 原因：导入 `backend/sql/mysql/*.sql` 时，客户端的连接字符集不是 utf8mb4。
-- 典型是 `docker exec -i <mysql容器> mysql`——没有 LANG 时 mysql 客户端默认 latin1，
-- 文件里的 UTF-8 字节被当成 latin1 收下、再转成 utf8mb4 落库，中文就成了双重编码。
-- 这些文件现在都以 `SET NAMES utf8mb4;` 开头（自带声明），新导入不会再踩；
-- 本脚本用于修已经脏了的库。
--
-- 安全性：只改**能证明**是双重编码的单元格——把值按 latin1 解回字节、再按 utf8mb4 解，
-- 结果与原值不同且含汉字，才认定它是双重编码。英文、正常中文、本来就含 Latin-1 字符
-- （`é`、`·`）的值都不会被误改；解不出合法 UTF-8 的（NULL）也一律跳过。
-- 幂等：修过的行不再匹配条件，重复执行是空操作。
--
-- 用法：
--   mysql -h 127.0.0.1 -P 13308 -uroot -p --default-character-set=utf8mb4 ruoyi-vue-pro < repair-mojibake.sql
-- ========================================
SET NAMES utf8mb4;

DELIMITER $$
DROP PROCEDURE IF EXISTS `repair_mojibake` $$
CREATE PROCEDURE `repair_mojibake`()
BEGIN
  DECLARE done INT DEFAULT 0;
  DECLARE v_tbl VARCHAR(64);
  DECLARE v_col VARCHAR(64);
  DECLARE v_fixed VARCHAR(255);
  DECLARE v_n INT DEFAULT 0;
  DECLARE cur CURSOR FOR
    SELECT c.table_name, c.column_name
    FROM information_schema.columns c
    JOIN information_schema.tables t
      ON t.table_schema = c.table_schema AND t.table_name = c.table_name
    WHERE c.table_schema = DATABASE()
      AND t.table_type = 'BASE TABLE'
      AND c.data_type IN ('varchar', 'char', 'text', 'mediumtext', 'longtext')
      AND c.collation_name LIKE 'utf8mb4%';
  DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;

  OPEN cur;
  read_loop: LOOP
    FETCH cur INTO v_tbl, v_col;
    IF done = 1 THEN
      LEAVE read_loop;
    END IF;
    -- 按 latin1 解回字节、再按 utf8mb4 解 = 若原值是双重编码，这里得到真值
    SET v_fixed = CONCAT('CONVERT(CAST(CONVERT(`', v_col, '` USING latin1) AS BINARY) USING utf8mb4)');
    SET @s = CONCAT(
      'UPDATE `', v_tbl, '` SET `', v_col, '` = ', v_fixed,
      ' WHERE `', v_col, '` IS NOT NULL',
      '   AND ', v_fixed, ' IS NOT NULL',
      '   AND BINARY ', v_fixed, ' <> BINARY `', v_col, '`',
      '   AND ', v_fixed, ' REGEXP ''[一-龥]''');
    PREPARE stmt FROM @s;
    EXECUTE stmt;
    SET v_n = ROW_COUNT();
    DEALLOCATE PREPARE stmt;
    IF v_n > 0 THEN
      SELECT v_tbl AS `表`, v_col AS `列`, v_n AS `修复行数`;
    END IF;
  END LOOP;
  CLOSE cur;
END $$

CALL `repair_mojibake`() $$
DROP PROCEDURE `repair_mojibake` $$
DELIMITER ;
