# 企业模块核心业务流程文档

## 1. 概述

本文档描述了企业模块的核心业务流程，旨在阐明用户与系统、以及系统各组件之间的交互。
流程图主要使用 Mermaid 语法绘制。

## 2. 核心业务流程

### 2.1 企业入驻与认证流程

> **重要说明**：根据企业类型，系统采用两种不同的业务流程:
> 1. **企业入驻流程**：适用于回收企业、物流企业和处置企业。这些企业需要通过后台管理系统完成入驻申请、审核和认证流程。入驻企业的用户需要"企业申请人"角色，后续可获得"企业初级管理员"和"企业管理员"等角色。
> 2. **企业绑定流程**：仅适用于产废企业。产废企业的C端用户通过会员中心APP进行企业绑定，无需经过企业入驻审核流程。
>
> 以下描述的主要是企业入驻流程，即回收企业、物流企业和处置企业的流程。产废企业的绑定流程则相对简化。

此流程描述了一个新企业从注册意向到最终认证完成并可以开展业务的全过程。重点区分了"平台入驻审核"和"第三方真实性认证"两个独立阶段。

#### 2.1.1 整体流程概览

```mermaid
flowchart TD
    A[用户访问平台] --> B{已有平台账号?}
    B -- 否 --> C[注册平台账号]
    C --> D[登录平台]
    B -- 是 --> D

    D --> D1{内置租户管理员?}
    D1 -- 是 --> V[直接使用平台功能]

    D1 -- 否 --> D2{用户所属部门(或上级)\n是机构类型且已关联企业?}
    D2 -- 是 --> D3[系统自动创建用户与企业关联]
    D3 --> S[企业关联成功, 用户获得对应角色权限]
    S --> T[根据需要进入认证中心]
    T --> U[完成各项认证]
    U --> V

    D2 -- 否 --> E[进入企业中心/引导企业入驻]
    E --> F{选择操作}
    F -- 创建新企业 --> G[填写企业基本信息]
    G --> G1[选择企业类型]
    G1 --> H[提交企业入驻申请]
    H -- 系统创建企业记录 (状态: 入驻待审核) & 为申请人赋予临时管理权限 --> I[等待平台管理员入驻审核]

    F -- 关联已有企业 --> J[搜索企业]
    J --> J1{企业是否存在?}
    J1 -- 否 --> J2[提示企业不存在,引导创建或重新搜索]
    J2 --> E
    J1 -- 是 --> J3{是否允许直接关联/或需申请?}
    J3 -- 允许直接关联 (如邀请码) --> K[用户与企业关联成功]
    K --> S
    J3 -- 需要申请加入 --> J4[用户提交加入申请]
    J4 --> J5[等待企业管理员审核]
    J5 --> J6{审核通过?}
    J6 -- 是 --> K
    J6 -- 否 --> J7[通知用户申请被拒, 可重新操作]
    J7 --> E

    I --> Q1[管理员审核企业入驻材料]
    Q1 --> Q2{审核通过?}
    Q2 -- 是 --> S1[入驻审核通过, 状态更新为: 待启动认证]
    S1 --> S2[管理员根据企业类型为用户分配正式角色]
    S2 --> S3[引导用户进入认证中心完成实名认证]
    Q2 -- 否 --> Q3[企业状态更新为入驻审核拒绝]
    Q3 --> Q4[通知用户原因, 可重新申请]
    Q4 --> G

    S3 --> NC[用户访问认证中心]
    NC --> NC1{选择认证类型}
    NC1 -- 法人实名认证 --> L[法人代表实名认证]
    NC1 -- 企业实名认证 --> N[企业对公认证/三要素验证]

    L -- 通过e签宝 --> L1[提交法人身份信息]
    L1 --> L2[e签宝人脸/要素校验]
    L2 --> L3{认证成功?}
    L3 -- 是 --> M[法人认证成功记录]
    L3 -- 否 --> L4[提示失败原因, 可重试]
    L4 --> L

    N -- 通过e签宝 --> N1[提交企业名称、信用代码、法人信息]
    N1 --> N2[e签宝验证]
    N2 --> N3{验证成功?}
    N3 -- 是 --> O[企业自动认证成功]
    N3 -- 否 --> P[企业自动认证失败]

    M --> NC2{是否完成所有必要认证?}
    O --> NC2
    NC2 -- 是 --> FC[企业状态更新为已认证, 获得完整业务权限]
    NC2 -- 否 --> NC
    FC --> T

    P --> PR[转人工审核流程]
    PR --> PR1[认证管理员复核]
    PR1 --> PR2{复核通过?}
    PR2 -- 是 --> O
    PR2 -- 否 --> PR3[认证状态更新为失败]
    PR3 --> NC

    subgraph legenda[流程说明]
        direction LR
        manual[人工操作]
        system_auto[系统自动处理]
        esign[e签宝/第三方服务]
    end

    classDef manual fill:#f9f,stroke:#333,stroke-width:2px
    classDef system_auto fill:#ccf,stroke:#333,stroke-width:2px
    classDef esign fill:#cfc,stroke:#333,stroke-width:2px

    class C,D,E,F,G,G1,J,J4,L1,N1,Q1,PR1,T manual
    class H,K,L3,M,N3,O,PR2,S,S1,S2,S3,FC,U,V,D1,D2,D3,J1,J3,J5,J6,NC,NC1,NC2 system_auto
    class L2,N2 esign
```

