# 电子签章配置管理文档

## 1. 概述

本文档描述了电子签章模块的配置管理功能，包括配置页面设计、环境切换机制、以及基于e签宝的鉴权配置管理。

## 2. 配置管理需求

### 2.1 功能需求
- 支持测试环境和生产环境的独立配置
- 提供可视化的配置管理界面
- 支持配置的实时切换和生效
- 提供配置验证和测试功能
- 支持配置的备份和恢复
- 提供配置变更的审计日志

### 2.2 安全需求
- 敏感信息（如AppSecret）加密存储
- 配置操作需要管理员权限
- 支持配置变更的审批流程
- 提供操作日志和审计追踪

## 3. 配置数据结构

### 3.1 配置实体设计

```java
@Data
@TableName("esign_config")
public class ESignConfigDO extends BaseDO {
    
    /**
     * 主键ID
     */
    @TableId
    private Long id;
    
    /**
     * 环境类型：test-测试环境，prod-生产环境
     */
    private String environment;
    
    /**
     * API服务地址
     */
    private String apiUrl;
    
    /**
     * 应用ID
     */
    private String appId;
    
    /**
     * 应用密钥（加密存储）
     */
    private String appSecret;
    
    /**
     * 回调地址
     */
    private String callbackUrl;
    
    /**
     * 连接超时时间（毫秒）
     */
    private Integer connectTimeout;
    
    /**
     * 读取超时时间（毫秒）
     */
    private Integer readTimeout;
    
    /**
     * 状态：0-禁用，1-启用
     */
    private Integer status;
    
    /**
     * 配置描述
     */
    private String description;
    
    /**
     * 最后测试时间
     */
    private LocalDateTime lastTestTime;
    
    /**
     * 测试结果：0-失败，1-成功
     */
    private Integer testResult;
    
    /**
     * 测试错误信息
     */
    private String testErrorMsg;
}
```

### 3.2 配置VO设计

```java
@Data
@Schema(description = "电子签章配置创建请求")
public class ESignConfigCreateReqVO {
    
    @Schema(description = "环境类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "test")
    @NotEmpty(message = "环境类型不能为空")
    @Pattern(regexp = "^(test|prod)$", message = "环境类型只能是test或prod")
    private String environment;
    
    @Schema(description = "API服务地址", requiredMode = Schema.RequiredMode.REQUIRED, example = "https://smlopenapi.esign.cn")
    @NotEmpty(message = "API服务地址不能为空")
    @URL(message = "API服务地址格式不正确")
    private String apiUrl;
    
    @Schema(description = "应用ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "4f6b2c8e-1234-5678-9abc-def012345678")
    @NotEmpty(message = "应用ID不能为空")
    private String appId;
    
    @Schema(description = "应用密钥", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "应用密钥不能为空")
    private String appSecret;
    
    @Schema(description = "回调地址", example = "https://your-domain.com/api/esign/callback")
    @URL(message = "回调地址格式不正确")
    private String callbackUrl;
    
    @Schema(description = "连接超时时间", example = "30000")
    @Min(value = 1000, message = "连接超时时间不能小于1000毫秒")
    @Max(value = 300000, message = "连接超时时间不能大于300000毫秒")
    private Integer connectTimeout = 30000;
    
    @Schema(description = "读取超时时间", example = "60000")
    @Min(value = 1000, message = "读取超时时间不能小于1000毫秒")
    @Max(value = 600000, message = "读取超时时间不能大于600000毫秒")
    private Integer readTimeout = 60000;
    
    @Schema(description = "配置描述")
    private String description;
}

@Data
@Schema(description = "电子签章配置响应")
public class ESignConfigRespVO {
    
    @Schema(description = "配置ID", example = "1")
    private Long id;
    
    @Schema(description = "环境类型", example = "test")
    private String environment;
    
    @Schema(description = "API服务地址", example = "https://smlopenapi.esign.cn")
    private String apiUrl;
    
    @Schema(description = "应用ID", example = "4f6b2c8e-1234-5678-9abc-def012345678")
    private String appId;
    
    @Schema(description = "应用密钥（脱敏显示）", example = "****-****-****-****")
    private String appSecretMask;
    
    @Schema(description = "回调地址", example = "https://your-domain.com/api/esign/callback")
    private String callbackUrl;
    
    @Schema(description = "连接超时时间", example = "30000")
    private Integer connectTimeout;
    
    @Schema(description = "读取超时时间", example = "60000")
    private Integer readTimeout;
    
    @Schema(description = "状态", example = "1")
    private Integer status;
    
    @Schema(description = "配置描述")
    private String description;
    
    @Schema(description = "最后测试时间")
    private LocalDateTime lastTestTime;
    
    @Schema(description = "测试结果", example = "1")
    private Integer testResult;
    
    @Schema(description = "测试错误信息")
    private String testErrorMsg;
    
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
```

