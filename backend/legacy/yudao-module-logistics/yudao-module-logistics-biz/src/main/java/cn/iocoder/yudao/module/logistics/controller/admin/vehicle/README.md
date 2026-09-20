# 车辆信息管理 API 文档

## 概述

车辆信息管理模块提供了完整的车辆生命周期管理功能，包括车辆注册、信息维护、状态管理等。

## API 接口列表

### 1. 创建车辆信息
- **接口地址**: `POST /logistics/vehicle/create`
- **功能描述**: 创建新的车辆信息
- **权限要求**: `logistics:vehicle:create`
- **请求参数**: `VehicleCreateReqVO`
- **响应结果**: 返回车辆ID

### 2. 更新车辆信息
- **接口地址**: `PUT /logistics/vehicle/update`
- **功能描述**: 更新车辆信息
- **权限要求**: `logistics:vehicle:update`
- **请求参数**: `VehicleUpdateReqVO`
- **响应结果**: 操作成功标识

### 3. 删除车辆信息
- **接口地址**: `DELETE /logistics/vehicle/delete`
- **功能描述**: 删除车辆信息（运输中的车辆不能删除）
- **权限要求**: `logistics:vehicle:delete`
- **请求参数**: `id` (车辆ID)
- **响应结果**: 操作成功标识

### 4. 获取车辆详情
- **接口地址**: `GET /logistics/vehicle/get`
- **功能描述**: 根据ID获取车辆详细信息
- **权限要求**: `logistics:vehicle:query`
- **请求参数**: `id` (车辆ID)
- **响应结果**: `VehicleRespVO`

### 5. 车辆信息分页查询
- **接口地址**: `GET /logistics/vehicle/page`
- **功能描述**: 分页查询车辆信息列表
- **权限要求**: `logistics:vehicle:query`
- **请求参数**: `VehiclePageReqVO`
- **响应结果**: `PageResult<VehicleRespVO>`

### 6. 导出车辆信息Excel
- **接口地址**: `GET /logistics/vehicle/export-excel`
- **功能描述**: 导出车辆信息到Excel文件
- **权限要求**: `logistics:vehicle:export`
- **请求参数**: `VehiclePageReqVO`
- **响应结果**: Excel文件下载

### 7. 根据企业ID查询车辆列表
- **接口地址**: `GET /logistics/vehicle/list-by-enterprise`
- **功能描述**: 获取指定企业的所有车辆
- **权限要求**: `logistics:vehicle:query`
- **请求参数**: `enterpriseId` (企业ID)
- **响应结果**: `List<VehicleRespVO>`

### 8. 根据状态查询车辆列表
- **接口地址**: `GET /logistics/vehicle/list-by-status`
- **功能描述**: 获取指定状态的所有车辆
- **权限要求**: `logistics:vehicle:query`
- **请求参数**: `status` (车辆状态)
- **响应结果**: `List<VehicleRespVO>`

### 9. 更新车辆状态
- **接口地址**: `PUT /logistics/vehicle/update-status`
- **功能描述**: 更新车辆状态
- **权限要求**: `logistics:vehicle:update`
- **请求参数**: `id` (车辆ID), `status` (新状态)
- **响应结果**: 操作成功标识

### 10. 根据车牌号查询车辆
- **接口地址**: `GET /logistics/vehicle/get-by-plate-number`
- **功能描述**: 根据车牌号获取车辆信息
- **权限要求**: `logistics:vehicle:query`
- **请求参数**: `plateNumber` (车牌号)
- **响应结果**: `VehicleRespVO`

## 数据模型

### VehicleCreateReqVO (创建请求)
```json
{
  "enterpriseId": 1024,           // 所属企业ID (必填)
  "plateNumber": "京A12345",      // 车牌号 (必填)
  "vehicleType": "厢式货车",       // 车辆类型
  "capacityKg": 5000.00,          // 载重能力(kg)
  "gpsDeviceId": "GPS001",        // GPS设备ID
  "status": 0,                    // 车辆状态 (必填)
  "licenseExpiryDate": "2025-12-31", // 行驶证到期日期
  "insuranceExpiryDate": "2025-12-31", // 保险到期日期
  "maintenanceDate": "2024-01-01", // 上次维护日期
  "vehiclePhotos": ["http://example.com/photo1.jpg"], // 车辆照片URLs
  "licensePhotos": ["http://example.com/license1.jpg"] // 行驶证照片URLs
}
```

### VehicleRespVO (响应结果)
```json
{
  "id": 1024,                     // 车辆ID
  "enterpriseId": 1024,           // 所属企业ID
  "enterpriseName": "XX物流公司",  // 企业名称
  "plateNumber": "京A12345",      // 车牌号
  "vehicleType": "厢式货车",       // 车辆类型
  "capacityKg": 5000.00,          // 载重能力(kg)
  "gpsDeviceId": "GPS001",        // GPS设备ID
  "status": 0,                    // 车辆状态
  "statusName": "可用",           // 车辆状态名称
  "licenseExpiryDate": "2025-12-31", // 行驶证到期日期
  "insuranceExpiryDate": "2025-12-31", // 保险到期日期
  "maintenanceDate": "2024-01-01", // 上次维护日期
  "vehiclePhotos": ["http://example.com/photo1.jpg"], // 车辆照片URLs
  "licensePhotos": ["http://example.com/license1.jpg"], // 行驶证照片URLs
  "createTime": "2024-01-01 00:00:00", // 创建时间
  "updateTime": "2024-01-01 00:00:00"  // 更新时间
}
```

## 车辆状态枚举

| 状态值 | 状态名称 | 描述 |
|--------|----------|------|
| 0 | 可用 | 车辆空闲，可以分配任务 |
| 1 | 运输中 | 车辆正在执行运输任务 |
| 2 | 维护中 | 车辆正在维护，不可用 |

## 业务规则

1. **车牌号唯一性**: 系统中不能存在相同的车牌号
2. **删除限制**: 状态为"运输中"的车辆不能删除
3. **状态管理**: 车辆状态只能在有效的枚举值之间切换
4. **企业关联**: 车辆必须关联到有效的企业
5. **照片存储**: 车辆照片和行驶证照片以JSON数组格式存储URL列表

## 错误码

| 错误码 | 错误信息 | 说明 |
|--------|----------|------|
| 1020001000 | 车辆不存在 | 指定的车辆ID不存在 |
| 1020001001 | 车牌号已存在 | 车牌号重复 |
| 1020001002 | 车辆正在使用中，无法删除 | 运输中的车辆不能删除 |
| 1020001003 | 车辆状态无效 | 车辆状态值不在有效范围内 |

## 使用示例

### 创建车辆
```bash
curl -X POST /logistics/vehicle/create \
  -H "Content-Type: application/json" \
  -d '{
    "enterpriseId": 1024,
    "plateNumber": "京A12345",
    "vehicleType": "厢式货车",
    "capacityKg": 5000.00,
    "status": 0
  }'
```

### 查询车辆分页
```bash
curl -X GET "/logistics/vehicle/page?pageNo=1&pageSize=10&plateNumber=京A"
```

### 更新车辆状态
```bash
curl -X PUT "/logistics/vehicle/update-status?id=1024&status=1"
``` 