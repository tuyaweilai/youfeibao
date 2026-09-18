# 企业付款模块数据库设计文档

## 1. 引言
本文档详细描述了企业付款模块所需的新增数据表结构。设计时遵循了 `project-architecture-overview.mdc` 的指导，优先复用现有模块（如用户、订单）的实体，仅针对本模块特有的业务数据创建新表。所有表名建议使用模块前缀 `pay_`。

## 2. 设计原则
- **复用性**：尽可能复用项目中已有的用户表（如 `system_users` 或 `member_users`）、订单表（假设为 `waste_order`）等，通过外键关联。
- **规范性**：字段命名清晰，数据类型选择恰当，关键字段建立索引。
- **扩展性**：设计时考虑未来可能的业务扩展，如支持更多支付渠道、更复杂的对账规则等。
- **一致性**：金额相关的字段统一使用 `DECIMAL` 类型，并明确精度。

## 3. 数据表定义

### 3.1 支付渠道配置表 (`pay_channel_config`)
存储平台支持的支付渠道及其配置信息。

| 字段名          | 数据类型          | 约束        | 描述                                           | 备注                             |
| :-------------- | :---------------- | :---------- | :--------------------------------------------- | :------------------------------- |
| `id`            | `BIGINT`          | PK, AutoInc | 主键ID                                         |                                  |
| `channel_code`  | `VARCHAR(32)`     | Not Null, UK| 渠道编码 (如 ICBC_REVERSE, WECHAT_CORP_PAY)    | 唯一，用于程序识别               |
| `channel_name`  | `VARCHAR(64)`     | Not Null    | 渠道名称 (如 工行反向开票, 微信企业付款)         |                                  |
| `config_json`   | `JSON`            | Not Null    | 渠道配置信息 (JSON格式，含app_id, mch_id, keys)  | 加密存储敏感信息                 |
| `status`        | `TINYINT`         | Not Null    | 状态 (0:禁用, 1:启用)                          |                                  |
| `remark`        | `VARCHAR(255)`    |             | 备注                                           |                                  |
| `creator`       | `VARCHAR(64)`     |             | 创建者                                         | 关联 `system_users.username`     |
| `create_time`   | `DATETIME`        | Not Null    | 创建时间                                       |                                  |
| `updater`       | `VARCHAR(64)`     |             | 更新者                                         | 关联 `system_users.username`     |
| `update_time`   | `DATETIME`        | Not Null    | 更新时间                                       |                                  |
| `deleted`       | `BIT(1)`          | Not Null    | 是否删除 (0:未删除, 1:已删除)                  | 逻辑删除                         |

**索引**：
- `uk_channel_code` ON (`channel_code`, `deleted`)

### 3.2 支付订单表 (`pay_payment_order`)
记录每一笔支付请求，作为核心交易凭证。

