-- 连接字符集：文件里有中文注释。客户端默认不是 utf8mb4 时（例如没有 LANG 的
-- `docker exec -i <mysql容器> mysql`，其默认是 latin1），中文会被双重编码存进库。
SET NAMES utf8mb4;

-- ========================================
-- 物流域：车辆与司机的最小档案（V2a #77，见 ADR 0032 与 CONTEXT.md「物流与运力」）
--
-- 物流是独立的业务模块：不管有没有工行都能独立存在，因此这两张表不引用任何 icbc 表，
-- 也不承载工行语义（ADR 0032）。
--
--   1. logistics_vehicle  车辆（车牌、类型、载重、状态）
--   2. logistics_driver   司机（关联租户内系统用户、姓名、手机号、来源、状态）
--
-- 两张都是租户表（带 tenant_id），不进 yudao.tenant.ignore-tables。
--
-- **唯一性为什么不在 DB 层约束**：车牌与司机关联的用户都要求「租户内唯一」，但两张表都是
-- 逻辑删除（deleted）。若建 UNIQUE KEY (tenant_id, plate_no)，被软删的行仍占着键值，
-- 结果是「删掉的车牌再也建不回来」；若把 deleted 也放进唯一键，则同车牌只能存在一行删除记录。
-- 两者都比「建档这个低频人工动作偶发并发重复」更难接受，因此唯一性由 Service 校验
-- （见 LogisticsVehicleServiceImpl#assertPlateNoAvailable / LogisticsDriverServiceImpl#assertUserIdAvailable，
-- 这两个校验也带错误码与单测）。建档是低频人工动作，并发冲突的实际风险远小于上述两个陷阱。
--
-- 幂等：CREATE TABLE IF NOT EXISTS。菜单在 logistics-menu.sql 里幂等维护。
-- ========================================

CREATE TABLE IF NOT EXISTS `logistics_vehicle` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `plate_no` varchar(32) NOT NULL COMMENT '车牌号（租户内唯一，对外标识）',
  `vehicle_type` varchar(64) DEFAULT NULL COMMENT '车辆类型（厢式货车 / 平板 / 自卸等）',
  `capacity_ton` decimal(10,3) DEFAULT NULL COMMENT '载重能力（吨）',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '车辆状态：0-可用，1-运输中（由运输任务驱动，不接受手工设置），2-维护中',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `tenant_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '租户编号',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_vehicle_tenant_plate` (`tenant_id`, `plate_no`),
  KEY `idx_vehicle_tenant_status` (`tenant_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='物流 - 车辆档案';

CREATE TABLE IF NOT EXISTS `logistics_driver` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint unsigned NOT NULL COMMENT '关联的租户内系统用户编号（司机登录司机端用，租户内唯一）',
  `name` varchar(64) NOT NULL COMMENT '司机姓名',
  `mobile` varchar(32) DEFAULT NULL COMMENT '手机号',
  `source` tinyint NOT NULL DEFAULT '1' COMMENT '司机来源：1-自有，2-承运商',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '司机状态：0-在职，1-离职，2-请假',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `tenant_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '租户编号',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_driver_tenant_user` (`tenant_id`, `user_id`),
  KEY `idx_driver_tenant_status` (`tenant_id`, `status`),
  KEY `idx_driver_tenant_source` (`tenant_id`, `source`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='物流 - 司机档案';
