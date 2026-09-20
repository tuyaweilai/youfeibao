-- 连接字符集：文件里有中文注释。客户端默认不是 utf8mb4 时（例如没有 LANG 的
-- `docker exec -i <mysql容器> mysql`，其默认是 latin1），中文会被双重编码存进库。
SET NAMES utf8mb4;

-- ========================================
-- 物流域：多停靠点集货（V5 #72，见 ADR 0031 与 CONTEXT.md「物流与运力」）
--
-- 1. logistics_transport_stop  停靠点：一车提三家时的每一个提货 / 送货地点
-- 2. logistics_transport_node 加 stop_id：节点归到某一个停靠点
--
-- **一次集货不构成把几个出售者合并结算的依据**（ADR 0031、#59 的 Implementation Decisions 第 4 条）：
-- 每个停靠点各自交接、各自复磅、各自结算；整车复磅只核对总运输量。模型与文案都不得暗示
-- 「整车复磅可以合并结算」。
--
-- 停靠点状态（0 待处理 / 1 进行中 / 2 已完成 / 3 已取消）**按停靠点独立推进**：
-- 取消一个点只取消这一个（单点取消不影响其它点）。「已完成」以**交接完成**为准。
--
-- 出售者只存**编号 + 姓名 / 手机号快照**：物流不引用 icbc 的类（ADR 0032），
-- 编号是 icbc 侧的 `icbc_payee_info.id`，物流不知道它长什么样。
--
-- 幂等：ADD COLUMN 按 information_schema 判存在再执行；建表用 CREATE TABLE IF NOT EXISTS。
-- 注意 `--` 后必须跟空格才是注释：中文全角括号紧跟 `--` 会被 MySQL 当成语句，报 1064。
-- ========================================

CREATE TABLE IF NOT EXISTS `logistics_transport_stop` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `task_id` bigint unsigned NOT NULL COMMENT '运输任务编号',
  `task_no` varchar(64) NOT NULL COMMENT '运输任务单号（冗余）',
  `stop_no` int NOT NULL COMMENT '停靠顺序（从 1 开始，同一任务内唯一）',
  `stop_type` tinyint NOT NULL DEFAULT '1' COMMENT '停靠点类型：1-提货，2-送货',
  `payee_id` bigint unsigned DEFAULT NULL COMMENT '出售者编号（icbc 侧编号，可空）',
  `payee_name` varchar(64) DEFAULT NULL COMMENT '出售者姓名快照',
  `payee_mobile` varchar(32) DEFAULT NULL COMMENT '出售者手机号快照',
  `address` varchar(255) NOT NULL COMMENT '地址',
  `contact_name` varchar(64) DEFAULT NULL COMMENT '联系人',
  `contact_phone` varchar(32) DEFAULT NULL COMMENT '联系电话',
  `cargo_name` varchar(100) DEFAULT NULL COMMENT '货物名称（计划提示，不是品类权威）',
  `estimated_quantity` decimal(16,4) DEFAULT NULL COMMENT '约量（计划提示）',
  `quantity_unit` varchar(16) DEFAULT NULL COMMENT '约量单位',
  `expected_arrival_time` datetime DEFAULT NULL COMMENT '预计到站时间',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态：0-待处理，1-进行中，2-已完成，3-已取消',
  `cancel_reason` varchar(500) DEFAULT NULL COMMENT '取消原因（取消必填）',
  `cancel_time` datetime DEFAULT NULL COMMENT '取消时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `tenant_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '租户编号',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_stop_tenant_task_no` (`tenant_id`, `task_id`, `stop_no`),
  KEY `idx_stop_tenant_task` (`tenant_id`, `task_id`),
  KEY `idx_stop_tenant_payee` (`tenant_id`, `payee_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='物流 - 运输停靠点（一车可提多家，各自交接各自结算）';

-- 运输节点加 stop_id：到提货点 / 交接完成 / 起运归到某一个停靠点
SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
             AND TABLE_NAME = 'logistics_transport_node' AND COLUMN_NAME = 'stop_id');
SET @ddl := IF(@col = 0, 'ALTER TABLE `logistics_transport_node` ADD COLUMN `stop_id` bigint unsigned DEFAULT NULL COMMENT ''停靠点编号（提货相关节点非空；到达场站/卸货完成为整趟收尾，为空）'' AFTER `task_no`', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx := (SELECT COUNT(1) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE()
             AND TABLE_NAME = 'logistics_transport_node' AND INDEX_NAME = 'idx_node_tenant_stop');
SET @ddl := IF(@idx = 0, 'ALTER TABLE `logistics_transport_node` ADD INDEX `idx_node_tenant_stop` (`tenant_id`, `stop_id`)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
