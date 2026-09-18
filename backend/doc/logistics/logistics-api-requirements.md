# 物流运输 API 需求清单（调整版）

## 1. 引言

本文档列出了"危废再生资源全流程追溯SaaS平台"中物流运输模块所需的API接口。这些API旨在支持回收企业、物流公司、司机以及平台管理等角色的各项操作。

## 2. API 命名约定和通用规则

-   **基路径：** `/api/v1/logistics` (示例)
-   **认证：** 所有接口均需通过认证授权 (如 JWT Token)。
-   **请求/响应格式：** JSON。
-   **错误处理：**遵循标准的HTTP状态码，并在响应体中提供详细错误信息。
-   **分页：** 对于列表查询接口，支持分页参数 (如 `page`, `pageSize`)。
-   **排序：** 对于列表查询接口，支持排序参数 (如 `sortBy`, `sortOrder`)。

## 3. API 清单

### 3.1 物流分配与管理 (针对回收企业/平台管理员)

| 序号 | API 端点                      | HTTP 方法 | 描述                                                            | 主要请求参数 (示例)                                                                 | 主要响应内容 (示例)                                                                   |
|------|-------------------------------|-----------|-----------------------------------------------------------------|-----------------------------------------------------------------------------------|-------------------------------------------------------------------------------------|
| 1    | `/assignments`                | `POST`    | 为订单分配物流 (US-005)                                           | `orderId`, `logisticsCompanyId`, `vehicleId` (可选), `driverId` (可选), `plannedPickupTime`, `logisticsType` | `logisticsAssignmentDetails` (分配记录详情)                                               |
| 2    | `/assignments/{assignmentId}` | `PUT`     | 修改物流分配信息 (如更换车辆/司机，仅限特定状态)                        | `vehicleId`, `driverId`, `plannedPickupTime`                                        | `logisticsAssignmentDetails`                                                        |
| 3    | `/assignments/{assignmentId}` | `GET`     | 查询特定物流分配详情                                                  | -                                                                                 | `logisticsAssignmentDetails`                                                        |
| 4    | `/assignments`                | `GET`     | 查询物流分配列表 (支持按订单ID, 物流公司, 状态等筛选)                  | `orderId`, `logisticsCompanyId`, `status`, `page`, `pageSize`                       | `List<logisticsAssignmentDetails>`, `paginationInfo`                                    |
| 5    | `/assignments/{assignmentId}/cancel` | `POST`    | 取消物流分配 (仅限特定状态)                                          | `reason`                                                                          | `Success/Failure` message                                                           |

### 3.2 物流任务执行 (针对物流公司调度员/司机)

| 序号 | API 端点                               | HTTP 方法 | 描述                                               | 主要请求参数 (示例)                                                              | 主要响应内容 (示例)                                                                |
|------|----------------------------------------|-----------|----------------------------------------------------|--------------------------------------------------------------------------------|----------------------------------------------------------------------------------|
| 6    | `/tasks`                               | `GET`     | 物流司机/调度查询名下运输任务列表                      | `status`, `dateRange`, `page`, `pageSize`                                        | `List<logisticsTaskSummary>`, `paginationInfo`                                     |
| 7    | `/tasks/{assignmentId}`                | `GET`     | 查询特定运输任务详情                                   | -                                                                              | `logisticsTaskDetails` (包含订单信息、产废方信息、节点历史等)                        |
| 8    | `/tasks/{assignmentId}/accept`         | `POST`    | 司机接受运输任务                                     | -                                                                              | `Success/Failure` message, updated `logisticsAssignmentDetails`                    |
| 9    | `/tasks/{assignmentId}/reject`         | `POST`    | 司机拒绝运输任务                                     | `reason`                                                                       | `Success/Failure` message                                                        |
| 10   | `/tasks/{assignmentId}/nodes`          | `POST`    | 司机上报运输节点 (US-006)                            | `nodeType`, `timestamp`, `latitude`, `longitude`, `photosUrls` (可选), `remarks` (可选) | `transportNodeDetails`, updated `logisticsAssignmentDetails`                     |
| 11   | `/tasks/{assignmentId}/nodes`          | `GET`     | 查询运输任务的节点历史                                 | -                                                                              | `List<transportNodeDetails>`                                                       |
| 12   | `/tasks/{assignmentId}/in-transit-volume` | `POST` | (系统内部或特定设备触发) 上报在途量 (US-006, US-020a) | `currentInTransitKg`, `timestamp`, `latitude`, `longitude`                     | `Success/Failure` message                                                        |

