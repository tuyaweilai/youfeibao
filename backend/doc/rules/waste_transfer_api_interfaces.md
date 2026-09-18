# 危险废物转移模块接口需求清单 (V2.0)

## 1. API设计原则

- 所有接口遵循 RESTful 风格设计
- 请求和响应体主要使用 JSON 格式
- 所有API接口返回统一的 `CommonResult<T>` 结构
- 涉及分页的列表查询，使用统一的 `PageResult<T>` 作为 `CommonResult` 的data部分
- 接口路径根据资源进行组织，例如 `/admin-api/waste/transfer/appointment` 用于管理后台的预约管理
- 权限控制基于Spring Security和方法注解（如 `@PreAuthorize`）
- 所有金额字段使用 `BigDecimal` 类型，前端传递字符串格式
- 时间字段统一使用 `LocalDateTime` 类型，格式为 `yyyy-MM-dd HH:mm:ss`

## 2. 后台管理端 API (`/admin-api/waste/transfer/`)

### 2.1 预约管理 (Appointment Management)

#### 获取预约单分页列表

- **ID:** WTA-ADMIN-APPT-001
- **路径:** `GET /admin-api/waste/transfer/appointment/page`
- **描述:** 管理员分页查询所有或符合条件的预约单
- **请求参数:** `AppointmentPageReqVO`
```json
{
  "pageNo": 1,
  "pageSize": 10,
  "appointmentNo": "AP202401010001",
  "producingEnterpriseName": "XX汽修厂",
  "assignedRecyclingEnterpriseName": "XX回收公司",
  "status": 1,
  "wasteCode": "HW08",
  "createTimeBegin": "2024-01-01 00:00:00",
  "createTimeEnd": "2024-01-31 23:59:59",
  "expectedCollectionTimeBegin": "2024-01-01 00:00:00",
  "expectedCollectionTimeEnd": "2024-01-31 23:59:59"
}
```
- **响应:** `CommonResult<PageResult<AppointmentRespVO>>`
- **权限:** `waste:transfer:appointment:query`

#### 获取预约单详情

- **ID:** WTA-ADMIN-APPT-002
- **路径:** `GET /admin-api/waste/transfer/appointment/get`
- **请求参数:** `id` (预约单ID)
- **响应:** `CommonResult<AppointmentDetailRespVO>`
- **权限:** `waste:transfer:appointment:query`

#### 手动变更预约单的回收企业 (US-029)

- **ID:** WTA-ADMIN-APPT-003
- **路径:** `PUT /admin-api/waste/transfer/appointment/assign-recycler`
- **描述:** 管理员为预约单手动指定或更改回收企业
- **请求参数:** `UpdateAppointmentRecyclerReqVO`
```json
{
  "id": 1001,
  "newRecyclingEnterpriseId": 2001,
  "remark": "原回收企业产能不足，手动调整"
}
```
- **响应:** `CommonResult<Boolean>`
- **权限:** `waste:transfer:appointment:assign`

#### 取消预约单 (管理员操作)

- **ID:** WTA-ADMIN-APPT-004
- **路径:** `PUT /admin-api/waste/transfer/appointment/cancel`
- **描述:** 管理员因特定原因取消预约单
- **请求参数:** `CancelAppointmentReqVO`
```json
{
  "id": 1001,
  "cancellationReason": "产废企业已自行处理"
}
```
- **响应:** `CommonResult<Boolean>`
- **权限:** `waste:transfer:appointment:cancel`

### 2.2 订单管理 (Order Management)

#### 获取订单分页列表

- **ID:** WTA-ADMIN-ORDER-001
- **路径:** `GET /admin-api/waste/transfer/order/page`
- **描述:** 管理员分页查询所有或符合条件的订单
- **请求参数:** `OrderPageReqVO`
```json
{
  "pageNo": 1,
  "pageSize": 10,
  "orderNo": "WO202401010001",
  "appointmentNo": "AP202401010001",
  "producingEnterpriseName": "XX汽修厂",
  "recyclingEnterpriseName": "XX回收公司",
  "logisticsEnterpriseName": "XX物流",
  "businessStatus": 1,
  "paymentStatus": 0,
  "sourceType": 0,
  "wasteCode": "HW08",
  "createTimeBegin": "2024-01-01 00:00:00",
  "createTimeEnd": "2024-01-31 23:59:59"
}
```
- **响应:** `CommonResult<PageResult<OrderRespVO>>`
- **权限:** `waste:transfer:order:query`

#### 获取订单详情

- **ID:** WTA-ADMIN-ORDER-002
- **路径:** `GET /admin-api/waste/transfer/order/get`
- **请求参数:** `id` (订单ID)
- **响应:** `CommonResult<OrderDetailRespVO>` (包含废物信息、关联方、物流节点、过磅记录、支付发票状态等)
- **权限:** `waste:transfer:order:query`

#### 更新订单状态 (管理员操作)

- **ID:** WTA-ADMIN-ORDER-003
- **路径:** `PUT /admin-api/waste/transfer/order/update-status`
- **描述:** 管理员在特定情况下修改订单状态
- **请求参数:** `UpdateOrderStatusReqVO`
```json
{
  "id": 1001,
  "newStatus": 5,
  "remark": "异常订单手动关闭"
}
```
- **响应:** `CommonResult<Boolean>`
- **权限:** `waste:transfer:order:update`

#### 管理订单补单 (US-023)

- **ID:** WTA-ADMIN-ORDER-004
- **路径:** `POST /admin-api/waste/transfer/order/create-supplement`
- **描述:** 管理员为原订单创建补单
- **请求参数:** `CreateSupplementOrderReqVO`
```json
{
  "relatedOrderId": 1001,
  "supplementType": 1,
  "quantityDifference": "0.5",
  "amountDifference": "250.00",
  "reason": "实际收运量超出预估"
}
```
- **响应:** `CommonResult<Long>` (新补单的ID)
- **权限:** `waste:transfer:order:supplement`

