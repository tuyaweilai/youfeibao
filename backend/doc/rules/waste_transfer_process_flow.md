# 危险废物转移模块核心业务流程文档

## 1. 概述

本文档使用Mermaid流程图描述危险废物转移模块中的关键业务流程，包括预约发起、回收方分配、订单生成与处理、临时订单创建等，旨在清晰地展示各角色之间的交互和系统处理逻辑。

## 2. 核心业务流程

### 2.1 产废企业发起预约及回收方分配流程

*关联用户故事：US-003, US-027, US-028, US-029*

```mermaid
flowchart TD
    A[产废企业APP/PC端] --> B(填写预约信息: 废物类型、数量、时间、地点)
    B --> C{系统配置: 允许手动选择回收方?}
    C -- 是 --> D[显示签约回收企业列表]
    D --> E{用户选择回收企业?}
    E -- 是 --> F[用户选定回收企业]
    F --> H[生成预约单 指定回收方]
    E -- 否 --> G[系统进入自动分配逻辑]
    C -- 否 --> G

    subgraph AutoAssign [系统自动分配逻辑 US-028]
        direction LR
        G1[读取区域分配规则 US-027]
        G1 --> G2{根据产废企业区域匹配规则?}
        G2 -- 是 --> G3[指派规则指定的回收企业]
        G3 --> G5[分配成功]
        G2 -- 否 --> G4[执行备选/默认分配策略 或 通知管理员]
        G4 --> G5
    end
    G --> G1
    G5 --> H

    H --> I{管理员是否手动变更回收方? US-029}
    I -- 是 --> J[管理员在后台修改预约单的回收企业]
    J --> K[更新预约单回收方 手动指定优先]
    I -- 否 --> K

    K --> L[通知指定的回收企业有新预约]
    L --> M[预约单状态: 待回收方处理]

    classDef userAction fill:#f9f,stroke:#333,stroke-width:2px
    classDef systemLogic fill:#ccf,stroke:#333,stroke-width:2px
    classDef adminAction fill:#ffc,stroke:#333,stroke-width:2px

    class A,B,D,E,F userAction
    class C,G,G1,G2,G3,G4,G5,H,K,L,M systemLogic
    class I,J adminAction
```

### 2.2 回收企业处理预约并生成订单流程

*关联用户故事：US-004, US-005*

```mermaid
flowchart TD
    A[回收企业收到新预约通知] --> B[登录系统/APP查看待处理预约]
    B --> C[查看预约详情]
    C --> D{接收此预约?}
    D -- 是 --> E[确认接收预约]
    E --> F[系统生成订单 关联预约单]
    F --> G[订单状态: 待指派物流/待处理]
    G --> H{回收企业指派物流? US-005}
    H -- 是 --> I[选择物流类型/关联物流公司]
    I --> J[更新订单物流信息, 推送至物流方]
    H -- 否 --> K[订单进入后续环节]
    J --> K

    D -- 否 --> L[填写拒绝原因]
    L --> M[提交拒绝]
    M --> N[预约单状态: 已拒绝]
    N --> O[通知产废企业预约被拒绝]

    classDef recyclerAction fill:#f9f,stroke:#333,stroke-width:2px
    classDef systemLogic fill:#ccf,stroke:#333,stroke-width:2px

    class A,B,C,D,E,H,I,L,M recyclerAction
    class F,G,J,K,N,O systemLogic
```

### 2.3 临时订单（扫街回收）创建流程

*关联用户故事：US-017*

```mermaid
flowchart TD
    A[司机发现临时回收需求] --> B[打开物流APP]
    B --> C[选择"上报临时需求"功能]
    C --> D[填写基本信息：废物类型、预估数量、产废方信息]
    D --> E[获取当前GPS位置]
    E --> F[拍摄现场照片]
    F --> G[提交临时需求]
    
    G --> H{物流模块处理}
    H --> I[调用危废模块API创建订单]
    I --> J[危废模块生成订单]
    J --> K[返回订单信息给物流模块]
    K --> L[物流模块创建运输任务]
    L --> M[关联订单和任务]
    M --> N[返回成功信息给司机]
    
    N --> O[司机看到新任务]
    O --> P[开始执行运输任务]

    classDef driverAction fill:#f9f,stroke:#333,stroke-width:2px
    classDef logisticsModule fill:#ccf,stroke:#333,stroke-width:2px
    classDef wasteModule fill:#cfc,stroke:#333,stroke-width:2px

    class A,B,C,D,E,F,G,O,P driverAction
    class H,I,K,L,M,N logisticsModule
    class J wasteModule
```

### 2.4 智能报价流程（竞价模式 vs 非竞价模式）

*关联用户故事：US-037, US-038, US-045, US-046*

