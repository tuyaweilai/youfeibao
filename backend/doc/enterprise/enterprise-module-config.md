# 企业模块配置指南

## 概述

本文档介绍了基于 Spring 设计哲学改进后的企业模块配置方式，通过事件驱动、AOP切面、SPI机制等实现了完全解耦的企业绑定功能。

## 配置说明

### 基础配置

在 `application.yml` 中添加以下配置：

```yaml
yudao:
  enterprise:
    # 是否启用企业模块
    enabled: true
    
    # 企业绑定配置
    binding:
      # 是否启用自动绑定
      auto-binding: true
      # 绑定失败是否中断登录
      failure-interrupt-login: false
      # 绑定超时时间（秒）
      timeout-seconds: 30
      
    # 企业认证配置
    authentication:
      # 是否启用企业认证
      enabled: true
      # 认证提供商
      provider: "esign"
```

### 模块启用配置

- **完全启用企业模块**：设置 `yudao.enterprise.enabled=true`
- **仅启用企业绑定**：设置 `yudao.enterprise.binding.auto-binding=true`
- **禁用企业模块**：设置 `yudao.enterprise.enabled=false`

## 架构改进点

### 1. 事件驱动机制

- **LoginEvent**：用户登录成功后发布的事件
- **事件监听器**：通过 `@EventListener` 注解监听登录事件
- **异步处理**：支持异步处理企业绑定逻辑

### 2. AOP 切面处理

- **LoginAspect**：拦截登录成功方法，处理横切关注点
- **透明处理**：不影响原有登录逻辑
- **异常隔离**：企业绑定异常不影响登录成功

### 3. SPI 扩展机制

- **LoginPostProcessor**：登录后处理器接口
- **可插拔架构**：支持多个处理器链式处理
- **优先级控制**：通过 `@Order` 注解控制执行顺序

### 4. 条件装配

- **@ConditionalOnEnterpriseModule**：条件注解
- **环境特定**：根据配置决定是否装配相关组件
- **向后兼容**：不启用时完全不影响原有功能

## 扩展示例

### 自定义登录后处理器

```java
@Component
@Order(200)
public class CustomLoginProcessor implements LoginPostProcessor {
    
    @Override
    public String getName() {
        return "CustomLoginProcessor";
    }
    
    @Override
    public boolean process(LoginEvent event) {
        // 自定义处理逻辑
        return true; // 继续执行后续处理器
    }
    
    @Override
    public void enhanceResponse(AuthLoginRespVO respVO, LoginEvent event) {
        // 增强登录响应
    }
}
```

### 事件监听器

```java
@Component
public class LoginEventListener {
    
    @EventListener
    @Async
    public void handleLoginEvent(LoginEvent event) {
        // 异步处理登录事件
    }
}
```

## 升级指南

### 从旧版本升级

1. **配置迁移**：将原有配置迁移到新的配置结构
2. **依赖检查**：确保 `yudao.enterprise.enabled=true`
3. **功能测试**：验证企业绑定功能正常工作

### 兼容性

- **完全向后兼容**：原有API接口保持不变
- **渐进式升级**：可以逐步迁移到新架构
- **配置兼容**：支持新旧配置并存

## 故障排除

### 常见问题

1. **企业绑定不生效**
   - 检查 `yudao.enterprise.enabled` 配置
   - 确认 `EnterpriseBindingLoginProcessor` 被正确加载

2. **性能问题**
   - 调整 `timeout-seconds` 配置
   - 启用异步处理

3. **扩展冲突**
   - 检查处理器优先级配置
   - 确认 SPI 配置文件正确

### 调试方法

```yaml
logging:
  level:
    cn.iocoder.yudao.framework.security.core.aop: DEBUG
    cn.iocoder.yudao.module.enterprise.service.auth: DEBUG
```

## 最佳实践

1. **配置管理**：使用配置中心统一管理
2. **监控告警**：添加企业绑定成功率监控
3. **性能优化**：使用缓存减少重复查询
4. **异常处理**：完善异常处理和日志记录 