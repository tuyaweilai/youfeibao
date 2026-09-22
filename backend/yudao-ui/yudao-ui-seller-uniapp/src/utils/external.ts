/**
 * 打开第三方外部链接（#95）：H5 直接跳转，小程序 / App 用 `pages/webview` 的 `<web-view>` 承接。
 *
 * 小程序 / App 不能直接打开外部 URL，必须有一个 web-view 页面；H5 则不需要中间页。
 * 自然人端首页的「去签署」与本页（ONBOARDING 落点）的「去签署」是同一个动作，
 * 收在这里免得两份复制各漂一半。
 */
export function openExternalUrl(url: string, preferNewTab = false) {
  if (!url) {
    return
  }
  // #ifdef H5
  // 多步流程（如逐张确认开票信息）留在本站更好：能新开标签就别把当前页顶掉；
  // 被浏览器拦下时退回同标签跳转，不静默失败
  if (preferNewTab && window.open(url, '_blank')) {
    return
  }
  window.location.href = url
  // #endif
  // #ifndef H5
  uni.navigateTo({ url: `/pages/webview/index?url=${encodeURIComponent(url)}` })
  // #endif
}
