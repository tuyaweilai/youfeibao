# 交接：当前进度与如何继续

新会话先读 `AGENTS.md`、`CONTEXT.md`、`docs/adr/`，再读本文件与 GitHub issues。

## 起本地环境

```bash
cp .env.example .env          # 填 MYSQL_PASSWORD、YUDAO_ENCRYPTOR_PASSWORD；值含 & 要加引号
set -a; source .env; set +a
cd backend && docker compose up -d            # MySQL 13308 / Redis 16382
# 首次：按 backend/sql/mysql/README.md 导入建表与种子数据
mvn -pl yudao-server -am -DskipTests install  # 首次
mvn -pl yudao-server spring-boot:run          # 48080，默认 icbc.gateway.mode=fake
# 前端
cd backend/yudao-ui/yudao-ui-admin-vue3 && pnpm install && pnpm dev   # 3100
```

登录：请求头 `tenant-id: 1`，账号 `admin` / `admin123`。

## 已完成（main 关键提交）

- `39d1711` #4 租户、角色、租户隔离
- `97c8fa8` / `453210f` 并入 PC 前端（tuya-saas-pc-vue3）并放行 pnpm 构建脚本
- `b3592f4` 密钥移出配置与 AI 测试
- `b92d48f` 本地库：脱敏后的 yudao 建表 + 种子数据（ADR 0012）
- `4195ea9` / `87e3b58` / `81d5767` #18 管理后台：出售者档案、付方档案、开票申请、付款、发票下载与证据、开票就绪自检
- `5c74388` / `a2804fa` #5：三层资质、编码配置、企业授权；开票申请商品明细接编码配置
- `d36dc4e` #5 收口：资质失效冻结开票 + 平台运营跨租户核实
- 本次提交 #5 剩余口子：品类计税方法（简易/一般）并约束专票、企业授权录入有效期、平台级报废产品编码表、资质到期提醒定时任务
- 本次提交 #6：出售者建档——实人认证 H5、收方入驻两条成败线四种组合、留联系方式兜底、框架收购协议、首次反向开票与代办税费授权、回头客带档、开票门禁

## 管理后台菜单现状

- 反向开票：出售者档案 / 付方档案 / 开票申请 / 付款 / 发票下载与证据
- 租户开票就绪：企业信息 / 企业资质 / 开票就绪自检 / 三层资质 / 编码配置 / 企业授权
- 平台运营：资质核实（跨租户）/ 报废产品编码表

## #5 剩余小口子（已处理）

1. 计税方法：已在 `icbc_goods_config` 增加 `tax_method`（SIMPLE/GENERAL），预下单时简易计税品类禁止开专票（票种 01）。
   - **维度已定：按品类**（非企业级）。理由是 AC 把「计税方法」与「品类/计量单位/税率」并列，后三者本就按品类配；不同报废产品可各自选择简易/一般。若以后要改成企业级，需新增租户级配置表 + 品类继承/覆盖。
2. 企业授权有效期：`PUT /icbc/enterprise-auth/update-result` 可录授权时间与有效期止；过期自动置为已失效。
3. 平台级编码表：新增全局表 `icbc_scrap_code`（已登记进 `yudao.tenant.ignore-tables`），平台运营维护、租户只读，品类表单可一键带出。
4. 到期预警：新增 `icbc_expiry_warning` 与 `QualificationExpiryReminderJob`（`@TenantJob`，每日 08:00，种子见 `icbc-jobs.sql`），开票就绪页展示待处理预警。

> 预警形式已定：**落库 + 开票就绪页主动提醒（方案 A）**，即本产品内的「主动通知」。**不**接站内信推送。理由是推送卡在收件人解析（`TenantApi` 只暴露 `getTenantIdList/validateTenant`，拿不到租户联系人；`AdminUserApi` 也没有按租户列用户）。若将来要做真推送：给 `TenantApi` 加 `getTenant(id)→contactUserId`，job 内用 `NotifyMessageSendApi.sendSingleMessageToAdmin` 并种 `system_notify_template`。

## #6 出售者建档（已完成）

一条链路把一次性手续办完：**实人认证 H5 → 收方入驻（绑定本人银行卡）→ 框架收购协议 → 首次授权**；之后回头客只凭身份证 / 手机号带档。