#### 订单分摊管理

- **ID:** WTA-ADMIN-ORDER-005
- **路径:** `GET /admin-api/waste/transfer/order/allocation/page`
- **描述:** 查询订单分摊记录
- **请求参数:** `OrderAllocationPageReqVO`
- **响应:** `CommonResult<PageResult<OrderAllocationRespVO>>`
- **权限:** `waste:transfer:order:query`

#### 手动调整订单分摊

- **ID:** WTA-ADMIN-ORDER-006
- **路径:** `PUT /admin-api/waste/transfer/order/allocation/adjust`
- **描述:** 管理员手动调整订单分摊结果
- **请求参数:** `AdjustOrderAllocationReqVO`
```json
{
  "orderId": 1001,
  "newAllocatedQuantity": "2.1",
  "adjustmentReason": "质量差异调整"
}
```
- **响应:** `CommonResult<Boolean>`
- **权限:** `waste:transfer:order:allocation:adjust`

### 2.3 回收方分配规则配置 (Recycler Assignment Rule Configuration - US-027)

#### 创建分配规则

- **ID:** WTA-ADMIN-RULE-001
- **路径:** `POST /admin-api/waste/transfer/assignment-rule/create`
- **请求参数:** `RecyclerAssignmentRuleCreateReqVO`
```json
{
  "ruleName": "华东地区废矿物油分配规则",
  "priority": 1,
  "isEnabled": true,
  "matchProvinceCode": "310000",
  "matchCityCode": "310100",
  "matchDistrictCode": "",
  "matchWasteCategoryId": 1001,
  "assignedRecyclingEnterpriseId": 2001,
  "description": "上海市废矿物油优先分配给XX回收公司"
}
```
- **响应:** `CommonResult<Long>` (规则ID)
- **权限:** `waste:transfer:assignment-rule:create`

#### 更新分配规则

- **ID:** WTA-ADMIN-RULE-002
- **路径:** `PUT /admin-api/waste/transfer/assignment-rule/update`
- **请求参数:** `RecyclerAssignmentRuleUpdateReqVO`
- **响应:** `CommonResult<Boolean>`
- **权限:** `waste:transfer:assignment-rule:update`

#### 删除分配规则

- **ID:** WTA-ADMIN-RULE-003
- **路径:** `DELETE /admin-api/waste/transfer/assignment-rule/delete`
- **请求参数:** `id` (规则ID)
- **响应:** `CommonResult<Boolean>`
- **权限:** `waste:transfer:assignment-rule:delete`

#### 获取分配规则详情

- **ID:** WTA-ADMIN-RULE-004
- **路径:** `GET /admin-api/waste/transfer/assignment-rule/get`
- **请求参数:** `id` (规则ID)
- **响应:** `CommonResult<RecyclerAssignmentRuleRespVO>`
- **权限:** `waste:transfer:assignment-rule:query`

#### 获取分配规则分页列表

- **ID:** WTA-ADMIN-RULE-005
- **路径:** `GET /admin-api/waste/transfer/assignment-rule/page`
- **请求参数:** `RecyclerAssignmentRulePageReqVO`
- **响应:** `CommonResult<PageResult<RecyclerAssignmentRuleRespVO>>`
- **权限:** `waste:transfer:assignment-rule:query`

#### 更新系统回收方分配总开关 (US-027)

- **ID:** WTA-ADMIN-SYS-001
- **路径:** `PUT /admin-api/waste/transfer/system-config/update-assignment-mode`
- **描述:** 配置是否允许产废企业手动选择回收方
- **请求参数:** `UpdateAssignmentModeReqVO`
```json
{
  "allowUserSelection": true
}
```
- **响应:** `CommonResult<Boolean>`
- **权限:** `waste:transfer:system-config:update`

### 2.4 价格管理 (Price Management)

#### 市场价格基准管理

- **ID:** WTA-ADMIN-PRICE-001
- **路径:** `POST /admin-api/waste/transfer/price-benchmark/create`
- **描述:** 创建市场价格基准
- **请求参数:** `PriceBenchmarkCreateReqVO`
```json
{
  "wasteCode": "HW08",
  "wasteName": "废矿物油",
  "price": "500.00",
  "priceUnit": "元/吨",
  "regionCode": "310000",
  "regionName": "上海市",
  "effectiveDate": "2024-01-01",
  "expireDate": "2024-12-31",
  "priceSource": "市场调研",
  "remark": "2024年度基准价格"
}
```
- **响应:** `CommonResult<Long>`
- **权限:** `waste:transfer:price:benchmark:create`

#### 回收企业价格配置管理

- **ID:** WTA-ADMIN-PRICE-002
- **路径:** `GET /admin-api/waste/transfer/recycler-price/page`
- **描述:** 查询回收企业价格配置
- **请求参数:** `RecyclerPricePageReqVO`
- **响应:** `CommonResult<PageResult<RecyclerPriceRespVO>>`
- **权限:** `waste:transfer:price:recycler:query`

#### 客户专属价格管理

- **ID:** WTA-ADMIN-PRICE-003
- **路径:** `GET /admin-api/waste/transfer/customer-price/page`
- **描述:** 查询客户专属价格配置
- **请求参数:** `CustomerPricePageReqVO`
- **响应:** `CommonResult<PageResult<CustomerPriceRespVO>>`
- **权限:** `waste:transfer:price:customer:query`

### 2.5 付款配置管理 (Payment Configuration)

#### 产废企业付款配置管理

- **ID:** WTA-ADMIN-PAYMENT-001
- **路径:** `GET /admin-api/waste/transfer/payment-config/page`
- **描述:** 查询产废企业付款配置
- **请求参数:** `PaymentConfigPageReqVO`
- **响应:** `CommonResult<PageResult<PaymentConfigRespVO>>`
- **权限:** `waste:transfer:payment-config:query`

