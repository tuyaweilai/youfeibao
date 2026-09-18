# 企业付款模块 API 接口需求清单

## 1. 引言
本文档定义了企业付款模块所需的核心 API 接口，旨在为开发团队提供清晰的接口设计指导。接口设计遵循项目 `api-test-endpoints.mdc` 中定义的规范，模块路径暂定为 `/payment`。

## 2. 通用约定
- 所有接口均需进行权限校验。
- 返回值统一使用 `CommonResult<T>` 包装。
- 敏感信息在日志中需脱敏处理。
- 接口支持幂等性（对创建/更新操作尤其重要）。

## 3. API 接口清单

### 3.1 支付渠道管理 (管理后台)

| 接口名称                     | 端点路径                               | HTTP方法 | 描述                                       | 关键请求参数                                                                 | 关键响应参数                                     | 用户故事    |
| :--------------------------- | :------------------------------------- | :------- | :----------------------------------------- | :--------------------------------------------------------------------------- | :----------------------------------------------- | :---------- |
| 创建支付渠道配置             | `/payment/channel/create`              | POST     | 新增一个支付渠道（如工行、微信支付）的配置 | `channelCode`, `channelName`, `configJson` (含密钥、商户号等), `status`      | `channelId`, `channelCode`, `status`             | FP-FN-014   |
| 更新支付渠道配置             | `/payment/channel/update`              | PUT      | 修改指定支付渠道的配置                     | `channelId`, `channelName`, `configJson`, `status`                           | `channelId`, `status`                            | FP-FN-014   |
| 查询支付渠道列表             | `/payment/channel/list`                | GET      | 获取所有支付渠道的配置列表                 | `pageNo`, `pageSize`, `channelName`, `status`                                | 列表 (`channelId`, `channelName`, `status`)      | FP-FN-014   |
| 获取支付渠道详情             | `/payment/channel/get`                 | GET      | 获取指定支付渠道的详细配置                 | `channelId`                                                                  | `channelId`, `channelName`, `configJson`, `status` | FP-FN-014   |
| 删除支付渠道配置             | `/payment/channel/delete`              | DELETE   | 删除指定支付渠道的配置                     | `channelId`                                                                  | `boolean success`                                | FP-FN-014   |

### 3.2 B2C 支付接口 (回收企业端调用)

| 接口名称                 | 端点路径                                  | HTTP方法 | 描述                                                         | 关键请求参数                                                                                                                              | 关键响应参数                                                                                                | 用户故事             |
| :----------------------- | :---------------------------------------- | :------- | :----------------------------------------------------------- | :---------------------------------------------------------------------------------------------------------------------------------------- | :---------------------------------------------------------------------------------------------------------- | :------------------- |
| 发起B2C支付 (统一入口)   | `/payment/b2c/initiate`                   | POST     | 回收企业向个人发起支付（如货款），系统根据订单和配置选择渠道 | `orderId`, `payeeType` (USER/MEMBER), `payeeId`, `amount`, `paymentReason`, `channelCode` (可选，不传则系统推荐/默认)                          | `paymentId`, `status` ("PROCESSING", "PENDING_CONFIRMATION"), `redirectUrl` (若需跳转), `channelSpecificData` | US-007, US-012       |
| 查询B2C支付状态          | `/payment/b2c/status`                     | GET      | 查询特定B2C支付订单的状态                                    | `paymentId` 或 `orderId`                                                                                                                    | `paymentId`, `orderId`, `status`, `amount`, `paidTime`, `channelTxnId`, `errorMessage`                        | US-012, US-018       |
| B2C支付回调接收          | `/payment/b2c/notify/{channelCode}`       | POST     | 接收来自具体支付渠道（如工行、微信）的异步支付结果通知         | (根据渠道定义，包含签名验证)                                                                                                              | (根据渠道定义，通常为成功或失败的响应字符串)                                                                    | US-007, US-012       |
| B2C收款确认 (个人用户)   | `/payment/b2c/confirm-receipt`            | POST     | 个人收款方确认收款 (如通过短信链接+验证码)                     | `token` (或 `paymentId` + `verificationCode`)                                                                                               | `boolean success`, `message`                                                                                | US-018               |

#### 3.2.1 工行反向开票特定接口 (系统内部或特定场景调用)

