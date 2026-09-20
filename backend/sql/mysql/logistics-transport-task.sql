-- 连接字符集：文件里有中文注释。客户端默认不是 utf8mb4 时（例如没有 LANG 的
-- `docker exec -i <mysql容器> mysql`，其默认是 latin1），中文会被双重编码存进库。
SET NAMES utf8mb4;

-- ========================================
-- 物流域：运输任务与运输节点（V2b #78，见 ADR 0032 与 CONTEXT.md「物流与运力」）
--
--   1. logistics_transport_task  运输任务：一车 + 一司机 + 一次执行（一个出发地、一个提货点、一个时间窗）
--   2. logistics_transport_node  运输节点：过程事实与货物流凭证（两个时间 + 位置 + 照片）
--
-- 一期「一个任务 = 一次执行」（ADR 0032 第 5 条），**不建独立「车次」实体**；多停靠点集货归 V5（#72）。
-- 两张都是租户表（带 tenant_id），不进 yudao.tenant.ignore-tables。
--
-- **唯一键这回要建**，与车辆车牌的处理正好相反，原因值得写清：
--   - 车牌（logistics_vehicle）不建唯一键：它是**人会重复使用的业务值**，逻辑删除的行仍占着键值，
--     建了就「删掉的车牌再也建不回来」；
--   - 客户端请求号（本表）要建唯一键：它是**每次提交新生成的 UUID**，不存在被复用的可能，
--     建唯一键正是弱网重复补传的并发兜底（Service 预检之外的最后一层）。
--
-- 任务可挂采购安排（purchase_order_id + 单号快照，都可空）；什么都不挂也能派车——司机直接上门收购
-- 是常态（与「直接收购」同一逻辑，ADR 0027）。
--
-- 幂等：CREATE TABLE IF NOT EXISTS。菜单在 logistics-menu.sql 里幂等维护。
-- ========================================

CREATE TABLE IF NOT EXISTS `logistics_transport_task` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `task_no` varchar(64) NOT NULL COMMENT '运输任务单号（对外可见）',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态：0-待分配，1-已分配，2-已接单，3-执行中，4-已完成，5-已取消',
  `vehicle_id` bigint unsigned DEFAULT NULL COMMENT '车辆编号（派车后非空）',
  `plate_no` varchar(32) DEFAULT NULL COMMENT '车牌号快照（档案改名/删档不影响历史单据）',
  `driver_id` bigint unsigned DEFAULT NULL COMMENT '司机编号（派车后非空）',
  `driver_name` varchar(64) DEFAULT NULL COMMENT '司机姓名快照',
  `driver_mobile` varchar(32) DEFAULT NULL COMMENT '司机手机号快照',
  `departure_address` varchar(255) DEFAULT NULL COMMENT '出发地',
  `pickup_address` varchar(255) NOT NULL COMMENT '提货点地址',
  `pickup_contact_name` varchar(64) DEFAULT NULL COMMENT '提货点联系人',
  `pickup_contact_phone` varchar(32) DEFAULT NULL COMMENT '提货点联系电话',
  `expected_start_time` datetime DEFAULT NULL COMMENT '时间窗开始',
  `expected_end_time` datetime DEFAULT NULL COMMENT '时间窗结束',
  `purchase_order_id` bigint unsigned DEFAULT NULL COMMENT '关联采购订单编号（可空）',
  `purchase_order_no` varchar(64) DEFAULT NULL COMMENT '采购订单号快照',
  `assign_time` datetime DEFAULT NULL COMMENT '派车时间',
  `accept_time` datetime DEFAULT NULL COMMENT '接单时间',
  `start_time` datetime DEFAULT NULL COMMENT '起运时间（上报起运节点时落，取节点发生时间）',
  `complete_time` datetime DEFAULT NULL COMMENT '完成时间',
  `cancel_time` datetime DEFAULT NULL COMMENT '取消时间',
  `cancel_reason` varchar(500) DEFAULT NULL COMMENT '取消原因（取消必填）',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `tenant_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '租户编号',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_task_tenant_no` (`tenant_id`, `task_no`),
  KEY `idx_task_tenant_status` (`tenant_id`, `status`),
  KEY `idx_task_tenant_vehicle` (`tenant_id`, `vehicle_id`),
  KEY `idx_task_tenant_driver` (`tenant_id`, `driver_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='物流 - 运输任务';

CREATE TABLE IF NOT EXISTS `logistics_transport_node` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `task_id` bigint unsigned NOT NULL COMMENT '运输任务编号',
  `task_no` varchar(64) NOT NULL COMMENT '运输任务单号（冗余，便于按单号查询与展示）',
  `node_type` tinyint NOT NULL COMMENT '节点类型：1-到达提货点，2-交接完成，3-起运，4-到达场站，5-卸货完成',
  `node_time` datetime NOT NULL COMMENT '发生时间：事情实际发生的时刻（补录时可能早于上报时间）',
  `report_time` datetime NOT NULL COMMENT '上报时间：客户端提交上来的时刻',
  `location` varchar(500) DEFAULT NULL COMMENT '位置描述',
  `latitude` decimal(10,7) DEFAULT NULL COMMENT '纬度',
  `longitude` decimal(10,7) DEFAULT NULL COMMENT '经度',
  `photos` text COMMENT '凭证照片 URL 列表（JSON 数组文本）',
  `operator_id` bigint unsigned DEFAULT NULL COMMENT '上报人编号（司机或代录的调度）',
  `operator_name` varchar(64) DEFAULT NULL COMMENT '上报人姓名快照',
  `client_request_id` varchar(64) NOT NULL COMMENT '客户端请求号（幂等键：弱网重复补传只落一条）',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `tenant_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '租户编号',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_node_client_request` (`tenant_id`, `client_request_id`),
  KEY `idx_node_tenant_task` (`tenant_id`, `task_id`),
  KEY `idx_node_tenant_task_no` (`tenant_id`, `task_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='物流 - 运输节点（过程事实与货物流凭证）';