### 3.3 过磅数据管理 (针对回收企业操作员)

| 序号 | API 端点                                      | HTTP 方法 | 描述                                      | 主要请求参数 (示例)                                                                | 主要响应内容 (示例)                                                              |
|------|-----------------------------------------------|-----------|-------------------------------------------|----------------------------------------------------------------------------------|----------------------------------------------------------------------------------|
| 13   | `/weighbridge-logs`                           | `POST`    | 录入订单的过磅数据 (US-009)                 | `logisticsAssignmentId`, `orderId`, `grossWeightKg`, `tareWeightKg`, `weighTime`, `weighbridgeSlipUrl` (可选), `isCorrection`, `originalLogId` (若isCorrection) | `weighbridgeLogDetails`, potentially triggers anomaly (US-022)                   |
| 14   | `/weighbridge-logs/{logId}`                   | `GET`     | 查询特定过磅记录详情                          | -                                                                                | `weighbridgeLogDetails`                                                          |
| 15   | `/weighbridge-logs/by-assignment/{assignmentId}` | `GET`    | 查询指定物流分配的过磅记录列表                | -                                                                                | `List<weighbridgeLogDetails>`                                                    |

### 3.4 临时订单与扫街回收 (针对收运司机)

| 序号 | API 端点                           | HTTP 方法 | 描述                                     | 主要请求参数 (示例)                                                                                                | 主要响应内容 (示例)                                     |
|------|------------------------------------|-----------|------------------------------------------|--------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------|
| 16   | `/temporary-orders`                | `POST`    | 司机创建临时回收订单 (US-017)              | `wasteType`, `estimatedQuantity`, `pickupLocation` (lat, lon, address), `producerInfo` (可选), `photosUrls` (可选) | `logisticsAssignmentDetails` (新创建的临时订单任务)     |
| 17   | `/temporary-orders/{assignmentId}/payment-voucher` | `POST` | 为临时订单生成支付凭证 (若收款人不在场) (US-017) | -                                                                                                                  | `paymentVoucherDetails` (含二维码信息或链接)          |

### 3.5 在途量监控与核销 (主要供内部及特定角色查询)

| 序号 | API 端点                                       | HTTP 方法 | 描述                                      | 主要请求参数 (示例)                | 主要响应内容 (示例)                                                              |
|------|------------------------------------------------|-----------|-------------------------------------------|------------------------------------|----------------------------------------------------------------------------------|
| 18   | `/in-transit-volumes/vehicles/{vehicleId}`     | `GET`     | 查询特定车辆当前在途量 (US-020)               | -                                  | `vehicleInTransitVolumeDetails` (包含当前量、关联订单等)                             |
| 19   | `/in-transit-volumes/summary`                  | `GET`     | 查询在途量概览 (如按区域、物流公司统计) (US-020) | `region`, `logisticsCompanyId`     | `List<inTransitVolumeSummary>`                                                     |
| 20   | `/tasks/{assignmentId}/unload-confirm`         | `POST`    | 确认卸货完成并核销在途量 (US-021)             | `actualUnloadedWeightKg` (从过磅获取) | `Success/Failure` message, updated `logisticsAssignmentDetails`, `inTransitVolumeUpdate` |

### 3.6 现金代付记录 (针对收运司机/回收企业财务)