| 字段名                | 数据类型         | 约束        | 描述                                                         | 备注                                                               |
| :-------------------- | :--------------- | :---------- | :----------------------------------------------------------- | :----------------------------------------------------------------- |
| `id`                  | `BIGINT`         | PK, AutoInc | 主键ID                                                       |                                                                    |
| `payment_no`          | `VARCHAR(64)`    | Not Null, UK| 平台内部支付订单号 (全局唯一)                                  |                                                                    |
| `biz_order_id`        | `BIGINT`         | Not Null    | 关联的业务订单ID (如危废回收订单ID)                          | FK to `waste_order.id`                                               |
| `biz_order_no`        | `VARCHAR(64)`    |             | 关联的业务订单号 (冗余)                                        |                                                                    |
| `payment_type`        | `VARCHAR(16)`    | Not Null    | 支付类型 (B2C, B2B)                                          |                                                                    |
| `channel_id`          | `BIGINT`         | Not Null    | 使用的支付渠道ID                                               | FK to `pay_channel_config.id`                                        |
| `channel_code`        | `VARCHAR(32)`    | Not Null    | 使用的支付渠道编码 (冗余)                                      |                                                                    |
| `channel_txn_id`      | `VARCHAR(128)`   |             | 支付渠道返回的交易流水号                                       |                                                                    |
| `payer_id`            | `BIGINT`         | Not Null    | 付款方ID (如回收企业用户ID)                                  | FK to `system_users.id` (或其他相关企业用户表)                     |
| `payer_name`          | `VARCHAR(128)`   |             | 付款方名称 (冗余)                                              |                                                                    |
| `payee_id`            | `BIGINT`         | Not Null    | 收款方ID (如产废个人用户ID 或 产废企业用户ID)                  | FK to `member_users.id` or `system_users.id`                         |
| `payee_name`          | `VARCHAR(128)`   |             | 收款方名称 (冗余)                                              |                                                                    |
| `payee_account_no`    | `VARCHAR(128)`   |             | 收款方账号 (银行卡号/微信OpenID等，加密存储)                   |                                                                    |
| `amount`              | `DECIMAL(10,2)`  | Not Null    | 支付金额                                                       |                                                                    |
| `currency`            | `VARCHAR(3)`     | Not Null    | 币种 (默认 CNY)                                                |                                                                    |
| `status`              | `VARCHAR(32)`    | Not Null    | 支付状态 (PROCESSING, SUCCESS, FAILED, PENDING_CONFIRMATION, REFUNDED) | 枚举管理                                                             |
| `payment_reason`      | `VARCHAR(255)`   |             | 支付原因/备注                                                  |                                                                    |
| `paid_time`           | `DATETIME`       |             | 支付成功时间                                                   |                                                                    |
| `notify_url`          | `VARCHAR(255)`   |             | 支付结果异步通知地址                                           |                                                                    |
| `return_url`          | `VARCHAR(255)`   |             | 支付完成后同步跳转地址                                         |                                                                    |
| `channel_extra_data`  | `JSON`           |             | 渠道特定附加参数 (如工行预下单ID)                              |                                                                    |
| `error_code`          | `VARCHAR(64)`    |             | 渠道错误码                                                     |                                                                    |
| `error_message`       | `VARCHAR(255)`   |             | 渠道错误信息                                                   |                                                                    |
| `creator`             | `VARCHAR(64)`    |             | 创建者                                                       |                                                                    |
| `create_time`         | `DATETIME`       | Not Null    | 创建时间                                                     |                                                                    |
| `updater`             | `VARCHAR(64)`    |             | 更新者                                                       |                                                                    |
| `update_time`         | `DATETIME`       | Not Null    | 更新时间                                                     |                                                                    |
| `deleted`             | `BIT(1)`         | Not Null    | 是否删除 (0:未删除, 1:已删除)                                |                                                                    |

**索引**：
- `uk_payment_no` ON (`payment_no`, `deleted`)
- `idx_biz_order_id` ON (`biz_order_id`)
- `idx_channel_txn_id` ON (`channel_txn_id`)
- `idx_status_create_time` ON (`status`, `create_time`)

### 3.3 发票信息表 (`pay_invoice`)
存储发票信息，包括上传的发票和通过工行等渠道生成的电子发票。

| 字段名             | 数据类型         | 约束        | 描述                                                        | 备注                                                         |
| :----------------- | :--------------- | :---------- | :---------------------------------------------------------- | :----------------------------------------------------------- |
| `id`               | `BIGINT`         | PK, AutoInc | 主键ID                                                      |                                                              |
| `invoice_type`     | `VARCHAR(32)`    | Not Null    | 发票类型 (ICBC_REVERSE_E_INVOICE, VAT_SPECIAL, GENERAL_E_INVOICE) | 枚举管理                                                       |
| `invoice_code`     | `VARCHAR(64)`    |             | 发票代码                                                      |                                                              |
| `invoice_number`   | `VARCHAR(64)`    | Not Null    | 发票号码                                                      | `invoice_code`+`invoice_number` 可考虑联合唯一（视税务规则） |
| `issue_date`       | `DATE`           | Not Null    | 开票日期                                                      |                                                              |
| `issuer_name`      | `VARCHAR(128)`   | Not Null    | 开票方名称                                                    |                                                              |
| `issuer_tax_no`    | `VARCHAR(32)`    |             | 开票方税号                                                    |                                                              |
| `recipient_name`   | `VARCHAR(128)`   | Not Null    | 收票方名称                                                    |                                                              |
| `recipient_tax_no` | `VARCHAR(32)`    |             | 收票方税号                                                    |                                                              |
| `amount`           | `DECIMAL(10,2)`  | Not Null    | 发票金额 (不含税)                                             |                                                              |
| `tax_amount`       | `DECIMAL(10,2)`  | Not Null    | 税额                                                        |                                                              |
| `total_amount`     | `DECIMAL(10,2)`  | Not Null    | 价税合计                                                    |                                                              |
| `status`           | `VARCHAR(32)`    | Not Null    | 发票状态 (PENDING_VERIFICATION, VERIFIED, INVALID, USED)    | 枚举管理                                                       |
| `file_path`        | `VARCHAR(255)`   |             | 发票文件存储路径 (如PDF, OFD)                               |                                                              |
| `file_url`         | `VARCHAR(512)`   |             | 发票文件访问URL (若为外部存储)                              |                                                              |
| `remarks`          | `VARCHAR(255)`   |             | 备注                                                        |                                                              |
| `linked_biz_type`  | `VARCHAR(16)`    |             | 关联业务类型 (ORDER, PAYMENT)                               |                                                              |
| `linked_biz_id`    | `BIGINT`         |             | 关联业务ID                                                  |                                                              |
| `source_channel`   | `VARCHAR(32)`    |             | 发票来源渠道 (如 ICBC, MANUAL_UPLOAD)                       |                                                              |
| `creator`          | `VARCHAR(64)`    |             | 创建者/上传者                                               |                                                              |
| `create_time`      | `DATETIME`       | Not Null    | 创建时间                                                    |                                                              |
| `updater`          | `VARCHAR(64)`    |             | 更新者                                                      |                                                              |
| `update_time`      | `DATETIME`       | Not Null    | 更新时间                                                    |                                                              |
| `deleted`          | `BIT(1)`         | Not Null    | 是否删除 (0:未删除, 1:已删除)                               |                                                              |