#### 2.1.2 认证状态流转

```mermaid
stateDiagram-v2
    direction LR
    [*] --> 待提交基本信息
    待提交基本信息 --> 入驻待审核: 提交企业入驻申请
    入驻待审核 --> 入驻申请失败: 平台管理员审核拒绝
    入驻待审核 --> 入驻成功_待启动认证: 平台管理员审核通过
    入驻申请失败 --> 待提交基本信息: 重新提交申请
    
    入驻成功_待启动认证 --> 法人认证中: 用户启动法人实名认证
    入驻成功_待启动认证 --> 企业认证中: 用户启动企业三要素认证
    
    法人认证中 --> 法人认证失败: 认证失败
    法人认证中 --> 法人认证成功: 认证通过
    法人认证失败 --> 法人认证中: 重新发起认证
    
    企业认证中 --> e签宝认证中: 选择e签宝三要素
    e签宝认证中 --> 认证审核中: e签宝验证失败或需人工复核
    e签宝认证中 --> 企业认证成功: e签宝验证成功且无需复核
    
    企业认证中 --> 认证审核中: 选择人工审核
    
    认证审核中 --> 企业认证成功: 人工审核通过
    认证审核中 --> 企业认证失败: 人工审核拒绝
    企业认证失败 --> 企业认证中: 重新发起认证
    
    法人认证成功 --> 认证完成检查: 状态更新
    企业认证成功 --> 认证完成检查: 状态更新
    认证完成检查 --> 已认证: 所有必要认证均已完成
    认证完成检查 --> 部分认证完成: 仍有认证项未完成
    部分认证完成 --> 法人认证中: 继续未完成认证
    部分认证完成 --> 企业认证中: 继续未完成认证
    
    已认证 --> 待完善资质: 需上传业务资质
    已认证 --> 已禁用: 管理员操作
    已禁用 --> 已认证: 管理员操作

    待完善资质 --> 资质审核中: 提交资质文件
    资质审核中 --> 资质有效: 资质审核通过
    资质审核中 --> 资质无效: 资质审核拒绝
    资质有效 --> 已认证: 最终状态
```

**数据表关联:**
*   `enterprise_info`: 记录企业基本信息，`status` 字段反映当前整体状态，包括入驻审核状态和认证状态。
*   `enterprise_type`: 企业类型表，用于分类不同企业并关联不同角色模板。
*   `member_personal_auth`: 记录法人代表的个人实名认证过程和结果。
*   `enterprise_auth_record`: 详细记录每一项认证（如法人认证、企业三要素、人工审核）的步骤、状态、结果、e签宝流水等。
*   `enterprise_audit_log`: 记录管理员的审核操作，包括入驻审核和资质审核。
*   `system_role`: 系统角色表，包含"企业申请人"、"企业初级管理员(待认证)"、"企业管理员(已认证)"等角色。
*   `system_user_role`: 用户与角色关联表。

### 2.2 企业信息管理流程