## 4. 配置管理服务

### 4.1 服务接口设计

```java
public interface ESignConfigService {
    
    /**
     * 创建配置
     */
    Long createConfig(ESignConfigCreateReqVO createReqVO);
    
    /**
     * 更新配置
     */
    void updateConfig(ESignConfigUpdateReqVO updateReqVO);
    
    /**
     * 删除配置
     */
    void deleteConfig(Long id);
    
    /**
     * 获取配置详情
     */
    ESignConfigRespVO getConfig(Long id);
    
    /**
     * 获取配置列表
     */
    PageResult<ESignConfigRespVO> getConfigPage(ESignConfigPageReqVO pageReqVO);
    
    /**
     * 根据环境获取配置
     */
    ESignConfigRespVO getConfigByEnvironment(String environment);
    
    /**
     * 切换环境
     */
    void switchEnvironment(String environment);
    
    /**
     * 获取当前环境
     */
    String getCurrentEnvironment();
    
    /**
     * 测试配置连接
     */
    ESignTestResultVO testConfig(Long id);
    
    /**
     * 启用/禁用配置
     */
    void updateConfigStatus(Long id, Integer status);
    
    /**
     * 获取配置变更历史
     */
    List<ESignConfigHistoryRespVO> getConfigHistory(Long id);
}
```

### 4.2 服务实现要点

```java
@Service
@Slf4j
public class ESignConfigServiceImpl implements ESignConfigService {
    
    @Autowired
    private ESignConfigMapper eSignConfigMapper;
    
    @Autowired
    private ESignClient eSignClient;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    private static final String CURRENT_ENV_KEY = "esign:current:environment";
    private static final String CONFIG_CACHE_KEY = "esign:config:";
    
    @Override
    @Transactional
    public Long createConfig(ESignConfigCreateReqVO createReqVO) {
        // 1. 校验环境唯一性
        validateEnvironmentUnique(createReqVO.getEnvironment(), null);
        
        // 2. 加密敏感信息
        ESignConfigDO config = ESignConfigConvert.INSTANCE.convert(createReqVO);
        config.setAppSecret(encryptAppSecret(createReqVO.getAppSecret()));
        
        // 3. 保存配置
        eSignConfigMapper.insert(config);
        
        // 4. 清除缓存
        clearConfigCache(createReqVO.getEnvironment());
        
        // 5. 记录操作日志
        recordConfigOperation("CREATE", config.getId(), createReqVO);
        
        return config.getId();
    }
    
    @Override
    public void switchEnvironment(String environment) {
        // 1. 校验环境配置存在且启用
        ESignConfigDO config = getEnabledConfigByEnvironment(environment);
        if (config == null) {
            throw exception(ESIGN_CONFIG_NOT_EXISTS);
        }
        
        // 2. 更新当前环境
        redisTemplate.opsForValue().set(CURRENT_ENV_KEY, environment);
        
        // 3. 重新初始化客户端
        refreshESignClient(config);
        
        // 4. 记录操作日志
        recordEnvironmentSwitch(environment);
        
        log.info("电子签章环境已切换至: {}", environment);
    }
    
    @Override
    public ESignTestResultVO testConfig(Long id) {
        // 1. 获取配置
        ESignConfigDO config = eSignConfigMapper.selectById(id);
        if (config == null) {
            throw exception(ESIGN_CONFIG_NOT_EXISTS);
        }
        
        // 2. 解密配置
        String appSecret = decryptAppSecret(config.getAppSecret());
        
        // 3. 测试连接
        ESignTestResultVO result = new ESignTestResultVO();
        try {
            // 调用e签宝API测试连接
            boolean success = testESignConnection(config.getApiUrl(), 
                config.getAppId(), appSecret);
            
            result.setSuccess(success);
            result.setMessage(success ? "连接测试成功" : "连接测试失败");
            
            // 4. 更新测试结果
            updateTestResult(id, success ? 1 : 0, result.getMessage());
            
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("连接测试异常: " + e.getMessage());
            updateTestResult(id, 0, e.getMessage());
            log.error("电子签章配置测试失败", e);
        }
        
        return result;
    }
    
    /**
     * 加密应用密钥
     */
    private String encryptAppSecret(String appSecret) {
        // 使用AES加密
        return AESUtils.encrypt(appSecret, getEncryptKey());
    }
    
    /**
     * 解密应用密钥
     */
    private String decryptAppSecret(String encryptedAppSecret) {
        // 使用AES解密
        return AESUtils.decrypt(encryptedAppSecret, getEncryptKey());
    }
}
```

