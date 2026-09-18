# 电子签章集成调用规范文档

## 1. 概述

本文档定义了电子签章模块与现有系统其他板块的集成方式和调用规范，确保各模块间的协作顺畅和数据一致性。

## 2. 集成架构

### 2.1 模块依赖关系

```
┌─────────────────────────────────────────────────────────────┐
│                    业务应用层                                │
├─────────────────────────────────────────────────────────────┤
│  合同管理模块    │  危废转移模块    │  物流模块    │  其他模块  │
├─────────────────────────────────────────────────────────────┤
│                 电子签章服务层                               │
├─────────────────────────────────────────────────────────────┤
│  ESignService   │  ESignClient    │  CallbackHandler        │
├─────────────────────────────────────────────────────────────┤
│                电子签章框架层                                │
├─────────────────────────────────────────────────────────────┤
│  yudao-spring-boot-starter-esign                           │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 集成方式

#### 2.2.1 依赖引入
```xml
<!-- 在需要使用电子签章的模块中引入 -->
<dependency>
    <groupId>cn.iocoder.yudao</groupId>
    <artifactId>yudao-spring-boot-starter-esign</artifactId>
    <version>${yudao.version}</version>
</dependency>
```

#### 2.2.2 自动配置
```java
// 启动类添加注解
@EnableESign
@SpringBootApplication
public class YudaoServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(YudaoServerApplication.class, args);
    }
}
```

## 3. 核心接口定义

### 3.1 电子签章服务接口

```java
/**
 * 电子签章服务接口
 * 提供给业务模块调用的统一接口
 */
public interface ESignService {
    
    /**
     * 发起合同签署
     * @param request 签署请求
     * @return 签署响应
     */
    ESignResponse initiateContractSign(ESignRequest request);
    
    /**
     * 查询签署状态
     * @param contractId 合同ID
     * @return 签署状态
     */
    ESignStatusResponse getSignStatus(String contractId);
    
    /**
     * 下载已签署合同
     * @param contractId 合同ID
     * @return 合同文件字节数组
     */
    byte[] downloadSignedContract(String contractId);
    
    /**
     * 撤销签署
     * @param contractId 合同ID
     */
    void cancelSign(String contractId);
    
    /**
     * 批量签署
     * @param requests 批量签署请求
     * @return 批量签署响应
     */
    List<ESignResponse> batchSign(List<ESignRequest> requests);
}
```

### 3.2 请求响应DTO定义

```java
/**
 * 电子签章请求DTO
 */
@Data
public class ESignRequest {
    
    /**
     * 业务模块标识
     */
    private String moduleCode;
    
    /**
     * 业务对象ID（如合同ID）
     */
    private Long businessId;
    
    /**
     * 合同标题
     */
    private String contractTitle;
    
    /**
     * 合同内容（PDF文件的Base64编码或文件URL）
     */
    private String contractContent;
    
    /**
     * 内容类型：BASE64、FILE_URL
     */
    private String contentType;
    
    /**
     * 签署方信息
     */
    private List<SignerInfo> signers;
    
    /**
     * 签署类型：SINGLE-单方签署，MULTI-多方签署
     */
    private String signType;
    
    /**
     * 签署顺序：SERIAL-串行，PARALLEL-并行
     */
    private String signOrder;
    
    /**
     * 回调地址
     */
    private String callbackUrl;
    
    /**
     * 扩展参数
     */
    private Map<String, Object> extParams;
}

/**
 * 签署方信息
 */
@Data
public class SignerInfo {
    
    /**
     * 签署方类型：PERSONAL-个人，ENTERPRISE-企业
     */
    private String signerType;
    
    /**
     * 签署方名称
     */
    private String signerName;
    
    /**
     * 联系方式（手机号或邮箱）
     */
    private String contact;
    
    /**
     * 证件类型
     */
    private String idType;
    
    /**
     * 证件号码
     */
    private String idNumber;
    
    /**
     * 签署位置（页码，坐标等）
     */
    private SignPosition signPosition;
    
    /**
     * 签署顺序（串行签署时使用）
     */
    private Integer signOrder;
}

/**
 * 电子签章响应DTO
 */
@Data
public class ESignResponse {
    
    /**
     * 是否成功
     */
    private Boolean success;
    
