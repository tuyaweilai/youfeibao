# 工行反向发票API集成指南

## 1. 概述

本文档详细说明了工行反向发票API的集成要求，包括接口列表、参数说明、签名规则、加密要求等。工行反向发票是专门为报废产品收购、农产品收购等特定场景设计的支付+开票一体化解决方案。

## 2. 接口环境

### 2.1 环境地址
- **测试环境**：https://apipcs3.dccnet.com.cn/api
- **生产环境**：https://api.icbc.com.cn/api

### 2.2 接口认证
- **AppID**：由工行分配的应用标识
- **AppSecret**：用于签名的密钥
- **AES密钥**：用于敏感信息加密

## 3. 通用规范

### 3.1 请求格式
```json
{
  "appId": "应用ID",
  "msgId": "消息ID（UUID）",
  "timestamp": "时间戳（毫秒）",
  "sign": "签名",
  "encryptType": "AES",
  "bizContent": "加密后的业务数据"
}
```

### 3.2 响应格式
```json
{
  "returnCode": "0",
  "returnMsg": "成功",
  "msgId": "消息ID",
  "sign": "签名",
  "bizContent": "加密后的业务数据"
}
```

### 3.3 签名规则
1. 将所有参数按照参数名ASCII码从小到大排序
2. 拼接成key1=value1&key2=value2格式
3. 在最后拼接&key=AppSecret
4. 使用MD5加密并转大写

```java
public String generateSign(Map<String, String> params, String appSecret) {
    // 1. 参数排序
    TreeMap<String, String> sortedParams = new TreeMap<>(params);
    
    // 2. 拼接参数
    StringBuilder sb = new StringBuilder();
    for (Map.Entry<String, String> entry : sortedParams.entrySet()) {
        if (!"sign".equals(entry.getKey()) && StringUtils.isNotBlank(entry.getValue())) {
            sb.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
        }
    }
    
    // 3. 拼接密钥
    sb.append("key=").append(appSecret);
    
    // 4. MD5加密
    return DigestUtils.md5Hex(sb.toString()).toUpperCase();
}
```

### 3.4 AES加密规则
- 算法：AES/ECB/PKCS5Padding
- 密钥长度：128位
- 编码：Base64

## 4. 核心接口说明

### 4.1 收方新增接口

#### 接口地址
POST /reverse/receiver/add

#### 业务参数（bizContent解密后）
```json
{
  "outUserId": "外部用户编号（最长20位）",
  "receiverType": "3",  // 1:企业 3:自然人
  "receiverName": "收方户名",
  "receiverAccount": "收方账号",
  "idType": "0",  // 0:身份证
  "idNo": "证件号码",
  "mobile": "手机号",
  "occupation": "001",  // 职业代码
  "address": "常用住址"
}
```

#### 响应参数
```json
{
  "outUserId": "外部用户编号",
  "receiverStatus": "1",  // 0:不可用 1:可用
  "mediumId": "工行电子账户账号",
  "openacctStatus": "01"  // 00:初始 01:开户中 02:开户成功 03:开户失败
}
```

### 4.2 收方查询接口

#### 接口地址
POST /reverse/receiver/query

#### 业务参数
```json
{
  "receiverAccount": "收方账号",
  "idNo": "证件号码"
}
```

#### 响应参数
```json
{
  "receiverList": [{
    "outUserId": "外部用户编号",
    "receiverName": "收方户名",
    "receiverAccount": "收方账号",
    "receiverStatus": "1",
    "mediumId": "工行电子账户账号",
    "openacctStatus": "02"
  }]
}
```

### 4.3 反向开票预下单接口

#### 接口地址
POST /reverse/invoice/preorder

#### 业务参数
```json
{
  "outOrderId": "外部订单号（最长64位）",
  "outUserId": "付方编号",
  "outVendorId": "收方编号",
  "orderAmount": "1580.50",  // 订单金额
  "projectName": "废矿物油",  // 商品名称
  "goodsNum": "2.5",  // 商品数量
  "goodsUnit": "吨",  // 商品单位
  "specificElements": "24",  // 特定要素 24:报废产品收购
  "notifyUrl": "https://xxx/callback/invoice",  // 开票回调地址
  "returnUrl": "https://xxx/return"  // 前端跳转地址
}
```

#### 响应参数
```json
{
  "outOrderId": "外部订单号",
  "icbcOrderId": "工行订单号",
  "invoiceUrl": "https://xxx",  // 用户确认开票的H5页面
  "expireTime": "2024-01-01 12:00:00"  // 预下单失效时间
}
```

### 4.4 预下单查询接口

#### 接口地址
POST /reverse/invoice/prequery

#### 业务参数
```json
{
  "outOrderId": "外部订单号"
}
```

#### 响应参数
```json
{
  "outOrderId": "外部订单号",
  "icbcOrderId": "工行订单号",
  "orderStatus": "2",  // 1:待确认 2:已确认 3:已取消 4:已失效
  "invoiceStatus": "1",  // 0:未开票 1:已开票 2:开票失败
  "invoiceCode": "发票代码",
  "invoiceNumber": "发票号码",
  "invoiceDate": "2024-01-01",
  "invoiceAmount": "1580.50"
}
```

### 4.5 付方支付接口

#### 接口地址
POST /reverse/payment/pay

#### 业务参数
```json
{
  "outOrderId": "外部订单号",
  "icbcOrderId": "工行订单号",
  "payAmount": "1580.50",
  "payerAccount": "付方账号",
  "payerName": "付方名称",
  "receiverAccount": "收方账号",
  "receiverName": "收方名称",
  "remark": "危废转移订单付款"
}
```