#### 2.2.1 创建企业与入驻审核 (用户/管理员)
1.  **触发条件:** 用户在平台注册后，希望创建并入驻一个新的企业实体；或管理员代为录入企业。
2.  **流程:**
    *   用户/管理员通过接口 `POST /admin-api/enterprise/info/create` 提交企业核心信息（名称、信用代码、法人姓名等）与企业类型（如适用）。
    *   系统校验信息唯一性（如信用代码）。
    *   在 `enterprise_info` 表创建一条新记录，`status` 初始化为"**入驻待审核**"。
    *   系统为提交申请的用户自动关联一个临时的"**企业申请人**"角色，该角色允许用户在审核前管理其提交的申请资料和查看审核进度。
    *   平台管理员审核企业入驻申请：
        *   审核通过：企业状态更新为"**入驻成功_待启动认证**"，管理员根据企业类型为申请用户分配相应的初始管理员角色（如"企业初级管理员(待认证)"），同时撤销其"企业申请人"角色。用户获得基础操作权限，尤其是访问认证中心的权限。
        *   审核拒绝：企业状态更新为"**入驻申请失败**"，用户收到通知，可重新提交申请。
    *   审核后，如果是通过，系统引导用户进入认证中心完成后续的第三方认证。

#### 2.2.2 认证中心与真实性认证
1.  **触发条件:** 企业入驻申请被平台管理员审核通过后，企业管理员（申请人）需要完成真实性认证。
2.  **流程:**
    *   企业管理员进入"认证中心"，可以看到需要完成的认证项（通常包括法人实名认证和企业对公认证）。
    *   **法人实名认证**:
        *   通过e签宝等第三方服务进行人脸识别和身份要素验证。
        *   认证结果记录到 `member_personal_auth` 表。
    *   **企业对公认证**:
        *   通过e签宝等第三方服务进行企业三要素（企业名称、统一社会信用代码、法定代表人）验证。
        *   认证结果记录到 `enterprise_auth_record` 表。
    *   当所有必要的认证项都完成并通过后，企业状态自动更新为"**已认证**"，企业获得平台完整的业务功能权限。

#### 2.2.3 更新企业信息 (用户/管理员)
1.  **触发条件:** 企业信息发生变更，需要更新。
2.  **流程:**
    *   用户/管理员通过接口 `PUT /admin-api/enterprise/info/update` 提交要更新的企业信息。
    *   系统根据更新的字段判断是否涉及核心信息（如企业名称、法人、信用代码）。
    *   **核心信息变更:** 可能需要将企业状态置为"待重新认证"或触发特定的审核流程，并记录变更到 `enterprise_audit_log`。
    *   **非核心信息变更:** 直接更新 `enterprise_info` 表对应字段。
    *   所有变更操作记录到 `enterprise_audit_log` (如果配置了详细审计)。

#### 2.2.4 查询企业信息
*   **获取企业详情 (`GET /admin-api/enterprise/info/get`):**
    *   根据企业ID查询 `enterprise_info` 表获取基本信息。
    *   关联查询 `enterprise_auth_record` 获取最新认证状态和历史。
    *   关联查询 `enterprise_qualification` 获取已上传的资质列表。
    *   关联查询 `enterprise_store` 获取门店列表。
    *   关联查询 `enterprise_user_relation` 获取关联用户。
*   **分页查询企业列表 (`GET /admin-api/enterprise/info/page`):**
    *   管理员根据条件（名称、类型、状态等）从 `enterprise_info` 表分页查询。

### 2.3 企业资质管理流程

#### 2.3.1 添加企业资质
1.  **触发条件:** 企业完成核心认证后，需要上传和管理其经营所需的各类资质证书。
2.  **流程:**
    *   企业用户通过接口 `POST /admin-api/enterprise/qualification/create` 提交资质信息（资质类型、名称、编号、有效期、发证机关、资质文件附件ID）。
    *   系统在 `enterprise_qualification` 表创建一条新记录，`status` 初始化为"待审核"或根据配置直接为"有效"（如果无需审核）。
    *   如果需要审核，则进入资质审核子流程。