    /**
     * 错误码
     */
    private String errorCode;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * e签宝合同ID
     */
    private String eSignContractId;
    
    /**
     * 签署链接（多方签署时为签署方提供）
     */
    private List<SignUrlInfo> signUrls;
    
    /**
     * 签署状态
     */
    private String signStatus;
    
    /**
     * 预计完成时间
     */
    private LocalDateTime estimatedCompleteTime;
}
```

## 4. 业务模块集成示例

### 4.1 合同管理模块集成

```java
@Service
@Slf4j
public class ContractESignServiceImpl implements ContractESignService {
    
    @Autowired
    private ESignService eSignService;
    
    @Autowired
    private ContractService contractService;
    
    /**
     * 发起合同电子签署
     */
    @Override
    public void initiateContractSign(Long contractId) {
        // 1. 获取合同信息
        ContractDO contract = contractService.getContract(contractId);
        if (contract == null) {
            throw new BusinessException("合同不存在");
        }
        
        // 2. 构建签署请求
        ESignRequest request = buildSignRequest(contract);
        
        // 3. 调用电子签章服务
        ESignResponse response = eSignService.initiateContractSign(request);
        
        // 4. 处理响应结果
        if (response.getSuccess()) {
            // 更新合同状态为签署中
            contractService.updateSignStatus(contractId, ContractSignStatusEnum.SIGNING);
            
            // 保存e签宝合同ID
            contractService.updateESignContractId(contractId, response.getESignContractId());
            
            // 发送签署通知
            sendSignNotification(contract, response.getSignUrls());
            
        } else {
            log.error("合同签署发起失败: {}", response.getErrorMessage());
            throw new BusinessException("签署发起失败: " + response.getErrorMessage());
        }
    }
    
    /**
     * 构建签署请求
     */
    private ESignRequest buildSignRequest(ContractDO contract) {
        ESignRequest request = new ESignRequest();
        request.setModuleCode("CONTRACT");
        request.setBusinessId(contract.getId());
        request.setContractTitle(contract.getName());
        request.setContractContent(contract.getFileUrl());
        request.setContentType("FILE_URL");
        request.setSignType("MULTI");
        request.setSignOrder("SERIAL");
        request.setCallbackUrl("/api/contract/esign/callback");
        
        // 构建签署方信息
        List<SignerInfo> signers = buildSignerInfos(contract);
        request.setSigners(signers);
        
        return request;
    }
    
    /**
     * 处理签署回调
     */
    @Override
    public void handleSignCallback(ESignCallbackDTO callback) {
        // 1. 验证回调签名
        if (!validateCallbackSignature(callback)) {
            log.error("签署回调签名验证失败");
            return;
        }
        
        // 2. 根据e签宝合同ID查找业务合同
        ContractDO contract = contractService.getByESignContractId(callback.getContractId());
        if (contract == null) {
            log.error("未找到对应的业务合同: {}", callback.getContractId());
            return;
        }
        
        // 3. 更新合同状态
        switch (callback.getSignStatus()) {
            case "COMPLETED":
                contractService.updateSignStatus(contract.getId(), ContractSignStatusEnum.COMPLETED);
                // 下载已签署合同
                downloadAndSaveSignedContract(contract.getId());
                break;
            case "CANCELLED":
                contractService.updateSignStatus(contract.getId(), ContractSignStatusEnum.CANCELLED);
                break;
            case "FAILED":
                contractService.updateSignStatus(contract.getId(), ContractSignStatusEnum.FAILED);
                break;
        }
        
        // 4. 发送状态变更通知
        sendStatusChangeNotification(contract, callback.getSignStatus());
    }
}
```

### 4.2 危废转移模块集成

```java
@Service
@Slf4j
public class WasteTransferESignServiceImpl implements WasteTransferESignService {
    
    @Autowired
    private ESignService eSignService;
    