#### 对公付款凭证管理

- **ID:** WTA-ADMIN-PAYMENT-002
- **路径:** `GET /admin-api/waste/transfer/payment-voucher/page`
- **描述:** 查询对公付款凭证
- **请求参数:** `PaymentVoucherPageReqVO`
- **响应:** `CommonResult<PageResult<PaymentVoucherRespVO>>`
- **权限:** `waste:transfer:payment-voucher:query`

#### 确认对公付款凭证

- **ID:** WTA-ADMIN-PAYMENT-003
- **路径:** `PUT /admin-api/waste/transfer/payment-voucher/confirm`
- **描述:** 管理员确认对公付款凭证
- **请求参数:** `ConfirmPaymentVoucherReqVO`
```json
{
  "id": 1001,
  "confirmResult": 1,
  "remark": "凭证真实有效"
}
```
- **响应:** `CommonResult<Boolean>`
- **权限:** `waste:transfer:payment-voucher:confirm`

## 3. 产废企业APP/用户端 API (`/app-api/waste/transfer/`)

### 3.1 预约管理 (Appointment)

#### 发起危废转移预约 (US-003)

- **ID:** WTA-APP-APPT-001
- **路径:** `POST /app-api/waste/transfer/appointment/create`
- **描述:** 产废企业用户提交新的废物转移预约
- **请求参数:** `AppointmentCreateReqVO`
```json
{
  "producingStoreId": 1001,
  "producingContactName": "张三",
  "producingContactPhone": "13800138000",
  "producingAddressDetail": "XX市XX区XX路XX号",
  "wasteCode": "HW08",
  "wasteName": "废矿物油",
  "wasteCategoryId": 1001,
  "estimatedQuantity": "2.5",
  "quantityUnit": "吨",
  "packagingType": "桶装",
  "expectedCollectionTimeStart": "2024-01-01 14:00:00",
  "expectedCollectionTimeEnd": "2024-01-01 18:00:00",
  "selectedRecyclingEnterpriseId": 2001,
  "userRemark": "请提前联系"
}
```
- **响应:** `CommonResult<Long>` (预约单ID)
- **权限:** (认证后的产废企业用户)

#### 获取我的预约单分页列表

- **ID:** WTA-APP-APPT-002
- **路径:** `GET /app-api/waste/transfer/appointment/page`
- **描述:** 产废企业用户查询自己发起的预约单
- **请求参数:** `MyAppointmentPageReqVO`
```json
{
  "pageNo": 1,
  "pageSize": 10,
  "status": 1,
  "wasteCode": "HW08",
  "createTimeBegin": "2024-01-01 00:00:00",
  "createTimeEnd": "2024-01-31 23:59:59"
}
```
- **响应:** `CommonResult<PageResult<AppointmentRespVO>>`
- **权限:** (认证后的产废企业用户)

#### 获取我的预约单详情

- **ID:** WTA-APP-APPT-003
- **路径:** `GET /app-api/waste/transfer/appointment/get`
- **请求参数:** `id` (预约单ID, 需校验是否属于当前用户企业)
- **响应:** `CommonResult<AppointmentDetailRespVO>`
- **权限:** (认证后的产废企业用户)

#### 取消我的预约单

- **ID:** WTA-APP-APPT-004
- **路径:** `PUT /app-api/waste/transfer/appointment/cancel`
- **描述:** 产废企业用户取消尚未被处理的预约单
- **请求参数:** `CancelAppointmentReqVO`
```json
{
  "id": 1001,
  "cancellationReason": "计划变更，暂不处理"
}
```
- **响应:** `CommonResult<Boolean>`
- **权限:** (认证后的产废企业用户, 且预约单状态允许取消)

#### 获取可选择的回收企业列表 (US-003)

- **ID:** WTA-APP-APPT-005
- **路径:** `GET /app-api/waste/transfer/appointment/list-available-recyclers`
- **描述:** 当系统配置允许手动选择时，产废企业获取可选择的回收企业列表
- **请求参数:** (当前产废企业ID从token中获取)
- **响应:** `CommonResult<List<EnterpriseSimpleRespVO>>`
- **权限:** (认证后的产废企业用户, 且系统配置允许)

### 3.2 订单管理 (Order)

#### 获取我的订单分页列表

- **ID:** WTA-APP-ORDER-001
- **路径:** `GET /app-api/waste/transfer/order/page`
- **描述:** 产废企业用户查询自己相关的订单
- **请求参数:** `MyOrderPageReqVO`
```json
{
  "pageNo": 1,
  "pageSize": 10,
  "businessStatus": 1,
  "paymentStatus": 0,
  "sourceType": 0,
  "wasteCode": "HW08",
  "createTimeBegin": "2024-01-01 00:00:00",
  "createTimeEnd": "2024-01-31 23:59:59"
}
```
- **响应:** `CommonResult<PageResult<OrderRespVO>>`
- **权限:** (认证后的产废企业用户)

#### 获取我的订单详情

- **ID:** WTA-APP-ORDER-002
- **路径:** `GET /app-api/waste/transfer/order/get`
- **请求参数:** `id` (订单ID, 需校验是否属于当前用户企业)
- **响应:** `CommonResult<OrderDetailRespVO>`
- **权限:** (认证后的产废企业用户)

#### 确认对公付款凭证

- **ID:** WTA-APP-ORDER-003
- **路径:** `PUT /app-api/waste/transfer/order/confirm-payment-voucher`
- **描述:** 产废企业确认收到的对公付款凭证
- **请求参数:** `ConfirmPaymentVoucherReqVO`
```json
{
  "orderId": 1001,
  "voucherId": 2001,
  "confirmResult": 1,
  "remark": "金额正确，已收到款项"
}
```
- **响应:** `CommonResult<Boolean>`
- **权限:** (认证后的产废企业用户)

