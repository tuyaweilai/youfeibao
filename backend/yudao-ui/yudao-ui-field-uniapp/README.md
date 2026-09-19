# 收货员现场端（uni-app，一期 H5）

手机 / 平板上完成一笔收购的现场登记。形态与基座的决策见 `docs/adr/0011`（终端矩阵）与 `docs/adr/0016`（基座与工行页面承载）。

## 现状

- **#22 工程骨架**：最小 uni-app（vue3 + vite + TS + pinia），一期只发 H5。
- **#23 登录与导航**：`src/utils/request.ts` 统一带 `tenant-id` + token，401 清登录态回登录页；`src/store/auth.ts` 持久化登录态；首页进入「收购登记 / 出售者建档 / 我的收购单」，未登录拦截。
- **#24 收购登记主流程**：`pages/acquisition/index` 选品类带出单位 / 税率 / 计税方法 / 税收分类编码，身份证或手机号带档，数量 / 单价自动算金额、毛重 − 皮重自动算净重，漏填品类或磅单号拦住提交，提交后展示额度提示；`pages/acquisition/detail` 看进度（已登记 / 待付款 / 已付款 / 已开票）、可打印并导出确认书 Excel；`pages/my-acquisitions` 列单据进详情。
- **#25 现场照片与识别回填**：拍磅单 / 车头 / 车尾照片并上传（`/infra/file/upload`），车牌手输并实时比对（一致 / 不一致 / 无法比对）；详情页可「修正识别结果」调 `/icbc/acquisition/correct` 后重新比对。一期无真实 OCR，重量与车牌默认手工录入（ADR 0013）。
- 待后续子票：#26 新出售者一次性手续（工行 H5 容器）、#27 弱网暂存与补传。

## 开发

```bash
cd backend/yudao-ui/yudao-ui-field-uniapp
pnpm install
pnpm dev:h5        # 本地开发，默认 http://localhost:5173，/admin-api 代理到 http://localhost:48080
pnpm build:h5      # 生产构建，产物在 dist/
```

Node >= 16、pnpm >= 8.6（与 PC 后台一致）。后端需按根目录 README 起本地环境；默认登录租户 `1`、账号 `admin` / `admin123`（登录页可改租户编号）。

API 基地址由 `.env` 的 `VITE_APP_BASE_URL` 控制（默认相对路径 `/admin-api`）；生产部署时前端与后端同源即可，或改成后端地址。

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
  App.vue           应用根（全局样式）
  main.ts           入口（vue3 + pinia）
  pages.json        路由与窗口样式
  manifest.json     应用标识与渠道配置
  config/env.ts     API 基地址与租户编号
  utils/request.ts  统一请求封装（tenant-id + token + 401 处理）
  utils/auth.ts     token / 用户信息本地存储
  utils/download.ts 带鉴权下载（确认书 Excel）
  utils/upload.ts   拍照 + 上传到文件服务
  utils/plate.ts    车牌比对归一化
  store/auth.ts     pinia 登录态
  api/auth.ts       登录 / 登出 / 权限信息接口
  api/goodsConfig.ts 启用品类
  api/payee.ts      回头客带档
  api/acquisition.ts 收购登记
  pages/login/      登录页
  pages/home/       首页（导航 + 退出）
  pages/acquisition/ 登记表单 + 确认书详情
  pages/payee/      出售者建档（占位，#26）
  pages/my-acquisitions/ 我的收购单
  env.d.ts          TS 类型声明
```
