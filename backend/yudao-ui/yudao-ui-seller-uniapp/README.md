# 自然人出售者端（uni-app，一期微信小程序优先 / H5 兜底）

给卖废品的自然人用的终端。两条进入路径：

1. **一次性令牌**（回收企业签发）：免登录只读，发票下载、额度查询、汇算清缴对账单、留联系方式、工行实名 / 收方入驻、本人自填建档。
2. **场站二维码**（#34，印在磅房 / 墙上）：码内**不带任何令牌**，只编码场站码；扫码先看公开信息，再用手机号验证查看「我的待确认」与记录。

形态与终端矩阵见 `docs/adr/0011`；对外口径见 `docs/adr/0021`（金额只讲「本平台累计」、不出现「已到账」、付款 / 开票 / 税费三条状态线分别显示）。

## 现状

### #28 / #29 令牌路径（保留，未收紧）

- `pages/index`：从启动参数 / URL 取 `token` 与 `purpose`；按用途只放行对应功能（`QUOTA_QUERY` / `INVOICE_DOWNLOAD` / `SETTLEMENT_STATEMENT` / `CONTACT_LEAD` / `ONBOARDING` / `ONBOARDING_WIZARD`）。
- 我的额度：`GET /icbc/public/quota`；我的发票：`GET /icbc/public/invoice/download`；汇算清缴：`GET /icbc/public/settlement`；留联系方式：`POST /icbc/public/contact-lead`；工行实名 / 入驻：`GET /icbc/public/onboarding/form`（后端生成自动提交表单，小程序 `web-view`、H5 新窗口）。

### #34 场站扫码路径（本次）

- `pages/station`：`GET /admin-api/icbc/public/station?code=场站码` 解析公开信息（回收企业、场站、地址、是否在收货、场站电话），**只有公开信息，不含任何个人数据**；读取按 IP 限流。
- `pages/login`：手机号 + 短信验证码（`/app-api/icbc/seller/auth/*`）。登录凭证落在平台租户；首次进入时用登录手机号在本租户内匹配收方档案并绑定（`subjects/bind-by-mobile`）。匹配不到就进首页给明确空态，不造假列表。
- `pages/home`：五项顺序为 **待我确认 / 我的记录 / 收款记录 / 发票与税费 / 我的资料**。
  - 待我确认：只放需要他动作的（待签协议、待确认结算单、企业改过需重新确认的异议单）。
  - 我的记录：卖货记录按回收企业分组（跨企业仅本人可见）；单笔确认书可打印。
  - 收款记录：待付款 / 处理中 / **银行已受理（回单号）** / 失败（给下一步）；**「我收到了」只记自然人自行确认，不改银行状态**。
  - 发票与税费：年度汇总 + 逐票 PDF 下载；开票 / 税费 / 上传三条状态线分别显示。
  - 我的资料：收款账户尾号、变更银行卡入口（S7）、企业授权列表与自助撤销、联系方式、客服。
- `pages/settlement/detail`：确认结算（勾选）/ 有异议（固定原因枚举 + 说明），并显示企业回复与「企业尚未回复」。
- **单笔收购 / 结算确认书**：后端 `text/html` 打印页（`.../acquisition/confirmation`、`.../settlement/confirmation`），H5 用浏览器「打印 / 保存为 PDF」；一期不引 PDF 库、不做批量 Excel 导出。

## 开发

```bash
cd backend/yudao-ui/yudao-ui-seller-uniapp
cp .env.example .env   # 可选；默认 /admin-api、/app-api、tenant 1
pnpm install
pnpm dev:h5            # http://localhost:5174，/admin-api 与 /app-api 都代理到 48080
pnpm build:h5
pnpm build:mp-weixin   # 产物在 dist/build/mp-weixin，用微信开发者工具打开
pnpm ts:check
```

- 令牌入口：`https://<seller-app>/#/?token=<一次性令牌>&purpose=QUOTA_QUERY`
- 场站入口：`https://<seller-app>/#/?station=<场站码>`（后台「场站」页可复制 / 生成二维码）

## 目录

```
src/
  pages/index/        令牌入口（一次性令牌的五类功能）
  pages/station/      场站二维码首屏（公开信息）
  pages/login/        手机号验证与身份匹配
  pages/home/         首页五项（待确认 / 记录 / 收款 / 发票 / 资料）
  pages/settlement/   结算确认与异议
  config/env.ts       /admin-api 与 /app-api 基地址、当前租户
  utils/request.ts    公开端点请求 + 自然人端登录态请求
  utils/token.ts      一次性令牌、场站码解析
  utils/auth.ts       会员令牌与当前自然人主体
  store/auth.ts       登录态单例
  api/public.ts       公开令牌端点 + 场站解析
  api/seller.ts       登录、身份绑定、首页与记录、结算确认
```
