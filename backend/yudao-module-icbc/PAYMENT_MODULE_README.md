# 工行付方支付模块（issue #9）

## 模块概述

平台代回收企业向出售者付款。付款的起点是一笔**已预开票成功**的收购：经工行「开票付方支付」
UI 接口生成**企业支付页面**，回收企业在页面上授权后，货款从回收企业账户经**公对私结算**
（`payChannel=05`）直付到出售者本人银行卡。平台只发指令、归集回单，**不碰资金、无过桥**
（见 ADR 0006 / 0010）。

## 核心组件

- 控制器：`PaymentController`（`/admin-api/icbc/payment`）
- 服务：`PaymentService` / `PaymentServiceImpl`
- 数据：`PaymentOrderDO` / `PaymentOrderMapper`（表 `icbc_payment_order`）
- 入站：`PaymentNotifyHandler`（`notifyType=02`，走异步通知唯一入口）
- 出站：`IcbcGateway.submitPayment` / `queryInvoiceInfo`（唯一一条缝）

## 状态机

平台把工行 `payStatus` 收敛为 `PaymentStatusEnum`，异常态保留可见：

| 工行 payStatus | 平台状态 | 名 | 可重新发起 |
|---|---|---|---|
| `-1` / `00` | 0 | 待支付 | |
| `01` | 1 | 支付中 | |
| `02` | 2 | 支付成功 | |
| `03` | 3 | 支付失败 | 是 |
| `04` | 4 | 订单关闭 | 是 |
| `05` | 5 | 已冲正 | 是 |
| `06` | 6 | 已退汇 | 是 |
| `07` | 7 | 他行已扣款本行未入账 | |
| `12` | 8 | 已支付待签收 | |
| `25` | 9 | 部分成功 | 是 |

「可重新发起」集合 = 失败 / 关闭 / 冲正 / 退汇 / 部分成功。成功与在途**不重复提交**，
避免二次付款。

## 数据库（`icbc_payment_order`）

在原字段之外，本模块补充：

| 字段 | 说明 |
|---|---|
| acquisition_id | 来源收购单编号（资金流证据挂回的那笔收购） |
| invoice_order_id | 来源开票订单编号（预开票成功的那张票） |
| pay_status | 工行原始支付状态码 |
| actually_received_amount | 实际到账金额（部分成功时小于应付） |
| receipt_no / receipt_time / receipt_file_url | 转账回单归档 |
| retry_count | 异常后重新发起次数 |

增量迁移见 `backend/sql/mysql/icbc-payment.sql`（幂等 ALTER）。

## API

### 发起付款

`POST /admin-api/icbc/payment/apply`，权限 `icbc:payment:create`

```json
{
  "partnerOrderId": "ACQ202601011200001234",
  "amount": 1000.00,
  "verifiedCode": "20201128531215026",
  "ukeyId": "20201128531215026"
}
```

- `partnerOrderId` 或 `acquisitionId` 二选一；`amount` 不填按收购单金额付款。
- **金额必须等于收购单金额**，否则报 `1030004007` 被拦下。
- 只有预开票成功（`preInvoiceStatus=02`）的收购才能付款，否则报 `1030004006`。
- 返回 `payPageHtml`（工行自动提交表单），前端用 `openIcbcForm()` 打开。
- 已成功 / 在途时返回 `duplicate=true`，不再下发工行；异常态重新发起则复用同一支付单并 `retry_count+1`。

### 查询支付状态

`GET /admin-api/icbc/payment/query?partnerOrderId=...`，权限 `icbc:payment:query`

先取本地快照，再经 `queryInvoiceInfo` 主动查询收敛一次。通知与查询共用
`PaymentServiceImpl.applyPaymentStatus`，所以两侧结果一致。

### 转账回单

`GET /admin-api/icbc/payment/receipt?partnerOrderId=...`，权限 `icbc:payment:query`

支付成功 / 部分成功后返回回单号、归档时间、实际到账金额与回单文件地址（若有）。

## 状态收敛与证据归档

- 入站通知：`notifyType=02` 经 `POST /admin-api/icbc/callback/notify` 落表后由
  `PaymentNotifyHandler` 分发，调用 `applyPaymentStatus`。
- 支付成功：归档转账回单（`receipt_no` / `receipt_time`）、回写开票单支付状态，
  并把收购单推进为「已付款」。
- 资金流证据：`InvoiceEvidenceServiceImpl` 从支付单取「转账回单」作为该笔收购的资金流证据。

## 错误码（`1-030-004-xxx`）

- `1030004000` 支付订单不存在
- `1030004002` 支付金额错误
- `1030004003` 支付失败
- `1030004006` 该笔收购尚未预开票成功，不能发起付款
- `1030004007` 付款金额与收购单金额不一致
- `1030004008` 该笔开票申请未挂回收购单
- `1030004009` 支付尚未成功，暂无转账回单
- `1030004010` 工行返回结果未知，勿重复提交
- `1030004011` 按合作方订单号未找到开票申请

## 测试

```bash
cd backend
mvn -pl yudao-module-icbc/yudao-module-icbc-biz test -Dtest='PaymentServiceImplTest,PaymentNotifyHandlerTest'
```

测试从平台服务进入，工行调用走 `FakeIcbcGateway`（`icbc.gateway.mode=fake`），不触网。
