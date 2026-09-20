-- 创建危废转移预约表
CREATE TABLE IF NOT EXISTS `waste_transfer_appointment` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '预约ID',
  `appointment_no` varchar(64) NOT NULL COMMENT '预约单号',
  
  -- 产废企业信息
  `producer_enterprise_id` bigint(20) NOT NULL COMMENT '产废企业ID',
  `producer_enterprise_name` varchar(100) DEFAULT NULL COMMENT '产废企业名称',
  `producer_contact_name` varchar(64) NOT NULL COMMENT '产废企业联系人',
  `producer_contact_phone` varchar(20) NOT NULL COMMENT '产废企业联系电话',
  
  -- 回收企业信息
  `recycler_enterprise_id` bigint(20) DEFAULT NULL COMMENT '回收企业ID',
  `recycler_enterprise_name` varchar(100) DEFAULT NULL COMMENT '回收企业名称',
  `assignment_type` tinyint(4) NOT NULL DEFAULT '0' COMMENT '分配方式(0:自动分配,1:手动指定)',
  `assignment_time` datetime DEFAULT NULL COMMENT '分配时间',
  `assignment_operator` varchar(64) DEFAULT NULL COMMENT '分配操作人',
  
  -- 废物信息
  `waste_code` varchar(50) NOT NULL COMMENT '危险废物代码',
  `waste_name` varchar(100) NOT NULL COMMENT '危险废物名称',
  `waste_category` varchar(50) NOT NULL COMMENT '废物类别',
  `estimated_quantity` decimal(10,2) NOT NULL COMMENT '预估数量',
  `quantity_unit` varchar(10) NOT NULL COMMENT '数量单位',
  `waste_description` varchar(500) DEFAULT '' COMMENT '废物描述',
  
  -- 地址信息
  `pickup_address` varchar(500) NOT NULL COMMENT '取货地址',
  `pickup_latitude` decimal(10,7) DEFAULT NULL COMMENT '取货地址纬度',
  `pickup_longitude` decimal(10,7) DEFAULT NULL COMMENT '取货地址经度',
  `delivery_address` varchar(500) DEFAULT NULL COMMENT '送货地址',
  `delivery_latitude` decimal(10,7) DEFAULT NULL COMMENT '送货地址纬度',
  `delivery_longitude` decimal(10,7) DEFAULT NULL COMMENT '送货地址经度',
  
  -- 时间信息
  `expected_pickup_time` datetime NOT NULL COMMENT '期望取货时间',
  `expected_delivery_time` datetime DEFAULT NULL COMMENT '期望送达时间',
  
  -- 状态信息
  `appointment_status` tinyint(4) NOT NULL DEFAULT '0' COMMENT '预约状态(0:待处理,1:待回收方确认,2:已确认,3:已拒绝,4:已生成订单,5:已取消)',
  `confirm_time` datetime DEFAULT NULL COMMENT '确认时间',
  `reject_time` datetime DEFAULT NULL COMMENT '拒绝时间',
  `reject_reason` varchar(500) DEFAULT '' COMMENT '拒绝原因',
  `cancel_time` datetime DEFAULT NULL COMMENT '取消时间',
  `cancel_reason` varchar(500) DEFAULT '' COMMENT '取消原因',
  
  -- 业务信息
  `business_mode` tinyint(4) NOT NULL DEFAULT '0' COMMENT '业务模式(0:独立运营,1:平台竞价,2:混合模式)',
  `is_urgent` bit(1) DEFAULT FALSE COMMENT '是否紧急',
  `priority_level` SMALLINT DEFAULT 0 COMMENT '优先级(0-100)',
  
  -- 关联信息
  `order_id` bigint(20) DEFAULT NULL COMMENT '关联订单ID',
  `contract_id` bigint(20) DEFAULT NULL COMMENT '关联合同ID',
  
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `tenant_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '租户ID',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT FALSE COMMENT '是否删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='危废转移预约表'; 