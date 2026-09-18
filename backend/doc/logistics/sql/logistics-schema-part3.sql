-- 物流运输模块数据库表结构脚本（第三部分）
-- 创建日期：2024年05月
-- 更新日期：2024年05月

-- 创建车辆信息表
CREATE TABLE IF NOT EXISTS `logistics_vehicle` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `enterprise_id` bigint unsigned NOT NULL COMMENT '所属企业ID（关联系统企业表）',
  `plate_number` varchar(20) NOT NULL COMMENT '车牌号',
  `vehicle_type` varchar(50) DEFAULT NULL COMMENT '车辆类型',
  `capacity_kg` decimal(10,2) DEFAULT NULL COMMENT '载重能力(kg)',
  `gps_device_id` varchar(100) DEFAULT NULL COMMENT 'GPS设备ID',
  `status` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '车辆状态(0:可用,1:运输中,2:维护中)',
  `license_expiry_date` date DEFAULT NULL COMMENT '行驶证到期日期',
  `insurance_expiry_date` date DEFAULT NULL COMMENT '保险到期日期',
  `maintenance_date` date DEFAULT NULL COMMENT '上次维护日期',
  `vehicle_photos` text COMMENT '车辆照片URLs(JSON数组)',
  `license_photos` text COMMENT '行驶证照片URLs(JSON数组)',
  `tenant_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '租户ID',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_plate_number` (`plate_number`),
  KEY `idx_enterprise_id` (`enterprise_id`),
  KEY `idx_status` (`status`),
  KEY `idx_tenant_id_deleted` (`tenant_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='车辆信息表';

-- 创建司机资质信息表（作为用户表的扩展）
CREATE TABLE IF NOT EXISTS `logistics_driver_qualification` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint unsigned NOT NULL COMMENT '关联的用户ID',
  `enterprise_id` bigint unsigned NOT NULL COMMENT '所属企业ID（关联系统企业表）',
  `driver_code` varchar(32) NOT NULL COMMENT '司机编号',
  `driving_license_no` varchar(50) NOT NULL COMMENT '驾驶证号码',
  `driving_license_type` varchar(20) NOT NULL COMMENT '驾驶证类型',
  `driving_license_expiry_date` date NOT NULL COMMENT '驾驶证到期日期',
  `qualification_cert_no` varchar(50) DEFAULT NULL COMMENT '从业资格证号码',
  `qualification_cert_expiry_date` date DEFAULT NULL COMMENT '从业资格证到期日期',
  `hazardous_transport_cert_no` varchar(50) DEFAULT NULL COMMENT '危险品运输资质证号',
  `hazardous_transport_cert_expiry_date` date DEFAULT NULL COMMENT '危险品运输资质证到期日期',
  `vehicle_type_permitted` varchar(100) DEFAULT NULL COMMENT '准驾车型',
  `years_of_experience` int DEFAULT NULL COMMENT '驾龄(年)',
  `status` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '司机状态(0:在职,1:离职,2:请假)',
  `join_date` date DEFAULT NULL COMMENT '入职日期',
  `leave_date` date DEFAULT NULL COMMENT '离职日期',
  `driver_license_photos` text COMMENT '驾驶证照片URLs(JSON数组)',
  `qualification_cert_photos` text COMMENT '从业资格证照片URLs(JSON数组)',
  `hazardous_cert_photos` text COMMENT '危险品运输资质证照片URLs(JSON数组)',
  `last_training_date` date DEFAULT NULL COMMENT '最近一次培训日期',
  `next_training_date` date DEFAULT NULL COMMENT '下次培训日期',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `tenant_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '租户ID',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`),
  UNIQUE KEY `uk_driver_code` (`driver_code`),
  KEY `idx_enterprise_id` (`enterprise_id`),
  KEY `idx_driving_license_no` (`driving_license_no`),
  KEY `idx_status` (`status`),
  KEY `idx_tenant_id_deleted` (`tenant_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='司机资质信息表'; 