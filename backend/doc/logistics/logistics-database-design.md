# 物流运输数据库设计文档 (V2.0)

## 1. 引言

本文档定义了"危废再生资源全流程追溯SaaS平台"中物流运输模块相关的数据库表结构、字段及关系。旨在为数据库设计和开发提供依据。

## 2. 模块职责定位

### 2.1 物流模块职责
- **核心职责**：管理运输任务的执行和跟踪
- **主要功能**：
  - 运输任务管理（接收、分配、执行）
  - 运输节点跟踪（装货、运输、卸货）
  - 车辆过磅操作（记录、推送）
  - 现金代付管理（记录、对账）
  - 车辆和司机管理

### 2.2 与危废模块的边界
- **物流模块负责**：运输执行、节点上报、过磅操作、现金代付
- **危废模块负责**：业务决策、金额计算、付款管理、订单分摊
- **接口协作**：
  - 物流模块接收危废模块的运输需求
  - 物流模块推送过磅数据给危废模块
  - 物流模块记录现金代付供危废模块查询

## 3. 核心表结构设计

### 3.1 运输任务表 (`logistics_transport_task`) - 核心表

```sql
CREATE TABLE `logistics_transport_task` (
  `id` bigint(20) NOT NULL COMMENT '任务ID',
  `task_no` varchar(64) NOT NULL COMMENT '任务单号',
  `order_id` bigint(20) NOT NULL COMMENT '关联的危废订单ID',
  `order_no` varchar(64) NOT NULL COMMENT '危废订单号',
  
  -- 任务分配
  `enterprise_id` bigint(20) DEFAULT NULL COMMENT '物流公司ID',
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
```

### 3.2 运输节点记录表 (`logistics_transport_node`)

```sql
CREATE TABLE `logistics_transport_node` (
  `id` bigint(20) NOT NULL COMMENT '节点ID',
  `task_id` bigint(20) NOT NULL COMMENT '运输任务ID',
  `node_type` tinyint(4) NOT NULL COMMENT '节点类型(1:接单,2:到达取货点,3:装货完成,4:开始运输,5:到达送货点,6:卸货完成)',
  `node_time` datetime NOT NULL COMMENT '节点时间',
  `node_location` varchar(500) DEFAULT '' COMMENT '节点位置',
  `latitude` decimal(10,7) DEFAULT NULL COMMENT '纬度',
  `longitude` decimal(10,7) DEFAULT NULL COMMENT '经度',
  `operator_id` bigint(20) NOT NULL COMMENT '操作人ID',
  `operator_name` varchar(64) NOT NULL COMMENT '操作人姓名',
  `photos` text COMMENT '现场照片URLs(JSON数组)',
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
```

### 3.3 车辆过磅记录表 (`logistics_vehicle_weighing`)

```sql
CREATE TABLE `logistics_vehicle_weighing` (
  `id` bigint(20) NOT NULL COMMENT '过磅记录ID',
  `weighing_no` varchar(64) NOT NULL COMMENT '过磅单号',
  `weighing_batch_no` varchar(64) NOT NULL COMMENT '过磅批次号（用于危废模块关联）',
  `vehicle_id` bigint(20) NOT NULL COMMENT '车辆ID',
  `vehicle_no` varchar(20) NOT NULL COMMENT '车牌号',
  `driver_id` bigint(20) NOT NULL COMMENT '司机ID',
  `driver_name` varchar(64) NOT NULL COMMENT '司机姓名',
  
  -- 过磅数据
  `weighing_time` datetime NOT NULL COMMENT '过磅时间',
  `gross_weight` decimal(10,2) NOT NULL COMMENT '毛重(吨)',
  `tare_weight` decimal(10,2) NOT NULL COMMENT '皮重(吨)',
  `net_weight` decimal(10,2) NOT NULL COMMENT '净重(吨)',
  
  -- 关联订单
  `related_order_ids` text COMMENT '关联的订单IDs(JSON数组)',
  `order_count` int(11) NOT NULL DEFAULT '0' COMMENT '订单数量',
  
  -- 操作信息
  `weighing_location` varchar(200) NOT NULL COMMENT '过磅地点',
  `operator_id` bigint(20) NOT NULL COMMENT '过磅员ID',
  `operator_name` varchar(64) NOT NULL COMMENT '过磅员姓名',
  `weighing_photos` text COMMENT '过磅单照片URLs(JSON数组)',
  
  -- 推送状态（推送给危废模块）
  `push_status` tinyint(4) NOT NULL DEFAULT '0' COMMENT '推送状态(0:待推送,1:已推送,2:推送失败)',
  `push_time` datetime DEFAULT NULL COMMENT '推送时间',
  `push_response` text COMMENT '推送响应',
  
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `tenant_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '租户ID',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_weighing_no` (`weighing_no`),
  UNIQUE KEY `uk_weighing_batch_no` (`weighing_batch_no`),
  KEY `idx_vehicle_id` (`vehicle_id`),
  KEY `idx_weighing_time` (`weighing_time`),
  KEY `idx_push_status` (`push_status`),
  KEY `idx_tenant_id_deleted` (`tenant_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='车辆过磅记录表';