**索引**：
- `uk_invoice_code_number` ON (`invoice_code`, `invoice_number`, `deleted`) (如果业务上要求唯一)
- `idx_linked_biz` ON (`linked_biz_type`, `linked_biz_id`)
- `idx_status` ON (`status`)

### 3.4 对账批次表 (`pay_reconciliation_batch`)
记录对账任务的批次信息。

| 字段名             | 数据类型         | 约束        | 描述                                          | 备注                                        |
| :----------------- | :--------------- | :---------- | :-------------------------------------------- | :------------------------------------------ |
| `id`               | `BIGINT`         | PK, AutoInc | 主键ID                                        |                                             |
| `batch_no`         | `VARCHAR(64)`    | Not Null, UK| 对账批次号 (全局唯一)                         |                                             |
| `reconciliation_type`|`VARCHAR(32)`    | Not Null    | 对账类型 (PAYMENT_VS_INVOICE, LOGISTICS_CASH) | 枚举                                        |
| `partner_id`       | `BIGINT`         |             | 对账伙伴ID (如产废企业ID, 物流公司ID)         |                                             |
| `partner_name`     | `VARCHAR(128)`   |             | 对账伙伴名称 (冗余)                           |                                             |
| `start_date`       | `DATE`           | Not Null    | 对账周期开始日期                              |                                             |
| `end_date`         | `DATE`           | Not Null    | 对账周期结束日期                              |                                             |
| `status`           | `VARCHAR(32)`    | Not Null    | 对账状态 (PROCESSING, COMPLETED, FAILED)    | 枚举                                        |
| `summary_json`     | `JSON`           |             | 对账结果概要 (如总笔数, 成功数, 失败数, 差异) |                                             |
| `file_path`        | `VARCHAR(255)`   |             | 对账结果文件存储路径 (如对账单Excel)        |                                             |
| `creator`          | `VARCHAR(64)`    |             | 创建者                                        |                                             |
| `create_time`      | `DATETIME`       | Not Null    | 创建时间                                      |                                             |
| `updater`          | `VARCHAR(64)`    |             | 更新者                                        |                                             |
| `update_time`      | `DATETIME`       | Not Null    | 更新时间                                      |                                             |
| `deleted`          | `BIT(1)`         | Not Null    | 是否删除 (0:未删除, 1:已删除)                 |                                             |

**索引**：
- `uk_batch_no` ON (`batch_no`, `deleted`)
- `idx_partner_status` ON (`partner_id`, `status`)

### 3.5 对账明细表 (`pay_reconciliation_detail`)
记录对账批次中的具体明细项。

| 字段名             | 数据类型         | 约束        | 描述                                                         | 备注                                  |
| :----------------- | :--------------- | :---------- | :----------------------------------------------------------- | :------------------------------------ |
| `id`               | `BIGINT`         | PK, AutoInc | 主键ID                                                       |                                       |
| `batch_id`         | `BIGINT`         | Not Null    | 关联的对账批次ID                                             | FK to `pay_reconciliation_batch.id`  |
| `biz_doc_type`     | `VARCHAR(32)`    | Not Null    | 业务单据类型 (PAYMENT_ORDER, INVOICE, LOGISTICS_PAYMENT)     |                                       |
| `biz_doc_id`       | `BIGINT`         | Not Null    | 业务单据ID (如支付订单ID, 发票ID)                            |                                       |
| `biz_doc_no`       | `VARCHAR(64)`    |             | 业务单据号 (冗余)                                              |                                       |
| `amount`           | `DECIMAL(10,2)`  |             | 金额                                                         |                                       |
| `status_before`    | `VARCHAR(32)`    |             | 对账前状态                                                   |                                       |
| `status_after`     | `VARCHAR(32)`    |             | 对账后状态/对账结果 (MATCHED, MISMATCHED_AMOUNT, MISSING_REMOTE) |                                       |
| `remarks`          | `VARCHAR(255)`   |             | 差异说明或备注                                               |                                       |
| `creator`          | `VARCHAR(64)`    |             | 创建者                                                       |                                       |
| `create_time`      | `DATETIME`       | Not Null    | 创建时间                                                     |                                       |
| `deleted`          | `BIT(1)`         | Not Null    | 是否删除 (0:未删除, 1:已删除)                                |                                       |

