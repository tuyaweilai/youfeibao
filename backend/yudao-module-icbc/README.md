# 工商银行反向开票模块

## 模块简介

工商银行反向开票模块是一个专门用于对接工商银行聚富通接口，实现企业代个人开票的完整业务流程的模块。

## 功能特性

- 🏦 **收方信息管理** - 个人销售方信息的录入、审核和管理
- 🏢 **付方信息管理** - 企业采购方信息的管理和维护
- 📋 **开票订单管理** - 反向开票订单的创建、流程控制和状态跟踪
- 💰 **支付管理** - 企业向个人的支付处理和状态同步
- 🧾 **发票管理** - 发票的开具、下载、红冲等完整生命周期管理
- 🔗 **工行接口对接** - 与工行聚富通接口的完整对接和SDK封装
- 📞 **回调处理** - 工行异步回调通知的接收和处理
- 🔐 **安全保障** - 数据加密、签名验证、权限控制等安全措施

## 模块结构

```
yudao-module-icbc/
├── yudao-module-icbc-api/          # API模块 - 对外暴露的接口定义
│   └── src/main/java/cn/iocoder/yudao/module/icbc/
│       ├── api/                    # API接口定义
│       └── enums/                  # 枚举类定义
└── yudao-module-icbc-biz/          # 业务实现模块
    └── src/main/java/cn/iocoder/yudao/module/icbc/
        ├── controller/             # 控制器层
        │   ├── admin/              # 管理后台接口
        │   └── app/                # 用户APP接口
        ├── service/                # 服务层
        ├── dal/                    # 数据访问层
        │   ├── dataobject/         # 数据对象
        │   ├── mysql/              # MySQL访问
        │   └── redis/              # Redis访问
        ├── convert/                # 转换器
        ├── framework/              # 框架组件
        ├── job/                    # 定时任务
        ├── mq/                     # 消息队列
        └── util/                   # 工具类
```

## 数据库设计

模块包含8个核心数据表：

1. **icbc_payee_info** - 收方信息表
2. **icbc_payer_info** - 付方信息表  
3. **icbc_invoice_order** - 反向开票订单表
4. **icbc_order_item** - 订单商品明细表
5. **icbc_api_log** - 工行接口调用日志表
6. **icbc_callback_notify** - 工行回调通知表
7. **icbc_red_invoice** - 红冲发票表
8. **icbc_config** - 系统配置表

详细的数据库设计请参考：`doc/icbc/工行反向发票数据库设计.sql`

## 业务流程

### 核心流程

1. **收方入驻** → 2. **付方管理** → 3. **创建订单** → 4. **个人确认** → 5. **企业支付** → 6. **自动开票** → 7. **发票下载**

### 详细流程设计

详细的业务流程设计请参考：`doc/icbc/工行反向发票流程设计文档.md`

## 接口测试

模块初始化完成后，可以通过以下接口测试模块是否正常工作：

### 管理后台接口
```
GET /admin-api/icbc/test/get
```

### 用户APP接口  
```
GET /app-api/icbc/test/get
```

成功响应格式：
```json
{
    "code": 0,
    "data": "工行反向开票模块初始化成功！",
    "msg": ""
}
```

## 开发指南

### 环境要求

- JDK 1.8+
- Maven 3.6+
- MySQL 5.7+
- Redis 3.0+

### 快速开始

1. **数据库初始化**
   ```sql
   -- 执行数据库脚本
   source doc/icbc/工行反向发票数据库设计.sql
   ```

2. **配置工行接口参数**
   ```yaml
   # 在application.yml中配置工行接口参数
   icbc:
     api:
       base-url: https://api.icbc.com.cn
       app-id: your_app_id
       private-key: your_private_key
       public-key: icbc_public_key
   ```

3. **启动服务**
   ```bash
   mvn spring-boot:run
   ```

4. **测试接口**
   ```bash
   curl http://localhost:48080/admin-api/icbc/test/get
   ```

### 开发规范

- 遵循项目统一的代码规范
- 使用统一的错误码定义
- 接口返回统一的CommonResult格式
- 敏感数据必须加密存储
- 所有工行接口调用必须记录日志

## 配置说明

### 工行接口配置

| 配置项 | 说明 | 示例 |
|--------|------|------|
| icbc.api.base_url | 工行API基础URL | https://api.icbc.com.cn |
| icbc.api.app_id | 应用ID | your_app_id |
| icbc.api.private_key | RSA私钥 | -----BEGIN PRIVATE KEY----- |
| icbc.api.public_key | 工行RSA公钥 | -----BEGIN PUBLIC KEY----- |
| icbc.api.aes_key | AES加密密钥 | your_aes_key |
| icbc.api.timeout | 接口超时时间(毫秒) | 30000 |

### 业务配置

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| icbc.business.auto_confirm | 是否自动确认订单 | false |
| icbc.business.auto_download | 是否自动下载发票 | true |

## 安全说明

### 数据安全
- 身份证号码使用AES加密存储
- 银行卡号使用AES加密存储  
- 私钥等敏感配置加密存储

### 接口安全
- 所有工行接口调用使用RSA签名
- 回调接口验证工行签名
- 强制使用HTTPS传输

### 访问控制
- 基于RBAC的权限控制
- 接口级权限验证
- 多租户数据隔离

## 监控运维

### 日志监控
- 业务操作日志
- 接口调用日志
- 系统异常日志

### 业务监控
- 订单成功率
- 支付成功率  
- 开票成功率
- 接口响应时间

### 告警机制
- 业务失败率告警
- 接口异常告警
- 系统资源告警

## 常见问题

### Q: 如何配置工行接口参数？
A: 在系统配置表`icbc_config`中配置，或通过管理后台配置界面进行配置。

### Q: 如何处理工行接口调用失败？
A: 系统会自动重试，重试失败后会记录错误日志，需要人工处理。

### Q: 如何查看接口调用日志？
A: 可以通过`icbc_api_log`表查看所有接口调用记录。

### Q: 如何处理回调通知？
A: 系统会自动处理工行回调通知，处理失败的会加入重试队列。

## 更新日志

### v1.0.0 (2024-01-XX)
- 🎉 模块初始化完成
- ✨ 基础框架搭建
- 📝 文档编写完成
- 🧪 测试接口创建

## 联系方式

如有问题，请联系开发团队或提交Issue。

## 许可证

本项目遵循项目统一的许可证协议。 