### 3.3 付款配置管理

#### 获取我的付款配置

- **ID:** WTA-APP-PAYMENT-001
- **路径:** `GET /app-api/waste/transfer/payment-config/my-config`
- **描述:** 获取当前企业的付款配置
- **响应:** `CommonResult<PaymentConfigRespVO>`
- **权限:** (认证后的产废企业用户)

#### 更新付款配置

- **ID:** WTA-APP-PAYMENT-002
- **路径:** `PUT /app-api/waste/transfer/payment-config/update`
- **描述:** 更新企业付款配置
- **请求参数:** `UpdatePaymentConfigReqVO`
```json
{
  "paymentMethod": 2,
  "personalPayeeName": "张三",
  "personalPayeePhone": "13800138000",
  "personalPayeeIdCard": "310101199001011234",
  "personalBankName": "中国工商银行",
  "personalBankAccount": "6222021234567890123",
  "personalAccountName": "张三",
  "relationshipToEnterprise": "法人代表"
}
```
- **响应:** `CommonResult<Boolean>`
- **权限:** (认证后的产废企业用户)

## 4. 回收企业APP/用户端 API (`/app-api/recycler/waste/transfer/`)

### 4.1 预约管理 (Appointment)

#### 获取待处理/相关的预约单分页列表 (US-004)

- **ID:** WTA-RECYCLER-APPT-001
- **路径:** `GET /app-api/recycler/waste/transfer/appointment/page`
- **描述:** 回收企业用户查询指派给本企业或已处理的预约单
- **请求参数:** `RecyclerAppointmentPageReqVO`
```json
{
  "pageNo": 1,
  "pageSize": 10,
  "status": 1,
  "producingEnterpriseName": "XX汽修厂",
  "wasteCode": "HW08",
  "createTimeBegin": "2024-01-01 00:00:00",
  "createTimeEnd": "2024-01-31 23:59:59"
}
```
- **响应:** `CommonResult<PageResult<AppointmentRespVO>>`
- **权限:** (认证后的回收企业用户)

#### 获取预约单详情 (US-004)

- **ID:** WTA-RECYCLER-APPT-002
- **路径:** `GET /app-api/recycler/waste/transfer/appointment/get`
- **请求参数:** `id` (预约单ID, 需校验是否属于当前回收企业)
- **响应:** `CommonResult<AppointmentDetailRespVO>`
- **权限:** (认证后的回收企业用户)

#### 接收预约 (US-004)

- **ID:** WTA-RECYCLER-APPT-003
- **路径:** `POST /app-api/recycler/waste/transfer/appointment/accept`
- **描述:** 回收企业确认接收预约，通常会触发订单生成
- **请求参数:** `AcceptAppointmentReqVO`
```json
{
  "id": 1001,
  "quotedPrice": "500.00",
  "priceUnit": "元/吨",
  "totalAmount": "1250.00",
  "quotationRemark": "包含运费，质量要求符合标准",
  "validUntil": "2024-01-03 18:00:00",
  "remark": "确认接收，3日内安排收运"
}
```
- **响应:** `CommonResult<Long>` (生成的订单ID)
- **权限:** (认证后的回收企业用户, 且预约单状态为"待回收方确认")

#### 拒绝预约 (US-004)

- **ID:** WTA-RECYCLER-APPT-004
- **路径:** `POST /app-api/recycler/waste/transfer/appointment/reject`
- **描述:** 回收企业拒绝预约
- **请求参数:** `RejectAppointmentReqVO`
```json
{
  "id": 1001,
  "rejectionReason": "当前产能不足，无法及时处理"
}
```
- **响应:** `CommonResult<Boolean>`
- **权限:** (认证后的回收企业用户, 且预约单状态为"待回收方确认")

### 4.2 订单管理 (Order)

#### 获取我的订单分页列表

- **ID:** WTA-RECYCLER-ORDER-001
- **路径:** `GET /app-api/recycler/waste/transfer/order/page`
- **描述:** 回收企业用户查询自己相关的订单
- **请求参数:** `RecyclerOrderPageReqVO`
```json
{
  "pageNo": 1,
  "pageSize": 10,
  "businessStatus": 1,
  "paymentStatus": 0,
  "sourceType": 0,
  "producingEnterpriseName": "XX汽修厂",
  "wasteCode": "HW08",
  "createTimeBegin": "2024-01-01 00:00:00",
  "createTimeEnd": "2024-01-31 23:59:59"
}
```
- **响应:** `CommonResult<PageResult<OrderRespVO>>`
- **权限:** (认证后的回收企业用户)

#### 获取我的订单详情

- **ID:** WTA-RECYCLER-ORDER-002
- **路径:** `GET /app-api/recycler/waste/transfer/order/get`
- **请求参数:** `id` (订单ID, 需校验是否属于当前用户企业)
- **响应:** `CommonResult<OrderDetailRespVO>`
- **权限:** (认证后的回收企业用户)

#### 指派物流给订单 (US-005)

- **ID:** WTA-RECYCLER-ORDER-003
- **路径:** `POST /app-api/recycler/waste/transfer/order/assign-logistics`
- **描述:** 回收企业为订单指派物流方（自有车队或第三方物流公司）
- **请求参数:** `AssignLogisticsReqVO`
```json
{
  "orderId": 1001,
  "logisticsType": 2,
  "logisticsEnterpriseId": 3001,
  "vehicleId": 4001,
  "driverUserId": 5001,
  "estimatedPickupTime": "2024-01-02 14:00:00",
  "remark": "请提前联系产废方"
}
```
- **响应:** `CommonResult<Boolean>`
- **权限:** (认证后的回收企业用户, 订单状态允许指派物流)

#### 创建补单 (US-023)