**索引**：
- `idx_batch_id` ON (`batch_id`)
- `idx_biz_doc` ON (`biz_doc_type`, `biz_doc_id`)

### 3.6 物流代付款项记录表 (`pay_logistics_cash_payment`)
US-031: 物流方代付现金记录与对账支持。

| 字段名             | 数据类型         | 约束        | 描述                                             | 备注                                   |
| :----------------- | :--------------- | :---------- | :----------------------------------------------- | :------------------------------------- |
| `id`               | `BIGINT`         | PK, AutoInc | 主键ID                                           |                                        |
| `biz_order_id`     | `BIGINT`         | Not Null    | 关联的业务订单ID                                 | FK to `waste_order.id`                   |
| `logistics_company_id`| `BIGINT`       | Not Null    | 物流公司ID                                       | 关联物流公司表 (假设存在)              |
| `driver_id`        | `BIGINT`         |             | 司机ID                                           | 关联司机用户表 (假设存在)              |
| `amount_paid`      | `DECIMAL(10,2)`  | Not Null    | 代付金额                                         |                                        |
| `payment_time`     | `DATETIME`       | Not Null    | 付款时间                                         |                                        |
| `status`           | `VARCHAR(32)`    | Not Null    | 状态 (PENDING_CONFIRMATION, CONFIRMED, RECONCILED, DISPUTED) | 枚举                                   |
| `remarks`          | `VARCHAR(255)`   |             | 备注                                             |                                        |
| `evidence_file_ids`| `JSON`           |             | 凭证文件ID列表 (JSON数组，关联文件管理模块)      |                                        |
| `reconciliation_batch_id`| `BIGINT`   |             | 关联的对账批次ID (对账完成后填写)                | FK to `pay_reconciliation_batch.id` (Nullable) |
| `creator`          | `VARCHAR(64)`    |             | 创建者 (通常是司机或物流调度员)                  |                                        |
| `create_time`      | `DATETIME`       | Not Null    | 创建时间                                         |                                        |
| `updater`          | `VARCHAR(64)`    |             | 更新者 (通常是回收企业财务)                      |                                        |
| `update_time`      | `DATETIME`       | Not Null    | 更新时间                                         |                                        |
| `deleted`          | `BIT(1)`         | Not Null    | 是否删除 (0:未删除, 1:已删除)                    |                                        |

**索引**：
- `idx_biz_order_id` ON (`biz_order_id`)
- `idx_logistics_company_status` ON (`logistics_company_id`, `status`)

### 3.7 危废订单支付扩展表 (`pay_waste_order_ext`)
管理危废订单与支付的关联关系，以及工行特定的业务数据。

