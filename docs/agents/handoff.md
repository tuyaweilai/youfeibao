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
- `fb87607` #8 开票申请：按收购单预下单、五类前置校验与自然人确认
- `e282e87` / `f2b5327` #9 付款：预开票成功后付方支付、状态收敛与转账回单归档
- 本次提交 #10：开票、缴税与上传状态——付款成功触发开票、三条状态线独立收敛与可见、缴税凭证
- 本次提交 #14：红冲与发票取消——四种原因、红蓝一一对应、确认单撤销与预开票取消
- 本次提交 #12：额度风控与跨租户合并——连续 12 个月滚动台账、500 万硬校验与经营主体登记引导、月 10 万免征线、1% / 3% 分列
- 本次提交 #13：代办税费申报——按月清单与合计、申报期预警与缺项、缴款凭证归档并关联发票、10 万元免征单独列出、补缴按 1% / 3% 分列、出售者汇算清缴提醒与免登录对账
- 本次提交 #15：平台运营通知监控与重放——九类通知跨租户查看与分类概览、失败原因可见与手动重放（不重复业务、已成功不重放）、全平台五流齐备率与异常票清单
- 本次提交 #16：平台计费计量——按成功开具的报废产品收购发票张数计量、红冲后不重复计入、按租户与期间查看应计费用、台账平台自持租户不可写

## 管理后台菜单现状

- 反向开票：出售者档案 / 出售者建档 / 收购登记 / 付方档案 / 开票申请（按收购单）/ 付款 / 发票下载与证据 / 一票一档 / 额度台账 / 代办税费申报（申报清单 + 补缴 + 汇算清缴）
- 租户开票就绪：企业信息 / 企业资质 / 开票就绪自检 / 三层资质 / 编码配置 / 企业授权
- 平台运营：资质核实（跨租户）/ 报废产品编码表 / 通知监控（跨租户，含九类概览与重放）/ 全平台证据与异常票 / 计费计量

## 现场端（uni-app，一期 H5）

- 工程在 `backend/yudao-ui/yudao-ui-field-uniapp/`：新建最小 uni-app（vue3 + vite + TS + pinia），决策见 `docs/adr/0016`。
- **#22 基座 / #23 登录骨架已完成**：#23 起请求统一带 `tenant-id` + token（`src/utils/request.ts`，401 清登录态回登录页）、登录态持久化（`src/store/auth.ts`）、首页三入口与未登录拦截。
- **#24 收购登记主流程已完成**：选品类带出单位 / 税率 / 计税方法 / 编码，身份证或手机号带档，金额与净重自动推算，漏填品类 / 磅单号拦住提交，额度提示与确认书导出（`pages/acquisition/index|detail`、`pages/my-acquisitions`）。为此给收货员补了只读的 `icbc:goods-config:query`（现场选品类要读配置）。
- **#25 照片与识别回填已完成**：拍磅单 / 车头 / 车尾上传（`/infra/file/upload`），车牌实时比对，详情页可修正识别结果（`/icbc/acquisition/correct`）后重新比对；一期无真实 OCR，手工录入为主。
- **#26 新出售者手续已完成**：`pages/payee` 带档或新建后依次实名 / 入驻 / 协议 / 授权，工行自动提交表单用新窗口承载（`utils/icbcForm.ts`），后端 `sync` 收敛，失败可留联系方式。
- **#27 弱网暂存与补传已完成**：照片选完即存 base64，提交失败 / 断网落本地草稿，恢复后 `/icbc/acquisition/sync-offline` 逐条幂等补传（`utils/draft.ts`、`pages/offline`，首页有计数入口）。
- 现场端一期子票 #22–#27 已全部完成；#36 又补上 `pages/settlement`（结束本次收货 / 看确认进度 / 一键转达确认链接），首页菜单名为「结算与确认」。

## 自然人出售者端（uni-app，一期小程序优先 / H5 兜底）

- 工程在 `backend/yudao-ui/yudao-ui-seller-uniapp/`（决策见 `docs/adr/0011`）。
- **#28 薄前端已完成**：入口取 `token` + `purpose`（一次性令牌，无账号），按用途放行额度查询 / 发票下载 / 汇算清缴 / 留联系方式，调 `/icbc/public/*`（#21）。命令：`pnpm dev:h5` / `pnpm build:h5` / `pnpm build:mp-weixin` / `pnpm ts:check`。
- **#29 承载工行实名 / 确认 / 入驻页面已完成**：新增令牌用途 `ONBOARDING`（绑定收方）与公开端点 `GET /icbc/public/onboarding/form`（后端直接输出工行自动提交表单 HTML）、`POST /icbc/public/onboarding/sync`（向工行查一次并收敛）、`GET /icbc/public/onboarding/page`（JSON：步骤 + 表单）。自然人端新增「实名与入驻」：小程序用 `web-view`、H5 新窗口打开；现场端可一键生成自助链接（`VITE_APP_SELLER_URL`）交给出售者。容器顺序：一期 H5、随后小程序、不做 App。
- **工行答复（2026-09-19）**：实人认证走工行 H5 活体，H5 与小程序 `web-view` 都可用、不做 App；`jumpUrl` / `failJumpUrl` 可定制但**不支持 scheme**，回跳只能用 https 页面（我们自己的域名，如 `https://yiyoubao.baibaitan.com/seller/...`），小程序在容器内打开后再由前端路由回具体页面；`trxChannel` 按渠道上送即可、用错无后果。据此在 `icbc.seller-onboarding.jump-url` / `fail-jump-url` 配 https 落地页即可。
- 命令：`pnpm dev:h5` / `pnpm build:h5` / `pnpm ts:check`；开发期 `/admin-api` 代理到 48080，登录租户 1 + `admin/admin123`。
- H5 运行时能力（相机 / 离线存储 / 承接工行自动提交表单）需真机冒烟，清单在 `yudao-ui-field-uniapp/README.md`。

## #31 自然人身份层与注册（已完成）

按 [ADR 0017](docs/adr/0017-自然人身份层为平台级并引入注册.md) 把自然人身份从「每租户一份」升为平台级，并引入注册。**这是 #33 / #34 的底座。**