```

### 3.4 司机现金代付记录表 (`logistics_cash_advance`)

```sql
CREATE TABLE `logistics_cash_advance` (
  `id` bigint(20) NOT NULL COMMENT '代付记录ID',
  `task_id` bigint(20) NOT NULL COMMENT '运输任务ID',
  `order_id` bigint(20) NOT NULL COMMENT '订单ID',
  `driver_id` bigint(20) NOT NULL COMMENT '司机ID',
  `driver_name` varchar(64) NOT NULL COMMENT '司机姓名',
  
  -- 支付信息
  `payment_amount` decimal(10,2) NOT NULL COMMENT '支付金额',
  `payment_time` datetime NOT NULL COMMENT '支付时间',
  `payment_location` varchar(500) DEFAULT '' COMMENT '支付地点',
  `payee_name` varchar(64) NOT NULL COMMENT '收款人姓名',
  `payee_phone` varchar(20) DEFAULT '' COMMENT '收款人电话',
  
  -- 凭证信息
  `payment_photos` text COMMENT '支付凭证照片URLs(JSON数组)',
  `receipt_photos` text COMMENT '收据照片URLs(JSON数组)',
  
  -- 通知状态（通知危废模块）
  `notify_status` tinyint(4) NOT NULL DEFAULT '0' COMMENT '通知状态(0:待通知,1:已通知,2:通知失败)',
  `notify_time` datetime DEFAULT NULL COMMENT '通知时间',
  
  -- 对账状态
  `reconcile_status` tinyint(4) NOT NULL DEFAULT '0' COMMENT '对账状态(0:待对账,1:已确认,2:有异议)',
  `reconcile_time` datetime DEFAULT NULL COMMENT '对账时间',
  `reconcile_remark` varchar(500) DEFAULT '' COMMENT '对账备注',
  
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `tenant_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '租户ID',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_task_id` (`task_id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_driver_id` (`driver_id`),
  KEY `idx_notify_status` (`notify_status`),
  KEY `idx_reconcile_status` (`reconcile_status`),
  KEY `idx_tenant_id_deleted` (`tenant_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='司机现金代付记录表';
```

### 3.5 车辆信息表 (`logistics_vehicle`)

```sql
CREATE TABLE `logistics_vehicle` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `enterprise_id` bigint NOT NULL COMMENT '所属企业ID（关联系统企业表）',
  `plate_number` varchar(20) NOT NULL COMMENT '车牌号',
  `vehicle_type` varchar(50) DEFAULT NULL COMMENT '车辆类型',
  `capacity_kg` decimal(10,2) DEFAULT NULL COMMENT '载重能力(kg)',
  `gps_device_id` varchar(100) DEFAULT NULL COMMENT 'GPS设备ID',
  `status` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '车辆状态(0:可用,1:运输中,2:维护中)',
  `license_expiry_date` date DEFAULT NULL COMMENT '行驶证到期日期',
  `insurance_expiry_date` date DEFAULT NULL COMMENT '保险到期日期',
  `maintenance_date` date DEFAULT NULL COMMENT '上次维护日期',
  `vehicle_photos` varchar(2000) DEFAULT NULL COMMENT '车辆照片URLs(JSON数组)',
  `license_photos` varchar(2000) DEFAULT NULL COMMENT '行驶证照片URLs(JSON数组)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_plate_number` (`plate_number`, `deleted`, `tenant_id`),
  KEY `idx_enterprise_id` (`enterprise_id`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='车辆信息表';
```

## 4. 枚举值定义

### 4.1 物流分配状态 (logistics_assignment.status)
- 0: 待接受 (PENDING_ACCEPTANCE)
- 1: 已接受 (ACCEPTED)
- 2: 前往取货 (EN_ROUTE_TO_PICKUP)
- 3: 装货中 (LOADING)
- 4: 前往送货 (EN_ROUTE_TO_DELIVERY)
- 5: 卸货中 (UNLOADING)
- 6: 已完成 (COMPLETED)
- 7: 已取消 (CANCELED)

### 4.2 物流类型 (logistics_assignment.logistics_type)
- 0: 自营 (SELF_OPERATED)
- 1: 第三方 (THIRD_PARTY)

### 4.3 车辆状态 (logistics_vehicle.status)
- 0: 可用 (AVAILABLE)
- 1: 运输中 (IN_TRANSIT)
- 2: 维护中 (MAINTENANCE)

### 4.4 司机状态 (logistics_driver.status)
- 0: 在职 (ACTIVE)
- 1: 离职 (INACTIVE)
- 2: 请假 (ON_LEAVE)

### 4.5 运输节点类型 (logistics_transport_node.node_type)
- 0: 接受任务 (ACCEPTED_TASK)
- 1: 前往取货 (EN_ROUTE_TO_PICKUP)
- 2: 到达取货点 (ARRIVED_PICKUP)
- 3: 装货完成 (LOAD_COMPLETED)
- 4: 前往送货 (EN_ROUTE_TO_DELIVERY)
- 5: 到达送货点 (ARRIVED_DELIVERY_SITE)
- 6: 开始卸货 (UNLOAD_STARTED)
- 7: 卸货完成 (UNLOAD_COMPLETED)
- 8: 到达回收站 (ARRIVED_RECYCLING_STATION)
- 9: 异常上报 (EXCEPTION_REPORTED)

### 4.6 在途量事件类型 (logistics_in_transit_volume_log.event_type)
- 0: 装货完成 (LOAD_COMPLETED)
- 1: 开始卸货 (UNLOAD_STARTED)
- 2: 定时上报 (PERIODIC_REPORT)

### 4.7 现金代付状态 (logistics_cash_payment_record.status)
- 0: 已记录 (RECORDED)
- 1: 回收企业已确认 (CONFIRMED_BY_RECYCLER)
- 2: 已结算 (SETTLED)

## 5. 注意事项

1. **表命名规范**: 所有表名采用 `logistics_` 前缀，使用小写字母和下划线命名法
2. **字段命名规范**: 所有字段名使用小写字母和下划线命名法，避免使用驼峰命名
3. **必备字段**: 每个表都包含标准的创建时间、更新时间、创建者、更新者、删除标志字段
4. **字段类型规范**: 
   - 主键使用 `bigint unsigned` 类型
   - 状态字段使用 `tinyint unsigned` 类型
   - 金额字段使用 `decimal(10,2)` 类型
   - 时间字段使用 `datetime` 类型
   - 文本字段根据长度选择 `varchar` 或 `text` 类型
5. **索引规范**: 
   - 主键索引必须有
   - 唯一约束使用 `uk_` 前缀
   - 普通索引使用 `idx_` 前缀
   - 外键字段、状态字段、时间字段都应添加索引
6. **枚举值**: 所有状态字段使用数字枚举，在注释中说明各个值的含义
7. **字符集**: 统一使用 `utf8mb4` 字符集和 `utf8mb4_unicode_ci` 排序规则
8. **存储引擎**: 统一使用 InnoDB 存储引擎
9. **外键关联**: 
   - `enterprise_id`