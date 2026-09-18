# 工商银行接口测试指南

## 概述

本文档提供了工商银行反向开票模块的完整测试指南，包括JDK配置、签名验证和接口测试。

## 环境要求

### JDK版本
- **项目JDK版本**: Java 1.8 (JDK 8)
- **工行SDK兼容性**: 支持 Java 1.5 及以上版本
- **签名工具包**: 兼容 JDK 14+ (InfosecCrypto_Java1_02_JDK14+.jar)

### 依赖检查
```bash
# 检查JDK版本
java -version
# 应该显示: java version "1.8.0_xxx"

# 检查Maven版本
mvn -version
```

## 配置说明

### 1. 从测试代码提取的配置

根据提供的 `IcbcBankInterfaceImpl.java` 测试代码，我们提取了以下关键配置：

```yaml
icbc:
  api:
    app-id: $(ICBC_APP_ID)              # 应用ID
    out-vendor-id: $(ICBC_OUT_VENDOR_ID)       # 子商户标识
    aes-key: $(ICBC_AES_KEY)         # AES加密密钥
    sign-type: RSA2                           # 签名算法
    private-key: |                            # RSA私钥（完整）
      $(ICBC_PRIVATE_KEY)...
    apigw-public-key: |                       # 工行API网关公钥
      $(ICBC_APIGW_PUBLIC_KEY)...
```

### 2. 配置文件设置

将配置添加到 `application.yml` 或使用专门的配置文件 `application-icbc-test.yml`：

```bash
# 复制配置文件
cp application-icbc-test.yml src/main/resources/
```

## 测试接口说明

### 1. 健康检查
```bash
GET /admin-api/icbc/test/health
```
- **功能**: 检查模块基本运行状态
- **无需权限**: 可直接访问
- **返回**: 模块运行状态和当前时间

### 2. 配置信息检查
```bash
GET /admin-api/icbc/test/config
```
- **功能**: 查看当前配置信息（密钥状态，不显示具体内容）
- **权限**: 需要 `icbc:test:query` 权限
- **返回**: 配置完整性检查结果

### 3. SDK连接测试
```bash
GET /admin-api/icbc/test/connection
```
- **功能**: 测试工行SDK客户端创建是否正常
- **权限**: 需要 `icbc:test:query` 权限
- **返回**: SDK连接状态

### 4. 签名验证测试
```bash
GET /admin-api/icbc/test/signature
```
- **功能**: 验证RSA私钥和公钥配置是否正确
- **权限**: 需要 `icbc:test:query` 权限
- **返回**: 签名配置验证结果

### 5. 发票查询接口测试
```bash
GET /admin-api/icbc/test/invoice-query?outOrderId=test001&outUserId=user001
```
- **功能**: 测试工行发票查询接口调用
- **参数**: 
  - `outOrderId`: 订单ID（可选，默认自动生成）
  - `outUserId`: 用户ID（可选，默认自动生成）
- **权限**: 需要 `icbc:test:query` 权限
- **返回**: 接口调用结果和响应数据

### 6. 支付表单生成测试
```bash
# 直接返回HTML页面
GET /admin-api/icbc/test/payment-form?outOrderId=pay001&outUserId=user001

# 返回JSON格式
GET /admin-api/icbc/test/payment-form-json?outOrderId=pay001&outUserId=user001
```
- **功能**: 生成工行支付表单HTML
- **参数**: 
  - `outOrderId`: 订单ID（可选，默认自动生成）
  - `outUserId`: 用户ID（可选，默认自动生成）
- **权限**: 需要 `icbc:test:query` 权限
- **返回**: 支付表单HTML或JSON格式

### 7. 综合测试
```bash
GET /admin-api/icbc/test/all
```
- **功能**: 执行所有测试项目，获取完整测试报告
- **权限**: 需要 `icbc:test:query` 权限
- **返回**: 所有测试项目的执行结果和统计信息

## 测试步骤

### 第一步：基础环境检查
1. 确认JDK版本为1.8
2. 确认Maven可以正常编译项目
3. 确认工行SDK JAR包已正确加载

