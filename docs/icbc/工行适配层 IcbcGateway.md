# 工行适配层 IcbcGateway

> 对应票据 #3。本文说明「唯一一条缝」的边界与用法，是后续所有工行相关票据的入口。

## 为什么是一条缝

工行的约束逼出来的：接口权限按分行逐接口人工配置、沙箱不支持 UI 类型接口、预下单与付方支付都是 UI 页面接口。把这些散在各业务里，测试要触网、换环境要改多处、变更无法隔离。

所以：**所有出站调用走一个端口，所有入站通知走一个入口。** 平台其余部分不再出现工行的网关地址、签名与加解密逻辑。

## 出站：`IcbcGateway`

端口定义在 `yudao-module-icbc-api`，只依赖平台自己的模型，不依赖工行 SDK。

| 端口方法 | 工行接口 | 类型 | 覆盖票据 |
|---|---|---|---|
| `submitPayeeOnboarding` | `/ui/jft/ui/user/edpopenacct/submit/V1` | UI 页面 | #6 |
| `queryPayeeOnboarding` | `/api/jft/api/user/edpopenacct/query/V1` | 数据 | #6 |
| `submitEnterpriseAuthorization` | `/ui/jft/ui/invoice/authorization/V1` | UI 页面 | #5 |
| `submitPreOrder` | `/ui/jft/ui/invoice/pre/order/V1` | UI 页面 | #8 |
| `queryInvoiceInfo` | `/api/jft/api/invoice/queryInvoiceInfo/V1` | 数据 | #8 #9 #10 #14 |
| `submitPayment` | `/ui/jft/ui/invoice/pay/V1` | UI 页面 | #9 |
| `downloadInvoice` | `/api/jft/api/invoice/download/V1` | 数据 | #11 |
| `cancelInvoice` | `/api/jft/api/invoice/reversal/V1` | 数据 | #14 |
| `applyRedInvoice` | `/ui/jft/ui/red/invoice/offset/V1` | UI 页面 | #14 |
| `revokeRedInvoice` | `/api/jft/api/red/invoice/offset/revoke/V1` | 数据 | #14 |
| `checkConnectivity` | `/api/jft/api/user/edpreceive/query/V1` | 数据 | #3 |

UI 页面接口平台不直接请求，端口返回自动提交表单 HTML，由浏览器携带签名 POST 到工行。

### 实现

- 生产：`IcbcSdkGateway`（`gateway.sdk`），`icbc.gateway.mode=sdk`（默认）
- 测试 / 本地：`FakeIcbcGateway`（`gateway.fake`），`icbc.gateway.mode=fake`。它不触网，记录调用序列，返回可覆盖的预设结果，测试可断言「平台发出了什么指令」。

密钥、私钥、AES/SM2、网关基地址只在 `gateway.config.IcbcProperties` 与 `gateway.sdk` 内出现，走环境变量 `ICBC_*` 注入。`IcbcSeamBoundaryTest` 会扫描源码，任何越出 `gateway` 包的工行痕迹都会让测试失败。

## 不重复提交

工行文档明确：返回 `-500041`（代理异常）、`-500042`（代理超时）或未知异常时**切勿重复提交**，须先调对应查询接口确认指令状态。

端口把结果分成三类（`IcbcOutcome`）：

- `SUCCESS`：工行已受理
- `BUSINESS_FAILED`：工行明确拒绝（正数业务错误码）
- `UNKNOWN`：代理异常 / 超时 / 系统异常 / 负数或未知返回码

业务用 `IcbcSubmitCoordinator` 执行「提交 + 查询确认」：提交结果未知时**只调用查询接口，绝不再次提交**，再用查询状态翻译成提交结果。

## 入站：九类异步通知

工行九类通知（`CallbackNotifyTypeEnum`：预下单异常、支付、开票、缴税、发票上传、发票取消、红票申请、红票上传、红票撤销）从**同一个入口**进入：

```
POST /admin-api/icbc/callback/notify
```

处理顺序固定为 **解析 → 落表（PENDING）→ 分发 → 回写状态**：

1. `IcbcNotifyParser` 解析外层 `{notifyData: <base64 JSON>, signData}`，兼容明文 JSON；`notifyId` 缺失时用「类型 + 业务号 + 内容摘要」构造稳定幂等键。
2. `icbc_callback_notify` 表以 `notify_id` 唯一约束去重，先落库。
3. 按类型分发给 `IcbcNotifyHandler`；没有处理器的类型落表为失败（待后续票据补上），仍可重放。
4. 重放接口 `POST /admin-api/icbc/callback/replay` 只重放失败 / 待处理的通知，已成功的不再处理。

通知到达早于平台数据落库的情形天然成立：处理失败不影响记录，数据落库后可重放。

## 真实连通性

- 管理接口：`GET /admin-api/icbc/test/connectivity`
- 联调测试：`IcbcSdkGatewayLiveTest`，设置 `ICBC_APP_ID` 等环境变量后执行；任何来自工行的业务响应（含业务错误码）都判定为可达。