## 5. 配置管理Controller

### 5.1 管理后台Controller

```java
@RestController
@RequestMapping("/admin-api/esign/config")
@Tag(name = "管理后台 - 电子签章配置")
@Validated
public class ESignConfigController {
    
    @Autowired
    private ESignConfigService eSignConfigService;
    
    @PostMapping("/create")
    @Operation(summary = "创建电子签章配置")
    @PreAuthorize("@ss.hasPermission('esign:config:create')")
    public CommonResult<Long> createConfig(@Valid @RequestBody ESignConfigCreateReqVO createReqVO) {
        return success(eSignConfigService.createConfig(createReqVO));
    }
    
    @PutMapping("/update")
    @Operation(summary = "更新电子签章配置")
    @PreAuthorize("@ss.hasPermission('esign:config:update')")
    public CommonResult<Boolean> updateConfig(@Valid @RequestBody ESignConfigUpdateReqVO updateReqVO) {
        eSignConfigService.updateConfig(updateReqVO);
        return success(true);
    }
    
    @DeleteMapping("/delete")
    @Operation(summary = "删除电子签章配置")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('esign:config:delete')")
    public CommonResult<Boolean> deleteConfig(@RequestParam("id") Long id) {
        eSignConfigService.deleteConfig(id);
        return success(true);
    }
    
    @GetMapping("/get")
    @Operation(summary = "获得电子签章配置")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('esign:config:query')")
    public CommonResult<ESignConfigRespVO> getConfig(@RequestParam("id") Long id) {
        return success(eSignConfigService.getConfig(id));
    }
    
    @GetMapping("/page")
    @Operation(summary = "获得电子签章配置分页")
    @PreAuthorize("@ss.hasPermission('esign:config:query')")
    public CommonResult<PageResult<ESignConfigRespVO>> getConfigPage(@Valid ESignConfigPageReqVO pageReqVO) {
        return success(eSignConfigService.getConfigPage(pageReqVO));
    }
    
    @PostMapping("/switch-environment")
    @Operation(summary = "切换环境")
    @Parameter(name = "environment", description = "环境类型", required = true)
    @PreAuthorize("@ss.hasPermission('esign:config:switch')")
    public CommonResult<Boolean> switchEnvironment(@RequestParam("environment") String environment) {
        eSignConfigService.switchEnvironment(environment);
        return success(true);
    }
    
    @GetMapping("/current-environment")
    @Operation(summary = "获取当前环境")
    @PreAuthorize("@ss.hasPermission('esign:config:query')")
    public CommonResult<String> getCurrentEnvironment() {
        return success(eSignConfigService.getCurrentEnvironment());
    }
    
    @PostMapping("/test")
    @Operation(summary = "测试配置连接")
    @Parameter(name = "id", description = "配置ID", required = true)
    @PreAuthorize("@ss.hasPermission('esign:config:test')")
    public CommonResult<ESignTestResultVO> testConfig(@RequestParam("id") Long id) {
        return success(eSignConfigService.testConfig(id));
    }
    
    @PutMapping("/update-status")
    @Operation(summary = "修改配置状态")
    @PreAuthorize("@ss.hasPermission('esign:config:update')")
    public CommonResult<Boolean> updateConfigStatus(@RequestParam("id") Long id,
                                                   @RequestParam("status") Integer status) {
        eSignConfigService.updateConfigStatus(id, status);
        return success(true);
    }
    
    @GetMapping("/history")
    @Operation(summary = "获取配置变更历史")
    @Parameter(name = "id", description = "配置ID", required = true)
    @PreAuthorize("@ss.hasPermission('esign:config:query')")
    public CommonResult<List<ESignConfigHistoryRespVO>> getConfigHistory(@RequestParam("id") Long id) {
        return success(eSignConfigService.getConfigHistory(id));
    }
}
```