```mermaid
flowchart TD
    A[产废企业发起预约] --> B[系统读取回收企业业务模式配置]
    B --> C{回收企业配置的业务模式}
    
    C -- 独立运营模式 --> D[非竞价流程]
    C -- 平台竞价模式 --> E[竞价流程]
    C -- 混合模式 --> F{检查产废企业类型配置}
    F -- 该客户设置为竞价 --> E
    F -- 该客户设置为直接报价 --> D

    subgraph NonCompetitive [非竞价模式 - 直接报价]
        direction TB
        D --> D1[系统应用回收企业价格策略]
        D1 --> D2{查找客户专属价格}
        D2 -- 有专属价格 --> D3[应用客户专属价格]
        D2 -- 无专属价格 --> D4{查找区域价格}
        D4 -- 有区域价格 --> D5[应用区域价格]
        D4 -- 无区域价格 --> D6[应用基础价格]
        D3 --> D7[自动生成固定报价]
        D5 --> D7
        D6 --> D7
        D7 --> D8[展示报价给产废企业]
        D8 --> D9[产废企业确认接受]
        D9 --> D10[生成订单]
    end

    subgraph Competitive [竞价模式 - 多方报价]
        direction TB
        E --> E1[推送预约给已签约的回收企业]
        E1 --> E2[各回收企业基于价格策略制定报价]
        E2 --> E3[在规定时间内提交报价]
        E3 --> E4[产废企业比较报价]
        E4 --> E5{选择报价}
        E5 -- 接受某个报价 --> E6[确认选定的回收企业]
        E5 -- 超时未选择 --> E7[报价失效]
        E6 --> E8[生成订单与选定的回收企业]
        E7 --> E9[通知回收企业报价失效]
    end

    D10 --> G[进入物流指派流程]
    E8 --> G
    
    classDef systemLogic fill:#ccf,stroke:#333,stroke-width:2px
    classDef userAction fill:#f9f,stroke:#333,stroke-width:2px
    classDef autoProcess fill:#cfc,stroke:#333,stroke-width:2px

    class A,D8,D9,E4,E5,E6 userAction
    class B,C,F,D1,D2,D4,D7,E1,E3 systemLogic
    class D3,D5,D6,E2,E7,E9 autoProcess
```

### 2.5 价格策略应用流程

*关联用户故事：US-046, US-047, US-048*

```mermaid
flowchart TD
    A[价格查询请求] --> B[获取产废企业信息]
    B --> C[获取回收企业ID]
    C --> D[读取价格策略配置]
    
    D --> E{是否有客户专属价格?}
    E -- 是 --> F[检查专属价格有效期]
    F --> G{专属价格是否有效?}
    G -- 是 --> H[返回客户专属价格 优先级=1]
    G -- 否 --> I[检查区域价格]
    
    E -- 否 --> I
    I --> J{是否有适用的区域价格?}
    J -- 是 --> K[检查区域价格有效期]
    K --> L{区域价格是否有效?}
    L -- 是 --> M[返回区域价格 优先级=2]
    L -- 否 --> N[使用基础价格]
    
    J -- 否 --> N
    N --> O[返回基础价格 优先级=3]
    
    H --> P[记录价格应用日志]
    M --> P
    O --> P
    P --> Q[返回最终价格及优先级说明]

    classDef priceLevel1 fill:#ffcccc,stroke:#333,stroke-width:2px
    classDef priceLevel2 fill:#ffffcc,stroke:#333,stroke-width:2px
    classDef priceLevel3 fill:#ccffcc,stroke:#333,stroke-width:2px

    class H priceLevel1
    class M priceLevel2
    class O priceLevel3
```

### 2.6 订单状态流转图

```mermaid
stateDiagram-v2
    direction LR
    [*] --> 待处理
    待处理 --> 待指派物流 : 回收方接单
    待处理 --> 已取消 : 用户/管理员取消
    待处理 --> 已拒绝 : 回收方拒绝-针对预约

    待指派物流 --> 待收运 : 物流已指派
    待指派物流 --> 已取消 : 回收方/管理员取消
    
    待收运 --> 运输中 : 物流方确认装货
    运输中 --> 待过磅卸货 : 到达回收站点
    运输中 --> 运输异常 : 发生异常
    运输异常 --> 运输中 : 异常已处理
    运输异常 --> 已取消 : 无法继续

    待过磅卸货 --> 已过磅待确认 : 完成卸货过磅
    已过磅待确认 --> 待结算 : 回收方确认入库量
    待结算 --> 已完成 : 完成支付与开票
    待结算 --> 结算异常 : 发生对账问题

    已完成 --> [*]
    已取消 --> [*]
```

## 3. 关键数据表关联说明

- **`waste_transfer_appointment` (预约单表):** 存储预约的核心信息，包括产废方、意向回收方（或系统分配的回收方）、废物详情、预约时间、状态等。`status` 字段反映预约的生命周期（待处理、已接受、已拒绝、已取消、已转订单）。

- **`waste_transfer_order` (订单主表):** 当预约被接受或临时订单创建时生成。包含预约单ID（若有）、产废方、回收方、物流方（若已分配）、废物详情（可能来自预约或重新确认）、订单金额（预估/实际）、订单状态、来源类型（预约/扫街）等。`status` 字段反映订单的复杂流转过程。

- **`waste_recycler_assignment_rule` (回收企业分配规则表):** 存储系统管理员配置的用于自动分配回收企业的规则，如按区域、废物类型等将产废方匹配到特定的回收企业。与 `waste_transfer_appointment` 在分配时关联。

- **`enterprise_info` (企业信息表):** 为预约单和订单中的产废企业、回收企业、物流企业提供详细的企业背景信息。

- **`system_users` / `member_users` (用户表):** 关联操作的发起人、处理人。

*(其他如物流节点、过磅、支付、发票等表将与订单表通过订单ID进行关联，具体设计见相应模块。)*

## 4. 过磅分摊流程

```