#### 2.3.2 资质审核 (如果启用)
1.  **触发条件:** 新资质提交且系统配置需要审核，或资质信息更新触发重新审核。
2.  **流程:**
    *   管理员在后台查看待审核资质列表。
    *   管理员审核资质文件和信息的真实性、有效性。
    *   通过接口 `PUT /admin-api/enterprise/qualification/audit` (假设) 更新资质记录的 `status`（有效/审核拒绝）和 `audit_remarks`。
    *   操作记录到 `enterprise_audit_log`。

#### 2.3.3 资质到期提醒 (系统功能)
1.  **触发条件:** 定时任务扫描 `enterprise_qualification` 表。
2.  **流程:**
    *   系统检查 `expiry_date` 字段，对于即将到期（如提前30天、15天、7天）的资质，生成提醒通知。
    *   通过站内信、邮件、短信等方式通知企业管理员。

### 2.4 多门店管理流程 (US-015 相关)

#### 2.4.1 创建门店
1.  **触发条件:** 已认证的企业希望添加下属门店。
2.  **流程:**
    *   企业管理员通过接口 `POST /admin-api/enterprise/store/create` 提交门店信息（名称、地址、联系方式等，可关联 `parent_id` 形成层级）。
    *   系统在 `enterprise_store` 表创建记录。

#### 2.4.2 门店数据隔离与汇总
*   **数据隔离:** 门店用户登录后，其查询和操作的数据范围（如订单、客户）应基于其所属的 `store_id` 进行过滤。
*   **数据汇总:** 总部用户（企业管理员）在查看报表或数据时，系统能汇总其下所有门店的数据；同时提供按门店筛选查看的能力。

### 2.5 用户与企业/门店关系管理流程

#### 2.5.1 关联用户到企业/门店
1.  **触发条件:** 企业管理员添加员工，或用户接受企业邀请。
2.  **流程:**
    *   通过接口 `POST /admin-api/enterprise/user-relation/add`。
    *   在 `enterprise_user_relation` 表创建记录，指定 `user_id`, `enterprise_id`, `store_id` (可选), `relation_type`。
    *   同时可能需要为用户分配相应的系统角色（关联 `system_role` 和 `system_user_role`）。

#### 2.5.2 角色与权限管理
1.  **企业申请人角色**：提交企业入驻申请的用户获得此临时角色，具有查看和管理自己申请的权限。
2.  **企业初级管理员(待认证)角色**：入驻审核通过后，申请人被授予此角色，拥有基础企业信息管理权限和访问认证中心的权限。
3.  **企业管理员(已认证)角色**：企业完成所有必要第三方认证后，管理员角色获得完整业务功能权限。
4.  **权限动态校验**：系统可能会针对某些敏感操作，额外检查关联企业的认证状态（如只有"已认证"状态的企业才能发起某些业务交易）。

### 2.6 危废转移订单流程 (收货确认与付款触发)

#### 2.6.1 订单状态流转优化

危废转移订单的状态流转已针对您的业务需求进行了优化，明确了收货确认与付款触发的关系：

```mermaid
stateDiagram-v2
    direction LR
    [*] --> 待分配物流
    待分配物流 --> 待收运 : 物流已指派
    待收运 --> 运输中 : 物流方确认装货
    运输中 --> 已确认收货 : 收运员现场确认收货
    已确认收货 --> 待车辆过磅 : 💰付款方式选择完成
    待车辆过磅 --> 车辆已过磅 : 车辆过磅完成
    车辆已过磅 --> 订单分摊完成 : 按比例分摊到各订单
    订单分摊完成 --> 已完成 : 业务结束
    
    运输中 --> 已取消 : 异常取消
    待分配物流 --> 已取消 : 取消订单
    
    note right of 已确认收货 : 三种付款方式：\n1.现金已付\n2.立即付款\n3.待付款
    note right of 车辆已过磅 : 车辆级过磅，非单个订单
    note right of 订单分摊完成 : 按预估量比例分摊车辆总重量
```

#### 2.6.2 核心业务原则

**重要变更**：
1. **收货确认 = 付款触发**：收运员确认收货时选择付款方式，立即或后续触发付款
2. **车辆级过磅**：针对整车进行过磅，而非单个订单
3. **订单分摊机制**：车辆总过磅量按预估量比例分摊到各个订单
4. **三重核准**：出车预估量 vs 收运员确认量 vs 实际过磅量的相互核准

