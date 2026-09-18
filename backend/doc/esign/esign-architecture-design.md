# 电子签章模块架构设计文档

## 1. 项目概述

本文档描述了基于e签宝的电子签章模块在"危废再生资源全流程追溯SaaS平台"中的架构设计和实现方案。

### 1.1 设计目标
- 为合同管理模块提供电子签章能力
- 支持多环境配置（测试/生产）
- 提供统一的签章服务接口
- 确保签章过程的安全性和法律效力
- 支持多种签章场景和流程

### 1.2 技术选型
- **电子签章服务商**：e签宝
- **集成方式**：Spring Boot Starter
- **配置管理**：Spring Boot Configuration Properties
- **环境切换**：Profile-based配置

## 2. 架构设计

### 2.1 整体架构图

```
┌─────────────────────────────────────────────────────────────┐
│                    业务应用层                                │
├─────────────────────────────────────────────────────────────┤
│  合同管理模块    │  其他业务模块    │  管理后台配置页面        │
├─────────────────────────────────────────────────────────────┤
│                 电子签章服务层                               │
├─────────────────────────────────────────────────────────────┤
│  ESignService   │  ConfigService  │  NotificationService    │
├─────────────────────────────────────────────────────────────┤
│                电子签章框架层                                │
├─────────────────────────────────────────────────────────────┤
│  ESignClient    │  Configuration  │  Exception Handler      │
├─────────────────────────────────────────────────────────────┤
│                  e签宝SDK层                                 │
├─────────────────────────────────────────────────────────────┤
│              e签宝API服务                                   │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 模块结构设计

#### 2.2.1 推荐方案：独立Starter模块

```
ruoyi-vue-pro/yudao-framework/yudao-spring-boot-starter-esign/
├── pom.xml                                    # Maven配置文件
├── libs/                                      # 第三方SDK存放目录
│   ├── esign-sdk-3.0.19.jar                 # e签宝主要SDK
│   └── esign-dependencies.jar                # 相关依赖（如有）
├── src/main/java/cn/iocoder/yudao/framework/esign/
│   ├── config/
│   │   ├── ESignAutoConfiguration.java       # 自动配置类
│   │   ├── ESignProperties.java              # 配置属性类
│   │   └── ESignEnvironmentConfig.java       # 环境配置类
│   ├── client/
│   │   ├── ESignClient.java                  # 电子签章客户端接口
│   │   └── impl/
│   │       ├── ESignClientImpl.java          # 具体实现
│   │       └── ESignEnvironmentSelector.java # 环境选择器
│   ├── service/
│   │   ├── ESignService.java                 # 签章服务接口
│   │   ├── ESignServiceImpl.java             # 签章服务实现
│   │   ├── ESignConfigService.java           # 配置管理服务
│   │   └── ESignNotificationService.java     # 通知服务
│   ├── dto/
│   │   ├── request/
│   │   │   ├── SignRequest.java              # 签署请求DTO
│   │   │   ├── CreateContractRequest.java    # 创建合同请求
│   │   │   └── SendSignRequest.java          # 发送签署请求
│   │   ├── response/
│   │   │   ├── SignResponse.java             # 签署响应DTO
│   │   │   ├── ContractResponse.java         # 合同响应
│   │   │   └── SignStatusResponse.java       # 签署状态响应
│   │   └── config/
│   │       ├── ESignConfigDTO.java           # 配置DTO
│   │       └── EnvironmentConfigDTO.java     # 环境配置DTO
│   ├── enums/
│   │   ├── SignStatusEnum.java               # 签署状态枚举
│   │   ├── SignTypeEnum.java                 # 签署类型枚举
│   │   └── EnvironmentEnum.java              # 环境枚举
│   ├── exception/
│   │   ├── ESignException.java               # 电子签章异常类
│   │   ├── ESignConfigException.java         # 配置异常
│   │   └── ESignApiException.java            # API调用异常
│   └── util/
│       ├── ESignUtils.java                   # 工具类
│       └── SignatureUtils.java               # 签名工具类
└── src/main/resources/
    ├── META-INF/
    │   └── spring.factories                  # Spring Boot自动配置
    └── application-esign.yml                 # 默认配置模板
