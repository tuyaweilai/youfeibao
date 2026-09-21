# field-shared：现场端共享能力

收货员现场端与司机端**唯一真正共用**的能力：自然人**准入四步**（实名 → 收方入驻 → 框架收购协议 → 首次授权）。

决策见 [ADR 0030](../../../docs/adr/0030-司机现场只做准入与交接.md)（司机上门时现场没有收货员，四步要在司机手机上完成）、
[ADR 0033](../../../docs/adr/0033-司机端的准入能力由-icbc-侧按角色授予.md)（这套接口的权限由 icbc 侧按角色 code 授予）。

## 这里有什么

| 文件 | 内容 |
|---|---|
| `src/api/onboarding.ts` | 四步的接口：建档总览、发起/同步实人认证、发起/同步收方入驻、留联系方式、框架协议、首次授权 |
| `src/api/payee.ts` | 自然人档案：带档（身份证号 / 手机号）、建档 |
| `src/api/publicToken.ts` | 签发 / 作废一次性令牌（把建档链接交给出售者本人；#94 加了自填建档链接的作废） |
| `src/api/wizard.ts` | 建档向导（#91）的五步壳接口：无状态识别三枚 + 一次性落库 |
| `src/utils/icbcForm.ts` | 工行 UI 页面接口的自动提交表单：新窗口承载（ADR 0016） |
| `src/utils/realName.ts` | 实人认证状态的判定：**1 是「认证中」，只有 2 是「认证通过」**（#90） |
| `src/utils/onboarding.ts` | 收方入驻状态的判定：只有 `READY` 是「入驻完成」（#90 顺带修的同类问题） |
| `src/composables/useSellerOnboarding.ts` | **四步的业务逻辑与状态**：接口编排、校验、工行表单承载、提示文案 |
| `src/composables/useHandoffLink.ts` | **「交给出售者本人」的转达入口**：一次性令牌、链接、二维码与有效期文案（#91 复审 ST-B 收口，两端同一份）；#94 加了 `useWizardInviteLink`（绑定链接本身的本人自填建档链接，可作废） |

## 宿主怎么用

```ts
import { useSellerOnboarding, createPayee, findReturningCustomer } from '@youfeibao/field-shared'
```

宿主工程在 `vite.config.ts` 里把 `@youfeibao/field-shared` 指向本目录的 `src`（**直接吃源码，无构建产物**），
并在 `tsconfig.json` 的 `paths` 里登记同名映射。请求封装 `@/utils/request` 与登录态**由宿主提供**：
两端的存储键前缀不同（`field_` / `driver_`），恰恰是不该共享的部分。

## 两个踩过的坑（别再试一遍）

1. **不要试图跨工程共用一个 `.vue` 组件。** 试过三条路都失败：
   - 把组件放在 `packages/` 用别名引入 → `vue-tsc` 报满屏 `Property 'xxx' does not exist on type '{}'`（文件在 app 目录外时它不做 SFC 类型生成，看起来像路径问题，其实不是）；
   - 用 `node_modules` 里的 `file:` 依赖引入 → TS 不从 `node_modules` 里读 `.ts` 源码，得先给共享包加一层构建产物；Vite 也会因为包内的 `.vue` 与 `@/` 解析失败而构建不过；
   - 把组件软链进各端 `src/` → `vue-tsc` 解析到软链的**真实路径**，又回到第一种情况。
   所以结论是：**共享逻辑（API + composable）不共享模板**。四步的接口、状态与校验只有一份，各端只写自己的模板与文案。
2. **`defineEmits` 不要用调用签名写法**（`defineEmits<{(e: 'x'): void}>()`）。在本工程的 `vue-tsc` 版本下它会让整个 `<script setup>` 的绑定解析失败，症状与上一条一模一样，白白排查很久。用对象形式：`defineEmits<{ x: [] }>()`。