| 字段名                    | 数据类型         | 约束        | 描述                                                         | 备注                                   |
| :------------------------ | :--------------- | :---------- | :----------------------------------------------------------- | :------------------------------------- |
| `id`                      | `BIGINT`         | PK, AutoInc | 主键ID                                                       |                                        |
| `waste_order_id`          | `BIGINT`         | Not Null    | 危废订单ID                                                   | FK to `waste_transfer_order.id`        |
| `payment_order_id`        | `BIGINT`         | Not Null    | 支付订单ID                                                   | FK to `pay_payment_order.id`           |
| `icbc_receiver_status`    | `TINYINT`        | Not Null    | 工行收方状态 (0:未入驻, 1:审核中, 2:已入驻, 3:审核拒绝)      | 默认0                                  |
| `icbc_receiver_account`   | `VARCHAR(50)`    |             | 工行收方账号                                                 |                                        |
| `icbc_receiver_audit_time`| `DATETIME`       |             | 工行审核时间                                                 |                                        |
| `icbc_pre_order_status`   | `TINYINT`        | Not Null    | 预下单状态 (0:未创建, 1:待确认, 2:已确认, 3:已取消)          | 默认0                                  |
| `icbc_invoice_code`       | `VARCHAR(20)`    |             | 发票代码                                                     |                                        |
| `icbc_invoice_number`     | `VARCHAR(20)`    |             | 发票号码                                                     |                                        |
| `icbc_invoice_date`       | `DATE`           |             | 开票日期                                                     |                                        |
| `icbc_invoice_url`        | `VARCHAR(500)`   |             | 发票下载地址                                                 |                                        |
| `payment_trigger_time`    | `DATETIME`       |             | 支付触发时间                                                 |                                        |
| `payment_complete_time`   | `DATETIME`       |             | 支付完成时间                                                 |                                        |
| `payment_channel_txn_id`  | `VARCHAR(128)`   |             | 工行交易流水号                                               |                                        |
| `waste_code`              | `VARCHAR(50)`    | Not Null    | 危废代码                                                     |                                        |
| `waste_category`          | `VARCHAR(100)`   | Not Null    | 危废类别                                                     |                                        |
| `is_agricultural_purchase`| `BIT(1)`         | Not Null    | 是否农产品收购                                               | 默认0                                  |
| `is_scrap_purchase`       | `BIT(1)`         | Not Null    | 是否报废产品收购                                             | 默认0                                  |
| `remark`                  | `VARCHAR(500)`   |             | 备注                                                         |                                        |
| `tenant_id`               | `BIGINT`         | Not Null    | 租户ID                                                       | 默认0                                  |
| `creator`                 | `VARCHAR(64)`    |             | 创建者                                                       |                                        |
| `create_time`             | `DATETIME`       | Not Null    | 创建时间                                                     |                                        |
| `updater`                 | `VARCHAR(64)`    |             | 更新者                                                       |                                        |
| `update_time`             | `DATETIME`       | Not Null    | 更新时间                                                     |                                        |
| `deleted`                 | `BIT(1)`         | Not Null    | 是否删除                                                     | 默认0                                  |

**索引**：
- `uk_waste_order_id` ON (`waste_order_id`, `deleted`)
- `idx_payment_order_id` ON (`payment_order_id`)
- `idx_icbc_receiver_status` ON (`icbc_receiver_status`)
- `idx_tenant_id_deleted` ON (`tenant_id`, `deleted`)

### 3.8 工行收方信息表 (`pay_icbc_receiver`)
管理在工行系统中的收方（产废方）入驻信息。

| 字段名                | 数据类型         | 约束        | 描述                                                         | 备注                                   |
| :-------------------- | :--------------- | :---------- | :----------------------------------------------------------- | :------------------------------------- |
| `id`                  | `BIGINT`         | PK, AutoInc | 主键ID                                                       |                                        |
| `out_user_id`         | `VARCHAR(20)`    | Not Null    | 外部用户编号                                                 | 唯一标识                               |
| `receiver_type`       | `TINYINT`        | Not Null    | 收方类型 (1:企业, 3:自然人)                                 |                                        |
| `receiver_name`       | `VARCHAR(60)`    | Not Null    | 收方户名                                                     |                                        |
| `receiver_account`    | `VARCHAR(50)`    | Not Null    | 收方账号                                                     |                                        |
| `id_type`             | `VARCHAR(3)`     | Not Null    | 证件类型 (0:身份证)                                         |                                        |
| `id_no`               | `VARCHAR(18)`    | Not Null    | 证件号码                                                     | 加密存储                               |
| `mobile`              | `VARCHAR(20)`    | Not Null    | 手机号                                                       |                                        |
| `audit_status`        | `TINYINT`        | Not Null    | 审核状态 (0:待提交, 1:审核中, 2:审核通过, 3:审核拒绝)       | 默认0                                  |
| `submit_time`         | `DATETIME`       |             | 提交时间                                                     |                                        |
| `audit_time`          | `DATETIME`       |             | 审核时间                                                     |                                        |
| `audit_remark`        | `VARCHAR(255)`   |             | 审核备注                                                     |                                        |
| `icbc_receiver_status`| `VARCHAR(1)`     |             | 工行收方状态 (0:不可用, 1:可用)                             |                                        |
| `icbc_medium_id`      | `VARCHAR(50)`    |             | 工行电子账户账号                                             |                                        |
| `icbc_openacct_status`| `VARCHAR(2)`     |             | 开户状态 (00:初始, 01:开户中, 02:开户成功, 03:开户失败)     |                                        |
| `occupation`          | `VARCHAR(3)`     |             | 职业                                                         |                                        |
| `address`             | `VARCHAR(120)`   |             | 常用住址                                                     |                                        |
| `company_name`        | `VARCHAR(60)`    |             | 关联企业名称                                                 |                                        |
| `tenant_id`           | `BIGINT`         | Not Null    | 租户ID                                                       | 默认0                                  |
| `creator`             | `VARCHAR(64)`    |             | 创建者                                                       |                                        |
| `create_time`         | `DATETIME`       | Not Null    | 创建时间                                                     |                                        |
| `updater`             | `VARCHAR(64)`    |             | 更新者                                                       |                                        |
| `update_time`         | `DATETIME`       | Not Null    | 更新时间                                                     |                                        |
| `deleted`             | `BIT(1)`         | Not Null    | 是否删除                                                     | 默认0                                  |

