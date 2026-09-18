# 工行付方支付模块

## 模块概述

工行付方支付模块实现了企业（付方）向个人（收方）的支付功能，基于工商银行聚富通开票付方支付接口。该模块支持企业通过工行网银向个人账户进行货款支付，并提供支付状态查询和回调通知处理功能。

## 技术架构

### 核心组件

1. **控制器层 (Controller)**
   - `PaymentController`: 提供付方支付相关的REST API接口

2. **服务层 (Service)**
   - `PaymentService`: 支付业务逻辑接口
   - `PaymentServiceImpl`: 支付业务逻辑实现

3. **数据访问层 (DAL)**
   - `PaymentOrderDO`: 支付订单数据实体
   - `PaymentOrderMapper`: 支付订单数据访问接口

4. **视图对象 (VO)**
   - `PaymentReqVO`: 支付请求参数
   - `PaymentRespVO`: 支付响应结果
   - `PaymentStatusQueryReqVO`: 支付状态查询请求
   - `PaymentStatusQueryRespVO`: 支付状态查询响应

5. **对象转换器 (Convert)**
   - `PaymentConvert`: VO与DO之间的对象转换

### 数据库设计

#### 支付订单表 (icbc_payment_order)

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | bigint | 主键ID |
| order_no | varchar(64) | 支付订单号 |
| partner_order_id | varchar(35) | 合作方订单ID |
| icbc_order_no | varchar(64) | 工行订单号 |
| payee_no | varchar(40) | 收方编号 |
| payer_no | varchar(20) | 付方编号 |
| payment_amount | decimal(10,2) | 支付金额 |
| payment_status | tinyint | 支付状态 |
| payment_time | datetime | 支付时间 |
| payment_serial_no | varchar(64) | 支付流水号 |
| verified_code | varchar(30) | 机构编码 |
| ukey_id | varchar(24) | U盾ID |
| redirect_url | text | 支付页面重定向URL |
| msg_id | varchar(32) | 消息通讯唯一编号 |
| error_code | varchar(20) | 错误码 |
| error_msg | varchar(500) | 错误信息 |
| remark | varchar(500) | 备注 |

#### 支付状态说明

- 0: 待支付
- 1: 支付中
- 2: 支付成功
- 3: 支付失败
- 4: 已取消

## API接口

### 1. 创建付方支付

**接口地址**: `POST /admin-api/icbc/payment/create`

**请求参数**:
```json
{
  "appId": "$(ICBC_APP_ID)",
  "outOrderId": "2018040908",
  "outVendorId": "010020200513111111",
  "outUserId": "10000000000000003",
  "verifiedCode": "20201128531215026",
  "ukeyId": "20201128531215026"
}
```

**响应结果**:
```json
{
  "code": 0,
  "data": {
    "returnCode": "0",
    "returnMsg": "成功",
    "redirectUrl": "https://gw.open.icbc.com.cn/ui/jft/ui/invoice/pay/V1?appId=...",
    "msgId": "urcnl24ciutr9",
    "outOrderId": "2018040908",
    "icbcOrderNo": "ICBC202312010001",
    "paymentStatus": "PENDING"
  },
  "msg": "操作成功"
}
```

### 2. 查询支付状态

**接口地址**: `GET /admin-api/icbc/payment/query`

**请求参数**:
```
outOrderId=2018040908
```

**响应结果**:
```json
{
  "code": 0,
  "data": {
    "returnCode": "0",
    "returnMsg": "成功",
    "outOrderId": "2018040908",
    "icbcOrderNo": "ICBC202312010001",
    "paymentStatus": "SUCCESS",
    "paymentAmount": 1000.00,
    "paymentTime": "2023-12-01T10:30:00",
    "outVendorId": "010020200513111111",
    "outUserId": "10000000000000003",
    "paymentSerialNo": "PAY202312010001"
  },
  "msg": "操作成功"
}
```

### 3. 支付回调通知

**接口地址**: `POST /admin-api/icbc/payment/notify`

**请求参数**: 工行回调的原始数据

**响应结果**:
```json
{
  "code": 0,
  "data": true,
  "msg": "操作成功"
}
```