```

#### 2.2.2 业务模块集成

```
ruoyi-vue-pro/yudao-module-contract/yudao-module-contract-biz/
├── src/main/java/cn/iocoder/yudao/module/contract/
│   ├── controller/admin/esign/
│   │   ├── ESignConfigController.java        # 电子签章配置管理
│   │   └── ESignManageController.java        # 签章管理
│   ├── service/esign/
│   │   ├── ContractESignService.java         # 合同电子签章服务
│   │   └── ContractESignServiceImpl.java     # 实现类
│   └── convert/esign/
│       └── ESignConvert.java                 # 转换类
```

## 3. 核心组件设计

### 3.1 配置管理组件

#### 3.1.1 配置属性类
```java
@ConfigurationProperties(prefix = "yudao.esign")
@Data
public class ESignProperties {
    
    /**
     * 是否启用电子签章
     */
    private Boolean enabled = false;
    
    /**
     * 当前环境：test/prod
     */
    private String environment = "test";
    
    /**
     * 测试环境配置
     */
    private EnvironmentConfig test = new EnvironmentConfig();
    
    /**
     * 生产环境配置
     */
    private EnvironmentConfig prod = new EnvironmentConfig();
    
    @Data
    public static class EnvironmentConfig {
        /**
         * API服务地址
         */
        private String apiUrl;
        
        /**
         * 应用ID
         */
        private String appId;
        
        /**
         * 应用密钥
         */
        private String appSecret;
        
        /**
         * 回调地址
         */
        private String callbackUrl;
        
        /**
         * 连接超时时间（毫秒）
         */
        private Integer connectTimeout = 30000;
        
        /**
         * 读取超时时间（毫秒）
         */
        private Integer readTimeout = 60000;
    }
}
```

#### 3.1.2 环境选择器
```java
@Component
public class ESignEnvironmentSelector {
    
    @Autowired
    private ESignProperties eSignProperties;
    
    /**
     * 获取当前环境配置
     */
    public EnvironmentConfig getCurrentConfig() {
        String env = eSignProperties.getEnvironment();
        if ("prod".equals(env)) {
            return eSignProperties.getProd();
        }
        return eSignProperties.getTest();
    }
    
    /**
     * 切换环境
     */
    public void switchEnvironment(String environment) {
        eSignProperties.setEnvironment(environment);
        // 重新初始化客户端
        refreshClient();
    }
}
```

### 3.2 客户端组件

#### 3.2.1 客户端接口
```java
public interface ESignClient {
    
    /**
     * 创建合同
     */
    ContractResponse createContract(CreateContractRequest request);
    
    /**
     * 发送签署
     */
    SignResponse sendSign(SendSignRequest request);
    
    /**
     * 查询签署状态
     */
    SignStatusResponse getSignStatus(String contractId);
    
    /**
     * 下载合同
     */
    byte[] downloadContract(String contractId);
    
    /**
     * 撤销签署
     */
    void cancelSign(String contractId);
}
```

### 3.3 服务层组件

#### 3.3.1 签章服务接口
```java
public interface ESignService {
    
    /**
     * 发起合同签署
     */
    SignResponse initiateContractSign(SignRequest request);
    
    /**
     * 查询签署进度
     */
    SignStatusResponse querySignProgress(String contractId);
    
    /**
     * 处理签署回调
     */
    void handleSignCallback(String callbackData);
    