## 6. 前端配置页面设计

### 6.1 配置列表页面

```vue
<template>
  <div class="app-container">
    <!-- 搜索工作栏 -->
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="环境类型" prop="environment">
        <el-select v-model="queryParams.environment" placeholder="请选择环境类型" clearable>
          <el-option label="测试环境" value="test" />
          <el-option label="生产环境" value="prod" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable>
          <el-option label="启用" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 操作工具栏 -->
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" 
                   v-hasPermi="['esign:config:create']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="info" plain icon="el-icon-sort" size="mini" @click="toggleExpandAll">展开/折叠</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <!-- 当前环境显示 -->
    <el-alert :title="`当前环境: ${currentEnvironment === 'test' ? '测试环境' : '生产环境'}`" 
              :type="currentEnvironment === 'test' ? 'warning' : 'success'" 
              show-icon :closable="false" class="mb8">
    </el-alert>

    <!-- 数据表格 -->
    <el-table v-loading="loading" :data="configList" row-key="id">
      <el-table-column label="环境类型" align="center" prop="environment" width="100">
        <template slot-scope="scope">
          <el-tag :type="scope.row.environment === 'test' ? 'warning' : 'success'">
            {{ scope.row.environment === 'test' ? '测试' : '生产' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="API地址" align="center" prop="apiUrl" :show-overflow-tooltip="true" />
      <el-table-column label="应用ID" align="center" prop="appId" :show-overflow-tooltip="true" />
      <el-table-column label="应用密钥" align="center" prop="appSecretMask" />
      <el-table-column label="状态" align="center" prop="status" width="80">
        <template slot-scope="scope">
          <el-switch v-model="scope.row.status" :active-value="1" :inactive-value="0" 
                     @change="handleStatusChange(scope.row)" />
        </template>
      </el-table-column>
      <el-table-column label="测试结果" align="center" prop="testResult" width="100">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.testResult === 1" type="success">成功</el-tag>
          <el-tag v-else-if="scope.row.testResult === 0" type="danger">失败</el-tag>
          <span v-else>未测试</span>
        </template>
      </el-table-column>
      <el-table-column label="最后测试时间" align="center" prop="lastTestTime" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.lastTestTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="300">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)"
                     v-hasPermi="['esign:config:update']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-connection" @click="handleTest(scope.row)"
                     v-hasPermi="['esign:config:test']">测试</el-button>
          <el-button size="mini" type="text" icon="el-icon-switch-button" @click="handleSwitch(scope.row)"
                     v-hasPermi="['esign:config:switch']" :disabled="scope.row.environment === currentEnvironment">
                     切换</el-button>
          <el-button size="mini" type="text" icon="el-icon-time" @click="handleHistory(scope.row)"
                     v-hasPermi="['esign:config:query']">历史</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)"
                     v-hasPermi="['esign:config:delete']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 添加或修改配置对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="600px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="环境类型" prop="environment">
          <el-radio-group v-model="form.environment" :disabled="form.id !== undefined">
            <el-radio label="test">测试环境</el-radio>
            <el-radio label="prod">生产环境</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="API服务地址" prop="apiUrl">
          <el-input v-model="form.apiUrl" placeholder="请输入API服务地址" />
        </el-form-item>
        <el-form-item label="应用ID" prop="appId">
          <el-input v-model="form.appId" placeholder="请输入应用ID" />
        </el-form-item>
        <el-form-item label="应用密钥" prop="appSecret">
          <el-input v-model="form.appSecret" type="password" placeholder="请输入应用密钥" show-password />
        </el-form-item>
        <el-form-item label="回调地址" prop="callbackUrl">
          <el-input v-model="form.callbackUrl" placeholder="请输入回调地址" />
        </el-form-item>
        <el-form-item label="连接超时时间" prop="connectTimeout">
          <el-input-number v-model="form.connectTimeout" :min="1000" :max="300000" :step="1000" />
          <span class="input-unit">毫秒</span>
        </el-form-item>
        <el-form-item label="读取超时时间" prop="readTimeout">
          <el-input-number v-model="form.readTimeout" :min="1000" :max="600000" :step="1000" />
          <span class="input-unit">毫秒</span>
        </el-form-item>
        <el-form-item label="配置描述" prop="description">
          <el-input v-model="form.description" type="textarea" placeholder="请输入配置描述" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listConfig, getConfig, delConfig, addConfig, updateConfig, 
         switchEnvironment, getCurrentEnvironment, testConfig, updateConfigStatus } from "@/api/esign/config";

export default {
  name: "ESignConfig",
  data() {
    return {
      // 遮罩层
      loading: true,
      // 显示搜索条件
      showSearch: true,
      // 配置表格数据
      configList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 当前环境
      currentEnvironment: 'test',
      // 查询参数
      queryParams: {
        pageNo: 1,
        pageSize: 10,
        environment: null,
        status: null
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        environment: [
          { required: true, message: "环境类型不能为空", trigger: "change" }
        ],
        apiUrl: [
          { required: true, message: "API服务地址不能为空", trigger: "blur" },
          { type: 'url', message: "请输入正确的URL格式", trigger: "blur" }
        ],
        appId: [
          { required: true, message: "应用ID不能为空", trigger: "blur" }
        ],
        appSecret: [
          { required: true, message: "应用密钥不能为空", trigger: "blur" }
        ]
      }
    };
  },
  created() {
    this.getList();
    this.getCurrentEnv();
  },
  methods: {
    /** 查询配置列表 */
    getList() {
      this.loading = true;
      listConfig(this.queryParams).then(response => {
        this.configList = response.data.list;
        this.total = response.data.total;
        this.loading = false;
      });
    },
    /** 获取当前环境 */
    getCurrentEnv() {
      getCurrentEnvironment().then(response => {
        this.currentEnvironment = response.data;
      });
    },
    /** 测试配置 */
    handleTest(row) {
      this.$modal.loading("正在测试连接...");
      testConfig(row.id).then(response => {
        this.$modal.closeLoading();
        if (response.data.success) {
          this.$modal.msgSuccess("连接测试成功");
        } else {
          this.$modal.msgError("连接测试失败: " + response.data.message);
        }
        this.getList();
      }).catch(() => {
        this.$modal.closeLoading();
      });
    },
    /** 切换环境 */
    handleSwitch(row) {
      this.$modal.confirm('是否确认切换到"' + (row.environment === 'test' ? '测试' : '生产') + '"环境？').then(() => {
        return switchEnvironment(row.environment);
      }).then(() => {
        this.getCurrentEnv();
        this.$modal.msgSuccess("环境切换成功");
      }).catch(() => {});
    },
    /** 状态修改 */
    handleStatusChange(row) {
      let text = row.status === 1 ? "启用" : "停用";
      this.$modal.confirm('确认要"' + text + '""' + (row.environment === 'test' ? '测试' : '生产') + '"环境配置吗？').then(() => {
        return updateConfigStatus(row.id, row.status);
      }).then(() => {
        this.$modal.msgSuccess(text + "成功");
      }).catch(() => {
        row.status = row.status === 0 ? 1 : 0;
      });
    }
    // ... 其他方法
  }
};
</script>
```

