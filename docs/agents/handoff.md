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
- 本次提交 #7：收购登记——从既有出售者档案发起、按品类带出单位/税率/编码、毛重皮重净重、磅单与车牌识别回填及人工修正、交易地点时间、缺要件拦截、离线补传去重、单笔收购确认书导出、状态可见；证据链的合同流/货物流/信息流改从收购单自动取

## 管理后台菜单现状

- 反向开票：出售者档案 / 出售者建档 / 收购登记 / 付方档案 / 开票申请（按收购单）/ 付款 / 发票下载与证据 / 一票一档
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

## #7 收购登记（已完成）

收货员在现场完整登记一笔收购，登记完这一笔的合同流、货物流、信息流骨架就成形了。

1. **收购单** `icbc_acquisition`：从既有出售者档案发起，选品类后自动带出计量单位 / 税率 / 计税方法 / 税收分类合并编码（`IcbcGoodsConfigDO`）。
2. **必须要件**：出售者、品类、数量、金额、磅单缺一即拒，报错逐个列出缺了什么（`ACQUISITION_REQUIRED_ELEMENT_MISSING`）。金额或净重可留空，分别按「数量 × 单价」「毛重 − 皮重」推算。
3. **识别**：`AcquisitionRecognitionPort` 新端口（见 ADR 0013），默认 `StubAcquisitionRecognition` 返回空、不阻断；识别只在空缺处回填，人工值优先。磅单车牌与车辆照片车牌在业务层比对，结论为 `plateMatched`（`null` 表示无法比对）。识别结果可经 `POST /icbc/acquisition/correct` 人工修正并重新比对。
4. **离线补传**：`POST /icbc/acquisition/sync-offline`，按 `client_request_id` 幂等（`(tenant_id, client_request_id)` 唯一），逐条返回成败，重复补传返回既有单据、不产生重复。
5. **确认书**：`GET /icbc/acquisition/confirmation/export` 导出单笔收购确认书（Excel，可打印），含名称 / 数量 / 规格 / 单价 / 金额 / 时间 / 地点 / 车牌 / 结算方式。
6. **状态**：`AcquisitionStatusEnum`（已登记 / 待付款 / 已付款 / 已开票 / 已取消）。`linkInvoice(acquisitionId, partnerOrderId)` 挂票并置待付款；`markPaidByInvoicePartnerOrderId` / `markInvoicedByInvoicePartnerOrderId` 供付款 / 开票回调推进。**#8 发起开票时应调用 `linkInvoice`**。
7. **证据链接线**：`icbc_evidence` 的合同流 / 货物流 / 信息流在 `InvoiceEvidenceServiceImpl` 里改为从收购单自动取（合同流=收购确认书、货物流=磅单+车头车尾照片、信息流=收购台账条目并以收购单字段为准）；无收购单时保持原人工补录 / 发票明细兜底。
8. **权限**：`icbc:acquisition:create|update|query|export`，已登记进 `RecyclingPermission` + `RecyclingRoleEnum`（管理员、收货员可写；开票员只读；财务可读可导出）。

> 现场端（拍照、相机、断网本地暂存）由 #20（uni-app H5）承载；后端已提供照片 URL 字段、识别回填、幂等补传与去重。一期无真实 OCR 供应商，磅单 / 车牌默认手工录入。

## #8 开票申请：预下单与自然人确认（已完成）

开票员对已登记的收购发起开票申请。校验通过后经工行预下单取得**自然人确认页面**；出售者确认后预开票状态变为「预开票成功」。**这一步不产生发票**，真正的票等付款之后（#9 / #10）。

1. **入口**：`POST /icbc/invoice-application/apply`（单笔）与 `POST .../apply-batch`（批量，逐笔独立成败）；前置校验 `GET .../pre-check?acquisitionId=&invoiceType=`。开票员只选「哪些收购单 + 专票/普票」，其余字段从收购单、出售者档案、付方档案推导（`InvoiceApplicationServiceImpl.buildPreOrderReq`）。
2. **五类校验 + 补齐方式**：租户三层资质（`isTenantReady`）、出售者状态（`assertReadyForInvoice`）、票种与计税方法（简易计税禁专票）、品类税收分类编码、收购单要件（含出售者姓名/身份证/手机/地址）；每项返回 `message`（哪里不满足）与 `remedy`（怎么补），不通过不产生任何业务。另有付方档案与收购单状态两项。
3. **幂等**：业务单号 = 收购单号 `acquisitionNo`，写进 `icbc_invoice_order.partner_order_id`（唯一）。同一收购单重复发起直接返回既有订单（`duplicate=true`），不下发第二次工行预下单。
4. **状态线**：新增 `icbc_invoice_order.acquisition_id / confirm_status / pre_invoice_status / pre_order_time`。自然人确认（`InvoiceConfirmStatusEnum` 00/01/02）与预开票（`PreInvoiceStatusEnum` 00–04）两条线独立收敛；`order_status` 由二者推出（确认完成 + 预开票成功 = 已确认）。通知（`InvoiceNotifyHandler`=03、`PreOrderExceptionNotifyHandler`=01）与预查询（`InvoiceOrderServiceImpl.queryInvoiceInfo` 现经适配层）两条路径都回写，通知乱序/重复/早到均幂等。
5. **报文固定值**：`payChannel=05 公对私结算`（`InvoicePreOrderReqVO` 默认值也由 02 改为 05）、`specificElements=24`、`buyerInvTypeCode=04`、`iitProject=1`。
6. **权限**：`icbc:invoice-application:apply|query`，管理员 / 开票员可发起，财务可查。前端新增页 `views/icbc/invoiceApplication/index.vue`（选收购单一→前置校验→发起→打开确认页→查状态；`jumpUrlBase` 默认取当前站点 origin，本地联调按需改成后端地址）。
7. **落地**：迁移 `sql/mysql/icbc-invoice-application.sql`（幂等 ALTER + 菜单）；建表语句与测试表同步更新。

