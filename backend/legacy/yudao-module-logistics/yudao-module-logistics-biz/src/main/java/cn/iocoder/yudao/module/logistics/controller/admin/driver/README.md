# 司机资质信息管理

## 功能概述

司机资质信息管理模块是物流运输系统的重要组成部分，负责管理司机的基本信息、驾驶资质、从业资格等信息。该模块通过扩展系统用户表的方式，为司机用户提供专业的资质信息管理功能。

## 核心特性

### 1. 司机资质管理
- **基本信息**：司机编号、所属企业、联系方式等
- **驾驶资质**：驾驶证号码、驾驶证类型、到期日期、准驾车型等
- **从业资格**：从业资格证号码、危险品运输资质证等
- **工作状态**：在职、离职、请假等状态管理
- **培训记录**：最近培训日期、下次培训日期等

### 2. 证件照片管理
- 驾驶证照片上传和存储
- 从业资格证照片管理
- 危险品运输资质证照片管理
- 支持多张照片存储（JSON数组格式）

### 3. 数据校验
- 司机编号唯一性校验
- 驾驶证号码唯一性校验
- 用户关联性校验
- 必填字段验证

### 4. 状态管理
- 司机工作状态实时更新
- 证件到期提醒（待实现）
- 培训计划管理（待实现）

## API 接口

### 基础CRUD操作

| 接口 | 方法 | 路径 | 描述 |
|------|------|------|------|
| 创建司机资质信息 | POST | `/logistics/driver-qualification/create` | 新增司机资质信息 |
| 更新司机资质信息 | PUT | `/logistics/driver-qualification/update` | 修改司机资质信息 |
| 删除司机资质信息 | DELETE | `/logistics/driver-qualification/delete` | 删除司机资质信息 |
| 获取司机资质信息 | GET | `/logistics/driver-qualification/get` | 查询单个司机资质信息 |
| 分页查询司机资质信息 | GET | `/logistics/driver-qualification/page` | 分页查询司机资质信息列表 |

### 业务查询接口

| 接口 | 方法 | 路径 | 描述 |
|------|------|------|------|
| 根据企业ID查询 | GET | `/logistics/driver-qualification/list-by-enterprise` | 查询指定企业的司机列表 |
| 根据状态查询 | GET | `/logistics/driver-qualification/list-by-status` | 查询指定状态的司机列表 |
| 根据用户ID查询 | GET | `/logistics/driver-qualification/get-by-user-id` | 根据用户ID查询司机资质信息 |
| 根据司机编号查询 | GET | `/logistics/driver-qualification/get-by-driver-code` | 根据司机编号查询司机资质信息 |
| 根据驾驶证号查询 | GET | `/logistics/driver-qualification/get-by-driving-license-no` | 根据驾驶证号码查询司机资质信息 |

### 状态管理接口

| 接口 | 方法 | 路径 | 描述 |
|------|------|------|------|
| 更新司机状态 | PUT | `/logistics/driver-qualification/update-status` | 更新司机工作状态 |

### 数据导出接口

| 接口 | 方法 | 路径 | 描述 |
|------|------|------|------|
| 导出Excel | GET | `/logistics/driver-qualification/export-excel` | 导出司机资质信息Excel文件 |

## 数据模型

### 司机状态枚举
```java
public enum DriverStatusEnum {
    ACTIVE(0, "在职"),
    INACTIVE(1, "离职"),
    ON_LEAVE(2, "请假");
}
```

### 主要字段说明

| 字段名 | 类型 | 必填 | 描述 |
|--------|------|------|------|
| userId | Long | 是 | 关联的系统用户ID |
| enterpriseId | Long | 是 | 所属企业ID |
| driverCode | String | 是 | 司机编号（唯一） |
| drivingLicenseNo | String | 是 | 驾驶证号码（唯一） |
| drivingLicenseType | String | 是 | 驾驶证类型（如C1、B2等） |
| drivingLicenseExpiryDate | LocalDate | 是 | 驾驶证到期日期 |
| qualificationCertNo | String | 否 | 从业资格证号码 |
| hazardousTransportCertNo | String | 否 | 危险品运输资质证号 |
| vehicleTypePermitted | String | 否 | 准驾车型 |
| yearsOfExperience | Integer | 否 | 驾龄（年） |
| status | Integer | 是 | 司机状态 |

## 权限控制

所有接口都需要相应的权限验证：
- `logistics:driver:create` - 创建司机资质信息权限
- `logistics:driver:update` - 更新司机资质信息权限
- `logistics:driver:delete` - 删除司机资质信息权限
- `logistics:driver:query` - 查询司机资质信息权限
- `logistics:driver:export` - 导出司机资质信息权限

## 使用示例

### 创建司机资质信息
```json
POST /logistics/driver-qualification/create
{
  "userId": 1024,
  "enterpriseId": 100,
  "driverCode": "D001",
  "drivingLicenseNo": "310101199001010001",
  "drivingLicenseType": "C1",
  "drivingLicenseExpiryDate": "2030-12-31",
  "qualificationCertNo": "QC001",
  "hazardousTransportCertNo": "HZ001",
  "vehicleTypePermitted": "C1,B2",
  "yearsOfExperience": 5,
  "status": 0,
  "joinDate": "2024-01-01",
  "driverLicensePhotos": ["http://example.com/license1.jpg"],
  "qualificationCertPhotos": ["http://example.com/cert1.jpg"],
  "remark": "经验丰富的司机"
}
```

### 查询司机资质信息
```json
GET /logistics/driver-qualification/get?id=1024

Response:
{
  "code": 200,
  "data": {
    "id": 1024,
    "userId": 1024,
    "userName": "张三",
    "userMobile": "13800138000",
    "enterpriseId": 100,
    "enterpriseName": "XX物流公司",
    "driverCode": "D001",
    "drivingLicenseNo": "310101199001010001",
    "drivingLicenseType": "C1",
    "drivingLicenseExpiryDate": "2030-12-31",
    "status": 0,
    "createTime": "2024-01-01 00:00:00"
  },
  "msg": "操作成功"
}
```

## 注意事项

1. **用户关联**：司机资质信息必须关联到系统中已存在的用户
2. **唯一性约束**：司机编号和驾驶证号码在同一租户下必须唯一
3. **照片存储**：照片URL以JSON数组格式存储，支持多张照片
4. **状态管理**：司机状态变更会影响任务分配和调度
5. **数据完整性**：删除司机资质信息前需要确保没有关联的运输任务

## 扩展功能（待实现）

1. **证件到期提醒**：自动检测证件到期并发送提醒
2. **培训计划管理**：管理司机培训计划和记录
3. **绩效评估**：基于运输任务完成情况进行绩效评估
4. **违章记录**：记录和管理司机违章信息
5. **健康档案**：管理司机健康检查记录 