### 6.2 环境切换组件

```vue
<template>
  <div class="environment-switcher">
    <el-card class="box-card">
      <div slot="header" class="clearfix">
        <span>环境配置</span>
        <el-button style="float: right; padding: 3px 0" type="text" @click="refreshStatus">刷新</el-button>
      </div>
      
      <div class="environment-status">
        <el-row :gutter="20">
          <el-col :span="12">
            <div class="env-card" :class="{ active: currentEnvironment === 'test' }">
              <div class="env-header">
                <i class="el-icon-cpu"></i>
                <span>测试环境</span>
                <el-tag v-if="currentEnvironment === 'test'" type="warning" size="mini">当前</el-tag>
              </div>
              <div class="env-content">
                <p><strong>API地址:</strong> {{ testConfig.apiUrl || '未配置' }}</p>
                <p><strong>应用ID:</strong> {{ testConfig.appId || '未配置' }}</p>
                <p><strong>状态:</strong> 
                  <el-tag :type="testConfig.status === 1 ? 'success' : 'danger'" size="mini">
                    {{ testConfig.status === 1 ? '启用' : '禁用' }}
                  </el-tag>
                </p>
                <p><strong>连接状态:</strong>
                  <el-tag v-if="testConfig.testResult === 1" type="success" size="mini">正常</el-tag>
                  <el-tag v-else-if="testConfig.testResult === 0" type="danger" size="mini">异常</el-tag>
                  <el-tag v-else type="info" size="mini">未测试</el-tag>
                </p>
              </div>
              <div class="env-actions">
                <el-button size="mini" @click="testConnection('test')" :loading="testing.test">测试连接</el-button>
                <el-button size="mini" type="primary" @click="switchEnv('test')" 
                          :disabled="currentEnvironment === 'test' || testConfig.status !== 1">切换</el-button>
              </div>
            </div>
          </el-col>
          
          <el-col :span="12">
            <div class="env-card" :class="{ active: currentEnvironment === 'prod' }">
              <div class="env-header">
                <i class="el-icon-monitor"></i>
                <span>生产环境</span>
                <el-tag v-if="currentEnvironment === 'prod'" type="success" size="mini">当前</el-tag>
              </div>
              <div class="env-content">
                <p><strong>API地址:</strong> {{ prodConfig.apiUrl || '未配置' }}</p>
                <p><strong>应用ID:</strong> {{ prodConfig.appId || '未配置' }}</p>
                <p><strong>状态:</strong> 
                  <el-tag :type="prodConfig.status === 1 ? 'success' : 'danger'" size="mini">
                    {{ prodConfig.status === 1 ? '启用' : '禁用' }}
                  </el-tag>
                </p>
                <p><strong>连接状态:</strong>
                  <el-tag v-if="prodConfig.testResult === 1" type="success" size="mini">正常</el-tag>
                  <el-tag v-else-if="prodConfig.testResult === 0" type="danger" size="mini">异常</el-tag>
                  <el-tag v-else type="info" size="mini">未测试</el-tag>
                </p>
              </div>
              <div class="env-actions">
                <el-button size="mini" @click="testConnection('prod')" :loading="testing.prod">测试连接</el-button>
                <el-button size="mini" type="primary" @click="switchEnv('prod')" 
                          :disabled="currentEnvironment === 'prod' || prodConfig.status !== 1">切换</el-button>
              </div>
            </div>
          </el-col>
        </el-row>
      </div>
    </el-card>
  </div>
</template>

<script>
import { getConfigByEnvironment, getCurrentEnvironment, switchEnvironment, testConfig } from "@/api/esign/config";

export default {
  name: "EnvironmentSwitcher",
  data() {
    return {
      currentEnvironment: 'test',
      testConfig: {},
      prodConfig: {},
      testing: {
        test: false,
        prod: false
      }
    };
  },
  created() {
    this.loadConfigs();
  },
  methods: {
    async loadConfigs() {
      try {
        // 获取当前环境
        const currentEnvRes = await getCurrentEnvironment();
        this.currentEnvironment = currentEnvRes.data;
        
        // 获取测试环境配置
        const testConfigRes = await getConfigByEnvironment('test');
        this.testConfig = testConfigRes.data || {};
        
        // 获取生产环境配置
        const prodConfigRes = await getConfigByEnvironment('prod');
        this.prodConfig = prodConfigRes.data || {};
        
      } catch (error) {
        console.error('加载配置失败:', error);
      }
    },
    
    async testConnection(environment) {
      const config = environment === 'test' ? this.testConfig : this.prodConfig;
      if (!config.id) {
        this.$message.warning('请先配置' + (environment === 'test' ? '测试' : '生产') + '环境');
        return;
      }
      
      this.testing[environment] = true;
      try {
        const response = await testConfig(config.id);
        if (response.data.success) {
          this.$message.success('连接测试成功');
        } else {
          this.$message.error('连接测试失败: ' + response.data.message);
        }
        // 重新加载配置以更新测试结果
        this.loadConfigs();
      } catch (error) {
        this.$message.error('连接测试异常');
      } finally {
        this.testing[environment] = false;
      }
    },
    
    async switchEnv(environment) {
      try {
        await this.$confirm('确认要切换到' + (environment === 'test' ? '测试' : '生产') + '环境吗？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        });
        
        await switchEnvironment(environment);
        this.currentEnvironment = environment;
        this.$message.success('环境切换成功');
        
        // 通知父组件环境已切换
        this.$emit('environment-changed', environment);
        
      } catch (error) {
        if (error !== 'cancel') {
          this.$message.error('环境切换失败');
        }
      }
    },
    
    refreshStatus() {
      this.loadConfigs();
    }
  }
};
</script>

<style scoped>
.environment-switcher {
  margin-bottom: 20px;
}

.env-card {
  border: 2px solid #e4e7ed;
  border-radius: 8px;
  padding: 16px;
  transition: all 0.3s;
  height: 200px;
}

.env-card.active {
  border-color: #409eff;
  background-color: #f0f9ff;
}

.env-header {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
  font-weight: bold;
}

.env-header i {
  margin-right: 8px;
  font-size: 18px;
}

.env-content {
  margin-bottom: 16px;
  font-size: 13px;
}

.env-content p {
  margin: 4px 0;
  line-height: 1.5;
}

.env-actions {
  text-align: center;
}
</style>
```

