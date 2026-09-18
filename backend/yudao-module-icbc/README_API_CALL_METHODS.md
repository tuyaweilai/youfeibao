# 工行接口调用方式说明

本文档介绍了工行聚富通智慧清分收方查询接口的两种调用方式：官方SDK调用和手动HTTP调用。

## 1. 官方SDK调用方式（原有方式）

### 接口地址
```
GET /icbc/test/user-query
```

### 特点
- ✅ 使用工行官方提供的SDK
- ✅ 封装度高，使用简单
- ✅ 官方维护，稳定性好
- ❌ 调试困难，无法直接查看HTTP请求细节
- ❌ 依赖SDK版本，升级可能有兼容性问题

### 实现原理
```java
// 使用官方SDK的DefaultIcbcClient
DefaultIcbcClient client = createDefaultClient();
JftApiUserEdpreceiveQueryRequestV1 request = new JftApiUserEdpreceiveQueryRequestV1();
// ... 设置请求参数
JftApiUserEdpreceiveQueryResponseV1 response = client.execute(request, msgId);
```

### 测试示例
```bash
curl "http://localhost:48080/admin-api/icbc/test/user-query?outUserId=test123&receiverAccount=6214760200004864721&businessType=0004"
```

## 2. 手动HTTP调用方式（新增方式）

### 接口地址
```
GET /icbc/test/user-query-manual
```

### 特点
- ✅ 完全绕开官方SDK
- ✅ 可以完全控制HTTP请求过程
- ✅ 便于调试，可以查看完整的请求和响应
- ✅ 不依赖SDK版本
- ❌ 需要手动处理签名、参数构建等细节
- ❌ 维护成本相对较高

### 实现原理
```java
// 手动构建HTTP POST请求
Map<String, String> requestParams = new LinkedHashMap<>();
requestParams.put("app_id", icbcProperties.getAppId());
requestParams.put("msg_id", msgId);
requestParams.put("format", "json");
requestParams.put("charset", "UTF-8");
requestParams.put("sign_type", "RSA2");
requestParams.put("timestamp", timestamp);
requestParams.put("biz_content", bizContentJson);

// 生成签名
String signature = generateRSASignature(buildSignContent(requestParams));
requestParams.put("sign", signature);

// 发起HTTP POST请求
String responseBody = sendHttpPostRequest(url, requestParams);
```

### 测试示例
```bash
curl "http://localhost:48080/admin-api/icbc/test/user-query-manual?outUserId=test123&receiverAccount=6214760200004864721&businessType=0004"
```

## 3. 两种方式的对比

| 特性 | 官方SDK方式 | 手动HTTP方式 |
|------|-------------|--------------|
| 实现复杂度 | 简单 | 中等 |
| 调试便利性 | 困难 | 容易 |
| 请求可见性 | 低 | 高 |
| 维护成本 | 低 | 中等 |
| 灵活性 | 低 | 高 |
| 稳定性 | 高 | 中等 |
| 升级兼容性 | 可能有问题 | 无问题 |

## 4. 日志输出对比

### 官方SDK方式日志
```
2024-12-12 09:30:00.123 INFO  - 使用工行官方SDK标准方式（POST）进行调用
2024-12-12 09:30:00.124 INFO  - 生成的请求标识: msgId=MSG_1734000000123_abc12345, timestamp=2024-12-12 09:30:00
```

### 手动HTTP方式日志
```
2024-12-12 09:30:00.123 INFO  - === 开始手动HTTP调用聚富通智慧清分收方查询接口 ===
2024-12-12 09:30:00.124 INFO  - 参数: outUserId=test123, receiverAccount=6214760200004864721, businessType=0004
2024-12-12 09:30:00.125 INFO  - 生成的请求标识: msgId=MSG_1734000000125_def67890, timestamp=2024-12-12 09:30:00
2024-12-12 09:30:00.126 INFO  - 业务内容JSON: {"appId":"$(ICBC_APP_ID)","outUserId":"test123","receiverAccount":"6214760200004864721","businessType":"0004"}
2024-12-12 09:30:00.127 INFO  - 签名原始字符串: app_id=$(ICBC_APP_ID)&biz_content={"appId":"$(ICBC_APP_ID)",...}&charset=UTF-8&format=json&msg_id=MSG_1734000000125_def67890&sign_type=RSA2&timestamp=2024-12-12 09:30:00
2024-12-12 09:30:00.128 INFO  - 生成的签名: ABC123...XYZ789
2024-12-12 09:30:00.129 INFO  - 发起HTTP POST请求到: https://lahw.baibaitan.com/icbc-api/api/jft/api/user/edpreceive/query/V1
2024-12-12 09:30:00.130 INFO  - POST数据: app_id=$(ICBC_APP_ID)&msg_id=MSG_1734000000125_def67890&...
2024-12-12 09:30:00.200 INFO  - HTTP响应码: 200
2024-12-12 09:30:00.201 INFO  - HTTP响应内容: {"return_code":"10100000","return_msg":"成功",...}
```

## 5. 使用建议

### 开发阶段
- **推荐使用手动HTTP方式**，便于调试和问题排查
- 可以清楚地看到每个请求参数和响应内容
- 便于理解工行接口的具体要求

### 生产环境
- **可以选择官方SDK方式**，稳定性更好
- 或者继续使用手动HTTP方式，但需要加强错误处理和监控

### 问题排查
- **优先使用手动HTTP方式**，可以获得更详细的调试信息
- 对比两种方式的请求参数，确保一致性

## 6. 注意事项

1. **两种方式使用相同的配置**：都从 `IcbcProperties` 读取配置信息
2. **签名算法一致**：都使用相同的RSA2签名算法
3. **时间戳格式一致**：都使用中国标准时间（Asia/Shanghai）
4. **字符编码一致**：都使用UTF-8编码
5. **错误处理**：手动HTTP方式提供了更详细的错误信息

## 7. 扩展性

基于手动HTTP调用方式，可以很容易地：
- 添加更多的调试信息
- 实现自定义的重试机制
- 添加请求/响应的缓存
- 实现更灵活的错误处理
- 支持更多的HTTP配置选项

这两种方式可以并存，根据具体需求选择使用。 