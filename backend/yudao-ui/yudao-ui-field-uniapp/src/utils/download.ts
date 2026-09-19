import { API_BASE_URL, getTenantId } from '@/config/env'
import { getToken } from '@/utils/auth'

/**
 * 带鉴权下载后端文件（如收购确认书 Excel）。
 * H5 下触发浏览器下载；小程序 / App 下用 openDocument 预览。
 */
export function downloadWithAuth(path: string, filename: string): Promise<void> {
  return new Promise<void>((resolve, reject) => {
    uni.downloadFile({
      url: API_BASE_URL + path,
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
          showMenu: true,
          success: () => resolve(),
          fail: (err) => reject(new Error(err.errMsg || '打开文件失败'))
        })
        // #endif
      },
      fail: (err) => reject(new Error(err.errMsg || '下载失败'))
    })
  })
}