1. **实人认证**是收方入驻的前置：`IcbcGateway.submitFaceVerification` / `queryFaceVerification`（新增端口，SDK 用 `JftUiUserFaceH5SubmitRequestV1`；结果查询 SDK 无类，适配层自建 `FaceH5QueryRequest` / `FaceH5QueryResponse`）。认证结果也可走通知（`verifyResult`）。
2. **收方入驻两条成败线**：`openacctStatus`（02 成功 / 03 失败）× `result`（pass / reject），四种组合在 `PayeeOnboardingOutcomeEnum` 里各自给出状态名与下一步；两条线未到齐时不推进状态机（保留「秒过但异步」的正确性）。审核拒绝即使缺 `openacctStatus` 也落状态与原因（数据接口回调只带 `result`）。
3. **异步通知**：实人认证与收方入驻两类回调报文没有 `notifyType`，`IcbcNotifyParser` 按特征字段（`verifyResult` / `openacctStatus` / `result`）推断为新增的 `FACE_VERIFY` / `PAYEE_ONBOARDING`，复用 `CallbackNotifyService` 的落表 + 重放。
4. **框架收购协议**（`icbc_framework_agreement`）：每个出售者一份生效协议，重签时旧协议作废留痕；名称 / 数量 / 规格 / 回收期次 / 结算方式必填。
5. **首次授权**（`icbc_seller_authorization`）：反向开票、代办税费两个独立开关 + 渠道 / 办理人 / 附件留痕。
6. **开票门禁**：`SellerOnboardingServiceImpl.assertReadyForInvoice*` 要求实名通过 + 入驻 READY + 生效协议 + 两项授权齐备；`InvoiceOrderServiceImpl.createPreOrder` 按 `outUserId` 调门禁（查不到档案时放行，兼容既有数据）。
7. **回头客**：`findReturningCustomer` 按身份证或手机号带档；前端 `icbc/payeeOnboarding` 页有「回头客带档」入口。
8. **零安装**：实人认证与入驻都是工行 H5 页面（`trxChannel` 默认 `03`），不要求 App 或公众号。

> 待联调确认：实人认证查询的 `authResult` 成功取值（当前只有 `1` 视为通过，通知的 `verifyResult` 是权威路径）；收方入驻回调我们只用页面接口的报文，含 `openacctStatus`。

## 下一步建议

- **A.** #7 收购登记与五流证据骨架——#6 建档已完成，是它的直接前置；现场登记会同时产出合同流 / 货物流 / 信息流；
- **B.** #8 开票申请：预下单与自然人确认——`payChannel` 按工行答复固定上送 `05 公对私结算`，且已接入 #6 的开票门禁；
- **C.** #11 一票一档证据链 / 齐备率 / 台账导出（已完成主体，剩余见票据）。

## 约定与坑

- **权限**：icbc 控制器用 `@icbc.hasPermission`（`RecyclingRoleEnum` 写死角色→权限）。新增权限**必须**在 `RecyclingPermission` + `RecyclingRoleEnum` 登记，否则连超管也 403（未登记即拒绝）。
- **工行 UI 页面**：预下单/付款/入驻返回的是自动提交表单 HTML，用 `src/views/icbc/util.ts` 的 `openIcbcForm()` 打新窗口，不能当 URL 跳。
- **新增 icbc 表**：工行返回字段（如 `payee_no`/`payer_no`）在本地库应为可空；表放 `backend/sql/mysql/`，菜单用 `icbc-menu.sql` 幂等维护。**全局表**（无 `tenant_id`，如 `icbc_scrap_code`）必须登记进 `yudao-server/src/main/resources/application.yaml` 的 `yudao.tenant.ignore-tables`，否则会被拼上 `tenant_id`。单测表结构在 `yudao-module-icbc-biz/src/test/resources/sql/create_tables.sql`。
- **测 icbc**：`mvn -pl yudao-module-icbc/yudao-module-icbc-biz test`；改了 `-api` 先 `mvn -pl ...-api -DskipTests install`。**不要**用 `-am test`（上游模块有既有失败会挡住 reactor）。
- **前端**：`pnpm build:local` 验证编译；`pnpm ts:check` 有 1247 个既有 TS 错误，判断自己的改动看 `src/(views|api)/icbc` 有无新报错即可（跑 ts:check 需加 `NODE_OPTIONS=--max-old-space-size=6144`）。
- **时间字段**：yudao 全局 Jackson 把 `LocalDateTime` 按**毫秒时间戳**序列化/反序列化（`TimestampLocalDateTimeSerializer/Deserializer`）。因此 `@RequestBody` 里的 `LocalDateTime` 字段，前端日期选择器必须用 `value-format="x"`，接口类型声明为 `number`；字段上的 `@DateTimeFormat` 对 JSON body **无效**（只作用于 query/form）。不要用 `YYYY-MM-DD HH:mm:ss`，否则反序列化会得到 0 或报错。
- **工具**：不要在同一条消息里同时发 `edit` 和依赖它的 `bash`（会并发，文件可能未落盘）。