| 序号 | API 端点                               | HTTP 方法 | 描述                                         | 主要请求参数 (示例)                                                | 主要响应内容 (示例)                                             |
|------|----------------------------------------|-----------|----------------------------------------------|--------------------------------------------------------------------|---------------------------------------------------------------|
| 21   | `/cash-payments`                       | `POST`    | 司机记录现场代付的现金 (US-031)                | `logisticsAssignmentId`, `orderId`, `amount`, `paymentTime`, `recipientInfo` (可选), `remarks` (可选) | `cashPaymentRecordDetails`                                      |
| 22   | `/cash-payments`                       | `GET`     | 查询现金代付记录 (可按司机、订单、日期等筛选)      | `driverId`, `orderId`, `dateRange`, `status`                         | `List<cashPaymentRecordDetails>`, `paginationInfo`                |
| 23   | `/cash-payments/{recordId}/confirm`      | `POST`    | (回收企业) 确认收到某笔代付记录用于后续对账 (US-031) | -                                                                  | `cashPaymentRecordDetails` (状态更新)                           |

### 3.7 物流公司、车辆、司机管理 (针对平台管理员/物流公司管理员)

*这部分API可能属于更广泛的"企业管理"或"主数据管理"模块，此处列出与物流直接相关的部分。*

| 序号 | API 端点                         | HTTP 方法 | 描述                           | 主要请求参数 (示例)                                | 主要响应内容 (示例)                                   |
|------|----------------------------------|-----------|--------------------------------|----------------------------------------------------|-----------------------------------------------------|
| 24   | `/companies`                     | `POST`    | 新增物流公司                     | `companyDetails` (名称, 联系方式, 资质文件等)         | `logisticsCompanyDetails`                             |
| 25   | `/companies/{companyId}`           | `PUT`     | 修改物流公司信息                   | `companyDetails`                                   | `logisticsCompanyDetails`                             |
| 26   | `/companies/{companyId}`           | `GET`     | 查询物流公司详情                   | -                                                  | `logisticsCompanyDetails`                             |
| 27   | `/companies`                     | `GET`     | 查询物流公司列表                   | `name`, `status`, `page`, `pageSize`                 | `List<logisticsCompanySummary>`, `paginationInfo`       |
| 28   | `/companies/{companyId}/vehicles`  | `POST`    | 为物流公司新增车辆                 | `vehicleDetails` (车牌号, 类型, GPS设备ID等)        | `vehicleDetails`                                      |
| 29   | `/vehicles/{vehicleId}`          | `PUT`     | 修改车辆信息                     | `vehicleDetails`                                   | `vehicleDetails`                                      |
| 30   | `/vehicles/{vehicleId}`          | `GET`     | 查询车辆详情                     | -                                                  | `vehicleDetails`                                      |
| 31   | `/companies/{companyId}/drivers` | `POST`    | 为物流公司新增司机 (关联用户系统)    | `driverDetails` (关联userId, 资质文件等)            | `driverDetails`                                       |
| 32   | `/drivers/{driverId}`            | `PUT`     | 修改司机信息                     | `driverDetails`                                    | `driverDetails`                                       |
| 33   | `/drivers/{driverId}`            | `GET`     | 查询司机详情                     | -                                                  | `driverDetails`                                       |

## 4. 数据对象 (DTO/VO) 示例 (部分)

-   **LogisticsAssignmentDetails:** 包含分配ID, 订单信息摘要, 物流公司信息, 车辆信息, 司机信息, 状态, 时间戳等。
-   **TransportNodeDetails:** 包含节点ID, 类型, 时间戳, 位置信息, 照片, 备注等。
-   **WeighbridgeLogDetails:** 包含过磅记录ID, 关联订单/分配ID, 重量数据, 时间, 是否异常, 照片等。
-   **CashPaymentRecordDetails:** 包含记录ID, 关联订单/分配ID, 司机, 金额, 时间, 状态等。

## 5. 未来可能扩展

-   与第三方物流平台对接的API。
-   运输路线规划与优化建议API。
-   基于AI的ETA (预计到达时间) 预测API。

### 1. 运输任务管理

#### 1.1 创建运输任务（危废模块调用）
- **路径**: `POST /api/logistics/transport-task/create`
- **描述**: 接收危废模块的运输需求，创建运输任务
- **请求参数**: 见危废模块文档
- **响应**:
```json
{
  "code": 200,
  "data": {
    "taskId": 12345,
    "taskNo": "TT202401010001",
    "status": 0  // 待分配
  }
}
```