1. **平台级自然人主体**：新表 `icbc_natural_person`（**无 tenant_id**，已进 `ignore-tables` 与 `IcbcTenantTestConfiguration`），身份证件号码为唯一锚点，持有平台级 `outUserId`（`NP` + 32 位十六进制）；接口 `NaturalPersonService`。同身份证已有主体且手机号/姓名不一致时**拒绝、不覆盖、不自动合并**（`NATURAL_PERSON_IDENTITY_TAKEN`）。
2. **登录凭证与身份分离**：`icbc_natural_person_login`（多对多）。登录凭证复用 `yudao-module-member`（给 `MemberUserApi` 加了 `createUserIfAbsent`），**落在平台租户 `icbc.seller.platform-tenant-id`（默认 0）**；登录代码在 `TenantUtils.execute(平台租户)` 下跑，不跟着他扫码的那家企业的租户走。
3. **归属划分**：**实人认证结果归自然人主体**（跨企业复用），**收方入驻状态归收方档案**（与子商户绑定的动作）。`icbc_payee_info` 新增 `natural_person_id`；历史档案第一次访问时由 `PayeeInfoService.ensureNaturalPerson` 补挂。
4. **`outUserId` 改成平台级**：实人认证 / 收方入驻 / 预下单 / 付款全用主体的编号；`partnerPayeeId` 降级为「收方档案编号」。预下单取 `发票/付款` 的 `outUserId` 已改（`InvoiceApplicationServiceImpl.outUserIdOf`）；额度台账也把主体 `outUserId` 收进 `payee_no` 口径（旧票是档案编号，两个都要收）。
5. **修正子商户不一致**：`submitPayeeOnboarding` / `queryPayeeOnboarding` 改用**本租户付方档案**的 `partnerPayerId`（原来用全局 `icbc.out-vendor-id`）；没配付方档案直接报 `SELLER_ONBOARDING_PAYER_NOT_CONFIGURED`。入驻回执靠 `appIdSub` 反查租户（`TenantUtils.executeIgnore` 查付方档案）——**定位不到就抛异常让通知落失败、在通知监控里人工处理，不猜、不跨企业乱写**。
6. **自然人登录端点**：`/app-api/icbc/seller/auth/*`（`sms-send` / `sms-login` / `logout` / `subjects` / `subjects/bind` / `subjects/unbind`）。令牌用户类型是**会员**（`/app-api` 通道）。绑定身份就是「确认时才注册」的落地：按收方档案找人，手机号与身份登记不一致就拒绝。**后续所有自然人侧业务端点先过 `assertBound`（显式指定 naturalPersonId，不静默推断）。**
7. **平台运营人工入口**：`/icbc/platform/natural-person/*`（分页 / 认领 / 解绑 / 停用恢复），权限 `icbc:platform:natural-person:query|manage` 只登记在平台运营；菜单 5173 / 5174；前端 `views/icbc/naturalPerson`。
8. **既有令牌路径行为不变**（`PublicTokenPurposeEnum` 五类），注册没有收紧任何既有通道。
9. **顺带修了一个潜在缺陷**：`icbc_callback_notify.notify_id` 由 64 加宽到 128。`IcbcNotifyParser` 的幂等键是「类型:业务号:sha1」拼出来的，业务号一变长就会溢出（平台级 outUserId 让它在测试里真的溢出了）。

> **待工行书面确认**：收方与实人认证是否按子商户隔离。ADR 0017 已按「两种答复都不返工」定形（实人认证归主体、入驻状态归档案）；确认后只需调整「是否允许复用既有收方」。

> **#31 未做的部分**：自然人端（`yudao-ui-seller-uniapp`）的登录界面与「待我确认」等页面属于 #34；#31 只交付后端端点与平台运营 PC 页。

> **身份冲突人工清单**：`GET /icbc/platform/natural-person/conflicts`（权限 `icbc:platform:natural-person:query`）跨租户只读聚合 `icbc_payee_info`，列出「同一身份证、姓名或手机号不一致」的档案；平台运营「自然人主体」页有「身份冲突清单」弹窗。清单只读，不自动合并；裁决走人工认领 / 解绑 / 停用。迁移 SQL 末尾的原查询保留作 DBA 兜底核对。

> **绑定身份的租户口径**：`bindSubject` 运行在扫码企业的租户下，但登录凭证落在平台租户，读凭证必须回到平台租户（`inPlatformTenant`）；否则手机号一致性校验会被静默跳过。

## #32 计价模型（已完成，提交 `6d03243`）

按 [ADR 0019](docs/adr/0019-结算重量为唯一计价基准.md) 让收购单表达现场真实计价，**是 #33 的硬前置**。

1. `icbc_acquisition` 新增：`deduction`（扣杂原始值）、`deduction_method`（WEIGHT/RATIO）、`settlement_weight`、`adjustment_amount`、`adjustment_reason`、`quantity_note`、`driver_name`、`driver_mobile`。字段 `realNameStatus` 之外无其他迁移。
2. **结算重量 = 毛重 − 皮重 − 扣杂**，唯一计价基准；**金额 = 结算重量 × 单价 + 调整项**；数量降级为展示与发票明细字段。
3. 扣杂支持按重量 / 按比例，只存原始录法 + 换算结果；调整项非零必须带原因；结算重量不得为负。历史数据扣杂按 0 兼容，旧单金额不回算。
4. 发票明细 `icbc_order_item.quantity_note` 保留口径说明（发票数量与磅单净重不再相等）。
5. 现场端（`yudao-ui-field-uniapp/pages/acquisition`）可录扣杂 / 调整项 / 司机，并实时展示结算重量与金额推算。
6. 迁移 SQL 在 `sql/mysql/icbc-acquisition.sql`（幂等），测试建表同步。

> **#33 现已完成**（原 `Blocked by: #32`）：结算单聚合、确认门禁 `SETTLEMENT_CONFIRMED`、异议与版本快照、超时转线下签字、确认记录进合同流。

## #33 结算单 + 确认门禁 + 异议与版本留痕（已完成，提交 `3d07e4c`）

在收购与开票之间补上出售者的认可（ADR 0018 / 0022 / 0024），是本次升级的核心。