## 7. 配置安全机制

### 7.1 密钥加密存储

```java
@Component
public class ESignSecurityManager {
    
    @Value("${yudao.esign.encrypt.key:default-encrypt-key}")
    private String encryptKey;
    
    /**
     * 加密应用密钥
     */
    public String encryptAppSecret(String appSecret) {
        try {
            return AESUtils.encrypt(appSecret, encryptKey);
        } catch (Exception e) {
            throw new ESignConfigException("密钥加密失败", e);
        }
    }
    
    /**
     * 解密应用密钥
     */
    public String decryptAppSecret(String encryptedAppSecret) {
        try {
            return AESUtils.decrypt(encryptedAppSecret, encryptKey);
        } catch (Exception e) {
            throw new ESignConfigException("密钥解密失败", e);
        }
    }
    
    /**
     * 脱敏显示密钥
     */
    public String maskAppSecret(String appSecret) {
        if (StringUtils.isBlank(appSecret)) {
            return "";
        }
        if (appSecret.length() <= 8) {
            return "****";
        }
        return appSecret.substring(0, 4) + "****" + appSecret.substring(appSecret.length() - 4);
    }
}
```

### 7.2 权限控制

```java
// 权限配置
public class ESignPermissions {
    public static final String CONFIG_CREATE = "esign:config:create";
    public static final String CONFIG_UPDATE = "esign:config:update";
    public static final String CONFIG_DELETE = "esign:config:delete";
    public static final String CONFIG_QUERY = "esign:config:query";
    public static final String CONFIG_TEST = "esign:config:test";
    public static final String CONFIG_SWITCH = "esign:config:switch";
}
```