#### 1.2 任务分配
- **路径**: `PUT /api/logistics/transport-task/{taskId}/assign`
- **描述**: 分配任务给具体的车辆和司机
- **请求参数**:
```json
{
  "logisticsCompanyId": 301,
  "vehicleId": 101,
  "driverId": 201
}
```

#### 1.3 司机接单
- **路径**: `PUT /api/logistics/transport-task/{taskId}/accept`
- **描述**: 司机确认接受任务

#### 1.4 司机拒单
- **路径**: `PUT /api/logistics/transport-task/{taskId}/reject`
- **描述**: 司机拒绝任务
- **请求参数**:
```json
{
  "reason": "车辆故障"
}
```

### 2. 运输节点上报

#### 2.1 上报运输节点
- **路径**: `POST /api/logistics/transport-task/{taskId}/node`
- **描述**: 司机上报运输过程中的关键节点
- **请求参数**:
```json
{
  "nodeType": 3,  // 1:接单,2:到达取货点,3:装货完成,4:开始运输,5:到达送货点,6:卸货完成
  "nodeTime": "2024-01-01 09:30:00",
  "location": {
    "address": "XX市XX区XX路",
    "latitude": 31.2304,
    "longitude": 121.4737
  },
  "actualQuantity": 2.6,  // 装货完成时填写
  "photos": ["photo_url1", "photo_url2"],
  "remark": "已完成装货，共10桶"
}
```

#### 2.2 查询运输轨迹
- **路径**: `GET /api/logistics/transport-task/{taskId}/track`
- **描述**: 查询运输任务的完整轨迹

### 3. 车辆过磅管理

#### 3.1 录入过磅数据
- **路径**: `POST /api/logistics/weighing/record`
- **描述**: 录入车辆过磅数据
- **请求参数**:
```json
{
  "vehicleId": 101,
  "vehicleNo": "沪A12345",
  "driverId": 201,
  "grossWeight": 15.5,
  "tareWeight": 10.6,
  "netWeight": 4.9,
  "weighingTime": "2024-01-01 10:00:00",
  "weighingLocation": "XX回收站",
  "relatedOrderIds": [67890, 67891, 67892],
  "photos": ["weighing_slip_url"]
}
```

#### 3.2 查询车辆过磅记录
- **路径**: `GET /api/logistics/weighing/vehicle/{vehicleId}`
- **描述**: 查询指定车辆的过磅记录
- **请求参数**: 
  - `startDate`: 开始日期
  - `endDate`: 结束日期

### 4. 现金代付管理

#### 4.1 记录现金代付
- **路径**: `POST /api/logistics/cash-advance/record`
- **描述**: 司机记录现金代付信息
- **请求参数**:
```json
{
  "taskId": 12345,
  "orderId": 67890,
  "paymentAmount": 800.00,
  "paymentTime": "2024-01-01 09:00:00",
  "payeeName": "张三",
  "payeePhone": "13800138000",
  "paymentPhotos": ["payment_photo_url"],
  "receiptPhotos": ["receipt_photo_url"]
}
```

#### 4.2 查询代付记录
- **路径**: `GET /api/logistics/cash-advance/list`
- **描述**: 查询现金代付记录
- **请求参数**:
  - `driverId`: 司机ID
  - `reconcileStatus`: 对账状态
  - `startDate`: 开始日期
  - `endDate`: 结束日期

### 5. 临时需求处理

#### 5.1 上报临时需求
- **路径**: `POST /api/logistics/temporary-demand/report`
- **描述**: 司机发现临时回收需求时上报
- **请求参数**:
```json
{
  "wasteType": "废矿物油",
  "estimatedQuantity": 2.5,
  "location": {
    "address": "XX市XX区XX路XX号",
    "latitude": 31.2304,
    "longitude": 121.4737
  },
  "producerInfo": {
    "name": "张三",
    "phone": "13800138000"
  },
  "photos": ["site_photo_url"],
  "remark": "个人车主，废机油约10桶"
}
```
- **响应**:
```json
{
  "code": 200,
  "data": {
    "orderId": 67890,
    "orderNo": "WO202401010001",
    "taskId": 12345,
    "taskNo": "TT202401010001"
  }
} 