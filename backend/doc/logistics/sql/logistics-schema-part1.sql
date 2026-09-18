-- 物流运输模块数据库表结构脚本（第一部分）
-- 创建日期：2024年05月

-- 创建物流运输任务表
CREATE TABLE IF NOT EXISTS `logistics_transport_task` (
  `id` bigint(20) NOT NULL COMMENT '任务ID',
  `task_no` varchar(64) NOT NULL COMMENT '任务单号',
  `order_id` bigint(20) NOT NULL COMMENT '关联的危废订单ID',
  `order_no` varchar(64) NOT NULL COMMENT '危废订单号',
  
  -- 任务分配
  `logistics_company_id` bigint(20) DEFAULT NULL COMMENT '物流公司ID',
  `vehicle_id` bigint(20) DEFAULT NULL COMMENT '分配的车辆ID',
  `driver_id` bigint(20) DEFAULT NULL COMMENT '分配的司机ID',
  `assign_time` datetime DEFAULT NULL COMMENT '分配时间',
  `accept_time` datetime DEFAULT NULL COMMENT '接单时间',
  
  -- 取货信息
  `pickup_enterprise_id` bigint(20) NOT NULL COMMENT '取货企业ID',
  `pickup_address` varchar(500) NOT NULL COMMENT '取货地址',
  `pickup_contact_name` varchar(64) NOT NULL COMMENT '取货联系人',
  `pickup_contact_phone` varchar(20) NOT NULL COMMENT '取货联系电话',
  `expected_pickup_time` datetime NOT NULL COMMENT '预计取货时间',
  `actual_pickup_time` datetime DEFAULT NULL COMMENT '实际取货时间',
  
  -- 送货信息
  `delivery_enterprise_id` bigint(20) NOT NULL COMMENT '送货企业ID',
  `delivery_address` varchar(500) NOT NULL COMMENT '送货地址',
  `delivery_contact_name` varchar(64) NOT NULL COMMENT '送货联系人',
  `delivery_contact_phone` varchar(20) NOT NULL COMMENT '送货联系电话',
  `expected_delivery_time` datetime NOT NULL COMMENT '预计送达时间',
  `actual_delivery_time` datetime DEFAULT NULL COMMENT '实际送达时间',
  
  -- 货物信息
  `waste_code` varchar(50) NOT NULL COMMENT '危险废物代码',
  `waste_name` varchar(100) NOT NULL COMMENT '危险废物名称',
  `estimated_quantity` decimal(10,2) NOT NULL COMMENT '预估数量',
  `actual_quantity` decimal(10,2) DEFAULT NULL COMMENT '实际装货数量',
  `quantity_unit` varchar(10) NOT NULL COMMENT '数量单位',
  
  -- 任务状态
  `task_status` tinyint(4) NOT NULL DEFAULT '0' COMMENT '任务状态(0:待分配,1:待接单,2:待取货,3:运输中,4:已送达,5:已完成,6:已取消)',
  `current_location` varchar(500) DEFAULT '' COMMENT '当前位置',
  `current_latitude` decimal(10,7) DEFAULT NULL COMMENT '当前纬度',
  `current_longitude` decimal(10,7) DEFAULT NULL COMMENT '当前经度',
  
  -- 异常信息
  `is_abnormal` bit(1) DEFAULT b'0' COMMENT '是否异常',
  `abnormal_type` tinyint(4) DEFAULT NULL COMMENT '异常类型',
  `abnormal_reason` varchar(500) DEFAULT '' COMMENT '异常原因',
  `is_temporary` bit(1) DEFAULT b'0' COMMENT '是否临时订单(扫街回收)',
  
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `tenant_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '租户ID',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_task_no` (`task_no`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_vehicle_id` (`vehicle_id`),
  KEY `idx_driver_id` (`driver_id`),
  KEY `idx_task_status` (`task_status`),
  KEY `idx_tenant_id_deleted` (`tenant_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物流运输任务表';

-- 创建运输节点记录表
CREATE TABLE IF NOT EXISTS `logistics_transport_node` (
  `id` bigint(20) NOT NULL COMMENT '节点ID',
  `task_id` bigint(20) NOT NULL COMMENT '运输任务ID',
  `node_type` tinyint(4) NOT NULL COMMENT '节点类型(0:接受任务,1:前往取货,2:到达取货点,3:装货完成,4:前往送货,5:到达送货点,6:开始卸货,7:卸货完成,8:到达回收站,9:异常上报)',
  `node_time` datetime NOT NULL COMMENT '节点时间',
  `node_location` varchar(500) DEFAULT '' COMMENT '节点位置',
  `latitude` decimal(10,7) DEFAULT NULL COMMENT '纬度',
  `longitude` decimal(10,7) DEFAULT NULL COMMENT '经度',
  `operator_id` bigint(20) NOT NULL COMMENT '操作人ID',
  `operator_name` varchar(64) NOT NULL COMMENT '操作人姓名',
  `photos` text COMMENT '现场照片URLs(JSON数组)',
  `additional_data` text COMMENT '附加数据(JSON对象)',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `tenant_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '租户ID',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_task_id` (`task_id`),
  KEY `idx_node_type` (`node_type`),
  KEY `idx_node_time` (`node_time`),
  KEY `idx_tenant_id_deleted` (`tenant_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='运输节点记录表'; 