**索引**：
- `uk_out_user_id` ON (`out_user_id`, `deleted`)
- `uk_receiver_account` ON (`receiver_account`, `deleted`)
- `idx_audit_status` ON (`audit_status`)
- `idx_id_no` ON (`id_no`)
- `idx_tenant_id_deleted` ON (`tenant_id`, `deleted`)

## 4. 枚举值建议

- **支付状态 (`pay_payment_order.status`)**
  - `PROCESSING`: 处理中
  - `SUCCESS`: 支付成功
  - `FAILED`: 支付失败
  - `PENDING_CONFIRMATION`: 等待收款人确认 (如某些B2C场景)
  - `PENDING_PAYER_ACTION`: 等待付款人操作 (如跳转银行页面支付)
  - `CLOSED`: 支付关闭 (超时或取消)
  - `REFUNDING`: 退款中
  - `REFUNDED`: 已退款
- **发票状态 (`pay_invoice.status`)**
  - `PENDING_VERIFICATION`: 待核验 (如上传后)
  - `VERIFIED_VALID`: 核验通过/有效
  - `VERIFIED_INVALID`: 核验无效/作废
  - `LINKED_TO_PAYMENT`: 已关联支付/已使用
- **对账状态 (`pay_reconciliation_batch.status`, `pay_reconciliation_detail.status_after`)**
  - `PROCESSING`: 处理中
  - `COMPLETED_MATCH`: 对账完成-全部匹配
  - `COMPLETED_PARTIAL_MATCH`: 对账完成-部分匹配/有差异
  - `FAILED`: 对账失败
  - `MATCHED`: (明细) 匹配
  - `MISMATCHED_AMOUNT`: (明细) 金额不符
  - `MISSING_PLATFORM`: (明细) 平台缺失
  - `MISSING_PARTNER`: (明细) 对方缺失

## 5. 关系图 (ERD - 文本示意)
```
[pay_channel_config] 1--* [pay_payment_order]
[pay_payment_order] *--1 [waste_order] (Existing)
[pay_payment_order] *--1 [system_users] (Payer - Existing)
[pay_payment_order] *--1 [member_users / system_users] (Payee - Existing)

[pay_invoice] *--0..1 [pay_payment_order] (Invoice linked to payment)
[pay_invoice] *--0..1 [waste_order] (Invoice linked to order)

[pay_reconciliation_batch] 1--* [pay_reconciliation_detail]
[pay_reconciliation_detail] *--1 [pay_payment_order / pay_invoice / pay_logistics_cash_payment] (Polymorphic based on biz_doc_type)

[pay_logistics_cash_payment] *--1 [waste_order] (Existing)
[pay_logistics_cash_payment] *--0..1 [pay_reconciliation_batch]
```
**注意**：实际ERD图建议使用专业工具绘制。 

## 6. 危废支付相关表设计

### 6.1 危废订单支付扩展表 (`pay_waste_order_ext`)
管理危废订单与支付的关联关系，以及工行特定的业务数据。