### 4. 生成支付页面URL

**接口地址**: `GET /admin-api/icbc/payment/generate-url`

**请求参数**: 与创建支付相同

**响应结果**:
```json
{
  "code": 0,
  "data": "https://gw.open.icbc.com.cn/ui/jft/ui/invoice/pay/V1?appId=...",
  "msg": "操作成功"
}
```

## 配置说明

### 工行接口配置

在 `application.yml` 中配置工行接口相关参数：

```yaml
icbc:
  api:
    app-id: "$(ICBC_APP_ID)"
    out-vendor-id: "$(ICBC_OUT_VENDOR_ID)"
    private-key: "your-rsa-private-key"
    apigw-public-key: "icbc-rsa-public-key"
    aes-key: "$(ICBC_AES_KEY)"
    base-url: "https://gw.open.icbc.com.cn"
    payment-url: "https://gw.open.icbc.com.cn/ui/jft/ui/invoice/pay/V1"
    payment-query-url: "https://gw.open.icbc.com.cn/api/jft/api/payment/queryPaymentStatus/V1"
    timeout: 30000
```

## 业务流程

### 支付流程

1. **创建支付订单**
   - 验证请求参数
   - 检查订单是否已存在
   - 生成支付订单号和消息ID
   - 创建支付订单记录
   - 生成支付页面重定向URL

2. **用户支付**
   - 用户通过重定向URL跳转到工行支付页面
   - 用户在工行页面完成支付操作

3. **支付结果处理**
   - 工行通过回调接口通知支付结果
   - 系统更新支付订单状态
   - 发送业务通知

4. **支付状态查询**
   - 提供主动查询接口
   - 返回最新的支付状态信息

### 错误处理

系统定义了完整的错误码体系：

- `1_030_004_000`: 支付订单不存在
- `1_030_004_001`: 订单状态不允许支付
- `1_030_004_002`: 支付金额错误
- `1_030_004_003`: 支付失败
- `1_030_004_004`: 支付订单已存在
- `1_030_004_005`: 支付参数错误

## 安全考虑

1. **参数验证**
   - 使用JSR-303注解进行参数校验
   - 验证必填字段和字段长度

2. **权限控制**
   - 使用Spring Security进行接口权限控制
   - 需要相应的权限才能访问接口

3. **数据安全**
   - 敏感信息加密存储
   - 支付金额使用BigDecimal避免精度问题

4. **接口安全**
   - 支持工行接口的签名验证
   - 支持数据加密传输

## 部署说明

### 数据库初始化

执行以下SQL脚本创建支付订单表：

```sql
-- 执行 sql/mysql/icbc_payment_order.sql
```

### 配置检查

1. 确保工行接口配置正确
2. 确保数据库连接正常
3. 确保相关权限配置正确

### 测试验证

1. 运行单元测试验证功能正常
2. 使用测试环境验证接口调用
3. 验证支付流程完整性

## 故障排查

### 常见问题

1. **支付订单创建失败**
   - 检查参数是否正确
   - 检查订单是否已存在
   - 检查数据库连接

2. **支付状态查询失败**
   - 检查订单ID是否正确
   - 检查订单是否存在
   - 检查工行接口配置

3. **回调处理失败**
   - 检查回调数据格式
   - 检查签名验证
   - 检查业务逻辑处理

### 日志查看

- 应用日志：`~/logs/yudao-server.log`
- 错误日志：`~/logs/yudao-server-error.log`
- 支付相关日志搜索关键字：`PaymentService`、`PaymentController`

## 扩展功能

### 后续可扩展的功能

1. **批量支付**
   - 支持一次性向多个收方支付

2. **支付限额控制**
   - 设置单笔和日累计支付限额

3. **支付审批流程**
   - 大额支付需要审批

4. **支付统计报表**
   - 支付金额统计
   - 支付成功率统计

5. **自动对账**
   - 与工行对账文件自动对账

## 版本历史

- v1.0.0: 初始版本，实现基础的付方支付功能
  - 支付订单创建
  - 支付状态查询
  - 支付回调处理
  - 支付URL生成 