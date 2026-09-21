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
- 本次提交 #41（T03）：菜单清场与租户套餐骨架——删死菜单 / 三条外链 / 演示菜单、停用 ERP 菜单树、落十项一级骨架、既有 icbc 页面重新挂载、「租户开票就绪」取消并入「基础资料」、落「回收企业套餐」

## 管理后台菜单现状

**#41（T03）已把菜单收敛成租户套餐化骨架**，提交见下方「本次提交 #41」。

- 一级菜单（租户 1 的 `admin` 实测，通过 `/admin-api/system/auth/get-permission-info`）：工作台 / 系统管理 / 基础设施 / 基础资料 / 交易对方 / 采购管理 / 回收作业 / 仓储管理 / 结算管理 / 财务票务 / 业务追溯 / 经营报表 / 平台运营（共 13 个）。
- 基础资料：企业信息 / 企业资质 / 三层资质 / 编码配置 / 开票就绪自检 / 场站 / 付方档案。
- 交易对方：出售者档案 / 出售者建档 / 企业授权 / 触达记录 / 单位供货方。
- 回收作业：到站预约 / 收购登记；结算管理：结算单；业务追溯：一票一档。
- 财务票务：开票申请 / 开票申请（按收购单）/ 付款 / 发票下载与证据 / 代办税费申报 / 额度台账。
- 采购管理 / 经营报表 / 工作台：已有一级菜单与占位页（`views/icbc/{purchase,report,workbench}`）。仓储管理由 #43（T05）挂了 库位维护 / 批次维护 / 库存查询（页面在 `views/erp/stock/*`），不再是占位页；其余子项由后续票（#52 / #54 / #57 等）落地。
- 平台运营：资质核实 / 报废产品编码表 / 通知监控 / 全平台证据与异常票 / 计费计量 / 自然人主体（系统租户专有，不进回收企业套餐）。
- 已删：报表 / 支付 / 工作流程 / 会员中心 / 商城 / 公众号 / CRM / AI / IoT 的死菜单、三条外链（作者动态 / Boot 文档 / Cloud 文档）与演示菜单（代码生成案例）。
- ERP 系统那一棵（id 2563–2702）整棵 `status=1` 停用、行保留，等 T01 放开 ERP 后由后续票收敛进采购 / 仓储骨架。
- 回收企业套餐：`system_tenant_package.id = 200`（「回收企业套餐」），`menu_ids` 253 个，覆盖十项骨架 + 系统管理 / 基础设施，不含平台运营。
- 「租户开票就绪」的配置项落「基础资料」（企业信息 / 企业资质 / 三层资质 / 编码配置 / 开票就绪自检）；**企业授权按骨架留在「交易对方」**（它是自然人给回收企业的授权，是交易对方关系，不是本企业自己的配置），与验收措辞的「配置项落基础资料」有一处有意偏差。

> **平台运营与验收口径的唯一偏差**：验收写「一级菜单外加系统管理、基础设施」，实测租户 1 还多一个「平台运营」。原因是租户 1 是系统租户（`packageId = 0`）、`admin` 是 `super_admin`，而平台运营按 ADR 0026 就是系统租户自己的前台、不能删（删了平台运营页面与接口全没）。回收企业租户拿到的是 `system_tenant_package.id = 200`，不含平台运营。

## 回收企业 PC 端菜单骨架（已实施，#41 T03）

2026-09-20 定，同日由 #41 落地。决策见 `docs/adr/0025`–`0028`；评估依据见 `docs/research/2026-09-20-yudao-ERP-契合度评估.md`。菜单种子在 `backend/sql/mysql/icbc-menu.sql`（幂等，最后导）。

```
工作台          待办与预警（今日到场、待称重、待验收、待入库、待结算确认、异议、付款/票务失败、额度与资质预警、开票就绪状态徽标）
基础资料        企业与场站 / 仓库库位 / 品类等级 / 人员权限
交易对方        出售者档案 / 出售者建档 / 企业授权 / 协议 / 交易记录 ｜ 单位供货方
采购管理        采购合同 / 采购订单 / 执行进度
回收作业        到站预约 / 到场登记 / 交接批次 / 称重质检（现场端是主入口，PC 只做查改补录）
仓储管理        待入库 / 入库单 / 出入库流水 / 库存查询 / 盘点调整
结算管理        待结算 / 结算单 / 异议处理 / 期间对账
财务票务        付款记录 / 发票 / 进项收票 / 代办税费 / 异常处理
业务追溯        关联单据查询 / 一票一档
经营报表        采购履约 / 收购台账 / 库存 / 结算付款
```

- 三级菜单进**租户套餐**（`tenant_package.menu_ids`），权限收口到菜单与角色-菜单（ADR 0026，由 #40 T02 落地运行时收口）。
- **平台运营不属于租户菜单**：它是系统租户（`packageId = TenantDO.PACKAGE_ID_SYSTEM`），菜单全量。
- 曾经的「租户开票就绪」一级菜单取消：配置项进「基础资料」，**就绪状态**降级为工作台顶部徽标（T18）。
- 「ERP 系统」那一棵（`system_menu` id 2563–2702，140 行）已整棵停用、行保留：等 T01 放开 ERP 后，`stock` 域收敛进「仓储管理」、`sale` / `finance` / `account` / `statistics` 销售侧继续不分配。

## ERP 引入：已做的与未做的

**已做**：

- `backend/sql/mysql/erp.sql`（33 张表，全部带 `tenant_id`；来源见文件头），已进 README 导入顺序。
- 编译与启动冒烟已过：放开 `backend/pom.xml` 与 `yudao-server/pom.xml` 两行注释后 `mvn -pl yudao-server -am -DskipTests install` 成功、服务 `Started YudaoServerApplication` 成功（**pom 已还原**，工作树干净）。

**未做**：

- 放开两个 pom（`backend/pom.xml:31`、`yudao-server/pom.xml:142`）。
- 把 33 张表按 ADR 0025/0027/0028 改造：`goods_config_id` 替 `product_id`（约 12 张）、`erp_warehouse.station_id`、新建库位/批次/采购合同、`erp_stock` 唯一约束、6 张单据表唯一索引的可选项。改造以增量 SQL 叠加，**不直接改 `erp.sql`**（保持可与上游比对）。
- ERP 菜单收敛：T03 已把「ERP 系统」整棵停用、行保留；放开 ERP 后由 T04 / T05 / T14 / T16 把 `stock` 域菜单挂到「仓储管理」下。

**本地起服务的坑**：引 ERP 之后不能直接 `mvn -pl yudao-server spring-boot:run`（`erp-biz` 不在 m2 里会 `Could not find artifact`），先 `mvn -pl yudao-server -am -DskipTests install`。

> `mvn -pl yudao-module-erp -am ...` 只会构建父 pom 自己，**不会进子模块**；要用 `-pl yudao-module-erp/yudao-module-erp-biz -am`。

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

## #39 T01 放开 ERP 并让本地能起（已完成）

回收企业经营作业系统的地基：ERP 成为默认模块，本地库建好 33 张 `erp_*` 表，仓储分页接口不再报 SQL 错。

1. **放开两个 pom**：`backend/pom.xml` 的 `<module>yudao-module-erp</module>`、`yudao-server/pom.xml` 的 `yudao-module-erp-biz` 依赖不再注释。
2. **建表**：`backend/sql/mysql/erp.sql`（33 张表）已在 `ruoyi-vue-pro.sql` 之后导入，已往本地库导过；33 张表逐张核对均有 `tenant_id`。
3. **冒烟**：`mvn -pl yudao-server -am -DskipTests install` + `spring-boot:run` 均成功（`Started YudaoServerApplication in 10.5s`，无 bean / 路由冲突）；租户 1 的 `admin` 调 `GET /admin-api/erp/warehouse/page` 与 `/erp/stock/page` 都返回 `{"list":[],"total":0}`。
4. **ADR 0025 同步修订**：按 #38 规格，`purchase` 域不启用（采购履约链建在 `icbc`），实际只启用 `stock` 域。

## #40 T02 权限收口到菜单与角色-菜单（已完成）

把 `RecyclingRoleEnum` 从「运行时判权」改成「单一来源」：由它幂等生成菜单权限行、角色授权与租户套餐，
运行时统一走 yudao 原生的 `@ss.hasPermission`（ADR 0026）。

1. **运行时**：`icbc` 侧 33 个 Controller 的注解从 `@icbc.hasPermission('...')` 改为 `@ss.hasPermission('...')`，
   所有写死的权限字符串统一成 `RecyclingPermission.*` 常量。删掉 `RecyclingPermissionChecker` 及其测试
   （`RecyclingRoleEnum` 也不再提供 `roleCodesForPermission`）。
2. **同步**：新增 `RecyclingPermissionSyncService`。全局部分（`system_menu` 权限行 + 回收企业套餐菜单）
   由启动 runner `RecyclingPermissionSyncRunner` 自动跑（`@Profile("!unit-test")`，失败只 warn 不阻断启动）；
   租户内的角色与角色-菜单由 `POST /icbc/tenant/role/init`（超管）跑。两段都幂等，重复调用返回全 0。
3. **跨模块 API**：system 模块新增 `MenuApi`（按权限查 / 建按钮权限行），`PermissionApi` 加 `addRoleMenus`
   （只追加不覆盖，保留套餐带来的系统菜单），`TenantApi` 加 `addTenantPackageMenuIds`（追加进套餐）。
4. **一致性测试**：`RecyclingPermissionAnnotationConsistencyTest` 反射扫描 `controller/admin` 下的
   `@PreAuthorize`，锁住「注解里的权限 = `RecyclingRoleEnum.allPermissions()`」；新增权限忘了登记会红。
5. **实测**：启动日志「新增权限行 5 个、回收企业套餐补入 5 个」（即 5 个只在注解里、SQL 没种的权限）；
   超管调 `/icbc/tenant/role/init` 首次返回 `createdRoleCount=4 / assignedRoleMenuCount=136`，二次全 0；
   新建租户（套餐 200）的 admin 调权限接口拿到 197 条权限、含全部 icbc 业务权限且无 `icbc:platform:*`；
   租户 admin 调收购/出售者接口 200、调平台计费接口返回 `code:403`。icbc 模块 430 测试全绿。

## #42 T04 库存域换列 + StockApi（已完成）

库存以「品类」为维度而不是 ERP 自己的产品；库存能力通过 `StockApi` 开放给回收业务模块。

1. **换列（12 张表）**：`erp-stock-goods-config.sql`（叠加在 `erp.sql` 之后，幂等）把 `product_id` →
   `goods_config_id`、10 张明细表删 `product_unit_id`；`erp_warehouse` 加 `station_id`；
   `erp_stock` 加唯一约束 `uk_goods_config_warehouse (goods_config_id, warehouse_id)`；删 `erp_product*` 三张表。
   README 导入顺序同步。
2. **ERP 侧**：12 张表的 DO / VO / Service / Mapper 全部改成 `goodsConfigId`；删掉整个 `product` 域
   （controller / service / dal / vo）——ERP 不再知道 icbc 品类，`goodsConfigId` 对它是不透明 id；
   库存不足的报错也不再回产品名（改为回品类编号，如 `品类(100)`）。库存服务 `updateStockCountIncrement`
   捕获 `DuplicateKeyException`，并发下靠唯一约束坍缩到一行余额。
3. **StockApi**：`erp-api` 新增 `StockApi`（`in` / `out` / `getStockCount` / `getStockSum`）与
   `StockChangeReqDTO`；`erp-biz` 的 `StockApiImpl` 按「业务类型 + 业务编号 + 业务项编号」幂等；
   新增业务类型 `RECEIPT_IN(90)` / `RECEIPT_IN_CANCEL(91)`。`icbc-biz` POM 加 `yudao-module-erp-api` 依赖，
   方向恒为 icbc → erp。
4. **测试基座**：`yudao-module-erp-biz/src/test/` 从空补上 `UnitTestConfiguration`（只建 H2 数据源、
   不组件扫描）、`application-unit-test.yaml`、`sql/create_tables.sql` / `clean.sql`（库存域 13 张表）。
   新增 `ErpStockServiceTest`（余额行唯一、库存不足拦截）、`ErpStockRecordServiceTest`（余额 = 全部流水重算）、
   `StockApiImplTest`（入/出/余额、业务项幂等）；icbc 侧 `StockApiDependencyTest` 锁依赖可注入。
   ERP 6 测试 + icbc 431 测试全绿。
5. **实测**：本地库跑完迁移后 12 张表只有 `goods_config_id`、`erp_stock` 有唯一约束、`erp_product*` 0 张；
   重启后租户 1 建仓库（`stationId=7`）能写能读、`/erp/warehouse/page` 与 `/erp/stock/page` 均空列表；
   迁移重复跑幂等。前端 `erp/stock/warehouse` 表单加「归属场站」字段（#43 会换成场站下拉）。

## #43 T05 库位与批次建模与维护界面（已完成）

在仓库之下补库位与批次，库存从「品类 + 仓库」两维扩成「品类 + 仓库 + 库位 + 批次」四维（ADR 0027）。

1. **新表**：`erp_stock_location`（库位，挂仓库）、`erp_stock_batch`（批次，批次号唯一，可空品类 / 入库时间）。迁移 `backend/sql/mysql/erp-stock-location-batch.sql`（幂等，排在 `erp-stock-goods-config.sql` 之后）；README 导入顺序同步。
2. **余额与流水加维度**：`erp_stock` / `erp_stock_record` 各加 `location_id` / `batch_id`，用 `NOT NULL DEFAULT 0`（0 = 未指定）而不是 NULL——NULL 在唯一索引里互不相等，会让「未指定」出现多行余额。唯一约束从 `uk_goods_config_warehouse` 换成 `uk_goods_config_warehouse_location_batch`（AC 明确「并保持唯一」）。
3. **ERP 服务**：`ErpStockService` 新增 4 维的 `getStock` / `getStockCount` / `updateStockCountIncrement`；二维的 `getStock` 仍定位「未指定库位 / 批次」那一行，二维的 `getStockCount` 改成该仓库下全部库位 / 批次之和。`ErpStockLocationService` / `ErpStockBatchService` 提供维护 CRUD；**还有库存余额的库位 / 批次不允许删除**。批次支持 `getOrCreateStockBatch(批次号, 品类, 入库时间)`——收货自动生成时同一批次号复用一条（并发靠 `uk_tenant_batch_no` 兜底）。
4. **StockApi 支持拆库位与可入库上限**：`StockChangeReqDTO` 加 `locationId` / `batchId` / `maxCount`；同一业务项（业务类型 + 业务编号 + 业务项编号）仍只写一次流水，拆到多个库位时用不同的业务项编号；传了 `maxCount` 时按「业务类型 + 业务编号 + 品类」累计校验，超过报 `STOCK_IN_EXCEED_AVAILABLE`（AC「同一品类的货可拆到两个库位，合计不超过可入库量」；#52 的「分多次入库」把收购单传成 `bizId` 即可跨入库单累计）。
5. **菜单 / 权限**：`icbc-menu.sql` 在「仓储管理」（5205，改成目录）下挂 5183 库位维护 / 5187 批次维护 / 5191 库存查询（权限 `erp:stock-location:*`、`erp:stock-batch:*`、`erp:stock:query`）。ERP 权限不走 `RecyclingRoleEnum`，靠回收企业套餐递归带上；`RecyclingPermissionAnnotationConsistencyTest` 只扫 icbc 控制器，不受影响。
6. **前端**：新增 `views/erp/stock/location`、`views/erp/stock/batch` 与对应 `api/erp/stock/*`；`views/erp/stock/stock` 从早已删掉的 `productId` 改成 `goodsConfigId` 并补库位 / 批次筛选（AC4 四个维度都能查）。
7. **测试**：ERP 侧 19 个（新增 `ErpStockLocationServiceTest`、`ErpStockBatchServiceTest`；`ErpStockServiceTest` 补拆库位 / 拆批次；`StockApiImplTest` 补拆库位 + 上限拦截）；`create_tables.sql` / `clean.sql` 同步。icbc 431 测试不受影响。

> 实测：本地库跑迁移后 `erp_stock` 唯一约束是四维、`erp_stock_location` / `erp_stock_batch` 建好，重跑迁移幂等；48081 起服务后建仓库 / 库位 / 批次、分页筛选、同名库位拦截、删除都通过。

## #44 T06 单位供货方档案（已完成）

与自然人出售者并列的第二种交易对方。按 [ADR 0027](docs/adr/0027-采购与仓储的对象模型.md) / [ADR 0029](docs/adr/0029-卖方主体分六类与反向开票准入.md)，单位供货方的档案落在 ERP 供应商表上（不新建 icbc 表，不把自然人复制一份）。

1. **主体类型六态**：新增 `SellerSubjectTypeEnum`（自然人出售者 / 个体工商户 / 个人独资企业 / 合伙企业 / 企业法人 / 农民专业合作社，带 `natural` 判定位）与 `TaxpayerQualificationEnum`（一般纳税人 / 小规模纳税人），都放在 **`yudao-module-erp-api`**——`erp_supplier` 在 ERP 侧，而依赖方向恒为 icbc → erp（icbc 只能看到 `erp-api`）。
2. **字段**：`erp_supplier` 加 `subject_type` / `taxpayer_qualification` / `address`；税号 / 开户行 / 开户账号 / 联系人 / 联系电话沿用上游已有列。迁移 `backend/sql/mysql/erp-supplier.sql`（幂等，排在 `erp.sql` 之后），README 导入顺序同步。
3. **服务层门禁**（门禁只在 `ErpSupplierServiceImpl` 一处）：主体类型必填；**拒收自然人**（`SUPPLIER_SUBJECT_TYPE_NATURAL_NOT_ALLOWED`，指向「出售者档案」），自然人档案在 `icbc_payee_info`，复制进 `erp_supplier` 会造出第二个事实源。
4. **停用而非删除**：`status` 沿用上游；`deleteSupplier` 在 `erp_purchase_order` / `erp_purchase_in` / `erp_purchase_return` 里查引用，有引用报 `SUPPLIER_DELETE_FAIL_REFERENCED` 并提示改为停用。
5. **菜单 / 权限**：`icbc-menu.sql` 在「交易对方」（5202）下挂 5193 单位供货方 + 5194–5197 增删改导出（权限 `erp:supplier:*`，页面 `views/erp/purchase/supplier`）；随套餐递归进回收企业套餐，不进 `RecyclingRoleEnum`（与 #43 的 `erp:stock-*` 同一做法）。
6. **前端**：`api/erp/purchase/supplier` 加主体类型 / 纳税人资格 / 地址与选项常量；列表加主体类型筛选与两列（主体类型 / 纳税人资格）；表单加主体类型（必填）与纳税人资格下拉、地址输入。顺带修掉列表里未使用的 `dateFormatter` 导入与表单 `formData` 的类型报错。
7. **测试**：新增 `ErpSupplierServiceTest` 7 例（六态中的五类可建档、自然人拒收（建 / 改）、主体类型必填、增改查与按主体类型分页、无引用可删、被采购订单引用不可删）；ERP 侧 27 测试全绿，`mvn -pl yudao-server -am -DskipTests install` 通过；`create_tables.sql` / `clean.sql` 同步。

> **AC4「采购单据的对手方位置能选到单位供货方」**：上游 `erp_purchase_order` / `erp_purchase_in` / `erp_purchase_return` 已有 `supplier_id`，表单已用 `SupplierApi.getSupplierSimpleList()` 选供货方，故本票结构上满足；双外键 `counterparty_type + payee_id + supplier_id` 与「恰好一个非空」的 CHECK 约束属 #46（T08）采购订单。

> **AC1 的口径**：六态是枚举本身；单位供货方档案只收其中「自然人以外」的五类——这正是「判定规则是是否属于自然人」的落地，不是把自然人塞进供应商表。

## #45 T07 采购合同（已完成）

采购条款的对象：一个合同 → 多个采购订单 → 多次收货。**与自然人出售者的「框架收购协议」是两件事**——后者是开票前置（开票与代办税费授权），前者是采购条款，不合并。

1. **落点落在 `icbc` 侧**（`icbc_purchase_contract` / `_category` / `_version` 三张租户表）：规格 #38 把采购履约链建在 `icbc` 模块（修订 ADR 0025），因为合同要同时承载自然人出售者（`icbc_payee_info`）与单位供货方（`erp_supplier`）两种对手方，而 `erp` 看不到 `icbc_payee_info`、依赖方向恒为 `icbc → erp`。ADR 0027 已同步加修订注（原写 `erp_purchase_contract`）。迁移 `backend/sql/mysql/icbc-purchase-contract.sql`（幂等），README 导入顺序同步。
2. **对手方用「主体类型六态 + 双可空 id」承载**：`counterparty_type`（复用 `erp-api` 的 `SellerSubjectTypeEnum`）+ `payee_id` / `supplier_id`，恰好一个非空（Service 校验 + MySQL `chk_purchase_contract_counterparty` CHECK 兜底）。`payee_id` / `supplier_id` 加了 `@TableField(updateStrategy = ALWAYS)`——换对手方时要把另一个 id 真正清成 NULL，MyBatis-Plus 默认 NOT_NULL 策略会跳过 null 清不掉；所有更新路径都传整份 DO，故安全。**单位供货方只存 id + 名称快照，不做 ERP 侧存在性校验**（icbc 只有 `erp-api`，`erp_supplier` 没有对外 API；`erp_supplier` 本身没有 `subject_type` 之外的变化，这一条留给 #46 的双外键收口时一起考虑）。
3. **状态机一条主线**：草稿 →（送审）/ 待审核 →（通过）生效 →（关闭）关闭；驳回退回草稿。**每次送审落一版快照**（`icbc_purchase_contract_version`，含适用品类、SHA-256 哈希、审核结论）。**改已生效合同 = 提新版**：新版本 + 回到待审核，变更原因必填，重新审核通过前整份合同不再是有效采购依据。**过期不落库**，由「已生效 + `end_date` 早于今天」推导（与 #13 的「逾期」同一做法）。
4. **唯一门禁 `PurchaseContractService#assertUsableAsPurchaseBasis(contractId)`**：要求已生效且未过期。采购订单（#46）等后续单据调它，不要各自复制判断。
5. **权限 / 菜单**：新增 `icbc:purchase-contract:query|manage|audit`（管理员全量；收货员与财务只读，现场要选「有效采购安排」）；菜单 5220–5226 挂在「采购管理」（5203 由占位页改为目录）。前端 `views/icbc/purchaseContract/index.vue`（列表 / 编辑 / 送审 / 审核 / 关闭 / 明细含版本留痕）与 `api/icbc/purchaseContract`。
6. **测试**：`PurchaseContractServiceTest` 15 例（未审核不得作为依据、送审 → 审核 → 生效、驳回退回、改已生效合同提新版并重新送审、关闭、过期推导、双方恰好一个非空且可切换、有效期、品类必填且属本租户、只删草稿、分页筛选）与 `RecyclingRoleEnumTest#testPurchaseContractPermissions`；icbc 侧 447 测试全绿，前端 `pnpm ts:check` 无新增错误类别（仅全库既有的 auto-import d.ts 缺失噪声）、`pnpm build:local` 通过。

> **未做的**：单位供货方存在性校验（见第 2 条，待 #46 的双外键一起收）；合同列表 `categories` 为逐行查询（与结算单列表同一做法，分页小，未优化）。

## 下一步建议

- **A.** #13 代办税费申报——**已完成**；
- **B.** #15 平台运营：通知监控与重放——**已完成**；#16 计费计量——**已完成**；
- **C.** #20 收货员现场端（uni-app H5）、#19 自然人出售者端。
- **D.** #35 预约到站与 #36 触达（短信三条 + 收货员转达）——**已完成**；#37 换银行卡编码部分已完成，仅剩「收方与实人认证是否按子商户隔离」待工行书面答复（两种答复都不改数据结构）。

## 约定与坑

- **权限**：icbc 控制器统一用 `@ss.hasPermission(RecyclingPermission.X)`（yudao 原生菜单权限，见 ADR 0026）。
  新增权限三步：在 `RecyclingPermission` 登记常量 → 挂到 `RecyclingRoleEnum` 的相应角色 → Controller 上用常量。
  `system_menu` 权限行 / 角色授权 / 回收企业套餐由 `RecyclingPermissionSyncService` 幂等生成，启动时自动补全局部分；
  一致性测试（`RecyclingPermissionAnnotationConsistencyTest`）会把「注解 ≠ 枚举」判为失败，别再写死字符串。