## 8. 配置变更审计

### 8.1 操作日志记录

```java
@Component
public class ESignConfigAuditLogger {
    
    @Autowired
    private OperateLogService operateLogService;
    
    public void recordConfigOperation(String operation, Long configId, Object requestData) {
        OperateLogCreateReqDTO logReq = new OperateLogCreateReqDTO();
        logReq.setModule("电子签章配置");
        logReq.setName(operation);
        logReq.setType(OperateTypeEnum.UPDATE.getType());
        logReq.setContent("配置ID: " + configId + ", 操作: " + operation);
        logReq.setRequestMethod("POST");
        logReq.setRequestUrl("/admin-api/esign/config/" + operation.toLowerCase());
        logReq.setRequestParams(JSON.toJSONString(requestData));
        
        operateLogService.createOperateLog(logReq);
    }
    
    public void recordEnvironmentSwitch(String fromEnv, String toEnv) {
        OperateLogCreateReqDTO logReq = new OperateLogCreateReqDTO();
        logReq.setModule("电子签章配置");
        logReq.setName("环境切换");
        logReq.setType(OperateTypeEnum.UPDATE.getType());
        logReq.setContent("从 " + fromEnv + " 环境切换到 " + toEnv + " 环境");
        
        operateLogService.createOperateLog(logReq);
    }
}
```

