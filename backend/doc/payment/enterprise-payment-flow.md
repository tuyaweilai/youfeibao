# 企业付款模块业务流程图

## 1. 引言
本文档使用 Mermaid 流程图描述企业付款模块中的核心业务流程，旨在清晰地展示不同角色和系统间的交互。

## 2. 核心业务流程

### 2.1 B2C支付流程 (以工行反向开票为例)

```mermaid
sequenceDiagram
    participant Recycler as 回收企业用户
    participant Platform as SaaS平台后端
    participant ICBCSystem as 工行聚富通系统
    participant Payee as 个人收款方(产废用户)

    Recycler->>+Platform: 1. 发起对个人付款 (订单ID, 收款人信息, 金额)
    Platform->>Platform: 2. 校验订单及付款信息
    Platform->>Platform: 3. 准备工行预下单请求 (FP-FN-001, US-007)
    Platform->>ICBCSystem: 4. 调用工行开票信息预下单接口
    ICBCSystem-->>Platform: 5. 返回预下单结果 (含工行确认URL)
    Platform-->>-Recycler: 6. 返回工行URL让收款人确认 (或平台代为通知收款人)

    Note over Payee, ICBCSystem: 收款人跳转至工行页面进行身份验证和开票确认
    Payee->>ICBCSystem: 7. 在工行页面确认开票信息
    ICBCSystem->>Platform: 8. (异步)发送发票状态变更通知 (invoiceNotifyUrl)
    Platform->>Platform: 9. 更新内部订单/发票状态

    Recycler->>+Platform: 10. (可选步骤或由系统触发) 发起支付
    Platform->>ICBCSystem: 11. 调用工行付方支付接口
    ICBCSystem-->>Platform: 12. 返回支付操作结果 (含工行支付页面URL)
    Platform-->>-Recycler: 13. 返回工行支付URL让付款企业操作

    Note over Recycler, ICBCSystem: 回收企业用户跳转至工行企业网银/页面完成支付
    Recycler->>ICBCSystem: 14. 在工行系统完成支付
    ICBCSystem->>Platform: 15. (异步)发送支付结果通知
    Platform->>Platform: 16. 更新支付状态, 记录交易流水 (US-012)
    Platform->>Payee: 17. (可选)通知收款人款项已支付

    Recycler->>+Platform: 18. 查询/下载发票 (US-008)
    Platform->>ICBCSystem: 19. 调用工行发票下载接口
    ICBCSystem-->>Platform: 20. 返回发票文件数据
    Platform->>Platform: 21. 存储发票文件
    Platform-->>-Recycler: 22. 提供发票下载

```

### 2.2 B2C支付流程 (其他第三方支付渠道)

```mermaid
sequenceDiagram
    participant Recycler as 回收企业用户
    participant Platform as SaaS平台后端
    participant ThirdPartyPay as 第三方支付系统 (如微信支付)
    participant Payee as 个人收款方(产废用户)

    Recycler->>+Platform: 1. 发起对个人付款 (订单ID, 收款人账户, 金额, 指定渠道)
    Platform->>Platform: 2. 校验信息, 选择支付渠道 (FP-FN-002, US-012)
    Platform->>ThirdPartyPay: 3. 调用第三方支付接口 (如企业付款到零钱/卡)
    ThirdPartyPay-->>Platform: 4. 返回支付请求结果 (受理成功/失败, 渠道订单号)
    Platform->>Platform: 5. 更新内部支付状态为"处理中"
    Platform-->>-Recycler: 6. (可选)告知支付已受理

    ThirdPartyPay->>Platform: 7. (异步)发送支付结果通知 (成功/失败)
    Platform->>Platform: 8. 验签并更新支付状态, 记录交易流水 (US-012)
    Platform->>Payee: 9. (可选,若支付成功) 通知收款人款项已到账
    Platform->>Recycler: 10. (可选,若支付成功) 通知回收企业支付成功

    Payee->>+Platform: 11. (如需确认) 确认收款 (FP-FN-007, US-018)
    Platform->>Platform: 12. 更新收款确认状态
    Platform-->>-Payee: 13. 返回确认结果
```