1. **结算单**：`icbc_settlement`（一次到场批次一张）+ `icbc_settlement_version`（整单快照，只追加，当前版本用指针指）。`icbc_acquisition` 挂 `settlement_id` / `batch_key` / `cancel_reason`。
2. **生成**：收货员显式「结束本次收货」（`POST /icbc/settlement/generate`），聚合同一出售者尚未归组的收购单；**生成后不得再加单**，要加只能新建；离线按 `batch_key` 归入同一结算单。结算是否结清由其下收购单**推导**，不落库。
3. **确认门禁**：开票 pre-check 新增 `SETTLEMENT_CONFIRMED`，未确认不下发工行预下单。自然人端 `POST /app-api/icbc/seller/settlement/confirm` 勾选即确认，留痕时间 / IP / 设备 / 该版快照 SHA-256 哈希。
4. **异议与版本**：固定原因枚举（重量 / 扣杂 / 单价 / 品类 / 货物 / 其他）；企业两个动作——改（新版本 + 原因 → 回待确认）或 不改但附说明（→ 回待确认）；连续异议 ≥3 次提示转线下；已开票后不得改，只能红冲。不做聊天 / 工单。
5. **线下签字逃生门**：`POST /icbc/settlement/offline-sign`，上传带签字的纸质确认书 + 办理人，等价于确认。
6. **作废**：未开票的收购单可作废（留原因、对自然人可见、作废后不可再开票付款）。
7. **超时**（`icbc.settlement.*` 可配）：确认 7 天 / 异议后企业处理 3 天 / 已确认未开票 30 天。到期**不自动确认**，升级为「需线下签字确认」；`SettlementTimeoutJob`（每日 08:30，@TenantJob）执行。异议超时自然人侧显示「企业尚未回复」。
8. **证据**：确认记录（含快照哈希、线下签字件）作为合同流签署证据进一票一档，**不新增第六流**。
9. **权限 / 菜单 / SQL**：`icbc:settlement-confirm:query|manage` 登记进 `RecyclingPermission` + `RecyclingRoleEnum`；菜单 5175 / 5176；迁移 `icbc-settlement.sql`（幂等）；测试建表与 `clean.sql` 同步。企业管理 PC 页 `views/icbc/settlement`。

> **自然人端 UI 属 #34**：#33 只交付后端端点（`/app-api/icbc/seller/settlement/*`）。


## #34 自然人端首页与记录（已完成）

把自然人端从「一次性令牌的只读薄前端」变成他能真正翻账的地方：场站二维码扫码进场 → 待我确认 → 确认 / 有异议 → 看历史、收款、发票与授权。对外文案遵守 ADR 0021。

1. **场站与场站二维码**：新表 `icbc_station`（租户表），一码一场站，码内**不带任何令牌**，只编码 `stationCode`（全局唯一，与 `uk_station_code` 对齐）。管理后台「场站」页（权限 `icbc:station:query|manage`，菜单 5177/5178）可增删改、复制入口链接、渲染二维码（`components/Qrcode`）。入口链接来自 `icbc.station.entry-url`（`ICBC_STATION_ENTRY_URL`），未配置时退化为 `?station=<code>`。
2. **免登录首屏**：`GET /admin-api/icbc/public/station?code=` 只返回公开信息（回收企业名称、场站、地址、是否在收货、场站电话）+ 所属 `tenantId`，**不含任何个人数据**；按 IP 固定窗口限流（`icbc.station.resolve-per-minute`，默认 60，进程内计数，不引 Redis / protection starter）。免登录端点在既有白名单与 `tenant.ignore-urls` 内。
3. **手机号验证与身份匹配**：复用 #31 的 `/app-api/icbc/seller/auth/*`。新增 `POST /subjects/bind-by-mobile`：他扫码进来没有 `payeeId`，用登录手机号在**当前租户**内找收方档案并走与 `bindSubject` 相同的手机号一致性校验；匹配不到返回空列表（前端给「你在这家场站没有待确认的货」空态，不造假列表）。
4. **首页五项**（`pages/home`）：待我确认 / 我的记录（按回收企业分组，跨企业仅本人可见）/ 收款记录 / 发票与税费 / 我的资料。后端在 `SellerPortalServiceImpl`，端点 `/app-api/icbc/seller/portal/*`，全部要求显式 `naturalPersonId` 并 `assertBound`；跨租户读取集中在本类，用 `TenantUtils.executeIgnore` 显式表达。
5. **收款口径**：`icbc_payment_order` 新增 `seller_received_confirmed_at` / `seller_received_confirm_ip`。收款状态只讲「待付款 / 处理中 / 银行已受理（回单号）/ 失败（给下一步）」；「我收到了」由 `POST /payments/received` 记录，**不改银行状态**。
6. **企业授权自助撤销**：`icbc_seller_authorization` 新增 `revoked_at` / `revoke_reason`；`POST /authorizations/revoke` 按租户撤销（两个授权位归零 + 留痕），只拦未来，不追溯已开票。
7. **确认书打印**：后端输出可打印 HTML（`/portal/acquisition/confirmation`、`/portal/settlement/confirmation`），H5 用浏览器「打印 / 保存为 PDF」；一期**不引 PDF 库**，也不做批量 Excel 导出。
8. **发票 PDF**：`GET /portal/invoice/download` 校验发票属于本人后复用 `InvoiceDownloadService` 输出工行 PDF。
9. **管理后台菜单**：`icbc-menu.sql` 新增 5177 / 5178；`RecyclingPermission` / `RecyclingRoleEnum` 登记 `STATION_QUERY|MANAGE`。迁移 `icbc-station.sql`、`icbc-seller-portal.sql`（幂等）；测试建表与 `clean.sql` 同步。
10. **顺带修一个测试基础设施缺陷**：`UnitTestConfiguration` 的内嵌 H2 库名固定为 `testdb`（`DB_CLOSE_DELAY=-1`），多个测试上下文共享同一个库；Spring 淘汰某个上下文时 `SHUTDOWN` 会把还在用的库删掉，另一个上下文变成「空库」。已改为 `.generateUniqueName(true)`，每个上下文独立库名（#34 新增两个测试类后正好触发）。

> **场站维度已补齐（#33/#34 收口）**：「按该场站 + 该自然人主体匹配待确认结算单」已落地——`icbc_acquisition` / `icbc_settlement` 都挂上了 `station_id`，`portal/home` 接受可选 `stationId` 并按该场站过滤待确认结算单（不传则仍跨企业）。见下节。

> **对外口径**：金额一律「本平台累计，不含其他渠道」；额度另标「税务端可核验的口径由各回收企业的开票记录构成」；**不出现「已到账」**；付款 / 开票 / 税费三条状态线分别显示。


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

## #10 开票、缴税与上传状态（已完成）

付款完成后，工行把报废产品收购发票真正开出来、代办缴完税、把票上传税务端；平台把这三条状态线各自收敛、各自可见，并给出异常时的下一步。