- **ID:** WTA-RECYCLER-ORDER-004
- **路径:** `POST /app-api/recycler/waste/transfer/order/create-supplement`
- **描述:** 回收企业为原订单创建补单
- **请求参数:** `CreateSupplementOrderReqVO`
```json
{
  "relatedOrderId": 1001,
  "supplementType": 1,
  "actualQuantityDifference": "0.3",
  "unitPrice": "500.00",
  "amountDifference": "150.00",
  "reason": "实际收运量超出预估",
  "remark": "现场确认增加0.3吨"
}
```
- **响应:** `CommonResult<Long>` (新补单的ID)
- **权限:** (认证后的回收企业用户)

#### 上传对公付款凭证

- **ID:** WTA-RECYCLER-ORDER-005
- **路径:** `POST /app-api/recycler/waste/transfer/order/upload-payment-voucher`
- **描述:** 回收企业上传对公付款凭证
- **请求参数:** `UploadPaymentVoucherReqVO`
```json
{
  "orderId": 1001,
  "paymentType": 1,
  "paymentAmount": "1250.00",
  "paymentDate": "2024-01-02",
  "paymentBank": "中国工商银行",
  "paymentAccount": "1234567890123456789",
  "payeeBank": "中国建设银行",
  "payeeAccount": "9876543210987654321",
  "payeeName": "XX汽修厂",
  "transactionNo": "20240102001234567890",
  "transferVoucherUrl": "https://example.com/voucher1.jpg",
  "bankReceiptUrl": "https://example.com/receipt1.jpg"
}
```
- **响应:** `CommonResult<Long>` (凭证ID)
- **权限:** (认证后的回收企业用户)

#### 补充个人收款信息

- **ID:** WTA-RECYCLER-ORDER-006
- **路径:** `PUT /app-api/recycler/waste/transfer/order/update-personal-payee`
- **描述:** 为个人结算订单补充收款人信息
- **请求参数:** `UpdatePersonalPayeeReqVO`
```json
{
  "orderId": 1001,
  "payeeName": "张三",
  "payeePhone": "13800138000",
  "payeeIdCard": "310101199001011234",
  "bankName": "中国工商银行",
  "bankAccount": "6222021234567890123",
  "accountName": "张三",
  "relationshipToEnterprise": "法人代表"
}
```
- **响应:** `CommonResult<Boolean>`
- **权限:** (认证后的回收企业用户)

### 4.3 价格管理

#### 获取我的价格配置

- **ID:** WTA-RECYCLER-PRICE-001
- **路径:** `GET /app-api/recycler/waste/transfer/price-config/my-config`
- **描述:** 获取当前回收企业的价格配置
- **请求参数:** `MyPriceConfigPageReqVO`
- **响应:** `CommonResult<PageResult<RecyclerPriceRespVO>>`
- **权限:** (认证后的回收企业用户)

#### 更新价格配置

- **ID:** WTA-RECYCLER-PRICE-002
- **路径:** `PUT /app-api/recycler/waste/transfer/price-config/update`
- **描述:** 更新回收企业价格配置
- **请求参数:** `UpdateRecyclerPriceReqVO`
```json
{
  "id": 1001,
  "purchasePrice": "520.00",
  "priceUnit": "元/吨",
  "minQuantity": "0.5",
  "maxQuantity": "10.0",
  "effectiveDate": "2024-01-01",
  "expireDate": "2024-12-31",
  "isNegotiable": false,
  "remark": "2024年度价格调整"
}
```
- **响应:** `CommonResult<Boolean>`
- **权限:** (认证后的回收企业用户)

#### 设置客户专属价格

- **ID:** WTA-RECYCLER-PRICE-003
- **路径:** `POST /app-api/recycler/waste/transfer/customer-price/create`
- **描述:** 为特定客户设置专属价格
- **请求参数:** `CreateCustomerPriceReqVO`
```json
{
  "customerEnterpriseId": 1001,
  "wasteCode": "HW08",
  "wasteName": "废矿物油",
  "priceType": 1,
  "specialPrice": "550.00",
  "priceUnit": "元/吨",
  "minQuantity": "1.0",
  "maxQuantity": "20.0",
  "effectiveDate": "2024-01-01",
  "expireDate": "2024-12-31",
  "priceAdvantageDesc": "VIP客户专享价格，比基础价格优惠10%"
}
```
- **响应:** `CommonResult<Long>`
- **权限:** (认证后的回收企业用户)

## 5. 物流端/收运员APP端 API (`/app-api/logistics/waste/transfer/`)

### 5.1 任务管理 (Task/Order)

#### 获取我的运输任务列表

- **ID:** WTA-LOGISTICS-TASK-001
- **路径:** `GET /app-api/logistics/waste/transfer/task/list`
- **描述:** 物流司机/收运员查询分配给自己的运输任务（订单）
- **请求参数:** `MyTaskListReqVO`
```json
{
  "status": 2,
  "dateBegin": "2024-01-01",
  "dateEnd": "2024-01-31"
}
```
- **响应:** `CommonResult<List<OrderSimpleRespVO>>`
- **权限:** (认证后的物流用户/司机)

#### 获取运输任务详情

- **ID:** WTA-LOGISTICS-TASK-002
- **路径:** `GET /app-api/logistics/waste/transfer/task/get`
- **请求参数:** `orderId`
- **响应:** `CommonResult<OrderDetailForLogisticsRespVO>`
- **权限:** (认证后的物流用户/司机)

#### 创建临时订单/扫街回收 (US-017)

