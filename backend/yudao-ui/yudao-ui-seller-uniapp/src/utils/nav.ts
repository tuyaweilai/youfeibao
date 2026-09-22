/**
 * 底部导航（tabBar）相关的跳转与跨页参数。
 *
 * <p>原生的 `switchTab` **不能带 query**：场站编号、要选中的子页签只能先存起来，
 * 由目标页取走。收在这里，免得每处跳转各写一份键名。
 */

/** 目标页要预选的子页签（例如首页点「收款记录」直接进交易页的收款记录） */
const TAB_HINT_KEY = 'seller_tab_hint'

/**
 * 场站编号：**只活在本次进屋的会话里**，不进本地存储。
 *
 * <p>它是「这次扫的是哪家场站的码」的提示，用来按场站筛待办（#34）。存进 localStorage 会变成
 * 跨天跨库的陈旧值：场站被删或换过库之后，前端仍拿着旧编号去请求（2026-09-22 实测踩到，
 * 后端报 `场站不存在`、首页 500）。内存变量在同一份 SPA / 小程序运行时里跨页有效，
 * 刷新即清零，语义正好是「本次进屋」。
 *
 * <p>旧版本把它写在 localStorage 的 `seller_station_id` 里，现在没人读那个键了，
 * 浏览器里可能还留着，无害。
 */
let stationId: number | undefined

export type SellerTab = '/pages/home/index' | '/pages/transaction/index' | '/pages/my/index'

/** 记住这次进屋的场站编号（空值不覆盖已有的） */
export function rememberStationId(id?: string | number | null) {
  if (id === undefined || id === null || id === '') {
    return
  }
  const value = Number(id)
  if (value) {
    stationId = value
  }
}

/** 后端认不出这个场站（已删 / 换库）：忘掉它，别再拿它去筛 */
export function forgetStationId() {
  stationId = undefined
}

export function currentStationId(): number | undefined {
  return stationId
}

/** 切到底部导航的某一页；带 `hint` 时目标页会把它当作预选子页签 */
export function switchSellerTab(tab: SellerTab, hint?: string) {
  if (hint) {
    uni.setStorageSync(TAB_HINT_KEY, hint)
  }
  uni.switchTab({ url: tab })
}

/**
 * 取走预选子页签（取一次即清），没有时返回空串。
 * 一次性语义很重要：否则用户手动切到别的子页签，下次进页又被旧提示带回原处。
 */
export function takeTabHint(): string {
  const hint = uni.getStorageSync(TAB_HINT_KEY) || ''
  if (hint) {
    uni.removeStorageSync(TAB_HINT_KEY)
  }
  return hint
}

/** 登录后的落点：首页 tab（`switchTab` 是唯一能进 tabBar 页的跳法） */
export function goSellerHome() {
  switchSellerTab('/pages/home/index')
}