1. **三条独立状态线**：新增枚举 `InvoiceIssueStatusEnum`（未开票 / 开票中 / 已开票 / 开票失败）、`TaxStatusEnum`（未缴税 / 缴税中 / 缴税成功 / 缴税失败 / 金额不一致 / 未知异常 / 无需缴税）、`UploadStatusEnum`（未上传 / 处理中 / 已受理 / 上传中 / 上传成功 / 上传失败）。`icbc_invoice_order` 补 `upload_status / tax_real_amount / tax_time / tax_payment_method / tax_voucher_no`。
2. **通知与查询唯一收敛入口**：`InvoiceOrderService.applyInvoiceInfo(partnerOrderId, InvoiceInfo)`。通知 01/03/04/05 与主动预查询都调它，两侧天然一致。通知报文经 `InvoiceNotifyInfoAssembler` 装配成与预查询同一个 `InvoiceInfo`。新增 `TaxNotifyHandler`（04）与 `InvoiceUploadNotifyHandler`（05）。
3. **付款成功触发开票**：`PaymentServiceImpl.applyPaymentStatus` 首次成功时调 `InvoiceOrderService.onPaymentSucceeded` —— 先把开票状态推进为「开票中」，再尽力向工行确认一次；该方法**不抛异常**，开票数据缺失或查询失败都不把已成功的付款拖回失败。
4. **乱序 / 重复 / 早到最终一致**：重复通知按 `notifyId` 去重；乱序时已结清的成功态（已开票 / 缴税成功 / 上传成功）不被旧的进行中态回退；早到时查不到业务单抛 `CALLBACK_BUSINESS_NOT_EXISTS`，通知落失败可重放。订单状态由各状态线推出且不回退（已取消不碰）。
5. **缴税凭证**：`GET /icbc/invoice-order/tax-certificate`（+ `/export` 出 Excel 可打印）。缴税成功或无需缴税后可出；含扣缴义务人、出售者（身份证脱敏）、发票号、应缴/实缴税额、缴税时间、缴纳方式、应征凭证序号。复用权限 `icbc:invoice-order:query`（管理员 / 开票员 / 财务）。
6. **异常可见 + 下一步**：`InvoiceQueryRespVO` 增 `invoiceStatusName / taxStatusName / uploadStatusName / nextAction` 等；异常态优先给出该条的下一步动作。前端 `views/icbc/invoiceApplication` 状态弹窗改为五条线分列展示，并加「查看缴税凭证 / 导出凭证」。
7. **落地**：迁移 `sql/mysql/icbc-invoice-issuance.sql`（幂等 ALTER）；`icbc_invoice_tables.sql` 与测试表同步。

> 两条易混的状态线：`pre_invoice_status` 是工行的「预开票状态」（自然人确认后即为预开票成功，**不产生发票**）；`invoice_status` 是「发票真正开出」的状态，**拿到发票号码才算已开票**。付款成功是后者的触发点。`invoiceNo` 现由工行 `invoiceCode`（发票号码）回填，证据链的发票流随之可用。

> 待联调确认：工行通知 03 是否在付款后再次下发并带 `invoiceCode`（当前无此通知时靠 `onPaymentSucceeded` 的主动查询 + 后续预查询补齐）；04 通知是否带 `voucherNum`（当前凭证序号取征收信息明细的 `voucherNum`，缺失时凭证仍可出、该字段为空）。

## #14 红冲与发票取消（已完成）

把「票开错了 / 退货 / 中止 / 折让」与「没付款的预开票不想开了」两条路走通，红蓝一一对应。

1. **三种操作别混**：红字冲销开票（UI `/ui/jft/ui/red/invoice/offset/V1`，对已开出的蓝票另开红票）、红字确认单撤销（数据 `.../offset/revoke/V1`，票未上传成功时收回）、发票取消（数据 `/api/jft/api/invoice/reversal/V1`，**仅限预开票成功但未支付**）。
2. **新表 `icbc_red_invoice`**：一张蓝票同时最多一张生效中的红票；`red_offset_no`（工行 `outRedOffsetId`）唯一，`partner_order_id` 挂回蓝票。撤销后再冲会新增一条（保留旧记录留痕），失败复用同一条 + `isRedo=Y`。
3. **红冲原因**：`RedInvoiceReasonEnum`（01 开票有误 / 02 销货退回 / 03 服务中止 / 04 销售折让）。**开票有误强制全额红冲**：金额留空按蓝票金额、填写则必须一致，明细自动按蓝票生成、若手填则逐行比对金额与数量（`RED_INVOICE_AMOUNT_MISMATCH` / `RED_INVOICE_GOODS_MISMATCH`）。
4. **状态线**：`RedOffsetStatusEnum` 把工行 `redOffsetStatus`（00–11：申请中 / 申请成功 / 上传成功 / 撤销成功…）收敛为平台状态，`red_offset_status_code` 原样保留工行码。**只有 07 上传成功才算红票开出**（`isRedInvoiceIssued`）；撤销允许到上传成功之前（07/09/10 不可撤）。
5. **收敛单一入口**：`RedInvoiceService.applyRedInvoiceInfo(redOffsetNo, InvoiceInfo)` 同时被通知（07/08/09，新增 `RedApplyNotifyHandler` / `RedUploadNotifyHandler` / `RedRevokeNotifyHandler`）与主动查询（`GET /icbc/red-invoice/query`）调用。终态（红冲成功 / 已撤销）不回退；重复幂等；早到抛 `CALLBACK_BUSINESS_NOT_EXISTS` 可重放。红冲通知报文同时带蓝票 `outOrderId`，处理时**显式取 `outRedOffsetId`**（`AbstractRedNotifyHandler`）。
6. **发票取消**：`RedInvoiceService.cancelPreInvoice` 仅放行「预开票成功 + 未支付」，成功后 `applyInvoiceCancelled` 把预开票状态置取消、订单状态置 9；已支付 / 已开票拒走取消、改走红冲。06 通知由 `InvoiceCancelNotifyHandler` 收敛。
7. **证据与状态同步**：蓝票查询响应带红冲状态 / 红冲流水号 / 红票号；证据链发票流在红票开出后增列「红字发票（红冲）」（`InvoiceEvidenceServiceImpl`）。
8. **权限**：新增 `icbc:red-invoice:apply|revoke|query` 与 `icbc:invoice-order:cancel`，已登记进 `RecyclingPermission` + `RecyclingRoleEnum`（管理员 / 开票员可发起与撤销，财务只读，平台运营可查）。
9. **落地**：`sql/mysql/icbc-red-invoice.sql`（建表）+ `icbc-menu.sql`（按钮权限，删除范围放宽到 5100–5199）；前端 `views/icbc/invoiceApplication` 状态弹窗加「取消预开票 / 发起红冲 / 撤销红字确认单」与发起红冲对话框，接口在 `api/icbc/invoice` 的 `RedInvoiceApi`。

> 待联调确认：红字冲销请求报文的金额与数量正负号约定（当前按「与蓝票一致」传正值，工行响应里的红冲金额是负值）；`redOffsetStatus` 是否随 07 通知下发（当前缺失时靠主动查询 `outRedOffsetId` 补齐）。

## #12 额度风控与跨租户合并（已完成）