#### 2.6.3 收货确认与付款配置流程 (重新设计)

```mermaid
sequenceDiagram
    participant Driver as 收运员
    participant System as 系统
    participant Producer as 产废企业
    participant RecyclingFin as 回收企业财务
    participant Payment as 支付系统

    Note over Driver,System: 1. 收运员到达现场，准备确认收货
    Driver->>System: 获取产废企业付款配置
    System->>System: 查询付款配置表
    
    alt 配置为对公结算
        System->>Driver: 显示"对公结算，仅需确认收货"
        Driver->>System: 确认收货(无需选择付款方式)
        System->>System: 订单状态→已确认收货，付款状态→待凭证上传
        System->>RecyclingFin: 通知需要上传对公付款凭证
        Note over RecyclingFin: 线下转账后上传凭证
        RecyclingFin->>System: 上传银行转账凭证
        System->>Producer: 通知确认付款凭证
        Producer->>System: 确认凭证有效
        System->>System: 付款状态→凭证已确认
        
    else 配置为个人结算且已设置收款人
        System->>Driver: 显示收款人信息，可选择付款方式
        Driver->>System: 选择付款方式(现金已付/立即付款/待付款)
        alt 现金已付
            Driver->>System: 录入现金金额+收据照片
            System->>Producer: 现金收款确认
            System->>System: 付款状态→已付款
        else 立即付款
            System->>Payment: 调用支付接口
            Payment->>Producer: 线上转账到预设账户
            System->>Producer: 付款成功通知
            System->>System: 付款状态→已付款
        else 待付款
            System->>RecyclingFin: 通知需要后续付款
            System->>System: 付款状态→未付款
        end
        
    else 配置为个人结算但未设置收款人
        System->>Driver: 显示"仅确认收货，收款人信息待补充"
        Driver->>System: 确认收货(无付款选择)
        System->>System: 付款状态→待填写收款人信息
        System->>RecyclingFin: 通知需要填写收款人信息
        RecyclingFin->>System: 填写收款人详细信息
        System->>Producer: 请求确认收款人信息
        Producer->>System: 确认收款人信息正确
        System->>System: 可以进行付款流程
        Note over RecyclingFin: 然后选择付款方式进行付款
        
    else 无付款配置
        System->>Driver: 显示"企业未配置付款方式"
        Driver->>System: 仅确认收货
        System->>RecyclingFin: 通知需要联系产废企业配置付款方式
        System->>System: 付款状态→配置待完善
    end
    
    System->>System: 订单状态 → 已确认收货
    System->>System: 记录收货时间、地点、付款配置ID
```

#### 2.6.4 车辆过磅与订单分摊流程

```mermaid
sequenceDiagram
    participant Vehicle as 收运车辆
    participant WeighingOp as 过磅员
    participant System as 系统
    participant Orders as 相关订单

    Note over Vehicle: 一车收集多个订单
    Vehicle->>WeighingOp: 回到回收企业
    WeighingOp->>System: 记录车辆过磅数据
    
    System->>System: 计算三重核准差异
    Note over System: 预估量 vs 确认量 vs 过磅量
    
    alt 差异在正常范围内
        System->>Orders: 按预估量比例分摊总重量
        System->>Orders: 重新计算最终结算金额
        System->>System: 订单状态 → 订单分摊完成
    else 差异超过阈值
        System->>System: 触发异常处理
        System->>WeighingOp: 需要人工审核确认
        Note over System: 暂停自动分摊
    end
```

#### 2.6.5 现金付款对账流程

```mermaid
sequenceDiagram
    participant Driver as 收运员
    participant Producer as 产废企业  
    participant LogisticsFin as 物流财务
    participant RecyclingFin as 回收企业财务
    participant System as 系统

    Note over Driver,Producer: 现场现金付款
    Driver->>Producer: 支付现金(如800元)
    Driver->>System: 录入现金付款记录
    
    Note over System: 车辆过磅完成后
    System->>System: 计算订单分摊金额(如735元)
    System->>System: 发现差额(800-735=65元)
    
    System->>LogisticsFin: 生成垫付差额记录
    System->>RecyclingFin: 需退还65元给物流企业
    
    RecyclingFin->>LogisticsFin: 确认差额处理
    System->>System: 完成对账，订单结束
```

