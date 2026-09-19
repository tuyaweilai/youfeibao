import { API_BASE_URL, APP_API_BASE_URL, getTenantId } from '@/config/env'
import { getToken } from '@/utils/auth'

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

/**
 * 带登录态下载后端文件（自然人端 `/app-api`）。
 */
export function downloadWithAuth(path: string, filename: string): Promise<void> {
  return new Promise<void>((resolve, reject) => {
    uni.downloadFile({
      url: APP_API_BASE_URL + path,
      header: {
        'tenant-id': getTenantId(),
        Authorization: `Bearer ${getToken()}`
      },
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

/**
 * 带登录态打开后端生成的 HTML（确认书）：H5 用浏览器「打印 / 保存为 PDF」。
 * 后端确认书端点要求登录态，`window.open` 带不了请求头，所以先取回 HTML 再写进新窗口。
 */
export function openHtmlWithAuth(path: string): Promise<void> {
  return new Promise<void>((resolve, reject) => {
    uni.request({
      url: APP_API_BASE_URL + path,
      header: {
        'tenant-id': getTenantId(),
        Authorization: `Bearer ${getToken()}`
      },
      dataType: 'text',
      success: (res) => {
        if (res.statusCode !== 200 || typeof res.data !== 'string') {
          reject(new Error(`打开失败（HTTP ${res.statusCode}）`))
          return
        }
        // #ifdef H5
        const win = window.open('', '_blank')
        if (!win) {
          reject(new Error('浏览器拦截了新窗口，请允许弹窗后重试'))
          return
        }
        win.document.open()
        win.document.write(res.data as string)
        win.document.close()
        resolve()
        // #endif
        // #ifndef H5
        reject(new Error('小程序端暂不支持直接打印，请用手机浏览器打开本页操作'))
        // #endif
      },
      fail: (err) => reject(new Error(err.errMsg || '打开失败'))
    })
  })
}