「在票开出来之前就把额度问题拦住」。工行没有事前校验接口，也没有额度视图（见 `docs/icbc/工行答复-2026-09-18.md` §四），所以台账是平台自建的，且**派生**于票据事实（决策见 `docs/adr/0014-额度台账为派生视图.md`）。

1. **额度属于自然人，不属于租户**：`NaturalPersonQuotaServiceImpl.loadLedger` 在 `TenantUtils.executeIgnore` 下按身份证号把同一自然人在本平台各租户的收方档案找齐，再跨租户汇总。
2. **连续 12 个月滚动窗口**（不是自然年）：已开票按**开票日期**落窗口；在途（预开票成功待开票、开票中）也占额度；**红冲只扣「原蓝票也在窗口内」的那部分**——蓝票已滚出窗口的，本就未计入，不能再放一次额度。口径只有一处，三个入口（登记提示、开票硬校验、出售者查询）共用。
3. **硬校验**：开票申请前置校验新增 `SELLER_QUOTA` 项；超限时不下发工行预下单，报错里给出已用 / 余量 / 怎么补。已用额度 = 已开票 + 在途 − 红冲。
4. **引导经营主体登记**：因额度被拒时落 `icbc_seller_quota_guidance`（待引导 / 已引导 / 已办结），同一出售者同时只有一条未办结记录（再次被拒只刷新额度与时间）。新增页 `views/icbc/quota` 与 `GET /icbc/quota/seller|check`、`GET /icbc/quota/guidance/page`、`PUT /icbc/quota/guidance/handle`。
5. **收购登记时给余量提示**：`POST /icbc/acquisition/create` 返回 `AcquisitionCreateRespVO`（额度上限 / 已用 / 余量 / 结论 / 是否超 10 万免征线），前端登记成功后弹窗展示；**只提示不拦截**，硬校验在开票申请。确认书导出也带上额度余量。
6. **出售者能自己查余量，零客户端**：公开令牌端点 `GET /icbc/public/quota` 扩成完整台账（已开票 / 在途 / 红冲 / 余量 / 月销售额 / 1% 与 3% 分列）；额度页可一键生成二维码 / 链接交给出售者（复用 `icbc:public-token:create`）。
7. **月 10 万元免征线**按「出售者 × 月」判定（跨租户合并），超线只提醒「须按时代办申报缴款」，不是拒绝理由。
8. **1% / 3% 分列**：`icbc_invoice_order` 新增 `tax_rate`，预下单时从品类配置落库；台账按 0.01（3% 减按 1%）与 0.03（放弃减按）分列，征收率未识别的历史数据单列 `otherAmount`。放弃减按时报文补上 `unuseReduceTaxCode=55`（工行「税率传 3 时必输」）。
9. **权限**：新增 `icbc:quota:query`（管理员 / 收货员 / 开票员 / 财务 / 平台运营）与 `icbc:quota:guidance:handle`（管理员 / 收货员）；菜单见 `icbc-menu.sql` 5162–5163。
10. **落地**：`sql/mysql/icbc-quota.sql`（`icbc_invoice_order.tax_rate` 幂等 ALTER + 引导建表）；`icbc_invoice_tables.sql`、测试表与 `clean.sql` 同步。

> 待联调确认：`tax_rate=3%` 是否就是「放弃享受减按 1%」（当前据此上送 `unuseReduceTaxCode=55`）；在途是否计入额度占用（当前计入，为避免并发超额）；征收率目前只来自品类配置，若工行要求按出售者声明，需在出售者建档加字段覆盖品类税率。

## #15 平台运营：通知监控与重放（已完成）

运营能看到工行推来的每一类通知处理成没成、卡在哪，失败的能手动重放；并在同一个地方看到全平台的五流齐备率与异常票。

1. **通知是全局事件，不按租户过滤**：`icbc_callback_notify` 记录由 `@TenantIgnore` 的入口落库（`tenant_id` 默认 0），因此平台查询统一用 `TenantUtils.executeIgnore`（与 `PlatformInvoiceQueryServiceImpl` 同一模式），不用 `@TenantIgnore` 注解——单测里没有注册该注解的切面，用 `executeIgnore` 才能在测试里也真正跨租户。
2. **九类通知概览**：`CallbackNotifyService#getPlatformCallbackNotifySummary` 按九类固定顺序铺开，返回总数 / 待处理 / 成功 / 失败与按类型细分；某类一条没收到也会在概览里显示为 0，不伪造。
3. **失败可定位、可重放、不重复**：详情返回 `notifyTypeName / businessName / processStatusName / processMsg / nextAction`；`replay` 复用 #3 的行为——待处理与失败会重新分发，已成功直接忽略，同一 `notifyId` 唯一索引兜底，重放不产生重复业务。
4. **权限**：`icbc:platform:callback:query` / `icbc:platform:callback:retry` 只登记在平台运营（超管放行），租户管理员不再拥有通知读写——跨租户的通知只在平台侧可见。
5. **全平台五流齐备率与异常票**：`PlatformEvidenceService`（新增）在 `TenantUtils.executeIgnore` 下复用 `InvoiceEvidenceService#getCompleteness`，得到全平台齐备率与逐票明细（`tenantId` 已补进 `EvidenceCompletenessItemRespVO`）；异常票清单把状态线异常（开票失败 / 缴税异常 / 上传失败 / 付款异常 / 红冲异常）与「已开票但五流不齐」各自列为一条 `reason`，未开出的票缺证据不算异常。权限 `icbc:platform:evidence:query`。
6. **三种时序**：#3 / #14 已覆盖乱序（终态不回退）、重复（按 `notifyId` 去重）、早到（处理失败可重放）；#15 不改收敛逻辑，只在平台侧把它们可视化。
7. **落地**：无新表；`icbc-menu.sql` 5147–5149（通知监控 / 重放通知 / 全平台证据与异常票）；旧 `/icbc/callback/page|get|replay` 收敛为平台端点 `/icbc/platform/callback/*`，入口 `POST /icbc/callback/notify` 不变。前端新增 `views/icbc/platformCallback` 与 `views/icbc/platformEvidence`。

> 测试：`PlatformCallbackNotifyServiceImplTest`（跨租户分页、九类概览、按业务聚合、业务名映射）与 `PlatformEvidenceServiceImplTest`（跨租户齐备率、四类状态异常、红冲异常、已开票证据不齐、健康票不误报）。

## #16 平台计费计量（已完成）

运营按租户、按期间看到本期成功开具的发票张数与应计费用，口径与对账一致，台账不可被租户篡改。

