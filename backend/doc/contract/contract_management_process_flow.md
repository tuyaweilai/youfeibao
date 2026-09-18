# 合同管理模块 - 业务流程图文档 (Business Process Flow Diagram Document)

以下为合同管理模块的核心业务流程，使用Mermaid语法描述。

### 2.1. 流程1: 新建电子合同并发起签署

```mermaid
sequenceDiagram
    participant User as 企业用户
    participant System as 系统平台
    participant Reviewer as 内部审批人 (可选)
    participant ESign as 电子签章服务 (如e签宝)
    participant Signatory1 as 签署方1
    participant Signatory2 as 签署方2

    User->>System: 请求创建新合同
    System-->>User: 显示合同类型和模板选择
    User->>System: 选择类型/模板，填写合同信息，指定签署方
    alt 内部审批流程
        User->>System: 提交合同草稿以供审批
        System->>Reviewer: 通知审批请求
        Reviewer->>System: 审阅合同 (批准/拒绝/修改意见)
        System-->>User: 返回审批结果
        User->>System: (若有修改) 更新合同信息
    end
    User->>System: 确认并发起签署流程
    System->>ESign: 发起电子合同签署请求 (含合同文档, 签署方信息)
    ESign-->>System: 返回签署流程ID/链接
    System->>Signatory1: 发送签署通知 (含链接)
    System->>Signatory2: 发送签署通知 (含链接)
    Signatory1->>ESign: 访问链接，查看合同并签署
    ESign-->>System: Webhook: Signatory1已签署
    Signatory2->>ESign: 访问链接，查看合同并签署
    ESign-->>System: Webhook: Signatory2已签署 (合同签署完成)
    System->>System: 更新合同状态为"已生效"
    System->>User: 通知合同已生效
    System->>Signatory1: 通知合同已生效
    System->>Signatory2: 通知合同已生效
```

### 2.2. 流程2: 上传已签署的纸质合同

```mermaid
sequenceDiagram
    participant User as 企业用户
    participant System as 系统平台
    participant Admin as 平台管理员 (可选审核)

    User->>System: 选择上传纸质合同功能
    System-->>User: 提供上传界面和元数据填写表单
    User->>System: 上传合同扫描件，填写合同编号、名称、双方、生效/失效日期等元数据
    User->>System: 提交纸质合同信息
    alt 管理员审核流程
        System->>Admin: 通知有新的纸质合同待审核
        Admin->>System: 审核合同信息和扫描件 (通过/驳回)
        System-->>User: 反馈审核结果
        User->>System: (若驳回) 修改信息或重新上传
    end
    System->>System: 将合同信息存档，状态更新为"已生效" (或根据审核结果)
    System-->>User: 提示合同已成功上传并激活
```

### 2.3. 流程3: 合同到期与续约提醒

```mermaid
sequenceDiagram
    participant System as 系统平台
    participant User as 企业用户 (合同负责人)

    loop 每日/定期检查
        System->>System: 检查所有"已生效"合同的失效日期
    end

    alt 合同即将到期 (如提前30天)
        System->>User: 发送合同"[合同名称]"即将到期提醒 (邮件/站内信)
    end

    User->>System: (收到提醒后) 查看合同详情
    User->>System: 选择发起续约流程 (可能创建新版本或新合同)
    Note right of User: 后续流程类似新合同创建与签署

    alt 合同已到期
        System->>System: 更新合同状态为"已过期"
        System->>User: 发送合同"[合同名称]"已过期通知
    end
```
### 2.4. 流程4: 业务办理时校验关联合同
```mermaid
sequenceDiagram
    participant User as 企业用户
    participant System as 系统平台
    participant ContractModule as 合同模块

    User->>System: 请求办理业务 (如: 创建危废转移订单)
    System->>ContractModule: 校验发起方与接收方之间是否存在有效的相关合同
    ContractModule->>ContractModule: 查询相关合同类型和有效期
    alt 存在有效合同
        ContractModule->>ContractModule: 校验合同是否在有效期内，范围是否覆盖当前业务
        alt 合同有效且适用
             ContractModule-->>System: 返回有效合同信息
             System-->>User: 允许继续办理业务，并关联订单与合同
        else 合同不适用或已过期
             ContractModule-->>System: 返回合同状态异常
             System-->>User: 提示"合同无效/过期/不适用"，阻止业务办理
             System-->>User: 引导用户签署新合同或续约
        end
    else 无有效合同
        ContractModule-->>System: 返回无有效合同
        System-->>User: 提示"无有效合同"，阻止业务办理
        System-->>User: 引导用户签署新合同
    end
```

### 2.5. 流程5: 合同到期自动提醒与处理
```mermaid
sequenceDiagram
    participant System as 系统平台
    participant ContractModule as 合同模块
    participant NotificationService as 通知服务
    participant User as 企业用户

    loop 每日定时任务
        System->>ContractModule: 检查即将到期的合同
        ContractModule->>ContractModule: 查询30天、15天、7天内到期的合同
        ContractModule-->>System: 返回即将到期合同列表
        
        loop 处理每个即将到期合同
            System->>NotificationService: 发送到期提醒通知
            NotificationService->>User: 发送邮件/站内信/短信提醒
            System->>ContractModule: 更新合同状态为"即将到期"
        end
        
        ContractModule->>ContractModule: 检查已到期合同
        ContractModule-->>System: 返回已到期合同列表
        
        loop 处理每个已到期合同
            System->>ContractModule: 更新合同状态为"已到期"
            System->>NotificationService: 发送到期通知
            NotificationService->>User: 发送到期通知
            System->>System: 检查关联业务，标记为不可用
        end
    end
```

### 2.6. 流程6: 合同统计与分析
```mermaid
sequenceDiagram
    participant User as 企业管理员
    participant System as 系统平台
    participant ContractModule as 合同模块
    participant AnalyticsService as 分析服务

    User->>System: 请求查看合同统计报表
    System->>ContractModule: 查询合同数据
    ContractModule->>ContractModule: 统计各状态合同数量
    ContractModule->>ContractModule: 计算签署效率和周期
    ContractModule-->>AnalyticsService: 提供原始数据
    
    AnalyticsService->>AnalyticsService: 生成统计图表
    AnalyticsService->>AnalyticsService: 分析趋势和异常
    AnalyticsService-->>System: 返回分析结果
    
    System-->>User: 展示统计报表和分析结果
    
    alt 用户请求导出报表
        User->>System: 请求导出数据
        System->>AnalyticsService: 生成导出文件
        AnalyticsService-->>System: 返回文件下载链接
        System-->>User: 提供文件下载
    end
``` 