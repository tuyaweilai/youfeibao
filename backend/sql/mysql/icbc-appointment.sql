-- 连接字符集：文件里有中文种子数据。客户端默认不是 utf8mb4 时（例如没有 LANG 的
-- `docker exec -i <mysql容器> mysql`，其默认是 latin1），中文会被双重编码存进库，
-- 界面上就是乱码。这里显式声明，导入时不必再依赖客户端参数。
SET NAMES utf8mb4;

-- ========================================
-- 到站预约（#35，见 docs/adr/0020-预约到站不是订单.md 与 docs/adr/0004）
--
-- 自然人主动声明「我将在某个时间到某个场站卖某品类、大约多少量、车牌是多少」，
-- 用于排队与到站登记时带出。**不是订单**：
--   * 不是合同、不占额度、不产生开票、不进五流；
--   * 没有「企业接受 / 拒绝」动作，只有「到场」与「未到场」；
--   * 预计数量一律以「约」标注，任何统计与额度口径都不得引用本表。
--
-- 幂等：CREATE TABLE IF NOT EXISTS。租户表（带 tenant_id），不进 ignore-tables。
-- 菜单与按钮权限在 icbc-menu.sql 里幂等维护。
-- ========================================

CREATE TABLE IF NOT EXISTS `icbc_appointment` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `appointment_no` varchar(64) NOT NULL COMMENT '预约编号',
  `natural_person_id` bigint unsigned NOT NULL COMMENT '自然人主体编号（平台级身份）',
  `payee_id` bigint unsigned DEFAULT NULL COMMENT '收方（出售者）档案编号；本租户尚未建档则为空',
  `seller_name` varchar(100) DEFAULT NULL COMMENT '出售者姓名快照',
  `station_id` bigint unsigned NOT NULL COMMENT '场站编号',
  `station_code` varchar(64) DEFAULT NULL COMMENT '场站码快照',
  `station_name` varchar(100) DEFAULT NULL COMMENT '场站名称快照',
  `goods_config_id` bigint unsigned NOT NULL COMMENT '品类配置编号',
  `category_name` varchar(100) DEFAULT NULL COMMENT '品类名称快照',
  `unit` varchar(32) DEFAULT NULL COMMENT '计量单位快照',
  `expected_quantity` decimal(14,4) DEFAULT NULL COMMENT '预计数量（可空；界面一律以「约」标注）',
  `plate_no` varchar(32) DEFAULT NULL COMMENT '车牌号',
  `expected_arrival_time` datetime DEFAULT NULL COMMENT '预计到站时间',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态：0-待到站，1-已到场，2-未到场，9-已取消',
  `arrived_at` datetime DEFAULT NULL COMMENT '实际到场时间',
  `acquisition_id` bigint unsigned DEFAULT NULL COMMENT '到场后建的收购单编号（只作关联）',
  `cancelled_at` datetime DEFAULT NULL COMMENT '取消时间',
  `cancel_reason` varchar(255) DEFAULT NULL COMMENT '取消原因',
  `no_show_reason` varchar(255) DEFAULT NULL COMMENT '未到场说明',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `tenant_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '租户编号',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_appointment_no` (`appointment_no`),
  KEY `idx_appointment_natural_person` (`tenant_id`, `natural_person_id`, `status`),
  KEY `idx_appointment_payee` (`tenant_id`, `payee_id`, `status`),
  KEY `idx_appointment_station` (`tenant_id`, `station_id`, `status`),
  KEY `idx_appointment_arrival_time` (`tenant_id`, `expected_arrival_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='到站预约（不是订单）';
