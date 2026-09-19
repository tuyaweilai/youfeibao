/**
 * 打开工行 UI 页面接口返回的自动提交表单。
 *
 * 实人认证、收方入驻等是「UI 页面接口」：返回的不是 URL，而是一段自动 POST 到工行网关的
 * 表单 HTML，必须由浏览器替我们提交（ADR 0009）。ADR 0016 定了现场端 H5 用新窗口承载。
 */
export function openIcbcFormHtml(formHtml: string, title = '工行页面') {
  if (!formHtml) {
    return
  }
  // #ifdef H5
  const target = window.open('', '_blank', 'width=560,height=800')
  if (!target) {
    // 弹窗被拦截时退回 blob 新标签页
    const blob = new Blob([formHtml], { type: 'text/html' })
    window.open(URL.createObjectURL(blob), '_blank')
    return
  }
  target.document.open()
  target.document.write(formHtml)
  target.document.title = title
  target.document.close()
  // #endif
  // #ifndef H5
  // 小程序 / App 渠道改用 web-view 承载（后续渠道），一期 H5 不做
  uni.showModal({
    title: '当前渠道不支持',
    content: '请在浏览器（H5）中打开工行页面',
    showCancel: false
  })
  // #endif
}