```sql
CREATE TABLE `pay_waste_order_ext` (
  `id` bigint(20) NOT NULL COMMENT '主键ID',
  `waste_order_id` bigint(20) NOT NULL COMMENT '危废订单ID',
  `payment_order_id` bigint(20) NOT NULL COMMENT '支付订单ID',
  
  -- 工行收方信息
  `icbc_receiver_status` tinyint(4) DEFAULT '0' COMMENT '工行收方状态(0:未入驻,1:审核中,2:已入驻,3:审核拒绝)',
  `icbc_receiver_account` varchar(50) DEFAULT '' COMMENT '工行收方账号',
  `icbc_receiver_audit_time` datetime DEFAULT NULL COMMENT '工行审核时间',
  
  -- 工行开票信息
  `icbc_pre_order_status` tinyint(4) DEFAULT '0' COMMENT '预下单状态(0:未创建,1:待确认,2:已确认,3:已取消)',
  `icbc_invoice_code` varchar(20) DEFAULT '' COMMENT '发票代码',
  `icbc_invoice_number` varchar(20) DEFAULT '' COMMENT '发票号码',
  `icbc_invoice_date` date DEFAULT NULL COMMENT '开票日期',
  `icbc_invoice_url` varchar(500) DEFAULT '' COMMENT '发票下载地址',
  
  -- 支付信息
  `payment_trigger_time` datetime DEFAULT NULL COMMENT '支付触发时间',
  `payment_complete_time` datetime DEFAULT NULL COMMENT '支付完成时间',
  `payment_channel_txn_id` varchar(128) DEFAULT '' COMMENT '工行交易流水号',
  
  -- 业务关联
  `waste_code` varchar(50) NOT NULL COMMENT '危废代码',
  `waste_category` varchar(100) NOT NULL COMMENT '危废类别',
  `is_agricultural_purchase` bit(1) DEFAULT b'0' COMMENT '是否农产品收购',
  `is_scrap_purchase` bit(1) DEFAULT b'0' COMMENT '是否报废产品收购',
  
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `tenant_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '租户ID',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_waste_order_id` (`waste_order_id`, `deleted`),
  KEY `idx_payment_order_id` (`payment_order_id`),
  KEY `idx_icbc_receiver_status` (`icbc_receiver_status`),
  KEY `idx_tenant_id_deleted` (`tenant_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='危废订单支付扩展表';