> 待联调确认：工行 `confirmStatus` 是否随 03 通知一起下发（当前通知缺失时也能靠预查询补齐）；`areaCode` 目前由开票员填写，后续可考虑挂到租户企业信息。

## #9 付款：付方支付与资金流回单归档（已完成）

对**预开票成功**的收购发起付款，生成企业支付页面；回收企业授权后货款经公对私结算直付出售者本人银行卡，平台不碰资金。

1. **入口**：`POST /icbc/payment/apply`（`PaymentApplyReqVO`：`partnerOrderId` 或 `acquisitionId` 二选一 + `amount` + `verifiedCode`/`ukeyId`）。只有 `preInvoiceStatus=02` 才能付款；付款金额恒等于收购单金额，传入不一致金额报 `PAYMENT_AMOUNT_MISMATCH`。
2. **状态机**：新增 `PaymentStatusEnum`，把工行 `payStatus`（`-1/00/01/02/03/04/05/06/07/12/25`）收敛为 10 个平台状态；失败 / 关闭 / 冲正 / 退汇 / 部分成功**可重新发起**，成功与在途不重复提交（同一支付单复用，`retry_count+1`）。
3. **收敛单一入口**：`PaymentServiceImpl.applyPaymentStatus` 同时被通知路径（新增 `PaymentNotifyHandler`，`notifyType=02`）与主动查询路径（`queryPaymentStatus` 经 `queryInvoiceInfo` 取 `payStatus`）调用，两侧一致。通知早于落库时抛 `CALLBACK_BUSINESS_NOT_EXISTS`，通知落失败可重放。
4. **回单归档**：支付成功时把 `receipt_no/receipt_time/actually_received_amount` 归档到支付单，回写开票单支付状态，并把收购单推进为「已付款」；`GET /icbc/payment/receipt` 可取回单。资金流证据由 `InvoiceEvidenceServiceImpl` 取「转账回单」。
5. **落库**：`icbc_payment_order` 新增 `acquisition_id / invoice_order_id / pay_status / actually_received_amount / receipt_no / receipt_time / receipt_file_url / retry_count`，迁移见 `sql/mysql/icbc-payment.sql`；测试表同步。
6. **权限**：复用已登记的 `icbc:payment:create|query`；移除旧的 `/icbc/payment/create` 与 `/icbc/payment/notify`（回调统一走 `/admin-api/icbc/callback/notify`）。
7. **前端**：`views/icbc/payment/index.vue` 改为按合作方订单号发起、查状态（异常态高亮 + 重新发起）、查回单。

> 待联调确认：`verifiedCode`/`ukeyId` 在 `payChannel=05` 场景下是否必填（当前为可选透传）；工行是否提供转账回单文件（当前 `receipt_file_url` 可空，以回单号与流水为准）。

## 下一步建议

- **A.** #10 发票下载与五条状态机：付款成功后下载发票原件、归集五条状态线；
- **B.** #20 收货员现场端（uni-app H5）：相机、离线暂存与补传（对接 `sync-offline`）。

## 约定与坑

- **权限**：icbc 控制器用 `@icbc.hasPermission`（`RecyclingRoleEnum` 写死角色→权限）。新增权限**必须**在 `RecyclingPermission` + `RecyclingRoleEnum` 登记，否则连超管也 403（未登记即拒绝）。
- **工行 UI 页面**：预下单/付款/入驻返回的是自动提交表单 HTML，用 `src/views/icbc/util.ts` 的 `openIcbcForm()` 打新窗口，不能当 URL 跳。
- **新增 icbc 表**：工行返回字段（如 `payee_no`/`payer_no`）在本地库应为可空；表放 `backend/sql/mysql/`，菜单用 `icbc-menu.sql` 幂等维护。**全局表**（无 `tenant_id`，如 `icbc_scrap_code`）必须登记进 `yudao-server/src/main/resources/application.yaml` 的 `yudao.tenant.ignore-tables`，否则会被拼上 `tenant_id`。单测表结构在 `yudao-module-icbc-biz/src/test/resources/sql/create_tables.sql`。
- **测 icbc**：`mvn -pl yudao-module-icbc/yudao-module-icbc-biz test`；改了 `-api` 先 `mvn -pl ...-api -DskipTests install`。**不要**用 `-am test`（上游模块有既有失败会挡住 reactor）。
- **前端**：`pnpm build:local` 验证编译；`pnpm ts:check` 有 1247 个既有 TS 错误，判断自己的改动看 `src/(views|api)/icbc` 有无新报错即可（跑 ts:check 需加 `NODE_OPTIONS=--max-old-space-size=6144`）。
- **时间字段**：yudao 全局 Jackson 把 `LocalDateTime` 按**毫秒时间戳**序列化/反序列化（`TimestampLocalDateTimeSerializer/Deserializer`）。因此 `@RequestBody` 里的 `LocalDateTime` 字段，前端日期选择器必须用 `value-format="x"`，接口类型声明为 `number`；字段上的 `@DateTimeFormat` 对 JSON body **无效**（只作用于 query/form）。不要用 `YYYY-MM-DD HH:mm:ss`，否则反序列化会得到 0 或报错。
- **工具**：不要在同一条消息里同时发 `edit` 和依赖它的 `bash`（会并发，文件可能未落盘）。