- **ID:** WTA-LOGISTICS-TASK-003
- **路径:** `POST /app-api/logistics/waste/transfer/order/create-temporary`
- **描述:** 收运员现场快速创建临时回收订单
- **请求参数:** `TemporaryOrderCreateReqVO`
```json
{
  "wasteCode": "HW08",
  "wasteName": "废矿物油",
  "estimatedQuantity": "1.5",
  "quantityUnit": "吨",
  "packagingType": "桶装",
  "producingContactName": "李四",
  "producingContactPhone": "13900139000",
  "producingAddressDetail": "XX市XX区XX路XX号",
  "locationLatitude": "31.2304",
  "locationLongitude": "121.4737",
  "sitePhotosFileIds": [10001, 10002],
  "remark": "现场发现临时需求"
}
```
- **响应:** `CommonResult<Long>` (新订单ID)
- **权限:** (认证后的物流用户/司机)

#### 上报订单物流节点 (US-006)

- **ID:** WTA-LOGISTICS-TASK-004
- **路径:** `POST /app-api/logistics/waste/transfer/order/report-node`
- **描述:** 收运员上报装货、在途、卸货、到达回收站点等物流节点信息
- **请求参数:** `LogisticsNodeReportReqVO`
```json
{
  "orderId": 1001,
  "nodeType": 2,
  "timestamp": "2024-01-02 10:30:00",
  "gpsLatitude": "31.2304",
  "gpsLongitude": "121.4737",
  "address": "XX市XX区XX路XX号",
  "photoFileIds": [10003, 10004],
  "remark": "已装货完成，开始运输",
  "currentWasteVolume": "2.3"
}
```
- **响应:** `CommonResult<Boolean>`
- **权限:** (认证后的物流用户/司机)

#### 记录现场现金支付 (US-031)

- **ID:** WTA-LOGISTICS-TASK-005
- **路径:** `POST /app-api/logistics/waste/transfer/order/record-cash-payment`
- **描述:** 收运员在确认收货时，标记已现金支付并记录金额
- **请求参数:** `RecordCashPaymentReqVO`
```json
{
  "orderId": 1001,
  "paidAmount": "1250.00",
  "paymentTime": "2024-01-02 15:30:00",
  "payeeName": "张三",
  "payeePhone": "13800138000",
  "paymentLocation": "XX市XX区XX路XX号",
  "receiptPhotoFileIds": [10005]
}
```
- **响应:** `CommonResult<Boolean>`
- **权限:** (认证后的物流用户/司机)

## 6. 模块间协作接口 (Internal API)

### 6.1 供物流模块调用的接口

#### 创建临时订单

- **ID:** WTA-INTERNAL-001
- **路径:** `POST /internal-api/waste/order/create-temporary`
- **描述:** 物流司机发现临时需求时调用此接口创建订单
- **请求参数:** `InternalTemporaryOrderCreateReqVO`
```json
{
  "wasteCode": "HW08",
  "wasteName": "废矿物油",
  "estimatedQuantity": "2.5",
  "quantityUnit": "吨",
  "producerInfo": {
    "name": "张三",
    "phone": "13800138000",
    "address": "XX市XX区XX路XX号"
  },
  "location": {
    "latitude": "31.2304",
    "longitude": "121.4737"
  },
  "driverId": 12345,
  "photos": ["url1", "url2"]
}
```
- **响应:**
```json
{
  "code": 0,
  "data": {
    "orderId": 67890,
    "orderNo": "WO202401010001"
  },
  "msg": ""
}
```

#### 更新订单物流状态

- **ID:** WTA-INTERNAL-002
- **路径:** `PUT /internal-api/waste/order/{orderId}/logistics-status`
- **描述:** 物流模块更新订单的物流状态
- **请求参数:** `UpdateOrderLogisticsStatusReqVO`
```json
{
  "transportTaskId": 12345,
  "transportTaskNo": "TT202401010001",
  "logisticsStatus": 2,
  "currentLocation": {
    "address": "XX市XX区",
    "latitude": "31.2304",
    "longitude": "121.4737"
  },
  "estimatedArrival": "2024-01-01 16:00:00"
}
```
- **响应:** `CommonResult<Boolean>`

#### 接收过磅结果

- **ID:** WTA-INTERNAL-003
- **路径:** `POST /internal-api/waste/order/weighing-result`
- **描述:** 接收车辆过磅结果并执行订单分摊
- **请求参数:** `WeighingResultReqVO`
```json
{
  "vehicleWeighingId": 12345,
  "vehicleId": 101,
  "vehicleNo": "沪A12345",
  "orderIds": [67890, 67891, 67892],
  "netWeight": "4.9",
  "weighingTime": "2024-01-01 10:00:00",
  "weighingLocation": "XX回收站"
}
```
- **响应:**
```json
{
  "code": 0,
  "data": {
    "allocationResults": [
      {
        "orderId": 67890,
        "allocatedQuantity": "1.96",
        "allocationRatio": "0.4",
        "finalAmount": "980.00"
      }
    ]
  },
  "msg": ""
}
```

#### 记录现金支付

- **ID:** WTA-INTERNAL-004
- **路径:** `POST /internal-api/waste/order/{orderId}/cash-payment`
- **描述:** 记录司机的现金支付信息
- **请求参数:** `InternalCashPaymentReqVO`
```json
{
  "amount": "800.00",
  "driverId": 12345,
  "driverName": "王五",
  "paymentTime": "2024-01-01 09:00:00",
  "payeeName": "张三",
  "payeePhone": "13800138000",
  "paymentLocation": {
    "address": "XX市XX区",
    "latitude": "31.2304",
    "longitude": "121.4737"
  },
  "photos": ["receipt_url1"]
}
```
- **响应:** `CommonResult<Boolean>`

### 6.2 危废模块主动调用物流模块的接口

#### 创建运输任务