    /**
     * 下载已签署合同
     */
    byte[] downloadSignedContract(String contractId);
}
```

## 4. 数据库设计

### 4.1 电子签章配置表
```sql
CREATE TABLE esign_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    environment VARCHAR(10) NOT NULL COMMENT '环境：test/prod',
    api_url VARCHAR(255) NOT NULL COMMENT 'API服务地址',
    app_id VARCHAR(100) NOT NULL COMMENT '应用ID',
    app_secret VARCHAR(255) NOT NULL COMMENT '应用密钥（加密存储）',
    callback_url VARCHAR(255) COMMENT '回调地址',
    connect_timeout INT DEFAULT 30000 COMMENT '连接超时时间',
    read_timeout INT DEFAULT 60000 COMMENT '读取超时时间',
    status TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户ID',
    UNIQUE KEY uk_environment (environment, deleted, tenant_id)
) COMMENT '电子签章配置表';
```

### 4.2 签署记录表
```sql
CREATE TABLE esign_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    contract_id BIGINT NOT NULL COMMENT '合同ID',
    esign_contract_id VARCHAR(100) COMMENT 'e签宝合同ID',
    sign_type TINYINT NOT NULL COMMENT '签署类型：1-单方签署，2-多方签署',
    sign_status TINYINT DEFAULT 1 COMMENT '签署状态：1-待签署，2-签署中，3-已完成，4-已撤销',
    sign_url VARCHAR(500) COMMENT '签署链接',
    callback_data TEXT COMMENT '回调数据',
    error_message VARCHAR(1000) COMMENT '错误信息',
    sign_start_time DATETIME COMMENT '签署开始时间',
    sign_end_time DATETIME COMMENT '签署完成时间',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户ID',
    INDEX idx_contract_id (contract_id),
    INDEX idx_esign_contract_id (esign_contract_id),
    INDEX idx_sign_status (sign_status)
) COMMENT '电子签章记录表';
```

## 5. 安全设计

### 5.1 配置安全
- 应用密钥使用AES加密存储
- 支持密钥轮换机制
- API调用使用HTTPS协议
- 实现请求签名验证

### 5.2 权限控制
- 配置管理需要超级管理员权限
- 签署操作需要相应的业务权限
- 支持操作审计日志

### 5.3 数据安全
- 敏感数据加密传输
- 签署文件安全存储
- 支持数据备份和恢复

## 6. 监控和运维

### 6.1 监控指标
- API调用成功率
- 签署完成率
- 响应时间统计
- 错误率统计

### 6.2 日志记录
- 关键操作日志
- 错误异常日志
- 性能监控日志
- 安全审计日志

### 6.3 告警机制
- API调用失败告警
- 配置异常告警
- 性能异常告警

## 7. 扩展性设计

### 7.1 多服务商支持
- 抽象签章服务接口
- 支持服务商切换
- 统一的配置管理

### 7.2 功能扩展
- 支持批量签署
- 支持签署模板
- 支持自定义签署流程

### 7.3 集成扩展
- 支持Webhook回调
- 支持消息队列集成
- 支持缓存优化

## 8. 部署说明

### 8.1 依赖安装
```bash
# 安装e签宝SDK到本地Maven仓库
mvn install:install-file \
  -Dfile=libs/esign-sdk-3.0.19.jar \
  -DgroupId=com.esign \
  -DartifactId=esign-sdk \
  -Dversion=3.0.19 \
  -Dpackaging=jar
```

### 8.2 配置示例
```yaml
yudao:
  esign:
    enabled: true
    environment: test
    test:
      api-url: https://smlopenapi.esign.cn
      app-id: your-test-app-id
      app-secret: your-test-app-secret
      callback-url: https://your-domain.com/api/esign/callback
    prod:
      api-url: https://openapi.esign.cn
      app-id: your-prod-app-id
      app-secret: your-prod-app-secret
      callback-url: https://your-domain.com/api/esign/callback
```

## 9. 测试策略

### 9.1 单元测试
- 配置管理测试
- 客户端接口测试
- 服务层逻辑测试

### 9.2 集成测试
- e签宝API集成测试
- 数据库操作测试
- 端到端流程测试

### 9.3 性能测试
- 并发签署测试
- 大文件处理测试
- 长时间运行测试

## 10. 版本规划

### 10.1 V1.0 基础版本
- 基本签署功能
- 配置管理
- 环境切换

### 10.2 V1.1 增强版本
- 批量签署
- 签署模板
- 监控告警

### 10.3 V2.0 高级版本
- 多服务商支持
- 高级工作流
- 智能分析 