1. **计量口径只有一处**：`PlatformBillingServiceImpl`。蓝票「成功开具」= {@code invoice_status=已开票 且 有发票号}、业务类型为报废产品收购（{@code SCRAP}）、开票日期落在期间内；被成功红冲（红票上传成功）的蓝票从计费张数里扣掉，红票本身永不单独计入——即「对账口径一致、红冲后不重复计入」。进行中的红冲不减计费。
2. **期间口径**：新增 `IcbcMonthRange`（`yyyy-MM` ↔ {@code [start, end)}），额度台账、代报申报、计费共用；`TaxDeclarationServiceImpl.MonthRange` 也改为委托它，月口径不再有两份。
3. **平台自持台账**：新表 `icbc_billing_ledger`（全局表，`tenant_id` 是被计费租户，已登记进 `yudao.tenant.ignore-tables`），唯一键 `(tenant_id, period_month)`，重新计量幂等覆盖。租户侧无任何读写入口，权限只在平台运营。
4. **单价与应计费用**：单价走配置 `icbc.billing.unit-price`（默认 0，按合同注入），计费时快照进台账，历史账不随单价变化；应计费用 = 计费张数 × 单价。
5. **端点与权限**：`GET /icbc/platform/billing/page`（`icbc:platform:billing:query`）、`POST /icbc/platform/billing/generate` 与 `/generate-tenant`（`icbc:platform:billing:manage`），只登记在平台运营。前端 `views/icbc/platformBilling`。
6. **落地**：`sql/mysql/icbc-billing.sql`（建表）+ `icbc-menu.sql` 5150 / 5172；`application.yaml` 加 `icbc_billing_ledger` 到 ignore-tables；测试建表与 `clean.sql` 同步。

> 测试：`PlatformBillingServiceImplTest`（只计本期已开出的报废产品票、红冲扣减且红票不重复计、重新计量幂等、跨租户、分页筛选、非法期间）。

## #13 代办税费申报（已完成）

财务在次月申报期前看到本期待申报的代办税费清单与金额，按时申报缴纳；出售者在次年汇算清缴前被提醒并可取自己的信息。工行只逐票缴税，申报清单、补缴、汇算清缴都是平台自持的派生工作流（决策见 `docs/adr/0015-代办税费申报为派生工作流.md`）。

1. **按月派生清单**：`TaxDeclarationServiceImpl.build` 从当月 `icbc_invoice_order`（已开票，按开票日期）与 `icbc_red_invoice`（上传成功，按红票日期）按出售者归集，算出净销售额；税额口径集中在 `IcbcTaxConstants`（增值税减按 1% / 放弃减按 3%、附加税费 6%、个税预缴 0.5%）。
2. **10 万元免征线**：按「自然人 × 月跨租户」判定，新增 `NaturalPersonQuotaService#getCrossTenantMonthlyNetAmount` 复用额度台账，不重算；**计税金额只按本租户当月开票额**。超过免征线的出售者在明细里 `overExempt=true`，单独列出。
3. **申报三步状态机**：`TaxDeclarationStatusEnum`（待申报 / 已申报待缴款 / 已缴款）。`POST /icbc/tax-declaration/declare` 报送报告表，`POST /icbc/tax-declaration/pay` 缴款并归档凭证；「逾期」由 `daysLeft<0` 推导，不落状态。
4. **预警与缺项**：`GET /icbc/tax-declaration/warning/list` 即时推导三类预警（申报期临近 / 逾期可能被暂停开票资格 / 数据不齐）；`GET /icbc/tax-declaration/precheck` 列出缺什么（票未开出、缴税未结清、出售者身份不全、红冲未成功、征收率未识别）与怎么补，其中身份与征收率缺项会挡住申报。
5. **凭证与发票关联**：缴款成功把 `voucherNo / voucherFileUrl` 写回申报单，并把覆盖到的票写入 `icbc_tax_declaration_invoice`（蓝票计入 / 红票冲减）。
6. **补缴**：`icbc_tax_supplement` 按 1% / 3% 分列，`GET /icbc/tax-supplement/summary` 给待补缴累计。已缴款申报单重新计算发现正差额时自动 `recordAuto`（同一申报单只留一条未缴清补缴，覆盖而非累加，job 每日重算幂等）。
7. **汇算清缴**：`icbc_settlement_reminder`（出售者 × 纳税年度）+ `AnnualSettlementReminderJob`；对账单给当年开票与已缴税款，出售者可凭新增公开令牌用途 `SETTLEMENT_STATEMENT` 走 `GET /icbc/public/settlement` 自取。
8. **定时任务**：`TaxDeclarationReminderJob`（每日 09:00，刷新上月清单并统计预警，仅当月有开票才生成）、`AnnualSettlementReminderJob`（每月 1 日 09:30），种子见 `icbc-jobs.sql`。
9. **权限**：新增 `icbc:tax-declaration:query|manage`、`icbc:tax-supplement:manage`、`icbc:settlement:query|remind`，已登记进 `RecyclingPermission` + `RecyclingRoleEnum`（财务可管理全流程，开票员 / 平台运营只读，管理员全量）。菜单见 `icbc-menu.sql` 5164–5171。
10. **落地**：`sql/mysql/icbc-tax-declaration.sql`（5 张新表）；`icbc_invoice_tables.sql`、测试建表与 `clean.sql` 同步；前端 `views/icbc/tax/index.vue`（三个页签：申报清单 / 待补缴 / 汇算清缴）。

> 待联调确认：申报期截止日固定为次月 15 日（未顺延法定节假日）；附加税费综合率 6% 为「城建 7% + 教育费附加 3% + 地方教育附加 2%，自然人减半」的简化；个税预缴 0.5% 由平台按销售额计算，与工行逐票 `taxAmount` 的对应关系待现场核对。汇算清缴对账单只覆盖本租户，跨企业合并仍由出售者在税务端完成。

## #35 预约到站（已完成）

自然人主动声明「我将在某个时间到某个场站卖某品类、大约多少量、车牌是多少」，用于排队与**到站登记时带出**。**它不是订单**（ADR 0020 / 0004）：不占额度、不产生开票、不进五流；没有接单 / 拒单，只有到场与未到场。