| 接口名称                 | 端点路径                                     | HTTP方法 | 描述                                                              | 关键请求参数                                                                                                                               | 关键响应参数                                                                                                       | 用户故事/参考文档          |
| :----------------------- | :------------------------------------------- | :------- | :---------------------------------------------------------------- | :----------------------------------------------------------------------------------------------------------------------------------------- | :----------------------------------------------------------------------------------------------------------------- | :----------------------- |
| 工行开票信息预下单       | `/payment/icbc/invoice/pre-order`            | POST     | 调用工行开票信息预下单接口 (返回工行URL，前端跳转)                  | `orderId`, `payeeInfo` (个人信息), `payerInfo` (企业信息), `amount`, `itemDetails`, `invoiceNotifyUrl`, `invoiceJumpUrl`                 | `icbcUrl` (工行页面URL), `internalPreOrderId`                                                                        | `工行反向发票开发需求文档.md` |
| 工行开票信息预查询       | `/payment/icbc/invoice/query-info`           | GET      | 调用工行开票信息预查询接口                                          | `orderId` 或 `internalPreOrderId`                                                                                                            | `status`, `invoiceNo`, `invoiceDate`, `pdfUrl` (若有), `paymentStatus`                                               | `工行反向发票开发需求文档.md` |
| 工行付方支付             | `/payment/icbc/invoice/pay`                  | POST     | 调用工行付方支付接口 (返回工行URL，前端跳转)                        | `orderId` 或 `internalPreOrderId`                                                                                                            | `icbcUrl` (工行支付页面URL)                                                                                          | `工行反向发票开发需求文档.md` |
| 工行发票下载             | `/payment/icbc/invoice/download`             | GET      | 调用工行发票下载接口，并将文件流返回或存储后提供下载链接              | `orderId` 或 `invoiceNo`                                                                                                                     | `fileStream` 或 `downloadLink`, `fileName`                                                                           | US-008                   |
| 工行发票状态回调         | `/payment/icbc/invoice/notify`               | POST     | 接收工行发票状态变更的异步通知                                      | (工行定义的回调参数，含验签)                                                                                                               | (响应工行)                                                                                                         | `工行反向发票开发需求文档.md` |

### 3.3 B2B 支付接口 (回收企业端调用)

| 接口名称             | 端点路径                                  | HTTP方法 | 描述                                           | 关键请求参数                                                                                                | 关键响应参数                                                | 用户故事 |
| :------------------- | :---------------------------------------- | :------- | :--------------------------------------------- | :---------------------------------------------------------------------------------------------------------- | :---------------------------------------------------------- | :------- |
| 记录对公付款信息     | `/payment/b2b/record-transfer`            | POST     | 回收企业记录一笔对公转账信息                     | `orderId`, `payerAccountId`, `payeeAccountId`, `amount`, `transferDate`, `bankTxnId` (银行流水号), `remarks`, `voucherFileIds` (凭证文件ID列表) | `paymentId`, `status` ("PENDING_VERIFICATION")            | US-030   |
| 查询对公付款记录     | `/payment/b2b/list-transfers`             | GET      | 查询对公付款记录列表                           | `pageNo`, `pageSize`, `orderId`, `payeeName`, `status`                                                        | 列表 (`paymentId`, `orderId`, `amount`, `status`, `transferDate`) | US-030   |
| 更新对公付款状态     | `/payment/b2b/update-transfer-status`     | PUT      | (由财务或产废方)更新对公付款状态（如已确认收款） | `paymentId`, `status` ("CONFIRMED", "REJECTED"), `remarks`                                                  | `boolean success`                                           | US-030   |

### 3.4 发票管理接口

| 接口名称             | 端点路径                               | HTTP方法 | 描述                                                                | 关键请求参数                                                                                                     | 关键响应参数                                                     | 用户故事             |
| :------------------- | :------------------------------------- | :------- | :------------------------------------------------------------------ | :--------------------------------------------------------------------------------------------------------------- | :--------------------------------------------------------------- | :------------------- |
| 上传发票文件         | `/payment/invoice/upload`              | POST     | 产废企业上传发票文件（如提供给回收企业的增值税发票）                      | `file` (发票文件), `invoiceType`, `issuerName`, `invoiceCode`, `invoiceNumber`, `issueDate`, `amount`, `orderId` (可选) | `invoiceId`, `filePath`, `status` ("PENDING_VERIFICATION")       | US-016, US-030       |
| 查询发票列表         | `/payment/invoice/list`                | GET      | 查询发票列表（自己上传的或与自己相关的）                                | `pageNo`, `pageSize`, `invoiceCode`, `invoiceNumber`, `orderId`, `status`                                          | 列表 (`invoiceId`, `invoiceNumber`, `amount`, `status`, `type`)    | US-008, US-016, US-030 |
| 获取发票详情         | `/payment/invoice/get`                 | GET      | 获取特定发票的详细信息及文件下载链接                                  | `invoiceId`                                                                                                      | `invoiceDetails`, `downloadLink`                                   | US-008, US-016, US-030 |
| 更新发票状态         | `/payment/invoice/update-status`       | PUT      | (由回收企业财务)更新发票状态（如"已收票/验票通过"、"作废"）             | `invoiceId`, `status`, `remarks`                                                                                   | `boolean success`                                                | US-030               |
| 关联发票与订单/支付  | `/payment/invoice/link`                | POST     | 将已上传或已存在的发票关联到订单或支付记录                              | `invoiceId`, `linkType` ("ORDER"/"PAYMENT"), `linkId`                                                            | `boolean success`                                                | US-016, US-030       |

