-- 连接字符集：文件里有中文种子数据。客户端默认不是 utf8mb4 时（例如没有 LANG 的
-- `docker exec -i <mysql容器> mysql`，其默认是 latin1），中文会被双重编码存进库，
-- 界面上就是乱码。这里显式声明，导入时不必再依赖客户端参数。
SET NAMES utf8mb4;

-- ========================================
-- 收购接收结论与称量差异（#53 T15，见 docs/adr/0028 与 CONTEXT.md「结算重量」）
--
-- 验收结论可以是接收、部分接收或拒收：
--   * 接收量 accepted_weight：实际留下（进入库存）的重量；
--   * 退回量 rejected_weight：因质量 / 规格不合格退回出售者的重量；
--   * 余货出场量 residual_weight：未接收、随车（或随后）带离场站的余货重量。
--
-- 两条口径：
--   * 拒收部分（退回量 + 余货出场量）**不进应付**——金额按「结算重量 − 退回量 − 余货出场量」重算；
--     也**不进正常库存**——实物量取接收量（有值优先于净重），见 ADR 0028。
--   * 称量差异 weight_diff = 实物量（接收量优先，无则净重）− 结算重量（唯计价基准）。
--     只要两侧都算得出来就落库，**不静默抹平**；任一侧未知则为空，不拿 0 冒充。
--     差额供 #57 的异常表直接消费。
--
-- 全部可空：历史数据与新单未做接收结论时为空，行为不变（与 #50 / #51 的追加列同一做法）。
-- 幂等：仅当列不存在时 ALTER；不新增表，clean.sql 不需同步。菜单与权限已在 icbc-menu.sql 幂等维护。
-- ========================================

SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
             AND TABLE_NAME = 'icbc_acquisition' AND COLUMN_NAME = 'accepted_weight');
SET @ddl := IF(@col = 0, 'ALTER TABLE `icbc_acquisition` ADD COLUMN `accepted_weight` decimal(14,4) DEFAULT NULL COMMENT ''接收量：本次实际留下（进库存）的重量（#53）'' AFTER `quantity_note`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
             AND TABLE_NAME = 'icbc_acquisition' AND COLUMN_NAME = 'rejected_weight');
SET @ddl := IF(@col = 0, 'ALTER TABLE `icbc_acquisition` ADD COLUMN `rejected_weight` decimal(14,4) DEFAULT NULL COMMENT ''退回量：因质量 / 规格不合格退回出售者的重量（#53）'' AFTER `accepted_weight`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
             AND TABLE_NAME = 'icbc_acquisition' AND COLUMN_NAME = 'residual_weight');
SET @ddl := IF(@col = 0, 'ALTER TABLE `icbc_acquisition` ADD COLUMN `residual_weight` decimal(14,4) DEFAULT NULL COMMENT ''余货出场量：未接收、带离场站的余货重量（#53）'' AFTER `rejected_weight`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
             AND TABLE_NAME = 'icbc_acquisition' AND COLUMN_NAME = 'reject_reason');
SET @ddl := IF(@col = 0, 'ALTER TABLE `icbc_acquisition` ADD COLUMN `reject_reason` varchar(500) DEFAULT NULL COMMENT ''拒收原因；退回量大于 0 时必填（#53）'' AFTER `residual_weight`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
             AND TABLE_NAME = 'icbc_acquisition' AND COLUMN_NAME = 'weight_diff');
SET @ddl := IF(@col = 0, 'ALTER TABLE `icbc_acquisition` ADD COLUMN `weight_diff` decimal(14,4) DEFAULT NULL COMMENT ''称量差异 = 实物量 − 结算重量（不静默抹平，#53）'' AFTER `reject_reason`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx := (SELECT COUNT(1) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE()
             AND TABLE_NAME = 'icbc_acquisition' AND INDEX_NAME = 'idx_acquisition_weight_diff');
SET @ddl := IF(@idx = 0, 'ALTER TABLE `icbc_acquisition` ADD INDEX `idx_acquisition_weight_diff` (`tenant_id`, `weight_diff`)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