1. **表 `icbc_appointment`**（租户表）：`natural_person_id` + `payee_id`（本租户未建档可空）+ `station_id/station_code/station_name` + `goods_config_id/category_name/unit` 快照 + `expected_quantity`（可空）+ `plate_no` + `expected_arrival_time` + `status`（待到站 / 已到场 / 未到场 / 已取消）+ `arrived_at/acquisition_id` + 取消与未到场留痕。迁移 `icbc-appointment.sql`（幂等）。
2. **自然人端**：`/app-api/icbc/seller/appointment/create|cancel|list|goods`。发起时按场站码解析到场站所属租户，写入落在**那家回收企业**；`assertBound` 显式校验身份；取消只限「待到站」。`list` 跨企业只对本人可见（`TenantUtils.executeIgnore`）。
3. **现场端**：`GET /icbc/appointment/pending?payeeId=` 按出售者（经自然人主体）返回本租户待到站预约，按预计到站时间升序——这是本功能的**全部价值**（带出品类 / 约多少 / 车牌）。字段端 `pages/acquisition` 带档后展示预约卡，点「带出」回填；登记成功后调 `arrive` 挂上收购单。
4. **管理后台页**：`views/icbc/appointment/index.vue`，可查到站预约并标记到场 / 未到场；菜单 5179 / 5180，权限 `icbc:appointment:query|manage` 登记进 `RecyclingPermission` + `RecyclingRoleEnum`（管理员 / 收货员；平台运营不参与）。
5. **口径**：`expectedQuantityText` 一律以「约」标注；响应带 `scopeNote`（不是订单 / 不占额度 / 不产生开票 / 不进五流）；界面上也写清。**任何统计与额度口径都不得引用预约数据**（额度只认收购单与发票事实）。
6. **测试**：`AppointmentServiceTest`（未绑定拒结、场站码无效拒结、快照与「约」文案、负数量 / 缺时间拒结、取消只限本人且只限待到站、到场幂等、终态拒绝、按出售者带出只取待到站并按时间升序、跨企业本人可见、分页筛选）与 `RecyclingRoleEnumTest#testAppointmentPermissions`。

> **场站维度已补齐**：见 #34 后的「场站维度收口」一节；`station_id` 已挂到收购单 / 结算单。

## 场站维度收口：一次到场批次按「出售者 + 场站」（#33/#34 收口）

补上 #33 / #34 唯一未满足的两条 AC：结算聚合原来只按「出售者 + batchKey」，没有场站与班次维度；
自然人端待确认原来跨全部租户返回，没有按场站过滤。

1. **数据**：`icbc_acquisition.station_id`、`icbc_settlement.station_id` + `station_name`（快照）；迁移
   `icbc-acquisition.sql` / `icbc-settlement.sql` 幂等 ALTER，测试建表同步。历史数据为空仍可聚合（`eqIfPresent`）。
2. **登记带场站**：`AcquisitionCreateReqVO.stationId` 落库；现场端收购页新增场站选择，预约带出时同时带出场站。
3. **聚合按场站**：`SettlementService.generate` 按「出售者 + 场站」取未归组收购单（`selectUngroupedByPayeeId(payeeId, stationId, batchKey)`），结算单固化场站快照。
4. **班次窗口只建议**（ADR 0018）：新增 `icbc.settlement.shift-hours`（默认 4）与
   `GET /icbc/settlement/batch-suggestion?payeeId=&stationId=`，返回窗口内未归组候选；**不自动合并**，
   是否合并由现场「结束本次收货」决定。现场端结算页展示「本班次建议 N 笔」。
5. **自然人端按场站匹配**：`/icbc/seller/portal/home` 新增可选 `stationId`；公开场站解析回传 `stationId`，
   场站码经登录带到首页；待确认结算单按「该场站 + 该自然人主体」过滤，匹配不到给明确空态（不传场站则回退跨企业）。
6. **测试**：`SettlementServiceTest` 补同场站聚合、班次建议两例；`SellerPortalServiceTest` 补按场站匹配一例。
   后端 435 测试全绿；现场端 / 自然人端 `pnpm ts:check` 与 `pnpm build:h5` 均通过。

## #36 出售者触达：短信三条 + 收货员一键转达（已完成）

结算确认成了开票硬前置之后，「他得知道有一单在等他确认」是流程能否走通的隐藏依赖。按
[ADR 0023](docs/adr/0023-触达只靠短信与收货员转达.md)：一期触达只有短信（可配置开关）与**收货员一键把确认链接转达给他**两条路；不做 App 推送、不做公众号。

1. **只发三条短信**：结算单待确认（生成 / 企业改版后）、付款异常（失败 / 冲正 / 退汇 / 部分成功等）、发票已开出。文案与 `system_sms_template` 里的内容一致，只说可核验的事（ADR 0021）；发票短信复用现成的一次性发票下载令牌（ORDER 维度）。
2. **链接为一次性令牌**：结算 / 付款用新增用途 `PublicTokenPurposeEnum.SELLER_NOTICE`（PAYEE 维度），发票用 `INVOICE_DOWNLOAD`。链接形如 `https://<seller-app>/#/?token=...&purpose=...`，打开即可查看，**不需要注册**；要确认时再用手机号验证（登录即注册）。
3. **开关默认关闭**：平台级 `icbc.notify.sms-enabled`（环境变量 `ICBC_NOTIFY_SMS_ENABLED`）+ 租户级 `icbc_notify_setting.sms_enabled`，两者取或；费用与到达率是运营成本，谁开谁清楚。
4. **入口地址**：`icbc.notify.seller-app-url`（`ICBC_SELLER_APP_URL`），未配置时退化用 `icbc.station.entry-url`；两条都为空就拼不出链接，记录会落「未配置入口」。
5. **收货员一键转达（现场端为主）**：`POST /icbc/notify/settlement/forward-link`，返回一次性链接 + 可直接复制的短信文案，可顺带发短信（**由人显式触发**，不受自动开关限制）。现场端新增 `pages/settlement/index|detail`（首页菜单「结算与确认」）：可「结束本次收货」（按手机号 / 身份证带出出售者后生成结算单）、按确认状态筛选、一键复制确认链接 / 短信转达；收购单明细页也可直接「结束本次收货」。PC 结算页保留转达入口供后台使用。无手机号时照样能复制链接转达。
6. **幂等**：`icbc_seller_notify` 唯一键 `(tenant_id, biz_type, biz_key)`，`biz_key` 对结算带版本号、对付款带异常状态码；同一事件只发一次。发不出去也留记录并写明原因（开关关闭 / 未留手机号 / 未配入口 / 通道失败）。
7. **尽力而为**：短信通道异常只落一条失败记录，**不抛回业务**（结算生成 / 付款收敛 / 开票收敛都不会因短信失败回滚）。通道用 `ObjectProvider<SmsSendApi>` 拿，测试与未接通道的环境不因缺 Bean 启动失败。
8. **落地**：`icbc-seller-notify.sql`（触达记录 + 租户级开关 + 三条短信模板，模板先用 DEBUG 渠道占位，报备后改真实渠道与 `api_template_id`）；菜单 5181 / 5182；新权限 `icbc:seller-notify:query|manage`（管理员 / 收货员可管理，财务只读）；收货员现场端 `pages/settlement`（列表 / 明细 / 结束本次收货 / 转达）；PC 页 `views/icbc/sellerNotify` 与结算页转达按钮；自然人端 `pages/index` 新增「待办提醒」入口。

