# 自然人出售者端（uni-app，一期微信小程序优先 / H5 兜底）

给卖废品的自然人用的薄前端：**不建账号**，由回收企业签发的**一次性令牌**进入，提供发票下载、额度查询、汇算清缴对账单、留联系方式。形态与终端矩阵见 `docs/adr/0011`。

## 现状（#28）

- `pages/index`：从启动参数 / URL 取 `token` 与 `purpose`；按用途只放行对应功能（`QUOTA_QUERY` / `INVOICE_DOWNLOAD` / `SETTLEMENT_STATEMENT` / `CONTACT_LEAD`），没带用途时给出全部入口。
- 我的额度：`GET /icbc/public/quota`（滚动 500 万、1% / 3% 分列、本月 10 万免征线）。
- 我的发票：`GET /icbc/public/invoice/download`（PDF，H5 下载 / 小程序 openDocument）。
- 汇算清缴：`GET /icbc/public/settlement`。
- 留联系方式：`POST /icbc/public/contact-lead`。
- **实名与收方入驻（#29）**：`ONBOARDING` 用途的令牌进入，`POST /icbc/public/onboarding/sync` 取当前步骤，`GET /icbc/public/onboarding/form` 打开工行的自动提交表单（小程序用 `web-view`、H5 用新窗口），跳回后再 sync 收敛。工行页面内容由后端生成，前端不拼工行 URL。

**承载工行的实名 / 开票确认 / 收方入驻页面**：容器问题已由工行答复解除（实人认证走工行 H5 活体，H5 与小程序 `web-view` 都可用，不做 App）。

## 开发

```bash
cd backend/yudao-ui/yudao-ui-seller-uniapp
cp .env.example .env   # 可选，默认值即可跑
pnpm install
pnpm dev:h5            # http://localhost:5174，/admin-api 代理到 48080
pnpm build:h5
pnpm build:mp-weixin   # 产物在 dist/build/mp-weixin，用微信开发者工具打开
pnpm ts:check
```

入口链接形如 `https://<seller-app>/#/?token=<一次性令牌>&purpose=QUOTA_QUERY`。令牌由回收企业在「额度台账 / 一票一档 / 代办税费申报」页签发。

## 目录

```
src/
  App.vue            应用根
  main.ts            入口
  pages.json         路由
  manifest.json      渠道配置
  config/env.ts      API 基地址
  utils/request.ts   公开端点请求（不带 token 头 / tenant-id）
  utils/token.ts     入口令牌与用途
  utils/download.ts  发票 PDF 下载
  api/public.ts      公开端点
  pages/index/       入口与四个功能
```