    /**
     * 为危废转移订单生成运输合同并发起签署
     */
    @Override
    public void generateTransportContract(Long orderId) {
        // 1. 获取订单信息
        WasteTransferOrderDO order = wasteTransferService.getOrder(orderId);
        
        // 2. 生成运输合同PDF
        byte[] contractPdf = generateTransportContractPdf(order);
        
        // 3. 构建签署请求
        ESignRequest request = new ESignRequest();
        request.setModuleCode("WASTE_TRANSFER");
        request.setBusinessId(orderId);
        request.setContractTitle("危废运输合同-" + order.getOrderNo());
        request.setContractContent(Base64.getEncoder().encodeToString(contractPdf));
        request.setContentType("BASE64");
        request.setSignType("MULTI");
        request.setCallbackUrl("/api/waste/transfer/esign/callback");
        
        // 构建签署方：产废企业、回收企业、运输企业
        List<SignerInfo> signers = Arrays.asList(
            buildProducerSigner(order),
            buildRecyclerSigner(order),
            buildTransporterSigner(order)
        );
        request.setSigners(signers);
        
        // 4. 发起签署
        ESignResponse response = eSignService.initiateContractSign(request);
        
        // 5. 处理结果
        if (response.getSuccess()) {
            // 更新订单合同状态
            wasteTransferService.updateContractStatus(orderId, "SIGNING");
        }
    }
}
```

## 5. 回调处理机制

### 5.1 统一回调处理器

```java
@RestController
@RequestMapping("/api/esign/callback")
@Slf4j
public class ESignCallbackController {
    
    @Autowired
    private ESignCallbackDispatcher callbackDispatcher;
    
    /**
     * 统一回调接口
     */
    @PostMapping("/unified")
    public CommonResult<Boolean> handleCallback(@RequestBody ESignCallbackDTO callback) {
        try {
            // 1. 验证回调签名
            if (!validateSignature(callback)) {
                log.error("回调签名验证失败: {}", callback);
                return CommonResult.error("签名验证失败");
            }
            
            // 2. 分发到具体的业务模块处理
            callbackDispatcher.dispatch(callback);
            
            return CommonResult.success(true);
            
        } catch (Exception e) {
            log.error("处理签署回调异常", e);
            return CommonResult.error("处理失败");
        }
    }
}

/**
 * 回调分发器
 */
@Component
public class ESignCallbackDispatcher {
    
    @Autowired
    private Map<String, ESignCallbackHandler> handlerMap;
    
    public void dispatch(ESignCallbackDTO callback) {
        // 根据模块代码分发到对应的处理器
        String moduleCode = callback.getModuleCode();
        ESignCallbackHandler handler = handlerMap.get(moduleCode.toLowerCase() + "ESignCallbackHandler");
        
        if (handler != null) {
            handler.handle(callback);
        } else {
            log.warn("未找到对应的回调处理器: {}", moduleCode);
        }
    }
}
```

### 5.2 业务模块回调处理器

```java
/**
 * 合同模块回调处理器
 */
@Component("contractESignCallbackHandler")
public class ContractESignCallbackHandler implements ESignCallbackHandler {
    
    @Override
    public void handle(ESignCallbackDTO callback) {
        // 处理合同签署回调
        contractESignService.handleSignCallback(callback);
    }
}

/**
 * 危废转移模块回调处理器
 */
@Component("wasteTransferESignCallbackHandler")
public class WasteTransferESignCallbackHandler implements ESignCallbackHandler {
    
    @Override
    public void handle(ESignCallbackDTO callback) {
        // 处理危废转移合同签署回调
        wasteTransferESignService.handleSignCallback(callback);
    }
}
```

## 6. 异常处理和重试机制

### 6.1 异常处理

```java
@Component
public class ESignExceptionHandler {
    
    /**
     * 处理签署异常
     */
    public void handleSignException(String moduleCode, Long businessId, Exception e) {
        log.error("电子签章异常 - 模块: {}, 业务ID: {}", moduleCode, businessId, e);
        
        // 1. 记录异常日志
        recordExceptionLog(moduleCode, businessId, e);
        
        // 2. 发送异常告警
        sendExceptionAlert(moduleCode, businessId, e);
        
        // 3. 更新业务状态
        updateBusinessStatus(moduleCode, businessId, "SIGN_FAILED");
    }
}
```

### 6.2 重试机制

```java
@Component
public class ESignRetryHandler {
    
    @Retryable(value = {ESignException.class}, maxAttempts = 3, backoff = @Backoff(delay = 2000))
    public ESignResponse retrySign(ESignRequest request) {
        return eSignService.initiateContractSign(request);
    }
    
