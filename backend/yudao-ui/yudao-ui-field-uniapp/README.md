# 收货员现场端（uni-app，一期 H5）

手机 / 平板上完成一笔收购的现场登记。形态与基座的决策见 `docs/adr/0011`（终端矩阵）与 `docs/adr/0016`（基座与工行页面承载）。

## 现状

**#22 工程骨架**：最小 uni-app（vue3 + vite + TS + pinia），一期只发 H5。业务页面按子票推进：

- #23 工程骨架与登录（请求封装、鉴权、导航）
- #24 收购登记主流程（配置带出、门禁与确认书）
- #25 现场照片与识别回填
- #26 新出售者一次性手续（工行 H5 容器）
- #27 弱网暂存与离线补传

## 开发

```bash
cd backend/yudao-ui/yudao-ui-field-uniapp
pnpm install
pnpm dev:h5        # 本地开发，默认 5173
pnpm build:h5      # 生产构建，产物在 dist/
pnpm ts:check      # 类型检查
```

Node >= 16、pnpm >= 8.6（与 PC 后台一致）。

## 一期边界（H5）

- **相机**：`uni.chooseImage` / `<input capture>` 拍磅单、车牌、车头车尾；无原生扫码。
- **身份证带档**：手输身份证号或手机号 + `/icbc/seller-onboarding/returning-customer`；「扫身份证」不在一期（H5 无 NFC）。
- **工行 UI 页面**：实人认证 / 收方入驻的自动提交表单由后端下发，H5 用新窗口 / 新标签打开，前端不拼工行 URL（ADR 0009 / 0016）。
- **离线**：IndexedDB 存照片 blob、localStorage 存草稿，恢复后 `POST /icbc/acquisition/sync-offline` 幂等补传。
- **识别**：一期无真实 OCR，磅单 / 车牌默认手工录入（ADR 0013）；识别端口为 stub 返回空时不阻断。

## H5 能力冒烟清单（需真机 / 微信内置浏览器人工跑）

工程骨架可构建（`pnpm build:h5` 通过），但 H5 的运行时能力必须在真机确认，结论回写到本文件：

- [ ] 相机：`uni.chooseImage(sourceType: ['camera','album'])` 能拿到照片（磅单、车牌、车头车尾）。
- [ ] 离线：断网后能写入 IndexedDB（照片 blob）与 localStorage（草稿），恢复后可读回。
- [ ] 工行表单：后端下发的自动提交表单 HTML 用新窗口打开能正常 POST 到工行（沙箱环境不可用则记录）。
- [ ] 跨域 / 域名：微信内置浏览器能打开我们的 H5 页并跳工行页。
- [ ] 手输身份证 / 手机号经 `/returning-customer` 能带出老档案。

任何一项不达标，升级到小程序 / App 渠道（同一套 uni-app 代码，ADR 0016）。

## 目录

```
src/
  App.vue           应用根（启动钩子）
  main.ts           入口（vue3 + pinia）
  pages.json        路由与窗口样式
  manifest.json     应用标识与渠道配置
  pages/home/       首页
  env.d.ts          TS 类型声明
```