### 2.3 B2B对公转账与发票协同流程

```mermaid
sequenceDiagram
    participant RecyclerFinance as 回收企业财务
    participant Platform as SaaS平台后端
    participant ProducerFinance as 产废企业财务

    RecyclerFinance->>+Platform: 1. 记录对公付款 (订单ID, 金额, 收款方账户, 转账凭证) (FP-FN-004, US-030)
    Platform->>Platform: 2. 保存付款记录, 状态为"待确认"
    Platform-->>-RecyclerFinance: 3. 返回操作成功

    ProducerFinance->>+Platform: 4. 上传发票 (发票号, 金额, 开票日期, 发票文件) (FP-FN-006, US-016, US-030)
    Platform->>Platform: 5. 保存发票信息, 状态为"待核验"
    Platform-->>-ProducerFinance: 6. 返回操作成功, 告知发票已上传

    RecyclerFinance->>+Platform: 7. 查询付款记录和关联发票
    Platform-->>-RecyclerFinance: 8. 显示付款和发票列表

    RecyclerFinance->>+Platform: 9. 核验发票, 更新发票状态为"已验真" / "已收票"
    Platform->>Platform: 10. 更新发票状态
    Platform-->>-RecyclerFinance: 11. 返回操作成功

    ProducerFinance->>+Platform: 12. 查询付款状态
    Platform-->>-ProducerFinance: 13. 显示付款状态 (如回收方已确认收到发票并付款)

    Note over Platform: 对账流程 (US-030)
    RecyclerFinance->>Platform: 14. 发起对账 (选择时间范围, 产废企业)
    Platform->>Platform: 15. 生成对账单 (付款记录, 发票记录)
    Platform->>RecyclerFinance: 16. 提供对账单预览/下载
    RecyclerFinance->>Platform: 17. 确认对账结果或标记差异
```

### 2.4 物流代垫款项对账流程

```mermaid
sequenceDiagram
    participant LogisticsDriver as 物流司机
    participant Platform as SaaS平台后端
    participant RecyclerOps as 回收企业运营/财务
    participant LogisticsFinance as 物流公司财务

    LogisticsDriver->>+Platform: 1. 上报完成收运, 记录代付现金 (订单ID, 实付金额) (FP-FN-010, US-031)
    Platform->>Platform: 2. 保存代付记录, 状态为"待回收方确认"
    Platform-->>-LogisticsDriver: 3. 返回操作成功

    RecyclerOps->>+Platform: 4. 查询与某物流公司的代付款项列表
    Platform-->>-RecyclerOps: 5. 返回代付款项列表

    RecyclerOps->>+Platform: 6. (结合入库量) 审核物流代付款项, 更新状态为"已确认待结算"或"有异议"
    Platform->>Platform: 7. 更新代付记录状态
    Platform-->>-RecyclerOps: 8. 返回操作成功

    Note over Platform: 月度/周期性对账
    RecyclerOps->>Platform: 9. 发起与物流公司的对账 (选择周期, 物流公司)
    Platform->>Platform: 10. 生成包含已确认代付款项的对账单初稿
    Platform->>RecyclerOps: 11. 提供对账单给回收企业
    RecyclerOps->>LogisticsFinance: 12. (线下或通过平台) 与物流公司财务核对对账单

    LogisticsFinance->>+Platform: 13. (或回收企业代操作) 上传双方确认的结算单/对账凭证
    Platform->>Platform: 14. 关联凭证, 更新对账状态为"已完成"
    Platform-->>-LogisticsFinance: 15. 返回操作成功
    Platform->>RecyclerOps: 16. (通知) 对账已完成
``` 