### 第二步：配置验证
1. 访问健康检查接口：
   ```bash
   curl http://localhost:48080/admin-api/icbc/test/health
   ```

2. 检查配置信息：
   ```bash
   curl -H "Authorization: Bearer YOUR_TOKEN" \
        http://localhost:48080/admin-api/icbc/test/config
   ```

### 第三步：连接和签名测试
1. 测试SDK连接：
   ```bash
   curl -H "Authorization: Bearer YOUR_TOKEN" \
        http://localhost:48080/admin-api/icbc/test/connection
   ```

2. 测试签名验证：
   ```bash
   curl -H "Authorization: Bearer YOUR_TOKEN" \
        http://localhost:48080/admin-api/icbc/test/signature
   ```

### 第四步：接口功能测试
1. 测试发票查询：
   ```bash
   curl -H "Authorization: Bearer YOUR_TOKEN" \
        "http://localhost:48080/admin-api/icbc/test/invoice-query?outOrderId=test_$(date +%s)&outUserId=user_$(date +%s)"
   ```

2. 测试支付表单生成：
   ```bash
   curl -H "Authorization: Bearer YOUR_TOKEN" \
        "http://localhost:48080/admin-api/icbc/test/payment-form-json?outOrderId=pay_$(date +%s)&outUserId=user_$(date +%s)"
   ```

### 第五步：综合测试
```bash
curl -H "Authorization: Bearer YOUR_TOKEN" \
     http://localhost:48080/admin-api/icbc/test/all
```

## 预期结果

### 成功响应示例
```json
{
    "code": 0,
    "data": "测试成功的具体信息",
    "msg": ""
}
```

### 配置检查成功示例
```json
{
    "code": 0,
    "data": {
        "appId": "$(ICBC_APP_ID)",
        "signType": "RSA2",
        "privateKeyConfigured": true,
        "apigwPublicKeyConfigured": true,
        "aesKeyConfigured": true,
        "configurationComplete": true
    },
    "msg": ""
}
```

### 综合测试成功示例
```json
{
    "code": 0,
    "data": {
        "connectionTest": {
            "success": true,
            "message": "工行SDK连接测试成功！"
        },
        "signatureTest": {
            "success": true,
            "message": "签名验证配置检查通过！"
        },
        "configTest": {
            "success": true,
            "data": {...}
        },
        "invoiceQueryTest": {
            "success": true,
            "data": {...}
        },
        "summary": {
            "totalTests": 4,
            "successCount": 4,
            "failureCount": 0,
            "allPassed": true
        }
    },
    "msg": ""
}
```

## 常见问题排查

### 1. 签名验证失败
- **问题**: 返回 "RSA私钥未配置" 或 "工行API网关公钥未配置"
- **解决**: 检查配置文件中的私钥和公钥是否正确设置

### 2. SDK连接失败
- **问题**: 工行SDK客户端创建失败
- **解决**: 
  - 检查JAR包是否正确加载
  - 确认JDK版本兼容性
  - 查看详细错误日志

### 3. 接口调用异常
- **问题**: 工行API调用返回错误
- **解决**:
  - 检查网络连接
  - 确认应用ID和密钥配置正确
  - 查看工行接口返回的具体错误码

### 4. 权限不足
- **问题**: 返回403权限错误
- **解决**: 确保用户具有 `icbc:test:query` 权限

## 日志查看

测试过程中可以查看详细日志：

```bash
# 查看应用日志
tail -f logs/spring.log | grep -i icbc

# 查看特定测试的日志
tail -f logs/spring.log | grep -i "工行\|icbc\|签名"
```

## 下一步

签名验证测试通过后，可以进行：
1. 实际业务接口开发
2. 回调处理功能测试
3. 完整业务流程测试
4. 生产环境配置

## 技术支持

如遇到问题，请：
1. 查看详细错误日志
2. 检查配置文件设置
3. 确认网络连接状态
4. 联系开发团队获取支持 