# 工行反向开票预下单模块

## 模块概述

本模块实现了工行反向开票预下单功能，支持农产品收购和报废产品收购两种业务类型的发票开具。

## 功能特性

### 1. 预下单功能
- 支持创建反向开票预下单
- 自动生成订单号和商品明细
- 完整的参数校验和业务规则验证
- 支持跳转到工行开票页面

### 2. 查询功能
- 支持根据合作方订单ID查询开票信息
- 实时同步工行接口状态
- 提供完整的订单状态信息

### 3. 业务类型支持
- **农产品收购** (特定要素16)：需要机构编码和U盾ID
- **报废产品收购** (特定要素24)：标准流程

## 技术架构

### 核心组件

```
├── controller/admin/invoice/
│   ├── InvoiceOrderController.java          # 控制器层
│   └── vo/
│       ├── InvoicePreOrderReqVO.java        # 预下单请求VO
│       ├── InvoicePreOrderRespVO.java       # 预下单响应VO
│       ├── InvoiceQueryReqVO.java           # 查询请求VO
│       └── InvoiceQueryRespVO.java          # 查询响应VO
├── service/invoice/
│   ├── InvoiceOrderService.java             # 服务接口
│   └── impl/
│       └── InvoiceOrderServiceImpl.java     # 服务实现
├── dal/
│   ├── dataobject/invoice/
│   │   ├── InvoiceOrderDO.java              # 订单实体
│   │   └── OrderItemDO.java                 # 商品明细实体
│   └── mysql/invoice/
│       ├── InvoiceOrderMapper.java          # 订单Mapper
│       └── OrderItemMapper.java             # 商品明细Mapper
└── convert/invoice/
    └── InvoiceOrderConvert.java             # 对象转换器
```

### 数据库设计

#### 订单表 (icbc_invoice_order)
- 存储订单基本信息、状态、发票信息
- 支持多种状态跟踪：订单状态、开票状态、支付状态、缴税状态

#### 商品明细表 (icbc_order_item)
- 存储订单商品详细信息
- 支持税额计算和商品分类

## API接口

### 1. 创建预下单
```http
POST /admin-api/icbc/invoice-order/pre-order
Content-Type: application/json

{
  "outOrderId": "2018040908",
  "outVendorId": "010020200513111111",
  "outUserId": "10000000000000003",
  "orderAmount": 1000.00,
  "invoiceType": "02",
  "specificElements": "24",
  "buyerInvTypeCode": "04",
  "naturalPersonName": "张三",
  "cardType": "111",
  "cardNumber": "110101199001011234",
  "sellerAddress": "北京市朝阳区",
  "sellerTelephone": "13800138000",
  "taxpayerNo": "91110000123456789X",
  "taxpayerName": "北京某某有限公司",
  "drawerName": "李四",
  "drawerCardType": "111",
  "drawerCardNumber": "110101199001011234",
  "areaCode": "110000",
  "mac": "00:11:22:33:44:55",
  "taxRate": 0.13,
  "payJumpUrl": "https://example.com/pay/return",
  "invoiceNotifyUrl": "https://example.com/invoice/notify",
  "goodsInfo": [
    {
      "goodsSeqno": "1",
      "projectName": "废铁回收",
      "goodsNum": 100,
      "goodsAmt": 1000.00,
      "price": 10.00,
      "units": "吨",
      "taxRate": 0.13,
      "mergedCode": "1090101010000000000"
    }
  ]
}
```

### 2. 查询开票信息
```http
GET /admin-api/icbc/invoice-order/query?outOrderId=2018040908
```

## 配置说明

### 工行接口配置
在 `application.yml` 中配置：

```yaml
icbc:
  api:
    app-id: "$(ICBC_APP_ID)"
    private-key: "your-rsa-private-key"
    apigw-public-key: "icbc-api-gateway-public-key"
    aes-key: "$(ICBC_AES_KEY)"
    sign-type: "RSA2"
    encrypt-type: "AES"
    pre-order-url: "https://gw.open.icbc.com.cn/ui/jft/ui/invoice/pre/order/V1"
    invoice-query-url: "https://gw.open.icbc.com.cn/api/jft/api/invoice/queryInvoiceInfo/V1"
```

## 业务规则

### 1. 参数验证规则
- 特定要素与收购发票类型必须匹配：
  - 农产品收购(16) → 农产品收购发票(01)
  - 报废产品收购(24) → 报废产品收购发票(04)
- 农产品收购必须提供机构编码和U盾ID
- 商品金额总和必须等于订单总金额

### 2. 状态管理
- **订单状态**：待确认 → 已确认 → 已支付 → 已开票 → 已完成
- **开票状态**：未开票 → 开票中 → 开票成功/失败
- **支付状态**：未支付 → 支付中 → 支付成功/失败
- **缴税状态**：未缴税 → 缴税中 → 缴税成功/失败

### 3. 税额计算
```java
// 税额 = 金额 * 税率 / (1 + 税率)
BigDecimal taxAmount = goodsAmount
    .multiply(taxRate)
    .divide(BigDecimal.ONE.add(taxRate), 2, BigDecimal.ROUND_HALF_UP);
```

## 安全考虑

1. **参数校验**：使用JSR-303注解进行完整的参数验证
2. **权限控制**：基于Spring Security的方法级权限控制
3. **数据加密**：敏感数据使用AES加密传输
4. **签名验证**：使用RSA2算法进行接口签名

## 测试

### 单元测试
```bash
# 运行单元测试
mvn test -Dtest=InvoiceOrderServiceTest
```

### 集成测试
1. 配置测试环境的工行接口参数
2. 使用测试数据进行完整流程测试
3. 验证数据库记录和状态更新

## 部署说明

### 1. 数据库初始化
```sql
-- 执行建表脚本
source sql/mysql/icbc_invoice_tables.sql;
```

### 2. 配置文件
确保生产环境配置了正确的工行接口参数和密钥。

### 3. 权限配置
为相关角色分配工行反向开票的操作权限。

## 常见问题

### Q1: 预下单失败，提示"订单已存在"
**A**: 检查合作方订单ID是否重复，每个订单ID只能使用一次。

### Q2: 农产品收购提示缺少机构编码
**A**: 农产品收购(特定要素16)必须提供verifiedCode和ukeyId参数。

### Q3: 商品金额校验失败
**A**: 确保所有商品金额总和等于订单总金额，精确到分。

## 更新日志

### v1.0.0 (2023-12-01)
- ✅ 实现预下单功能
- ✅ 实现查询功能  
- ✅ 完整的参数校验
- ✅ 数据库设计和实现
- ✅ 单元测试覆盖

## 后续计划

1. 集成真实的工行API调用
2. 实现发票文件下载功能
3. 添加订单状态变更通知
4. 优化错误处理和重试机制 