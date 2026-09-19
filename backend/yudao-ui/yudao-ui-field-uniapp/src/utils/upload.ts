import { API_BASE_URL, getTenantId } from '@/config/env'
import { getToken } from '@/utils/auth'

/** yudao 统一响应体 */
interface CommonResult<T> {
  code: number
  data: T
  msg: string
}

/** 拍照 / 从相册选图，返回本地临时路径 */
export function chooseImage(count = 1): Promise<string[]> {
  return new Promise<string[]>((resolve, reject) => {
    uni.chooseImage({
      count,
      sizeType: ['compressed'],
      sourceType: ['camera', 'album'],
      success: (res) => resolve(res.tempFilePaths as string[]),
      fail: (err) => reject(new Error(err.errMsg || '选择照片失败'))
    })
  })
}

/** 上传单张图片到 yudao 文件服务，返回可访问的文件 URL */
export function uploadImage(filePath: string): Promise<string> {
  return new Promise<string>((resolve, reject) => {
    uni.uploadFile({
      url: API_BASE_URL + '/infra/file/upload',
      filePath,
      name: 'file',
      header: {
        'tenant-id': getTenantId(),
        Authorization: `Bearer ${getToken()}`
      },
      success: (res) => {
        if (res.statusCode !== 200) {
          reject(new Error(`上传失败（HTTP ${res.statusCode}）`))
          return
        }
        try {
          const body = JSON.parse(res.data) as CommonResult<string>
          if (body.code !== 0) {
            reject(new Error(body.msg || '上传失败'))
            return
          }
          resolve(body.data)
        } catch {
          reject(new Error('上传响应解析失败'))
        }
      },
      fail: (err) => reject(new Error(err.errMsg || '上传失败'))
    })
  })
}

/** 拍照并上传，返回文件 URL */
export async function captureAndUpload(): Promise<string> {
  const paths = await chooseImage(1)
  if (!paths.length) {
    throw new Error('未选择照片')
  }
  return uploadImage(paths[0])
}