- **工行 UI 页面**：预下单/付款/入驻返回的是自动提交表单 HTML，用 `src/views/icbc/util.ts` 的 `openIcbcForm()` 打新窗口，不能当 URL 跳。
- **新增 icbc 表**：工行返回字段（如 `payee_no`/`payer_no`）在本地库应为可空；表放 `backend/sql/mysql/`，菜单用 `icbc-menu.sql` 幂等维护。**全局表**（无租户隔离语义，如 `icbc_scrap_code`、`icbc_billing_ledger`——后者的 `tenant_id` 是「被计费租户」这个数据列，不是隔离维度）必须登记进 `yudao-server/src/main/resources/application.yaml` 的 `yudao.tenant.ignore-tables`，否则会被拼上 `tenant_id`。租户隔离的单测要打开拦截器（`IcbcTenantTestConfiguration`），它那里同步维护了忽略表清单。单测表结构在 `yudao-module-icbc-biz/src/test/resources/sql/create_tables.sql`。
- **测 icbc**：`mvn -pl yudao-module-icbc/yudao-module-icbc-biz test`；改了 `-api` 先 `mvn -pl ...-api -DskipTests install`。**不要**用 `-am test`（上游模块有既有失败会挡住 reactor）。
- **ERP 已启用（#39）**：`erp-biz` 不在 `.m2` 里时 `mvn -pl yudao-server spring-boot:run` 直接失败，改完 `erp` / `icbc` / 任何模块都要先 `mvn -pl yudao-server -am -DskipTests install`。单独构建子模块必须写全路径：`mvn -pl yudao-module-erp/yudao-module-erp-biz -am`（`-pl yudao-module-erp -am` 只构建父 pom，不进子模块）。详见 [backend/sql/mysql/README.md](../../backend/sql/mysql/README.md#构建与启动erp-已启用后的两个坑39)。
- **ERP 库存维度是品类（#42）**：12 张表只有 `goods_config_id`（不再有 `product_id` / `product_unit_id`），
  ERP 不知道 `icbc_goods_config`，不校验品类存在；回收业务写库存只走 `erp-api` 的 `StockApi`（按业务项幂等），
  不要直接碰 `erp_stock` / `erp_stock_record`。测 ERP：`mvn -pl yudao-module-erp/yudao-module-erp-biz test`，
  测试建的 H2 库需同步改 `yudao-module-erp-biz/src/test/resources/sql/create_tables.sql`。
- **额度台账口径**：只在 `NaturalPersonQuotaServiceImpl` 一处；改口径（如是否算在途）不要散到调用方去。额度是**软上限**——平台自己的台账，跨平台累计不可见。
- **前端**：`pnpm build:local` 验证编译；`pnpm ts:check` 有 1247 个既有 TS 错误，判断自己的改动看 `src/(views|api)/icbc` 有无新报错即可（跑 ts:check 需加 `NODE_OPTIONS=--max-old-space-size=6144`）。
- **时间字段**：yudao 全局 Jackson 把 `LocalDateTime` 按**毫秒时间戳**序列化/反序列化（`TimestampLocalDateTimeSerializer/Deserializer`）。因此 `@RequestBody` 里的 `LocalDateTime` 字段，前端日期选择器必须用 `value-format="x"`，接口类型声明为 `number`；字段上的 `@DateTimeFormat` 对 JSON body **无效**（只作用于 query/form）。不要用 `YYYY-MM-DD HH:mm:ss`，否则反序列化会得到 0 或报错。
- **触达本地联调**：`ICBC_SELLER_APP_URL`（自然人端入口，本地 `http://localhost:5174`）不配，「复制确认链接 / 短信转达」直接报「尚未配置自然人端入口地址（icbc.notify.seller-app-url）」——`SellerNotifyServiceImpl.resolveSellerAppUrl()` 在 `seller-app-url` 为空时退化用 `ICBC_STATION_ENTRY_URL`，两条都空就抛 `SELLER_NOTIFY_LINK_UNAVAILABLE`。
- **短信日志列宽**：yudao 快照里 `system_sms_log`.`template_content` / `template_params` 只有 `varchar(255)`，而带一次性令牌链接的正文约 320–380 字符（链接自身约 260 字符）→ 插日志报 `Data too long for column 'template_content'`，**短信永远发不出去**（转达退化成「复制链接当面给他」，自动触达落一条发送失败记录）。单测把 `SmsSendApi` Mock 掉了，不写这张表，所以只有真库能暴露。`icbc-seller-notify.sql` 已幂等加宽到 `varchar(1024)`；以后再往短信里塞长链接照这个口子走，**别去改快照 `ruoyi-vue-pro.sql`（ADR 0012）**。
- **工具**：不要在同一条消息里同时发 `edit` 和依赖它的 `bash`（会并发，文件可能未落盘）。

## 重启后端时修掉的启动阻塞（2026-09-19）

本地后端自 12:00 起一直没重启，而 #31–#37 都是傍晚之后落的库，`.m2` 里 `yudao-module-icbc-biz` 的 jar 还是 11:58 的旧版本。重启（先 `mvn -pl yudao-module-icbc/yudao-module-icbc-biz -am -DskipTests install`，再 `mvn -pl yudao-server spring-boot:run`）后暴露出三个**只有整机启动才会触发、单测发现不了**的冲突，已修：

1. **`@Service` bean 名冲突**：icbc 的 `AppointmentServiceImpl` 与 `yudao-module-waste` 的同名类都默认叫 `appointmentServiceImpl`，`ConflictingBeanDefinitionException`。给 icbc 的显式起名 `icbcAppointmentServiceImpl`。
2. **Mapper 注入名冲突**：`AppointmentServiceImpl` 里 `@Resource private IcbcAppointmentMapper appointmentMapper` 按字段名先命中 waste 的 `appointmentMapper` bean，类型不符。字段改名 `icbcAppointmentMapper`。
3. **控制器路由冲突**：`tax.SettlementController`（#13 汇算清缴）与 `settlement.SettlementConfirmController`（#33 结算单）都占 `/icbc/settlement/page`，`Ambiguous mapping`。把汇算清缴挪到 `/icbc/settlement-reminder`（对应 `icbc_settlement_reminder`），前端 `api/icbc/tax/index.ts` 同步。

顺带修了一个登录 UX 缺陷：`SellerAuthServiceImpl.inPlatformTenant` 直接 `TenantUtils.execute`，而后者把异常包成裸 `RuntimeException`，导致 `ServiceException`（如「短信发送过于频繁」）被全局异常处理器当成 `500 系统异常`。现拆回原样。前端 `utils/request.ts` 也改成**只有带令牌的请求**收到 401 才清登录态跳登录；登录/取码这类公开请求的 401 原样报错，不再 `reLaunch` 掉页面（这正是「点获取验证码手机号消失、无提示」的直接原因）。

> 提醒：`pages.json` / `manifest.json` 改动不会热更新，加/改页面后要重启对应 `pnpm dev:h5`；后端同理，改完 icbc 模块要先 `install` 再重启，否则 `spring-boot:run` 仍从 `.m2` 读旧 jar。

### 让本地环境真正能登录（2026-09-19 补记）

修完上面三处后，登录仍走不通，原因是**本地库和本地 jar 都落后于源码**（源码在，但没被加载/应用）：

1. **本地库缺 #31–#37 的迁移表**（`icbc_natural_person`、`icbc_station`、`icbc_settlement`、`icbc_appointment`、`icbc_seller_notify`、`icbc_payee_bank_card_change` 等）。按 `backend/sql/mysql/README.md` 的顺序补跑这些 `icbc-*.sql` 即可。
2. **`icbc-natural-person.sql` 在 MySQL 8 上有排序规则 bug**：建表没写 `COLLATE`，MySQL 8 默认 `utf8mb4_0900_ai_ci`，回填时与快照表的 `utf8mb4_unicode_ci` 关联报 `Illegal mix of collations`。已给两张表补 `COLLATE=utf8mb4_unicode_ci`，并加幂等 `CONVERT`（修老库）。
3. **`.m2` 里除 icbc 以外的模块 jar 偏旧**：`MemberUserApi.createUserIfAbsent`（member-biz）、`TenantApi.getTenantName`（system-biz）等新方法在接口里、不在旧实现 jar 里 → 运行期 `AbstractMethodError`。只 `-am` 装 icbc 模块不够，要：`mvn -pl yudao-server -am -DskipTests install`（整棵依赖树）。
4. **token 租户跟着登录请求头走**：`sms-login` 是 ignore-url，签发 token 时用当前租户上下文。若扫码进了场站（带 `tenant-id`），token 租户 = 场站租户，后续 `/app-api` 请求头一致就不会被越权校验拦。**curl 手测登录务必也带 `tenant-id`**，否则签出平台租户 token，再带场站租户访问即 403「您无权访问该租户的数据」。这也是框架 `TenantSecurityWebFilter` 的硬约束：越权校验在 ignore-url 判断**之前**且不可绕过。

登录测试数据（本地）：场站码 `STATION_TEST`（租户 1）、出售者手机号 `13800139999`（演示出售者，已有一张待确认结算单）、验证码固定 `9999`（`application.yaml` 的 `sms-code.begin/end-code`）。入口：`http://localhost:5174/#/?station=STATION_TEST`。

### 菜单 / 种子数据中文乱码：用 latin1 客户端导过 SQL（2026-09-20 修）

管理后台里「反向开票」「租户开票就绪」「平台运营」整片菜单显示成 `åå‘å¼€ç¥¨` 这种。库里存的确实是双重编码（`hex(name)` = `C3A5C28F…`，而不是 `E58F8DE59091…`）：

1. **原因**：导入时客户端连接字符集不是 utf8mb4。典型是 `docker exec -i youfeibao-mysql mysql`——容器里没有 `LANG`，mysql 客户端的默认字符集是 **latin1**，文件里的 UTF-8 字节被当作 latin1 收下再转成 utf8mb4 落库。快照 `ruoyi-vue-pro.sql` / `quartz.sql` / `member-2024-01-18.sql` 因为自带 `SET NAMES utf8mb4;` 一直没事，手写的 icbc / enterprise 文件没这行，才中招。
2. **已脏的范围（本地库）**：`system_menu.name` 88 行（5008–5012 工行反向开票、5100–5182 全部 icbc 菜单）、`infra_job.name` 3 行、`system_sms_template` 3 条的 name/content/remark、`system_sms_log.template_content` 2 行。**已修**，别的地方（含 `system_menu.permission` 等 ASCII 列）没有脏。
3. **修法**：`backend/sql/mysql/repair-mojibake.sql`——扫全库只改能证明是双重编码的值（英文、正常中文、`é`/`·` 这类 Latin-1 字符都不动），幂等，输出「表 / 列 / 修复行数」。用法见 `backend/sql/mysql/README.md` 的「排查：菜单 / 种子数据中文乱码」。
4. **防复发**：`backend/sql/mysql/` 下 30 个手写 SQL 文件已统一在开头加 `SET NAMES utf8mb4;`，导入不再依赖客户端参数；README 的导入命令也带上 `--default-character-set=utf8mb4` 并写明 `docker exec` 的坑。

> 教训：这类损坏是**静默**的，只表现为界面乱码，跑 SQL 时一句报错都没有。新增带中文种子数据的 `*.sql` 时，记得同样把 `SET NAMES utf8mb4;` 写在第一行。

> 改完种子数据别急着说「没生效」：**菜单**缓在前端 localStorage 的 `roleRouters`（重登才刷新），**字典 / 短信模板**缓在 Redis（`sms_template:<code>` 等，`redis-cli --scan` 删对应键）。本次修完后短信正文仍是乱码就是这个原因，删 `sms_template:icbc_seller_notify_*` 后正常。

## 非自然人卖方的取票口径（2026-09-20 查实）

决策见 `docs/adr/0029`，核查过程见 `docs/research/2026-09-20-非自然人卖方的取票口径.md`。要点：

- **卖方主体分六类**，反向开票的准入是硬约束「主体 = 自然人」。依据：5 号公告第一条定义出售者为自然人；《增值税法实施条例》第三条「个人包括个体工商户和自然人」。
- **高频混淆**：个体户**不能**被反向开票，但个体户**能**当回收企业去反向开票（5 号公告第二条原文「包括单位和个体工商户」）。前者是卖方准入，后者是租户资格，别混。
- **两个高优先待验证**（都要问主管税务机关）：个体工商户作为出售者的执行口径（各省 12366 可能不一致）；自然人在报废产品交易中能否取得代开专票。
- ⚠️ **要盯的时间点**：财政部 税务总局公告 2026 年第 28 号《境内单位代扣代缴自然人增值税管理办法》，**2026-11-01 起施行**，正文 PDF 被 WAF 拦截未取得。若它把回收企业设为向自然人付款的扣缴义务人，会同时动**付款链路（工行公对私直付）与代办税费**。生效前必须拿到正文核对。
- 前序研究引用的《增值税暂行条例》**已随《增值税法》2026-01-01 施行而废止**，涉及税法的表述要用增值税法 + 实施条例。

## 回收企业经营作业系统：规格与票据（2026-09-20）