- **ID:** WTA-EXTERNAL-001
- **路径:** `POST /api/logistics/transport-task/create`
- **描述:** 订单确认后创建运输任务
- **请求参数:** `CreateTransportTaskReqVO`
```json
{
  "orderId": 67890,
  "orderNo": "WO202401010001",
  "wasteInfo": {
    "wasteCode": "HW08",
    "wasteName": "废矿物油",
    "quantity": "2.5",
    "unit": "吨",
    "packagingType": "桶装"
  },
  "pickupInfo": {
    "enterpriseId": 1001,
    "enterpriseName": "XX汽修厂",
    "address": "XX市XX区XX路XX号",
    "contactName": "张三",
    "contactPhone": "13800138000",
    "expectedTime": "2024-01-01 14:00:00"
  },
  "deliveryInfo": {
    "enterpriseId": 2001,
    "enterpriseName": "XX回收公司",
    "address": "XX市XX区XX路XX号",
    "contactName": "李四",
    "contactPhone": "13900139000"
  }
}
```
- **响应:** `CommonResult<Long>` (运输任务ID)

#### 查询运输状态

- **ID:** WTA-EXTERNAL-002
- **路径:** `GET /api/logistics/transport-task/{taskId}/status`
- **描述:** 查询运输任务的当前状态
- **响应:** `CommonResult<TransportTaskStatusRespVO>`

#### 取消运输任务

- **ID:** WTA-EXTERNAL-003
- **路径:** `PUT /api/logistics/transport-task/{taskId}/cancel`
- **描述:** 取消运输任务
- **请求参数:** `CancelTransportTaskReqVO`
```json
{
  "reason": "订单取消"
}
```
- **响应:** `CommonResult<Boolean>`

## 7. VO对象定义规范

### 7.1 请求对象 (ReqVO) 示例

#### AppointmentCreateReqVO
```java
@Data
@ApiModel("产废企业 - 创建预约请求VO")
public class AppointmentCreateReqVO {

    @ApiModelProperty(value = "产废门店ID", example = "1001")
    private Long producingStoreId;

    @ApiModelProperty(value = "产废方联系人", required = true, example = "张三")
    @NotBlank(message = "产废方联系人不能为空")
    @Size(max = 64, message = "联系人姓名长度不能超过64个字符")
    private String producingContactName;

    @ApiModelProperty(value = "产废方联系电话", required = true, example = "13800138000")
    @NotBlank(message = "联系电话不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String producingContactPhone;

    @ApiModelProperty(value = "产废方详细地址", required = true, example = "XX市XX区XX路XX号")
    @NotBlank(message = "详细地址不能为空")
    @Size(max = 255, message = "地址长度不能超过255个字符")
    private String producingAddressDetail;

    @ApiModelProperty(value = "危险废物代码", required = true, example = "HW08")
    @NotBlank(message = "危险废物代码不能为空")
    @Size(max = 50, message = "废物代码长度不能超过50个字符")
    private String wasteCode;

    @ApiModelProperty(value = "危险废物名称", required = true, example = "废矿物油")
    @NotBlank(message = "危险废物名称不能为空")
    @Size(max = 100, message = "废物名称长度不能超过100个字符")
    private String wasteName;

    @ApiModelProperty(value = "废物类别ID", example = "1001")
    private Long wasteCategoryId;

    @ApiModelProperty(value = "预估数量", required = true, example = "2.5")
    @NotNull(message = "预估数量不能为空")
    @DecimalMin(value = "0.01", message = "预估数量必须大于0")
    @DecimalMax(value = "999999.99", message = "预估数量不能超过999999.99")
    private BigDecimal estimatedQuantity;

    @ApiModelProperty(value = "数量单位", required = true, example = "吨")
    @NotBlank(message = "数量单位不能为空")
    @Size(max = 10, message = "单位长度不能超过10个字符")
    private String quantityUnit;

    @ApiModelProperty(value = "包装方式", example = "桶装")
    @Size(max = 50, message = "包装方式长度不能超过50个字符")
    private String packagingType;

    @ApiModelProperty(value = "期望收集开始时间", required = true, example = "2024-01-01 14:00:00")
    @NotNull(message = "期望收集开始时间不能为空")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime expectedCollectionTimeStart;

    @ApiModelProperty(value = "期望收集结束时间", required = true, example = "2024-01-01 18:00:00")
    @NotNull(message = "期望收集结束时间不能为空")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime expectedCollectionTimeEnd;

    @ApiModelProperty(value = "选定的回收企业ID", example = "2001")
    private Long selectedRecyclingEnterpriseId;

    @ApiModelProperty(value = "用户备注", example = "请提前联系")
    @Size(max = 255, message = "备注长度不能超过255个字符")
    private String userRemark;
}
```

### 7.2 响应对象 (RespVO) 示例

