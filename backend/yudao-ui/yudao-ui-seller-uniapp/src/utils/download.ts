import { API_BASE_URL } from '@/config/env'

/** 用令牌下载发票 PDF：H5 触发浏览器下载，小程序 / App 用 openDocument 预览 */
export function downloadInvoicePdf(token: string, filename = '发票.pdf'): Promise<void> {
  return new Promise<void>((resolve, reject) => {
    uni.downloadFile({
      url: `${API_BASE_URL}/icbc/public/invoice/download?token=${encodeURIComponent(token)}`,
      success: (res) => {
        if (res.statusCode !== 200) {
          reject(new Error(`下载失败（HTTP ${res.statusCode}）`))
          return
        }
        // #ifdef H5
        const link = document.createElement('a')
        link.href = res.tempFilePath
        link.download = filename
        document.body.appendChild(link)
        link.click()
        document.body.removeChild(link)
        resolve()
        // #endif
        // #ifndef H5
        uni.openDocument({
          filePath: res.tempFilePath,
          fileType: 'pdf',
          showMenu: true,
          success: () => resolve(),
          fail: (err) => reject(new Error(err.errMsg || '打开失败'))
        })
        // #endif
      },
      fail: (err) => reject(new Error(err.errMsg || '下载失败'))
    })
  })
}
