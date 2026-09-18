# 物流运输模块数据库说明文档

## 1. 概述

本目录包含"危废再生资源全流程追溯SaaS平台"中物流运输模块的数据库脚本文件。脚本分为多个部分，可单独执行，也可通过主脚本一次性执行所有SQL语句。

## 2. 文件说明

| 文件名 | 描述 |
|---------|---------|
| `logistics-all-tables.sql` | 主脚本，包含所有表创建和基础数据插入 |
| `logistics-schema-part1.sql` | 核心业务表：运输任务、运输节点 |
| `logistics-schema-part2.sql` | 过磅和代付表：车辆过磅、现金代付 |
| `logistics-schema-part3.sql` | 基础数据表：车辆、司机资质 |
| `logistics-schema-part4.sql` | 扩展功能表：在途量、临时订单、统计数据 |

## 3. 执行说明

### 3.1 一次性执行所有脚本

```bash
mysql -u用户名 -p密码 数据库名 < logistics-all-tables.sql
```

### 3.2 分步执行各部分脚本

```bash
# 1. 先创建核心业务表
mysql -u用户名 -p密码 数据库名 < logistics-schema-part1.sql

# 2. 创建过磅和代付表
mysql -u用户名 -p密码 数据库名 < logistics-schema-part2.sql

# 3. 创建基础数据表
mysql -u用户名 -p密码 数据库名 < logistics-schema-part3.sql

# 4. 创建扩展功能表
mysql -u用户名 -p密码 数据库名 < logistics-schema-part4.sql
```

## 4. 表结构说明

### 4.1 核心业务表

| 表名 | 描述 | 主要字段 |
|------|------|----------|
| `logistics_transport_task` | 运输任务表 | 任务ID、任务单号、订单信息、取货信息、送货信息、货物信息、任务状态 |
| `logistics_transport_node` | 运输节点记录表 | 节点ID、任务ID、节点类型、节点时间、节点位置、操作人信息、照片 |

### 4.2 过磅和代付表

| 表名 | 描述 | 主要字段 |
|------|------|----------|
| `logistics_vehicle_weighing` | 车辆过磅记录表 | 过磅ID、过磅单号、车辆信息、过磅数据、关联订单、推送状态 |
| `logistics_cash_advance` | 司机现金代付记录表 | 代付ID、任务ID、司机信息、支付信息、凭证信息、对账状态 |

### 4.3 基础数据表

| 表名 | 描述 | 主要字段 |
|------|------|----------|
| `logistics_vehicle` | 车辆信息表 | 车辆ID、企业ID、车牌号、车辆类型、载重能力、状态 |
| `logistics_driver_qualification` | 司机资质信息表 | 用户ID、企业ID、驾驶证信息、从业资格证信息、危险品运输资质 |

### 4.4 扩展功能表

| 表名 | 描述 | 主要字段 |
|------|------|----------|
| `logistics_in_transit_volume` | 在途量监控记录表 | 记录ID、车辆信息、司机信息、当前在途量、关联任务、预警信息 |
| `logistics_in_transit_volume_log` | 在途量变更日志表 | 日志ID、在途量记录ID、事件类型、事件时间、变更数据 |
| `logistics_temporary_order` | 临时订单表 | 订单ID、订单号、任务ID、废物信息、产废方信息、支付信息 |
| `logistics_transport_statistics` | 运输统计表 | 统计ID、统计日期、统计类型、任务统计、运输量统计、效率统计 |

## 5. 数据库设计规范

1. **表命名规范**：所有表名采用 `logistics_` 前缀，使用小写字母和下划线命名法
2. **字段命名规范**：所有字段名使用小写字母和下划线命名法，避免使用驼峰命名
3. **必备字段**：每个表都包含标准的创建时间、更新时间、创建者、更新者、删除标志字段
4. **字段类型规范**：
   - 主键使用 `bigint unsigned` 类型
   - 状态字段使用 `tinyint unsigned` 类型
   - 金额字段使用 `decimal(10,2)` 类型
   - 时间字段使用 `datetime` 类型
   - 文本字段根据长度选择 `varchar` 或 `text` 类型
5. **索引规范**：
   - 主键索引必须有
   - 唯一约束使用 `uk_` 前缀
   - 普通索引使用 `idx_` 前缀
   - 外键字段、状态字段、时间字段都应添加索引
6. **字符集**：统一使用 `utf8mb4` 字符集和 `utf8mb4_unicode_ci` 排序规则
7. **存储引擎**：统一使用 InnoDB 存储引擎

## 6. 与系统其他模块的关联

1. **企业关联**：物流模块不再维护独立的物流公司表，而是直接关联系统现有的企业表
2. **用户关联**：司机信息不再独立维护基本信息，而是通过 `logistics_driver_qualification` 表扩展系统用户表，仅存储驾驶相关资质信息
3. **实名认证**：司机的身份信息通过系统现有的实名认证功能维护，无需在物流模块重复存储 