### 2.7 三重核准业务逻辑

#### 2.7.1 核准环节说明

1. **出车预估量**：收运车辆出发前，系统汇总该车计划收集的所有订单预估量
2. **收运员确认量**：收运员在各个产废企业现场确认实际收集的数量
3. **实际过磅量**：车辆回到回收企业后，整车过磅获得的净重量

#### 2.7.2 差异分析与责任判定

```mermaid
flowchart TD
    A[车辆过磅完成] --> B{计算总体差异率}
    B -->|差异≤5%| C[正常范围]
    B -->|5%<差异≤10%| D[轻微异常]
    B -->|差异>10%| E[严重异常]
    
    C --> F[自动分摊到各订单]
    D --> G[标记异常但继续分摊]
    E --> H[暂停分摊，人工审核]
    
    F --> I[订单分摊完成]
    G --> I
    H --> J[异常处理完成] --> I
    
    I --> K[更新最终结算金额]
    K --> L[处理现金付款差额]
    L --> M[订单完成]
```

#### 2.7.3 典型业务场景

**场景示例：一车收集3个订单**

| 环节 | 订单A | 订单B | 订单C | 总计 |
|------|-------|-------|-------|------|
| **出车预估量** | 2.0吨 | 1.5吨 | 1.5吨 | 5.0吨 |
| **收运员确认量** | 2.1吨 | 1.4吨 | 1.6吨 | 5.1吨 |
| **车辆过磅量** | - | - | - | 4.9吨 |
| **分摊后数量** | 1.96吨 | 1.47吨 | 1.47吨 | 4.9吨 |
| **分摊比例** | 40% | 30% | 30% | 100% |

**付款方式与最终结算：**
- 订单A：立即付款 → 已收1000元 → 分摊后应收980元 → 多收20元
- 订单B：现金已付800元 → 分摊后应收735元 → 多收65元，需退还物流企业
- 订单C：待付款 → 分摊后应付735元 → 管理员确认付款

### 2.8 系统集成接口要求

#### 2.8.1 地磅设备集成

1. **接口类型**：支持串口(RS232/RS485)、网络接口(TCP/IP)
2. **数据格式**：重量数据实时推送，JSON格式
3. **异常处理**：设备离线自动切换手动录入
4. **数据校验**：皮重、毛重、净重的逻辑校验

#### 2.8.2 支付接口集成

1. **工行反向开票接口**：企业对个人付款+自动开票
2. **第三方支付接口**：微信企业付款、支付宝等
3. **银行接口**：对公转账接口
4. **支付状态同步**：实时回调确认支付结果

#### 2.8.3 国家固废系统对接

1. **联单数据同步**：订单完成后自动推送
2. **运输轨迹上报**：GPS定位数据定时上传
3. **异常情况报告**：超时、路线偏离等异常自动上报

### 2.9 业务规则配置

#### 2.9.1 过磅差异阈值配置

- **正常范围**：±5% (系统自动处理)
- **异常预警**：±5%~±10% (记录异常但继续处理)  
- **严重异常**：>±10% (暂停处理，人工介入)

#### 2.9.2 现金付款管理

- **差额阈值**：差额≤10元自动处理，>10元需确认
- **对账周期**：按月批量对账
- **退款处理**：支持现金退款和下次订单抵扣

#### 2.9.3 订单分摊规则

- **默认方式**：按预估量比例分摊
- **备选方式**：按确认量比例、平均分摊、人工指定
- **调整权限**：财务主管以上权限可人工调整分摊结果

### 2.10 产废企业付款配置管理流程

#### 2.10.1 付款方式配置流程

```mermaid
flowchart TD
    A[产废企业管理员] --> B{选择配置类型}
    
    B --> C[对公结算配置]
    B --> D[个人结算配置]
    
    C --> C1[填写企业银行信息]
    C1 --> C2[配置税号和发票信息]
    C2 --> C3[设置付款阈值规则]
    C3 --> C4[提交配置审核]
    
    D --> D1[添加个人收款人]
    D1 --> D2[填写收款人详细信息]
    D2 --> D3[设置收款人关系]
    D3 --> D4[配置自动付款规则]
    D4 --> D5[提交配置审核]
    
    C4 --> E[系统管理员审核]
    D5 --> E
    
    E --> F{审核结果}
    F -->|通过| G[配置生效]
    F -->|拒绝| H[通知修改原因]
    
    G --> I[订单可使用此配置]
    H --> B
    
    I --> J[收运员确认收货时应用配置]
```