## 9. 配置验证和测试

### 9.1 配置验证器

```java
@Component
public class ESignConfigValidator {
    
    /**
     * 验证配置有效性
     */
    public void validateConfig(ESignConfigCreateReqVO config) {
        // 1. 基础字段验证
        if (StringUtils.isBlank(config.getApiUrl())) {
            throw new IllegalArgumentException("API地址不能为空");
        }
        
        // 2. URL格式验证
        try {
            new URL(config.getApiUrl());
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("API地址格式不正确");
        }
        
        // 3. 环境唯一性验证
        // 4. 其他业务规则验证
    }
    
    /**
     * 测试配置连接
     */
    public boolean testConnection(String apiUrl, String appId, String appSecret) {
        try {
            // 调用e签宝API进行连接测试
            // 这里需要根据e签宝SDK的具体API来实现
            return true;
        } catch (Exception e) {
            log.error("配置连接测试失败", e);
            return false;
        }
    }
}
```

## 10. 部署和运维

### 10.1 配置文件示例

```yaml
# application-esign.yml
yudao:
  esign:
    # 加密密钥（生产环境请使用强密钥）
    encrypt:
      key: "your-32-char-encrypt-key-here"
    
    # 默认配置
    default:
      connect-timeout: 30000
      read-timeout: 60000
      
    # 缓存配置
    cache:
      ttl: 3600  # 缓存时间（秒）
      
    # 监控配置
    monitor:
      enabled: true
      alert-threshold: 0.8  # 失败率告警阈值
```

### 10.2 监控指标

```java
@Component
public class ESignConfigMonitor {
    
    private final MeterRegistry meterRegistry;
    private final Counter configSwitchCounter;
    private final Timer configTestTimer;
    
    public ESignConfigMonitor(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.configSwitchCounter = Counter.builder("esign.config.switch")
            .description("电子签章环境切换次数")
            .register(meterRegistry);
        this.configTestTimer = Timer.builder("esign.config.test")
            .description("配置测试耗时")
            .register(meterRegistry);
    }
    
    public void recordEnvironmentSwitch(String environment) {
        configSwitchCounter.increment(Tags.of("environment", environment));
    }
    
    public void recordConfigTest(String environment, boolean success, Duration duration) {
        configTestTimer.record(duration, Tags.of("environment", environment, "success", String.valueOf(success)));
    }
}
```

这个配置管理文档提供了完整的电子签章配置管理方案，包括数据结构设计、服务实现、前端页面、安全机制等各个方面。通过这套方案，可以实现灵活的多环境配置管理和安全的配置切换功能。 