```

### 6.2 工行收方信息表 (`pay_icbc_receiver`)
管理在工行系统中的收方（产废方）入驻信息。

```sql
CREATE TABLE `pay_icbc_receiver` (
  `id` bigint(20) NOT NULL COMMENT '主键ID',
  `out_user_id` varchar(20) NOT NULL COMMENT '外部用户编号',
  `receiver_type` tinyint(4) NOT NULL COMMENT '收方类型(1:企业,3:自然人)',
  `receiver_name` varchar(60) NOT NULL COMMENT '收方户名',
  `receiver_account` varchar(50) NOT NULL COMMENT '收方账号',
  `id_type` varchar(3) NOT NULL COMMENT '证件类型(0:身份证)',
  `id_no` varchar(18) NOT NULL COMMENT '证件号码',
  `mobile` varchar(20) NOT NULL COMMENT '手机号',
  
  -- 审核信息
  `audit_status` tinyint(4) NOT NULL DEFAULT '0' COMMENT '审核状态(0:待提交,1:审核中,2:审核通过,3:审核拒绝)',
  `submit_time` datetime DEFAULT NULL COMMENT '提交时间',
  `audit_time` datetime DEFAULT NULL COMMENT '审核时间',
  `audit_remark` varchar(255) DEFAULT '' COMMENT '审核备注',
  
  -- 工行返回信息
  `icbc_receiver_status` varchar(1) DEFAULT '' COMMENT '工行收方状态(0:不可用,1:可用)',
  `icbc_medium_id` varchar(50) DEFAULT '' COMMENT '工行电子账户账号',
  `icbc_openacct_status` varchar(2) DEFAULT '' COMMENT '开户状态(00:初始,01:开户中,02:开户成功,03:开户失败)',
  
  -- 扩展信息
  `occupation` varchar(3) DEFAULT '' COMMENT '职业',
  `address` varchar(120) DEFAULT '' COMMENT '常用住址',
  `company_name` varchar(60) DEFAULT '' COMMENT '关联企业名称',
  
  `tenant_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '租户ID',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_out_user_id` (`out_user_id`, `deleted`),
  UNIQUE KEY `uk_receiver_account` (`receiver_account`, `deleted`),
  KEY `idx_audit_status` (`audit_status`),
  KEY `idx_id_no` (`id_no`),
  KEY `idx_tenant_id_deleted` (`tenant_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工行收方信息表';
```

### 6.3 工行预下单记录表 (`pay_icbc_pre_order`)
记录工行反向开票的预下单信息。

```sql
CREATE TABLE `pay_icbc_pre_order` (
  `id` bigint(20) NOT NULL COMMENT '主键ID',
  `payment_order_id` bigint(20) NOT NULL COMMENT '支付订单ID',
  `out_order_id` varchar(64) NOT NULL COMMENT '外部订单号',
  `icbc_order_id` varchar(64) DEFAULT '' COMMENT '工行订单号',
  
  -- 订单信息
  `order_amount` decimal(10,2) NOT NULL COMMENT '订单金额',
  `project_name` varchar(200) NOT NULL COMMENT '商品名称',
  `goods_num` decimal(10,2) NOT NULL COMMENT '商品数量',
  `goods_unit` varchar(20) NOT NULL COMMENT '商品单位',
  `specific_elements` varchar(2) NOT NULL COMMENT '特定要素(24:报废产品收购)',
  
  -- 付方信息
  `out_user_id` varchar(20) NOT NULL COMMENT '付方编号',
  `payer_name` varchar(60) NOT NULL COMMENT '付方名称',
  
  -- 收方信息
  `out_vendor_id` varchar(20) NOT NULL COMMENT '收方编号',
  `receiver_name` varchar(60) NOT NULL COMMENT '收方名称',
  `receiver_account` varchar(50) NOT NULL COMMENT '收方账号',
  
  -- 预下单状态
  `pre_order_status` tinyint(4) NOT NULL DEFAULT '0' COMMENT '状态(0:待提交,1:待确认,2:已确认,3:已取消,4:已失效)',
  `submit_time` datetime DEFAULT NULL COMMENT '提交时间',
  `confirm_time` datetime DEFAULT NULL COMMENT '确认时间',
  `expire_time` datetime DEFAULT NULL COMMENT '失效时间',
  
  -- 开票信息
  `invoice_status` tinyint(4) DEFAULT '0' COMMENT '开票状态(0:未开票,1:开票中,2:已开票,3:开票失败)',
  `invoice_time` datetime DEFAULT NULL COMMENT '开票时间',
  `invoice_fail_reason` varchar(255) DEFAULT '' COMMENT '开票失败原因',
  
  -- 回调信息
  `callback_url` varchar(255) DEFAULT '' COMMENT '回调地址',
  `callback_status` tinyint(4) DEFAULT '0' COMMENT '回调状态(0:未回调,1:回调成功,2:回调失败)',
  `callback_time` datetime DEFAULT NULL COMMENT '回调时间',
  
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `tenant_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '租户ID',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_out_order_id` (`out_order_id`, `deleted`),
  KEY `idx_payment_order_id` (`payment_order_id`),
  KEY `idx_pre_order_status` (`pre_order_status`),
  KEY `idx_invoice_status` (`invoice_status`),
  KEY `idx_tenant_id_deleted` (`tenant_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工行预下单记录表';
```

## 7. 更新后的关系图

```
危废支付相关表关系：
[waste_transfer_order] 1--1 [pay_waste_order_ext]
[pay_waste_order_ext] *--1 [pay_payment_order]
[pay_payment_order] 1--0..1 [pay_icbc_pre_order]
[pay_icbc_receiver] 1--* [pay_payment_order] (通过payee_id关联)

完整关系图：
[pay_channel_config] 1--* [pay_payment_order]
[pay_payment_order] *--1 [waste_transfer_order] (通过biz_order_id)
[pay_payment_order] *--1 [system_users] (Payer)
[pay_payment_order] *--1 [member_users / system_users] (Payee)
[pay_payment_order] 1--0..1 [pay_waste_order_ext]
[pay_payment_order] 1--0..1 [pay_icbc_pre_order]

[pay_invoice] *--0..1 [pay_payment_order]
[pay_invoice] *--0..1 [waste_transfer_order]

[pay_reconciliation_batch] 1--* [pay_reconciliation_detail]
[pay_reconciliation_detail] *--1 [pay_payment_order / pay_invoice / pay_logistics_cash_payment]

[pay_logistics_cash_payment] *--1 [waste_transfer_order]
[pay_logistics_cash_payment] *--0..1 [pay_reconciliation_batch]

[pay_icbc_receiver] 独立管理收方信息
```

## 8. 数据库设计要点

### 8.1 危废支付特殊性
- **收方管理**：产废方作为收款方需要在工行入驻，需要独立管理其审核状态
- **反向开票**：支付前需要先开票，开票需要产废方确认，流程比普通支付复杂
- **特定要素**：危废属于报废产品收购，需要标记特定要素代码
- **发票关联**：每笔支付都会生成发票，需要管理发票与支付的关联关系

### 8.2 性能优化建议
- **索引设计**：针对高频查询场景设计索引，如订单ID、支付状态、审核状态等
- **分表策略**：预下单记录表和支付订单表可能数据量较大，建议按月分表
- **缓存策略**：收方信息查询频繁，建议使用Redis缓存
- **异步处理**：工行回调处理建议使用消息队列异步处理

### 8.3 安全性考虑
- **敏感信息加密**：身份证号、银行账号等敏感信息需要加密存储
- **数据脱敏**：查询接口返回的敏感信息需要脱敏处理
- **操作审计**：所有支付相关操作需要记录操作日志
- **权限控制**：严格控制支付相关接口的访问权限 