    @Recover
    public ESignResponse recover(ESignException e, ESignRequest request) {
        log.error("电子签章重试失败，进入恢复处理: {}", request.getBusinessId());
        
        // 恢复处理逻辑
        ESignResponse response = new ESignResponse();
        response.setSuccess(false);
        response.setErrorMessage("重试失败: " + e.getMessage());
        
        return response;
    }
}
```

## 7. 监控和日志

### 7.1 调用监控

```java
@Component
public class ESignMonitor {
    
    private final MeterRegistry meterRegistry;
    private final Counter signRequestCounter;
    private final Timer signRequestTimer;
    
    public ESignMonitor(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.signRequestCounter = Counter.builder("esign.request.count")
            .description("电子签章请求次数")
            .register(meterRegistry);
        this.signRequestTimer = Timer.builder("esign.request.duration")
            .description("电子签章请求耗时")
            .register(meterRegistry);
    }
    
    public void recordSignRequest(String moduleCode, boolean success, Duration duration) {
        signRequestCounter.increment(Tags.of("module", moduleCode, "success", String.valueOf(success)));
        signRequestTimer.record(duration, Tags.of("module", moduleCode));
    }
}
```

### 7.2 操作日志

```java
@Aspect
@Component
@Slf4j
public class ESignLogAspect {
    
    @Around("@annotation(ESignLog)")
    public Object logESignOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();
        
        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;
            
            log.info("电子签章操作成功 - 方法: {}, 参数: {}, 耗时: {}ms", 
                methodName, JSON.toJSONString(args), duration);
            
            return result;
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            
            log.error("电子签章操作失败 - 方法: {}, 参数: {}, 耗时: {}ms, 异常: {}", 
                methodName, JSON.toJSONString(args), duration, e.getMessage());
            
            throw e;
        }
    }
}
```

## 8. 配置管理

### 8.1 模块配置

```yaml
# application.yml
yudao:
  esign:
    enabled: true
    environment: test
    
    # 回调配置
    callback:
      base-url: https://your-domain.com
      signature-key: your-callback-signature-key
      
    # 重试配置
    retry:
      max-attempts: 3
      delay: 2000
      
    # 监控配置
    monitor:
      enabled: true
      metrics-enabled: true
      
    # 模块特定配置
    modules:
      contract:
        default-sign-type: MULTI
        default-sign-order: SERIAL
      waste-transfer:
        auto-generate-contract: true
        contract-template: waste-transport-template
```

### 8.2 权限配置

```java
// 权限定义
public class ESignPermissions {
    // 基础权限
    public static final String SIGN_INITIATE = "esign:sign:initiate";
    public static final String SIGN_QUERY = "esign:sign:query";
    public static final String SIGN_CANCEL = "esign:sign:cancel";
    public static final String SIGN_DOWNLOAD = "esign:sign:download";
    
    // 模块特定权限
    public static final String CONTRACT_SIGN = "contract:esign:sign";
    public static final String WASTE_TRANSFER_SIGN = "waste:transfer:esign:sign";
}
```

## 9. 最佳实践

### 9.1 调用规范

1. **统一异常处理**：所有调用都应该包装在try-catch中
2. **参数验证**：调用前验证必要参数的完整性
3. **状态同步**：及时更新业务对象的签署状态
4. **日志记录**：记录关键操作和异常信息
5. **回调处理**：实现幂等的回调处理逻辑

### 9.2 性能优化

1. **异步处理**：签署发起采用异步方式
2. **批量操作**：支持批量签署以提高效率
3. **缓存策略**：缓存常用的配置和状态信息
4. **连接池**：使用连接池管理HTTP连接

### 9.3 安全考虑

1. **签名验证**：验证所有回调请求的签名
2. **权限控制**：严格控制签署操作权限
3. **数据加密**：敏感数据传输加密
4. **审计日志**：完整记录所有操作日志

## 10. 故障排查

### 10.1 常见问题

1. **签署失败**：检查配置、网络、权限
2. **回调丢失**：检查回调地址、签名验证
3. **状态不同步**：检查回调处理逻辑
4. **性能问题**：检查并发量、网络延迟

### 10.2 排查工具

1. **日志查询**：通过日志追踪问题
2. **监控指标**：通过监控发现异常
3. **健康检查**：定期检查服务状态
4. **测试工具**：提供测试接口验证功能

这个集成调用规范文档提供了完整的电子签章模块集成指南，确保各业务模块能够正确、安全、高效地使用电子签章功能。 