#### 2.10.2 付款配置优先级规则

```mermaid
flowchart TD
    A[订单产生] --> B{是否指定特殊配置?}
    B -->|是| C[使用订单指定配置]
    B -->|否| D{门店级别是否有配置?}
    
    D -->|是| E[使用门店级配置]
    D -->|否| F{企业级别是否有默认配置?}
    
    F -->|是| G[使用企业默认配置]
    F -->|否| H[无配置，需要临时处理]
    
    C --> I[配置验证]
    E --> I
    G --> I
    H --> J[通知需要配置]
    
    I --> K{配置是否有效?}
    K -->|是| L[应用到订单]
    K -->|否| J
    
    L --> M[收运员确认收货时使用]
```

## 3. 关键接口交互说明 (时序图示例)

### 3.1 企业通过e签宝认证时序图 (简化)

```mermaid
sequenceDiagram
    actor User as 用户/前端
    participant Gateway as API网关
    participant EnterpriseService as 企业服务
    participant MemberService as 用户/认证服务
    participant ESignService as e签宝适配服务
    participant ESignPlatform as e签宝平台

    User->>Gateway: POST /admin-api/enterprise/info/create
    Gateway->>EnterpriseService: createEnterprise(info)
    EnterpriseService->>EnterpriseService: 设置状态为入驻待审核
    EnterpriseService->>EnterpriseService: 分配临时企业申请人角色
    EnterpriseService-->>Gateway: enterpriseId
    Gateway-->>User: 企业ID
    
    Note over User,Gateway: 平台管理员审核入驻申请
    
    User->>Gateway: POST /admin-api/enterprise/certification/initiate-personal-auth
    Gateway->>MemberService: submitPersonalAuth(userId, realName, idCardNo)
    MemberService->>ESignService: initiateFaceAuth(realName, idCardNo)
    ESignService->>ESignPlatform: 请求人脸识别/要素认证
    ESignPlatform-->>ESignService: 认证流程ID, 认证URL/结果
    ESignService-->>MemberService: authFlowId, status
    alt 认证成功
        MemberService->>MemberService: 更新状态为认证通过
    else 认证失败
        MemberService->>MemberService: 更新状态为认证失败
    end
    MemberService-->>Gateway: 法人认证结果
    Gateway-->>User: 法人认证结果

    User->>Gateway: POST /admin-api/enterprise/certification/initiate-enterprise-auth
    Gateway->>EnterpriseService: submitEnterpriseAuth(enterpriseId)
    EnterpriseService->>ESignService: initiateEnterpriseAuth(name, creditCode)
    ESignService->>ESignPlatform: 请求企业三要素验证
    ESignPlatform-->>ESignService: 验证结果
    ESignService-->>EnterpriseService: authResult
    alt 验证成功
        EnterpriseService->>EnterpriseService: 更新企业认证状态为成功
        EnterpriseService->>EnterpriseService: 检查所有必要认证是否完成
        alt 所有认证已完成
            EnterpriseService->>EnterpriseService: 更新企业状态为已认证
            EnterpriseService->>EnterpriseService: 更新用户角色为企业管理员(已认证)
            opt 区块链存证
                EnterpriseService->>ESignService: saveBlockchainProof()
                ESignService-->>EnterpriseService: txHash
                EnterpriseService->>EnterpriseService: 更新txHash
            end
        else 还有认证未完成
            EnterpriseService->>EnterpriseService: 保持企业状态为入驻成功_待启动认证
        end
    else 验证失败或需人工
        EnterpriseService->>EnterpriseService: 更新状态为认证中/审核中
        EnterpriseService->>EnterpriseService: 创建记录失败/待审核
    end
    EnterpriseService-->>Gateway: 企业认证结果
    Gateway-->>User: 企业认证结果
``` 