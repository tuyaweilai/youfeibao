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

## 管理后台菜单现状

- 反向开票：出售者档案 / 付方档案 / 开票申请 / 付款 / 发票下载与证据
- 租户开票就绪：企业信息 / 企业资质 / 开票就绪自检 / 三层资质 / 编码配置 / 企业授权
- 平台运营：资质核实（跨租户）

## #5 剩余小口子（未做）

1. 计税方法（简易 / 一般）未建模，开票申请的票种也没受它约束。
2. 企业授权的「录入授权有效期」未做；状态是人工回填（工行无查询接口）。
3. 编码配置是**租户级**；「平台运营维护报废产品编码表」需跨租户/全局维度。
4. 到期预警只有 `expiring` 接口与标记，**没有主动通知/定时任务**。

## 下一步建议

- **A.** 补 #5 上述四个小口子；
- **B.** #6 出售者建档（实名/绑卡/入驻）——#7 的前置，但有工行外部前置（活体模式、小程序域名白名单，见 ADR 0011）；
- **C.** #11 一票一档证据链 / 齐备率 / 台账导出（无外部依赖，可持续产出）。

## 约定与坑

- **权限**：icbc 控制器用 `@icbc.hasPermission`（`RecyclingRoleEnum` 写死角色→权限）。新增权限**必须**在 `RecyclingPermission` + `RecyclingRoleEnum` 登记，否则连超管也 403（未登记即拒绝）。
- **工行 UI 页面**：预下单/付款/入驻返回的是自动提交表单 HTML，用 `src/views/icbc/util.ts` 的 `openIcbcForm()` 打新窗口，不能当 URL 跳。
- **新增 icbc 表**：工行返回字段（如 `payee_no`/`payer_no`）在本地库应为可空；表放 `backend/sql/mysql/`，菜单用 `icbc-menu.sql` 幂等维护。
- **测 icbc**：`mvn -pl yudao-module-icbc/yudao-module-icbc-biz test`；改了 `-api` 先 `mvn -pl ...-api -DskipTests install`。**不要**用 `-am test`（上游模块有既有失败会挡住 reactor）。
- **前端**：`pnpm build:local` 验证编译；`pnpm ts:check` 有 1247 个既有 TS 错误，判断自己的改动看 `src/(views|api)/icbc` 有无新报错即可（跑 ts:check 需加 `NODE_OPTIONS=--max-old-space-size=6144`）。
- **工具**：不要在同一条消息里同时发 `edit` 和依赖它的 `bash`（会并发，文件可能未落盘）。