> **前置确认（阻塞交付，不阻塞编码）**：短信通道供应商与签名报备——有审核周期与费用。在此之前开关保持关闭，收货员转达链路可独立工作。

> 测试：`SellerNotifyServiceTest`（默认关闭只落记录、开启后发且幂等、未留手机号 / 未配入口可解释、付款仅异常态提醒、发票带票号、转达链接与文案、免登录通知口径、租户开关、改版重发）。

## #37 换银行卡与工行答复收口（编码部分已完成，第 5 条待工行书面答复）

按 [ADR 0010](docs/adr/0010-付款走公对私直付到银行卡.md)：工行收方入驻绑的是本人**一张**卡，换卡必须重走收方入驻。

1. **不新造流程**：换卡复用既有的 `ONBOARDING` 令牌与后端输出表单机制。自然人端发起后拿到令牌，用它打开 `/icbc/public/onboarding/form`；`PublicAccessServiceImpl.buildOnboardingPage` 发现有在途变更时不因「建档已完成」而短路，直接输出**新卡**的收方入驻页。
2. **不允许多张卡**：收方档案 `icbc_payee_info.bank_card_no` 是唯一生效中的那张卡；待变更的新卡只活在 `icbc_payee_bank_card_change`（租户表，`icbc-bank-card-change.sql` 幂等）的表上，工行审核通过才搬到档案上。同一收方同一时刻只允许一笔在途变更。
3. **审核期间新交易的付款挂起**：`PayeeBankCardChangeService#assertPaymentNotSuspended` 是 `PaymentServiceImpl.applyPayment` 发起新指令前的唯一门禁（已成功 / 在途的重复调用照旧返回，不误伤）。企业侧可见：`/icbc/payee-info/page|get` 带 `bankCardChangeStatusName`，PC 出售者档案列表给「变更中」标签 + 「换卡记录」弹窗（含取消变更，用于清掉没办完的在途单）。
4. **结果归属拆开**：变更在途时入驻结果走 `PayeeBankCardChangeService#applyOnboardingResult`，**不改建档状态**——通过则新卡生效（搬到档案）并留痕为「已生效」；拒绝 / 开户失败则变更单记「已拒绝」，**原卡继续有效**（不因一次换卡失败把他的收款能力打掉）。
5. **AC5 待工行书面答复**：收方与实人认证是否按子商户隔离。两种答复**都不改数据结构**，只需按答复调整「是否允许复用既有收方」；本期未动。

> 测试：`PayeeBankCardChangeServiceTest`（发起快照旧卡尾号 / 未入驻拒结 / 重复在途拒结 / 通过换卡生效 / 拒绝保旧卡 / 仅 result 拒绝也收敛 / 无在途不碰档案 / 付款挂起与取消后恢复 / 历史与批量）；`SellerOnboardingServiceImplTest` 补换卡三例；`PaymentServiceImplTest` 补挂起一例；`PublicAccessServiceImplTest` 补换卡页与同步两例；`IcbcTenantIsolationTest` 补换卡表租户隔离一例。

> 已知简化：只有管理后台（PC）与自然人端能看到「变更中」；现场端登记时不做提示（付款本来就从 PC 发起，挂起发生在发起那一刻）。「同一收方同时只允许一笔在途」由服务层校验，未加 DB 约束（MySQL 局部唯一索引与 H2 兼容性权衡）。

## 下一步建议

- **A.** #13 代办税费申报——**已完成**；
- **B.** #15 平台运营：通知监控与重放——**已完成**；#16 计费计量——**已完成**；
- **C.** #20 收货员现场端（uni-app H5）、#19 自然人出售者端。
- **D.** #35 预约到站与 #36 触达（短信三条 + 收货员转达）——**已完成**；#37 换银行卡编码部分已完成，仅剩「收方与实人认证是否按子商户隔离」待工行书面答复（两种答复都不改数据结构）。

## 约定与坑

- **权限**：icbc 控制器用 `@icbc.hasPermission`（`RecyclingRoleEnum` 写死角色→权限）。新增权限**必须**在 `RecyclingPermission` + `RecyclingRoleEnum` 登记，否则连超管也 403（未登记即拒绝）。
- **工行 UI 页面**：预下单/付款/入驻返回的是自动提交表单 HTML，用 `src/views/icbc/util.ts` 的 `openIcbcForm()` 打新窗口，不能当 URL 跳。
- **新增 icbc 表**：工行返回字段（如 `payee_no`/`payer_no`）在本地库应为可空；表放 `backend/sql/mysql/`，菜单用 `icbc-menu.sql` 幂等维护。**全局表**（无租户隔离语义，如 `icbc_scrap_code`、`icbc_billing_ledger`——后者的 `tenant_id` 是「被计费租户」这个数据列，不是隔离维度）必须登记进 `yudao-server/src/main/resources/application.yaml` 的 `yudao.tenant.ignore-tables`，否则会被拼上 `tenant_id`。租户隔离的单测要打开拦截器（`IcbcTenantTestConfiguration`），它那里同步维护了忽略表清单。单测表结构在 `yudao-module-icbc-biz/src/test/resources/sql/create_tables.sql`。
- **测 icbc**：`mvn -pl yudao-module-icbc/yudao-module-icbc-biz test`；改了 `-api` 先 `mvn -pl ...-api -DskipTests install`。**不要**用 `-am test`（上游模块有既有失败会挡住 reactor）。
- **额度台账口径**：只在 `NaturalPersonQuotaServiceImpl` 一处；改口径（如是否算在途）不要散到调用方去。额度是**软上限**——平台自己的台账，跨平台累计不可见。
- **前端**：`pnpm build:local` 验证编译；`pnpm ts:check` 有 1247 个既有 TS 错误，判断自己的改动看 `src/(views|api)/icbc` 有无新报错即可（跑 ts:check 需加 `NODE_OPTIONS=--max-old-space-size=6144`）。
- **时间字段**：yudao 全局 Jackson 把 `LocalDateTime` 按**毫秒时间戳**序列化/反序列化（`TimestampLocalDateTimeSerializer/Deserializer`）。因此 `@RequestBody` 里的 `LocalDateTime` 字段，前端日期选择器必须用 `value-format="x"`，接口类型声明为 `number`；字段上的 `@DateTimeFormat` 对 JSON body **无效**（只作用于 query/form）。不要用 `YYYY-MM-DD HH:mm:ss`，否则反序列化会得到 0 或报错。
- **工具**：不要在同一条消息里同时发 `edit` 和依赖它的 `bash`（会并发，文件可能未落盘）。