- **规格**：[#38](https://github.com/tuyaweilai/youfeibao/issues/38)（`ready-for-agent`），56 条 user story、12 条实现决策、测试决策、out of scope、further notes。
- **19 张票**：`#39`–`#57`，边用 GitHub 原生 issue dependencies 连（21 条）。
- **frontier：无。`#39`–`#57` 十九张全部完成并合并进 `main`**（第一轮到第五轮，五轮并行）。规格 #38 的 user story 全部落地。
- **跟进票 `#58` 也已接完**（提交 `dbf7ea1`）：关联采购订单的收购单现在会落成交记录（验收 / 结算分列）并走交货门禁。
- **两处收口已做**（提交 `a5b1d06`）：① #54 期初加 `active_key` + 唯一索引，堆住并发导入；② 接收结论变更时会同步修正订单成交（拒收后「验收」跟着降），#58 原先的已知限制已消。
- **剩两处只需业务确认（无代码要改）**：#55 的「节点级上下游」目前由四栅顺序隐式表达（显式字段属可选增强）；#57 的「超入库量 / 重复关联」判定口径是我按最小可核验解释写的（超入库量 = 累计已过账入库 > 可入库实物量；重复关联 = 同一收购单多张未作废付款单），需业务拍定。

### 第五轮并行约定（#54 / #55 / #57）—— 已完成并合并（最后一轮）

| 票 | 分支 | 菜单 ID 段 | 错误码段 |
|---|---|---|---|
| #54 T16 非销售出库、调拨与盘点调整 | `t16-stock-ops` | 5259–5299 | `1_030_037_xxx` |
| #55 T17 关联单据查询 | `t17-trace` | 5300–5319 | `1_030_038_xxx` |
| #57 T19 经营报表与异常表 | `t19-reports` | 5322–5399 | `1_030_039_xxx` |

菜单清理区间 5100–5399，上述段均已避开已用的 5255–5258 / 5320–5321。脊柱文件规则同前四轮。三票的分工：

- **`#54` 拥有库存的写入**：非销售出库 / 跨仓调拨 / 盘点调整 / 期初。库存只经 ERP 的 stock 域写，**不要直接改 `erp_stock` / `erp_stock_record`**；`StockApi` 现有 `in` / `out` / `getStockCount` / `getStockSum`（带 `maxCount`）不够时，**扩 `erp-api` 的 `StockApi` + `erp-biz` 实现**（依赖方向恒为 icbc → erp），业务类型枚举追加。
- **`#55` 拥有「一批货经历了什么」的单链路追溯**：只读聚合（采购订单—现场收货—仓储入库—结算确认四栏 + 付款 / 发票），按单号 / 车牌 / 主体反查；脱敏复用 `MaskUtils`，导出同样受限。
- **`#57` 拥有经营报表与异常表**：采购履约 / 收购台账 / 库存 / 结算付款四张表 + 一张异常表（磅差 / 超采购量 / 超入库量 / 重复关联 / 长期未确认 / 资料缺失）。#53 的 `weight_diff` 清单与 #47 的 `icbc_purchase_exception` 直接消费，不重算口径。
- **`#55` 与 `#57` 都只读、都跨域**：各把自己的聚合放在自己的包（`service/trace` vs `service/report`），**不要互相 `import`**；“异常”的判定口径以 #57 为准（#55 只展示链路自身的状态）。
- **`#57` 的异常表是派生清单，不新建表**（与额度台账 ADR 0014 同一做法）。预约约量与采购计划量不得混入实际收购量。

### 第四轮并行约定（#52 / #53）—— 已完成并合并

| 票 | 分支 | 菜单 ID 段 | 错误码段 |
|---|---|---|---|
| #52 T14 待入库 → 入库单 → 库存流水 | `t14-stock-in` | 5255–5319 | `1_030_035_xxx` |
| #53 T15 拒收 / 部分接收与余货出场 | `t15-partial-receipt` | 5320–5379 | `1_030_036_xxx` |

菜单 ID 清理区间已从 5100–5299 扩到 **5100–5399**（5100 段不够用了；5300–5399 在种子里为空，无碰撞）。脊柱文件规则同前三轮。额外约定（避免又出现跨票缺口）：

- **`#53` 拥有收购单的「接收结论」字段**：`accepted_weight`（接收量）、`rejected_weight`（退回量）、`residual_weight`（余货出场）与拒收原因；拒收部分不进应付、不进库存。称量差异（结算重量 vs 入库重量）落字段 + 只读差异清单，#57 的异常表直接消费。
- **`#52` 拥有入库单与库存**：`icbc_stock_in` + 明细 + 走 #43 的 `StockApi`（业务类型 | `RECEIPT_IN(90)` / `RECEIPT_IN_CANCEL(91)`），不要直接碰 `erp_stock*`。
- **可入库实物量只有一个取数点**：`#52` 把它抽成一个方法（先取 `net_weight`，实物口径）；`#53` 的 `accepted_weight` 落地后由人工改该方法优先取 `accepted_weight`（有值优先）。两票**不要**互相 `import` 对方新增的类。
- **`#52` 落地后还要接一处**：`#56` 工作台的 `WorkbenchTodoCodeEnum.PENDING_STOCK_IN` 现在标了「待接入」，把 `unavailableReason` 清掉并在 `WorkbenchServiceImpl` 加一个分支（人工接线）。

合并与接线结果：合并顺序 #52 → #53（每个合完跑一次 icbc 测试，最终 590 全绿）。**三处跨票接线已一次做完**（提交 `23c455b`）：

1. **可入库实物量改取实物口径**：`StockInServiceImpl#resolveAvailableQuantity` 改调 `IcbcAcquisitionDO#resolvePhysicalWeight()`（接收量优先，无则净重）——拒收 / 退回 / 余货出场的部分不进库存。
2. **工作台待入库接入**：`PENDING_STOCK_IN` 清掉「待接入」，`WorkbenchServiceImpl#pendingStockIn` 按「已验收且未入库」取数。
3. **采购履约「入库」口径接入**：新增 `StockInService#getStockedQuantityByOrderItems`（只计已过账，入库单→收购单→订单明细），`getProgress` 汇总后填 `STOCKED_IN`，五个口径全部可算。

踩到的坑：这些服务在测试里被 `@ComponentScan` 扫到，给 `PurchaseOrderServiceImpl` / `WorkbenchServiceImpl` 加了 `@Resource StockInService` 后，会连带实例化 `StockInServiceImpl` → 需要 `StockApi`；受影响上下文的测试类各补了一个 `@MockBean StockApi`（真实 `StockInServiceImpl` 对空库返回空结果）。

### 第三轮并行约定（#47 / #51）

| 票 | 分支 | 菜单 ID 段 | 错误码段 |
|---|---|---|---|
| #47 T09 履约五口径与执行进度 | `t09-order-progress` | 5250–5269 | `1_030_033_xxx` |
| #51 T13 收购单关联采购安排与「直接收购」 | `t13-acquisition-link` | 5270–5289 | `1_030_034_xxx` |

脊柱文件规则同前两轮（各票只追加、不重排；权限枚举、测试建表 / `clean.sql` / `README.md` / `handoff.md` 均只追自己的部分）。额外约定：

- **`#47` 拥有 `PurchaseOrderService` 与 `PurchaseOrderProgressRespVO` 的进度口径**；`#51` 只调用 #46 已有的 `assertUsableAsPurchaseBasis` / 新增一个「可选采购安排查询」读取方法，不改 #47 的进度 VO。
- **`#47` 的「入库」口径暂不可算**（入库单是 #52）：按 #56 的做法标 `unavailableReason` / 「待接入」，不要硬凑数字；`#52` 落地后再接。
- **`#51` 会给 `icbc_acquisition` 加「采购订单 / 订单明细」关联列**（与 #50 的 `handover_batch_id` 同表），采用 `NOT NULL DEFAULT 0`，别用 NULL（同一票的教训见 ADR 0027/唯一索引）。
- **落库时不要用 `git add -A`**：main 工作树里有一批与本轮无关的现场端 WIP，`-A` 会把它卷进提交（第二轮已踩过）。只 `git add` 自己改的文件。

### 并行开工约定（2026-09-20，已完成一轮）

`#56`/`#45`/`#48`/`#50` 四张已用 `git worktree` 并行跑完并合入 `main`（合并顺序 #45 → #48 → #50 → #56，每合一个跑一次 icbc 测试，最终 491 测试全绿）。当时的分工与菜单 ID 段：

| 票 | 分支 | 菜单 ID 段 | 主要抢的文件 |
|---|---|---|---|
| #56 T18 工作台 | `t18-workbench` | 5210–5219 | `RecyclingPermission`/`RecyclingRoleEnum`、`icbc-menu.sql`、`handoff.md` |
| #45 T07 采购合同 | `t07-purchase-contract` | 5220–5229 | `icbc-menu.sql`、`README.md`、`handoff.md` |
| #48 T10 卖方主体准入 | `t10-seller-admission` | 5230–5239（本票未用） | `RecyclingPermission`/`RecyclingRoleEnum`、`create_tables.sql`/`clean.sql`、`handoff.md` |
| #50 T12 交接批次与有效磅次 | `t12-handover-batch` | 5240–5249 | 上述全部 + `icbc-menu.sql`、`README.md` |

合并时实际撞到的「脊柱文件」与解法（下一轮并行照做）：

- **错误码段**：`ErrorCodeConstants` 两边都从 `1_030_028_000` 起，必撞。**开票前先定段**：本轮定为 #45 → 028、#48 → 029、#50 → 030。
- **`icbc-menu.sql`**：ID 分段后不撞号，自动合并；但仍会删 5100–5299 重建，**谁跑谁清掉别人的段**，合并后重跑一次并重登刷新菜单缓存。
- **`RecyclingPermission` / `RecyclingRoleEnum`**：只追加、不重排，一致性测试会锁死「注解 = 枚举」，同一次提交改齐。
- **`icbc_acquisition`（DO / VO / Service / 测试建表）**：#48（六态）与 #50（磅次）都改，是冲突最密的地方；各票只追加自己的列 / 分支。
- **`create_tables.sql` / `clean.sql` / `README.md` / `handoff.md`**：追加式，合并时按段收口即可。
- **`.m2` 共享**：四个 worktree 共用 `~/.m2`，`-am install` 会互相覆盖 SNAPSHOT。改 `-api` 又要同轮测 `-biz` 时，用一条 reactor 命令：`mvn -pl yudao-module-icbc/yudao-module-icbc-api,yudao-module-icbc/yudao-module-icbc-biz test`。
- **工作目录**：一票一目录一分支（`../youfeibao-t07` 等），**不要在同一目录多开窗口**。

### 第二轮并行约定（#46 / #49）—— 已完成并合并

| 票 | 分支 | 菜单 ID 段 | 错误码段 |
|---|---|---|---|
| #46 T08 采购订单 | `t08-purchase-order` | 5227–5239（用了 5227–5236） | `1_030_031_xxx` |
| #49 T11 进项收票登记与勾稽 | `t11-input-invoice` | 5244–5259（用了 5244–5249） | `1_030_032_xxx` |

合并顺序 #46 → #49（每个合完跑一次 icbc 测试，最终 533 全绿）。实际撞点与解法：

- **`ErrorCodeConstants` 又撞**（插入点相同）：两票段不重叠，去标记保两边即可（本轮无重号，比第一轮轻）。
- **两票不要互相编译依赖**（已按约定做到）：`#46` 在 `PurchaseOrderService` 上留 `getOrderAmount(id) → PurchaseOrderAmountDTO`（单号 + 单据金额 + 是否可作采购依据）；`#49` 的勾稽用通用关联表 `icbc_input_invoice_link(biz_type, biz_id, biz_no, biz_amount, linked_amount)`，`InputInvoiceBizTypeEnum` 含 `ACQUISITION` / `PURCHASE_ORDER`（已接）/ `STOCK_IN`（预留 #52）。
- **合并后接线已完成**（提交 `2168273`）：`InputInvoiceServiceImpl` 在 `bizType=PURCHASE_ORDER` 时经 `getOrderAmount` 取单号与单据金额、忽略客户端传值，金额上限口径只有一处；测试补了 `stubOrder` 与「客户端夸大金额也无效」一例。
- **其余脊柱文件**（`RecyclingPermission`/`RoleEnum`、`create_tables.sql` / `clean.sql` / `README.md` / `handoff.md`）：自动合或追加式手工合，与第一轮相同。
- **合并时踩到的坑**：解 `IcbcTenantIsolationTest` 的字段冲突时漏了一个 `@Resource`，表现为注入为 null（`Cannot invoke ... because this.inputInvoiceMapper is null`）。追加字段时「注解也要跟着复制一份」。

| 票 | 标题 | blocked by |
|---|---|---|
| #39 | T01 放开 ERP 并让本地能起 | — |
| #40 | T02 权限收口到菜单与角色-菜单 | — |
| #41 | T03 菜单清场与租户套餐骨架 | — |
| #42 | T04 库存域换列 + StockApi | #39 |
| #43 | T05 库位与批次建模与维护界面 | #42 |
| #44 | T06 单位供货方档案 | #39 #40 |
| #45 | T07 采购合同 | #41 #44 |
| #46 | T08 采购订单 | #45 |
| #47 | T09 履约五口径与执行进度 | #46 |
| #48 | T10 卖方主体准入硬约束与非自然人取票路径 | #44 |
| #49 | T11 进项收票登记与勾稽 | #48 |
| #50 | T12 交接批次与有效磅次 | #39 |
| #51 | T13 收购单关联采购安排与「直接收购」 | #46 #50 |
| #52 | T14 待入库 → 入库单 → 库存流水 | #43 #51 |
| #53 | T15 拒收 / 部分接收与余货出场 | #51 |
| #54 | T16 非销售出库、调拨与盘点调整 | #52 |
| #55 | T17 关联单据查询 | #52 |
| #56 | T18 工作台待办与开票就绪徽标 | #41 |
| #57 | T19 经营报表与异常表 | #47 #52 |

**两条落地时要记得改的东西**：

1. **ADR 0025 的域取舍在规格里被修订**：`purchase` 域不启用（采购履约链建在 `icbc` 模块，因为采购订单必须与收购单在同一模块内做关联追溯；`erp` 侧反向依赖 icbc 是错的）。实际只启用 `stock` 域。**#39 已把 ADR 0025 同步改掉**。
2. **`yudao-module-erp-biz/src/test/` 是空的**，没有 `create_tables.sql` / `clean.sql`。凡涉及 ERP 库存能力的测试，先把这两份 H2 资源建起来（`#42` 起需要）。

## #48 T10 卖方主体准入硬约束与非自然人取票路径（已完成）

把「反向开票只对自然人」从操作员判断变成系统级硬约束（ADR 0029），非自然人卖方一律拦下并指向进项收票路径。

1. **准入只有一条规则、一个家**：新增 `SellerAdmissionService`（`service/admission`）。判定唯一依据是
   `SellerSubjectTypeEnum.isNaturalType()`（主体是不是自然人，不是「有没有营业执照」）。非自然人抛
   `SELLER_SUBJECT_TYPE_NOT_NATURAL`（`1_030_029_000`，合并 #45 后让出 028 段），错误信息给出主体名 + 「由对方自行开具增值税发票，
   并在「进项收票」登记与勾稽」。`null` 按自然人放行（历史单据没有该字段，否则老单开不出票）；未知类型拒绝。
2. **采购单据留六态快照**：`icbc_acquisition.seller_subject_type`（`icbc-acquisition.sql` 幂等 ALTER，默认 1），
   进 `AcquisitionRespVO`（含 `sellerSubjectTypeName`），PC 收购单列表与现场端收购详情可见。收购单只收自然人：
   登记时传非自然人直接拒（错误同上），未传默认自然人。
3. **反向开票通道三处一起拦**：`InvoiceApplicationServiceImpl` 新增前置校验项 `SELLER_SUBJECT_TYPE`
   （消息 + 补齐方式都指向由对方开票）；`InvoicePreOrderReqVO` 带 `sellerSubjectType`（平台侧字段、不上送工行），
   `InvoiceOrderServiceImpl.createPreOrder` 在下发工行前再硬校验一次——预下单是真正的闸门，单靠 UI 前置校验不算硬约束。
4. **AC4「两件事别混」在结构上分开**：卖方准入是 `SellerAdmissionService`（只看卖方主体），租户资格是
   `IcbcQualificationService.isTenantReady()`（三层资质），两层互不推断。个体户**能**作为回收企业去反向开票
   （5 号公告第二条），那是租户维度，不在本门禁内。
5. **测试**：`SellerAdmissionServiceTest`（六态逐一：自然人放行、其余五类拒且消息指向进项收票、null 兼容历史、
   未知类型拒）；`InvoiceApplicationServiceTest` 补六态前置校验、非自然人不下发工行、卖方准入与租户资格两条线
   独立；`InvoiceOrderServiceTest` 补预下单闸门对五类非自然人拒、自然人放行；`AcquisitionServiceImplTest` 补
   自然人快照与非自然人拒收。icbc 442 测试全绿；PC `pnpm build:local`、现场端 `pnpm build:h5` 均通过。

> **口径留档**：本票把「采购单据」落在 `icbc_acquisition`（收购单）上。#46（T08 采购订单）会给
> `erp_purchase_order` 加 `counterparty_type + payee_id + supplier_id` 双外键，那时单位供货方的采购单据
> 才真正承载非自然人主体类型；本票的 `SellerAdmissionService` 已按「主体是否自然人」做成通用门禁，可直接复用。

> **未做（不属本票）**：进项收票登记与勾稽是 #49（T11）；`createPreOrder` 直接调用时若 `sellerSubjectType`
> 为空按自然人放行（兼容历史），收购单/开票申请这条主路径恒会带上该字段。

## #50 T12 交接批次与有效磅次（已完成）

把「这批货经历了什么」和「哪一次过磅算数」落成两个对象（CONTEXT 的「交接批次」「有效磅次」）：
一个交易对方的一次**物理交接**记为一个交接批次；过磅保留每一次原始读数（磅次），
**只有被选定的那一次参与计量**，其余留档不参与。现场端为主，PC 可查改补录。

1. **两张新表（租户表，不进 ignore-tables）**：
   - `icbc_handover_batch`：批次号、交易对方（`payee_id` + 姓名 / 手机号快照）、`station_id` / `station_name` 或 `visit_address`（二者至少一个）、`occur_time`、`source_type`、司机与手机号、车牌；`appointment_id` / `purchase_order_id` **都可空**。
   - `icbc_weighing`：`batch_id` + `seq_no`（唯一）、毛重 / 皮重 / 净重、过磅时间、磅单号与照片、磅单上车牌、`effective`、备注。
     `effective` 至多一条为 true：新增第一次磅次时自动置有效；改由 `POST /icbc/handover-batch/weighing/effective` 显式指定，指定时一条 UPDATE 先把其余置 false（并发下不会出现两个「有效磅次」）。
2. **收购单引用有效磅次的值与版本**：`icbc_acquisition` 加 `handover_batch_id` / `weighing_id` / `weighing_seq_no`。收购登记带 `handoverBatchId` 时，**毛重 / 皮重 / 净重 / 磅单号一律取自该批次的有效磅次**（请求里手填的值被覆盖，这才叫「只有那一次参与计量」）；批次没有有效磅次时直接报 `WEIGHING_EFFECTIVE_NOT_SELECTED`，不猜、不退回手填值；出售者与批次交易对方不一致报 `ACQUISITION_BATCH_PAYEE_MISMATCH`。反过来，按有效磅次计量的收购单**不能手工改重量**（`WEIGHING_LOCKED_FOR_ACQUISITION`），与重量无关的补录照旧。
3. **两条刻意的不变量**：**不按「车牌 + 日期」去重**（同一车同一天两次送货就是两个批次，磅单与收购单各归各）；**预约与采购订单都不是建批次的必要条件**（临时上门的散户不被流程挡住）。
4. **有效磅次锁定**：该批次已产生**未作废**的收购单后不允许再改有效磅次（计量结果引用的是当时那一版）；把收购单作废后重新可指定——单据仍保留、作废原因对自然人可见（#33 的作废动作）。批次响应带 `weighingChangeLocked` 供界面置灰。
5. **权限 / 菜单**：新增 `icbc:handover-batch:query|manage`，登记进 `RecyclingPermission` + `RecyclingRoleEnum`（管理员 / 收货员可管理，开票员 / 财务只读）；菜单 5240–5243 挂在「回收作业」（5204）下。
6. **现场端（AC5）**：新增 `pages/handover/index.vue`（首页菜单「交接批次」）：带出售者档案 → 登记批次（场站或上门地址、来源方式、车牌、司机）→ 多次磅次 → 指定有效磅次 → 「按此批次登记收购」跳到 `pages/acquisition/index?handoverBatchId=`（该页带出批次的车牌 / 司机 / 场站与有效磅次重量，提交时把批次挂上）。PC 新增 `views/icbc/handoverBatch/index.vue`（查、补录、看磅次、指定有效）。
7. **落地**：迁移 `backend/sql/mysql/icbc-handover-batch.sql`（幂等；两张新表 + 收购单三个列 + 索引），已进 README 导入顺序；菜单只追加 `icbc-menu.sql`；测试建表与 `clean.sql` 同步。**测试**：`HandoverBatchServiceTest`（11 例）+ `AcquisitionServiceImplTest` 新增 6 例（按有效磅次计量并覆盖手填值、无有效磅次拒结、交易对方不一致拒结、一次混装拆成多张收购单共用同一次磅次、同车同日两批不串、按磅次计量后锁定手工改重量）+ `IcbcTenantIsolationTest` 新增两表租户隔离 + `RecyclingRoleEnumTest` 权限；icbc 450 测试全绿；现场端 `pnpm ts:check` / `pnpm build:h5`、PC `pnpm build:local` 均通过。

> **与 #51 的分工**：本票只负责「批次 → 磅次 → 有效磅次 → 收购单引用」，采购订单关联（`purchaseOrderId` 只落字段、不做门禁与「直接收购」口径）留给 #51；批次与结算单还没有直接外键，结算仍是「出售者 + 场站」聚合（#33 口径不变）。

> **踩到的坑**：`.m2` 是各 worktree 共享的，并行票的 `-am install` 会把你刚装的 `yudao-module-icbc-api` 覆盖回旧版，表现为「刚才编译过、现在找不到符号」。稳妥做法是把 api 与 biz 放进同一次 reactor：`mvn -o -pl yudao-module-icbc/yudao-module-icbc-api,yudao-module-icbc/yudao-module-icbc-biz test`。

## #56 T18 工作台待办与开票就绪徽标（已完成）

工作台（菜单 5200）从占位页变成一屏：八类待办 + 三条预警 + 开票就绪徽标，每项带口径与来源明细。

1. **后端只读聚合**：`GET /icbc/workbench/overview`（权限 `icbc:workbench:query`），
   `WorkbenchServiceImpl`。`WorkbenchTodoCodeEnum`（`icbc-api`）是待办项的**唯一来源**（编码 / 名称 /
   口径 `definition`），前端按 `code` 决定下钻到哪个模块——所以后端不返回前端路由，层不混。
2. **八类待办与取数（都在本票内落地，只读，不新增表）**：
   - 今日到场 / 上门 ← `icbc_appointment` 待到站且预计到站 ≤ 今日（含逾期未处理）；
   - 待称重 ← `icbc_acquisition` 未作废且 `net_weight` 为空（登记要件允许重量留空）；
   - 待验收 ← 未作废、已录磅重、`settlement_id` 为空（现场还没「结束本次收货」）；
   - 待入库 ← **显式标注「待接入」**（见下第 3 条）；
   - 待结算确认 / 异议 ← `icbc_settlement` 处于待确认 / 有异议；
   - 付款失败 ← `icbc_payment_order` 异常态（复用 `PaymentStatusEnum.exceptionStatuses()`）；
   - 票务失败 ← `icbc_invoice_order` 预开票失败 / 开票失败 / 缴税异常 / 上传失败任一（同一张票只算一条；
     红冲是另一张单 `icbc_red_invoice` 的状态线，不在本项口径内，已在枚举的 `definition` 里写明）。
   每项附最多 10 条来源明细（单号 / 谁 / 说明 / 状态 / 时间 / 金额），总数照实报。
3. **「待入库」判 `available=false` 而不是硬凑一个数字**：入库单是 T14（#52）才有的东西，
   批次与验收结论是 T12/T15（#50/#53）。在它们落地前，「已入库」与「待入库」区分不开，硬算会给出
   一个**用户无法清零**的待办（比如「待入库 37」却没有任何入库动作）。所以它出现在八项里、写明原因
   （`unavailableReason`），界面上显示「待接入」+「—」而不是数字。**#52 落地后**：把
   `WorkbenchTodoCodeEnum.PENDING_STOCK_IN` 的 `unavailableReason` 清空、在 `WorkbenchServiceImpl.loadTodo`
   的 `default` 前加一个 `case PENDING_STOCK_IN` 分支即可，其余不用动。
   > 待称重 / 待验收同样会在 T12 之后改成按交接批次取数（现在按收购单的现有事实取，口径写在枚举里）。
4. **三条预警**：额度（未办结的 `icbc_seller_quota_guidance`）、资质到期（待处理的
   `icbc_expiry_warning`）、开票就绪（未就绪时 DANGER，就绪时 OK）。就绪检查只算**本地库能判定**的四项
   （三层资质 / 企业授权 / 付方档案 / 启用品类），**不打工行网络**——连通性仍在「基础资料 - 开票就绪自检」页按需验。
5. **AC4「开票就绪不再是一级菜单」**：#41 已把「租户开票就绪」一级菜单取消、自检页挂在「基础资料」下
   （菜单 5117，是配置入口）。本票把**就绪状态**做成工作台顶部徽标，点开是同一份检查表 + 逐项「去处理」。
6. **落地物**：`WorkbenchController` / `WorkbenchService(+Impl)` / 6 个 VO / `WorkbenchTodoCodeEnum` /
   `RecyclingPermission.WORKBENCH_QUERY`（挂到管理员 / 收货员 / 开票员 / 财务）+ `RecyclingRoleEnum`；
   `icbc-menu.sql` 追加 **5210 工作台待办查询**（在 5200 下，随套餐递归进回收企业套餐）；
   9 个 Mapper 各追加工作台专用的 `selectCount*/selectList*`（不改既有方法）；`PaymentStatusEnum` /
   `TaxStatusEnum` / `UploadStatusEnum` / `PreInvoiceStatusEnum` 各追加 `exceptionStatuses()`
   （异常口径仍只有一处，`isException` 也读同一个集合，`contains(null)` 不会 NPE）。
   前端重写 `views/icbc/workbench/index.vue` + 新增 `api/icbc/workbench`（明细抽屉 + 就绪弹窗）。
   **没有新表**，所以 `create_tables.sql` / `clean.sql` 未动。
7. **测试**：`WorkbenchServiceTest` 13 例（八项固定顺序且都带口径、今日到场含逾期且不含未来 / 已到场、
   待称重与待验收互斥、结算按确认状态分列、付款异常态、票务四条线且同票只算一条、额度与资质预警只算未办结 /
   待处理、就绪由未就绪翻到就绪、明细上限 10 但总数照实）。**icbc 444 测试全绿**。
8. **并行开工的坑（新发现）**：四个 worktree 共用 `~/.m2`。我在 `-pl ...-api install` 之后跑
   `-pl ...-biz test`，中途被别的 worktree 的 `install` 覆盖了 `yudao-module-icbc-api` 的 SNAPSHOT jar，
   于是 biz 编译报「找不到 WorkbenchTodoCodeEnum / WORKBENCH_QUERY」。**改 api 又要在同一轮里测 biz 时，
   用一条 reactor 命令**：`mvn -pl yudao-module-icbc/yudao-module-icbc-api,yudao-module-icbc/yudao-module-icbc-biz test`
   （两个模块同在一个 session，api 走 reactor 的 `target/classes`，不读 `.m2`）。
9. **本地验收**：`icbc-menu.sql` 已在一个临时库（`t18_menu_check`，用完即删）整份跑通并通过：5210 落 5200 下、
   套餐菜单 93 个含它。**没有动共享本地库**——四个 worktree 会各自重跑这份文件（它删 5100–5299 再重建），
   谁跑谁把别人刚加的段删掉，所以本票只在需要时跑，并且跑完记得重新登录刷新菜单缓存（前端 `roleRouters`）。
10. **前端**：`pnpm install --prefer-offline`（6.8s）+ `pnpm build:local` 通过。

## #46 T08 采购订单（已完成）

采购执行依据：向谁买哪些品类、多少量、什么价、在哪段时间、哪个场站，一单多条品类明细、
一条明细可分多次收货。它不是交易对方下的单（那是到站预约），零散收购可以不挂订单。分支
`t08-purchase-order`，菜单段 5227–5239，错误码段 `1_030_031_xxx`。

1. **四张租户表**（`backend/sql/mysql/icbc-purchase-order.sql`，幂等；测试建表与 `clean.sql` 同步）：
   - `icbc_purchase_order`：订单主体。对手方沿用 ADR 0029 的「主体类型六态 + `payee_id` / `supplier_id`
     双可空 id」，`chk_purchase_order_counterparty` + Service 双重保证恰好一个非空；可空关联
     `contract_id`（快照 `contract_no`）与 `station_id`（快照 `station_name`）；状态、计划量 / 金额快照、
     暂停与关闭留痕。`contract_id` / `station_id` / `payee_id` / `supplier_id` / `suspend_reason` /
     `suspended_time` 都加 `@TableField(updateStrategy = ALWAYS)`（换对手方 / 恢复时要把 null 真正写回）。
   - `icbc_purchase_order_item`：一条明细一个品类，落名称 / 单位快照；`price_mode`（1 固定单价 /
     2 按交货日价格表）+ `unit_price`（固定价或兜底价）+ 计划量 / 金额。
   - `icbc_purchase_order_price`：交货日价格表。取值语义是「交货日不晚于当日的最新一条」，
     没覆盖到回退明细参考单价；改价整组重建。
   - `icbc_purchase_order_deal`：**每次成交留价格快照与调整原因**，只追加；同一明细可多条，
     已收量由这些记录的数量汇总推导（不落冗余字段）。带 `source_type/source_id/source_no` 供 #51
     把收购单挂回来。
2. **状态机**：草稿 →（开始执行）执行中 ⇄（暂停 / 恢复）→ 完成 →（关闭）关闭；关闭是终态。
   暂停必填原因，恢复清空暂停痕迹。**只有「执行中」且未过期可作为采购依据**——
   `PurchaseOrderService#assertUsableAsPurchaseBasis(id)` 是唯一门禁，收购登记（#51）选采购安排时调用它。
   超期不落库，由 `end_date` 推导。
3. **定价**：固定单价直接用明细单价；按交货日价格表取「不晚于交货日的最新一条」，未覆盖回退参考单价。
   成交价与参考价不一致时 `adjust_reason` 必填。`resolveUnitPrice(itemId, deliveryDate)` 公开供调用。
4. **合同门禁复用**：关联合同时走 `PurchaseContractService#assertUsableAsPurchaseBasis`，
   未审核生效或已过期的合同建不出订单（测试锁死）。
5. **给 #49 的只读方法**（本轮并行约定）：`PurchaseOrderService#getOrderAmount(id)` 返回
   `PurchaseOrderAmountDTO`（`orderNo` / `totalAmount` / `counterpartyName` / `usableAsPurchaseBasis`）。
   本票自身不调用；#49 合并后把 `PURCHASE_ORDER` 的 `biz_amount` / `biz_no` 接到这里。
6. **给 #47 / #51 的口子**：`getProgress(id)` 先立起「计划 / 已收 / 未收」两个口径（`scopeNote` 写明
   五口径见 #47，到时在同一个 VO 上扩，不另起一套）；`recordDeal` 是收购单挂回价格快照的入口。
7. **权限 / 菜单**：`icbc:purchase-order:query|manage` 登记进 `RecyclingPermission` + `RecyclingRoleEnum`
   （管理员可维护；收货员 / 开票员 / 财务只读；平台运营不参与）。`icbc-menu.sql` 追加
   5227 采购订单 + 5228–5236 按钮（挂在「采购管理」5203 下，随套餐递归）。
8. **前端**：`api/icbc/purchaseOrder` + `views/icbc/purchaseOrder/index.vue`（列表 / 筛选 / 新增编辑
   含多明细与交货日价格表 / 记录成交 / 执行进度 / 明细含成交记录 / 状态流转 / 只允许删草稿）。
9. **测试**：`PurchaseOrderServiceTest` 16 例（多条明细与金额汇总、合同未生效不得建单、对手方恰好一个非空、
   场站快照、明细与价格表校验、状态流转与终态、暂停必填原因、仅执行中可作依据、过期、分次成交与价格快照、
   调整原因必填、按交货日价格表取价与回退、进度、只删草稿、只读金额、分页）+ `RecyclingRoleEnumTest`
   采购订单权限 + `IcbcTenantIsolationTest` 采购订单租户隔离。**icbc 509 测试全绿**（原 491 + 18）。

> **未做（属 #47 / #51）**：五口径分列、超量 / 过期 / 跨场站拦截与授权、退货扣回、关闭后的业务回查；
> 收购单关联采购订单明细与「直接收购」口径。本票只把订单、定价与成交价格快照做齐，并把门禁
> `assertUsableAsPurchaseBasis` 与只读金额方法留给后续票。

## #49 T11 进项收票登记与勾稽（已完成）

单位供货方（企业 / 个体工商户 / 个人独资企业 / 合伙企业 / 农民专业合作社）向回收企业开具增值税
发票后，由财务登记票面事实并勾稽到采购单据，让**票、货、款三者对得上**。自然人出售者不在本链路
（他们走反向开票，见 ADR 0029）。

1. **两张租户表**：`icbc_input_invoice`（票面事实 + 勾稽合计 + 状态）+ `icbc_input_invoice_link`
   （**通用关联表**：`biz_type` / `biz_id` / `biz_no` / `biz_amount` / `linked_amount`）。迁移
   `backend/sql/mysql/icbc-input-invoice.sql`（幂等，已进 README 导入顺序）；测试建表与 `clean.sql` 同步。
2. **登记唯一**：按「销方 + 发票号码」唯一。`seller_key` = 有税号用税号、否则用销方名称，与 `invoice_no`
   一起构成 `uk_input_invoice_seller_no`；服务层先查再给可读报错（`INPUT_INVOICE_DUPLICATED`）。
3. **勾稽不超限**：`InputInvoiceBizTypeEnum` 先支持 `ACQUISITION`（本分支已有）与 `PURCHASE_ORDER`
   （#46 并行落地，**本分支不 import 它的类**），预留 `STOCK_IN`（#52）。单据号与单据金额一律由调用方
   传入，金额上限按 `bizAmount` 校验：**同一单据的累计勾稽金额不得超过它**，一张票的累计勾稽金额
   不得超过其价税合计。合并 #46 后由人工把 `PURCHASE_ORDER` 的 `bizAmount` 查数接到采购订单的只读方法上
   （一小段）。
4. **状态**：`InputInvoiceStatusEnum`（已登记 / 部分勾稽 / 已勾稽），由「已勾稽金额与价税合计」的关系
   推导并落库，每次勾稽 / 取消勾稽后重算；已勾稽的票不能改票面事实（要改先取消勾稽）。
5. **权限 / 菜单**：`icbc:input-invoice:query|manage` 登记进 `RecyclingPermission` + `RecyclingRoleEnum`
   （管理员全量；财务可管理；开票员只读）；菜单 5244–5249 挂在「财务票务」（5207）下随套餐递归进回收企业套餐。
6. **前端**：`views/icbc/inputInvoice/index.vue` + `api/icbc/inputInvoice`（列表 / 登记 / 修改 / 删除 /
   勾稽记录弹窗含取消勾稽；单据编号与金额由使用方填写，等 #46 / #52 落地后接入选择器）。
7. **测试**：`InputInvoiceServiceTest` 21 例（票面事实齐全、按销方 + 号码唯一、票种 / 金额校验、
   部分 → 已勾稽的状态流转、超单据金额 / 超累计 / 超发票金额 / 重复勾稽 / 非法单据类型 / 零金额拒结、
   取消勾稽重算、已勾稽不可改删、分页筛选、详情带勾稽）；`IcbcTenantIsolationTest` 补一例；
   `RecyclingRoleEnumTest` 补权限一例。icbc 全量测试见下方「提交记录」。

> **与 #46 的接口约定**：`InputInvoiceBizTypeEnum.PURCHASE_ORDER` 已就位，`bizAmount` 由调用方给出；
> #46 合并后只需在采购订单侧提供一个按 `id` 取「单号 + 金额」的只读方法并在前端接上选择器，本票
> 的表结构与校验逻辑不用动。

> **未做（不属本票）**：进项抵扣认证与发票查验平台对接（规格 #38 明确 out of scope，进项一期只做到
> 收票登记与勾稽）。

## #47 T09 履约五口径与执行进度（已完成）

让订单的执行进度按**计划 / 验收 / 入库 / 结算 / 未履行**五个口径分列、不混口径（用户故事 21），
并让超量 / 过期 / 跨场站交货按企业配置拦截或提交授权审核。分支 `t09-order-progress`，
菜单段 5250–5269（用了 5250–5254），错误码段 `1_030_033_xxx`。

1. **五口径的取数只有一处**：`PurchaseOrderService#getProgress`，口径定义（编码 / 名称 / 口径说明 /
   数据来源 / 是否取得到数）的唯一来源是 `PurchaseProgressMeasureEnum`（`icbc-api`），响应里随
   数字一起返回，前端只展示、不另算一遍：
   - **计划** ← `icbc_purchase_order_item.quantity` 汇总；
   - **验收** ← `icbc_purchase_order_deal.quantity` 汇总（**退货记负数，自动扣回** —— AC3）；
   - **结算** ← 成交记录中「来源收购单已归入结算单」的那部分（顺 `deal.source_type=ACQUISITION` +
     `source_id` 读收购单已有的 `settlement_id`，**不依赖 #51 的关联列**）；
   - **入库** ← **标 `available=false` + `unavailableReason`（「待接入」），数量为空而不是 0**。
     入库单是 #52；`PurchaseProgressMeasureEnum.STOCKED_IN` 清空 `unavailableReason`、在
     `getProgress` 里按「来源收购单已入库」补一段取数即可，其余不用动（照 #56 对待「待入库」的做法）；
   - **未履行** ← 计划 − 本单履约口径量，**可为负**（超收是要被看见的异常，不截断）。
   **完成比例必须带口径**：`completionBasis` / `completionBasisName` / `completionBasisDefinition` /
   `completionRatio` 一起返回（AC1）。口径落在租户级配置 `icbc_purchase_setting.performance_basis`
   （默认验收口径；只允许选**能取到数**的口径，入库口径不可选——采购合同 #45 没有承载该字段）。
2. **异常可见**：`getProgress` 另返回 `anomalies`（`OVER_QUANTITY` 验收超计划、`EXPIRED_EXECUTING`
   执行中却已过期、`PENDING_EXCEPTION` 有待审核授权），明细行带 `overQuantity` 标记。
3. **交货门禁 + 授权审核（AC2）**：配置表 `icbc_purchase_setting` 按异常类型各配一项
   `BLOCK`（拦截）/ `APPROVAL`（提交授权审核），**默认 BLOCK，没配过不等于放行**。
   - `checkDelivery(req)` 只读地返回三类异常、企业配置的处理方式、有没有生效中的授权放行；
     `assertDeliveryAllowed(req)` 不允许就抛 `PURCHASE_ORDER_DELIVERY_BLOCKED` /
     `PURCHASE_ORDER_DELIVERY_NEEDS_APPROVAL` 并给出逐条原因。
   - **门禁接在 `recordDeal` 里**（收货即成交记录），所以 UI 手工登记的成交也走同一道闸；
     `#51` 收购登记把收购单挂到订单上时调 `assertDeliveryAllowed` 即可，不要再另写一份判断。
   - **订单状态是硬门禁**：草稿 / 暂停 / 完成 / 关闭一律拒（前两者抛 `PURCHASE_ORDER_NOT_DELIVERABLE`，
     关闭抛 `PURCHASE_ORDER_CLOSED_NOT_DELIVERABLE`），企业配置与授权都放宽不了；**过期**才是可配置的那一条。
   - 新增 `icbc_purchase_exception`（履约异常授权单）：提交 → 审核（通过 / 拒绝）→ 已通过的授权按范围放行。
     授权范围：超量给「追加量」（多张已通过授权的追加量**累加**，超出的部分仍拦）、
     过期给「有效期」、跨场站必须**指定场站**（只对该场站放行）。授权只放宽它自己那一件事，不改订单状态。
4. **退货按口径扣回（AC3）**：`recordDeal` 接受**负数数量**（退货），验收 / 结算两个口径都会跟着减；
   已暂停 / 完成 / 关闭的订单**仍可登记退货**（否则货退回来没地方记），只有草稿单不可以。
5. **关闭不删除已发生的业务（AC4）**：关闭后订单、成交记录、授权单都还在（`deleteOrder` 仍只允许草稿），
   测试锁死「关闭后 `getDealList` / `getProgress` 照常可读、`deleteOrder` 被拒」。
6. **权限 / 菜单**：新增 `icbc:purchase-setting:query|manage`、`icbc:purchase-exception:query|request|audit`
   （管理员全量；收货员可提交可查、不能自己审；财务只看；平台运营不参与）。菜单 5250–5254 挂在
   「采购管理」（5203）下，随套餐递归进回收企业套餐（已在临时库 `t09_menu_check` 整份跑通后删库）。
7. **落地**：迁移 `backend/sql/mysql/icbc-purchase-order-progress.sql`（幂等，两张租户表，已进 README 导入顺序）；
   测试建表与 `clean.sql` 同步；新增枚举 `PurchaseProgressMeasureEnum` / `PurchasePerformanceBasisEnum` /
   `PurchaseDeliveryRuleEnum` / `PurchaseExceptionTypeEnum` / `PurchaseExceptionStatusEnum` /
   `PurchaseDealSourceTypeEnum`（成交记录的来源类型，`#51` 应使用它而不是写字符串）。
8. **测试**：`PurchaseOrderServiceTest` 24 例（五口径分列且来源分开、结算口径、退货扣回、异常可见、
   三类门禁 BLOCK / APPROVAL / 硬门禁、授权范围收紧与到期、配置默认值与非法值、关闭保留业务）+
   新增 `PurchaseOrderExceptionServiceTest` 6 例（提交校验与不重复提交、审核收紧范围、拒绝与过有效期、
   订单级异常不挂明细、口径说明、分页）+ `RecyclingRoleEnumTest` 补权限 + `IcbcTenantIsolationTest`
   补两表租户隔离。**icbc 549 测试全绿**；PC `pnpm build:local` 通过、`pnpm ts:check` 在
   `views|api/icbc/purchaseOrder` 下零新增报错。
9. **前端**：`views/icbc/purchaseOrder/index.vue` 的执行进度弹窗改为五口径表格（含口径说明、数据来源、
   「待接入」标注、完成比例口径标签、异常提示条）；新增 `views/icbc/purchaseOrder/exception.vue`
   （授权单列表 / 提交 / 审核 / 口径说明）与 `views/icbc/purchaseOrder/setting.vue`（履约配置）。

> **给 #51 / #52 的接口**：`#51` 把收购单挂到订单上时，成交记录的 `source_type` 用
> `PurchaseDealSourceTypeEnum.ACQUISITION`、`source_id` 填收购单编号（结算口径靠它找结算状态），
> 并在登记前调 `assertDeliveryAllowed`；`#52` 的入库单落地后只改 `PurchaseProgressMeasureEnum.STOCKED_IN`
> 与 `getProgress` 的入库取数，不改 VO 与前端（`stockedQuantity` 从空变成数字）。

> **与 #51 的约定已遵守**：本票没有动 `icbc_acquisition` 的任何列，也没有碰采购合同。
> 合并时若 #51 先合，仍需人工确认它的成交记录写入与 `recordDeal` 的门禁一致（超量 / 过期 / 跨场站）。

## #51 T13 收购单关联采购安排与「直接收购」（已完成）

一张收购单可以挂到一条**有效采购安排**（执行中且未过期的采购订单 + 品类明细），也可以什么都不挂。
不挂的收购在报表 / 列表标为「直接收购」——**它不是失败也不是缺失**，零散散户不必虚造订单。

1. **数据**：`icbc_acquisition` 加 `purchase_order_id` / `purchase_order_item_id`，`NOT NULL DEFAULT 0`
   （0 = 未关联，即直接收购）。迁移 `backend/sql/mysql/icbc-acquisition-purchase-link.sql`（幂等，
   必须在 `icbc-acquisition.sql` 与 `icbc-purchase-order.sql` 之后），已进 README 导入顺序；
   测试建表同步（H2 `ADD COLUMN IF NOT EXISTS`）。**没有新表，`clean.sql` 未动。**
2. **门禁只有一处**：`AcquisitionServiceImpl.applyPurchaseArrangement` 调 #46 已有的
   `PurchaseOrderService#assertUsableAsPurchaseBasis`（执行中 + 未过期），不复制判断；明细必须属于该订单
   （`getOrderItem`），订单交易对方必须是本次收购的出售者，且明细品类必须与本次收购品类一致
   （否则履约进度会串主体 / 串品类）。订单 / 明细缺一即拒
   （`ACQUISITION_PURCHASE_ARRANGEMENT_INCOMPLETE`，错误码 `1_030_034_xxx`）。
3. **可选采购安排查询**：`PurchaseOrderService#getUsableArrangements(payeeId)` 返回该自然人出售者
   「执行中且未过期」的订单 + 品类明细（`PurchaseArrangementRespVO`），与 `assertUsableAsPurchaseBasis`
   共用同一个 `isExpired` 判定。端点 `GET /icbc/acquisition/purchase-arrangement/list`（权限复用
   `icbc:purchase-order:query`，收货员已有）。#47 拥有的 `PurchaseOrderProgressRespVO` 未改。
4. **报表 / 列表口径**：`AcquisitionRespVO` 增 `directAcquisition`（是否直接收购）与
   `purchaseArrangementText`（直接收购 / 采购订单）；分页增 `directAcquisition` 筛选。PC 收购单列表加
   「采购安排」列（直接收购用中性 `info` 标签，**不是 danger**）与筛选。
5. **现场端（AC4）**：`pages/acquisition` 新增「有效采购安排（可选）」卡片——先选订单、再选品类明细，
   第 0 项是「不关联（直接收购）」；不选就是直接收购，选了必须选到明细且明细品类与上面的品类一致。
   从交接批次进来时，若批次上挂了采购订单（#50 预留的字段）会替现场预选订单，明细仍由现场挑。
6. **AC 覆盖**：同一交接批次下多张收购单、同出售者 + 同场站（新增测试，缺磅次 / 场站用真实交接批次服务造）；
   一车两种品类生成两张收购单与一份批次记录（#50 已有测试继续有效）。
7. **测试**：`AcquisitionServiceImplTest` 新增 10 例（关联可用订单、未关联落 0、草稿 / 过期订单拒、
   明细不属订单拒、交易对方不符拒、品类不符拒、只给明细拒、同批次同出售者同场站、分页按直接收购筛选）；
   `PurchaseOrderServiceTest` 新增 3 例（只列执行中未过期且属该出售者的订单、payeeId 空返回空、明细不属订单拒）。
   不新增权限 / 菜单（复用既有 `icbc:acquisition:*` 与 `icbc:purchase-order:query`），
   `RecyclingPermission` / `RecyclingRoleEnum` 未动。icbc 全量 **545 测试全绿**；现场端 `pnpm ts:check` 与
   `pnpm build:h5`、PC `pnpm build:local` 均通过。

> **与 #47 的分工**：本票只落「收购单 → 采购订单 / 明细」的关联与「直接收购」口径，不碰执行进度的五口径
> 与超量 / 过期 / 跨场站的业务拦截（那是 #47）。`recordDeal` 未被本票调用——成交价格快照仍由采购订单侧
> 手工登记，进度归集留给 #47。

> **未做**：批次级采购订单的 UI 选择（`icbc_handover_batch.purchase_order_id` 只做带出，不在交接批次页选）；
> 收购单创建后改挂 / 解绑采购安排（登记后即固定）。

## 已知跨票缺口：收购单关联采购安排未进履约口径与交货门禁（待决策）

- **现象**：#47 把「验收 / 结算」取数与超量 / 过期 / 跨场站门禁都收敛到 `PurchaseOrderService#recordDeal`
  （「成交记录就是这一车验收了多少落到订单上的唯一入口」）；#51 让收购单可关联采购订单明细，但**只调了
  `assertUsableAsPurchaseBasis`，没调 `recordDeal`**。结果：关联了订单的收购单，不进「验收」口径、也不过交货门禁。
- **根因是口径没定**：`recordDeal` 只有一个 `quantity`，而平台有两个重量口径（ADR 0028）——
  `net_weight`（毛 − 皮）= 实物收到多少；`settlement_weight`（毛 − 皮 − 扣杂）= 计价基准（ADR 0019）。
  二选一或把 deal 拆成两列，是**产品决策**，不在实现票范围里，所以没擅自接。
- 另有两处待定：收购单**作废**时是否落负数 deal（AC3 退货扣回）；离线补传的幂等（同一收购单不能产生两条 deal）。
- 已开跟进票记录选项，决策后再接（改动范围：`AcquisitionServiceImpl#applyPurchaseArrangement` + 作废路径 + 测试）。

## #52 T14 待入库 → 入库单 → 库存流水（已完成）

验收后的货进待入库，仓管选仓库 / 库位 / 批次确认实际入库量；入库是从收购单派生的**单向动作**，
库存写入只经 #43 的 `StockApi`，**icbc 不直接碰 `erp_stock*`**（ADR 0027 / 0028）。分支
`t14-stock-in`，菜单段 5255–5319（用了 5255–5258），错误码段 `1_030_035_xxx`。

1. **两张租户表**（`backend/sql/mysql/icbc-stock-in.sql`，幂等；测试建表与 `clean.sql` 同步）：
   `icbc_stock_in`（入库单：`status` 0-待过账 / 1-已过账 / 2-已作废，`available_quantity` 是确认时
   的可入库实物量快照）与 `icbc_stock_in_item`（明细：仓库 / 库位 / 批次编号 + 数量，0 = 未指定；
    **只存维度编号不存名称**——icbc 只依赖 `erp-api`，拿不到 ERP 仓库表，名称由前端用 ERP 的
   simple-list 解析）。
2. **只有过账才加库存**：`createStockIn` 落待过账（不动库存），`postStockIn` 才逐条调
   `StockApi.in`（`RECEIPT_IN(90)`）写流水；`confirmStockIn` = 建单 + 过账（仓管一次成型）。
   作废已过账的单走 `RECEIPT_IN_CANCEL(91)` 冲销。**幂等**：业务项编号 = 入库明细编号，
   重复确认直接返回（不重复加库存）；同一收购单分多次入库时业务编号同为收购单编号。
3. **累计入库不超可入库实物量（含并发）**：`StockApi.in` 传 `maxCount`，
   **跨入库单**按「业务类型 + 收购单 + 品类」累计校验；作废后把已冲销量加回上限
   （`maxCount = 可入库实物量 + 已冲销量`），使净效果是「累计入库（已过账）≤ 可入库量」。
   过账前用 `IcbcAcquisitionMapper#selectByIdForUpdate`（`SELECT ... FOR UPDATE`）锁住收购单行，
   把同一收购单的并发过账串行化；`sumReversed` 用 `postedTime != null` 区分「已过账后作废」
   与「待过账直接作废」（后者从未写 `RECEIPT_IN`，不能当冲销）。
4. **可入库实物量只有一个取数点**：`StockInService#resolveAvailableQuantity(acquisition)`，
   现在取净重（实物口径）。**#53 落地 `accepted_weight` 后只改这一个方法**（有接收量优先取接收量），
   待入库列表的 SQL 只做「已归入结算单 + 未作废」的粗筛，不在 SQL 里再写一份重量口径。
5. **待入库口径**：已验收（`settlement_id` 非空）、未作废、且「可入库实物量 − 累计入库 > 0」的收购单。
   累计入库 = 当前已过账入库单合计（作废的自然不在其中）。列表在工作队列规模上按 id 倒序在内存里分页。
6. **权限 / 菜单**：新增 `icbc:stock-in:query|manage` 登记进 `RecyclingPermission` + `RecyclingRoleEnum`
   （管理员 / 收货员可管理；开票员 / 财务只读；平台运营不参与）。`icbc-menu.sql` 追加
   5255 待入库与入库单 + 5256–5258 按钮（挂在「仓储管理」5205 下，随套餐递归进回收企业套餐；
   已在临时库整份跑通并确认 5255 在套餐 `menu_ids` 里后删库）。
7. **前端**：`api/icbc/stockIn` + `views/icbc/stockIn/index.vue`（待入库列表 + 确认入库弹窗可加多条
   库位明细；入库单列表 + 过账 / 作废 / 详情）。AC5 的「只有入库记录时只称累计入库，不称当前库存」
   在页面顶部用告警写明。`pnpm build:local` 通过。
8. **测试**：`StockInServiceTest` 14 例（唯一取数点、待入库筛选与三个数量、建单不动库存、
   拆库位过账与与 `StockApi` 的契约、跨入库单累计、累计越界拒、冲销后上限加回、重复过账幂等、
   待过账 / 已过账作废、未验收 / 已作废 / 无重量门禁、分页与详情）；`IcbcTenantIsolationTest` 补两表
   租户隔离；`RecyclingRoleEnumTest` 补权限。**icbc 578 测试全绿**（1 skipped 为既有）。

> **遗留（#52 落地后的人工接线，本票未做）**：
> 1. `WorkbenchTodoCodeEnum.PENDING_STOCK_IN` 的 `unavailableReason` 可清空，并在
>    `WorkbenchServiceImpl.loadTodo` 加分支，按「已验收且未入库」取数（见「第四轮并行约定」）；
> 2. `PurchaseProgressMeasureEnum.STOCKED_IN` 可清空 `unavailableReason`，在
>    `PurchaseOrderService#getProgress` 按「来源收购单已入库」补入库口径取数（见 #47 小节）。
> 两处都属跨票接线，避免与并行票抢文件，留给合并后人工接。
>
> **未做（不属本票）**：结算重量与入库重量的差异清单 / 异常表（#53 / #57）；
> 采购订单履约的入库口径（#47 预留）；`InputInvoiceBizTypeEnum.STOCK_IN` 的勾稽接入
> （入库单无金额，暂不接）。

## #53 T15 拒收 / 部分接收与余货出场（已完成）

验收结论可以是**接收 / 部分接收 / 拒收**：接收量留下、退回量与余货出场量离场。拒收部分
**不形成采购应付、不进正常库存**，但仍在收购单上可追溯（ADR 0028：结算重量只作计价基准，
实物在库量是另一个数字）。分支 `t15-partial-receipt`，菜单段 5320–5379（用了 5320 / 5321），
错误码段 `1_030_036_xxx`（用了 000–004）。

1. **`icbc_acquisition` 五个新列（全可空，历史数据行为不变）**：`accepted_weight`（接收量）、
   `rejected_weight`（退回量）、`residual_weight`（余货出场量）、`reject_reason`（拒收原因）、
   `weight_diff`（称量差异）。迁移 `backend/sql/mysql/icbc-acquisition-acceptance.sql`（幂等，
   必须在 `icbc-acquisition.sql` 之后，已进 README 导入顺序）；测试建表同步（H2 `ADD COLUMN IF NOT EXISTS`）。
   **没有新表，`clean.sql` 未动。**
2. **接收结论只有一个入口**：`POST /icbc/acquisition/acceptance`（`icbc:acquisition:acceptance`，
   管理员 / 收货员）。校验：三个重量都不为负；退回量 > 0 必须有拒收原因；三者之和不超过过磅净重
   （只校验「不超过」不强制「等于」——少掉的那部分正是要靠差异暴露的）。**不静默抹平**。
3. **拒收部分不进应付**：金额按 `（结算重量 − 退回量 − 余货出场量）× 单价 + 调整项` 重算（应付量不小于 0），
   在 `applyAcceptancePricing` 一处；拒收部分也不进库存——实物量取 `accepted_weight`（有值优先于 `net_weight`）。
4. **实物量的唯一取数点是 `IcbcAcquisitionDO#resolvePhysicalWeight()`**（接收量优先，无则净重）。
   > **给 #52（T14）的接线**：入库的「可入库实物量」请调这个方法，不要在入库侧另写一遍
   > 「用 net 还是 accepted」；#53 已落 `accepted_weight`，有值即优先。
5. **称量差异落字段 + 只读清单**：`weight_diff` = 实物量 − 结算重量（未做接收结论时实物量取净重，
   所以有扣杂的单也会看到差额），只要两侧都算得出就落库；`GET /icbc/acquisition/weight-diff/page`
   （`icbc:acquisition:weight-diff:query`，只读）按 `hasDifference` / `onlyAccepted` / 出售者 / 时间过滤，
   带 `physicalWeight` 与 `differenceNote` 供 **#57 的异常表直接消费**。
6. **两道时序门禁（防止口径静默变化）**：已挂开票申请（金额已固定）或已归入结算单（#33 版本已快照）后，
   不允许再改接收结论，分别报 `ACQUISITION_ACCEPTANCE_AFTER_INVOICE_LINKED` /
   `ACQUISITION_ACCEPTANCE_AFTER_SETTLEMENT`。接收结论应在「结束本次收货」前记录。
7. **权限 / 菜单**：`RecyclingPermission` 追加 `ACQUISITION_ACCEPTANCE`、`ACQUISITION_WEIGHT_DIFF_QUERY`
   （只追加，未重排）；`RecyclingRoleEnum` 挂到管理员 / 收货员（可记录）与开票员 / 财务（差异只读）；
   `icbc-menu.sql` 追加 5320「接收结论与称量差异」（回收作业 5204 下）+ 5321「记录接收结论」。
8. **前端**：`views/icbc/acquisition/AcceptanceForm.vue`（弹窗：带出净重 / 结算重量 / 单价，
   实时算应付与差异，拒收必填原因）；`views/icbc/acquisition/index.vue` 操作列加「接收结论」；
   新增只读页 `views/icbc/acquisition/weightDiff.vue`（差异清单 + 记录入口）。
9. **测试**：`AcquisitionServiceImplTest` 新增 9 例（部分接收重算应付与差异、全拒收应付为 0 仍可追溯、
   退回无原因拒、超净重拒、负重量拒、已挂开票拒、已归结算拒、扣杂差异不抹平、差异清单过滤、
   修正识别重算差异）；`RecyclingRoleEnumTest` 新增接收结论 / 差异清单权限一例。
   **icbc 572 测试全绿**；PC `pnpm build:local` 通过。

## #54 T16 非销售出库、跨仓调拨、盘点调整与期初（已完成）

补上让余额能被称作「当前库存」的其余能力。库存写入只经 ERP 的 stock 域（`StockApi`），
icbc 不直接碰 `erp_stock*`（ADR 0027 / 0028）。分支 `t16-stock-ops`，菜单段 5259–5299（用了
5259–5273），错误码段 `1_030_037_xxx`（用了 000–034）。

1. **扩 `StockApi`（erp-api + erp-biz）**：新增 `move(StockMoveReqDTO)`（跨仓调拨：源减目标加，
   一次写 `MOVE_OUT(32)` + `MOVE_IN(30)` 两条流水）与 `adjustTo(StockAdjustReqDTO)`（盘点对齐实盘数：
   ERP 在同一个事务里加行锁读余额、算差额，盘盈写 `CHECK_MORE_IN(40)`、盘亏写 `CHECK_LESS_OUT(42)`，
   账实相符不写流水），两者都按「业务类型 + 业务编号 + 业务项编号」幂等。期初复用 `in`。
   余额行加锁读收在 `ErpStockService#getStockForUpdate`（`StockApiImpl` 不碰 DAL）。
   `ErpStockRecordBizTypeEnum` **追加** `SCRAP_OUT(100)` / `RETURN_OUT(102)` / `INTERNAL_USE_OUT(104)` /
   `OPENING_IN(110)` 及各自的 `*_CANCEL(101/103/105/111)`。
2. **四类单据（icbc 侧，`backend/sql/mysql/icbc-stock-ops.sql`，幂等；测试建表与 `clean.sql` 同步）**：
   `icbc_stock_out`（报损 / 退货出库 / 内部领用，**不挂客户**，AC1）+ 明细、`icbc_stock_move`（源 / 目标
   都带仓库 + 库位 + 批次）+ 明细、`icbc_stock_check`（明细存实盘数，账面 / 差额过账时落库）、
   `icbc_stock_opening`（一个「品类 + 仓库 + 库位 + 批次」一行，导入即过账）。四类共用
   `StockOpsStatusEnum`（0-待过账 / 1-已过账 / 2-已作废）与 `StockOutTypeEnum`。
3. **登记 / 过账 / 作废**：登记不动库存；过账才经 `StockApi` 写流水；作废已过账的按**相反方向**冲销
   （出库用 `*_CANCEL`、调拨源加回目标减掉、盘点按**记录的差额**冲销而不是重算、期初用
   `OPENING_IN_CANCEL`）。作废都必须写原因。盘点差额由 ERP 算，icbc 只把实盘数递过去。
4. **期初的两条规矩**：同一维度只允许一条**生效**期初（重复导入整批拒绝，先校验全批再写，不留半份）；
   作废后该维度可重新导入。录错了走作废重导或盘点调整。
5. **AC2「余额与流水始终一致」由 ERP 侧断言**：`StockApiImplTest` 断言调拨后两个仓库各自
   「余额 = 该维度全部流水重算」，盘点后「盘盈 / 盘亏流水之和 = 余额」。
6. **AC4「页面才显示当前库存」**：新增 `GET /icbc/stock-ops/readiness`
   （`StockOpsReadinessServiceImpl`，权限 `icbc:stock:readiness:query`）返回四项能力位、
   `openingImported`、`capabilitiesReady`、`currentStockReady`、`label`（当前库存 / 累计入库）与 `notice`。
   判定写成两层：四项能力齐备（#54 落地后为 true）**且已导入期初**才 `label=当前库存`；没导期初时余额
   漏掉启用平台之前的存量，只能说“累计入库”。「库存查询」页（`views/erp/stock/stock/index.vue`）
   按 `label` 渲染列名并显示 `notice`；#52 页面上那句「出库 / 调拨 / 盘点尚未落地」的告警已相应改写。
7. **权限 / 菜单**：`RecyclingPermission` 追加 9 个（`STOCK_OUT_QUERY|MANAGE`、`STOCK_MOVE_QUERY|MANAGE`、
   `STOCK_CHECK_QUERY|MANAGE`、`STOCK_OPENING_QUERY|MANAGE`、`STOCK_READINESS_QUERY`，只追加未重排）；
   `RecyclingRoleEnum` 挂到管理员 / 收货员（可管理）与开票员 / 财务（只读 + 就绪查询）；
   `icbc-menu.sql` 在「仓储管理」（5205）下追加 5259 非销售出库 / 5263 跨仓调拨 / 5267 盘点调整 /
   5271 期初导入及其按钮（随套餐递归进回收企业套餐）。
8. **前端**：`api/icbc/stockOps` + `views/icbc/{stockOut,stockMove,stockCheck,stockOpening}`（登记 / 过账 /
   作废 / 详情；期初页支持多行录入与「从 Excel 粘贴」按名称解析维度）。`pnpm build:local` 通过。
9. **测试**：ERP 34 全绿（`StockApiImplTest` 新增 6 例：调拨余额与流水一致、调拨幂等、调拨源不足整单拒、
   盘点盘盈盘亏、账实相符不写流水、盘点幂等）；icbc 634 全绿（新增 `StockOutServiceTest` 12、
   `StockMoveServiceTest` 9、`StockCheckServiceTest` 10、`StockOpeningServiceTest` 9、
   `StockOpsReadinessServiceTest` 2，`RecyclingRoleEnumTest` / `IcbcTenantIsolationTest` 各补一例）。
   本地库（临时 scratch 库）整份跑 `icbc-stock-ops.sql` 与 `icbc-menu.sql` 通过、迁移重跑幂等、
   5259–5273 落库正确并进套餐 `menu_ids`。

> **与 ADR 0025 §1 的有意偏差（留档）**：ADR 0025 原写「`stock_out` 保留但只用于非销售出库」、
> 「重造调拨 / 盘点没有意义」。本票按第五轮并行约定（「库存只经 ERP 的 stock 域写；`StockApi` 不够时
> 扩 `StockApi` + 业务类型枚举追加」）把四类**业务单据**建在 icbc 侧、**库存余额与流水仍归 ERP**：
> ERP 的 `erp_stock_out/move/check` 单据服务与前端页面自 #42 删掉 product 域后已不可达（前端仍在用
> `productId`），且 `erp_stock_out` 强制挂客户、缺「报损 / 退货出库 / 内部领用」类型，与 AC1 直接冲突。
> 「重造调拨 / 盘点没有意义」读作「余额 + 流水的记账机制不重造」——本票没有自己的余额表，全部经
> `StockApi` 写 `erp_stock` / `erp_stock_record`。**ADR 0025 已补 2026-09-20 修订注**。

> **遗留**：①`icbc_stock_opening` 的「同一维度一条生效期初」是服务层校验 + 普通索引，并发导入
> （两个管理员同时导同一维度）理论上可能各写一条；期初是 go-live 前的一次性动作，没上唯一约束
> （作废后要允许重导）。②盘点的「对齐到实盘数」在余额行**不存在**时没有行可锁，靠
> `updateStockCountIncrement` 的并发插入兜底，极端并发下差额可能按旧账面算；icbc 侧的单据状态
> 串行化了同一单据的重复过账。③#55（一票一档 / 链路追溯）与 #57（经营报表）要把这四类库存作业
> 纳入追溯与报表口径时，直接读 `icbc_stock_*` 单号即可。

## #55 T17 关联单据查询（已完成）

在一张详情页里回答「这批货经历了什么」：采购订单—现场收货—仓储入库—结算确认**四栏**，后接付款与发票；
按单号 / 车牌 / 主体反查。只读聚合，**不新建业务表**，聚合放在 `service/trace`，不 import #57 的报表类
（「异常」的权威判定归 #57）。分支 `t17-trace`，菜单段 5300–5319（用了 5300–5302），错误码段 `1_030_038_xxx`。

1. **聚合锚点是「收购单」**：一行 = 一次物理交接里的一个品类（一张收购单），六栏按固定顺序铺开
   （`TraceStageCodeEnum.ordered()`：采购订单 / 现场收货 / 仓储入库 / 结算确认 / 付款 / 发票）。
   一对多（多次磅次 / 多张入库单与库位明细 / 多个结算版本 / 多次付款 / 多张发票与红冲）在阶段内
   `nodes[].children[]` **全部展开**（AC1）。
2. **状态五态**（`TraceStageStatusEnum`）：未开始 / 处理中 / 已完成 / 异常 / **无需该环节**。
   「**直接收购**」（`purchase_order_id = 0`）在采购订单栏显示为「无需该环节」，不是缺失、不虚造订单（AC2）。
3. **AC4 差异不默认一对一**：`buildDifferences` 把 **已过账入库量 vs 结算重量** 逐条比出来——
   两者不等 → `STOCK_IN_VS_SETTLEMENT`（例：入库 14.74 / 结算 28.33，差额 −13.59 并写明「结算重量只是计价基准、
   实物在库另有口径，需人工核实」）；已结算却查不到入库 → `MISSING_STOCK_IN_LINK`；已入库未结算 →
   `MISSING_SETTLEMENT_LINK`；另有实物量 vs 结算重量的 `WEIGHT_DIFF_VS_SETTLEMENT`（直接读 #53 的 `weight_diff`）。
   只说明、不抹平，也不用库存反推金额。
4. **AC5 汇总不重复计数**：合计口径是「每张收购单各计一次」，不把同一结算单 / 采购订单再加一遍；
   响应带 `scopeNote`（本次筛选覆盖了什么）与 `hiddenDetailCount`（命中未在本页展示的明细数）。
5. **反查六入口**（`TraceKeywordTypeEnum`）：`AUTO` 按「收购单号 → 交接批次号 → 采购订单号 → 入库单号 →
   结算单号 → 发票号 → 支付单号 → 车牌 → 出售者」逐个试；也可显式指定。车牌同时匹配收购单上的
   磅单 / 车辆车牌与**交接批次上的车牌**（`IcbcHandoverBatchMapper.selectListByPlateNo`）。
   至少给一个条件，否则报 `TRACE_QUERY_CONDITION_REQUIRED`（无条件全表翻页是 #57 的事）。
6. **AC3 原单 / 附件 / 操作历史 / 上下游**：每个节点带 `detailPath`（跳到既有原单页），附件从
   收购单（磅单 / 车头车尾）、磅次、押金单回单、发票原件、结算线下签字件汇总；操作历史按**业务单据自身的时点**
   重建（登记 / 磅次 / 入库过账 / 结算生成与确认 / 付款 / 开票），不伪造系统操作日志；上下游由四栏顺序表达。
7. **AC6 脱敏与导出**：`MaskUtils` 追加 `maskBankCard` / `maskTaxNo`；税号 / 身份证 / 手机号 / 银行卡默认脱敏，
   有 `icbc:trace:sensitive:view` 才返原值（管理员 / 财务）。导出 `GET /icbc/trace/export` 需
   `icbc:trace:export`（管理员 / 财务），走同一套脱敏判断，并调 `OperateLogApi` **留一条导出操作记录**；
   命中超过 500 条报 `TRACE_EXPORT_LIMIT_EXCEEDED` 提示缩小范围。
8. **权限 / 菜单**：`RecyclingPermission` 追加 `TRACE_QUERY` / `TRACE_SENSITIVE_VIEW` / `TRACE_EXPORT`
   （只追加）；`RecyclingRoleEnum` 四个租户内角色都能查，未脱敏与导出只给管理员 / 财务；
   `icbc-menu.sql` 在「业务追溯」5208 下追加 5300 关联单据查询 + 5301 查看未脱敏字段 + 5302 导出关联单据。
9. **落地**：新增 `service/trace/TraceQueryService(+Impl)`、`controller/admin/trace/TraceController` 与 12 个 VO、
   4 个枚举；只给既有 Mapper **追加**查询方法（`selectListByTraceSearch` / `selectListByPlateNo` /
   `selectListBySellerKeyword` / `selectListByPlateNo` / `selectListByAcquisitionIds` / `selectListByOrderIds`），
   不建表、不动 `create_tables.sql` / `clean.sql`；前端 `api/icbc/trace` + `views/icbc/trace/index.vue`
   （搜索 + 汇总 + 四栏表格 + 详情抽屉）。
10. **测试**：`TraceQueryServiceTest` 23 例（六栏固定顺序、直接收购「无需该环节」、入库 vs 结算差异、
    缺失入库 / 缺失结算、称量差异、脱敏与岗位放开、车牌 / 结算单号 / 发票号 / 主体反查、汇总不重复、
    分页与未展示明细、只看差异、附件与操作历史、开票异常态、导出留记录、导出超限）；
    `RecyclingRoleEnumTest` 追加 trace 权限一例。**icbc 614 测试全绿（1 skipped 为既有）**；PC `pnpm build:local` 通过。

> **与 #57 的边界**：本票只展示链路自身的状态与差异，不做异常汇总表、不做按期间报表；
> 「磅差 / 超采购量 / 超入库量 / 重复关联 / 长期未确认 / 资料缺失」的权威判定仍在 #57（T19）。
> **未做**：节点级显式的「上下游」字段（由四栏顺序表达）；导出只出当前筛选结果，不做按期间批量。

## #57 T19 经营报表与异常表（已完成）

给回收企业自己的经营视角：采购履约 / 收购台账 / 库存 / 结算付款四张表 + 一张异常表。全部
**只读聚合**，聚合放在 `service/report`，不 import #55 的追溯类；异常表是**派生清单、不新建表**
（同额度台账 ADR 0014）。分支 `t19-reports`，菜单段 5322–5399（用了 5322），错误码段 `1_030_039_xxx`（用了 000）。

1. **四张表（口径随响应返回，前端只展示、不重算）**：
   - **采购履约** ← 消费 `PurchaseOrderService#getProgress`（#47 五口径的唯一来源，本包不重算）：
     计划量 / 实际履约量（= 完成比例口径量，验收或结算）/ 余额 / 验收 / 入库 / 结算 / 到期日 / 完成比例
     与超量、过期异常；
   - **收购台账** ← `icbc_acquisition`：交易对方 / 场站 / 回收方式（直接收购或采购订单）/ 品类与等级 /
     毛皮净重、结算重量、接收量、退回量、余货出场量、称量差异 / 成交金额 / 对应结算单与发票；
   - **库存** ← 新开的 ERP 只读端口 `StockReportApi`（在库量与入出流水，带仓库 / 库位 / 批次名与
     批次入库时间算库龄）；**只给数量口径，不带金额**（ADR 0027）；
   - **结算付款** ← `icbc_settlement` + 该结算单下未作废收购单金额 + 逐笔付款单：结算金额 / 办理进度 /
     回单状态 / 失败原因 / 未办理时长。
2. **异常表 = 六类派生清单**（`ReportAnomalyTypeEnum` 是判定口径的唯一来源，含严重程度与建议下钻入口）：
   - **磅差** ← 直接读 #53 已落库的 `icbc_acquisition.weight_diff`（≠ 0）；
   - **超采购量** ← #47 `getProgress` 明细的 `overQuantity`（消费口径，不重算）；
   - **超入库量** ← 同一收购单累计**已过账**入库量 > 可入库实物量（`resolvePhysicalWeight()`，#52 的上限）；
   - **重复关联** ← 同一收购单关联多张未作废付款单（重复付款风险）；
   - **长期未确认** ← 结算单未确认且已过 `deadline_time`（未设时按生成时间 + 48h）；
   - **资料缺失** ← 未作废收购单缺五流骨架要件（品类 / 交易时间 / 地点 / 出售者 / 净重 / 单价 / 磅单号）。
   `type` 为空时六类合并、按时间倒序、内存分页；未知类型报 `REPORT_ANOMALY_TYPE_UNKNOWN`。
3. **AC6「口径不混」**：收购台账只取收购单**已发生的计量结果**，预约约量（`icbc_appointment.expected_quantity`）
   与采购计划量（`icbc_purchase_order_item.quantity`）不混入；`ReportTableEnum` 把每张表的口径写死并随
   `/icbc/report/tables` 返回。测试里专门造了「只有预约与计划、没有收购单」的场合并断言台账为空。
4. **跨域只读端口**：`erp-api` 新增 `StockReportApi`（`getStockBalancePage` / `getStockRecordPage`），
   `erp-biz` 的 `StockReportApiImpl` 复用 `ErpStockService` / `ErpStockRecordService` 的分页并补名称与库龄。
   **没有动 `StockApi`**（写入端口归 #54），避免与并行票抢文件；icbc 依旧不直接碰 `erp_stock*`。
5. **权限 / 菜单**：`RecyclingPermission` 追加 `REPORT_QUERY`（只追加、未重排），挂到管理员 / 财务；
   `icbc-menu.sql` 追加 5322「经营报表查询」按钮行（经营报表页 5209 下）。
6. **前端**：`api/icbc/report` + 重写 `views/icbc/report/index.vue`（五页签、每张表带口径条、来源单据与
   下钻入口）；采购履约下钻复用 `PurchaseOrderApi#getProgress` 展示五口径明细。`pnpm build:local` 与
   `pnpm ts:check` 均通过（0 error）。
7. **测试**：`ReportServiceTest` 17 例（五张表带口径、履约五口径来自 getProgress、台账带各重量口径且排除
   作废、AC6 不混预约 / 计划、库存委派 ERP 并算库龄、结算金额与派生进度 / 回单 / 失败原因 / 未办理时长、
   派生筛选 total 正确、六类异常各自命中与合并排序、未知类型报错、出售者筛选）；`RecyclingRoleEnumTest`
   新增报表权限一例。**icbc 607 测试全绿**（1 skipped 为既有）。

> **接线提醒**：`ReportServiceImpl` 依赖 `PurchaseOrderService`（#47）与 `StockReportApi`（ERP 只读端口）。
> 测试用 `@MockBean` 顶替两者（跨模块 / 跨票只消费口径）。库存要覆盖 #54 的非销售出库 / 调拨 / 盘点后，
> 入出流水会经 `ErpStockRecordBizTypeEnum` 的新业务类型自然出现，报表无需改。
>
> **未做（不属本票）**：异常表的导出与批量处理动作（只读清单）；P1 经营分析（品类 / 场站 / 出售者贡献 /
> 运输费用）；`create_tables.sql` / `clean.sql` 未动（**没有新表**）。

## #58 收购单关联采购订单后进履约口径与交货门禁（已完成，提交 `dbf7ea1`）

第二轮与第三轮合起来留下的最后一处跨票缺口，按**口径 C** 接（成交记录把「验收量」与「结算量」分开）：

1. **`icbc_purchase_order_deal` 加 `accepted_quantity`**（迁移 `icbc-deal-accepted-quantity.sql`，幂等；为空时按 `quantity` 计，兼容历史）。验收口径取它，结算口径仍取 `quantity`（计价基准，ADR 0019）。
2. **关联采购安排的收购单，登记时落一条成交记录**（`AcquisitionServiceImpl#syncPurchaseDeal`，`sign=1`）：
   - `quantity`（结算量）= 结算重量 − 退回量 − 余货出场量（与 #53 的应付口径一致）；没录重量时回退申报数量；
   - `acceptedQuantity`（验收量）= 接收量优先、无接收结论取净重、都没录取申报数量；
   - `stationId` 透传给交货门禁：`PurchaseOrderDealReqVO` 补了 `stationId`，`recordDeal` 里把它带进 `assertDeliveryAllowed`——**超量 / 过期 / 跨场站现在对收购来的货也生效**；
   - 单价与订单参考价不一致时自动带原因「按收购单成交价」。
3. **收购单作废时按相反方向扣回**（`SettlementServiceImpl#cancelAcquisition` → `syncPurchaseDeal(acq, -1)`）：来源类型用 `ACQUISITION_CANCEL`（与 `ACQUISITION` 分开，各自幂等），重复作废只扣一次。
4. **幂等**：登记路径不需要额外幂等——收购单本身按 `clientRequestId` 幂等，重复补传返回既有单据、根本走不到写成交；只有作废反冲需要显式查重（`PurchaseOrderService#selectDealsBySource`）。
5. **测试**：`AcquisitionServiceImplTest` 新增 3 例（验收 / 结算分列、离线补传不重复计、作废反冲幂等）。icbc 679 全绿。

> **踩到的坑**：一开始给 `recordDeal` 加了「按来源幂等」，结果把同一张收购单的多次成交 / 退货全挡掉了（`PurchaseOrderServiceTest#testProgress_returnDeductsFromTheChosenMeasure` 红）。成交记录是**追加式**的，同一来源可以有多条；幂等只属于「同一次登记」，而那层幂等已经由收购单的 `clientRequestId` 保证。

> **原先的已知限制已收**（提交 `a5b1d06`）：成交写在登记那一刻，而接收结论（拒收 / 部分接收）可以在这之后改——现在 `recordAcceptance` 会调 `PurchaseOrderService#correctAcquisitionDeal` **就地修正**那条成交的数量（同一笔收购的修正，不追加新成交），订单的「验收」跟着降。于是 `icbc_purchase_order_deal` 的唯一例外就是这条：由收购单产生的成交会被就地修正，其余仍然只追加。

## 整机启动的 bean 名冲突：第五轮之后又冒了两次（2026-09-20 修）

`#45`–`#58` 合并后重启整机，暴露两处**单测发现不了、只有 `spring-boot:run` 才会炸**的注入冲突（与 2026-09-19 那次同类）：

1. **`contractMapper`**：`PurchaseContractServiceImpl`（#45）的 `@Resource` 字段名撞 `yudao-module-contract` 的 `ContractMapper` bean（`@Resource` 先按名字找）。报 `BeanNotOfRequiredTypeException: Bean named 'contractMapper' ... but was actually of type jdk.proxy2.$Proxy...`。已改名 `icbcPurchaseContractMapper`。
2. **`appointmentMapper`**：`WorkbenchServiceImpl`（#56）的字段名撞 `yudao-module-waste` 的 `AppointmentMapper`。已改名 `icbcAppointmentMapper`。

**教训（写进排查清单）**：icbc 侧凡是 `@Resource` 字段名写成通用名（`xxxMapper` / `xxxService`），都可能撞上同仓库里别的模块的同名 bean。**重命名的字段名要带 `icbc` 前缀**（历史已有 `icbcAppointmentServiceImpl` / `icbcAppointmentMapper` 的做法）。

**排查脚本**（本次用的思路，可复用）：扫 icbc 的 `@Resource` 字段名，与其它模块里带 `@Mapper/@Service/@Component/...` 的类的 decapitalize 简单名取交集；再扫 icbc 与其它模块是否有一对同名且都带 spring 注解的类。

**顺带**：跑着的后端 jar 旧了也会让人误判成前端问题——`GET /admin-api/icbc/purchase-contract/page` 在旧进程里是 404，而菜单接口是**读库**的、照样正常，于是「菜单里有、页面打不开」看起来像前端缓存。**改完 icbc/erp 等模块，必须 `mvn -pl yudao-server -am -DskipTests install` 再 `spring-boot:run`**（`AGENTS.md` 已有，但这次又踩了）。

## 物流与司机端：设计已对齐（2026-09-20）

回收企业自营车队 + 第三方承运商的运力作业（车辆、司机、派单接单、运输过程、上门提货、运费）已完成一轮设计对齐，决策落 **ADR 0030 / 0031 / 0032**，词表补了「物流与运力」一节（车辆 / 司机 / 调度 / 承运商 / 承运合同 / 运输任务 / 停靠点 / 运输节点 / 交接确认 / 交接登记 / 轨迹 / 运费），并修订了「收购单」「场站」「交接批次」与「货物流」（「物流」现在指**承运业务**，与**货物流**是作业与证据两件事）。本节记的是**设计**；实施进度见下一节 V1 起。

要点：

1. **物流独立成模块**（ADR 0032）：不建在 icbc 内、也不复用 vendored 的 `yudao-module-logistics`；物流不编译依赖 icbc，只存 icbc 侧编号，追溯反向由 icbc 拉物流的节点与凭证。
2. **`yudao-module-waste` + vendored `yudao-module-logistics` 从 pom 停编、代码留库**（ADR 0032 第 4 条）。已核实：除 icbc 里两处**注释**外，没有别的模块引用这两个模块的类，停编不会造成编译失败（停编后必须重跑 `mvn -pl yudao-server -am -DskipTests install` 与整机启动验证）。顺带说明：icbc 侧为绕开 bean 名冲突做的改名（`icbcAppointmentServiceImpl` / `icbcAppointmentMapper` / `icbcPurchaseContractMapper`）**保留不回退**；icbc 的单测上下文 `UnitTestConfiguration` 只 `@ComponentScan("cn.iocoder.yudao.module.icbc")`，本来就不加载其它模块的 bean。
3. **司机端是独立 uni-app 工程**，与收货员现场端共享基建与「自然人准入四步」页面能力（抽共享包）；司机角色**不进 PC 菜单**，PC 新增一级「物流管理」（车辆 / 司机 / 承运商与承运合同 / 运输任务 / 节点与凭证 / 运费对账）。
4. **司机现场只做两件事**：引导自然人完成准入四步（ADR 0030），登记交接（品类 + 参考量 + 参考单价 + 凭证）。**结算确认一律不代点**；缺要件可先收货，状态「待补档」，付款与开票被门禁拦住。
5. **回场复磅才定稿**（ADR 0031）：现场不产生金额、不生成收购单；结算单与收购单归**派单场站**，实际提货地址另存。一车多停靠点各自交接、各自复磅、各自结算。
6. **一任务即一次执行**（一车一司机，含若干停靠点），一期不建独立「车次」实体。
7. **轨迹**一期只有上报时的一次性定位快照；演示用模拟轨迹必须标记来源且**不进一票一档**。
8. **运费**是另一笔账（对承运商），一期到「承运合同 + 对账 + 外部付款凭证登记」，**不做**对公付款通道（ADR 0006）；不改变收购单金额。
9. **不做**司机现场现金收购与现金代付（原 vendored 的 `TemporaryOrder` / `CashAdvance`），不因没带卡而丢货、也不垫资（ADR 0006 / 0010）。

票据：**#59** 规格票（父）+ 垂直切片 **#68–#76**（V1 模块骨架与依赖方向 / V2 一趟活跑通 / V3 档案与派车门禁 / V4 运输过程做全 / V5 多停靠点集货 / V6 交接登记→回场复磅→收购单 / V7 司机端现场准入四步 / V8 承运合同与运费对账 / V9 轨迹演示件）。原生 sub-issue 与 `blocked_by` 已挂。

> 先发的横切版子票 #59 下的 #60–#67（T20–T27）**已关闭**，各自在关闭评论里注明替代票：后端一票、前端一票、权限一票、纯接线一票不符合 tracer bullet 要求。映射是 #60→#68、#61→#69+#70、#62→#69+#71+#72、#63→#69+#74、#64→#73、#65→#75、#66→#69、#67→#76。#59 正文的子票清单因此已过期，以原生 sub-issue 链接为准（已在 #59 留评论说明）。
### V1 #68 物流模块骨架与依赖方向（已完成，分支 `v1-module-skeleton`）

纯 prefactor，交付的是地基而非功能：

1. **新模块就位**：`backend/yudao-module-logistics/`（`-api` + `-biz`），artifactId 与停编前同名，所以 `backend/pom.xml` 的 `<module>yudao-module-logistics</module>` 与 `yudao-server/pom.xml` 的依赖行**都不用动**，只是指向了新代码。错误码段取 **`1_030_200_xxx`**（icbc 占 001~050、erp 占 100~1xx）。
2. **读面定死**：`LogisticsTransportApi` 只有两个查询轴——按任务编号取节点、按交接批次编号取凭证；契约是「查不到（含入参为 null）返回**空列表**，不返回 null、不抛异常」，因为缺凭证是业务状态而不是错误（自送的货本来就可能没有运输节点，ADR 0031 也不因缺轨迹拒收）。实现是**有意留空的骨架**，真实查询由 #69 / #73 补。
3. **停编 + 留库**：vendored 的 `yudao-module-waste` 与快照版 `yudao-module-logistics` 移到 **`backend/legacy/`**（新增 `legacy/README.md` 写清为什么不能加回 reactor），从两个 pom 摘掉。移走而不是原地保留，是因为新模块要用同一个目录名与 artifactId。
4. **测试底座**：`UnitTestConfiguration`（H2 + `generateUniqueName(true)`）+ `application-unit-test.yaml` + `sql/{create_tables,clean,init_data}.sql`，另有 `LogisticsBoundaryTest` 用源码扫描把 ADR 0032 锁住（物流不得出现 icbc 的类、工行 SDK/网关地址/客户端痕迹，pom 不得依赖 icbc 工件）——手法同 icbc 的 `IcbcSeamBoundaryTest`。

**验收实测**（在 worktree 里跑）：物流模块 9 个测试全绿；icbc **682 个测试全绿**（1 skip 是 live 测试）；`mvn -pl yudao-server -am -DskipTests install` 通过；`spring-boot:run`（48081，避开占用的 48080）启动成功，`/actuator/beans` 里能看到 `logisticsTransportApiImpl`，登录接口正常；`dependency:tree` 里**没有** `waste`，只有 `logistics-biz → logistics-api`。

**这一票踩到的三个坑（下一票照做）**：

1. **`-pl yudao-module-logistics -am` 只构建父 pom**，子模块根本没装进 m2——这正是 `AGENTS.md` 记过的坑，我又踩了一次。装子模块要用全路径：`-pl yudao-module-logistics/yudao-module-logistics-api,yudao-module-logistics/yudao-module-logistics-biz -am`。
2. **`~/.m2` 里早就躺着 vendored 版的两个 artifact**（`yudao-module-logistics-api` 是 1.9KB 的空壳 jar、`-biz` 是 308KB，都是 9 月 18 装的）。停编并不会把它们从 m2 清掉：**如果不先把新模块 install 一遍就 `spring-boot:run`，Maven 会静默解析到旧的 vendored jar**，表现是「物流模块像不存在」或类找不到。改完新模块必须 `install` 再起服务（与 icbc/erp 同一条纪律）。`yudao-module-waste-biz` 的旧 jar 也还留在 m2，但没有模块再声明它，不会被拉进来。
3. **Spring 拒绝「只含注释」的 SQL 脚本**（`'script' must not be null or empty`）。V1 还没有业务表，`create_tables.sql` / `clean.sql` 里各留了一条 `SELECT 1;` 占位（文件里已写明首次真实建表时删掉）。新增表的票别忘了给 `clean.sql` 补 `DELETE`。

**V2 又拆了一次（同日）**：#69 按「一张票要能在一个上下文窗口里做完」拆成三张垂直切片——**#77（V2a 车辆与司机最小档案 + 物流域菜单与权限机制）→ #78（V2b 运输任务与节点）→ #79（V2c 司机端工程与接单）**。依据是实测：ADR 0026 之后权限行与角色-菜单由同步服务从枚举幂等生成，而物流不得依赖 icbc（ADR 0032），这套要在物流侧从零镜像一套；司机端又是一整个新工程；再加上 4 张表，原票是其他票的 3–4 倍。下游依赖已改接：#70 → #77，#71 / #72 / #73 / #76 → #78，#74 → #79。

**物流的菜单 ID 不要用固定段**：原本打算用 5400–5499，但 V2a 实测发现固定段本身就不可行——`system_menu` 里同步服务插的权限行是**自增 id**，而显式 id 插入并不推高 InnoDB 计数器（本地库实测 `MAX(id)=5328` 而 `AUTO_INCREMENT=5210`），任何固定段都会与下一次自增撞号。物流改为「按标记删 → 自增插 → `LAST_INSERT_ID()` 串父子 → 子树 id 并进套餐」，见 `backend/sql/mysql/logistics-menu.sql`。另外**无论用哪一段都不能是 5300–5399**：`icbc-menu.sql` 会 `DELETE ... BETWEEN 5100 AND 5399`。

frontier 随之推进到 **#77（V2a）**。

### V2a #77 车辆与司机最小档案 + 物流域菜单与权限机制（已完成）

物流第一次有用户可见的东西：能建车、建司机，并从 PC 后台的「物流管理」进去。

1. **档案两张表**：`logistics_vehicle`（车牌、类型、载重、状态）、`logistics_driver`（关联租户内系统用户、姓名、手机号、自有/承运商来源、状态）。**唯一性只在 Service 层校验**（车牌租户内唯一、一个用户一份司机档案），因为两张表都是逻辑删除，建 DB 唯一键就「删掉的车牌再也建不回来」——理由写在 `logistics-vehicle-driver.sql` 文件头。车辆状态里「运输中」不接受手工设置（由运输任务驱动）。
2. **物流域自己的权限机制**：`LogisticsPermission`（11 条：车辆 5 + 司机 5 + 本租户角色初始化 1）、`LogisticsRoleEnum`（管理员 / 调度 / 司机，司机本票无权限、待 V2b 补）、`LogisticsPermissionSyncService` + 开机 runner（`@Profile("!unit-test")`）+ `POST /logistics/permission/init`。与 icbc **完全各建各的**（ADR 0032），互不依赖。
3. **菜单**：`logistics-menu.sql` 落一级「物流管理」+ 车辆档案 / 司机档案，并并进「回收企业套餐」（200）。按钮权限行由同步服务生成，不手写 SQL。
4. **PC 页面**：`views/logistics/vehicle|driver/index.vue` + `api/logistics/*`。

**验收实测**：物流模块 27 个测试全绿（V1 的 9 个 + V2a 的 18 个）；icbc **682 个测试全绿**；`yudao-server` install + 启动通过；实测接口链路为——runner 启动即建 11 条权限行、`init` 建 `logistics_dispatcher`（2 个菜单）/`logistics_driver`（0 个菜单）且**再跑一次全 0**、菜单树出现「物流管理 → 车辆档案 / 司机档案」、建车返回 id、重复车牌回 `1030201001`、手工设「运输中」回 `1030201002`、司机重复用户回 `1030202001`。前端 `vue-tsc` 下**新增文件零错误**（仓库既有基线 1254 条，都是 vendored 的 erp/mall/pay/crm 视图）。

**这一票踩到的坑（后续票照做）**：

1. **`system_menu` 不能用固定 ID 段**：实测本地库 `MAX(id)=5328` 而 `AUTO_INCREMENT=5210`——显式 ID 插入并不推高 InnoDB 计数器，而同步服务插的权限行是自增的，任何固定段（比如原计划的 5400–5499）都可能与下一次自增撞号。物流改为：**按标记删 → 自增插 → `LAST_INSERT_ID()` 串父子 → 子树 id 并进套餐**。icbc 那套 5100–5399 固定段是历史包袱，别照抄。
2. **菜单脚本有两个「越导越多」的坑，都已处理**：① 根菜单的 `permission` 是空串、`component` 是 NULL，只用 `permission LIKE 'logistics:%' OR component LIKE 'logistics/%'` 会**漏掉根节点**，于是每次导入多出一棵树——要从**所有** `path='/logistics'` 的根递归展开来认领旧树（这样也能自愈已脏的库）；② 套餐菜单用「并集」会**累积已删除的 id**，要改成「丢掉 system_menu 里已不存在的 id」再整体写回。
3. **H2 测试建表脚本里 `deleted` 必须是 `BOOLEAN`**：单测配置是 `logic-delete-value: true`，写成 `TINYINT` 会在每次查询炸 `Values of types "TINYINT" and "BOOLEAN" are not comparable`。
4. **PC 前端做类型检查要先补两样**（worktree 里没有）：`node_modules`（可软链主工作树）与 `src/types/auto-imports.d.ts` / `auto-components.d.ts`（vite 插件生成物，被 gitignore），并且要 `NODE_OPTIONS=--max_old_space_size=8192`，否则 vue-tsc 直接 OOM。**本仓库的 admin 前端本来就有 1254 条类型错误**（vendored 的 erp/mall/pay/crm/ai），所以判断标准只能是「新增文件零错误」，不是「ts:check 通过」。

> 本地库里留了 V2a 的演示数据（车辆「浙A88888」、司机「张三」、物流菜单与两个角色），要在页面上删掉随时可以。

frontier 推进到 **#78（V2b 运输任务与节点）**。

### V2b #78 运输任务与节点（已完成）

物流的第一条完整作业链：**派一趟活 → 上报起运 → 看时间线与断点**。

1. **两张表**：`logistics_transport_task`（一车 + 一司机 + 一次执行，含状态机、时间窗、采购安排快照、车牌与司机快照）、`logistics_transport_node`（过程事实与货物流凭证：发生时间与上报时间**分开记**、位置、照片、幂等键）。
2. **状态机只在一处**：`LogisticsTransportTaskStatusEnum#canTransitTo`（待分配 → 已分配 → 已接单 → 执行中 → 已完成；任意非终态 → 已取消且必填原因；同状态视为幂等）。**状态推进只经 `transitStatus` 一条路**，非法推进一律报错而不是静默忽略。
3. **派车与释放车辆是「运输中」的唯一出入口**：`LogisticsVehicleService#occupyByTask / releaseByTask`——档案 CRUD 拒绝手工设置该状态（V2a 立的规矩在这里闭环）。维修中的车不会因为跑完一趟就变回可用。
4. **只开放「起运」一类节点**（其余四类与异常见 V4 #71），写入按 `clientRequestId` 幂等，起运把任务推进到「执行中」并把 `startTime` 落在**发生时间**上。
5. **读取面落地**：`LogisticsTransportApiImpl#getNodeListByTaskNo` 由骨架换成真实查询，契约不变（空列表而非 null、按发生时间正序、带车牌与司机快照）；按交接批次取凭证仍留空，归 V6 #73。
6. **PC 页面**：`views/logistics/task/index.vue`（列表 + 派车 / 接单 / 完成 / 取消 + 详情抽屉里的时间线与断点 + 补录节点）。

**验收实测**：物流模块 **53 个测试全绿**（V2a 的 27 + V2b 的 26）；icbc **682 全绿**；install + 启动通过；接口链路实测——不带车与人建任务落到「待分配」、**未派车就上报起运被状态机拦住**（`1030200002`）、派车后车辆变「运输中」、接单 → 上报起运（发生 10:30 / 上报 21:01 分开留痕，`startTime` 取发生时间）→ 任务「执行中」、同一 `clientRequestId` 重复上报返回同一 nodeId、上报非起运类型回 `1030203000`、完成或取消后车辆回到「可用」、详情里的断点正确排除已上报的起运；菜单连导两次稳定（4 个菜单、套餐 4 个 id、0 悬挂）；前端新增文件 `vue-tsc` 零错误。

**这一票踩到的坑（后续票尤其注意第 1 条）**：

1. **`LocalDateTime` 在协议上是毫秒时间戳，不是字符串**。框架用 `TimestampLocalDateTimeDeserializer`（只认 `Long`），传 `"2026-09-20 10:30:00"` 不报错，**静默变成 1970-01-01 08:00:00** ——数据库里躺着一个 1970 的时间，接口一切正常。前端一律写 `value-format="x"`（仓库里已有 35 处先例，含 icbc 页面），后端用毫秒值自测。查询参数（URL 里的范围查询）是另一条路，仍用 `@DateTimeFormat` 字符串，两者不要混。
2. **派生字段在 `get` 与 `page` 两个出口都要补**：`statusName` 我只在 `page` 里填了，详情抽屉会显示空白。列表与详情走不同方法时，这类「前端直接显示的派生文案」最容易漏一边。
3. **测试抓到的设计确认**：没派车就上报「起运」会被状态机拦住（待分配不能到执行中）。这是对的——半套流程不该被写进事实里；当时是测试建的任务没派车，改的是测试而不是状态机。

frontier 推进到 **#79（V2c 司机端工程与接单）**。

### V2c #79 司机端工程与接单（已完成）

司机第一次有了自己的端：**在自己手机上看到派给自己的活、接单、上报起运**。

1. **独立 uni-app 工程** `backend/yudao-ui/yudao-ui-driver-uniapp`（端口 5174，本地存储键前缀 `driver_`），与收货员现场端**各自构建、各自发布**（ADR 0032 的取舍）。共享基建（request / upload / auth / main.ts / tsconfig）是**复制**过来的；**要抽共享包时放 `backend/yudao-ui/packages/field-shared/`**（README 写明了位置与代价），本票不做这次抽取。
2. **司机端接口** `/logistics/driver-app/*`（profile / task page / task get / task accept / node report），走 `/admin-api` + token，与现场端同一套鉴权（ADR 0016）。**没有金额入口**。
3. **归属校验只有一个落点**：`LogisticsDriverAppService` 先按登录账号找司机档案，再强制把范围限到「派给我的任务」——权限位只回答「他能不能用司机端」，「这是不是他的任务」由这里回答；入参里的 `driverId` 一律不信。他的入参里塞别人的 driverId 也翻不到别人的活。
4. **权限补齐**：`LogisticsRoleEnum.DRIVER` 拿到 `logistics:driver-app:task:query|accept` 与 `node:report` 三条，**不进 PC 菜单**；后台接口对司机一律 403。
5. **弱网**：上报失败时把请求体与照片（base64）一起暂存本机，任务列表顶部出现「待补传」入口，补传按 `clientRequestId` 幂等（可放心重试）。
6. 任务表加 **`cargo_name` / `estimated_quantity` / `quantity_unit`** 三个字段（增量 SQL `logistics-transport-cargo.sql`）：给司机看的「这趟去拉什么」。**是计划提示，不是品类权威**——权威品类在交接登记与收购单上（ADR 0028），物流不引用 icbc 的品类配置（ADR 0032），所以只能是文本 + 约量，页面与接口文案都写明了。

**验收实测**：物流模块 **60 个测试全绿**（V2b 的 53 + 司机端归属校验 7）；icbc **682 全绿**；install + 启动通过；`pnpm ts:check` **零错误**、`pnpm build:h5` 构建通过；接口链路用**真司机登录态**实测（建了 `driver01` 用户 + 分配司机角色）——profile 认得出司机、任务列表只列自己的、取别人的任务回 `1030200007`、司机调后台派车接口 403、接单、上报起运（位置快照 + 照片 + 报人=司机张三）后任务变「执行中」且时间线正确。

**这一票踩到的坑**：

1. **`--` 后必须跟空格才是 MySQL 注释**。我写了一行 `--（与 …` （中文全角括号紧跟 `--`），MySQL 当成语句直接报 1064。文件里现在留了注释说明；所有 `logistics-*.sql` 都不该再出现 `^--[^ ]` 的行。
2. **手工造验收夹具时别忘 `tenant_id`**：我用 SQL 直接插 `system_user_role` 给司机分配角色，漏了 `tenant_id`（默认 0），框架按租户过滤 → 司机成了「没有角色」，表现为 `get-permission-info` 里 roles 为空、司机端全部 403。**产品路径（后台分配角色）没这个问题**，是夹具的坑；排查时先看 roles 有没有，别急着怀疑权限注解。
3. **`pages.json` 里的 `enablePullDownRefresh` 要与页面里的 `onPullDownRefresh` 配套**：少一个，下拉刷新就是句空话（页面提示写了「下拉刷新」但拉不动）。

frontier 推进：司机端这条线只剩 **#74（V7 现场引导自然人准入四步）**，它被 #79 解锁。另有 #70 / #71 / #72 / #76 原本就绪，#73 需 #71，#75 需 #70。

## 第六轮并行约定（波次 A：#70 / #74 / #76）—— 2026-09-20 定，开工中

五张就绪票里，**#71 与 #72 不能并行**（都改运输任务的 DO/Service/VO 与两个前端），所以并行只在其余的票之间做。波次划分：

| 波次 | 票 | 为什么能/不能并行 |
|---|---|---|
| **A** | **#70 V3 档案做全与派车门禁**、**#74 V7 司机端准入四步**、**#76 V9 轨迹演示件** | 改动面基本不相交：一个动后端档案与 PC 档案页、一个动司机端、一个动 PC 任务详情 |
| B | #71 V4 过程做全 → #72 V5 多停靠点 | 两者都改 task 模型与两个端，串行 |
| C | #73 V6（需 #71）、#75 V8（需 #70） | 一个动 icbc 侧交接/收购接线，一个动承运商运费，互不相交 |

**「脊柱文件」与段位约定**（这些文件每票都会碰，先分段才不会撞）：

| 项 | #70 | #74 | #76 |
|---|---|---|---|
| 错误码段 | 车辆 `1_030_201_1xx`、司机 `1_030_202_1xx`、承运商 `1_030_205_xxx` | `1_030_202_3xx` | `1_030_206_xxx` |
| 权限常量 | 车辆/司机 CRUD、承运商 CRUD | `logistics:driver-app:onboarding:*`（**司机端入口权限**；真正的准入接口权限在 icbc 侧，见 ADR 0033） | `logistics:demo:track:query`（演示件单独权限，便于一键关掉） |
| 菜单行 | 承运商档案（sort 30） | 不加菜单 | 不加菜单（入口在任务详情里） |
| handoff | 各写各的 worktree，**合并时按 V3 / V7 / V9 三段收口** | | |

其余脊柱文件（`LogisticsPermission` / `LogisticsRoleEnum` / `ErrorCodeConstants` / `sql/mysql/README.md` / `logistics-menu.sql` / 测试 `create_tables.sql` · `clean.sql`）一律**只追加、不重排**；同时用到 `-api` 与 `-biz` 时走一条 reactor 命令（`~/.m2` 是各 worktree 共享的）。

**本轮新增一条 ADR**：[ADR 0033](docs/adr/0033-司机端的准入能力由-icbc-侧按角色授予.md) —— 司机端的自然人准入能力由 **icbc 侧按角色 code `logistics_driver` 授予**。这是 #74 的前置决策：物流不出现任何 `icbc:*` 权限常量（ADR 0032 的边界测试继续守），icbc 侧只知道一个角色 code。

### V3 #70 档案做全与派车门禁（已完成）

1. **档案补全**：车辆加行驶证 / 保险到期日、照片、GPS 设备号；司机加驾驶证与从业资格证号码 / 类型 / 到期日、所属承运商；新增**承运商档案** `logistics_carrier`（停用而非删除：历史任务的司机与运费要留着）。
2. **门禁分硬软**（这是本票的核心取舍）：
   - **软门禁（证件过期）**：不能派车，但管理员可带原因**授权放行**（`PUT /assign-override`，只挂 `logistics:transport-task:override` 权限），原因 / 授权人 / 时间落到任务上。**没有这条留痕就不放行。**
   - **硬门禁（车辆维修中、司机离职、承运商已停用）**：不可绕过；走授权路径会明确回「该情况不能授权放行」——那不是流程不便，是无证运营。
   - **当天到期仍算有效**（过期 = 早于今天），否则当天换证的车就出不了门。
   - 创建路径（带车带人建任务）**不做**授权放行：要带过期证件出车，先建任务再走 `assign-override`，让授权那一步显式发生。
3. **到期提醒与门禁共用同一套「过期」判定**（`isDocumentExpired` 只有一处），并有一条测试专门钉住两者结论一致——两处各写一套就一定会出现「提醒说没事、派车被拦」。
4. **导出**：车辆 / 司机 / 承运商三个 `export-excel`；**分页与导出共用同一个条件构造**（`buildQuery`），避免「导出和列表看到的不是一回事」。
5. **PC**：车辆 / 司机页补字段（含上传照片用独立 ref 的做法，与 `purchaseContract` 一致）、到期列标红、导出按钮；新增承运商页；派车弹窗加「授权放行」勾选与原因；工作台加「物流证件到期」卡片（走物流自己的接口，不塞进 icbc 的聚合预警）。

**验收实测**：物流 **74 个测试全绿**（+14）；icbc **682 全绿**；install + 启动通过；`vue-tsc` 新增文件零错误（基线仍 1254）；接口链路实测——承运商建档、承运商来源司机不带承运商回 `1030202101`、证件过期正常派车回 `1030201100`、授权放行不带原因被参数校验挡住、带原因成功且留痕（原因 / 授权人=1 / 时间）、**维修中的车走授权路径回 `1030200009`**、到期提醒列出已过期证件、三个导出端点都返回 200 与 xls。

**这一票踩到的坑**：

1. **`LocalDate` 在协议上被序列化成 JSON 数组** `[2026,9,17]`。本项目此前**没有任何 LocalDate 字段上过接口**（icbc 全是 LocalDateTime，走 epoch 毫秒的定制序列化器），所以没有既有约定可抄。已在字段上加 `@JsonFormat(pattern = "yyyy-MM-dd")`，把日期定成 ISO 字符串（前端日期选择器与比较都按字符串来）。**后续票的 LocalDate 字段照此办理**，否则日期列会显示成方括号。
2. **`logistics-menu.sql` 会连带删掉同步服务建的按钮权限行**（它按 `permission LIKE 'logistics:%'` 清场，与 icbc-menu.sql 同一手法）。重导菜单后各角色会短暂失去物流域授权（表现是 403），**必须补调一次 `POST /logistics/permission/init`**。已写进 `backend/sql/mysql/README.md`，建议并进部署脚本。
3. **`--` 后必须跟空格**这个坑两票各踩一次，所以加了 `LogisticsSqlConventionsTest` 用测试守住，不再靠记性。

frontier 推进：**#75（V8 承运合同与运费对账）** 已被 #70 解锁（它需要承运商档案）。就绪的还有 #71 / #72 / #74 / #76。

### V9 #76 轨迹演示件（已完成）

物流的**演示件**：任务详情里能看到一条模拟轨迹与时间回放。这一票的验收不在「线画得好不好看」，
而在**隔离**——演示数据一个字节都不能进证据链。

1. **不落库**：`LogisticsTrackDemoServiceImpl` 里没有任何 mapper / insert；接口路径也刻意带 `demo`
   （`GET /logistics/demo/transport-track`），免得将来有人把它当「轨迹接口」接进证据链。
2. **不进读取面**：icbc 一票一档消费的 `LogisticsTransportApi` 只读真实节点表，演示产出永远到不了那里。
3. **一键关闭**：`logistics.demo.transport-track.enabled`，**默认 false**（演示能力不该在自己会出现的地方出现）；
   关掉时接口返回 `enabled=false` 且**连点都不给**，页面退化成一句说明。
4. **一眼可辨**：响应里 `source=SIMULATED` + 每个点 `simulated=true` + 自带说明文案；页面上是橙色虚线 +
   「演示数据」角标 + `note` 原文——税总 5 号公告第十七条把「运输发票或凭证」列为真实性材料，
   在证据链里放假路径违反 ADR 0021，所以它必须看起来就不像真的。
5. **确定性**：插值按「任务编号 + 段号」做种子，同一个任务每次是同一条线（否则演示时每刷新一次换一条路，看着就像坏了）。
6. **锚在真事实上**：锚点取**真实上报过的**节点经纬度，只在锚点之间插值；锚点不足时用固定演示原点，
   页面会显示「无锚点：连起点都是编的」。
7. **不引地图服务**：SVG 把经纬度线性映射到 0–100 自绘（高德/腾讯要 key 与报备）。它证明不了任何事，
   所以也不该被当地图用。

**验收实测**：物流 **81 个测试全绿**（+7 条演示测试，其中大半是**否定式**断言：跑完节点表没多行、读取面看不到它、
关掉就没有点）；icbc **682 全绿**；启动通过（`--logistics.demo.transport-track.enabled=true` 证明配置名对）；
`vue-tsc` 新增文件零错误；接口实测——`enabled=true` / `source=SIMULATED` / 7 个点全带标记 /
同一任务两次同一条线 / **节点表 3 → 3（演示数据没写进证据链）** / 带经纬度节点的任务锚点为 1 且坐标就是上报的那两个。

**一个如实的限制**：目前只有「起运」一类节点能上报（其余四类归 #71），所以一趟任务**最多一个锚点**，
而插值需要 ≥2 个锚点——也就是说**锚点之间插值那条路现在跑不到**，画出来的是从单个锚点出发的回退段。
等 #71 开放四类节点后，同样的代码会自动画出「起运 → 到达场站 → 卸货完成」之间的插值路径。

> 演示开关默认关闭。要看演示效果，起服务时加 `--logistics.demo.transport-track.enabled=true`（或写进 `application-local.yaml`）。

frontier 推进：**#74（V7 司机端现场引导自然人准入四步）** 与波次 A 之外的四张（#71 / #72 / #73 / #75）就绪。

### V7 #74 司机端现场引导自然人准入四步（已完成，含一处如实偏差）

司机上门时现场没有收货员（ADR 0030），所以自然人**准入四步**要在司机的手机上完成。

1. **ADR 0033 落地**：icbc 的权限同步在同步角色时，**按角色 code `logistics_driver`** 把准入所需权限授予司机角色
   （若该角色不存在则跳过，顺序不是硬约束）。清单只有 6 条：建自然人档案、查档案、实名/入驻、框架协议、
   首次授权、签发一次性令牌——**没有**收购登记、结算确认、付款、开票。有 3 条 icbc 侧测试守着这份清单「要窄」。
2. **共享包** `backend/yudao-ui/packages/field-shared/`：准入四步的**接口**（onboarding / payee / publicToken）、
   **工行表单承载**（icbcForm）、**业务逻辑 composable**（`useSellerOnboarding`：接口编排、状态、协议必填校验、
   失败留联系方式兜底）。两个端都以**源码**方式消费（Vite 数组别名 + tsconfig paths），源码只有一份。
3. **司机端**：新增「为出售者建档」页（带档 / 新建 → 四步 → 把建档链接交给本人），任务详情加入口。
   现场端那三个 `api/*.ts` 改为对该共享包的再导出（调用方不动）。
4. **不代点确认**：司机端只有「生成并转达链接」（签发 `ONBOARDING` 一次性令牌），**没有任何「替出售者确认」的入口**；
   四步里人脸与绑卡由本人在场完成。

**一处如实偏差（模板没共享）**：票面要求「不复制粘贴」，但**跨工程共用一个 `.vue` 在本工具链下做不到**——三条路都试过并失败：
① 放 `packages/` 用别名引入 → `vue-tsc` 满屏 `Property 'xxx' does not exist on type '{}'`（文件在 app 目录外时不生成 SFC 类型）；
② `node_modules` 里用 `file:` 依赖 → TS 不从 `node_modules` 读 `.ts` 源码，得给共享包加构建产物；Vite 也构建不过；
③ 软链进各端 `src/` → `vue-tsc` 解析到真实路径，回到 ①。
结论是**共享逻辑、不共享模板**，已写进 `packages/field-shared/README.md`（连 `defineEmits` 用调用签名写法会造成同样症状也记了）。
**剩下的口子**：现场端 `pages/payee/index.vue` 仍在用它自己的那套编排（api 层已共享）；迁移到 `useSellerOnboarding`
是后续小票——留着是因为它当时正在被另一个会话改动，且我无法在这个会话里目视验证它的界面。

**验收实测**：icbc **685 测试全绿**（+3）；两个 uni-app **`ts:check` 零错误 + `uni build` 通过**；
接口与数据实测——`POST /icbc/tenant/role/init` 之后，DB 里司机角色恰好拿到那 6 条 `icbc:*` 权限；
司机调 `GET /icbc/seller-onboarding/get` 通过（code=0），调 `GET /icbc/acquisition/page` 与
`GET /icbc/settlement/page` 均 **403**。

> **一个待查的小现象**：`get-permission-info` 里司机的 `permissions` 只列出 3 条物流域权限，
> 没列出那 6 条 icbc 权限（但 DB 里有、鉴权也放行）。像是该接口按租户菜单包过滤或走了角色-菜单缓存。
> 目前不影响功能（司机端页面不靠权限串渲染），但将来若要用权限串控制前端显隐，得先查清它。

### V4 #71 运输过程做全：五类节点、异常与改派（已完成）

把运输过程做全：五类节点全部开放、双时间与照片必填策略落地、异常成为**独立标记**、改派**保留承接关系**。

1. **五类节点全开**：删掉 V2b 的 `SUPPORTED_NODE_TYPES` 白名单与 `TRANSPORT_NODE_TYPE_NOT_SUPPORTED_YET` 错误码；到达提货点 / 交接完成 / 起运 / 到达场站 / 卸货完成都可上报。仍只有「起运」推进状态（其余四类不推进，状态机只有一处）。
2. **双时间**：`nodeTime`（发生）与 `reportTime`（上报）分存；时间线按 `nodeTime` 正序，补录晚到不倒序业务。「已完成」的任务**仍可补录**（弱网事后补报事实是常态），只拦「待分配」与「已取消」。
3. **照片必填策略写在枚举里**：`LogisticsTransportNodeTypeEnum.photoRequired` 只有「交接完成 / 卸货完成」为 true，服务层照它拦（`TRANSPORT_NODE_PHOTO_REQUIRED`），PC 与司机端也照同一份清单提示。
4. **异常是独立标记**：新增 `LogisticsTransportAbnormalTypeEnum`（八类）+ 节点表异常字段（`abnormal_type` / `abnormal_reason` / `abnormal_resolved_*`）；异常事实**没有节点类型**（`node_type` 放开为可空）。上报走独立入口，**不推进也不回退状态机**（有测试钉住）。解决留痕（谁 / 何时 / 怎么解决）不覆盖。
5. **改派保留承接关系**：新增 `logistics_transport_task_reassign`（原车原人 → 新车新人 + 原因 + 改派人），只追加不覆盖；`reassignTask` 只放行「已分配 / 已接单 / 执行中」，门禁与首次派车一致（证件过期 / 维修中 / 离职都拦），**不改状态机**。首次派车 `assignTask` 仍是「只能派待分配」。
6. **断点**：断点计算抽到 `TransportNodeGaps`（PC 与司机端共用一处，异常事实不参与判定），响应给 `missingNodeNames`（还差哪几步）与 `missingEvidenceNames`（哪一步有记录但缺凭证）。
7. **权限 / 菜单**：新增 `logistics:transport-task:reassign` 与 `logistics:transport-node:abnormal:resolve`（管理员 / 调度），**上报异常复用节点上报权限**；按钮权限行由同步服务生成，`logistics-menu.sql` 未动。
8. **落地**：`backend/sql/mysql/logistics-transport-process.sql`（幂等：按 information_schema 判存在再 ALTER，建表用 IF NOT EXISTS；已进 README 导入顺序，并在临时库跑过两遍验证幂等）；测试建表与 `clean.sql` 同步；PC `views/logistics/task/index.vue`（节点类型选择 + 照片必填提示 + 异常上报 / 解决 + 改派 + 断点）与司机端 `pages/task/detail.vue`（五类节点按钮 + 异常上报 + 断点；异常与节点共用弱网草稿队列，`kind` 区分）。
9. **测试**：物流模块 **106 全绿**（新增 / 改写：五类节点全开与存储、补录晚到不倒序、双时间、照片必填两类、未知类型 / 待分配 / 已取消拦截、已完成可补录、异常不改状态机、异常幂等与必填、解决留痕与不覆盖、改派承接记录 + 释放原车 + 不改状态、司机端异常归属）；`LogisticsTransportAbnormalTypeEnumTest` / `TransportNodeGapsTest` 新增；PC `pnpm build:local` 通过且 `vue-tsc` 在 `views|api/logistics` 零新增错误（仓库基线 1254）；司机端 `pnpm ts:check` / `pnpm build:h5` 通过。

> **两处口径说明（留档）**：① **异常的照片不是必填**——AC 的「照片必填策略」只点名「交接完成 / 卸货完成」；异常「带说明、照片与解决留痕」读作有这些字段，页面上仍鼓励拍照。② 节点上报的门禁只拦「待分配 / 已取消」，**已完成允许补录**；对已完成任务报「起运」仍会被状态机拦（那等于重开一趟活）。

> **未做**：改派的**授权放行**（证件过期时中途换车没有逃生门，只能先换证；首次派车有 `assign-override`）；异常清单的跨任务聚合页（只在任务详情里看）；节点级显式上下游（仍由五类顺序表达）。

> **现场端司机 app 的 `pnpm-lock.yaml` 之前没入库**（V2c 建的工程，field/seller/admin 都有），本票 `pnpm install` 时生成，随本票一起提交。

frontier：V4 #71 **完成**。剩余 #72（V5 多停靠点集货，需 #71，与 #71 串行）、#73（V6，需 #71）、#75（V8，就绪）。

### V5 #72 多停靠点集货：一车提三家（已完成）

把「一趟活可以提多家」做进模型：停靠点成为任务下的子对象，每个点各自推进、各自凭证，且**任何地方都不暗示可以合并结算**。

1. **新表 `logistics_transport_stop`**（一任务多停靠点，`stop_no` 租户内任务级唯一）：停靠类型（提货 / 送货）、出售者（**只存 icbc 侧编号 + 姓名 / 手机号快照**，ADR 0032）、地址联系人、品类 / 约量与计划提示、预计到站时间、**独立状态**（待处理 / 进行中 / 已完成 / 已取消）与取消留痕。
2. **节点归到停靠点**：`logistics_transport_node` 加 `stop_id`。提货相关三类（到达提货点 / 交接完成 / 起运）在**有停靠点的任务**上必须带 `stop_id`，整趟收尾的两类（到达场站 / 卸货完成）不许带；没有停靠点的历史任务按单点口径放行。一条测试专钉「有停靠点却不说是哪一家 → 拦」。
3. **进度按点各自收敛**：停靠点状态由**它自己的**节点驱动（「交接完成」→ 已完成，其余提货节点 → 进行中）；`getStopRespListByTaskId` 返回每个点自己的节点、断点（只看停靠点三类）与缺凭证。
4. **单点取消不影响其它点**：`cancelStop`（必填原因）只改这一个；已取消的点拒绝节点上报；已完成 / 已取消不能再取消。
5. **建任务时一次带多个停靠点**：任务创建 VO 拆出 `LogisticsTransportTaskCreateReqVO`（带 `stops`），**因为响应 VO 也继承保存 VO**，同名不同泛型会与父类 getter 冲突。停靠点为空且提货点地址也为空直接拒（`TRANSPORT_TASK_STOP_REQUIRED`）；有停靠点时任务的 `pickup_address` 取**第一个停靠点**快照（列表仍能看「去哪儿」，权威在停靠点）。后可 `addStop` 追加（终态任务不接受）。
6. **文案与口径**：任务详情 / 列表与司机端都回 `pendingStopCount`（还剩几家没提）与 `scopeNote`（「一次集货不构成把几个出售者合并结算的依据：每个停靠点各自交接、各自复磅、各自结算，整车复磅只核对总运输量」）。
7. **断点计算抽出**：`TransportNodeConverter`（DO→VO，任务详情与停靠点详情共用）与 `TransportNodeGaps`（新增 `missingStopNodeNames`），避免同一节点在两页显示不一样。
8. **落库**：`backend/sql/mysql/logistics-transport-stop.sql`（幂等；建表 + 节点加列 / 索引；临时库跑过两遍验证幂等，`uk_stop_tenant_task_no` 三段组合正确）；测试建表与 `clean.sql` 同步；已进 README 导入顺序。
9. **前端**：PC `views/logistics/task`（新建时的停靠点编辑器 + 详情里的逐点进度 / 逐点上报与取消 / 追加停靠点 / 口径提示）与司机端 `pages/task/detail`（逐点卡片 + 按点上报三类 + 整趟收尾两类 + 异常可挂点；列表显「还剩 N 家没提」）；司机端弱网草稿带上 `stopId`。
10. **测试**：物流模块 **119 全绿**（新增 `LogisticsTransportStopServiceImplTest` 12 例：停靠顺序与快照、逐点独立进度、整趟收尾不进任何点、无 `stopId` / 跨任务 `stopId` / 收尾带 `stopId` 三道拦截、单点取消不影响其它点、已完成不可取消、取消必填原因、追加停靠点、终态不可追加；`LogisticsTransportNodeTypeEnumTest` 补 stop-scoped 一例）。PC `pnpm build:local`、司机端 `pnpm ts:check` / `pnpm build:h5` 通过，`vue-tsc` 在 `views|api/logistics` 零新增错误。

> **口径说明（留档）**：停靠点的「已完成」以**交接完成**为准（货装上了车），不是「起运」（可能还在等别家）；停靠点断点只看三类提货节点，「起运」未报仍会显示为断点。

> **未做**：停靠点级的**改动节点**（只支持追加与取消）；PC 停靠点表单的**出售者下拉**（现在存姓名快照，可选编号；实际挂靠 icbc 出售者档案归 V6 #73 的交接登记）；整车复磅只核对总运输量目前仅靠不提供「整车合计结算」这类字段 / 页面来保证（无专门的「整车复磅」页面）。

frontier：V5 #72 **完成**。波次 A + 串行链 #71 → #72 已收口；剩余 #73（V6 交接登记 → 回场复磅 → 收购单，需 #71）、#75（V8 承运合同与运费，就绪）。

### V6 #73 交接登记 → 回场复磅 → 收购单（已完成）

上门提货的闭环接上了：**司机在现场登记交接 → 磅房按它回场复磅 → 按有效磅次生成收购单**，
两侧靠一条引用挂接、缺身份证 / 银行卡时走「待补档」。设计决策见 **ADR 0034**。

1. **交接登记落在物流**（`logistics_transport_handover`，司机端 `pages/handover`）：品类（icbc 侧
   `goods_config_id` + 名称快照）、参考量、参考单价、凭证照片、要件状态、司机与车辆（引用 + 快照）。
   **不产生金额、不产生收购单**——表里没有金额字段，服务层也不调任何收购 / 结算服务。
   幂等键 `client_request_id`；同一个停靠点只登记一次（重复登记报 `1030207009`）。
2. **回场复磅落在 icbc**：`POST /icbc/handover-batch/intake` 按某条现场交接登记建**交接批次**
   （`logistics_handover_id`、现场参考值、要件状态、司机 / 车辆 id + 快照）并落第一次磅次；
   **场站必填且是派单场站**（`1_030_030_012`），上门地址进 `visit_address` 再落到收购单交易地址。
   同一个 `logistics_handover_id` **幂等**（重放返回既有批次、不产生第二条磅次）。
3. **收购单**：`POST /icbc/acquisition/create` 带 `handoverBatchId`（沿用 #50 的入口）。
   单价**默认取现场参考价**；数量没给时取**结算重量**（计量口径，不是参考量）；参考量 / 参考价被显式改动
   而不填 `referenceFixReason` → `1_030_030_014`。收购单落 `logistics_handover_id` / `driver_id` /
   `vehicle_id` / `reference_*` / `document_status`。
4. **待补档是一条状态**：从交接登记继承到批次再到收购单；开票 precheck 新增 `SELLER_DOCUMENTS`
   项（`待补档：缺身份证` + 补齐办法），付款入口也各拦一道（预开票可能是补档前就存在的历史单据）。
   **不进开票申请、不进台账口径（额度台账派生自票据事实）、不计入额度**；`POST /icbc/acquisition/complete-documents`
   补档放行，留 `document_completed_at/by` 与说明，`document_gap`（当时缺什么）保留不抹。
5. **追溯读到物流**：icbc 编译依赖 `yudao-module-logistics-api`（依赖方向恒为 icbc → 物流，ADR 0032）。
   一票一档多两类货物流附件：**现场交接凭证**（司机拍的照片）与**运输凭证**（运输节点照片），
   按 `logistics_handover_id` 反查物流读取面取回。`getEvidenceListByHandoverId` 返回**该停靠点的提货节点
   + 整趟收尾节点**（卸货完成也是这批货的凭证），不含别家停靠点的节点。
6. **`logistics-api` 的第三个读取轴**：`getHandover(id)`、`getRecentHandoverList()`（icbc 侧过滤掉已建过批次的
   = 磅房的待复磅队列）、`getEvidenceListByHandoverId(id)`。**物流不写 icbc 状态**——「已复磅」只有 icbc 知道。
   为此把 V2b 遗留的 `getEvidenceListByHandoverBatchId` 改名为 `getEvidenceListByHandoverId`（契约不变：空列表不抛异常）。
7. **权限**：物流新增 `logistics:transport-handover:query|manage`（PC 查处 / 代录）、
   `logistics:driver-app:handover:report|query`（司机端）；icbc 侧给司机角色补一条**只读**的
   `icbc:goods-config:query`（交接登记要选权威品类，ADR 0028），司机权限清单 6 → 7 条（`RecyclingPermissionSyncDriverGrantTest` 跟着改，
   并新增「不能有 GOODS_CONFIG_CREATE」的断言）。补档放行复用 `icbc:acquisition:update`。
8. **落地**：`backend/sql/mysql/logistics-transport-handover.sql`（建表）+ `icbc-handover-logistics-link.sql`
   （两表各补一组列，按 `information_schema` 幂等）；两份都进 `README.md` 导入顺序，本地库各导过两遍验证幂等；
   测试建表与 `clean.sql` 同步。
9. **icbc 单测的一个基础设施改动**：`HandoverBatchServiceImpl` / `TraceQueryServiceImpl` 现在注入跨模块的
   `LogisticsTransportApi`，而 icbc 的 `UnitTestConfiguration` 只 `@ComponentScan` 本模块——如果只在个别测试里
   `@MockBean`，**其它每个 icbc 测试类都会在上下文启动时报 `NoSuchBeanDefinitionException`**。已在
   `UnitTestConfiguration` 里注册一个**空实现** `StubLogisticsTransportApi`（它同时是真实业务语义：自送的货
   没有现场交接也没有运输节点），需要断言的用例再各自 `@MockBean` 覆盖。

**验收实测**（本地 MySQL + Redis，全部真接口）：

- 物流 **135 个测试全绿**（V5 的 119 + 交接登记 13 + 读取面契约 3）；icbc **696 全绿**（V4 前的 682 + 新增 14）；
  `mvn -pl yudao-server -am -DskipTests install` 通过；`spring-boot:run` 启动成功。
- 全链路实测：建「一车两家」任务（车 1 / 司机 5）→ 两家各登记交接（一家 `COMPLETE`、一家 `PENDING` 缺身份证，
  参考量 12.5 吨 / 2600 元、8 吨 / 1200 元）→ 重复提交同一 `clientRequestId` 返回同一条 →
  `pending-intake-list` 两家都带现场参考量与照片 → 各 `intake` 一次（18t−5.5t、9t−1t）→ 重复 `intake` 返回同一批次且
  磅次仍只有一条 → 复磅后候选列表清空 → 按批次生成收购单：**单价自动取 2600 / 1200、数量取结算重量 12500 / 8000、
  金额 32,500,000 / 9,600,000**，`logisticsHandoverId` / `driverId=5` / `vehicleId=1` / `documentStatus` /
  `referenceQuantity=12.5` 全部落库，`stationId=1`（派单场站）+ 交易地址为**上门地址**（不是场站名）。
- 门禁实测：改单价 2500 不给原因 → `1030030014`；`pre-check` 对待补档报 `SELLER_DOCUMENTS` 不通过（带补齐办法）→
  `complete-documents` 后同一项转为通过、`documentCompletedAt` 与办理说明留痕；`documentGap` 保留。
- 追溯实测：一票一档列出「现场交接凭证 1」（司机照片）+「运输凭证 到达提货点 / 卸货完成」（节点照片）；
  另一家的收购单只拿到**自己**的现场凭证 + 整趟收尾节点，**不含别人停靠点的节点**。
- 前端：司机端 `pnpm ts:check` 零错误 + `pnpm build:h5` 通过；现场端 `pnpm ts:check` 零错误；
  PC `vue-tsc` 基线仍 **1254**（新增文件零错误，剩余两条是 `acquisition/index.vue` 里本来就有的）。

**这一票踩到的坑（后续票注意）**：

1. **跨模块 API 会让「每个」测试上下文都缺 bean**：icbc 的 `UnitTestConfiguration` 是 `@ComponentScan` 整个模块，
   所以给任一 Service 加一个跨模块 `@Resource` 之后，**所有** icbc 测试都要能拿到那个 bean。只在个别测试里
   `@MockBean` 不够——要么在 `UnitTestConfiguration` 里给空实现（本票的做法），要么逐个测试补 `@MockBean`。
2. **本地库的 logistics 表可能落后于代码**：本票实测时本地库连 `logistics_transport_stop` 都没有（V5 的 SQL 没导过），
   表现是「建任务 500，日志里 Table doesn't exist」。**在本地跑验收前，先按 `README.md` 的顺序把 logistics 那一段 SQL 补齐。**
3. **`mvn -pl yudao-module-icbc/yudao-module-icbc-biz` 单独构建会用到 m2 里的旧 `yudao-module-logistics-api`**：
   logistics-api 改了字段（本票加了 `address`）之后必须先 install 它，否则 icbc 侧编译报「找不到符号」而看起来像自己的问题。

> **未做（如实记下）**：① 司机端交接登记**没有弱网草稿队列**（只有稳定的 `clientRequestId`，重试幂等；节点与异常有草稿队列）；
> ② 物流侧**没有独立的「交接登记」PC 页面**（磅房在 icbc 的交接批次页按候选列表复磅，司机在自己端登记）；
> ③ **停靠点的出售者下拉**仍未接到 icbc 档案（V5 的遗留，现在交接登记里带了 `payeeId` 快照，停靠点本身还是文本快照）；
> ④ 参考量 / 参考价的**修正只留一个原因字段**，没有「改前 / 改后」的逐项历史（原参考值 + 原因都在，够审计但不够细）。

frontier：**#73（V6）完成**。物流这条线只剩 **#75（V8 承运合同与运费对账，就绪）**。

### V8 #75 承运合同与运费对账（已完成）

运力吃紧时找第三方的那笔账：**承运合同定运价与计费方式 → 按趟次汇集运费 → 确认应付 → 登记外部付款凭证**。
运费是**另一笔账**，不改变收购单金额与发票金额（CONTEXT.md「运费」）。

1. **承运合同**（`logistics_carrier_contract`）：有效期、适用线路或**品类**（至少一个，否则运价无从适用）、
   计费方式（按车 / 按吨 / 按公里）、运价、**附加费与其承担方**（JSON：名称 / 金额 / `bearer` 1-承运商 2-本企业）。
   停用而不是删除。品类用 icbc 侧 `goods_config_id` + 名称快照（ADR 0032：物流不引用 icbc 的类）。
2. **承运商运费单**（`logistics_freight_order`，**一趟一张**）：运价与计费方式从**有效期内**的合同快照下来。
   两个数加一条原因表达差异、**不抹平**：`expectedAmount`（基础运费 = 计费量 × 运价，
   `surchargeAmount` = 本企业承担 − 承运商承担）、`actualAmount`（对账确认）、`varianceAmount`（实际 − 应有）与
   `varianceReason`（非零差异必填）。状态机 `待确认应付 → 已确认应付 → 已登记付款凭证`。
3. **自有车不虚造承运商运费**：建运费单时从任务上的司机档案取来源（`driver.source` + `carrierId`），
   不是承运商的车直接报 `1030208003`。路桥 / 燃油等**内部成本**另走 `logistics_transport_cost`
   （`cost_type` 路桥 / 燃油 / 其他 + `bearer` 实际承担方），它不是运费、不进运费单。
4. **付款只登记外部付款凭证**（ADR 0006：不接对公付款通道）：`PUT /logistics/freight/pay` 记凭证号 / 附件 / 实付 / 时间，
   必须先确认应付（`1030208011`），凭证号与附件至少一个（`1030208010`）。
5. **对账汇总**（`GET /logistics/freight/reconciliation`）：按**承运商 + 合同**汇集趟次 / 应有 / 实际 / 差异 / 待确认 / 已登记凭证；
   未对账的趟次按应有计入实际合计（不能因为「还没确认」少算欠款）。口径在服务层一处，不写第二套 SQL。
6. **账本隔离的两道锁**（`LogisticsFreightLedgerBoundaryTest` + `LogisticsFreightServiceImplTest`）：
   `logistics-api` 读取面**不含任何运费类型**（icbc 拿不到），icbc 源码不出现 `logistics.freight` / `LogisticsFreight`，
   运费 DO 不出现收购 / 发票金额字段名；并有单测钉死「运费确认 + 登记凭证后，现场参考价一个字不变」
   （收购单金额 = 结算重量 × 单价 + 调整项，单价正是现场参考价）。
7. **权限与角色**：新增 `logistics:carrier-contract:create|update|delete|query|export`、
   `logistics:freight:create|update|query|confirm|pay|export`、`logistics:transport-cost:create|update|delete|query`；
   `LogisticsRoleEnum` 新增 **财务**（`logistics_finance`：运费查询 / 确认应付 / 登记付款凭证 / 导出），
   调度拿到合同的查询与运费的汇集，管理员全量。菜单加「承运合同」「运费对账」两页。
8. **落地**：`backend/sql/mysql/logistics-freight.sql`（三张表）+ `logistics-menu.sql` 新增两页 + 进 `README.md` 导入顺序；
   测试建表与 `clean.sql` 同步。前端 `api/logistics/{carrierContract,freight,transportCost}` 与
   `views/logistics/{carrierContract,freight}`（运费单 / 对账汇总 / 运输费用三个页签）。

**验收实测**：物流 **156 个测试全绿**（V6 的 135 + 本票 21）；`mvn -pl yudao-server -am -DskipTests install` 通过；
PC `vue-tsc` 新增文件**零错误**（仓库既有基线 1255 条，都是 vendored 视图）。

> **未做（如实记下）**：① 承运合同**没有「按线路 / 品类自动匹配」**——汇集运费时由人显式选合同，
> 选完校验「合同属于这一趟的承运商 + 在有效期内」；自动匹配等真实运价表更全时再做。
> ② 对账汇总是**服务层聚合**（取全量再分组），没有分页；趟次量级大了要改成分页或 SQL 聚合。
> ③ 内部成本（`logistics_transport_cost`）只做了登记 / 改 / 删 / 查，**没有与承运商运费合并成「这趟的成本」报表**。
> ④ 一次集货（一车多家）目前按**整趟一张运费单**汇集（运价按车 / 吨 / 公里本就按趟谈），
> 若要按停靠点拆运费，需要引入停靠点级的计费量。

frontier：**#75（V8）完成**。物流 EPIC #59 的垂直切片 #68–#79 全部落地；父票 #59 可收。

## ⚠️ 一处需要知情的历史问题：`d98f31b` 混进了别人的 WIP

提交 `d98f31b`（消息是「第六轮并行约定 + ADR 0033」，只该含文档）**同时带进了现场端 4 个文件的未提交改动**：
`yudao-ui-field-uniapp/src/{App.vue, pages.json, pages/login/index.vue, uni.scss}`（+512 / −51，登录页改版）。

原因：那一提交是在**主工作树**里用 `git add -A` 做的，把当时主工作树里别人正在改的东西一起暂存了。
**内容完整保留**（没丢），只是提交消息与内容不符。当时的意图是「只提交我改的文档」。

怎么处理由那次改动的作者定：若只是还没写完，接着改就好（已经进 main 了）；
若要把它从那个提交里摘出来，需要 rebase main——那是共享历史，**没有明确指示不要做**。

**教训（下一轮注意）**：主工作树里做提交时**不要用 `git add -A`**，要显式列出本次改动的文件；
或者干脆也开一个 worktree 做文档提交。工作树里的 `git add -A` 是安全的（那里面只有本次改动）。

## #80 收方入驻改走数据接口，废弃页面接口与电子钱包（#83 已落地）

**决策**：收方入驻不再走工行页面接口（`edpopenacct/submit`，那把「入驻 + 电子钱包开户」串在一次调用里），
改走**数据接口** `edpreceive/add`（`businessType=0004`）。电子钱包从字段、状态机、词汇里整体删除。
依据与备选见 **ADR 0035**；`CONTEXT.md` 的「收方入驻 / 智慧清分 / 收款账户变更」三条词条已改到新口径；
研究文档 §3.1 顶部留了作废横幅（带日期的记录不改原文）。

**已完成：`#83` 收方入驻走数据接口，状态机只剩审核一维**

- 端口 `submitPayeeOnboarding` 换成数据接口语义，返回**受理回执**而不是页面表单；请求 DTO 删掉
  `corpNo / camsPublicKey / accountOrgNo / preFillItems / trxChannel / skipImgUpload`，补上
  `accountCode`（是否我行卡）与 `bankName`。**顺带作废**的是上一轮发现的两个页面接口 bug
  （漏上送必输的 `corpNo` / `camsPublicKey`、预填字段名写成 `receiverAccount` 而非 `bindMedium`）——
  它们随页面接口一起没了，不用修。
- 状态机收敛成三态（审核中 / 通过 / 拒绝），删掉「开户 × 审核」四组合；回调只解析 `result`，
  查询带 `auditStatus`；**受理即「审核中」**，不假设同步返回就是通过。
- 幂等：已在途或已通过不再打第二次工行。
- **换卡改走收方修改接口**（`updatePayeeBankCard`）——原计划在 #86，但不在这里做就会重复新增
  （同一个 `outUserId` 工行会拒），已在 #86 留 comment 说明切分细化。
- 后台 `payeeOnboarding` 页面同步：去掉开户状态列、发起入驻不再开窗、加「是否我行卡」。
- **测试**：ICBC 模块 **697 个全绿**；全量 `mvn -DskipTests compile` 通过；PC `pnpm build:local` 通过。
  票里说"7 处 `formHtml` 断言全部改写"，实际改了 4 处、保留 3 处——那 3 处断言的是**实人认证**，
  它仍然是页面接口，保留是对的。

> ⚠️ **两个要点知情**
> 1. **`accountCode` 的缺省值是我定的、没有经过确认**：文档里它必输（`0-非我行用户 / 1-我行用户`），
>    而 spec 没定缺省。当前 `DEFAULT_ACCOUNT_CODE = "1"`（缺省视为工行卡），理由与代价写在常量注释里
>    （猜错的代价是工行驳回入驻，不是默默写错一笔钱）。要改就改这一个常量，或改成前端必选。
> 2. **现场端此刻是过渡态**：第 2 步的「发起收方入驻」按钮仍然存在，点了会**受理成功但不开工行窗口**
>    （不再有页面），要靠「查询结果」看状态。这一页由 **#88** 整体重做（只登记与转达）。

**frontier**：#82（实名回跳）、#83（本票）、#84（字段校验收口）三张已解，#85 / #86 解锁。
`#81`（自然人建档向导：身份证/银行卡 OCR + 电子签章）挂在 #80 之后，**只记议题没有 spec**，
开始前要先走 `/grill-with-docs`。

## #85 实名通过即自动入驻（幂等）（已落地）

**决策**：实名结果落地的两处——异步通知 `handleFaceVerifyNotify` 与主动查询 `syncRealName`——在写入
实名通过后**立即自动发起**收方入驻，全程不需要任何人再点「发起收方入驻」。依据见 ADR 0035 决策 8。

- **幂等**：已有入驻状态（审核中 / 通过 / 拒绝）就不再发起；「通知与查询都到、查询被点多次」只打一次工行。
- 实名是平台级的、入驻是「自然人 × 回收企业」级的：回调没有租户上下文，按自然人主体跨租户取出全部
  收方档案，逐租户发起（与 `handleOnboardingNotify` 同一套定位方式）。
- **失败可见且可重试**：缺卡、缺付方档案或工行调用失败时不回滚实名、也不静默——把原因写进 `auditMsg`
  （前缀「自动发起收方入驻失败：」），状态停在「未发起」，`invoiceBlockReason` 直接展示它；
  人工可在后台「建档」页重试（现有 `submitOnboarding` 入口）。
- **测试**：`SellerOnboardingServiceImplTest` 新增 6 个用例（自动发起、通知/查询去重、未通过不发起、
  已拒绝不再发起、失败可见且可重试）；ICBC 模块 **710 个全绿**，全量 `compile` 通过。

**frontier**：#85 已解。同批待做：#82（实名回跳）、#84（字段校验收口）、#86（换卡，见卡变更）、
#87（钱包概念清除）、#88（现场端重做）、#89（自然人端入口与提醒）。

## #88 现场端：只登记与转达，不做实名（已落地）

现场端从「带着出售者在工行页面上跑手续」变成**只登记与转达**：建档 → 显示「待本人实名」+ 二维码 /
可复制链接 + 只读准入进度。实名只能本人做，入驻自动做（#85）。

1. **现场端 `pages/payee/index.vue` 重写**：删掉「发起实名认证 / 查询结果 / 发起收方入驻 / 查询结果」
   以及**协议与首次授权的编辑表单**（父票 #80 决策 6/15：准入进度只读，现场只做两件事）。
   准入四步的接口与状态改走**共享 composable `useSellerOnboarding`**（#74 留下的「现场端还在用自己的编排」
   这个口子顺手收了）。进度在 `onShow` 重读一次（出售者做完实名回到这一页就能看到），另有「刷新进度」。
2. **二维码**：新依赖 `qrcode`（浏览器走 canvas 渲染成 data URL）。**两个坑**：
   - `qrcode` 依赖 `dijkstrajs`，pnpm 不提升幽灵依赖 → Rollup 解析不到、把它留成产物里的
     `import "dijkstrajs"`（浏览器 404、二维码出不来）。**必须把 `dijkstrajs` 显式写进 dependencies**；
   - **不装 `@types/qrcode`**：它带进 `@types/node`，pnpm 的 peer 解析会重写整份 lockfile 几百行
     （vite 的 peer 后缀全变）。改为在 `src/types/qrcode.d.ts` 里声明用到的那一个 API。
3. **自然人端 `pages/index/index.vue` 加非微信降级**（AC5）：H5 上 `!MicroMessenger` 时，
   实名 tab 不再给一个点了没反应的「去工行页面」按钮，改成「请用微信打开才能做人脸」+ 复制本页链接。
   顺带把那个按钮文案「（实名 / 绑卡）」里的「绑卡」去掉——入驻页 #83 已经没了。
   > **与 #89 有重叠**：#89 的第 3 点（自然人端实名入口 + 结算确认提醒 + 非微信降级）会有更完整的处理，
   > 这里只做了「不让本人点了没反应」这一层，改的是同一个文件，注意别覆盖。
4. **未做（如实记下）**：① 进度不会在页面停留期间自动轮询刷新（要靠 `onShow` 或「刷新进度」）；
   ② 现场端不再能签协议 / 授权，谁在这条链路里补这两步由后续票定（司机端 #74 仍有）。

**验收实测**：现场端 `pnpm ts:check` 零错误、`pnpm build:h5` 通过，产物里没有 `dijkstrajs` 裸外部依赖、
canvas + `toDataURL` 渲染器已打包；自然人端 `pnpm ts:check` 零错误、`pnpm build:h5` 通过。
**本票无后端改动**，故未重跑 icbc 测试（#85 关闭前刚跑过：703 全绿）。

**顺手发现的一个既有 bug（#74 司机端，不在本票范围）**：已单独开票 **#90**。`yudao-ui-driver-uniapp/src/pages/onboarding/index.vue`
用 `overview?.realNameStatus === 1` 判断「已认证」，但 `PayeeRealNameStatusEnum` 里 **1 是「认证中」、2 才是「认证通过」**。
表现是「认证中」被标成绿色「已认证」，且此时**不显示**「发起实名认证」按钮。现场端这次用的是 2（`REAL_NAME_PASSED`）。
成因：#74 只有后端测试，前端在这一处没有断言。

**frontier**：#88 已解。剩余同批待做 **#82**（实名回跳）、**#84**（字段校验收口）、**#86**（换卡）；
#89 现在只被 **#86** 阻塞，**#87** 被 #86 阻塞。

## #82 实名认证：签完回来就看到结果（已落地）

工行实名结果页的「确认」现在跳回我们自己的落点页，**不用再有人点「查询结果」**。

1. **两个跳转地址真的上送了**：`SellerOnboardingServiceImpl.startRealName` 给工行上送 `jumpUrl`（成功）
   与 `failJumpUrl`（失败），两者是同一个令牌拼的、只差 `&from=face-success` / `&from=face-fail`
   （工行文档把这两个字段标为必输，此前一个都没上）。
2. **入口地址的唯一来源**：新增 `SellerAppLinkBuilder`（`service/token`），入口取
   `icbc.notify.seller-app-url`、缺失回退 `icbc.station.entry-url`，令牌与链接形状只有这一份。
   `SellerNotifyServiceImpl`（#36 触达）一并改用它，删掉了自己那套 `resolveSellerAppUrl` 与拼链接。
3. **缺配置当场报错**（AC4）：`requirePayeeLink` 抛 `SELLER_APP_ENTRY_NOT_CONFIGURED`（`1_030_013_010`），
   不再静默带空跳转地址去求工行。**本地 fake 模式也要配 `ICBC_SELLER_APP_URL`（如 `http://localhost:5174`）**，
   否则发起实名会报错；已在 `.env.example` 与 `application-local.yaml` 写明。顺带删掉了随入驻页面一起作废的
   `icbc.seller-onboarding.jump-url` / `fail-jump-url` 两个死配置。
4. **落点页**（自然人端 `pages/index/index.vue`）：工行跳回时带令牌与 `from` 标记，页面打开即
   `syncOnboarding` 主动查一次（这条链路本来就有，无需再点「查询结果」）；`from=face-success` 显示
   「已从工行返回，实名结果已刷新」；失败或 `realNameStatus=3` 时显示「实名未通过 + 原因」，
   按钮文案变「重新发起实名认证」。后端公开状态 VO 补了 `realNameStatus` / `realNameMsg`（原来只有状态名）。
5. **先到先写**（AC5）：`NaturalPersonServiceImpl.applyRealNameResult` 把 **PASSED 当终态**——
   异步通知与主动查询都可能晚到，晚到的失败不许把「通过」改回去（失败仍可通过重试走到通过）。

**验收实测**：ICBC 模块 **712 个测试全绿**（#85 关闭时的 703 + 本票 9：`SellerAppLinkBuilderTest` 4 +
`NaturalPersonServiceImplTest` 2 + `SellerOnboardingJumpUrlRequiredTest` 1 + 实名跳转 1 + 公开状态 1，
另有 `SellerOnboardingServiceImplTest` 因签发令牌补了租户上下文）；全量 `compile` 通过。自然人端
`pnpm ts:check` 零错误、`build:h5` 与 `build:mp-weixin` 都通过。

**这一票踩到的坑**：

1. **改了 `yudao-module-icbc-api` 就不能只跑 biz 的测试**：不加 `-am` 时 biz 用的是 `~/.m2` 里的旧
   api jar，新加的 `ErrorCodeConstants` 常量找不到，接着 javac 中止注解处理，报出一大片**假的 Lombok
   「找不到 getter/setter」**（看起来像 Lombok 坏了）。先 `mvn -pl yudao-module-icbc/yudao-module-icbc-api -am -DskipTests install` 再跑 biz 测试。
2. **单测里签发公开令牌需要租户上下文**：`PublicTokenServiceImpl.mint` 要求 `TenantContextHolder` 非空，
   否则回 `PUBLIC_TOKEN_INVALID`。`SellerOnboardingServiceImplTest` 现在在 `@BeforeEach` 里
   `setTenantId(1L)`、`@AfterEach` 里 `clear()`（与 `SellerNotifyServiceTest` 同一做法）。

**frontier**：#82 已解。剩余同批：**#84**（字段校验收口）、**#86**（换卡）；#89 被 #86 阻塞、#87 被 #86 阻塞。
另有 #90（司机端实名状态判断的枚举值错，本票顺手发现）。

## #84 建档与后台的字段校验收口（已落地）

三处「现场能填进去、工行不收」或「填了也没用」的字段被收口。

1. **职业只能是工行的 15 值字典**：新增 `IcbcOccupationEnum`（api 层，15 值 + 中文名，`ArrayValuable<String>`），
   `PayeeInfoSaveReqVO.occupation` 加 `@InEnum` 校验；后台 `PayeeForm` 的「职业」从自由文本改成 `el-select`
   （选项常量 `ICBC_OCCUPATION_OPTIONS` 在 `api/icbc/payee/index.ts`）。`SellerOnboardingServiceImpl`
   的缺省值 `DEFAULT_OCCUPATION` 改引用 `IcbcOccupationEnum.OTHER.getCode()`（行为不变，仍是 14）。
2. **现场端银行卡号与住址**：`pages/payee/index.vue` 建档前校验——卡号为空 / 非 16–19 位数字拦下；
   住址按工行规则（**不少于 4 个汉字，或不少于 7 个字符**）拦下，各自给出原因。后端
   `PayeeInfoSaveReqVO.bankCardNo` 加了同一条 `@Pattern`（不填仍允许，卡可后补）。
3. **预下单的「交易渠道」清掉**：`InvoicePreOrderReqVO.trxChannel`、后台 `api/icbc/invoice/index.ts`
   的类型、`views/icbc/invoice/index.vue` 里写死的 `trxChannel: '01'` 都删了。它与入驻那个同名字段
   是两套字典，且适配层从未上送（ADR 0035 决策 7）。

**验收实测**：ICBC 模块 **718 个测试全绿**（#82 的 712 + 本票 6：`IcbcOccupationEnumTest` 3 +
`PayeeInfoSaveReqVOValidationTest` 3）；全量 `compile` 通过；现场端 `pnpm ts:check` 零错误、
`build:h5` 通过；PC `pnpm build:local` 通过。

**测试抓到的一件事**：`randomPojo` 背后是 Podam 7.2.11，它有 `BeanValidationStrategy`，会读 JSR-303
注解去造值——加了 `@Pattern(^\d{16,19}$)` 之后，它**造不出**这个字段，`bankCardNo` 变成 `null`，
`PayeeInfoServiceImplTest.testUpdatePayeeInfo_success` 因此断言失败（DO 里是随机串、入参是 null）。
已在该用例里显式给上合法卡号与职业，并在注释里写明原因。**以后给 VO 字段加 `@Pattern` / 字典校验，
先看有没有随机造数的测试会踩到。**

**一处如实说明（未做）**：自然人端 `onboardingFormUrl(token, trxChannel)` 上还挂着一个 `trxChannel`
（`03/05` 那套入驻字典），后端 `writeOnboardingForm` **根本不读它**——是 #83 删入驻页面时留下的死参数。
本票范围是预下单，没动它；它与 #89（换卡 / 自然人端重做）或 #87（词汇清除）同批收掉更顺。

**frontier**：#84 已解。剩余同批：**#86**（换卡）；#87、#89 由 #86 阻塞；另有 #90（司机端实名枚举值）。

## #86 换卡改走收方修改接口（已落地）

#83 已经把换卡的**出站**改成了收方修改数据接口（`updatePayeeBankCard`），本票收掉剩下的三处。

1. **回调按 `operaType` 区分新增 / 修改**：新增 `PayeeOnboardingOperaTypeEnum`（`MODIFY="02"`，
   另有 `isModifyAuditStatus("3")` 对应查询接口的「修改审核中」）。`PayeeOnboardingNotifyHandler`
   读 `operaType` 并传给 `handleOnboardingNotify`；`reconcileOnboardingStatus` 增加 `operaType` 参数，
   **只按它（或查询的 auditStatus=3）决定结果落在收方档案还是换卡单**，不再靠「有没有在途换卡单」推断。
   - 修改结果**没有在途换卡单**时只 `log.warn`，不改写建档状态——以前会被当成新增结果，
     把「本机已取消、工行仍在下发」的修改拒绝，错误地写成整个收方入驻被拒（#86 的动机）。
   - 查询兜底没有 `operaType`，用 `auditStatus=3` 表达同一件事，在 `syncOnboarding` 里映射。
2. **换卡发起侧字段补齐**：`accountCode`（是否我行卡）从入驻提交 VO 挪到**换卡单自己**——
   `PayeeBankCardChangeSaveReqVO` / `IcbcPayeeBankCardChangeDO` 各加一个字段，
   `SubmitBankCardChange` 从变更单取（为空仍按 1-我行用户兜底）；`SellerBankCardChangeReqVO`
   （自然人端）也接上，页面的绑定归 #89。顺带修掉「新卡行名从入驻提交 VO 取」的旧写法，
   免得把首次入驻的旧卡识别结果带到修改上。
3. **回归测试确认无遗漏**：`PayeeBankCardChangeServiceTest`（11 条）与 `PaymentServiceImplTest`
   已覆盖「不允许多张卡 / 审核期间付款挂起 / 被拒后原卡继续有效」；本票补
   `PayeeOnboardingOperaTypeEnumTest` 3 条、`SellerOnboardingNotifyTest` 端到端
   「带 `operaType=02` 的回调只收敛换卡单」1 条、`SellerOnboardingServiceImplTest`
   3 条（修改回调改换卡单 / 无在途变更时不动建档状态 / `getOnboarding` 显「银行审核中」）。

**落地**：`sql/mysql/icbc-bank-card-change-account-code.sql`（幂等 `ALTER`，按 `information_schema`
判存在；建表脚本与测试 `create_tables.sql` 同步成最终形状；已进 `README.md` 导入顺序，在临时库
跑过「列不存在 → 加列」与「列已存在 → 跳过」两种）。顺带把 `icbc-bank-card-change.sql` 头注里
「复用 ONBOARDING 页面」的旧说法改成了收方修改数据接口。

**验收实测**：ICBC 模块 **725 个测试全绿**（#84 的 718 + 本票 7）；全量 `compile` 通过。
本票**无前端改动**（换卡表单与「是否本人行卡」由 #89 绑）。

**一个签名变更（后续票注意）**：`SellerOnboardingService.reconcileOnboardingStatus(...)` 与
`handleOnboardingNotify(...)` 都多了最后一个 `operaType` 参数；直接调用它们的测试已全部跟改。

**frontier**：#86 已解 → **#87（钱包概念清除）与 #89（自然人端重做）同时解锁**；另有 #90（司机端实名枚举值）。

## #87 钱包概念彻底清除（已落地）

#83 / #86 让状态机不再产生这些值，本票把「列、页面、词」一并清掉。

1. **三张表的残留列一起删**（AC 点名的两张 + 付方同名死列）：
   - `icbc_payee_info`：`icbc_medium_id`、`icbc_openacct_status`
   - `icbc_payee_bank_card_change`：`icbc_medium_id`、`icbc_openacct_status`
   - `icbc_payer_info`：同上两个 —— 当初（ADR 0010）降级为「原样透传」，之后**再没有任何代码写过它们**
     （付方回调只带 `payerStatus`），一并清掉，接口里也不再出现「开户状态」。
   落地：`sql/mysql/icbc-wallet-columns-drop.sql`（**不可逆**，幂等 `DROP COLUMN`，按
   `information_schema` 判存在）；三个建表脚本与测试 `create_tables.sql` 同步成最终形状；
   README 导入顺序加了本文件，并注明新库不需要跑它。
2. **代码侧**：`PayeeInfoDO` / `IcbcPayeeBankCardChangeDO` / `PayerInfoDO`（含 test 树里那份同名 DO）
   删字段；`PayeeInfoRespVO` / `PayeeBankCardChangeRespVO` / `PayerInfoRespVO` 删字段；
   `handlePayeeAuditCallback` 去掉 `icbcMediumId` 入参（连带 controller 的 `@RequestParam`）；
   `IcbcNotifyParser` 推断收方回调**不再靠 `openacctStatus`**（只靠 `result` + `outUserId`）；
   三处解释性注释改写（`PayeeOnboardingOutcomeEnum` / `PayeeOnboardingReq` / `PayeeOnboardingStatus`）。
   后台 `views/icbc/payee/index.vue` 删「工行开户状态」列，`api/icbc/{payee,payer}` 类型删字段，
   `field-shared` 的两个 VO 类型删字段。
3. **测试**：`PayeeInfoServiceImplTest` / `PayerInfoServiceImplTest` / `IcbcNotifyParserTest` /
   `SellerOnboardingNotifyTest` 跟改；test 树的 `PayeeInfoMapper.xml` / `PayerInfoMapper.xml`
   resultMap 里的 `icbc_medium_id` / `icbc_openacct_status` 一并删（留着会在用该 resultMap 时报「没有 getter」）。
4. **CONTEXT.md** 的「收方入驻」「智慧清分」词条在 #83/ADR 0035 时就已是新口径（`_Avoid_` 里保留
   被弃用词是刻意的），本票无需再改。

**AC4 实测（在本地已建库上）**：把 `icbc-wallet-columns-drop.sql` 跑在本地 `ruoyi-vue-pro` 上
（六列都在、值全为 NULL）→ 六列消失、既有行数据完整；重启 `yudao-server` **15 秒启动成功**；
`/icbc/payee-info/page`、`/icbc/payer-info/page`、`/icbc/payee-info/bank-card-change/list`、
`/icbc/seller-onboarding/get` 都 200，响应里再无 `icbcMediumId` / `icbcOpenacctStatus`。
幂等在临时库验过「有列→删」「无列→跳过」两种。

**验收实测**：ICBC 模块 **725 个测试全绿**（数量不变，删的是断言不是用例）；全量 `compile` 通过；
现场端 / 自然人端 `pnpm ts:check` 零错误 + 现场端 `build:h5` 通过；PC `pnpm build:local` 通过。

**这一票踩到的坑（重要，下一次也要记得）**：本地库此前**没导过 #86 的
`icbc-bank-card-change-account-code.sql`**，新构建起来后 `/icbc/payee-info/page` 直接 500
（`Unknown column 'account_code'`）。补跑那条增量后正常。**在本地跑验收前，先把近期
`backend/sql/mysql/*.sql` 的增量按 README 顺序补齐**——handoff 已经警告过「本地库落后于代码」，
这次是同一个坑。

**frontier**：#87 已解。只剩 **#89（自然人端重做）** 与 **#90（司机端实名枚举值）**；#80 的子票收完即可收父票。
