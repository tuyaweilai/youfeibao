/**
 * 打开工行 UI 页面接口返回的自动提交表单。
 *
 * 预下单、付方支付、收方入驻、实人认证这些工行接口是「UI 页面接口」：
 * 返回的不是 URL，而是一段自动 POST 到工行网关的表单 HTML。直接跳转 URL
 * 不行，必须把这段 HTML 写进新窗口，让浏览器替我们提交。
 */
export function openIcbcForm(formHtml: string, title = '工行页面') {
  if (!formHtml) {
    return
  }
  const w = window.open('', '_blank', 'width=560,height=800')
  if (!w) {
    // 弹窗被拦截时，退回 blob 新标签页
    const blob = new Blob([formHtml], { type: 'text/html' })
    window.open(URL.createObjectURL(blob), '_blank')
    return
  }
  w.document.open()
  w.document.write(formHtml)
  w.document.title = title
  w.document.close()
}