#### OrderDetailRespVO
```java
@Data
@ApiModel("订单详情响应VO")
public class OrderDetailRespVO {

    @ApiModelProperty(value = "订单ID", example = "1001")
    private Long id;

    @ApiModelProperty(value = "订单号", example = "WO202401010001")
    private String orderNo;

    @ApiModelProperty(value = "预约单ID", example = "1001")
    private Long appointmentId;

    @ApiModelProperty(value = "预约单号", example = "AP202401010001")
    private String appointmentNo;

    @ApiModelProperty(value = "产废企业信息")
    private EnterpriseSimpleRespVO producingEnterprise;

    @ApiModelProperty(value = "回收企业信息")
    private EnterpriseSimpleRespVO recyclingEnterprise;

    @ApiModelProperty(value = "物流企业信息")
    private EnterpriseSimpleRespVO logisticsEnterprise;

    @ApiModelProperty(value = "废物信息")
    private WasteInfoRespVO wasteInfo;

    @ApiModelProperty(value = "价格信息")
    private OrderPriceRespVO priceInfo;

    @ApiModelProperty(value = "业务状态", example = "1", notes = "0:待确认,1:已确认,2:待结算,3:已结算,4:已完成,5:已取消")
    private Integer businessStatus;

    @ApiModelProperty(value = "业务状态名称", example = "已确认")
    private String businessStatusName;

    @ApiModelProperty(value = "付款状态", example = "0", notes = "0:未付款,1:已付款,2:付款失败,3:待凭证上传,4:凭证已上传,5:凭证已确认")
    private Integer paymentStatus;

    @ApiModelProperty(value = "付款状态名称", example = "未付款")
    private String paymentStatusName;

    @ApiModelProperty(value = "订单来源类型", example = "0", notes = "0:预约转订单,1:扫街临时订单,2:补单")
    private Integer sourceType;

    @ApiModelProperty(value = "订单来源类型名称", example = "预约转订单")
    private String sourceTypeName;

    @ApiModelProperty(value = "物流状态", example = "2", notes = "0:待分配,1:待收运,2:运输中,3:已送达,4:已过磅")
    private Integer logisticsStatus;

    @ApiModelProperty(value = "物流状态名称", example = "运输中")
    private String logisticsStatusName;

    @ApiModelProperty(value = "分摊信息")
    private OrderAllocationRespVO allocationInfo;

    @ApiModelProperty(value = "付款配置信息")
    private PaymentConfigSimpleRespVO paymentConfig;

    @ApiModelProperty(value = "对公付款凭证")
    private PaymentVoucherRespVO paymentVoucher;

    @ApiModelProperty(value = "用户备注", example = "请提前联系")
    private String userRemark;

    @ApiModelProperty(value = "内部备注", example = "特殊处理")
    private String internalRemark;

    @ApiModelProperty(value = "创建时间", example = "2024-01-01 10:00:00")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "更新时间", example = "2024-01-01 15:30:00")
    private LocalDateTime updateTime;
}
```

## 8. 错误码定义

```java
public interface WasteTransferErrorCodeConstants {
    
    // 预约相关错误码 (1-050-001-000 ~ 1-050-001-999)
    ErrorCode APPOINTMENT_NOT_EXISTS = new ErrorCode(1050001001, "预约单不存在");
    ErrorCode APPOINTMENT_STATUS_NOT_ALLOW_CANCEL = new ErrorCode(1050001002, "当前预约状态不允许取消");
    ErrorCode APPOINTMENT_NOT_BELONG_TO_USER = new ErrorCode(1050001003, "预约单不属于当前用户");
    ErrorCode APPOINTMENT_RECYCLER_ALREADY_ASSIGNED = new ErrorCode(1050001004, "预约单已分配回收企业");
    
    // 订单相关错误码 (1-050-002-000 ~ 1-050-002-999)
    ErrorCode ORDER_NOT_EXISTS = new ErrorCode(1050002001, "订单不存在");
    ErrorCode ORDER_STATUS_NOT_ALLOW_UPDATE = new ErrorCode(1050002002, "当前订单状态不允许修改");
    ErrorCode ORDER_NOT_BELONG_TO_USER = new ErrorCode(1050002003, "订单不属于当前用户");
    ErrorCode ORDER_ALLOCATION_NOT_COMPLETED = new ErrorCode(1050002004, "订单分摊尚未完成");
    
    // 分配规则相关错误码 (1-050-003-000 ~ 1-050-003-999)
    ErrorCode ASSIGNMENT_RULE_NOT_EXISTS = new ErrorCode(1050003001, "分配规则不存在");
    ErrorCode ASSIGNMENT_RULE_PRIORITY_DUPLICATE = new ErrorCode(1050003002, "分配规则优先级重复");
    
    // 价格相关错误码 (1-050-004-000 ~ 1-050-004-999)
    ErrorCode PRICE_CONFIG_NOT_EXISTS = new ErrorCode(1050004001, "价格配置不存在");
    ErrorCode PRICE_EFFECTIVE_DATE_INVALID = new ErrorCode(1050004002, "价格生效日期无效");
    
    // 付款相关错误码 (1-050-005-000 ~ 1-050-005-999)
    ErrorCode PAYMENT_CONFIG_NOT_EXISTS = new ErrorCode(1050005001, "付款配置不存在");
    ErrorCode PAYMENT_VOUCHER_NOT_EXISTS = new ErrorCode(1050005002, "付款凭证不存在");
    ErrorCode PAYMENT_AMOUNT_NOT_MATCH = new ErrorCode(1050005003, "付款金额与订单金额不匹配");
}
```

## 9. 接口优化建议

### 9.1 性能优化
1. **分页查询优化**：对于大数据量的分页查询，建议使用游标分页或深度分页优化
2. **缓存策略**：对于频繁查询的企业信息、价格配置等，建议使用Redis缓存
3. **批量操作**：提供批量确认、批量分配等接口，减少网络请求次数

### 9.2 安全性增强
1. **数据权限**：严格校验用户只能操作属于自己企业的数据
2. **操作日志**：记录所有关键操作的审计日志
3. **接口限流**：对于创建类接口实施限流保护

### 9.3 用户体验优化
1. **状态机验证**：在状态变更前进行严格的状态机验证
2. **友好错误提示**：提供详细的错误信息和解决建议
3. **实时通知**：关键状态变更时推送实时通知

### 9.4 扩展性考虑
1. **版本控制**：为API接口设计版本控制机制
2. **插件化设计**：价格计算、分配算法等支持插件化扩展
3. **多租户支持**：确保所有接口都支持多租户隔离

## 10. 总结

本API接口文档经过全面复审和改进，主要增强了以下方面：

1. **完善了VO对象定义**：提供了详细的请求和响应对象示例，包含完整的字段校验注解
2. **补充了缺失接口**：增加了价格管理、付款配置、过磅分摊等核心功能接口
3. **优化了接口设计**：统一了命名规范，完善了错误处理机制
4. **增强了安全性**：加强了数据权限校验和操作审计
5. **提升了可维护性**：提供了清晰的模块间协作接口定义

该接口文档现已能够完整支撑危险废物转移模块的所有业务需求，为后续的开发实施提供了详细的技术规范。 