#### 响应参数
```json
{
  "outOrderId": "外部订单号",
  "icbcPayId": "工行支付流水号",
  "payStatus": "1",  // 0:处理中 1:成功 2:失败
  "payTime": "2024-01-01 10:30:00",
  "payUrl": "https://xxx"  // 需要跳转的支付页面（如需要）
}
```

### 4.6 支付状态查询接口

#### 接口地址
POST /reverse/payment/query

#### 业务参数
```json
{
  "outOrderId": "外部订单号",
  "icbcPayId": "工行支付流水号"
}
```

#### 响应参数
```json
{
  "outOrderId": "外部订单号",
  "icbcPayId": "工行支付流水号",
  "payStatus": "1",
  "payTime": "2024-01-01 10:30:00",
  "payAmount": "1580.50",
  "failReason": ""  // 失败原因
}
```

### 4.7 发票下载接口

#### 接口地址
POST /reverse/invoice/download

#### 业务参数
```json
{
  "invoiceCode": "发票代码",
  "invoiceNumber": "发票号码"
}
```

#### 响应参数
```json
{
  "invoiceCode": "发票代码",
  "invoiceNumber": "发票号码",
  "fileType": "PDF",  // PDF/OFD
  "fileContent": "Base64编码的文件内容"
}
```

## 5. 回调通知

### 5.1 开票状态回调

#### 回调数据格式
```json
{
  "notifyType": "INVOICE_STATUS",
  "outOrderId": "外部订单号",
  "icbcOrderId": "工行订单号",
  "result": "pass",  // pass:通过 reject:拒绝
  "invoiceCode": "发票代码",
  "invoiceNumber": "发票号码",
  "invoiceDate": "2024-01-01",
  "invoiceAmount": "1580.50",
  "rejectReason": ""  // 拒绝原因
}
```

### 5.2 支付状态回调

#### 回调数据格式
```json
{
  "notifyType": "PAYMENT_STATUS",
  "outOrderId": "外部订单号",
  "icbcPayId": "工行支付流水号",
  "payStatus": "1",  // 1:成功 2:失败
  "payTime": "2024-01-01 10:30:00",
  "payAmount": "1580.50",
  "failReason": ""
}
```

### 5.3 收方审核回调

#### 回调数据格式
```json
{
  "notifyType": "RECEIVER_AUDIT",
  "outUserId": "外部用户编号",
  "auditStatus": "1",  // 1:通过 2:拒绝
  "auditTime": "2024-01-01 10:00:00",
  "rejectReason": "",
  "mediumId": "工行电子账户账号"
}
```

## 6. 错误码说明

| 错误码 | 错误描述 | 处理建议 |
|--------|----------|----------|
| 0 | 成功 | - |
| 1001 | 参数错误 | 检查必填参数 |
| 1002 | 签名错误 | 检查签名算法 |
| 1003 | 解密失败 | 检查AES密钥 |
| 2001 | 收方不存在 | 先调用收方新增 |
| 2002 | 收方审核中 | 等待审核完成 |
| 2003 | 收方审核拒绝 | 联系工行处理 |
| 3001 | 预下单不存在 | 检查订单号 |
| 3002 | 预下单已失效 | 重新创建预下单 |
| 3003 | 发票开具失败 | 查看具体原因 |
| 4001 | 余额不足 | 检查付方账户 |
| 4002 | 支付失败 | 查看具体原因 |
| 5001 | 系统繁忙 | 稍后重试 |

## 7. 集成注意事项

### 7.1 安全要求
1. **密钥管理**：AppSecret和AES密钥必须安全存储，不能硬编码
2. **HTTPS通信**：所有接口调用必须使用HTTPS
3. **IP白名单**：生产环境需要配置IP白名单
4. **敏感信息**：身份证、银行账号等敏感信息必须加密传输

### 7.2 业务要求
1. **收方入驻**：首次支付前必须完成收方入驻
2. **开票确认**：预下单后需要收方确认开票信息
3. **订单有效期**：预下单有效期为24小时
4. **幂等性**：所有接口支持幂等，可以安全重试

### 7.3 性能要求
1. **超时设置**：建议设置30秒超时
2. **重试机制**：网络异常可重试3次
3. **并发限制**：单商户QPS限制100
4. **批量处理**：暂不支持批量接口

### 7.4 测试要求
1. **测试账号**：使用工行提供的测试账号
2. **测试数据**：测试环境数据每日清理
3. **联调测试**：上线前需要完成联调测试
4. **压力测试**：大促前需要进行压力测试

## 8. 常见问题

### 8.1 收方入驻相关
**Q：收方入驻需要多久？**
A：一般1-2个工作日完成审核，可通过查询接口或等待回调获取结果。

**Q：一个身份证可以绑定多个银行账号吗？**
A：可以，但需要分别进行入驻。

### 8.2 开票相关
**Q：开票信息可以修改吗？**
A：预下单后不能修改，如需修改需要取消后重新下单。

**Q：发票什么时候生成？**
A：收方确认开票信息后立即生成电子发票。

### 8.3 支付相关
**Q：支付失败如何处理？**
A：可以重新发起支付，使用同一个预下单。

**Q：支持部分支付吗？**
A：不支持，必须全额支付。

## 9. 联系方式

- **技术支持邮箱**：icbc-api@icbc.com.cn
- **技术支持电话**：95588
- **工单系统**：https://developer.icbc.com.cn 