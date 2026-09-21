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

/** 把本地图片路径读成 base64 dataURL，便于弱网时把照片一起暂存到本地 */
export async function pathToDataUrl(tempPath: string): Promise<string> {
  // #ifdef H5
  const blob = await (await fetch(tempPath)).blob()
  return await new Promise<string>((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(reader.result as string)
    reader.onerror = () => reject(new Error('读取照片失败'))
    reader.readAsDataURL(blob)
  })
  // #endif
  // #ifndef H5
  return tempPath
  // #endif
}

/** 估算 dataURL 里 base64 部分的长度（不含 `data:...;base64,` 前缀） */
function base64Length(dataUrl: string): number {
  const index = dataUrl.indexOf(',')
  return index >= 0 ? dataUrl.length - index - 1 : dataUrl.length
}

/**
 * 把图片压到 base64 不超过 `maxBase64Length`（腾讯 OCR 单张上限 10M，#93）。
 *
 * 现场手机原图常超限：`chooseImage` 的 compressed 只是一种压缩，兜不住大图。H5 用 canvas
 * 先按需降 JPEG 质量、再降分辨率；非 H5 交回原图，由调用方做上限拦截。压到不能再压仍超限时
 * 返回最小结果，让调用方给出可读提示——不静默把一张必被厂商拒绝的图传上去。
 */
export async function compressDataUrl(dataUrl: string, maxBase64Length: number): Promise<string> {
  // 本来就没超限就原样返回：避免无谓的二次编码损失
  if (base64Length(dataUrl) <= maxBase64Length) {
    return dataUrl
  }
  // #ifdef H5
  const image = await loadImage(dataUrl)
  let width = image.naturalWidth || image.width
  let height = image.naturalHeight || image.height
  let quality = 0.8
  const first = renderJpeg(image, width, height, quality)
  if (!first) {
    // 拿不到 2d context：压不了，交回原图由调用方拦截（不静默传一张必被厂商拒的图）
    return dataUrl
  }
  let result = first
  for (let attempt = 0; attempt < 24 && base64Length(result) > maxBase64Length; attempt++) {
    if (quality > 0.4) {
      quality = Math.max(0.4, Number((quality - 0.1).toFixed(2)))
    } else {
      if (width <= 320 || height <= 320) {
        // 已经降到下限仍超限：停，别拿巨大字符串反复空转
        break
      }
      width = Math.max(1, Math.round(width * 0.8))
      height = Math.max(1, Math.round(height * 0.8))
      quality = 0.7
    }
    const next = renderJpeg(image, width, height, quality)
    if (!next) {
      break
    }
    result = next
  }
  return result
  // #endif
  // #ifndef H5
  return dataUrl
  // #endif
}

// #ifdef H5
function loadImage(src: string): Promise<HTMLImageElement> {
  return new Promise<HTMLImageElement>((resolve, reject) => {
    const image = new Image()
    image.onload = () => resolve(image)
    image.onerror = () => reject(new Error('读取照片失败'))
    image.src = src
  })
}

/** 渲染 JPEG；拿不到 2d context 时返回 null，由调用方决定降级（不再把原图当压缩结果）。 */
function renderJpeg(
  image: HTMLImageElement,
  width: number,
  height: number,
  quality: number
): string | null {
  const canvas = document.createElement('canvas')
  canvas.width = width
  canvas.height = height
  const context = canvas.getContext('2d')
  if (!context) {
    return null
  }
  context.drawImage(image, 0, 0, width, height)
  return canvas.toDataURL('image/jpeg', quality)
}
// #endif

/** 把 base64 dataURL 还原成上传接口可用的本地路径 */
export function dataUrlToUploadPath(dataUrl: string): string {
  // #ifdef H5
  const [meta, base64] = dataUrl.split(',')
  const mime = meta.match(/:(.*?);/)?.[1] || 'image/jpeg'
  const binary = atob(base64)
  const bytes = new Uint8Array(binary.length)
  for (let i = 0; i < binary.length; i++) {
    bytes[i] = binary.charCodeAt(i)
  }
  return URL.createObjectURL(new Blob([bytes], { type: mime }))
  // #endif
  // #ifndef H5
  return dataUrl
  // #endif
}
