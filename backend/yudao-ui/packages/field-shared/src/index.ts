/**
 * 现场端共享能力（V7 #74）。
 *
 * **为什么现在才抽**：V2c 时只有两台端、共享面还小，过早抽象会把两边绑死；到 V7 这份「自然人准入四步」
 * 成了收货员与司机**唯一真正共用**的能力（两边几乎不共享页面），才值得抽出来。
 *
 * **边界**：这里只放「两个端都一模一样」的东西——准入四步的接口、工行表单承载、四步的业务逻辑（composable）。
 * **模板不在共享范围内**：跨工程共用一个 `.vue` 会与 uni-app + vue-tsc 打架，`README.md` 里记了试过的三条路。
 * 请求封装（`@/utils/request`）与登录态由**宿主工程**提供：两端的存储键前缀不同（`field_` / `driver_`），
 * 恰恰是不该共享的部分。
 */
export * from './api/onboarding'
export * from './api/payee'
export * from './api/publicToken'
export { openIcbcFormHtml } from './utils/icbcForm'
export { useSellerOnboarding } from './composables/useSellerOnboarding'