### 3.5 对账与凭证接口

| 接口名称                 | 端点路径                               | HTTP方法 | 描述                                                         | 关键请求参数                                                            | 关键响应参数                                               | 用户故事 |
| :----------------------- | :------------------------------------- | :------- | :----------------------------------------------------------- | :---------------------------------------------------------------------- | :--------------------------------------------------------- | :------- |
| 下载收款/付款对账单      | `/payment/statement/download`          | GET      | 用户下载指定时间范围内的收款或付款对账单                       | `userType` (PRODUCER/RECYCLER), `userId`, `dateFrom`, `dateTo`, `statementType` (RECEIPT/PAYMENT) | 文件流 (Excel/PDF)                                         | US-013   |
| 物流代付款项记录         | `/payment/logistics/record-cash-payment` | POST     | 物流方记录现场代付的现金                                     | `orderId`, `amountPaid`, `paymentTime`, `remarks`, `evidenceFileIds` (可选) | `logisticsPaymentId`, `status`                               | US-031   |
| 查询物流代付款项         | `/payment/logistics/list-cash-payments`| GET      | 回收企业查询物流代付款项列表                                   | `pageNo`, `pageSize`, `orderId`, `logisticsCompanyId`, `status`         | 列表 (`logisticsPaymentId`, `orderId`, `amountPaid`, `status`) | US-031   |
| 物流代付款项对账确认     | `/payment/logistics/confirm-reconciliation` | POST   | 回收企业与物流公司确认代付款项的对账结果                     | `reconciliationBatchId`, `details`, `status`                         | `boolean success`                                          | US-031   |

### 3.6 资金账户管理接口 (简化版，具体视实现复杂度)

| 接口名称               | 端点路径                                | HTTP方法 | 描述                                                   | 关键请求参数                                                                | 关键响应参数                                                     | 用户故事 |
| :--------------------- | :-------------------------------------- | :------- | :----------------------------------------------------- | :-------------------------------------------------------------------------- | :--------------------------------------------------------------- | :------- |
| 查询收运员账户余额     | `/payment/hauler-account/balance`       | GET      | 查询指定收运员的承包账户余额 (若实现虚拟账户)            | `haulerId`                                                                  | `accountId`, `balance`, `currency`                                 | US-014   |
| 收运员账户流水记录     | `/payment/hauler-account/transactions`  | GET      | 查询收运员账户的交易流水                               | `haulerId`, `pageNo`, `pageSize`, `dateFrom`, `dateTo`                    | 列表 (`txnId`, `type`, `amount`, `timestamp`, `description`)       | US-014   |
| 多门店资金归集配置     | `/payment/multi-store/config-collection`| POST     | 配置多门店产废企业的资金统一归集规则 (管理后台)        | `headquarterId`, `storeId`, `collectionAccountId`, `isEnabled`            | `boolean success`                                                | US-015   |

### 3.7 临时订单支付监控 (管理后台)

| 接口名称             | 端点路径                                    | HTTP方法 | 描述                                               | 关键请求参数                                        | 关键响应参数                                              | 用户故事 |
| :------------------- | :------------------------------------------ | :------- | :------------------------------------------------- | :-------------------------------------------------- | :-------------------------------------------------------- | :------- |
| 查询待处理临时订单   | `/payment/temp-order/pending-list`          | GET      | 获取支付状态异常或超时的临时订单列表                 | `pageNo`, `pageSize`, `minAgeHours` (最小未确认时长)  | 列表 (`orderId`, `status`, `amount`, `creationTime`)      | US-019   |
| 处理临时订单支付     | `/payment/temp-order/process`               | POST     | 管理员手动处理临时订单支付（如冻结、关闭、重试）     | `orderId`, `action` (FREEZE/CLOSE/RETRY_PAYMENT)  | `boolean success`                                         | US-019   |

</rewritten_file> 